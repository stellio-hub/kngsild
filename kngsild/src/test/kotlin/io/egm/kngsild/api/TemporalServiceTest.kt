package io.egm.kngsild.api

import arrow.core.right
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.egm.kngsild.utils.AuthUtils
import io.egm.kngsild.utils.NgsiLdAttributeNG
import io.egm.kngsild.utils.NgsiLdUtils
import io.egm.kngsild.utils.UriUtils.toUri
import io.egm.kngsild.utils.groupByProperty
import org.junit.jupiter.api.*
import org.mockito.Mockito

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TemporalServiceTest {

    private lateinit var wireMockServer: WireMockServer

    @BeforeAll
    fun beforeAll() {
        wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(8089))
        wireMockServer.start()
        // If not using the default port, we need to instruct explicitly the client (quite redundant)
        WireMock.configureFor(8089)
    }

    @AfterEach
    fun resetWiremock() {
        WireMock.reset()
    }

    @AfterAll
    fun afterAll() {
        wireMockServer.stop()
    }

    @Test
    fun `it should add attributes instances`() {
        val ngsiLdAttributes = listOf(
            NgsiLdAttributeNG(
                "volume",
                mapOf(
                    "type" to "Property",
                    "value" to 2.0,
                    "observedAt" to "2021-08-12T12:00:00Z"
                )
            ),
            NgsiLdAttributeNG(
                "volume",
                mapOf(
                    "type" to "Property",
                    "value" to 2.2,
                    "observedAt" to "2021-08-12T13:00:00Z"
                )
            )
        )

        WireMock.stubFor(
            WireMock.post(WireMock.urlMatching("/ngsi-ld/v1/temporal/entities/urn:ngsi-ld:Building:01/attrs"))
                .willReturn(WireMock.noContent())
        )

        val mockedAuthUtils = Mockito.mock(AuthUtils::class.java)
        Mockito.`when`(
            mockedAuthUtils.getToken()
        ).thenReturn("token".right())
        val temporalService = TemporalService("http://localhost:8089", mockedAuthUtils)

        val response = temporalService.addAttributes(
            "urn:ngsi-ld:Building:01".toUri()!!,
            ngsiLdAttributes.groupByProperty(),
            NgsiLdUtils.coreContext
        )

        Assertions.assertTrue(response.isRight())

        WireMock.verify(
            WireMock.postRequestedFor(WireMock.urlEqualTo("/ngsi-ld/v1/temporal/entities/urn:ngsi-ld:Building:01/attrs"))
                .withRequestBody(
                    WireMock.equalToJson(
                        """
                        {
                            "volume" : [ {
                                "type" : "Property",
                                "value" : 2.0,
                                "observedAt" : "2021-08-12T12:00:00Z"
                            }, {
                                "type" : "Property",
                                "value" : 2.2,
                                "observedAt" : "2021-08-12T13:00:00Z"
                            } ]
                        }                    
                        """.trimIndent()
                    )
                )
        )
    }
}
