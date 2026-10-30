package com.cmanzano.pictohabla.data.repository

import com.cmanzano.pictohabla.core.database.dao.HistoryDao
import com.cmanzano.pictohabla.core.database.entity.PhraseHistoryEntity
import com.cmanzano.pictohabla.core.model.PhraseHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repositorio para la persistencia y gestión del historial de expresiones comunicativas.
 */
class HistoryRepository(
    private val historyDao: HistoryDao,
    private val maxHistoryLimit: Int = 100
) {

    /**
     * Emite la lista de frases registradas en orden cronológico inverso (más recientes primero).
     */
    fun getHistory(): Flow<List<PhraseHistory>> {
        return historyDao.getAllHistoryFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Registra una expresión comunicativa en el historial si el texto no está en blanco.
     * Limita el historial al número máximo configurado para evitar consumo excesivo de memoria.
     */
    suspend fun recordPhrase(
        spokenText: String,
        pictoIds: List<Int>,
        languageCode: String = "ES"
    ) = withContext(Dispatchers.IO) {
        val trimmed = spokenText.trim()
        if (trimmed.isEmpty()) return@withContext

        val entity = PhraseHistoryEntity(
            spokenText = trimmed,
            pictoIdsCsv = pictoIds.joinToString(","),
            languageCode = languageCode,
            timestamp = System.currentTimeMillis()
        )
        historyDao.insert(entity)
        historyDao.trimToMaxEntries(maxHistoryLimit)
    }

    /**
     * Elimina una entrada individual del historial.
     */
    suspend fun deleteEntry(id: Long) = withContext(Dispatchers.IO) {
        historyDao.deleteById(id)
    }

    /**
     * Vacía por completo el historial de expresiones.
     */
    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        historyDao.clearAll()
    }
}
