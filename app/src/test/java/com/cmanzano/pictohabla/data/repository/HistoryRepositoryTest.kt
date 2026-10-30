package com.cmanzano.pictohabla.data.repository

import com.cmanzano.pictohabla.core.database.dao.HistoryDao
import com.cmanzano.pictohabla.core.database.entity.PhraseHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeHistoryDao : HistoryDao {
    private val entries = mutableListOf<PhraseHistoryEntity>()
    private val flow = MutableStateFlow<List<PhraseHistoryEntity>>(emptyList())
    private var nextId = 1L

    override fun getAllHistoryFlow(): Flow<List<PhraseHistoryEntity>> = flow

    override suspend fun insert(entity: PhraseHistoryEntity): Long {
        val assigned = entity.copy(id = nextId++)
        entries.add(0, assigned) // Insertar al inicio (más reciente)
        updateFlow()
        return assigned.id
    }

    override suspend fun deleteById(id: Long) {
        entries.removeAll { it.id == id }
        updateFlow()
    }

    override suspend fun clearAll() {
        entries.clear()
        updateFlow()
    }

    override suspend fun getCount(): Int = entries.size

    override suspend fun trimToMaxEntries(limit: Int) {
        if (entries.size > limit) {
            val toKeep = entries.take(limit)
            entries.clear()
            entries.addAll(toKeep)
            updateFlow()
        }
    }

    private fun updateFlow() {
        flow.value = entries.toList()
    }
}

class HistoryRepositoryTest {

    private lateinit var fakeDao: FakeHistoryDao
    private lateinit var repository: HistoryRepository

    @Before
    fun setUp() {
        fakeDao = FakeHistoryDao()
        repository = HistoryRepository(fakeDao, maxHistoryLimit = 5)
    }

    @Test
    fun recordPhrase_ignoresEmptyOrBlankPhrases() = runTest {
        repository.recordPhrase("", emptyList())
        repository.recordPhrase("   ", listOf(1, 2))
        assertEquals(0, fakeDao.getCount())

        val history = repository.getHistory().first()
        assertTrue(history.isEmpty())
    }

    @Test
    fun recordPhrase_ordersEntriesFromNewestToOldest() = runTest {
        repository.recordPhrase("Primera frase", listOf(20))
        repository.recordPhrase("Segunda frase", listOf(20, 30))
        repository.recordPhrase("Tercera frase", listOf(20, 30, 40))

        val history = repository.getHistory().first()
        assertEquals(3, history.size)
        assertEquals("Tercera frase", history[0].spokenText)
        assertEquals("Segunda frase", history[1].spokenText)
        assertEquals("Primera frase", history[2].spokenText)
    }

    @Test
    fun deleteEntry_removesSpecificPhrase() = runTest {
        repository.recordPhrase("Frase a borrar", listOf(10))
        val historyBefore = repository.getHistory().first()
        assertEquals(1, historyBefore.size)
        val idToDelete = historyBefore[0].id

        repository.deleteEntry(idToDelete)
        val historyAfter = repository.getHistory().first()
        assertTrue(historyAfter.isEmpty())
    }

    @Test
    fun clearHistory_wipesAllEntries() = runTest {
        repository.recordPhrase("Frase 1", listOf(1))
        repository.recordPhrase("Frase 2", listOf(2))
        assertEquals(2, fakeDao.getCount())

        repository.clearHistory()
        assertEquals(0, fakeDao.getCount())
        assertTrue(repository.getHistory().first().isEmpty())
    }

    @Test
    fun recordPhrase_enforcesMaximumLimit() = runTest {
        // El repositorio fue configurado con límite máximo de 5
        for (i in 1..8) {
            repository.recordPhrase("Frase $i", listOf(i))
        }

        val history = repository.getHistory().first()
        assertEquals("El historial debe limitarse a un máximo de 5 elementos", 5, history.size)
        assertEquals("La frase más reciente debe ser la 8", "Frase 8", history.first().spokenText)
    }
}
