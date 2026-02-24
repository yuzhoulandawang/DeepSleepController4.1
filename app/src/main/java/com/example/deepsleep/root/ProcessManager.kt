package com.example.deepsleep.root

import android.util.Log
import com.example.deepsleep.data.SettingsRepository
import com.example.deepsleep.model.SuppressMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

object ProcessManager {
    private const val TAG = "ProcessManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var suppressJob: Job? = null
    private var isScreenOn = true

    fun onScreenStateChanged(screenOn: Boolean) {
        isScreenOn = screenOn
        updateSuppressJob()
    }

    fun onConfigChanged() {
        updateSuppressJob()
        updateFreezeConfig()
    }

    private fun updateSuppressJob() {
        suppressJob?.cancel()
        suppressJob = null

        val settings = runBlocking { SettingsRepository.settings.first() }
        val suppress = settings.processManagement.suppress

        if (!suppress.enabled) {
            return
        }

        val shouldRun = when (suppress.mode) {
            SuppressMode.AGGRESSIVE -> true
            SuppressMode.CONSERVATIVE -> !isScreenOn
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
                delay(60_000)
            }
        }
    }

    private fun updateFreezeConfig() {
        val settings = runBlocking { SettingsRepository.settings.first() }
        val freeze = settings.processManagement.freeze
        FreezerService.updateConfig(freeze.enabled, freeze.delaySeconds)
    }

    fun onDestroy() {
        suppressJob?.cancel()
        scope.cancel()
    }
}