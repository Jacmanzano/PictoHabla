package com.cmanzano.pictohabla.feature.board.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmanzano.pictohabla.core.designsystem.PictoColors
import com.cmanzano.pictohabla.core.designsystem.PictoDimens
import com.cmanzano.pictohabla.core.designsystem.PictoHaptics
import com.cmanzano.pictohabla.core.designsystem.PictoTypography
import com.cmanzano.pictohabla.core.model.Pictogram
import com.cmanzano.pictohabla.core.util.ImageStorageHelper

/**
 * Barra de construcción y reproducción de frases con sección visual propia.
 * Distribución espaciosa y accesible:
 * 1. Título «Tu frase» con contador opcional.
 * 2. Fila horizontal desplazable de pictogramas seleccionados (con soporte de eliminación individual).
 * 3. Fila inferior de controles: a la izquierda «Quitar último» y «Borrar frase», a la derecha el botón destacado «Hablar».
 */
@Composable
fun PhraseBar(
    selectedPictos: List<Pictogram>,
    languageCode: String,
    onSpeakClick: () -> Unit,
    onClearClick: () -> Unit,
    onRemoveLast: () -> Unit,
    onRemoveAtIndex: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    highContrast: Boolean = false,
    hapticsEnabled: Boolean = true
) {
    val hapticFeedback = LocalHapticFeedback.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (highContrast) Color(0xFF161616) else MaterialTheme.colorScheme.surface,
        shape = PictoDimens.CornerLarge,
        shadowElevation = if (highContrast) PictoDimens.ElevationNone else PictoDimens.ElevationLow,
        border = BorderStroke(
            width = if (highContrast) PictoDimens.BorderWidthStandard else 1.dp,
            color = if (highContrast) Color.White else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PictoDimens.SpaceMedium)
        ) {
            // 1. Título de sección: «Tu frase»
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = PictoDimens.SpaceSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tu frase",
                    style = PictoTypography.SectionTitle,
                    color = if (highContrast) PictoColors.HighlightHighContrast else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                if (selectedPictos.isNotEmpty()) {
                    Text(
                        text = "${selectedPictos.size} palabra${if (selectedPictos.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (highContrast) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. Fila horizontal de pictogramas seleccionados o estado vacío
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (selectedPictos.isEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tarjeta simulada de "+ Añadir" para invitar visualmente al niño
                        Surface(
                            modifier = Modifier
                                .size(PictoDimens.PhraseBarPictoSize)
                                .semantics {
                                    contentDescription = "Espacio vacío para añadir pictogramas a la frase"
                                },
                            shape = PictoDimens.CornerMedium,
                            color = if (highContrast) Color(0xFF242424) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.5.dp,
                                if (highContrast) Color.Gray else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = if (highContrast) Color.LightGray else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Añadir",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (highContrast) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.width(PictoDimens.SpaceMedium))

                        Text(
                            text = "Toca los pictogramas para construir tu frase",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (highContrast) Color(0xFFCCCCCC) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PictoDimens.SpaceSmall)
                    ) {
                        itemsIndexed(selectedPictos) { index, picto ->
                            val chipName = picto.getName(languageCode)
                            val localBitmap = remember(picto.localImagePath) {
                                ImageStorageHelper.loadBitmap(picto.localImagePath)?.asImageBitmap()
                            }

                            Card(
                                onClick = {
                                    PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                                    onRemoveAtIndex(index)
                                },
                                modifier = Modifier
                                    .size(PictoDimens.PhraseBarPictoSize)
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = "Posición ${index + 1}: $chipName. Toca para quitar de la frase."
                                    },
                                shape = PictoDimens.CornerMedium,
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
                                            .padding(PictoDimens.SpaceExtraSmall),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        if (localBitmap != null) {
                                            Image(
                                                bitmap = localBitmap,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        } else if (picto.icon != null) {
                                            Icon(
                                                imageVector = picto.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(36.dp),
                                                tint = if (highContrast) Color.White else picto.category.textColor
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = chipName,
                                            style = PictoTypography.PictoChipLabel,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (highContrast) Color.White else picto.category.textColor
                                        )
                                    }

                                    // Indicador de orden secuencial accesible
                                    Surface(
                                        shape = CircleShape,
                                        color = if (highContrast) Color.White else Color.Black.copy(alpha = 0.7f),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(4.dp)
                                            .size(18.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${index + 1}",
                                                fontSize = 10.sp,
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

            Spacer(Modifier.height(PictoDimens.SpaceMedium))

            // 3. Fila de controles diferenciados: «Quitar último» · «Borrar» a la izquierda, «Hablar» a la derecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Acciones de edición
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PictoDimens.SpaceSmall)
                ) {
                    FilledTonalButton(
                        onClick = {
                            PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                            onRemoveLast()
                        },
                        enabled = selectedPictos.isNotEmpty(),
                        modifier = Modifier
                            .height(40.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = "Quitar el último pictograma de la frase"
                            },
                        shape = PictoDimens.CornerMedium,
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Quitar último",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            PictoHaptics.performEmphasis(hapticFeedback, hapticsEnabled)
                            onClearClick()
                        },
                        enabled = selectedPictos.isNotEmpty(),
                        modifier = Modifier
                            .height(40.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = "Borrar toda la frase"
                            },
                        shape = PictoDimens.CornerMedium,
                        border = if (selectedPictos.isNotEmpty()) {
                            BorderStroke(1.dp, if (highContrast) Color(0xFFFF5252) else MaterialTheme.colorScheme.error)
                        } else null,
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = if (selectedPictos.isNotEmpty()) {
                                if (highContrast) Color(0xFFFF5252) else MaterialTheme.colorScheme.error
                            } else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Borrar",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedPictos.isNotEmpty()) {
                                if (highContrast) Color(0xFFFF5252) else MaterialTheme.colorScheme.error
                            } else Color.Gray
                        )
                    }
                }

                // Botón principal HABLAR: elemento más destacado de la sección
                Button(
                    onClick = {
                        PictoHaptics.performEmphasis(hapticFeedback, hapticsEnabled)
                        onSpeakClick()
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Hablar: reproducir la frase completa en voz alta"
                        },
                    shape = PictoDimens.CornerPill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (highContrast) PictoColors.HighlightHighContrast else PictoColors.Primary,
                        contentColor = if (highContrast) Color.Black else Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = PictoDimens.ElevationStandard),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Hablar",
                            style = PictoTypography.SpeakButtonText
                        )
                    }
                }
            }
        }
    }
}
