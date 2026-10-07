package com.example.cache
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM cached_messages WHERE chat_id = :chatId ORDER BY created_at ASC")
    fun getMessagesForChat(chatId: String): Flow<List<CachedMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedMessage(message: CachedMessage)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedMessages(messages: List<CachedMessage>)
    
    @Query("DELETE FROM cached_messages WHERE chat_id = :chatId")
    suspend fun deleteMessagesForChat(chatId: String)

    @Query("SELECT * FROM cached_messages WHERE status = 'SENDING' ORDER BY created_at ASC")
    suspend fun getPendingMessages(): List<CachedMessage>

    @Query("UPDATE cached_messages SET status = :status WHERE id = :id")
    suspend fun updateMessageStatus(id: String, status: String)

    @Query("DELETE FROM cached_messages WHERE id = :id")
    suspend fun deleteMessageById(id: String)

    @Query("UPDATE cached_messages SET status = 'READ' WHERE id IN (:ids)")
    suspend fun markMessagesRead(ids: List<String>)

    @Query("SELECT id FROM cached_messages WHERE status = 'READ'")
    suspend fun getReadMessageIds(): List<String>

    // ---- إضافات Local-First (قراءة/كتابة محلية فقط) ----
    @Query("SELECT * FROM (SELECT * FROM cached_messages WHERE chat_id = :chatId ORDER BY created_at DESC LIMIT :limit) ORDER BY created_at ASC")
    fun getRecentMessagesForChat(chatId: String, limit: Int): Flow<List<CachedMessage>>

    @Query("SELECT * FROM cached_messages WHERE id = :id LIMIT 1")
    suspend fun getCachedMessageById(id: String): CachedMessage?

    @Query("SELECT * FROM cached_messages WHERE id IN (:ids)")
    suspend fun getCachedMessagesByIds(ids: List<String>): List<CachedMessage>

    @Query("UPDATE cached_messages SET content = :content, edited_at = COALESCE(edited_at, :editedAt), status = CASE WHEN status = 'READ' THEN 'READ' ELSE :status END, reactions = :reactions, reply_markup = COALESCE(:replyMarkup, reply_markup) WHERE id = :id")
    suspend fun updateVolatile(id: String, content: String, editedAt: String?, status: String, reactions: String?, replyMarkup: String?)

    @Query("DELETE FROM cached_messages WHERE chat_id = :chatId AND created_at >= :fromTime AND created_at <= :toTime AND status IN ('SENT','READ','DELIVERED') AND id NOT IN (:keepIds)")
    suspend fun deleteSyncedMissingInWindow(chatId: String, fromTime: String, toTime: String, keepIds: List<String>)
}
