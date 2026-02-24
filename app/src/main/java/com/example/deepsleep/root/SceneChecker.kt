package com.example.deepsleep.root

import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.PowerManager
import android.telephony.TelephonyManager
import com.example.deepsleep.data.LogRepository
import com.example.deepsleep.data.WhitelistRepository
import com.example.deepsleep.model.AppSettings
import com.example.deepsleep.model.LogLevel
import com.example.deepsleep.model.WhitelistType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SceneChecker(
    private val context: Context,
    private val logRepository: LogRepository,
    private val whitelistRepository: WhitelistRepository
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private var networkWhitelist: List<String> = emptyList()
    private var lastWhitelistUpdate = 0L
    private val whitelistCacheDuration = 30_000L

    private suspend fun updateNetworkWhitelist() {
        val now = System.currentTimeMillis()
        if (now - lastWhitelistUpdate < whitelistCacheDuration) return
        try {
            networkWhitelist = whitelistRepository.loadItems(context, WhitelistType.NETWORK).map { it.name }
            lastWhitelistUpdate = now
            logRepository.appendLog(LogLevel.DEBUG, "SceneChecker", "网络白名单已更新，共 ${networkWhitelist.size} 个应用")
        } catch (e: Exception) {
            logRepository.appendLog(LogLevel.ERROR, "SceneChecker", "更新网络白名单失败: ${e.message}")
        }
    }

    private fun isInNetworkWhitelist(packageName: String): Boolean = networkWhitelist.contains(packageName)

    suspend fun shouldBlockDeepSleep(settings: AppSettings): Boolean = withContext(Dispatchers.IO) {
        updateNetworkWhitelist()
        val blockedScenes = mutableListOf<String>()

        if (settings.checkNetworkTraffic && isNetworkTrafficActive()) blockedScenes.add("流量活跃")
        if (settings.checkAudioPlayback && isAudioPlaying()) blockedScenes.add("音频播放")
        if (settings.checkNavigation && isNavigationActive()) blockedScenes.add("导航应用")
        if (settings.checkPhoneCall && isInPhoneCall()) blockedScenes.add("通话状态")
        if (settings.checkNfcP2p && isNfcP2pActive()) blockedScenes.add("NFC/P2P")
        if (settings.checkWifiHotspot && isWifiHotspotEnabled()) blockedScenes.add("WiFi热点")
        if (settings.checkUsbTethering && isUsbTetheringEnabled()) blockedScenes.add("USB网络共享")
        if (settings.checkScreenCasting && isScreenCasting()) blockedScenes.add("投屏")
        if (settings.checkCharging && isCharging()) blockedScenes.add("充电状态")

        if (blockedScenes.isNotEmpty()) {
            logRepository.appendLog(LogLevel.INFO, "SceneChecker", "深度睡眠被阻止: ${blockedScenes.joinToString(", ")}")
        }
        blockedScenes.isNotEmpty()
    }

    private suspend fun isNetworkTrafficActive(): Boolean {
        return try {
            val activeNetwork = connectivityManager.activeNetworkInfo ?: return false
            if (!activeNetwork.isConnected) return false
            val runningAppsResult = RootCommander.exec("dumpsys activity processes | grep -E 'Record=\\{' | grep -v 'STOPPED'")
            val runningPackages = runningAppsResult.out.mapNotNull { line ->
                Regex("([^/]+)/(\\w+)").find(line)?.groupValues?.getOrNull(1)
            }.distinct()
            if (runningPackages.isNotEmpty()) {
                return runningPackages.any { !isInNetworkWhitelist(it) }
            }
            false
        } catch (e: Exception) {
            logRepository.appendLog(LogLevel.ERROR, "SceneChecker", "检查流量活跃失败: ${e.message}")
            false
        }
    }

    private suspend fun isAudioPlaying(): Boolean {
        return try {
            audioManager.isMusicActive && audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) > 0
        } catch (e: Exception) {
            logRepository.appendLog(LogLevel.ERROR, "SceneChecker", "检查音频播放失败: ${e.message}")
            false
        }
    }

    private suspend fun isNavigationActive(): Boolean {
        return try {
            RootCommander.exec("dumpsys activity top | grep -E '(google|amap|mapnav|com.autonavi)' | grep -v 'STOPPED'").out.isNotEmpty()
        } catch (e: Exception) {
            logRepository.appendLog(LogLevel.ERROR, "SceneChecker", "检查导航应用失败: ${e.message}")
            false
        }
    }

    private suspend fun isInPhoneCall(): Boolean {   // 改为 suspend
        return try {
            val callState = telephonyManager.callState
            callState == TelephonyManager.CALL_STATE_OFFHOOK || callState == TelephonyManager.CALL_STATE_RINGING
        } catch (e: Exception) {
            logRepository.appendLog(LogLevel.ERROR, "SceneChecker", "检查通话状态失败: ${e.message}")
            false
        }
    }

    private suspend fun isNfcP2pActive(): Boolean {
        return try {
            RootCommander.exec("dumpsys nfc | grep 'P2P' | grep -v 'disabled' | grep -v 'false'").out.any { it.contains("P2P") && it.contains("true") }
        } catch (e: Exception) {
            logRepository.appendLog(LogLevel.ERROR, "SceneChecker", "检查 NFC P2P 失败: ${e.message}")
            false
        }
    }

    private suspend fun isWifiHotspotEnabled(): Boolean {
        return try {
            RootCommander.exec("dumpsys connectivity | grep -i 'tethering'").out.any { it.contains("tethering") }
        } catch (e: Exception) {
            logRepository.appendLog(LogLevel.ERROR, "SceneChecker", "检查 WiFi 热点失败: ${e.message}")
            false
        }
    }

    private suspend fun isUsbTetheringEnabled(): Boolean {
        return try {
            RootCommander.exec("dumpsys connectivity | grep -i 'usb' | grep -i 'rndis'").out.any { it.contains("rndis") || it.contains("tethering") }
        } catch (e: Exception) {
            logRepository.appendLog(LogLevel.ERROR, "SceneChecker", "检查 USB 网络共享失败: ${e.message}")
            false
        }
    }

    private suspend fun isScreenCasting(): Boolean {
        return try {
            val r1 = RootCommander.exec("dumpsys media.audio_flinger | grep -i 'hdmi'")
            val r2 = RootCommander.exec("dumpsys display | grep -i 'cast'")
            val r3 = RootCommander.exec("dumpsys connectivity | grep -i 'miracast'")
            r1.out.any { it.contains("hdmi") } || r2.out.any { it.contains("cast") } || r3.out.any { it.contains("miracast") }
        } catch (e: Exception) {
            logRepository.appendLog(LogLevel.ERROR, "SceneChecker", "检查投屏失败: ${e.message}")
            false
        }
    }

    private suspend fun isCharging(): Boolean {   // 改为 suspend
        return try {
            val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        } catch (e: Exception) {
            logRepository.appendLog(LogLevel.ERROR, "SceneChecker", "检查充电状态失败: ${e.message}")
            false
        }
    }

    suspend fun getBlockingScenes(settings: AppSettings): List<String> = withContext(Dispatchers.IO) {
        updateNetworkWhitelist()
        val scenes = mutableListOf<String>()
        if (settings.checkNetworkTraffic && isNetworkTrafficActive()) scenes.add("流量活跃")
        if (settings.checkAudioPlayback && isAudioPlaying()) scenes.add("音频播放")
        if (settings.checkNavigation && isNavigationActive()) scenes.add("导航应用")
        if (settings.checkPhoneCall && isInPhoneCall()) scenes.add("通话状态")
        if (settings.checkNfcP2p && isNfcP2pActive()) scenes.add("NFC/P2P")
        if (settings.checkWifiHotspot && isWifiHotspotEnabled()) scenes.add("WiFi热点")
        if (settings.checkUsbTethering && isUsbTetheringEnabled()) scenes.add("USB网络共享")
        if (settings.checkScreenCasting && isScreenCasting()) scenes.add("投屏")
        if (settings.checkCharging && isCharging()) scenes.add("充电状态")
        scenes
    }
}