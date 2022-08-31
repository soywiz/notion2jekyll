package com.soywiz.notionapi

import com.soywiz.notionapi.dto.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.net.URL
import java.text.*
import java.util.*
import javax.xml.crypto.Data

// SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy").parse("Tue Aug 30 10:25:00 CEST 2022")
class NotionCachedAPI(val api: NotionAPI, val folder: File = File("./.notion_cache")) {
    val imagesFolder = File(folder, "images")
    val databasesFolder = File(folder, "databases")
    val pagesFolder = File(folder, "pages")
    init {
        folder.mkdirs()
        imagesFolder.mkdirs()
        databasesFolder.mkdirs()
        pagesFolder.mkdirs()
    }

    suspend fun getDatabase(databaseId: String): DatabaseInfo {
        val databaseId = File(databaseId).nameWithoutExtension
        val cachedFile = File(databasesFolder, "$databaseId.database")
        if (!cachedFile.exists()) {
            val database = api.databaseGet(databaseId)
            val pages = api.databaseQuery(databaseId)
            val databaseInfo = DatabaseInfo(database, pages.toList().filterIsInstance<Page>())
            cachedFile.writeText(api.mapper.writeValueAsString(databaseInfo))
        }
        return api.mapper.readValue(cachedFile.readText(), DatabaseInfo::class.java)
    }

    data class DatabaseInfo(
        val database: Database?,
        val pages: List<Page>
    )

    suspend fun getFullPage(page: Page): PageInfo = getFullPage(page.id, page.last_edited_time)

    suspend fun getFullPage(pageId: String, last_edited_time: Date): PageInfo {
        val pageId = File(pageId).nameWithoutExtension
        val cachedFile = File(pagesFolder, "$pageId.page")
        if (!cachedFile.exists() || cachedFile.lastModified() != last_edited_time.time) {
            println("Didn't hit cache for $cachedFile and ${last_edited_time.time}")
            val page = api.pageGet(pageId)
            val props = page?.properties?.entries?.associate { it.key to api.pageGetProperty(pageId, it.value.id).toList() }
            val blocks = page?.let { api.blocksChildren(pageId).toList() }
            val pageInfo = PageInfo(page, props, blocks)

            if (pageInfo.blocks != null) {
                for (block in pageInfo.blocks.filterIsInstance<ImageBlock>()) {
                    val url = block.image.file?.url?.let { URL(it) } ?: continue
                    val fileName = File(url.path).name
                    val localFile = File(imagesFolder, fileName)
                    if (!localFile.exists()) {
                        localFile.writeBytes(url.readBytes())
                    }
                    block.image.file?.url = "images/$fileName"
                }
            }

            cachedFile.writeText(api.mapper.writeValueAsString(pageInfo))
            cachedFile.setLastModified(last_edited_time.time)
        }
        return api.mapper.readValue(cachedFile.readText(), PageInfo::class.java).also { pageInfo ->

        }
    }

    data class PageInfo(
        val page: Page?,
        val props: Map<String, List<PropertyItem>>?,
        val blocks: List<Block>?
    ) {
        inline fun <reified T> findPropOfType(): List<Map.Entry<String, List<PropertyItem>>> =
            props?.entries?.filter { it.value.first() is T } ?: emptyList()

        val propsLC by lazy { props?.entries?.associate { it.key.lowercase().trim() to it.value } ?: emptyMap() }
        val title: String get() = findPropOfType<TitlePropertyItem>().firstOrNull()?.value?.toPlaintext() ?: ""
        val permalink: String get() = findPropOfType<RichTextPropertyItem>().firstOrNull { it.key.equals("permalink", ignoreCase = true) }?.value?.toPlaintext() ?: permalink(title)
        val sponsor: String get() = propsLC["sponsor"]?.toPlaintext() ?: ""
        val category: String get() = propsLC["category"]?.toPlaintext() ?: ""
        val created: String get() = findPropOfType<CreatedTimePropertyItem>().firstOrNull()?.value?.toPlaintext() ?: ""
        val published: String get() = propsLC["published"]?.toPlaintext()?.substringBefore('+') ?: created
        val tags: String get() = propsLC["tags"]?.toPlaintext() ?: ""

        val contentMarkdown: String get() = blocks?.toMarkdown() ?: ""
    }
}

fun permalink(input: String): String {
    return Normalizer.normalize(input, Normalizer.Form.NFD)
        .replace("\u0301", "")
        .replace(Regex("\\W+"), "-")
        .lowercase()
        .trim('-')
}

