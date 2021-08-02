package io.egm.kngsild.utils

import io.egm.kngsild.utils.JsonUtils.serializeObject
import io.egm.kngsild.utils.UriUtils.toUri
import java.net.URI
import java.time.ZonedDateTime

typealias NgsildEntity = Map<String, Any>
typealias NgsildMultiAttribute = List<Map<String, Any>>
typealias NgsiLdAttribute = Map<String, Any>

data class NgsiLdAttributeNG(
    val propertyName: String,
    val propertyValue: Map<String, Any>
)

fun List<NgsiLdAttributeNG>.serialize(): String =
    this.groupBy {
        it.propertyName
    }.mapValues { entry ->
        val attributes = entry.value
        if (attributes.size == 1)
            attributes[0].propertyValue
        else
            this.map { it.propertyValue }
    }.let {
        serializeObject(it)
    }

class NgsiLdPropertyBuilder(
    private val propertyName: String
) {

    private var attributeMap = mutableMapOf<String, Any>()

    fun withValue(value: Any): NgsiLdPropertyBuilder {
        attributeMap["value"] = value
        return this
    }

    fun withObservedAt(observedAt: ZonedDateTime?): NgsiLdPropertyBuilder {
        observedAt?.apply {
            attributeMap["observedAt"] = this
        }
        return this
    }

    fun withUnitCode(unitCode: String?): NgsiLdPropertyBuilder {
        unitCode?.apply {
            attributeMap["unitCode"] = this
        }
        return this
    }

    fun withDatasetId(datasetId: URI?): NgsiLdPropertyBuilder {
        datasetId?.apply {
            attributeMap["datasetId"] = this
        }
        return this
    }

    fun withSubProperty(name: String, value: Any): NgsiLdPropertyBuilder {
        attributeMap[name] = mapOf(
            "type" to "Property",
            "value" to value
        )
        return this
    }

    fun build(): NgsiLdAttributeNG =
        NgsiLdAttributeNG(propertyName, attributeMap.plus("type" to "Property"))
}

class NgsiLdEntityBuilder(
    val id: URI,
    val type: String,
    val contexts: List<String> = emptyList()
) {

    fun build(): NgsildEntity =
        mapOf("id" to id)
            .plus("type" to type)
            .let {
                if (contexts.isNotEmpty())
                    it.plus("@context" to contexts)
                else
                    it.plus("@context" to NgsiLdUtils.coreContext)
            }
}

object NgsiLdUtils {

    const val coreContext = "https://uri.etsi.org/ngsi-ld/v1/ngsi-ld-core-context.jsonld"

    fun propertyAttribute(value: Any) = mapOf(
        "type" to "Property",
        "value" to value
    )
}

fun NgsildEntity.getRelationshipObject(relationshipName: String): URI? =
    ((this[relationshipName] as NgsiLdAttribute?)?.get("object") as String?)?.toUri()

fun NgsildEntity.hasAttribute(attributeName: String, datasetId: URI?): Boolean =
    when (val attributeEntry = this[attributeName]) {
        null -> false
        is List<*> -> {
            (attributeEntry as NgsildMultiAttribute?)?.find {
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
