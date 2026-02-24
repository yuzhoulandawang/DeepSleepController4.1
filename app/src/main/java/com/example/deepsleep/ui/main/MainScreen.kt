package com.example.deepsleep.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.deepsleep.model.*
import kotlinx.coroutines.launch

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
            StatusCard(settings)

            DeepSleepCard(settings.deepSleep, viewModel)

            PerformanceCard(
                perf = settings.performanceOptimization,
                onUpdate = { viewModel.updatePerformanceOptimization(it) }
            )

            ProcessManagementCard(
                pm = settings.processManagement,
                onUpdate = { viewModel.updateProcessManagement(it) }
            )

            BackgroundOptimizationCard(
                bg = settings.backgroundOptimization,
                onUpdate = { viewModel.updateBackgroundOptimization(it) }
            )

            ClickableItem(
                title = "场景检测",
                subtitle = "配置阻止深度睡眠的场景条件",
                onClick = onNavigateToSceneCheck
            )

            ClickableItem(
                title = "白名单管理",
                subtitle = "管理不受优化的应用",
                onClick = onNavigateToWhitelist
            )

            ClickableItem(
                title = "日志",
                subtitle = "查看应用运行日志",
                onClick = onNavigateToLogs
            )
        }
    }
}

@Composable
fun StatusCard(settings: AppSettings) {
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
fun DeepSleepCard(deepSleep: DeepSleep, viewModel: MainViewModel) {
    SettingsSection(title = "深度睡眠") {
        SwitchItem(
            title = "启用深度睡眠",
            subtitle = "息屏后自动进入深度睡眠并阻止退出",
            checked = deepSleep.enabled,
            onCheckedChange = { viewModel.setDeepSleepEnabled(it) }
        )
        if (deepSleep.enabled) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            NumberInputField(
                label = "延迟进入时间（秒）",
                value = deepSleep.delaySeconds.toString(),
                onValueChange = { newValue ->
                    newValue.toIntOrNull()?.let {
                        viewModel.setDeepSleepDelaySeconds(it)
                    }
                }
            )
            NumberInputField(
                label = "状态检查间隔（秒）",
                value = deepSleep.checkIntervalSeconds.toString(),
                onValueChange = { newValue ->
                    newValue.toIntOrNull()?.let {
                        viewModel.setDeepSleepCheckInterval(it)
                    }
                }
            )
            SwitchItem(
                title = "进入时开启系统省电",
                subtitle = "进入深度睡眠时自动开启系统省电模式",
                checked = deepSleep.enablePowerSaverOnSleep,
                onCheckedChange = { viewModel.setEnablePowerSaverOnSleep(it) }
            )
            SwitchItem(
                title = "退出时关闭系统省电",
                subtitle = "退出深度睡眠时自动关闭系统省电模式",
                checked = deepSleep.disablePowerSaverOnWake,
                onCheckedChange = { viewModel.setDisablePowerSaverOnWake(it) }
            )
        }
    }
}

@Composable
fun PerformanceCard(
    perf: PerformanceOptimization,
    onUpdate: (PerformanceOptimization) -> Unit
) {
    var showAdvancedDialog by remember { mutableStateOf(false) }

    SettingsSection(title = "性能优化") {
        SwitchItem(
            title = "启用性能优化",
            subtitle = "自动管理 CPU/GPU 参数和核心绑定",
            checked = perf.enabled,
            onCheckedChange = { onUpdate(perf.copy(enabled = it)) }
        )
        if (perf.enabled) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "当前模式",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PerformanceModeChip(
                    mode = PerformanceMode.ECO,
                    currentMode = perf.selectedMode,
                    onClick = { onUpdate(perf.copy(selectedMode = PerformanceMode.ECO)) }
                )
                PerformanceModeChip(
                    mode = PerformanceMode.DAILY,
                    currentMode = perf.selectedMode,
                    onClick = { onUpdate(perf.copy(selectedMode = PerformanceMode.DAILY)) }
                )
                PerformanceModeChip(
                    mode = PerformanceMode.PERFORMANCE,
                    currentMode = perf.selectedMode,
                    onClick = { onUpdate(perf.copy(selectedMode = PerformanceMode.PERFORMANCE)) }
                )
            }
            Button(
                onClick = { showAdvancedDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("高级自定义")
            }
        }
    }

    if (showAdvancedDialog) {
        PerformanceAdvancedDialog(
            perf = perf,
            onDismiss = { showAdvancedDialog = false },
            onConfirm = { updatedPerf ->
                onUpdate(updatedPerf)
                showAdvancedDialog = false
            }
        )
    }
}

