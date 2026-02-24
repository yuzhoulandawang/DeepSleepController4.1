package com.example.deepsleep.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

class FreezerService : Service() {
    private val TAG = "FreezerService"

    private val FROZEN_GROUP = "/dev/freezer/frozen"
    private val THAW_GROUP = "/dev/freezer/thaw"
    private val FREEZE_DELAY = 30L
    private val MONITOR_LEVEL = 2
    private val FG_CACHE_FILE = "/dev/local/tmp/fg_app.cache"
    private val FG_CACHE_TTL = 2
    private val STATE_DIR = "/dev/local/tmp/freeze_state"
    private val WORKER_DIR = "$STATE_DIR/workers"

    private val SYSTEM_WHITELIST = setOf(
        "com.android.inputmethod.latin",
        "com.spotify.music",
        "com.android.launcher3",
        "com.android.dialer",
        "com.android.systemui"
    )

    private val ENABLE_FREEZER = true
    private var lastFgApp: String? = null
    private var lastFgCheckTime = 0L
    private var currentFgApp: String? = null
    private var currentIsSystem = false
    private val tokenMap = ConcurrentHashMap<String, String>()
    private val freezeTasks = ConcurrentHashMap<String, Job?>()
    private val monitorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        try {
            Shell.cmd("mkdir -p $FROZEN_GROUP", "mkdir -p $THAW_GROUP").exec()
            Shell.cmd("echo FROZEN > $FROZEN_GROUP/freezer.state").exec()
            Shell.cmd("echo THAWED > $THAW_GROUP/freezer.state").exec()
            Shell.cmd("mkdir -p $STATE_DIR", "mkdir -p $WORKER_DIR").exec()
            Log.i(TAG, "Freezer groups initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startMonitor()
        return START_STICKY
    }

    private fun startMonitor() {
        monitorScope.launch {
            currentFgApp = getForegroundApp()
            currentIsSystem = isSystemApp(currentFgApp)
            writeFgCache(currentFgApp)
            Log.i(TAG, "Monitor started, initial FG: $currentFgApp (system=$currentIsSystem)")

            val tags = when (MONITOR_LEVEL) {
                1 -> listOf("-b", "events", "-s", "wm_set_resumed_activity:V", "ActivityManager:V")
                2 -> listOf("-b", "events", "-b", "system", "-b", "main", "-v", "tag", "-s",
                    "wm_set_resumed_activity:V", "ActivityManager:V", "WindowManager:V")
                3 -> listOf("-b", "all", "-s", "wm_set_resumed_activity:V")
                else -> listOf("-b", "events", "-s", "wm_set_resumed_activity:V")
            }

            try {
                val process = ProcessBuilder("logcat", *tags.toTypedArray()).redirectErrorStream(true).start()
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                reader.useLines { lines ->
                    lines.forEach { line ->
                        parseLogcatLine(line)?.let { pkg -> handleForegroundChange(pkg) }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Monitor failed", e)
            }
        }
    }

    private fun parseLogcatLine(line: String): String? {
        val skipKeywords = listOf("recents_animation_input_consumer", "SnapshotStartingWindow",
            "InputMethod", "NOT_VISIBLE", "NO_WINDOW", "Update InputWindowHandle", "finishDrawingLocked",
            "showSurfaceRobustly", "interceptKey", "drawing", "token", "laying", "animat",
            "orientation", "config", "type=1400", "leaving", "Ignore")

        if (skipKeywords.any { line.contains(it) }) return null

        var pkg: String? = null
        var isSwitch = false

        when {
            line.contains("wm_set_resumed_activity") -> {
                val afterBracket = line.substringAfter("[", "")
                val afterComma = afterBracket.substringAfter(",", "")
                pkg = afterComma.substringBefore("/")
                if (!pkg.isNullOrBlank()) isSwitch = true
            }
            line.contains("cmp=") -> {
                val cmp = line.substringAfter("cmp=", "")
                pkg = cmp.substringBefore("/")
                isSwitch = true
            }
            line.contains("moveTaskToFront") || line.contains("realActivity=") -> {
                val real = line.substringAfter("realActivity=", "")
                pkg = real.substringBefore("/")
                isSwitch = true
            }
        }

        if (isSwitch && !pkg.isNullOrBlank()) {
            pkg = pkg.replace("}", "").replace(":", "").trim()
            if (pkg.contains(".") && !pkg.contains("=") && !pkg.contains("[") && !pkg.contains("]")) {
                return pkg
            }
        }
        return null
    }

    private fun handleForegroundChange(newPkg: String) {
        if (newPkg == currentFgApp) return
        val isNewSystem = isSystemApp(newPkg)

        if (isNewSystem && currentIsSystem) {
            currentFgApp = newPkg
            writeFgCache(newPkg)
            return
        }

        if (checkFileExists("$STATE_DIR/$newPkg.token")) {
            val newToken = generateToken()
            tokenMap[newPkg] = newToken
            writeFile("$STATE_DIR/$newPkg.token", newToken)
            unfreezePackage(newPkg)
            currentFgApp = newPkg
            currentIsSystem = isNewSystem
            writeFgCache(newPkg)
            Log.d(TAG, "Switch to previously frozen app: $newPkg")
            return
        }

        if (newPkg != currentFgApp) {
            val newToken = generateToken()
            tokenMap[newPkg] = newToken
            writeFile("$STATE_DIR/$newPkg.token", newToken)
            unfreezePackage(newPkg)

            if (ENABLE_FREEZER && !currentFgApp.isNullOrBlank() && !currentIsSystem) {
                val fgApp = currentFgApp
                if (fgApp != null && !hasActiveWorker(fgApp)) {
                    val oldToken = generateToken()
                    tokenMap[fgApp] = oldToken
                    writeFile("$STATE_DIR/$fgApp.token", oldToken)
                    startWorker(fgApp, oldToken)
                }
            }

            currentFgApp = newPkg
            currentIsSystem = isNewSystem
            writeFgCache(newPkg)
            Log.d(TAG, "Switch to app: $newPkg (system=$isNewSystem)")
        }
    }

    private fun isSystemApp(pkg: String?): Boolean {
        if (pkg.isNullOrBlank()) return false
        if (SYSTEM_WHITELIST.contains(pkg)) return true
        val pid = getPidsForPackage(pkg).firstOrNull() ?: return false
        val uid = readUidFromProc(pid) ?: return false
        return uid < 10000
    }

    private fun readUidFromProc(pid: Int): Int? {
        val result = Shell.cmd("cat /proc/$pid/status").exec()
        val line = result.out.firstOrNull { it.startsWith("Uid:") } ?: return null
        return line.split(Regex("\\s+")).getOrNull(1)?.toIntOrNull()
    }

    private fun getPidsForPackage(pkg: String): List<Int> {
        return try {
            val result = Shell.cmd("pgrep -f $pkg").exec()
            result.out.mapNotNull { it.toIntOrNull() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun freezePackage(pkg: String) {
        try {
            val pids = getPidsForPackage(pkg)
            pids.forEach { pid ->
                Shell.cmd("echo $pid >> $FROZEN_GROUP/tasks").exec()
            }
            Log.i(TAG, "FROZEN: $pkg (PIDs: $pids)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to freeze $pkg", e)
        }
    }

    private fun unfreezePackage(pkg: String) {
        try {
            val pids = getPidsForPackage(pkg)
            pids.forEach { pid ->
                Shell.cmd("echo $pid >> $THAW_GROUP/tasks").exec()
            }
            Log.i(TAG, "THAWED: $pkg")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to thaw $pkg", e)
        }
    }

    private fun generateToken(): String {
        val uptime = runCatching {
            Shell.cmd("cat /proc/uptime").exec().out.firstOrNull()?.substringBefore(" ")
        }.getOrNull()
        return "${uptime ?: System.currentTimeMillis()}.${System.currentTimeMillis()}"
    }

    private fun hasActiveWorker(pkg: String): Boolean {
        val files = runCatching {
            Shell.cmd("ls $WORKER_DIR | grep ${pkg}_").exec().out
        }.getOrNull() ?: return false
        return files.any { fileName ->
            val pid = runCatching { Shell.cmd("cat $WORKER_DIR/$fileName").exec().out.firstOrNull()?.toIntOrNull() }.getOrNull()
            if (pid != null && Shell.cmd("[ -d /proc/$pid ]").exec().isSuccess) {
                true
            } else {
                Shell.cmd("rm -f $WORKER_DIR/$fileName").exec()
                false
            }
        }
    }

    private fun startWorker(pkg: String, token: String) {
        val job = monitorScope.launch {
            delay(FREEZE_DELAY * 1000)
            val currentToken = runCatching {
                Shell.cmd("cat $STATE_DIR/$pkg.token").exec().out.firstOrNull()
            }.getOrNull()
            if (token == currentToken && !isSystemApp(pkg)) {
                freezePackage(pkg)
            } else {
                Log.d(TAG, "Worker cancelled for $pkg")
            }
            freezeTasks.remove(pkg)
        }
        freezeTasks[pkg] = job
    }

    private fun getForegroundApp(): String? {
        val cacheFileExists = runCatching { Shell.cmd("[ -f $FG_CACHE_FILE ]").exec().isSuccess }.getOrDefault(false)
        if (cacheFileExists) {
            val cached = runCatching { Shell.cmd("cat $FG_CACHE_FILE").exec().out.firstOrNull() }.getOrNull()
            if (!cached.isNullOrBlank()) return cached
        }

        val windowOutput = runCatching {
            Shell.cmd("dumpsys window").exec().out.joinToString("\n")
        }.getOrNull() ?: return null

        for (line in windowOutput.lines()) {
            if (line.contains("mCurrentFocus") || line.contains("mFocusedApp")) {
                val regex = Regex("u[0-9]+\\s+([a-zA-Z0-9.]+)")
                val match = regex.find(line)
                if (match != null) return match.groupValues[1]
            }
        }
        return null
    }

    private fun writeFgCache(pkg: String?) {
        if (pkg.isNullOrBlank()) return
        Shell.cmd("echo $pkg > $FG_CACHE_FILE").exec()
        lastFgApp = pkg
        lastFgCheckTime = System.currentTimeMillis() / 1000
    }

    private fun checkFileExists(path: String): Boolean {
        return Shell.cmd("[ -f $path ]").exec().isSuccess
    }

    private fun writeFile(path: String, content: String) {
        Shell.cmd("echo $content > $path").exec()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        monitorScope.cancel()
        freezeTasks.values.forEach { it?.cancel() }
    }
}