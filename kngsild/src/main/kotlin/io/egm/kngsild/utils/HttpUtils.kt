package io.egm.kngsild.utils

import io.egm.kngsild.utils.UriUtils.toUri
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.nio.charset.StandardCharsets

object HttpUtils {

    val httpClient: HttpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .build()

    const val APPLICATION_JSON = "application/json"
    const val APPLICATION_JSONLD = "application/ld+json"
    const val NGSILD_TENANT_HEADER = "NGSILD-Tenant"
    val DEFAULT_TENANT_URI = "urn:ngsi-ld:tenant:default".toUri()

    fun buildFormDataFromMap(data: Map<String, String>): HttpRequest.BodyPublisher {
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
        "<$context>; rel=\"http://www.w3.org/ns/json-ld#context\"; type=\"application/ld+json\""
}

typealias ResourceLocation = String