@Composable
fun PerformanceModeChip(
    mode: PerformanceMode,
    currentMode: PerformanceMode,
    onClick: () -> Unit
) {
    val isSelected = mode == currentMode
    val modeName = when (mode) {
        PerformanceMode.ECO -> "省电"
        PerformanceMode.DAILY -> "日常"
        PerformanceMode.PERFORMANCE -> "性能"
    }
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(modeName) },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun PerformanceAdvancedDialog(
    perf: PerformanceOptimization,
    onDismiss: () -> Unit,
    onConfirm: (PerformanceOptimization) -> Unit
) {
    var ecoProfile by remember { mutableStateOf(perf.ecoProfile) }
    var dailyProfile by remember { mutableStateOf(perf.dailyProfile) }
    var performanceProfile by remember { mutableStateOf(perf.performanceProfile) }
    var selectedTab by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("性能参数自定义") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("省电") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("日常") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("性能") })
                }
                Spacer(modifier = Modifier.height(8.dp))
                when (selectedTab) {
                    0 -> ProfileEditSection(
                        profile = ecoProfile,
                        onProfileChange = { ecoProfile = it }
                    )
                    1 -> ProfileEditSection(
                        profile = dailyProfile,
                        onProfileChange = { dailyProfile = it }
                    )
                    2 -> ProfileEditSection(
                        profile = performanceProfile,
                        onProfileChange = { performanceProfile = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        perf.copy(
                            ecoProfile = ecoProfile,
                            dailyProfile = dailyProfile,
                            performanceProfile = performanceProfile
                        )
                    )
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun ProfileEditSection(
    profile: PerformanceProfile,
    onProfileChange: (PerformanceProfile) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("CPU 参数", style = MaterialTheme.typography.titleSmall)
        NumberInputField(
            label = "升频速率限制（微秒）",
            value = profile.cpu.upRate.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let {
                    onProfileChange(profile.copy(cpu = profile.cpu.copy(upRate = it)))
                }
            }
        )
        NumberInputField(
            label = "降频速率限制（微秒）",
            value = profile.cpu.downRate.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let {
                    onProfileChange(profile.copy(cpu = profile.cpu.copy(downRate = it)))
                }
            }
        )
        NumberInputField(
            label = "高负载阈值（%）",
            value = profile.cpu.hispeedLoad.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let {
                    onProfileChange(profile.copy(cpu = profile.cpu.copy(hispeedLoad = it)))
                }
            }
        )
        NumberInputField(
            label = "目标负载（%）",
            value = profile.cpu.targetLoads.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let {
                    onProfileChange(profile.copy(cpu = profile.cpu.copy(targetLoads = it)))
                }
            }
        )

        Text("GPU 参数", style = MaterialTheme.typography.titleSmall)
        NumberInputField(
            label = "最大频率（Hz）",
            value = profile.gpu.maxFreq.toString(),
            onValueChange = { newValue ->
                newValue.toLongOrNull()?.let {
                    onProfileChange(profile.copy(gpu = profile.gpu.copy(maxFreq = it)))
                }
            }
        )
        NumberInputField(
            label = "最小频率（Hz）",
            value = profile.gpu.minFreq.toString(),
            onValueChange = { newValue ->
                newValue.toLongOrNull()?.let {
                    onProfileChange(profile.copy(gpu = profile.gpu.copy(minFreq = it)))
                }
            }
        )
        NumberInputField(
            label = "空闲定时器（ms）",
            value = profile.gpu.idleTimer.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let {
                    onProfileChange(profile.copy(gpu = profile.gpu.copy(idleTimer = it)))
                }
            }
        )
        SwitchItem(
            title = "节流开关",
            subtitle = "启用GPU节流",
            checked = profile.gpu.throttlingEnabled,
            onCheckedChange = {
                onProfileChange(profile.copy(gpu = profile.gpu.copy(throttlingEnabled = it)))
            }
        )
        SwitchItem(
            title = "总线分割",
            subtitle = "启用总线分割",
            checked = profile.gpu.busSplitEnabled,
            onCheckedChange = {
                onProfileChange(profile.copy(gpu = profile.gpu.copy(busSplitEnabled = it)))
            }
        )
        NumberInputField(
            label = "热功率等级",
            value = profile.gpu.thermalPwrLevel.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let {
                    onProfileChange(profile.copy(gpu = profile.gpu.copy(thermalPwrLevel = it)))
                }
            }
        )
        NumberInputField(
            label = "温度触发点（mC）",
            value = profile.gpu.tripPointTemp.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let {
                    onProfileChange(profile.copy(gpu = profile.gpu.copy(tripPointTemp = it)))
                }
            }
        )
        NumberInputField(
            label = "温度滞后（mC）",
            value = profile.gpu.tripPointHyst.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let {
                    onProfileChange(profile.copy(gpu = profile.gpu.copy(tripPointHyst = it)))
                }
            }
        )
    }
}

