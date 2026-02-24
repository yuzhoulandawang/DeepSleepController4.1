package com.example.deepsleep.root

import android.util.Log
import com.example.deepsleep.data.LogRepository
import com.example.deepsleep.model.GpuParams
import com.example.deepsleep.model.LogLevel

object GpuOptimizer {
    private const val TAG = "GpuOptimizer"
    private const val KGSL_BASE = "/sys/class/kgsl/kgsl-3d0"

    suspend fun applyGpuParams(params: GpuParams): Boolean {
        Log.d(TAG, "Applying GPU params: maxFreq=${params.maxFreq}, minFreq=${params.minFreq}, idleTimer=${params.idleTimer}")

        var allSuccess = true

        allSuccess = allSuccess && RootCommander.safeWrite("$KGSL_BASE/throttling", if (params.throttlingEnabled) "1" else "0")
        allSuccess = allSuccess && RootCommander.safeWrite("$KGSL_BASE/bus_split", if (params.busSplitEnabled) "1" else "0")
        allSuccess = allSuccess && RootCommander.safeWrite("$KGSL_BASE/idle_timer", params.idleTimer.toString())
        allSuccess = allSuccess && RootCommander.safeWrite("$KGSL_BASE/max_gpuclk", params.maxFreq.toString())
        allSuccess = allSuccess && RootCommander.safeWrite("$KGSL_BASE/thermal_pwrlevel", params.thermalPwrLevel.toString())

        // 动态查找 devfreq 目录设置 min/max freq
        val devfreqBase = "/sys/class/devfreq"
        val kgslDirs = RootCommander.exec("ls $devfreqBase | grep kgsl").out
        kgslDirs.forEach { dir ->
            allSuccess = allSuccess && RootCommander.safeWrite("$devfreqBase/$dir/min_freq", params.minFreq.toString())
            allSuccess = allSuccess && RootCommander.safeWrite("$devfreqBase/$dir/max_freq", params.maxFreq.toString())
        }

        // 温度设置
        val gpuZone = findGpuThermalZone()
        if (gpuZone != null) {
            allSuccess = allSuccess && RootCommander.safeWrite("/sys/class/thermal/thermal_zone$gpuZone/trip_point_0_temp", params.tripPointTemp.toString())
            allSuccess = allSuccess && RootCommander.safeWrite("/sys/class/thermal/thermal_zone$gpuZone/trip_point_0_hyst", params.tripPointHyst.toString())
        }

        LogRepository.appendLog(
            if (allSuccess) LogLevel.INFO else LogLevel.ERROR,
            TAG,
            "GPU parameters applied (success=$allSuccess)"
        )
        return allSuccess
    }

    private suspend fun findGpuThermalZone(): Int? {
        for (i in 0..50) {
            val type = RootCommander.exec("cat /sys/class/thermal/thermal_zone$i/type 2>/dev/null").out.joinToString("").trim()
            if (type.contains("gpu", ignoreCase = true)) {
                return i
            }
        }
        return null
    }

    // 保留原有模式方法...
}