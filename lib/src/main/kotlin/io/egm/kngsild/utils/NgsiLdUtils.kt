package io.egm.kngsild.utils

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
