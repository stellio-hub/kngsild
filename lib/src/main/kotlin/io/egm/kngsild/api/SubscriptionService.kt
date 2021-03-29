package io.egm.kngsild.api

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.egm.kngsild.model.ApplicationError
import io.egm.kngsild.model.ContextBrokerError
import io.egm.kngsild.utils.HttpUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class SubscriptionService(
    private val httpUtils: HttpUtils
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun create(
        brokerUrl: String,
        authServerUrl: String,
        authClientId: String,
        authClientSecret: String,
        authGrantType: String,
        subscriptionPayload: String
    ): Either<ApplicationError, HttpResponse<String>> {
        return httpUtils.getToken(authServerUrl, authClientId, authClientSecret, authGrantType).flatMap {
            val request = HttpRequest.newBuilder().uri(
                URI.create("$brokerUrl/ngsi-ld/v1/subscriptions")
            )
                .setHeader("Content-Type", "application/ld+json")
                .setHeader("Authorization", "Bearer $it")
                .POST(HttpRequest.BodyPublishers.ofString(subscriptionPayload)).build()
            try {
                val response = httpUtils.httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() == HttpURLConnection.HTTP_CREATED)
                    response.right()
                // In case we loose a subscription in the local storage, it should be re-created regardless the 409
                // returned from the context broker
                else if (response.statusCode() == HttpURLConnection.HTTP_CONFLICT) {
                    logger.warn("Subscription already exists in context broker")
                    response.right()
                } else ContextBrokerError(
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
