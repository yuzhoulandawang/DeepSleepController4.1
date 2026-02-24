package com.example.deepsleep.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.deepsleep.MainActivity
import com.example.deepsleep.R
import com.example.deepsleep.data.*
import com.example.deepsleep.model.PerformanceMode
import com.example.deepsleep.root.*
import kotlinx.coroutines.*

class DeepSleepService : Service() {
    private val TAG = "DeepSleepService"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var enterJob: Job? = null

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> handleScreenOn()
                Intent.ACTION_SCREEN_OFF -> handleScreenOff()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerReceiver(screenStateReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        })
        startForeground(NOTIFICATION_ID, createNotification("服务运行中"))
        Log.i(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    private fun handleScreenOn() {
        serviceScope.launch {
            enterJob?.cancel()
            enterJob = null

            DeepSleepController.exitDeepSleep()

            val settings = SettingsRepository.settings.first()
            if (settings.performanceOptimization.enabled) {
                val mode = settings.performanceOptimization.selectedMode
                val profile = when (mode) {
                    PerformanceMode.ECO -> settings.performanceOptimization.ecoProfile
                    PerformanceMode.DAILY -> settings.performanceOptimization.dailyProfile
                    PerformanceMode.PERFORMANCE -> settings.performanceOptimization.performanceProfile
                }
                OptimizationManager.applyPerformanceProfile(profile, mode)
            }

            ProcessManager.onScreenStateChanged(true)

            if (settings.deepSleep.disablePowerSaverOnWake) {
                PowerSaverController.disablePowerSaver()
            }

            LogRepository.info(TAG, "Screen ON: applied performance and exited deep sleep")
        }
    }

    private fun handleScreenOff() {
        serviceScope.launch {
            val settings = SettingsRepository.settings.first()
            if (!settings.deepSleep.enabled && !settings.performanceOptimization.enabled && !settings.processManagement.suppress.enabled) {
                return@launch
            }

            val delay = settings.deepSleep.delaySeconds * 1000L
            enterJob = launch {
                delay(delay)

                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                if (pm.isInteractive) return@launch

                if (settings.sceneCheck.enabled) {
                    val sceneChecker = SceneChecker(this@DeepSleepService)
                    if (sceneChecker.shouldBlockDeepSleep(settings)) {
                        LogRepository.info(TAG, "场景检测阻止进入深度睡眠")
                        return@launch
                    }
                }

                if (settings.performanceOptimization.enabled) {
                    OptimizationManager.applyPerformanceProfile(
                        settings.performanceOptimization.ecoProfile,
                        PerformanceMode.ECO
                    )
                }

                ProcessManager.onScreenStateChanged(false)

                if (settings.deepSleep.enabled) {
                    DeepSleepController.enterDeepSleep(
                        blockExit = true,
                        checkIntervalSeconds = settings.deepSleep.checkIntervalSeconds
                    )
                }

                if (settings.deepSleep.enablePowerSaverOnSleep) {
                    PowerSaverController.enablePowerSaver()
                }

                LogRepository.info(TAG, "Screen OFF: entered deep sleep mode")
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(screenStateReceiver)
        serviceScope.cancel()
        ProcessManager.onDestroy()
        runBlocking { DeepSleepController.exitDeepSleep() }   // 修复点
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "deepsleep_service"
        const val ACTION_START = "com.example.deepsleep.ACTION_START"
        const val ACTION_STOP = "com.example.deepsleep.ACTION_STOP"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "DeepSleep Controller", NotificationManager.IMPORTANCE_LOW).apply {
                description = "DeepSleep 后台服务"
                setShowBadge(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DeepSleep 控制器")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}