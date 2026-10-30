package com.cmanzano.pictohabla.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.cmanzano.pictohabla.core.model.Pictogram
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Gestor y cargador de imágenes de pictogramas oficiales de ARASAAC y fotos locales.
 *
 * Características principales:
 * 1. Caché en memoria LRU de alto rendimiento para garantizar un desplazamiento fluido a 60/120 fps.
 * 2. Carga 100% offline desde los recursos integrados en assets (`pictograms/{id}.png`).
 * 3. Soporte para nuevos pictogramas ARASAAC descargados por cuidadores a almacenamiento persistente privado (`arasaac_pictos/`).
 * 4. Soporte transparente para fotografías personalizadas del usuario.
 * 5. Prioridad visual estricta sin sustituciones artificiales por emojis.
 */
object ArasaacImageHelper {

    private const val ARASAAC_DIR_NAME = "arasaac_pictos"
    private const val ASSETS_PREFIX = "pictograms"

    // Caché LRU en memoria con límite dinámico basado en la memoria disponible (máx 24 MB)
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceIn(4096, 24576)

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    /**
     * Carga el [Bitmap] correspondiente a un [Pictogram], respetando la jerarquía de prioridad:
     * 1. Imagen ARASAAC local (desde filesDir si fue descargada, o desde assets integrada).
     * 2. Fotografía personalizada local si es un pictograma propio.
     * 3. Null si no hay representación de imagen disponible.
     */
    fun loadPictogramBitmap(context: Context, picto: Pictogram): Bitmap? {
        // Prioridad 1: Pictograma oficial ARASAAC (si tiene arasaacId)
        if (picto.arasaacId != null) {
            val bitmap = loadArasaacBitmap(context, picto.arasaacId)
            if (bitmap != null) return bitmap
        }

        // Prioridad 2: Fotografía personalizada local
        if (picto.isCustom && !picto.localImagePath.isNullOrBlank()) {
            val cacheKey = "custom_${picto.localImagePath}"
            synchronized(memoryCache) {
                memoryCache.get(cacheKey)?.let { return it }
            }

            val bitmap = ImageStorageHelper.loadBitmap(picto.localImagePath)
            if (bitmap != null) {
                synchronized(memoryCache) {
                    memoryCache.put(cacheKey, bitmap)
                }
                return bitmap
            }
        }

        return null
    }

    /**
     * Carga el Bitmap de un pictograma oficial ARASAAC por su ID numérico.
     * Consulta primero la caché en memoria, luego el almacenamiento privado y finalmente assets.
     */
    fun loadArasaacBitmap(context: Context, arasaacId: Int): Bitmap? {
        val cacheKey = "arasaac_$arasaacId"

        // 1. Verificación en caché de memoria
        synchronized(memoryCache) {
            memoryCache.get(cacheKey)?.let { return it }
        }

        // 2. Verificación en almacenamiento privado de la app (pictogramas descargados por el usuario)
        try {
            val userFile = File(File(context.filesDir, ARASAAC_DIR_NAME), "$arasaacId.png")
            if (userFile.exists() && userFile.length() > 0) {
                val bitmap = BitmapFactory.decodeFile(userFile.absolutePath)
                if (bitmap != null) {
                    synchronized(memoryCache) {
                        memoryCache.put(cacheKey, bitmap)
                    }
                    return bitmap
                }
            }
        } catch (_: Exception) {
        }

        // 3. Verificación en assets integrados de la app (catálogo inicial 100% offline)
        try {
            val assetPath = "$ASSETS_PREFIX/$arasaacId.png"
            context.assets.open(assetPath).use { input ->
                val bitmap = BitmapFactory.decodeStream(input)
                if (bitmap != null) {
                    synchronized(memoryCache) {
                        memoryCache.put(cacheKey, bitmap)
                    }
                    return bitmap
                }
            }
        } catch (_: Exception) {
        }

        return null
    }

    /**
     * Comprueba si la imagen de un pictograma ARASAAC está disponible localmente sin red.
     */
    fun hasLocalArasaacImage(context: Context, arasaacId: Int): Boolean {
        // En filesDir
        val userFile = File(File(context.filesDir, ARASAAC_DIR_NAME), "$arasaacId.png")
        if (userFile.exists() && userFile.length() > 0) return true

        // En assets
        return try {
            context.assets.open("$ASSETS_PREFIX/$arasaacId.png").use { true }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Guarda de forma persistente los bytes de una imagen PNG oficial de ARASAAC en el directorio privado.
     * Retorna la ruta absoluta del archivo guardado.
     */
    fun saveArasaacImage(context: Context, arasaacId: Int, imageBytes: ByteArray): String {
        val dir = File(context.filesDir, ARASAAC_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, "$arasaacId.png")
        FileOutputStream(file).use { out ->
            out.write(imageBytes)
        }

        // Pre-carga e inserta en caché de memoria
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        if (bitmap != null) {
            synchronized(memoryCache) {
                memoryCache.put("arasaac_$arasaacId", bitmap)
            }
        }

        return file.absolutePath
    }

    /**
     * Guarda el flujo de entrada de una imagen PNG de ARASAAC directamente en almacenamiento privado.
     */
    fun saveArasaacImage(context: Context, arasaacId: Int, inputStream: InputStream): String {
        val bytes = inputStream.readBytes()
        return saveArasaacImage(context, arasaacId, bytes)
    }

    /**
     * Limpia la memoria caché de imágenes en caso de advertencia de memoria baja.
     */
    fun clearCache() {
        synchronized(memoryCache) {
            memoryCache.evictAll()
        }
    }
}
