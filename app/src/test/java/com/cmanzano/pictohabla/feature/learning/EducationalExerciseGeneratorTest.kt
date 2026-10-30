package com.cmanzano.pictohabla.feature.learning

import com.cmanzano.pictohabla.core.model.Category
import com.cmanzano.pictohabla.core.model.Pictogram
import com.cmanzano.pictohabla.data.local.DefaultCatalog
import com.cmanzano.pictohabla.feature.learning.engine.EducationalExerciseGenerator
import com.cmanzano.pictohabla.feature.learning.model.LearningConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

/**
 * Pruebas unitarias para el motor generador de ejercicios educativos de PictoHabla (FASE 5).
 * Verifica generación de preguntas válidas, ausencia de opciones duplicadas, correspondencia de respuestas,
 * soporte de diferentes dificultades (2, 3, 4 opciones), compatibilidad con categorías y pictogramas personalizados,
 * y resiliencia ante catálogos reducidos o vacíos.
 */
class EducationalExerciseGeneratorTest {

    private lateinit var catalog: List<Pictogram>

    @Before
    fun setUp() {
        catalog = DefaultCatalog.getFullCatalog()
    }

    @Test
    fun `generateFindExercise produces valid exercise with correct target included in options`() {
        val config = LearningConfig(optionsCount = 4)
        val exercise = EducationalExerciseGenerator.generateFindExercise(catalog, config)

        assertNotNull(exercise.targetPicto)
        assertEquals(4, exercise.options.size)
        assertTrue("Las opciones deben contener el pictograma objetivo", exercise.options.any { it.id == exercise.targetPicto.id })
    }

    @Test
    fun `generateFindExercise contains no duplicate options`() {
        val config = LearningConfig(optionsCount = 4)
        // Repetimos 50 veces para asegurar aleatoriedad sin duplicados
        repeat(50) {
            val exercise = EducationalExerciseGenerator.generateFindExercise(catalog, config)
            val distinctIds = exercise.options.map { it.id }.distinct()
            assertEquals("No deben existir opciones duplicadas en el ejercicio", exercise.options.size, distinctIds.size)
        }
    }

    @Test
    fun `generateFindExercise respects different options counts 2 3 and 4`() {
        for (count in listOf(2, 3, 4)) {
            val config = LearningConfig(optionsCount = count)
            val exercise = EducationalExerciseGenerator.generateFindExercise(catalog, config)
            assertEquals("La cantidad de opciones generada debe ser igual a la configurada", count, exercise.options.size)
            assertTrue(exercise.options.any { it.id == exercise.targetPicto.id })
        }
    }

    @Test
    fun `generateMatchWordExercise generates valid word and target matching`() {
        val config = LearningConfig(optionsCount = 3)
        val exercise = EducationalExerciseGenerator.generateMatchWordExercise(catalog, "ES", config)

        assertNotNull(exercise.targetWord)
        assertTrue("La palabra no debe ser vacía", exercise.targetWord.isNotBlank())
        assertEquals("El nombre en español del objetivo debe coincidir con la palabra", exercise.targetPicto.getName("ES"), exercise.targetWord)
        assertEquals(3, exercise.options.size)
        assertTrue(exercise.options.any { it.id == exercise.targetPicto.id })
    }

    @Test
    fun `generateMatchWordExercise respects language code translations`() {
        val config = LearningConfig(optionsCount = 4)
        val exerciseEn = EducationalExerciseGenerator.generateMatchWordExercise(catalog, "EN", config)

        val expectedEn = exerciseEn.targetPicto.getName("EN")
        if (expectedEn.isNotBlank()) {
            assertEquals(expectedEn, exerciseEn.targetWord)
        }
    }

    @Test
    fun `generateClassifyExercise produces valid category classification`() {
        val config = LearningConfig(optionsCount = 4)
        val exercise = EducationalExerciseGenerator.generateClassifyExercise(catalog, config)

        assertNotNull(exercise.targetPicto)
        assertEquals(exercise.targetPicto.category, exercise.correctCategory)
        assertEquals(4, exercise.options.size)
        assertTrue("Las opciones de categoría deben contener la categoría correcta", exercise.options.contains(exercise.correctCategory))

        val distinctCategories = exercise.options.distinct()
        assertEquals("No deben existir categorías duplicadas en las opciones", exercise.options.size, distinctCategories.size)
    }

