package com.example.deepsleep.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.deepsleep.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

object SettingsRepository {
    private lateinit var dataStore: DataStore<Preferences>

    // 深度睡眠
    private val DEEP_SLEEP_ENABLED = booleanPreferencesKey("deep_sleep_enabled")
    private val DEEP_SLEEP_DELAY_SECONDS = intPreferencesKey("deep_sleep_delay_seconds")
    private val DEEP_SLEEP_CHECK_INTERVAL = intPreferencesKey("deep_sleep_check_interval")
    private val ENABLE_POWER_SAVER_ON_SLEEP = booleanPreferencesKey("enable_power_saver_on_sleep")
    private val DISABLE_POWER_SAVER_ON_WAKE = booleanPreferencesKey("disable_power_saver_on_wake")

    // 性能优化
    private val PERF_ENABLED = booleanPreferencesKey("perf_enabled")
    private val PERF_SELECTED_MODE = stringPreferencesKey("perf_selected_mode")

    // CPU 参数 - 省电
    private val ECO_CPU_UP_RATE = intPreferencesKey("eco_cpu_up_rate")
    private val ECO_CPU_DOWN_RATE = intPreferencesKey("eco_cpu_down_rate")
    private val ECO_CPU_HISPEED_LOAD = intPreferencesKey("eco_cpu_hispeed_load")
    private val ECO_CPU_TARGET_LOADS = intPreferencesKey("eco_cpu_target_loads")

    // CPU 参数 - 日常
    private val DAILY_CPU_UP_RATE = intPreferencesKey("daily_cpu_up_rate")
    private val DAILY_CPU_DOWN_RATE = intPreferencesKey("daily_cpu_down_rate")
    private val DAILY_CPU_HISPEED_LOAD = intPreferencesKey("daily_cpu_hispeed_load")
    private val DAILY_CPU_TARGET_LOADS = intPreferencesKey("daily_cpu_target_loads")

    // CPU 参数 - 性能
    private val PERF_CPU_UP_RATE = intPreferencesKey("perf_cpu_up_rate")
    private val PERF_CPU_DOWN_RATE = intPreferencesKey("perf_cpu_down_rate")
    private val PERF_CPU_HISPEED_LOAD = intPreferencesKey("perf_cpu_hispeed_load")
    private val PERF_CPU_TARGET_LOADS = intPreferencesKey("perf_cpu_target_loads")

    // GPU 参数 - 省电
    private val ECO_GPU_MAX_FREQ = longPreferencesKey("eco_gpu_max_freq")
    private val ECO_GPU_MIN_FREQ = longPreferencesKey("eco_gpu_min_freq")
    private val ECO_GPU_IDLE_TIMER = intPreferencesKey("eco_gpu_idle_timer")
    private val ECO_GPU_THROTTLING = booleanPreferencesKey("eco_gpu_throttling")
    private val ECO_GPU_BUS_SPLIT = booleanPreferencesKey("eco_gpu_bus_split")
    private val ECO_GPU_THERMAL_PWR = intPreferencesKey("eco_gpu_thermal_pwr")
    private val ECO_GPU_TRIP_TEMP = intPreferencesKey("eco_gpu_trip_temp")
    private val ECO_GPU_TRIP_HYST = intPreferencesKey("eco_gpu_trip_hyst")

    // GPU 参数 - 日常
    private val DAILY_GPU_MAX_FREQ = longPreferencesKey("daily_gpu_max_freq")
    private val DAILY_GPU_MIN_FREQ = longPreferencesKey("daily_gpu_min_freq")
    private val DAILY_GPU_IDLE_TIMER = intPreferencesKey("daily_gpu_idle_timer")
    private val DAILY_GPU_THROTTLING = booleanPreferencesKey("daily_gpu_throttling")
    private val DAILY_GPU_BUS_SPLIT = booleanPreferencesKey("daily_gpu_bus_split")
    private val DAILY_GPU_THERMAL_PWR = intPreferencesKey("daily_gpu_thermal_pwr")
    private val DAILY_GPU_TRIP_TEMP = intPreferencesKey("daily_gpu_trip_temp")
    private val DAILY_GPU_TRIP_HYST = intPreferencesKey("daily_gpu_trip_hyst")

