package io.egm.kngsild.api

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.egm.kngsild.model.ApplicationError
import io.egm.kngsild.model.ContextBrokerError
import io.egm.kngsild.utils.*
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
        contextUrl: String
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
                .build()
            return try {
                logger.debug("Appending attributes $serializedPayload to entity $entityId")
                val response = HttpUtils.httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                logger.debug("Http response status code: ${response.statusCode()}")
                logger.debug("Http response body: ${response.body()}")
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
}
