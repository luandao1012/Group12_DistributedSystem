package com.example.chat_p2p_app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chat_p2p_app.model.AuthCredentials
import com.example.chat_p2p_app.model.AuthResult
import com.example.chat_p2p_app.model.User
import com.example.chat_p2p_app.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthResult>(AuthResult.Loading)
    val authState: StateFlow<AuthResult> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        authRepository.getCurrentUser()?.let { firebaseUser ->
            _currentUser.value = User(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName ?: "",
                photoUrl = firebaseUser.photoUrl?.toString()
            )
            _authState.value = AuthResult.Success(_currentUser.value!!)
        } ?: run {
            _authState.value = AuthResult.Error("Not logged in")
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthResult.Error("Email and password cannot be empty")
            return
        }

        viewModelScope.launch {
            authRepository.signIn(email, password).collect { result ->
                _authState.value = result
                if (result is AuthResult.Success) {
                    _currentUser.value = result.user
                }
            }
        }
    }

    fun signUp(email: String, password: String, displayName: String) {
        if (email.isBlank() || password.isBlank() || displayName.isBlank()) {
            _authState.value = AuthResult.Error("All fields are required")
            return
        }

        if (password.length < 6) {
            _authState.value = AuthResult.Error("Password must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            val credentials = AuthCredentials(email, password, displayName)
            authRepository.signUp(credentials).collect { result ->
                _authState.value = result
                if (result is AuthResult.Success) {
                    _currentUser.value = result.user
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _currentUser.value = null
            _authState.value = AuthResult.Error("Not logged in")
        }
    }

    fun clearError() {
        if (_authState.value is AuthResult.Error) {
            _authState.value = AuthResult.Loading
        }
    }

    fun isUserLoggedIn(): Boolean {
        return authRepository.isUserLoggedIn()
    }
} 