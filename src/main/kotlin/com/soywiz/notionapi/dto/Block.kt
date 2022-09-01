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
    JsonSubTypes.Type(value = ToggleBlock::class, name = "toggle"),
    JsonSubTypes.Type(value = ToDoBlock::class, name = "to_do"),
    JsonSubTypes.Type(value = BulletedListItem::class, name = "bulleted_list_item"),
    JsonSubTypes.Type(value = NumberedListItem::class, name = "numbered_list_item"),
    JsonSubTypes.Type(value = QuoteBlock::class, name = "quote"),
    JsonSubTypes.Type(value = CalloutBlock::class, name = "callout"),
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
    JsonSubTypes.Type(value = TableBlock::class, name = "table"),
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

    @get:JsonIgnore
    open val group: Boolean get() = false

    override fun toString(): String = "Block(type=$type, id=$id)"
    open fun toMarkdown(context: BlockContext = BlockContext()): String = "Block($type)"
}

class BlockContext {
    var repeatedIndex: Int = 0
}

fun List<Block>.toMarkdown(): String {
    val out = StringBuilder()
    val context: BlockContext = BlockContext()
    for (n in indices) {
        val prevBlock = this.getOrNull(n - 1)
        val block = this[n]
        val nextBlock = this.getOrNull(n + 1)
        if (prevBlock != null && prevBlock::class == block::class) {
            context.repeatedIndex++
        } else {
            context.repeatedIndex = 0
        }
        out.append(block.toMarkdown(context))
        if (nextBlock != null && nextBlock::class == block::class && block.group) {
            out.append("\n")
        } else {
            out.append("\n\n")
        }
    }
    //return joinToString("\n\n") { it.toMarkdown() }
    return out.toString()
}

data class TableOfContentsBlock(var table_of_contents: TableOfContents) : Block() {
    data class TableOfContents(var color: String)

    override fun toMarkdown(context: BlockContext): String = "## Table of Contents\n{:toc}"
}

open class BaseTextualBlock : Block() {
    data class Paragraph(
        var rich_text: List<RichTextEntry>,
        var color: String,
        val checked: Boolean?,
    ) {
        fun toMarkdown() = rich_text.toMarkdown()
    }
}

data class ToggleBlock(var toggle: Paragraph) : BaseTextualBlock() {
    override fun toMarkdown(context: BlockContext): String {
        val str = StringBuilder()
        str.appendLine("<details markdown=1>")
        str.appendLine("<summary markdown=1>${toggle.rich_text.toMarkdown()}</summary>")
        str.appendLine() // <-- content here after retrieving children
        str.appendLine("</details>")
        return str.toString()
    }
}

data class TableBlock(var table: Table) : BaseTextualBlock() {
    data class Table(val table_width: Int, val has_column_header: Boolean, val has_row_header: Boolean)
    override fun toMarkdown(context: BlockContext): String {
        return "<table><!-- TODO children --></table>"
    }
}

data class ParagraphBlock(var paragraph: Paragraph) : BaseTextualBlock() {
    override fun toMarkdown(context: BlockContext): String = paragraph.rich_text.toMarkdown()
}

data class ToDoBlock(var to_do: Paragraph) : BaseTextualBlock() {
    override fun toMarkdown(context: BlockContext): String {
        val checkbox = if (to_do.checked == true) "✅" else "🟩"
        return "- $checkbox " + to_do.rich_text.toMarkdown()
    }

    @get:JsonIgnore
    override val group: Boolean get() = true
}

data class BulletedListItem(var bulleted_list_item: Paragraph) : BaseTextualBlock() {
    override fun toMarkdown(context: BlockContext): String = "* " + bulleted_list_item.rich_text.toMarkdown()

    @get:JsonIgnore
    override val group: Boolean get() = true
}

data class NumberedListItem(var numbered_list_item: Paragraph) : BaseTextualBlock() {
    override fun toMarkdown(context: BlockContext): String = "${context.repeatedIndex + 1}. " + numbered_list_item.rich_text.toMarkdown()

    @get:JsonIgnore
    override val group: Boolean get() = true
}


data class QuoteBlock(var quote: Paragraph) : BaseTextualBlock() {
    override fun toMarkdown(context: BlockContext): String = "> " + quote.rich_text.toMarkdown()
}

data class CalloutBlock(var callout: Paragraph) : BaseTextualBlock() {
    override fun toMarkdown(context: BlockContext): String = "> " + callout.rich_text.toMarkdown()
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

    override fun toMarkdown(context: BlockContext): String = "#".repeat(count) + " " + heading.toMarkdown()
}

data class DividerBlock(var divider: Divider) : Block() {
    class Divider

    override fun toMarkdown(context: BlockContext): String = "---"
}

data class VideoBlock(var video: Video) : Block() {
    data class Video(
        var caption: List<RichTextEntry>,
        var type: String,
        var external: External?
    )

    data class External(var url: String)

    override fun toMarkdown(context: BlockContext): String {
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

    data class ImageFile(
        var url: String,
        var expiry_time: String?
    )

    override fun toMarkdown(context: BlockContext): String = "![${image.caption.toMarkdown()}](${image.file?.url})"
}

data class CodeBlock(
    var code: Code
) : Block() {
    data class Code(
        var caption: List<Any?>,
        var rich_text: List<RichTextEntry>,
        var language: String,
    )

    override fun toMarkdown(context: BlockContext): String = buildString {
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
