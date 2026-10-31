package com.cmanzano.pictohabla.feature.board

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmanzano.pictohabla.R
import com.cmanzano.pictohabla.core.designsystem.PictoColors
import com.cmanzano.pictohabla.core.designsystem.PictoDimens
import com.cmanzano.pictohabla.core.designsystem.PictoHaptics
import com.cmanzano.pictohabla.core.designsystem.PictoTypography
import com.cmanzano.pictohabla.core.model.Board
import com.cmanzano.pictohabla.core.model.Category
import com.cmanzano.pictohabla.core.model.Pictogram
import com.cmanzano.pictohabla.core.util.normalizeForSearch
import com.cmanzano.pictohabla.core.util.processPhraseForSpeech
import com.cmanzano.pictohabla.feature.board.components.LandscapePhraseSidePanel
import com.cmanzano.pictohabla.feature.board.components.PhraseBar
import com.cmanzano.pictohabla.feature.board.components.PictogramCard
import com.cmanzano.pictohabla.feature.settings.AdultGateDialog
import com.cmanzano.pictohabla.navigation.PictoBottomNavBar
import com.cmanzano.pictohabla.navigation.PictoScreen

/**
 * Pantalla principal del comunicador aumentativo (Tablero CAA) - FASE 4.5.
 * Rediseño visual centrado en claridad, jerarquía y respiración entre elementos:
 * 1. Cabecera limpia con protagonismo de «PictoHabla» y acceso al menú protegido.
 * 2. Barra de frase espaciosa con fila horizontal de pictogramas y controles de acción diferenciados.
 * 3. Buscador con espacio propio y esquinas redondeadas.
 * 4. Selector de categorías mediante píldoras horizontales legibles y sin truncar.
 * 5. Cuadrícula de pictogramas con separación consistente y alto contraste.
 * 6. Navegación principal inferior accesible (Tablero, Favoritos, Historial, Aprender).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AACBoardScreen(
    activeBoard: Board?,
    allPictograms: List<Pictogram>,
    favoriteIds: Set<Int>,
    currentPhrase: List<Pictogram>,
    onPhraseChange: (List<Pictogram>) -> Unit,
    languageCode: String,
    speechSpeed: Float,
    speechPitch: Float,
    highContrast: Boolean,
    hapticsEnabled: Boolean = true,
    largeGrid: Boolean = false,
    onSpeakPhrase: (String, List<Int>) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onNavigateFavorites: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateLearn: () -> Unit,
    onNavigateBoards: () -> Unit,
    onNavigateCustomPicto: () -> Unit,
    onNavigateArasaacSearch: () -> Unit = {}
) {
    val hapticFeedback = LocalHapticFeedback.current

    // null representa la vista del tablero activo personalizado.
    // Si tiene valor de Category, muestra los pictogramas de esa categoría del catálogo general.
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var searchText by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var pendingAdultNavigation by remember { mutableStateOf<(() -> Unit)?>(null) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Pictogramas a mostrar según la pestaña y la búsqueda
    val displayedPictograms = remember(
        allPictograms,
        activeBoard,
        selectedCategory,
        searchText,
        languageCode
    ) {
        if (searchText.isNotBlank()) {
            allPictograms.filter { picto ->
                picto.matchesSearch(searchText, languageCode)
            }
        } else if (selectedCategory == null) {
            // Modo "Mi Tablero"
            if (activeBoard != null && activeBoard.pictograms.isNotEmpty()) {
                activeBoard.pictograms
            } else {
                allPictograms.take(16)
            }
        } else {
            // Modo categoría específica
            allPictograms.filter { it.category == selectedCategory }
        }
    }

    // Configuración adaptativa de columnas
    val activeColumns = activeBoard?.columnsCount ?: 3
    val gridCells = if (selectedCategory == null && searchText.isBlank()) {
        GridCells.Fixed(if (isLandscape) (activeColumns + 1).coerceAtMost(6) else activeColumns)
    } else {
        val minCardWidth = if (isLandscape) {
            if (largeGrid) PictoDimens.CardMinSizeLandscapeLarge else PictoDimens.CardMinSizeLandscapeStandard
        } else {
            if (largeGrid) PictoDimens.CardMinSizePortraitLarge else PictoDimens.CardMinSizePortraitStandard
        }
        GridCells.Adaptive(minSize = minCardWidth)
    }

    val gridCardHeight = if (isLandscape) {
        if (largeGrid) PictoDimens.CardHeightLandscapeLarge else PictoDimens.CardHeightLandscapeStandard
    } else {
        if (largeGrid) PictoDimens.CardHeightPortraitLarge else PictoDimens.CardHeightPortraitStandard
    }

    val isPhoneLandscapeDevice = isLandscape && configuration.screenHeightDp < 500

    Scaffold(
        topBar = {
            // Cabecera limpia y profesional: «PictoHabla» con protagonismo absoluto
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = if (highContrast) Color.Black else MaterialTheme.colorScheme.surface,
                shadowElevation = if (highContrast) PictoDimens.ElevationNone else PictoDimens.ElevationLow,
                border = if (highContrast) BorderStroke(PictoDimens.BorderWidthThin, Color.White) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = PictoDimens.HeaderPaddingHorizontal,
                            vertical = if (isPhoneLandscapeDevice) PictoDimens.SpaceExtraSmall else PictoDimens.HeaderPaddingVertical
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Identidad visual oficial: Logotipo y nombre de la aplicación
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (isPhoneLandscapeDevice) 6.dp else PictoDimens.SpaceSmall)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_pictohabla_symbol),
                            contentDescription = "Logotipo de PictoHabla",
                            modifier = Modifier.size(if (isPhoneLandscapeDevice) 26.dp else 36.dp),
                            contentScale = ContentScale.Fit
                        )
                        Column {
                            Text(
                                text = "PictoHabla",
                                style = if (isPhoneLandscapeDevice) {
                                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                                } else {
                                    PictoTypography.AppHeaderTitle
                                },
                                color = if (highContrast) PictoColors.HighlightHighContrast else PictoColors.Primary
                            )
                            if (!isPhoneLandscapeDevice) {
                                Text(
                                    text = "Comunícate a tu manera",
                                    style = PictoTypography.AppHeaderSubtitle,
                                    color = if (highContrast) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Acceso a opciones del sistema (menú protegido para cuidadores)
                    Box {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                                    menuExpanded = true
                                },
                                modifier = Modifier
                                    .size(PictoDimens.TouchTargetMinimum)
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = "Abrir menú general de opciones y ajustes para cuidadores"
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = null,
                                    tint = if (highContrast) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Menú desplegable protegido por Adult Gate
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Crear pictograma con foto") },
                                onClick = {
                                    menuExpanded = false
                                    pendingAdultNavigation = onNavigateCustomPicto
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Buscar en ARASAAC") },
                                onClick = {
                                    menuExpanded = false
                                    pendingAdultNavigation = onNavigateArasaacSearch
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                                }
                            )

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text("Ajustes del sistema") },
                                onClick = {
                                    menuExpanded = false
                                    pendingAdultNavigation = onNavigateSettings
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Settings, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Barra de navegación principal inferior accesible
            PictoBottomNavBar(
                currentScreen = PictoScreen.BOARD,
                onNavigateTablero = { /* ya en tablero */ },
                onNavigateFavorites = onNavigateFavorites,
                onNavigateHistory = onNavigateHistory,
                onNavigateLearn = onNavigateLearn,
                highContrast = highContrast,
                hapticsEnabled = hapticsEnabled
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            val isPhoneLandscape = AACBoardLayoutPolicy.isPhoneLandscape(
                isOrientationLandscape = isLandscape,
                screenWidthDp = configuration.screenWidthDp,
                screenHeightDp = configuration.screenHeightDp,
                availableWidthDp = maxWidth.value,
                availableHeightDp = maxHeight.value
            )

            if (isPhoneLandscape) {
                // NUEVO DISEÑO COMPACTO DE DOS ZONAS PARA MÓVILES EN HORIZONTAL
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = PictoDimens.SpaceSmall, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(PictoDimens.SpaceSmall)
                ) {
                    // 1. PANEL LATERAL (~33% de ancho): Frase construida y controles
                    LandscapePhraseSidePanel(
                        currentPhrase = currentPhrase,
                        languageCode = languageCode,
                        highContrast = highContrast,
                        hapticsEnabled = hapticsEnabled,
                        onSpeakClick = {
                            val speechText = processPhraseForSpeech(currentPhrase, languageCode)
                            val pictoIds = currentPhrase.map { it.id }
                            onSpeakPhrase(speechText, pictoIds)
                        },
                        onClearClick = { onPhraseChange(emptyList()) },
                        onRemoveLast = {
                            if (currentPhrase.isNotEmpty()) {
                                onPhraseChange(currentPhrase.dropLast(1))
                            }
                        },
                        onRemoveAtIndex = { index ->
                            if (index in currentPhrase.indices) {
                                onPhraseChange(currentPhrase.toMutableList().apply { removeAt(index) })
                            }
                        },
                        modifier = Modifier
                            .weight(0.33f)
                            .fillMaxHeight()
                    )

                    // 2. ZONA PRINCIPAL (~67% de ancho): Selección de pictogramas
                    Column(
                        modifier = Modifier
                            .weight(0.67f)
                            .fillMaxHeight()
                    ) {
                        // Selector de categorías en píldoras horizontales compactas
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Píldora: «Mi tablero»
                            item {
                                val isBoardSelected = selectedCategory == null && searchText.isEmpty()
                                val boardLabel = activeBoard?.name ?: if (languageCode == "EN") "My Board" else "Mi tablero"

                                Surface(
                                    onClick = {
                                        PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                                        selectedCategory = null
                                        searchText = ""
                                    },
                                    shape = PictoDimens.CornerPill,
                                    color = if (isBoardSelected) {
                                        if (highContrast) PictoColors.HighlightHighContrast else PictoColors.Primary
                                    } else {
                                        if (highContrast) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surface
                                    },
                                    border = if (isBoardSelected) null else BorderStroke(
                                        1.dp,
                                        if (highContrast) Color.Gray else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .semantics {
                                            role = Role.Tab
                                            contentDescription = "Pestaña Tablero $boardLabel" + if (isBoardSelected) ", seleccionada" else ""
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GridView,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (isBoardSelected) {
                                                if (highContrast) Color.Black else Color.White
                                            } else {
                                                if (highContrast) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = boardLabel,
                                            fontSize = 12.sp,
                                            fontWeight = if (isBoardSelected) FontWeight.Black else FontWeight.Bold,
                                            color = if (isBoardSelected) {
                                                if (highContrast) Color.Black else Color.White
                                            } else {
                                                if (highContrast) Color.White else MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }
                            }

                            // Píldoras para cada categoría de Fitzgerald
                            items(Category.entries) { category ->
                                val isCategorySelected = selectedCategory == category && searchText.isEmpty()
                                val categoryLabel = category.getLabel(languageCode)

                                Surface(
                                    onClick = {
                                        PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                                        selectedCategory = category
                                        searchText = ""
                                    },
                                    shape = PictoDimens.CornerPill,
                                    color = if (isCategorySelected) {
                                        if (highContrast) Color(0xFF333333) else category.color
                                    } else {
                                        if (highContrast) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surface
                                    },
                                    border = BorderStroke(
                                        if (isCategorySelected) PictoDimens.BorderWidthStandard else 1.dp,
                                        if (isCategorySelected) {
                                            if (highContrast) Color.White else category.borderColor
                                        } else {
                                            if (highContrast) Color.Gray else MaterialTheme.colorScheme.outlineVariant
                                        }
                                    ),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .semantics {
                                            role = Role.Tab
                                            contentDescription = "Categoría $categoryLabel" + if (isCategorySelected) ", seleccionada" else ""
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = category.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (highContrast) Color.White else if (isCategorySelected) category.textColor else Color.Gray
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = categoryLabel,
                                            fontSize = 12.sp,
                                            fontWeight = if (isCategorySelected) FontWeight.Black else FontWeight.Medium,
                                            color = if (highContrast) Color.White else if (isCategorySelected) category.textColor else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // 2. Buscador rápido compacto
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { newValue ->
                                searchText = newValue
                                if (newValue.endsWith(" ")) {
                                    val word = newValue.trim().split(" ").lastOrNull()
                                    if (word != null) {
                                        val match = allPictograms.find {
                                            it.matchesWord(word, languageCode)
                                        }
                                        if (match != null) {
                                            onPhraseChange(currentPhrase + match)
                                            searchText = ""
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            placeholder = {
                                Text(
                                    text = if (languageCode == "EN") "Search pictogram..." else "Buscar pictograma...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 12.sp,
                                    color = if (highContrast) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = if (highContrast) PictoColors.HighlightHighContrast else PictoColors.Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchText.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { searchText = "" },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = if (languageCode == "EN") "Clear search" else "Borrar búsqueda",
                                                tint = if (highContrast) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                val words = searchText.trim().split(" ")
                                                var added = false
                                                words.forEach { word ->
                                                    val match = allPictograms.find {
                                                        it.matchesWord(word, languageCode)
                                                    }
                                                    if (match != null) {
                                                        onPhraseChange(currentPhrase + match)
                                                        added = true
                                                    }
                                                }
                                                if (!added) {
                                                    onSpeakPhrase(searchText, emptyList())
                                                }
                                                searchText = ""
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Send,
                                                contentDescription = "Añadir o pronunciar",
                                                tint = if (highContrast) PictoColors.HighlightHighContrast else PictoColors.Primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            shape = PictoDimens.CornerPill,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (highContrast) Color.White else PictoColors.Primary,
                                unfocusedBorderColor = if (highContrast) Color.Gray else MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = if (highContrast) Color.Black else MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = if (highContrast) Color.Black else MaterialTheme.colorScheme.surface
                            )
                        )

                        Spacer(Modifier.height(4.dp))

                        // 3. Cuadrícula de pictogramas con todo el espacio restante disponible y scroll vertical fluido
                        val phoneLandscapeGridCells = if (selectedCategory == null && searchText.isBlank()) {
                            GridCells.Fixed((activeColumns + 1).coerceIn(3, 5))
                        } else {
                            GridCells.Adaptive(minSize = if (largeGrid) 100.dp else 84.dp)
                        }
                        val phoneLandscapeCardHeight = if (largeGrid) 104.dp else 90.dp
                        val phoneLandscapeIconSize = if (largeGrid) 48.dp else 40.dp

                        LazyVerticalGrid(
                            columns = phoneLandscapeGridCells,
                            contentPadding = PaddingValues(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            items(displayedPictograms, key = { it.id }) { picto ->
                                val isFav = picto.id in favoriteIds
                                PictogramCard(
                                    picto = picto,
                                    languageCode = languageCode,
                                    isFavorite = isFav,
                                    highContrast = highContrast,
                                    hapticsEnabled = hapticsEnabled,
                                    iconSize = phoneLandscapeIconSize,
                                    onToggleFavorite = { onToggleFavorite(picto.id) },
                                    modifier = Modifier.height(phoneLandscapeCardHeight)
                                ) {
                                    onPhraseChange(currentPhrase + picto)
                                }
                            }
                        }
                    }
                }
            } else {
                // DISEÑO ORIGINAL (Móvil vertical, Tablet vertical, Tablet horizontal)
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
            // Barra lateral de categorías para modo horizontal (Tablets / Apaisado)
            if (isLandscape) {
                Surface(
                    modifier = Modifier
                        .width(220.dp)
                        .fillMaxHeight(),
                    color = if (highContrast) Color(0xFF121212) else MaterialTheme.colorScheme.surface
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(PictoDimens.SpaceSmall),
                        verticalArrangement = Arrangement.spacedBy(PictoDimens.SpaceSmall)
                    ) {
                        // Opción Mi Tablero
                        item {
                            val isSelected = selectedCategory == null && searchText.isEmpty()
                            val boardLabel = activeBoard?.name ?: "Mi Tablero"

                            Surface(
                                onClick = {
                                    PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                                    selectedCategory = null
                                    searchText = ""
                                },
                                shape = PictoDimens.CornerMedium,
                                color = if (isSelected) {
                                    if (highContrast) PictoColors.HighlightHighContrast else PictoColors.Primary
                                } else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = PictoDimens.CategoryChipHeight)
                                    .semantics {
                                        role = Role.Tab
                                        contentDescription = "Pestaña Tablero $boardLabel" + if (isSelected) ", seleccionada" else ""
                                    },
                                border = if (isSelected) null else BorderStroke(1.dp, if (highContrast) Color.Gray else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = PictoDimens.SpaceMedium, vertical = PictoDimens.SpaceSmall),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Dashboard,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = if (isSelected) {
                                            if (highContrast) Color.Black else Color.White
                                        } else {
                                            if (highContrast) Color.White else MaterialTheme.colorScheme.primary
                                        }
                                    )
                                    Spacer(Modifier.width(PictoDimens.SpaceSmall))
                                    Text(
                                        text = boardLabel,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) {
                                            if (highContrast) Color.Black else Color.White
                                        } else {
                                            if (highContrast) Color.White else MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }

                        // Categorías del catálogo
                        items(Category.entries) { category ->
                            val isSelected = selectedCategory == category && searchText.isEmpty()
                            val categoryLabel = category.getLabel(languageCode)

                            Surface(
                                onClick = {
                                    PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                                    selectedCategory = category
                                    searchText = ""
                                },
                                shape = PictoDimens.CornerMedium,
                                color = if (isSelected) {
                                    if (highContrast) Color(0xFF333333) else category.color
                                } else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = PictoDimens.CategoryChipHeight)
                                    .semantics {
                                        role = Role.Tab
                                        contentDescription = "Categoría $categoryLabel" + if (isSelected) ", seleccionada" else ""
                                    },
                                border = BorderStroke(
                                    if (isSelected) PictoDimens.BorderWidthStandard else 1.dp,
                                    if (isSelected) {
                                        if (highContrast) Color.White else category.borderColor
                                    } else {
                                        if (highContrast) Color.Gray else MaterialTheme.colorScheme.outlineVariant
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = PictoDimens.SpaceMedium, vertical = PictoDimens.SpaceSmall),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = if (highContrast) Color.White else if (isSelected) category.textColor else Color.Gray
                                    )
                                    Spacer(Modifier.width(PictoDimens.SpaceSmall))
                                    Text(
                                        text = categoryLabel,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (highContrast) Color.White else if (isSelected) category.textColor else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Columna principal de la interfaz
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Spacer(Modifier.height(PictoDimens.SpaceBetweenSections))

                // 1. Barra de construcción de frases espaciosa
                PhraseBar(
                    selectedPictos = currentPhrase,
                    languageCode = languageCode,
                    highContrast = highContrast,
                    hapticsEnabled = hapticsEnabled,
                    onSpeakClick = {
                        val speechText = processPhraseForSpeech(currentPhrase, languageCode)
                        val pictoIds = currentPhrase.map { it.id }
                        onSpeakPhrase(speechText, pictoIds)
                    },
                    onClearClick = { onPhraseChange(emptyList()) },
                    onRemoveLast = {
                        if (currentPhrase.isNotEmpty()) {
                            onPhraseChange(currentPhrase.dropLast(1))
                        }
                    },
                    onRemoveAtIndex = { index ->
                        if (index in currentPhrase.indices) {
                            onPhraseChange(currentPhrase.toMutableList().apply { removeAt(index) })
                        }
                    },
                    modifier = Modifier.padding(horizontal = PictoDimens.GridHorizontalPadding)
                )

                Spacer(Modifier.height(PictoDimens.SpaceBetweenSections))

                // 2. Buscador rápido con espacio propio y esquinas redondeadas
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { newValue ->
                        searchText = newValue
                        if (newValue.endsWith(" ")) {
                            val word = newValue.trim().split(" ").lastOrNull()
                            if (word != null) {
                                val match = allPictograms.find {
                                    it.matchesWord(word, languageCode)
                                }
                                if (match != null) {
                                    onPhraseChange(currentPhrase + match)
                                    searchText = ""
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PictoDimens.SearchFieldHeight)
                        .padding(horizontal = PictoDimens.GridHorizontalPadding),
                    placeholder = {
                        Text(
                            text = if (languageCode == "EN") "Search pictogram..." else "Buscar pictograma...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (highContrast) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = if (highContrast) PictoColors.HighlightHighContrast else PictoColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { searchText = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = if (languageCode == "EN") "Clear search" else "Borrar búsqueda",
                                        tint = if (highContrast) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = {
                                    val words = searchText.trim().split(" ")
                                    var added = false
                                    words.forEach { word ->
                                        val match = allPictograms.find {
                                            it.matchesWord(word, languageCode)
                                        }
                                        if (match != null) {
                                            onPhraseChange(currentPhrase + match)
                                            added = true
                                        }
                                    }
                                    if (!added) {
                                        onSpeakPhrase(searchText, emptyList())
                                    }
                                    searchText = ""
                                }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Añadir o pronunciar",
                                        tint = if (highContrast) PictoColors.HighlightHighContrast else PictoColors.Primary
                                    )
                                }
                            }
                        }
                    },
                    shape = PictoDimens.CornerPill,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (highContrast) Color.White else PictoColors.Primary,
                        unfocusedBorderColor = if (highContrast) Color.Gray else MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = if (highContrast) Color.Black else MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = if (highContrast) Color.Black else MaterialTheme.colorScheme.surface
                    )
                )

                // 3. Selector de categorías horizontal para modo vertical (Píldoras accesibles)
                if (!isLandscape) {
                    Spacer(Modifier.height(PictoDimens.SpaceBetweenSections))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = PictoDimens.GridHorizontalPadding),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Píldora: «Mi tablero»
                        item {
                            val isBoardSelected = selectedCategory == null && searchText.isEmpty()
                            val boardLabel = activeBoard?.name ?: "Mi tablero"

                            Surface(
                                onClick = {
                                    PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                                    selectedCategory = null
                                    searchText = ""
                                },
                                shape = PictoDimens.CornerPill,
                                color = if (isBoardSelected) {
                                    if (highContrast) PictoColors.HighlightHighContrast else PictoColors.Primary
                                } else {
                                    if (highContrast) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surface
                                },
                                border = if (isBoardSelected) null else BorderStroke(
                                    1.dp,
                                    if (highContrast) Color.Gray else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .height(PictoDimens.CategoryChipHeight)
                                    .semantics {
                                        role = Role.Tab
                                        contentDescription = "Categoría $boardLabel" + if (isBoardSelected) ", seleccionada" else ""
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GridView,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isBoardSelected) {
                                            if (highContrast) Color.Black else Color.White
                                        } else {
                                            if (highContrast) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = boardLabel,
                                        fontSize = 13.sp,
                                        fontWeight = if (isBoardSelected) FontWeight.Black else FontWeight.Bold,
                                        color = if (isBoardSelected) {
                                            if (highContrast) Color.Black else Color.White
                                        } else {
                                            if (highContrast) Color.White else MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }

                        // Píldoras para cada categoría de Fitzgerald
                        items(Category.entries) { category ->
                            val isCategorySelected = selectedCategory == category && searchText.isEmpty()
                            val categoryLabel = category.getLabel(languageCode)

                            Surface(
                                onClick = {
                                    PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                                    selectedCategory = category
                                    searchText = ""
                                },
                                shape = PictoDimens.CornerPill,
                                color = if (isCategorySelected) {
                                    if (highContrast) Color(0xFF333333) else category.color
                                } else {
                                    if (highContrast) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(
                                    if (isCategorySelected) PictoDimens.BorderWidthStandard else 1.dp,
                                    if (isCategorySelected) {
                                        if (highContrast) Color.White else category.borderColor
                                    } else {
                                        if (highContrast) Color.Gray else MaterialTheme.colorScheme.outlineVariant
                                    }
                                ),
                                modifier = Modifier
                                    .height(PictoDimens.CategoryChipHeight)
                                    .semantics {
                                        role = Role.Tab
                                        contentDescription = "Categoría $categoryLabel" + if (isCategorySelected) ", seleccionada" else ""
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (highContrast) Color.White else if (isCategorySelected) category.textColor else Color.Gray
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = categoryLabel,
                                        fontSize = 13.sp,
                                        fontWeight = if (isCategorySelected) FontWeight.Black else FontWeight.Medium,
                                        color = if (highContrast) Color.White else if (isCategorySelected) category.textColor else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(PictoDimens.SpaceBetweenSections))

                // 4. Cuadrícula táctil de pictogramas accesible y espaciosa
                LazyVerticalGrid(
                    columns = gridCells,
                    contentPadding = PaddingValues(
                        horizontal = PictoDimens.GridHorizontalPadding,
                        vertical = PictoDimens.SpaceSmall
                    ),
                    horizontalArrangement = Arrangement.spacedBy(PictoDimens.CardGridSpacing),
                    verticalArrangement = Arrangement.spacedBy(PictoDimens.CardGridSpacing),
                    modifier = Modifier.weight(1f)
                ) {
                    items(displayedPictograms, key = { it.id }) { picto ->
                        val isFav = picto.id in favoriteIds
                        PictogramCard(
                            picto = picto,
                            languageCode = languageCode,
                            isFavorite = isFav,
                            highContrast = highContrast,
                            hapticsEnabled = hapticsEnabled,
                            onToggleFavorite = { onToggleFavorite(picto.id) },
                            modifier = Modifier.height(gridCardHeight)
                        ) {
                            onPhraseChange(currentPhrase + picto)
                        }
                    }
                }
            }
        }
    }
}
}

    // Barrera de protección para adultos (profesores y cuidadores)
    pendingAdultNavigation?.let { destinationCallback ->
        AdultGateDialog(
            onDismiss = { pendingAdultNavigation = null },
            onSuccess = {
                pendingAdultNavigation = null
                destinationCallback()
            }
        )
    }
}
