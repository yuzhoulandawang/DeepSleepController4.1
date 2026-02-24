package com.example.deepsleep.root

import android.util.Log
import com.example.deepsleep.data.LogRepository
import com.example.deepsleep.model.LogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ProcessSuppressor {
    private const val TAG = "ProcessSuppressor"

    suspend fun suppressBackgroundApps(score: Int): Int = withContext(Dispatchers.IO) {
        try {
            val suppressedCount = mutableListOf<String>()

            val procDirsResult = RootCommander.exec("ls /proc | grep -E '^[0-9]+$'")
            val pids = procDirsResult.out.mapNotNull { it.toIntOrNull() }

            pids.forEach { pid ->
                val cmdlineResult = RootCommander.exec("cat /proc/$pid/cmdline 2>/dev/null")
                val cmdline = cmdlineResult.out.firstOrNull()?.trim()?.substringBefore(" ") ?: return@forEach

                if (cmdline.isNotBlank() &&
                    !cmdline.startsWith("com.android") &&
                    !cmdline.contains("deepsleep") &&
                    !cmdline.contains("systemui")) {

                    val oomResult = RootCommander.exec("cat /proc/$pid/oom_score_adj 2>/dev/null")
                    val currentScore = oomResult.out.firstOrNull()?.toIntOrNull() ?: 0
                    if (currentScore > score) {
                        val success = RootCommander.safeWrite("/proc/$pid/oom_score_adj", score.toString())
                        if (success) {
                            suppressedCount.add(cmdline)
                        }
                    }
                }
            }

            LogRepository.appendLog(LogLevel.INFO, TAG, "Suppressed ${suppressedCount.size} background apps with OOM score $score")
            return@withContext suppressedCount.size
        } catch (e: Exception) {
            Log.e(TAG, "Failed to suppress background apps", e)
            LogRepository.appendLog(LogLevel.ERROR, TAG, "Failed to suppress background apps: ${e.message}")
            return@withContext 0
        }
    }
}