package com.cmanzano.pictohabla.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Tabla de unión cruzada (CrossRef) entre Tableros y Pictogramas.
 * Permite que un mismo pictograma pertenezca a varios tableros
 * conservando un orden y posición independiente en cada uno.
 */
@Entity(
    tableName = "board_pictograms",
    primaryKeys = ["boardId", "pictoId"],
    foreignKeys = [
        ForeignKey(
            entity = BoardEntity::class,
            parentColumns = ["id"],
            childColumns = ["boardId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PictogramEntity::class,
            parentColumns = ["id"],
            childColumns = ["pictoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["boardId", "position"]),
        Index(value = ["pictoId"])
    ]
)
data class BoardPictogramCrossRef(
    val boardId: Long,
    val pictoId: Int,
    val position: Int
)
