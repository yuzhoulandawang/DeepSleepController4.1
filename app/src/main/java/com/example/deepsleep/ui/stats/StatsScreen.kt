package com.example.deepsleep.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.deepsleep.model.Statistics
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatsViewModel = viewModel()
) {
    val statistics by viewModel.statistics.collectAsState()

    // 自动刷新：每秒更新一次数据
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refreshStatistics()
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("统计数据") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 总体概览卡片（移除节省电量和释放内存）
            item {
                StatsCard(title = "📊 优化概览") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatRow(
                            label = "总运行时长",
                            value = formatDuration(statistics.totalRuntime)
                        )
                        StatRow(
                            label = "优化次数",
                            value = "${statistics.totalOptimizations}"
                        )
                    }
                }
            }

            // 进程压制统计
            item {
                StatsCard(title = "🔧 进程压制") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatRow(
                            label = "压制应用总数",
                            value = "${statistics.suppressedApps}"
                        )
                        StatRow(
                            label = "释放进程数",
                            value = "${statistics.killedProcesses}"
                        )
                        StatRow(
                            label = "OOM 调整次数",
                            value = "${statistics.oomAdjustments}"
                        )
                        StatRow(
                            label = "平均 OOM 评分",
                            value = "${statistics.avgOomScore}"
                        )
                    }
                }
            }

            // 应用冻结统计
            item {
                StatsCard(title = "❄️ 应用冻结") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatRow(
                            label = "冻结应用总数",
                            value = "${statistics.frozenApps}"
                        )
                        StatRow(
                            label = "解冻应用总数",
                            value = "${statistics.thawedApps}"
                        )
                        StatRow(
                            label = "平均冻结时长",
                            value = formatDuration(statistics.avgFreezeTime)
                        )
                        StatRow(
                            label = "阻止冻结次数",
                            value = "${statistics.preventedFreezes}"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun StatRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// 辅助函数
fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${secs}s"
        else -> "${secs}s"
    }
}