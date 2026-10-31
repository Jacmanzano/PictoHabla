package com.cmanzano.pictohabla.feature.board

import com.cmanzano.pictohabla.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Pruebas unitarias para validar:
 * 1. La presencia del recurso oficial del logo de PictoHabla (ic_pictohabla_symbol).
 * 2. La reorganización de «Vocabulario Esencial»: retirado de la barra superior y reubicado en Ajustes.
 * 3. La protección parental de las secciones críticas.
 */
class NavigationAndVisualIdentityTest {

    @Test
    fun `official visual identity resources exist in project`() {
        val symbolFile = File("src/main/res/drawable/ic_pictohabla_symbol.png")
        val logoFile = File("src/main/res/drawable/ic_pictohabla_logo.png")

        assertTrue("El recurso ic_pictohabla_symbol debe existir", symbolFile.exists() || File("app/src/main/res/drawable/ic_pictohabla_symbol.png").exists())
        assertTrue("El recurso ic_pictohabla_logo debe existir", logoFile.exists() || File("app/src/main/res/drawable/ic_pictohabla_logo.png").exists())
    }

    @Test
    fun `AACBoardScreen top bar does not contain legacy AssistChip for active board and includes official symbol`() {
        val boardFile = File("app/src/main/java/com/cmanzano/pictohabla/feature/board/AACBoardScreen.kt")
        val content = if (boardFile.exists()) boardFile.readText() else File("src/main/java/com/cmanzano/pictohabla/feature/board/AACBoardScreen.kt").readText()

        // Verificar que ic_pictohabla_symbol está presente en la barra superior
        assertTrue("La barra superior debe incluir el símbolo oficial de PictoHabla", content.contains("ic_pictohabla_symbol"))
        assertTrue("Debe tener contentDescription accesible para el logo", content.contains("Logotipo de PictoHabla"))

        // Verificar que el chip de vocabulario esencial ha sido retirado de la barra superior
        assertFalse("El AssistChip del tablero activo no debe estar en la cabecera", content.contains("AssistChip(\n                                    onClick = {\n                                        PictoHaptics.performClick(hapticFeedback, hapticsEnabled)\n                                        pendingAdultNavigation = onNavigateBoards"))
    }

    @Test
    fun `SettingsScreen contains dedicated section for Vocabulario Esencial and Parental Control`() {
        val settingsFile = File("app/src/main/java/com/cmanzano/pictohabla/feature/settings/SettingsScreen.kt")
        val content = if (settingsFile.exists()) settingsFile.readText() else File("src/main/java/com/cmanzano/pictohabla/feature/settings/SettingsScreen.kt").readText()

        assertTrue("SettingsScreen debe contener la sección Vocabulario Esencial y Tableros", content.contains("Vocabulario Esencial y Tableros"))
        assertTrue("SettingsScreen debe contener el botón para abrir Vocabulario Esencial", content.contains("Abrir Vocabulario Esencial y Tableros"))
        assertTrue("SettingsScreen debe contener la sección Control Parental", content.contains("Control Parental"))
        assertTrue("SettingsScreen debe contener la gestión de PIN de 6 dígitos", content.contains("PIN de 6 dígitos para Adultos"))
    }
}
