package com.cmanzano.pictohabla.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.cmanzano.pictohabla.core.model.Board
import com.cmanzano.pictohabla.core.model.Pictogram
import com.cmanzano.pictohabla.core.tts.TtsManager
import com.cmanzano.pictohabla.data.repository.BoardRepository
import com.cmanzano.pictohabla.data.repository.HistoryRepository
import com.cmanzano.pictohabla.data.repository.PictogramRepository
import com.cmanzano.pictohabla.data.repository.SettingsRepository
import com.cmanzano.pictohabla.feature.board.AACBoardScreen
import com.cmanzano.pictohabla.feature.boardmanager.BoardEditorScreen
import com.cmanzano.pictohabla.feature.boardmanager.BoardsManagerScreen
import com.cmanzano.pictohabla.feature.custompicto.CustomPictoScreen
import com.cmanzano.pictohabla.feature.favorites.FavoritesScreen
import com.cmanzano.pictohabla.feature.history.HistoryScreen
import com.cmanzano.pictohabla.feature.learning.LearningScreen
import com.cmanzano.pictohabla.feature.settings.SettingsScreen
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Destinos de navegación de PictoHabla.
 */
enum class PictoScreen {
    BOARD,
    FAVORITES,
    HISTORY,
    LEARN,
    SETTINGS,
    BOARDS_MANAGER,
    BOARD_EDITOR,
    CUSTOM_PICTO
}

/**
 * Contenedor de navegación principal conectado a la persistencia local de Room,
 * gestión de tableros, creador de pictogramas y repositorios de PictoHabla.
 */
