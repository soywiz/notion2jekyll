package com.soywiz.notionapi.dto

import com.fasterxml.jackson.annotation.*
import java.util.*

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    defaultImpl = Block::class,
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ParagraphBlock::class, name = "paragraph"),
    JsonSubTypes.Type(value = DividerBlock::class, name = "divider"),
    JsonSubTypes.Type(value = ImageBlock::class, name = "image"),
    JsonSubTypes.Type(value = VideoBlock::class, name = "video"),
    JsonSubTypes.Type(value = Heading1Block::class, name = "heading_1"),
    JsonSubTypes.Type(value = Heading2Block::class, name = "heading_2"),
    JsonSubTypes.Type(value = Heading3Block::class, name = "heading_3"),
    JsonSubTypes.Type(value = Heading4Block::class, name = "heading_4"),
    JsonSubTypes.Type(value = Heading5Block::class, name = "heading_5"),
    JsonSubTypes.Type(value = Heading6Block::class, name = "heading_6"),
    JsonSubTypes.Type(value = TableOfContentsBlock::class, name = "table_of_contents"),
    JsonSubTypes.Type(value = CodeBlock::class, name = "code"),
)
open class Block(
) : NObject() {
    var id: String = ""
    var created_time: Date = Date()
    var last_edited_time: Date = Date()
    var created_by: PartialUser = PartialUser("")
    var last_edited_by: PartialUser = PartialUser("")
    var has_children: Boolean = false
    var archived: Boolean = false
    var type: String = ""
    override fun toString(): String = "Block(type=$type, id=$id)"
    open fun toMarkdown(): String = "Block($type)"
}

data class TableOfContentsBlock(val table_of_contents: TableOfContents) : Block() {
    data class TableOfContents(val color: String)

    override fun toMarkdown(): String = "{:toc}"
}

data class ParagraphBlock(val paragraph: Paragraph) : Block() {
    data class Paragraph(
        val rich_text: List<RichTextEntry>,
        val color: String,
    ) {
        fun toMarkdown() = rich_text.toMarkdown()
    }

    override fun toMarkdown(): String = paragraph.rich_text.toMarkdown()
}

abstract class HeadingBlock(val count: Int) : Block() {
    abstract val heading: Heading

    data class Heading(
        val rich_text: List<RichTextEntry>,
        val is_toggleable: Boolean,
        val color: String,
    ) {
        fun toMarkdown() = RichTextEntry.toMarkdown(rich_text)
    }

    override fun toMarkdown(): String = "#".repeat(count) + " " + heading.toMarkdown()
}

data class DividerBlock(val divider: Divider) : Block() {
    class Divider

    override fun toMarkdown(): String = "---"
}

data class VideoBlock(val video: Video) : Block() {
    data class Video(
        val caption: List<RichTextEntry>,
        val type: String,
        val external: External?
    )

    data class External(val url: String)

    override fun toMarkdown(): String = "<iframe width=\"420\" height=\"315\" src=\"${video.external?.url}\"></iframe>"
}

data class ImageBlock(val image: Image) : Block() {
    data class Image(
        val caption: List<RichTextEntry>,
        val type: String,
        val file: File?
    )

    data class File(var url: String, val expiry_time: String)

    override fun toMarkdown(): String = "[${image.caption.toMarkdown()}](${image.file?.url})"
}

data class CodeBlock(
    val code: Code
) : Block() {
    data class Code(
        val caption: List<Any?>,
        val rich_text: List<RichTextEntry>,
        val language: String,
    )

    override fun toMarkdown(): String = buildString {
        append("```${code.language}\n")
        append(code.rich_text.toMarkdown())
        append("```")
    }
}

data class Heading1Block(val heading_1: Heading) : HeadingBlock(1) {
    override val heading get() = heading_1
}

data class Heading2Block(val heading_2: Heading) : HeadingBlock(2) {
    override val heading get() = heading_2
}

data class Heading3Block(val heading_3: Heading) : HeadingBlock(3) {
    override val heading get() = heading_3
}

data class Heading4Block(val heading_4: Heading) : HeadingBlock(4) {
    override val heading get() = heading_4
}

data class Heading5Block(val heading_5: Heading) : HeadingBlock(5) {
    override val heading get() = heading_5
}

data class Heading6Block(val heading_6: Heading) : HeadingBlock(6) {
    override val heading get() = heading_6
}
