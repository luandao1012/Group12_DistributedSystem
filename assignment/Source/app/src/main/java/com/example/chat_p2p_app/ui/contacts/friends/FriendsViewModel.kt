package com.example.chat_p2p_app.ui.contacts.friends

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
class FriendsViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _friends = MutableLiveData<List<User>>()
    val friends: LiveData<List<User>> = _friends

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    init {
        loadFriends()
    }

    fun loadFriends() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = authRepository.getCurrentUser()
                currentUser?.let { user ->
                    val friendsList = friendRepository.getFriendsList(user.uid)
                    _friends.value = friendsList
                    _isLoading.value = false
                } ?: run {
                    _errorMessage.value = "User not logged in"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load friends: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun searchFriends(query: String) {
        val currentFriends = _friends.value ?: return
        if (query.isEmpty()) {
            loadFriends()
        } else {
            val filteredFriends = currentFriends.filter { friend ->
                friend.displayName.contains(query, ignoreCase = true) ||
                friend.email.contains(query, ignoreCase = true)
            }
            _friends.value = filteredFriends
        }
    }

    fun clearError() {
        _errorMessage.value = ""
    }
    
    fun getCurrentUser(): User? {
        val firebaseUser = authRepository.getCurrentUser()
        return firebaseUser?.let { user ->
            User(
                uid = user.uid,
                email = user.email ?: "",
                displayName = user.displayName ?: "",
                photoUrl = user.photoUrl?.toString()
            )
        }
    }
} 