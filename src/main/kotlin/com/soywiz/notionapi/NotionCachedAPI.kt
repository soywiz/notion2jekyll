package com.soywiz.notionapi

import com.fasterxml.jackson.annotation.*
import com.soywiz.notionapi.dto.*
import com.soywiz.util.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.net.*
import java.text.*
import java.util.*
import kotlin.reflect.*

// SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy").parse("Tue Aug 30 10:25:00 CEST 2022")
class NotionCachedAPI(val api: NotionAPI, val folder: File = File("./.notion_cache")) : Closeable {
    val imagesFolder = File(folder, "images")
    val filesFolder = File(folder, "files")
    //val imagesFolder = File(folder, "../static/images")
    //val filesFolder = File(folder, "../static/files")
    val databasesFolder = File(folder, "databases")
    val pagesFolder = File(folder, "pages")
    init {
        folder.mkdirs()
        imagesFolder.mkdirs()
        filesFolder.mkdirs()
        databasesFolder.mkdirs()
        pagesFolder.mkdirs()
    }

    suspend fun getDatabase(databaseId: String): DatabaseInfo {
        val databaseId = File(databaseId).nameWithoutExtension
        val cachedFile = File(databasesFolder, "$databaseId.database")
        fun readDatabase() = api.mapper.readValue(cachedFile.readText(), DatabaseInfo::class.java)
        suspend fun generate() {
            val now = System.currentTimeMillis()
            val fileTime = cachedFile.lastModified()
            //if (cachedFile.exists() && now < fileTime + 3600 * 1000) {
            //    println("Database cached. File exists and modified in less than 1 hour now=$now, fileTime=$fileTime")
            //    return
            //}
            println("Database not cached: now=$now, fileTime=$fileTime")

            val database = api.databaseGet(databaseId)

            val cachedDatabase = if (cachedFile.exists()) readDatabase() else null
            if (cachedDatabase?.database?.last_edited_time_sure == database.last_edited_time_sure) {
                println("Database pages already cached since the database was not updated")
            } else {
                println("Database pages not cached")
                val pages = api.databaseQuery(databaseId)
                val databaseInfo = DatabaseInfo(database, pages.toList().filterIsInstance<Page>())
                cachedFile.writeText(api.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(databaseInfo))
            }

            cachedFile.setLastModified(now)
        }
        generate()
        return readDatabase()
    }

    suspend fun getFullPage(page: Page): PageInfo = getFullPage(page.id, page.last_edited_time)

    suspend fun getFullPage(pageId: String, last_edited_time: Date): PageInfo {
        val pageId = File(pageId).nameWithoutExtension
        val cachedFile = File(pagesFolder, "$pageId.page")
        if (!cachedFile.exists() || cachedFile.lastModified() != last_edited_time.time) {
            println("Didn't hit cache for $cachedFile and ${last_edited_time.time}")
            val page = api.pageGet(pageId)
            val props = page.properties
            //val props = page.properties.entries.associate {
            //    val propList = api.pageGetProperty(pageId, it.value.id).toList()
            //    //println("[PROP] : key[${it.key}]=${propList}")
            //    it.key to propList
            //}
            val blocks = page.let { api.blocksChildren(pageId).toList() }
            val pageInfo = PageInfo(page, blocks)

            val coverImage = pageInfo.page.cover

            fun downloadContent(url: String, folder: String, defaultExt: String): String {
                val url = URL(url)
                val urlPath = File(url.path)
                val baseName = File(url.path.replace("/", "-"))
                    .nameWithoutExtension
                    .replace("secure.notion-static.com-", "")
                    .trim('-')
                val ext = urlPath.extension.takeIf { it.isNotBlank() } ?: defaultExt
                val fileName = "$baseName.$ext"
                val localFile = File(if (folder == "files") filesFolder else imagesFolder, fileName)
                if (!localFile.exists()) {
                    println("download=$url")
                    localFile.writeBytes(url.readBytes())
                }
                return "/$folder/$fileName"
            }

            fun downloadImage(url: String): String {
                return downloadContent(url, "images", "jpg")
            }

            fun downloadFile(url: String): String {
                return downloadContent(url, "files", "bin")
            }

            if (coverImage != null) {
                coverImage.url = downloadImage(coverImage.url)
            }

            for (block in pageInfo.blocks.filterIsInstance<ImageBlock>()) {
                block.image.url = downloadImage(block.image.url)
            }
            for (block in pageInfo.blocks.filterIsInstance<FileBlock>()) {
                block.file.url = downloadFile(block.file.url)
            }

            val pageInfoString = api.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(pageInfo)
            cachedFile.writeText(pageInfoString)
            val newPage = api.mapper.readValue(cachedFile.readText(), PageInfo::class.java)
            //println("$page,$newPage")
            cachedFile.setLastModified(last_edited_time.time)
        }
        return api.mapper.readValue(cachedFile.readText(), PageInfo::class.java)
    }

