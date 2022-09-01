package com.soywiz.notionapi.dto

import com.fasterxml.jackson.annotation.*

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = NotionFileExternal::class, name = "external"),
    JsonSubTypes.Type(value = NotionEmoji::class, name = "emoji"),
    JsonSubTypes.Type(value = NotionFile::class, name = "file"),
)
open class NotionFile {
    var type: String = ""

    @JsonAnySetter
    var extra: Map<String, Any?> = LinkedHashMap()

    override fun toString(): String = "NotionFile(type=$type, $extra)"
}

data class NotionFileExternal(
    var external: External,
) : NotionFile() {
    data class External(var url: String)
}

data class NotionEmoji(
    var emoji: String,
) : NotionFile() {
}
