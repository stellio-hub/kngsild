package io.egm.kngsild.utils

import arrow.core.left
import arrow.core.right
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.configureFor
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.reset
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.egm.kngsild.model.AccessTokenNotRetrieved
import io.egm.kngsild.utils.JsonUtils.serializeObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthUtilsTest {

    private lateinit var wireMockServer: WireMockServer

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
    fun `it should return an access token`() {
        stubFor(
            post(urlMatching("/auth"))
                .willReturn(
                    ok().withBody(serializeObject(mapOf("access_token" to "token")))
                )
        )

        val response = AuthUtils().getToken(
            "http://localhost:8089/auth",
            "client_id",
            "client_secret",
            "client_credentials"
        )

        assertTrue(response.isRight())
        assertEquals(response, "token".right())
    }

    @Test
    fun `it should return a left AccessTokenNotRetrieved if no access token was retrieved`() {
        stubFor(
            post(urlMatching("/auth"))
                .willReturn(ok().withBody(serializeObject(emptyMap<String, Any>())))
        )

        val response = AuthUtils().getToken(
            "http://localhost:8089/auth",
            "client_id",
            "client_secret",
            "client_credentials"
        )

        assertTrue(response.isLeft())
        assertEquals(response, AccessTokenNotRetrieved("Unable to get an access token").left())
    }
}