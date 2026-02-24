package com.example.deepsleep.root

import android.util.Log
import com.example.deepsleep.model.CpuParams

object WaltOptimizer {
    private const val TAG = "WaltOptimizer"

    suspend fun applyCpuParams(params: CpuParams): Boolean {
        Log.d(TAG, "Applying CPU params: upRate=${params.upRate}, downRate=${params.downRate}, hispeedLoad=${params.hispeedLoad}, targetLoads=${params.targetLoads}")

        val policiesResult = RootCommander.exec("ls -d /sys/devices/system/cpu/cpufreq/policy* 2>/dev/null")
        if (!policiesResult.isSuccess) {
            Log.w(TAG, "No CPU policy directories found")
            return false
        }

        val policies = policiesResult.out.joinToString("\n").trim().split("\n").filter { it.isNotEmpty() }
        var anyApplied = false

        for (policy in policies) {
            val waltDir = "$policy/walt"
            val dirExists = RootCommander.exec("test -d $waltDir").isSuccess
            if (!dirExists) {
                Log.d(TAG, "WALT dir not found: $waltDir")
                continue
            }

            if (RootCommander.safeWrite("$waltDir/up_rate_limit_us", params.upRate.toString())) anyApplied = true
            if (RootCommander.safeWrite("$waltDir/down_rate_limit_us", params.downRate.toString())) anyApplied = true
            if (RootCommander.safeWrite("$waltDir/hispeed_load", params.hispeedLoad.toString())) anyApplied = true
            if (RootCommander.safeWrite("$waltDir/target_loads", params.targetLoads.toString())) anyApplied = true
        }

        return anyApplied
    }

    // 可保留原有的 applyMode 方法，但内部调用 applyCpuParams
}