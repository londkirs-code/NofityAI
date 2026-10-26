package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.ApiKeyEntity
import com.example.data.model.ChatConversationEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ProviderType

class Converters {
    @TypeConverter
    fun fromProviderType(value: ProviderType): String = value.name

    @TypeConverter
    fun toProviderType(value: String): ProviderType = try {
        ProviderType.valueOf(value)
    } catch (_: Exception) {
        ProviderType.GEMINI
    }
}

@Database(
    entities = [
        ApiKeyEntity::class,
        ChatConversationEntity::class,
        ChatMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun apiKeyDao(): ApiKeyDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notifai_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
