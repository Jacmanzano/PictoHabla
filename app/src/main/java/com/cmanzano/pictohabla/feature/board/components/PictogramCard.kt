package com.cmanzano.pictohabla.feature.board.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cmanzano.pictohabla.core.designsystem.PictoColors
import com.cmanzano.pictohabla.core.designsystem.PictoDimens
import com.cmanzano.pictohabla.core.designsystem.PictoHaptics
import com.cmanzano.pictohabla.core.designsystem.PictoTypography
import com.cmanzano.pictohabla.core.model.Pictogram
import com.cmanzano.pictohabla.core.util.ImageStorageHelper

/**
 * Tarjeta accesible para mostrar un pictograma individual en la cuadrícula.
 * Incorpora:
 * - Soporte para fotos locales personalizadas y vectores Material
 * - Zona táctil amplia (44 dp) para marcar/desmarcar favoritos
 * - Soporte para pulsación prolongada como alternativa accesible
 * - Borde y paleta según la Clave de Fitzgerald Modificada
 * - Semántica completa para lectores de pantalla TalkBack
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PictogramCard(
    picto: Pictogram,
    languageCode: String,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    highContrast: Boolean = false,
    hapticsEnabled: Boolean = true,
    onToggleFavorite: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current

    val borderColor = if (highContrast) PictoColors.BorderHighContrast else picto.category.borderColor
    val borderWidth = if (highContrast) PictoDimens.BorderWidthHighContrast else PictoDimens.BorderWidthStandard
    val shape = PictoDimens.CornerMedium

    val pictoName = picto.getName(languageCode)
    val categoryLabel = picto.category.getLabel(languageCode)
    val favoriteLabel = if (isFavorite) ", marcado como favorito" else ""

    // Carga de foto local persistente si es un pictograma personalizado
    val localBitmap = remember(picto.localImagePath) {
        ImageStorageHelper.loadBitmap(picto.localImagePath)?.asImageBitmap()
    }

    ElevatedCard(
        shape = shape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (highContrast) Color.Black else picto.category.color
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = PictoDimens.ElevationStandard,
            pressedElevation = PictoDimens.ElevationLow
        ),
        modifier = modifier
            .border(borderWidth, borderColor, shape)
            .combinedClickable(
                onClick = {
                    PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                    onClick()
                },
                onLongClick = if (onToggleFavorite != null) {
                    {
                        PictoHaptics.performEmphasis(hapticFeedback, hapticsEnabled)
                        onToggleFavorite()
                    }
                } else null
            )
            .semantics {
                role = Role.Button
                contentDescription = "$pictoName, categoría $categoryLabel$favoriteLabel"
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = PictoDimens.SpaceSmall, vertical = PictoDimens.SpaceExtraSmall),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Renderizado de imagen local o icono vectorial
                if (localBitmap != null) {
                    Image(
                        bitmap = localBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(PictoDimens.IconSizeLarge)
                            .border(1.dp, Color.Black.copy(alpha = 0.2f), PictoDimens.CornerSmall)
                    )
                } else if (picto.icon != null) {
                    Icon(
                        imageVector = picto.icon,
                        contentDescription = null,
                        modifier = Modifier.size(PictoDimens.IconSizeLarge),
                        tint = if (highContrast) Color.White else picto.category.textColor
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(PictoDimens.IconSizeLarge),
                        tint = if (highContrast) Color.White else picto.category.textColor
                    )
                }

                Spacer(modifier = Modifier.height(PictoDimens.SpaceExtraSmall))

                Text(
                    text = pictoName,
                    style = PictoTypography.PictoLabelMedium,
                    textAlign = TextAlign.Center,
                    color = if (highContrast) Color.White else picto.category.textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Zona táctil amplia y cómoda (44 dp) para marcar favorito
            if (onToggleFavorite != null) {
                Surface(
                    onClick = {
                        PictoHaptics.performEmphasis(hapticFeedback, hapticsEnabled)
                        onToggleFavorite()
                    },
                    shape = CircleShape,
                    color = if (isFavorite) Color.White.copy(alpha = 0.9f) else Color.Transparent,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(44.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = if (isFavorite) {
                                "Quitar $pictoName de favoritos"
                            } else {
                                "Añadir $pictoName a favoritos"
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = if (isFavorite) {
                                Color(0xFFFFB300) // Amarillo ámbar
                            } else {
                                if (highContrast) Color.White.copy(alpha = 0.7f) else Color.DarkGray.copy(alpha = 0.45f)
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
