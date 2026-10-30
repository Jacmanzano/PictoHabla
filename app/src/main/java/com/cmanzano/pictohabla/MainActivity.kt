package com.cmanzano.pictohabla

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.cmanzano.pictohabla.core.database.AppDatabase
import com.cmanzano.pictohabla.core.tts.TtsManager
import com.cmanzano.pictohabla.data.repository.BoardRepository
import com.cmanzano.pictohabla.data.repository.HistoryRepository
import com.cmanzano.pictohabla.data.repository.PictogramRepository
import com.cmanzano.pictohabla.data.repository.SettingsRepository
import com.cmanzano.pictohabla.navigation.MainNavigation
import com.cmanzano.pictohabla.ui.theme.PictoHablaTheme

/**
 * Actividad principal de PictoHabla.
 * Inicializa la base de datos local Room, los repositorios persistentes y los servicios centrales del dispositivo.
 */
class MainActivity : ComponentActivity() {

    private lateinit var ttsManager: TtsManager
    private lateinit var database: AppDatabase
    private lateinit var pictogramRepository: PictogramRepository
    private lateinit var boardRepository: BoardRepository
    private lateinit var historyRepository: HistoryRepository
    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicialización de la base de datos y repositorios locales (Offline-First)
        database = AppDatabase.getInstance(this)
        pictogramRepository = PictogramRepository(database.pictogramDao(), database.favoriteDao())
        boardRepository = BoardRepository(database.boardDao(), database.pictogramDao())
        historyRepository = HistoryRepository(database.historyDao())
        settingsRepository = SettingsRepository(this)
        ttsManager = TtsManager(this)

        enableEdgeToEdge()

        setContent {
            val userSettings by settingsRepository.settings.collectAsState()

            PictoHablaTheme(darkTheme = userSettings.highContrast) {
                MainNavigation(
                    pictogramRepository = pictogramRepository,
                    historyRepository = historyRepository,
                    settingsRepository = settingsRepository,
                    boardRepository = boardRepository,
                    ttsManager = ttsManager
                )
            }
        }
    }

    override fun onDestroy() {
        ttsManager.shutdown()
        super.onDestroy()
    }
}
