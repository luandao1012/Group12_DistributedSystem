package com.example.chat_p2p_app.ui.contacts.requests

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chat_p2p_app.model.FriendRequest
import com.example.chat_p2p_app.model.User
import com.example.chat_p2p_app.repository.AuthRepository
import com.example.chat_p2p_app.repository.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendRequestsViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _requests = MutableLiveData<List<FriendRequest>>()
    val requests: LiveData<List<FriendRequest>> = _requests

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _requestAccepted = MutableLiveData<Boolean>()
    val requestAccepted: LiveData<Boolean> = _requestAccepted

    private val _requestRejected = MutableLiveData<Boolean>()
    val requestRejected: LiveData<Boolean> = _requestRejected

    init {
        loadFriendRequests()
    }

    fun loadFriendRequests() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = authRepository.getCurrentUser()
                currentUser?.let { user ->
                    friendRepository.getPendingFriendRequests(user.uid).collect { requestList ->
                        _requests.value = requestList
                        _isLoading.value = false
                    }
                } ?: run {
                    _errorMessage.value = "User not logged in"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load friend requests: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun acceptFriendRequest(request: FriendRequest) {
        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                currentUser?.let { firebaseUser ->
                    val user = User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        displayName = firebaseUser.displayName ?: "",
                        photoUrl = firebaseUser.photoUrl?.toString()
                    )
                    
                    friendRepository.acceptFriendRequest(request).collect { success ->
                        if (success) {
                            _requestAccepted.value = true
                            val currentRequests = _requests.value?.toMutableList() ?: mutableListOf()
                            currentRequests.remove(request)
                            _requests.value = currentRequests
                        } else {
                            _errorMessage.value = "Failed to accept friend request"
                        }
                    }
                } ?: run {
                    _errorMessage.value = "User not logged in"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to accept friend request: ${e.message}"
            }
        }
    }

    fun rejectFriendRequest(request: FriendRequest) {
        viewModelScope.launch {
            try {
                friendRepository.rejectFriendRequest(request.id).collect { success ->
                    if (success) {
                        _requestRejected.value = true
                        val currentRequests = _requests.value?.toMutableList() ?: mutableListOf()
                        currentRequests.remove(request)
                        _requests.value = currentRequests
                    } else {
                        _errorMessage.value = "Failed to reject friend request"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to reject friend request: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = ""
    }

    fun clearRequestAccepted() {
        _requestAccepted.value = false
    }

    fun clearRequestRejected() {
        _requestRejected.value = false
    }
} 