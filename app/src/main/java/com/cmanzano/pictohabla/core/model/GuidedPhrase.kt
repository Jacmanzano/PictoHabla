package com.cmanzano.pictohabla.core.model

/**
 * Representa una frase guiada para el modo de aprendizaje y práctica comunicativa.
 *
 * @property title Título o intención comunicativa en varios idiomas.
 * @property pictos Secuencia ordenada de pictogramas que componen la frase estructurada.
 */
data class GuidedPhrase(
    val title: Map<String, String>,
    val pictos: List<Pictogram>
) {
    fun getTitle(languageCode: String): String {
        return title[languageCode] ?: title["ES"] ?: ""
    }
}
