package com.example.deepsleep.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.deepsleep.model.LogEntry
import com.example.deepsleep.model.LogLevel
import com.example.deepsleep.root.RootCommander
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogRepository {

    private const val TAG = "LogRepository"
    private val logDir = "/data/local/tmp/deep_sleep_logs"
    private val logPath = "$logDir/main.log"
    private val mutex = Mutex()

    suspend fun debug(tag: String, message: String) {
        appendLog(LogLevel.DEBUG, tag, message)
    }

    suspend fun info(tag: String, message: String) {
        appendLog(LogLevel.INFO, tag, message)
    }

    suspend fun success(tag: String, message: String) {
        appendLog(LogLevel.SUCCESS, tag, message)
    }

    suspend fun warn(tag: String, message: String) {
        appendLog(LogLevel.WARNING, tag, message)
    }

    suspend fun error(tag: String, message: String, throwable: Throwable? = null) {
        appendLog(LogLevel.ERROR, tag, message, throwable?.stackTraceToString())
    }

    suspend fun fatal(tag: String, message: String, throwable: Throwable? = null) {
        appendLog(LogLevel.FATAL, tag, message, throwable?.stackTraceToString())
    }

    suspend fun readLogs(): List<LogEntry> = withContext(Dispatchers.IO) {
        try {
            val content = RootCommander.readFile(logPath) ?: return@withContext emptyList()
            content.lineSequence()
                .filter { it.isNotBlank() }
                .mapNotNull { line -> parseLogLine(line) }
                .toList()
        } catch (e: Exception) {
            Log.e(TAG, "读取日志失败", e)
            emptyList()
        }
    }

    private fun parseLogLine(line: String): LogEntry? {
        return try {
            val regex = """\[(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})\]\s+\[(\w+)\]\s+(?:\[([^\]]+)\]\s+)?(.*)""".toRegex()
            val match = regex.find(line)
            if (match != null) {
                val level = try {
                    LogLevel.valueOf(match.groupValues[2].uppercase())
                } catch (e: Exception) {
                    LogLevel.INFO
                }
                LogEntry(
                    timestamp = parseTimestamp(match.groupValues[1]),
                    level = level,
                    tag = match.groupValues[3].ifEmpty { "General" },
                    message = match.groupValues[4]
                )
            } else {
                val oldRegex = """\[(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})\]\s+(.*)""".toRegex()
                val oldMatch = oldRegex.find(line)
                if (oldMatch != null) {
                    LogEntry(
                        timestamp = parseTimestamp(oldMatch.groupValues[1]),
                        level = LogLevel.INFO,
                        tag = "",
                        message = oldMatch.groupValues[2]
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析日志行失败: $line", e)
            null
        }
    }

    private fun parseTimestamp(timestamp: String): Long {
        return try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(timestamp)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    suspend fun appendLog(level: LogLevel, tag: String, message: String, throwable: String? = null) = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val tagStr = if (tag.isNotEmpty()) "[$tag]" else ""
                val throwableStr = if (throwable != null) "\n$throwable" else ""
                val logLine = "[$timestamp] [$level] $tagStr $message$throwableStr"
                RootCommander.mkdir(logDir)
                RootCommander.exec("printf '%s\\n' \"$logLine\" >> $logPath")
                rotateLogsIfNeeded()
            } catch (e: Exception) {
                Log.e(TAG, "写入日志失败", e)
            }
        }
    }

    suspend fun clearLogs(): Boolean = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                RootCommander.exec("echo '' > $logPath").isSuccess
            } catch (e: Exception) {
                Log.e(TAG, "清除日志失败", e)
                false
            }
        }
    }

    suspend fun getLogSize(): String {
        return try {
            val result = RootCommander.exec("wc -c $logPath 2>/dev/null")
            val bytes = result.out.firstOrNull()?.trim()?.split(" ")?.firstOrNull()?.toLongOrNull() ?: 0L
            when {
                bytes > 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
                bytes > 1024 -> String.format("%.2f KB", bytes / 1024.0)
                else -> "$bytes B"
            }
        } catch (e: Exception) {
            "0 B"
        }
    }

    suspend fun createShareableFile(context: Context): Uri? = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "logs")
            cacheDir.mkdirs()
            val destFile = File(cacheDir, "deep_sleep_logs_${System.currentTimeMillis()}.txt")
            RootCommander.exec("cp $logPath ${destFile.absolutePath} 2>/dev/null")
            RootCommander.exec("chmod 644 ${destFile.absolutePath} 2>/dev/null")
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)
        } catch (e: Exception) {
            Log.e(TAG, "创建分享文件失败", e)
            null
        }
    }

    private suspend fun rotateLogsIfNeeded() {
        try {
            if (!RootCommander.fileExists(logPath)) return
            val sizeResult = RootCommander.exec("wc -c $logPath")
            val bytes = sizeResult.out.firstOrNull()?.trim()?.split(" ")?.firstOrNull()?.toLongOrNull() ?: 0L
            if (bytes > 2 * 1024 * 1024) {
                RootCommander.exec("sed -i '1,1000d' $logPath")
            }
        } catch (e: Exception) {
            Log.e(TAG, "日志轮转失败", e)
        }
    }
}