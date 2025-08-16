package com.example.chat_p2p_app.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.chat_p2p_app.model.ChatMessage
import com.example.chat_p2p_app.model.MessageType
import java.util.Date

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "conversation_id")
    val conversationId: String,

    @ColumnInfo(name = "sender_id")
    val senderId: String,

    @ColumnInfo(name = "sender_name")
    val senderName: String,

    @ColumnInfo(name = "receiver_id")
    val receiverId: String,

    @ColumnInfo(name = "message")
    val message: String,

    @ColumnInfo(name = "message_type")
    val messageType: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
) {

    fun toChatMessage(currentUserId: String): ChatMessage {
        return ChatMessage(
            id = id,
            senderId = senderId,
            senderName = senderName,
            message = message,
            timestamp = Date(timestamp),
            messageType = MessageType.valueOf(messageType),
            isFromMe = senderId == currentUserId
        )
    }
}
