package com.m2h.m2haichatbot.data.local

import androidx.room.*
import com.m2h.m2haichatbot.data.local.dao.*
import com.m2h.m2haichatbot.data.local.entities.*

@Database(
    entities = [ChatEntity::class, MessageEntity::class, AIModelEntity::class],
    version = 2,
    exportSchema = false
)
abstract class M2HAIDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun aiModelDao(): AIModelDao
}
