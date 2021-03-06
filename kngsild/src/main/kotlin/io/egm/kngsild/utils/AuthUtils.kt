package io.egm.kngsild.utils

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.egm.kngsild.model.AccessTokenNotRetrieved
import io.egm.kngsild.model.ApplicationError
import io.egm.kngsild.model.AuthenticationServerError
import io.egm.kngsild.utils.HttpUtils.buildFormDataFromMap
import io.egm.kngsild.utils.HttpUtils.httpClient
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class AuthUtils(
    private val serverUrl: String,
    private val clientId: String,
    private val clientSecret: String,
    private val grantType: String
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun getToken(): Either<ApplicationError, String> {
        val request = HttpRequest
            .newBuilder()
            .uri(URI.create(serverUrl))
            .POST(
                buildFormDataFromMap(
                    mapOf(
                        "client_id" to clientId,
                        "client_secret" to clientSecret,
                        "grant_type" to grantType
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
}
