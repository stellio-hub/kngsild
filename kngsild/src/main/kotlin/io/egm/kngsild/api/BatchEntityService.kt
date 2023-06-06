package io.egm.kngsild.api

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.egm.kngsild.model.ApplicationError
import io.egm.kngsild.model.ContextBrokerError
import io.egm.kngsild.utils.AuthUtils
import io.egm.kngsild.utils.HttpUtils
import io.egm.kngsild.utils.HttpUtils.APPLICATION_JSON
import io.egm.kngsild.utils.HttpUtils.APPLICATION_JSONLD
import io.egm.kngsild.utils.HttpUtils.DEFAULT_TENANT_URI
import io.egm.kngsild.utils.HttpUtils.httpClient
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class BatchEntityService(
    private val contextBrokerUrl: String,
    private val authUtils: AuthUtils
) {

    companion object {
        private const val HTTP_MULTI_STATUS = 207
    }

    private val logger = LoggerFactory.getLogger(javaClass)
    private val batchCreationSuccessCodes = listOf(HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_CREATED)
    private val batchUpsertSuccessCodes = listOf(
        HttpURLConnection.HTTP_CREATED,
        HttpURLConnection.HTTP_NO_CONTENT
    )
    private val batchDeleteSuccessCodes = listOf(
        HttpURLConnection.HTTP_NO_CONTENT,
        HTTP_MULTI_STATUS
    )

    fun create(
        payload: String,
        tenantUri: URI? = DEFAULT_TENANT_URI
    ): Either<ApplicationError, HttpResponse<String>> {
        return authUtils.getToken().flatMap {
            val request = HttpRequest.newBuilder().uri(
                URI.create("$contextBrokerUrl/ngsi-ld/v1/entityOperations/create")
            )
                .setHeader("Content-Type", APPLICATION_JSONLD)
                .setHeader("Authorization", "Bearer $it")
                .setHeader("NGSILD-Tenant", tenantUri.toString())
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
        payload: String,
        queryParams: Map<String, String>,
        tenantUri: URI? = DEFAULT_TENANT_URI
    ): Either<ApplicationError, HttpResponse<String>> {
        val params: String = HttpUtils.paramsUrlBuilder(queryParams)
        return authUtils.getToken().flatMap {
            val request = HttpRequest.newBuilder().uri(
                URI.create("$contextBrokerUrl/ngsi-ld/v1/entityOperations/upsert$params")
            )
                .setHeader("Content-Type", APPLICATION_JSONLD)
                .setHeader("Authorization", "Bearer $it")
                .setHeader("NGSILD-Tenant", tenantUri.toString())
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

    fun delete(
        payload: String,
        tenantUri: URI? = DEFAULT_TENANT_URI
    ): Either<ApplicationError, HttpResponse<String>> {
        return authUtils.getToken().flatMap {
            val request = HttpRequest.newBuilder().uri(
                URI.create("$contextBrokerUrl/ngsi-ld/v1/entityOperations/delete")
            )
                .setHeader("Content-Type", APPLICATION_JSON)
                .setHeader("Authorization", "Bearer $it")
                .setHeader("NGSILD-Tenant", tenantUri.toString())
                .POST(HttpRequest.BodyPublishers.ofString(payload)).build()

            try {
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                logger.debug("Http response status code: ${response.statusCode()}")
                logger.debug("Http response body: ${response.body()}")
                if (batchDeleteSuccessCodes.contains(response.statusCode()))
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
