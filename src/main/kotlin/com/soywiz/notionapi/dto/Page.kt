package com.soywiz.notionapi.dto

import java.util.*

data class Page(
    var id: String,
    var created_time: Date,
    var last_edited_time: Date,
    var created_by: PartialUser,
    var last_edited_by: PartialUser,
    var cover: Image?,
    var archived: Boolean,
    var url: String,
    var properties: Map<String, PropInfo>,
) : NObject()
