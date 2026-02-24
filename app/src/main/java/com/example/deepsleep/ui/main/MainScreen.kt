package com.example.deepsleep.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.deepsleep.model.AppSettings
import kotlinx.coroutines.launch

/**
 * 主页面（整合所有设置项，无图标，芯片式模式选择，CPU调度优化合并了CPU绑定）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToLogs: () -> Unit,
    onNavigateToWhitelist: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSceneCheck: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DeepSleep 控制器") }
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
            // 状态卡片
            StatusCard(settings, viewModel)

            // 深度睡眠控制
            DeepSleepControlSection(settings, viewModel)

            // 深度 Doze 配置
            SettingsSection(title = "深度 Doze 配置") {
                SwitchItem(
                    title = "启用深度 Doze",
                    subtitle = "息屏后自动进入 Device Idle 模式",
                    checked = settings.deepDozeEnabled,
                    onCheckedChange = { viewModel.setDeepDozeEnabled(it) }
                )
                if (settings.deepDozeEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    NumberInputField(
                        label = "延迟进入时间（秒）",
                        value = settings.deepDozeDelaySeconds.toString(),
                        onValueChange = { newValue ->
                            newValue.toIntOrNull()?.let {
                                scope.launch { viewModel.setDeepDozeDelaySeconds(it) }
                            }
                        },
                        focusManager = focusManager
                    )
                    SwitchItem(
                        title = "强制 Doze 模式",
                        subtitle = "禁用 motion 检测，强制进入 Doze",
                        checked = settings.deepDozeForceMode,
                        onCheckedChange = { viewModel.setDeepDozeForceMode(it) }
                    )
                }
            }

            // 深度睡眠 Hook 版本
            SettingsSection(title = "深度睡眠（Hook 版本）") {
                SwitchItem(
                    title = "启用深度睡眠 Hook",
                    subtitle = "息屏后强制进入深度休眠，屏蔽自动退出",
                    checked = settings.deepSleepHookEnabled,
                    onCheckedChange = { viewModel.setDeepSleepHookEnabled(it) }
                )
                if (settings.deepSleepHookEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    NumberInputField(
                        label = "延迟进入时间（秒）",
                        value = settings.deepSleepDelaySeconds.toString(),
                        onValueChange = { newValue ->
                            newValue.toIntOrNull()?.let {
                                scope.launch { viewModel.setDeepSleepDelaySeconds(it) }
                            }
                        },
                        focusManager = focusManager
                    )
                    SwitchItem(
                        title = "阻止自动退出",
                        subtitle = "屏蔽移动、广播等自动退出条件",
                        checked = settings.deepSleepBlockExit,
                        onCheckedChange = { viewModel.setDeepSleepBlockExit(it) }
                    )
                    NumberInputField(
                        label = "状态检查间隔（秒）",
                        value = settings.deepSleepCheckInterval.toString(),
                        onValueChange = { newValue ->
                            newValue.toIntOrNull()?.let {
                                scope.launch { viewModel.setDeepSleepCheckInterval(it) }
                            }
                        },
                        focusManager = focusManager
                    )
                }
            }

            // 系统省电模式联动
            SettingsSection(title = "系统省电模式") {
                SwitchItem(
                    title = "睡眠时开启省电模式",
                    subtitle = "进入深度睡眠时自动开启系统省电",
                    checked = settings.enablePowerSaverOnSleep,
                    onCheckedChange = { viewModel.setEnablePowerSaverOnSleep(it) }
                )
                SwitchItem(
                    title = "唤醒时关闭省电模式",
                    subtitle = "退出深度睡眠时自动关闭系统省电",
                    checked = settings.disablePowerSaverOnWake,
                    onCheckedChange = { viewModel.setDisablePowerSaverOnWake(it) }
                )
            }

            // 后台优化
            BackgroundOptimizationSection(settings, viewModel)

            // 白名单管理
            WhitelistSection(settings, viewModel, onNavigateToWhitelist)

            // CPU 调度优化（合并了CPU绑定）
            CpuSchedulerSection(settings, viewModel)

            // GPU 优化
            GpuOptimizationSectionChip(settings, viewModel)

            // 电池优化
            BatteryOptimizationSection(settings, viewModel)

            // 进程压制
            ProcessSuppressSection(settings, viewModel, focusManager)

            // Freezer 服务
            FreezerSection(settings, viewModel, focusManager)

            // 场景检测
            SettingsSection(title = "场景检测") {
                SwitchItem(
                    title = "启用场景检测",
                    subtitle = "检测特定场景并调整优化策略",
                    checked = settings.sceneCheckEnabled,
                    onCheckedChange = { viewModel.setSceneCheckEnabled(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ClickableItem(
                    title = "配置检测项",
                    subtitle = "选择要检测的场景类型",
                    onClick = onNavigateToSceneCheck
                )
            }

            // 统计数据入口
            ClickableItem(
                title = "统计数据",
                subtitle = "查看优化效果统计",
                onClick = onNavigateToStats
            )

            Spacer(modifier = Modifier.weight(1f))

            // 日志入口
            ClickableItem(
                title = "日志",
                subtitle = "查看应用运行日志",
                onClick = onNavigateToLogs
            )
        }
    }
}

// ========== 组件 ==========
@Composable
fun StatusCard(settings: AppSettings, viewModel: MainViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (settings.rootGranted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (settings.rootGranted) "Root 权限已获取" else "未获取 Root 权限",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (settings.serviceRunning) "服务运行中" else "服务未运行",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DeepSleepControlSection(settings: AppSettings, viewModel: MainViewModel) {
    SettingsSection(title = "深度睡眠控制") {
        SwitchItem(
            title = "启用深度睡眠控制",
            subtitle = "控制系统进入深度睡眠模式",
            checked = settings.deepSleepEnabled,
            onCheckedChange = { viewModel.setDeepSleepEnabled(it) }
        )
        if (settings.deepSleepEnabled) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SwitchItem(
                title = "抑制唤醒",
                subtitle = "阻止应用唤醒设备",
                checked = settings.wakeupSuppressEnabled,
                onCheckedChange = { viewModel.setWakeupSuppressEnabled(it) }
            )
            SwitchItem(
                title = "抑制闹钟",
                subtitle = "阻止非重要闹钟唤醒",
                checked = settings.alarmSuppressEnabled,
                onCheckedChange = { viewModel.setAlarmSuppressEnabled(it) }
            )
        }
    }
}

@Composable
fun BackgroundOptimizationSection(settings: AppSettings, viewModel: MainViewModel) {
    SettingsSection(title = "后台优化") {
        SwitchItem(
            title = "启用后台优化",
            subtitle = "优化后台应用行为",
            checked = settings.backgroundOptimizationEnabled,
            onCheckedChange = { viewModel.setBackgroundOptimizationEnabled(it) }
        )
        if (settings.backgroundOptimizationEnabled) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SwitchItem(
                title = "应用挂起",
                subtitle = "挂起不活跃的后台应用",
                checked = settings.appSuspendEnabled,
                onCheckedChange = { viewModel.setAppSuspendEnabled(it) }
            )
            SwitchItem(
                title = "后台限制",
                subtitle = "限制后台应用资源使用",
                checked = settings.backgroundRestrictEnabled,
                onCheckedChange = { viewModel.setBackgroundRestrictEnabled(it) }
            )
        }
    }
}

@Composable
fun WhitelistSection(
    settings: AppSettings,
    viewModel: MainViewModel,
    onNavigateToWhitelist: () -> Unit
) {
    SettingsSection(title = "白名单管理") {
        ClickableItem(
            title = "管理白名单",
            subtitle = "选择不受深度睡眠影响的应用",
            onClick = onNavigateToWhitelist
        )
        if (settings.whitelist.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "已添加 ${settings.whitelist.size} 个应用",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BatteryOptimizationSection(settings: AppSettings, viewModel: MainViewModel) {
    SettingsSection(title = "电池优化") {
        SwitchItem(
            title = "启用电池优化",
            subtitle = "优化电池使用效率",
            checked = settings.batteryOptimizationEnabled,
            onCheckedChange = { viewModel.setBatteryOptimizationEnabled(it) }
        )
        if (settings.batteryOptimizationEnabled) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SwitchItem(
                title = "省电模式",
                subtitle = "降低功耗以延长续航",
                checked = settings.powerSavingEnabled,
                onCheckedChange = { viewModel.setPowerSavingEnabled(it) }
            )
        }
    }
}

@Composable
fun CpuSchedulerSection(settings: AppSettings, viewModel: MainViewModel) {
    SettingsSection(title = "CPU 调度优化") {
        SwitchItem(
            title = "启用 CPU 调度优化",
            subtitle = "优化 CPU 调度器并自动应用 CPU 绑定",
            checked = settings.cpuOptimizationEnabled,
            onCheckedChange = { viewModel.setCpuOptimizationEnabled(it) }
        )
        if (settings.cpuOptimizationEnabled) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SwitchItem(
                title = "自动切换 CPU 模式",
                subtitle = "亮屏/息屏时自动切换模式",
                checked = settings.autoSwitchCpuMode,
                onCheckedChange = { viewModel.setAutoSwitchCpuMode(it) }
            )
            if (settings.autoSwitchCpuMode) {
                Text(
                    text = "亮屏模式",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CpuModeChip(
                        mode = "daily",
                        currentMode = settings.cpuModeOnScreen,
                        onClick = { viewModel.setCpuModeOnScreen("daily") }
                    )
                    CpuModeChip(
                        mode = "standby",
                        currentMode = settings.cpuModeOnScreen,
                        onClick = { viewModel.setCpuModeOnScreen("standby") }
                    )
                    CpuModeChip(
                        mode = "performance",
                        currentMode = settings.cpuModeOnScreen,
                        onClick = { viewModel.setCpuModeOnScreen("performance") }
                    )
                }

                Text(
                    text = "息屏模式",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CpuModeChip(
                        mode = "daily",
                        currentMode = settings.cpuModeOnScreenOff,
                        onClick = { viewModel.setCpuModeOnScreenOff("daily") }
                    )
                    CpuModeChip(
                        mode = "standby",
                        currentMode = settings.cpuModeOnScreenOff,
                        onClick = { viewModel.setCpuModeOnScreenOff("standby") }
                    )
                    CpuModeChip(
                        mode = "performance",
                        currentMode = settings.cpuModeOnScreenOff,
                        onClick = { viewModel.setCpuModeOnScreenOff("performance") }
                    )
                }
            } else {
                Text(
                    text = "当前 CPU 模式",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CpuModeChip(
                        mode = "daily",
                        currentMode = settings.cpuMode,
                        onClick = { viewModel.setCpuMode("daily") }
                    )
                    CpuModeChip(
                        mode = "standby",
                        currentMode = settings.cpuMode,
                        onClick = { viewModel.setCpuMode("standby") }
                    )
                    CpuModeChip(
                        mode = "performance",
                        currentMode = settings.cpuMode,
                        onClick = { viewModel.setCpuMode("performance") }
                    )
                }
            }
        }
    }
}

@Composable
fun GpuOptimizationSectionChip(settings: AppSettings, viewModel: MainViewModel) {
    SettingsSection(title = "GPU 优化") {
        SwitchItem(
            title = "启用 GPU 优化",
            subtitle = "优化 GPU 性能和功耗",
            checked = settings.gpuOptimizationEnabled,
            onCheckedChange = { viewModel.setGpuOptimizationEnabled(it) }
        )
        if (settings.gpuOptimizationEnabled) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "GPU 模式",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                GpuModeChip(
                    mode = "default",
                    currentMode = settings.gpuMode,
                    onClick = { viewModel.setGpuMode("default") }
                )
                GpuModeChip(
                    mode = "performance",
                    currentMode = settings.gpuMode,
                    onClick = { viewModel.setGpuMode("performance") }
                )
                GpuModeChip(
                    mode = "power_saving",
                    currentMode = settings.gpuMode,
                    onClick = { viewModel.setGpuMode("power_saving") }
                )
            }
        }
    }
}

@Composable
fun ProcessSuppressSection(
    settings: AppSettings,
    viewModel: MainViewModel,
    focusManager: FocusManager
) {
    var scoreText by remember { mutableStateOf(settings.suppressScore.toString()) }
    val scope = rememberCoroutineScope()

    SettingsSection(title = "进程压制") {
        SwitchItem(
            title = "启用进程压制",
            subtitle = "调整后台进程 OOM 评分",
            checked = settings.processSuppressEnabled,
            onCheckedChange = { viewModel.setProcessSuppressEnabled(it) }
        )
        if (settings.processSuppressEnabled) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            NumberInputField(
                label = "压制评分（-1000 到 1000）",
                value = scoreText,
                onValueChange = { newValue ->
                    scoreText = newValue
                    newValue.toIntOrNull()?.let {
                        scope.launch { viewModel.setSuppressScore(it) }
                    }
                },
                focusManager = focusManager
            )
        }
    }
}

@Composable
fun FreezerSection(
    settings: AppSettings,
    viewModel: MainViewModel,
    focusManager: FocusManager
) {
    var delayText by remember { mutableStateOf(settings.freezeDelay.toString()) }
    val scope = rememberCoroutineScope()

    SettingsSection(title = "Freezer 服务") {
        SwitchItem(
            title = "启用 Freezer",
            subtitle = "冻结不活跃的后台进程",
            checked = settings.freezerEnabled,
            onCheckedChange = { viewModel.setFreezerEnabled(it) }
        )
        if (settings.freezerEnabled) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            NumberInputField(
                label = "冻结延迟（秒）",
                value = delayText,
                onValueChange = { newValue ->
                    delayText = newValue
                    newValue.toIntOrNull()?.let {
                        scope.launch { viewModel.setFreezeDelay(it) }
                    }
                },
                focusManager = focusManager
            )
        }
    }
}

// ========== 通用组件 ==========
@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
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

@Composable
fun ClickableItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NumberInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    focusManager: FocusManager
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            singleLine = true
        )
    }
}

@Composable
fun CpuModeChip(
    mode: String,
    currentMode: String,
    onClick: () -> Unit
) {
    val isSelected = mode == currentMode
    val modeName = when (mode) {
        "daily" -> "日常"
        "standby" -> "待机"
        "performance" -> "性能"
        else -> mode
    }

    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(modeName) },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun GpuModeChip(
    mode: String,
    currentMode: String,
    onClick: () -> Unit
) {
    val isSelected = mode == currentMode
    val modeName = when (mode) {
        "default" -> "默认"
        "performance" -> "性能"
        "power_saving" -> "节能"
        else -> mode
    }

    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(modeName) },
        shape = RoundedCornerShape(16.dp)
    )
}