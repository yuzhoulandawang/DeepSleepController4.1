package com.example.deepsleep.data

import android.content.Context
import android.util.Log
import com.example.deepsleep.model.WhitelistItem
import com.example.deepsleep.model.WhitelistType
import com.example.deepsleep.root.RootCommander
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 白名单仓库 - 单例对象
 * 管理各类白名单的读取和写入
 */
object WhitelistRepository {
    
    private const val TAG = "WhitelistRepository"
    
    private val basePath = "/data/local/tmp/deep_sleep_logs"
    private val mutex = Mutex()
    
    suspend fun loadItems(context: Context, type: WhitelistType): List<WhitelistItem> = withContext(Dispatchers.IO) {
        try {
            val path = getPath(type)
            val content = RootCommander.readFile(path) ?: return@withContext emptyList()
            
            content.lineSequence()
                .mapIndexedNotNull { index, line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("#") || trimmed.isEmpty()) return@mapIndexedNotNull null
                    
                    val parts = trimmed.split("#", limit = 2)
                    val name = parts[0].trim()
                    val note = parts.getOrNull(1)?.trim() ?: ""
                    // 使用内容哈希生成稳定 ID
                    val id = (type.name + name + note).hashCode().toString()
                    
                    WhitelistItem(
                        id = id,
                        name = name,
                        note = note,
                        type = type
                    )
                }
                .toList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load items for type: $type", e)
            emptyList()
        }
    }
    
    suspend fun addItem(context: Context, name: String, note: String, type: WhitelistType) = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val path = getPath(type)
                val line = if (note.isNotBlank()) "$name # $note" else name
                
                RootCommander.mkdir(basePath)
                RootCommander.exec("printf '%s\\n' \"$line\" >> $path")
                
                Log.d(TAG, "Added whitelist item: $name ($type)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add item", e)
            }
        }
    }
    
    suspend fun updateItem(context: Context, item: WhitelistItem) = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val path = getPath(item.type)
                val items = loadItems(context, item.type).toMutableList()
                
                val index = items.indexOfFirst { it.id == item.id }
                if (index != -1) {
                    items[index] = item
                    writeItems(path, items)
                    Log.d(TAG, "Updated whitelist item: ${item.name}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update item", e)
            }
        }
    }
    
    suspend fun deleteItem(context: Context, item: WhitelistItem) = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val path = getPath(item.type)
                val items = loadItems(context, item.type).filter { it.id != item.id }
                writeItems(path, items)
                Log.d(TAG, "Deleted whitelist item: ${item.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete item", e)
            }
        }
    }
    
    private suspend fun writeItems(path: String, items: List<WhitelistItem>) {
        val content = items.joinToString("\n") { item ->
            if (item.note.isNotBlank()) "${item.name} # ${item.note}" else item.name
        }
        RootCommander.exec("printf '%s\\n' \"$content\" > $path")
    }
    
    private fun getPath(type: WhitelistType): String {
        return when (type) {
            WhitelistType.SUPPRESS -> "$basePath/suppress_whitelist.txt"
            WhitelistType.BACKGROUND -> "$basePath/bg_whitelist.txt"
            WhitelistType.NETWORK -> "$basePath/network_whitelist.txt"
        }
    }
}
