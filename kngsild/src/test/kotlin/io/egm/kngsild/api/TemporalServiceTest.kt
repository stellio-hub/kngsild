package io.egm.kngsild.api

import arrow.core.right
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.egm.kngsild.utils.*
import io.egm.kngsild.utils.UriUtils.toUri
import org.junit.jupiter.api.*
import org.mockito.Mockito
import java.net.URI

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
            WireMock.postRequestedFor(
                WireMock.urlEqualTo("/ngsi-ld/v1/temporal/entities/urn:ngsi-ld:Building:01/attrs")
            )
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

    @Test
    fun `it should retrieve temporal data of an entityId`() {
        val entityId = gimmeNgsildEntity("urn:ngsi-ld:Sensor:01".toUri()!!, emptyMap())
        WireMock.stubFor(
            WireMock.get(WireMock.urlMatching("/ngsi-ld/v1/temporal/entities/urn:ngsi-ld:Sensor:01"))
                .willReturn(WireMock.ok().withBody(JsonUtils.serializeObject(entityId)))
        )

        val mockedAuthUtils = Mockito.mock(AuthUtils::class.java)
        Mockito.`when`(
            mockedAuthUtils.getToken()
        ).thenReturn("token".right())
        val temporalService = TemporalService("http://localhost:8089", mockedAuthUtils)

        val response = temporalService.retrieve(
            "urn:ngsi-ld:Sensor:01".toUri()!!,
            emptyMap(),
            NgsiLdUtils.coreContext
        )

        Assertions.assertTrue(response.isRight())
        Assertions.assertTrue(response.exists { it["id"] == "urn:ngsi-ld:Sensor:01" })
    }

    private fun gimmeNgsildEntity(id: URI, attributes: Map<String, Any>): NgsildEntity =
        mapOf(
            "id" to id
        ).plus(attributes)
}
