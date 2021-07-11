package io.egm.kngsild.utils

import io.egm.kngsild.utils.UriUtils.toUri
import java.net.URI

typealias NgsildEntity = Map<String, Any>
typealias NgsildAttribute = List<Map<String, Any>>
typealias NgsiLdRelationshipInstance = Map<String, Any>

object NgsiLdUtils {

    const val coreContext = "https://uri.etsi.org/ngsi-ld/v1/ngsi-ld-core-context.jsonld"

    fun propertyAttribute(value: Any) = mapOf(
        "type" to "Property",
        "value" to value
    )
}

fun NgsildEntity.getRelationshipObject(relationshipName: String): URI? =
    ((this[relationshipName] as NgsiLdRelationshipInstance?)?.get("object") as String?)?.toUri()
