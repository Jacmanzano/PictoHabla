package com.cmanzano.pictohabla.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Modelo de datos para un resultado de búsqueda en la API oficial de ARASAAC.
 */
data class ArasaacSearchResult(
    val id: Int,
    val name: String,
    val categories: List<String>,
    val keywords: List<String>
)

/**
 * Modelo de datos con traducción bilingüe oficial (ES/EN) y palabras clave de ARASAAC.
 */
data class ArasaacPictogramDetails(
    val id: Int,
    val nameEs: String,
    val nameEn: String,
    val keywordsEs: List<String>,
    val keywordsEn: List<String>,
    val categories: List<String>
)

/**
 * Servicio de conexión a la API REST oficial de ARASAAC (https://api.arasaac.org/v1).
 * Implementado utilizando las capacidades nativas de Android sin librerías externas pesadas.
 */
object ArasaacApiService {

    const val DEFAULT_BASE_URL = "https://api.arasaac.org/v1"
    var baseUrl: String = DEFAULT_BASE_URL
    private const val USER_AGENT = "PictoHabla-AAC/1.0 (Android; Contact: info@pictohabla.app)"
    private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    fun resetBaseUrl() {
        baseUrl = DEFAULT_BASE_URL
    }

    /**
     * Busca pictogramas por palabra o concepto en la API oficial de ARASAAC.
     */
    suspend fun searchPictograms(query: String, locale: String = "es"): Result<List<ArasaacSearchResult>> =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return@withContext Result.success(emptyList())

            try {
                val encodedQuery = URLEncoder.encode(trimmed.lowercase(), "UTF-8")
                val urlString = "$baseUrl/pictograms/$locale/bestsearch/$encodedQuery"
                val url = URL(urlString)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("User-Agent", USER_AGENT)
                    setRequestProperty("Accept", "application/json")
                }

