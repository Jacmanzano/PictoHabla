package com.cmanzano.pictohabla.feature.board.components

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.cmanzano.pictohabla.core.model.Pictogram
import com.cmanzano.pictohabla.core.util.ArasaacImageHelper

/**
 * Componente unificado para renderizar la imagen de un pictograma en cualquier pantalla.
 *
 * Sigue la jerarquía visual oficial:
 * 1. Imagen oficial ARASAAC (PNG con transparencia original y escalado sin deformación).
 * 2. Fotografía personalizada guardada en el dispositivo.
 * 3. Icono vectorial accesible de respaldo si no hay imagen de mapa de bits.
 * 4. Icono neutro y discreto de recurso no disponible (sin emojis artificiales).
 */
@Composable
fun PictogramImage(
    picto: Pictogram,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    highContrast: Boolean = false
) {
    val context = LocalContext.current

    val bitmap = remember(picto.id, picto.arasaacId, picto.localImagePath) {
        ArasaacImageHelper.loadPictogramBitmap(context, picto)?.asImageBitmap()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier
        )
    } else if (picto.icon != null) {
        Icon(
            imageVector = picto.icon,
            contentDescription = null,
            tint = if (highContrast) Color.White else picto.category.textColor,
            modifier = modifier
        )
    } else {
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = null,
            tint = if (highContrast) Color.LightGray else picto.category.textColor.copy(alpha = 0.6f),
            modifier = modifier
        )
    }
}
