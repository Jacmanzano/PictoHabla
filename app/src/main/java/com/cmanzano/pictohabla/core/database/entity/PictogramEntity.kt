package com.cmanzano.pictohabla.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cmanzano.pictohabla.core.model.Category
import com.cmanzano.pictohabla.core.model.Pictogram
import com.cmanzano.pictohabla.data.local.DefaultCatalog

/**
 * Entidad de Room para persistir el catálogo de pictogramas en la base de datos local SQLite.
 */
@Entity(tableName = "pictograms")
data class PictogramEntity(
    @PrimaryKey
    val id: Int,
    val nameEs: String,
    val nameEn: String = "",
    val nameFr: String = "",
    val nameDe: String = "",
    val nameIt: String = "",
    val categoryName: String,
    val isNumber: Boolean = false,
    val digit: Int? = null,
    val customImageUrl: String? = null,
    val localImagePath: String? = null,
    val speechText: String? = null,
    val isCustom: Boolean = false,
    val orderIndex: Int = 0,
    val arasaacId: Int? = null,
    val keywordsEs: String = "",
    val keywordsEn: String = ""
) {
    /**
     * Convierte la entidad de Room al modelo de dominio [Pictogram],
     * enlazando su icono estático, etiquetas bilingües y palabras clave desde [DefaultCatalog] si procede.
     */
    fun toDomain(): Pictogram {
        val category = try {
            Category.valueOf(categoryName)
        } catch (_: Exception) {
            Category.THINGS
        }

        val catalogMatch = DefaultCatalog.getFullCatalog().find { it.id == id }

        val names = mutableMapOf<String, String>().apply {
            if (nameEs.isNotBlank()) {
                put("ES", nameEs)
            } else if (catalogMatch != null) {
                put("ES", catalogMatch.getName("ES"))
            }

            if (nameEn.isNotBlank() && !nameEn.equals(nameEs, ignoreCase = true)) {
                put("EN", nameEn)
            } else if (catalogMatch != null && catalogMatch.names.containsKey("EN")) {
                put("EN", catalogMatch.getName("EN"))
            } else if (arasaacId != null) {
                val offline = com.cmanzano.pictohabla.core.network.ArasaacTranslationHelper.getTranslation(arasaacId)
                if (offline != null && offline.nameEn.isNotBlank()) {
                    put("EN", offline.nameEn)
                } else if (nameEn.isNotBlank()) {
                    put("EN", nameEn)
                }
            } else if (nameEn.isNotBlank()) {
                put("EN", nameEn)
            }
        }

        val keywordsMap = mutableMapOf<String, List<String>>().apply {
            if (keywordsEs.isNotBlank()) {
                put("ES", keywordsEs.split(",").map { it.trim() }.filter { it.isNotEmpty() })
            } else if (catalogMatch != null && catalogMatch.keywords.containsKey("ES")) {
                put("ES", catalogMatch.getKeywords("ES"))
            } else if (arasaacId != null) {
                val offline = com.cmanzano.pictohabla.core.network.ArasaacTranslationHelper.getTranslation(arasaacId)
                if (offline != null && offline.keywordsEs.isNotEmpty()) {
                    put("ES", offline.keywordsEs)
                }
            }

            if (keywordsEn.isNotBlank() && !keywordsEn.equals(keywordsEs, ignoreCase = true)) {
                put("EN", keywordsEn.split(",").map { it.trim() }.filter { it.isNotEmpty() })
            } else if (catalogMatch != null && catalogMatch.keywords.containsKey("EN")) {
                put("EN", catalogMatch.getKeywords("EN"))
            } else if (arasaacId != null) {
                val offline = com.cmanzano.pictohabla.core.network.ArasaacTranslationHelper.getTranslation(arasaacId)
                if (offline != null && offline.keywordsEn.isNotEmpty()) {
                    put("EN", offline.keywordsEn)
                }
            }
        }

        val staticIcon = if (isNumber && digit != null) {
            DefaultCatalog.getNumberIcon(digit)
        } else {
            catalogMatch?.icon
        }

        val resolvedArasaacId = arasaacId ?: catalogMatch?.arasaacId

        return Pictogram(
            id = id,
            names = names,
            keywords = keywordsMap,
            icon = staticIcon,
            imageUrl = customImageUrl,
            localImagePath = localImagePath,
            speechText = speechText,
            category = category,
            isCustom = isCustom,
            isNumber = isNumber,
            digit = digit,
            arasaacId = resolvedArasaacId
        )
    }

    companion object {
        fun fromDomain(domain: Pictogram, order: Int = 0): PictogramEntity {
            return PictogramEntity(
                id = domain.id,
                nameEs = domain.names["ES"] ?: "",
                nameEn = domain.names["EN"] ?: "",
                nameFr = "",
                nameDe = "",
                nameIt = "",
                categoryName = domain.category.name,
                isNumber = domain.isNumber,
                digit = domain.digit,
                customImageUrl = domain.imageUrl,
                localImagePath = domain.localImagePath,
                speechText = domain.speechText,
                isCustom = domain.isCustom,
                orderIndex = order,
                arasaacId = domain.arasaacId,
                keywordsEs = domain.keywords["ES"]?.joinToString(", ") ?: "",
                keywordsEn = domain.keywords["EN"]?.joinToString(", ") ?: ""
            )
        }
    }
}
