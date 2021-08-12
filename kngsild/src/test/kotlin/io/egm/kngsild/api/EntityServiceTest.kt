package io.egm.kngsild.api

import arrow.core.left
import arrow.core.right
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.egm.kngsild.model.AccessTokenNotRetrieved
import io.egm.kngsild.model.AlreadyExists
import io.egm.kngsild.model.ResourceNotFound
import io.egm.kngsild.utils.AuthUtils
import io.egm.kngsild.utils.JsonUtils.serializeObject
import io.egm.kngsild.utils.NgsiLdAttributeNG
import io.egm.kngsild.utils.NgsiLdPropertyBuilder
import io.egm.kngsild.utils.NgsiLdUtils.coreContext
import io.egm.kngsild.utils.NgsildEntity
import io.egm.kngsild.utils.UriUtils.toUri
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.io.File
import java.net.URI
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntityServiceTest {

    private lateinit var wireMockServer: WireMockServer

    private val entityPayloadFile = javaClass.classLoader.getResource("ngsild/entities/entity.jsonld")
    private val entityAttributesUpdatePayloadFile = javaClass.classLoader
        .getResource("ngsild/entities/fragments/attributes_update_fragment.json")

    @BeforeAll
    fun beforeAll() {
        wireMockServer = WireMockServer(wireMockConfig().port(8089))
        wireMockServer.start()
        // If not using the default port, we need to instruct explicitly the client (quite redundant)
        configureFor(8089)
    }

    @AfterEach
    fun resetWiremock() {
        reset()
    }

    @AfterAll
    fun afterAll() {
        wireMockServer.stop()
    }

    @Test
    fun `it should create an entity`() {
        val entityPayload = File(entityPayloadFile!!.file).inputStream().readBytes().toString(Charsets.UTF_8)

        stubFor(
            post(urlMatching("/ngsi-ld/v1/entities"))
                .willReturn(
                    created()
                        .withHeader("Location", "/ngsi-ld/v1/entities/urn:ngsi-ld:Building:01")
                )
        )

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken()
        ).thenReturn("token".right())
        val entityService = EntityService("http://localhost:8089", mockedAuthUtils)

        val response = entityService.create(entityPayload)

        assertTrue(response.isRight())
        assertTrue(response.exists { it == "/ngsi-ld/v1/entities/urn:ngsi-ld:Building:01" })
    }

    @Test
    fun `it should return a left AlreadyExists if the entity already exists`() {
        val entityPayload = File(entityPayloadFile!!.file).inputStream().readBytes().toString(Charsets.UTF_8)

        stubFor(
            post(urlMatching("/ngsi-ld/v1/entities"))
                .willReturn(aResponse().withStatus(409))
        )

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken()
        ).thenReturn("token".right())
        val entityService = EntityService("http://localhost:8089", mockedAuthUtils)

        val response = entityService.create(entityPayload)

        assertTrue(response.isLeft())
        assertEquals(response, AlreadyExists("Entity already exists").left())
    }

    @Test
    fun `it should query entities`() {
        val firstEntity = gimmeNgsildEntity("urn:ngsi-ld:Sensor:01".toUri()!!, "Sensor", emptyMap())
        val secondEntity = gimmeNgsildEntity("urn:ngsi-ld:Sensor:02".toUri()!!, "Sensor", emptyMap())
        stubFor(
            get(urlMatching("/ngsi-ld/v1/entities"))
                .willReturn(ok().withBody(serializeObject(listOf(firstEntity, secondEntity))))
        )

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken()
        ).thenReturn("token".right())
        val entityService = EntityService("http://localhost:8089", mockedAuthUtils)

        val response = entityService.query(emptyMap(), coreContext)

        assertTrue(response.isRight())
        assertTrue(response.exists { it.size == 2 })
    }

    @Test
    fun `it should return an empty array if requested entities does not exist`() {
        stubFor(
            get(urlMatching("/ngsi-ld/v1/entities"))
                .willReturn(ok().withBody(emptyList<NgsildEntity>().toString()))
        )

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken()
        ).thenReturn("token".right())
        val entityService = EntityService("http://localhost:8089", mockedAuthUtils)

        val response = entityService.query(emptyMap(), coreContext)

        assertTrue(response.isRight())
        assertEquals(response, emptyList<NgsildEntity>().right())
    }

    @Test
    fun `it should return a left AccessTokenNotRetrieved from authUtils if no access token was retrieved`() {

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken()
        ).thenReturn(AccessTokenNotRetrieved("Unable to get an access token").left())
        val entityService = EntityService("http://localhost:8089", mockedAuthUtils)

        val response = entityService.query(emptyMap(), coreContext)

        assertTrue(response.isLeft())
        assertEquals(response, AccessTokenNotRetrieved("Unable to get an access token").left())
    }

    @Test
    fun `it should retrieve an entity`() {
        val entity = gimmeNgsildEntity("urn:ngsi-ld:Sensor:01".toUri()!!, "Sensor", emptyMap())
        stubFor(
            get(urlMatching("/ngsi-ld/v1/entities/urn:ngsi-ld:Sensor:01"))
                .willReturn(ok().withBody(serializeObject(entity)))
        )

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken()
        ).thenReturn("token".right())
        val entityService = EntityService("http://localhost:8089", mockedAuthUtils)

        val response = entityService.retrieve(
            "urn:ngsi-ld:Sensor:01".toUri()!!,
            emptyMap(),
            coreContext
        )

        assertTrue(response.isRight())
        assertTrue(response.exists { it["id"] == "urn:ngsi-ld:Sensor:01" })
    }

    @Test
    fun `it should return a left ResourceNotFound if the entity is not found`() {
        stubFor(
            get(urlMatching("/ngsi-ld/v1/entities/urn:ngsi-ld:Sensor:01"))
                .willReturn(notFound())
        )

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken()
        ).thenReturn("token".right())
        val entityService = EntityService("http://localhost:8089", mockedAuthUtils)

        val response = entityService.retrieve(
            "urn:ngsi-ld:Sensor:01".toUri()!!,
            emptyMap(),
            coreContext
        )

        assertTrue(response.isLeft())
        assertEquals(response, ResourceNotFound("Entity not found").left())
    }

    @Test
    fun `it should update entity attributes`() {
        val entityAttributesUpdatePayload = File(entityAttributesUpdatePayloadFile!!.file)
            .inputStream().readBytes().toString(Charsets.UTF_8)

        stubFor(
            patch(urlMatching("/ngsi-ld/v1/entities/urn:ngsi-ld:Building:01/attrs"))
                .willReturn(noContent())
        )

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken()
        ).thenReturn("token".right())
        val entityService = EntityService("http://localhost:8089", mockedAuthUtils)

        val response = entityService.updateAttributes(
            "urn:ngsi-ld:Building:01".toUri()!!,
            entityAttributesUpdatePayload,
            coreContext
        )

        assertTrue(response.isRight())
    }

    @Test
    fun `it should return a left ContextBrokerError if attributes were not updated`() {
        val entityAttributesUpdatePayload = File(entityAttributesUpdatePayloadFile!!.file)
            .inputStream().readBytes().toString(Charsets.UTF_8)

        stubFor(
            patch(urlMatching("/ngsi-ld/v1/entities/urn:ngsi-ld:Building:01/attrs"))
                .willReturn(notFound())
        )

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken()
        ).thenReturn("token".right())
        val entityService = EntityService("http://localhost:8089", mockedAuthUtils)

        val response = entityService.updateAttributes(
            "urn:ngsi-ld:Building:01".toUri()!!,
            entityAttributesUpdatePayload,
            coreContext
        )

        assertTrue(response.isLeft())
    }

    @Test
    fun `it should append a single entity attribute`() {
        val ngsiLdAttribute = listOf(
            NgsiLdAttributeNG(
                "volume",
                mapOf(
                    "type" to "Property",
                    "value" to 2.0
                )
            )
        )

        stubFor(
            post(urlMatching("/ngsi-ld/v1/entities/urn:ngsi-ld:Building:01/attrs"))
                .willReturn(noContent())
        )

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken()
        ).thenReturn("token".right())
        val entityService = EntityService("http://localhost:8089", mockedAuthUtils)

        val response = entityService.appendAttributes(
            "urn:ngsi-ld:Building:01".toUri()!!,
            ngsiLdAttribute,
            coreContext
        )

        assertTrue(response.isRight())

        verify(postRequestedFor(urlEqualTo("/ngsi-ld/v1/entities/urn:ngsi-ld:Building:01/attrs"))
            .withRequestBody(
                equalToJson(
                    """
                    {
                        "volume": {
                            "type": "Property",
                            "value": 2.0
                        }
                    }
                    """.trimIndent()
                )
            )
        )
    }

    @Test
    fun `it should append a multi-instance entity attribute`() {
        val ngsiLdAttribute = listOf(
            NgsiLdAttributeNG(
                "volume",
                mapOf(
                    "type" to "Property",
                    "value" to 2.0
                )
            ),
            NgsiLdAttributeNG(
                "volume",
                mapOf(
                    "type" to "Property",
                    "value" to 2.0,
                    "datasetId" to "urn:ngsi-ld:Dataset:123"
                )
            )
        )

        stubFor(
            post(urlMatching("/ngsi-ld/v1/entities/urn:ngsi-ld:Building:01/attrs"))
                .willReturn(noContent())
        )

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken()
        ).thenReturn("token".right())
        val entityService = EntityService("http://localhost:8089", mockedAuthUtils)

        val response = entityService.appendAttributes(
            "urn:ngsi-ld:Building:01".toUri()!!,
            ngsiLdAttribute,
            coreContext
        )

        assertTrue(response.isRight())

        verify(postRequestedFor(urlEqualTo("/ngsi-ld/v1/entities/urn:ngsi-ld:Building:01/attrs"))
            .withRequestBody(
                equalToJson(
                    """
                    {
                      "volume": [{
                        "type": "Property",
                        "value": 2.0
                      }, {
                        "type": "Property",
                        "value": 2.0,
                        "datasetId": "urn:ngsi-ld:Dataset:123"
                      }]
                    }
                    """.trimIndent()
                )
            )
        )
    }

    @Test
    fun `it should append two single instance entity attributes`() {
        val ngsiLdAttribute = listOf(
            NgsiLdAttributeNG(
                "volume",
                mapOf(
                    "type" to "Property",
                    "value" to 2.0
                )
            ),
            NgsiLdAttributeNG(
                "pressure",
                mapOf(
                    "type" to "Property",
                    "value" to 23.0
                )
            )
        )

        stubFor(
            post(urlMatching("/ngsi-ld/v1/entities/urn:ngsi-ld:Building:01/attrs"))
                .willReturn(noContent())
        )

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken()
        ).thenReturn("token".right())
        val entityService = EntityService("http://localhost:8089", mockedAuthUtils)

        val response = entityService.appendAttributes(
            "urn:ngsi-ld:Building:01".toUri()!!,
            ngsiLdAttribute,
            coreContext
        )

        assertTrue(response.isRight())

        verify(postRequestedFor(urlEqualTo("/ngsi-ld/v1/entities/urn:ngsi-ld:Building:01/attrs"))
            .withRequestBody(
                equalToJson(
                    """
                    {
                        "volume": {
                            "type": "Property",
                            "value": 2.0
                        },
                        "pressure": {
                            "type": "Property",
                            "value": 23.0
                        }                        
                    }
                    """.trimIndent()
                )
            )
        )
    }

    @Test
    fun `it should partially update an entity attribute`() {
        val now = ZonedDateTime.parse("2021-08-12T09:44:00.80868Z")
        val partialPropery = NgsiLdPropertyBuilder("temperature")
            .withValue(2.1)
            .withObservedAt(now)
            .build()

        stubFor(
            patch(urlMatching("/ngsi-ld/v1/entities/urn:ngsi-ld:Building:01/attrs/temperature"))
                .willReturn(noContent())
        )

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken()
        ).thenReturn("token".right())
        val entityService = EntityService("http://localhost:8089", mockedAuthUtils)

        val response = entityService.partialAttributeUpdate(
            "urn:ngsi-ld:Building:01".toUri()!!,
            "temperature",
            partialPropery.propertyValue,
            coreContext
        )

        assertTrue(response.isRight())

        verify(
            patchRequestedFor(urlEqualTo("/ngsi-ld/v1/entities/urn:ngsi-ld:Building:01/attrs/temperature"))
            .withRequestBody(
                equalToJson(
                    """
                    {
                        "value": 2.1,
                        "observedAt": "2021-08-12T09:44:00.80868Z" 
                    }
                    """.trimIndent()
                )
            )
        )
    }

    private fun gimmeNgsildEntity(id: URI, type: String, attributes: Map<String, Any>): NgsildEntity =
        mapOf(
            "id" to id,
            "type" to type
        ).plus(attributes)
}
