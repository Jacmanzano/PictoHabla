package com.cmanzano.pictohabla.feature.boardmanager

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmanzano.pictohabla.core.designsystem.PictoColors
import com.cmanzano.pictohabla.core.designsystem.PictoDimens
import com.cmanzano.pictohabla.core.designsystem.PictoHaptics
import com.cmanzano.pictohabla.core.model.Board
import com.cmanzano.pictohabla.core.model.Category
import com.cmanzano.pictohabla.core.model.Pictogram
import com.cmanzano.pictohabla.core.util.ImageStorageHelper
import com.cmanzano.pictohabla.core.util.normalizeForSearch
import com.cmanzano.pictohabla.feature.board.components.PictogramImage

/**
 * Pantalla para editar los datos de un tablero y organizar sus pictogramas.
 * Permite cambiar el nombre, número de columnas (1..6) y reordenar pictogramas
 * con botones accesibles de dirección (izquierda/derecha).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardEditorScreen(
    board: Board?,
    allAvailablePictograms: List<Pictogram>,
    languageCode: String,
    highContrast: Boolean = false,
    hapticsEnabled: Boolean = true,
    onUpdateBoardMetadata: (name: String, description: String, columns: Int) -> Unit,
    onMovePictogram: (pictoId: Int, moveLeft: Boolean) -> Unit,
    onRemovePictogram: (pictoId: Int) -> Unit,
    onAddPictograms: (List<Int>) -> Unit,
    onBack: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current

    if (board == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Tablero no encontrado")
        }
        return
    }

    var name by remember(board.name) { mutableStateOf(board.name) }
    var description by remember(board.description) { mutableStateOf(board.description) }
    var columnsCount by remember(board.columnsCount) { mutableIntStateOf(board.columnsCount) }
    var isMetadataChanged by remember { mutableStateOf(false) }

    var showAddPictosDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Editar: ${board.name}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                            onBack()
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Volver a la gestión de tableros"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = if (highContrast) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (isMetadataChanged) {
                        IconButton(
                            onClick = {
                                PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                                onUpdateBoardMetadata(name, description, columnsCount)
                                isMetadataChanged = false
                            },
                            modifier = Modifier.semantics {
                                role = Role.Button
                                contentDescription = "Guardar cambios del tablero"
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                tint = if (highContrast) Color.White else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (highContrast) Color.Black else MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(PictoDimens.SpaceMedium),
            verticalArrangement = Arrangement.spacedBy(PictoDimens.SpaceMedium)
        ) {
            // Sección de Configuración General del Tablero
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = PictoDimens.CornerMedium,
                    colors = CardDefaults.cardColors(
                        containerColor = if (highContrast) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PictoDimens.SpaceMedium)
                    ) {
                        Text(
                            text = "Ajustes del Tablero",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(PictoDimens.SpaceSmall))

                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                isMetadataChanged = true
                            },
                            label = { Text("Nombre") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(PictoDimens.SpaceSmall))

                        OutlinedTextField(
                            value = description,
                            onValueChange = {
                                description = it
                                isMetadataChanged = true
                            },
                            label = { Text("Descripción") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(PictoDimens.SpaceMedium))

                        Text(
                            text = "Columnas en cuadrícula: $columnsCount",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(PictoDimens.SpaceExtraSmall))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            (1..6).forEach { col ->
                                FilterChip(
                                    selected = columnsCount == col,
                                    onClick = {
                                        columnsCount = col
                                        isMetadataChanged = true
                                    },
                                    label = { Text("$col", fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        if (isMetadataChanged) {
                            Spacer(Modifier.height(PictoDimens.SpaceMedium))
                            Button(
                                onClick = {
                                    PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                                    onUpdateBoardMetadata(name, description, columnsCount)
                                    isMetadataChanged = false
                                },
                                modifier = Modifier.align(Alignment.End),
                                shape = PictoDimens.CornerSmall
                            ) {
                                Text("Guardar Cambios", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Encabezado de la lista de pictogramas
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pictogramas (${board.pictograms.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = {
                            PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                            showAddPictosDialog = true
                        },
                        shape = PictoDimens.CornerMedium,
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Añadir más pictogramas a este tablero"
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(PictoDimens.SpaceExtraSmall))
                        Text("Añadir", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (board.pictograms.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = PictoDimens.SpaceLarge),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(PictoDimens.SpaceLarge),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Este tablero aún no tiene pictogramas.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(Modifier.height(PictoDimens.SpaceMedium))
                            OutlinedButton(onClick = { showAddPictosDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(PictoDimens.SpaceExtraSmall))
                                Text("Añadir pictogramas ahora")
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(board.pictograms, key = { _, picto -> picto.id }) { index, picto ->
                    BoardPictoRowItem(
                        index = index,
                        totalCount = board.pictograms.size,
                        picto = picto,
                        languageCode = languageCode,
                        highContrast = highContrast,
                        onMoveLeft = {
                            PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                            onMovePictogram(picto.id, true)
                        },
                        onMoveRight = {
                            PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                            onMovePictogram(picto.id, false)
                        },
                        onRemove = {
                            PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                            onRemovePictogram(picto.id)
                        }
                    )
                }
            }
        }
    }

    // Diálogo para buscar y añadir pictogramas del catálogo general
    if (showAddPictosDialog) {
        AddPictogramsDialog(
            allPictograms = allAvailablePictograms,
            alreadyInBoardIds = board.pictograms.map { it.id }.toSet(),
            languageCode = languageCode,
            highContrast = highContrast,
            onDismiss = { showAddPictosDialog = false },
            onConfirmAdd = { selectedIds ->
                showAddPictosDialog = false
                if (selectedIds.isNotEmpty()) {
                    onAddPictograms(selectedIds)
                }
            }
        )
    }
}

/**
 * Fila accesible para cada pictograma dentro del editor de tablero.
 * Incluye controles direccionales accesibles para reordenar y botón de retirar.
 */
