package com.cmanzano.pictohabla.core.util

import com.cmanzano.pictohabla.core.model.Pictogram
import java.text.Normalizer

/**
 * Normaliza una cadena de texto para búsquedas insensibles a mayúsculas,
 * tildes y signos de puntuación.
 */
fun String.normalizeForSearch(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    return temp.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .replace("[¿?¡!()\"]".toRegex(), "")
        .lowercase()
        .trim()
}

/**
 * Procesa una lista de pictogramas para generar la frase textual que será
 * pronunciada por el motor Text-To-Speech.
 *
 * Agrupa dígitos consecutivos en números completos (ej. [1] y [2] -> "12")
 * y utiliza `getSpokenText` para soportar pronunciaciones personalizadas.
 */
fun processPhraseForSpeech(pictos: List<Pictogram>, languageCode: String): String {
    val result = mutableListOf<String>()
    var currentNumber = ""

    pictos.forEach { picto ->
        if (picto.isNumber && picto.digit != null) {
            currentNumber += picto.digit.toString()
        } else {
            if (currentNumber.isNotEmpty()) {
                result.add(currentNumber)
                currentNumber = ""
            }
            val spoken = picto.getSpokenText(languageCode)
            if (spoken.isNotEmpty()) {
                result.add(spoken)
            }
        }
    }
    if (currentNumber.isNotEmpty()) {
        result.add(currentNumber)
    }

    return result.joinToString(" ")
}
