package com.cmanzano.pictohabla.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cmanzano.pictohabla.core.model.PhraseHistory

/**
 * Entidad de Room para persistir el historial de expresiones comunicativas emitidas por TTS.
 */
@Entity(tableName = "phrase_history")
data class PhraseHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val spokenText: String,
    val pictoIdsCsv: String, // IDs separados por coma, ej: "20,30,44"
    val languageCode: String = "ES",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(): PhraseHistory {
        val ids = if (pictoIdsCsv.isBlank()) {
            emptyList()
        } else {
            pictoIdsCsv.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
        }

        return PhraseHistory(
            id = id,
            spokenText = spokenText,
            pictoIds = ids,
            languageCode = languageCode,
            timestamp = timestamp
        )
    }

    companion object {
        fun fromDomain(domain: PhraseHistory): PhraseHistoryEntity {
            return PhraseHistoryEntity(
                id = domain.id,
                spokenText = domain.spokenText,
                pictoIdsCsv = domain.pictoIds.joinToString(","),
                languageCode = domain.languageCode,
                timestamp = domain.timestamp
            )
        }
    }
}
