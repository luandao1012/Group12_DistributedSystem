package com.example.chat_p2p_app.repository

import android.util.Log
import com.example.chat_p2p_app.common.ConstValue.TAG
import com.example.chat_p2p_app.model.FriendRequest
import com.example.chat_p2p_app.model.Friendship
import com.example.chat_p2p_app.model.RequestStatus
import com.example.chat_p2p_app.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun getAllUsersExceptFriends(currentUserId: String): Flow<List<User>> = flow {
        try {
            val friends = getFriendsList(currentUserId)
            val friendIds = friends.map { it.uid }.toSet()

            val snapshot = firestore.collection("users")
                .whereNotIn("uid", listOf(currentUserId))
                .get()
                .await()

            val allUsers = snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)
            }

            val filteredUsers = allUsers.filter { user ->
                user.uid != currentUserId && !friendIds.contains(user.uid)
            }

            emit(filteredUsers)
        } catch (e: Exception) {
            Log.e(TAG, "getAllUsersExceptFriends: $e")
            emit(emptyList())
        }
    }

    suspend fun sendFriendRequest(fromUser: User, toUser: User): Flow<Boolean> = flow {
        try {
            val request = FriendRequest(
                fromUserId = fromUser.uid,
                toUserId = toUser.uid,
                fromUserName = fromUser.displayName,
                toUserName = toUser.displayName,
                status = RequestStatus.PENDING
            )

            firestore.collection("friend_requests")
                .add(request)
                .await()

            emit(true)
        } catch (e: Exception) {
            Log.e(TAG, "sendFriendRequest: $e")
            emit(false)
        }
    }

    suspend fun getPendingFriendRequests(userId: String): Flow<List<FriendRequest>> = flow {
        try {
            val snapshot = firestore.collection("friend_requests")
                .whereEqualTo("toUserId", userId)
                .whereEqualTo("status", RequestStatus.PENDING)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val requests = snapshot.documents.mapNotNull { doc ->
                doc.toObject(FriendRequest::class.java)?.copy(id = doc.id)
            }

            emit(requests)
        } catch (e: Exception) {
            Log.e(TAG, "getPendingFriendRequests: $e")
            emit(emptyList())
        }
    }

    suspend fun acceptFriendRequest(request: FriendRequest): Flow<Boolean> = flow {
        try {
            firestore.collection("friend_requests")
                .document(request.id)
                .update("status", RequestStatus.ACCEPTED)
                .await()

            val friendship = Friendship(
                user1Id = request.fromUserId,
                user2Id = request.toUserId,
                user1Name = request.fromUserName,
                user2Name = request.toUserName
            )

            firestore.collection("friendships")
                .add(friendship)
                .await()

            emit(true)
        } catch (e: Exception) {
            Log.e(TAG, "acceptFriendRequest: $e")
            emit(false)
        }
    }

    suspend fun rejectFriendRequest(requestId: String): Flow<Boolean> = flow {
        try {
            firestore.collection("friend_requests")
                .document(requestId)
                .update("status", RequestStatus.REJECTED)
                .await()

            emit(true)
        } catch (e: Exception) {
            Log.e(TAG, "rejectFriendRequest: $e")
            emit(false)
        }
    }

    suspend fun getFriendsList(userId: String): List<User> {
        return try {
            val snapshot = firestore.collection("friendships")
                .whereEqualTo("user1Id", userId)
                .get()
                .await()

            val friendships1 = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Friendship::class.java)
            }

            val snapshot2 = firestore.collection("friendships")
                .whereEqualTo("user2Id", userId)
                .get()
                .await()

            val friendships2 = snapshot2.documents.mapNotNull { doc ->
                doc.toObject(Friendship::class.java)
            }

            val allFriendships = friendships1 + friendships2
            val friendIds = allFriendships.map { friendship ->
                if (friendship.user1Id == userId) friendship.user2Id else friendship.user1Id
            }

            val friends = mutableListOf<User>()
            for (friendId in friendIds) {
                val user = getUserById(friendId)
                user?.let { friends.add(it) }
            }

            friends
        } catch (e: Exception) {
            Log.e(TAG, "getFriendsList: $e")
            emptyList()
        }
    }

    private suspend fun getUserById(userId: String): User? {
        return try {
            val document = firestore.collection("users").document(userId).get().await()
            document.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }
} 