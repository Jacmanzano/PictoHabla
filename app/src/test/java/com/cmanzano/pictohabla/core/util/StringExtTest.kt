package com.cmanzano.pictohabla.core.util

import com.cmanzano.pictohabla.core.model.Category
import com.cmanzano.pictohabla.core.model.Pictogram
import org.junit.Assert.assertEquals
import org.junit.Test

class StringExtTest {

    @Test
    fun normalizeForSearch_removesAccentsAndPunctuation() {
        assertEquals("manzana", "Manzana".normalizeForSearch())
        assertEquals("platano", "Plátano".normalizeForSearch())
        assertEquals("que dia es hoy", "¿Qué día es hoy?".normalizeForSearch())
        assertEquals("adios", "¡Adiós!".normalizeForSearch())
    }

    @Test
    fun processPhraseForSpeech_aggregatesConsecutiveNumbersCorrectly() {
        val catNum = Category.NUMBERS
        val picto1 = Pictogram(id = 1001, names = mapOf("ES" to "uno"), category = catNum, isNumber = true, digit = 1)
        val picto2 = Pictogram(id = 1002, names = mapOf("ES" to "dos"), category = catNum, isNumber = true, digit = 2)
        val pictoManzana = Pictogram(id = 40, names = mapOf("ES" to "Manzana"), category = Category.FOOD)

        // [1] + [2] -> "12"
        val speech = processPhraseForSpeech(listOf(picto1, picto2), "ES")
        assertEquals("12", speech)

        // [1] + [2] + [Manzana] -> "12 Manzana"
        val speech2 = processPhraseForSpeech(listOf(picto1, picto2, pictoManzana), "ES")
        assertEquals("12 Manzana", speech2)
    }

    @Test
    fun processPhraseForSpeech_handlesLanguageTranslations() {
        val pictoYo = Pictogram(id = 20, names = mapOf("ES" to "Yo", "EN" to "I", "FR" to "Je"), category = Category.PEOPLE)
        val pictoQuiero = Pictogram(id = 30, names = mapOf("ES" to "Quiero", "EN" to "Want", "FR" to "Veux"), category = Category.ACTIONS)
        val pictoAgua = Pictogram(id = 44, names = mapOf("ES" to "Agua", "EN" to "Water", "FR" to "Eau"), category = Category.FOOD)

        val spanishPhrase = processPhraseForSpeech(listOf(pictoYo, pictoQuiero, pictoAgua), "ES")
        assertEquals("Yo Quiero Agua", spanishPhrase)

        val englishPhrase = processPhraseForSpeech(listOf(pictoYo, pictoQuiero, pictoAgua), "EN")
        assertEquals("I Want Water", englishPhrase)
    }

    @Test
    fun processPhraseForSpeech_usesCustomSpeechTextWhenAvailable() {
        val pictoCustom = Pictogram(
            id = 100001,
            names = mapOf("ES" to "Galleta"),
            speechText = "Quiero una rica galleta de avena",
            category = Category.FOOD,
            isCustom = true
        )
        val phrase = processPhraseForSpeech(listOf(pictoCustom), "ES")
        assertEquals("Quiero una rica galleta de avena", phrase)
    }
}
