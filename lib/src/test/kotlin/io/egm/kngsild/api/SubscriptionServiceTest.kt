package io.egm.kngsild.api

import arrow.core.left
import arrow.core.right
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.configureFor
import com.github.tomakehurst.wiremock.client.WireMock.created
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.reset
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.egm.kngsild.model.AccessTokenNotRetrieved
import io.egm.kngsild.model.AlreadyExists
import io.egm.kngsild.utils.AuthUtils
import io.egm.kngsild.utils.HttpUtils
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
            mockedAuthUtils.getToken(
                any(String::class.java),
                any(String::class.java),
                any(String::class.java),
                any(String::class.java),
            )
        ).thenReturn("token".right())
        val subscriptionService = SubscriptionService(HttpUtils(), mockedAuthUtils)

        val response = subscriptionService.create(
            "http://localhost:8089",
            "http://localhost:8090",
            "client_id",
            "client_secret",
            "client_credentials",
            subscriptionPayload
        )

        assertTrue(response.isRight())
        assertTrue(response.exists { it.statusCode() == HttpURLConnection.HTTP_CREATED })
    }

    @Test
    fun `it should return AlreadyExists exception when the subscription already exists`() {
        val subscriptionPayload = File(subscriptionPayloadFile!!.file)
            .inputStream().readBytes().toString(Charsets.UTF_8)

        stubFor(
            post(urlMatching("/ngsi-ld/v1/subscriptions"))
                .willReturn(aResponse().withStatus(409))
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
        val subscriptionService = SubscriptionService(HttpUtils(), mockedAuthUtils)

        val response = subscriptionService.create(
            "http://localhost:8089",
            "http://localhost:8090",
            "client_id",
            "client_secret",
            "client_credentials",
            subscriptionPayload
        )

        assertTrue(response.isLeft())
        assertEquals(response, AlreadyExists("Subscription already exists").left())
    }

    @Test
    fun `it should return AccessTokenNotRetrieved exception from authUtils if no access token was retrieved`() {
        val subscriptionPayload = File(subscriptionPayloadFile!!.file)
            .inputStream().readBytes().toString(Charsets.UTF_8)

        val mockedAuthUtils = mock(AuthUtils::class.java)
        `when`(
            mockedAuthUtils.getToken(
                any(String::class.java),
                any(String::class.java),
                any(String::class.java),
                any(String::class.java),
            )
        ).thenReturn(AccessTokenNotRetrieved("Unable to get an access token").left())
        val subscriptionService = SubscriptionService(HttpUtils(), mockedAuthUtils)

        val response = subscriptionService.create(
            "http://localhost:8089",
            "http://localhost:8090",
            "client_id",
            "client_secret",
            "client_credentials",
            subscriptionPayload
        )

        assertTrue(response.isLeft())
        assertEquals(response, AccessTokenNotRetrieved("Unable to get an access token").left())
    }

    private fun <T> any(type: Class<T>): T = Mockito.any<T>(type)
}
