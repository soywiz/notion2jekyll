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
    JsonSubTypes.Type(value = NotionExternal::class, name = "external"),
)
open class NObject {
    open var `object`: String = ""

    @JsonAnySetter
    var extra: LinkedHashMap<String, Any?> = LinkedHashMap()

    override fun toString(): String = "${this::class.jvmName}(${`object`}, $extra)"
}

fun <T : NObject> T.clean(): T {
    extra.remove("request_id")
    return this
}

data class NListObject<T>(
    override var `object`: String,
    var results: List<T>,
    var next_cursor: String?,
    var has_more: Boolean,
    var type: String,
) : NObject()

data class NErrorObject(var status: Int, var code: String, var message: String) : NObject()

class NError(var error: NErrorObject) : Throwable("$error")
