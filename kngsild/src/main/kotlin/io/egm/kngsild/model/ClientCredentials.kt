package io.egm.kngsild.model

data class ClientCredentials(
    val serverUrl: String,
    val clientId: String,
    val clientSecret: String,
    val grantType: String
)
