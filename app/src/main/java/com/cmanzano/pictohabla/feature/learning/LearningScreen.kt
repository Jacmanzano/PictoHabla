package com.cmanzano.pictohabla.feature.learning

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.cmanzano.pictohabla.core.util.processPhraseForSpeech
import com.cmanzano.pictohabla.data.local.DefaultCatalog
import com.cmanzano.pictohabla.navigation.PictoBottomNavBar
import com.cmanzano.pictohabla.navigation.PictoScreen

/**
 * Pantalla de aprendizaje interactivo con secuencias de comunicación guiadas.
 * Diseñada para reforzar la comprensión sintáctica paso a paso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningScreen(
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
    val phrases = remember { DefaultCatalog.getGuidedPhrases() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Aprender Frases", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                            onBack()
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Volver al tablero principal"
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
