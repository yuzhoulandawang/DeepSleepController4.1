package com.example.deepsleep.root

import android.util.Log
import com.example.deepsleep.data.LogRepository
import com.example.deepsleep.model.LogLevel
import com.example.deepsleep.model.PerformanceMode
import com.example.deepsleep.model.PerformanceProfile

object OptimizationManager {
    private const val TAG = "OptimizationManager"

    // 应用性能配置（包括 CPU/GPU 参数和绑核）
    suspend fun applyPerformanceProfile(profile: PerformanceProfile, mode: PerformanceMode) {
        Log.i(TAG, "Applying performance profile for mode: $mode")

        // 1. 应用 CPU 参数
        WaltOptimizer.applyCpuParams(profile.cpu)

        // 2. 应用 GPU 参数
        GpuOptimizer.applyGpuParams(profile.gpu)

        // 3. 应用绑核策略（由模式决定）
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

        // 可选设置：cpu_exclusive 等
        RootCommander.safeWrite("$cpusetBase/background/cpu_exclusive", "1")
        RootCommander.safeWrite("$cpusetBase/background/sched_load_balance", "0")
        RootCommander.safeWrite("$cpusetBase/system-background/sched_load_balance", "0")
        RootCommander.safeWrite("$cpusetBase/top-app/sched_load_balance", "1")

        return allSuccess
    }
}