package com.example.cache
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM cached_chats ORDER BY timestamp DESC")
    fun getAllCachedChats(): Flow<List<CachedChat>>
    
    @Query("SELECT * FROM cached_chats WHERE chat_id = :chatId")
    suspend fun getCachedChatById(chatId: String): CachedChat?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedChats(chats: List<CachedChat>)
    
    @Query("DELETE FROM cached_chats WHERE chat_id = :chatId")
    suspend fun deleteChatById(chatId: String)
}
