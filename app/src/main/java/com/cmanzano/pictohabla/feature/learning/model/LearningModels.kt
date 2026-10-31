package com.cmanzano.pictohabla.feature.learning.model

import com.cmanzano.pictohabla.core.model.Category
import com.cmanzano.pictohabla.core.model.Pictogram

/**
 * Tipos de actividades educativas disponibles en el Modo Aprender de PictoHabla.
 */
enum class LearningActivityType {
    FIND_PICTOGRAM,      // Modo 1: Encuentra el pictograma
    WHAT_MEANS,          // Modo 2: ¿Qué significa?
    LISTEN_AND_FIND,     // Modo 3: Escucha y encuentra
    VISUAL_MEMORY,       // Modo 4: Memoria visual
    BUILD_PHRASE,        // Modo 5: Construye la frase
    CLASSIFY_CATEGORY,   // Clasifica por categoría
    MATCH_WORD,          // Relaciona palabra e imagen
    GUIDED_PHRASES       // Frases guiadas
}

/**
 * Configuración accesible del Modo Aprender para adultos y cuidadores.
 *
 * @property optionsCount Número de opciones a mostrar (2, 3 o 4 para adaptar la dificultad cognitiva/motora).
 * @property categoryFilter Filtro opcional por categoría específica (null = todo el catálogo).
 * @property autoSpeak Reproduce automáticamente el audio TTS al cargar una nueva pregunta.
 */
data class LearningConfig(
    val optionsCount: Int = 4,
    val categoryFilter: Category? = null,
    val autoSpeak: Boolean = true
)

/**
 * Ejercicio para el Modo 1: Encuentra el pictograma a partir de una palabra o concepto.
 */
data class FindPictogramExercise(
    val targetPicto: Pictogram,
    val options: List<Pictogram>
)

/**
 * Ejercicio para el Modo 2: ¿Qué significa? (Dado un pictograma, elige la palabra traducida correcta).
 */
data class WhatMeansExercise(
    val targetPicto: Pictogram,
    val correctWord: String,
    val options: List<String>
)

/**
 * Ejercicio para el Modo 3: Escucha y encuentra (Escucha mediante TTS y selecciona el pictograma).
 */
data class ListenExercise(
    val targetPicto: Pictogram,
    val targetWord: String,
    val options: List<Pictogram>
)

/**
 * Carta individual para el Modo 4: Memoria visual.
 */
data class MemoryCard(
    val id: Int,
    val pairId: Int,
    val picto: Pictogram,
    val isFaceUp: Boolean = false,
    val isMatched: Boolean = false
)

/**
 * Ejercicio para el Modo 4: Memoria visual (Tablero de parejas de cartas boca abajo).
 */
data class MemoryExercise(
    val pairsCount: Int,
    val cards: List<MemoryCard>
)

/**
 * Ejercicio para el Modo 5: Construye la frase (Ordena pictogramas para formar una expresión comunicativa).
 */
data class BuildPhraseExercise(
    val targetSentence: String,
    val targetPictos: List<Pictogram>,
    val availableOptions: List<Pictogram>
)

/**
 * Ejercicio para la clasificación por categoría semántica/gramatical.
 */
data class ClassifyExercise(
    val targetPicto: Pictogram,
    val correctCategory: Category,
    val options: List<Category>
)

/**
 * Ejercicio para relacionar palabra e imagen.
 */
data class MatchWordExercise(
    val targetWord: String,
    val targetPicto: Pictogram,
    val options: List<Pictogram>
)

/**
 * Estadísticas locales de aprendizaje y progreso.
 */
data class LearningStats(
    val totalCompleted: Int = 0,
    val totalHits: Int = 0,
    val completedByMode: Map<LearningActivityType, Int> = emptyMap()
)
