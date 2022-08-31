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

fun Iterable<Block>.toMarkdown() = joinToString("\n\n") { it.toMarkdown() }

data class TableOfContentsBlock(var table_of_contents: TableOfContents) : Block() {
    data class TableOfContents(var color: String)

    override fun toMarkdown(): String = "## Table of Contents\n{:toc}"
}

data class ParagraphBlock(var paragraph: Paragraph) : Block() {
    data class Paragraph(
        var rich_text: List<RichTextEntry>,
        var color: String,
    ) {
        fun toMarkdown() = rich_text.toMarkdown()
    }

    override fun toMarkdown(): String = paragraph.rich_text.toMarkdown()
}

abstract class HeadingBlock(var count: Int) : Block() {
    abstract var heading: Heading

    data class Heading(
        var rich_text: List<RichTextEntry>,
        var is_toggleable: Boolean,
        var color: String,
    ) {
        fun toMarkdown() = RichTextEntry.toMarkdown(rich_text)
    }

    override fun toMarkdown(): String = "#".repeat(count) + " " + heading.toMarkdown()
}

data class DividerBlock(var divider: Divider) : Block() {
    class Divider

    override fun toMarkdown(): String = "---"
}

data class VideoBlock(var video: Video) : Block() {
    data class Video(
        var caption: List<RichTextEntry>,
        var type: String,
        var external: External?
    )

    data class External(var url: String)

    override fun toMarkdown(): String {
        val url = video.external?.url?.replace("watch?v=", "embed/")
        return """<iframe width="560" height="315" src="$url" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>"""
    }
}

data class ImageBlock(var image: Image) : Block() {
    data class Image(
        var caption: List<RichTextEntry>,
        var type: String,
        var file: ImageFile?
    )

    data class ImageFile(var url: String, var expiry_time: String)

    override fun toMarkdown(): String = "![${image.caption.toMarkdown()}](${image.file?.url})"
}

data class CodeBlock(
    var code: Code
) : Block() {
    data class Code(
        var caption: List<Any?>,
        var rich_text: List<RichTextEntry>,
        var language: String,
    )

    override fun toMarkdown(): String = buildString {
        append("```${code.language}\n")
        append(code.rich_text.toMarkdown().trim())
        append("\n```")
    }
}

data class Heading1Block(var heading_1: Heading) : HeadingBlock(1) {
    override var heading by ::heading_1
}

data class Heading2Block(var heading_2: Heading) : HeadingBlock(2) {
    override var heading by ::heading_2
}

data class Heading3Block(var heading_3: Heading) : HeadingBlock(3) {
    override var heading by ::heading_3
}

data class Heading4Block(var heading_4: Heading) : HeadingBlock(4) {
    override var heading by ::heading_4
}

data class Heading5Block(var heading_5: Heading) : HeadingBlock(5) {
    override var heading by ::heading_5
}

data class Heading6Block(var heading_6: Heading) : HeadingBlock(6) {
    override var heading by ::heading_6
}
