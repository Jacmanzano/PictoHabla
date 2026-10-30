package com.cmanzano.pictohabla.feature.learning.model

import com.cmanzano.pictohabla.core.model.Category
import com.cmanzano.pictohabla.core.model.Pictogram

/**
 * Tipos de actividades educativas disponibles en el Modo Aprender.
 */
enum class LearningActivityType {
    FIND_PICTOGRAM,      // Actividad 1: Encuentra el pictograma
    MATCH_WORD,          // Actividad 2: Relaciona palabra e imagen
    CLASSIFY_CATEGORY,   // Actividad 3: Clasifica pictogramas
    GUIDED_PHRASES       // Frases guiadas (heredado de fases previas)
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
 * Ejercicio para la Actividad 1: Encuentra el pictograma a partir del nombre o locución.
 */
data class FindPictogramExercise(
    val targetPicto: Pictogram,
    val options: List<Pictogram>
)

/**
 * Ejercicio para la Actividad 2: Relaciona una palabra escrita con su pictograma.
 */
data class MatchWordExercise(
    val targetWord: String,
    val targetPicto: Pictogram,
    val options: List<Pictogram>
)

/**
 * Ejercicio para la Actividad 3: Clasifica un pictograma en su categoría semántica/gramatical.
 */
data class ClassifyExercise(
    val targetPicto: Pictogram,
    val correctCategory: Category,
    val options: List<Category>
)
