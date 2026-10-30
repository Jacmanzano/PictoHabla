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
    val orderIndex: Int = 0
) {
    /**
     * Convierte la entidad de Room al modelo de dominio [Pictogram],
     * enlazando su icono estático desde [DefaultCatalog] si existe.
     */
    fun toDomain(): Pictogram {
        val category = try {
            Category.valueOf(categoryName)
        } catch (_: Exception) {
            Category.THINGS
        }

        val names = mutableMapOf<String, String>().apply {
            if (nameEs.isNotBlank()) put("ES", nameEs)
            if (nameEn.isNotBlank()) put("EN", nameEn)
            if (nameFr.isNotBlank()) put("FR", nameFr)
            if (nameDe.isNotBlank()) put("DE", nameDe)
            if (nameIt.isNotBlank()) put("IT", nameIt)
        }

        val staticIcon = if (isNumber && digit != null) {
            DefaultCatalog.getNumberIcon(digit)
        } else {
            DefaultCatalog.getFullCatalog().find { it.id == id }?.icon
        }

        return Pictogram(
            id = id,
            names = names,
            icon = staticIcon,
            imageUrl = customImageUrl,
            localImagePath = localImagePath,
            speechText = speechText,
            category = category,
            isCustom = isCustom,
            isNumber = isNumber,
            digit = digit
        )
    }

    companion object {
        fun fromDomain(domain: Pictogram, order: Int = 0): PictogramEntity {
            return PictogramEntity(
                id = domain.id,
                nameEs = domain.names["ES"] ?: "",
                nameEn = domain.names["EN"] ?: "",
                nameFr = domain.names["FR"] ?: "",
                nameDe = domain.names["DE"] ?: "",
                nameIt = domain.names["IT"] ?: "",
                categoryName = domain.category.name,
                isNumber = domain.isNumber,
                digit = domain.digit,
                customImageUrl = domain.imageUrl,
                localImagePath = domain.localImagePath,
                speechText = domain.speechText,
                isCustom = domain.isCustom,
                orderIndex = order
            )
        }
    }
}
