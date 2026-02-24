package com.example.deepsleep.data

import android.content.Context
import com.example.deepsleep.root.*

class UninstallManager(private val context: Context) {

    suspend fun performUninstall(): UninstallResult {
        return try {
            stopService()
            DozeController.enableMotion()
            DeepSleepController.exitDeepSleep()          // 已添加导入
            BackgroundOptimizer.restoreAll()
            // WaltOptimizer.restoreDefault()            // 已移除
            cleanupProperties()
            cleanupFiles()
            UninstallResult.Success
        } catch (e: Exception) {
            UninstallResult.Error(e.message ?: "未知错误")
        }
    }

    private suspend fun stopService() {
        RootCommander.exec("am stopservice -n ${context.packageName}/.service.DeepSleepService")
    }

    private suspend fun cleanupProperties() {
        RootCommander.exec(
            "setprop persist.sys.doze.quick '' 2>/dev/null || true",
            "setprop debug.performance.tuning '' 2>/dev/null || true",
            "setprop persist.deep_sleep.opt_bg '' 2>/dev/null || true"
        )
    }

    private suspend fun cleanupFiles() {
        RootCommander.exec(
            "rm -rf /data/local/tmp/deep_sleep_logs",
            "rm -f /data/local/tmp/deep_sleep.pid"
        )
    }
}

sealed class UninstallResult {
    object Success : UninstallResult()
    data class Error(val message: String) : UninstallResult()
}