package com.soywiz.notionapi.dto

import java.util.*

data class Database(
    val id: String,
    val cover: Image,
    val icon: Image?,
    val created_time: Date,
    val created_by: PartialUser,
    val last_edited_time: Date?,
    val last_edited_by: PartialUser?,
    val title: List<RichTextEntry>,
    val description: List<RichTextEntry>,
    val is_inline: Boolean,
    val properties: Map<String, PropInfo>,
    val parent: Any?, // Reference
    val url: String,
    val archived: Boolean,
) : NObject()
