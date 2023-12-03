package com.soywiz.notionapi.dto

import org.junit.Test
import kotlin.test.assertEquals

class MarkdownToBlocksTest {
    @Test
    fun testSimple() {
        assertEquals("hello _my _**_world_**", markdownProcess("hello _my **world**_"))
    }

    @Test
    fun testHeader() {
        assertEquals("# hello _world_", markdownProcess("# hello *world*"))
        assertEquals("## hello _world_", markdownProcess("## hello *world*"))
    }

    @Test
    fun testQuote() {
        assertEquals("> hi", markdownProcess("> hi"))
    }

    @Test
    fun testAttributes() {
        assertEquals("he~~`ll`o wor~~ld", markdownProcess("he~~`ll`o wor~~ld"))
    }

    @Test
    fun testCode() {
        val code = """
            ```kotlin
            fun test() = 10
            
            fun demo() = 20
            ```
        """.trimIndent()
        assertEquals(code, markdownProcess(code))
    }

    @Test
    fun testLink() {
        val code = "[hello](world)"
        assertEquals(code, markdownProcess(code))
    }

    @Test
    fun testLink2() {
        val code = "<http://link>"
        assertEquals(code, markdownProcess(code))
    }

    @Test
    fun testImage() {
        val code = "![hello](world)"
        assertEquals(code, markdownProcess(code))
    }

    @Test
    fun testUnorderedList() {
        val code = "* one\n* two\n* three"
        assertEquals(code, markdownProcess(code))
    }

    @Test
    fun testOrderedList() {
        val code = "1. one\n2. two\n3. three"
        assertEquals(code, markdownProcess(code))
    }

    @Test
    fun testTwoParagraph() {
        assertEquals("1\n\n2", markdownProcess("1\n\n2\n\n"))
    }

    private fun markdownProcess(str: String): String =
        MarkdownToBlocks.markdownToBlocks(str).toMarkdown().trim()
}
