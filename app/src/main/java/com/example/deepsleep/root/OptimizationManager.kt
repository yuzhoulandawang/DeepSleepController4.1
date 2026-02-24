package com.example.deepsleep.root

import android.util.Log
import com.example.deepsleep.data.LogRepository
import com.example.deepsleep.model.LogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object OptimizationManager {

    private const val TAG = "OptimizationManager"

    enum class PerformanceMode {
        PERFORMANCE, DAILY, STANDBY
    }

    private suspend fun writeToFile(path: String, value: Any): Boolean {
        return RootCommander.safeWrite(path, value.toString())
    }

    private suspend fun fileExists(path: String): Boolean = RootCommander.fileExists(path)

    private suspend fun writeToFileIfExists(path: String, value: Any): Boolean {
        return if (fileExists(path)) {
            writeToFile(path, value)
        } else {
            Log.d(TAG, "File not exists, skip: $path")
            true
        }
    }

    private suspend fun applyCpuSet(mode: PerformanceMode): Boolean {
        val cpusetBase = "/dev/cpuset"
        if (!fileExists(cpusetBase)) {
            Log.w(TAG, "cpuset directory not found")
            return false
        }

        val (topApp, foreground, background, systemBackground) = when (mode) {
            PerformanceMode.PERFORMANCE -> arrayOf("0-7", "2-7", "0-1", "0-1")
            PerformanceMode.DAILY -> arrayOf("0-7", "0-6", "0-1", "0-3")
            PerformanceMode.STANDBY -> arrayOf("0-3", "0-1", "0-1", "0-1")
        }

        var allSuccess = true
        allSuccess = allSuccess && writeToFile("$cpusetBase/top-app/cpus", topApp)
        allSuccess = allSuccess && writeToFile("$cpusetBase/foreground/cpus", foreground)
        allSuccess = allSuccess && writeToFile("$cpusetBase/background/cpus", background)
        allSuccess = allSuccess && writeToFile("$cpusetBase/system-background/cpus", systemBackground)

        val cpuExclusive = if (mode == PerformanceMode.PERFORMANCE || mode == PerformanceMode.DAILY) "1" else "0"
        writeToFile("$cpusetBase/background/cpu_exclusive", cpuExclusive)
        writeToFile("$cpusetBase/background/sched_load_balance", "0")
        writeToFile("$cpusetBase/system-background/sched_load_balance", "0")
        writeToFile("$cpusetBase/top-app/sched_load_balance", "1")

        return allSuccess
    }

    private suspend fun getAvailableGpuFreqs(): List<Long> {
        val result = RootCommander.exec("cat /sys/class/kgsl/kgsl-3d0/gpu_available_frequencies")
        return result.out.firstOrNull()?.trim()?.split(" ")?.mapNotNull { it.toLongOrNull() }
            ?: listOf(903000000, 834000000, 770000000, 720000000, 680000000,
                629000000, 578000000, 500000000, 422000000, 310000000, 231000000)
    }

    private fun selectClosestFreq(target: Long, available: List<Long>): Long {
        return available.minByOrNull { kotlin.math.abs(it - target) } ?: target
    }

    private suspend fun applyGpuOptimization(mode: PerformanceMode): Boolean {
        val gpuBase = "/sys/class/kgsl/kgsl-3d0"
        if (!fileExists(gpuBase)) {
            Log.w(TAG, "GPU directory not found")
            return false
        }

        val availableFreqs = getAvailableGpuFreqs()
        if (availableFreqs.isEmpty()) {
            Log.e(TAG, "No available GPU frequencies")
            return false
        }

        val (targetMax, targetMin) = when (mode) {
            PerformanceMode.PERFORMANCE -> Pair(903000000L, 578000000L)
            PerformanceMode.DAILY -> Pair(770000000L, 310000000L)
            PerformanceMode.STANDBY -> Pair(500000000L, 231000000L)
        }

        val maxFreq = selectClosestFreq(targetMax, availableFreqs)
        val minFreq = selectClosestFreq(targetMin, availableFreqs)

        var allSuccess = true

        val params = when (mode) {
            PerformanceMode.PERFORMANCE -> arrayOf(0, 0, 1, 1, 1, 1, 10, 0)
            PerformanceMode.DAILY -> arrayOf(1, 1, 0, 0, 0, 0, 50, 5)
            PerformanceMode.STANDBY -> arrayOf(1, 1, 0, 0, 0, 0, 100, 8)
        }
        val throttling = params[0]
        val busSplit = params[1]
        val forceClkOn = params[2]
        val forceRailOn = params[3]
        val forceNoNap = params[4]
        val forceBusOn = params[5]
        val idleTimer = params[6]
        val thermalPwrlevel = params[7]

        allSuccess = allSuccess and writeToFileIfExists("$gpuBase/throttling", throttling)
        allSuccess = allSuccess and writeToFileIfExists("$gpuBase/bus_split", busSplit)
        allSuccess = allSuccess and writeToFileIfExists("$gpuBase/force_clk_on", forceClkOn)
        allSuccess = allSuccess and writeToFileIfExists("$gpuBase/force_rail_on", forceRailOn)
        allSuccess = allSuccess and writeToFileIfExists("$gpuBase/force_no_nap", forceNoNap)
        allSuccess = allSuccess and writeToFileIfExists("$gpuBase/force_bus_on", forceBusOn)
        allSuccess = allSuccess and writeToFileIfExists("$gpuBase/idle_timer", idleTimer)
        allSuccess = allSuccess and writeToFileIfExists("$gpuBase/max_gpuclk", maxFreq)
        allSuccess = allSuccess and writeToFileIfExists("$gpuBase/thermal_pwrlevel", thermalPwrlevel)

        val devfreqBase = "/sys/class/devfreq"
        File(devfreqBase).listFiles { file -> file.isDirectory && file.name.contains("kgsl") }?.forEach { dir ->
            allSuccess = allSuccess and writeToFileIfExists("${dir.absolutePath}/min_freq", minFreq)
            allSuccess = allSuccess and writeToFileIfExists("${dir.absolutePath}/max_freq", maxFreq)
        }

        return allSuccess
    }

    suspend fun applyAllOptimizations(mode: PerformanceMode): Boolean {
        Log.i(TAG, "Applying optimizations for mode: $mode")

        val cpuSuccess = applyCpuSet(mode)
        val gpuSuccess = applyGpuOptimization(mode)

        val success = cpuSuccess && gpuSuccess

        Log.i(TAG, "Optimizations applied: cpu=$cpuSuccess, gpu=$gpuSuccess, overall=$success")

        LogRepository.appendLog(
            if (success) LogLevel.INFO else LogLevel.WARNING,
            TAG,
            "Optimizations applied for mode: $mode (cpu=$cpuSuccess, gpu=$gpuSuccess)"
        )

        return success
    }
}