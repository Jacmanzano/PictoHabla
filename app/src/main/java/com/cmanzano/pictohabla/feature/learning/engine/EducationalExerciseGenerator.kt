package com.cmanzano.pictohabla.feature.learning.engine

import com.cmanzano.pictohabla.core.model.Category
import com.cmanzano.pictohabla.core.model.Pictogram
import com.cmanzano.pictohabla.data.local.DefaultCatalog
import com.cmanzano.pictohabla.feature.learning.model.BuildPhraseExercise
import com.cmanzano.pictohabla.feature.learning.model.ClassifyExercise
import com.cmanzano.pictohabla.feature.learning.model.FindPictogramExercise
import com.cmanzano.pictohabla.feature.learning.model.LearningConfig
import com.cmanzano.pictohabla.feature.learning.model.ListenExercise
import com.cmanzano.pictohabla.feature.learning.model.MatchWordExercise
import com.cmanzano.pictohabla.feature.learning.model.MemoryCard
import com.cmanzano.pictohabla.feature.learning.model.MemoryExercise
import com.cmanzano.pictohabla.feature.learning.model.WhatMeansExercise
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
     * Genera un ejercicio para la Actividad 1 / Modo 1: "Encuentra el pictograma".
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
     * Genera un ejercicio para el Modo 2: "¿Qué significa?".
     * Muestra un pictograma y varias palabras traducidas para identificar su significado.
     */
    fun generateWhatMeansExercise(
        allPictos: List<Pictogram>,
        languageCode: String,
        config: LearningConfig = LearningConfig(),
        random: Random = Random
    ): WhatMeansExercise {
        val catalog = resolveCatalog(allPictos)
        val pool = getFilteredPool(catalog, config.categoryFilter)

        val target = pool.random(random)
        val correctWord = target.getName(languageCode).ifBlank { target.getName("ES") }
        val requiredCount = config.optionsCount.coerceIn(2, 4)

        val distractorsPool = (pool.filter { it.id != target.id } + catalog.filter { it.id != target.id })
            .distinctBy { it.id }
            .map { it.getName(languageCode).ifBlank { it.getName("ES") } }
            .filter { it.isNotBlank() && !it.equals(correctWord, ignoreCase = true) }
            .distinct()

        val distractors = distractorsPool.shuffled(random).take((requiredCount - 1).coerceAtMost(distractorsPool.size))
        val options = (distractors + correctWord).shuffled(random)

        return WhatMeansExercise(
            targetPicto = target,
            correctWord = correctWord,
            options = options
        )
    }

    /**
     * Genera un ejercicio para el Modo 3: "Escucha y encuentra".
     * Reproduce una locución con TTS y el usuario elige entre varias tarjetas con pictogramas.
     */
    fun generateListenExercise(
        allPictos: List<Pictogram>,
        languageCode: String,
        config: LearningConfig = LearningConfig(),
        random: Random = Random
    ): ListenExercise {
        val catalog = resolveCatalog(allPictos)
        val pool = getFilteredPool(catalog, config.categoryFilter)

        val target = pool.random(random)
        val targetWord = target.getName(languageCode).ifBlank { target.getName("ES") }
        val requiredCount = config.optionsCount.coerceIn(2, 4)

        val distractorsPool = (pool.filter { it.id != target.id } + catalog.filter { it.id != target.id })
            .distinctBy { it.id }

        val distractors = distractorsPool.shuffled(random).take((requiredCount - 1).coerceAtMost(distractorsPool.size))
        val options = (distractors + target).shuffled(random)

        return ListenExercise(
            targetPicto = target,
            targetWord = targetWord,
            options = options
        )
    }

    /**
     * Genera un ejercicio para el Modo 4: "Memoria visual" (juego de parejas).
     * @param pairsCount Número de parejas a emparejar (2, 3 o 4 para dificultad adaptativa).
     */
    fun generateMemoryExercise(
        allPictos: List<Pictogram>,
        pairsCount: Int = 3,
        random: Random = Random
    ): MemoryExercise {
        val catalog = resolveCatalog(allPictos)
        val safePairsCount = pairsCount.coerceIn(2, 6)

        // Seleccionar pictogramas distintos para las parejas
        val selectedPictos = catalog.shuffled(random).take(safePairsCount)

        val cardList = mutableListOf<MemoryCard>()
        selectedPictos.forEachIndexed { pairIndex, picto ->
            cardList.add(MemoryCard(id = 0, pairId = pairIndex, picto = picto))
            cardList.add(MemoryCard(id = 0, pairId = pairIndex, picto = picto))
        }

        val shuffledCards = cardList.shuffled(random).mapIndexed { index, card ->
            card.copy(id = index)
        }

        return MemoryExercise(
            pairsCount = safePairsCount,
            cards = shuffledCards
        )
    }

    /**
     * Genera un ejercicio para el Modo 5: "Construye la frase".
     * Ofrece una frase objetivo comunicativa con sus pictogramas correspondientes y distractores.
     */
    fun generateBuildPhraseExercise(
        allPictos: List<Pictogram>,
        languageCode: String,
        random: Random = Random
    ): BuildPhraseExercise {
        val catalog = resolveCatalog(allPictos)
        val guidedPhrases = DefaultCatalog.getGuidedPhrases()

        // Seleccionamos una frase guiada representativa
        val selectedPhrase = guidedPhrases.random(random)
        val targetSentence = selectedPhrase.getTitle(languageCode)
        val targetPictos = selectedPhrase.pictos

        val targetIds = targetPictos.map { it.id }.toSet()
        val distractorCandidates = catalog.filter { it.id !in targetIds }
        val distractors = distractorCandidates.shuffled(random).take(2)

        val availableOptions = (targetPictos + distractors).shuffled(random)

        return BuildPhraseExercise(
            targetSentence = targetSentence,
            targetPictos = targetPictos,
            availableOptions = availableOptions
        )
    }

    /**
     * Genera un ejercicio para "Relaciona palabra e imagen".
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
     * Genera un ejercicio para la Actividad de "Clasifica pictogramas".
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
