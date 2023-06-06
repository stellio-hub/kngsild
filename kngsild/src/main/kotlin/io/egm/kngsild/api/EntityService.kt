package io.egm.kngsild.api

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.egm.kngsild.model.AlreadyExists
import io.egm.kngsild.model.ApplicationError
import io.egm.kngsild.model.ContextBrokerError
import io.egm.kngsild.model.ResourceNotFound
import io.egm.kngsild.utils.*
import io.egm.kngsild.utils.HttpUtils.APPLICATION_JSON
import io.egm.kngsild.utils.HttpUtils.APPLICATION_JSONLD
import io.egm.kngsild.utils.HttpUtils.DEFAULT_TENANT_URI
import io.egm.kngsild.utils.HttpUtils.httpClient
import io.egm.kngsild.utils.HttpUtils.httpLinkHeaderBuilder
import io.egm.kngsild.utils.HttpUtils.paramsUrlBuilder
import io.egm.kngsild.utils.JsonUtils.deserializeObject
import io.egm.kngsild.utils.JsonUtils.serializeObject
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class EntityService(
    private val contextBrokerUrl: String,
    private val authUtils: AuthUtils
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val entityApiRootPath = "/ngsi-ld/v1/entities"
    private val patchSuccessCode = listOf(HttpURLConnection.HTTP_NO_CONTENT)
    private val postSuccessCode = listOf(HttpURLConnection.HTTP_NO_CONTENT)

    fun create(
        entityPayload: String,
        tenantUri: URI? = DEFAULT_TENANT_URI
    ): Either<ApplicationError, ResourceLocation> {
        return authUtils.getToken().flatMap {
            logger.debug("Creating entity $entityPayload")
            val request = HttpRequest.newBuilder().uri(
                URI.create("$contextBrokerUrl$entityApiRootPath")
            )
                .setHeader("Content-Type", APPLICATION_JSONLD)
                .setHeader("Authorization", "Bearer $it")
                .setHeader("NGSILD-Tenant", tenantUri.toString())
                .POST(HttpRequest.BodyPublishers.ofString(entityPayload)).build()
            try {
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                when {
                    response.statusCode() == HttpURLConnection.HTTP_CREATED ->
                        response.headers().firstValue("Location").get().right()
                    response.statusCode() == HttpURLConnection.HTTP_CONFLICT ->
                        AlreadyExists("Entity already exists").left()
                    else ->
                        ContextBrokerError(
                            "Failed to create entity, " +
                                "received ${response.statusCode()} (${response.body()}) from context broker"
                        ).left()
                }
            } catch (e: IOException) {
                val errorMessage = e.message ?: "Error encountered while creating entity in context broker"
                ContextBrokerError(errorMessage).left()
            }
        }
    }

    fun query(
        queryParams: Map<String, String>,
        contextUrl: String,
        tenantUri: URI? = DEFAULT_TENANT_URI
    ): Either<ApplicationError, List<NgsildEntity>> {
        val params: String = paramsUrlBuilder(queryParams)
        return authUtils.getToken().flatMap {
            val request = HttpRequest
                .newBuilder()
                .uri(
                    URI.create("$contextBrokerUrl$entityApiRootPath$params")
                )
                .setHeader("Accept", APPLICATION_JSONLD)
                .setHeader("Link", httpLinkHeaderBuilder(contextUrl))
                .setHeader("Authorization", "Bearer $it")
                .setHeader("NGSILD-Tenant", tenantUri.toString())
                .GET().build()

            try {
                logger.debug("Issuing query: /ngsi-ld/v1/entities$params")
                val httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                logger.debug("Http response status code: ${httpResponse.statusCode()}")
                logger.trace("Http response body: ${httpResponse.body()}")

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

    fun retrieve(
        entityId: URI,
        queryParams: Map<String, String>,
        contextUrl: String,
        tenantUri: URI? = DEFAULT_TENANT_URI
    ): Either<ApplicationError, NgsildEntity> {
        val params: String = paramsUrlBuilder(queryParams)
        return authUtils.getToken().flatMap {
            val request = HttpRequest
                .newBuilder()
                .uri(
                    URI.create("$contextBrokerUrl$entityApiRootPath/$entityId$params")
                )
                .setHeader("Accept", APPLICATION_JSONLD)
                .setHeader("Link", httpLinkHeaderBuilder(contextUrl))
                .setHeader("Authorization", "Bearer $it")
                .setHeader("NGSILD-Tenant", tenantUri.toString())
                .GET().build()

            try {
                logger.debug("Issuing retrieve: /ngsi-ld/v1/entities/$entityId$params")
                val httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                logger.debug("Http response status code: ${httpResponse.statusCode()}")
                logger.debug("Http response body: ${httpResponse.body()}")

                if (httpResponse.statusCode() == HttpURLConnection.HTTP_OK)
                    deserializeObject(httpResponse.body()).right()
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

    fun updateAttributes(
        entityId: URI,
        attributesPayload: String,
        contextUrl: String,
        tenantUri: URI? = DEFAULT_TENANT_URI
    ): Either<ApplicationError, String?> {
        return authUtils.getToken().flatMap {
            val request = HttpRequest
                .newBuilder()
                .uri(
                    URI
                        .create("$contextBrokerUrl$entityApiRootPath/$entityId/attrs")
                )
                .method("PATCH", HttpRequest.BodyPublishers.ofString(attributesPayload))
                .setHeader("Content-Type", APPLICATION_JSON)
                .setHeader("Accept", APPLICATION_JSON)
                .setHeader("Link", httpLinkHeaderBuilder(contextUrl))
                .setHeader("Authorization", "Bearer $it")
                .setHeader("NGSILD-Tenant", tenantUri.toString())
                .build()
            return try {
                logger.debug("Patching entity $entityId with payload $attributesPayload")
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                logger.debug("Http response body: ${response.body()} (${response.statusCode()})")
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

    fun appendAttributes(
        entityId: URI,
        attributes: List<NgsiLdAttributeNG>,
        contextUrl: String,
        tenantUri: URI? = DEFAULT_TENANT_URI
    ): Either<ApplicationError, String> {
        if (attributes.isEmpty()) {
            logger.info("Empty attributes list received as input, returning")
            return postSuccessCode.first().toString().right()
        }

        return authUtils.getToken().flatMap { token ->
            val serializedPayload = attributes.serialize()
            val request = HttpRequest
                .newBuilder()
                .uri(
                    URI
                        .create("$contextBrokerUrl$entityApiRootPath/$entityId/attrs")
                )
                .method("POST", HttpRequest.BodyPublishers.ofString(serializedPayload))
                .setHeader("Content-Type", APPLICATION_JSON)
                .setHeader("Accept", APPLICATION_JSON)
                .setHeader("Link", httpLinkHeaderBuilder(contextUrl))
                .setHeader("Authorization", "Bearer $token")
                .setHeader("NGSILD-Tenant", tenantUri.toString())
                .build()
            try {
                logger.debug("Appending attributes $serializedPayload to entity $entityId")
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                logger.debug("Http response body: ${response.body()} (${response.statusCode()})")
                if (postSuccessCode.contains(response.statusCode()))
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

    fun partialAttributeUpdate(
        entityId: URI,
        attributeName: String,
        ngsiLdAttribute: NgsiLdAttribute,
        contextUrl: String,
        tenantUri: URI? = DEFAULT_TENANT_URI
    ): Either<ApplicationError, String> {
        return authUtils.getToken().flatMap {
            val requestPayload = serializeObject(ngsiLdAttribute.minus("type"))
            val request = HttpRequest
                .newBuilder()
                .uri(URI.create("$contextBrokerUrl$entityApiRootPath/$entityId/attrs/$attributeName"))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(requestPayload))
                .setHeader("Content-Type", APPLICATION_JSON)
                .setHeader("Accept", APPLICATION_JSON)
                .setHeader("Link", httpLinkHeaderBuilder(contextUrl))
                .setHeader("Authorization", "Bearer $it")
                .setHeader("NGSILD-Tenant", tenantUri.toString())
                .build()
            return try {
                logger.debug("Patching attribute $attributeName of entity $entityId with payload $requestPayload")
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                logger.debug("Http response body: ${response.body()} (${response.statusCode()})")
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
