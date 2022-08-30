package com.soywiz.notionapi.dto

import java.util.*

data class Page(
    val id: String,
    val created_time: Date,
    val last_edited_time: Date,
    val created_by: PartialUser,
    val last_edited_by: PartialUser,
    val cover: Image?,
    val archived: Boolean,
    val url: String,
    val properties: Map<String, PropInfo>,
) : NObject()
