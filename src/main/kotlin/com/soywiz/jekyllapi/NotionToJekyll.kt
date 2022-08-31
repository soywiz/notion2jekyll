package com.soywiz.jekyllapi

import com.soywiz.notionapi.*
import com.soywiz.util.*
import java.io.*

fun PageInfo.toFileWithFrontMatter(): FileWithFrontMatter {
    val page = this
    val published = page.published
    val headers = mutableMapOf<String, Any?>(
        "title" to page.title,
        "notion_page_id" to page.page.id,
        "permalink" to "/" + page.permalink.trim('/') + "/",
        "sponsor" to page.sponsor.toIntOrNull(),
        "category" to page.category,
        "date" to published,
        "feature_image" to page.cover?.let { "/" + it.trim('/') },
        "tags" to page.tags.split(",").map { it.trim() },
    )

    val file = if (published == null) {
        File("../drafts/%s.md".format(page.permalink))
    } else {
        File(
            "%04d/%04d-%02d-%02d-%s.md".format(
                published.fullYear,
                published.fullYear,
                published.month1,
                published.dayInMonth,
                page.permalink
            )
        )
    }
    //println("file=$file")
    return FileWithFrontMatter(headers, page.contentMarkdown, file)
}