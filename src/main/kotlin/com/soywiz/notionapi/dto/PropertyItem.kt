package com.soywiz.notionapi.dto

import com.fasterxml.jackson.annotation.*
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

open class BaseSelectPropertyItem(
) : PropertyItem() {
    class Select(val id: String, val name: String, val color: String)
}

data class SelectPropertyItem(
    val select: Select
) : BaseSelectPropertyItem() {
    override fun toMarkdown(): String = select.name
}

data class MultiSelectPropertyItem(
    val multi_select: List<Select>
) : BaseSelectPropertyItem() {
    override fun toMarkdown(): String = multi_select.joinToString(", ") { it.name }
}

data class RichTextPropertyItem(
    val rich_text: RichTextEntry
) : PropertyItem() {
    override fun toMarkdown(): String = rich_text.toMarkdown()
    override fun toPlaintext(): String = rich_text.toPlaintext()
}

data class TitlePropertyItem(
    val title: RichTextEntry
) : PropertyItem() {
    override fun toMarkdown(): String = title.toMarkdown()
    override fun toPlaintext(): String = title.toPlaintext()
}

data class LastEditedTimePropertyItem(
    val last_edited_time: Date
) : PropertyItem() {
    override fun toMarkdown(): String = last_edited_time.toString()
}

data class CreatedTimePropertyItem(
    val created_time: Date
) : PropertyItem() {
    override fun toMarkdown(): String = created_time.toString()
}

data class DatePropertyItem(
    val date: DateInfo,
) : PropertyItem() {
    data class DateInfo(
        val start: String,
        val end: String? = null,
        val time_zone: String? = null
    )
    override fun toMarkdown(): String = when {
        date.end != null -> "${date.start}-${date.end}"
        else -> date.start
    }
}
