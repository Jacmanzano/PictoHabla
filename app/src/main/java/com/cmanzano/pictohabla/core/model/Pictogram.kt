package com.cmanzano.pictohabla.core.model

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Modelo de dominio para un pictograma en PictoHabla.
 *
 * @property id Identificador numérico único del pictograma.
 * @property names Nombres o etiquetas traducidas indexadas por código de idioma ("ES", "EN", etc.).
 * @property category Categoría semántica y gramatical según la Clave de Fitzgerald Modificada.
 * @property icon Icono vectorial de Material Design para pictogramas predeterminados.
 * @property imageUrl URL opcional de imagen externa (ARASAAC / web).
 * @property localImagePath Ruta persistente en almacenamiento interno para fotos personalizadas (100% offline).
 * @property speechText Texto opcional de pronunciación personalizada para el sintetizador TTS.
 * @property isCustom Indica si el pictograma fue creado localmente por el usuario o educador.
 * @property isNumber Indica si el pictograma representa un dígito numérico (0..9) para concatenación inteligente.
 * @property digit Valor entero (0..9) cuando [isNumber] es verdadero; null en caso contrario.
 * @property arasaacId Identificador oficial del pictograma en el catálogo ARASAAC (cuando proceda).
 */
data class Pictogram(
    val id: Int,
    val names: Map<String, String>,
    val category: Category,
    val icon: ImageVector? = null,
    val imageUrl: String? = null,
    val localImagePath: String? = null,
    val speechText: String? = null,
    val isCustom: Boolean = false,
    val isNumber: Boolean = false,
    val digit: Int? = null,
    val arasaacId: Int? = null
) {
    /**
     * Constructor secundario de conveniencia para mantener compatibilidad con firmas
     * posicionales legadas: (id, names, icon, imageUrl, category, isNumber, digit, arasaacId).
     */
    constructor(
        id: Int,
        names: Map<String, String>,
        icon: ImageVector?,
        imageUrl: String? = null,
        category: Category,
        isNumber: Boolean = false,
        digit: Int? = null,
        arasaacId: Int? = null
    ) : this(
        id = id,
        names = names,
        category = category,
        icon = icon,
        imageUrl = imageUrl,
        localImagePath = null,
        speechText = null,
        isCustom = false,
        isNumber = isNumber,
        digit = digit,
        arasaacId = arasaacId
    )

    /**
     * Constructor secundario simplificado para pictogramas básicos del catálogo: (id, names, icon, category, arasaacId).
     */
    constructor(
        id: Int,
        names: Map<String, String>,
        icon: ImageVector?,
        category: Category,
        arasaacId: Int? = null
    ) : this(
        id = id,
        names = names,
        category = category,
        icon = icon,
        imageUrl = null,
        localImagePath = null,
        speechText = null,
        isCustom = false,
        isNumber = false,
        digit = null,
        arasaacId = arasaacId
    )

    fun getName(languageCode: String): String {
        return names[languageCode] ?: names["ES"] ?: ""
    }

    /**
     * Retorna el texto exacto que debe pronunciar el sintetizador TTS:
     * Si tiene pronunciación personalizada configurada la utiliza; de lo contrario usa el nombre visible.
     */
    fun getSpokenText(languageCode: String): String {
        if (!speechText.isNullOrBlank()) {
            return speechText
        }
        return getName(languageCode)
    }
}