                val responseCode = connection.responseCode
                if (responseCode == 404) {
                    return@withContext Result.success(emptyList())
                }
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext Result.failure(Exception("Error de servidor ARASAAC: HTTP $responseCode"))
                }

                val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(jsonText)
                val results = mutableListOf<ArasaacSearchResult>()

                for (i in 0 until jsonArray.length().coerceAtMost(30)) {
                    val item = jsonArray.getJSONObject(i)
                    val id = item.getInt("_id")

                    val keywordsList = mutableListOf<String>()
                    val keywordsArray = item.optJSONArray("keywords")
                    var preferredName = trimmed
                    if (keywordsArray != null && keywordsArray.length() > 0) {
                        for (k in 0 until keywordsArray.length()) {
                            val kwObj = keywordsArray.getJSONObject(k)
                            val kw = kwObj.optString("keyword", "")
                            if (kw.isNotBlank()) keywordsList.add(kw)
                        }
                        if (keywordsList.isNotEmpty()) {
                            preferredName = keywordsList.first()
                        }
                    }

                    val categoriesList = mutableListOf<String>()
                    val categoriesArray = item.optJSONArray("categories")
                    if (categoriesArray != null) {
                        for (c in 0 until categoriesArray.length()) {
                            categoriesList.add(categoriesArray.getString(c))
                        }
                    }

                    results.add(
                        ArasaacSearchResult(
                            id = id,
                            name = preferredName,
                            categories = categoriesList,
                            keywords = keywordsList
                        )
                    )
                }

                Result.success(results)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Descarga los bytes del archivo PNG oficial de un pictograma ARASAAC (resolución 500 px).
     * Comprueba la cabecera mágica PNG antes de entregar los datos.
     */
    suspend fun downloadPictogramBytes(arasaacId: Int): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val urlString = "$baseUrl/pictograms/$arasaacId?resolution=500"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("User-Agent", USER_AGENT)
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("No se pudo descargar la imagen del pictograma $arasaacId (HTTP ${connection.responseCode})"))
            }

            val outputStream = ByteArrayOutputStream()
            BufferedInputStream(connection.inputStream).use { input ->
                input.copyTo(outputStream)
            }
            val bytes = outputStream.toByteArray()

            if (bytes.size < 8 || !bytes.take(8).toByteArray().contentEquals(PNG_MAGIC)) {
                return@withContext Result.failure(Exception("El archivo descargado para el pictograma $arasaacId no es una imagen PNG válida"))
            }

            Result.success(bytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Consulta la información bilingüe oficial de un pictograma.
     * Utiliza preferentemente el endpoint multilingüe `/pictograms/{id}/languages/es,en`.
     * Si no devuelve ambos idiomas o falla, recurre a consultas individuales `/pictograms/es/{id}`
     * y `/pictograms/en/{id}`. Como último recurso sin conexión, consulta el diccionario offline.
     */
    suspend fun getPictogramDetails(arasaacId: Int): Result<ArasaacPictogramDetails> =
        withContext(Dispatchers.IO) {
            val offlineTrans = ArasaacTranslationHelper.getTranslation(arasaacId)
            val esList = mutableListOf<String>()
            val enList = mutableListOf<String>()
            val categoriesList = mutableListOf<String>()

            // 1. Intento preferente: Endpoint oficial multilingüe
            try {
                val urlString = "$baseUrl/pictograms/$arasaacId/languages/es,en"
                val url = URL(urlString)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("User-Agent", USER_AGENT)
                    setRequestProperty("Accept", "application/json")
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObj = org.json.JSONObject(jsonText)

                    val keywordsByLocale = jsonObj.optJSONObject("keywordsByLocale")
                    keywordsByLocale?.optJSONArray("es")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val kw = arr.optJSONObject(i)?.optString("keyword", "") ?: ""
                            if (kw.isNotBlank() && !esList.contains(kw)) esList.add(kw)
                        }
                    }

                    keywordsByLocale?.optJSONArray("en")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val kw = arr.optJSONObject(i)?.optString("keyword", "") ?: ""
                            if (kw.isNotBlank() && !enList.contains(kw)) enList.add(kw)
                        }
                    }

                    jsonObj.optJSONArray("categories")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            categoriesList.add(arr.getString(i))
                        }
                    }
                }
            } catch (e: Exception) {
                // Se gestiona con las consultas individuales o diccionario offline
            }

            // 2. Consultas alternativas independientes si falta alguno de los dos idiomas
            if (esList.isEmpty()) {
                val (fallbackEs, cats) = fetchIndividualLanguage(arasaacId, "es")
                esList.addAll(fallbackEs)
                if (categoriesList.isEmpty()) categoriesList.addAll(cats)
            }

            if (enList.isEmpty()) {
                val (fallbackEn, cats) = fetchIndividualLanguage(arasaacId, "en")
                enList.addAll(fallbackEn)
                if (categoriesList.isEmpty()) categoriesList.addAll(cats)
            }

            // 3. Fallback al diccionario offline empaquetado para cualquier idioma restante
            if (offlineTrans != null) {
                if (esList.isEmpty()) {
                    esList.addAll(offlineTrans.keywordsEs)
                }
                if (enList.isEmpty()) {
                    enList.addAll(offlineTrans.keywordsEn)
                }
            }

            // 4. Extracción y resolución estricta de nombres principales
            val nameEs = esList.firstOrNull()?.replaceFirstChar { it.uppercase() }
                ?: (offlineTrans?.nameEs ?: "")

            val nameEn = enList.firstOrNull()?.replaceFirstChar { it.uppercase() }
                ?: (offlineTrans?.nameEn ?: "")

            // No permitir que nameEn sea idéntico a nameEs cuando no hay traducción inglesa real
            val finalNameEn = if (nameEn.isNotBlank()) nameEn else ""

            if (nameEs.isNotBlank() || finalNameEn.isNotBlank()) {
                Result.success(
                    ArasaacPictogramDetails(
                        id = arasaacId,
                        nameEs = nameEs,
                        nameEn = finalNameEn,
                        keywordsEs = esList,
                        keywordsEn = enList,
                        categories = categoriesList
                    )
                )
            } else {
                Result.failure(Exception("No se pudieron obtener traducciones oficiales para el pictograma ARASAAC $arasaacId"))
            }
        }

    /**
     * Consulta auxiliar para obtener palabras clave y categorías de un idioma específico:
     * `GET /pictograms/{locale}/{id}`
     */
    private fun fetchIndividualLanguage(arasaacId: Int, locale: String): Pair<List<String>, List<String>> {
        val keywords = mutableListOf<String>()
        val categories = mutableListOf<String>()
        try {
            val urlString = "$baseUrl/pictograms/$locale/$arasaacId"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObj = org.json.JSONObject(jsonText)

                jsonObj.optJSONArray("keywords")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val kw = arr.optJSONObject(i)?.optString("keyword", "") ?: ""
                        if (kw.isNotBlank() && !keywords.contains(kw)) keywords.add(kw)
                    }
                }

                jsonObj.optJSONArray("categories")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        categories.add(arr.getString(i))
                    }
                }
            }
        } catch (e: Exception) {
            // Error de conexión individual
        }
        return Pair(keywords, categories)
    }
}
