package com.cmanzano.pictohabla.feature.board

/**
 * Política adaptativa de distribución espacial para el tablero de comunicación CAA (AACBoardScreen).
 *
 * Determina con precisión matemática la distribución adecuada para cada dispositivo y orientación:
 * - Teléfono en vertical: Diseño vertical con cabecera, barra de frase superior, selector y cuadrícula.
 * - Teléfono en horizontal: Nuevo diseño compacto de dos zonas (panel de frase ~33% a la izquierda y selector+cuadrícula a la derecha).
 * - Tablet en vertical: Diseño vertical amplio.
 * - Tablet en horizontal: Diseño existente con barra lateral de categorías a la izquierda y frase/cuadrícula a la derecha.
 */
object AACBoardLayoutPolicy {

    enum class BoardLayoutMode {
        PHONE_PORTRAIT,
        PHONE_LANDSCAPE_TWO_ZONE,
        TABLET_PORTRAIT,
        TABLET_LANDSCAPE_SIDEBAR
    }

    /**
     * Determina si la ventana corresponde a un teléfono en orientación horizontal
     * o a una ventana apaisada de baja altura disponible.
     *
     * @param isOrientationLandscape Indica si la orientación física o de ventana es apaisada.
     * @param screenWidthDp Ancho total de la pantalla en dp.
     * @param screenHeightDp Alto total de la pantalla en dp.
     * @param availableWidthDp Ancho disponible para el contenido en dp.
     * @param availableHeightDp Alto disponible para el contenido en dp (excluyendo barras del sistema).
     */
    fun isPhoneLandscape(
        isOrientationLandscape: Boolean,
        screenWidthDp: Int,
        screenHeightDp: Int,
        availableWidthDp: Float = screenWidthDp.toFloat(),
        availableHeightDp: Float = screenHeightDp.toFloat()
    ): Boolean {
        val isLandscapeWindow = isOrientationLandscape || availableWidthDp > availableHeightDp
        return isLandscapeWindow && (screenHeightDp < 500 || availableHeightDp < 460f)
    }

    /**
     * Resuelve el modo de distribución exacto para el tablero según las dimensiones reales de la ventana.
     */
    fun resolveLayoutMode(
        isOrientationLandscape: Boolean,
        screenWidthDp: Int,
        screenHeightDp: Int,
        availableWidthDp: Float = screenWidthDp.toFloat(),
        availableHeightDp: Float = screenHeightDp.toFloat()
    ): BoardLayoutMode {
        val isPhoneLs = isPhoneLandscape(
            isOrientationLandscape = isOrientationLandscape,
            screenWidthDp = screenWidthDp,
            screenHeightDp = screenHeightDp,
            availableWidthDp = availableWidthDp,
            availableHeightDp = availableHeightDp
        )

        if (isPhoneLs) {
            return BoardLayoutMode.PHONE_LANDSCAPE_TWO_ZONE
        }

        val isLandscape = isOrientationLandscape || availableWidthDp > availableHeightDp
        return if (isLandscape) {
            BoardLayoutMode.TABLET_LANDSCAPE_SIDEBAR
        } else {
            val isTabletWidth = screenWidthDp >= 600 || availableWidthDp >= 600f
            if (isTabletWidth) BoardLayoutMode.TABLET_PORTRAIT else BoardLayoutMode.PHONE_PORTRAIT
        }
    }
}
