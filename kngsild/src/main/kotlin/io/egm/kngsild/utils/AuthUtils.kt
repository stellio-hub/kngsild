package io.egm.kngsild.utils

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.egm.kngsild.model.*
import io.egm.kngsild.utils.HttpUtils.buildFormDataFromMap
import io.egm.kngsild.utils.HttpUtils.httpClient
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class AuthUtils(
    private val clientCredentials: ClientCredentials? = null,
    private val providedToken: ProvidedToken? = null,
    private val authType: AuthType = AuthType.NONE
) {

    init {
        when (authType) {
            AuthType.PROVIDED_TOKEN ->
                if (providedToken == null)
                    throw ConfigurationError(configurationErrorMessage("ProvidedToken"))
            AuthType.CLIENT_CREDENTIALS ->
                if (clientCredentials == null)
                    throw ConfigurationError(configurationErrorMessage("ClientCredentials"))
        }
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    private fun configurationErrorMessage(errorMessage: String): String =
        "You have chosen a $errorMessage type authentification but no configuration of this type has been made"

    fun getToken(): Either<ApplicationError, String> {
        when (authType) {
            AuthType.NONE -> {
                logger.debug("Authentication is not enabled, returning random string")
                return "Unused-Thing".right()
            }
            AuthType.PROVIDED_TOKEN -> return providedToken!!.accessToken.right()

            AuthType.CLIENT_CREDENTIALS -> {
                val request = HttpRequest
                    .newBuilder()
                    .uri(URI.create(clientCredentials!!.serverUrl))
                    .POST(
                        buildFormDataFromMap(
                            mapOf(
                                "client_id" to clientCredentials.clientId,
                                "client_secret" to clientCredentials.clientSecret,
                                "grant_type" to clientCredentials.grantType
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
                    val errorMessage =
                        e.message ?: "Error encountered while requesting token from authentication server"
                    logger.warn(errorMessage)
                    AuthenticationServerError(errorMessage).left()
                }
            }
        }
    }
}

enum class AuthType {
    NONE,
    PROVIDED_TOKEN,
    CLIENT_CREDENTIALS,
}

data class ClientCredentials(
    val serverUrl: String,
    val clientId: String,
    val clientSecret: String
) {
    val grantType = "client_credentials"
}

data class ProvidedToken(val accessToken: String)
