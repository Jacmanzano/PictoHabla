package com.cmanzano.pictohabla.feature.learning.activities

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.cmanzano.pictohabla.feature.learning.model.BuildPhraseExercise
import com.cmanzano.pictohabla.feature.learning.model.LearningActivityType
import com.cmanzano.pictohabla.navigation.PictoBottomNavBar
import com.cmanzano.pictohabla.navigation.PictoScreen

/**
 * Modo 5: "Construye la frase".
 * Presenta una frase objetivo y permite ordenar pictogramas con pistas visuales de la clave de Fitzgerald.
 * Permite corregir o retirar pictogramas colocados individualmente sin necesidad de reiniciar la frase completa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildPhraseActivityScreen(
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

    var exercise by remember {
        mutableStateOf(EducationalExerciseGenerator.generateBuildPhraseExercise(allPictograms, languageCode))
    }

    val selectedSlotPictos = remember { mutableStateListOf<Pictogram?>() }
    var feedbackState by remember { mutableStateOf<FeedbackState>(FeedbackState.None) }

    fun initializeSlots(targetCount: Int) {
        selectedSlotPictos.clear()
        repeat(targetCount) {
            selectedSlotPictos.add(null)
        }
    }

    LaunchedEffect(exercise) {
        initializeSlots(exercise.targetPictos.size)
        feedbackState = FeedbackState.None
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (languageCode == "EN") "Build the Sentence" else "Construye la frase",
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
                actions = {
                    IconButton(
                        onClick = {
                            PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                            initializeSlots(exercise.targetPictos.size)
                            feedbackState = FeedbackState.None
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Limpiar ranuras de la frase"
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = PictoDimens.SpaceLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(PictoDimens.SpaceMedium))

            // Frase objetivo con botón para escuchar
            Card(
                shape = PictoDimens.CornerMedium,
                colors = CardDefaults.cardColors(
                    containerColor = if (highContrast) Color.Black else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (highContrast) Color.White else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PictoDimens.SpaceMedium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (languageCode == "EN") "Target sentence:" else "Frase objetivo:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "\"${exercise.targetSentence}\"",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (highContrast) PictoColors.HighlightHighContrast else PictoColors.Primary
                        )
                    }
                    IconButton(
                        onClick = {
                            PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                            onSpeakWord(exercise.targetSentence)
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Escuchar frase objetivo"
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = if (highContrast) Color.White else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(PictoDimens.SpaceLarge))

            // Ranuras de la frase en construcción (con pistas de color de la clave de Fitzgerald)
            Text(
                text = if (languageCode == "EN") "Place the pictograms in order (tap to remove):" else "Coloca los pictogramas en orden (toca para quitar):",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(PictoDimens.SpaceSmall))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                contentPadding = PaddingValues(horizontal = PictoDimens.SpaceSmall)
            ) {
                items(selectedSlotPictos.indices.toList()) { slotIndex ->
                    val pictoInSlot = selectedSlotPictos[slotIndex]
                    val expectedCategory = exercise.targetPictos.getOrNull(slotIndex)?.category

                    Card(
                        shape = PictoDimens.CornerMedium,
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                pictoInSlot != null -> if (highContrast) Color.Black else pictoInSlot.category.color
                                else -> if (highContrast) Color(0xFF1A1A1A) else Color(0xFFF5F5F5)
                            }
                        ),
                        border = BorderStroke(
                            width = 2.dp,
                            color = when {
                                pictoInSlot != null -> if (highContrast) Color.White else pictoInSlot.category.borderColor
                                expectedCategory != null -> if (highContrast) Color.Gray else expectedCategory.borderColor
                                else -> Color.LightGray
                            }
                        ),
                        modifier = Modifier
                            .size(88.dp)
                            .padding(4.dp)
                            .clickable(enabled = pictoInSlot != null) {
                                PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                                selectedSlotPictos[slotIndex] = null
                                feedbackState = FeedbackState.None
                            }
                            .semantics {
                                role = Role.Button
                                contentDescription = if (pictoInSlot != null) {
                                    "Ranura ${slotIndex + 1}: ${pictoInSlot.getName(languageCode)}. Toca para quitar."
                                } else {
                                    "Ranura ${slotIndex + 1} vacía. Tipo esperado: ${expectedCategory?.getLabel(languageCode) ?: ""}"
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (pictoInSlot != null) {
                                PictogramImage(
                                    picto = pictoInSlot,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color.Red.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.TopEnd)
                                )
                            } else {
                                Text(
                                    text = "${slotIndex + 1}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(PictoDimens.SpaceMedium))

            // Botón de comprobar
            val allFilled = selectedSlotPictos.all { it != null }
            Button(
                onClick = {
                    PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                    val currentIds = selectedSlotPictos.mapNotNull { it?.id }
                    val targetIds = exercise.targetPictos.map { it.id }

                    if (currentIds == targetIds) {
                        PictoHaptics.performEmphasis(hapticFeedback, hapticsEnabled)
                        onSpeakWord(exercise.targetSentence)
                        feedbackState = FeedbackState.Success(
                            if (languageCode == "EN") "Perfect! You built the sentence!" else "¡Perfecto! ¡Has construido la frase correctamente!"
                        )
                        progressManager.recordResult(LearningActivityType.BUILD_PHRASE, isHit = true)
                    } else {
                        PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                        feedbackState = FeedbackState.TryAgain(
                            if (languageCode == "EN") "Some pictograms are not in the right order. Try adjusting them!" else "El orden no es correcto. Toca los pictogramas que quieras cambiar."
                        )
                        progressManager.recordResult(LearningActivityType.BUILD_PHRASE, isHit = false)
                    }
                },
                enabled = allFilled,
                shape = PictoDimens.CornerMedium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (highContrast) Color(0xFF2E7D32) else PictoColors.Success
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (languageCode == "EN") "Check Sentence" else "Comprobar frase",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(PictoDimens.SpaceMedium))
            HorizontalDivider()
            Spacer(Modifier.height(PictoDimens.SpaceSmall))

            // Opciones disponibles para elegir
            Text(
                text = if (languageCode == "EN") "Available Pictograms:" else "Pictogramas disponibles:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(PictoDimens.SpaceSmall))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PictoDimens.SpaceSmall),
                contentPadding = PaddingValues(horizontal = PictoDimens.SpaceSmall)
            ) {
                items(exercise.availableOptions) { picto ->
                    // Ver si ya está en una ranura
                    val alreadyUsedCount = selectedSlotPictos.count { it?.id == picto.id }
                    val targetOccurrences = exercise.targetPictos.count { it.id == picto.id }
                    val canAdd = alreadyUsedCount < targetOccurrences.coerceAtLeast(1)

                    Card(
                        shape = PictoDimens.CornerMedium,
                        colors = CardDefaults.cardColors(
                            containerColor = if (highContrast) Color.Black else picto.category.color
                        ),
                        border = BorderStroke(
                            width = 2.dp,
                            color = if (canAdd) {
                                if (highContrast) Color.White else picto.category.borderColor
                            } else Color.LightGray.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .size(76.dp)
                            .clickable(enabled = canAdd) {
                                PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                                // Encontrar la primera ranura vacía
                                val firstEmptyIndex = selectedSlotPictos.indexOfFirst { it == null }
                                if (firstEmptyIndex != -1) {
                                    selectedSlotPictos[firstEmptyIndex] = picto
                                    feedbackState = FeedbackState.None
                                }
                            }
                            .semantics {
                                role = Role.Button
                                contentDescription = "Añadir: ${picto.getName(languageCode)}"
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            PictogramImage(
                                picto = picto,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Banner de retroalimentación
            ExerciseFeedbackBanner(
                state = feedbackState,
                highContrast = highContrast,
                onNext = {
                    exercise = EducationalExerciseGenerator.generateBuildPhraseExercise(allPictograms, languageCode)
                },
                onRetry = {
                    feedbackState = FeedbackState.None
                }
            )

            Spacer(Modifier.height(PictoDimens.SpaceSmall))
        }
    }
}
