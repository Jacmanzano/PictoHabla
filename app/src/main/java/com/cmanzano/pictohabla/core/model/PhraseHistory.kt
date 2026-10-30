package com.cmanzano.pictohabla.core.model

/**
 * Modelo de dominio que representa una entrada del historial de comunicación.
 *
 * @property id Identificador único de la expresión.
 * @property spokenText Texto pronunciado por el sintetizador de voz.
 * @property pictoIds Secuencia de IDs de pictogramas que componían la frase.
 * @property languageCode Código de idioma en que fue emitida.
 * @property timestamp Marca temporal en milisegundos.
 */
data class PhraseHistory(
    val id: Long = 0,
    val spokenText: String,
    val pictoIds: List<Int>,
    val languageCode: String = "ES",
    val timestamp: Long = System.currentTimeMillis()
)