@Composable
fun ProcessManagementCard(
    pm: ProcessManagement,
    onUpdate: (ProcessManagement) -> Unit
) {
    SettingsSection(title = "进程管理") {
        SwitchItem(
            title = "启用进程管理",
            subtitle = "管理后台进程压制和冻结",
            checked = pm.enabled,
            onCheckedChange = { onUpdate(pm.copy(enabled = it)) }
        )
        if (pm.enabled) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("进程压制", style = MaterialTheme.typography.titleSmall)
            SwitchItem(
                title = "启用压制",
                subtitle = "调整OOM评分",
                checked = pm.suppress.enabled,
                onCheckedChange = { onUpdate(pm.copy(suppress = pm.suppress.copy(enabled = it))) }
            )
            if (pm.suppress.enabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = pm.suppress.mode == SuppressMode.AGGRESSIVE,
                        onClick = { onUpdate(pm.copy(suppress = pm.suppress.copy(mode = SuppressMode.AGGRESSIVE))) },
                        label = { Text("激进") }
                    )
                    FilterChip(
                        selected = pm.suppress.mode == SuppressMode.CONSERVATIVE,
                        onClick = { onUpdate(pm.copy(suppress = pm.suppress.copy(mode = SuppressMode.CONSERVATIVE))) },
                        label = { Text("保守") }
                    )
                }
                NumberInputField(
                    label = "OOM值（-1000 到 1000）",
                    value = pm.suppress.oomScore.toString(),
                    onValueChange = { newValue ->
                        newValue.toIntOrNull()?.let {
                            onUpdate(pm.copy(suppress = pm.suppress.copy(oomScore = it)))
                        }
                    }
                )
            }

            Text("冻结", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
            SwitchItem(
                title = "启用冻结",
                subtitle = "应用退出前台后延迟冻结",
                checked = pm.freeze.enabled,
                onCheckedChange = { onUpdate(pm.copy(freeze = pm.freeze.copy(enabled = it))) }
            )
            if (pm.freeze.enabled) {
                NumberInputField(
                    label = "冻结延迟（秒）",
                    value = pm.freeze.delaySeconds.toString(),
                    onValueChange = { newValue ->
                        newValue.toIntOrNull()?.let {
                            onUpdate(pm.copy(freeze = pm.freeze.copy(delaySeconds = it)))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BackgroundOptimizationCard(
    bg: BackgroundOptimization,
    onUpdate: (BackgroundOptimization) -> Unit
) {
    SettingsSection(title = "后台优化") {
        SwitchItem(
            title = "启用后台优化",
            subtitle = "限制应用后台行为",
            checked = bg.enabled,
            onCheckedChange = { onUpdate(bg.copy(enabled = it)) }
        )
        if (bg.enabled) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SwitchItem(
                title = "禁止后台运行",
                subtitle = "通过 appops 限制",
                checked = bg.restrictBackground,
                onCheckedChange = { onUpdate(bg.copy(restrictBackground = it)) }
            )
            SwitchItem(
                title = "忽略唤醒锁",
                subtitle = "应用无法持有唤醒锁",
                checked = bg.ignoreWakeLock,
                onCheckedChange = { onUpdate(bg.copy(ignoreWakeLock = it)) }
            )
            SwitchItem(
                title = "设为 Rare 桶",
                subtitle = "降低应用调度优先级",
                checked = bg.setStandbyBucketRare,
                onCheckedChange = { onUpdate(bg.copy(setStandbyBucketRare = it)) }
            )
        }
    }
}

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
fun NumberInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    focusManager: FocusManager? = null
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
                onDone = { focusManager?.clearFocus() }
            ),
            singleLine = true
        )
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