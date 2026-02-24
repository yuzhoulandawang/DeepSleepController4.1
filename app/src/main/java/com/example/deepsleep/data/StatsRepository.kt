package com.example.deepsleep.data

import android.util.Log
import com.example.deepsleep.model.Statistics
import com.example.deepsleep.root.RootCommander
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object StatsRepository {

    private const val TAG = "StatsRepository"
    private const val STATS_FILE = "/data/local/tmp/deep_sleep_logs/stats.txt"
    private val statsMutex = Mutex()

    private val _statistics = MutableStateFlow(Statistics())
    val statistics: StateFlow<Statistics> = _statistics.asStateFlow()

    private var sessionStartTime = System.currentTimeMillis()
    private var isLoaded = false

    private var _totalRuntimeHistory = 0L
    private var _totalOptimizations = 0
    private var _powerSaved = 0L
    private var _memoryReleased = 0L
    private var _gpuOptimizations = 0
    private var _avgGpuFreq = 770000000L
    private var _gpuThrottlingCount = 0
    private var _currentGpuMode = "daily"
    private var _cpuBindingCount = 0
    private var _currentCpuMode = "daily"
    private var _cpuUsageOptimized = 0
    private var _suppressedApps = 0
    private var _killedProcesses = 0
    private var _oomAdjustments = 0
    private var _avgOomScore = 500
    private var _frozenApps = 0
    private var _thawedApps = 0
    private var _avgFreezeTime = 0L
    private var _preventedFreezes = 0
    private var _gameSceneCount = 0
    private var _navigationSceneCount = 0
    private var _chargingSceneCount = 0
    private var _callSceneCount = 0
    private var _castSceneCount = 0
    private val _recentActivities = mutableListOf<String>()

    suspend fun ensureLoaded() {
        if (!isLoaded) {
            statsMutex.withLock {
                if (!isLoaded) {
                    loadStatsFromFile()
                    isLoaded = true
                }
            }
        }
    }

    private suspend fun loadStatsFromFile() {
        withContext(Dispatchers.IO) {
            val content = RootCommander.readFile(STATS_FILE)
            if (content != null) {
                val map = content.lineSequence()
                    .filter { it.contains("=") }
                    .associate {
                        val parts = it.split("=", limit = 2)
                        parts[0].trim() to parts[1].trim()
                    }

                _totalRuntimeHistory = map["TOTAL_RUNTIME"]?.toLongOrNull() ?: 0L
                _totalOptimizations = map["TOTAL_OPTIMIZATIONS"]?.toIntOrNull() ?: 0
                _powerSaved = map["POWER_SAVED"]?.toLongOrNull() ?: 0L
                _memoryReleased = map["MEMORY_RELEASED"]?.toLongOrNull() ?: 0L
                _gpuOptimizations = map["GPU_OPTIMIZATIONS"]?.toIntOrNull() ?: 0
                _avgGpuFreq = map["AVG_GPU_FREQ"]?.toLongOrNull() ?: 770000000L
                _gpuThrottlingCount = map["GPU_THROTTLING_COUNT"]?.toIntOrNull() ?: 0
                _currentGpuMode = map["CURRENT_GPU_MODE"] ?: "daily"
                _cpuBindingCount = map["CPU_BINDING_COUNT"]?.toIntOrNull() ?: 0
                _currentCpuMode = map["CURRENT_CPU_MODE"] ?: "daily"
                _cpuUsageOptimized = map["CPU_USAGE_OPTIMIZED"]?.toIntOrNull() ?: 0
                _suppressedApps = map["SUPPRESSED_APPS"]?.toIntOrNull() ?: 0
                _killedProcesses = map["KILLED_PROCESSES"]?.toIntOrNull() ?: 0
                _oomAdjustments = map["OOM_ADJUSTMENTS"]?.toIntOrNull() ?: 0
                _avgOomScore = map["AVG_OOM_SCORE"]?.toIntOrNull() ?: 500
                _frozenApps = map["FROZEN_APPS"]?.toIntOrNull() ?: 0
                _thawedApps = map["THAWED_APPS"]?.toIntOrNull() ?: 0
                _avgFreezeTime = map["AVG_FREEZE_TIME"]?.toLongOrNull() ?: 0L
                _preventedFreezes = map["PREVENTED_FREEZES"]?.toIntOrNull() ?: 0
                _gameSceneCount = map["GAME_SCENE_COUNT"]?.toIntOrNull() ?: 0
                _navigationSceneCount = map["NAVIGATION_SCENE_COUNT"]?.toIntOrNull() ?: 0
                _chargingSceneCount = map["CHARGING_SCENE_COUNT"]?.toIntOrNull() ?: 0
                _callSceneCount = map["CALL_SCENE_COUNT"]?.toIntOrNull() ?: 0
                _castSceneCount = map["CAST_SCENE_COUNT"]?.toIntOrNull() ?: 0
                _recentActivities.clear()
                _recentActivities.addAll(map["RECENT_ACTIVITIES"]?.split("|")?.filter { it.isNotBlank() } ?: emptyList())
            }

            updateStateFlow()
        }
    }

    private suspend fun updateStateFlow() {
        val currentTotal = _totalRuntimeHistory + (System.currentTimeMillis() - sessionStartTime)
        _statistics.value = Statistics(
            totalRuntime = currentTotal,
            totalOptimizations = _totalOptimizations,
            powerSaved = _powerSaved,
            memoryReleased = _memoryReleased,
            gpuOptimizations = _gpuOptimizations,
            avgGpuFreq = _avgGpuFreq,
            gpuThrottlingCount = _gpuThrottlingCount,
            currentGpuMode = _currentGpuMode,
            cpuBindingCount = _cpuBindingCount,
            currentCpuMode = _currentCpuMode,
            cpuUsageOptimized = _cpuUsageOptimized,
            suppressedApps = _suppressedApps,
            killedProcesses = _killedProcesses,
            oomAdjustments = _oomAdjustments,
            avgOomScore = _avgOomScore,
            frozenApps = _frozenApps,
            thawedApps = _thawedApps,
            avgFreezeTime = _avgFreezeTime,
            preventedFreezes = _preventedFreezes,
            gameSceneCount = _gameSceneCount,
            navigationSceneCount = _navigationSceneCount,
            chargingSceneCount = _chargingSceneCount,
            callSceneCount = _callSceneCount,
            castSceneCount = _castSceneCount,
            recentActivities = _recentActivities
        )
    }

    private suspend fun saveStatsToFile() = statsMutex.withLock {
        withContext(Dispatchers.IO) {
            val stats = _statistics.value
            val content = buildString {
                appendLine("TOTAL_RUNTIME=$_totalRuntimeHistory")
                appendLine("TOTAL_OPTIMIZATIONS=${stats.totalOptimizations}")
                appendLine("POWER_SAVED=${stats.powerSaved}")
                appendLine("MEMORY_RELEASED=${stats.memoryReleased}")
                appendLine("GPU_OPTIMIZATIONS=${stats.gpuOptimizations}")
                appendLine("AVG_GPU_FREQ=${stats.avgGpuFreq}")
                appendLine("GPU_THROTTLING_COUNT=${stats.gpuThrottlingCount}")
                appendLine("CURRENT_GPU_MODE=${stats.currentGpuMode}")
                appendLine("CPU_BINDING_COUNT=${stats.cpuBindingCount}")
                appendLine("CURRENT_CPU_MODE=${stats.currentCpuMode}")
                appendLine("CPU_USAGE_OPTIMIZED=${stats.cpuUsageOptimized}")
                appendLine("SUPPRESSED_APPS=${stats.suppressedApps}")
                appendLine("KILLED_PROCESSES=${stats.killedProcesses}")
                appendLine("OOM_ADJUSTMENTS=${stats.oomAdjustments}")
                appendLine("AVG_OOM_SCORE=${stats.avgOomScore}")
                appendLine("FROZEN_APPS=${stats.frozenApps}")
                appendLine("THAWED_APPS=${stats.thawedApps}")
                appendLine("AVG_FREEZE_TIME=${stats.avgFreezeTime}")
                appendLine("PREVENTED_FREEZES=${stats.preventedFreezes}")
                appendLine("GAME_SCENE_COUNT=${stats.gameSceneCount}")
                appendLine("NAVIGATION_SCENE_COUNT=${stats.navigationSceneCount}")
                appendLine("CHARGING_SCENE_COUNT=${stats.chargingSceneCount}")
                appendLine("CALL_SCENE_COUNT=${stats.callSceneCount}")
                appendLine("CAST_SCENE_COUNT=${stats.castSceneCount}")
                appendLine("RECENT_ACTIVITIES=${stats.recentActivities.joinToString("|")}")
            }

            RootCommander.exec("mkdir -p /data/local/tmp/deep_sleep_logs")
            RootCommander.exec("printf '%s\\n' \"$content\" > $STATS_FILE")
        }
    }

    // ========== 统计数据获取方法 ==========
    suspend fun getTotalRuntime(): Long = withContext(Dispatchers.IO) { _statistics.value.totalRuntime }

    suspend fun getTotalOptimizations(): Int = withContext(Dispatchers.IO) { _totalOptimizations }

    suspend fun getPowerSaved(): Long = withContext(Dispatchers.IO) { _powerSaved }

    suspend fun getMemoryReleased(): Long = withContext(Dispatchers.IO) { _memoryReleased }

    suspend fun getGpuOptimizations(): Int = withContext(Dispatchers.IO) { _gpuOptimizations }

    suspend fun getAvgGpuFreq(): Long = withContext(Dispatchers.IO) {
        try {
            val devfreqBase = "/sys/class/devfreq"
            val kgslDirsResult = RootCommander.exec("ls $devfreqBase | grep kgsl")
            val kgslDir = kgslDirsResult.out.firstOrNull()
            if (kgslDir != null) {
                val curFreqResult = RootCommander.exec("cat $devfreqBase/$kgslDir/cur_freq")
                val freq = curFreqResult.out.firstOrNull()?.trim()?.toLongOrNull()
                if (freq != null) {
                    _avgGpuFreq = (_avgGpuFreq * 0.9 + freq * 0.1).toLong()
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Could not read GPU frequency: ${e.message}")
        }
        return@withContext _avgGpuFreq
    }

    suspend fun getGpuThrottlingCount(): Int = withContext(Dispatchers.IO) { _gpuThrottlingCount }

    suspend fun getCurrentGpuMode(): String = withContext(Dispatchers.IO) { _currentGpuMode }

    suspend fun getCpuBindingCount(): Int = withContext(Dispatchers.IO) { _cpuBindingCount }

    suspend fun getCurrentCpuMode(): String = withContext(Dispatchers.IO) { _currentCpuMode }

    suspend fun getCpuUsageOptimized(): Int = withContext(Dispatchers.IO) { _cpuUsageOptimized }

    suspend fun getSuppressedApps(): Int = withContext(Dispatchers.IO) { _suppressedApps }

    suspend fun getKilledProcesses(): Int = withContext(Dispatchers.IO) { _killedProcesses }

    suspend fun getOomAdjustments(): Int = withContext(Dispatchers.IO) { _oomAdjustments }

    suspend fun getAvgOomScore(): Int = withContext(Dispatchers.IO) { _avgOomScore }

    suspend fun getFrozenApps(): Int = withContext(Dispatchers.IO) { _frozenApps }

    suspend fun getThawedApps(): Int = withContext(Dispatchers.IO) { _thawedApps }

    suspend fun getAvgFreezeTime(): Long = withContext(Dispatchers.IO) { _avgFreezeTime }

    suspend fun getPreventedFreezes(): Int = withContext(Dispatchers.IO) { _preventedFreezes }

    suspend fun getGameSceneCount(): Int = withContext(Dispatchers.IO) { _gameSceneCount }

    suspend fun getNavigationSceneCount(): Int = withContext(Dispatchers.IO) { _navigationSceneCount }

    suspend fun getChargingSceneCount(): Int = withContext(Dispatchers.IO) { _chargingSceneCount }

    suspend fun getCallSceneCount(): Int = withContext(Dispatchers.IO) { _callSceneCount }

    suspend fun getCastSceneCount(): Int = withContext(Dispatchers.IO) { _castSceneCount }

    suspend fun getRecentActivities(): List<String> = withContext(Dispatchers.IO) { _recentActivities.toList() }

    // ========== 统计数据记录方法 ==========
    suspend fun recordGpuOptimization() = statsMutex.withLock {
        _gpuOptimizations++
        _totalOptimizations++
        updateAndSave()
    }

    suspend fun recordAppSuppressed(count: Int) = statsMutex.withLock {
        _suppressedApps += count
        _oomAdjustments++
        _totalOptimizations++
        updateAndSave()
    }

    suspend fun recordCpuBinding() = statsMutex.withLock {
        _cpuBindingCount++
        _totalOptimizations++
        updateAndSave()
    }

    suspend fun recordFrozenApp() = statsMutex.withLock {
        _frozenApps++
        updateAndSave()
    }

    suspend fun recordThawedApp() = statsMutex.withLock {
        _thawedApps++
        updateAndSave()
    }

    suspend fun recordSceneDetected(sceneType: String) = statsMutex.withLock {
        when (sceneType) {
            "game" -> _gameSceneCount++
            "navigation" -> _navigationSceneCount++
            "charging" -> _chargingSceneCount++
            "call" -> _callSceneCount++
            "cast" -> _castSceneCount++
        }
        updateAndSave()
    }

    private suspend fun updateAndSave() {
        updateStateFlow()
        saveStatsToFile()
    }

    suspend fun resetStats() = statsMutex.withLock {
        withContext(Dispatchers.IO) {
            _totalRuntimeHistory = 0L
            _totalOptimizations = 0
            _powerSaved = 0L
            _memoryReleased = 0L
            _gpuOptimizations = 0
            _gpuThrottlingCount = 0
            _currentGpuMode = "daily"
            _cpuBindingCount = 0
            _currentCpuMode = "daily"
            _cpuUsageOptimized = 0
            _suppressedApps = 0
            _killedProcesses = 0
            _oomAdjustments = 0
            _avgOomScore = 500
            _frozenApps = 0
            _thawedApps = 0
            _avgFreezeTime = 0L
            _preventedFreezes = 0
            _gameSceneCount = 0
            _navigationSceneCount = 0
            _chargingSceneCount = 0
            _callSceneCount = 0
            _castSceneCount = 0
            _recentActivities.clear()
            sessionStartTime = System.currentTimeMillis()

            updateStateFlow()
            saveStatsToFile()
        }
    }
}