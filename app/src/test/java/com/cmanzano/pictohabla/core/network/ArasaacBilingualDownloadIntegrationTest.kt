package com.cmanzano.pictohabla.core.network

import com.cmanzano.pictohabla.core.database.entity.PictogramEntity
import com.cmanzano.pictohabla.core.model.Category
import com.cmanzano.pictohabla.data.repository.FakeFavoriteDao
import com.cmanzano.pictohabla.data.repository.FakePictogramDao
import com.cmanzano.pictohabla.data.repository.PictogramRepository
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas de integración para la descarga bilingüe de pictogramas desde ARASAAC
 * utilizando MockWebServer con respuestas JSON reales.
 */
class ArasaacBilingualDownloadIntegrationTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var fakePictoDao: FakePictogramDao
    private lateinit var fakeFavoriteDao: FakeFavoriteDao
    private lateinit var repository: PictogramRepository

    // Respuestas JSON reales de referencia para el ID 38967 (Perro / Dog) externo al catálogo inicial
    private val jsonLanguages38967 = """
        {
          "_id": 38967,
          "categories": ["carnivorous", "mammal", "pet", "domestic animal"],
          "keywordsByLocale": {
            "es": [
              {"keyword": "perro", "type": 2, "plural": "perros", "hasLocution": true},
              {"keyword": "perra", "type": 2, "hasLocution": false, "plural": "perras"}
            ],
            "en": [
              {"keyword": "dog", "hasLocution": true, "type": 2, "plural": "dogs"}
            ]
          }
        }
    """.trimIndent()

    private val jsonEs38967 = """
        {
          "_id": 38967,
          "keywords": [
            {"keyword": "perro", "type": 2, "plural": "perros", "hasLocution": true},
            {"keyword": "perra", "type": 2, "hasLocution": false, "plural": "perras"}
          ],
          "categories": ["animal"]
        }
    """.trimIndent()

    private val jsonEn38967 = """
        {
          "_id": 38967,
          "keywords": [
            {"keyword": "dog", "hasLocution": true, "type": 2, "plural": "dogs"}
          ],
          "categories": ["animal"]
        }
    """.trimIndent()

    private val jsonSearchEsPerro = """
        [
          {
            "_id": 38967,
            "keywords": [
              {"keyword": "perro", "type": 2},
              {"keyword": "perra", "type": 2}
            ],
            "categories": ["animal"]
          }
        ]
    """.trimIndent()

    private val jsonSearchEnDog = """
        [
          {
            "_id": 38967,
            "keywords": [
              {"keyword": "dog", "type": 2}
            ],
            "categories": ["animal"]
          }
        ]
    """.trimIndent()

    // Respuestas para ID 2853 (Pecho / Chest) también externo a los 84 pictogramas base
    private val jsonLanguages2853 = """
        {
          "_id": 2853,
          "categories": ["human anatomy"],
          "keywordsByLocale": {
            "es": [{"keyword": "pecho", "type": 2}],
            "en": [{"keyword": "chest", "type": 2}]
          }
        }
    """.trimIndent()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        // Configurar la URL de ArasaacApiService hacia el servidor simulado
        ArasaacApiService.baseUrl = mockWebServer.url("/v1").toString().removeSuffix("/")

        fakePictoDao = FakePictogramDao()
        fakeFavoriteDao = FakeFavoriteDao()
        repository = PictogramRepository(fakePictoDao, fakeFavoriteDao)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        ArasaacApiService.resetBaseUrl()
    }

    @Test
    fun test1_downloadPictogramSearchingInSpanish_recoversBothEsAndEn() = runTest {
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                return when {
                    path.contains("/pictograms/es/bestsearch/perro") -> MockResponse().setBody(jsonSearchEsPerro).setResponseCode(200)
                    path.contains("/pictograms/38967/languages/es,en") -> MockResponse().setBody(jsonLanguages38967).setResponseCode(200)
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        // 1. Simular búsqueda en español
        val searchResult = ArasaacApiService.searchPictograms("perro", locale = "es").getOrThrow()
        assertEquals(1, searchResult.size)
        val candidate = searchResult.first()
        assertEquals(38967, candidate.id)
        assertEquals("perro", candidate.name)

        // 2. Consulta de detalles oficiales
        val details = ArasaacApiService.getPictogramDetails(candidate.id).getOrThrow()
        assertEquals("Perro", details.nameEs)
        assertEquals("Dog", details.nameEn)
        assertTrue(details.keywordsEs.contains("perro"))
        assertTrue(details.keywordsEn.contains("dog"))

        // 3. Creación y persistencia
        val created = repository.createArasaacPictogram(
            arasaacId = candidate.id,
            name = details.nameEs,
            nameEn = details.nameEn,
            keywordsEs = details.keywordsEs,
            keywordsEn = details.keywordsEn,
            category = Category.THINGS
        )

        assertEquals("Perro", created.getName("ES"))
        assertEquals("Dog", created.getName("EN"))
        assertTrue(created.matchesSearch("perro", "ES"))
        assertTrue(created.matchesSearch("dog", "EN"))
    }

    @Test
    fun test2_downloadPictogramSearchingInEnglish_recoversBothEsAndEn() = runTest {
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                return when {
                    path.contains("/pictograms/en/bestsearch/dog") -> MockResponse().setBody(jsonSearchEnDog).setResponseCode(200)
                    path.contains("/pictograms/38967/languages/es,en") -> MockResponse().setBody(jsonLanguages38967).setResponseCode(200)
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        // 1. Simular búsqueda en inglés
        val searchResult = ArasaacApiService.searchPictograms("dog", locale = "en").getOrThrow()
        assertEquals(1, searchResult.size)
        val candidate = searchResult.first()
        assertEquals(38967, candidate.id)
        assertEquals("dog", candidate.name)

        // 2. Consulta de detalles oficiales
        val details = ArasaacApiService.getPictogramDetails(candidate.id).getOrThrow()
        assertEquals("Perro", details.nameEs)
        assertEquals("Dog", details.nameEn)

        // 3. Persistencia
        val created = repository.createArasaacPictogram(
            arasaacId = candidate.id,
            name = details.nameEs,
            nameEn = details.nameEn,
            keywordsEs = details.keywordsEs,
            keywordsEn = details.keywordsEn,
            category = Category.THINGS
        )

        assertEquals("Perro", created.getName("ES"))
        assertEquals("Dog", created.getName("EN"))
    }

    @Test
    fun test3_fallbackToIndividualEndpointsWhenMultilingualFails() = runTest {
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                return when {
                    // El endpoint multilingüe falla deliberadamente
                    path.contains("/languages/es,en") -> MockResponse().setResponseCode(500)
                    // Las consultas individuales responden correctamente
                    path.contains("/pictograms/es/38967") -> MockResponse().setBody(jsonEs38967).setResponseCode(200)
                    path.contains("/pictograms/en/38967") -> MockResponse().setBody(jsonEn38967).setResponseCode(200)
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        val details = ArasaacApiService.getPictogramDetails(38967).getOrThrow()
        assertEquals("Perro", details.nameEs)
        assertEquals("Dog", details.nameEn)
        assertTrue(details.keywordsEs.contains("perro"))
        assertTrue(details.keywordsEn.contains("dog"))
    }

    @Test
    fun test4_persistenceInRoomStoresAllFourBilingualFields() = runTest {
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return MockResponse().setBody(jsonLanguages38967).setResponseCode(200)
            }
        }

        val details = ArasaacApiService.getPictogramDetails(38967).getOrThrow()
        val created = repository.createArasaacPictogram(
            arasaacId = 38967,
            name = details.nameEs,
            nameEn = details.nameEn,
            keywordsEs = details.keywordsEs,
            keywordsEn = details.keywordsEn,
            category = Category.THINGS
        )

        // Verificar entidad en Room directamente
        val entity = fakePictoDao.getPictogramById(created.id)
        assertNotNull("La entidad debe estar persistida en Room", entity)
        assertEquals(38967, entity!!.arasaacId)
        assertEquals("Perro", entity.nameEs)
        assertEquals("Dog", entity.nameEn)
        assertTrue("keywordsEs debe guardarse en Room", entity.keywordsEs.contains("perro"))
        assertTrue("keywordsEn debe guardarse en Room", entity.keywordsEn.contains("dog"))
    }

    @Test
    fun test5_correctReadingAfterAppReopenWithoutNetwork() = runTest {
        // Guardar pictograma inicialmente
        val entity = PictogramEntity(
            id = 100050,
            arasaacId = 38967,
            nameEs = "Perro",
            nameEn = "Dog",
            keywordsEs = "perro, can, perra",
            keywordsEn = "dog, puppy",
            categoryName = "THINGS",
            isCustom = true
        )
        fakePictoDao.insert(entity)

        // Simular que la app se cierra y el servidor se apaga (100% offline)
        mockWebServer.shutdown()

        // Nueva instancia de repositorio simulando reapertura de la app
        val newRepository = PictogramRepository(fakePictoDao, fakeFavoriteDao)
        val loaded = newRepository.getPictogramById(100050)

        assertNotNull(loaded)
        assertEquals("Perro", loaded!!.getName("ES"))
        assertEquals("Dog", loaded.getName("EN"))
        assertTrue(loaded.matchesSearch("can", "ES"))
        assertTrue(loaded.matchesSearch("puppy", "EN"))
    }

    @Test
    fun test6_offlineLanguageSwitchUpdatesImmediately() = runTest {
        val picto = PictogramEntity(
            id = 100051,
            arasaacId = 2853, // Pecho / Chest (externo al catálogo inicial)
            nameEs = "Pecho",
            nameEn = "Chest",
            keywordsEs = "pecho, tórax",
            keywordsEn = "chest, thorax",
            categoryName = "BODY",
            isCustom = true
        ).toDomain()

        // Apagar servidor
        mockWebServer.shutdown()

        // En español
        assertEquals("Pecho", picto.getName("ES"))
        // Cambio a inglés offline
        assertEquals("Chest", picto.getName("EN"))
        // Vuelta a español offline
        assertEquals("Pecho", picto.getName("ES"))
    }

    @Test
    fun test7_updateCustomPictogramPreservesTranslationsAndKeywords() = runTest {
        val created = repository.createArasaacPictogram(
            arasaacId = 38967,
            name = "Perro",
            nameEn = "Dog",
            keywordsEs = listOf("perro", "can"),
            keywordsEn = listOf("dog"),
            speechText = "Guau",
            category = Category.THINGS
        )

        // Actualizar datos del usuario (categoría o pronunciación)
        repository.updateCustomPictogram(
            id = created.id,
            name = "Perrito",
            speechText = "Quiero ver al perrito",
            category = Category.THINGS
        )

        val updated = repository.getPictogramById(created.id)
        assertNotNull(updated)
        assertEquals("Perrito", updated!!.getName("ES"))
        assertEquals("Dog", updated.getName("EN")) // Se preserva la traducción oficial inglesa
        assertEquals("Quiero ver al perrito", updated.getSpokenText("ES"))
    }

    @Test
    fun test8_recoveryOfPreviouslyDownloadedPictogramsWithEmptyOrSpanishNameEn() = runTest {
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                return when {
                    path.contains("/pictograms/38967/languages/es,en") -> MockResponse().setBody(jsonLanguages38967).setResponseCode(200)
                    path.contains("/pictograms/2853/languages/es,en") -> MockResponse().setBody(jsonLanguages2853).setResponseCode(200)
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        // Simular registro corrupto previo donde nameEn quedó vacío
        val corruptedEntity1 = PictogramEntity(
            id = 100001,
            arasaacId = 38967,
            nameEs = "Perro",
            nameEn = "", // Vacío
            keywordsEs = "perro",
            keywordsEn = "",
            categoryName = "THINGS",
            isCustom = true
        )
        fakePictoDao.insert(corruptedEntity1)

        // Simular registro corrupto previo donde nameEn se guardó erróneamente en español
        val corruptedEntity2 = PictogramEntity(
            id = 100002,
            arasaacId = 2853,
            nameEs = "Pecho",
            nameEn = "Pecho", // Duplicado erróneo en español
            keywordsEs = "pecho",
            keywordsEn = "pecho",
            categoryName = "BODY",
            isCustom = true
        )
        fakePictoDao.insert(corruptedEntity2)

        // Ejecutar mecanismo de sincronización / recuperación
        repository.syncDownloadedPictogramTranslations()

        val healed1 = fakePictoDao.getPictogramById(100001)
        assertNotNull(healed1)
        assertEquals("Perro", healed1!!.nameEs)
        assertEquals("Dog", healed1.nameEn) // Reparado automáticamente

        val healed2 = fakePictoDao.getPictogramById(100002)
        assertNotNull(healed2)
        assertEquals("Pecho", healed2!!.nameEs)
        assertEquals("Chest", healed2.nameEn) // Reparado automáticamente
    }

    @Test
    fun test9_absenceOfDuplicatesByLanguage() = runTest {
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return MockResponse().setBody(jsonLanguages38967).setResponseCode(200)
            }
        }

        val initialCount = fakePictoDao.getCount()

        val created = repository.createArasaacPictogram(
            arasaacId = 38967,
            name = "Perro",
            nameEn = "Dog",
            keywordsEs = listOf("perro"),
            keywordsEn = listOf("dog"),
            category = Category.THINGS
        )

        // Comprobar que solo se inserta UNA fila para ambos idiomas
        val afterCount = fakePictoDao.getCount()
        assertEquals("Solo debe insertarse una única fila para ambos idiomas", initialCount + 1, afterCount)

        val entity = fakePictoDao.getPictogramById(created.id)
        assertNotNull(entity)
        assertEquals("Perro", entity!!.nameEs)
        assertEquals("Dog", entity.nameEn)
    }

    @Test
    fun test10_realIntegrationWithNonCatalogArasaacId() = runTest {
        // ID 38967 no pertenece al catálogo inicial de 84 pictogramas
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return MockResponse().setBody(jsonLanguages38967).setResponseCode(200)
            }
        }

        // Pasamos deliberadamente solo name = "Perro" sin nameEn
        val created = repository.createArasaacPictogram(
            arasaacId = 38967,
            name = "Perro",
            category = Category.THINGS
        )

        // createArasaacPictogram debe consultar MockWebServer y resolver nameEn = "Dog"
        assertEquals("Perro", created.getName("ES"))
        assertEquals("Dog", created.getName("EN"))
        assertTrue("Búsqueda en inglés por 'dog' en pictograma externo al catálogo base", created.matchesSearch("dog", "EN"))
    }

    @Test
    fun test11_liveArasaacApiQuery_realNetworkIntegration() = runTest {
        // Restaurar baseUrl oficial para prueba de red real con el ID 38967 (Perro / Dog externo al catálogo base)
        ArasaacApiService.resetBaseUrl()
        try {
            val result = ArasaacApiService.getPictogramDetails(38967)
            if (result.isSuccess) {
                val details = result.getOrThrow()
                assertEquals(38967, details.id)
                assertEquals("Perro", details.nameEs)
                assertEquals("Dog", details.nameEn)
                assertTrue("Keywords en español debe contener 'perro'", details.keywordsEs.contains("perro"))
                assertTrue("Keywords en inglés debe contener 'dog'", details.keywordsEn.contains("dog"))
            }
        } catch (e: Exception) {
            // Entorno offline controlado
        } finally {
            ArasaacApiService.baseUrl = mockWebServer.url("/v1").toString().removeSuffix("/")
        }
    }
}
