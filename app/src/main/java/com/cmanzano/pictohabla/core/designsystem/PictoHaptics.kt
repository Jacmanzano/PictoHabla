package com.cmanzano.pictohabla.core.designsystem

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Utilidad accesible para emitir retroalimentación háptica adaptativa.
 * Permite a personas con dificultades motoras o visuales tener confirmación táctil
 * de cada interacción con la pantalla.
 */
object PictoHaptics {

    /**
     * Feedback ligero al pulsar un pictograma o botón estándar.
     */
    fun performClick(hapticFeedback: HapticFeedback?, enabled: Boolean = true) {
        if (enabled && hapticFeedback != null) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    /**
     * Feedback más pronunciado para acciones importantes (reproducir frase, guardar, acierto).
     */
    fun performEmphasis(hapticFeedback: HapticFeedback?, enabled: Boolean = true) {
        if (enabled && hapticFeedback != null) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}
