package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val name: String,
    val subtitle: String? = null,
    val avatarUrl: String? = null,
    val message: String = "",
    val draft: String = "",
    val time: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false,
    val isTyping: Boolean = false,
    val unreadCount: Int = 0,
    val hasStar: Boolean = false,
    val isMuted: Boolean = false,
    val isBot: Boolean = false,
    val isChannel: Boolean = false,
    val isGroup: Boolean = false,
    val isVerified: Boolean = false,
    val hasSparkleBadge: Boolean = false,
    val isReadReceipt: Boolean = false,
    val isMine: Boolean = false,
    val isNotes: Boolean = false,
    val isDefaultAvatar: Boolean = false,
    val isFavorite: Boolean = false,
    val isBlocked: Boolean = false,
    val isArchived: Boolean = false,
    val lastMediaType: String? = null,
    val lastMediaUrl: String? = null,
    val lastThumbnailUrl: String? = null,
    val participantIds: String = "[]" // Stored as JSON string
)

fun ChatEntity.toModel(): com.example.ui.ChatModel {
    val participants = try {
        kotlinx.serialization.json.Json.decodeFromString<List<String>>(participantIds)
    } catch (e: Exception) {
        if (participantIds.isNotEmpty() && participantIds != "[]") listOf(participantIds) else emptyList()
    }
    return com.example.ui.ChatModel(
        id = id,
        name = name,
        subtitle = subtitle,
        avatarUrl = avatarUrl,
        message = message,
        draft = draft,
        time = time,
        timestamp = timestamp,
        isOnline = isOnline,
        isTyping = isTyping,
        unreadCount = unreadCount,
        hasStar = hasStar,
        isMuted = isMuted,
        isBot = isBot,
        isChannel = isChannel,
        isGroup = isGroup,
        isVerified = isVerified,
        hasSparkleBadge = hasSparkleBadge,
        isReadReceipt = isReadReceipt,
        isMine = isMine,
        isNotes = isNotes,
        isDefaultAvatar = isDefaultAvatar,
        isFavorite = isFavorite,
        isBlocked = isBlocked,
        isArchived = isArchived,
        lastMediaType = lastMediaType,
        lastMediaUrl = lastMediaUrl,
        participantIds = participants
    )
}

fun com.example.ui.ChatModel.toEntity(): ChatEntity {
    return ChatEntity(
        id = id,
        name = name,
        subtitle = subtitle,
        avatarUrl = avatarUrl,
        message = message,
        draft = draft,
        time = time,
        timestamp = timestamp,
        isOnline = isOnline,
        isTyping = isTyping,
        unreadCount = unreadCount,
        hasStar = hasStar,
        isMuted = isMuted,
        isBot = isBot,
        isChannel = isChannel,
        isGroup = isGroup,
        isVerified = isVerified,
        hasSparkleBadge = hasSparkleBadge,
        isReadReceipt = isReadReceipt,
        isMine = isMine,
        isNotes = isNotes,
        isDefaultAvatar = isDefaultAvatar,
        isFavorite = isFavorite,
        isBlocked = isBlocked,
        isArchived = isArchived,
        lastMediaType = lastMediaType,
        lastMediaUrl = lastMediaUrl,
        participantIds = try { Json.encodeToString(participantIds) } catch(e: Exception) { "[]" }
    )
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY hasStar DESC, timestamp DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @androidx.room.Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatById(chatId: String): ChatEntity?

    @androidx.room.Query("SELECT * FROM chats WHERE id = :chatId")
    fun observeChatById(chatId: String): Flow<ChatEntity?>

    @androidx.room.Query("UPDATE chats SET draft = :draft WHERE id = :chatId")
    suspend fun updateDraft(chatId: String, draft: String)

    @androidx.room.Query("UPDATE chats SET isMuted = :isMuted WHERE id = :chatId")
    suspend fun updateMuteState(chatId: String, isMuted: Boolean)

    @androidx.room.Query("UPDATE chats SET hasStar = :hasStar WHERE id = :chatId")
    suspend fun updatePinState(chatId: String, hasStar: Boolean)
    
    @androidx.room.Query("UPDATE chats SET isFavorite = :isFavorite WHERE id = :chatId")
    suspend fun updateFavoriteState(chatId: String, isFavorite: Boolean)
    
    @androidx.room.Query("UPDATE chats SET isBlocked = :isBlocked WHERE id = :chatId")
    suspend fun updateBlockState(chatId: String, isBlocked: Boolean)

    @androidx.room.Query("UPDATE chats SET isArchived = :isArchived WHERE id = :chatId")
    suspend fun updateArchiveState(chatId: String, isArchived: Boolean)
    
    @androidx.room.Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChatById(chatId: String)
    
    @androidx.room.Query("UPDATE chats SET message = '', time = '' WHERE id = :chatId")
    suspend fun clearChatMessage(chatId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chats: List<ChatEntity>)
    


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chat: ChatEntity)
}


@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val username: String? = null,
    val fullName: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val walletAddress: String? = null,
    val chain: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val cryptvoraId: String? = null,
    val location: String? = null,
    val website: String? = null,
    val badge: String? = null,
    val postsCount: Long = 0,
    val followersCount: Long = 0,
    val followingCount: Long = 0,
    val isFollowing: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE id = :userId")
    fun observeProfile(userId: String): Flow<ProfileEntity?>
    
    @Query("SELECT * FROM profiles WHERE id = :userId")
    suspend fun getProfileById(userId: String): ProfileEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)
}

@Database(entities = [ChatEntity::class, ProfileEntity::class], version = 9, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun profileDao(): ProfileDao
}

object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "app_database"
            ).fallbackToDestructiveMigration().build()
            INSTANCE = instance
            instance
        }
    }
}
