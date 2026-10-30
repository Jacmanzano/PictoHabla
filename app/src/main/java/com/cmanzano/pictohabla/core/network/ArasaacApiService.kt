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
 * Servicio de conexión a la API REST oficial de ARASAAC (https://api.arasaac.org/v1).
 * Implementado utilizando las capacidades nativas de Android sin librerías externas pesadas.
 */
object ArasaacApiService {

    private const val BASE_URL = "https://api.arasaac.org/v1"
    private const val USER_AGENT = "PictoHabla-AAC/1.0 (Android; Contact: info@pictohabla.app)"
    private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    /**
     * Busca pictogramas por palabra o concepto en la API oficial de ARASAAC.
     */
    suspend fun searchPictograms(query: String, locale: String = "es"): Result<List<ArasaacSearchResult>> =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return@withContext Result.success(emptyList())

            try {
                val encodedQuery = URLEncoder.encode(trimmed.lowercase(), "UTF-8")
                val urlString = "$BASE_URL/pictograms/$locale/bestsearch/$encodedQuery"
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
            val urlString = "$BASE_URL/pictograms/$arasaacId?resolution=500"
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
}