@Composable
fun MainNavigation(
    pictogramRepository: PictogramRepository,
    historyRepository: HistoryRepository,
    settingsRepository: SettingsRepository,
    boardRepository: BoardRepository,
    ttsManager: TtsManager
) {
    val coroutineScope = rememberCoroutineScope()

    // Inicialización idempotente del catálogo y tablero predeterminado en SQLite
    LaunchedEffect(Unit) {
        pictogramRepository.initializeCatalogIfNeeded()
        boardRepository.initializeDefaultBoardIfNeeded()
    }

    // Flujos observables de datos persistidos
    val allPictograms by pictogramRepository.getAllPictograms().collectAsState(initial = emptyList())
    val favoriteIds by pictogramRepository.getFavoriteIds().collectAsState(initial = emptySet())
    val favoritePictos by pictogramRepository.getFavoritePictograms().collectAsState(initial = emptyList())
    val historyList by historyRepository.getHistory().collectAsState(initial = emptyList())
    val userSettings by settingsRepository.settings.collectAsState()
    val allBoards by boardRepository.getAllBoards().collectAsState(initial = emptyList())

    // Cálculo del ID de tablero activo
    val effectiveActiveBoardId = userSettings.activeBoardId
        ?: allBoards.firstOrNull { it.isDefault }?.id
        ?: allBoards.firstOrNull()?.id
        ?: 1L

    val activeBoardWithPictos by remember(effectiveActiveBoardId) {
        boardRepository.getBoardWithPictograms(effectiveActiveBoardId)
    }.collectAsState(initial = null)

    var currentScreen by remember { mutableStateOf(PictoScreen.BOARD) }
    var currentPhrase by remember { mutableStateOf(listOf<Pictogram>()) }
    var editingBoardId by remember { mutableStateOf<Long?>(null) }
    var editingCustomPicto by remember { mutableStateOf<Pictogram?>(null) }

    val currentEditingBoard by remember(editingBoardId) {
        if (editingBoardId != null) {
            boardRepository.getBoardWithPictograms(editingBoardId!!)
        } else {
            flowOf<Board?>(null)
        }
    }.collectAsState(initial = null)

    AnimatedContent(
        targetState = currentScreen,
        label = "main_screen_transition",
        transitionSpec = { fadeIn() togetherWith fadeOut() }
    ) { screen ->
        when (screen) {
            PictoScreen.BOARD -> AACBoardScreen(
                activeBoard = activeBoardWithPictos,
                allPictograms = allPictograms,
                favoriteIds = favoriteIds,
                currentPhrase = currentPhrase,
                onPhraseChange = { currentPhrase = it },
                languageCode = userSettings.languageCode,
                speechSpeed = userSettings.speechSpeed,
                speechPitch = userSettings.speechPitch,
                highContrast = userSettings.highContrast,
                hapticsEnabled = userSettings.hapticsEnabled,
                largeGrid = userSettings.largeGrid,
                onSpeakPhrase = { spokenText, pictoIds ->
                    val success = ttsManager.speak(
                        text = spokenText,
                        languageCode = userSettings.languageCode,
                        speed = userSettings.speechSpeed,
                        pitch = userSettings.speechPitch
                    )
                    if (success && spokenText.isNotBlank()) {
                        coroutineScope.launch {
                            historyRepository.recordPhrase(
                                spokenText = spokenText,
                                pictoIds = pictoIds,
                                languageCode = userSettings.languageCode
                            )
                        }
                    }
                },
                onToggleFavorite = { pictoId ->
                    coroutineScope.launch {
                        pictogramRepository.toggleFavorite(pictoId)
                    }
                },
                onNavigateFavorites = { currentScreen = PictoScreen.FAVORITES },
                onNavigateHistory = { currentScreen = PictoScreen.HISTORY },
                onNavigateSettings = { currentScreen = PictoScreen.SETTINGS },
                onNavigateLearn = { currentScreen = PictoScreen.LEARN },
                onNavigateBoards = { currentScreen = PictoScreen.BOARDS_MANAGER },
                onNavigateCustomPicto = {
                    editingCustomPicto = null
                    currentScreen = PictoScreen.CUSTOM_PICTO
                }
            )

            PictoScreen.BOARDS_MANAGER -> BoardsManagerScreen(
                boards = allBoards,
                activeBoardId = effectiveActiveBoardId,
                onSelectActiveBoard = { boardId ->
                    settingsRepository.setActiveBoardId(boardId)
                },
                onEditBoard = { boardId ->
                    editingBoardId = boardId
                    currentScreen = PictoScreen.BOARD_EDITOR
                },
                onCreateBoard = { name, description, columns ->
                    coroutineScope.launch {
                        val newId = boardRepository.createBoard(name, description, columns)
                        editingBoardId = newId
                        currentScreen = PictoScreen.BOARD_EDITOR
                    }
                },
                onDuplicateBoard = { boardId ->
                    coroutineScope.launch {
                        boardRepository.duplicateBoard(boardId, "")
                    }
                },
                onDeleteBoard = { boardId ->
                    coroutineScope.launch {
                        boardRepository.deleteBoard(boardId)
                        if (userSettings.activeBoardId == boardId) {
                            settingsRepository.setActiveBoardId(1L)
                        }
                    }
                },
                onBack = { currentScreen = PictoScreen.BOARD },
                highContrast = userSettings.highContrast,
                hapticsEnabled = userSettings.hapticsEnabled
            )

            PictoScreen.BOARD_EDITOR -> BoardEditorScreen(
                board = currentEditingBoard,
                allAvailablePictograms = allPictograms,
                languageCode = userSettings.languageCode,
                highContrast = userSettings.highContrast,
                hapticsEnabled = userSettings.hapticsEnabled,
                onUpdateBoardMetadata = { name, description, columns ->
                    coroutineScope.launch {
                        if (editingBoardId != null) {
                            boardRepository.updateBoard(editingBoardId!!, name, description, columns)
                        }
                    }
                },
                onMovePictogram = { pictoId, moveLeft ->
                    coroutineScope.launch {
                        if (editingBoardId != null) {
                            boardRepository.movePictogram(editingBoardId!!, pictoId, moveLeft)
                        }
                    }
                },
                onRemovePictogram = { pictoId ->
                    coroutineScope.launch {
                        if (editingBoardId != null) {
                            boardRepository.removePictogramFromBoard(editingBoardId!!, pictoId)
                        }
                    }
                },
                onAddPictograms = { selectedIds ->
                    coroutineScope.launch {
                        if (editingBoardId != null) {
                            selectedIds.forEach { pictoId ->
                                boardRepository.addPictogramToBoard(editingBoardId!!, pictoId)
                            }
                        }
                    }
                },
                onBack = { currentScreen = PictoScreen.BOARDS_MANAGER }
            )

            PictoScreen.CUSTOM_PICTO -> CustomPictoScreen(
                editingPicto = editingCustomPicto,
                languageCode = userSettings.languageCode,
                highContrast = userSettings.highContrast,
                hapticsEnabled = userSettings.hapticsEnabled,
                onSavePicto = { name, speechText, category, localImagePath ->
                    coroutineScope.launch {
                        if (editingCustomPicto == null) {
                            val newPicto = pictogramRepository.createCustomPictogram(
                                name = name,
                                speechText = speechText,
                                category = category,
                                localImagePath = localImagePath
                            )
                            // Lo añadimos automáticamente al tablero activo
                            boardRepository.addPictogramToBoard(effectiveActiveBoardId, newPicto.id)
                        } else {
                            pictogramRepository.updateCustomPictogram(
                                id = editingCustomPicto!!.id,
                                name = name,
                                speechText = speechText,
                                category = category,
                                localImagePath = localImagePath
                            )
                        }
                        currentScreen = PictoScreen.BOARD
                    }
                },
                onDeletePicto = { id ->
                    coroutineScope.launch {
                        pictogramRepository.deleteCustomPictogram(id)
                        currentScreen = PictoScreen.BOARD
                    }
                },
                onBack = { currentScreen = PictoScreen.BOARD }
            )

            PictoScreen.FAVORITES -> FavoritesScreen(
                favoritePictos = favoritePictos,
                languageCode = userSettings.languageCode,
                highContrast = userSettings.highContrast,
                hapticsEnabled = userSettings.hapticsEnabled,
                largeGrid = userSettings.largeGrid,
                onPictoClick = { picto ->
                    currentPhrase = currentPhrase + picto
                    ttsManager.speak(
                        text = picto.getSpokenText(userSettings.languageCode),
                        languageCode = userSettings.languageCode,
                        speed = userSettings.speechSpeed,
                        pitch = userSettings.speechPitch
                    )
                },
                onToggleFavorite = { pictoId ->
                    coroutineScope.launch {
                        pictogramRepository.toggleFavorite(pictoId)
                    }
                },
                onBack = { currentScreen = PictoScreen.BOARD },
                onNavigateTablero = { currentScreen = PictoScreen.BOARD },
                onNavigateHistory = { currentScreen = PictoScreen.HISTORY },
                onNavigateLearn = { currentScreen = PictoScreen.LEARN }
            )

            PictoScreen.HISTORY -> HistoryScreen(
                historyList = historyList,
                highContrast = userSettings.highContrast,
                hapticsEnabled = userSettings.hapticsEnabled,
                onSpeakPhrase = { text ->
                    ttsManager.speak(
                        text = text,
                        languageCode = userSettings.languageCode,
                        speed = userSettings.speechSpeed,
                        pitch = userSettings.speechPitch
                    )
                },
                onRecoverPhrase = { historyItem ->
                    coroutineScope.launch {
                        val recoveredPictos = pictogramRepository.getPictogramsByIds(historyItem.pictoIds)
                        currentPhrase = recoveredPictos
                        currentScreen = PictoScreen.BOARD
                    }
                },
                onDeleteEntry = { id ->
                    coroutineScope.launch {
                        historyRepository.deleteEntry(id)
                    }
                },
                onClearAll = {
                    coroutineScope.launch {
                        historyRepository.clearHistory()
                    }
                },
                onBack = { currentScreen = PictoScreen.BOARD },
                onNavigateTablero = { currentScreen = PictoScreen.BOARD },
                onNavigateFavorites = { currentScreen = PictoScreen.FAVORITES },
                onNavigateLearn = { currentScreen = PictoScreen.LEARN }
            )

            PictoScreen.LEARN -> LearningScreen(
                languageCode = userSettings.languageCode,
                highContrast = userSettings.highContrast,
                hapticsEnabled = userSettings.hapticsEnabled,
                onSpeakWord = { word ->
                    ttsManager.speak(
                        text = word,
                        languageCode = userSettings.languageCode,
                        speed = userSettings.speechSpeed,
                        pitch = userSettings.speechPitch
                    )
                },
                onBack = { currentScreen = PictoScreen.BOARD },
                onNavigateTablero = { currentScreen = PictoScreen.BOARD },
                onNavigateFavorites = { currentScreen = PictoScreen.FAVORITES },
                onNavigateHistory = { currentScreen = PictoScreen.HISTORY }
            )

            PictoScreen.SETTINGS -> SettingsScreen(
                currentLanguage = userSettings.languageCode,
                onLanguageChange = { settingsRepository.setLanguage(it) },
                highContrast = userSettings.highContrast,
                onContrastToggle = { settingsRepository.setHighContrast(it) },
                speed = userSettings.speechSpeed,
                onSpeedChange = { settingsRepository.setSpeechSpeed(it) },
                pitch = userSettings.speechPitch,
                onPitchChange = { settingsRepository.setSpeechPitch(it) },
                hapticsEnabled = userSettings.hapticsEnabled,
                onHapticsToggle = { settingsRepository.setHapticsEnabled(it) },
                largeGrid = userSettings.largeGrid,
                onLargeGridToggle = { settingsRepository.setLargeGrid(it) },
                onTestVoice = {
                    val testPhrase = when (userSettings.languageCode) {
                        "EN" -> "Hello, this is my PictoHabla voice"
                        "FR" -> "Bonjour, ceci est ma voix PictoHabla"
                        "DE" -> "Hallo, das ist meine PictoHabla Stimme"
                        "IT" -> "Ciao, questa è la mia voce PictoHabla"
                        else -> "Hola, esta es mi voz en PictoHabla"
                    }
                    ttsManager.speak(
                        text = testPhrase,
                        languageCode = userSettings.languageCode,
                        speed = userSettings.speechSpeed,
                        pitch = userSettings.speechPitch
                    )
                },
                onBack = { currentScreen = PictoScreen.BOARD }
            )
        }
    }
}
