package com.cmanzano.pictohabla.feature.learning.activities

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.cmanzano.pictohabla.feature.learning.model.MemoryCard
import com.cmanzano.pictohabla.navigation.PictoBottomNavBar
import com.cmanzano.pictohabla.navigation.PictoScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Modo 4: "Memoria visual".
 * Juego accesible de parejas de pictogramas boca abajo con dificultad adaptable (2, 3 o 4 parejas).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryActivityScreen(
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
    val coroutineScope = rememberCoroutineScope()

    var pairsCount by remember { mutableIntStateOf(3) }
    var cards = remember {
        mutableStateListOf<MemoryCard>().apply {
            addAll(EducationalExerciseGenerator.generateMemoryExercise(allPictograms, pairsCount).cards)
        }
    }

    var firstSelectedCardIndex by remember { mutableStateOf<Int?>(null) }
    var secondSelectedCardIndex by remember { mutableStateOf<Int?>(null) }
    var isCheckingMatch by remember { mutableStateOf(false) }
    var feedbackState by remember { mutableStateOf<FeedbackState>(FeedbackState.None) }

    fun resetBoard(newPairs: Int) {
        pairsCount = newPairs
        firstSelectedCardIndex = null
        secondSelectedCardIndex = null
        isCheckingMatch = false
        feedbackState = FeedbackState.None
        val exercise = EducationalExerciseGenerator.generateMemoryExercise(allPictograms, newPairs)
        cards.clear()
        cards.addAll(exercise.cards)
    }

    val allMatched = cards.isNotEmpty() && cards.all { it.isMatched }

    LaunchedEffect(allMatched) {
        if (allMatched) {
            PictoHaptics.performEmphasis(hapticFeedback, hapticsEnabled)
            feedbackState = FeedbackState.Success(
                if (languageCode == "EN") "Congratulations! You found all pairs!" else "¡Felicidades! ¡Has encontrado todas las parejas!"
            )
            progressManager.recordResult(LearningActivityType.VISUAL_MEMORY, isHit = true)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (languageCode == "EN") "Visual Memory" else "Memoria visual",
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
                            resetBoard(pairsCount)
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Reiniciar partida de memoria"
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
            Spacer(Modifier.height(PictoDimens.SpaceSmall))

            // Selector de parejas / dificultad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (languageCode == "EN") "Pairs: " else "Parejas: ",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(2 to "2 (Fácil)", 3 to "3 (Medio)", 4 to "4 (Reto)").forEach { (count, label) ->
                    FilterChip(
                        selected = pairsCount == count,
                        onClick = {
                            resetBoard(count)
                        },
                        label = { Text(if (languageCode == "EN") "$count pairs" else label) },
                        modifier = Modifier.padding(horizontal = 4.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (highContrast) Color.White else MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = if (highContrast) Color.Black else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(Modifier.height(PictoDimens.SpaceSmall))

            // Cuadrícula de cartas
            val columns = if (pairsCount <= 3) 3 else 4
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = PictoDimens.SpaceSmall),
                horizontalArrangement = Arrangement.spacedBy(PictoDimens.SpaceSmall),
                verticalArrangement = Arrangement.spacedBy(PictoDimens.SpaceSmall)
            ) {
                items(cards.indices.toList()) { index ->
                    val card = cards[index]
                    val isRevealed = card.isFaceUp || card.isMatched

                    Card(
                        shape = PictoDimens.CornerMedium,
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                card.isMatched -> if (highContrast) Color(0xFF1B5E20) else Color(0xFFE8F5E9)
                                isRevealed -> if (highContrast) Color.Black else card.picto.category.color
                                else -> if (highContrast) Color(0xFF222222) else MaterialTheme.colorScheme.primaryContainer
                            }
                        ),
                        border = BorderStroke(
                            width = if (card.isMatched) 3.dp else 1.5.dp,
                            color = when {
                                card.isMatched -> PictoColors.Success
                                isRevealed -> if (highContrast) Color.White else card.picto.category.borderColor
                                else -> if (highContrast) Color.Gray else MaterialTheme.colorScheme.primary
                            }
                        ),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable(enabled = !isRevealed && !isCheckingMatch) {
                                PictoHaptics.performClick(hapticFeedback, hapticsEnabled)

                                if (firstSelectedCardIndex == null) {
                                    firstSelectedCardIndex = index
                                    cards[index] = card.copy(isFaceUp = true)
                                } else if (secondSelectedCardIndex == null && index != firstSelectedCardIndex) {
                                    secondSelectedCardIndex = index
                                    cards[index] = card.copy(isFaceUp = true)
                                    isCheckingMatch = true

                                    val firstIdx = firstSelectedCardIndex!!
                                    val firstCard = cards[firstIdx]
                                    val secondCard = cards[index]

                                    if (firstCard.pairId == secondCard.pairId) {
                                        // ¡Pareja encontrada!
                                        PictoHaptics.performEmphasis(hapticFeedback, hapticsEnabled)
                                        val pictoName = firstCard.picto.getName(languageCode).ifBlank { firstCard.picto.getName("ES") }
                                        onSpeakWord(pictoName)

                                        cards[firstIdx] = firstCard.copy(isMatched = true)
                                        cards[index] = secondCard.copy(isMatched = true)
                                        firstSelectedCardIndex = null
                                        secondSelectedCardIndex = null
                                        isCheckingMatch = false
                                    } else {
                                        // No coinciden: voltear tras una pausa amistosa
                                        coroutineScope.launch {
                                            delay(1000)
                                            cards[firstIdx] = firstCard.copy(isFaceUp = false)
                                            cards[index] = secondCard.copy(isFaceUp = false)
                                            firstSelectedCardIndex = null
                                            secondSelectedCardIndex = null
                                            isCheckingMatch = false
                                        }
                                    }
                                }
                            }
                            .semantics {
                                role = Role.Button
                                contentDescription = if (isRevealed) {
                                    card.picto.getName(languageCode) + if (card.isMatched) ", pareja encontrada" else ""
                                } else {
                                    "Carta oculta"
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(PictoDimens.SpaceSmall),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isRevealed) {
                                PictogramImage(
                                    picto = card.picto,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (card.isMatched) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = PictoColors.Success,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.TopEnd)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Extension,
                                    contentDescription = null,
                                    tint = if (highContrast) Color.White else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Banner al completar todas las parejas
            ExerciseFeedbackBanner(
                state = feedbackState,
                highContrast = highContrast,
                onNext = {
                    resetBoard(pairsCount)
                },
                onRetry = {
                    resetBoard(pairsCount)
                }
            )

            Spacer(Modifier.height(PictoDimens.SpaceSmall))
        }
    }
}
