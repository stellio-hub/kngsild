package io.egm.kngsild.api.entities

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.egm.kngsild.model.ApplicationError
import io.egm.kngsild.model.ContextBrokerError
import io.egm.kngsild.model.NgsildEntity
import io.egm.kngsild.model.ResourceNotFound
import io.egm.kngsild.utils.HttpUtils
import io.egm.kngsild.utils.JsonUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class EntityService(
    private val httpUtils: HttpUtils
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun query(
        brokerUrl: String,
        authServerUrl: String,
        authClientId: String,
        authClientSecret: String,
        authGrantType: String,
        queryParams: Map<String, String>,
        contextUrl: String
    ): Either<ApplicationError, List<NgsildEntity>> {
        val params: String = httpUtils.paramsUrlBuilder(queryParams)
        return httpUtils.getToken(authServerUrl, authClientId, authClientSecret, authGrantType).flatMap {
            val request = HttpRequest
                .newBuilder()
                .uri(
                    URI.create("$brokerUrl/ngsi-ld/v1/entities$params")
                )
                .setHeader("Accept", "application/ld+json")
                .setHeader("Link", httpUtils.httpLinkHeaderBuilder(contextUrl))
                .setHeader("Authorization", "Bearer $it")
                .GET().build()

            try {
                logger.debug("Issuing query: /ngsi-ld/v1/entities$params")
                val httpResponse = httpUtils.httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                logger.debug("Http response status code: ${httpResponse.statusCode()}")
                logger.debug("Http response body: ${httpResponse.body()}")

                val response: List<Any> = JsonUtils.mapper.readValue(
                    httpResponse.body(),
                    JsonUtils.mapper.typeFactory.constructCollectionType(List::class.java, Any::class.java)
                )
                if (response.isNotEmpty()) {
                    val res = response as List<NgsildEntity>
                    res.right()
                } else {
                    logger.warn("Could not find requested entities")
                    ResourceNotFound("Could not find requested entities").left()
                }
            } catch (e: IOException) {
                val errorMessage = e.message ?: "Error encountered while processing GET request"
                ContextBrokerError(errorMessage).left()
            }
        }
    }
}
