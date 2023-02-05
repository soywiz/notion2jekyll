package com.soywiz.notionapi

import com.soywiz.notionapi.dto.*
import org.junit.Test
import kotlin.test.*

class RichTextEntryTest {
    val entry = RichTextEntry("hello world")
    val entryBold = RichTextEntry("hello world", annotations = RichTextEntry.Annotations(bold = true))
    val entryLong = RichTextEntry("hello world, this is a test to see how performs splitting with markdown and plain text")
    val entryLongBreak = RichTextEntry("hello world,\nthis is a test\nto see how performs splitting\nwith markdown and plain text")

    @Test
    fun testSplit() {
        assertEquals("hello\nworld", entry.toMarkdownAll(split = true, maxLineLength = 8))
        assertEquals("hello world", entry.toMarkdownAll(split = true, maxLineLength = 32))

        assertEquals("**hello**\n**world**", entryBold.toMarkdownAll(split = true, maxLineLength = 8, ignoreLineBreaks = false))
        assertEquals("**hello world**", entryBold.toMarkdownAll(split = true, maxLineLength = 32))
    }

    @Test
    fun testNoSplit() {
        assertEquals("hello world", entry.toMarkdownAll(split = false, maxLineLength = 8))
        assertEquals("hello world", entry.toMarkdownAll(split = false, maxLineLength = 32))

        assertEquals("**hello world**", entryBold.toMarkdownAll(split = false, maxLineLength = 8))
        assertEquals("**hello world**", entryBold.toMarkdownAll(split = false, maxLineLength = 32))

        assertEquals("hello world", entry.toMarkdownAll(split = false, maxLineLength = 8))
    }

    @Test
    fun testText() {
        assertEquals("hello world", entry.toPlaintextAll(split = false, maxLineLength = 8))
        assertEquals("hello world", entry.toPlaintextAll(split = false, maxLineLength = 32))

        assertEquals("hello world", entryBold.toPlaintextAll(split = false, maxLineLength = 8))
        assertEquals("hello world", entryBold.toPlaintextAll(split = false, maxLineLength = 32))

        assertEquals("hello world", entry.toPlaintextAll(split = false, maxLineLength = 8))
    }

    @Test
    fun testText2() {
        assertEquals(
            """
                hello world, this is a test to 
                see how performs splitting with 
                markdown and plain text
            """.trimIndent(),
            entryLong.toPlaintextAll(split = true, maxLineLength = 32, ignoreLineBreaks = false)
        )
        assertEquals(
            """
                hello world, this is a test to 
                see how performs splitting with 
                markdown and plain text
            """.trimIndent(),
            entryLongBreak.toPlaintextAll(split = true, maxLineLength = 32, ignoreLineBreaks = true)
        )
        assertEquals(
            """
                hello world,
                this is a test
                to 
                see how performs splitting
                with 
                markdown and plain text
            """.trimIndent(),
            entryLongBreak.toPlaintextAll(split = true, maxLineLength = 32, ignoreLineBreaks = false)
        )
    }
}