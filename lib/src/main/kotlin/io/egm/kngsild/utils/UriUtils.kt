package io.egm.kngsild.utils

import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URISyntaxException

object UriUtils {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun String.toUri(): URI? =
        try {
            URI(this)
        } catch (e: URISyntaxException) {
            logger.warn("Unable to parse URI")
            null
        }
}
