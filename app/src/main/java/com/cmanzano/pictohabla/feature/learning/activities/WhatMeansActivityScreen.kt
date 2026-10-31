package com.cmanzano.pictohabla.feature.learning.activities

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmanzano.pictohabla.core.designsystem.PictoColors
import com.cmanzano.pictohabla.core.designsystem.PictoDimens
import com.cmanzano.pictohabla.core.designsystem.PictoHaptics
import com.cmanzano.pictohabla.core.model.Pictogram
import com.cmanzano.pictohabla.feature.board.components.PictogramImage
import com.cmanzano.pictohabla.feature.learning.LearningProgressManager
import com.cmanzano.pictohabla.feature.learning.components.ExerciseFeedbackBanner
import com.cmanzano.pictohabla.feature.learning.components.FeedbackState
import com.cmanzano.pictohabla.feature.learning.engine.EducationalExerciseGenerator
import com.cmanzano.pictohabla.feature.learning.model.LearningActivityType
import com.cmanzano.pictohabla.feature.learning.model.LearningConfig
import com.cmanzano.pictohabla.feature.learning.model.WhatMeansExercise
import com.cmanzano.pictohabla.navigation.PictoBottomNavBar
import com.cmanzano.pictohabla.navigation.PictoScreen

/**
 * Modo 2: "¿Qué significa?".
 * Muestra un pictograma centralizado y varias opciones de palabras para que el usuario identifique su significado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatMeansActivityScreen(
    allPictograms: List<Pictogram>,
    languageCode: String,
    highContrast: Boolean = false,
    hapticsEnabled: Boolean = true,
    onSpeakWord: (String) -> Unit,
    onBackToHub: () -> Unit,
    onNavigateTablero: () -> Unit,
    onNavigateFavorites: () -> Unit,
    onNavigateHistory: () -> Unit
) {
    val context = LocalContext.current
    val progressManager = remember(context) { LearningProgressManager.getInstance(context) }
    val hapticFeedback = LocalHapticFeedback.current

    var optionsCount by remember { mutableIntStateOf(3) }
    var exercise by remember {
        mutableStateOf(
            EducationalExerciseGenerator.generateWhatMeansExercise(
                allPictos = allPictograms,
                languageCode = languageCode,
                config = LearningConfig(optionsCount = optionsCount)
            )
        )
    }

    var selectedWord by remember { mutableStateOf<String?>(null) }
    var feedbackState by remember { mutableStateOf<FeedbackState>(FeedbackState.None) }

    LaunchedEffect(exercise) {
        selectedWord = null
        feedbackState = FeedbackState.None
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (languageCode == "EN") "What does it mean?" else "¿Qué significa?",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                            onBackToHub()
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Volver al menú de actividades de aprendizaje"
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (highContrast) Color.Black else MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            PictoBottomNavBar(
                currentScreen = PictoScreen.LEARN,
                onNavigateTablero = onNavigateTablero,
                onNavigateFavorites = onNavigateFavorites,
                onNavigateHistory = onNavigateHistory,
                onNavigateLearn = onBackToHub,
                highContrast = highContrast,
                hapticsEnabled = hapticsEnabled
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = PictoDimens.SpaceLarge),
            contentPadding = PaddingValues(vertical = PictoDimens.SpaceMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PictoDimens.SpaceMedium)
        ) {
            // Selector de dificultad accesible
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Opciones: ",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf(2, 3, 4).forEach { count ->
                        FilterChip(
                            selected = optionsCount == count,
                            onClick = {
                                optionsCount = count
                                exercise = EducationalExerciseGenerator.generateWhatMeansExercise(
                                    allPictos = allPictograms,
                                    languageCode = languageCode,
                                    config = LearningConfig(optionsCount = count)
                                )
                            },
                            label = { Text("$count") },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (highContrast) Color.White else MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = if (highContrast) Color.Black else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // Pictograma objetivo mostrado en grande
            item {
                Card(
                    shape = PictoDimens.CornerLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = if (highContrast) Color.Black else exercise.targetPicto.category.color
                    ),
                    border = BorderStroke(
                        width = if (highContrast) 3.dp else 2.dp,
                        color = if (highContrast) Color.White else exercise.targetPicto.category.borderColor
                    ),
                    modifier = Modifier
                        .size(160.dp)
                        .semantics {
                            contentDescription = "Pictograma a identificar"
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(PictoDimens.SpaceMedium),
                        contentAlignment = Alignment.Center
                    ) {
                        PictogramImage(
                            picto = exercise.targetPicto,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Pregunta orientadora
            item {
                Text(
                    text = if (languageCode == "EN") "Select the correct word:" else "Elige la palabra correcta:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (highContrast) PictoColors.HighlightHighContrast else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }

            // Opciones de palabras
            items(exercise.options) { word ->
                val isSelected = selectedWord == word
                val isCorrect = word.equals(exercise.correctWord, ignoreCase = true)

                val backgroundColor by animateColorAsState(
                    targetValue = when {
                        isSelected && isCorrect -> if (highContrast) Color(0xFF1B5E20) else Color(0xFFC8E6C9)
                        isSelected && !isCorrect -> if (highContrast) Color(0xFFB71C1C) else Color(0xFFFFCDD2)
                        else -> if (highContrast) Color(0xFF222222) else MaterialTheme.colorScheme.surface
                    },
                    label = "card_color_anim"
                )

                Surface(
                    onClick = {
                        PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                        selectedWord = word
                        if (isCorrect) {
                            PictoHaptics.performEmphasis(hapticFeedback, hapticsEnabled)
                            onSpeakWord(word)
                            feedbackState = FeedbackState.Success(
                                if (languageCode == "EN") "Great job! It means $word" else "¡Muy bien! Significa $word"
                            )
                            progressManager.recordResult(LearningActivityType.WHAT_MEANS, isHit = true)
                        } else {
                            PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                            feedbackState = FeedbackState.TryAgain(
                                if (languageCode == "EN") "Try another word!" else "¡Casi! Prueba otra palabra."
                            )
                            progressManager.recordResult(LearningActivityType.WHAT_MEANS, isHit = false)
                        }
                    },
                    shape = PictoDimens.CornerMedium,
                    color = backgroundColor,
                    border = BorderStroke(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = when {
                            isSelected && isCorrect -> PictoColors.Success
                            isSelected && !isCorrect -> PictoColors.Error
                            else -> if (highContrast) Color.White else MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = PictoDimens.TouchTargetMinimum)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Opción: $word"
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PictoDimens.SpaceMedium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = word,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (highContrast) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected && isCorrect) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PictoColors.Success
                            )
                        }
                    }
                }
            }

            // Banner de retroalimentación
            item {
                ExerciseFeedbackBanner(
                    state = feedbackState,
                    highContrast = highContrast,
                    onNext = {
                        exercise = EducationalExerciseGenerator.generateWhatMeansExercise(
                            allPictos = allPictograms,
                            languageCode = languageCode,
                            config = LearningConfig(optionsCount = optionsCount)
                        )
                    },
                    onRetry = {
                        selectedWord = null
                        feedbackState = FeedbackState.None
                    }
                )
            }
        }
    }
}
