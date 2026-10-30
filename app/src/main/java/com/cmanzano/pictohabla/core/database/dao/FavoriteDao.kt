package com.cmanzano.pictohabla.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cmanzano.pictohabla.core.database.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT pictoId FROM favorites ORDER BY addedAt DESC")
    fun getAllFavoriteIdsFlow(): Flow<List<Int>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE pictoId = :pictoId LIMIT 1)")
    suspend fun isFavorite(pictoId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE pictoId = :pictoId")
    suspend fun removeFavorite(pictoId: Int)

    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun getFavoriteCount(): Int
}
