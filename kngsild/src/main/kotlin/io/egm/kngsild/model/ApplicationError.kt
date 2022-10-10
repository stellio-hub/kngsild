package io.egm.kngsild.model

sealed class ApplicationError(override val message: String) : Exception(message)

data class AccessTokenNotRetrieved(override val message: String) : ApplicationError(message)
data class AuthenticationServerError(override val message: String) : ApplicationError(message)
data class ContextBrokerError(override val message: String) : ApplicationError(message)
data class ResourceNotFound(override val message: String) : ApplicationError(message)
data class AlreadyExists(override val message: String) : ApplicationError(message)
data class ConfigurationError(override val message: String) : ApplicationError(message)
