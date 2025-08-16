package com.example.chat_p2p_app.repository

import android.util.Log
import com.example.chat_p2p_app.common.ConstValue.TAG
import com.example.chat_p2p_app.database.dao.ChatHistoryDao
import com.example.chat_p2p_app.database.dao.ChatMessageDao
import com.example.chat_p2p_app.database.entity.ChatHistoryEntity
import com.example.chat_p2p_app.database.entity.ChatMessageEntity
import com.example.chat_p2p_app.model.ChatMessage
import com.example.chat_p2p_app.model.MessageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatHistoryRepository @Inject constructor(
    private val chatHistoryDao: ChatHistoryDao,
    private val chatMessageDao: ChatMessageDao,
    private val userRepository: UserRepository
) {
    suspend fun saveChatMessage(
        senderId: String,
        senderName: String,
        receiverId: String,
        message: String,
        messageType: MessageType = MessageType.TEXT
    ): Boolean {
        return try {
            val conversationId = ChatHistoryEntity.generateConversationId(senderId, receiverId)
            val messageId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()

            val messageEntity = ChatMessageEntity(
                id = messageId,
                conversationId = conversationId,
                senderId = senderId,
                senderName = senderName,
                receiverId = receiverId,
                message = message,
                messageType = messageType.name,
                timestamp = timestamp,
                createdAt = timestamp
            )

            chatMessageDao.insertMessage(messageEntity)
            updateChatHistory(senderId, senderName, receiverId, message, timestamp, senderId)
            Log.d(TAG, "Chat message saved successfully to local database")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving chat message to local database", e)
            false
        }
    }

    private suspend fun updateChatHistory(
        senderId: String,
        senderName: String,
        receiverId: String,
        lastMessage: String,
        timestamp: Long,
        lastMessageSenderId: String
    ) {
        try {
            val conversationId = ChatHistoryEntity.generateConversationId(senderId, receiverId)
            val receiverName = userRepository.getUserNameById(receiverId)
            val receiverPhotoUrl = userRepository.getUserPhotoUrlById(receiverId)

            val senderHistoryId = ChatHistoryEntity.generateChatHistoryId(senderId, receiverId)
            val existingSenderHistory = chatHistoryDao.getChatHistoryById(senderHistoryId)

            val senderHistory = ChatHistoryEntity(
                id = senderHistoryId,
                currentUserId = senderId,
                otherUserId = receiverId,
                otherUserName = receiverName,
                otherUserPhotoUrl = receiverPhotoUrl,
                lastMessage = lastMessage,
                lastMessageTimestamp = timestamp,
                lastMessageSenderId = lastMessageSenderId,
                createdAt = existingSenderHistory?.createdAt ?: timestamp,
                updatedAt = timestamp
            )

            val senderPhotoUrl = userRepository.getUserPhotoUrlById(senderId)

            val receiverHistoryId = ChatHistoryEntity.generateChatHistoryId(receiverId, senderId)
            val existingReceiverHistory = chatHistoryDao.getChatHistoryById(receiverHistoryId)

            val receiverHistory = ChatHistoryEntity(
                id = receiverHistoryId,
                currentUserId = receiverId,
                otherUserId = senderId,
                otherUserName = senderName,
                otherUserPhotoUrl = senderPhotoUrl,
                lastMessage = lastMessage,
                lastMessageTimestamp = timestamp,
                lastMessageSenderId = lastMessageSenderId,
                createdAt = existingReceiverHistory?.createdAt ?: timestamp,
                updatedAt = timestamp
            )

            chatHistoryDao.insertOrUpdateChatHistory(senderHistory)
            chatHistoryDao.insertOrUpdateChatHistory(receiverHistory)

        } catch (e: Exception) {
            Log.e(TAG, "Error updating chat history in local database", e)
        }
    }

    suspend fun getChatHistory(userId: String): List<ChatHistoryEntity> {
        return try {
            chatHistoryDao.getChatHistoriesForUser(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting chat history from local database", e)
            emptyList()
        }
    }

    fun getChatHistoryFlow(userId: String): Flow<List<ChatHistoryEntity>> {
        return chatHistoryDao.getChatHistoriesForUserFlow(userId)
    }

    suspend fun getConversationMessages(userId1: String, userId2: String): List<ChatMessage> {
        return try {
            val conversationId = ChatHistoryEntity.generateConversationId(userId1, userId2)
            val messageEntities = chatMessageDao.getMessagesForConversation(conversationId)
            messageEntities.map { it.toChatMessage(userId1) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting conversation messages from local database", e)
            emptyList()
        }
    }

    fun getConversationMessagesFlow(userId1: String, userId2: String): Flow<List<ChatMessage>> {
        val conversationId = ChatHistoryEntity.generateConversationId(userId1, userId2)
        return chatMessageDao.getMessagesForConversationFlow(conversationId)
            .map { entities -> entities.map { it.toChatMessage(userId1) } }
    }


    suspend fun deleteChatHistory(currentUserId: String, otherUserId: String): Boolean {
        return try {
            val conversationId = ChatHistoryEntity.generateConversationId(currentUserId, otherUserId)
            val historyId = ChatHistoryEntity.generateChatHistoryId(currentUserId, otherUserId)
            chatHistoryDao.deleteChatHistory(historyId)
            chatMessageDao.deleteAllMessagesForConversation(conversationId)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting chat history from local database", e)
            false
        }
    }
}
