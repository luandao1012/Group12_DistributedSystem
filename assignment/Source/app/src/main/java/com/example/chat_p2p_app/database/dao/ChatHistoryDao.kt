package com.example.chat_p2p_app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chat_p2p_app.database.entity.ChatHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatHistoryDao {

    @Query("SELECT * FROM chat_histories WHERE current_user_id = :userId ORDER BY last_message_timestamp DESC")
    suspend fun getChatHistoriesForUser(userId: String): List<ChatHistoryEntity>

    @Query("SELECT * FROM chat_histories WHERE current_user_id = :userId ORDER BY last_message_timestamp DESC")
    fun getChatHistoriesForUserFlow(userId: String): Flow<List<ChatHistoryEntity>>

    @Query("SELECT * FROM chat_histories WHERE id = :historyId")
    suspend fun getChatHistoryById(historyId: String): ChatHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateChatHistory(chatHistory: ChatHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateChatHistories(chatHistories: List<ChatHistoryEntity>)

    @Query("DELETE FROM chat_histories WHERE id = :historyId")
    suspend fun deleteChatHistory(historyId: String)

    @Query("DELETE FROM chat_histories WHERE current_user_id = :userId")
    suspend fun deleteAllChatHistoriesForUser(userId: String)
}
