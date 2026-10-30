package com.cmanzano.pictohabla.data.repository

import com.cmanzano.pictohabla.core.database.dao.FavoriteDao
import com.cmanzano.pictohabla.core.database.dao.PictogramDao
import com.cmanzano.pictohabla.core.database.entity.FavoriteEntity
import com.cmanzano.pictohabla.core.database.entity.PictogramEntity
import com.cmanzano.pictohabla.core.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakePictogramDao : PictogramDao {
    private val data = mutableMapOf<Int, PictogramEntity>()
    private val flow = MutableStateFlow<List<PictogramEntity>>(emptyList())

    override fun getAllPictogramsFlow(): Flow<List<PictogramEntity>> = flow

    override fun getPictogramsByCategoryFlow(categoryName: String): Flow<List<PictogramEntity>> {
        return flow.map { list -> list.filter { it.categoryName == categoryName } }
    }

    override fun getCustomPictogramsFlow(): Flow<List<PictogramEntity>> {
        return flow.map { list -> list.filter { it.isCustom } }
    }

    override suspend fun getPictogramById(id: Int): PictogramEntity? = data[id]

    override suspend fun getPictogramsByIds(ids: List<Int>): List<PictogramEntity> {
        return ids.mapNotNull { data[it] }
    }

    override suspend fun insertAll(pictograms: List<PictogramEntity>) {
        pictograms.forEach { data[it.id] = it }
        flow.value = data.values.sortedBy { it.orderIndex }
    }

    override suspend fun insert(entity: PictogramEntity): Long {
        data[entity.id] = entity
        flow.value = data.values.sortedBy { it.orderIndex }
        return entity.id.toLong()
    }

    override suspend fun update(entity: PictogramEntity) {
        data[entity.id] = entity
        flow.value = data.values.sortedBy { it.orderIndex }
    }

    override suspend fun deleteById(id: Int) {
        data.remove(id)
        flow.value = data.values.sortedBy { it.orderIndex }
    }

    override suspend fun getCount(): Int = data.size

    override suspend fun getAllPictogramsList(): List<PictogramEntity> = data.values.toList()

    override suspend fun getNextCustomId(): Int {
        val customIds = data.keys.filter { it >= 100000 }
        return if (customIds.isEmpty()) 100000 else customIds.maxOrNull()!! + 1
    }
}

class FakeFavoriteDao : FavoriteDao {
    private val favorites = mutableMapOf<Int, FavoriteEntity>()
    private val flow = MutableStateFlow<List<Int>>(emptyList())

    override fun getAllFavoriteIdsFlow(): Flow<List<Int>> = flow

    override suspend fun isFavorite(pictoId: Int): Boolean = favorites.containsKey(pictoId)

    override suspend fun addFavorite(favorite: FavoriteEntity) {
        favorites[favorite.pictoId] = favorite
        flow.value = favorites.values.sortedByDescending { it.addedAt }.map { it.pictoId }
    }

    override suspend fun removeFavorite(pictoId: Int) {
        favorites.remove(pictoId)
        flow.value = favorites.values.sortedByDescending { it.addedAt }.map { it.pictoId }
    }

    override suspend fun getFavoriteCount(): Int = favorites.size
}

class PictogramRepositoryTest {

    private lateinit var fakePictoDao: FakePictogramDao
    private lateinit var fakeFavoriteDao: FakeFavoriteDao
    private lateinit var repository: PictogramRepository

    @Before
    fun setUp() {
        fakePictoDao = FakePictogramDao()
        fakeFavoriteDao = FakeFavoriteDao()
        repository = PictogramRepository(fakePictoDao, fakeFavoriteDao)
    }

    @Test
    fun initializeCatalogIfNeeded_isIdempotentAndDoesNotDuplicate() = runTest {
        // Primera inicialización: carga el catálogo base
        repository.initializeCatalogIfNeeded()
        val countAfterFirst = fakePictoDao.getCount()
        assertTrue("El catálogo debe tener pictogramas iniciales", countAfterFirst > 0)

        // Segunda inicialización: no debe duplicar registros
        repository.initializeCatalogIfNeeded()
        val countAfterSecond = fakePictoDao.getCount()
        assertEquals("El número de pictogramas no debe cambiar tras reinicializar", countAfterFirst, countAfterSecond)
    }

