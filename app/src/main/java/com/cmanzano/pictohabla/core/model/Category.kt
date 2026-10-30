package com.cmanzano.pictohabla.core.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.cmanzano.pictohabla.core.designsystem.PictoColors

/**
 * Categorías de comunicación aumentativa y alternativa (CAA).
 * Siguen el estándar de codificación visual basado en la Clave de Fitzgerald Modificada:
 * - Personas / Pronombres: Amarillo
 * - Acciones / Verbos: Verde
 * - Alimentos / Objetos: Naranja
 * - Sentimientos / Emociones: Azul / Rosa
 * - Lugares: Violeta
 * - Social / Modales: Rosa pastel
 * - Estructura / Preguntas: Gris neutro
 */
enum class Category(
    val labels: Map<String, String>,
    val color: Color,
    val borderColor: Color,
    val textColor: Color,
    val icon: ImageVector
) {
    PEOPLE(
        labels = mapOf(
            "ES" to "Personas",
            "EN" to "People",
            "FR" to "Personnes",
            "DE" to "Personen",
            "IT" to "Persone"
        ),
        color = PictoColors.Fitzgerald.PeopleBackground,
        borderColor = PictoColors.Fitzgerald.PeopleBorder,
        textColor = PictoColors.Fitzgerald.PeopleText,
        icon = Icons.Filled.Group
    ),
    ACTIONS(
        labels = mapOf(
            "ES" to "Acciones",
            "EN" to "Actions",
            "FR" to "Actions",
            "DE" to "Aktionen",
            "IT" to "Azioni"
        ),
        color = PictoColors.Fitzgerald.ActionsBackground,
        borderColor = PictoColors.Fitzgerald.ActionsBorder,
        textColor = PictoColors.Fitzgerald.ActionsText,
        icon = Icons.Filled.PlayArrow
    ),
    FOOD(
        labels = mapOf(
            "ES" to "Comida",
            "EN" to "Food",
            "FR" to "Nourriture",
            "DE" to "Essen",
            "IT" to "Cibo"
        ),
        color = PictoColors.Fitzgerald.FoodBackground,
        borderColor = PictoColors.Fitzgerald.FoodBorder,
        textColor = PictoColors.Fitzgerald.FoodText,
        icon = Icons.Filled.Restaurant
    ),
    FEELINGS(
        labels = mapOf(
            "ES" to "Sentimientos",
            "EN" to "Feelings",
            "FR" to "Sentiments",
            "DE" to "Gefühle",
            "IT" to "Sentimenti"
        ),
        color = PictoColors.Fitzgerald.FeelingsBackground,
        borderColor = PictoColors.Fitzgerald.FeelingsBorder,
        textColor = PictoColors.Fitzgerald.FeelingsText,
        icon = Icons.Filled.SentimentSatisfied
    ),
    PLACES(
        labels = mapOf(
            "ES" to "Lugares",
            "EN" to "Places",
            "FR" to "Lieux",
            "DE" to "Orte",
            "IT" to "Luoghi"
        ),
        color = PictoColors.Fitzgerald.PlacesBackground,
        borderColor = PictoColors.Fitzgerald.PlacesBorder,
        textColor = PictoColors.Fitzgerald.PlacesText,
        icon = Icons.Filled.Place
    ),
    THINGS(
        labels = mapOf(
            "ES" to "Cosas",
            "EN" to "Things",
            "FR" to "Choses",
            "DE" to "Dinge",
            "IT" to "Cose"
        ),
        color = PictoColors.Fitzgerald.ThingsBackground,
        borderColor = PictoColors.Fitzgerald.ThingsBorder,
        textColor = PictoColors.Fitzgerald.ThingsText,
        icon = Icons.Filled.Category
    ),
    SOCIAL(
        labels = mapOf(
            "ES" to "Social",
            "EN" to "Social",
            "FR" to "Social",
            "DE" to "Soziales",
            "IT" to "Sociale"
        ),
        color = PictoColors.Fitzgerald.SocialBackground,
        borderColor = PictoColors.Fitzgerald.SocialBorder,
        textColor = PictoColors.Fitzgerald.SocialText,
        icon = Icons.AutoMirrored.Filled.Chat
    ),
    BODY(
        labels = mapOf(
            "ES" to "Cuerpo",
            "EN" to "Body",
            "FR" to "Corps",
            "DE" to "Körper",
            "IT" to "Corpo"
        ),
        color = PictoColors.Fitzgerald.BodyBackground,
        borderColor = PictoColors.Fitzgerald.BodyBorder,
        textColor = PictoColors.Fitzgerald.BodyText,
        icon = Icons.Filled.Accessibility
    ),
    NUMBERS(
        labels = mapOf(
            "ES" to "Números",
            "EN" to "Numbers",
            "FR" to "Nombres",
            "DE" to "Zahlen",
            "IT" to "Numeri"
        ),
        color = PictoColors.Fitzgerald.NumbersBackground,
        borderColor = PictoColors.Fitzgerald.NumbersBorder,
        textColor = PictoColors.Fitzgerald.NumbersText,
        icon = Icons.Filled.Numbers
    ),
    WEATHER(
        labels = mapOf(
            "ES" to "Clima",
            "EN" to "Weather",
            "FR" to "Météo",
            "DE" to "Wetter",
            "IT" to "Meteo"
        ),
        color = PictoColors.Fitzgerald.WeatherBackground,
        borderColor = PictoColors.Fitzgerald.WeatherBorder,
        textColor = PictoColors.Fitzgerald.WeatherText,
        icon = Icons.Filled.WbSunny
    ),
    QUESTIONS(
        labels = mapOf(
            "ES" to "Preguntas",
            "EN" to "Questions",
            "FR" to "Questions",
            "DE" to "Fragen",
            "IT" to "Domande"
        ),
        color = PictoColors.Fitzgerald.QuestionsBackground,
        borderColor = PictoColors.Fitzgerald.QuestionsBorder,
        textColor = PictoColors.Fitzgerald.QuestionsText,
        icon = Icons.AutoMirrored.Filled.Help
    );

    fun getLabel(languageCode: String): String {
        return labels[languageCode] ?: labels["ES"] ?: name
    }
}
