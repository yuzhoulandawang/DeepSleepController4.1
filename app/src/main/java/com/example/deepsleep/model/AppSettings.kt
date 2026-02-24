package com.example.deepsleep.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 应用设置数据类
 * 注意：所有字段必须与 SettingsRepository 中的键一一对应
 */
@Parcelize
data class AppSettings(
    // ========== 根权限状态 ==========
    val rootGranted: Boolean = false,
    val serviceRunning: Boolean = false,

    // ========== 深度睡眠控制 ==========
    val deepSleepEnabled: Boolean = false,
    val wakeupSuppressEnabled: Boolean = false,
    val alarmSuppressEnabled: Boolean = false,

    // ========== 深度 Doze 配置 ==========
    val deepDozeEnabled: Boolean = false,
    val deepDozeDelaySeconds: Int = 30,
    val deepDozeForceMode: Boolean = false,

    // ========== 深度睡眠 Hook 配置 ==========
    val deepSleepHookEnabled: Boolean = false,
    val deepSleepDelaySeconds: Int = 60,
    val deepSleepBlockExit: Boolean = false,
    val deepSleepCheckInterval: Int = 30,

    // ========== 后台优化 ==========
    val backgroundOptimizationEnabled: Boolean = false,
    val appSuspendEnabled: Boolean = false,
    val backgroundRestrictEnabled: Boolean = false,

    // ========== GPU 优化 ==========
    val gpuOptimizationEnabled: Boolean = false,
    val gpuMode: String = "default",
    val gpuThrottlingEnabled: Boolean = false,
    val gpuBusSplitEnabled: Boolean = false,
    val gpuIdleTimer: Int = 50,
    val gpuMaxFreq: Long = 770000000L,
    val gpuMinFreq: Long = 310000000L,
    val gpuThermalPwrLevel: Int = 5,
    val gpuTripPointTemp: Int = 55000,
    val gpuTripPointHyst: Int = 5000,

    // ========== 电池优化 ==========
    val batteryOptimizationEnabled: Boolean = false,
    val powerSavingEnabled: Boolean = false,

    // ========== 系统省电模式联动 ==========
    val enablePowerSaverOnSleep: Boolean = false,
    val disablePowerSaverOnWake: Boolean = false,

    // ========== CPU 绑定 ==========
    val cpuBindEnabled: Boolean = false,
    val cpuMode: String = "daily",

    // ========== CPU 调度优化 ==========
    val cpuOptimizationEnabled: Boolean = false,
    val autoSwitchCpuMode: Boolean = false,
    val allowManualCpuMode: Boolean = true,
    val cpuModeOnScreen: String = "daily",
    val cpuModeOnScreenOff: String = "standby",

    // ========== CPU 参数 - 日常模式 ==========
    val dailyUpRateLimit: Int = 1000,
    val dailyDownRateLimit: Int = 500,
    val dailyHiSpeedLoad: Int = 85,
    val dailyTargetLoads: Int = 80,

    // ========== CPU 参数 - 待机模式 ==========
    val standbyUpRateLimit: Int = 5000,
    val standbyDownRateLimit: Int = 0,
    val standbyHiSpeedLoad: Int = 95,
    val standbyTargetLoads: Int = 90,

    // ========== CPU 参数 - 默认模式 ==========
    val defaultUpRateLimit: Int = 0,
    val defaultDownRateLimit: Int = 0,
    val defaultHiSpeedLoad: Int = 90,
    val defaultTargetLoads: Int = 90,

    // ========== CPU 参数 - 性能模式 ==========
    val perfUpRateLimit: Int = 0,
    val perfDownRateLimit: Int = 0,
    val perfHiSpeedLoad: Int = 75,
    val perfTargetLoads: Int = 70,

    // ========== Freezer 服务 ==========
    val freezerEnabled: Boolean = false,
    val freezeDelay: Int = 30,

    // ========== 白名单 ==========
    val whitelist: List<String> = emptyList(),

    // ========== 场景检测 ==========
    val sceneCheckEnabled: Boolean = false,
    val checkNetworkTraffic: Boolean = true,
    val checkAudioPlayback: Boolean = true,
    val checkNavigation: Boolean = true,
    val checkPhoneCall: Boolean = true,
    val checkNfcP2p: Boolean = true,
    val checkWifiHotspot: Boolean = true,
    val checkUsbTethering: Boolean = true,
    val checkScreenCasting: Boolean = true,
    val checkCharging: Boolean = false,

    // ========== 进程压制（OOM 评分） ==========
    val processSuppressEnabled: Boolean = false,
    val suppressScore: Int = 500
) : Parcelable
