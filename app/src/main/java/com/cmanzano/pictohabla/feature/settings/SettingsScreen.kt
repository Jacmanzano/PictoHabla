package com.cmanzano.pictohabla.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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

/**
 * Pantalla de configuración accesible de PictoHabla.
 * Permite ajustar idioma, parámetros de síntesis de voz (TTS),
 * tamaño de cuadrícula, feedback háptico y modo de alto contraste.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    highContrast: Boolean,
    onContrastToggle: (Boolean) -> Unit,
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    pitch: Float,
    onPitchChange: (Float) -> Unit,
    hapticsEnabled: Boolean,
    onHapticsToggle: (Boolean) -> Unit,
    largeGrid: Boolean,
    onLargeGridToggle: (Boolean) -> Unit,
    onTestVoice: () -> Unit,
    onBack: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Ajustes de PictoHabla", fontWeight = FontWeight.Bold) },
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = PictoDimens.SpaceExtraLarge)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(PictoDimens.SpaceSmall))

            // 1. SECCIÓN IDIOMA
            Text(
                text = "Idioma de Comunicación",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(PictoDimens.SpaceMedium))

            val languages = listOf(
                "Español" to "ES",
                "English" to "EN",
                "Français" to "FR",
                "Deutsch" to "DE",
                "Italiano" to "IT"
            )

            languages.forEach { (name, code) ->
                val isSelected = currentLanguage == code
                Surface(
                    onClick = {
                        PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                        onLanguageChange(code)
                    },
                    shape = PictoDimens.CornerMedium,
                    color = if (isSelected) {
                        if (highContrast) Color(0xFF333333) else MaterialTheme.colorScheme.primaryContainer
                    } else Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = PictoDimens.SpaceExtraSmall)
                        .semantics {
                            role = Role.RadioButton
                            contentDescription = "Idioma $name" + if (isSelected) ", seleccionado" else ""
                        },
                    border = if (isSelected) {
                        BorderStroke(
                            PictoDimens.BorderWidthStandard,
                            if (highContrast) Color.White else MaterialTheme.colorScheme.primary
                        )
                    } else null
                ) {
                    Row(
                        modifier = Modifier.padding(PictoDimens.SpaceMedium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Text(
                            text = name,
                            modifier = Modifier.padding(start = PictoDimens.SpaceMedium),
                            fontWeight = FontWeight.Bold,
                            color = if (highContrast) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = PictoDimens.SpaceLarge))

            // 2. SECCIÓN SINTETIZADOR DE VOZ (TTS)
            Text(
                text = "Voz y Pronunciación (TTS)",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(PictoDimens.SpaceMedium))

            Text("Velocidad de habla: ${(speed * 100).toInt()}%", fontWeight = FontWeight.Bold)
            Slider(
                value = speed,
                onValueChange = onSpeedChange,
                valueRange = 0.5f..1.8f,
                modifier = Modifier.semantics { contentDescription = "Velocidad de voz ${(speed * 100).toInt()}%" }
            )

            Text("Tono de la voz: ${(pitch * 100).toInt()}%", fontWeight = FontWeight.Bold)
            Slider(
                value = pitch,
                onValueChange = onPitchChange,
                valueRange = 0.5f..1.8f,
                modifier = Modifier.semantics { contentDescription = "Tono de voz ${(pitch * 100).toInt()}%" }
            )

            OutlinedButton(
                onClick = {
                    PictoHaptics.performEmphasis(hapticFeedback, hapticsEnabled)
                    onTestVoice()
                },
                modifier = Modifier
                    .padding(top = PictoDimens.SpaceSmall)
                    .heightIn(min = PictoDimens.TouchTargetMinimum)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Probar la pronunciación de voz actual"
                    },
                shape = PictoDimens.CornerMedium,
                border = BorderStroke(1.5.dp, if (highContrast) Color.White else MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                Text(
                    text = "  Probar voz de PictoHabla",
                    fontWeight = FontWeight.Bold,
                    color = if (highContrast) Color.White else MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(Modifier.padding(vertical = PictoDimens.SpaceLarge))

            // 3. SECCIÓN ACCESIBILIDAD VISUAL Y MOTORA
            Text(
                text = "Accesibilidad y Ergonomía",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(PictoDimens.SpaceMedium))

            // Conmutador Alto Contraste
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = PictoDimens.SpaceSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(PictoDimens.SpaceMedium))
                Column(Modifier.weight(1f)) {
                    Text("Modo Alto Contraste", fontWeight = FontWeight.Bold)
                    Text(
                        text = "Fondo oscuro y bordes resaltados para baja visión o fotofobia",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = highContrast,
                    onCheckedChange = {
                        PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                        onContrastToggle(it)
                    }
                )
            }

            // Conmutador Tamaño de Cuadrícula
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = PictoDimens.SpaceSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.GridOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(PictoDimens.SpaceMedium))
                Column(Modifier.weight(1f)) {
                    Text("Pictogramas Grandes", fontWeight = FontWeight.Bold)
                    Text(
                        text = "Aumenta el tamaño táctil de cada pictograma para facilitar la pulsación",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = largeGrid,
                    onCheckedChange = {
                        PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                        onLargeGridToggle(it)
                    }
                )
            }

            // Conmutador Feedback Háptico
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = PictoDimens.SpaceSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Vibration, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(PictoDimens.SpaceMedium))
                Column(Modifier.weight(1f)) {
                    Text("Vibración Táctil (Feedback Háptico)", fontWeight = FontWeight.Bold)
                    Text(
                        text = "Emite una suave confirmación física al pulsar pictogramas y botones",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = hapticsEnabled,
                    onCheckedChange = {
                        PictoHaptics.performClick(hapticFeedback, true)
                        onHapticsToggle(it)
                    }
                )
            }

            HorizontalDivider(Modifier.padding(vertical = PictoDimens.SpaceLarge))

            // 4. ATRIBUCIÓN LEGAL ARASAAC
            Card(
                shape = PictoDimens.CornerMedium,
                colors = CardDefaults.cardColors(
                    containerColor = if (highContrast) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, if (highContrast) Color.Gray else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(PictoDimens.SpaceLarge)) {
                    Text(
                        text = "Atribución y Licencias",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(PictoDimens.SpaceExtraSmall))
                    Text(
                        text = "Los símbolos pictográficos utilizados son propiedad del Gobierno de Aragón y han sido creados por Sergio Palao para ARASAAC (http://www.arasaac.org), que los distribuye bajo Licencia Creative Commons BY-NC-SA.",
                        fontSize = 12.sp,
                        color = if (highContrast) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(PictoDimens.SpaceSection))
        }
    }
}
