package com.example.chat_p2p_app.ui.contacts.allusers

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chat_p2p_app.model.User
import com.example.chat_p2p_app.repository.AuthRepository
import com.example.chat_p2p_app.repository.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AllUsersViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _friendRequestSent = MutableLiveData<Boolean>()
    val friendRequestSent: LiveData<Boolean> = _friendRequestSent

    init {
        loadAllUsers()
    }

    fun loadAllUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = authRepository.getCurrentUser()
                currentUser?.let { user ->
                    friendRepository.getAllUsersExceptFriends(user.uid).collect { userList ->
                        _users.value = userList
                        _isLoading.value = false
                    }
                } ?: run {
                    _errorMessage.value = "User not logged in"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load users: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun sendFriendRequest(toUser: User) {
        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                currentUser?.let { firebaseUser ->
                    val fromUser = User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        displayName = firebaseUser.displayName ?: "",
                        photoUrl = firebaseUser.photoUrl?.toString()
                    )
                    
                    friendRepository.sendFriendRequest(fromUser, toUser).collect { success ->
                        if (success) {
                            _friendRequestSent.value = true
                            val currentUsers = _users.value?.toMutableList() ?: mutableListOf()
                            currentUsers.remove(toUser)
                            _users.value = currentUsers
                        } else {
                            _errorMessage.value = "Failed to send friend request"
                        }
                    }
                } ?: run {
                    _errorMessage.value = "User not logged in"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to send friend request: ${e.message}"
            }
        }
    }

    fun searchUsers(query: String) {
        val currentUsers = _users.value ?: return
        if (query.isEmpty()) {
            loadAllUsers()
        } else {
            val filteredUsers = currentUsers.filter { user ->
                user.displayName.contains(query, ignoreCase = true) ||
                user.email.contains(query, ignoreCase = true)
            }
            _users.value = filteredUsers
        }
    }

    fun clearError() {
        _errorMessage.value = ""
    }

    fun clearFriendRequestSent() {
        _friendRequestSent.value = false
    }
} 