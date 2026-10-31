package com.cmanzano.pictohabla.feature.parental

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmanzano.pictohabla.core.designsystem.PictoDimens
import kotlinx.coroutines.delay

/**
 * Diálogo de control parental con PIN de 6 dígitos y autorización del dispositivo Android.
 * Sustituye el antiguo mecanismo de suma por un sistema robusto, con protección anti-fuerza bruta
 * y recuperación segura mediante biometría o clave del sistema.
 */
@Composable
fun ParentalPinDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    pinManager: ParentalPinManager? = null
) {
    val context = LocalContext.current
    val manager = remember(pinManager, context) {
        pinManager ?: ParentalPinManager.getInstance(context)
    }

    val isPinSet = remember { manager.isPinSet() }
    val isDeviceSecure = remember { DeviceAuthHelper.isDeviceSecure(context) }

    // Estados para configuración inicial
    var isDeviceAuthorizedForSetup by remember { mutableStateOf(false) }
    var setupPin by remember { mutableStateOf("") }
    var setupConfirmPin by remember { mutableStateOf("") }
    var setupErrorMessage by remember { mutableStateOf<String?>(null) }

    // Estados para verificación regular
    var enterPin by remember { mutableStateOf("") }
    var verifyErrorMessage by remember { mutableStateOf<String?>(null) }
    var isPinHidden by remember { mutableStateOf(true) }

    val setupAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            isDeviceAuthorizedForSetup = true
            setupErrorMessage = null
        } else {
            setupErrorMessage = "Autorización del dispositivo no completada."
        }
    }

    val verifyAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            manager.resetAttempts()
            onSuccess()
        } else {
            verifyErrorMessage = "Autenticación del dispositivo no completada."
        }
    }

    // Estado de bloqueo progresivo
    var remainingLockoutSeconds by remember {
        mutableLongStateOf(manager.getLockoutRemainingSeconds())
    }

    LaunchedEffect(remainingLockoutSeconds) {
        if (remainingLockoutSeconds > 0) {
            delay(1000)
            remainingLockoutSeconds = manager.getLockoutRemainingSeconds()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (!isPinSet) Icons.Default.Security else Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = if (!isPinSet) "Configuración de Control Parental" else "Zona para Adultos y Cuidadores",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isPinSet) {
                    // CASO A: EL PIN TODAVÍA NO HA SIDO CONFIGURADO
                    if (!isDeviceSecure) {
                        // El dispositivo no tiene bloqueo de pantalla: educar al usuario
                        Text(
                            text = "Para proteger el acceso de los niños a la configuración, debes configurar primero un bloqueo de pantalla (PIN, patrón o huella) en tu dispositivo Android.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(PictoDimens.SpaceMedium))
                        OutlinedButton(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                                } catch (_: Exception) {}
                            },
                            shape = PictoDimens.CornerMedium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Abrir Ajustes de Seguridad de Android")
                        }
                    } else if (!isDeviceAuthorizedForSetup) {
                        // El adulto debe autorizarse mediante el dispositivo para crear el primer PIN
                        Text(
                            text = "Para garantizar que un menor no cree el PIN por su cuenta, autoriza la configuración inicial con la huella o el código de bloqueo de tu dispositivo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(PictoDimens.SpaceMedium))
                        Button(
                            onClick = {
                                val intent = DeviceAuthHelper.createConfirmDeviceCredentialIntent(
                                    context = context,
                                    title = "Configurar Control Parental",
                                    description = "Autoriza como adulto para crear el PIN"
                                )
                                if (intent != null) {
                                    try {
                                        setupAuthLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        setupErrorMessage = e.message ?: "Error al abrir la autenticación"
                                    }
                                } else {
                                    isDeviceAuthorizedForSetup = true
                                }
                            },
                            shape = PictoDimens.CornerMedium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Autorizar como adulto")
                        }

                        if (setupErrorMessage != null) {
                            Spacer(Modifier.height(PictoDimens.SpaceSmall))
                            Text(
                                text = setupErrorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Adulto autorizado: introducir y confirmar el nuevo PIN de 6 dígitos
                        Text(
                            text = "Crea un PIN numérico de 6 dígitos que recordarás para acceder a las opciones de adultos:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(PictoDimens.SpaceMedium))

                        OutlinedTextField(
                            value = setupPin,
                            onValueChange = {
                                setupPin = it.filter { char -> char.isDigit() }.take(6)
                                setupErrorMessage = null
                            },
                            label = { Text("Nuevo PIN (6 dígitos)") },
                            visualTransformation = if (isPinHidden) PasswordVisualTransformation() else VisualTransformation.None,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            trailingIcon = {
                                IconButton(onClick = { isPinHidden = !isPinHidden }) {
                                    Icon(
                                        if (isPinHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isPinHidden) "Mostrar PIN" else "Ocultar PIN"
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = PictoDimens.CornerMedium
                        )

                        Spacer(Modifier.height(PictoDimens.SpaceSmall))

                        OutlinedTextField(
                            value = setupConfirmPin,
                            onValueChange = {
                                setupConfirmPin = it.filter { char -> char.isDigit() }.take(6)
                                setupErrorMessage = null
                            },
                            label = { Text("Confirmar PIN (6 dígitos)") },
                            visualTransformation = if (isPinHidden) PasswordVisualTransformation() else VisualTransformation.None,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = PictoDimens.CornerMedium
                        )

                        if (setupErrorMessage != null) {
                            Spacer(Modifier.height(PictoDimens.SpaceSmall))
                            Text(
                                text = setupErrorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // CASO B: EL PIN YA ESTÁ CONFIGURADO
                    if (remainingLockoutSeconds > 0) {
                        Text(
                            text = "Has superado el límite de intentos consecutivos.",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(PictoDimens.SpaceSmall))
                        Text(
                            text = "Por seguridad, debes esperar $remainingLockoutSeconds segundos antes de volver a intentarlo.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(PictoDimens.SpaceMedium))

                        // Opción de rescate mediante biometría / credenciales del sistema
                        if (isDeviceSecure) {
                            OutlinedButton(
                                onClick = {
                                    val intent = DeviceAuthHelper.createConfirmDeviceCredentialIntent(
                                        context = context,
                                        title = "Desbloquear Control Parental",
                                        description = "Autoriza con la clave o biometría de Android"
                                    )
                                    if (intent != null) {
                                        try {
                                            verifyAuthLauncher.launch(intent)
                                        } catch (e: Exception) {
                                            verifyErrorMessage = e.message ?: "Error de autenticación"
                                        }
                                    } else {
                                        verifyErrorMessage = "No hay bloqueo de pantalla configurado."
                                    }
                                },
                                shape = PictoDimens.CornerMedium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text("Desbloquear con Android")
                            }
                        }
                    } else {
                        Text(
                            text = "Introduce tu PIN de 6 dígitos para acceder:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(PictoDimens.SpaceMedium))

                        OutlinedTextField(
                            value = enterPin,
                            onValueChange = {
                                enterPin = it.filter { char -> char.isDigit() }.take(6)
                                verifyErrorMessage = null
                            },
                            label = { Text("PIN parental (6 dígitos)") },
                            visualTransformation = if (isPinHidden) PasswordVisualTransformation() else VisualTransformation.None,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            trailingIcon = {
                                IconButton(onClick = { isPinHidden = !isPinHidden }) {
                                    Icon(
                                        if (isPinHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isPinHidden) "Mostrar PIN" else "Ocultar PIN"
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = PictoDimens.CornerMedium
                        )

                        if (verifyErrorMessage != null) {
                            Spacer(Modifier.height(PictoDimens.SpaceSmall))
                            Text(
                                text = verifyErrorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(Modifier.height(PictoDimens.SpaceSmall))

                        // Opción de recuperación si olvidó el PIN
                        if (isDeviceSecure) {
                            TextButton(
                                onClick = {
                                    val intent = DeviceAuthHelper.createConfirmDeviceCredentialIntent(
                                        context = context,
                                        title = "Recuperar Control Parental",
                                        description = "Verifica tu identidad como adulto"
                                    )
                                    if (intent != null) {
                                        try {
                                            verifyAuthLauncher.launch(intent)
                                        } catch (e: Exception) {
                                            verifyErrorMessage = e.message ?: "Error de autenticación"
                                        }
                                    } else {
                                        verifyErrorMessage = "No hay bloqueo de pantalla configurado."
                                    }
                                }
                            ) {
                                Text(
                                    text = "¿Olvidaste el PIN? Acceder con Android",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isPinSet) {
                if (isDeviceAuthorizedForSetup) {
                    Button(
                        onClick = {
                            if (setupPin != setupConfirmPin) {
                                setupErrorMessage = "Los dos códigos introducidos no coinciden."
                                return@Button
                            }
                            when (val result = manager.setInitialPin(setupPin)) {
                                is PinValidationResult.Valid -> onSuccess()
                                is PinValidationResult.Invalid -> setupErrorMessage = result.message
                            }
                        },
                        enabled = setupPin.length == 6 && setupConfirmPin.length == 6,
                        shape = PictoDimens.CornerSmall
                    ) {
                        Text("Guardar y Entrar", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                if (remainingLockoutSeconds <= 0) {
                    Button(
                        onClick = {
                            when (val result = manager.verifyPin(enterPin)) {
                                is PinVerificationResult.Success -> onSuccess()
                                is PinVerificationResult.Incorrect -> {
                                    verifyErrorMessage = "PIN incorrecto. Te quedan ${result.remainingAttempts} intento(s)."
                                    enterPin = ""
                                }
                                is PinVerificationResult.LockedOut -> {
                                    remainingLockoutSeconds = result.remainingSeconds
                                    verifyErrorMessage = "Has sido bloqueado temporalmente por seguridad."
                                    enterPin = ""
                                }
                            }
                        },
                        enabled = enterPin.length == 6,
                        shape = PictoDimens.CornerSmall
                    ) {
                        Text("Acceder", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Diálogo para cambiar el PIN parental desde la pantalla de Ajustes.
 * Solicita el PIN actual (o autorización del dispositivo), seguido del nuevo PIN y su confirmación.
 */
@Composable
fun ChangeParentalPinDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    pinManager: ParentalPinManager? = null
) {
    val context = LocalContext.current
    val manager = remember(pinManager, context) {
        pinManager ?: ParentalPinManager.getInstance(context)
    }

    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPinHidden by remember { mutableStateOf(true) }

    val changeAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val validation = manager.validatePinFormat(newPin)
            if (validation !is PinValidationResult.Valid) {
                errorMessage = (validation as PinValidationResult.Invalid).message
                return@rememberLauncherForActivityResult
            }
            if (newPin != confirmNewPin) {
                errorMessage = "El nuevo PIN y su confirmación no coinciden."
                return@rememberLauncherForActivityResult
            }
            manager.resetPinWithDeviceAuth(newPin)
            onSuccess()
        } else {
            errorMessage = "Autenticación cancelada."
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Cambiar PIN Parental", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Introduce tu PIN actual y el nuevo código de 6 dígitos:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(PictoDimens.SpaceMedium))

                OutlinedTextField(
                    value = currentPin,
                    onValueChange = {
                        currentPin = it.filter { char -> char.isDigit() }.take(6)
                        errorMessage = null
                    },
                    label = { Text("PIN Actual") },
                    visualTransformation = if (isPinHidden) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    trailingIcon = {
                        IconButton(onClick = { isPinHidden = !isPinHidden }) {
                            Icon(
                                if (isPinHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = PictoDimens.CornerMedium
                )

                Spacer(Modifier.height(PictoDimens.SpaceSmall))

                OutlinedTextField(
                    value = newPin,
                    onValueChange = {
                        newPin = it.filter { char -> char.isDigit() }.take(6)
                        errorMessage = null
                    },
                    label = { Text("Nuevo PIN (6 dígitos)") },
                    visualTransformation = if (isPinHidden) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = PictoDimens.CornerMedium
                )

                Spacer(Modifier.height(PictoDimens.SpaceSmall))

                OutlinedTextField(
                    value = confirmNewPin,
                    onValueChange = {
                        confirmNewPin = it.filter { char -> char.isDigit() }.take(6)
                        errorMessage = null
                    },
                    label = { Text("Confirmar Nuevo PIN") },
                    visualTransformation = if (isPinHidden) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = PictoDimens.CornerMedium
                )

                if (errorMessage != null) {
                    Spacer(Modifier.height(PictoDimens.SpaceSmall))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                if (DeviceAuthHelper.isDeviceSecure(context)) {
                    Spacer(Modifier.height(PictoDimens.SpaceSmall))
                    TextButton(
                        onClick = {
                            val intent = DeviceAuthHelper.createConfirmDeviceCredentialIntent(
                                context = context,
                                title = "Restablecer PIN Parental",
                                description = "Autoriza con la clave o biometría del dispositivo"
                            )
                            if (intent != null) {
                                try {
                                    changeAuthLauncher.launch(intent)
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Error de autenticación"
                                }
                            } else {
                                errorMessage = "No hay bloqueo de pantalla configurado."
                            }
                        }
                    ) {
                        Text("Olvidé el actual: Cambiar con Android", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (val result = manager.changePin(currentPin, newPin, confirmNewPin)) {
                        is PinChangeResult.Success -> onSuccess()
                        is PinChangeResult.Error -> errorMessage = result.message
                    }
                },
                enabled = currentPin.length == 6 && newPin.length == 6 && confirmNewPin.length == 6,
                shape = PictoDimens.CornerSmall
            ) {
                Text("Actualizar PIN", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
