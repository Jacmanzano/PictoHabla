package com.cmanzano.pictohabla.data.local

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.LiveHelp
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.cmanzano.pictohabla.core.model.Category
import com.cmanzano.pictohabla.core.model.GuidedPhrase
import com.cmanzano.pictohabla.core.model.Pictogram

/**
 * Catálogo de inicio integrado con vocabulario esencial de comunicación (AAC Core Vocabulary)
 * respaldado por pictogramas oficiales de ARASAAC con imágenes locales para funcionamiento 100% offline.
 */
object DefaultCatalog {

    /**
     * Función constructora auxiliar para pictogramas estándar del catálogo base con ID oficial ARASAAC.
     */
    private fun createPicto(
        id: Int,
        names: Map<String, String>,
        icon: ImageVector,
        category: Category,
        arasaacId: Int
    ): Pictogram {
        return Pictogram(
            id = id,
            names = names,
            category = category,
            icon = icon,
            arasaacId = arasaacId
        )
    }

    /**
     * Función constructora auxiliar para pictogramas numéricos (0..9) con ID oficial ARASAAC.
     */
    private fun createNumberPicto(
        id: Int,
        names: Map<String, String>,
        icon: ImageVector,
        digit: Int,
        arasaacId: Int
    ): Pictogram {
        return Pictogram(
            id = id,
            names = names,
            category = Category.NUMBERS,
            icon = icon,
            isNumber = true,
            digit = digit,
            arasaacId = arasaacId
        )
    }

    fun getNumberIcon(i: Int): ImageVector = when (i) {
        1 -> Icons.Default.Filter1
        2 -> Icons.Default.Filter2
        3 -> Icons.Default.Filter3
        4 -> Icons.Default.Filter4
        5 -> Icons.Default.Filter5
        6 -> Icons.Default.Filter6
        7 -> Icons.Default.Filter7
        8 -> Icons.Default.Filter8
        9 -> Icons.Default.Filter9
        else -> Icons.Default.ExposureZero
    }

