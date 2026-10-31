package com.cmanzano.pictohabla.feature.settings

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.cmanzano.pictohabla.feature.parental.ChangeParentalPinDialog
import com.cmanzano.pictohabla.feature.parental.DeviceAuthHelper
import com.cmanzano.pictohabla.feature.parental.ParentalPinDialog
import com.cmanzano.pictohabla.feature.parental.ParentalPinManager

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
    onBack: () -> Unit,
    onNavigateBoards: () -> Unit = {}
) {
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val pinManager = remember(context) { ParentalPinManager.getInstance(context) }
    var isPinConfigured by remember { mutableStateOf(pinManager.isPinSet()) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showSetupPinDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val deviceAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pinManager.resetPinWithDeviceAuth(null)
            isPinConfigured = false
            statusMessage = "El PIN ha sido restablecido. Configura uno nuevo cuando lo desees."
        } else {
            statusMessage = "No se pudo restablecer: autenticación del dispositivo no completada."
        }
    }

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
                "English" to "EN"
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

            // 4. SECCIÓN VOCABULARIO ESENCIAL Y TABLEROS
            Text(
                text = "Vocabulario Esencial y Tableros",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(PictoDimens.SpaceMedium))

            Card(
                shape = PictoDimens.CornerMedium,
                colors = CardDefaults.cardColors(
                    containerColor = if (highContrast) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, if (highContrast) Color.Gray else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PictoDimens.SpaceLarge)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Dashboard,
                            contentDescription = null,
                            tint = if (highContrast) PictoColors.HighlightHighContrast else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(PictoDimens.SpaceMedium))
                        Column {
                            Text(
                                text = "Gestor de Vocabulario Esencial",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Organiza las categorías, personaliza los tableros de comunicación y selecciona el tablero activo.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(PictoDimens.SpaceMedium))
                    Button(
                        onClick = {
                            PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                            onNavigateBoards()
                        },
                        shape = PictoDimens.CornerMedium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Dashboard, contentDescription = null)
                        Spacer(Modifier.width(PictoDimens.SpaceSmall))
                        Text("Abrir Vocabulario Esencial y Tableros", fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = PictoDimens.SpaceLarge))

            // 5. SECCIÓN CONTROL PARENTAL
            Text(
                text = "Control Parental",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(PictoDimens.SpaceMedium))

            Card(
                shape = PictoDimens.CornerMedium,
                colors = CardDefaults.cardColors(
                    containerColor = if (highContrast) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, if (highContrast) Color.Gray else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PictoDimens.SpaceLarge)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isPinConfigured) Icons.Default.Lock else Icons.Default.Security,
                                contentDescription = null,
                                tint = if (highContrast) PictoColors.HighlightHighContrast else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(PictoDimens.SpaceMedium))
                            Column {
                                Text(
                                    text = "PIN de 6 dígitos para Adultos",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (isPinConfigured) "Estado: Configurado y activo" else "Estado: Sin configurar",
                                    fontSize = 12.sp,
                                    color = if (isPinConfigured) {
                                        if (highContrast) Color.Green else PictoColors.Success
                                    } else {
                                        if (highContrast) Color.Yellow else MaterialTheme.colorScheme.error
                                    },
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(PictoDimens.SpaceMedium))
                    Text(
                        text = "Protege el acceso a los ajustes, la edición de tableros y la descarga de pictogramas frente al uso accidental por parte de los niños.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(PictoDimens.SpaceMedium))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PictoDimens.SpaceSmall)
                    ) {
                        Button(
                            onClick = {
                                PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                                if (isPinConfigured) {
                                    showChangePinDialog = true
                                } else {
                                    showSetupPinDialog = true
                                }
                            },
                            shape = PictoDimens.CornerMedium,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(if (isPinConfigured) Icons.Default.LockReset else Icons.Default.Lock, contentDescription = null)
                            Spacer(Modifier.width(PictoDimens.SpaceSmall))
                            Text(if (isPinConfigured) "Cambiar PIN" else "Configurar PIN", fontWeight = FontWeight.Bold)
                        }

                        if (isPinConfigured && DeviceAuthHelper.isDeviceSecure(context)) {
                            OutlinedButton(
                                onClick = {
                                    PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                                    val intent = DeviceAuthHelper.createConfirmDeviceCredentialIntent(
                                        context = context,
                                        title = "Restablecer PIN Parental",
                                        description = "Autoriza con la clave o huella de tu dispositivo"
                                    )
                                    if (intent != null) {
                                        try {
                                            deviceAuthLauncher.launch(intent)
                                        } catch (e: Exception) {
                                            statusMessage = "No se pudo iniciar la autenticación: ${e.message}"
                                        }
                                    } else {
                                        statusMessage = "El dispositivo no tiene configurado un bloqueo seguro de pantalla."
                                    }
                                },
                                shape = PictoDimens.CornerMedium,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null)
                                Spacer(Modifier.width(PictoDimens.SpaceSmall))
                                Text("Restablecer", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (statusMessage != null) {
                        Spacer(Modifier.height(PictoDimens.SpaceSmall))
                        Text(
                            text = statusMessage ?: "",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = PictoDimens.SpaceLarge))

            // 6. ACERCA DE PICTOHABLA
            Card(
                shape = PictoDimens.CornerMedium,
                colors = CardDefaults.cardColors(
                    containerColor = if (highContrast) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, if (highContrast) Color.Gray else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PictoDimens.SpaceLarge),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_pictohabla_logo),
                        contentDescription = "Logotipo oficial de PictoHabla",
                        modifier = Modifier
                            .height(88.dp)
                            .padding(bottom = PictoDimens.SpaceSmall)
                    )
                    Text(
                        text = "PictoHabla",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (highContrast) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Versión 1.0 — Comunicación Aumentativa y Alternativa (CAA)",
                        fontSize = 12.sp,
                        color = if (highContrast) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(PictoDimens.SpaceMedium))

            // 5. ATRIBUCIÓN LEGAL ARASAAC
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

    if (showChangePinDialog) {
        ChangeParentalPinDialog(
            onDismiss = { showChangePinDialog = false },
            onSuccess = {
                showChangePinDialog = false
                isPinConfigured = true
                statusMessage = "PIN parental actualizado con éxito."
            }
        )
    }

    if (showSetupPinDialog) {
        ParentalPinDialog(
            onDismiss = { showSetupPinDialog = false },
            onSuccess = {
                showSetupPinDialog = false
                isPinConfigured = true
                statusMessage = "PIN parental configurado con éxito."
            }
        )
    }
}
