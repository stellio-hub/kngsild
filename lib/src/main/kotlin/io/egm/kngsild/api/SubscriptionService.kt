package io.egm.kngsild.api

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.egm.kngsild.model.AlreadyExists
import io.egm.kngsild.model.ApplicationError
import io.egm.kngsild.model.ContextBrokerError
import io.egm.kngsild.utils.AuthUtils
import io.egm.kngsild.utils.HttpUtils.httpClient
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class SubscriptionService(
    private val authUtils: AuthUtils
) {

    fun create(
        brokerUrl: String,
        authServerUrl: String,
        authClientId: String,
        authClientSecret: String,
        authGrantType: String,
        subscriptionPayload: String
    ): Either<ApplicationError, HttpResponse<String>> {
        return authUtils.getToken(authServerUrl, authClientId, authClientSecret, authGrantType).flatMap {
            val request = HttpRequest.newBuilder().uri(
                URI.create("$brokerUrl/ngsi-ld/v1/subscriptions")
            )
                .setHeader("Content-Type", "application/ld+json")
                .setHeader("Authorization", "Bearer $it")
                .POST(HttpRequest.BodyPublishers.ofString(subscriptionPayload)).build()
            try {
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() == HttpURLConnection.HTTP_CREATED)
                    response.right()
                else if (response.statusCode() == HttpURLConnection.HTTP_CONFLICT)
                    AlreadyExists("Subscription already exists").left()
                else ContextBrokerError(
                    "Failed to create Subscription, " +
                        "received ${response.statusCode()} (${response.body()}) from context broker"
                ).left()
            } catch (e: IOException) {
                val errorMessage = e.message ?: "Error encountered while creating subscription in context broker"
                ContextBrokerError(errorMessage).left()
            }
        }
    }
}
