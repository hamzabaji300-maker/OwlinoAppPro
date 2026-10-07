package com.example.cache
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CachedChat::class, CachedMessage::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cachedChatDao(): ChatDao
    abstract fun cachedMessageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE cached_messages ADD COLUMN edited_at TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE cached_messages ADD COLUMN file_size INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE cached_messages ADD COLUMN duration_ms INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE cached_messages ADD COLUMN link_url TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE cached_messages ADD COLUMN link_title TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE cached_messages ADD COLUMN link_description TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE cached_messages ADD COLUMN link_image_url TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE cached_messages ADD COLUMN reactions TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE cached_messages ADD COLUMN reply_markup TEXT DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cached_app_database"
                )
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
