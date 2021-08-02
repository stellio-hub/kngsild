package io.egm.kngsild.utils

import io.egm.kngsild.utils.UriUtils.toUri
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class NgsiLdUtilsTest {

    @Test
    fun `it should find an attribute by name and datasetId`() {
        val ngsildEntity = mapOf(
            "id" to "urn:ngsi-ld:Enttiy:123",
            "type" to "Entity",
            "attribute1" to mapOf(
                "type" to "Property",
                "value" to 1.0
            ),
            "attribute2" to mapOf(
                "type" to "Property",
                "value" to 1.0,
                "datasetId" to "urn:ngsi-ld:Dataset:123".toUri()
            ),
            "multiAttribute" to listOf(
                mapOf(
                    "type" to "Property",
                    "value" to 1.0
                ),
                mapOf(
                    "type" to "Property",
                    "value" to 1.0,
                    "datasetId" to "urn:ngsi-ld:Dataset:123".toUri()
                )
            )
        )

        assertTrue(ngsildEntity.hasAttribute("attribute1", null))
        assertFalse(ngsildEntity.hasAttribute("attribute1", "urn:ngsi-ld:Dataset:123".toUri()))
        assertFalse(ngsildEntity.hasAttribute("attribute2", null))
        assertTrue(ngsildEntity.hasAttribute("attribute2", "urn:ngsi-ld:Dataset:123".toUri()))
        assertFalse(ngsildEntity.hasAttribute("attribute3", null))
        assertFalse(ngsildEntity.hasAttribute("attribute3", "urn:ngsi-ld:Dataset:123".toUri()))
        assertTrue(ngsildEntity.hasAttribute("multiAttribute", null))
        assertTrue(ngsildEntity.hasAttribute("multiAttribute", "urn:ngsi-ld:Dataset:123".toUri()))
        assertFalse(ngsildEntity.hasAttribute("multiAttribute", "urn:ngsi-ld:Dataset:456".toUri()))
    }

    @Test
    fun `it should build an entity`() {
        val entity =
            NgsiLdEntityBuilder("urn:ngsi-ld:Entity:123".toUri()!!, "Entity")
                .build()

        assertEquals("urn:ngsi-ld:Entity:123", entity["id"].toString())
        assertEquals("Entity", entity["type"])
        assertEquals(NgsiLdUtils.coreContext, entity["@context"])
    }

    @Test
    fun `it should build a property`() {
        val property =
            NgsiLdPropertyBuilder("temperature")
                .withValue(31.0)
                .withObservedAt(ZonedDateTime.now())
                .withUnitCode("CEL")
                .withDatasetId("urn:ngsi-ld:Dataset:temperature".toUri())
                .withSubProperty("reliability", 0.98)
                .build()

        assertEquals("temperature", property.propertyName)
        assertEquals(6, property.propertyValue.size)
        val value = property.propertyValue["value"]
        assertEquals(31.0, value)
    }
}
