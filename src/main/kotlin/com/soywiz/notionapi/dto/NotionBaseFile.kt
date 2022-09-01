package com.soywiz.notionapi.dto

import com.fasterxml.jackson.annotation.*

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = NotionEmoji::class, name = "emoji"),
    JsonSubTypes.Type(value = NotionFile::class, name = "file"),
    JsonSubTypes.Type(value = NotionExternal::class, name = "external"),
)
open class NotionBaseFile {
    var type: String = ""

    @JsonAnySetter
    var extra: Map<String, Any?> = LinkedHashMap()

    override fun toString(): String = "NotionFile(type=$type, $extra)"
}

data class NotionExternal(
    var external: External,
) : NotionBaseFile() {
    data class External(var url: String)
}

data class NotionFile(
    var file: NFile,
) : NotionBaseFile() {
    data class NFile(var url: String, var expiry_time: String?)
}

data class NotionEmoji(
    var emoji: String,
) : NotionBaseFile() {
}
