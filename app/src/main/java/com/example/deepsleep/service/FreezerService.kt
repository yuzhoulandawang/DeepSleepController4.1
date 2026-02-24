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

    companion object {
        private var enabled = false
        private var delaySeconds = 30

        fun updateConfig(enabled: Boolean, delaySeconds: Int) {
            this.enabled = enabled
            this.delaySeconds = delaySeconds
        }
    }

    private val FROZEN_GROUP = "/dev/freezer/frozen"
    private val THAW_GROUP = "/dev/freezer/thaw"
    private val STATE_DIR = "/dev/local/tmp/freeze_state"
    private val WORKER_DIR = "$STATE_DIR/workers"

    private val SYSTEM_WHITELIST = setOf(
        "com.android.inputmethod.latin",
        "com.spotify.music",
        "com.android.launcher3",
        "com.android.dialer",
        "com.android.systemui"
    )

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
        if (enabled) {
            startMonitor()
        }
        return START_STICKY
    }

    private fun startMonitor() {
        monitorScope.launch {
            currentFgApp = getForegroundApp()
            currentIsSystem = isSystemApp(currentFgApp)
            Log.i(TAG, "Monitor started, initial FG: $currentFgApp (system=$currentIsSystem)")

            val tags = listOf("-b", "events", "-s", "wm_set_resumed_activity:V")
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
            "InputMethod", "NOT_VISIBLE", "NO_WINDOW")
        if (skipKeywords.any { line.contains(it) }) return null

        var pkg: String? = null
        when {
            line.contains("wm_set_resumed_activity") -> {
                val afterBracket = line.substringAfter("[", "")
                val afterComma = afterBracket.substringAfter(",", "")
                pkg = afterComma.substringBefore("/")
            }
            line.contains("cmp=") -> {
                val cmp = line.substringAfter("cmp=", "")
                pkg = cmp.substringBefore("/")
            }
        }
        return pkg?.replace("}", "")?.replace(":", "")?.trim()?.takeIf { it.contains(".") }
    }

    private fun handleForegroundChange(newPkg: String) {
        if (newPkg == currentFgApp) return
        val isNewSystem = isSystemApp(newPkg)

        if (isNewSystem && currentIsSystem) {
            currentFgApp = newPkg
            return
        }

        if (checkFileExists("$STATE_DIR/$newPkg.token")) {
            val newToken = generateToken()
            tokenMap[newPkg] = newToken
            writeFile("$STATE_DIR/$newPkg.token", newToken)
            unfreezePackage(newPkg)
            currentFgApp = newPkg
            currentIsSystem = isNewSystem
            return
        }

        if (newPkg != currentFgApp) {
            val newToken = generateToken()
            tokenMap[newPkg] = newToken
            writeFile("$STATE_DIR/$newPkg.token", newToken)
            unfreezePackage(newPkg)

            if (enabled && !currentFgApp.isNullOrBlank() && !currentIsSystem) {
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
        return runCatching {
            Shell.cmd("pgrep -f $pkg").exec().out.mapNotNull { it.toIntOrNull() }
        }.getOrDefault(emptyList())
    }

    private fun freezePackage(pkg: String) {
        val pids = getPidsForPackage(pkg)
        pids.forEach { pid ->
            Shell.cmd("echo $pid >> $FROZEN_GROUP/tasks").exec()
        }
        Log.i(TAG, "FROZEN: $pkg")
    }

    private fun unfreezePackage(pkg: String) {
        val pids = getPidsForPackage(pkg)
        pids.forEach { pid ->
            Shell.cmd("echo $pid >> $THAW_GROUP/tasks").exec()
        }
        Log.i(TAG, "THAWED: $pkg")
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
            delay(delaySeconds * 1000L)
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

    private fun checkFileExists(path: String): Boolean = Shell.cmd("[ -f $path ]").exec().isSuccess
    private fun writeFile(path: String, content: String) = Shell.cmd("echo $content > $path").exec()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        monitorScope.cancel()
        freezeTasks.values.forEach { it?.cancel() }
        super.onDestroy()
    }
}