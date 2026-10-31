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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hearing
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.cmanzano.pictohabla.navigation.PictoBottomNavBar
import com.cmanzano.pictohabla.navigation.PictoScreen

/**
 * Modo 3: "Escucha y encuentra".
 * Reproduce una palabra con el sintetizador de voz (TTS) y el usuario debe elegir la imagen que escuchó.
 * Ofrece soporte accesible para reproducir tantas veces como se desee y pista visual si el TTS falla o no está disponible.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenActivityScreen(
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
            EducationalExerciseGenerator.generateListenExercise(
                allPictos = allPictograms,
                languageCode = languageCode,
                config = LearningConfig(optionsCount = optionsCount)
            )
        )
    }

    var selectedOptionId by remember { mutableStateOf<Int?>(null) }
    var feedbackState by remember { mutableStateOf<FeedbackState>(FeedbackState.None) }
    var showVisualHint by remember { mutableStateOf(false) }

    // Reproduce el audio automáticamente al generar un ejercicio nuevo
    LaunchedEffect(exercise) {
        selectedOptionId = null
        feedbackState = FeedbackState.None
        showVisualHint = false
        if (exercise.targetWord.isNotBlank()) {
            onSpeakWord(exercise.targetWord)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (languageCode == "EN") "Listen and Find" else "Escucha y encuentra",
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = PictoDimens.SpaceLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(PictoDimens.SpaceSmall))

            // Selector de opciones / dificultad
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
                            exercise = EducationalExerciseGenerator.generateListenExercise(
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

            Spacer(Modifier.height(PictoDimens.SpaceSmall))

            // Botón principal accesible para escuchar / repetir
            Button(
                onClick = {
                    PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                    onSpeakWord(exercise.targetWord)
                },
                shape = PictoDimens.CornerMedium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (highContrast) Color(0xFF1E88E5) else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Escuchar palabra objetivo otra vez"
                    }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.size(PictoDimens.SpaceMedium))
                Text(
                    text = if (languageCode == "EN") "Listen again" else "Escuchar de nuevo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Pista visual opcional en caso de que el motor TTS no funcione o el usuario lo requiera
            if (showVisualHint) {
                Spacer(Modifier.height(PictoDimens.SpaceExtraSmall))
                Text(
                    text = "Palabra: \"${exercise.targetWord}\"",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                TextButton(onClick = { showVisualHint = true }) {
                    Text(
                        text = "¿No se oye? Mostrar palabra escrita",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(PictoDimens.SpaceSmall))

            // Cuadrícula de opciones con pictogramas reales
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (optionsCount == 2) 2 else 2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = PictoDimens.SpaceSmall),
                horizontalArrangement = Arrangement.spacedBy(PictoDimens.SpaceMedium),
                verticalArrangement = Arrangement.spacedBy(PictoDimens.SpaceMedium)
            ) {
                items(exercise.options) { picto ->
                    val isSelected = selectedOptionId == picto.id
                    val isCorrect = picto.id == exercise.targetPicto.id

                    val borderColor by animateColorAsState(
                        targetValue = when {
                            isSelected && isCorrect -> PictoColors.Success
                            isSelected && !isCorrect -> PictoColors.Error
                            else -> if (highContrast) Color.White else picto.category.borderColor
                        },
                        label = "picto_border_color"
                    )

                    Card(
                        shape = PictoDimens.CornerMedium,
                        colors = CardDefaults.cardColors(
                            containerColor = if (highContrast) Color.Black else picto.category.color
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 3.5.dp else 2.dp,
                            color = borderColor
                        ),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable {
                                PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                                selectedOptionId = picto.id
                                if (isCorrect) {
                                    PictoHaptics.performEmphasis(hapticFeedback, hapticsEnabled)
                                    feedbackState = FeedbackState.Success(
                                        if (languageCode == "EN") "Correct! You found it!" else "¡Correcto! ¡Lo has encontrado!"
                                    )
                                    progressManager.recordResult(LearningActivityType.LISTEN_AND_FIND, isHit = true)
                                } else {
                                    PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                                    feedbackState = FeedbackState.TryAgain(
                                        if (languageCode == "EN") "Listen again and try!" else "¡Casi! Escucha otra vez e inténtalo."
                                    )
                                    progressManager.recordResult(LearningActivityType.LISTEN_AND_FIND, isHit = false)
                                }
                            }
                            .semantics {
                                role = Role.Button
                                contentDescription = "Opción: ${picto.getName(languageCode)}"
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(PictoDimens.SpaceSmall),
                            contentAlignment = Alignment.Center
                        ) {
                            PictogramImage(
                                picto = picto,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (isSelected && isCorrect) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = PictoColors.Success,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Banner de retroalimentación constructiva
            ExerciseFeedbackBanner(
                state = feedbackState,
                highContrast = highContrast,
                onNext = {
                    exercise = EducationalExerciseGenerator.generateListenExercise(
                        allPictos = allPictograms,
                        languageCode = languageCode,
                        config = LearningConfig(optionsCount = optionsCount)
                    )
                },
                onRetry = {
                    selectedOptionId = null
                    feedbackState = FeedbackState.None
                    onSpeakWord(exercise.targetWord)
                }
            )

            Spacer(Modifier.height(PictoDimens.SpaceSmall))
        }
    }
}
