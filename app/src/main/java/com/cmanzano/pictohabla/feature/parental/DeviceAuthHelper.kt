package com.cmanzano.pictohabla.feature.parental

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent

/**
 * Utilidad para coordinar la autenticación segura del dispositivo Android
 * mediante las credenciales del sistema (huella dactilar, PIN, patrón o contraseña).
 *
 * Utiliza los servicios nativos de la plataforma Android (KeyguardManager) sin
 * requerir dependencias de FragmentActivity ni bibliotecas de terceros.
 */
object DeviceAuthHelper {

    /**
     * Comprueba si el dispositivo tiene configurado un bloqueo de pantalla seguro
     * (PIN, patrón, huella o contraseña en los Ajustes del sistema de Android).
     */
    fun isDeviceSecure(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return keyguardManager?.isDeviceSecure == true
    }

    /**
     * Genera el Intent del sistema para solicitar la confirmación de identidad
     * con las credenciales de bloqueo de pantalla del dispositivo (biometría, PIN, patrón o contraseña).
     */
    @Suppress("DEPRECATION")
    fun createConfirmDeviceCredentialIntent(
        context: Context,
        title: CharSequence = "Autorización de adulto",
        description: CharSequence = "Usa tu huella o el código de bloqueo de pantalla de Android"
    ): Intent? {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return keyguardManager?.createConfirmDeviceCredentialIntent(title, description)
    }
}
