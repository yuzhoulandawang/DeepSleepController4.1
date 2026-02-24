package com.example.deepsleep.ui.whitelist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.deepsleep.model.WhitelistItem
import com.example.deepsleep.model.WhitelistType
import kotlinx.coroutines.launch

/**
 * 白名单管理页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen(
    viewModel: WhitelistViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<WhitelistItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("白名单管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 类型切换
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = { expanded = true },
                            label = { 
                                Text(
                                    when (uiState.currentType) {
                                        WhitelistType.SUPPRESS -> "进程压制"
                                        WhitelistType.BACKGROUND -> "后台优化"
                                        WhitelistType.NETWORK -> "网络白名单"
                                    }
                                ) 
                            },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("进程压制") },
                                onClick = {
                                    viewModel.switchType(WhitelistType.SUPPRESS)
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("后台优化") },
                                onClick = {
                                    viewModel.switchType(WhitelistType.BACKGROUND)
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("网络白名单") },
                                onClick = {
                                    viewModel.switchType(WhitelistType.NETWORK)
                                    expanded = false
                                }
                            )
                        }
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            InfoBanner(type = uiState.currentType)
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.items.isEmpty()) {
                EmptyWhitelistView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        WhitelistItemCard(
                            item = item,
                            onEdit = { editingItem = item },
                            onDelete = {
                                scope.launch {
                                    viewModel.deleteItem(item)
                                    snackbarHostState.showSnackbar("已删除 ${item.name}")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    
    if (showAddDialog || editingItem != null) {
        AddEditDialog(
            item = editingItem,
            type = uiState.currentType,
            onDismiss = { 
                showAddDialog = false
                editingItem = null
            },
            onConfirm = { name, note ->
                scope.launch {
                    if (editingItem != null) {
                        viewModel.updateItem(editingItem!!.copy(name = name, note = note))
                        snackbarHostState.showSnackbar("已更新")
                    } else {
                        viewModel.addItem(name, note, uiState.currentType)
                        snackbarHostState.showSnackbar("已添加 $name")
                    }
                    showAddDialog = false
                    editingItem = null
                }
            }
        )
    }
}

@Composable
fun InfoBanner(type: WhitelistType) {
    val (icon, title, desc) = when (type) {
        WhitelistType.SUPPRESS -> Triple(
            Icons.Default.Security,
            "进程压制白名单",
            "这些进程不会被调整 OOM 分数"
        )
        WhitelistType.BACKGROUND -> Triple(
            Icons.Default.AppShortcut,
            "后台优化白名单",
            "这些应用不会被限制后台权限"
        )
        WhitelistType.NETWORK -> Triple(
            Icons.Default.Wifi,
            "网络白名单",
            "这些应用在深度睡眠模式下仍可访问网络"
        )
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun EmptyWhitelistView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "白名单为空",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "点击右上角 + 添加项目",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WhitelistItemCard(
    item: WhitelistItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (item.note.isNotEmpty()) {
                    Text(
                        text = item.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
        }
    }
}

@Composable
fun AddEditDialog(
    item: WhitelistItem?,
    type: WhitelistType,
    onDismiss: () -> Unit,
    onConfirm: (name: String, note: String) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var note by remember { mutableStateOf(item?.note ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "添加白名单" else "编辑白名单") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("应用包名") },
                    placeholder = { Text("com.example.app") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    placeholder = { Text("输入备注说明") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), note.trim())
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
