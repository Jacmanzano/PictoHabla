package com.cmanzano.pictohabla.feature.boardmanager

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
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

/**
 * Pantalla de gestión de tableros de comunicación (CAA).
 * Permite cambiar el tablero activo, crear tableros nuevos, duplicarlos, editarlos o eliminarlos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardsManagerScreen(
    boards: List<Board>,
    activeBoardId: Long,
    onSelectActiveBoard: (Long) -> Unit,
    onEditBoard: (Long) -> Unit,
    onCreateBoard: (name: String, description: String, columns: Int) -> Unit,
    onDuplicateBoard: (Long) -> Unit,
    onDeleteBoard: (Long) -> Unit,
    onBack: () -> Unit,
    highContrast: Boolean = false,
    hapticsEnabled: Boolean = true,
    languageCode: String = "ES"
) {
    val hapticFeedback = LocalHapticFeedback.current
    val isEn = languageCode.equals("EN", ignoreCase = true)

    var showCreateDialog by remember { mutableStateOf(false) }
    var boardToDelete by remember { mutableStateOf<Board?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isEn) "Communication Boards" else "Tableros de Comunicación",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
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
                            contentDescription = if (isEn) "Back to communicator" else "Volver al comunicador"
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
                    IconButton(
                        onClick = {
                            PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                            showCreateDialog = true
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = if (isEn) "Create new board" else "Crear nuevo tablero"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = if (highContrast) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (highContrast) Color.Black else MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                    showCreateDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(if (isEn) "New Board" else "Nuevo Tablero", fontWeight = FontWeight.Bold) },
                containerColor = if (highContrast) Color.White else MaterialTheme.colorScheme.primary,
                contentColor = if (highContrast) Color.Black else MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.semantics {
                    role = Role.Button
                    contentDescription = if (isEn) "Create a new custom board" else "Crear un nuevo tablero personalizado"
                }
            )
        }
    ) { padding ->
        if (boards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isEn) "No boards available." else "No hay tableros disponibles.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(PictoDimens.SpaceMedium),
                verticalArrangement = Arrangement.spacedBy(PictoDimens.SpaceMedium)
            ) {
                items(boards, key = { it.id }) { board ->
                    val isActive = board.id == activeBoardId

                    BoardCardItem(
                        board = board,
                        isActive = isActive,
                        highContrast = highContrast,
                        isEn = isEn,
                        onActivate = {
                            PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                            onSelectActiveBoard(board.id)
                        },
                        onEdit = {
                            PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                            onEditBoard(board.id)
                        },
                        onDuplicate = {
                            PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                            onDuplicateBoard(board.id)
                        },
                        onDelete = {
                            PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                            boardToDelete = board
                        },
                        canDelete = !board.isDefault && boards.size > 1
                    )
                }
            }
        }
    }

    // Diálogo para crear un nuevo tablero
    if (showCreateDialog) {
        CreateBoardDialog(
            isEn = isEn,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, description, columns ->
                showCreateDialog = false
                onCreateBoard(name, description, columns)
            }
        )
    }

    // Diálogo de confirmación para eliminar tablero
    boardToDelete?.let { board ->
        AlertDialog(
            onDismissRequest = { boardToDelete = null },
            title = {
                Text(text = if (isEn) "Delete board" else "Eliminar tablero", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = if (isEn) {
                        "Are you sure you want to delete the board \"${board.name}\"? This action cannot be undone."
                    } else {
                        "¿Estás seguro de que deseas eliminar el tablero \"${board.name}\"? Esta acción no se puede deshacer."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = board.id
                        boardToDelete = null
                        onDeleteBoard(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isEn) "Delete" else "Eliminar", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { boardToDelete = null }) {
                    Text(if (isEn) "Cancel" else "Cancelar")
                }
            }
        )
    }
}

/**
 * Tarjeta accesible para mostrar la información y acciones de un tablero.
 */
