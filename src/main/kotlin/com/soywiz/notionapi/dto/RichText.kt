package com.soywiz.notionapi.dto


data class RichTextEntry(
    val plain_text: String,
    val href: String?,
    val annotations: Annotations,
    val type: String, // text, mention, equation,
    val text: Text?,
) {
    data class Text(val content: String, val link: String?)
    data class Annotations(
        val bold: Boolean,
        val italic: Boolean,
        val strikethrough: Boolean,
        val underline: Boolean,
        val code: Boolean,
        val color: String,
    )

    companion object {
        fun toMarkdown(items: List<RichTextEntry>) = items.joinToString("") { it.toMarkdown() }
    }

    fun toPlaintext(): String = plain_text
    fun toMarkdown(): String = buildString {
        if (annotations.strikethrough) append("~~")
        if (annotations.bold) append("**")
        if (annotations.italic) append("_")
        if (annotations.underline) append("<ins>")
        if (annotations.color != "default") append("<span style='color:${annotations.color}' markdown=1>")
        if (annotations.code) append("<code>")
        append(plain_text)
        if (annotations.code) append("</code>")
        if (annotations.color != "default") append("</span>")
        if (annotations.underline) append("</ins>")
        if (annotations.italic) append("_")
        if (annotations.bold) append("**")
        if (annotations.strikethrough) append("~~")
    }
}

fun List<RichTextEntry>.toMarkdown(): String = joinToString("") { it.toMarkdown() }
