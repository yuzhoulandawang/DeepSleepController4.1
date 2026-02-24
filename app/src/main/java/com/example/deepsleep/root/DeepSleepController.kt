package com.example.deepsleep.root

import android.util.Log
import kotlinx.coroutines.*

object DeepSleepController {
    private const val TAG = "DeepSleepController"
    private var isInDeepSleep = false
    private var checkJob: Job? = null

    suspend fun enterDeepSleep(blockExit: Boolean, checkIntervalSeconds: Int): Boolean {
        return try {
            val dozeSuccess = DozeController.forceIdle()
            if (!dozeSuccess) {
                Log.e(TAG, "Doze 进入失败")
                return false
            }

            if (blockExit) {
                RootCommander.exec("settings put global motion_detection_enabled 0")
                RootCommander.exec("echo 0 > /sys/power/autosleep")
            }

            RootCommander.exec("echo 'active' > /data/local/tmp/deep_sleep_status")
            isInDeepSleep = true

            if (blockExit) {
                startStatusCheck(checkIntervalSeconds)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "进入深度睡眠失败", e)
            false
        }
    }

    suspend fun exitDeepSleep(): Boolean {
        return try {
            checkJob?.cancel()
            checkJob = null
            RootCommander.exec("settings put global motion_detection_enabled 1")
            RootCommander.exec("echo 1 > /sys/power/autosleep")
            RootCommander.exec("rm -f /data/local/tmp/deep_sleep_status")
            DozeController.step()
            isInDeepSleep = false
            true
        } catch (e: Exception) {
            Log.e(TAG, "退出深度睡眠失败", e)
            false
        }
    }

    fun isInDeepSleep(): Boolean = isInDeepSleep

    private fun startStatusCheck(intervalSeconds: Int) {
        checkJob?.cancel()
        checkJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(intervalSeconds * 1000L)
                val isIdle = checkDozeStatus()
                if (!isIdle) {
                    Log.w(TAG, "检测到意外退出，正在重新进入...")
                    enterDeepSleep(blockExit = true, checkIntervalSeconds = intervalSeconds)
                }
            }
        }
    }

    private suspend fun checkDozeStatus(): Boolean {
        return DozeController.getState() == DozeState.IDLE
    }
}