    // GPU 参数 - 性能
    private val PERF_GPU_MAX_FREQ = longPreferencesKey("perf_gpu_max_freq")
    private val PERF_GPU_MIN_FREQ = longPreferencesKey("perf_gpu_min_freq")
    private val PERF_GPU_IDLE_TIMER = intPreferencesKey("perf_gpu_idle_timer")
    private val PERF_GPU_THROTTLING = booleanPreferencesKey("perf_gpu_throttling")
    private val PERF_GPU_BUS_SPLIT = booleanPreferencesKey("perf_gpu_bus_split")
    private val PERF_GPU_THERMAL_PWR = intPreferencesKey("perf_gpu_thermal_pwr")
    private val PERF_GPU_TRIP_TEMP = intPreferencesKey("perf_gpu_trip_temp")
    private val PERF_GPU_TRIP_HYST = intPreferencesKey("perf_gpu_trip_hyst")

    // 进程管理
    private val PROC_ENABLED = booleanPreferencesKey("proc_enabled")
    private val SUPPRESS_ENABLED = booleanPreferencesKey("suppress_enabled")
    private val SUPPRESS_MODE = stringPreferencesKey("suppress_mode")
    private val SUPPRESS_OOM = intPreferencesKey("suppress_oom")
    private val FREEZE_ENABLED = booleanPreferencesKey("freeze_enabled")
    private val FREEZE_DELAY = intPreferencesKey("freeze_delay")

    // 后台优化
    private val BG_ENABLED = booleanPreferencesKey("bg_enabled")
    private val BG_RESTRICT = booleanPreferencesKey("bg_restrict")
    private val BG_IGNORE_WAKE = booleanPreferencesKey("bg_ignore_wake")
    private val BG_STANDBY_RARE = booleanPreferencesKey("bg_standby_rare")

    // 场景检测
    private val SCENE_ENABLED = booleanPreferencesKey("scene_enabled")
    private val SCENE_NETWORK = booleanPreferencesKey("scene_network")
    private val SCENE_AUDIO = booleanPreferencesKey("scene_audio")
    private val SCENE_NAV = booleanPreferencesKey("scene_nav")
    private val SCENE_CALL = booleanPreferencesKey("scene_call")
    private val SCENE_NFC = booleanPreferencesKey("scene_nfc")
    private val SCENE_HOTSPOT = booleanPreferencesKey("scene_hotspot")
    private val SCENE_USB = booleanPreferencesKey("scene_usb")
    private val SCENE_CAST = booleanPreferencesKey("scene_cast")
    private val SCENE_CHARGING = booleanPreferencesKey("scene_charging")

    // 白名单（简单用逗号分隔）
    private val WHITELIST = stringPreferencesKey("whitelist")

    // 系统状态
    private val ROOT_GRANTED = stringPreferencesKey("root_granted")
    private val SERVICE_RUNNING = booleanPreferencesKey("service_running")

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

    fun initialize(context: Context) {
        dataStore = context.dataStore
    }

