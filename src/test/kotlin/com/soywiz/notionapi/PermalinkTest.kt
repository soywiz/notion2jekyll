package com.soywiz.notionapi

import org.junit.Test
import kotlin.test.*

class PermalinkTest {
    @Test
    fun test() {
        assertEquals("hello-world", permalink("Hello World!"))
        assertEquals("t-this-is-a-long-test-awesome-aaa", permalink("T!!%this is a long test awesome ááá"))
    }
}