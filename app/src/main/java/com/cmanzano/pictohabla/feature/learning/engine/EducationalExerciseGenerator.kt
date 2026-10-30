package com.cmanzano.pictohabla.feature.learning.engine

import com.cmanzano.pictohabla.core.model.Category
import com.cmanzano.pictohabla.core.model.Pictogram
import com.cmanzano.pictohabla.data.local.DefaultCatalog
import com.cmanzano.pictohabla.feature.learning.model.ClassifyExercise
import com.cmanzano.pictohabla.feature.learning.model.FindPictogramExercise
import com.cmanzano.pictohabla.feature.learning.model.LearningConfig
import com.cmanzano.pictohabla.feature.learning.model.MatchWordExercise
import kotlin.random.Random

/**
 * Motor generador de ejercicios educativos para PictoHabla.
 * Opera 100% offline, garantiza la ausencia de opciones duplicadas,
 * maneja catálogos reducidos con degradación elegante y preserva la coherencia semántica.
 */
object EducationalExerciseGenerator {

    /**
     * Resuelve el catálogo base a utilizar, recurriendo al catálogo por defecto si la lista está vacía.
     */
    fun resolveCatalog(provided: List<Pictogram>): List<Pictogram> {
        return if (provided.isNotEmpty()) provided else DefaultCatalog.getFullCatalog()
    }

    /**
     * Filtra el catálogo por categoría si el filtro está activo y tiene elementos suficientes (al menos 2).
     * Si no tiene suficientes, recurre al catálogo general para no bloquear al usuario.
     */
    fun getFilteredPool(allPictos: List<Pictogram>, categoryFilter: Category?): List<Pictogram> {
        val base = resolveCatalog(allPictos)
        if (categoryFilter == null) return base

        val filtered = base.filter { it.category == categoryFilter }
        return if (filtered.size >= 2) filtered else base
    }

    /**
     * Genera un ejercicio para la Actividad 1: "Encuentra el pictograma".
     */
    fun generateFindExercise(
        allPictos: List<Pictogram>,
        config: LearningConfig = LearningConfig(),
        random: Random = Random
    ): FindPictogramExercise {
        val catalog = resolveCatalog(allPictos)
        val pool = getFilteredPool(catalog, config.categoryFilter)

        val target = pool.random(random)
        val requiredCount = config.optionsCount.coerceIn(2, 4)

        // Candidatos distractores sin el objetivo
        val distractorsPool = (pool.filter { it.id != target.id } + catalog.filter { it.id != target.id })
            .distinctBy { it.id }

        val distractors = distractorsPool.shuffled(random).take((requiredCount - 1).coerceAtMost(distractorsPool.size))
        val options = (distractors + target).shuffled(random)

        return FindPictogramExercise(
            targetPicto = target,
            options = options
        )
    }

    /**
     * Genera un ejercicio para la Actividad 2: "Relaciona palabra e imagen".
     */
    fun generateMatchWordExercise(
        allPictos: List<Pictogram>,
        languageCode: String,
        config: LearningConfig = LearningConfig(),
        random: Random = Random
    ): MatchWordExercise {
        val catalog = resolveCatalog(allPictos)
        val pool = getFilteredPool(catalog, config.categoryFilter)

        val target = pool.random(random)
        val targetWord = target.getName(languageCode).ifBlank { target.getName("ES") }
        val requiredCount = config.optionsCount.coerceIn(2, 4)

        val distractorsPool = (pool.filter { it.id != target.id } + catalog.filter { it.id != target.id })
            .distinctBy { it.id }

        val distractors = distractorsPool.shuffled(random).take((requiredCount - 1).coerceAtMost(distractorsPool.size))
        val options = (distractors + target).shuffled(random)

        return MatchWordExercise(
            targetWord = targetWord,
            targetPicto = target,
            options = options
        )
    }

    /**
     * Genera un ejercicio para la Actividad 3: "Clasifica pictogramas".
     */
    fun generateClassifyExercise(
        allPictos: List<Pictogram>,
        config: LearningConfig = LearningConfig(),
        random: Random = Random
    ): ClassifyExercise {
        val catalog = resolveCatalog(allPictos)
        val pool = getFilteredPool(catalog, config.categoryFilter)

        val target = pool.random(random)
        val correctCategory = target.category
        val requiredCount = config.optionsCount.coerceIn(2, 4)

        val allCategories = Category.entries.toList()
        val otherCategories = allCategories.filter { it != correctCategory }
        val distractorCategories = otherCategories.shuffled(random).take(requiredCount - 1)
        val options = (distractorCategories + correctCategory).shuffled(random)

        return ClassifyExercise(
            targetPicto = target,
            correctCategory = correctCategory,
            options = options
        )
    }
}
