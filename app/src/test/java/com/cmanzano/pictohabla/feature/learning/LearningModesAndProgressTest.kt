package com.cmanzano.pictohabla.feature.learning

import com.cmanzano.pictohabla.core.model.Pictogram
import com.cmanzano.pictohabla.data.local.DefaultCatalog
import com.cmanzano.pictohabla.feature.learning.engine.EducationalExerciseGenerator
import com.cmanzano.pictohabla.feature.learning.model.LearningActivityType
import com.cmanzano.pictohabla.feature.learning.model.LearningConfig
import com.cmanzano.pictohabla.testutil.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para las 5 modalidades de aprendizaje y el sistema de seguimiento local de progreso.
 */
class LearningModesAndProgressTest {

    private lateinit var catalog: List<Pictogram>
    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var progressManager: LearningProgressManager

    @Before
    fun setUp() {
        catalog = DefaultCatalog.getFullCatalog()
        fakePrefs = FakeSharedPreferences()
        progressManager = LearningProgressManager(fakePrefs)
    }

    @Test
    fun `Modo 1 - FindPictogram supports progressive difficulty with 2, 3 and 4 options`() {
        listOf(2, 3, 4).forEach { count ->
            val exercise = EducationalExerciseGenerator.generateFindExercise(catalog, LearningConfig(optionsCount = count))
            assertEquals(count, exercise.options.size)
            assertTrue("Debe incluir el pictograma objetivo", exercise.options.any { it.id == exercise.targetPicto.id })
            assertEquals(count, exercise.options.map { it.id }.distinct().size)
        }
    }

    @Test
    fun `Modo 2 - WhatMeansExercise generates valid translated words for ES and EN without duplicates`() {
        // En español
        val exerciseEs = EducationalExerciseGenerator.generateWhatMeansExercise(catalog, "ES", LearningConfig(optionsCount = 3))
        assertNotNull(exerciseEs.targetPicto)
        assertTrue(exerciseEs.correctWord.isNotBlank())
        assertEquals(exerciseEs.targetPicto.getName("ES"), exerciseEs.correctWord)
        assertEquals(3, exerciseEs.options.size)
        assertTrue("Las opciones deben contener la palabra correcta", exerciseEs.options.contains(exerciseEs.correctWord))
        assertEquals("No debe haber palabras duplicadas", 3, exerciseEs.options.distinct().size)

        // En inglés
        val exerciseEn = EducationalExerciseGenerator.generateWhatMeansExercise(catalog, "EN", LearningConfig(optionsCount = 4))
        assertEquals(exerciseEn.targetPicto.getName("EN"), exerciseEn.correctWord)
        assertEquals(4, exerciseEn.options.size)
        assertTrue(exerciseEn.options.contains(exerciseEn.correctWord))
    }

    @Test
    fun `Modo 3 - ListenExercise includes target spoken word and unique pictogram options`() {
        listOf(2, 3, 4).forEach { count ->
            val exercise = EducationalExerciseGenerator.generateListenExercise(catalog, "ES", LearningConfig(optionsCount = count))
            assertNotNull(exercise.targetPicto)
            assertEquals(exercise.targetPicto.getName("ES"), exercise.targetWord)
            assertEquals(count, exercise.options.size)
            assertTrue(exercise.options.any { it.id == exercise.targetPicto.id })
            assertEquals(count, exercise.options.map { it.id }.distinct().size)
        }
    }

    @Test
    fun `Modo 4 - MemoryExercise generates correct number of pairs and matches`() {
        listOf(2, 3, 4).forEach { pairs ->
            val exercise = EducationalExerciseGenerator.generateMemoryExercise(catalog, pairsCount = pairs)
            assertEquals("El total de cartas debe ser 2 * parejas", pairs * 2, exercise.cards.size)

            // Cada pairId debe aparecer exactamente dos veces
            val pairCounts = exercise.cards.groupingBy { it.pairId }.eachCount()
            assertEquals("Debe haber exactamente $pairs parejas", pairs, pairCounts.size)
            pairCounts.forEach { (_, count) ->
                assertEquals("Cada pareja debe tener exactamente 2 cartas", 2, count)
            }

            // Las dos cartas de la misma pareja deben compartir el mismo pictograma
            for (p in 0 until pairs) {
                val pairCards = exercise.cards.filter { it.pairId == p }
                assertEquals(2, pairCards.size)
                assertEquals(pairCards[0].picto.id, pairCards[1].picto.id)
            }
        }
    }

    @Test
    fun `Modo 5 - BuildPhraseExercise provides target sentence and valid sequence`() {
        val exerciseEs = EducationalExerciseGenerator.generateBuildPhraseExercise(catalog, "ES")
        assertTrue(exerciseEs.targetSentence.isNotBlank())
        assertTrue("La frase objetivo debe requerir al menos 2 pictogramas", exerciseEs.targetPictos.size >= 2)

        // Todas las opciones disponibles deben incluir a todos los pictogramas objetivo
        val targetIds = exerciseEs.targetPictos.map { it.id }.toSet()
        val availableIds = exerciseEs.availableOptions.map { it.id }.toSet()
        assertTrue("Las opciones disponibles deben incluir todos los pictogramas requeridos", availableIds.containsAll(targetIds))
        assertTrue("Debe incluir distractores adicionales para practicar la discriminación", exerciseEs.availableOptions.size > exerciseEs.targetPictos.size)
    }

    @Test
    fun `LearningProgressManager tracks completions, hits, and per-mode stats locally`() {
        assertEquals(0, progressManager.getStats().totalCompleted)
        assertEquals(0, progressManager.getStats().totalHits)

        // Registrar aciertos y fallos en varios modos
        progressManager.recordResult(LearningActivityType.FIND_PICTOGRAM, isHit = true)
        progressManager.recordResult(LearningActivityType.FIND_PICTOGRAM, isHit = false)
        progressManager.recordResult(LearningActivityType.WHAT_MEANS, isHit = true)
        progressManager.recordResult(LearningActivityType.VISUAL_MEMORY, isHit = true)
        progressManager.recordResult(LearningActivityType.BUILD_PHRASE, isHit = true)

        val stats = progressManager.getStats()
        assertEquals(5, stats.totalCompleted)
        assertEquals(4, stats.totalHits)
        assertEquals(2, stats.completedByMode[LearningActivityType.FIND_PICTOGRAM])
        assertEquals(1, stats.completedByMode[LearningActivityType.WHAT_MEANS])
        assertEquals(1, stats.completedByMode[LearningActivityType.VISUAL_MEMORY])
        assertEquals(1, stats.completedByMode[LearningActivityType.BUILD_PHRASE])

        // Persistencia al crear nueva instancia con las mismas preferencias
        val reloadedManager = LearningProgressManager(fakePrefs)
        val reloadedStats = reloadedManager.getStats()
        assertEquals(5, reloadedStats.totalCompleted)
        assertEquals(4, reloadedStats.totalHits)

        // Reinicio
        reloadedManager.resetProgress()
        assertEquals(0, reloadedManager.getStats().totalCompleted)
        assertEquals(0, reloadedManager.getStats().totalHits)
    }
}
