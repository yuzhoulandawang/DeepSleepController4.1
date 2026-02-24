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

class SceneChecker(private val context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    suspend fun shouldBlockDeepSleep(settings: AppSettings): Boolean = withContext(Dispatchers.IO) {
        val blockedScenes = mutableListOf<String>()

        if (settings.sceneCheck.checkNetworkTraffic && isNetworkTrafficActive()) blockedScenes.add("流量活跃")
        if (settings.sceneCheck.checkAudioPlayback && isAudioPlaying()) blockedScenes.add("音频播放")
        if (settings.sceneCheck.checkNavigation && isNavigationActive()) blockedScenes.add("导航应用")
        if (settings.sceneCheck.checkPhoneCall && isInPhoneCall()) blockedScenes.add("通话状态")
        if (settings.sceneCheck.checkNfcP2p && isNfcP2pActive()) blockedScenes.add("NFC/P2P")
        if (settings.sceneCheck.checkWifiHotspot && isWifiHotspotEnabled()) blockedScenes.add("WiFi热点")
        if (settings.sceneCheck.checkUsbTethering && isUsbTetheringEnabled()) blockedScenes.add("USB网络共享")
        if (settings.sceneCheck.checkScreenCasting && isScreenCasting()) blockedScenes.add("投屏")
        if (settings.sceneCheck.checkCharging && isCharging()) blockedScenes.add("充电状态")

        if (blockedScenes.isNotEmpty()) {
            LogRepository.appendLog(LogLevel.INFO, "SceneChecker", "深度睡眠被阻止: ${blockedScenes.joinToString(", ")}")
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
            val whitelist = WhitelistRepository.loadItems(context, WhitelistType.NETWORK).map { it.name }
            return runningPackages.any { !whitelist.contains(it) }
        } catch (e: Exception) {
            false
        }
    }

    private fun isAudioPlaying(): Boolean = audioManager.isMusicActive && audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) > 0
    private suspend fun isNavigationActive(): Boolean = RootCommander.exec("dumpsys activity top | grep -E '(google|amap|mapnav|com.autonavi)' | grep -v 'STOPPED'").out.isNotEmpty()
    private fun isInPhoneCall(): Boolean = telephonyManager.callState == TelephonyManager.CALL_STATE_OFFHOOK
    private suspend fun isNfcP2pActive(): Boolean = RootCommander.exec("dumpsys nfc | grep 'P2P' | grep -v 'disabled'").out.any { it.contains("P2P") && it.contains("true") }
    private suspend fun isWifiHotspotEnabled(): Boolean = RootCommander.exec("dumpsys connectivity | grep -i 'tethering'").out.any { it.contains("tethering") }
    private suspend fun isUsbTetheringEnabled(): Boolean = RootCommander.exec("dumpsys connectivity | grep -i 'usb' | grep -i 'rndis'").out.any { it.contains("rndis") }
    private suspend fun isScreenCasting(): Boolean {
        val r1 = RootCommander.exec("dumpsys media.audio_flinger | grep -i 'hdmi'")
        val r2 = RootCommander.exec("dumpsys display | grep -i 'cast'")
        val r3 = RootCommander.exec("dumpsys connectivity | grep -i 'miracast'")
        return r1.out.any { it.contains("hdmi") } || r2.out.any { it.contains("cast") } || r3.out.any { it.contains("miracast") }
    }
    private fun isCharging(): Boolean {
        val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }
}