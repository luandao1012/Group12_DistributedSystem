package com.example.chat_p2p_app.database.dao

import androidx.room.*
import com.example.chat_p2p_app.database.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages WHERE conversation_id = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesForConversation(conversationId: String): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE conversation_id = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversationFlow(conversationId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversation_id = :conversationId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessageForConversation(conversationId: String): ChatMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM chat_messages WHERE conversation_id = :conversationId")
    suspend fun deleteAllMessagesForConversation(conversationId: String)

    @Query("DELETE FROM chat_messages WHERE sender_id = :userId OR receiver_id = :userId")
    suspend fun deleteAllMessagesForUser(userId: String)

    @Query("SELECT * FROM chat_messages WHERE conversation_id = :conversationId ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesWithPagination(conversationId: String, limit: Int, offset: Int): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE conversation_id = :conversationId AND message LIKE '%' || :searchQuery || '%' ORDER BY timestamp DESC")
    suspend fun searchMessages(conversationId: String, searchQuery: String): List<ChatMessageEntity>
}
