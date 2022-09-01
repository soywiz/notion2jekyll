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

            fun downloadImage(field: KMutableProperty0<String>) {
                val url = URL(field.get())
                val urlPath = File(url.path)
                val baseName = File(url.path.replace("/", "-"))
                    .nameWithoutExtension
                    .replace("secure.notion-static.com-", "")
                    .trim('-')
                val ext = urlPath.extension.takeIf { it.isNotBlank() } ?: "jpg"
                val fileName = "$baseName.$ext"
                val localFile = File(imagesFolder, fileName)
                if (!localFile.exists()) {
                    println("downloadImage=$url")
                    localFile.writeBytes(url.readBytes())
                }
                field.set("/images/$fileName")

            }

            if (coverImage != null) {
                when (coverImage) {
                    is NotionExternal -> downloadImage(coverImage.external::url)
                    is NotionFile -> {
                        downloadImage(coverImage.file::url)
                        coverImage.file.expiry_time = null
                    }
                }
            }

            for (block in pageInfo.blocks.filterIsInstance<ImageBlock>()) {
                val ifile = block.image.file ?: continue
                downloadImage(ifile::url)
                ifile.expiry_time = null
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
    val permalink: String get() = findPropOfType<RichTextPropertyItem>().firstOrNull { it.key.equals("permalink", ignoreCase = true) }?.value?.toPlaintext() ?: permalink(title)
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
        (page.cover as? NotionExternal?)?.external?.url
    }

    @get:JsonIgnore
    val allImages: List<String> by lazy {
        blocks.filterIsInstance<ImageBlock>().mapNotNull { it.image?.file?.url }
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

