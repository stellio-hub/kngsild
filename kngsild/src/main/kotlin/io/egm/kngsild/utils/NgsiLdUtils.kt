package io.egm.kngsild.utils

import io.egm.kngsild.utils.JsonUtils.serializeObject
import io.egm.kngsild.utils.UriUtils.toUri
import java.net.URI
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

typealias NgsildEntity = Map<String, Any>
typealias NgsildMultiAttribute = List<Map<String, Any>>
typealias NgsiLdAttribute = Map<String, Any>
typealias NgsiLdTemporalAttributesInstances = Map<String, List<NgsiLdAttribute>>

data class NgsiLdAttributeNG(
    val propertyName: String,
    val propertyValue: Map<String, Any>
)

fun List<NgsiLdAttributeNG>.groupByProperty(): NgsiLdTemporalAttributesInstances =
    this.groupBy {
        it.propertyName
    }.mapValues { entry ->
        entry.value.map { ngsiLdAttributeNG -> ngsiLdAttributeNG.propertyValue }
    }

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

    fun withSubProperty(name: String, value: Any?): NgsiLdPropertyBuilder {
        value?.apply {
            attributeMap[name] = mapOf(
                "type" to "Property",
                "value" to value
            )
        }
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

    private var attributes = mutableMapOf<String, Any>()

    fun addAttribute(ngsiLdAttributeNG: NgsiLdAttributeNG): NgsiLdEntityBuilder {
        attributes[ngsiLdAttributeNG.propertyName] = ngsiLdAttributeNG.propertyValue
        return this
    }

    fun build(): NgsildEntity =
        mapOf("id" to id)
            .plus("type" to type)
            .plus(attributes)
            .let {
                if (contexts.isNotEmpty())
                    it.plus("@context" to contexts)
                else
                    it.plus("@context" to NgsiLdUtils.NGSILD_CORE_CONTEXT)
            }
}

object NgsiLdUtils {

    const val NGSILD_CORE_CONTEXT = "https://uri.etsi.org/ngsi-ld/v1/ngsi-ld-core-context-v1.7.jsonld"

    fun propertyAttribute(value: Any) = mapOf(
        "type" to "Property",
        "value" to value
    )
}

fun <T> T.toDefaultDatasetId(): URI =
    "urn:ngsi-ld:Dataset:$this".toUri()!!

fun NgsildEntity.getRelationshipObject(relationshipName: String): URI? =
    ((this[relationshipName] as NgsiLdAttribute?)?.get("object") as String?)?.toUri()

fun NgsildEntity.hasAttribute(attributeName: String, datasetId: URI? = null): Boolean =
    when (val attributeEntry = this[attributeName]) {
        null -> false
        is List<*> -> {
            (attributeEntry as NgsildMultiAttribute?)?.find {
                if (datasetId != null)
                    it["datasetId"] == datasetId.toString()
                else
                    it["datasetId"] == null
            }?.isNotEmpty() ?: false
        }
        is Map<*, *> -> {
            if (datasetId != null)
                attributeEntry["datasetId"] == datasetId.toString()
            else
                attributeEntry["datasetId"] == null
        }
        else -> false
    }

fun NgsildEntity.getAttribute(attributeName: String, datasetId: URI? = null): NgsiLdAttribute? =
    when (val attributeEntry = this[attributeName]) {
        null -> null
        is List<*> -> {
            (attributeEntry as NgsildMultiAttribute?)?.find {
                if (datasetId != null)
                    it["datasetId"] == datasetId
                else
                    it["datasetId"] == null
            }
        }
        is Map<*, *> -> {
            val ngsiLdAttribute = attributeEntry as NgsiLdAttribute
            if (datasetId != null && ngsiLdAttribute["datasetId"] == datasetId)
                ngsiLdAttribute
            else if (datasetId == null && !ngsiLdAttribute.containsKey("datasetId"))
                ngsiLdAttribute
            else
                null
        }
        else -> null
    }

val formatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

fun ZonedDateTime.toNgsiLdFormat(): String =
    formatter.format(this)
