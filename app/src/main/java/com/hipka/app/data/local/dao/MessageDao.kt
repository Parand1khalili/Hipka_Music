package com.hipka.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hipka.app.data.local.entity.OfflineMessageEntity

@Dao
interface MessageDao {

    @Query(
        """
        SELECT * FROM offline_messages
        WHERE (senderId = :userA AND receiverId = :userB)
           OR (senderId = :userB AND receiverId = :userA)
        ORDER BY timestamp ASC
        """
    )
    suspend fun getConversationOnce(userA: String, userB: String): List<OfflineMessageEntity>

    @Query("SELECT * FROM offline_messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): OfflineMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: OfflineMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<OfflineMessageEntity>)

    @Query("UPDATE offline_messages SET status = :status WHERE id = :messageId")
    suspend fun updateStatus(messageId: String, status: String)

    @Query(
        """
        UPDATE offline_messages SET status = :status
        WHERE senderId = :senderId AND receiverId = :receiverId AND status != :status
        """
    )
    suspend fun updateStatusForConversation(senderId: String, receiverId: String, status: String)
}