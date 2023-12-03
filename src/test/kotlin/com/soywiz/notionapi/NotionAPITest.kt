package com.soywiz.notionapi

import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import okhttp3.Protocol
import okhttp3.RequestBody
import okhttp3.Response
import kotlin.test.Test
import kotlin.test.assertEquals

class NotionAPITest {
    @Test
    fun testRateLimited(): Unit = runTest {
        val log = arrayListOf<String>()
        val api = object : NotionAPI("MyBearer") {
            override suspend fun httpRequest(url: String, headers: Headers, body: RequestBody?): Response {
                return Response.Builder()
                    .protocol(Protocol.HTTP_1_1)
                    .request(buildHttpRequest(url, headers, body))
                    .also {
                        if (log.isEmpty()) {
                            it.code(429).message("Rate Limited").header("Retry-After", "25")
                        } else {
                            it.code(200).message("Ok")
                        }
                    }
                    .build()
                    .also {
                        log.add("url=$url, headers=[${headers.joinToString(", ")}], body=$body")
                    }
            }
        }
        val id = "123"
        assertEquals(0, this.currentTime)
        val body = api.request("databases/$id")
        assertEquals(25_000, this.currentTime)
        assertEquals("""
            url=https://api.notion.com/v1/databases/123, headers=[(Authorization, Bearer MyBearer), (Notion-Version, 2022-06-28)], body=null
            url=https://api.notion.com/v1/databases/123, headers=[(Authorization, Bearer MyBearer), (Notion-Version, 2022-06-28)], body=null
        """.trimIndent(), log.joinToString("\n"))
    }
}