package com.hipka.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hipka.app.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history WHERE userId = :userId ORDER BY timestamp DESC LIMIT 10")
    fun getSearchHistoryForUser(userId: String): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchQuery(query: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE userId = :userId AND `query` = :query")
    suspend fun deleteSearchQuery(userId: String, query: String)

    @Query("DELETE FROM search_history WHERE userId = :userId")
    suspend fun clearAllHistoryForUser(userId: String)
}