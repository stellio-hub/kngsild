package io.egm.kngsild.api

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.egm.kngsild.model.ApplicationError
import io.egm.kngsild.model.ContextBrokerError
import io.egm.kngsild.utils.AuthUtils
import io.egm.kngsild.utils.HttpUtils.APPLICATION_JSON
import io.egm.kngsild.utils.HttpUtils.APPLICATION_JSONLD
import io.egm.kngsild.utils.HttpUtils.httpClient
import io.egm.kngsild.utils.HttpUtils.httpLinkHeaderBuilder
import io.egm.kngsild.utils.HttpUtils.paramsUrlBuilder
import io.egm.kngsild.utils.JsonUtils
import io.egm.kngsild.utils.NgsildEntity
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class EntityService(
    private val authUtils: AuthUtils
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val patchSuccessCode = listOf(HttpURLConnection.HTTP_NO_CONTENT)

    fun query(
        brokerUrl: String,
        authServerUrl: String,
        authClientId: String,
        authClientSecret: String,
        authGrantType: String,
        queryParams: Map<String, String>,
        contextUrl: String
    ): Either<ApplicationError, List<NgsildEntity>> {
        val params: String = paramsUrlBuilder(queryParams)
        return authUtils.getToken(authServerUrl, authClientId, authClientSecret, authGrantType).flatMap {
            val request = HttpRequest
                .newBuilder()
                .uri(
                    URI.create("$brokerUrl/ngsi-ld/v1/entities$params")
                )
                .setHeader("Accept", APPLICATION_JSONLD)
                .setHeader("Link", httpLinkHeaderBuilder(contextUrl))
                .setHeader("Authorization", "Bearer $it")
                .GET().build()

            try {
                logger.debug("Issuing query: /ngsi-ld/v1/entities$params")
                val httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                logger.debug("Http response status code: ${httpResponse.statusCode()}")
                logger.debug("Http response body: ${httpResponse.body()}")

                val response: List<Any> = JsonUtils.mapper.readValue(
                    httpResponse.body(),
                    JsonUtils.mapper.typeFactory.constructCollectionType(List::class.java, Any::class.java)
                )

                val res = response as List<NgsildEntity>
                res.right()
            } catch (e: IOException) {
                val errorMessage = e.message ?: "Error encountered while processing GET request"
                ContextBrokerError(errorMessage).left()
            }
        }
    }

    fun updateAttributes(
        brokerUrl: String,
        authServerUrl: String,
        authClientId: String,
        authClientSecret: String,
        authGrantType: String,
        entityId: URI,
        attributesPayload: String,
        contextUrl: String
    ): Either<ApplicationError, String?> {
        return authUtils.getToken(authServerUrl, authClientId, authClientSecret, authGrantType).flatMap {
            val request = HttpRequest
                .newBuilder()
                .uri(
                    URI
                        .create("$brokerUrl/ngsi-ld/v1/entities/$entityId/attrs")
                )
                .method("PATCH", HttpRequest.BodyPublishers.ofString(attributesPayload))
                .setHeader("Content-Type", APPLICATION_JSON)
                .setHeader("Accept", APPLICATION_JSON)
                .setHeader("Link", httpLinkHeaderBuilder(contextUrl))
                .setHeader("Authorization", "Bearer $it")
                .build()
            return try {
                logger.debug("Patching entity $entityId with payload $attributesPayload")
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                logger.debug("Http response status code: ${response.statusCode()}")
                logger.debug("Http response body: ${response.body()}")
                if (patchSuccessCode.contains(response.statusCode()))
                    response.body().right()
                else
                    ContextBrokerError(
                        "Received ${response.statusCode()} (${response.body()}) from context broker"
                    ).left()
            } catch (e: IOException) {
                logger.warn(e.message ?: "Error encountered while processing PATCH request")
                val errorMessage = e.message ?: "Error encountered while patching to context broker"
                ContextBrokerError(errorMessage).left()
            }
        }
    }
}
