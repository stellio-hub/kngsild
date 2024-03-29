package io.egm.kngsild.api

import arrow.core.left
import arrow.core.right
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.egm.kngsild.model.AccessTokenNotRetrieved
import io.egm.kngsild.model.AlreadyExists
import io.egm.kngsild.utils.AuthUtils
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File
import java.net.HttpURLConnection

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SubscriptionServiceTest {

    private lateinit var wireMockServer: WireMockServer
    private val subscriptionPayloadFile = javaClass.classLoader
        .getResource("ngsild/subscriptions/subscription.json")

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
    fun `it should create a subscription`() {
        val subscriptionPayload = File(subscriptionPayloadFile!!.file)
            .inputStream().readBytes().toString(Charsets.UTF_8)

        stubFor(
            post(urlMatching("/ngsi-ld/v1/subscriptions"))
                .willReturn(created())
        )

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken()
        ).thenReturn("token".right())
        val subscriptionService = SubscriptionService("http://localhost:8089", mockedAuthUtils)

        val response = subscriptionService.create(subscriptionPayload)

        assertTrue(response.isRight { it.statusCode() == HttpURLConnection.HTTP_CREATED })
    }

    @Test
    fun `it should return a left AlreadyExists if the subscription already exists`() {
        val subscriptionPayload = File(subscriptionPayloadFile!!.file)
            .inputStream().readBytes().toString(Charsets.UTF_8)

        stubFor(
            post(urlMatching("/ngsi-ld/v1/subscriptions"))
                .willReturn(aResponse().withStatus(409))
        )

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken()
        ).thenReturn("token".right())
        val subscriptionService = SubscriptionService("http://localhost:8089", mockedAuthUtils)

        val response = subscriptionService.create(subscriptionPayload)

        assertTrue(response.isLeft())
        assertEquals(response, AlreadyExists("Subscription already exists").left())
    }

    @Test
    fun `it should return a left AccessTokenNotRetrieved from authUtils if no access token was retrieved`() {
        val subscriptionPayload = File(subscriptionPayloadFile!!.file)
            .inputStream().readBytes().toString(Charsets.UTF_8)

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken()
        ).thenReturn(AccessTokenNotRetrieved("Unable to get an access token").left())
        val subscriptionService = SubscriptionService("http://localhost:8089", mockedAuthUtils)

        val response = subscriptionService.create(subscriptionPayload)

        assertTrue(response.isLeft())
        assertEquals(response, AccessTokenNotRetrieved("Unable to get an access token").left())
    }
}
