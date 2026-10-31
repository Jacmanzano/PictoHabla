package com.cmanzano.pictohabla.feature.parental

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Resultado de la validación de formato del PIN parental.
 */
sealed class PinValidationResult {
    data object Valid : PinValidationResult()
    data class Invalid(val message: String) : PinValidationResult()
}

/**
 * Resultado de la verificación del PIN introducido.
 */
sealed class PinVerificationResult {
    data object Success : PinVerificationResult()
    data class Incorrect(val remainingAttempts: Int) : PinVerificationResult()
    data class LockedOut(val remainingSeconds: Long) : PinVerificationResult()
}

/**
 * Resultado del cambio de PIN.
 */
sealed class PinChangeResult {
    data object Success : PinChangeResult()
    data class Error(val message: String) : PinChangeResult()
}

/**
 * Gestor del PIN parental configurable y protección frente a fuerza bruta.
 *
 * Características de seguridad:
 * - PIN numérico de 6 dígitos con hashing SHA-256 y sal criptográfica aleatoria de 16 bytes.
 * - Rechaza combinaciones triviales (000000, 111111, 123456, etc.).
 * - Nunca almacena el PIN en texto plano ni lo registra en logs.
 * - Bloqueo progresivo ante intentos repetidos persistido en SharedPreferences:
 *     * 1 a 4 intentos fallidos: advertencia con intentos restantes.
 *     * 5 intentos: bloqueo de 30 segundos.
 *     * 6 intentos: bloqueo de 60 segundos (1 minuto).
 *     * 7 intentos: bloqueo de 120 segundos (2 minutos).
 *     * 8 o más intentos: bloqueo de 300 segundos (5 minutos).
 * - El tiempo de bloqueo y contador de fallos se conservan al cerrar o reiniciar la app.
 */