    val settings: Flow<AppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                deepSleep = DeepSleep(
                    enabled = preferences[DEEP_SLEEP_ENABLED] ?: false,
                    delaySeconds = preferences[DEEP_SLEEP_DELAY_SECONDS] ?: 60,
                    checkIntervalSeconds = preferences[DEEP_SLEEP_CHECK_INTERVAL] ?: 30,
                    enablePowerSaverOnSleep = preferences[ENABLE_POWER_SAVER_ON_SLEEP] ?: false,
                    disablePowerSaverOnWake = preferences[DISABLE_POWER_SAVER_ON_WAKE] ?: false
                ),
                performanceOptimization = PerformanceOptimization(
                    enabled = preferences[PERF_ENABLED] ?: false,
                    selectedMode = PerformanceMode.valueOf(preferences[PERF_SELECTED_MODE] ?: "DAILY"),
                    ecoProfile = PerformanceProfile(
                        cpu = CpuParams(
                            upRate = preferences[ECO_CPU_UP_RATE] ?: 5000,
                            downRate = preferences[ECO_CPU_DOWN_RATE] ?: 0,
                            hispeedLoad = preferences[ECO_CPU_HISPEED_LOAD] ?: 95,
                            targetLoads = preferences[ECO_CPU_TARGET_LOADS] ?: 90
                        ),
                        gpu = GpuParams(
                            maxFreq = preferences[ECO_GPU_MAX_FREQ] ?: 500_000_000,
                            minFreq = preferences[ECO_GPU_MIN_FREQ] ?: 231_000_000,
                            idleTimer = preferences[ECO_GPU_IDLE_TIMER] ?: 100,
                            throttlingEnabled = preferences[ECO_GPU_THROTTLING] ?: true,
                            busSplitEnabled = preferences[ECO_GPU_BUS_SPLIT] ?: true,
                            thermalPwrLevel = preferences[ECO_GPU_THERMAL_PWR] ?: 8,
                            tripPointTemp = preferences[ECO_GPU_TRIP_TEMP] ?: 45000,
                            tripPointHyst = preferences[ECO_GPU_TRIP_HYST] ?: 3000
                        )
                    ),
                    dailyProfile = PerformanceProfile(
                        cpu = CpuParams(
                            upRate = preferences[DAILY_CPU_UP_RATE] ?: 1000,
                            downRate = preferences[DAILY_CPU_DOWN_RATE] ?: 500,
                            hispeedLoad = preferences[DAILY_CPU_HISPEED_LOAD] ?: 85,
                            targetLoads = preferences[DAILY_CPU_TARGET_LOADS] ?: 80
                        ),
                        gpu = GpuParams(
                            maxFreq = preferences[DAILY_GPU_MAX_FREQ] ?: 770_000_000,
                            minFreq = preferences[DAILY_GPU_MIN_FREQ] ?: 310_000_000,
                            idleTimer = preferences[DAILY_GPU_IDLE_TIMER] ?: 50,
                            throttlingEnabled = preferences[DAILY_GPU_THROTTLING] ?: true,
                            busSplitEnabled = preferences[DAILY_GPU_BUS_SPLIT] ?: true,
                            thermalPwrLevel = preferences[DAILY_GPU_THERMAL_PWR] ?: 5,
                            tripPointTemp = preferences[DAILY_GPU_TRIP_TEMP] ?: 55000,
                            tripPointHyst = preferences[DAILY_GPU_TRIP_HYST] ?: 5000
                        )
                    ),
                    performanceProfile = PerformanceProfile(
                        cpu = CpuParams(
                            upRate = preferences[PERF_CPU_UP_RATE] ?: 0,
                            downRate = preferences[PERF_CPU_DOWN_RATE] ?: 0,
                            hispeedLoad = preferences[PERF_CPU_HISPEED_LOAD] ?: 75,
                            targetLoads = preferences[PERF_CPU_TARGET_LOADS] ?: 70
                        ),
                        gpu = GpuParams(
                            maxFreq = preferences[PERF_GPU_MAX_FREQ] ?: 903_000_000,
                            minFreq = preferences[PERF_GPU_MIN_FREQ] ?: 578_000_000,
                            idleTimer = preferences[PERF_GPU_IDLE_TIMER] ?: 10,
                            throttlingEnabled = preferences[PERF_GPU_THROTTLING] ?: false,
                            busSplitEnabled = preferences[PERF_GPU_BUS_SPLIT] ?: false,
                            thermalPwrLevel = preferences[PERF_GPU_THERMAL_PWR] ?: 0,
                            tripPointTemp = preferences[PERF_GPU_TRIP_TEMP] ?: 65000,
                            tripPointHyst = preferences[PERF_GPU_TRIP_HYST] ?: 7000
                        )
                    )
                ),
                processManagement = ProcessManagement(
                    enabled = preferences[PROC_ENABLED] ?: false,
                    suppress = ProcessSuppress(
                        enabled = preferences[SUPPRESS_ENABLED] ?: false,
                        mode = SuppressMode.valueOf(preferences[SUPPRESS_MODE] ?: "CONSERVATIVE"),
                        oomScore = preferences[SUPPRESS_OOM] ?: 800
                    ),
                    freeze = ProcessFreeze(
                        enabled = preferences[FREEZE_ENABLED] ?: false,
                        delaySeconds = preferences[FREEZE_DELAY] ?: 30
                    )
                ),
                backgroundOptimization = BackgroundOptimization(
                    enabled = preferences[BG_ENABLED] ?: false,
                    restrictBackground = preferences[BG_RESTRICT] ?: false,
                    ignoreWakeLock = preferences[BG_IGNORE_WAKE] ?: false,
                    setStandbyBucketRare = preferences[BG_STANDBY_RARE] ?: false
                ),
                sceneCheck = SceneCheck(
                    enabled = preferences[SCENE_ENABLED] ?: false,
                    checkNetworkTraffic = preferences[SCENE_NETWORK] ?: true,
                    checkAudioPlayback = preferences[SCENE_AUDIO] ?: true,
                    checkNavigation = preferences[SCENE_NAV] ?: true,
                    checkPhoneCall = preferences[SCENE_CALL] ?: true,
                    checkNfcP2p = preferences[SCENE_NFC] ?: true,
                    checkWifiHotspot = preferences[SCENE_HOTSPOT] ?: true,
                    checkUsbTethering = preferences[SCENE_USB] ?: true,
                    checkScreenCasting = preferences[SCENE_CAST] ?: true,
                    checkCharging = preferences[SCENE_CHARGING] ?: false
                ),
                whitelist = preferences[WHITELIST]?.split(",")?.filter { it.isNotBlank() }?.map { pkg ->
                    WhitelistItem(id = pkg, name = pkg, note = "", type = WhitelistType.PROCESS) // 简化
                } ?: emptyList(),
                rootGranted = preferences[ROOT_GRANTED]?.toBoolean() ?: false,
                serviceRunning = preferences[SERVICE_RUNNING] ?: false
            )
        }

    suspend fun updateDeepSleep(deepSleep: DeepSleep) {
        dataStore.edit { prefs ->
            prefs[DEEP_SLEEP_ENABLED] = deepSleep.enabled
            prefs[DEEP_SLEEP_DELAY_SECONDS] = deepSleep.delaySeconds
            prefs[DEEP_SLEEP_CHECK_INTERVAL] = deepSleep.checkIntervalSeconds
            prefs[ENABLE_POWER_SAVER_ON_SLEEP] = deepSleep.enablePowerSaverOnSleep
            prefs[DISABLE_POWER_SAVER_ON_WAKE] = deepSleep.disablePowerSaverOnWake
        }
    }

    suspend fun updatePerformanceOptimization(perf: PerformanceOptimization) {
        dataStore.edit { prefs ->
            prefs[PERF_ENABLED] = perf.enabled
            prefs[PERF_SELECTED_MODE] = perf.selectedMode.name

            // Eco
            prefs[ECO_CPU_UP_RATE] = perf.ecoProfile.cpu.upRate
            prefs[ECO_CPU_DOWN_RATE] = perf.ecoProfile.cpu.downRate
            prefs[ECO_CPU_HISPEED_LOAD] = perf.ecoProfile.cpu.hispeedLoad
            prefs[ECO_CPU_TARGET_LOADS] = perf.ecoProfile.cpu.targetLoads
            prefs[ECO_GPU_MAX_FREQ] = perf.ecoProfile.gpu.maxFreq
            prefs[ECO_GPU_MIN_FREQ] = perf.ecoProfile.gpu.minFreq
            prefs[ECO_GPU_IDLE_TIMER] = perf.ecoProfile.gpu.idleTimer
            prefs[ECO_GPU_THROTTLING] = perf.ecoProfile.gpu.throttlingEnabled
            prefs[ECO_GPU_BUS_SPLIT] = perf.ecoProfile.gpu.busSplitEnabled
            prefs[ECO_GPU_THERMAL_PWR] = perf.ecoProfile.gpu.thermalPwrLevel
            prefs[ECO_GPU_TRIP_TEMP] = perf.ecoProfile.gpu.tripPointTemp
            prefs[ECO_GPU_TRIP_HYST] = perf.ecoProfile.gpu.tripPointHyst

            // Daily
            prefs[DAILY_CPU_UP_RATE] = perf.dailyProfile.cpu.upRate
            prefs[DAILY_CPU_DOWN_RATE] = perf.dailyProfile.cpu.downRate
            prefs[DAILY_CPU_HISPEED_LOAD] = perf.dailyProfile.cpu.hispeedLoad
            prefs[DAILY_CPU_TARGET_LOADS] = perf.dailyProfile.cpu.targetLoads
            prefs[DAILY_GPU_MAX_FREQ] = perf.dailyProfile.gpu.maxFreq
            prefs[DAILY_GPU_MIN_FREQ] = perf.dailyProfile.gpu.minFreq
            prefs[DAILY_GPU_IDLE_TIMER] = perf.dailyProfile.gpu.idleTimer
            prefs[DAILY_GPU_THROTTLING] = perf.dailyProfile.gpu.throttlingEnabled
            prefs[DAILY_GPU_BUS_SPLIT] = perf.dailyProfile.gpu.busSplitEnabled
            prefs[DAILY_GPU_THERMAL_PWR] = perf.dailyProfile.gpu.thermalPwrLevel
            prefs[DAILY_GPU_TRIP_TEMP] = perf.dailyProfile.gpu.tripPointTemp
            prefs[DAILY_GPU_TRIP_HYST] = perf.dailyProfile.gpu.tripPointHyst

            // Performance
            prefs[PERF_CPU_UP_RATE] = perf.performanceProfile.cpu.upRate
            prefs[PERF_CPU_DOWN_RATE] = perf.performanceProfile.cpu.downRate
            prefs[PERF_CPU_HISPEED_LOAD] = perf.performanceProfile.cpu.hispeedLoad
            prefs[PERF_CPU_TARGET_LOADS] = perf.performanceProfile.cpu.targetLoads
            prefs[PERF_GPU_MAX_FREQ] = perf.performanceProfile.gpu.maxFreq
            prefs[PERF_GPU_MIN_FREQ] = perf.performanceProfile.gpu.minFreq
            prefs[PERF_GPU_IDLE_TIMER] = perf.performanceProfile.gpu.idleTimer
            prefs[PERF_GPU_THROTTLING] = perf.performanceProfile.gpu.throttlingEnabled
            prefs[PERF_GPU_BUS_SPLIT] = perf.performanceProfile.gpu.busSplitEnabled
            prefs[PERF_GPU_THERMAL_PWR] = perf.performanceProfile.gpu.thermalPwrLevel
            prefs[PERF_GPU_TRIP_TEMP] = perf.performanceProfile.gpu.tripPointTemp
            prefs[PERF_GPU_TRIP_HYST] = perf.performanceProfile.gpu.tripPointHyst
        }
    }

    suspend fun updateProcessManagement(pm: ProcessManagement) {
        dataStore.edit { prefs ->
            prefs[PROC_ENABLED] = pm.enabled
            prefs[SUPPRESS_ENABLED] = pm.suppress.enabled
            prefs[SUPPRESS_MODE] = pm.suppress.mode.name
            prefs[SUPPRESS_OOM] = pm.suppress.oomScore
            prefs[FREEZE_ENABLED] = pm.freeze.enabled
            prefs[FREEZE_DELAY] = pm.freeze.delaySeconds
        }
    }

    suspend fun updateBackgroundOptimization(bg: BackgroundOptimization) {
        dataStore.edit { prefs ->
            prefs[BG_ENABLED] = bg.enabled
            prefs[BG_RESTRICT] = bg.restrictBackground
            prefs[BG_IGNORE_WAKE] = bg.ignoreWakeLock
            prefs[BG_STANDBY_RARE] = bg.setStandbyBucketRare
        }
    }

    suspend fun updateSceneCheck(scene: SceneCheck) {
        dataStore.edit { prefs ->
            prefs[SCENE_ENABLED] = scene.enabled
            prefs[SCENE_NETWORK] = scene.checkNetworkTraffic
            prefs[SCENE_AUDIO] = scene.checkAudioPlayback
            prefs[SCENE_NAV] = scene.checkNavigation
            prefs[SCENE_CALL] = scene.checkPhoneCall
            prefs[SCENE_NFC] = scene.checkNfcP2p
            prefs[SCENE_HOTSPOT] = scene.checkWifiHotspot
            prefs[SCENE_USB] = scene.checkUsbTethering
            prefs[SCENE_CAST] = scene.checkScreenCasting
            prefs[SCENE_CHARGING] = scene.checkCharging
        }
    }

    suspend fun updateWhitelist(whitelist: List<WhitelistItem>) {
        val str = whitelist.joinToString(",") { it.name }
        dataStore.edit { prefs -> prefs[WHITELIST] = str }
    }

    suspend fun setRootGranted(granted: Boolean) {
        dataStore.edit { prefs -> prefs[ROOT_GRANTED] = granted.toString() }
    }

    suspend fun setServiceRunning(running: Boolean) {
        dataStore.edit { prefs -> prefs[SERVICE_RUNNING] = running }
    }
}