    override fun close() {
        api.close()
    }
}

data class DatabaseInfo(
    val database: Database,
    val pages: List<Page>
)

data class PageInfo(
    val page: Page,
    val blocks: List<Block>
) {
    @get:JsonIgnore
    val props = page.properties.entries.associate { it.key to kotlin.collections.listOf(it.value) }

    inline fun <reified T> findPropOfType(): List<Map.Entry<String, List<PropertyItem>>> =
        props.entries.filter { it.value.first() is T }

    @get:JsonIgnore
    val propsLC by lazy { props.entries.associate { it.key.lowercase().trim() to it.value.toList() } }
    @get:JsonIgnore
    val title: String get() = findPropOfType<TitlePropertyItem>().firstOrNull()?.value?.toPlaintext() ?: ""
    @get:JsonIgnore
    val permalink: String get() = findPropOfType<RichTextPropertyItem>().firstOrNull { it.key.equals("permalink", ignoreCase = true) }?.value?.toPlaintext()?.takeIf { it.isNotBlank() } ?: permalink(title)
    @get:JsonIgnore
    val sponsor: String get() = propsLC["sponsor"]?.toPlaintext() ?: ""
    @get:JsonIgnore
    val category: String get() = propsLC["category"]?.toPlaintext() ?: ""
    @get:JsonIgnore
    val created: Date get() = DateParse(findPropOfType<CreatedTimePropertyItem>().firstOrNull()?.value?.toPlaintext() ?: "")
    @get:JsonIgnore
    val published: Date? get() = propsLC["published"]?.toPlaintext()?.substringBefore('+')?.let {
        //println("page.id=${page.id}")
        //println("propsLC[\"published\"]?.toPlaintext()=${propsLC["published"]?.toPlaintext()}")
        //println("propsLC[\"published\"]=${propsLC["published"]}")
        if (it.isBlank()) null else DateParse(it)
    }
    @get:JsonIgnore
    val draft: Boolean get() = propsLC["draft"]?.toPlaintext() == "true"
    @get:JsonIgnore
    val tags: String get() = propsLC["tags"]?.toPlaintext() ?: ""

    @get:JsonIgnore
    val cover: String? by lazy {
        page.cover?.url
    }

    @get:JsonIgnore
    val allImages: List<String> by lazy {
        blocks.filterIsInstance<ImageBlock>().map { it.image.url }
    }

    @get:JsonIgnore
    val allFiles: List<String> by lazy {
        blocks.filterIsInstance<FileBlock>().map { it.file.url }
    }

    @get:JsonIgnore
    val featured: String? by lazy {
        cover ?: allImages.firstOrNull()
    }

    @get:JsonIgnore
    val allImagesAndCover: List<String> by lazy {
        (allImages + cover).filterNotNull()
    }

    @get:JsonIgnore
    val contentMarkdown: String get() = blocks.toMarkdown()
}

fun permalink(input: String): String {
    return Normalizer.normalize(input, Normalizer.Form.NFD)
        .replace("\u0301", "")
        .replace(Regex("\\W+"), "-")
        .lowercase()
        .trim('-')
}

