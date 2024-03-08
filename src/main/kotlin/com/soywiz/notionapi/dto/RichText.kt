package com.soywiz.notionapi.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.soywiz.util.*

interface IRichTextEntry

data class RichTextEntryImage(
    var plain_text: String,
    val url: String,
    val title: String? = null
) : IRichTextEntry

data class RichTextEntry(
    var plain_text: String,
    var href: String? = null,
    var annotations: Annotations = Annotations.DEFAULT,
    var type: String = "text", // text, mention, equation,
    var text: Text? = null,
    var equation: Equation? = null,
    var mention: Mention? = null,
) : IRichTextEntry {
    override fun toString(): String {
        return buildString {
            append("RichTextEntry")
            append("[").append(buildList<String> {
                if (href != null) append("href=$href")
                if (annotations != Annotations.DEFAULT) append("annotations=$annotations")
                if (type != "text") append("type=$type")
                if (equation != null) append("equation=$equation")
                if (mention != null) append("mention=$mention")
            }.joinToString(", ")).append("]")
            append("('").append(plain_text).append("')")
        }
    }

    @get:JsonIgnore
    val fixedPlainText = plain_text
        .replace('\u00A0', ' ') // non-breaking space
        .replace('`', '\'') // `
        .replace('’', '\'') // Right tildes

    data class Text(
        var content: String,
        var link: Link?
    )
    data class Equation(
        val expression: String
    )
    data class Mention(
        val mention: Any? // @TODO
    )
    data class Annotations(
        var bold: Boolean = false,
        var italic: Boolean = false,
        var strikethrough: Boolean = false,
        var underline: Boolean = false,
        var code: Boolean = false,
        var color: String = "default",
    ) {
        companion object {
            val DEFAULT = Annotations()
        }

        @get:JsonIgnore
        val any: Boolean = bold || italic || strikethrough || underline || code || color != "default"

        override fun toString(): String = "Annotations[" + buildList {
            if (bold) add("bold")
            if (italic) add("italic")
            if (strikethrough) add("strikethrough")
            if (underline) add("underline")
            if (code) add("code")
            if (color != "default") add("color=$color")
        }.joinToString(",") + "]"
    }

    data class Link(
        val url: String
    )

    @get:JsonIgnore
    val plain: Boolean = href == null && !annotations.any

    fun toPlaintext(): String = fixedPlainText
    fun toMarkdown(): String = buildString {
        val fullText = fixedPlainText
        val fullTextStartTrimmed = fullText.trimStart()
        val nspacesStart = fullText.length - fullTextStartTrimmed.length
        val fullTextEndTrimmed = fullTextStartTrimmed.trimEnd()
        val innerText = fullTextEndTrimmed
        val nspacesEnd = fullTextStartTrimmed.length - fullTextEndTrimmed.length
        val spacesStart = " ".repeat(nspacesStart)
        val spacesEnd = " ".repeat(nspacesEnd)

        append(spacesStart)
        if (annotations.strikethrough) append("~~")
        if (annotations.bold) append("**")
        if (annotations.italic) append("_")
        if (href != null && innerText != href) append("[")
        if (annotations.underline) append("<ins>")
        if (annotations.color != "default") append("<span style='color:${annotations.color}' markdown=1>")
        if (annotations.code) append("`")
        if (href == innerText) {
            append("<$innerText>")
        } else {
            append(innerText)
        }
        if (annotations.code) append("`")
        if (annotations.color != "default") append("</span>")
        if (annotations.underline) append("</ins>")
        if (href != null && fullText != href) append("]($href)")
        if (annotations.italic) append("_")
        if (annotations.bold) append("**")
        if (annotations.strikethrough) append("~~")
        append(spacesEnd)
    }

    fun toPlaintextAll(split: Boolean, maxLineLength: Int = MAX_LINE_LENGTH, ignoreLineBreaks: Boolean = IGNORE_LINE_BREAKS): String = Companion.toPlaintextAll(listOf(this), split, maxLineLength, ignoreLineBreaks)
    fun toMarkdownAll(split: Boolean, maxLineLength: Int = MAX_LINE_LENGTH, ignoreLineBreaks: Boolean = IGNORE_LINE_BREAKS): String = Companion.toMarkdownAll(listOf(this), split, maxLineLength, ignoreLineBreaks)

    class ChunkWithFormat(val chunk: String, val format: RichTextEntry) {
        //val length: Int = chunk.length + (format.href?.length ?: 0)
        val length: Int by lazy { format.copy(plain_text = chunk).toMarkdown().length }

        companion object {
            fun join(list: List<ChunkWithFormat>, split: Boolean, allowLineBreaks: Boolean): String {
                var out = ""
                var currentText = ""
                var currentFormat: RichTextEntry? = null
                fun flush() {
                    if (currentText.isEmpty()) return
                    var ctext = currentText
                    if (out.isEmpty()) ctext = ctext.trimStart()
                    if (ctext.isNotEmpty()) {
                        out += currentFormat!!.copy(plain_text = ctext).toMarkdown()
                    }
                    currentText = ""
                }
                for (item in list) {
                    if (item.format != currentFormat) {
                        flush()
                        currentFormat = item.format
                    }
                    currentText += item.chunk
                }
                flush()
                return out
            }
        }
    }

    //val allowSplit: Boolean = href == null
    val allowSplit: Boolean = href == null && !annotations.any

    companion object {
        val MAX_LINE_LENGTH = 120
        val IGNORE_LINE_BREAKS = false

        fun toMarkdownAll(items: List<RichTextEntry>, split: Boolean, maxLineLength: Int = MAX_LINE_LENGTH, ignoreLineBreaks: Boolean = IGNORE_LINE_BREAKS): String {
            val chunks = arrayListOf<ChunkWithFormat>()
            if (split) {
                for (item in items) {
                    val text = item.fixedPlainText.let { if (ignoreLineBreaks) it.replace("\n", " ") else it }
                    if (item.allowSplit) {
                        chunks += text.splitBySpaces().map { ChunkWithFormat(it, item) }
                    } else {
                        chunks += ChunkWithFormat(text, item)
                    }
                }
            } else {
                chunks += items.map { ChunkWithFormat(it.fixedPlainText, it) }
            }
            return joinChunks(chunks, split, maxLineLength, ignoreLineBreaks) { it.length }
                .map { ChunkWithFormat.join(it, split, ignoreLineBreaks).trim() }
                .filter { it.isNotEmpty() }
                .joinToString("\n")
        }

        fun toPlaintextAll(items: List<RichTextEntry>, split: Boolean, maxLineLength: Int = MAX_LINE_LENGTH, ignoreLineBreaks: Boolean = IGNORE_LINE_BREAKS): String {
            val chunks = arrayListOf<String>()
            for (item in items) {
                val text = item.fixedPlainText
                val ttext = if (ignoreLineBreaks) text.replace("\n", " ") else text
                chunks.addAll(ttext.splitInSeparatedChunks(maxLineLength))
            }
            return joinChunks(chunks, split, maxLineLength, ignoreLineBreaks) { it.length }.joinToString("\n") { it.joinToString("") }
        }

        private fun <T> joinChunks(chunks: List<T>, split: Boolean, maxLineLength: Int = MAX_LINE_LENGTH, ignoreLineBreaks: Boolean = IGNORE_LINE_BREAKS, length: (T) -> Int): List<List<T>> {
            if (!split) return listOf(chunks)
            val lines = arrayListOf<List<T>>()
            var line = arrayListOf<T>()
            var lineLength = 0
            fun flush() {
                if (line.isEmpty()) return
                lines += line
                line = arrayListOf<T>()
                lineLength = 0
            }
            for (chunk in chunks) {
                val chunkLength = length(chunk)
                if (lineLength + chunkLength >= maxLineLength) flush()
                line += chunk
                lineLength += chunkLength
            }
            flush()
            return lines
        }
    }
}

fun List<RichTextEntry>.toMarkdown(split: Boolean, allowLineBreaks: Boolean = RichTextEntry.IGNORE_LINE_BREAKS): String = RichTextEntry.toMarkdownAll(this, split, ignoreLineBreaks = allowLineBreaks)
fun List<RichTextEntry>.toPlaintext(split: Boolean, allowLineBreaks: Boolean = RichTextEntry.IGNORE_LINE_BREAKS): String = RichTextEntry.toPlaintextAll(this, split, ignoreLineBreaks = allowLineBreaks)
