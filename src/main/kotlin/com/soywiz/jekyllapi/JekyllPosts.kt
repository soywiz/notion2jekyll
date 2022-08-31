package com.soywiz.jekyllapi

import com.soywiz.util.*
import java.io.File

class JekyllPosts(postsFolder: File = File("posts")) {
    val postsFolder: File = postsFolder.canonicalFile
    val draftsFolder: File = File(postsFolder, "../drafts").canonicalFile
    val imagesFolder: File = File(postsFolder, "../images").canonicalFile
    init {
        postsFolder.mkdirs()
        draftsFolder.mkdirs()
        imagesFolder.mkdirs()
    }
    fun readAll(): List<JekyllNotionPage> = buildList {
        for (folder in listOf(draftsFolder, postsFolder)) {
            for (file in folder.walkBottomUp()) {
                when (file.extension.lowercase()) {
                    "md", "markdown" -> {
                        val fileRelative = file.relativeTo(postsFolder)
                        val page = JekyllNotionPage(file.readText(), fileRelative)
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
    constructor(file: File) : this(FileWithFrontMatter(file.readText(), file))
    constructor(rawFileContent: String, file: File) : this(FileWithFrontMatter(rawFileContent, file))
    val headers = file.headers
    val title = headers["title"]?.toString()
    val permalink = headers["permalink"]?.toString()
    val feature_image = headers["feature_image"]?.toString()
    val notionPageId = headers["notion_page_id"]?.toString()
    val markdownBody: String get() = file.bodyRaw

    override fun toString(): String = "JekyllNotionPage('$title')"
}

data class FileWithFrontMatter(val headers: MutableMap<String, Any?>, val bodyRaw: String, val file: File) {
    companion object {
        operator fun invoke(rawFileContent: String, file: File): FileWithFrontMatter {
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
            return FileWithFrontMatter(headers, bodyRaw, file)
        }

        fun buildMarkdownFile(headers: Map<String, Any?>, bodyRaw: String): String = buildString {
            appendLine("---")
            appendLine(YAML.dump(headers).trim())
            appendLine("---")
            appendLine("")
            append(bodyRaw.trim())
        }
    }


    fun toMarkdownString(): String = buildMarkdownFile(headers, bodyRaw)
    override fun toString(): String = toMarkdownString()
}
