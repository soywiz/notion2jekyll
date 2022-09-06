package com.soywiz.jekyllapi

import com.soywiz.notionapi.*
import com.soywiz.util.*
import java.io.*

fun PageInfo.toFileWithFrontMatter(postsFolder: File): FileWithFrontMatter {
    val page = this
    val published = page.published
    val draft = page.draft
    val headers = mutableMapOf<String, Any?>(
        "layout" to "post",
        "title" to page.title,
        "notion_page_id" to page.page.id,
        "permalink" to "/" + page.permalink.trim('/') + "/",
        "sponsor_tier" to page.sponsor.toIntOrNull(),
        "category" to page.category,
        "date" to published,
        "feature_image" to page.featured?.let { "/" + it.trim('/') },
        "tags" to (page.tags.takeIf { it.isNotBlank() }?.split(",")?.map { it.trim() } ?: emptyList()),
    )

    val file = if (published == null || draft) {
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
    val contentMarkdown = when (page.sponsor.toIntOrNull()) {
        null -> page.contentMarkdown
        else -> {
            //val SPONSOR_START = "\n\$SPONSOR\$:\n"
            //val SPONSOR_END = "\n:\$\$\n"
            val SPONSOR_START = "{% if sponsored() %}"
            val SPONSOR_END = "{% end %}"
            if (page.contentMarkdown.contains("\n---\n")) {
                page.contentMarkdown.replaceFirst("\n---\n", SPONSOR_START) + SPONSOR_END
            } else {
                SPONSOR_START + page.contentMarkdown + SPONSOR_END
            }
        }
    }
    return FileWithFrontMatter(headers, contentMarkdown, file, postsFolder)
}