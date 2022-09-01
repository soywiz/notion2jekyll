import com.soywiz.jekyllapi.*
import com.soywiz.notionapi.*
import java.io.*

suspend fun main(args: Array<String>) {
    val jekyllRoot = File(args.lastOrNull() ?: ".")
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
            val page = JekyllNotionPage(npage.toFileWithFrontMatter())
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
    }

}
