package com.soywiz.notionapi

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.kotlin.*
import com.soywiz.notionapi.dto.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import ru.gildor.coroutines.okhttp.*

class NotionAPI(private val bearer: String) {
    val mapper: ObjectMapper get() = objectMapper

    companion object {
        @PublishedApi internal val MediaTypeApplicationJson = "application/json".toMediaType()
        @PublishedApi internal val objectMapper = jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
    }

    private val client = OkHttpClient()
    val notionVersion = "2022-06-28"

    suspend fun pageGet(pageId: String): Page? {
        val body = request("pages/$pageId").body ?: return null
        return objectMapper.readValue(body.string())
    }

    suspend fun pageGetProperty(pageId: String, propertyId: String): Flow<PropertyItem> = paginate<PropertyItem> { cursor ->
        request("pages/$pageId/properties/$propertyId?page_size=1000" + (if (cursor != null) "&start_cursor=$cursor" else ""))
    }

    suspend fun databaseGet(id: String): Database? {
        val body = request("databases/$id").body ?: return null
        return objectMapper.readValue(body.string())
    }

    suspend fun databaseQuery(id: String, query: Map<String, Any?> = mapOf()): Flow<NObject> = paginate<NObject> { cursor ->
        val rquery = query + LinkedHashMap<String, Any?>().apply {
            this["page_size"] = 1000
            if (cursor != null) this["start_cursor"] = cursor
        }
        //println("QUERY: $rquery")
        request("databases/$id/query", body = objectMapper.writeValueAsString(rquery).toRequestBody(MediaTypeApplicationJson))
    }

    suspend fun blocksChildren(blockId: String): Flow<Block> = paginate<Block> { cursor ->
        request("blocks/$blockId/children?page_size=1000" + (if (cursor != null) "&start_cursor=$cursor" else ""))
    }

    inline suspend fun <reified T> paginate(noinline request: suspend (cursor: String?) -> Response): Flow<T> = flow {
        var cursor: String? = null
        while (true) {
            val res = request(cursor).body ?: return@flow
            val str = res.string()
            val obj = objectMapper.readValue<SimpleObject>(str)
            when (obj.`object`) {
                "error" -> {
                    throw NError(objectMapper.readValue<NErrorObject>(str))
                }

                "list" -> {
                    val result = objectMapper.readValue<NListObject<T>>(str)
                    res.close()
                    for (result in result.results) {
                        emit(result)
                    }
                    cursor = result.next_cursor
                    if (result.has_more) continue
                }

                else -> {
                    emit(objectMapper.readValue<T>(str))
                }
            }

            break
        }
    }

    suspend fun request(path: String, body: RequestBody? = null): Response {
        val request = Request.Builder()
            .url("https://api.notion.com/v1/$path")
            .header("Authorization", "Bearer $bearer")
            .header("Notion-Version", notionVersion)
            .also { if (body != null) it.post(body) }
            .build()
        return client.newCall(request).await()
    }

    data class SimpleObject(val `object`: String? = null)
}
