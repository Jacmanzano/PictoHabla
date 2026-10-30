package com.cmanzano.pictohabla.feature.learning

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.cmanzano.pictohabla.core.util.processPhraseForSpeech
import com.cmanzano.pictohabla.data.local.DefaultCatalog
import com.cmanzano.pictohabla.feature.learning.activities.ClassifyActivityScreen
import com.cmanzano.pictohabla.feature.learning.activities.FindPictogramActivityScreen
import com.cmanzano.pictohabla.feature.learning.activities.MatchWordActivityScreen
import com.cmanzano.pictohabla.feature.learning.components.LearningSettingsDialog
import com.cmanzano.pictohabla.feature.learning.model.LearningActivityType
import com.cmanzano.pictohabla.feature.learning.model.LearningConfig
import com.cmanzano.pictohabla.feature.settings.AdultGateDialog
import com.cmanzano.pictohabla.navigation.PictoBottomNavBar
import com.cmanzano.pictohabla.navigation.PictoScreen

/**
 * Pantalla principal del Modo Aprender y Actividades Educativas (FASE 5).
 * Coordina las tres actividades pedagógicas centrales:
 * 1. Encuentra el pictograma
 * 2. Relaciona palabra e imagen
 * 3. Clasifica pictogramas
 * y preserva la sección de frases guiadas heredada de fases previas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningScreen(
    allPictograms: List<Pictogram> = emptyList(),
    languageCode: String,
    highContrast: Boolean = false,
    hapticsEnabled: Boolean = true,
    onSpeakWord: (String) -> Unit,
    onBack: () -> Unit,
    onNavigateTablero: () -> Unit = onBack,
    onNavigateFavorites: () -> Unit = {},
    onNavigateHistory: () -> Unit = {}
) {
    val hapticFeedback = LocalHapticFeedback.current

    var activeSubActivity by remember { mutableStateOf<LearningActivityType?>(null) }
    var config by remember { mutableStateOf(LearningConfig(optionsCount = 4, categoryFilter = null, autoSpeak = true)) }
    var showAdultGate by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = activeSubActivity,
        label = "learning_screen_transition",
        transitionSpec = { fadeIn() togetherWith fadeOut() }
    ) { currentActivity ->
        when (currentActivity) {
            LearningActivityType.FIND_PICTOGRAM -> {
                FindPictogramActivityScreen(
                    allPictograms = allPictograms,
                    languageCode = languageCode,
                    highContrast = highContrast,
                    hapticsEnabled = hapticsEnabled,
                    onSpeakWord = onSpeakWord,
                    onBackToHub = { activeSubActivity = null },
                    onNavigateTablero = onNavigateTablero,
                    onNavigateFavorites = onNavigateFavorites,
                    onNavigateHistory = onNavigateHistory
                )
            }

            LearningActivityType.MATCH_WORD -> {
                MatchWordActivityScreen(
                    allPictograms = allPictograms,
                    languageCode = languageCode,
                    highContrast = highContrast,
                    hapticsEnabled = hapticsEnabled,
                    onSpeakWord = onSpeakWord,
                    onBackToHub = { activeSubActivity = null },
                    onNavigateTablero = onNavigateTablero,
                    onNavigateFavorites = onNavigateFavorites,
                    onNavigateHistory = onNavigateHistory
                )
            }

            LearningActivityType.CLASSIFY_CATEGORY -> {
                ClassifyActivityScreen(
                    allPictograms = allPictograms,
                    languageCode = languageCode,
                    highContrast = highContrast,
                    hapticsEnabled = hapticsEnabled,
                    onSpeakWord = onSpeakWord,
                    onBackToHub = { activeSubActivity = null },
                    onNavigateTablero = onNavigateTablero,
                    onNavigateFavorites = onNavigateFavorites,
                    onNavigateHistory = onNavigateHistory
                )
            }

            LearningActivityType.GUIDED_PHRASES -> {
                GuidedPhrasesSubScreen(
                    languageCode = languageCode,
                    highContrast = highContrast,
                    hapticsEnabled = hapticsEnabled,
                    onSpeakWord = onSpeakWord,
                    onBackToHub = { activeSubActivity = null },
                    onNavigateTablero = onNavigateTablero,
                    onNavigateFavorites = onNavigateFavorites,
                    onNavigateHistory = onNavigateHistory
                )
            }

            null -> {
                // Pantalla Inicial del Modo Aprender (Hub de Actividades)
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = "Modo Aprender",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = {
                                        PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                                        onBack()
                                    },
                                    modifier = Modifier.semantics {
                                        role = Role.Button
                                        contentDescription = "Volver al comunicador"
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
                                        contentDescription = "Configuración educativa para adultos"
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
                            onNavigateLearn = { /* ya en aprender */ },
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
                        contentPadding = PaddingValues(vertical = PictoDimens.SpaceLarge),
                        verticalArrangement = Arrangement.spacedBy(PictoDimens.SpaceLarge)
                    ) {
                        item {
                            Column {
                                Text(
                                    text = "Actividades Educativas",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (highContrast) PictoColors.HighlightHighContrast else PictoColors.Primary
                                )
                                Text(
                                    text = "Juegos accesibles para familiarizarse con los pictogramas y el vocabulario.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (highContrast) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Tarjeta 1: Encuentra el pictograma
                        item {
                            ActivityHubCard(
                                title = "Encuentra el pictograma",
                                description = "Escucha o lee la palabra y elige la imagen correcta entre las opciones.",
                                icon = Icons.Default.Search,
                                badgeColor = if (highContrast) Color.Black else Color(0xFFE0F2F1),
                                badgeBorder = if (highContrast) Color.White else PictoColors.Primary,
                                iconTint = if (highContrast) PictoColors.HighlightHighContrast else PictoColors.Primary,
                                highContrast = highContrast,
                                hapticsEnabled = hapticsEnabled,
                                onStart = {
                                    activeSubActivity = LearningActivityType.FIND_PICTOGRAM
                                }
                            )
                        }

                        // Tarjeta 2: Relaciona palabra e imagen
                        item {
                            ActivityHubCard(
                                title = "Relaciona palabra e imagen",
                                description = "Lee la palabra escrita y selecciona el pictograma que le corresponde.",
                                icon = Icons.AutoMirrored.Filled.MenuBook,
                                badgeColor = if (highContrast) Color.Black else Color(0xFFE8EAF6),
                                badgeBorder = if (highContrast) Color.White else Color(0xFF3F51B5),
                                iconTint = if (highContrast) PictoColors.HighlightHighContrast else Color(0xFF3F51B5),
                                highContrast = highContrast,
                                hapticsEnabled = hapticsEnabled,
                                onStart = {
                                    activeSubActivity = LearningActivityType.MATCH_WORD
                                }
                            )
                        }

                        // Tarjeta 3: Clasifica pictogramas
                        item {
                            ActivityHubCard(
                                title = "Clasifica pictogramas",
                                description = "Aprende a reconocer a qué categoría pertenece cada pictograma.",
                                icon = Icons.Default.Category,
                                badgeColor = if (highContrast) Color.Black else Color(0xFFFFF3E0),
                                badgeBorder = if (highContrast) Color.White else Color(0xFFE65100),
                                iconTint = if (highContrast) PictoColors.HighlightHighContrast else Color(0xFFE65100),
                                highContrast = highContrast,
                                hapticsEnabled = hapticsEnabled,
                                onStart = {
                                    activeSubActivity = LearningActivityType.CLASSIFY_CATEGORY
                                }
                            )
                        }

                        // Tarjeta 4 (Adicional / Conservada): Frases guiadas
                        item {
                            ActivityHubCard(
                                title = "Frases guiadas",
                                description = "Practica secuencias de comunicación paso a paso y refuerza su sintaxis.",
                                icon = Icons.Default.Forum,
                                badgeColor = if (highContrast) Color.Black else Color(0xFFF3E5F5),
                                badgeBorder = if (highContrast) Color.White else Color(0xFF7B1FA2),
                                iconTint = if (highContrast) PictoColors.HighlightHighContrast else Color(0xFF7B1FA2),
                                highContrast = highContrast,
                                hapticsEnabled = hapticsEnabled,
                                onStart = {
                                    activeSubActivity = LearningActivityType.GUIDED_PHRASES
                                }
                            )
                        }
                    }
                }
            }
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
            }
        )
    }
}

