package com.soywiz.notionapi.dto

import com.fasterxml.jackson.annotation.*

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = NotionEmoji::class, name = "emoji"),
    JsonSubTypes.Type(value = NotionFile::class, name = "file"),
    JsonSubTypes.Type(value = NotionExternal::class, name = "external"),
)
open class NotionBaseFile {
    var caption: List<RichTextEntry> = emptyList()
    var type: String = ""

    @JsonAnySetter
    var extra: Map<String, Any?> = LinkedHashMap()

    @get:JsonIgnore
    open var url: String get() = ""
        set(value) {}

    override fun toString(): String = "NotionFile(type=$type, $extra)"
}

data class NotionExternal(
    var external: External,
) : NotionBaseFile() {

    @get:JsonIgnore
    override var url: String by external::url

    data class External(var url: String)
}

data class NotionFile(
    var file: NFile,
    var name: String? = null,
) : NotionBaseFile() {
    data class NFile(
        var url: String,
        var expiry_time: String?
    )

    @get:JsonIgnore
    override var url: String
        get() = file.url
        set(value) {
            file.url = value
            file.expiry_time = null
        }
}

data class NotionEmoji(
    var emoji: String,
) : NotionBaseFile() {
}
