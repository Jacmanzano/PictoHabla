package com.cmanzano.pictohabla.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de Room que registra los identificadores de los pictogramas marcados como favoritos.
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val pictoId: Int,
    val addedAt: Long = System.currentTimeMillis()
)
