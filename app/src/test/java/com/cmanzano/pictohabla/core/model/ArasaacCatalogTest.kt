package com.cmanzano.pictohabla.core.model

import com.cmanzano.pictohabla.core.database.entity.FavoriteEntity
import com.cmanzano.pictohabla.core.database.entity.PhraseHistoryEntity
import com.cmanzano.pictohabla.core.database.entity.PictogramEntity
import com.cmanzano.pictohabla.data.local.DefaultCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer

/**
 * Pruebas automatizadas unitarias para verificar la integridad del catálogo ARASAAC,
 * la presencia y decodificación de imágenes locales, ausencia de emojis,
 * integridad del manifiesto JSON y compatibilidad con Room, Favoritos, Historial,
 * Tableros y Modo Aprender.
 *
 * Se ejecutan 100% offline sin depender de llamadas de red.
 */
class ArasaacCatalogTest {

    private val pngHeader = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private val assetsDir = File("src/main/assets/pictograms")
    private val manifestFile = File("src/main/assets/arasaac_catalog_manifest.json")

    @Test
    fun defaultCatalog_all84PictogramsHaveValidOfficialArasaacId() {
        val catalog = DefaultCatalog.getFullCatalog()
        assertEquals("El catálogo debe contener exactamente 84 conceptos", 84, catalog.size)

        catalog.forEach { picto ->
            assertNotNull("El pictograma ${picto.id} (${picto.getName("ES")}) debe tener un arasaacId asignado", picto.arasaacId)
            assertTrue("El arasaacId debe ser un número entero positivo válido", picto.arasaacId!! > 0)
        }
    }

    @Test
    fun defaultCatalog_arasaacIdsAreUniqueAcrossCatalog() {
        val catalog = DefaultCatalog.getFullCatalog()
        val arasaacIds = catalog.mapNotNull { it.arasaacId }
        val uniqueArasaacIds = arasaacIds.toSet()

        assertEquals(
            "Cada pictograma debe tener un ID ARASAAC único sin colisiones ni duplicados",
            84,
            uniqueArasaacIds.size
        )
        assertEquals(
            "La cantidad de IDs únicos debe coincidir con el total de pictogramas",
            arasaacIds.size,
            uniqueArasaacIds.size
        )
    }

    @Test
    fun defaultCatalog_internalIdsAreUniqueAcrossCatalog() {
        val catalog = DefaultCatalog.getFullCatalog()
        val internalIds = catalog.map { it.id }
        val uniqueInternalIds = internalIds.toSet()

        assertEquals(
            "Cada pictograma debe tener un ID interno único sin duplicados",
            internalIds.size,
            uniqueInternalIds.size
        )
    }