class ParentalPinManager(
    private val preferences: SharedPreferences,
    private val currentTimeProvider: () -> Long = { System.currentTimeMillis() }
) {

    companion object {
        private const val PREFS_NAME = "pictohabla_parental_pin"
        private const val KEY_PIN_HASH = "parental_pin_hash"
        private const val KEY_PIN_SALT = "parental_pin_salt"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts_count"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until_timestamp"

        const val PIN_LENGTH = 6
        const val MAX_ATTEMPTS_BEFORE_LOCKOUT = 5

        @Volatile
        private var instance: ParentalPinManager? = null

        fun getInstance(context: Context): ParentalPinManager {
            return instance ?: synchronized(this) {
                instance ?: ParentalPinManager(
                    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ).also { instance = it }
            }
        }
    }

    /**
     * Comprueba si el PIN parental ya ha sido configurado.
     */
    fun isPinSet(): Boolean {
        val hash = preferences.getString(KEY_PIN_HASH, null)
        val salt = preferences.getString(KEY_PIN_SALT, null)
        return !hash.isNullOrBlank() && !salt.isNullOrBlank()
    }

    /**
     * Valida las restricciones del PIN parental (6 dígitos numéricos, no trivial).
     */
    fun validatePinFormat(pin: String): PinValidationResult {
        if (pin.length != PIN_LENGTH) {
            return PinValidationResult.Invalid("El PIN debe tener exactamente $PIN_LENGTH dígitos numéricos.")
        }
        if (!pin.all { it.isDigit() }) {
            return PinValidationResult.Invalid("El PIN solo puede contener dígitos numéricos (0-9).")
        }

        // Rechazo de combinaciones triviales
        val isAllSameDigit = pin.all { it == pin[0] }
        if (isAllSameDigit) {
            return PinValidationResult.Invalid("El PIN no puede estar formado por el mismo dígito repetido (como 000000).")
        }

        val trivialSequences = setOf(
            "123456", "654321", "012345", "543210", "234567", "765432",
            "345678", "876543", "456789", "987654"
        )
        if (pin in trivialSequences) {
            return PinValidationResult.Invalid("El PIN no puede ser una secuencia consecutiva obvia. Elige un código más seguro.")
        }

        return PinValidationResult.Valid
    }

    /**
     * Establece el PIN inicial tras la correspondiente autorización del dispositivo.
     */
    fun setInitialPin(pin: String): PinValidationResult {
        val validation = validatePinFormat(pin)
        if (validation !is PinValidationResult.Valid) {
            return validation
        }

        val saltBytes = ByteArray(16)
        SecureRandom().nextBytes(saltBytes)
        val saltHex = saltBytes.toHex()
        val hashHex = hashPin(pin, saltBytes)

        preferences.edit()
            .putString(KEY_PIN_HASH, hashHex)
            .putString(KEY_PIN_SALT, saltHex)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()

        return PinValidationResult.Valid
    }

    /**
     * Comprueba si el sistema se encuentra actualmente bloqueado por intentos fallidos.
     */
    fun isLockedOut(): Boolean {
        val lockoutUntil = preferences.getLong(KEY_LOCKOUT_UNTIL, 0L)
        return currentTimeProvider() < lockoutUntil
    }

    /**
     * Obtiene los segundos restantes de bloqueo activo.
     */
    fun getLockoutRemainingSeconds(): Long {
        val lockoutUntil = preferences.getLong(KEY_LOCKOUT_UNTIL, 0L)
        val remainingMillis = lockoutUntil - currentTimeProvider()
        return if (remainingMillis > 0) (remainingMillis + 999) / 1000 else 0L
    }

    /**
     * Obtiene el número acumulado de intentos fallidos.
     */
    fun getFailedAttempts(): Int {
        return preferences.getInt(KEY_FAILED_ATTEMPTS, 0)
    }

    /**
     * Verifica si el PIN introducido coincide con el configurado.
     * Gestiona el contador de fallos y el bloqueo progresivo.
     */
    fun verifyPin(pin: String): PinVerificationResult {
        if (isLockedOut()) {
            return PinVerificationResult.LockedOut(getLockoutRemainingSeconds())
        }

        val storedHash = preferences.getString(KEY_PIN_HASH, null)
        val storedSaltHex = preferences.getString(KEY_PIN_SALT, null)

        if (storedHash.isNullOrBlank() || storedSaltHex.isNullOrBlank()) {
            return PinVerificationResult.Incorrect(0)
        }

        val saltBytes = try {
            storedSaltHex.hexToByteArray()
        } catch (_: Exception) {
            return PinVerificationResult.Incorrect(0)
        }

        val inputHash = hashPin(pin, saltBytes)
        val isMatch = MessageDigest.isEqual(storedHash.toByteArray(), inputHash.toByteArray())

        if (isMatch) {
            // Éxito: restablecemos contadores
            resetAttempts()
            return PinVerificationResult.Success
        } else {
            // Fallo: incrementamos intentos y aplicamos tiempo de espera si procede
            val newAttempts = getFailedAttempts() + 1
            var lockoutDurationSeconds = 0L

            if (newAttempts >= MAX_ATTEMPTS_BEFORE_LOCKOUT) {
                lockoutDurationSeconds = when (newAttempts) {
                    5 -> 30L        // 30 segundos
                    6 -> 60L        // 1 minuto
                    7 -> 120L       // 2 minutos
                    else -> 300L    // 5 minutos para 8 o más
                }
            }

            val lockoutUntil = if (lockoutDurationSeconds > 0) {
                currentTimeProvider() + (lockoutDurationSeconds * 1000)
            } else 0L

            preferences.edit()
                .putInt(KEY_FAILED_ATTEMPTS, newAttempts)
                .putLong(KEY_LOCKOUT_UNTIL, lockoutUntil)
                .apply()

            return if (lockoutDurationSeconds > 0) {
                PinVerificationResult.LockedOut(lockoutDurationSeconds)
            } else {
                val remaining = MAX_ATTEMPTS_BEFORE_LOCKOUT - newAttempts
                PinVerificationResult.Incorrect(remaining.coerceAtLeast(1))
            }
        }
    }

    /**
     * Modifica el PIN parental tras comprobar el PIN actual.
     */
    fun changePin(currentPin: String, newPin: String, confirmNewPin: String): PinChangeResult {
        if (newPin != confirmNewPin) {
            return PinChangeResult.Error("El nuevo PIN y su confirmación no coinciden.")
        }

        val verifyCurrent = verifyPin(currentPin)
        if (verifyCurrent !is PinVerificationResult.Success) {
            return when (verifyCurrent) {
                is PinVerificationResult.LockedOut -> PinChangeResult.Error("Demasiados intentos fallidos. Espera ${verifyCurrent.remainingSeconds} segundos.")
                is PinVerificationResult.Incorrect -> PinChangeResult.Error("El PIN actual es incorrecto.")
                else -> PinChangeResult.Error("No se pudo verificar el PIN actual.")
            }
        }

        val validation = validatePinFormat(newPin)
        if (validation !is PinValidationResult.Valid) {
            return PinChangeResult.Error((validation as PinValidationResult.Invalid).message)
        }

        setInitialPin(newPin)
        return PinChangeResult.Success
    }

    /**
     * Restablece o reconfigura el PIN mediante autorización biométrica o de credenciales del dispositivo.
     */
    fun resetPinWithDeviceAuth(newPin: String? = null): PinValidationResult {
        resetAttempts()
        return if (!newPin.isNullOrBlank()) {
            setInitialPin(newPin)
        } else {
            // Si no se suministra nuevo PIN, se borra el PIN para solicitar configuración inicial limpia
            preferences.edit()
                .remove(KEY_PIN_HASH)
                .remove(KEY_PIN_SALT)
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, 0L)
                .apply()
            PinValidationResult.Valid
        }
    }

    /**
     * Reinicia el contador de intentos fallidos y el bloqueo.
     */
    fun resetAttempts() {
        preferences.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    /**
     * Calcula el hash SHA-256 con sal para almacenamiento seguro.
     */
    private fun hashPin(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val hashBytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return hashBytes.toHex()
    }
}

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

private fun String.hexToByteArray(): ByteArray {
    val result = ByteArray(length / 2)
    for (i in 0 until length step 2) {
        result[i / 2] = substring(i, i + 2).toInt(16).toByte()
    }
    return result
}
