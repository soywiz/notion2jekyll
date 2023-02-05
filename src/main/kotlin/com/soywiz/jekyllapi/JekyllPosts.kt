package com.soywiz.jekyllapi

import com.soywiz.util.*
import java.io.File
import java.util.*

class JekyllPosts(jekyllRoot: File = File(".")) {
    val postsFolder: File = jekyllRoot["posts", "_posts"].canonicalFile
    val draftsFolder: File = jekyllRoot["drafts", "_drafts"].canonicalFile
    val imagesFolder: File = jekyllRoot["static/images"].canonicalFile
    val filesFolder: File = jekyllRoot["static/files"].canonicalFile
    init {
        postsFolder.mkdirs()
        draftsFolder.mkdirs()
        imagesFolder.mkdirs()
        filesFolder.mkdirs()
    }
    fun readAll(): List<JekyllNotionPage> = buildList {
        for (folder in listOf(draftsFolder, postsFolder)) {
            for (file in folder.walkBottomUp()) {
                when (file.extension.lowercase()) {
                    "md", "markdown" -> {
                        val fileRelative = file.relativeTo(postsFolder)
                        val page = JekyllNotionPage(file.readText(), fileRelative, file.canonicalFile)
                        //println("file: ${matter.headerRaw}")
                        if (page.notionPageId != null) {
                            add(page)
                            //add("notionPageId=${page.notionPageId}, title=${page.title}, file=${fileRelative}, headers=${page.headers}")
                        }
                    }
                }
            }
        }
    }

    fun delete(page: JekyllNotionPage?) {
        if (page == null) return
        val file = File(postsFolder, page.file.file.path)
        file.delete()
    }

    fun write(page: JekyllNotionPage) {
        val file = File(postsFolder, page.file.file.path)
        file.writeText(page.file.toMarkdownString())
    }
}

class JekyllNotionPage(val file: FileWithFrontMatter) {
    constructor(file: File, realFile: File) : this(FileWithFrontMatter(file.readText(), file, realFile))
    constructor(rawFileContent: String, file: File, realFile: File) : this(FileWithFrontMatter(rawFileContent, file, realFile))
    val headers = file.headers
    val title = headers["title"]?.toString()
    val author = headers["author"]?.toString()
    val permalink = headers["permalink"]?.toString()
    val feature_image = headers["feature_image"]?.toString()
    val notionPageId = headers["notion_page_id"]?.toString()
    val date: Date? = kotlin.runCatching {
        val date = headers["date"]
        (date as? Date?) ?: date?.toString()?.let { DateParse(it) }
    }.getOrNull()
    val markdownBody: String get() = file.bodyRaw

    fun save() = file.save()
    fun url(sitePrefix: String): String = sitePrefix.trimEnd('/') + "/" + permalink?.trim('/')

    override fun toString(): String = "JekyllNotionPage('$title')"
}

data class FileWithFrontMatter(val headers: MutableMap<String, Any?>, val bodyRaw: String, val file: File, val realFile: File) {
    companion object {
        operator fun invoke(rawFileContent: String, file: File, realFile: File): FileWithFrontMatter {
            val parts = run {
                val parts = "$rawFileContent\n".split("---\n", limit = 3)
                when {
                    parts.size >= 3 -> listOf(parts[1], parts[2])
                    else -> listOf(null, parts[0])
                }
            }
            val headerRaw = parts[0] ?: ""
            val bodyRaw =  parts[1] ?: ""
            val headers: MutableMap<String, Any?> =  kotlin.runCatching { YAML.load<Map<String, Any?>>(headerRaw) }.getOrElse { emptyMap() }.toMutableMap()
            return FileWithFrontMatter(headers, bodyRaw, file, realFile)
        }

        fun buildMarkdownFile(headers: Map<String, Any?>, bodyRaw: String): String = buildString {
            appendLine("---")
            appendLine(YAML.dump(headers).trim())
            appendLine("---")
            appendLine("")
            append(bodyRaw.trim())
        }
    }

    fun save() {
        realFile.writeText(toMarkdownString())
    }

    fun toMarkdownString(): String = buildMarkdownFile(headers, bodyRaw)
    override fun toString(): String = toMarkdownString()
}
