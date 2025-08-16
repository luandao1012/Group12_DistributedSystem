package com.example.chat_p2p_app.repository

import android.util.Log
import com.example.chat_p2p_app.common.ConstValue.TAG
import com.example.chat_p2p_app.model.AuthCredentials
import com.example.chat_p2p_app.model.AuthResult
import com.example.chat_p2p_app.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    suspend fun signIn(email: String, password: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { firebaseUser ->
                val user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString()
                )
                emit(AuthResult.Success(user))
                Log.d(TAG, "signIn success: $user")
            } ?: emit(AuthResult.Error("Sign in failed"))
        } catch (e: Exception) {
            Log.e(TAG, "signIn: $e")
            emit(AuthResult.Error(e.message ?: "Sign in failed"))
        }
    }

    suspend fun signUp(credentials: AuthCredentials): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            val result = auth.createUserWithEmailAndPassword(credentials.email, credentials.password).await()
            result.user?.let { firebaseUser ->
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(credentials.displayName)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()

                val user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = credentials.displayName ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString()
                )

                saveUserToFirestore(user)
                emit(AuthResult.Success(user))
                Log.d(TAG, "signUp success: $user")
            } ?: emit(AuthResult.Error("Sign up failed"))
        } catch (e: Exception) {
            Log.e(TAG, "signUp: $e")
            emit(AuthResult.Error(e.message ?: "Sign up failed"))
        }
    }

    private suspend fun saveUserToFirestore(user: User) {
        try {
            firestore.collection("users")
                .document(user.uid)
                .set(user)
                .await()
        } catch (e: Exception) {
            Log.d(TAG, "saveUserToFirestore: $e")
            e.printStackTrace()
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }

    fun getAuthStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun getUserFromFirestore(uid: String): User? {
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            document.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }
}


