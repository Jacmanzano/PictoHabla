package com.cmanzano.pictohabla.feature.arasaac

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmanzano.pictohabla.core.designsystem.PictoColors
import com.cmanzano.pictohabla.core.designsystem.PictoDimens
import com.cmanzano.pictohabla.core.designsystem.PictoHaptics
import com.cmanzano.pictohabla.core.model.Category
import com.cmanzano.pictohabla.core.model.Pictogram
import com.cmanzano.pictohabla.core.network.ArasaacApiService
import com.cmanzano.pictohabla.core.network.ArasaacSearchResult
import com.cmanzano.pictohabla.core.util.ArasaacImageHelper
import com.cmanzano.pictohabla.data.repository.PictogramRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pantalla para que educadores y cuidadores busquen e incorporen nuevos pictogramas
 * desde el catálogo oficial de ARASAAC mediante su API REST.
 *
 * Flujo:
 * 1. Búsqueda por palabra o concepto con llamada a la API oficial.
 * 2. Visualización de resultados con vista previa de imagen PNG y nombres.
 * 3. Selección y asignación de categoría de la Clave de Fitzgerald Modificada.
 * 4. Descarga local persistente a almacenamiento privado (`arasaac_pictos/`).
 * 5. Registro automático en Room: disponible 100% offline para siempre.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArasaacSearchScreen(
    pictogramRepository: PictogramRepository,
    languageCode: String,
    highContrast: Boolean,
    hapticsEnabled: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<ArasaacSearchResult>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }

    // Diálogo de confirmación para añadir pictograma
    var selectedResultForAdd by remember { mutableStateOf<ArasaacSearchResult?>(null) }
    var customName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Category.THINGS) }
    var isDownloading by remember { mutableStateOf(false) }

    fun executeSearch() {
        if (searchQuery.isBlank()) return
        PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
        isLoading = true
        errorMessage = null
        hasSearched = true

        coroutineScope.launch {
            val result = ArasaacApiService.searchPictograms(searchQuery.trim(), locale = "es")
            isLoading = false
            result.onSuccess { list ->
                searchResults = list
            }.onFailure { ex ->
                errorMessage = "No se pudo conectar con ARASAAC. Comprueba tu conexión a Internet."
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Buscar en ARASAAC",
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
                            contentDescription = "Volver al tablero de comunicación"
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (highContrast) Color.Black else MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = PictoDimens.SpaceMedium, vertical = PictoDimens.SpaceSmall)
        ) {
            // 1. Campo de búsqueda accesible
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ej: perro, zapato, piscina...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = PictoColors.Primary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Borrar texto")
                            }
                        }
                    },
                    singleLine = true,
                    shape = PictoDimens.CornerPill,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (highContrast) Color.White else PictoColors.Primary
                    )
                )

                Spacer(Modifier.width(PictoDimens.SpaceSmall))

                Button(
                    onClick = { executeSearch() },
                    shape = PictoDimens.CornerPill,
                    enabled = searchQuery.isNotBlank() && !isLoading
                ) {
                    Text("Buscar")
                }
            }

            Spacer(Modifier.height(PictoDimens.SpaceMedium))

            // 2. Estado de carga, error o resultados
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PictoColors.Primary)
                            Spacer(Modifier.height(PictoDimens.SpaceSmall))
                            Text(
                                text = "Consultando catálogo oficial de ARASAAC...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(PictoDimens.SpaceLarge)
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(PictoDimens.SpaceMedium))
                            OutlinedButton(onClick = { executeSearch() }) {
                                Text("Reintentar búsqueda")
                            }
                        }
                    }
                }

                hasSearched && searchResults.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.Gray
                            )
                            Spacer(Modifier.height(PictoDimens.SpaceSmall))
                            Text(
                                text = "No se encontraron pictogramas para \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Prueba con sinónimos o palabras en infinitivo / singular",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                !hasSearched -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = PictoDimens.CornerLarge,
                            colors = CardDefaults.cardColors(
                                containerColor = if (highContrast) Color(0xFF161616) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(1.dp, if (highContrast) Color.White else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.padding(PictoDimens.SpaceMedium)
                        ) {
                            Column(
                                modifier = Modifier.padding(PictoDimens.SpaceLarge),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = PictoColors.Primary
                                )
                                Spacer(Modifier.height(PictoDimens.SpaceSmall))
                                Text(
                                    text = "Amplía tu catálogo con ARASAAC",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(PictoDimens.SpaceExtraSmall))
                                Text(
                                    text = "Busca cualquier palabra para previsualizar y añadir nuevos símbolos oficiales. Una vez descargados, estarán siempre disponibles sin conexión a Internet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                else -> {
                    // Resultados en cuadrícula de tarjetas
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 110.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = PictoDimens.SpaceSmall),
                        horizontalArrangement = Arrangement.spacedBy(PictoDimens.SpaceSmall),
                        verticalArrangement = Arrangement.spacedBy(PictoDimens.SpaceSmall)
                    ) {
                        items(searchResults, key = { it.id }) { item ->
                            ArasaacResultCard(
                                result = item,
                                highContrast = highContrast,
                                onClick = {
                                    PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                                    selectedResultForAdd = item
                                    customName = item.name.replaceFirstChar { it.uppercase() }
                                    selectedCategory = inferCategoryFromArasaac(item.categories)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo de configuración para incorporar el pictograma
    if (selectedResultForAdd != null) {
        val candidate = selectedResultForAdd!!
        var categoryMenuExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                if (!isDownloading) selectedResultForAdd = null
            },
            title = {
                Text(
                    text = "Añadir a tu catálogo",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ArasaacAsyncPreview(arasaacId = candidate.id, size = 100.dp)
                    }

                    Spacer(Modifier.height(PictoDimens.SpaceSmall))

                    Text("Nombre visible:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(PictoDimens.SpaceSmall))

                    Text("Categoría Fitzgerald:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    ExposedDropdownMenuBox(
                        expanded = categoryMenuExpanded,
                        onExpandedChange = { categoryMenuExpanded = !categoryMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory.getLabel(languageCode),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false }
                        ) {
                            Category.entries.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.getLabel(languageCode)) },
                                    onClick = {
                                        selectedCategory = cat
                                        categoryMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customName.isNotBlank() && !isDownloading) {
                            isDownloading = true
                            coroutineScope.launch {
                                val dlResult = ArasaacApiService.downloadPictogramBytes(candidate.id)
                                dlResult.onSuccess { bytes ->
                                    val localPath = ArasaacImageHelper.saveArasaacImage(context, candidate.id, bytes)
                                    pictogramRepository.createArasaacPictogram(
                                        arasaacId = candidate.id,
                                        name = customName.trim(),
                                        category = selectedCategory,
                                        localImagePath = localPath
                                    )
                                    isDownloading = false
                                    selectedResultForAdd = null
                                    snackbarHostState.showSnackbar(
                                        "«$customName» añadido al catálogo (disponible offline)"
                                    )
                                }.onFailure { ex ->
                                    isDownloading = false
                                    snackbarHostState.showSnackbar(
                                        "Error al descargar la imagen: ${ex.message}"
                                    )
                                }
                            }
                        }
                    },
                    enabled = customName.isNotBlank() && !isDownloading
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Descargando...")
                    } else {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Guardar offline")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedResultForAdd = null },
                    enabled = !isDownloading
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Tarjeta individual para mostrar un resultado de búsqueda de ARASAAC con vista previa de imagen.
 */
@Composable
private fun ArasaacResultCard(
    result: ArasaacSearchResult,
    highContrast: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = PictoDimens.CornerMedium,
        colors = CardDefaults.cardColors(
            containerColor = if (highContrast) Color.Black else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (highContrast) PictoDimens.BorderWidthHighContrast else 1.dp,
            color = if (highContrast) Color.White else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .semantics {
                role = Role.Button
                contentDescription = "Pictograma oficial ${result.name}, ID ${result.id}. Toca para añadir al catálogo."
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PictoDimens.SpaceSmall),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ArasaacAsyncPreview(arasaacId = result.id, size = 64.dp)

            Spacer(Modifier.height(PictoDimens.SpaceExtraSmall))

            Text(
                text = result.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (highContrast) Color.White else MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "ID: ${result.id}",
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}

/**
 * Componente para cargar y previsualizar asíncronamente una imagen de ARASAAC
 * desde la caché local o descargándola de forma ligera por red.
 */
@Composable
private fun ArasaacAsyncPreview(
    arasaacId: Int,
    size: androidx.compose.ui.unit.Dp
) {
    val context = LocalContext.current
    var previewBitmap by remember(arasaacId) { mutableStateOf<ImageBitmap?>(null) }
    var isLoadingPreview by remember(arasaacId) { mutableStateOf(false) }

    LaunchedEffect(arasaacId) {
        // 1. Intentar cargar desde caché/almacenamiento local
        val local = ArasaacImageHelper.loadArasaacBitmap(context, arasaacId)
        if (local != null) {
            previewBitmap = local.asImageBitmap()
            return@LaunchedEffect
        }

        // 2. Descargar imagen para previsualización
        isLoadingPreview = true
        withContext(Dispatchers.IO) {
            val dlResult = ArasaacApiService.downloadPictogramBytes(arasaacId)
            dlResult.onSuccess { bytes ->
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) {
                    previewBitmap = bmp.asImageBitmap()
                }
            }
        }
        isLoadingPreview = false
    }

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        when {
            previewBitmap != null -> {
                Image(
                    bitmap = previewBitmap!!,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            isLoadingPreview -> {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
            else -> {
                Icon(
                    Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.LightGray
                )
            }
        }
    }
}

/**
 * Infiere la categoría de la Clave de Fitzgerald Modificada basándose en las categorías de ARASAAC.
 */
private fun inferCategoryFromArasaac(categories: List<String>): Category {
    val text = categories.joinToString(" ").lowercase()
    return when {
        text.contains("family") || text.contains("person") || text.contains("pronoun") || text.contains("profession") -> Category.PEOPLE
        text.contains("verb") || text.contains("action") -> Category.ACTIONS
        text.contains("food") || text.contains("beverage") || text.contains("feeding") -> Category.FOOD
        text.contains("feeling") || text.contains("emotion") || text.contains("symptom") || text.contains("sensation") -> Category.FEELINGS
        text.contains("place") || text.contains("building") || text.contains("room") -> Category.PLACES
        text.contains("body") || text.contains("anatomy") -> Category.BODY
        text.contains("meteorology") || text.contains("weather") -> Category.WEATHER
        text.contains("question") || text.contains("expression") -> Category.QUESTIONS
        text.contains("polite") || text.contains("social") -> Category.SOCIAL
        else -> Category.THINGS
    }
}
