package com.example.chat_p2p_app.repository

import android.util.Log
import com.example.chat_p2p_app.common.ConstValue.TAG
import com.example.chat_p2p_app.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val userCache = mutableMapOf<String, User>()

    suspend fun getUserById(userId: String): User? {
        return try {
            userCache[userId]?.let { return it }
            val document = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            val user = document.toObject(User::class.java)
            user?.let { userCache[userId] = it }
            user
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user by ID: $userId", e)
            null
        }
    }

    suspend fun getUserNameById(userId: String): String {
        return getUserById(userId)?.displayName ?: "Unknown User"
    }

    suspend fun getUserPhotoUrlById(userId: String): String? {
        return getUserById(userId)?.photoUrl
    }

    fun clearCache() {
        userCache.clear()
    }
}
