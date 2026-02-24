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
import com.example.deepsleep.model.*
import com.example.deepsleep.root.OptimizationManager
import com.example.deepsleep.root.ProcessManager
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

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> ProcessManager.onScreenStateChanged(true)
                Intent.ACTION_SCREEN_OFF -> ProcessManager.onScreenStateChanged(false)
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
                LogRepository.debug(TAG, "Settings loaded")
            }
        }
    }

    fun refreshRootStatus() {
        viewModelScope.launch {
            val hasRoot = try {
                RootCommander.checkRoot()
            } catch (e: Exception) {
                false
            }
            _settings.value = _settings.value.copy(rootGranted = hasRoot)
            SettingsRepository.setRootGranted(hasRoot)
        }
    }

    // 深度睡眠
    fun setDeepSleepEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val newDeepSleep = _settings.value.deepSleep.copy(enabled = enabled)
            _settings.value = _settings.value.copy(deepSleep = newDeepSleep)
            SettingsRepository.updateDeepSleep(newDeepSleep)
        }
    }

    fun setDeepSleepDelaySeconds(seconds: Int) {
        viewModelScope.launch {
            val newDeepSleep = _settings.value.deepSleep.copy(delaySeconds = seconds)
            _settings.value = _settings.value.copy(deepSleep = newDeepSleep)
            SettingsRepository.updateDeepSleep(newDeepSleep)
        }
    }

    fun setDeepSleepCheckInterval(seconds: Int) {
        viewModelScope.launch {
            val newDeepSleep = _settings.value.deepSleep.copy(checkIntervalSeconds = seconds)
            _settings.value = _settings.value.copy(deepSleep = newDeepSleep)
            SettingsRepository.updateDeepSleep(newDeepSleep)
        }
    }

    fun setEnablePowerSaverOnSleep(enabled: Boolean) {
        viewModelScope.launch {
            val newDeepSleep = _settings.value.deepSleep.copy(enablePowerSaverOnSleep = enabled)
            _settings.value = _settings.value.copy(deepSleep = newDeepSleep)
            SettingsRepository.updateDeepSleep(newDeepSleep)
        }
    }

    fun setDisablePowerSaverOnWake(enabled: Boolean) {
        viewModelScope.launch {
            val newDeepSleep = _settings.value.deepSleep.copy(disablePowerSaverOnWake = enabled)
            _settings.value = _settings.value.copy(deepSleep = newDeepSleep)
            SettingsRepository.updateDeepSleep(newDeepSleep)
        }
    }

    // 性能优化
    fun updatePerformanceOptimization(perf: PerformanceOptimization) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(performanceOptimization = perf)
            SettingsRepository.updatePerformanceOptimization(perf)
            ProcessManager.onConfigChanged()
        }
    }

    // 进程管理
    fun updateProcessManagement(pm: ProcessManagement) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(processManagement = pm)
            SettingsRepository.updateProcessManagement(pm)
            ProcessManager.onConfigChanged()
        }
    }

    // 后台优化
    fun updateBackgroundOptimization(bg: BackgroundOptimization) {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(backgroundOptimization = bg)
            SettingsRepository.updateBackgroundOptimization(bg)
        }
    }

    // 场景检测
    fun setCheckNetworkTraffic(enabled: Boolean) {
        viewModelScope.launch {
            val newScene = _settings.value.sceneCheck.copy(checkNetworkTraffic = enabled)
            _settings.value = _settings.value.copy(sceneCheck = newScene)
            SettingsRepository.updateSceneCheck(newScene)
        }
    }

    fun setCheckAudioPlayback(enabled: Boolean) {
        viewModelScope.launch {
            val newScene = _settings.value.sceneCheck.copy(checkAudioPlayback = enabled)
            _settings.value = _settings.value.copy(sceneCheck = newScene)
            SettingsRepository.updateSceneCheck(newScene)
        }
    }

    fun setCheckNavigation(enabled: Boolean) {
        viewModelScope.launch {
            val newScene = _settings.value.sceneCheck.copy(checkNavigation = enabled)
            _settings.value = _settings.value.copy(sceneCheck = newScene)
            SettingsRepository.updateSceneCheck(newScene)
        }
    }

    fun setCheckPhoneCall(enabled: Boolean) {
        viewModelScope.launch {
            val newScene = _settings.value.sceneCheck.copy(checkPhoneCall = enabled)
            _settings.value = _settings.value.copy(sceneCheck = newScene)
            SettingsRepository.updateSceneCheck(newScene)
        }
    }

    fun setCheckNfcP2p(enabled: Boolean) {
        viewModelScope.launch {
            val newScene = _settings.value.sceneCheck.copy(checkNfcP2p = enabled)
            _settings.value = _settings.value.copy(sceneCheck = newScene)
            SettingsRepository.updateSceneCheck(newScene)
        }
    }

    fun setCheckWifiHotspot(enabled: Boolean) {
        viewModelScope.launch {
            val newScene = _settings.value.sceneCheck.copy(checkWifiHotspot = enabled)
            _settings.value = _settings.value.copy(sceneCheck = newScene)
            SettingsRepository.updateSceneCheck(newScene)
        }
    }

    fun setCheckUsbTethering(enabled: Boolean) {
        viewModelScope.launch {
            val newScene = _settings.value.sceneCheck.copy(checkUsbTethering = enabled)
            _settings.value = _settings.value.copy(sceneCheck = newScene)
            SettingsRepository.updateSceneCheck(newScene)
        }
    }

    fun setCheckScreenCasting(enabled: Boolean) {
        viewModelScope.launch {
            val newScene = _settings.value.sceneCheck.copy(checkScreenCasting = enabled)
            _settings.value = _settings.value.copy(sceneCheck = newScene)
            SettingsRepository.updateSceneCheck(newScene)
        }
    }

    fun setCheckCharging(enabled: Boolean) {
        viewModelScope.launch {
            val newScene = _settings.value.sceneCheck.copy(checkCharging = enabled)
            _settings.value = _settings.value.copy(sceneCheck = newScene)
            SettingsRepository.updateSceneCheck(newScene)
        }
    }
}