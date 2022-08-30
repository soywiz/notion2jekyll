package com.soywiz.notionapi.dto

import com.fasterxml.jackson.annotation.*
import kotlin.collections.LinkedHashMap
import kotlin.reflect.jvm.*

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "object",
    visible = true,
    defaultImpl = NObject::class,
)
@JsonSubTypes(
    JsonSubTypes.Type(value = NListObject::class, name = "list"),
    JsonSubTypes.Type(value = NErrorObject::class, name = "error"),
    JsonSubTypes.Type(value = Page::class, name = "page"),
    JsonSubTypes.Type(value = Database::class, name = "database"),
    JsonSubTypes.Type(value = Block::class, name = "block"),
    JsonSubTypes.Type(value = PartialUser::class, name = "user"),
    JsonSubTypes.Type(value = ExternalImage::class, name = "external"),
)
open class NObject {
    open var `object`: String = ""

    @JsonAnySetter
    var extra: LinkedHashMap<String, Any?> = LinkedHashMap()

    override fun toString(): String = "${this::class.jvmName}(${`object`}, $extra)"
}

data class NListObject<T>(
    override var `object`: String,
    val results: List<T>,
    val next_cursor: String?,
    val has_more: Boolean,
    val type: String,
) : NObject()

data class NErrorObject(val status: Int, val code: String, val message: String) : NObject()

class NError(val error: NErrorObject) : Throwable("$error")
