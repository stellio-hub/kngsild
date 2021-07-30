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

fun NgsildEntity.hasAttribute(attributeName: String, datasetId: URI?): Boolean =
    when (val attributeEntry = this[attributeName]) {
        null -> false
        is List<*> -> {
            (attributeEntry as NgsildAttribute?)?.find {
                if (datasetId != null)
                    it["datasetId"] != null && it["datasetId"] == datasetId
                else
                    it["datasetId"] == null
            }?.isNotEmpty() ?: false
        }
        is Map<*,*> -> {
            if (datasetId != null)
                attributeEntry["datasetId"] != null && attributeEntry["datasetId"] == datasetId
            else
                attributeEntry["datasetId"] == null
        }
        else -> false
    }
