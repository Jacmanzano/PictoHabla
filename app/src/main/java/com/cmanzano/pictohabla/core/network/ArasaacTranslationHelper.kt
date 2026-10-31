package com.cmanzano.pictohabla.core.network

/**
 * Diccionario de traducción y palabras clave oficiales de ARASAAC disponible 100% offline.
 * Garantiza la consistencia bilingüe (ES/EN) de los 84 pictogramas base y conceptos esenciales
 * como perro, gato, etc., incluso en entornos sin conectividad.
 */
object ArasaacTranslationHelper {

    data class Translation(
        val arasaacId: Int,
        val nameEs: String,
        val nameEn: String,
        val keywordsEs: List<String>,
        val keywordsEn: List<String>
    )

    private val translations = mapOf<Int, Translation>(
        2517 to Translation(2517, "Perro", "Dog", listOf("perro", "can", "perra", "chucho", "cachorro"), listOf("dog", "canine", "puppy", "hound")),
        7202 to Translation(7202, "Perro", "Dog", listOf("perro", "can", "perra", "chucho", "cachorro"), listOf("dog", "canine", "puppy", "hound")),
        2406 to Translation(2406, "Gato", "Cat", listOf("gato", "minino", "gata", "felino"), listOf("cat", "kitten", "pussycat", "feline")),
        2626 to Translation(2626, "cero", "zero", listOf("0", "cero"), listOf("nought", "zero", "0")),
        2627 to Translation(2627, "uno", "one", listOf("uno", "1"), listOf("one", "1")),
        2628 to Translation(2628, "dos", "two", listOf("2", "dos"), listOf("2", "two")),
        2629 to Translation(2629, "tres", "three", listOf("3", "tres"), listOf("3", "three")),
        2630 to Translation(2630, "cuatro", "four", listOf("4", "cuatro"), listOf("4", "four")),
        2631 to Translation(2631, "cinco", "five", listOf("5", "cinco"), listOf("five", "5", "five ")),
        2632 to Translation(2632, "seis", "six", listOf("6", "seis"), listOf("6", "six")),
        2633 to Translation(2633, "siete", "seven", listOf("7", "siete"), listOf("7", "seven")),
        2634 to Translation(2634, "ocho", "eight", listOf("8", "ocho"), listOf("8", "eight")),
        2635 to Translation(2635, "nueve", "nine", listOf("9", "nueve"), listOf("9", "nine")),
        6009 to Translation(6009, "Hola", "Hello", listOf("hola"), listOf("hello", "hi")),
        8129 to Translation(8129, "Gracias", "Thanks", listOf("gracias"), listOf("thanks", "thank you")),
        8195 to Translation(8195, "Por favor", "Please", listOf("por favor", "rogar"), listOf("please", "beg", "beseech")),
        6028 to Translation(6028, "Adiós", "Bye", listOf("adiós", "hasta luego", "hasta la vista"), listOf("goodbye", "see you later", "see you", "bye")),
        5584 to Translation(5584, "Sí", "Yes", listOf("sí"), listOf("yes")),
        5526 to Translation(5526, "No", "No", listOf("no"), listOf("no")),
        11625 to Translation(11625, "Perdón", "Sorry", listOf("lo siento", "perdonar", "¡lo siento!", "¡perdón!", "perdón", "pedir perdón"), listOf("sorry", "forgive", "I'm sorry!", "sorry!", "I'm so sorry!")),
        7272 to Translation(7272, "Tengo hambre", "I am hungry", listOf("tengo hambre", "tener hambre"), listOf("i am hungry", "be hungry", "hunger")),
        7273 to Translation(7273, "Tengo sed", "I am thirsty", listOf("tengo sed", "sediento", "tener sed", "sed"), listOf("i am thirsty", "thirsty", "be thirsty", "thirst", "thirst")),
        12252 to Translation(12252, "Ayuda", "Help", listOf("apoyo", "ayuda", "colaboración", "cooperación"), listOf("support", "help", "colaboration", "cooperation")),
        3220 to Translation(3220, "Más", "More", listOf("más", "+"), listOf("more", "plus sign", "+")),
        7196 to Translation(7196, "Para", "Stop", listOf("para", "parar", "detener"), listOf("stop")),
        37162 to Translation(37162, "Otra vez", "Again", listOf("otra vez", "una vez más"), listOf("again", "once more", "once again")),
        5358 to Translation(5358, "Terminado", "Finished", listOf("terminado", "terminar", "fin", "acabar", "finalizar", "final"), listOf("finished", "finish", "end")),
        11696 to Translation(11696, "No sé", "I don't know", listOf("no sé", "duda", "dudar"), listOf("i don't know", "doubt", "doubt", "hesitate", "dither", "waver")),
        37826 to Translation(37826, "Me gusta", "I like it", listOf("me gusta", "gustar", "me gusta eso"), listOf("i like it", "like", "enjoy", "I like that")),
        37825 to Translation(37825, "No me gusta", "I dislike it", listOf("no me gusta", "no gustar", "no me gusta eso"), listOf("i dislike it", "dislike", "I do not like that", "I don't like that")),
        2617 to Translation(2617, "Yo", "I", listOf("yo", "me", "mí", "yo me"), listOf("I", "me", "for me")),
        2608 to Translation(2608, "Tú", "You", listOf("tú", "usted", "tú te"), listOf("you")),
        2458 to Translation(2458, "Mamá", "Mom", listOf("mamá"), listOf("mum", "mom", "mommy", "mother")),
        31146 to Translation(31146, "Papá", "Dad", listOf("papá"), listOf("dad", "daddy")),
        2244 to Translation(2244, "Abuelo", "Grandpa", listOf("abuelo", "yayo"), listOf("grandfather", "grandpa")),
        2243 to Translation(2243, "Abuela", "Grandma", listOf("abuela", "yaya"), listOf("grandmother", "granny", "grandma")),
        8487 to Translation(8487, "Amigo", "Friend", listOf("amigo"), listOf("friend")),
        6556 to Translation(6556, "Profesor", "Teacher", listOf("maestro", "profesor", "maestra", "profesora"), listOf("schoolteacher", "teacher", "professor")),
        6480 to Translation(6480, "Él", "He", listOf("él", "él se"), listOf("he")),
        7029 to Translation(7029, "Ella", "She", listOf("ella", "la", "lo", "sí", "se", "ella se"), listOf("she", "her", "herself", "with her")),
        7185 to Translation(7185, "Nosotros", "We", listOf("nosotros", "nosotros nos"), listOf("us", "we", "ourselves")),
        5441 to Translation(5441, "Quiero", "Want", listOf("quiero", "querer", "desear", "pedir", "yo quiero"), listOf("want", "wish for", "I want", "I want that one", "wish", "feel like")),
        37160 to Translation(37160, "Necesito", "Need", listOf("necesito", "necesitar"), listOf("need")),
        2349 to Translation(2349, "Comer", "Eat", listOf("comer", "tomar"), listOf("eat", "have", "take in", "ingest", "take")),
        2276 to Translation(2276, "Beber", "Drink", listOf("beber", "tomar"), listOf("drink")),
        2439 to Translation(2439, "Jugar", "Play", listOf("jugar"), listOf("play")),
        2369 to Translation(2369, "Dormir", "Sleep", listOf("dormir"), listOf("sleep")),
        2432 to Translation(2432, "Ir", "Go", listOf("ir"), listOf("go")),
        11708 to Translation(11708, "Venir", "Come", listOf("venir", "acudir", "llegar"), listOf("come", "arrive")),
        11749 to Translation(11749, "Hacer", "Do", listOf("hacer"), listOf("make", "do", "create")),
        2474 to Translation(2474, "Ver", "Watch", listOf("ver", "mirar", "observar"), listOf("see", "look", "look at", "watch", "observe")),
        2381 to Translation(2381, "Escuchar", "Listen", listOf("oír", "escuchar"), listOf("hear", "listen")),
        2719 to Translation(2719, "Correr", "Run", listOf("correr"), listOf("run")),
        2462 to Translation(2462, "Manzana", "Apple", listOf("manzana", "fruta", "frutas"), listOf("apple", "fruit", "fruits")),
        2530 to Translation(2530, "Plátano", "Banana", listOf("plátano"), listOf("banana")),
        2494 to Translation(2494, "Pan", "Bread", listOf("pan", "barra de pan"), listOf("bread", "bread stick", "French bread", "baguette")),
        2445 to Translation(2445, "Leche", "Milk", listOf("leche"), listOf("milk", "cows' milk", "pasteurized milk", "whole milk")),
        2248 to Translation(2248, "Agua", "Water", listOf("agua", "beber", "bebida", "sed"), listOf("water", "drink", "beverage")),
        2527 to Translation(2527, "Pizza", "Pizza", listOf("pizza"), listOf("pizza")),
        9907 to Translation(9907, "Feliz", "Happy", listOf("contento", "feliz", "contenta"), listOf("content", "happy")),
        2606 to Translation(2606, "Triste", "Sad", listOf("triste"), listOf("sad", "depressed", "unhappy")),
        2374 to Translation(2374, "Enfadado", "Angry", listOf("enfadado", "enfadada"), listOf("angry", "irritated", "annoyed")),
        2314 to Translation(2314, "Cansado", "Tired", listOf("cansado"), listOf("tired", "exhausted", "fatigued")),
        30620 to Translation(30620, "Duele", "Hurt", listOf("duele", "doler", "dolor"), listOf("hurt", "suffer", "ache", "pain")),
        35557 to Translation(35557, "Tengo frío", "I am cold", listOf("tengo frío", "frío"), listOf("i am cold", "cold")),
        6952 to Translation(6952, "Tengo calor", "I am hot", listOf("tengo calor", "tener calor", "hacer calor"), listOf("i am hot", "be hot")),
        2317 to Translation(2317, "Casa", "House", listOf("casa", "hogar", "vivienda", "edificio"), listOf("house", "home", "residence", "building")),
        6929 to Translation(6929, "Baño", "Bathroom", listOf("cuarto de baño", "baño"), listOf("bathroom", "men's bathroom", "toilet", "men's wastroom", "W.C.", "washroom")),
        3082 to Translation(3082, "Colegio", "School", listOf("colegio", "escuela"), listOf("school", "schoolhouse")),
        2859 to Translation(2859, "Parque", "Park", listOf("parque"), listOf("park", "playground", "playpark")),
        16713 to Translation(16713, "Quiero ir al baño", "I want to go to bathroom", listOf("quiero ir al baño", "hacer pis", "orinar", "pis", "pipí", "hacer pipí"), listOf("i want to go to bathroom", "pee", "urinate")),
        3241 to Translation(3241, "Pelota", "Ball", listOf("pelota", "pelota de playa"), listOf("ball", "beach ball", "beachball")),
        2450 to Translation(2450, "Libro", "Book", listOf("libro"), listOf("book")),
        2339 to Translation(2339, "Coche", "Car", listOf("coche", "automóvil", "auto", "carro"), listOf("car", "family touring car", "auto")),
        2475 to Translation(2475, "Mochila", "Backpack", listOf("mochila"), listOf("rucksack", "backpack")),
        2673 to Translation(2673, "Cabeza", "Head", listOf("cabeza"), listOf("head")),
        2928 to Translation(2928, "Mano", "Hand", listOf("mano", "mano derecha"), listOf("hand", "right hand")),
        2841 to Translation(2841, "Pie", "Foot", listOf("pie"), listOf("foot")),
        2798 to Translation(2798, "Sol", "Sun", listOf("Sol"), listOf("Sun")),
        3123 to Translation(3123, "Lluvia", "Rain", listOf("lluvia"), listOf("rain", "rainfall")),
        11351 to Translation(11351, "¿Qué?", "What?", listOf("¿qué?", "que"), listOf("what?", "that")),
        7764 to Translation(7764, "¿Dónde?", "Where?", listOf("dónde", "¿dónde?", "donde"), listOf("where", "where?")),
        9853 to Translation(9853, "¿Quién?", "Who?", listOf("quién", "desconocido", "¿quién es?", "¿quién es él?", "¿quién?"), listOf("who?", "who", "unknown", "Who's he?", "who is he?", "strange")),
        22621 to Translation(22621, "¿Cuándo?", "When?", listOf("cuándo", "¿cuándo?"), listOf("when?", "when")),
        24763 to Translation(24763, "¿Por qué?", "Why?", listOf("¿por qué?", "por qué"), listOf("why?", "why")),
        32488 to Translation(32488, "¿Qué día es hoy?", "What day is today?", listOf("¿qué día es hoy?", "calendario"), listOf("what day is today?", "calendar")),
        2549 to Translation(2549, "¿Qué hora es?", "What time is it?", listOf("¿qué hora es?", "reloj"), listOf("what time is it?", "watch", "wristwatch")),
    )

    fun getTranslation(arasaacId: Int): Translation? {
        return translations[arasaacId]
    }

    fun getAllTranslations(): Map<Int, Translation> {
        return translations
    }
}
