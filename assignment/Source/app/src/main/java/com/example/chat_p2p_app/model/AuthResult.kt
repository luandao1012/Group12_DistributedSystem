package com.example.chat_p2p_app.model

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Loading : AuthResult()
}

data class AuthCredentials(
    val email: String,
    val password: String,
    val displayName: String? = null
)
