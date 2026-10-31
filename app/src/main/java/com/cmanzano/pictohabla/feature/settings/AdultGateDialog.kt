package com.cmanzano.pictohabla.feature.settings

import androidx.compose.runtime.Composable
import com.cmanzano.pictohabla.feature.parental.ParentalPinDialog

/**
 * Barrera de control parental para adultos, profesores y cuidadores.
 * Integra el sistema de PIN parental configurable de 6 dígitos con protección
 * de fuerza bruta, tiempo de espera progresivo y autenticación biométrica/credencial del dispositivo.
 */
@Composable
fun AdultGateDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    ParentalPinDialog(
        onDismiss = onDismiss,
        onSuccess = onSuccess
    )
}
