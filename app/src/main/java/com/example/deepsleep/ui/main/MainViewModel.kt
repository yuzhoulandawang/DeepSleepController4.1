package com.example.deepsleep.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deepsleep.data.SettingsRepository
import com.example.deepsleep.data.LogRepository
import com.example.deepsleep.model.AppSettings
import com.example.deepsleep.model.ScreenState
import com.example.deepsleep.root.OptimizationManager
import com.example.deepsleep.root.ProcessSuppressor
import com.example.deepsleep.root.RootCommander
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private var screenState = ScreenState.ON

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> screenState = ScreenState.ON
                Intent.ACTION_SCREEN_OFF -> screenState = ScreenState.OFF
            }
            if (_settings.value.autoSwitchCpuMode) {
                applyAutoCpuMode()
            }
        }
    }

    fun registerScreenStateReceiver(context: Context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        context.registerReceiver(screenStateReceiver, filter)
    }

    fun unregisterScreenStateReceiver(context: Context) {
        context.unregisterReceiver(screenStateReceiver)
    }

    init {
        loadSettings()
        refreshRootStatus()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            SettingsRepository.settings.collect { appSettings ->
                _settings.value = appSettings
                LogRepository.debug(TAG, "Settings loaded: deepSleep=${appSettings.deepSleepEnabled}")
            }
        }
    }

    fun refreshRootStatus() {
        viewModelScope.launch {
            val hasRoot = try {
                RootCommander.checkRoot()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check root status via RootCommander", e)
                false
            }
            _settings.value = _settings.value.copy(rootGranted = hasRoot)
            SettingsRepository.setRootGranted(hasRoot)
            LogRepository.info(TAG, "Root status refreshed: $hasRoot")
        }
    }

    private fun applyAutoCpuMode() {
        viewModelScope.launch {
            val modeName = if (screenState == ScreenState.ON) {
                _settings.value.cpuModeOnScreen
            } else {
                _settings.value.cpuModeOnScreenOff
            }
            val optMode = when (modeName) {
                "performance" -> OptimizationManager.PerformanceMode.PERFORMANCE
                "standby" -> OptimizationManager.PerformanceMode.STANDBY
                else -> OptimizationManager.PerformanceMode.DAILY
            }
            OptimizationManager.applyAllOptimizations(optMode)
            LogRepository.info(TAG, "Auto switched CPU mode to: $modeName (screen=$screenState)")
        }
    }

    // ========== 深度睡眠控制 ==========
    fun setDeepSleepEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(deepSleepEnabled = enabled)
            SettingsRepository.setDeepSleepEnabled(enabled)
            LogRepository.info(TAG, "Deep sleep enabled: $enabled")
        }
    }

    fun setWakeupSuppressEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(wakeupSuppressEnabled = enabled)
            SettingsRepository.setWakeupSuppressEnabled(enabled)
        }
    }

    fun setAlarmSuppressEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(alarmSuppressEnabled = enabled)
            SettingsRepository.setAlarmSuppressEnabled(enabled)
        }
    }

    // ========== 深度 Doze 配置 ==========
    fun setDeepDozeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(deepDozeEnabled = enabled)
            SettingsRepository.setDeepDozeEnabled(enabled)
        }
    }

    fun setDeepDozeDelaySeconds(seconds: Int) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(deepDozeDelaySeconds = seconds)
            SettingsRepository.setDeepDozeDelaySeconds(seconds)
        }
    }

    fun setDeepDozeForceMode(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(deepDozeForceMode = enabled)
            SettingsRepository.setDeepDozeForceMode(enabled)
        }
    }

    // ========== 深度睡眠 Hook 配置 ==========
    fun setDeepSleepHookEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(deepSleepHookEnabled = enabled)
            SettingsRepository.setDeepSleepHookEnabled(enabled)
        }
    }

    fun setDeepSleepDelaySeconds(seconds: Int) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(deepSleepDelaySeconds = seconds)
            SettingsRepository.setDeepSleepDelaySeconds(seconds)
        }
    }

    fun setDeepSleepBlockExit(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(deepSleepBlockExit = enabled)
            SettingsRepository.setDeepSleepBlockExit(enabled)
        }
    }

    fun setDeepSleepCheckInterval(seconds: Int) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(deepSleepCheckInterval = seconds)
            SettingsRepository.setDeepSleepCheckInterval(seconds)
        }
    }

    // ========== 系统省电模式联动 ==========
    fun setEnablePowerSaverOnSleep(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(enablePowerSaverOnSleep = enabled)
            SettingsRepository.setEnablePowerSaverOnSleep(enabled)
        }
    }

    fun setDisablePowerSaverOnWake(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(disablePowerSaverOnWake = enabled)
            SettingsRepository.setDisablePowerSaverOnWake(enabled)
        }
    }

    // ========== 后台优化 ==========
    fun setBackgroundOptimizationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(backgroundOptimizationEnabled = enabled)
            SettingsRepository.setBackgroundOptimizationEnabled(enabled)
            LogRepository.info(TAG, "Background optimization: $enabled")
        }
    }

    fun setAppSuspendEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(appSuspendEnabled = enabled)
            SettingsRepository.setAppSuspendEnabled(enabled)
        }
    }

    fun setBackgroundRestrictEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(backgroundRestrictEnabled = enabled)
            SettingsRepository.setBackgroundRestrictEnabled(enabled)
            if (enabled) {
                val count = ProcessSuppressor.suppressBackgroundApps(_settings.value.suppressScore)
                LogRepository.info(TAG, "Background restrict enabled, suppressed $count apps")
            }
        }
    }

    // ========== GPU 优化 ==========
    fun setGpuOptimizationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(gpuOptimizationEnabled = enabled)
            SettingsRepository.setGpuOptimizationEnabled(enabled)
        }
    }

    fun setGpuMode(mode: String) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(gpuMode = mode)
            SettingsRepository.setGpuMode(mode)
            val optMode = when (mode) {
                "performance" -> OptimizationManager.PerformanceMode.PERFORMANCE
                "power_saving" -> OptimizationManager.PerformanceMode.STANDBY
                else -> OptimizationManager.PerformanceMode.DAILY
            }
            OptimizationManager.applyAllOptimizations(optMode)
            LogRepository.info(TAG, "GPU mode changed to: $mode")
        }
    }

    fun setGpuThrottling(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(gpuThrottlingEnabled = enabled)
            SettingsRepository.setGpuThrottling(enabled)
        }
    }

    fun setGpuBusSplit(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(gpuBusSplitEnabled = enabled)
            SettingsRepository.setGpuBusSplit(enabled)
        }
    }

    fun setGpuIdleTimer(timer: Int) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(gpuIdleTimer = timer)
            SettingsRepository.setGpuIdleTimer(timer)
        }
    }

    fun setGpuMaxFreq(freq: Long) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(gpuMaxFreq = freq)
            SettingsRepository.setGpuMaxFreq(freq)
        }
    }

    fun setGpuMinFreq(freq: Long) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(gpuMinFreq = freq)
            SettingsRepository.setGpuMinFreq(freq)
        }
    }

    fun setGpuThermalPwrLevel(level: Int) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(gpuThermalPwrLevel = level)
            SettingsRepository.setGpuThermalPwrLevel(level)
        }
    }

    fun setGpuTripPointTemp(temp: Int) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(gpuTripPointTemp = temp)
            SettingsRepository.setGpuTripPointTemp(temp)
        }
    }

    fun setGpuTripPointHyst(hyst: Int) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(gpuTripPointHyst = hyst)
            SettingsRepository.setGpuTripPointHyst(hyst)
        }
    }

    fun applyGpuPerformanceMode() = setGpuMode("performance")
    fun applyGpuPowerSavingMode() = setGpuMode("power_saving")
    fun applyGpuDefaultMode() = setGpuMode("default")

    // ========== 电池优化 ==========
    fun setBatteryOptimizationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(batteryOptimizationEnabled = enabled)
            SettingsRepository.setBatteryOptimizationEnabled(enabled)
        }
    }

    fun setPowerSavingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(powerSavingEnabled = enabled)
            SettingsRepository.setPowerSavingEnabled(enabled)
        }
    }

    // ========== CPU 调度优化 ==========
    fun setCpuOptimizationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(cpuOptimizationEnabled = enabled)
            SettingsRepository.setCpuOptimizationEnabled(enabled)
            if (enabled) {
                switchSchedulerToWalt()
                applyCpuOptimization()
            }
            LogRepository.info(TAG, "CPU optimization enabled: $enabled")
        }
    }

    fun setAutoSwitchCpuMode(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(autoSwitchCpuMode = enabled)
            SettingsRepository.setAutoSwitchCpuMode(enabled)
            if (enabled) {
                applyAutoCpuMode()
            }
        }
    }

    fun setCpuMode(mode: String) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(cpuMode = mode)
            SettingsRepository.setCpuMode(mode)
            if (!_settings.value.autoSwitchCpuMode) {
                applyCpuOptimization()
            }
        }
    }

    fun setCpuModeOnScreen(mode: String) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(cpuModeOnScreen = mode)
            SettingsRepository.setCpuModeOnScreen(mode)
        }
    }

    fun setCpuModeOnScreenOff(mode: String) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(cpuModeOnScreenOff = mode)
            SettingsRepository.setCpuModeOnScreenOff(mode)
        }
    }

    private suspend fun applyCpuOptimization() {
        val settings = _settings.value
        if (!settings.cpuOptimizationEnabled) return
        val modeToApply = if (settings.autoSwitchCpuMode) {
            null // 自动切换模式下，由屏幕监听触发
        } else {
            settings.cpuMode
        }
        modeToApply?.let { mode ->
            val optMode = when (mode) {
                "performance" -> OptimizationManager.PerformanceMode.PERFORMANCE
                "standby" -> OptimizationManager.PerformanceMode.STANDBY
                else -> OptimizationManager.PerformanceMode.DAILY
            }
            OptimizationManager.applyAllOptimizations(optMode)
            LogRepository.info(TAG, "Applied CPU optimization for mode: $mode")
        }
    }

    // ========== Freezer 服务 ==========
    fun setFreezerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(freezerEnabled = enabled)
            SettingsRepository.setFreezerEnabled(enabled)
        }
    }

    fun setFreezeDelay(delay: Int) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(freezeDelay = delay)
            SettingsRepository.setFreezeDelay(delay)
        }
    }

    // ========== 场景检测 ==========
    fun setSceneCheckEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(sceneCheckEnabled = enabled)
            SettingsRepository.setSceneCheckEnabled(enabled)
        }
    }

    fun setCheckNetworkTraffic(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(checkNetworkTraffic = enabled)
            SettingsRepository.setCheckNetworkTraffic(enabled)
        }
    }

    fun setCheckAudioPlayback(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(checkAudioPlayback = enabled)
            SettingsRepository.setCheckAudioPlayback(enabled)
        }
    }

    fun setCheckNavigation(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(checkNavigation = enabled)
            SettingsRepository.setCheckNavigation(enabled)
        }
    }

    fun setCheckPhoneCall(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(checkPhoneCall = enabled)
            SettingsRepository.setCheckPhoneCall(enabled)
        }
    }

    fun setCheckNfcP2p(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(checkNfcP2p = enabled)
            SettingsRepository.setCheckNfcP2p(enabled)
        }
    }

    fun setCheckWifiHotspot(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(checkWifiHotspot = enabled)
            SettingsRepository.setCheckWifiHotspot(enabled)
        }
    }

    fun setCheckUsbTethering(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(checkUsbTethering = enabled)
            SettingsRepository.setCheckUsbTethering(enabled)
        }
    }

    fun setCheckScreenCasting(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(checkScreenCasting = enabled)
            SettingsRepository.setCheckScreenCasting(enabled)
        }
    }

    fun setCheckCharging(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(checkCharging = enabled)
            SettingsRepository.setCheckCharging(enabled)
        }
    }

    // ========== 进程压制 ==========
    fun setProcessSuppressEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(processSuppressEnabled = enabled)
            SettingsRepository.setProcessSuppressEnabled(enabled)
        }
    }

    fun setSuppressScore(score: Int) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(suppressScore = score)
            SettingsRepository.setSuppressScore(score)
        }
    }

    // ========== 辅助函数 ==========
    private suspend fun switchSchedulerToWalt() {
        val script = """
            for policy in /sys/devices/system/cpu/cpufreq/policy*; do
                [ -d "${'$'}policy" ] || continue
                if [ -w "${'$'}policy/scaling_governor" ]; then
                    current=${'$'}(cat "${'$'}policy/scaling_governor" 2>/dev/null)
                    if [ "${'$'}current" != "walt" ]; then
                        echo "walt" > "${'$'}policy/scaling_governor" 2>/dev/null
                    fi
                fi
            done
        """.trimIndent()
        val result = RootCommander.exec(script)
        if (result.isSuccess) {
            LogRepository.info(TAG, "Successfully switched scaling_governor to walt for all policies")
        } else {
            LogRepository.error(TAG, "Failed to switch scaling_governor to walt: ${result.err}")
        }
    }
}