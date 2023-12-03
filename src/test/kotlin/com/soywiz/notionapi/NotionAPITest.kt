package com.soywiz.notionapi

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import okhttp3.Protocol
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.internal.http.RealResponseBody
import okio.Buffer
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

    @Test
    fun testDownloadPage() = runTest {
        val notion = NotionAPIFromResources()
        val page = notion.pageGet("425b0a161b064e2992bdeeb0b2ecbae6")
        assertEquals("https://www.notion.so/ALL-STUFF-TEST-425b0a161b064e2992bdeeb0b2ecbae6", page.url)

        val pageBlocks = notion.blocksChildren(page.id).toList()

        println("page=$page")
        for (block in pageBlocks) {
            println("block=$block")
        }
    }

    class NotionAPIFromResources() : NotionAPI("FakeBearer") {
        val loader = NotionAPIFromResources::class.java.classLoader

        companion object {
            val REGEX_BLOCK_CHILDREN = Regex("^https://api.notion.com/v1/blocks/(.+)/children.*\$")
        }

        override suspend fun httpRequest(url: String, headers: Headers, body: RequestBody?): Response {
            val response = Response.Builder()
            response.protocol(Protocol.HTTP_1_1).code(200).message("OK").request(buildHttpRequest(url, headers, body))

            when {
                url.startsWith("https://api.notion.com/v1/pages/") -> {
                    val id = url.substringAfterLast("/")
                    val bytes = loader.getResource("page.$id.json").readBytes()
                    val buffer = Buffer().write(bytes)
                    response.body(RealResponseBody(
                        "application/json", buffer.size, buffer
                    ))

                }
                REGEX_BLOCK_CHILDREN.matches(url) -> {
                    val result = REGEX_BLOCK_CHILDREN.matchEntire(url) ?: error("Not matching")
                    val id = result.groupValues[1]
                    val bytes = loader.getResource("blocks.$id.json").readBytes()
                    val buffer = Buffer().write(bytes)
                    response.body(RealResponseBody(
                        "application/json", buffer.size, buffer
                    ))
                }
                else -> TODO("url=$url")
            }

            return response.build()
        }
    }
}
