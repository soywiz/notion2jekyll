package com.soywiz.notionapi

import com.soywiz.util.*
import org.junit.Test
import kotlin.test.*

class ChunkSplitterTest {
    @Test
    fun testSplitBySpaces() {
        assertEquals(listOf(), "".splitBySpaces())
        assertEquals(listOf(" ", "hello", " "), " hello ".splitBySpaces())
        assertEquals(listOf("hello", " ", "this", " ", "is", " ", "a", "   ", "test"), "hello this is a   test".splitBySpaces())
    }

    @Test
    fun testSplitInSeparatedChunks() {
        assertEquals(listOf("hello this is a   test"), "hello this is a   test".splitInSeparatedChunks(100))
        assertEquals(listOf("hello this is a", "   test"), "hello this is a   test".splitInSeparatedChunks(16))
    }
}