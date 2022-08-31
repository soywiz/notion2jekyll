package com.soywiz.notionapi.dto

import com.fasterxml.jackson.annotation.*

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    visible = true,
    defaultImpl = PropInfo::class,
)
@JsonSubTypes(
    JsonSubTypes.Type(value = RichTextPropInfo::class, name = "rich_text"),
    JsonSubTypes.Type(value = SelectPropInfo::class, name = "select"),
    JsonSubTypes.Type(value = DatePropInfo::class, name = "date"),
    JsonSubTypes.Type(value = LastEditedTimeDatePropInfo::class, name = "last_edited_time"),
    JsonSubTypes.Type(value = CreatedDatePropInfo::class, name = "created_time"),
    JsonSubTypes.Type(value = TitlePropInfo::class, name = "title"),
    JsonSubTypes.Type(value = MultiSelectPropInfo::class, name = "multi_select"),
)
open class PropInfo() {
    open var id: String = ""
    var name: String = ""
    var type: String = ""

    @JsonAnySetter
    var extra: Map<String, Any?> = LinkedHashMap()

    override fun toString(): String = "PropInfo(name=$name, type=$type, id=$id)"
}

abstract class BaseSelectPropInfo() : PropInfo() {
    data class Option(var id: String?, var name: String?, var color: String?)
    data class Select(var options: List<Option>)
}

data class RichTextPropInfo(override var id: String, var rich_text: Any?) : PropInfo()
abstract class BaseDatePropInfo() : PropInfo()
data class DatePropInfo(override var id: String, var date: Any?) : BaseDatePropInfo()
data class CreatedDatePropInfo(override var id: String, var created_time: Any?) : BaseDatePropInfo()
data class LastEditedTimeDatePropInfo(override var id: String, var last_edited_time: Any?) : BaseDatePropInfo()
data class TitlePropInfo(override var id: String, var title: Any?) : PropInfo()
data class MultiSelectPropInfo(override var id: String, var multi_select: Select?) : BaseSelectPropInfo()
data class SelectPropInfo(override var id: String, var select: Select?) : BaseSelectPropInfo()
