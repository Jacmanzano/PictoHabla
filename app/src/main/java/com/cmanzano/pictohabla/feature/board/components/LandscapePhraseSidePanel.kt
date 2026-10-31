package com.cmanzano.pictohabla.feature.board.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.cmanzano.pictohabla.core.model.Pictogram

/**
 * Panel lateral compacto para la construcción y gestión de frases en teléfonos móviles
 * en orientación horizontal (landscape).
 *
 * Ocupa ~30-35 % del ancho disponible y organiza:
 * 1. Título compacto «Tu frase» con contador de palabras.
 * 2. Carrusel horizontal desplazable con los pictogramas seleccionados y su orden secuencial.
 * 3. Controles accesibles para quitar el último pictograma, borrar la frase y reproducir en voz alta.
 */
@Composable
fun LandscapePhraseSidePanel(
    currentPhrase: List<Pictogram>,
    languageCode: String,
    highContrast: Boolean,
    hapticsEnabled: Boolean,
    onSpeakClick: () -> Unit,
    onClearClick: () -> Unit,
    onRemoveLast: () -> Unit,
    onRemoveAtIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    val isEn = languageCode.equals("EN", ignoreCase = true)

    Surface(
        modifier = modifier,
        color = if (highContrast) Color(0xFF161616) else MaterialTheme.colorScheme.surface,
        shape = PictoDimens.CornerMedium,
        shadowElevation = if (highContrast) PictoDimens.ElevationNone else PictoDimens.ElevationLow,
        border = BorderStroke(
            width = if (highContrast) PictoDimens.BorderWidthStandard else 1.dp,
            color = if (highContrast) Color.White else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PictoDimens.SpaceSmall),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Cabecera: «Tu frase» y contador numérico
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isEn) "Your phrase" else "Tu frase",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (highContrast) PictoColors.HighlightHighContrast else MaterialTheme.colorScheme.onSurface
                )
                if (currentPhrase.isNotEmpty()) {
                    Surface(
                        shape = PictoDimens.CornerPill,
                        color = if (highContrast) Color.DarkGray else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${currentPhrase.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (highContrast) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // 2. Fila desplazable de pictogramas seleccionados (con orden numérico)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (currentPhrase.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .semantics {
                                contentDescription = if (isEn) {
                                    "Empty phrase area. Tap pictograms to build a phrase"
                                } else {
                                    "Área de frase vacía. Toca pictogramas para construir tu frase"
                                }
                            },
                        shape = PictoDimens.CornerSmall,
                        color = if (highContrast) Color(0xFF222222) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(
                            1.dp,
                            if (highContrast) Color.Gray else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = if (highContrast) Color.LightGray else PictoColors.Primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (isEn) "Tap pictograms\nto speak" else "Toca pictogramas\npara hablar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                lineHeight = 13.sp,
                                color = if (highContrast) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        itemsIndexed(currentPhrase) { index, picto ->
                            val chipName = picto.getName(languageCode)
                            Card(
                                onClick = {
                                    PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                                    onRemoveAtIndex(index)
                                },
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(72.dp)
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = "${index + 1}: $chipName. ${if (isEn) "Tap to remove" else "Toca para quitar"}"
                                    },
                                shape = PictoDimens.CornerSmall,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (highContrast) Color.Black else picto.category.color
                                ),
                                border = BorderStroke(
                                    width = PictoDimens.BorderWidthStandard,
                                    color = if (highContrast) Color.White else picto.category.borderColor
                                )
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(3.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        PictogramImage(
                                            picto = picto,
                                            highContrast = highContrast,
                                            modifier = Modifier.size(34.dp)
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = chipName,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            color = if (highContrast) Color.White else picto.category.textColor
                                        )
                                    }

                                    // Indicador de orden secuencial (1, 2, 3...)
                                    Surface(
                                        shape = CircleShape,
                                        color = if (highContrast) Color.White else Color.Black.copy(alpha = 0.7f),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(2.dp)
                                            .size(16.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${index + 1}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (highContrast) Color.Black else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // 3. Fila de botones de edición («Quitar» y «Borrar»)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                        onRemoveLast()
                    },
                    enabled = currentPhrase.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = if (isEn) "Remove last pictogram" else "Quitar último pictograma"
                        },
                    shape = PictoDimens.CornerMedium,
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isEn) "Back" else "Quitar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        PictoHaptics.performEmphasis(hapticFeedback, hapticsEnabled)
                        onClearClick()
                    },
                    enabled = currentPhrase.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = if (isEn) "Clear phrase" else "Borrar frase"
                        },
                    shape = PictoDimens.CornerMedium,
                    border = if (currentPhrase.isNotEmpty()) {
                        BorderStroke(1.dp, if (highContrast) Color(0xFFFF5252) else MaterialTheme.colorScheme.error)
                    } else null,
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = if (currentPhrase.isNotEmpty()) {
                            if (highContrast) Color(0xFFFF5252) else MaterialTheme.colorScheme.error
                        } else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isEn) "Clear" else "Borrar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentPhrase.isNotEmpty()) {
                            if (highContrast) Color(0xFFFF5252) else MaterialTheme.colorScheme.error
                        } else Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // 4. Botón principal HABLAR (destacado a todo el ancho del panel)
            Button(
                onClick = {
                    PictoHaptics.performEmphasis(hapticFeedback, hapticsEnabled)
                    onSpeakClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = if (isEn) "Speak phrase aloud" else "Hablar: reproducir la frase en voz alta"
                    },
                shape = PictoDimens.CornerPill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (highContrast) PictoColors.HighlightHighContrast else PictoColors.Primary,
                    contentColor = if (highContrast) Color.Black else Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = PictoDimens.ElevationStandard),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isEn) "Speak" else "Hablar",
                        style = PictoTypography.SpeakButtonText,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
