package com.cmanzano.pictohabla.feature.history

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmanzano.pictohabla.core.designsystem.PictoColors
import com.cmanzano.pictohabla.core.designsystem.PictoDimens
import com.cmanzano.pictohabla.core.designsystem.PictoHaptics
import com.cmanzano.pictohabla.core.model.PhraseHistory
import com.cmanzano.pictohabla.navigation.PictoBottomNavBar
import com.cmanzano.pictohabla.navigation.PictoScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla de Historial de Expresiones Comunicativas.
 * Permite revisar, volver a reproducir, recuperar en el tablero o eliminar frases anteriores.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    historyList: List<PhraseHistory>,
    highContrast: Boolean,
    hapticsEnabled: Boolean,
    languageCode: String = "ES",
    onSpeakPhrase: (String) -> Unit,
    onRecoverPhrase: (PhraseHistory) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit,
    onNavigateTablero: () -> Unit = onBack,
    onNavigateFavorites: () -> Unit = {},
    onNavigateLearn: () -> Unit = {}
) {
    val hapticFeedback = LocalHapticFeedback.current
    var showClearDialog by remember { mutableStateOf(false) }
    val isEnglish = languageCode.equals("EN", ignoreCase = true)

    val timeFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isEnglish) "Phrase History" else "Historial de Frases", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                            onBack()
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = if (isEnglish) "Back to main board" else "Volver al tablero principal"
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (historyList.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                PictoHaptics.performEmphasis(hapticFeedback, hapticsEnabled)
                                showClearDialog = true
                            },
                            modifier = Modifier.semantics {
                                role = Role.Button
                                contentDescription = if (isEnglish) "Clear all history" else "Vaciar todo el historial"
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                tint = if (highContrast) Color(0xFFFF5252) else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (highContrast) Color.Black else MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            PictoBottomNavBar(
                currentScreen = PictoScreen.HISTORY,
                onNavigateTablero = onNavigateTablero,
                onNavigateFavorites = onNavigateFavorites,
                onNavigateHistory = { /* ya en historial */ },
                onNavigateLearn = onNavigateLearn,
                highContrast = highContrast,
                hapticsEnabled = hapticsEnabled
            )
        }
    ) { padding ->
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text(if (isEnglish) "Clear all history?" else "¿Vaciar todo el historial?", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        if (isEnglish) "This action will delete all phrases saved in history. It cannot be undone."
                        else "Esta acción eliminará todas las frases guardadas en el historial. No se puede deshacer."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            PictoHaptics.performEmphasis(hapticFeedback, hapticsEnabled)
                            onClearAll()
                            showClearDialog = false
                        }
                    ) {
                        Text(if (isEnglish) "Yes, clear" else "Sí, vaciar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text(if (isEnglish) "Cancel" else "Cancelar")
                    }
                }
            )
        }

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(PictoDimens.SpaceExtraLarge),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(PictoDimens.SpaceMedium))
                    Text(
                        text = if (isEnglish) "History is empty" else "El historial está vacío",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(PictoDimens.SpaceSmall))
                    Text(
                        text = if (isEnglish) {
                            "Every time you build and speak a phrase on the board, it will be saved here to reuse whenever you want."
                        } else {
                            "Cada vez que construyas y reproduzcas una frase en el tablero, quedará registrada aquí para volver a usarla cuando quieras."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = PictoDimens.SpaceLarge),
                contentPadding = PaddingValues(vertical = PictoDimens.SpaceMedium),
                verticalArrangement = Arrangement.spacedBy(PictoDimens.SpaceMedium)
            ) {
                items(historyList, key = { it.id }) { item ->
                    val formattedDate = remember(item.timestamp) {
                        timeFormatter.format(Date(item.timestamp))
                    }

                    Card(
                        shape = PictoDimens.CornerLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = if (highContrast) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (highContrast) Color.White else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(PictoDimens.SpaceLarge)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formattedDate,
                                    fontSize = 12.sp,
                                    color = if (highContrast) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = {
                                        PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                                        onDeleteEntry(item.id)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Borrar esta frase del historial",
                                        tint = if (highContrast) Color(0xFFFF5252) else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(PictoDimens.SpaceSmall))

                            Text(
                                text = "«${item.spokenText}»",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (highContrast) Color.White else MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(Modifier.height(PictoDimens.SpaceMedium))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(PictoDimens.SpaceMedium)
                            ) {
                                // Botón para reproducir por TTS
                                FilledTonalButton(
                                    onClick = {
                                        PictoHaptics.performEmphasis(hapticFeedback, hapticsEnabled)
                                        onSpeakPhrase(item.spokenText)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = PictoDimens.CornerMedium
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                                    Spacer(Modifier.width(PictoDimens.SpaceExtraSmall))
                                    Text(if (isEnglish) "Listen" else "Escuchar", fontWeight = FontWeight.Bold)
                                }

                                // Botón para cargar en el tablero principal
                                OutlinedButton(
                                    onClick = {
                                        PictoHaptics.performEmphasis(hapticFeedback, hapticsEnabled)
                                        onRecoverPhrase(item)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = PictoDimens.CornerMedium,
                                    border = BorderStroke(1.5.dp, if (highContrast) Color.White else MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Input, contentDescription = null)
                                    Spacer(Modifier.width(PictoDimens.SpaceExtraSmall))
                                    Text(if (isEnglish) "Load phrase" else "Cargar frase", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
