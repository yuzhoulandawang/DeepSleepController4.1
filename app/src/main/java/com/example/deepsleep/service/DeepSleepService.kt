package com.example.deepsleep.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.deepsleep.MainActivity
import com.example.deepsleep.R
import com.example.deepsleep.data.LogRepository
import com.example.deepsleep.data.SettingsRepository
import com.example.deepsleep.data.StatsRepository
import com.example.deepsleep.model.AppSettings
import com.example.deepsleep.model.LogLevel
import com.example.deepsleep.root.OptimizationManager
import com.example.deepsleep.root.ProcessSuppressor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class DeepSleepService : Service() {

    companion object {
        private const val TAG = "DeepSleepService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "deepsleep_service"
        private const val CHANNEL_NAME = "DeepSleep Controller"
        const val ACTION_START = "com.example.deepsleep.ACTION_START"
        const val ACTION_STOP = "com.example.deepsleep.ACTION_STOP"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false
    private var optimizationJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.i(TAG, "Service created")
        serviceScope.launch {
            LogRepository.appendLog(LogLevel.INFO, TAG, "DeepSleep service created")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action}")
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        if (!isRunning) {
            startAsForeground()
            isRunning = true
            // 更新服务运行状态
            serviceScope.launch {
                SettingsRepository.setServiceRunning(true)
                LogRepository.appendLog(LogLevel.INFO, TAG, "DeepSleep service started")
            }
            startOptimizationLoop()
        }
        return START_STICKY
    }

    private fun startAsForeground() {
        startForeground(NOTIFICATION_ID, createNotification("DeepSleep 控制器运行中"))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
                description = "DeepSleep 后台优化服务"
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

    private fun startOptimizationLoop() {
        optimizationJob = serviceScope.launch {
            Log.i(TAG, "Starting optimization loop")
            while (isActive) {
                try {
                    val settings = SettingsRepository.settings.first()
                    if (settings.backgroundOptimizationEnabled) applyBackgroundOptimization(settings)
                    if (settings.gpuOptimizationEnabled) applyGpuOptimization(settings)
                    if (settings.processSuppressEnabled) applyProcessSuppression(settings)
                    if (settings.cpuBindEnabled) applyCpuBinding(settings)
                    delay(60000)
                } catch (e: CancellationException) {
                    Log.i(TAG, "Optimization loop cancelled")
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Optimization loop error", e)
                    LogRepository.appendLog(LogLevel.ERROR, TAG, "Optimization loop error: ${e.message}")
                    delay(10000)
                }
            }
        }
    }

    private suspend fun applyBackgroundOptimization(settings: AppSettings) {
        if (settings.backgroundRestrictEnabled) {
            val count = ProcessSuppressor.suppressBackgroundApps(settings.suppressScore)
            Log.d(TAG, "Background restrict: suppressed $count apps")
        }
    }

    private suspend fun applyGpuOptimization(settings: AppSettings) {
        val mode = when (settings.gpuMode) {
            "performance" -> OptimizationManager.PerformanceMode.PERFORMANCE
            "power_saving" -> OptimizationManager.PerformanceMode.STANDBY
            else -> OptimizationManager.PerformanceMode.DAILY
        }
        if (OptimizationManager.applyAllOptimizations(mode)) {
            StatsRepository.recordGpuOptimization()
        }
    }

    private suspend fun applyProcessSuppression(settings: AppSettings) {
        val count = ProcessSuppressor.suppressBackgroundApps(settings.suppressScore)
        if (count > 0) StatsRepository.recordAppSuppressed(count)
    }

    private suspend fun applyCpuBinding(settings: AppSettings) {
        val mode = when (settings.cpuMode) {
            "performance" -> OptimizationManager.PerformanceMode.PERFORMANCE
            "standby" -> OptimizationManager.PerformanceMode.STANDBY
            else -> OptimizationManager.PerformanceMode.DAILY
        }
        if (OptimizationManager.applyAllOptimizations(mode)) {
            StatsRepository.recordCpuBinding()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.i(TAG, "Service destroying")
        optimizationJob?.cancel()
        serviceScope.cancel()
        isRunning = false
        runBlocking {
            // 更新服务停止状态
            SettingsRepository.setServiceRunning(false)
            LogRepository.appendLog(LogLevel.INFO, TAG, "DeepSleep service destroyed")
        }
        super.onDestroy()
    }
}