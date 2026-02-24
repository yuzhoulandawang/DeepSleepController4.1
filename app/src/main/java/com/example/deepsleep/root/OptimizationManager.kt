package com.example.deepsleep.root

import android.util.Log
import com.example.deepsleep.data.LogRepository
import com.example.deepsleep.model.LogLevel
import com.example.deepsleep.model.PerformanceMode
import com.example.deepsleep.model.PerformanceProfile

object OptimizationManager {
    private const val TAG = "OptimizationManager"

    suspend fun applyPerformanceProfile(profile: PerformanceProfile, mode: PerformanceMode) {
        Log.i(TAG, "Applying performance profile for mode: $mode")

        WaltOptimizer.applyCpuParams(profile.cpu)
        GpuOptimizer.applyGpuParams(profile.gpu)
        applyCpuSet(mode)

        LogRepository.appendLog(LogLevel.INFO, TAG, "Applied performance profile for mode: $mode")
    }

    private suspend fun applyCpuSet(mode: PerformanceMode): Boolean {
        val cpusetBase = "/dev/cpuset"
        if (!RootCommander.fileExists(cpusetBase)) {
            Log.w(TAG, "cpuset directory not found")
            return false
        }

        val (topApp, foreground, background, systemBackground) = when (mode) {
            PerformanceMode.ECO -> arrayOf("0-3", "0-1", "0-1", "0-1")
            PerformanceMode.DAILY -> arrayOf("0-7", "0-6", "0-1", "0-3")
            PerformanceMode.PERFORMANCE -> arrayOf("0-7", "2-7", "0-1", "0-1")
        }

        var allSuccess = true
        allSuccess = allSuccess && RootCommander.safeWrite("$cpusetBase/top-app/cpus", topApp)
        allSuccess = allSuccess && RootCommander.safeWrite("$cpusetBase/foreground/cpus", foreground)
        allSuccess = allSuccess && RootCommander.safeWrite("$cpusetBase/background/cpus", background)
        allSuccess = allSuccess && RootCommander.safeWrite("$cpusetBase/system-background/cpus", systemBackground)

        RootCommander.safeWrite("$cpusetBase/background/cpu_exclusive", "1")
        RootCommander.safeWrite("$cpusetBase/background/sched_load_balance", "0")
        RootCommander.safeWrite("$cpusetBase/system-background/sched_load_balance", "0")
        RootCommander.safeWrite("$cpusetBase/top-app/sched_load_balance", "1")

        return allSuccess
    }
}