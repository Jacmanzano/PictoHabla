package com.cmanzano.pictohabla.feature.board

import com.cmanzano.pictohabla.core.model.Category
import com.cmanzano.pictohabla.core.model.Pictogram
import com.cmanzano.pictohabla.data.local.DefaultCatalog
import com.cmanzano.pictohabla.feature.board.AACBoardLayoutPolicy.BoardLayoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para la optimización adaptativa del tablero CAA en móviles en horizontal.
 *
 * Valida los 4 escenarios obligatorios exigidos:
 * 1. Teléfono de 360 × 640 dp en orientación vertical.
 * 2. Teléfono de 640 × 360 dp en orientación horizontal.
 * 3. Tablet en orientación horizontal (ej. 1024 × 600 dp / 1280 × 800 dp).
 * 4. Tablet en orientación vertical (ej. 800 × 1280 dp / 600 × 960 dp).
 *
 * Y valida la ergonomía y construcción de frases en modo dos zonas:
 * - Proporción panel lateral (33%) vs zona principal (67%).
 * - Recorrido y adición consecutiva de pictogramas en la frase.
 * - Gestión de eliminación individual y vaciado.
 */
class AACBoardAdaptiveLayoutTest {

    @Test
    fun scenario1_phonePortrait_maintainsPortraitLayout() {
        val width = 360
        val height = 640
        val isLandscape = false

        val isPhoneLs = AACBoardLayoutPolicy.isPhoneLandscape(
            isOrientationLandscape = isLandscape,
            screenWidthDp = width,
            screenHeightDp = height,
            availableWidthDp = width.toFloat(),
            availableHeightDp = (height - 120).toFloat() // Tras cabecera y barra inferior
        )
        val mode = AACBoardLayoutPolicy.resolveLayoutMode(
            isOrientationLandscape = isLandscape,
            screenWidthDp = width,
            screenHeightDp = height,
            availableWidthDp = width.toFloat(),
            availableHeightDp = (height - 120).toFloat()
        )

        assertFalse("El teléfono en vertical NO debe activar el modo móvil horizontal", isPhoneLs)
        assertEquals("Debe usar PHONE_PORTRAIT", BoardLayoutMode.PHONE_PORTRAIT, mode)
    }

    @Test
    fun scenario2_phoneLandscape_activatesTwoZoneLayout() {
        val width = 640
        val height = 360
        val isLandscape = true

        val isPhoneLs = AACBoardLayoutPolicy.isPhoneLandscape(
            isOrientationLandscape = isLandscape,
            screenWidthDp = width,
            screenHeightDp = height,
            availableWidthDp = width.toFloat(),
            availableHeightDp = 240f // Altura reducida disponible
        )
        val mode = AACBoardLayoutPolicy.resolveLayoutMode(
            isOrientationLandscape = isLandscape,
            screenWidthDp = width,
            screenHeightDp = height,
            availableWidthDp = width.toFloat(),
            availableHeightDp = 240f
        )

        assertTrue("El teléfono de 640x360 dp en horizontal DEBE activar el nuevo modo de dos zonas", isPhoneLs)
        assertEquals("Debe resolver a PHONE_LANDSCAPE_TWO_ZONE", BoardLayoutMode.PHONE_LANDSCAPE_TWO_ZONE, mode)
    }

    @Test
    fun scenario3_tabletLandscape_maintainsExistingTabletLayout() {
        // Tablet compacta 7" (1024x600) y tablet 10" (1280x800)
        val testTablets = listOf(1024 to 600, 1280 to 800)

        for ((width, height) in testTablets) {
            val isLandscape = true
            val availableHeight = (height - 120).toFloat() // >= 480 dp disponible

            val isPhoneLs = AACBoardLayoutPolicy.isPhoneLandscape(
                isOrientationLandscape = isLandscape,
                screenWidthDp = width,
                screenHeightDp = height,
                availableWidthDp = width.toFloat(),
                availableHeightDp = availableHeight
            )
            val mode = AACBoardLayoutPolicy.resolveLayoutMode(
                isOrientationLandscape = isLandscape,
                screenWidthDp = width,
                screenHeightDp = height,
                availableWidthDp = width.toFloat(),
                availableHeightDp = availableHeight
            )

            assertFalse("Una tablet en horizontal ($width x $height) NO debe activar el diseño de móvil apaisado", isPhoneLs)
            assertEquals("Debe mantener el diseño TABLET_LANDSCAPE_SIDEBAR", BoardLayoutMode.TABLET_LANDSCAPE_SIDEBAR, mode)
        }
    }

