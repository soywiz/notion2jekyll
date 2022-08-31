package com.soywiz.notionapi.dto

import java.util.*

data class Database(
    var id: String,
    var cover: Image,
    var icon: Image?,
    var created_time: Date,
    var created_by: PartialUser,
    var last_edited_time: Date?,
    var last_edited_by: PartialUser?,
    var title: List<RichTextEntry>,
    var description: List<RichTextEntry>,
    var is_inline: Boolean,
    var properties: Map<String, PropInfo>,
    var parent: Any?, // Reference
    var url: String,
    var archived: Boolean,
) : NObject() {
    val last_edited_time_sure: Date get() = last_edited_time ?: created_time

}
