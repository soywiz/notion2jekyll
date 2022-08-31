package com.soywiz.notionapi.dto


data class RichTextEntry(
    var plain_text: String,
    var href: String?,
    var annotations: Annotations,
    var type: String, // text, mention, equation,
    var text: Text?,
) {
    data class Text(
        var content: String,
        var link: Link?
    )
    data class Annotations(
        var bold: Boolean,
        var italic: Boolean,
        var strikethrough: Boolean,
        var underline: Boolean,
        var code: Boolean,
        var color: String,
    )

    data class Link(
        val url: String
    )

    companion object {
        fun toMarkdown(items: List<RichTextEntry>) = items.joinToString("") { it.toMarkdown() }
    }

    fun toPlaintext(): String = plain_text
    fun toMarkdown(): String = buildString {
        if (annotations.strikethrough) append("~~")
        if (annotations.bold) append("**")
        if (annotations.italic) append("_")
        if (href != null) append("[")
        if (annotations.underline) append("<ins>")
        if (annotations.color != "default") append("<span style='color:${annotations.color}' markdown=1>")
        if (annotations.code) append("<code>")
        append(plain_text)
        if (annotations.code) append("</code>")
        if (annotations.color != "default") append("</span>")
        if (annotations.underline) append("</ins>")
        if (href != null) append("]($href)")
        if (annotations.italic) append("_")
        if (annotations.bold) append("**")
        if (annotations.strikethrough) append("~~")
    }
}

fun List<RichTextEntry>.toMarkdown(): String = joinToString("") { it.toMarkdown() }
