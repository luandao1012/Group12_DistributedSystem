package com.example.chat_p2p_app.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_histories")
data class ChatHistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "current_user_id")
    val currentUserId: String,

    @ColumnInfo(name = "other_user_id")
    val otherUserId: String,

    @ColumnInfo(name = "other_user_name")
    val otherUserName: String,

    @ColumnInfo(name = "other_user_photo_url")
    val otherUserPhotoUrl: String? = null,

    @ColumnInfo(name = "last_message")
    val lastMessage: String,

    @ColumnInfo(name = "last_message_timestamp")
    val lastMessageTimestamp: Long,

    @ColumnInfo(name = "last_message_sender_id")
    val lastMessageSenderId: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
) {

    companion object {
        fun generateConversationId(userId1: String, userId2: String): String {
            val (low, high) = listOf(userId1, userId2).sorted()
            return "${low}_${high}"
        }

        fun generateChatHistoryId(currentUserId: String, otherUserId: String): String {
            val conversationId = generateConversationId(currentUserId, otherUserId)
            return "${currentUserId}_${conversationId}"
        }
    }
}
