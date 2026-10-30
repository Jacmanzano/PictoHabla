package com.cmanzano.pictohabla.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dimensiones, espaciados y objetivos táctiles accesibles para PictoHabla.
 * Cumple con los requerimientos de accesibilidad motora (72-96 dp en controles clave).
 */
object PictoDimens {
    // Espaciados estándar
    val SpaceExtraSmall: Dp = 4.dp
    val SpaceSmall: Dp = 8.dp
    val SpaceMedium: Dp = 12.dp
    val SpaceLarge: Dp = 16.dp
    val SpaceExtraLarge: Dp = 24.dp
    val SpaceSection: Dp = 16.dp
    val SpaceBetweenSections: Dp = 12.dp
    val HeaderPaddingHorizontal: Dp = 16.dp
    val HeaderPaddingVertical: Dp = 12.dp

    // Objetivos táctiles mínimos y recomendados para diversidad motora
    val TouchTargetMinimum: Dp = 48.dp
    val TouchTargetStandard: Dp = 64.dp
    val TouchTargetLarge: Dp = 76.dp
    val TouchTargetExtraLarge: Dp = 92.dp

    // Tamaños de tarjetas de pictogramas en la cuadrícula
    val CardMinSizePortraitStandard: Dp = 110.dp
    val CardMinSizePortraitLarge: Dp = 140.dp
    val CardMinSizeLandscapeStandard: Dp = 135.dp
    val CardMinSizeLandscapeLarge: Dp = 165.dp

    val CardHeightPortraitStandard: Dp = 120.dp
    val CardHeightPortraitLarge: Dp = 145.dp
    val CardHeightLandscapeStandard: Dp = 135.dp
    val CardHeightLandscapeLarge: Dp = 160.dp

    // Espaciado de la cuadrícula
    val CardGridSpacing: Dp = 14.dp
    val GridHorizontalPadding: Dp = 16.dp

    // Tamaño de pictogramas en la barra de frases
    val PhraseBarPictoSize: Dp = 84.dp
    val PhraseBarMinHeight: Dp = 96.dp

    // Controles y barras
    val SearchFieldHeight: Dp = 56.dp
    val CategoryChipHeight: Dp = 44.dp
    val BottomNavHeight: Dp = 68.dp

    // Iconos de pictogramas
    val IconSizeSmall: Dp = 24.dp
    val IconSizeMedium: Dp = 36.dp
    val IconSizeLarge: Dp = 56.dp
    val IconSizeExtraLarge: Dp = 72.dp

    // Bordes
    val BorderWidthThin: Dp = 1.dp
    val BorderWidthStandard: Dp = 2.5.dp
    val BorderWidthThick: Dp = 3.5.dp
    val BorderWidthHighContrast: Dp = 4.dp

    // Formas de esquinas redondeadas
    val CornerSmall = RoundedCornerShape(8.dp)
    val CornerMedium = RoundedCornerShape(16.dp)
    val CornerLarge = RoundedCornerShape(20.dp)
    val CornerExtraLarge = RoundedCornerShape(28.dp)
    val CornerPill = RoundedCornerShape(50)

    // Elevaciones
    val ElevationNone: Dp = 0.dp
    val ElevationLow: Dp = 2.dp
    val ElevationStandard: Dp = 4.dp
    val ElevationHigh: Dp = 8.dp
}
