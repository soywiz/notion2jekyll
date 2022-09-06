import com.fasterxml.jackson.module.kotlin.*
import com.soywiz.jekyllapi.*
import com.soywiz.notionapi.*
import com.soywiz.util.*
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import ru.gildor.coroutines.okhttp.*
import java.io.*
import java.text.SimpleDateFormat

enum class ScriptMode {
    SYNC, DISCORD
}

suspend fun main(args: Array<String>) {
    val margs = args.reversed().toMutableList()
    var rootPath = "."
    var mode = ScriptMode.SYNC
    var testMode = false
    while (margs.isNotEmpty()) {
        val arg = margs.removeLast()
        if (arg.startsWith("-")) {
            when (arg.lowercase()) {
                "-h", "--help" -> {
                    println("-h, --help -- Displays help")
                    println("-s, --sync -- Syncs notion -> jekyll posts")
                    println("-d, --discord -- Send discord messages from sponsored posts")
                    println("-t, --test -- Test mode (don't send discord links, and do not save files)")
                    return
                }

                "-s", "--sync" -> {
                    mode = ScriptMode.SYNC
                }

                "-d", "--discord" -> {
                    mode = ScriptMode.DISCORD
                }
                "t", "--test" -> {
                    testMode = true
                }
            }
        } else {
            rootPath = arg
        }
    }
    val jekyllRoot = File(rootPath)

    when (mode) {
        ScriptMode.SYNC -> {
            val notionSecret = System.getenv("NOTION_SECRET") ?: error("NOTION_SECRET environment variable not set")
            val databaseId = System.getenv("NOTION_DATABASE_ID") ?: error("NOTION_DATABASE_ID environment variable not set")
            if (!File(jekyllRoot, "posts").exists()) error("Folder '$jekyllRoot' doesn't contain a 'posts' folder. Potentially not a jekyll site")

            NotionCachedAPI(NotionAPI(notionSecret), File(jekyllRoot, ".notion_cache")).use { notion ->
                val posts = JekyllPosts(jekyllRoot)
                val existingPages = posts.readAll().associateBy { it.notionPageId }
                val newPageInfos = arrayListOf<PageInfo>()
                val newPages = LinkedHashMap<String, JekyllNotionPage>()

                for (page in notion.getDatabase(databaseId).pages) {
                    val npage: PageInfo = notion.getFullPage(page)
                    val page = JekyllNotionPage(npage.toFileWithFrontMatter(posts.postsFolder))
                    newPageInfos += npage
                    newPages[npage.page.id] = page
                }

                val removedIds = existingPages.keys - newPages.keys
                val addedIds = newPages.keys - existingPages.keys
                val keptIds = newPages.keys.intersect(existingPages.keys)

                println("removedIds=$removedIds")
                println("addedIds=$addedIds")
                println("keptIds=$keptIds")

                // Remove pages
                println("Deleting old pages [${removedIds.size}]...$removedIds")
                for (id in removedIds) {
                    val page = existingPages[id]!!
                    posts.delete(page)
                }

                // Add pages
                println("Adding new pages [${addedIds.size}]...$addedIds")
                for (id in addedIds) {
                    val page = newPages[id]!!
                    posts.write(page)
                }

                // Updated pages
                println("Keeping/Updating pages [${keptIds.size}]...$keptIds")
                for (id in keptIds) {
                    val oldPage = existingPages[id]!!
                    val newPage = newPages[id]!!
                    if (oldPage.file.file != newPage.file.file) {
                        posts.delete(oldPage)
                    }
                    posts.write(newPage)
                }

                // Ensure images are copied
                println("Copying images [${newPageInfos.size}]...")
                for (page in newPageInfos) {
                    for (image in page.allImagesAndCover) {
                        val baseFileName = File(image).name
                        val fromImageFile = File(notion.imagesFolder, baseFileName)
                        val intoImageFile = File(posts.imagesFolder, baseFileName)
                        if (!intoImageFile.exists()) {
                            println("fromImageFile=$fromImageFile, intoImageFile=$intoImageFile")
                            fromImageFile.copyTo(intoImageFile, overwrite = true)
                        }
                    }
                }

                // Ensure files are copied
                println("Copying files [${newPageInfos.size}]...")
                for (page in newPageInfos) {
                    for (image in page.allFiles) {
                        val baseFileName = File(image).name
                        val fromFile = File(notion.filesFolder, baseFileName)
                        val intoFile = File(posts.filesFolder, baseFileName)
                        if (!intoFile.exists()) {
                            println("fromFile=$fromFile, intoFile=$intoFile")
                            fromFile.copyTo(intoFile, overwrite = true)
                        }
                    }
                }
            }
        }

        ScriptMode.DISCORD -> {
            val client = OkHttpClient()
            try {
                val secrets = YAML.load<Map<String, Any?>>(File(jekyllRoot, "secrets.yml").readText())
                val SITE_PREFIX = secrets["SITE_PREFIX"] as? String? ?: error("Expecting SITE_PREFIX")
                val sponsorDiscordChannels = (secrets["SPONSOR_DISCORD_CHANNELS"] as? Map<Any?, String?>?)
                    ?.entries?.associate { it.key.toString().toInt() to it.value.toString() }
                    ?: emptyMap()

                fun getChannelForTier(price: Int): Map.Entry<Int, String> {
                    return sponsorDiscordChannels.filter { it.key <= price }.maxBy { it.key }
                }

                println("sponsorDiscordChannels=$sponsorDiscordChannels")
                val posts = JekyllPosts(jekyllRoot)
                val existingPages = posts.readAll().associateBy { it.notionPageId }

                for (page in existingPages.values.sortedBy { it.date }) {
                    val sponsor_tier = page.headers["sponsor_tier"]?.toString()?.toIntOrNull()
                    if (page.headers["discord_sent"] == true) continue
                    if (sponsor_tier != null) {
                        val (channelPrice, channelUrl) = getChannelForTier(sponsor_tier)
                        println("PAGE[${page.date}]: ${page.notionPageId} : $sponsor_tier : $channelPrice,$channelUrl")
                        //println(page.headers["date"])
                        val pageUrl = page.url(SITE_PREFIX)
                        val dateFormat = SimpleDateFormat("YYYY-MM-dd")
                        val messageContent = "`${dateFormat.format(page.date)}` : ${page.title} : $pageUrl"

                        if (testMode) {
                            println("  - [TEST] Message: $messageContent")
                        } else {
                            val request = Request.Builder()
                                .url(channelUrl)
                                .post(
                                    jacksonObjectMapper().writeValueAsString(
                                        mapOf(
                                            "content" to messageContent,
                                            "embeds" to emptyList<String>()
                                        )
                                    ).toRequestBody(MediaTypeApplicationJson)
                                )
                                .build()
                            client.newCall(request).await()

                            page.headers["discord_sent"] = true
                            page.save()
                        }
                    }
                }
            } finally {
                client.dispatcher.executorService.shutdown()
            }
        }
    }
}

/*
# https://discord.com/developers/docs/resources/webhook#execute-webhook
#{
#  "content": "Now I’m using notion for this blog + notion2jekyll tool for sponsors : https://soywiz.com/notion2jekyll",
#  "embeds": [
#    {
#      "title" : "Now I’m using notion for this blog",
#      "url" : "https://soywiz.com/notion2jekyll",
#      "description" : "Lately I wanted to start writing more blog posts, but writing them in markdown is not really comfortable nor fast to do:",
#      "image": { "url": "https://soywiz.com/images/bb0443e4-f506-4a6c-8917-6471c1ac8a0c-notion-1080.jpg" }
#    }
#  ]
#
*/