package com.cmanzano.pictohabla.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cmanzano.pictohabla.core.database.entity.PictogramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PictogramDao {

    @Query("SELECT * FROM pictograms ORDER BY orderIndex ASC, id ASC")
    fun getAllPictogramsFlow(): Flow<List<PictogramEntity>>

    @Query("SELECT * FROM pictograms")
    suspend fun getAllPictogramsList(): List<PictogramEntity>

    @Query("SELECT * FROM pictograms WHERE categoryName = :categoryName ORDER BY orderIndex ASC, id ASC")
    fun getPictogramsByCategoryFlow(categoryName: String): Flow<List<PictogramEntity>>

    @Query("SELECT * FROM pictograms WHERE isCustom = 1 ORDER BY id DESC")
    fun getCustomPictogramsFlow(): Flow<List<PictogramEntity>>

    @Query("SELECT * FROM pictograms WHERE id = :id LIMIT 1")
    suspend fun getPictogramById(id: Int): PictogramEntity?

    @Query("SELECT * FROM pictograms WHERE id IN (:ids)")
    suspend fun getPictogramsByIds(ids: List<Int>): List<PictogramEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pictograms: List<PictogramEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PictogramEntity): Long

    @Update
    suspend fun update(entity: PictogramEntity)

    @Query("DELETE FROM pictograms WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT COUNT(*) FROM pictograms")
    suspend fun getCount(): Int

    @Query("SELECT COALESCE(MAX(id), 99999) + 1 FROM pictograms WHERE id >= 100000")
    suspend fun getNextCustomId(): Int
}
