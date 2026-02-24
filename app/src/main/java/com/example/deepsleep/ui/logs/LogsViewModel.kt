package com.example.deepsleep.ui.logs

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deepsleep.data.LogRepository
import com.example.deepsleep.model.LogEntry
import com.example.deepsleep.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted

class LogsViewModel : ViewModel() {

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _selectedLevel = MutableStateFlow<LogLevel?>(null)
    val selectedLevel: StateFlow<LogLevel?> = _selectedLevel.asStateFlow()

    // 使用 stateIn 提供初始值，避免 Compose 中 collectAsState 需要初始值的问题
    val filteredLogs: StateFlow<List<LogEntry>> = combine(
        _logs,
        _selectedLevel
    ) { logs, level ->
        if (level == null) logs else logs.filter { it.level == level }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        refreshLogs()
    }

    fun refreshLogs() {
        viewModelScope.launch {
            _logs.value = try {
                LogRepository.readLogs().sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun setLevelFilter(level: LogLevel?) {
        _selectedLevel.value = level
    }

    fun clearLogs() {
        viewModelScope.launch {
            try {
                LogRepository.clearLogs()
                refreshLogs()
            } catch (e: Exception) {
                // 忽略错误
            }
        }
    }

    suspend fun exportLogs(context: Context): String {
        return try {
            val uri = LogRepository.createShareableFile(context)
            if (uri != null) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "分享日志"))
                "日志已导出"
            } else {
                "导出失败"
            }
        } catch (e: Exception) {
            "导出失败: ${e.message}"
        }
    }
}