@Composable
private fun BoardCardItem(
    board: Board,
    isActive: Boolean,
    highContrast: Boolean,
    isEn: Boolean,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    val cardBorder = if (isActive) {
        BorderStroke(2.dp, if (highContrast) Color.White else MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, if (highContrast) Color.Gray else MaterialTheme.colorScheme.outlineVariant)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append(if (isEn) "Board: ${board.name}. " else "Tablero: ${board.name}. ")
                    if (isActive) append(if (isEn) "Currently selected. " else "Actualmente seleccionado. ")
                    if (board.isDefault) append(if (isEn) "Default. " else "Predeterminado. ")
                    append(if (isEn) "${board.columnsCount} columns. " else "${board.columnsCount} columnas. ")
                    if (board.description.isNotBlank()) append("${board.description}. ")
                }
            },
        shape = PictoDimens.CornerMedium,
        colors = CardDefaults.cardColors(
            containerColor = if (highContrast) {
                if (isActive) Color(0xFF1E1E1E) else Color(0xFF121212)
            } else {
                if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surface
            }
        ),
        border = cardBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PictoDimens.SpaceMedium)
        ) {
            // Fila de encabezado con nombre y estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ViewModule,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = if (highContrast) Color.White else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(PictoDimens.SpaceSmall))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = board.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (highContrast) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    if (board.description.isNotBlank()) {
                        Text(
                            text = board.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (highContrast) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isActive) {
                    AssistChip(
                        onClick = {},
                        label = { Text(if (isEn) "Active" else "Activo", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            labelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(Modifier.height(PictoDimens.SpaceSmall))

            // Etiquetas con características del tablero
            Row(
                horizontalArrangement = Arrangement.spacedBy(PictoDimens.SpaceSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(if (isEn) "${board.columnsCount} columns" else "${board.columnsCount} columnas", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                )

                if (board.isDefault) {
                    AssistChip(
                        onClick = {},
                        label = { Text(if (isEn) "Default" else "Predeterminado", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    )
                }
            }

            Spacer(Modifier.height(PictoDimens.SpaceMedium))

            // Barra de acciones del tablero
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Botón principal de selección / activación
                if (!isActive) {
                    Button(
                        onClick = onActivate,
                        modifier = Modifier.heightIn(min = PictoDimens.TouchTargetMinimum),
                        shape = PictoDimens.CornerSmall
                    ) {
                        Text(if (isEn) "Use this board" else "Usar este tablero", fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.heightIn(min = PictoDimens.TouchTargetMinimum),
                        shape = PictoDimens.CornerSmall
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(PictoDimens.SpaceExtraSmall))
                        Text(if (isEn) "Edit pictograms" else "Editar pictogramas", fontWeight = FontWeight.Bold)
                    }
                }

                // Acciones secundarias
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isActive) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .size(PictoDimens.TouchTargetMinimum)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = if (isEn) "Edit pictograms of ${board.name}" else "Editar pictogramas de ${board.name}"
                                }
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = if (highContrast) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier
                            .size(PictoDimens.TouchTargetMinimum)
                            .semantics {
                                role = Role.Button
                                contentDescription = if (isEn) "Duplicate board ${board.name}" else "Duplicar tablero ${board.name}"
                            }
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = if (highContrast) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (canDelete) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(PictoDimens.TouchTargetMinimum)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = if (isEn) "Delete board ${board.name}" else "Eliminar tablero ${board.name}"
                                }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Diálogo accesible para crear un nuevo tablero con nombre, descripción y columnas.
 */
@Composable
private fun CreateBoardDialog(
    isEn: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, columns: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var columns by remember { mutableIntStateOf(3) }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (isEn) "Create new board" else "Crear nuevo tablero", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    isError = nameError,
                    label = { Text(if (isEn) "Board name *" else "Nombre del tablero *") },
                    placeholder = { Text(if (isEn) "E.g.: At home, School, Therapy..." else "Ej: En casa, Patio, Terapia...") },
                    singleLine = true,
                    supportingText = if (nameError) {
                        { Text(if (isEn) "The name is required" else "El nombre es obligatorio", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = PictoDimens.CornerMedium
                )

                Spacer(Modifier.height(PictoDimens.SpaceSmall))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (isEn) "Description (optional)" else "Descripción (opcional)") },
                    placeholder = { Text(if (isEn) "For everyday situations..." else "Para situaciones cotidianas...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = PictoDimens.CornerMedium
                )

                Spacer(Modifier.height(PictoDimens.SpaceMedium))

                Text(
                    text = if (isEn) "Number of columns: $columns" else "Número de columnas: $columns",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    (1..6).forEach { colCount ->
                        FilterChip(
                            selected = columns == colCount,
                            onClick = { columns = colCount },
                            label = { Text("$colCount", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.semantics {
                                contentDescription = if (isEn) "$colCount columns" else "$colCount columnas"
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    } else {
                        onConfirm(name, description, columns)
                    }
                },
                shape = PictoDimens.CornerSmall
            ) {
                Text(if (isEn) "Create" else "Crear", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isEn) "Cancel" else "Cancelar")
            }
        }
    )
}
