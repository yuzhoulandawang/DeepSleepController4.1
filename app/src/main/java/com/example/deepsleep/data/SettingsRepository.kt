package com.example.deepsleep.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.example.deepsleep.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object SettingsRepository {
    private lateinit var dataStore: DataStore<Preferences>

    // ========== 根权限状态 ==========
    private val ROOT_GRANTED = stringPreferencesKey("root_granted")
    private val SERVICE_RUNNING = booleanPreferencesKey("service_running")

    // ========== 深度睡眠控制 ==========
    private val DEEP_SLEEP_ENABLED = booleanPreferencesKey("deep_sleep_enabled")
    private val WAKEUP_SUPPRESS_ENABLED = booleanPreferencesKey("wakeup_suppress_enabled")
    private val ALARM_SUPPRESS_ENABLED = booleanPreferencesKey("alarm_suppress_enabled")

    // ========== 深度 Doze 配置 ==========
    private val DEEP_DOZE_ENABLED = booleanPreferencesKey("deep_doze_enabled")
    private val DEEP_DOZE_DELAY_SECONDS = intPreferencesKey("deep_doze_delay_seconds")
    private val DEEP_DOZE_FORCE_MODE = booleanPreferencesKey("deep_doze_force_mode")

    // ========== 深度睡眠 Hook 配置 ==========
    private val DEEP_SLEEP_HOOK_ENABLED = booleanPreferencesKey("deep_sleep_hook_enabled")
    private val DEEP_SLEEP_DELAY_SECONDS = intPreferencesKey("deep_sleep_delay_seconds")
    private val DEEP_SLEEP_BLOCK_EXIT = booleanPreferencesKey("deep_sleep_block_exit")
    private val DEEP_SLEEP_CHECK_INTERVAL = intPreferencesKey("deep_sleep_check_interval")

    // ========== 后台优化 ==========
    private val BACKGROUND_OPTIMIZATION_ENABLED = booleanPreferencesKey("background_optimization_enabled")
    private val APP_SUSPEND_ENABLED = booleanPreferencesKey("app_suspend_enabled")
    private val BACKGROUND_RESTRICT_ENABLED = booleanPreferencesKey("background_restrict_enabled")

    // ========== GPU 优化 ==========
    private val GPU_OPTIMIZATION_ENABLED = booleanPreferencesKey("gpu_optimization_enabled")
    private val GPU_MODE = stringPreferencesKey("gpu_mode")
    private val GPU_THROTTLING_ENABLED = booleanPreferencesKey("gpu_throttling_enabled")
    private val GPU_BUS_SPLIT_ENABLED = booleanPreferencesKey("gpu_bus_split_enabled")
    private val GPU_IDLE_TIMER = intPreferencesKey("gpu_idle_timer")
    private val GPU_MAX_FREQ = longPreferencesKey("gpu_max_freq")
    private val GPU_MIN_FREQ = longPreferencesKey("gpu_min_freq")
    private val GPU_THERMAL_PWR_LEVEL = intPreferencesKey("gpu_thermal_pwr_level")
    private val GPU_TRIP_POINT_TEMP = intPreferencesKey("gpu_trip_point_temp")
    private val GPU_TRIP_POINT_HYST = intPreferencesKey("gpu_trip_point_hyst")

    // ========== 电池优化 ==========
    private val BATTERY_OPTIMIZATION_ENABLED = booleanPreferencesKey("battery_optimization_enabled")
    private val POWER_SAVING_ENABLED = booleanPreferencesKey("power_saving_enabled")

    // ========== 系统省电模式联动 ==========
    private val ENABLE_POWER_SAVER_ON_SLEEP = booleanPreferencesKey("enable_power_saver_on_sleep")
    private val DISABLE_POWER_SAVER_ON_WAKE = booleanPreferencesKey("disable_power_saver_on_wake")

    // ========== CPU 绑定 ==========
    private val CPU_BIND_ENABLED = booleanPreferencesKey("cpu_bind_enabled")
    private val CPU_MODE = stringPreferencesKey("cpu_mode")

    // ========== CPU 调度优化 ==========
    private val CPU_OPTIMIZATION_ENABLED = booleanPreferencesKey("cpu_optimization_enabled")
    private val AUTO_SWITCH_CPU_MODE = booleanPreferencesKey("auto_switch_cpu_mode")
    private val ALLOW_MANUAL_CPU_MODE = booleanPreferencesKey("allow_manual_cpu_mode")
    private val CPU_MODE_ON_SCREEN = stringPreferencesKey("cpu_mode_on_screen")
    private val CPU_MODE_ON_SCREEN_OFF = stringPreferencesKey("cpu_mode_on_screen_off")

    // ========== CPU 参数 - 日常模式 ==========
    private val DAILY_UP_RATE_LIMIT = intPreferencesKey("daily_up_rate_limit")
    private val DAILY_DOWN_RATE_LIMIT = intPreferencesKey("daily_down_rate_limit")
    private val DAILY_HI_SPEED_LOAD = intPreferencesKey("daily_hi_speed_load")
    private val DAILY_TARGET_LOADS = intPreferencesKey("daily_target_loads")

    // ========== CPU 参数 - 待机模式 ==========
    private val STANDBY_UP_RATE_LIMIT = intPreferencesKey("standby_up_rate_limit")
    private val STANDBY_DOWN_RATE_LIMIT = intPreferencesKey("standby_down_rate_limit")
    private val STANDBY_HI_SPEED_LOAD = intPreferencesKey("standby_hi_speed_load")
    private val STANDBY_TARGET_LOADS = intPreferencesKey("standby_target_loads")

    // ========== CPU 参数 - 默认模式 ==========
    private val DEFAULT_UP_RATE_LIMIT = intPreferencesKey("default_up_rate_limit")
    private val DEFAULT_DOWN_RATE_LIMIT = intPreferencesKey("default_down_rate_limit")
    private val DEFAULT_HI_SPEED_LOAD = intPreferencesKey("default_hi_speed_load")
    private val DEFAULT_TARGET_LOADS = intPreferencesKey("default_target_loads")

    // ========== CPU 参数 - 性能模式 ==========
    private val PERF_UP_RATE_LIMIT = intPreferencesKey("perf_up_rate_limit")
    private val PERF_DOWN_RATE_LIMIT = intPreferencesKey("perf_down_rate_limit")
    private val PERF_HI_SPEED_LOAD = intPreferencesKey("perf_hi_speed_load")
    private val PERF_TARGET_LOADS = intPreferencesKey("perf_target_loads")

    // ========== Freezer 服务 ==========
    private val FREEZER_ENABLED = booleanPreferencesKey("freezer_enabled")
    private val FREEZE_DELAY = intPreferencesKey("freeze_delay")

    // ========== 进程压制 ==========
    private val PROCESS_SUPPRESS_ENABLED = booleanPreferencesKey("process_suppress_enabled")
    private val SUPPRESS_SCORE = intPreferencesKey("suppress_score")

    // ========== 场景检测 ==========
    private val SCENE_CHECK_ENABLED = booleanPreferencesKey("scene_check_enabled")
    private val CHECK_NETWORK_TRAFFIC = booleanPreferencesKey("check_network_traffic")
    private val CHECK_AUDIO_PLAYBACK = booleanPreferencesKey("check_audio_playback")
    private val CHECK_NAVIGATION = booleanPreferencesKey("check_navigation")
    private val CHECK_PHONE_CALL = booleanPreferencesKey("check_phone_call")
    private val CHECK_NFC_P2P = booleanPreferencesKey("check_nfc_p2p")
    private val CHECK_WIFI_HOTSPOT = booleanPreferencesKey("check_wifi_hotspot")
    private val CHECK_USB_TETHERING = booleanPreferencesKey("check_usb_tethering")
    private val CHECK_SCREEN_CASTING = booleanPreferencesKey("check_screen_casting")
    private val CHECK_CHARGING = booleanPreferencesKey("check_charging")

    // ========== 白名单 ==========
    private val WHITELIST = stringPreferencesKey("whitelist")

    private val Context.dataStore: DataStore<Preferences> by androidx.datastore.preferences.preferencesDataStore("settings")

    fun initialize(context: Context) {
        dataStore = context.dataStore
    }

    val settings: Flow<AppSettings>
        get() = dataStore.data.map { preferences ->
            AppSettings(
                rootGranted = preferences[ROOT_GRANTED]?.toBoolean() ?: false,
                serviceRunning = preferences[SERVICE_RUNNING] ?: false,
                deepSleepEnabled = preferences[DEEP_SLEEP_ENABLED] ?: false,
                wakeupSuppressEnabled = preferences[WAKEUP_SUPPRESS_ENABLED] ?: false,
                alarmSuppressEnabled = preferences[ALARM_SUPPRESS_ENABLED] ?: false,
                deepDozeEnabled = preferences[DEEP_DOZE_ENABLED] ?: false,
                deepDozeDelaySeconds = preferences[DEEP_DOZE_DELAY_SECONDS] ?: 30,
                deepDozeForceMode = preferences[DEEP_DOZE_FORCE_MODE] ?: false,
                deepSleepHookEnabled = preferences[DEEP_SLEEP_HOOK_ENABLED] ?: false,
                deepSleepDelaySeconds = preferences[DEEP_SLEEP_DELAY_SECONDS] ?: 60,
                deepSleepBlockExit = preferences[DEEP_SLEEP_BLOCK_EXIT] ?: false,
                deepSleepCheckInterval = preferences[DEEP_SLEEP_CHECK_INTERVAL] ?: 30,
                backgroundOptimizationEnabled = preferences[BACKGROUND_OPTIMIZATION_ENABLED] ?: false,
                appSuspendEnabled = preferences[APP_SUSPEND_ENABLED] ?: false,
                backgroundRestrictEnabled = preferences[BACKGROUND_RESTRICT_ENABLED] ?: false,
                gpuOptimizationEnabled = preferences[GPU_OPTIMIZATION_ENABLED] ?: false,
                gpuMode = preferences[GPU_MODE] ?: "default",
                gpuThrottlingEnabled = preferences[GPU_THROTTLING_ENABLED] ?: false,
                gpuBusSplitEnabled = preferences[GPU_BUS_SPLIT_ENABLED] ?: false,
                gpuIdleTimer = preferences[GPU_IDLE_TIMER] ?: 50,
                gpuMaxFreq = preferences[GPU_MAX_FREQ] ?: 770000000L,
                gpuMinFreq = preferences[GPU_MIN_FREQ] ?: 310000000L,
                gpuThermalPwrLevel = preferences[GPU_THERMAL_PWR_LEVEL] ?: 5,
                gpuTripPointTemp = preferences[GPU_TRIP_POINT_TEMP] ?: 55000,
                gpuTripPointHyst = preferences[GPU_TRIP_POINT_HYST] ?: 5000,
                batteryOptimizationEnabled = preferences[BATTERY_OPTIMIZATION_ENABLED] ?: false,
                powerSavingEnabled = preferences[POWER_SAVING_ENABLED] ?: false,
                enablePowerSaverOnSleep = preferences[ENABLE_POWER_SAVER_ON_SLEEP] ?: false,
                disablePowerSaverOnWake = preferences[DISABLE_POWER_SAVER_ON_WAKE] ?: false,
                cpuBindEnabled = preferences[CPU_BIND_ENABLED] ?: false,
                cpuMode = preferences[CPU_MODE] ?: "daily",
                cpuOptimizationEnabled = preferences[CPU_OPTIMIZATION_ENABLED] ?: false,
                autoSwitchCpuMode = preferences[AUTO_SWITCH_CPU_MODE] ?: false,
                allowManualCpuMode = preferences[ALLOW_MANUAL_CPU_MODE] ?: true,
                cpuModeOnScreen = preferences[CPU_MODE_ON_SCREEN] ?: "daily",
                cpuModeOnScreenOff = preferences[CPU_MODE_ON_SCREEN_OFF] ?: "standby",
                dailyUpRateLimit = preferences[DAILY_UP_RATE_LIMIT] ?: 1000,
                dailyDownRateLimit = preferences[DAILY_DOWN_RATE_LIMIT] ?: 500,
                dailyHiSpeedLoad = preferences[DAILY_HI_SPEED_LOAD] ?: 85,
                dailyTargetLoads = preferences[DAILY_TARGET_LOADS] ?: 80,
                standbyUpRateLimit = preferences[STANDBY_UP_RATE_LIMIT] ?: 5000,
                standbyDownRateLimit = preferences[STANDBY_DOWN_RATE_LIMIT] ?: 0,
                standbyHiSpeedLoad = preferences[STANDBY_HI_SPEED_LOAD] ?: 95,
                standbyTargetLoads = preferences[STANDBY_TARGET_LOADS] ?: 90,
                defaultUpRateLimit = preferences[DEFAULT_UP_RATE_LIMIT] ?: 0,
                defaultDownRateLimit = preferences[DEFAULT_DOWN_RATE_LIMIT] ?: 0,
                defaultHiSpeedLoad = preferences[DEFAULT_HI_SPEED_LOAD] ?: 90,
                defaultTargetLoads = preferences[DEFAULT_TARGET_LOADS] ?: 90,
                perfUpRateLimit = preferences[PERF_UP_RATE_LIMIT] ?: 0,
                perfDownRateLimit = preferences[PERF_DOWN_RATE_LIMIT] ?: 0,
                perfHiSpeedLoad = preferences[PERF_HI_SPEED_LOAD] ?: 75,
                perfTargetLoads = preferences[PERF_TARGET_LOADS] ?: 70,
                freezerEnabled = preferences[FREEZER_ENABLED] ?: false,
                freezeDelay = preferences[FREEZE_DELAY] ?: 30,
                processSuppressEnabled = preferences[PROCESS_SUPPRESS_ENABLED] ?: false,
                suppressScore = preferences[SUPPRESS_SCORE] ?: 500,
                sceneCheckEnabled = preferences[SCENE_CHECK_ENABLED] ?: false,
                checkNetworkTraffic = preferences[CHECK_NETWORK_TRAFFIC] ?: true,
                checkAudioPlayback = preferences[CHECK_AUDIO_PLAYBACK] ?: true,
                checkNavigation = preferences[CHECK_NAVIGATION] ?: true,
                checkPhoneCall = preferences[CHECK_PHONE_CALL] ?: true,
                checkNfcP2p = preferences[CHECK_NFC_P2P] ?: true,
                checkWifiHotspot = preferences[CHECK_WIFI_HOTSPOT] ?: true,
                checkUsbTethering = preferences[CHECK_USB_TETHERING] ?: true,
                checkScreenCasting = preferences[CHECK_SCREEN_CASTING] ?: true,
                checkCharging = preferences[CHECK_CHARGING] ?: false,
                whitelist = preferences[WHITELIST]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            )
        }

    // ========== Setter 方法（只保留一份）==========
    suspend fun setRootGranted(value: Boolean) {
        dataStore.edit { preferences -> preferences[ROOT_GRANTED] = value.toString() }
    }

    suspend fun setServiceRunning(value: Boolean) {
        dataStore.edit { preferences -> preferences[SERVICE_RUNNING] = value }
    }

    suspend fun setDeepSleepEnabled(value: Boolean) {
        dataStore.edit { preferences -> preferences[DEEP_SLEEP_ENABLED] = value }
    }

    suspend fun setWakeupSuppressEnabled(value: Boolean) {
        dataStore.edit { preferences -> preferences[WAKEUP_SUPPRESS_ENABLED] = value }
    }

    suspend fun setAlarmSuppressEnabled(value: Boolean) {
        dataStore.edit { preferences -> preferences[ALARM_SUPPRESS_ENABLED] = value }
    }

    suspend fun setDeepDozeEnabled(value: Boolean) {
        dataStore.edit { preferences -> preferences[DEEP_DOZE_ENABLED] = value }
    }

    suspend fun setDeepDozeDelaySeconds(value: Int) {
        dataStore.edit { preferences -> preferences[DEEP_DOZE_DELAY_SECONDS] = value }
    }

    suspend fun setDeepDozeForceMode(value: Boolean) {
        dataStore.edit { preferences -> preferences[DEEP_DOZE_FORCE_MODE] = value }
    }

    suspend fun setDeepSleepHookEnabled(value: Boolean) {
        dataStore.edit { preferences -> preferences[DEEP_SLEEP_HOOK_ENABLED] = value }
    }

    suspend fun setDeepSleepDelaySeconds(value: Int) {
        dataStore.edit { preferences -> preferences[DEEP_SLEEP_DELAY_SECONDS] = value }
    }

    suspend fun setDeepSleepBlockExit(value: Boolean) {
        dataStore.edit { preferences -> preferences[DEEP_SLEEP_BLOCK_EXIT] = value }
    }

    suspend fun setDeepSleepCheckInterval(value: Int) {
        dataStore.edit { preferences -> preferences[DEEP_SLEEP_CHECK_INTERVAL] = value }
    }

    suspend fun setBackgroundOptimizationEnabled(value: Boolean) {
        dataStore.edit { preferences -> preferences[BACKGROUND_OPTIMIZATION_ENABLED] = value }
    }

    suspend fun setAppSuspendEnabled(value: Boolean) {
        dataStore.edit { preferences -> preferences[APP_SUSPEND_ENABLED] = value }
    }

    suspend fun setBackgroundRestrictEnabled(value: Boolean) {
        dataStore.edit { preferences -> preferences[BACKGROUND_RESTRICT_ENABLED] = value }
    }

    suspend fun setGpuOptimizationEnabled(value: Boolean) {
        dataStore.edit { preferences -> preferences[GPU_OPTIMIZATION_ENABLED] = value }
    }

    suspend fun setGpuMode(value: String) {
        dataStore.edit { preferences -> preferences[GPU_MODE] = value }
    }

    suspend fun setGpuThrottling(value: Boolean) {
        dataStore.edit { preferences -> preferences[GPU_THROTTLING_ENABLED] = value }
    }

    suspend fun setGpuBusSplit(value: Boolean) {
        dataStore.edit { preferences -> preferences[GPU_BUS_SPLIT_ENABLED] = value }
    }

    suspend fun setGpuIdleTimer(value: Int) {
        dataStore.edit { preferences -> preferences[GPU_IDLE_TIMER] = value }
    }

    suspend fun setGpuMaxFreq(value: Long) {
        dataStore.edit { preferences -> preferences[GPU_MAX_FREQ] = value }
    }

    suspend fun setGpuMinFreq(value: Long) {
        dataStore.edit { preferences -> preferences[GPU_MIN_FREQ] = value }
    }

    suspend fun setGpuThermalPwrLevel(value: Int) {
        dataStore.edit { preferences -> preferences[GPU_THERMAL_PWR_LEVEL] = value }
    }

    suspend fun setGpuTripPointTemp(value: Int) {
        dataStore.edit { preferences -> preferences[GPU_TRIP_POINT_TEMP] = value }
    }

    suspend fun setGpuTripPointHyst(value: Int) {
        dataStore.edit { preferences -> preferences[GPU_TRIP_POINT_HYST] = value }
    }

    suspend fun setBatteryOptimizationEnabled(value: Boolean) {
        dataStore.edit { preferences -> preferences[BATTERY_OPTIMIZATION_ENABLED] = value }
    }

    suspend fun setPowerSavingEnabled(value: Boolean) {
        dataStore.edit { preferences -> preferences[POWER_SAVING_ENABLED] = value }
    }

    suspend fun setEnablePowerSaverOnSleep(value: Boolean) {
        dataStore.edit { preferences -> preferences[ENABLE_POWER_SAVER_ON_SLEEP] = value }
    }

    suspend fun setDisablePowerSaverOnWake(value: Boolean) {
        dataStore.edit { preferences -> preferences[DISABLE_POWER_SAVER_ON_WAKE] = value }
    }

    suspend fun setCpuBindEnabled(value: Boolean) {
        dataStore.edit { preferences -> preferences[CPU_BIND_ENABLED] = value }
    }

    suspend fun setCpuMode(value: String) {
        dataStore.edit { preferences -> preferences[CPU_MODE] = value }
    }

    suspend fun setCpuOptimizationEnabled(value: Boolean) {
        dataStore.edit { preferences -> preferences[CPU_OPTIMIZATION_ENABLED] = value }
    }

    suspend fun setAutoSwitchCpuMode(value: Boolean) {
        dataStore.edit { preferences -> preferences[AUTO_SWITCH_CPU_MODE] = value }
    }

    suspend fun setAllowManualCpuMode(value: Boolean) {
        dataStore.edit { preferences -> preferences[ALLOW_MANUAL_CPU_MODE] = value }
    }

    suspend fun setCpuModeOnScreen(value: String) {
        dataStore.edit { preferences -> preferences[CPU_MODE_ON_SCREEN] = value }
    }

    suspend fun setCpuModeOnScreenOff(value: String) {
        dataStore.edit { preferences -> preferences[CPU_MODE_ON_SCREEN_OFF] = value }
    }

    suspend fun setDailyUpRateLimit(value: Int) {
        dataStore.edit { preferences -> preferences[DAILY_UP_RATE_LIMIT] = value }
    }

    suspend fun setDailyDownRateLimit(value: Int) {
        dataStore.edit { preferences -> preferences[DAILY_DOWN_RATE_LIMIT] = value }
    }

    suspend fun setDailyHiSpeedLoad(value: Int) {
        dataStore.edit { preferences -> preferences[DAILY_HI_SPEED_LOAD] = value }
    }

    suspend fun setDailyTargetLoads(value: Int) {
        dataStore.edit { preferences -> preferences[DAILY_TARGET_LOADS] = value }
    }

    suspend fun setStandbyUpRateLimit(value: Int) {
        dataStore.edit { preferences -> preferences[STANDBY_UP_RATE_LIMIT] = value }
    }

    suspend fun setStandbyDownRateLimit(value: Int) {
        dataStore.edit { preferences -> preferences[STANDBY_DOWN_RATE_LIMIT] = value }
    }

    suspend fun setStandbyHiSpeedLoad(value: Int) {
        dataStore.edit { preferences -> preferences[STANDBY_HI_SPEED_LOAD] = value }
    }

    suspend fun setStandbyTargetLoads(value: Int) {
        dataStore.edit { preferences -> preferences[STANDBY_TARGET_LOADS] = value }
    }

    suspend fun setDefaultUpRateLimit(value: Int) {
        dataStore.edit { preferences -> preferences[DEFAULT_UP_RATE_LIMIT] = value }
    }

    suspend fun setDefaultDownRateLimit(value: Int) {
        dataStore.edit { preferences -> preferences[DEFAULT_DOWN_RATE_LIMIT] = value }
    }

    suspend fun setDefaultHiSpeedLoad(value: Int) {
        dataStore.edit { preferences -> preferences[DEFAULT_HI_SPEED_LOAD] = value }
    }

    suspend fun setDefaultTargetLoads(value: Int) {
        dataStore.edit { preferences -> preferences[DEFAULT_TARGET_LOADS] = value }
    }

    suspend fun setPerfUpRateLimit(value: Int) {
        dataStore.edit { preferences -> preferences[PERF_UP_RATE_LIMIT] = value }
    }

    suspend fun setPerfDownRateLimit(value: Int) {
        dataStore.edit { preferences -> preferences[PERF_DOWN_RATE_LIMIT] = value }
    }

    suspend fun setPerfHiSpeedLoad(value: Int) {
        dataStore.edit { preferences -> preferences[PERF_HI_SPEED_LOAD] = value }
    }

    suspend fun setPerfTargetLoads(value: Int) {
        dataStore.edit { preferences -> preferences[PERF_TARGET_LOADS] = value }
    }

    suspend fun setFreezerEnabled(value: Boolean) {
        dataStore.edit { preferences -> preferences[FREEZER_ENABLED] = value }
    }

    suspend fun setFreezeDelay(value: Int) {
        dataStore.edit { preferences -> preferences[FREEZE_DELAY] = value }
    }

    suspend fun setProcessSuppressEnabled(value: Boolean) {
        dataStore.edit { preferences -> preferences[PROCESS_SUPPRESS_ENABLED] = value }
    }

    suspend fun setSuppressScore(value: Int) {
        dataStore.edit { preferences -> preferences[SUPPRESS_SCORE] = value }
    }

    suspend fun setSceneCheckEnabled(value: Boolean) {
        dataStore.edit { preferences -> preferences[SCENE_CHECK_ENABLED] = value }
    }

    suspend fun setCheckNetworkTraffic(value: Boolean) {
        dataStore.edit { preferences -> preferences[CHECK_NETWORK_TRAFFIC] = value }
    }

    suspend fun setCheckAudioPlayback(value: Boolean) {
        dataStore.edit { preferences -> preferences[CHECK_AUDIO_PLAYBACK] = value }
    }

    suspend fun setCheckNavigation(value: Boolean) {
        dataStore.edit { preferences -> preferences[CHECK_NAVIGATION] = value }
    }

    suspend fun setCheckPhoneCall(value: Boolean) {
        dataStore.edit { preferences -> preferences[CHECK_PHONE_CALL] = value }
    }

    suspend fun setCheckNfcP2p(value: Boolean) {
        dataStore.edit { preferences -> preferences[CHECK_NFC_P2P] = value }
    }

    suspend fun setCheckWifiHotspot(value: Boolean) {
        dataStore.edit { preferences -> preferences[CHECK_WIFI_HOTSPOT] = value }
    }

    suspend fun setCheckUsbTethering(value: Boolean) {
        dataStore.edit { preferences -> preferences[CHECK_USB_TETHERING] = value }
    }

    suspend fun setCheckScreenCasting(value: Boolean) {
        dataStore.edit { preferences -> preferences[CHECK_SCREEN_CASTING] = value }
    }

    suspend fun setCheckCharging(value: Boolean) {
        dataStore.edit { preferences -> preferences[CHECK_CHARGING] = value }
    }

    suspend fun setWhitelist(value: List<String>) {
        dataStore.edit { preferences -> preferences[WHITELIST] = value.joinToString(",") }
    }
}