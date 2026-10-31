package com.cmanzano.pictohabla.feature.parental

import com.cmanzano.pictohabla.testutil.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para el sistema de PIN parental configurable de PictoHabla.
 * Verifica validación de formato, rechazo de combinaciones triviales, hashing seguro con sal,
 * protección frente a fuerza bruta con tiempos de espera progresivos, persistencia del bloqueo,
 * cambio de PIN y recuperación mediante credenciales del dispositivo.
 */
class ParentalPinManagerTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private var simulatedTime: Long = 1000000L
    private lateinit var pinManager: ParentalPinManager

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        simulatedTime = 1000000L
        pinManager = ParentalPinManager(fakePrefs) { simulatedTime }
    }

    @Test
    fun `isPinSet returns false initially`() {
        assertFalse("El PIN no debe estar configurado inicialmente", pinManager.isPinSet())
    }

    @Test
    fun `validatePinFormat accepts valid 6-digit non-trivial PIN`() {
        val validPin = "482915"
        val result = pinManager.validatePinFormat(validPin)
        assertTrue("Debe aceptar un PIN válido de 6 dígitos no trivial", result is PinValidationResult.Valid)
    }

    @Test
    fun `validatePinFormat rejects PIN with incorrect length`() {
        val shortPin = "1234"
        val longPin = "1234567"

        val shortResult = pinManager.validatePinFormat(shortPin)
        val longResult = pinManager.validatePinFormat(longPin)

        assertTrue(shortResult is PinValidationResult.Invalid)
        assertTrue(longResult is PinValidationResult.Invalid)
        assertTrue((shortResult as PinValidationResult.Invalid).message.contains("exactamente 6 dígitos"))
    }

    @Test
    fun `validatePinFormat rejects non-numeric characters`() {
        val nonNumeric = "12a456"
        val result = pinManager.validatePinFormat(nonNumeric)
        assertTrue(result is PinValidationResult.Invalid)
        assertTrue((result as PinValidationResult.Invalid).message.contains("dígitos numéricos"))
    }

    @Test
    fun `validatePinFormat rejects trivial repetitive PINs like 000000 or 111111`() {
        val allZeros = "000000"
        val allOnes = "111111"
        val allNines = "999999"

        assertTrue(pinManager.validatePinFormat(allZeros) is PinValidationResult.Invalid)
        assertTrue(pinManager.validatePinFormat(allOnes) is PinValidationResult.Invalid)
        assertTrue(pinManager.validatePinFormat(allNines) is PinValidationResult.Invalid)
    }

    @Test
    fun `validatePinFormat rejects trivial sequential PINs like 123456 or 654321`() {
        val sequentialAsc = "123456"
        val sequentialDesc = "654321"

        assertTrue(pinManager.validatePinFormat(sequentialAsc) is PinValidationResult.Invalid)
        assertTrue(pinManager.validatePinFormat(sequentialDesc) is PinValidationResult.Invalid)
    }

    @Test
    fun `setInitialPin securely hashes PIN and does not store plaintext`() {
        val secretPin = "749201"
        val result = pinManager.setInitialPin(secretPin)

        assertTrue(result is PinValidationResult.Valid)
        assertTrue(pinManager.isPinSet())

        // Aseguramos que el PIN no está almacenado en texto plano en las preferencias
        val allStoredValues = fakePrefs.all.values.map { it.toString() }
        assertFalse("El PIN en texto plano nunca debe almacenarse", allStoredValues.contains(secretPin))

        val hash = fakePrefs.getString("parental_pin_hash", null)
        val salt = fakePrefs.getString("parental_pin_salt", null)
        assertFalse(hash.isNullOrBlank())
        assertFalse(salt.isNullOrBlank())
        assertNotEquals(secretPin, hash)
    }

    @Test
    fun `verifyPin succeeds with correct PIN and resets failed attempts`() {
        val pin = "391842"
        pinManager.setInitialPin(pin)

        val verification = pinManager.verifyPin(pin)
        assertTrue("La verificación debe tener éxito con el PIN correcto", verification is PinVerificationResult.Success)
        assertEquals(0, pinManager.getFailedAttempts())
        assertFalse(pinManager.isLockedOut())
    }

    @Test
    fun `verifyPin decrements remaining attempts on incorrect PIN`() {
        val correctPin = "830192"
        val wrongPin = "112233"
        pinManager.setInitialPin(correctPin)

        val result1 = pinManager.verifyPin(wrongPin)
        assertTrue(result1 is PinVerificationResult.Incorrect)
        assertEquals(4, (result1 as PinVerificationResult.Incorrect).remainingAttempts)
        assertEquals(1, pinManager.getFailedAttempts())

        val result2 = pinManager.verifyPin(wrongPin)
        assertTrue(result2 is PinVerificationResult.Incorrect)
        assertEquals(3, (result2 as PinVerificationResult.Incorrect).remainingAttempts)
        assertEquals(2, pinManager.getFailedAttempts())
    }

    @Test
    fun `progressive lockout triggers after 5 failed attempts`() {
        val correctPin = "940281"
        val wrongPin = "554433"
        pinManager.setInitialPin(correctPin)

        // 4 intentos fallidos
        repeat(4) {
            val res = pinManager.verifyPin(wrongPin)
            assertTrue(res is PinVerificationResult.Incorrect)
        }
        assertFalse("No debe estar bloqueado tras 4 intentos", pinManager.isLockedOut())

        // 5º intento fallido -> Bloqueo de 30 segundos
        val fifthResult = pinManager.verifyPin(wrongPin)
        assertTrue("El 5º fallo debe activar el bloqueo", fifthResult is PinVerificationResult.LockedOut)
        assertEquals(30L, (fifthResult as PinVerificationResult.LockedOut).remainingSeconds)
        assertTrue(pinManager.isLockedOut())
        assertEquals(30L, pinManager.getLockoutRemainingSeconds())
    }

    @Test
    fun `lockout expires when time passes and allows retry`() {
        val correctPin = "940281"
        val wrongPin = "554433"
        pinManager.setInitialPin(correctPin)

        repeat(5) { pinManager.verifyPin(wrongPin) }
        assertTrue(pinManager.isLockedOut())

        // Avanzamos 15 segundos: sigue bloqueado
        simulatedTime += 15000L
        assertTrue(pinManager.isLockedOut())
        assertEquals(15L, pinManager.getLockoutRemainingSeconds())

        // Avanzamos 16 segundos adicionales (total 31 segundos): bloqueo expirado
        simulatedTime += 16000L
        assertFalse(pinManager.isLockedOut())
        assertEquals(0L, pinManager.getLockoutRemainingSeconds())

        // Ahora el PIN correcto puede ser verificado
        val success = pinManager.verifyPin(correctPin)
        assertTrue(success is PinVerificationResult.Success)
        assertEquals(0, pinManager.getFailedAttempts())
    }

    @Test
    fun `lockout escalates duration on consecutive failures`() {
        val correctPin = "772211"
        val wrongPin = "889900"
        pinManager.setInitialPin(correctPin)

        // 5 fallos: 30 segundos
        repeat(5) { pinManager.verifyPin(wrongPin) }
        assertEquals(30L, pinManager.getLockoutRemainingSeconds())

        // Expirar y fallar 6º: 60 segundos
        simulatedTime += 31000L
        val sixthResult = pinManager.verifyPin(wrongPin)
        assertTrue(sixthResult is PinVerificationResult.LockedOut)
        assertEquals(60L, (sixthResult as PinVerificationResult.LockedOut).remainingSeconds)

        // Expirar y fallar 7º: 120 segundos
        simulatedTime += 61000L
        val seventhResult = pinManager.verifyPin(wrongPin)
        assertTrue(seventhResult is PinVerificationResult.LockedOut)
        assertEquals(120L, (seventhResult as PinVerificationResult.LockedOut).remainingSeconds)

        // Expirar y fallar 8º: 300 segundos (5 minutos)
        simulatedTime += 121000L
        val eighthResult = pinManager.verifyPin(wrongPin)
        assertTrue(eighthResult is PinVerificationResult.LockedOut)
        assertEquals(300L, (eighthResult as PinVerificationResult.LockedOut).remainingSeconds)
    }

    @Test
    fun `lockout persists across application restarts`() {
        val correctPin = "159357"
        val wrongPin = "482619"
        pinManager.setInitialPin(correctPin)

        repeat(5) { pinManager.verifyPin(wrongPin) }
        assertTrue(pinManager.isLockedOut())

        // Simulamos cerrar y reabrir la app creando una nueva instancia del manager con las mismas preferencias
        val restartedPinManager = ParentalPinManager(fakePrefs) { simulatedTime }
        assertTrue("El bloqueo debe persistir al reiniciar la app", restartedPinManager.isLockedOut())
        assertEquals(30L, restartedPinManager.getLockoutRemainingSeconds())
        assertEquals(5, restartedPinManager.getFailedAttempts())
    }

    @Test
    fun `changePin successfully changes PIN with valid credentials`() {
        val oldPin = "629401"
        val newPin = "815372"
        pinManager.setInitialPin(oldPin)

        val changeResult = pinManager.changePin(oldPin, newPin, newPin)
        assertTrue("El cambio de PIN debe ser exitoso", changeResult is PinChangeResult.Success)

        // El PIN antiguo ya no funciona
        val oldVerify = pinManager.verifyPin(oldPin)
        assertTrue(oldVerify is PinVerificationResult.Incorrect)

        // El nuevo PIN funciona
        val newVerify = pinManager.verifyPin(newPin)
        assertTrue(newVerify is PinVerificationResult.Success)
    }

    @Test
    fun `changePin fails when confirmation does not match or current PIN is wrong`() {
        val oldPin = "629401"
        pinManager.setInitialPin(oldPin)

        val mismatch = pinManager.changePin(oldPin, "815372", "815373")
        assertTrue(mismatch is PinChangeResult.Error)
        assertTrue((mismatch as PinChangeResult.Error).message.contains("no coinciden"))

        val wrongCurrent = pinManager.changePin("000000", "815372", "815372")
        assertTrue(wrongCurrent is PinChangeResult.Error)
        assertTrue((wrongCurrent as PinChangeResult.Error).message.contains("incorrecto"))
    }

    @Test
    fun `resetPinWithDeviceAuth clears lockout and sets new PIN or resets to initial state`() {
        val oldPin = "381942"
        val wrongPin = "998877"
        pinManager.setInitialPin(oldPin)

        // Bloquear el sistema
        repeat(5) { pinManager.verifyPin(wrongPin) }
        assertTrue(pinManager.isLockedOut())

        // Recuperar y definir nuevo PIN tras autenticación del dispositivo
        val newPin = "502819"
        val resetResult = pinManager.resetPinWithDeviceAuth(newPin)
        assertTrue(resetResult is PinValidationResult.Valid)
        assertFalse("El bloqueo debe cancelarse tras la autenticación de dispositivo", pinManager.isLockedOut())
        assertEquals(0, pinManager.getFailedAttempts())

        // El nuevo PIN funciona inmediatamente
        assertTrue(pinManager.verifyPin(newPin) is PinVerificationResult.Success)
    }
}
