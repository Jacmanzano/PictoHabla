package com.cmanzano.pictohabla.feature.custompicto

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.cmanzano.pictohabla.core.model.Category
import com.cmanzano.pictohabla.core.model.Pictogram
import com.cmanzano.pictohabla.core.util.ImageStorageHelper
import kotlinx.coroutines.launch

/**
 * Pantalla para crear o editar un pictograma personalizado.
 * Permite seleccionar una foto del dispositivo, definir su nombre visible,
 * texto de pronunciación TTS y categoría gramatical según la Clave de Fitzgerald.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPictoScreen(
    editingPicto: Pictogram? = null,
    languageCode: String = "ES",
    highContrast: Boolean = false,
    hapticsEnabled: Boolean = true,
    onSavePicto: (name: String, speechText: String?, category: Category, localImagePath: String?) -> Unit,
    onDeletePicto: ((Int) -> Unit)? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val isEnglish = languageCode.equals("EN", ignoreCase = true)
    var name by remember { mutableStateOf(editingPicto?.getName(languageCode) ?: "") }
    var speechText by remember { mutableStateOf(editingPicto?.speechText ?: "") }
    var selectedCategory by remember { mutableStateOf(editingPicto?.category ?: Category.THINGS) }
    var localImagePath by remember { mutableStateOf(editingPicto?.localImagePath) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    // Selector de imágenes del sistema Android
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            coroutineScope.launch {
                val savedPath = ImageStorageHelper.saveImageToInternalStorage(context, uri)
                localImagePath = savedPath
            }
        }
    }

    val previewBitmap = remember(localImagePath) {
        ImageStorageHelper.loadBitmap(localImagePath)?.asImageBitmap()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isEnglish) {
                            if (editingPicto != null) "Edit Pictogram" else "New Pictogram"
                        } else {
                            if (editingPicto != null) "Editar Pictograma" else "Nuevo Pictograma"
                        },
                        fontWeight = FontWeight.Bold
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
                            contentDescription = if (isEnglish) "Back" else "Volver"
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (editingPicto != null && onDeletePicto != null) {
                        IconButton(
                            onClick = {
                                PictoHaptics.performEmphasis(hapticFeedback, hapticsEnabled)
                                showDeleteConfirm = true
                            },
                            modifier = Modifier.semantics {
                                role = Role.Button
                                contentDescription = if (isEnglish) "Delete pictogram" else "Eliminar pictograma"
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = if (highContrast) Color(0xFFFF5252) else MaterialTheme.colorScheme.error
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
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = {
                    Text(
                        if (isEnglish) "Delete this pictogram?" else "¿Eliminar este pictograma?",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        if (isEnglish) {
                            "This action will remove the custom pictogram from boards and favorites. Phrases already recorded in history will retain their spoken text."
                        } else {
                            "Esta acción eliminará el pictograma personalizado de los tableros y favoritos. Las frases registradas anteriormente en el historial conservarán su texto."
                        }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            PictoHaptics.performEmphasis(hapticFeedback, hapticsEnabled)
                            editingPicto?.let { onDeletePicto?.invoke(it.id) }
                            showDeleteConfirm = false
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(if (isEnglish) "Delete" else "Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(if (isEnglish) "Cancel" else "Cancelar")
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = PictoDimens.SpaceExtraLarge)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(PictoDimens.SpaceMedium))

            // 1. Selector y previsualización de imagen
            Text("Imagen o Fotografía", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(PictoDimens.SpaceSmall))

            Card(
                onClick = {
                    PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                    photoPickerLauncher.launch("image/*")
                },
                shape = PictoDimens.CornerLarge,
                colors = CardDefaults.cardColors(
                    containerColor = selectedCategory.color
                ),
                border = BorderStroke(PictoDimens.BorderWidthStandard, selectedCategory.borderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Toca para seleccionar una imagen o fotografía del dispositivo"
                    }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap,
                            contentDescription = "Previsualización de imagen seleccionada",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(PictoDimens.SpaceMedium)
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(54.dp),
                                tint = selectedCategory.textColor
                            )
                            Spacer(Modifier.height(PictoDimens.SpaceSmall))
                            Text(
                                text = if (isEnglish) "Tap to choose a photo from device" else "Toca para elegir una foto del dispositivo",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = selectedCategory.textColor
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(PictoDimens.SpaceLarge))

            // 2. Nombre visible del pictograma
            Text(
                text = if (isEnglish) "Visible name" else "Nombre visible",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(PictoDimens.SpaceExtraSmall))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(if (isEnglish) "E.g.: Grandma Carmen, My puppy, Favorite toy" else "Ej: Abuela Carmen, Mi perrito, Juguete favorito") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = PictoDimens.CornerMedium
            )

            Spacer(Modifier.height(PictoDimens.SpaceLarge))

            // 3. Pronunciación de voz TTS (Opcional)
            Text(
                text = if (isEnglish) "Spoken text (TTS)" else "Texto a pronunciar (TTS)",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(PictoDimens.SpaceExtraSmall))

            OutlinedTextField(
                value = speechText,
                onValueChange = { speechText = it },
                label = { Text(if (isEnglish) "Optional: If empty, visible name will be spoken" else "Opcional: Si lo dejas vacío se pronunciará el nombre visible") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = PictoDimens.CornerMedium
            )

            Spacer(Modifier.height(PictoDimens.SpaceLarge))

            // 4. Selector de categoría gramatical (Fitzgerald)
            Text(
                text = if (isEnglish) "Category (Fitzgerald Color)" else "Categoría (Color Fitzgerald)",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(PictoDimens.SpaceExtraSmall))

            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCategory.getLabel(languageCode),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(selectedCategory.color, RoundedCornerShape(4.dp))
                                .border(1.5.dp, selectedCategory.borderColor, RoundedCornerShape(4.dp))
                        )
                    },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    shape = PictoDimens.CornerMedium
                )

                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    Category.entries.forEach { category ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(category.color, RoundedCornerShape(4.dp))
                                            .border(1.5.dp, category.borderColor, RoundedCornerShape(4.dp))
                                    )
                                    Spacer(Modifier.width(PictoDimens.SpaceSmall))
                                    Text(category.getLabel(languageCode), fontWeight = FontWeight.Bold)
                                }
                            },
                            onClick = {
                                selectedCategory = category
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(PictoDimens.SpaceSection))

            // 5. Botón Guardar
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        isSaving = true
                        PictoHaptics.performEmphasis(hapticFeedback, hapticsEnabled)
                        onSavePicto(
                            name.trim(),
                            speechText.trim().ifEmpty { null },
                            selectedCategory,
                            localImagePath
                        )
                        onBack()
                    }
                },
                enabled = name.isNotBlank() && !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = if (isEnglish) "Save custom pictogram" else "Guardar pictograma personalizado"
                    },
                shape = PictoDimens.CornerMedium
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(PictoDimens.SpaceSmall))
                Text(if (isEnglish) "Save Pictogram" else "Guardar Pictograma", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(PictoDimens.SpaceLarge))
        }
    }
}