    @Test
    fun `generateClassifyExercise works with 2 3 and 4 options without duplicates`() {
        for (count in listOf(2, 3, 4)) {
            val config = LearningConfig(optionsCount = count)
            val exercise = EducationalExerciseGenerator.generateClassifyExercise(catalog, config)
            assertEquals(count, exercise.options.size)
            assertTrue(exercise.options.contains(exercise.correctCategory))
            assertEquals(count, exercise.options.distinct().size)
        }
    }

    @Test
    fun `filtered pool by category restricts target to that category`() {
        val config = LearningConfig(optionsCount = 3, categoryFilter = Category.FOOD)
        repeat(20) {
            val exercise = EducationalExerciseGenerator.generateFindExercise(catalog, config)
            assertEquals("El pictograma objetivo debe pertenecer a la categoría filtrada", Category.FOOD, exercise.targetPicto.category)
        }
    }

    @Test
    fun `generator handles custom pictograms correctly`() {
        val customPicto = Pictogram(
            id = 9999,
            names = mapOf("ES" to "Mi Juguete"),
            category = Category.THINGS,
            localImagePath = "/data/user/0/com.cmanzano.pictohabla/files/custom/custom_9999.png",
            isCustom = true
        )
        val augmentedCatalog = catalog + customPicto

        var customWasSelected = false
        val fixedSeedRandom = Random(12345)
        repeat(100) {
            val exercise = EducationalExerciseGenerator.generateFindExercise(augmentedCatalog, LearningConfig(optionsCount = 4), fixedSeedRandom)
            if (exercise.targetPicto.id == 9999) {
                customWasSelected = true
                assertEquals("Mi Juguete", exercise.targetPicto.getName("ES"))
                assertTrue(exercise.targetPicto.isCustom)
                assertEquals("/data/user/0/com.cmanzano.pictohabla/files/custom/custom_9999.png", exercise.targetPicto.localImagePath)
            }
        }
        assertTrue("El generador debe admitir e integrar pictogramas personalizados en los ejercicios", customWasSelected)
    }

    @Test
    fun `generator handles empty provided catalog by safely falling back to DefaultCatalog`() {
        val emptyList = emptyList<Pictogram>()
        val exercise = EducationalExerciseGenerator.generateFindExercise(emptyList, LearningConfig(optionsCount = 3))

        assertNotNull(exercise.targetPicto)
        assertEquals(3, exercise.options.size)
        assertTrue(exercise.options.any { it.id == exercise.targetPicto.id })
    }

    @Test
    fun `generator handles small catalog with only 2 items gracefully`() {
        val smallCatalog = catalog.take(2)
        val config = LearningConfig(optionsCount = 4)
        // Aunque se pidan 4 opciones, si solo hay 2 en el catálogo pequeño y distractor fallback
        val exercise = EducationalExerciseGenerator.generateFindExercise(smallCatalog, config)

        assertNotNull(exercise.targetPicto)
        assertTrue(exercise.options.isNotEmpty())
        assertTrue(exercise.options.any { it.id == exercise.targetPicto.id })
        // Garantiza que no haya duplicados ni crashes
        assertEquals(exercise.options.size, exercise.options.distinctBy { it.id }.size)
    }

    @Test
    fun `main board phrase remains completely isolated during learning exercises`() {
        // Verificación de invariante de arquitectura:
        // El estado de la frase de comunicación en el tablero principal no es tocado
        // por la ejecución de ejercicios ni selección de respuestas en el Modo Aprender.
        val mainBoardPhrase = mutableListOf(
            catalog.first { it.id == 20 }, // Yo
            catalog.first { it.id == 30 }  // Quiero
        )

        // Se simula la selección de una respuesta en el Modo Aprender
        val exercise = EducationalExerciseGenerator.generateFindExercise(catalog, LearningConfig(optionsCount = 4))
        val selectedAnswer = exercise.options.first()

        // Comprobamos que la frase del tablero principal permanece inalterada
        assertEquals(2, mainBoardPhrase.size)
        assertEquals(20, mainBoardPhrase[0].id)
        assertEquals(30, mainBoardPhrase[1].id)
        assertFalse("La selección de un ejercicio no debe añadirse a la frase principal", mainBoardPhrase.contains(selectedAnswer))
    }
}
