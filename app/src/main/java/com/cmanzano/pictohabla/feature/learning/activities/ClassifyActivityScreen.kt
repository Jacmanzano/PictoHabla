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
import com.cmanzano.pictohabla.core.model.Category
import com.cmanzano.pictohabla.core.model.Pictogram
import com.cmanzano.pictohabla.core.util.ImageStorageHelper
import com.cmanzano.pictohabla.feature.learning.components.ExerciseFeedbackBanner
import com.cmanzano.pictohabla.feature.learning.components.FeedbackState
import com.cmanzano.pictohabla.feature.learning.components.LearningSettingsDialog
import com.cmanzano.pictohabla.feature.learning.engine.EducationalExerciseGenerator
import com.cmanzano.pictohabla.feature.learning.model.LearningConfig
import com.cmanzano.pictohabla.feature.settings.AdultGateDialog
import com.cmanzano.pictohabla.navigation.PictoBottomNavBar
import com.cmanzano.pictohabla.navigation.PictoScreen

/**
 * Actividad 3: "Clasifica pictogramas".
 * Presenta un pictograma central y el usuario debe elegir a qué categoría pertenece
 * mediante botones amplios codificados con iconos, etiquetas y Clave de Fitzgerald.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassifyActivityScreen(
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
        mutableStateOf(EducationalExerciseGenerator.generateClassifyExercise(allPictograms, config))
    }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var feedbackState by remember { mutableStateOf<FeedbackState>(FeedbackState.None) }

    val pictoName = remember(exercise, languageCode) {
        exercise.targetPicto.getName(languageCode).ifBlank { exercise.targetPicto.getName("ES") }
    }
    val correctCategoryLabel = remember(exercise, languageCode) {
        exercise.correctCategory.getLabel(languageCode)
    }

    LaunchedEffect(exercise) {
        selectedCategory = null
        feedbackState = FeedbackState.None
        if (config.autoSpeak && pictoName.isNotBlank()) {
            onSpeakWord(pictoName)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Clasifica el pictograma",
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
            // Tarjeta destacada del pictograma a clasificar
            val localBitmap = remember(exercise.targetPicto.localImagePath) {
                ImageStorageHelper.loadBitmap(exercise.targetPicto.localImagePath)?.asImageBitmap()
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Pictograma a clasificar: $pictoName. ¿A qué categoría pertenece?"
                    },
                shape = PictoDimens.CornerLarge,
                colors = CardDefaults.cardColors(
                    containerColor = if (highContrast) Color(0xFF1E1E1E) else exercise.targetPicto.category.color.copy(alpha = 0.4f)
                ),
                border = BorderStroke(
                    width = if (highContrast) PictoDimens.BorderWidthHighContrast else 2.dp,
                    color = if (highContrast) PictoColors.HighlightHighContrast else exercise.targetPicto.category.borderColor
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PictoDimens.SpaceMedium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = PictoDimens.CornerMedium,
                            color = if (highContrast) Color.Black else exercise.targetPicto.category.color,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (highContrast) Color.White else exercise.targetPicto.category.borderColor
                            ),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                val targetIcon = exercise.targetPicto.icon
                                if (localBitmap != null) {
                                    Image(
                                        bitmap = localBitmap,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (targetIcon != null) {
                                    Icon(
                                        imageVector = targetIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(44.dp),
                                        tint = if (highContrast) Color.White else exercise.targetPicto.category.textColor
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(44.dp),
                                        tint = if (highContrast) Color.White else exercise.targetPicto.category.textColor
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.width(PictoDimens.SpaceMedium))

                        Column {
                            Text(
                                text = "¿Qué tipo de palabra es?",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (highContrast) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = pictoName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (highContrast) PictoColors.HighlightHighContrast else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            PictoHaptics.performEmphasis(hapticFeedback, hapticsEnabled)
                            onSpeakWord(pictoName)
                        },
                        shape = PictoDimens.CornerMedium,
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Escuchar pictograma $pictoName"
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(Modifier.height(PictoDimens.SpaceMedium))

            Text(
                text = "Selecciona su categoría:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PictoDimens.SpaceSmall)
            )

            Spacer(Modifier.height(PictoDimens.SpaceSmall))

            // Cuadrícula de categorías disponibles para clasificar
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = PictoDimens.SpaceSmall),
                horizontalArrangement = Arrangement.spacedBy(PictoDimens.SpaceMedium),
                verticalArrangement = Arrangement.spacedBy(PictoDimens.SpaceMedium)
            ) {
                items(exercise.options) { categoryOption ->
                    val isSelected = selectedCategory == categoryOption
                    val isCorrect = categoryOption == exercise.correctCategory
                    val categoryLabel = categoryOption.getLabel(languageCode)

                    val borderColor by animateColorAsState(
                        targetValue = when {
                            highContrast && isSelected && isCorrect -> PictoColors.HighlightHighContrast
                            isSelected && isCorrect -> Color(0xFF2E7D32)
                            isSelected && !isCorrect -> Color(0xFFFFA000)
                            highContrast -> PictoColors.BorderHighContrast
                            else -> categoryOption.borderColor
                        },
                        label = "cat_border_anim"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .border(
                                width = if (isSelected) 3.5.dp else if (highContrast) PictoDimens.BorderWidthHighContrast else PictoDimens.BorderWidthStandard,
                                color = borderColor,
                                shape = PictoDimens.CornerMedium
                            )
                            .clickable {
                                PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                                selectedCategory = categoryOption
                                if (isCorrect) {
                                    feedbackState = FeedbackState.Success("¡Genial! $pictoName es de la categoría $categoryLabel.")
                                    onSpeakWord("¡Correcto! Es $categoryLabel")
                                } else {
                                    feedbackState = FeedbackState.TryAgain("Vamos a intentarlo de nuevo.")
                                    onSpeakWord(categoryLabel)
                                }
                            }
                            .semantics {
                                role = Role.Button
                                contentDescription = "Categoría $categoryLabel"
                            },
                        shape = PictoDimens.CornerMedium,
                        colors = CardDefaults.cardColors(
                            containerColor = if (highContrast) Color.Black else categoryOption.color
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
                                Icon(
                                    imageVector = categoryOption.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = if (highContrast) Color.White else categoryOption.textColor
                                )

                                Spacer(Modifier.height(PictoDimens.SpaceExtraSmall))

                                Text(
                                    text = categoryLabel,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = if (highContrast) Color.White else categoryOption.textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (isSelected && isCorrect) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(PictoDimens.SpaceExtraSmall),
                                    shape = PictoDimens.CornerSmall,
                                    color = Color(0xFF2E7D32)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Correcto",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            ExerciseFeedbackBanner(
                state = feedbackState,
                highContrast = highContrast,
                onNext = {
                    exercise = EducationalExerciseGenerator.generateClassifyExercise(allPictograms, config)
                },
                onRetry = {
                    selectedCategory = null
                    feedbackState = FeedbackState.None
                }
            )
        }
    }

    if (showAdultGate) {
        AdultGateDialog(
            onDismiss = { showAdultGate = false },
            onSuccess = {
                showAdultGate = false
                showSettingsDialog = true
            }
        )
    }

    if (showSettingsDialog) {
        LearningSettingsDialog(
            currentConfig = config,
            languageCode = languageCode,
            highContrast = highContrast,
            onDismiss = { showSettingsDialog = false },
            onSaveConfig = { newConfig ->
                config = newConfig
                showSettingsDialog = false
                exercise = EducationalExerciseGenerator.generateClassifyExercise(allPictograms, newConfig)
            }
        )
    }
}
