package com.cmanzano.pictohabla.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.cmanzano.pictohabla.core.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repositorio de preferencias y ajustes de usuario persistidos en SharedPreferences.
 * Expone un StateFlow reactivo para que toda la interfaz se actualice de inmediato.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private fun loadSettings(): UserSettings {
        return UserSettings(
            languageCode = prefs.getString(KEY_LANGUAGE, "ES") ?: "ES",
            speechSpeed = prefs.getFloat(KEY_SPEED, 1.0f),
            speechPitch = prefs.getFloat(KEY_PITCH, 1.0f),
            highContrast = prefs.getBoolean(KEY_HIGH_CONTRAST, false),
            largeGrid = prefs.getBoolean(KEY_LARGE_GRID, false),
            hapticsEnabled = prefs.getBoolean(KEY_HAPTICS, true),
            activeBoardId = prefs.getLong(KEY_ACTIVE_BOARD, 1L)
        )
    }

    fun setLanguage(code: String) {
        prefs.edit().putString(KEY_LANGUAGE, code).apply()
        _settings.value = _settings.value.copy(languageCode = code)
    }

    fun setSpeechSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        prefs.edit().putFloat(KEY_SPEED, clamped).apply()
        _settings.value = _settings.value.copy(speechSpeed = clamped)
    }

    fun setSpeechPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.5f, 2.0f)
        prefs.edit().putFloat(KEY_PITCH, clamped).apply()
        _settings.value = _settings.value.copy(speechPitch = clamped)
    }

    fun setHighContrast(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HIGH_CONTRAST, enabled).apply()
        _settings.value = _settings.value.copy(highContrast = enabled)
    }

    fun setLargeGrid(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LARGE_GRID, enabled).apply()
        _settings.value = _settings.value.copy(largeGrid = enabled)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTICS, enabled).apply()
        _settings.value = _settings.value.copy(hapticsEnabled = enabled)
    }

    fun setActiveBoardId(boardId: Long) {
        prefs.edit().putLong(KEY_ACTIVE_BOARD, boardId).apply()
        _settings.value = _settings.value.copy(activeBoardId = boardId)
    }

    companion object {
        private const val PREFS_NAME = "pictohabla_user_preferences"
        private const val KEY_LANGUAGE = "key_language"
        private const val KEY_SPEED = "key_speed"
        private const val KEY_PITCH = "key_pitch"
        private const val KEY_HIGH_CONTRAST = "key_high_contrast"
        private const val KEY_LARGE_GRID = "key_large_grid"
        private const val KEY_HAPTICS = "key_haptics"
        private const val KEY_ACTIVE_BOARD = "key_active_board"
    }
}
