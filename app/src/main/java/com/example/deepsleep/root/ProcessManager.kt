package com.example.deepsleep.root

import android.util.Log
import com.example.deepsleep.data.SettingsRepository
import com.example.deepsleep.model.ProcessManagement
import com.example.deepsleep.model.SuppressMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

object ProcessManager {
    private const val TAG = "ProcessManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var suppressJob: Job? = null
    private var isScreenOn = true

    // 由 DeepSleepService 在屏幕状态变化时调用
    fun onScreenStateChanged(screenOn: Boolean) {
        isScreenOn = screenOn
        updateSuppressJob()
    }

    // 当配置变化时调用（例如用户修改开关或模式）
    fun onConfigChanged() {
        updateSuppressJob()
        updateFreezeConfig()
    }

    private fun updateSuppressJob() {
        // 取消现有任务
        suppressJob?.cancel()
        suppressJob = null

        // 读取当前配置
        val settings = runBlocking { SettingsRepository.settings.first() }
        val suppress = settings.processManagement.suppress

        if (!suppress.enabled) {
            return
        }

        val shouldRun = when (suppress.mode) {
            SuppressMode.AGGRESSIVE -> true  // 始终运行
            SuppressMode.CONSERVATIVE -> !isScreenOn // 仅息屏运行
        }

        if (shouldRun) {
            startSuppressJob(suppress.oomScore)
        }
    }

    private fun startSuppressJob(oomScore: Int) {
        suppressJob = scope.launch {
            while (isActive) {
                try {
                    ProcessSuppressor.suppressBackgroundApps(oomScore)
                } catch (e: Exception) {
                    Log.e(TAG, "Suppress error", e)
                }
                delay(60_000) // 固定间隔60秒
            }
        }
    }

    private fun updateFreezeConfig() {
        // 读取冻结配置并传递给 FreezerService
        val settings = runBlocking { SettingsRepository.settings.first() }
        val freeze = settings.processManagement.freeze
        FreezerService.updateConfig(freeze.enabled, freeze.delaySeconds)
    }

    fun onDestroy() {
        suppressJob?.cancel()
        scope.cancel()
    }
}