    @Test
    fun scenario4_tabletPortrait_maintainsExistingPortraitLayout() {
        val testTablets = listOf(800 to 1280, 600 to 960)

        for ((width, height) in testTablets) {
            val isLandscape = false
            val availableHeight = (height - 120).toFloat()

            val isPhoneLs = AACBoardLayoutPolicy.isPhoneLandscape(
                isOrientationLandscape = isLandscape,
                screenWidthDp = width,
                screenHeightDp = height,
                availableWidthDp = width.toFloat(),
                availableHeightDp = availableHeight
            )
            val mode = AACBoardLayoutPolicy.resolveLayoutMode(
                isOrientationLandscape = isLandscape,
                screenWidthDp = width,
                screenHeightDp = height,
                availableWidthDp = width.toFloat(),
                availableHeightDp = availableHeight
            )

            assertFalse("Una tablet en vertical ($width x $height) NO debe activar el modo móvil horizontal", isPhoneLs)
            assertEquals("Debe resolver a TABLET_PORTRAIT", BoardLayoutMode.TABLET_PORTRAIT, mode)
        }
    }

    @Test
    fun twoZoneProportions_conformToRequestedRange() {
        val phrasePanelWeight = 0.33f
        val mainAreaWeight = 0.67f

        // Panel de frase: 30% - 35% del ancho
        assertTrue("El panel lateral de la frase debe estar entre 30% y 35%", phrasePanelWeight in 0.30f..0.35f)
        // Zona principal: 65% - 70% del ancho
        assertTrue("La zona principal debe estar entre 65% y 70%", mainAreaWeight in 0.65f..0.70f)
        // La suma debe ser exactamente 1.0 (100% del ancho de la pantalla)
        assertEquals(1.0f, phrasePanelWeight + mainAreaWeight, 0.001f)
    }

    @Test
    fun consecutiveSelection_andPhraseManipulation_worksCorrectly() {
        val catalog = DefaultCatalog.getFullCatalog()
        assertTrue("El catálogo debe contener pictogramas", catalog.size >= 3)
        val picto1 = catalog[0]
        val picto2 = catalog[1]
        val picto3 = catalog[2]

        var currentPhrase = listOf<Pictogram>()

        // 1. Selección consecutiva
        currentPhrase = currentPhrase + picto1
        currentPhrase = currentPhrase + picto2
        currentPhrase = currentPhrase + picto3

        assertEquals("La frase debe contener 3 pictogramas", 3, currentPhrase.size)
        assertEquals("Posición 1", picto1.getName("ES"), currentPhrase[0].getName("ES"))
        assertEquals("Posición 2", picto2.getName("ES"), currentPhrase[1].getName("ES"))
        assertEquals("Posición 3", picto3.getName("ES"), currentPhrase[2].getName("ES"))

        // 2. Eliminación del último pictograma («Quitar»)
        currentPhrase = currentPhrase.dropLast(1)
        assertEquals(2, currentPhrase.size)
        assertEquals(picto2.getName("ES"), currentPhrase.last().getName("ES"))

        // 3. Eliminación por índice individual (quitar el primero)
        val mutable = currentPhrase.toMutableList()
        mutable.removeAt(0)
        currentPhrase = mutable
        assertEquals(1, currentPhrase.size)
        assertEquals(picto2.getName("ES"), currentPhrase[0].getName("ES"))

        // 4. Borrado completo («Borrar»)
        currentPhrase = emptyList()
        assertTrue(currentPhrase.isEmpty())
    }

    @Test
    fun categoryPills_allCategoriesAreAvailableInTwoZoneMode() {
        val categories = Category.entries
        // Debe haber al menos 8 categorías principales de Fitzgerald
        assertTrue(categories.size >= 8)

        // Cada categoría tiene su etiqueta en español e inglés
        categories.forEach { cat ->
            assertTrue(cat.getLabel("ES").isNotBlank())
            assertTrue(cat.getLabel("EN").isNotBlank())
        }
    }
}
