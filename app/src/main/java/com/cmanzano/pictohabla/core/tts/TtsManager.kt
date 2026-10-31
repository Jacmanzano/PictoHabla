package com.cmanzano.pictohabla.core.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Estados del motor Text-To-Speech (TTS).
 */
sealed interface TtsState {
    data object Initializing : TtsState
    data object Ready : TtsState
    data class Speaking(val text: String) : TtsState
    data class Error(val message: String) : TtsState
}

/**
 * Gestor robusto del motor Text-To-Speech (TTS) de Android.
 * - Soporta inicialización asíncrona no bloqueante.
 * - Comprobación de disponibilidad de idiomas con fallback a español.
 * - Notificación de estado mediante StateFlow.
 * - Retorna un valor booleano en `speak` indicando si la locución fue enviada con éxito.
 */
class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)

    private val _state = MutableStateFlow<TtsState>(TtsState.Initializing)
    val state: StateFlow<TtsState> = _state.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            setupUtteranceListener()
            _isReady.value = true
            _state.value = TtsState.Ready
            _errorMessage.value = null
        } else {
            _isReady.value = false
            val error = "El sintetizador de voz no está disponible en este dispositivo."
            _errorMessage.value = error
            _state.value = TtsState.Error(error)
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _state.value = TtsState.Speaking(utteranceId ?: "")
            }

            override fun onDone(utteranceId: String?) {
                _state.value = TtsState.Ready
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _state.value = TtsState.Ready
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _state.value = TtsState.Ready
            }
        })
    }

    /**
     * Pronuncia una cadena de texto.
     * @return `true` si la frase fue aceptada y enviada a la cola del sintetizador, `false` si hubo un error o está vacío.
     */
    fun speak(
        text: String,
        languageCode: String = "ES",
        speed: Float = 1.0f,
        pitch: Float = 1.0f
    ): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        val activeTts = tts

        if (activeTts == null || !_isReady.value) {
            _errorMessage.value = "El sintetizador de voz aún se está iniciando."
            return false
        }

        return try {
            val targetLocale = when (languageCode.uppercase()) {
                "EN" -> Locale.ENGLISH
                else -> Locale.forLanguageTag("es-ES")
            }

            val langResult = activeTts.setLanguage(targetLocale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback seguro a español o locale por defecto si el idioma específico no está instalado
                activeTts.language = Locale.forLanguageTag("es-ES")
            }

            activeTts.setSpeechRate(speed.coerceIn(0.5f, 2.0f))
            activeTts.setPitch(pitch.coerceIn(0.5f, 2.0f))

            val utteranceId = "PICTO_${System.currentTimeMillis()}"
            val queueResult = activeTts.speak(trimmed, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            queueResult == TextToSpeech.SUCCESS
        } catch (e: Exception) {
            _errorMessage.value = "Error al sintetizar voz: ${e.localizedMessage}"
            false
        }
    }

    /**
     * Detiene la locución en curso.
     */
    fun stop() {
        tts?.stop()
        _state.value = TtsState.Ready
    }

    /**
     * Libera los recursos del motor TTS.
     */
    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        } finally {
            tts = null
            _isReady.value = false
            _state.value = TtsState.Error("Motor de voz liberado")
        }
    }
}