    fun getFullCatalog(): List<Pictogram> {
        val list = mutableListOf<Pictogram>()

        // 0..9 NÚMEROS (NUMBERS) - ARASAAC IDs 2626..2635
        val numNames = listOf(
            mapOf("ES" to "cero", "EN" to "zero", "FR" to "zéro", "DE" to "null", "IT" to "zero"),
            mapOf("ES" to "uno", "EN" to "one", "FR" to "un", "DE" to "eins", "IT" to "uno"),
            mapOf("ES" to "dos", "EN" to "two", "FR" to "deux", "DE" to "zwei", "IT" to "due"),
            mapOf("ES" to "tres", "EN" to "three", "FR" to "trois", "DE" to "drei", "IT" to "tre"),
            mapOf("ES" to "cuatro", "EN" to "four", "FR" to "quatre", "DE" to "vier", "IT" to "quattro"),
            mapOf("ES" to "cinco", "EN" to "five", "FR" to "cinq", "DE" to "fünf", "IT" to "cinque"),
            mapOf("ES" to "seis", "EN" to "six", "FR" to "six", "DE" to "sechs", "IT" to "sei"),
            mapOf("ES" to "siete", "EN" to "seven", "FR" to "sept", "DE" to "sieben", "IT" to "sette"),
            mapOf("ES" to "ocho", "EN" to "eight", "FR" to "huit", "DE" to "acht", "IT" to "otto"),
            mapOf("ES" to "nueve", "EN" to "nine", "FR" to "neuf", "DE" to "neun", "IT" to "nove")
        )
        val numArasaacIds = listOf(2626, 2627, 2628, 2629, 2630, 2631, 2632, 2633, 2634, 2635)
        for (i in 0..9) {
            list.add(
                createNumberPicto(
                    id = 1000 + i,
                    names = numNames[i],
                    icon = getNumberIcon(i),
                    digit = i,
                    arasaacId = numArasaacIds[i]
                )
            )
        }

        // --- VOCABULARIO ESENCIAL Y SOCIAL (SOCIAL / QUESTIONS / ACTIONS / FOOD / FEELINGS) ---
        list.add(createPicto(1, mapOf("ES" to "Hola", "EN" to "Hello", "FR" to "Bonjour", "DE" to "Hallo", "IT" to "Ciao"), Icons.Default.WavingHand, Category.SOCIAL, 6009))
        list.add(createPicto(2, mapOf("ES" to "Gracias", "EN" to "Thanks", "FR" to "Merci", "DE" to "Danke", "IT" to "Grazie"), Icons.Default.Favorite, Category.SOCIAL, 8129))
        list.add(createPicto(3, mapOf("ES" to "Por favor", "EN" to "Please", "FR" to "S'il vous plaît", "DE" to "Bitte", "IT" to "Per favore"), Icons.Default.ThumbUp, Category.SOCIAL, 8195))
        list.add(createPicto(4, mapOf("ES" to "Adiós", "EN" to "Bye", "FR" to "Au revoir", "DE" to "Tschüss", "IT" to "Ciao"), Icons.AutoMirrored.Filled.Logout, Category.SOCIAL, 6028))
        list.add(createPicto(5, mapOf("ES" to "Sí", "EN" to "Yes", "FR" to "Oui", "DE" to "Ja", "IT" to "Sì"), Icons.Default.CheckCircle, Category.QUESTIONS, 5584))
        list.add(createPicto(6, mapOf("ES" to "No", "EN" to "No", "FR" to "Non", "DE" to "Nein", "IT" to "No"), Icons.Default.Cancel, Category.QUESTIONS, 5526))
        list.add(createPicto(7, mapOf("ES" to "Perdón", "EN" to "Sorry"), Icons.Default.SentimentDissatisfied, Category.SOCIAL, 11625))
        list.add(createPicto(8, mapOf("ES" to "Tengo hambre", "EN" to "I am hungry"), Icons.Default.Restaurant, Category.FOOD, 7272))
        list.add(createPicto(9, mapOf("ES" to "Tengo sed", "EN" to "I am thirsty"), Icons.Default.LocalDrink, Category.FOOD, 7273))
        list.add(createPicto(10, mapOf("ES" to "Ayuda", "EN" to "Help"), Icons.AutoMirrored.Filled.LiveHelp, Category.ACTIONS, 12252))
        list.add(createPicto(11, mapOf("ES" to "Más", "EN" to "More"), Icons.Default.AddCircle, Category.ACTIONS, 3220))
        list.add(createPicto(12, mapOf("ES" to "Para", "EN" to "Stop"), Icons.Default.StopCircle, Category.ACTIONS, 7196))
        list.add(createPicto(13, mapOf("ES" to "Otra vez", "EN" to "Again"), Icons.Default.Replay, Category.ACTIONS, 37162))
        list.add(createPicto(14, mapOf("ES" to "Terminado", "EN" to "Finished"), Icons.Default.DoneAll, Category.ACTIONS, 5358))
        list.add(createPicto(15, mapOf("ES" to "No sé", "EN" to "I don't know"), Icons.Default.QuestionMark, Category.QUESTIONS, 11696))
        list.add(createPicto(16, mapOf("ES" to "Me gusta", "EN" to "I like it"), Icons.Default.ThumbUp, Category.FEELINGS, 37826))
        list.add(createPicto(17, mapOf("ES" to "No me gusta", "EN" to "I dislike it"), Icons.Default.ThumbDown, Category.FEELINGS, 37825))

        // --- PERSONAS Y REFERENCIAS (PEOPLE) ---
        list.add(createPicto(20, mapOf("ES" to "Yo", "EN" to "I"), Icons.Default.Person, Category.PEOPLE, 2617))
        list.add(createPicto(21, mapOf("ES" to "Tú", "EN" to "You"), Icons.Default.PersonOutline, Category.PEOPLE, 2608))
        list.add(createPicto(22, mapOf("ES" to "Mamá", "EN" to "Mom"), Icons.Default.Face, Category.PEOPLE, 2458))
        list.add(createPicto(23, mapOf("ES" to "Papá", "EN" to "Dad"), Icons.Default.Face, Category.PEOPLE, 31146))
        list.add(createPicto(24, mapOf("ES" to "Abuelo", "EN" to "Grandpa"), Icons.Default.Elderly, Category.PEOPLE, 2244))
        list.add(createPicto(25, mapOf("ES" to "Abuela", "EN" to "Grandma"), Icons.Default.ElderlyWoman, Category.PEOPLE, 2243))
        list.add(createPicto(26, mapOf("ES" to "Amigo", "EN" to "Friend"), Icons.Default.Group, Category.PEOPLE, 8487))
        list.add(createPicto(27, mapOf("ES" to "Profesor", "EN" to "Teacher"), Icons.Default.School, Category.PEOPLE, 6556))
        list.add(createPicto(28, mapOf("ES" to "Él", "EN" to "He"), Icons.Default.Boy, Category.PEOPLE, 6480))
        list.add(createPicto(29, mapOf("ES" to "Ella", "EN" to "She"), Icons.Default.Girl, Category.PEOPLE, 7029))
        list.add(createPicto(200, mapOf("ES" to "Nosotros", "EN" to "We"), Icons.Default.Groups, Category.PEOPLE, 7185))

        // --- ACCIONES Y VERBOS (ACTIONS) ---
        list.add(createPicto(30, mapOf("ES" to "Quiero", "EN" to "Want"), Icons.Default.Favorite, Category.ACTIONS, 5441))
        list.add(createPicto(39, mapOf("ES" to "Necesito", "EN" to "Need"), Icons.Default.PriorityHigh, Category.ACTIONS, 37160))
        list.add(createPicto(31, mapOf("ES" to "Comer", "EN" to "Eat"), Icons.Default.Restaurant, Category.ACTIONS, 2349))
        list.add(createPicto(32, mapOf("ES" to "Beber", "EN" to "Drink"), Icons.Default.LocalDrink, Category.ACTIONS, 2276))
        list.add(createPicto(33, mapOf("ES" to "Jugar", "EN" to "Play"), Icons.Default.SportsEsports, Category.ACTIONS, 2439))
        list.add(createPicto(34, mapOf("ES" to "Dormir", "EN" to "Sleep"), Icons.Default.Bed, Category.ACTIONS, 2369))
        list.add(createPicto(35, mapOf("ES" to "Ir", "EN" to "Go"), Icons.AutoMirrored.Filled.DirectionsWalk, Category.ACTIONS, 2432))
        list.add(createPicto(18, mapOf("ES" to "Venir", "EN" to "Come"), Icons.AutoMirrored.Filled.CallReceived, Category.ACTIONS, 11708))
        list.add(createPicto(19, mapOf("ES" to "Hacer", "EN" to "Do"), Icons.Default.Build, Category.ACTIONS, 11749))
        list.add(createPicto(36, mapOf("ES" to "Ver", "EN" to "Watch"), Icons.Default.Visibility, Category.ACTIONS, 2474))
        list.add(createPicto(37, mapOf("ES" to "Escuchar", "EN" to "Listen"), Icons.Default.Hearing, Category.ACTIONS, 2381))
        list.add(createPicto(38, mapOf("ES" to "Correr", "EN" to "Run"), Icons.AutoMirrored.Filled.DirectionsRun, Category.ACTIONS, 2719))

        // --- COMIDA Y BEBIDA (FOOD) ---
        list.add(createPicto(40, mapOf("ES" to "Manzana", "EN" to "Apple"), Icons.Default.Restaurant, Category.FOOD, 2462))
        list.add(createPicto(41, mapOf("ES" to "Plátano", "EN" to "Banana"), Icons.Default.LunchDining, Category.FOOD, 2530))
        list.add(createPicto(42, mapOf("ES" to "Pan", "EN" to "Bread"), Icons.Default.BakeryDining, Category.FOOD, 2494))
        list.add(createPicto(43, mapOf("ES" to "Leche", "EN" to "Milk"), Icons.Default.Coffee, Category.FOOD, 2445))
        list.add(createPicto(44, mapOf("ES" to "Agua", "EN" to "Water"), Icons.Default.LocalDrink, Category.FOOD, 2248))
        list.add(createPicto(45, mapOf("ES" to "Pizza", "EN" to "Pizza"), Icons.Default.LocalPizza, Category.FOOD, 2527))

        // --- SENTIMIENTOS Y BIENESTAR (FEELINGS) ---
        list.add(createPicto(50, mapOf("ES" to "Feliz", "EN" to "Happy"), Icons.Default.SentimentVerySatisfied, Category.FEELINGS, 9907))
        list.add(createPicto(51, mapOf("ES" to "Triste", "EN" to "Sad"), Icons.Default.SentimentVeryDissatisfied, Category.FEELINGS, 2606))
        list.add(createPicto(52, mapOf("ES" to "Enfadado", "EN" to "Angry"), Icons.Default.MoodBad, Category.FEELINGS, 2374))
        list.add(createPicto(53, mapOf("ES" to "Cansado", "EN" to "Tired"), Icons.Default.BatteryAlert, Category.FEELINGS, 2314))
        list.add(createPicto(54, mapOf("ES" to "Duele", "EN" to "Hurt"), Icons.Default.Healing, Category.FEELINGS, 30620))
        list.add(createPicto(55, mapOf("ES" to "Tengo frío", "EN" to "I am cold"), Icons.Default.AcUnit, Category.FEELINGS, 35557))
        list.add(createPicto(56, mapOf("ES" to "Tengo calor", "EN" to "I am hot"), Icons.Default.WbSunny, Category.FEELINGS, 6952))

        // --- LUGARES (PLACES) ---
        list.add(createPicto(60, mapOf("ES" to "Casa", "EN" to "Home"), Icons.Default.Home, Category.PLACES, 2317))
        list.add(createPicto(61, mapOf("ES" to "Baño", "EN" to "Bathroom"), Icons.Default.Wc, Category.PLACES, 6929))
        list.add(createPicto(62, mapOf("ES" to "Colegio", "EN" to "School"), Icons.Default.School, Category.PLACES, 3082))
        list.add(createPicto(63, mapOf("ES" to "Parque", "EN" to "Park"), Icons.Default.Park, Category.PLACES, 2859))
        list.add(createPicto(64, mapOf("ES" to "Quiero ir al baño", "EN" to "I want to go to bathroom"), Icons.Default.Wc, Category.PLACES, 16713))

        // --- COSAS Y OBJETOS (THINGS) ---
        list.add(createPicto(70, mapOf("ES" to "Pelota", "EN" to "Ball"), Icons.Default.SportsBasketball, Category.THINGS, 3241))
        list.add(createPicto(71, mapOf("ES" to "Libro", "EN" to "Book"), Icons.AutoMirrored.Filled.MenuBook, Category.THINGS, 2450))
        list.add(createPicto(72, mapOf("ES" to "Coche", "EN" to "Car"), Icons.Default.DirectionsCar, Category.THINGS, 2339))
        list.add(createPicto(73, mapOf("ES" to "Mochila", "EN" to "Backpack"), Icons.Default.Backpack, Category.THINGS, 2475))

        // --- CUERPO (BODY) ---
        list.add(createPicto(80, mapOf("ES" to "Cabeza", "EN" to "Head"), Icons.Default.Face, Category.BODY, 2673))
        list.add(createPicto(81, mapOf("ES" to "Mano", "EN" to "Hand"), Icons.Default.BackHand, Category.BODY, 2928))
        list.add(createPicto(82, mapOf("ES" to "Pie", "EN" to "Foot"), Icons.Default.Accessibility, Category.BODY, 2841))

        // --- CLIMA (WEATHER) ---
        list.add(createPicto(90, mapOf("ES" to "Sol", "EN" to "Sun"), Icons.Default.WbSunny, Category.WEATHER, 2798))
        list.add(createPicto(91, mapOf("ES" to "Lluvia", "EN" to "Rain"), Icons.Default.Umbrella, Category.WEATHER, 3123))

        // --- PREGUNTAS (QUESTIONS) ---
        list.add(createPicto(100, mapOf("ES" to "¿Qué?", "EN" to "What?"), Icons.Default.QuestionMark, Category.QUESTIONS, 11351))
        list.add(createPicto(101, mapOf("ES" to "¿Dónde?", "EN" to "Where?"), Icons.Default.Map, Category.QUESTIONS, 7764))
        list.add(createPicto(104, mapOf("ES" to "¿Quién?", "EN" to "Who?"), Icons.Default.PersonSearch, Category.QUESTIONS, 9853))
        list.add(createPicto(105, mapOf("ES" to "¿Cuándo?", "EN" to "When?"), Icons.Default.AccessTime, Category.QUESTIONS, 22621))
        list.add(createPicto(106, mapOf("ES" to "¿Por qué?", "EN" to "Why?"), Icons.AutoMirrored.Filled.Help, Category.QUESTIONS, 24763))
        list.add(createPicto(102, mapOf("ES" to "¿Qué día es hoy?", "EN" to "What day is today?"), Icons.Default.Today, Category.QUESTIONS, 32488))
        list.add(createPicto(103, mapOf("ES" to "¿Qué hora es?", "EN" to "What time is it?"), Icons.Default.Schedule, Category.QUESTIONS, 2549))

        return list
    }

