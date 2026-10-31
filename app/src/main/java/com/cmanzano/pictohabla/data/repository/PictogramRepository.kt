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
            // Sincronizar catálogo predeterminado: actualizar etiquetas bilingües ES/EN, keywords y arasaacId
            existing.filter { !it.isCustom && defaultMap.containsKey(it.id) }
                .forEach { oldEntity ->
                    val defaultPicto = defaultMap[oldEntity.id]!!
                    val updated = oldEntity.copy(
                        nameEs = defaultPicto.names["ES"] ?: oldEntity.nameEs,
                        nameEn = defaultPicto.names["EN"] ?: oldEntity.nameEn,
                        keywordsEs = defaultPicto.keywords["ES"]?.joinToString(", ") ?: oldEntity.keywordsEs,
                        keywordsEn = defaultPicto.keywords["EN"]?.joinToString(", ") ?: oldEntity.keywordsEn,
                        arasaacId = defaultPicto.arasaacId ?: oldEntity.arasaacId
                    )
                    if (updated != oldEntity) {
                        pictogramDao.update(updated)
                    }
                }
        }
        // Recuperar y sincronizar traducciones bilingües de pictogramas descargados previamente
        syncDownloadedPictogramTranslations()
    }

    /**
     * Sincroniza y repara pictogramas de ARASAAC descargados previamente que tengan `nameEn`
     * vacío o incorrectamente guardado en español, garantizando coherencia bilingüe sin pérdida de datos.
     */
    suspend fun syncDownloadedPictogramTranslations() = withContext(Dispatchers.IO) {
        val existing = pictogramDao.getAllPictogramsList()
        existing.filter { it.arasaacId != null }.forEach { entity ->
            val arasaacId = entity.arasaacId!!
            val isEnMissingOrIdentical = entity.nameEn.isBlank() ||
                (entity.nameEn.equals(entity.nameEs, ignoreCase = true) && entity.nameEs.isNotBlank())
            val areKeywordsMissing = entity.keywordsEs.isBlank() || entity.keywordsEn.isBlank()

            if (isEnMissingOrIdentical || areKeywordsMissing) {
                // 1. Consulta el diccionario offline en primer lugar
                val offline = com.cmanzano.pictohabla.core.network.ArasaacTranslationHelper.getTranslation(arasaacId)
                var resolvedNameEn = if (offline != null && offline.nameEn.isNotBlank()) {
                    offline.nameEn
                } else {
                    ""
                }
                var resolvedKwEs = if (offline != null && offline.keywordsEs.isNotEmpty()) {
                    offline.keywordsEs
                } else {
                    emptyList()
                }
                var resolvedKwEn = if (offline != null && offline.keywordsEn.isNotEmpty()) {
                    offline.keywordsEn
                } else {
                    emptyList()
                }

                // 2. Si falta la traducción inglesa, consultar la API oficial de ARASAAC
                if (resolvedNameEn.isBlank()) {
                    val details = com.cmanzano.pictohabla.core.network.ArasaacApiService.getPictogramDetails(arasaacId).getOrNull()
                    if (details != null) {
                        if (details.nameEn.isNotBlank()) resolvedNameEn = details.nameEn
                        if (resolvedKwEs.isEmpty()) resolvedKwEs = details.keywordsEs
                        if (resolvedKwEn.isEmpty()) resolvedKwEn = details.keywordsEn
                    }
                }

                if (resolvedNameEn.isNotBlank() && !resolvedNameEn.equals(entity.nameEs, ignoreCase = true)) {
                    val updated = entity.copy(
                        nameEn = resolvedNameEn,
                        keywordsEs = if (entity.keywordsEs.isNotBlank()) entity.keywordsEs else resolvedKwEs.joinToString(", "),
                        keywordsEn = if (entity.keywordsEn.isNotBlank() && !entity.keywordsEn.equals(entity.keywordsEs, ignoreCase = true)) {
                            entity.keywordsEn
                        } else {
                            resolvedKwEn.joinToString(", ")
                        }
                    )
                    if (updated != entity) {
                        pictogramDao.update(updated)
                    }
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
     * Añade y persiste un nuevo pictograma descargado desde ARASAAC en el catálogo local
     * conservando sus traducciones y palabras clave oficiales en español e inglés.
     */
    suspend fun createArasaacPictogram(
        arasaacId: Int,
        name: String,
        nameEn: String = "",
        keywordsEs: List<String> = emptyList(),
        keywordsEn: List<String> = emptyList(),
        speechText: String? = null,
        category: Category,
        localImagePath: String? = null
    ): Pictogram = withContext(Dispatchers.IO) {
        val newId = pictogramDao.getNextCustomId()
        val offlineTrans = com.cmanzano.pictohabla.core.network.ArasaacTranslationHelper.getTranslation(arasaacId)

        var resolvedNameEs = name.trim().ifEmpty { offlineTrans?.nameEs ?: "" }
        var resolvedNameEn = nameEn.trim()
        val resolvedKwEs = keywordsEs.toMutableList()
        val resolvedKwEn = keywordsEn.toMutableList()

        val needsEnglishResolution = resolvedNameEn.isBlank() ||
            (resolvedNameEn.equals(resolvedNameEs, ignoreCase = true) && resolvedNameEs.isNotBlank())

        if (needsEnglishResolution || resolvedKwEn.isEmpty() || resolvedKwEs.isEmpty()) {
            if (offlineTrans != null) {
                if (resolvedNameEn.isBlank() || resolvedNameEn.equals(resolvedNameEs, ignoreCase = true)) {
                    if (offlineTrans.nameEn.isNotBlank()) resolvedNameEn = offlineTrans.nameEn
                }
                if (resolvedKwEs.isEmpty()) resolvedKwEs.addAll(offlineTrans.keywordsEs)
                if (resolvedKwEn.isEmpty()) resolvedKwEn.addAll(offlineTrans.keywordsEn)
                if (resolvedNameEs.isBlank()) resolvedNameEs = offlineTrans.nameEs
            }

            if (resolvedNameEn.isBlank() || resolvedNameEn.equals(resolvedNameEs, ignoreCase = true) || resolvedKwEn.isEmpty()) {
                val details = com.cmanzano.pictohabla.core.network.ArasaacApiService.getPictogramDetails(arasaacId).getOrNull()
                if (details != null) {
                    if (details.nameEn.isNotBlank()) resolvedNameEn = details.nameEn
                    if (resolvedNameEs.isBlank() && details.nameEs.isNotBlank()) resolvedNameEs = details.nameEs
                    if (resolvedKwEs.isEmpty()) resolvedKwEs.addAll(details.keywordsEs)
                    if (resolvedKwEn.isEmpty()) resolvedKwEn.addAll(details.keywordsEn)
                }
            }
        }

        if (resolvedNameEs.isBlank()) resolvedNameEs = "Símbolo $arasaacId"

        // Protección obligatoria: Nunca presentar silenciosamente el texto español como si fuese inglés
        if (resolvedNameEn.equals(resolvedNameEs, ignoreCase = true)) {
            val offline = com.cmanzano.pictohabla.core.network.ArasaacTranslationHelper.getTranslation(arasaacId)
            if (offline != null && !offline.nameEn.equals(resolvedNameEs, ignoreCase = true)) {
                resolvedNameEn = offline.nameEn
            } else {
                resolvedNameEn = ""
            }
        }

        val finalKwEs = if (resolvedKwEs.isNotEmpty()) resolvedKwEs else listOf(resolvedNameEs.lowercase())
        val finalKwEn = if (resolvedKwEn.isNotEmpty()) {
            resolvedKwEn
        } else if (resolvedNameEn.isNotBlank()) {
            listOf(resolvedNameEn.lowercase())
        } else {
            emptyList()
        }

        val entity = PictogramEntity(
            id = newId,
            nameEs = resolvedNameEs,
            nameEn = resolvedNameEn,
            categoryName = category.name,
            speechText = speechText?.trim()?.ifEmpty { null },
            localImagePath = localImagePath,
            isCustom = true,
            arasaacId = arasaacId,
            keywordsEs = finalKwEs.joinToString(", "),
            keywordsEn = finalKwEn.joinToString(", ")
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
