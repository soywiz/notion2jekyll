package com.soywiz.notionapi.dto

import kotlin.test.*

class RichTextEntryTest {
    @Test
    fun test() {
        assertEquals(
            "hi **hello** world",
            listOf(
                RichTextEntry("hi", annotations = RichTextEntry.Annotations()),
                RichTextEntry(" hello ", annotations = RichTextEntry.Annotations(bold = true)),
                RichTextEntry("world ", annotations = RichTextEntry.Annotations())
            ).toMarkdown(split = true)
        )
    }
}