package com.example.deepsleep.root

import android.util.Log
import com.example.deepsleep.BuildConfig
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RootCommander {
    private const val TAG = "RootCommander"

    init {
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(30)
        )
    }

    suspend fun requestRootAccess(): Boolean = withContext(Dispatchers.IO) {
        try {
            Shell.getShell()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun checkRoot(): Boolean = withContext(Dispatchers.IO) {
        Shell.cmd("id").exec().out.any { it.contains("uid=0") }
    }

    suspend fun exec(command: String): Shell.Result = withContext(Dispatchers.IO) {
        Shell.cmd(command).exec()
    }

    suspend fun exec(vararg commands: String): Shell.Result = withContext(Dispatchers.IO) {
        Shell.cmd(*commands).exec()
    }

    suspend fun execBatch(commands: List<String>): Shell.Result = withContext(Dispatchers.IO) {
        Shell.cmd(*commands.toTypedArray()).exec()
    }

    suspend fun safeWrite(path: String, value: String): Boolean = withContext(Dispatchers.IO) {
        Shell.cmd("printf '%s' \"$value\" > $path").exec().isSuccess
    }

    suspend fun readFile(path: String): String? = withContext(Dispatchers.IO) {
        val result = Shell.cmd("cat $path 2>/dev/null").exec()
        if (result.isSuccess) result.out.joinToString("\n") else null
    }

    suspend fun fileExists(path: String): Boolean = withContext(Dispatchers.IO) {
        Shell.cmd("[ -f $path ]").exec().isSuccess
    }

    suspend fun mkdir(path: String): Boolean = withContext(Dispatchers.IO) {
        Shell.cmd("mkdir -p $path").exec().isSuccess
    }
}