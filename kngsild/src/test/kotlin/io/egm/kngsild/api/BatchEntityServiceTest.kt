package io.egm.kngsild.api

import arrow.core.left
import arrow.core.right
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.egm.kngsild.model.AccessTokenNotRetrieved
import io.egm.kngsild.utils.AuthUtils
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File
import java.net.HttpURLConnection

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BatchEntityServiceTest {

    private lateinit var wireMockServer: WireMockServer

    private val batchEntityCreatePayloadFile = javaClass.classLoader
        .getResource("ngsild/entities/batch/entities_create.jsonld")
    private val batchEntityUpsertPayloadFile = javaClass.classLoader
        .getResource("ngsild/entities/batch/entities_upsert.jsonld")
    private val batchEntityDeletePayloadFile = javaClass.classLoader
        .getResource("ngsild/entities/batch/entities_delete.json")

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
    fun `it should create a batch of entities`() {
        val batchEntityPayload = File(batchEntityCreatePayloadFile!!.file)
            .inputStream().readBytes().toString(Charsets.UTF_8)

        stubFor(
            post(urlMatching("/ngsi-ld/v1/entityOperations/create"))
                .willReturn(created())
        )

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken()
        ).thenReturn("token".right())
        val batchEntityService = BatchEntityService("http://localhost:8089", mockedAuthUtils)

        val response = batchEntityService.create(batchEntityPayload)

        assertTrue(response.isRight { it.statusCode() == HttpURLConnection.HTTP_CREATED })
    }

    @Test
    fun `it should return a left ContextBrokerError if entities were not created`() {
        val batchEntityPayload = File(batchEntityCreatePayloadFile!!.file)
            .inputStream().readBytes().toString(Charsets.UTF_8)

        stubFor(
            post(urlMatching("/ngsi-ld/v1/entityOperations/create"))
                .willReturn(badRequest())
        )

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken()
        ).thenReturn("token".right())
        val batchEntityService = BatchEntityService("http://localhost:8089", mockedAuthUtils)

        val response = batchEntityService.create(batchEntityPayload)

        assertTrue(response.isLeft())
    }

    @Test
    fun `it should return a left AccessTokenNotRetrieved from authUtils if no access token was retrieved`() {
        val batchEntityPayload = File(batchEntityCreatePayloadFile!!.file)
            .inputStream().readBytes().toString(Charsets.UTF_8)

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken()
        ).thenReturn(AccessTokenNotRetrieved("Unable to get an access token").left())
        val batchEntityService = BatchEntityService("http://localhost:8089", mockedAuthUtils)

        val response = batchEntityService.create(batchEntityPayload)

        assertTrue(response.isLeft())
        assertEquals(response, AccessTokenNotRetrieved("Unable to get an access token").left())
    }

    @Test
    fun `it should upsert a batch of entities`() {
        val batchEntityPayload = File(batchEntityUpsertPayloadFile!!.file)
            .inputStream().readBytes().toString(Charsets.UTF_8)

        stubFor(
            post(urlMatching("/ngsi-ld/v1/entityOperations/upsert"))
                .willReturn(created())
        )

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken()
        ).thenReturn("token".right())
        val batchEntityService = BatchEntityService("http://localhost:8089", mockedAuthUtils)

        val response = batchEntityService.upsert(batchEntityPayload, emptyMap())

        assertTrue(response.isRight { it.statusCode() == HttpURLConnection.HTTP_CREATED })
    }

    @Test
    fun `it should return a left ContextBrokerError if entities were not upserted`() {
        val batchEntityPayload = File(batchEntityUpsertPayloadFile!!.file)
            .inputStream().readBytes().toString(Charsets.UTF_8)

        stubFor(
            post(urlMatching("/ngsi-ld/v1/entityOperations/upsert"))
                .willReturn(badRequest())
        )

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken()
        ).thenReturn("token".right())
        val batchEntityService = BatchEntityService("http://localhost:8089", mockedAuthUtils)

        val response = batchEntityService.upsert(batchEntityPayload, emptyMap())

        assertTrue(response.isLeft())
    }

    @Test
    fun `it should return a left AccessTokenNotRetrieved from authUtils if no access token was retrieved for upsert`() {
        val batchEntityPayload = File(batchEntityUpsertPayloadFile!!.file)
            .inputStream().readBytes().toString(Charsets.UTF_8)

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken()
        ).thenReturn(AccessTokenNotRetrieved("Unable to get an access token").left())
        val batchEntityService = BatchEntityService("http://localhost:8089", mockedAuthUtils)

        val response = batchEntityService.upsert(batchEntityPayload, emptyMap())

        assertTrue(response.isLeft())
        assertEquals(response, AccessTokenNotRetrieved("Unable to get an access token").left())
    }

    @Test
    fun `it should delete a batch of entities`() {
        val batchEntityPayload = File(batchEntityDeletePayloadFile!!.file)
            .inputStream().readBytes().toString(Charsets.UTF_8)

        stubFor(
            post(urlMatching("/ngsi-ld/v1/entityOperations/delete"))
                .willReturn(noContent())
        )

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken()
        ).thenReturn("token".right())
        val batchEntityService = BatchEntityService("http://localhost:8089", mockedAuthUtils)

        val response = batchEntityService.delete(batchEntityPayload)

        assertTrue(response.isRight { it.statusCode() == HttpURLConnection.HTTP_NO_CONTENT })
    }

    @Test
    fun `it should return a left ContextBrokerError if entities were not deleted`() {
        val batchEntityPayload = File(batchEntityDeletePayloadFile!!.file)
            .inputStream().readBytes().toString(Charsets.UTF_8)

        stubFor(
            post(urlMatching("/ngsi-ld/v1/entityOperations/delete"))
                .willReturn(badRequest())
        )

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken()
        ).thenReturn("token".right())
        val batchEntityService = BatchEntityService("http://localhost:8089", mockedAuthUtils)

        val response = batchEntityService.delete(batchEntityPayload)

        assertTrue(response.isLeft())
    }
}
