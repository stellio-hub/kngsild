package io.egm.kngsild.api

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.egm.kngsild.model.ApplicationError
import io.egm.kngsild.model.ContextBrokerError
import io.egm.kngsild.model.ResourceNotFound
import io.egm.kngsild.utils.*
import io.egm.kngsild.utils.HttpUtils.DEFAULT_TENANT_NAME
import io.egm.kngsild.utils.HttpUtils.NGSILD_TENANT_HEADER
import io.egm.kngsild.utils.JsonUtils.serializeObject
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class TemporalService(
    private val contextBrokerUrl: String,
    private val authUtils: AuthUtils
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val temporalApiRootPath = "/ngsi-ld/v1/temporal/entities"

    fun addAttributes(
        entityId: URI,
        ngsiLdTemporalAttributesInstances: NgsiLdTemporalAttributesInstances,
        contextUrl: String,
        tenantName: String? = DEFAULT_TENANT_NAME
    ): Either<ApplicationError, String> {
        return authUtils.getToken().flatMap { token ->
            val serializedPayload = serializeObject(ngsiLdTemporalAttributesInstances)
            val request = HttpRequest
                .newBuilder()
                .uri(URI.create("$contextBrokerUrl$temporalApiRootPath/$entityId/attrs"))
                .method("POST", HttpRequest.BodyPublishers.ofString(serializedPayload))
                .setHeader("Content-Type", HttpUtils.APPLICATION_JSON)
                .setHeader("Accept", HttpUtils.APPLICATION_JSON)
                .setHeader("Link", HttpUtils.httpLinkHeaderBuilder(contextUrl))
                .setHeader("Authorization", "Bearer $token")
                .setHeader(NGSILD_TENANT_HEADER, tenantName)
                .build()
            return try {
                logger.debug(
                    "Appending ${ngsiLdTemporalAttributesInstances.size} attributes: " +
                        "$contextBrokerUrl$temporalApiRootPath/$entityId/attrs"
                )
                logger.trace("Appending attributes {} to entity {}", serializedPayload, entityId)
                val response = HttpUtils.httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                logger.debug("Http response body: ${response.body()} (${response.statusCode()})")
                if (HttpURLConnection.HTTP_NO_CONTENT == response.statusCode())
                    response.body().right()
                else
                    ContextBrokerError(
                        "Received ${response.statusCode()} (${response.body()}) from context broker"
                    ).left()
            } catch (e: IOException) {
                logger.warn(e.message ?: "Error encountered while processing POST request")
                val errorMessage = e.message ?: "Error encountered while posting to context broker"
                ContextBrokerError(errorMessage).left()
            }
        }
    }

    fun retrieve(
        entityId: URI,
        queryParams: Map<String, String>,
        contextUrl: String,
        tenantName: String? = DEFAULT_TENANT_NAME
    ): Either<ApplicationError, NgsildEntity> {
        val params: String = HttpUtils.paramsUrlBuilder(queryParams)
        return authUtils.getToken().flatMap {
            val request = HttpRequest
                .newBuilder()
                .uri(
                    URI.create("$contextBrokerUrl$temporalApiRootPath/$entityId$params")
                )
                .setHeader("Accept", HttpUtils.APPLICATION_JSONLD)
                .setHeader("Link", HttpUtils.httpLinkHeaderBuilder(contextUrl))
                .setHeader("Authorization", "Bearer $it")
                .setHeader(NGSILD_TENANT_HEADER, tenantName)
                .GET().build()

            try {
                logger.debug("Issuing retrieve: {}{}/{}{}", contextBrokerUrl, temporalApiRootPath, entityId, params)
                val httpResponse = HttpUtils.httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                logger.debug("Http response status code: ${httpResponse.statusCode()}")
                logger.trace("Http response body: ${httpResponse.body()}")

                if (httpResponse.statusCode() == HttpURLConnection.HTTP_OK)
                    JsonUtils.deserializeObject(httpResponse.body()).right()
                else if (httpResponse.statusCode() == HttpURLConnection.HTTP_NOT_FOUND)
                    ResourceNotFound("Entity not found").left()
                else ContextBrokerError(
                    "Failed to retrieve entity, " +
                        "received ${httpResponse.statusCode()} (${httpResponse.body()}) from context broker"
                ).left()
            } catch (e: IOException) {
                val errorMessage = e.message ?: "Error encountered while processing GET request"
                ContextBrokerError(errorMessage).left()
            }
        }
    }
}
