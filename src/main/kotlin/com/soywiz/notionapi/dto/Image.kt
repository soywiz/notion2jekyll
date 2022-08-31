package com.soywiz.notionapi.dto

import com.fasterxml.jackson.annotation.*

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ExternalImage::class, name = "external"),
)
open class Image {
    var type: String = ""

    @JsonAnySetter
    var extra: Map<String, Any?> = LinkedHashMap()
}

data class ExternalImage(
    var external: External,
) : Image() {
    data class External(var url: String)
}