    @Test
    fun toggleFavorite_addsAndRemovesCorrectly() = runTest {
        repository.initializeCatalogIfNeeded()
        val pictoId = 40 // Manzana

        // Inicialmente no es favorito
        assertFalse(fakeFavoriteDao.isFavorite(pictoId))

        // Añadir a favoritos
        repository.toggleFavorite(pictoId)
        assertTrue(fakeFavoriteDao.isFavorite(pictoId))
        val favIds = repository.getFavoriteIds().first()
        assertTrue(favIds.contains(pictoId))

        // Quitar de favoritos
        repository.toggleFavorite(pictoId)
        assertFalse(fakeFavoriteDao.isFavorite(pictoId))
        val updatedFavIds = repository.getFavoriteIds().first()
        assertFalse(updatedFavIds.contains(pictoId))
    }

    @Test
    fun getPictogramsByIds_reconstructsPhrasePreservingOrder() = runTest {
        repository.initializeCatalogIfNeeded()
        val ids = listOf(20, 30, 40) // [Yo, Quiero, Manzana]

        val reconstructed = repository.getPictogramsByIds(ids)
        assertEquals(3, reconstructed.size)
        assertEquals(20, reconstructed[0].id)
        assertEquals(30, reconstructed[1].id)
        assertEquals(40, reconstructed[2].id)
    }

    @Test
    fun createCustomPictogram_persistsWithCustomFlagAndSpeechText() = runTest {
        repository.initializeCatalogIfNeeded()

        val createdPicto = repository.createCustomPictogram(
            name = "Galleta de chocolate",
            speechText = "Quiero una rica galleta de chocolate",
            category = Category.FOOD,
            localImagePath = "/data/user/0/com.cmanzano.pictohabla/files/custom_pictos/picto_123.jpg"
        )

        assertTrue(createdPicto.id >= 100000)
        val picto = repository.getPictogramById(createdPicto.id)
        assertNotNull(picto)
        assertTrue(picto!!.isCustom)
        assertEquals("Galleta de chocolate", picto.getName("ES"))
        assertEquals("Quiero una rica galleta de chocolate", picto.getSpokenText("ES"))
        assertEquals(Category.FOOD, picto.category)
    }

    @Test
    fun updateCustomPictogram_modifiesDataSuccessfully() = runTest {
        val created = repository.createCustomPictogram(
            name = "Pelota",
            speechText = "Jugar a la pelota",
            category = Category.THINGS
        )

        repository.updateCustomPictogram(
            id = created.id,
            name = "Pelota roja grande",
            speechText = "Quiero jugar con la pelota roja",
            category = Category.THINGS
        )

        val updated = repository.getPictogramById(created.id)
        assertNotNull(updated)
        assertEquals("Pelota roja grande", updated!!.getName("ES"))
        assertEquals("Quiero jugar con la pelota roja", updated.getSpokenText("ES"))
    }

    @Test
    fun deleteCustomPictogram_removesFromDatabase() = runTest {
        val created = repository.createCustomPictogram(
            name = "Temporal",
            category = Category.SOCIAL
        )
        assertNotNull(repository.getPictogramById(created.id))

        repository.deleteCustomPictogram(created.id)
        assertNull(repository.getPictogramById(created.id))
    }

    @Test
    fun createArasaacPictogram_persistsWithArasaacId() = runTest {
        val created = repository.createArasaacPictogram(
            arasaacId = 2462,
            name = "Manzana verde",
            speechText = "Comer manzana",
            category = Category.FOOD,
            localImagePath = "/data/user/0/app/files/arasaac_pictos/2462.png"
        )

        assertNotNull(created)
        assertEquals(2462, created.arasaacId)
        assertEquals("Manzana verde", created.getName("ES"))
        assertTrue(created.isCustom)
        assertEquals(Category.FOOD, created.category)

        val retrieved = repository.getPictogramById(created.id)
        assertNotNull(retrieved)
        assertEquals(2462, retrieved!!.arasaacId)
        assertEquals("Manzana verde", retrieved.getName("ES"))
    }
}
