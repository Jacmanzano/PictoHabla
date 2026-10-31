package com.cmanzano.pictohabla.core.model

import com.cmanzano.pictohabla.core.network.ArasaacTranslationHelper
import com.cmanzano.pictohabla.data.local.DefaultCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PictogramTranslationAndSearchTest {

    @Test
    fun testFiveExamplePictogramsTranslation() {
        val catalog = DefaultCatalog.getFullCatalog()

        // 1. Manzana = Apple (ID 40 / ARASAAC 2462)
        val manzana = catalog.find { it.id == 40 || it.arasaacId == 2462 }
        assertNotNull("Manzana debe existir en el catálogo inicial", manzana)
        assertEquals("Manzana", manzana!!.getName("ES"))
        assertEquals("Apple", manzana.getName("EN"))

        // 2. Casa = House (ID 60 / ARASAAC 2317)
        val casa = catalog.find { it.id == 60 || it.arasaacId == 2317 }
        assertNotNull("Casa debe existir en el catálogo inicial", casa)
        assertEquals("Casa", casa!!.getName("ES"))
        assertEquals("House", casa.getName("EN"))

        // 3. Agua = Water (ID 44 / ARASAAC 2248)
        val agua = catalog.find { it.id == 44 || it.arasaacId == 2248 }
        assertNotNull("Agua debe existir en el catálogo inicial", agua)
        assertEquals("Agua", agua!!.getName("ES"))
        assertEquals("Water", agua.getName("EN"))

        // 4. Perro = Dog (ARASAAC ID 2517 / 7202)
        val perroTranslation = ArasaacTranslationHelper.getTranslation(2517)
        assertNotNull("Traducción offline de Perro (2517) debe existir", perroTranslation)
        assertEquals("Perro", perroTranslation!!.nameEs)
        assertEquals("Dog", perroTranslation.nameEn)

        // 5. Gato = Cat (ARASAAC ID 2406)
        val gatoTranslation = ArasaacTranslationHelper.getTranslation(2406)
        assertNotNull("Traducción offline de Gato (2406) debe existir", gatoTranslation)
        assertEquals("Gato", gatoTranslation!!.nameEs)
        assertEquals("Cat", gatoTranslation.nameEn)
    }

    @Test
    fun testAll84InitialPictogramsHaveValidSpanishAndEnglishNames() {
        val catalog = DefaultCatalog.getFullCatalog()
        assertEquals("El catálogo debe contener exactamente 84 pictogramas", 84, catalog.size)

        catalog.forEach { picto ->
            val esName = picto.getName("ES")
            val enName = picto.getName("EN")

            assertTrue("El pictograma ${picto.id} debe tener un nombre en español no vacío", esName.isNotBlank())
            assertTrue("El pictograma ${picto.id} ('$esName') debe tener un nombre en inglés no vacío", enName.isNotBlank())

            // Verificamos que los nombres no sean idénticos cuando la palabra es claramente distinta (ej: Manzana != Apple)
            if (picto.id == 40) assertEquals("Apple", enName)
            if (picto.id == 60) assertEquals("House", enName)
            if (picto.id == 44) assertEquals("Water", enName)
        }
    }

    @Test
    fun testSearchKeywordsAvailableInBothLanguagesAndMatchCorrectly() {
        val catalog = DefaultCatalog.getFullCatalog()

        val manzana = catalog.first { it.id == 40 }
        // Búsqueda en español
        assertTrue("Búsqueda por nombre en ES", manzana.matchesSearch("manzana", "ES"))
        assertTrue("Búsqueda parcial en ES", manzana.matchesSearch("manz", "ES"))
        assertTrue("Búsqueda por sinónimo/keyword en ES", manzana.matchesSearch("fruta", "ES"))

        // Búsqueda en inglés
        assertTrue("Búsqueda por nombre en EN", manzana.matchesSearch("apple", "EN"))
        assertTrue("Búsqueda parcial en EN", manzana.matchesSearch("appl", "EN"))
        assertTrue("Búsqueda por sinónimo/keyword en EN", manzana.matchesSearch("fruit", "EN"))

        val casa = catalog.first { it.id == 60 }
        assertTrue("Casa busca 'casa' en ES", casa.matchesSearch("casa", "ES"))
        assertTrue("Casa busca 'hogar' en ES", casa.matchesSearch("hogar", "ES"))
        assertTrue("Casa busca 'house' en EN", casa.matchesSearch("house", "EN"))
        assertTrue("Casa busca 'home' en EN", casa.matchesSearch("home", "EN"))

        val agua = catalog.first { it.id == 44 }
        assertTrue("Agua busca 'agua' en ES", agua.matchesSearch("agua", "ES"))
        assertTrue("Agua busca 'water' en EN", agua.matchesSearch("water", "EN"))
        assertTrue("Agua busca 'drink' en EN", agua.matchesSearch("drink", "EN"))

        // Verificar búsqueda para Perro y Gato desde helper
        val perroPicto = Pictogram(
            id = 200001,
            arasaacId = 2517,
            names = mapOf("ES" to "Perro", "EN" to "Dog"),
            category = Category.THINGS,
            keywords = mapOf(
                "ES" to (ArasaacTranslationHelper.getTranslation(2517)?.keywordsEs ?: emptyList()),
                "EN" to (ArasaacTranslationHelper.getTranslation(2517)?.keywordsEn ?: emptyList())
            )
        )
        assertTrue("Perro busca 'perro' en ES", perroPicto.matchesSearch("perro", "ES"))
        assertTrue("Perro busca 'can' en ES", perroPicto.matchesSearch("can", "ES"))
        assertTrue("Perro busca 'dog' en EN", perroPicto.matchesSearch("dog", "EN"))
        assertTrue("Perro busca 'puppy' en EN", perroPicto.matchesSearch("puppy", "EN"))

        val gatoPicto = Pictogram(
            id = 200002,
            arasaacId = 2406,
            names = mapOf("ES" to "Gato", "EN" to "Cat"),
            category = Category.THINGS,
            keywords = mapOf(
                "ES" to (ArasaacTranslationHelper.getTranslation(2406)?.keywordsEs ?: emptyList()),
                "EN" to (ArasaacTranslationHelper.getTranslation(2406)?.keywordsEn ?: emptyList())
            )
        )
        assertTrue("Gato busca 'gato' en ES", gatoPicto.matchesSearch("gato", "ES"))
        assertTrue("Gato busca 'felino' en ES", gatoPicto.matchesSearch("felino", "ES"))
        assertTrue("Gato busca 'cat' en EN", gatoPicto.matchesSearch("cat", "EN"))
        assertTrue("Gato busca 'kitten' en EN", gatoPicto.matchesSearch("kitten", "EN"))
    }

    @Test
    fun testLanguageSwitchUpdatesTextImmediatelyWithoutRestart() {
        val catalog = DefaultCatalog.getFullCatalog()
        val picto = catalog.first { it.id == 40 } // Manzana

        // Idioma inicial ES
        var currentLang = "ES"
        assertEquals("Manzana", picto.getDisplayName(currentLang))
        assertEquals("Manzana", picto.getSpokenText(currentLang))

        // Cambio inmediato a EN
        currentLang = "EN"
        assertEquals("Apple", picto.getDisplayName(currentLang))
        assertEquals("Apple", picto.getSpokenText(currentLang))

        // Cambio de vuelta a ES
        currentLang = "ES"
        assertEquals("Manzana", picto.getDisplayName(currentLang))
        assertEquals("Manzana", picto.getSpokenText(currentLang))
    }

    @Test
    fun testCatalogAndTranslationsWork100PercentOffline() {
        // Ninguna de estas llamadas accede a la red: operan sobre recursos compilados
        val catalog = DefaultCatalog.getFullCatalog()
        assertEquals(84, catalog.size)

        catalog.forEach { p ->
            assertNotNull(p.arasaacId)
            val translation = ArasaacTranslationHelper.getTranslation(p.arasaacId!!)
            assertNotNull("Cada pictograma debe tener traducción offline disponible", translation)
            assertTrue(translation!!.nameEs.isNotBlank())
            assertTrue(translation.nameEn.isNotBlank())
            assertTrue(translation.keywordsEs.isNotEmpty())
            assertTrue(translation.keywordsEn.isNotEmpty())
        }
    }

    @Test
    fun testNoFrenchGermanOrItalianTranslationsPresent() {
        val catalog = DefaultCatalog.getFullCatalog()

        catalog.forEach { picto ->
            val keys = picto.names.keys
            assertEquals("Solo deben existir claves de idioma ES y EN", setOf("ES", "EN"), keys)
            assertFalse("No debe existir clave FR", keys.contains("FR"))
            assertFalse("No debe existir clave DE", keys.contains("DE"))
            assertFalse("No debe existir clave IT", keys.contains("IT"))

            // Comprobar fallback determinista a ES al solicitar idiomas no soportados
            assertEquals(picto.getName("ES"), picto.getName("FR"))
            assertEquals(picto.getName("ES"), picto.getName("DE"))
            assertEquals(picto.getName("ES"), picto.getName("IT"))
        }

        Category.entries.forEach { cat ->
            assertEquals(setOf("ES", "EN"), cat.labels.keys)
            assertEquals(cat.labels["ES"], cat.getLabel("FR"))
            assertEquals(cat.labels["ES"], cat.getLabel("DE"))
            assertEquals(cat.labels["ES"], cat.getLabel("IT"))
        }
    }
}
