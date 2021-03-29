package io.egm.kngsild.utils

import java.net.URI
import java.net.URISyntaxException

fun String.toUri(): URI? =
    try {
        val uri = URI(this)
        uri
    } catch (e: URISyntaxException) {
        null
    }
