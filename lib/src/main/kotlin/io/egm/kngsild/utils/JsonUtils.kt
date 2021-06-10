package io.egm.kngsild.utils

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object JsonUtils {

    @PublishedApi internal val mapper =
        jacksonObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    fun serializeObject(input: Any): String =
        mapper.writeValueAsString(input)

    fun deserializeObject(input: String): Map<String, Any> =
        mapper.readValue(
            input,
            mapper.typeFactory.constructMapLikeType(Map::class.java, String::class.java, Any::class.java)
        )
}