    /**
     * Secuencia de IDs de pictogramas para el tablero inicial predeterminado
     * "Tablero Principal - Vocabulario Esencial".
     */
    fun getDefaultBoardPictoIds(): List<Int> {
        return listOf(
            20,  // Yo
            30,  // Quiero
            44,  // Agua
            5,   // Sí
            6,   // No
            31,  // Comer
            40,  // Manzana
            10,  // Ayuda
            11,  // Más
            12,  // Para
            61,  // Baño
            50,  // Feliz
            54,  // Duele
            1,   // Hola
            4    // Adiós
        )
    }

    fun getGuidedPhrases(): List<GuidedPhrase> {
        val catalog = getFullCatalog()
        val findPicto = { id: Int -> catalog.first { it.id == id } }

        return listOf(
            GuidedPhrase(
                mapOf("ES" to "Tengo hambre", "EN" to "I'm hungry", "FR" to "J'ai faim", "DE" to "Ich habe Hunger", "IT" to "Ho fame"),
                listOf(findPicto(20), findPicto(30), findPicto(31), findPicto(40))
            ),
            GuidedPhrase(
                mapOf("ES" to "Saludar", "EN" to "Greeting", "FR" to "Saluer", "DE" to "Grüßen", "IT" to "Salutare"),
                listOf(findPicto(1), findPicto(21))
            ),
            GuidedPhrase(
                mapOf("ES" to "Ir al baño", "EN" to "Go to bathroom", "FR" to "Aller aux toilettes", "DE" to "Zur Toilette gehen", "IT" to "Andare in bagno"),
                listOf(findPicto(20), findPicto(30), findPicto(61))
            ),
            GuidedPhrase(
                mapOf("ES" to "Día y Hora", "EN" to "Day and Time", "FR" to "Jour et Heure", "DE" to "Tag und Uhrzeit", "IT" to "Giorno e Ora"),
                listOf(findPicto(102), findPicto(103))
            ),
            GuidedPhrase(
                mapOf("ES" to "Me duele algo", "EN" to "Something hurts", "FR" to "J'ai mal", "DE" to "Etwas tut weh", "IT" to "Mi fa male"),
                listOf(findPicto(20), findPicto(54), findPicto(80))
            )
        )
    }
}
