package io.egm.kngsild.api

import arrow.core.left
import arrow.core.right
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.configureFor
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.noContent
import com.github.tomakehurst.wiremock.client.WireMock.notFound
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.reset
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.egm.kngsild.model.AccessTokenNotRetrieved
import io.egm.kngsild.model.AlreadyExists
import io.egm.kngsild.model.ResourceNotFound
import io.egm.kngsild.utils.AuthUtils
import io.egm.kngsild.utils.JsonUtils.serializeObject
import io.egm.kngsild.utils.NgsildEntity
import io.egm.kngsild.utils.NgsildUtils.coreContext
import io.egm.kngsild.utils.UriUtils.toUri
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

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
            WireMock.post(urlMatching("/ngsi-ld/v1/entities"))
                .willReturn(WireMock.created())
        )

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken(
                any(String::class.java),
                any(String::class.java),
                any(String::class.java),
                any(String::class.java),
            )
        ).thenReturn("token".right())
        val entityService = EntityService(mockedAuthUtils)

        val response = entityService.create(
            "http://localhost:8089",
            "http://localhost:8090",
            "client_id",
            "client_secret",
            "client_credentials",
            entityPayload
        )

        assertTrue(response.isRight())
        assertTrue(response.exists { it.statusCode() == HttpURLConnection.HTTP_CREATED })
    }

    @Test
    fun `it should return a left AlreadyExists if the entity already exists`() {
        val entityPayload = File(entityPayloadFile!!.file).inputStream().readBytes().toString(Charsets.UTF_8)

        stubFor(
            WireMock.post(urlMatching("/ngsi-ld/v1/entities"))
                .willReturn(WireMock.aResponse().withStatus(409))
        )

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken(
                any(String::class.java),
                any(String::class.java),
                any(String::class.java),
                any(String::class.java),
            )
        ).thenReturn("token".right())
        val entityService = EntityService(mockedAuthUtils)

        val response = entityService.create(
            "http://localhost:8089",
            "http://localhost:8090",
            "client_id",
            "client_secret",
            "client_credentials",
            entityPayload
        )

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
            mockedAuthUtils.getToken(
                any(String::class.java),
                any(String::class.java),
                any(String::class.java),
                any(String::class.java),
            )
        ).thenReturn("token".right())
        val entityService = EntityService(mockedAuthUtils)

        val response = entityService.query(
            "http://localhost:8089",
            "http://localhost:8090",
            "client_id",
            "client_secret",
            "client_credentials",
            emptyMap(),
            coreContext
        )

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
            mockedAuthUtils.getToken(
                any(String::class.java),
                any(String::class.java),
                any(String::class.java),
                any(String::class.java),
            )
        ).thenReturn("token".right())
        val entityService = EntityService(mockedAuthUtils)

        val response = entityService.query(
            "http://localhost:8089",
            "http://localhost:8090",
            "client_id",
            "client_secret",
            "client_credentials",
            emptyMap(),
            coreContext
        )

        assertTrue(response.isRight())
        assertEquals(response, emptyList<NgsildEntity>().right())
    }

    @Test
    fun `it should return a left AccessTokenNotRetrieved from authUtils if no access token was retrieved`() {

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken(
                any(String::class.java),
                any(String::class.java),
                any(String::class.java),
                any(String::class.java),
            )
        ).thenReturn(AccessTokenNotRetrieved("Unable to get an access token").left())
        val entityService = EntityService(mockedAuthUtils)

        val response = entityService.query(
            "http://localhost:8089",
            "http://localhost:8090",
            "client_id",
            "client_secret",
            "client_credentials",
            emptyMap(),
            coreContext
        )

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
            mockedAuthUtils.getToken(
                any(String::class.java),
                any(String::class.java),
                any(String::class.java),
                any(String::class.java),
            )
        ).thenReturn("token".right())
        val entityService = EntityService(mockedAuthUtils)

        val response = entityService.retrieve(
            "http://localhost:8089",
            "http://localhost:8090",
            "client_id",
            "client_secret",
            "client_credentials",
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
            mockedAuthUtils.getToken(
                any(String::class.java),
                any(String::class.java),
                any(String::class.java),
                any(String::class.java),
            )
        ).thenReturn("token".right())
        val entityService = EntityService(mockedAuthUtils)

        val response = entityService.retrieve(
            "http://localhost:8089",
            "http://localhost:8090",
            "client_id",
            "client_secret",
            "client_credentials",
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
            mockedAuthUtils.getToken(
                any(String::class.java),
                any(String::class.java),
                any(String::class.java),
                any(String::class.java),
            )
        ).thenReturn("token".right())
        val entityService = EntityService(mockedAuthUtils)

        val response = entityService.updateAttributes(
            "http://localhost:8089",
            "http://localhost:8090",
            "client_id",
            "client_secret",
            "client_credentials",
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
            mockedAuthUtils.getToken(
                any(String::class.java),
                any(String::class.java),
                any(String::class.java),
                any(String::class.java),
            )
        ).thenReturn("token".right())
        val entityService = EntityService(mockedAuthUtils)

        val response = entityService.updateAttributes(
            "http://localhost:8089",
            "http://localhost:8090",
            "client_id",
            "client_secret",
            "client_credentials",
            "urn:ngsi-ld:Building:01".toUri()!!,
            entityAttributesUpdatePayload,
            coreContext
        )

        assertTrue(response.isLeft())
    }

    private fun gimmeNgsildEntity(id: URI, type: String, attributes: Map<String, Any>): NgsildEntity =
        mapOf(
            "id" to id,
            "type" to type
        ).plus(attributes)

    private fun <T> any(type: Class<T>): T = Mockito.any(type)
}
