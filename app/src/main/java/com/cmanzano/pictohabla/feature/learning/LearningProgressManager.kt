package com.cmanzano.pictohabla.feature.learning

import android.content.Context
import android.content.SharedPreferences
import com.cmanzano.pictohabla.feature.learning.model.LearningActivityType
import com.cmanzano.pictohabla.feature.learning.model.LearningStats

/**
 * Gestor de seguimiento local del aprendizaje en PictoHabla.
 * Almacena exclusivamente en el almacenamiento local del dispositivo (SharedPreferences)
 * las métricas educativas básicas sin requerir cuentas de usuario ni transmitir datos a servidores.
 */
class LearningProgressManager(
    private val preferences: SharedPreferences
) {

    companion object {
        private const val PREFS_NAME = "pictohabla_learning_progress"
        private const val KEY_TOTAL_COMPLETED = "stat_total_completed"
        private const val KEY_TOTAL_HITS = "stat_total_hits"
        private const val PREFIX_MODE_COUNT = "stat_mode_"

        @Volatile
        private var instance: LearningProgressManager? = null

        fun getInstance(context: Context): LearningProgressManager {
            return instance ?: synchronized(this) {
                instance ?: LearningProgressManager(
                    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ).also { instance = it }
            }
        }
    }

    /**
     * Registra el resultado de un ejercicio completado.
     */
    fun recordResult(mode: LearningActivityType, isHit: Boolean) {
        val totalCompleted = preferences.getInt(KEY_TOTAL_COMPLETED, 0) + 1
        val totalHits = preferences.getInt(KEY_TOTAL_HITS, 0) + (if (isHit) 1 else 0)
        val modeKey = PREFIX_MODE_COUNT + mode.name
        val modeCompleted = preferences.getInt(modeKey, 0) + 1

        preferences.edit()
            .putInt(KEY_TOTAL_COMPLETED, totalCompleted)
            .putInt(KEY_TOTAL_HITS, totalHits)
            .putInt(modeKey, modeCompleted)
            .apply()
    }

    /**
     * Obtiene las estadísticas de progreso acumuladas.
     */
    fun getStats(): LearningStats {
        val totalCompleted = preferences.getInt(KEY_TOTAL_COMPLETED, 0)
        val totalHits = preferences.getInt(KEY_TOTAL_HITS, 0)

        val completedByMode = mutableMapOf<LearningActivityType, Int>()
        for (type in LearningActivityType.entries) {
            val count = preferences.getInt(PREFIX_MODE_COUNT + type.name, 0)
            if (count > 0) {
                completedByMode[type] = count
            }
        }

        return LearningStats(
            totalCompleted = totalCompleted,
            totalHits = totalHits,
            completedByMode = completedByMode
        )
    }

    /**
     * Reinicia las estadísticas educativas locales.
     */
    fun resetProgress() {
        preferences.edit().clear().apply()
    }
}
