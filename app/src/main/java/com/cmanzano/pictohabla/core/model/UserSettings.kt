package com.cmanzano.pictohabla.core.model

/**
 * Ajustes y preferencias de usuario persistentes de PictoHabla.
 */
data class UserSettings(
    val languageCode: String = "ES",
    val speechSpeed: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val highContrast: Boolean = false,
    val largeGrid: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val activeBoardId: Long = 1L
)
