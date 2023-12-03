package com.soywiz.notionapi.dto

import com.vladsch.flexmark.ast.*
import com.vladsch.flexmark.ext.anchorlink.AnchorLink
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough
import com.vladsch.flexmark.ext.tables.TableBlock
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.parser.ParserEmulationProfile
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.data.MutableDataSet

object MarkdownToBlocks {
    val parser = Parser.builder(MutableDataSet().also { options ->
        options.setFrom(ParserEmulationProfile.KRAMDOWN)
        options.set(
            Parser.EXTENSIONS, listOf(
                //AbbreviationExtension.create(),
                //DefinitionExtension.create(),
                //FootnoteExtension.create(),
                //TablesExtension.create(),
                //TypographicExtension.create()
            )
        )
    }).build()

    fun markdownToBlocks(markdown: String): List<Block> {
        return parser.parse(markdown).toBlockBlocks()
    }

    fun Node.toRichText(annotations: RichTextEntry.Annotations = RichTextEntry.Annotations(), href: String? = null, out: ArrayList<IRichTextEntry> = arrayListOf()): List<IRichTextEntry> {
        val node = this
        for (item in node.childIterator) {
            when (item) {
                is Text -> out.add(RichTextEntry(item.chars.unescapeNoEntities(), href = href, annotations = annotations))
                is Emphasis -> item.toRichText(annotations.copy(italic = true), href, out)
                is StrongEmphasis -> item.toRichText(annotations.copy(bold = true, italic = true), href, out)
                is AnchorLink -> item.toRichText(annotations, href, out)
                is SoftLineBreak -> out.add(RichTextEntry("  \n"))
                is AutoLink ->
                    out.add(RichTextEntry(item.url.unescapeNoEntities(), href = item.url.unescapeNoEntities(), annotations = annotations))
                is Link -> item.toRichText(annotations, item.url.unescapeNoEntities(), out)
                is Code -> item.toRichText(annotations.copy(code = true), href, out)
                is Strikethrough -> item.toRichText(annotations.copy(strikethrough = true), href, out)
                is Image ->
                    out.add(RichTextEntryImage(item.text.unescapeNoEntities(), item.url.unescapeNoEntities(), item.title.unescapeNoEntities()))
                else -> TODO("item=$item")
            }
            //println("- item=$item")
        }
        return out
    }

    fun Node.toBlockBlocks(): List<Block> {
        val document = this
        val out = arrayListOf<Block>()
        for (item in document.childIterator) {
            var text: List<IRichTextEntry>? = null

            when (item) {
                is Paragraph -> {
                    text = item.toRichText()
                    val rtext = text.filterIsInstance<RichTextEntry>()
                    if (rtext.isNotEmpty()) {
                        out.add(ParagraphBlock(BaseTextualBlock.Paragraph(rtext, "default", null)))
                    }
                }
                is ListBlock -> {
                    var startNumber: Int? = null
                    var currentNumber: Int = 0
                    if (item is OrderedList) {
                        currentNumber = item.startNumber
                        startNumber = currentNumber
                    }
                    for (it in item.childIterator) {
                        val blocks2 = it.toBlockBlocks()
                        for (bb in blocks2) {
                            when (bb) {
                                is ParagraphBlock -> {
                                    if (startNumber != null) {
                                        out.add(NumberedListItem(bb.paragraph))
                                        currentNumber++
                                    } else {
                                        out.add(BulletedListItem(bb.paragraph))
                                    }
                                }
                                else -> TODO("bb=$bb")
                            }
                        }
                    }
                }
                is BlockQuote -> {
                    val blocks2 = item.toBlockBlocks()
                    for (it in blocks2) {
                        when (it) {
                            is ParagraphBlock -> {
                                out.add(QuoteBlock(it.paragraph))
                            }
                            else -> TODO("item=$it")
                        }
                    }
                }
                is Heading -> {
                    text = item.toRichText()
                    val ttext = text.filterIsInstance<RichTextEntry>()
                    val block = when (item.level) {
                        1 -> Heading1Block(HeadingBlock.Heading(ttext))
                        2 -> Heading2Block(HeadingBlock.Heading(ttext))
                        3 -> Heading3Block(HeadingBlock.Heading(ttext))
                        4 -> Heading4Block(HeadingBlock.Heading(ttext))
                        5 -> Heading5Block(HeadingBlock.Heading(ttext))
                        6 -> Heading6Block(HeadingBlock.Heading(ttext))
                        else -> TODO("level=${item.level}")
                    }
                    out.add(block)
                }
                is FencedCodeBlock -> {
                    val language = item.info.unescapeNoEntities()
                    val code = (item.firstChild as Text).chars.unescapeNoEntities()
                    out.add(CodeBlock(CodeBlock.Code(emptyList(), listOf(RichTextEntry(code)), language)))
                }
                is ThematicBreak -> {
                    out.add(DividerBlock(DividerBlock.Divider()))
                }
                is TableBlock -> {
                    val table = com.soywiz.notionapi.dto.TableBlock(com.soywiz.notionapi.dto.TableBlock.Table(1024, false, false))

                    for (it in item.childIterator) {
                        println("it=$it")
                    }

                    out.add(table)
                }
                else -> {
                    TODO("item=$item")
                }
            }

            if (text != null) {
                for (image in text.filterIsInstance<RichTextEntryImage>()) {
                    out.add(ImageBlock(NotionExternal(NotionExternal.External(image.url)).also {
                        it.caption = listOf(RichTextEntry(image.plain_text))
                    }))
                }
            }
        }
        return out
    }
}