    @Test
    fun manifest_integrityAndStrictMatchWithDefaultCatalog() {
        assertTrue("El archivo manifiesto arasaac_catalog_manifest.json debe existir en assets", manifestFile.exists())
        val manifestContent = manifestFile.readText(Charsets.UTF_8)

        val totalMatch = Regex(""""total_pictograms":\s*(\d+)""").find(manifestContent)
        assertNotNull("El manifiesto debe contener el campo 'total_pictograms'", totalMatch)
        assertEquals("El manifiesto debe declarar 84 pictogramas", 84, totalMatch!!.groupValues[1].toInt())

        // Extracción de cada entrada de pictograma en el manifiesto
        val itemRegex = Regex(""""internal_id":\s*(\d+)[\s\S]*?"arasaac_id":\s*(\d+)[\s\S]*?"local_asset_path":\s*"([^"]+)"""")
        val matches = itemRegex.findAll(manifestContent).toList()
        assertEquals("El manifiesto debe contener exactamente 84 definiciones completas", 84, matches.size)

        val catalogMap = DefaultCatalog.getFullCatalog().associateBy { it.id }
        val manifestArasaacIds = mutableSetOf<Int>()

        matches.forEach { match ->
            val internalId = match.groupValues[1].toInt()
            val arasaacId = match.groupValues[2].toInt()
            val localAssetPath = match.groupValues[3]

            // Comprobar existencia en catálogo Kotlin
            val catalogPicto = catalogMap[internalId]
            assertNotNull("El concepto del manifiesto ID $internalId debe existir en DefaultCatalog", catalogPicto)
            assertEquals(
                "El arasaacId del manifiesto debe coincidir exactamente con el catálogo en código para ID $internalId",
                catalogPicto!!.arasaacId,
                arasaacId
            )

            // Comprobar unicidad en manifiesto
            assertFalse(
                "El arasaacId $arasaacId no debe estar duplicado en el manifiesto",
                manifestArasaacIds.contains(arasaacId)
            )
            manifestArasaacIds.add(arasaacId)

            // Comprobar que el archivo asset referenciado existe físicamente
            val assetFile = File("src/main/assets", localAssetPath)
            assertTrue(
                "El archivo localAssetPath '$localAssetPath' del manifiesto debe existir físicamente",
                assetFile.exists()
            )
        }

        assertEquals("El manifiesto debe tener exactamente 84 IDs ARASAAC únicos", 84, manifestArasaacIds.size)
    }

    @Test
    fun images_all84PngFilesExistAreValidAndHaveCorrectDimensions() {
        val catalog = DefaultCatalog.getFullCatalog()
        assertTrue("La carpeta de assets de pictogramas debe existir", assetsDir.exists())

        catalog.forEach { picto ->
            val arId = picto.arasaacId!!
            val file = File(assetsDir, "$arId.png")
            assertTrue("El archivo de imagen assets/pictograms/$arId.png debe existir para ${picto.getName("ES")}", file.exists())
            assertTrue("El archivo $arId.png debe tener un tamaño superior a 1 KB", file.length() > 1024)

            val bytes = file.readBytes()
            assertTrue("El archivo $arId.png debe contener al menos 30 bytes", bytes.size > 30)

            // Comprobación de cabecera mágica PNG oficial (8 bytes)
            val header = bytes.copyOfRange(0, 8)
            assertTrue(
                "El archivo $arId.png debe comenzar con la cabecera mágica PNG oficial",
                header.contentEquals(pngHeader)
            )

            // Comprobación de chunk IHDR (anchura y altura válidas de imagen)
            // Bytes 12..15: 'IHDR', Bytes 16..19: width, Bytes 20..23: height
            val chunkType = String(bytes.copyOfRange(12, 16), Charsets.US_ASCII)
            assertEquals("El primer chunk de $arId.png debe ser IHDR", "IHDR", chunkType)

            val width = ByteBuffer.wrap(bytes.copyOfRange(16, 20)).int
            val height = ByteBuffer.wrap(bytes.copyOfRange(20, 24)).int
            assertTrue("La anchura de $arId.png debe ser al menos 100px (es $width)", width >= 100)
            assertTrue("La altura de $arId.png debe ser al menos 100px (es $height)", height >= 100)
        }
    }

    @Test
    fun defaultCatalog_noEmojisUsedInStandardCatalogNamesOrTitles() {
        val catalog = DefaultCatalog.getFullCatalog()
        val emojiRegex = Regex("[\\p{So}\\p{Cn}\\uD83C-\\uDBFF\\uDC00-\\uDFFF]")

        catalog.forEach { picto ->
            val esName = picto.getName("ES")
            assertFalse(
                "El nombre del pictograma '$esName' no debe contener emojis como sustituto de imagen",
                emojiRegex.containsMatchIn(esName)
            )
        }

        DefaultCatalog.getGuidedPhrases().forEach { phrase ->
            val title = phrase.getTitle("ES")
            assertFalse(
                "El título de frase guiada '$title' no debe contener emojis",
                emojiRegex.containsMatchIn(title)
            )
        }
    }

    @Test
    fun pictogramEntity_preservesArasaacIdInConversion() {
        val originalPicto = DefaultCatalog.getFullCatalog().first { it.id == 1 } // Hola -> 6009
        assertEquals(6009, originalPicto.arasaacId)

        val entity = PictogramEntity.fromDomain(originalPicto)
        assertEquals(6009, entity.arasaacId)

        val restoredPicto = entity.toDomain()
        assertEquals(originalPicto.id, restoredPicto.id)
        assertEquals(originalPicto.arasaacId, restoredPicto.arasaacId)
        assertEquals(originalPicto.getName("ES"), restoredPicto.getName("ES"))
        assertEquals(originalPicto.category, restoredPicto.category)
    }

    @Test
    fun pictogramEntity_resolvesArasaacIdFromCatalogIfEntityArasaacIdIsNull() {
        // Simula migración desde base de datos antigua v1 o v2 donde arasaacId era null
        val oldEntity = PictogramEntity(
            id = 2, // Gracias
            nameEs = "Gracias",
            categoryName = "SOCIAL",
            arasaacId = null
        )

        val domain = oldEntity.toDomain()
        assertEquals(
            "toDomain debe resolver automáticamente el ID ARASAAC para pictogramas del catálogo estándar",
            8129,
            domain.arasaacId
        )
    }

    @Test
    fun customPictograms_retainCustomPhotoWithoutOverwritingArasaacId() {
        val customPicto = Pictogram(
            id = 100001,
            names = mapOf("ES" to "Mi Perro"),
            category = Category.THINGS,
            localImagePath = "/data/user/0/com.cmanzano.pictohabla/files/custom_pictos/picto_123.jpg",
            isCustom = true,
            arasaacId = null
        )

        val entity = PictogramEntity.fromDomain(customPicto)
        assertTrue(entity.isCustom)
        assertEquals("/data/user/0/com.cmanzano.pictohabla/files/custom_pictos/picto_123.jpg", entity.localImagePath)
        assertNull(entity.arasaacId)

        val restored = entity.toDomain()
        assertTrue(restored.isCustom)
        assertEquals(customPicto.localImagePath, restored.localImagePath)
        assertNull(restored.arasaacId)
    }

    @Test
    fun defaultBoard_allPictogramsContainValidArasaacIds() {
        val catalogMap = DefaultCatalog.getFullCatalog().associateBy { it.id }
        val boardIds = DefaultCatalog.getDefaultBoardPictoIds()

        boardIds.forEach { pictoId ->
            val picto = catalogMap[pictoId]
            assertNotNull("El pictograma $pictoId del tablero esencial debe existir en el catálogo", picto)
            assertNotNull("El pictograma $pictoId del tablero esencial debe tener arasaacId", picto!!.arasaacId)
            assertTrue("El arasaacId debe ser positivo", picto.arasaacId!! > 0)
        }
    }

    @Test
    fun favoritesWorkflow_preservesArasaacIdThroughEntityMapping() {
        val catalogMap = DefaultCatalog.getFullCatalog().associateBy { it.id }
        val favoritePictogramIds = listOf(1, 2, 8, 22) // Hola, Gracias, Tengo hambre, Mamá

        favoritePictogramIds.forEach { pictoId ->
            val favoriteEntity = FavoriteEntity(pictoId = pictoId)
            val picto = catalogMap[favoriteEntity.pictoId]
            assertNotNull("El pictograma favorito debe existir en el catálogo", picto)
            assertNotNull("El pictograma favorito debe tener arasaacId", picto!!.arasaacId)
            assertTrue("El pictograma favorito debe tener arasaacId > 0", picto.arasaacId!! > 0)
        }
    }

    @Test
    fun historyWorkflow_preservesArasaacIdsForReconstructedPhrases() {
        val catalogMap = DefaultCatalog.getFullCatalog().associateBy { it.id }
        val historyEntity = PhraseHistoryEntity(
            spokenText = "Hola Gracias",
            pictoIdsCsv = "1,2"
        )

        val domainHistory = historyEntity.toDomain()
        assertEquals(2, domainHistory.pictoIds.size)

        domainHistory.pictoIds.forEach { id ->
            val picto = catalogMap[id]
            assertNotNull("El pictograma del historial debe resolverse en el catálogo", picto)
            assertEquals("Hola".takeIf { id == 1 } ?: "Gracias", picto!!.getName("ES"))
            assertNotNull("El pictograma del historial debe tener su arasaacId", picto.arasaacId)
            val file = File(assetsDir, "${picto.arasaacId}.png")
            assertTrue("El PNG del pictograma en el historial debe existir", file.exists())
        }
    }

    @Test
    fun learningMode_allGuidedPhrasesAndActivitiesUseValidArasaacPictograms() {
        val guidedPhrases = DefaultCatalog.getGuidedPhrases()
        assertTrue("Deben existir frases guiadas en el Modo Aprender", guidedPhrases.isNotEmpty())

        guidedPhrases.forEach { phrase ->
            phrase.pictos.forEach { picto ->
                assertNotNull(
                    "Cada pictograma de la frase guiada '${phrase.getTitle("ES")}' debe tener arasaacId",
                    picto.arasaacId
                )
                val file = File(assetsDir, "${picto.arasaacId}.png")
                assertTrue(
                    "El archivo de imagen para el pictograma '${picto.getName("ES")}' (${picto.arasaacId}.png) debe existir offline",
                    file.exists()
                )
            }
        }
    }
}
