package com.example.deepsleep.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.deepsleep.ui.main.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SceneCheckScreen(
    onNavigateBack: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("场景检测配置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "选择需要阻止深度睡眠的场景",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    SwitchItem(
                        title = "流量活跃",
                        subtitle = "有活跃流量时阻止深度睡眠",
                        checked = settings.checkNetworkTraffic,
                        onCheckedChange = { viewModel.setCheckNetworkTraffic(it) }
                    )
                    SwitchItem(
                        title = "音频播放",
                        subtitle = "有音频播放时阻止深度睡眠",
                        checked = settings.checkAudioPlayback,
                        onCheckedChange = { viewModel.setCheckAudioPlayback(it) }
                    )
                    SwitchItem(
                        title = "导航应用",
                        subtitle = "导航应用运行时阻止深度睡眠",
                        checked = settings.checkNavigation,
                        onCheckedChange = { viewModel.setCheckNavigation(it) }
                    )
                    SwitchItem(
                        title = "通话状态",
                        subtitle = "通话中阻止深度睡眠",
                        checked = settings.checkPhoneCall,
                        onCheckedChange = { viewModel.setCheckPhoneCall(it) }
                    )
                    SwitchItem(
                        title = "NFC/P2P",
                        subtitle = "NFC 传输中阻止深度睡眠",
                        checked = settings.checkNfcP2p,
                        onCheckedChange = { viewModel.setCheckNfcP2p(it) }
                    )
                    SwitchItem(
                        title = "WiFi 热点",
                        subtitle = "热点开启时阻止深度睡眠",
                        checked = settings.checkWifiHotspot,
                        onCheckedChange = { viewModel.setCheckWifiHotspot(it) }
                    )
                    SwitchItem(
                        title = "USB 网络共享",
                        subtitle = "USB 共享时阻止深度睡眠",
                        checked = settings.checkUsbTethering,
                        onCheckedChange = { viewModel.setCheckUsbTethering(it) }
                    )
                    SwitchItem(
                        title = "投屏",
                        subtitle = "投屏中阻止深度睡眠",
                        checked = settings.checkScreenCasting,
                        onCheckedChange = { viewModel.setCheckScreenCasting(it) }
                    )
                    SwitchItem(
                        title = "充电状态",
                        subtitle = "充电时阻止深度睡眠",
                        checked = settings.checkCharging,
                        onCheckedChange = { viewModel.setCheckCharging(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun SwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}