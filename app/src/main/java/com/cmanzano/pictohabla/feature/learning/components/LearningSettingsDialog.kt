package com.cmanzano.pictohabla.feature.learning.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmanzano.pictohabla.core.designsystem.PictoColors
import com.cmanzano.pictohabla.core.designsystem.PictoDimens
import com.cmanzano.pictohabla.core.model.Category
import com.cmanzano.pictohabla.feature.learning.model.LearningConfig

/**
 * Diálogo de configuración del Modo Aprender accesible para adultos, terapeutas y educadores.
 * Permite adaptar la dificultad motora y cognitiva de los ejercicios:
 * - Cantidad de alternativas (2, 3 o 4 opciones)
 * - Restricción temática por categoría
 * - Locución automática por voz al presentar cada ejercicio
 */
@Composable
fun LearningSettingsDialog(
    currentConfig: LearningConfig,
    languageCode: String,
    highContrast: Boolean = false,
    onDismiss: () -> Unit,
    onSaveConfig: (LearningConfig) -> Unit
) {
    var selectedOptionsCount by remember { mutableIntStateOf(currentConfig.optionsCount) }
    var selectedCategoryFilter by remember { mutableStateOf(currentConfig.categoryFilter) }
    var autoSpeakEnabled by remember { mutableStateOf(currentConfig.autoSpeak) }

    val optionsValues = listOf(2, 3, 4)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Ajustes de Aprendizaje",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(PictoDimens.SpaceMedium)
            ) {
                // Selector de número de opciones
                Column {
                    Text(
                        text = "Número de opciones por pregunta:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(PictoDimens.SpaceSmall))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        optionsValues.forEachIndexed { index, count ->
                            SegmentedButton(
                                selected = selectedOptionsCount == count,
                                onClick = { selectedOptionsCount = count },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = optionsValues.size),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = if (highContrast) PictoColors.HighlightHighContrast else PictoColors.Primary,
                                    activeContentColor = if (highContrast) Color.Black else Color.White
                                )
                            ) {
                                Text("$count", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }

                // Filtro por categoría temática
                Column {
                    Text(
                        text = "Vocabulario o categoría:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(PictoDimens.SpaceExtraSmall))

                    val scrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.spacedBy(PictoDimens.SpaceSmall)
                    ) {
                        // Opción "Todas las categorías"
                        val isAllSelected = selectedCategoryFilter == null
                        FilterChip(
                            selected = isAllSelected,
                            onClick = { selectedCategoryFilter = null },
                            label = { Text("Todas", fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                if (isAllSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                } else {
                                    Icon(Icons.Default.AllInclusive, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (highContrast) PictoColors.HighlightHighContrast else PictoColors.Primary,
                                selectedLabelColor = if (highContrast) Color.Black else Color.White,
                                selectedLeadingIconColor = if (highContrast) Color.Black else Color.White
                            )
                        )

                        // Chips individuales de categorías
                        Category.entries.forEach { cat ->
                            val isSelected = selectedCategoryFilter == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategoryFilter = cat },
                                label = { Text(cat.getLabel(languageCode), fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    } else {
                                        Icon(cat.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (highContrast) PictoColors.HighlightHighContrast else PictoColors.Primary,
                                    selectedLabelColor = if (highContrast) Color.Black else Color.White,
                                    selectedLeadingIconColor = if (highContrast) Color.Black else Color.White
                                )
                            )
                        }
                    }
                }

                // Switch de reproducción de voz automática
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Locución automática",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Pronunciar en voz alta al mostrar cada ejercicio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(PictoDimens.SpaceSmall))
                    Switch(
                        checked = autoSpeakEnabled,
                        onCheckedChange = { autoSpeakEnabled = it },
                        modifier = Modifier.semantics {
                            role = Role.Switch
                            contentDescription = "Activar locución automática de ejercicios"
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveConfig(
                        LearningConfig(
                            optionsCount = selectedOptionsCount,
                            categoryFilter = selectedCategoryFilter,
                            autoSpeak = autoSpeakEnabled
                        )
                    )
                },
                shape = PictoDimens.CornerMedium
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
