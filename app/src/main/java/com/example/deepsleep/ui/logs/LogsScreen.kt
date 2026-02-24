package com.example.deepsleep.ui.logs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.deepsleep.model.LogEntry
import com.example.deepsleep.model.LogLevel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: LogsViewModel = viewModel()
    val filteredLogs by viewModel.filteredLogs.collectAsState()
    val selectedLevel by viewModel.selectedLevel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("日志") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                val result = viewModel.exportLogs(context)
                                snackbarHostState.showSnackbar(result)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "导出")
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                viewModel.clearLogs()
                                snackbarHostState.showSnackbar("日志已清除")
                            }
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "清除")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 日志级别筛选
            LevelFilter(
                selectedLevel = selectedLevel,
                onLevelSelected = { viewModel.setLevelFilter(it) }
            )

            // 日志列表
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无日志",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredLogs) { log ->
                        LogItem(log = log)
                    }
                }
            }
        }
    }
}

@Composable
fun LevelFilter(
    selectedLevel: LogLevel?,
    onLevelSelected: (LogLevel?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedLevel == null,
            onClick = { onLevelSelected(null) },
            label = { Text("全部") }
        )
        FilterChip(
            selected = selectedLevel == LogLevel.DEBUG,
            onClick = { onLevelSelected(LogLevel.DEBUG) },
            label = { Text("调试") }
        )
        FilterChip(
            selected = selectedLevel == LogLevel.INFO,
            onClick = { onLevelSelected(LogLevel.INFO) },
            label = { Text("信息") }
        )
        FilterChip(
            selected = selectedLevel == LogLevel.SUCCESS,
            onClick = { onLevelSelected(LogLevel.SUCCESS) },
            label = { Text("成功") }
        )
        FilterChip(
            selected = selectedLevel == LogLevel.WARNING,
            onClick = { onLevelSelected(LogLevel.WARNING) },
            label = { Text("警告") }
        )
        FilterChip(
            selected = selectedLevel == LogLevel.ERROR,
            onClick = { onLevelSelected(LogLevel.ERROR) },
            label = { Text("错误") }
        )
        FilterChip(
            selected = selectedLevel == LogLevel.FATAL,
            onClick = { onLevelSelected(LogLevel.FATAL) },
            label = { Text("致命") }
        )
    }
}

@Composable
fun LogItem(log: LogEntry) {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val levelColor = getLogLevelColor(log.level)
    val levelName = when (log.level) {
        LogLevel.DEBUG -> "调试"
        LogLevel.INFO -> "信息"
        LogLevel.SUCCESS -> "成功"
        LogLevel.WARNING -> "警告"
        LogLevel.ERROR -> "错误"
        LogLevel.FATAL -> "致命"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = levelColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = dateFormat.format(Date(log.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = levelColor,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = levelName,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }

                if (log.tag.isNotEmpty()) {
                    Text(
                        text = log.tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (log.message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (log.throwable != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.throwable,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun getLogLevelColor(level: LogLevel): Color {
    return when (level) {
        LogLevel.DEBUG -> Color.Gray
        LogLevel.INFO -> Color(0xFF2196F3)
        LogLevel.SUCCESS -> Color(0xFF4CAF50)
        LogLevel.WARNING -> Color(0xFFFF9800)
        LogLevel.ERROR -> Color(0xFFF44336)
        LogLevel.FATAL -> Color(0xFF9C27B0)
    }
}