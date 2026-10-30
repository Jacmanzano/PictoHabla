package com.cmanzano.pictohabla.core.model

import com.cmanzano.pictohabla.data.local.DefaultCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogIntegrityTest {

    @Test
    fun defaultCatalog_allPictogramIdsAreUnique() {
        val catalog = DefaultCatalog.getFullCatalog()
        val uniqueIds = catalog.map { it.id }.toSet()
        assertEquals("Todos los IDs de pictogramas deben ser únicos", catalog.size, uniqueIds.size)
    }

    @Test
    fun defaultCatalog_allPictogramsHaveSpanishName() {
        val catalog = DefaultCatalog.getFullCatalog()
        catalog.forEach { picto ->
            val esName = picto.getName("ES")
            assertTrue("El pictograma ${picto.id} debe tener un nombre en español no vacío", esName.isNotBlank())
        }
    }

    @Test
    fun defaultCatalog_allGuidedPhrasesHaveValidPictograms() {
        val phrases = DefaultCatalog.getGuidedPhrases()
        assertTrue("Debe haber frases guiadas precargadas", phrases.isNotEmpty())
        phrases.forEach { phrase ->
            assertTrue("La frase guiada debe tener un título en español", phrase.getTitle("ES").isNotBlank())
            assertTrue("La frase guiada debe contener al menos un pictograma", phrase.pictos.isNotEmpty())
        }
    }

    @Test
    fun defaultCatalog_allPictogramsHaveValidCategory() {
        val catalog = DefaultCatalog.getFullCatalog()
        catalog.forEach { picto ->
            assertNotNull("El pictograma ${picto.id} debe tener una categoría no nula", picto.category)
            assertTrue("La categoría debe ser un valor válido del enum", Category.entries.contains(picto.category))
        }
    }

    @Test
    fun defaultCatalog_numericPictogramsHaveCorrectDigitsAndCategory() {
        val catalog = DefaultCatalog.getFullCatalog()
        val numbers = catalog.filter { it.isNumber }
        assertEquals("Debe haber exactamente 10 pictogramas numéricos (0..9)", 10, numbers.size)

        numbers.forEach { picto ->
            assertEquals("La categoría de un número debe ser NUMBERS", Category.NUMBERS, picto.category)
            assertNotNull("Un pictograma numérico debe tener un dígito definido", picto.digit)
            assertTrue("El dígito debe estar en el rango 0..9", picto.digit!! in 0..9)
        }

        // Pictogramas estándar no deben tener valor numérico ni flag isNumber
        val nonNumbers = catalog.filter { !it.isNumber }
        nonNumbers.forEach { picto ->
            assertFalse("Un pictograma normal no debe tener isNumber activado", picto.isNumber)
            assertNull("Un pictograma normal no debe tener dígito asignado (${picto.id}: ${picto.getName("ES")})", picto.digit)
        }
    }

    @Test
    fun defaultCatalog_essentialBoardIdsExistInCatalog() {
        val catalogIds = DefaultCatalog.getFullCatalog().map { it.id }.toSet()
        val boardIds = DefaultCatalog.getDefaultBoardPictoIds()

        boardIds.forEach { id ->
            assertTrue("El ID $id del tablero esencial debe existir en el catálogo", catalogIds.contains(id))
        }
    }
}
