package io.egm.kngsild.api

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.egm.kngsild.model.ApplicationError
import io.egm.kngsild.model.ContextBrokerError
import io.egm.kngsild.utils.AuthUtils
import io.egm.kngsild.utils.HttpUtils.httpClient
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class BatchEntityService(
    private val authUtils: AuthUtils
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val batchCreationSuccessCodes = listOf(HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_CREATED)
    private val batchUpsertSuccessCodes = listOf(
        HttpURLConnection.HTTP_CREATED,
        HttpURLConnection.HTTP_NO_CONTENT
    )

    fun create(
        brokerUrl: String,
        authServerUrl: String,
        authClientId: String,
        authClientSecret: String,
        authGrantType: String,
        payload: String
    ): Either<ApplicationError, HttpResponse<String>> {
        return authUtils.getToken(authServerUrl, authClientId, authClientSecret, authGrantType).flatMap {
            val request = HttpRequest.newBuilder().uri(
                URI.create("$brokerUrl/ngsi-ld/v1/entityOperations/create")
            )
                .setHeader("Content-Type", "application/ld+json")
                .setHeader("Authorization", "Bearer $it")
                .POST(HttpRequest.BodyPublishers.ofString(payload)).build()

            try {
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                logger.debug("Http response status code: ${response.statusCode()}")
                logger.debug("Http response body: ${response.body()}")
                if (batchCreationSuccessCodes.contains(response.statusCode()))
                    response.right()
                else
                    ContextBrokerError(
                        "Received ${response.statusCode()} (${response.body()}) from context broker"
                    ).left()
            } catch (e: IOException) {
                val errorMessage = e.message ?: "Error encountered while sending entities to context broker"
                ContextBrokerError(errorMessage).left()
            }
        }
    }

    fun upsert(
        brokerUrl: String,
        authServerUrl: String,
        authClientId: String,
        authClientSecret: String,
        authGrantType: String,
        payload: String
    ): Either<ApplicationError, HttpResponse<String>> {
        return authUtils.getToken(authServerUrl, authClientId, authClientSecret, authGrantType).flatMap {
            val request = HttpRequest.newBuilder().uri(
                URI.create("$brokerUrl/ngsi-ld/v1/entityOperations/upsert")
            )
                .setHeader("Content-Type", "application/ld+json")
                .setHeader("Authorization", "Bearer $it")
                .POST(HttpRequest.BodyPublishers.ofString(payload)).build()

            try {
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                logger.debug("Http response status code: ${response.statusCode()}")
                logger.debug("Http response body: ${response.body()}")
                if (batchUpsertSuccessCodes.contains(response.statusCode()))
                    response.right()
                else
                    ContextBrokerError(
                        "Received ${response.statusCode()} (${response.body()}) from context broker"
                    ).left()
            } catch (e: IOException) {
                val errorMessage = e.message ?: "Error encountered while sending entities to context broker"
                ContextBrokerError(errorMessage).left()
            }
        }
    }
}
