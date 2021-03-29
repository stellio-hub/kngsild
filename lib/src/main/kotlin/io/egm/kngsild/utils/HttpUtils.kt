package io.egm.kngsild.utils

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.egm.kngsild.model.AccessTokenNotRetrieved
import io.egm.kngsild.model.ApplicationError
import io.egm.kngsild.model.AuthenticationServerError
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

class HttpUtils {

    val httpClient: HttpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .build()

    private val logger = LoggerFactory.getLogger(javaClass)

    fun getToken(
        authServerUrl: String,
        authClientId: String,
        authClientSecret: String,
        authGrantType: String
    ): Either<ApplicationError, String> {
        val request = HttpRequest
            .newBuilder()
            .uri(URI.create(authServerUrl))
            .POST(
                buildFormDataFromMap(
                    mapOf(
                        "client_id" to authClientId,
                        "client_secret" to authClientSecret,
                        "grant_type" to authGrantType
                    )
                )
            )
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()

        return try {
            val httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body()
            val response: Map<String, String> = JsonUtils.mapper.readValue(
                httpResponse,
                JsonUtils.mapper.typeFactory.constructMapLikeType(
                    Map::class.java, String::class.java, String::class.java
                )
            )
            val accessToken = response["access_token"]

            accessToken?.right() ?: AccessTokenNotRetrieved("Unable to get an access token").left()
        } catch (e: IOException) {
            val errorMessage = e.message ?: "Error encountered while requesting token from authentication server"
            logger.warn(errorMessage)
            AuthenticationServerError(errorMessage).left()
        }
    }

    private fun buildFormDataFromMap(data: Map<String, String>): HttpRequest.BodyPublisher {
        return HttpRequest.BodyPublishers.ofString(paramsUrlBuilder(data))
    }

    fun paramsUrlBuilder(data: Map<String, String>): String {
        val builder = StringBuilder()
        builder.append("?")
        for ((key, value) in data) {
            if (builder.isNotEmpty()) {
                builder.append("&")
            }
            builder.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
            builder.append("=")
            builder.append(URLEncoder.encode(value, StandardCharsets.UTF_8))
        }
        return builder.toString()
    }

    fun httpLinkHeaderBuilder(context: String) =
        "<$context>;rel=http://www.w3.org/ns/json-ld#context;type=application/ld+json"
}
