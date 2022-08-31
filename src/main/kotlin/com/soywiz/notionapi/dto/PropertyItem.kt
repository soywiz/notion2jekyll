package com.soywiz.notionapi.dto

import com.fasterxml.jackson.annotation.*
import java.text.*
import java.util.*

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    defaultImpl = PropertyItem::class,
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(value = RichTextPropertyItem::class, name = "rich_text"),
    JsonSubTypes.Type(value = TitlePropertyItem::class, name = "title"),
    JsonSubTypes.Type(value = MultiSelectPropertyItem::class, name = "multi_select"),
    JsonSubTypes.Type(value = LastEditedTimePropertyItem::class, name = "last_edited_time"),
    JsonSubTypes.Type(value = CreatedTimePropertyItem::class, name = "created_time"),
    JsonSubTypes.Type(value = DatePropertyItem::class, name = "date"),
    JsonSubTypes.Type(value = SelectPropertyItem::class, name = "select"),
)
open class PropertyItem(
) : NObject() {
    var id: String = ""
    var type: String = ""

    open fun toMarkdown(): String = "PropertyItem(id=$id, type=$type, extra=$extra)"
    open fun toPlaintext(): String = toMarkdown()
}

fun Iterable<PropertyItem>.toMarkdown(): String = joinToString("") { it.toMarkdown() }
fun Iterable<PropertyItem>.toPlaintext(): String = joinToString("") { it.toPlaintext() }

open class BaseSelectPropertyItem(
) : PropertyItem() {
    class Select(var id: String, var name: String, var color: String)
}

data class SelectPropertyItem(
    var select: Select?
) : BaseSelectPropertyItem() {
    override fun toMarkdown(): String = select?.name ?: ""
}

data class MultiSelectPropertyItem(
    var multi_select: List<Select>
) : BaseSelectPropertyItem() {
    override fun toMarkdown(): String = multi_select.joinToString(", ") { it.name }
}

data class RichTextPropertyItem(
    var rich_text: RichTextEntry
) : PropertyItem() {
    override fun toMarkdown(): String = rich_text.toMarkdown()
    override fun toPlaintext(): String = rich_text.toPlaintext()
}

data class TitlePropertyItem(
    var title: RichTextEntry
) : PropertyItem() {
    override fun toMarkdown(): String = title.toMarkdown()
    override fun toPlaintext(): String = title.toPlaintext()
}

data class LastEditedTimePropertyItem(
    var last_edited_time: Date
) : PropertyItem() {
    override fun toMarkdown(): String = last_edited_time.toCannonicalString()
}

data class CreatedTimePropertyItem(
    var created_time: Date
) : PropertyItem() {
    override fun toMarkdown(): String = created_time.toCannonicalString()
}

fun Date.toCannonicalString(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").also {
    it.timeZone = TimeZone.getTimeZone("UTC")
}.format(this)

data class DatePropertyItem(
    var date: DateInfo,
) : PropertyItem() {
    data class DateInfo(
        var start: String,
        var end: String? = null,
        var time_zone: String? = null
    )
    override fun toMarkdown(): String = when {
        date.end != null -> "${date.start}-${date.end}"
        else -> date.start
    }
}
