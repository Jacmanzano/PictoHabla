package com.cmanzano.pictohabla.data.repository

import com.cmanzano.pictohabla.core.database.dao.FavoriteDao
import com.cmanzano.pictohabla.core.database.dao.PictogramDao
import com.cmanzano.pictohabla.core.database.entity.FavoriteEntity
import com.cmanzano.pictohabla.core.database.entity.PictogramEntity
import com.cmanzano.pictohabla.core.model.Category
import com.cmanzano.pictohabla.core.model.Pictogram
import com.cmanzano.pictohabla.core.util.ImageStorageHelper
import com.cmanzano.pictohabla.data.local.DefaultCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repositorio para la gestión del catálogo de pictogramas (predeterminados y personalizados)
 * y favoritos persistidos en Room.
 */
class PictogramRepository(
    private val pictogramDao: PictogramDao,
    private val favoriteDao: FavoriteDao
) {

    /**
     * Inicializa el catálogo local en SQLite si la base de datos está vacía,
     * o sincroniza los IDs oficiales de ARASAAC en bases de datos existentes de versiones anteriores.
     * Operación idempotente y no destructiva.
     */
    suspend fun initializeCatalogIfNeeded() = withContext(Dispatchers.IO) {
        val count = pictogramDao.getCount()
        val defaultList = DefaultCatalog.getFullCatalog()
        if (count == 0) {
            val entities = defaultList.mapIndexed { index, picto ->
                PictogramEntity.fromDomain(picto, order = index)
            }
            pictogramDao.insertAll(entities)
        } else {
            val defaultMap = defaultList.associateBy { it.id }
            val existing = pictogramDao.getAllPictogramsList()
            existing.filter { !it.isCustom && it.arasaacId == null && defaultMap.containsKey(it.id) }
                .forEach { oldEntity ->
                    val defaultPicto = defaultMap[oldEntity.id]
                    if (defaultPicto?.arasaacId != null) {
                        pictogramDao.update(oldEntity.copy(arasaacId = defaultPicto.arasaacId))
                    }
                }
        }
    }

    /**
     * Emite la lista completa de pictogramas.
     */
    fun getAllPictograms(): Flow<List<Pictogram>> {
        return pictogramDao.getAllPictogramsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Emite los pictogramas filtrados por categoría.
     */
    fun getPictogramsByCategory(category: Category): Flow<List<Pictogram>> {
        return pictogramDao.getPictogramsByCategoryFlow(category.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Emite la lista de pictogramas personalizados creados por el usuario/profesor.
     */
    fun getCustomPictograms(): Flow<List<Pictogram>> {
        return pictogramDao.getCustomPictogramsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Emite el conjunto reactivo de IDs de pictogramas marcados como favoritos.
     */
    fun getFavoriteIds(): Flow<Set<Int>> {
        return favoriteDao.getAllFavoriteIdsFlow().map { it.toSet() }
    }

    /**
     * Emite la lista de pictogramas favoritos ordenados reactivamente.
     */
    fun getFavoritePictograms(): Flow<List<Pictogram>> {
        return combine(getAllPictograms(), getFavoriteIds()) { pictos, favoriteIds ->
            pictos.filter { it.id in favoriteIds }
        }
    }

    /**
     * Añade o elimina un pictograma de la lista de favoritos de forma atómica.
     */
    suspend fun toggleFavorite(pictoId: Int) = withContext(Dispatchers.IO) {
        val isFav = favoriteDao.isFavorite(pictoId)
        if (isFav) {
            favoriteDao.removeFavorite(pictoId)
        } else {
            favoriteDao.addFavorite(FavoriteEntity(pictoId = pictoId))
        }
    }

    /**
     * Recupera un pictograma específico por su identificador.
     */
    suspend fun getPictogramById(id: Int): Pictogram? = withContext(Dispatchers.IO) {
        pictogramDao.getPictogramById(id)?.toDomain()
    }

    /**
     * Recupera una lista de pictogramas por sus identificadores (para reconstruir frases del historial).
     */
    suspend fun getPictogramsByIds(ids: List<Int>): List<Pictogram> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyList()
        val entities = pictogramDao.getPictogramsByIds(ids)
        val entityMap = entities.associateBy { it.id }
        // Preserva el orden exacto de los IDs recibidos
        ids.mapNotNull { entityMap[it]?.toDomain() }
    }

    /**
     * Crea y persiste un nuevo pictograma personalizado con foto local opcional.
     */
    suspend fun createCustomPictogram(
        name: String,
        speechText: String? = null,
        category: Category,
        localImagePath: String? = null
    ): Pictogram = withContext(Dispatchers.IO) {
        val newId = pictogramDao.getNextCustomId()
        val entity = PictogramEntity(
            id = newId,
            nameEs = name.trim(),
            categoryName = category.name,
            speechText = speechText?.trim()?.ifEmpty { null },
            localImagePath = localImagePath,
            isCustom = true
        )
        pictogramDao.insert(entity)
        entity.toDomain()
    }

    /**
     * Añade y persiste un nuevo pictograma descargado desde ARASAAC en el catálogo local.
     */
    suspend fun createArasaacPictogram(
        arasaacId: Int,
        name: String,
        speechText: String? = null,
        category: Category,
        localImagePath: String? = null
    ): Pictogram = withContext(Dispatchers.IO) {
        val newId = pictogramDao.getNextCustomId()
        val entity = PictogramEntity(
            id = newId,
            nameEs = name.trim(),
            categoryName = category.name,
            speechText = speechText?.trim()?.ifEmpty { null },
            localImagePath = localImagePath,
            isCustom = true,
            arasaacId = arasaacId
        )
        pictogramDao.insert(entity)
        entity.toDomain()
    }

    /**
     * Actualiza un pictograma personalizado existente por sus propiedades individuales.
     */
    suspend fun updateCustomPictogram(
        id: Int,
        name: String,
        speechText: String? = null,
        category: Category,
        localImagePath: String? = null
    ) = withContext(Dispatchers.IO) {
        val existing = pictogramDao.getPictogramById(id)
        if (existing != null) {
            val updated = existing.copy(
                nameEs = name.trim(),
                speechText = speechText?.trim()?.ifEmpty { null },
                categoryName = category.name,
                localImagePath = localImagePath ?: existing.localImagePath
            )
            pictogramDao.update(updated)
        }
    }

    /**
     * Actualiza un pictograma personalizado existente a partir de una instancia de dominio.
     */
    suspend fun updateCustomPictogram(picto: Pictogram) = withContext(Dispatchers.IO) {
        val entity = PictogramEntity.fromDomain(picto)
        pictogramDao.update(entity)
    }

    /**
     * Elimina un pictograma personalizado y su archivo de imagen local asociado.
     */
    suspend fun deleteCustomPictogram(id: Int) = withContext(Dispatchers.IO) {
        val existing = pictogramDao.getPictogramById(id)
        if (existing != null && existing.isCustom) {
            ImageStorageHelper.deleteImage(existing.localImagePath)
            pictogramDao.deleteById(id)
        }
    }
}