/**
 * Tarjeta accesible del menú de actividades educativas.
 */
@Composable
private fun ActivityHubCard(
    title: String,
    description: String,
    icon: ImageVector,
    badgeColor: Color,
    badgeBorder: Color,
    iconTint: Color,
    highContrast: Boolean,
    hapticsEnabled: Boolean,
    onStart: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = "Actividad $title: $description. Toca para comenzar."
            },
        shape = PictoDimens.CornerLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (highContrast) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (highContrast) PictoDimens.BorderWidthHighContrast else 1.5.dp,
            color = if (highContrast) Color.White else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = PictoDimens.ElevationStandard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PictoDimens.SpaceLarge)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = badgeColor,
                    border = BorderStroke(1.5.dp, badgeBorder),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(Modifier.width(PictoDimens.SpaceMedium))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (highContrast) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(PictoDimens.SpaceExtraSmall))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (highContrast) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(PictoDimens.SpaceMedium))

            Button(
                onClick = {
                    PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                    onStart()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Comenzar actividad $title"
                    },
                shape = PictoDimens.CornerMedium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (highContrast) PictoColors.HighlightHighContrast else PictoColors.Primary,
                    contentColor = if (highContrast) Color.Black else Color.White
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(PictoDimens.SpaceSmall))
                Text("Comenzar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

/**
 * Sub-pantalla que preserva la funcionalidad de Frases Guiadas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GuidedPhrasesSubScreen(
    languageCode: String,
    highContrast: Boolean,
    hapticsEnabled: Boolean,
    onSpeakWord: (String) -> Unit,
    onBackToHub: () -> Unit,
    onNavigateTablero: () -> Unit,
    onNavigateFavorites: () -> Unit,
    onNavigateHistory: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val phrases = remember { DefaultCatalog.getGuidedPhrases() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Frases Guiadas", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                            onBackToHub()
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Volver al Modo Aprender"
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
            contentPadding = PaddingValues(vertical = PictoDimens.SpaceLarge),
            verticalArrangement = Arrangement.spacedBy(PictoDimens.SpaceLarge)
        ) {
            items(phrases) { phrase ->
                val phraseTitle = phrase.getTitle(languageCode)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = PictoDimens.CornerLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = if (highContrast) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (highContrast) Color.White else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(PictoDimens.SpaceLarge)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = phraseTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (highContrast) PictoColors.HighlightHighContrast else MaterialTheme.colorScheme.primary
                            )
                            FilledTonalIconButton(
                                onClick = {
                                    PictoHaptics.performEmphasis(hapticFeedback, hapticsEnabled)
                                    val fullPhrase = processPhraseForSpeech(phrase.pictos, languageCode)
                                    onSpeakWord(fullPhrase)
                                },
                                modifier = Modifier.semantics {
                                    role = Role.Button
                                    contentDescription = "Escuchar frase completa: $phraseTitle"
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                            }
                        }

                        Spacer(Modifier.height(PictoDimens.SpaceMedium))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(PictoDimens.SpaceMedium)
                        ) {
                            items(phrase.pictos) { picto ->
                                val pictoName = picto.getName(languageCode)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(110.dp)
                                        .background(
                                            color = if (highContrast) Color.Black else picto.category.color,
                                            shape = PictoDimens.CornerMedium
                                        )
                                        .border(
                                            width = if (highContrast) PictoDimens.BorderWidthHighContrast else PictoDimens.BorderWidthStandard,
                                            color = if (highContrast) Color.White else picto.category.borderColor,
                                            shape = PictoDimens.CornerMedium
                                        )
                                        .clickable {
                                            PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                                            onSpeakWord(pictoName)
                                        }
                                        .padding(PictoDimens.SpaceMedium)
                                        .semantics {
                                            role = Role.Button
                                            contentDescription = "Pictograma $pictoName, toca para escuchar"
                                        }
                                ) {
                                    if (picto.icon != null) {
                                        Icon(
                                            imageVector = picto.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(PictoDimens.IconSizeLarge),
                                            tint = if (highContrast) Color.White else picto.category.textColor
                                        )
                                    }
                                    Spacer(Modifier.height(PictoDimens.SpaceExtraSmall))
                                    Text(
                                        text = pictoName,
                                        style = PictoTypography.PictoLabelMedium,
                                        textAlign = TextAlign.Center,
                                        color = if (highContrast) Color.White else picto.category.textColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
