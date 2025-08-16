package com.example.chat_p2p_app.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chat_p2p_app.common.ConstValue.TAG
import com.example.chat_p2p_app.database.entity.ChatHistoryEntity
import com.example.chat_p2p_app.repository.AuthRepository
import com.example.chat_p2p_app.repository.ChatHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatHistoryRepository: ChatHistoryRepository
) : ViewModel() {

    private val _chatHistories = MutableStateFlow<List<ChatHistoryEntity>>(emptyList())
    val chatHistories: StateFlow<List<ChatHistoryEntity>> = _chatHistories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    val currentUser = authRepository.getCurrentUser()

    init {
        loadChatHistories()
    }

    fun loadChatHistories() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                currentUser?.let { user ->
                    val histories = chatHistoryRepository.getChatHistory(user.uid)
                    _chatHistories.value = histories
                    Log.d(TAG, "Loaded ${histories.size} chat histories")
                } ?: run {
                    _chatHistories.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading chat histories", e)
                _errorMessage.tryEmit("Failed to load chat history: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshChatHistories() {
        loadChatHistories()
    }

    fun deleteChatHistory(chatHistory: ChatHistoryEntity) {
        viewModelScope.launch {
            try {
                val currentList = _chatHistories.value.toMutableList()
                currentList.removeAll { it.id == chatHistory.id }
                _chatHistories.value = currentList

                chatHistoryRepository.deleteChatHistory(chatHistory.currentUserId, chatHistory.otherUserId)
                Log.d(TAG, "Chat history deleted: ${chatHistory.otherUserName}")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting chat history", e)
                _errorMessage.tryEmit("Không thể xóa lịch sử chat: ${e.message}")
            }
        }
    }
}