@Composable
private fun BoardPictoRowItem(
    index: Int,
    totalCount: Int,
    picto: Pictogram,
    languageCode: String,
    highContrast: Boolean,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onRemove: () -> Unit
) {
    val canMoveUp = index > 0
    val canMoveDown = index < totalCount - 1
    val pictoName = picto.getName(languageCode)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Pictograma posición ${index + 1}: $pictoName, categoría ${picto.category.getLabel(languageCode)}"
            },
        shape = PictoDimens.CornerMedium,
        colors = CardDefaults.cardColors(
            containerColor = if (highContrast) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (highContrast) Color.Gray else picto.category.borderColor.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PictoDimens.SpaceSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Insignia de orden secuencial
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (highContrast) Color.White else picto.category.color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = if (highContrast) Color.Black else picto.category.textColor
                )
            }

            Spacer(Modifier.width(PictoDimens.SpaceSmall))

            // Miniatura del pictograma
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(PictoDimens.CornerSmall)
                    .background(Color.White)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                PictogramImage(
                    picto = picto,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.width(PictoDimens.SpaceSmall))

            // Información del pictograma
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pictoName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (highContrast) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = picto.category.getLabel(languageCode),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (highContrast) Color.LightGray else picto.category.textColor
                )
            }

            // Botones accesibles de reorganización
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Mover antes / izquierda
                IconButton(
                    onClick = onMoveLeft,
                    enabled = canMoveUp,
                    modifier = Modifier
                        .size(PictoDimens.TouchTargetMinimum)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Mover $pictoName hacia la izquierda o antes"
                        }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = if (canMoveUp) {
                            if (highContrast) Color.White else MaterialTheme.colorScheme.primary
                        } else Color.Gray.copy(alpha = 0.3f)
                    )
                }

                // Mover después / derecha
                IconButton(
                    onClick = onMoveRight,
                    enabled = canMoveDown,
                    modifier = Modifier
                        .size(PictoDimens.TouchTargetMinimum)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Mover $pictoName hacia la derecha o después"
                        }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = if (canMoveDown) {
                            if (highContrast) Color.White else MaterialTheme.colorScheme.primary
                        } else Color.Gray.copy(alpha = 0.3f)
                    )
                }

                // Quitar del tablero
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(PictoDimens.TouchTargetMinimum)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Quitar $pictoName de este tablero"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Diálogo interactivo para añadir pictogramas del catálogo completo al tablero.
 */
@Composable
private fun AddPictogramsDialog(
    allPictograms: List<Pictogram>,
    alreadyInBoardIds: Set<Int>,
    languageCode: String,
    highContrast: Boolean,
    onDismiss: () -> Unit,
    onConfirmAdd: (List<Int>) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedToAdd by remember { mutableStateOf(setOf<Int>()) }

    val filteredPictos = remember(allPictograms, searchText, selectedCategory, languageCode) {
        allPictograms.filter { picto ->
            val matchesCategory = selectedCategory == null || picto.category == selectedCategory
            val matchesSearch = searchText.isBlank() || picto.getName(languageCode)
                .normalizeForSearch()
                .contains(searchText.normalizeForSearch())
            matchesCategory && matchesSearch
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Añadir Pictogramas al Tablero",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                // Buscador
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Buscar pictograma...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = PictoDimens.CornerMedium
                )

                Spacer(Modifier.height(PictoDimens.SpaceSmall))

                // Filtro rápido de categorías
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PictoDimens.SpaceExtraSmall)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("Todas") }
                        )
                    }
                    items(Category.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = if (selectedCategory == cat) null else cat
                            },
                            label = { Text(cat.getLabel(languageCode)) }
                        )
                    }
                }

                Spacer(Modifier.height(PictoDimens.SpaceSmall))

                // Cuadrícula de selección
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 78.dp),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(PictoDimens.SpaceSmall),
                    verticalArrangement = Arrangement.spacedBy(PictoDimens.SpaceSmall)
                ) {
                    items(filteredPictos, key = { it.id }) { picto ->
                        val alreadyIn = picto.id in alreadyInBoardIds
                        val isSelected = picto.id in selectedToAdd

                        val cardBorder = when {
                            alreadyIn -> BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                            isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            else -> BorderStroke(1.dp, picto.category.borderColor.copy(alpha = 0.4f))
                        }

                        Card(
                            onClick = {
                                if (!alreadyIn) {
                                    selectedToAdd = if (isSelected) {
                                        selectedToAdd - picto.id
                                    } else {
                                        selectedToAdd + picto.id
                                    }
                                }
                            },
                            enabled = !alreadyIn,
                            modifier = Modifier
                                .size(78.dp)
                                .semantics {
                                    contentDescription = "${picto.getName(languageCode)}" +
                                            if (alreadyIn) ", ya está en el tablero"
                                            else if (isSelected) ", seleccionado para añadir"
                                            else ""
                                },
                            shape = PictoDimens.CornerSmall,
                            border = cardBorder,
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    alreadyIn -> Color.Gray.copy(alpha = 0.15f)
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    else -> Color.White
                                }
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    PictogramImage(
                                        picto = picto,
                                        modifier = Modifier.size(36.dp)
                                    )

                                    Text(
                                        text = picto.getName(languageCode),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        color = if (alreadyIn) Color.Gray else Color.Black
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmAdd(selectedToAdd.toList()) },
                enabled = selectedToAdd.isNotEmpty(),
                shape = PictoDimens.CornerSmall
            ) {
                Text("Añadir (${selectedToAdd.size})", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
