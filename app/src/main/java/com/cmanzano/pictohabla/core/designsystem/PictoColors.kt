package com.cmanzano.pictohabla.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Paleta de colores centralizada para PictoHabla.
 * Diseñada para transmitir serenidad, confianza, inclusión y máxima legibilidad
 * evitando saturación sensorial o elementos distractores.
 */
object PictoColors {
    // Colores principales de la marca (Verde azulado / Teal inclusivo y calmante)
    val Primary = Color(0xFF00796B)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFE0F2F1)
    val OnPrimaryContainer = Color(0xFF004D40)

    val Secondary = Color(0xFF455A64)
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFECEFF1)
    val OnSecondaryContainer = Color(0xFF263238)

    // Superficies y fondos
    val BackgroundLight = Color(0xFFF8F9FA) // Blanco cálido anti-reflejos
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceVariantLight = Color(0xFFF1F5F9)
    val OnBackgroundLight = Color(0xFF1E293B)
    val OnSurfaceLight = Color(0xFF1E293B)
    val OnSurfaceVariantLight = Color(0xFF475569)

    // Modo Alto Contraste (Fondo oscuro de alta definición)
    val BackgroundHighContrast = Color(0xFF000000)
    val SurfaceHighContrast = Color(0xFF121212)
    val SurfaceVariantHighContrast = Color(0xFF1E1E1E)
    val OnBackgroundHighContrast = Color(0xFFFFFFFF)
    val OnSurfaceHighContrast = Color(0xFFFFFFFF)
    val BorderHighContrast = Color(0xFFFFFFFF)
    val HighlightHighContrast = Color(0xFFFFD600)

    // Alertas y estados
    val Error = Color(0xFFD32F2F)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFFFEBEE)
    val OnErrorContainer = Color(0xFFB71C1C)

    val Success = Color(0xFF2E7D32)
    val OnSuccess = Color(0xFFFFFFFF)

    // Clave de Fitzgerald Modificada (Colores clínicos estandarizados de CAA)
    object Fitzgerald {
        // Personas / Pronombres: Amarillo
        val PeopleBackground = Color(0xFFFFF9C4)
        val PeopleBorder = Color(0xFFF57F17)
        val PeopleText = Color(0xFF212121)

        // Acciones / Verbos: Verde
        val ActionsBackground = Color(0xFFC8E6C9)
        val ActionsBorder = Color(0xFF2E7D32)
        val ActionsText = Color(0xFF1B5E20)

        // Alimentos y Comida: Naranja cálido
        val FoodBackground = Color(0xFFFFE0B2)
        val FoodBorder = Color(0xFFE65100)
        val FoodText = Color(0xFFBF360C)

        // Sentimientos / Estados: Azul
        val FeelingsBackground = Color(0xFFBBDEFB)
        val FeelingsBorder = Color(0xFF1565C0)
        val FeelingsText = Color(0xFF0D47A1)

        // Lugares: Violeta
        val PlacesBackground = Color(0xFFD1C4E9)
        val PlacesBorder = Color(0xFF6A1B9A)
        val PlacesText = Color(0xFF4A148C)

        // Cosas / Objetos: Naranja claro / Terracota
        val ThingsBackground = Color(0xFFFFCCBC)
        val ThingsBorder = Color(0xFFD84315)
        val ThingsText = Color(0xFF3E2723)

        // Social / Cortesía: Rosa
        val SocialBackground = Color(0xFFF8BBD0)
        val SocialBorder = Color(0xFFC2185B)
        val SocialText = Color(0xFF880E4F)

        // Cuerpo / Salud: Rojo coral suave
        val BodyBackground = Color(0xFFFFCDD2)
        val BodyBorder = Color(0xFFC62828)
        val BodyText = Color(0xFFB71C1C)

        // Números: Gris azulado
        val NumbersBackground = Color(0xFFCFD8DC)
        val NumbersBorder = Color(0xFF37474F)
        val NumbersText = Color(0xFF263238)

        // Clima: Cyan
        val WeatherBackground = Color(0xFFB2EBF2)
        val WeatherBorder = Color(0xFF00838F)
        val WeatherText = Color(0xFF006064)

        // Preguntas / Estructura: Gris neutro
        val QuestionsBackground = Color(0xFFEEEEEE)
        val QuestionsBorder = Color(0xFF424242)
        val QuestionsText = Color(0xFF212121)
    }
}
