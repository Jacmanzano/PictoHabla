package com.cmanzano.pictohabla.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cmanzano.pictohabla.core.database.entity.PhraseHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM phrase_history ORDER BY timestamp DESC")
    fun getAllHistoryFlow(): Flow<List<PhraseHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PhraseHistoryEntity): Long

    @Query("DELETE FROM phrase_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM phrase_history")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM phrase_history")
    suspend fun getCount(): Int

    @Query("DELETE FROM phrase_history WHERE id NOT IN (SELECT id FROM phrase_history ORDER BY timestamp DESC LIMIT :limit)")
    suspend fun trimToMaxEntries(limit: Int)
}
