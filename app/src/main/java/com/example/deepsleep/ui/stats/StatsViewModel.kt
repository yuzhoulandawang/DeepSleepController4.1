package com.example.deepsleep.ui.stats

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deepsleep.data.LogRepository
import com.example.deepsleep.data.StatsRepository
import com.example.deepsleep.model.Statistics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 统计数据页面 ViewModel
 * 管理统计数据的加载和刷新
 */
class StatsViewModel : ViewModel() {

    companion object {
        private const val TAG = "StatsViewModel"
    }

    private val _statistics = MutableStateFlow(Statistics())
    val statistics: StateFlow<Statistics> = _statistics.asStateFlow()

    init {
        loadStatistics()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            _statistics.value = StatsRepository.statistics.value
            LogRepository.debug(TAG, "Statistics loaded")
        }
    }

    fun refreshStatistics() {
        viewModelScope.launch {
            try {
                // 真实实现：从各模块获取最新统计数据
                val updatedStats = Statistics(
                    totalRuntime = StatsRepository.getTotalRuntime(),
                    totalOptimizations = StatsRepository.getTotalOptimizations(),
                    powerSaved = StatsRepository.getPowerSaved(),
                    memoryReleased = StatsRepository.getMemoryReleased(),
                    gpuOptimizations = StatsRepository.getGpuOptimizations(),
                    avgGpuFreq = StatsRepository.getAvgGpuFreq(),
                    gpuThrottlingCount = StatsRepository.getGpuThrottlingCount(),
                    currentGpuMode = StatsRepository.getCurrentGpuMode(),
                    cpuBindingCount = StatsRepository.getCpuBindingCount(),
                    currentCpuMode = StatsRepository.getCurrentCpuMode(),
                    cpuUsageOptimized = StatsRepository.getCpuUsageOptimized(),
                    suppressedApps = StatsRepository.getSuppressedApps(),
                    killedProcesses = StatsRepository.getKilledProcesses(),
                    oomAdjustments = StatsRepository.getOomAdjustments(),
                    avgOomScore = StatsRepository.getAvgOomScore(),
                    frozenApps = StatsRepository.getFrozenApps(),
                    thawedApps = StatsRepository.getThawedApps(),
                    avgFreezeTime = StatsRepository.getAvgFreezeTime(),
                    preventedFreezes = StatsRepository.getPreventedFreezes(),
                    gameSceneCount = StatsRepository.getGameSceneCount(),
                    navigationSceneCount = StatsRepository.getNavigationSceneCount(),
                    chargingSceneCount = StatsRepository.getChargingSceneCount(),
                    callSceneCount = StatsRepository.getCallSceneCount(),
                    castSceneCount = StatsRepository.getCastSceneCount(),
                    recentActivities = StatsRepository.getRecentActivities()
                )
                
                _statistics.value = updatedStats
                LogRepository.info(TAG, "Statistics refreshed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh statistics", e)
                LogRepository.error(TAG, "Failed to refresh statistics: ${e.message}")
            }
        }
    }
}
