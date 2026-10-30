package com.cmanzano.pictohabla.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Utilidad para almacenar y gestionar de forma persistente imágenes personalizadas
 * en el almacenamiento privado de la aplicación (100% offline).
 */
object ImageStorageHelper {

    private const val DIRECTORY_NAME = "custom_pictos"

    /**
     * Copia una imagen desde una URI externa al directorio privado de la aplicación.
     * Retorna la ruta absoluta del archivo guardado.
     */
    suspend fun saveImageToInternalStorage(context: Context, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, DIRECTORY_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val filename = "picto_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
        val destinationFile = File(dir, filename)

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destinationFile).use { output ->
                input.copyTo(output)
            }
        }

        destinationFile.absolutePath
    }

    /**
     * Elimina el archivo de imagen local del almacenamiento privado.
     */
    suspend fun deleteImage(filePath: String?) = withContext(Dispatchers.IO) {
        if (filePath.isNullOrBlank()) return@withContext
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Carga un mapa de bits (Bitmap) desde el archivo local sin bloquear el hilo principal.
     */
    fun loadBitmap(filePath: String?): Bitmap? {
        if (filePath.isNullOrBlank()) return null
        return try {
            val file = File(filePath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
