package com.cmanzano.pictohabla.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cmanzano.pictohabla.core.model.Board

/**
 * Entidad de Room para persistir tableros de comunicación en SQLite.
 */
@Entity(tableName = "boards")
data class BoardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val columnsCount: Int = 3,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Board {
        return Board(
            id = id,
            name = name,
            description = description,
            columnsCount = columnsCount,
            isDefault = isDefault,
            pictograms = emptyList(),
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(domain: Board): BoardEntity {
            return BoardEntity(
                id = domain.id,
                name = domain.name,
                description = domain.description,
                columnsCount = domain.columnsCount,
                isDefault = domain.isDefault,
                createdAt = domain.createdAt
            )
        }
    }
}
