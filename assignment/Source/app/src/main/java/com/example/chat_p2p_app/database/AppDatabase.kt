package com.example.chat_p2p_app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.chat_p2p_app.database.dao.ChatHistoryDao
import com.example.chat_p2p_app.database.dao.ChatMessageDao
import com.example.chat_p2p_app.database.entity.ChatHistoryEntity
import com.example.chat_p2p_app.database.entity.ChatMessageEntity

@Database(
    entities = [
        ChatHistoryEntity::class,
        ChatMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chatHistoryDao(): ChatHistoryDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        private const val DATABASE_NAME = "chat_p2p_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
