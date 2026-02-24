package com.example.deepsleep.ui.whitelist

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.deepsleep.data.WhitelistRepository
import com.example.deepsleep.model.WhitelistItem
import com.example.deepsleep.model.WhitelistType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 白名单页面 UI 状态
 */
data class WhitelistUiState(
    val currentType: WhitelistType = WhitelistType.SUPPRESS,
    val items: List<WhitelistItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 白名单页面 ViewModel
 * 管理白名单数据的状态和操作
 */
class WhitelistViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "WhitelistViewModel"
    }
    
    // 使用单例 WhitelistRepository
    private val _uiState = MutableStateFlow(WhitelistUiState())
    val uiState: StateFlow<WhitelistUiState> = _uiState.asStateFlow()
    
    init {
        loadItems()
    }
    
    fun switchType(type: WhitelistType) {
        _uiState.value = _uiState.value.copy(currentType = type)
        loadItems()
    }
    
    private fun loadItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val items = WhitelistRepository.loadItems(
                    getApplication(),
                    _uiState.value.currentType
                )
                _uiState.value = _uiState.value.copy(items = items, isLoading = false)
                Log.d(TAG, "Loaded ${items.size} items for type ${_uiState.value.currentType}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load items", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败"
                )
            }
        }
    }
    
    suspend fun addItem(name: String, note: String, type: WhitelistType) {
        try {
            WhitelistRepository.addItem(getApplication(), name, note, type)
            loadItems()
            Log.d(TAG, "Added item: $name")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add item", e)
        }
    }
    
    suspend fun updateItem(item: WhitelistItem) {
        try {
            WhitelistRepository.updateItem(getApplication(), item)
            loadItems()
            Log.d(TAG, "Updated item: ${item.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update item", e)
        }
    }
    
    suspend fun deleteItem(item: WhitelistItem) {
        try {
            WhitelistRepository.deleteItem(getApplication(), item)
            loadItems()
            Log.d(TAG, "Deleted item: ${item.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete item", e)
        }
    }
}
