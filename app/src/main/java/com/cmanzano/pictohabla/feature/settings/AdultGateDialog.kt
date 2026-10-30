package com.cmanzano.pictohabla.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmanzano.pictohabla.core.designsystem.PictoDimens
import kotlin.random.Random

/**
 * Diálogo de barrera de uso para adultos (profesores y cuidadores).
 * Evita accesos accidentales a la configuración durante el uso diario del comunicador por parte del niño.
 * (Aclaración: No es un mecanismo de seguridad de cuentas, sino una protección de accesibilidad).
 */
@Composable
fun AdultGateDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val num1 = remember { Random.nextInt(3, 8) }
    val num2 = remember { Random.nextInt(2, 6) }
    val expectedAnswer = remember { num1 + num2 }

    var userAnswer by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Zona para Adultos y Cuidadores",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Para evitar cambios accidentales en la voz, los tableros o el tamaño de los pictogramas, resuelve esta sencilla operación:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(PictoDimens.SpaceMedium))

                Text(
                    text = "¿Cuánto es $num1 + $num2?",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(PictoDimens.SpaceMedium))

                OutlinedTextField(
                    value = userAnswer,
                    onValueChange = {
                        userAnswer = it.filter { char -> char.isDigit() }.take(3)
                        hasError = false
                    },
                    isError = hasError,
                    label = { Text("Resultado") },
                    supportingText = if (hasError) {
                        { Text("Resultado incorrecto. Inténtalo de nuevo.", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = PictoDimens.CornerMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = userAnswer.toIntOrNull()
                    if (parsed == expectedAnswer) {
                        onSuccess()
                    } else {
                        hasError = true
                    }
                },
                shape = PictoDimens.CornerSmall
            ) {
                Text("Acceder", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
