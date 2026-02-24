package com.example.deepsleep.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppSettings(
    val deepSleep: DeepSleep = DeepSleep(),
    val performanceOptimization: PerformanceOptimization = PerformanceOptimization(),
    val processManagement: ProcessManagement = ProcessManagement(),
    val backgroundOptimization: BackgroundOptimization = BackgroundOptimization(),
    val sceneCheck: SceneCheck = SceneCheck(),
    val whitelist: List<WhitelistItem> = emptyList(),
    val rootGranted: Boolean = false,
    val serviceRunning: Boolean = false
) : Parcelable

@Parcelize
data class DeepSleep(
    val enabled: Boolean = false,
    val delaySeconds: Int = 60,
    val checkIntervalSeconds: Int = 30,
    val enablePowerSaverOnSleep: Boolean = false,
    val disablePowerSaverOnWake: Boolean = false
) : Parcelable

@Parcelize
data class PerformanceOptimization(
    val enabled: Boolean = false,
    val selectedMode: PerformanceMode = PerformanceMode.DAILY,
    val ecoProfile: PerformanceProfile = PerformanceProfile.defaultEco(),
    val dailyProfile: PerformanceProfile = PerformanceProfile.defaultDaily(),
    val performanceProfile: PerformanceProfile = PerformanceProfile.defaultPerformance()
) : Parcelable

enum class PerformanceMode { ECO, DAILY, PERFORMANCE }

@Parcelize
data class PerformanceProfile(
    val cpu: CpuParams,
    val gpu: GpuParams
) : Parcelable {
    companion object {
        fun defaultEco() = PerformanceProfile(
            cpu = CpuParams.defaultEco(),
            gpu = GpuParams.defaultEco()
        )
        fun defaultDaily() = PerformanceProfile(
            cpu = CpuParams.defaultDaily(),
            gpu = GpuParams.defaultDaily()
        )
        fun defaultPerformance() = PerformanceProfile(
            cpu = CpuParams.defaultPerformance(),
            gpu = GpuParams.defaultPerformance()
        )
    }
}

@Parcelize
data class CpuParams(
    val upRate: Int,
    val downRate: Int,
    val hispeedLoad: Int,
    val targetLoads: Int
) : Parcelable {
    companion object {
        fun defaultEco() = CpuParams(5000, 0, 95, 90)
        fun defaultDaily() = CpuParams(1000, 500, 85, 80)
        fun defaultPerformance() = CpuParams(0, 0, 75, 70)
    }
}

@Parcelize
data class GpuParams(
    val maxFreq: Long,
    val minFreq: Long,
    val idleTimer: Int,
    val throttlingEnabled: Boolean,
    val busSplitEnabled: Boolean,
    val thermalPwrLevel: Int,
    val tripPointTemp: Int,
    val tripPointHyst: Int
) : Parcelable {
    companion object {
        fun defaultEco() = GpuParams(
            maxFreq = 500_000_000,
            minFreq = 231_000_000,
            idleTimer = 100,
            throttlingEnabled = true,
            busSplitEnabled = true,
            thermalPwrLevel = 8,
            tripPointTemp = 45000,
            tripPointHyst = 3000
        )
        fun defaultDaily() = GpuParams(
            maxFreq = 770_000_000,
            minFreq = 310_000_000,
            idleTimer = 50,
            throttlingEnabled = true,
            busSplitEnabled = true,
            thermalPwrLevel = 5,
            tripPointTemp = 55000,
            tripPointHyst = 5000
        )
        fun defaultPerformance() = GpuParams(
            maxFreq = 903_000_000,
            minFreq = 578_000_000,
            idleTimer = 10,
            throttlingEnabled = false,
            busSplitEnabled = false,
            thermalPwrLevel = 0,
            tripPointTemp = 65000,
            tripPointHyst = 7000
        )
    }
}

@Parcelize
data class ProcessManagement(
    val enabled: Boolean = false,
    val suppress: ProcessSuppress = ProcessSuppress(),
    val freeze: ProcessFreeze = ProcessFreeze()
) : Parcelable

@Parcelize
data class ProcessSuppress(
    val enabled: Boolean = false,
    val mode: SuppressMode = SuppressMode.CONSERVATIVE,
    val oomScore: Int = 800
) : Parcelable

enum class SuppressMode { AGGRESSIVE, CONSERVATIVE }

@Parcelize
data class ProcessFreeze(
    val enabled: Boolean = false,
    val delaySeconds: Int = 30
) : Parcelable

@Parcelize
data class BackgroundOptimization(
    val enabled: Boolean = false,
    val restrictBackground: Boolean = false,
    val ignoreWakeLock: Boolean = false,
    val setStandbyBucketRare: Boolean = false
) : Parcelable

@Parcelize
data class SceneCheck(
    val enabled: Boolean = false,
    val checkNetworkTraffic: Boolean = true,
    val checkAudioPlayback: Boolean = true,
    val checkNavigation: Boolean = true,
    val checkPhoneCall: Boolean = true,
    val checkNfcP2p: Boolean = true,
    val checkWifiHotspot: Boolean = true,
    val checkUsbTethering: Boolean = true,
    val checkScreenCasting: Boolean = true,
    val checkCharging: Boolean = false
) : Parcelable

@Parcelize
data class WhitelistItem(
    val id: String,
    val name: String,
    val note: String = "",
    val type: WhitelistType
) : Parcelable

enum class WhitelistType { PROCESS, BACKGROUND, NETWORK }