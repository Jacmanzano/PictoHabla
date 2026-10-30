package com.cmanzano.pictohabla.feature.learning.activities

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmanzano.pictohabla.core.designsystem.PictoColors
import com.cmanzano.pictohabla.core.designsystem.PictoDimens
import com.cmanzano.pictohabla.core.designsystem.PictoHaptics
import com.cmanzano.pictohabla.core.designsystem.PictoTypography
import com.cmanzano.pictohabla.core.model.Pictogram
import com.cmanzano.pictohabla.core.util.ImageStorageHelper
import com.cmanzano.pictohabla.feature.board.components.PictogramImage
import com.cmanzano.pictohabla.feature.learning.components.ExerciseFeedbackBanner
import com.cmanzano.pictohabla.feature.learning.components.FeedbackState
import com.cmanzano.pictohabla.feature.learning.components.LearningSettingsDialog
import com.cmanzano.pictohabla.feature.learning.engine.EducationalExerciseGenerator
import com.cmanzano.pictohabla.feature.learning.model.FindPictogramExercise
import com.cmanzano.pictohabla.feature.learning.model.LearningConfig
import com.cmanzano.pictohabla.feature.settings.AdultGateDialog
import com.cmanzano.pictohabla.navigation.PictoBottomNavBar
import com.cmanzano.pictohabla.navigation.PictoScreen

/**
 * Actividad 1: "Encuentra el pictograma".
 * El usuario escucha o lee una palabra y debe seleccionar la imagen correcta entre varias opciones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindPictogramActivityScreen(
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
    val hapticFeedback = LocalHapticFeedback.current

    var config by remember { mutableStateOf(LearningConfig(optionsCount = 4, categoryFilter = null, autoSpeak = true)) }
    var showAdultGate by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    var exercise by remember {
        mutableStateOf(EducationalExerciseGenerator.generateFindExercise(allPictograms, config))
    }
    var selectedOptionId by remember { mutableStateOf<Int?>(null) }
    var feedbackState by remember { mutableStateOf<FeedbackState>(FeedbackState.None) }

    val targetName = remember(exercise, languageCode) {
        exercise.targetPicto.getName(languageCode).ifBlank { exercise.targetPicto.getName("ES") }
    }

    // Pronuncia la palabra al cargar un nuevo ejercicio si autoSpeak está activado
    LaunchedEffect(exercise) {
        selectedOptionId = null
        feedbackState = FeedbackState.None
        if (config.autoSpeak && targetName.isNotBlank()) {
            onSpeakWord(targetName)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Encuentra el pictograma",
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
                            showAdultGate = true
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Ajustes de dificultad para adultos"
                        }
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
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
                .padding(horizontal = PictoDimens.SpaceMedium, vertical = PictoDimens.SpaceSmall),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tarjeta de consigna e instrucción clara
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Instrucción: Encuentra $targetName. Toca el botón para escucharla."
                    },
                shape = PictoDimens.CornerLarge,
                colors = CardDefaults.cardColors(
                    containerColor = if (highContrast) Color(0xFF1E1E1E) else PictoColors.Primary.copy(alpha = 0.08f)
                ),
                border = BorderStroke(
                    width = if (highContrast) PictoDimens.BorderWidthHighContrast else 1.5.dp,
                    color = if (highContrast) PictoColors.HighlightHighContrast else PictoColors.Primary.copy(alpha = 0.3f)
                )
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
                            text = "¿Dónde está?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (highContrast) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = targetName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (highContrast) PictoColors.HighlightHighContrast else PictoColors.Primary
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            PictoHaptics.performEmphasis(hapticFeedback, hapticsEnabled)
                            onSpeakWord(targetName)
                        },
                        shape = PictoDimens.CornerMedium,
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Escuchar palabra $targetName"
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(PictoDimens.SpaceSmall))
                        Text("Escuchar", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(PictoDimens.SpaceMedium))

            // Cuadrícula de opciones de pictogramas
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = PictoDimens.SpaceSmall),
                horizontalArrangement = Arrangement.spacedBy(PictoDimens.SpaceMedium),
                verticalArrangement = Arrangement.spacedBy(PictoDimens.SpaceMedium)
            ) {
                items(exercise.options) { optionPicto ->
                    val isSelected = selectedOptionId == optionPicto.id
                    val isCorrect = optionPicto.id == exercise.targetPicto.id
                    val optionName = optionPicto.getName(languageCode)

                    val borderColor by animateColorAsState(
                        targetValue = when {
                            highContrast && isSelected && isCorrect -> PictoColors.HighlightHighContrast
                            isSelected && isCorrect -> Color(0xFF2E7D32)
                            isSelected && !isCorrect -> Color(0xFFFFA000)
                            highContrast -> PictoColors.BorderHighContrast
                            else -> optionPicto.category.borderColor
                        },
                        label = "card_border_anim"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .border(
                                width = if (isSelected) 3.5.dp else if (highContrast) PictoDimens.BorderWidthHighContrast else PictoDimens.BorderWidthStandard,
                                color = borderColor,
                                shape = PictoDimens.CornerMedium
                            )
                            .clickable {
                                PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                                selectedOptionId = optionPicto.id
                                if (isCorrect) {
                                    feedbackState = FeedbackState.Success("¡Muy bien! ¡Has encontrado $targetName!")
                                    onSpeakWord("¡Muy bien!")
                                } else {
                                    feedbackState = FeedbackState.TryAgain("Vamos a intentarlo de nuevo.")
                                    onSpeakWord(optionName)
                                }
                            }
                            .semantics {
                                role = Role.Button
                                contentDescription = "Opción $optionName"
                            },
                        shape = PictoDimens.CornerMedium,
                        colors = CardDefaults.cardColors(
                            containerColor = if (highContrast) Color.Black else optionPicto.category.color
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = PictoDimens.ElevationStandard)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(PictoDimens.SpaceSmall),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                PictogramImage(
                                    picto = optionPicto,
                                    highContrast = highContrast,
                                    modifier = Modifier.size(PictoDimens.IconSizeLarge)
                                )

                                Spacer(Modifier.height(PictoDimens.SpaceExtraSmall))

                                Text(
                                    text = optionName,
                                    style = PictoTypography.PictoLabelMedium,
                                    textAlign = TextAlign.Center,
                                    color = if (highContrast) Color.White else optionPicto.category.textColor,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Marca visual explícita de acierto (no solo color)
                            if (isSelected && isCorrect) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(PictoDimens.SpaceSmall),
                                    shape = PictoDimens.CornerSmall,
                                    color = Color(0xFF2E7D32)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Correcto",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Banner accesible de feedback positivo
            ExerciseFeedbackBanner(
                state = feedbackState,
                highContrast = highContrast,
                onNext = {
                    exercise = EducationalExerciseGenerator.generateFindExercise(allPictograms, config)
                },
                onRetry = {
                    selectedOptionId = null
                    feedbackState = FeedbackState.None
                }
            )
        }
    }

    // Puerta de adultos para acceder a ajustes
    if (showAdultGate) {
        AdultGateDialog(
            onDismiss = { showAdultGate = false },
            onSuccess = {
                showAdultGate = false
                showSettingsDialog = true
            }
        )
    }

    // Diálogo de configuración accesible
    if (showSettingsDialog) {
        LearningSettingsDialog(
            currentConfig = config,
            languageCode = languageCode,
            highContrast = highContrast,
            onDismiss = { showSettingsDialog = false },
            onSaveConfig = { newConfig ->
                config = newConfig
                showSettingsDialog = false
                exercise = EducationalExerciseGenerator.generateFindExercise(allPictograms, newConfig)
            }
        )
    }
}
