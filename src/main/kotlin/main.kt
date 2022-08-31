import com.soywiz.jekyllapi.*
import com.soywiz.notionapi.*
import java.io.*

suspend fun main() {
    val posts = JekyllPosts(File("../soywiz.com/posts"))
    val existingPages = posts.readAll().associateBy { it.notionPageId }
    val newPages = LinkedHashMap<String, JekyllNotionPage>()

    NotionCachedAPI(NotionAPI("secret_nss1EfFxsW2x5raz9TZ48VACglEK86YfqN8QqgxrbB0")).use { notion ->
        for (page in notion.getDatabase("38ceb3348ad54f64a743b3e2a3c5fd2c").pages) {
            val npage = notion.getFullPage(page)
            val page = JekyllNotionPage(npage.toFileWithFrontMatter())
            newPages[npage.page.id] = page
        }
    }

    val removedIds = existingPages.keys - newPages.keys
    val addedIds = newPages.keys - existingPages.keys
    val keptIds = newPages.keys.intersect(existingPages.keys)

    println("removedIds=$removedIds")
    println("addedIds=$addedIds")
    println("keptIds=$keptIds")

    // Remove pages
    for (id in removedIds) {
        val page = existingPages[id]!!
        posts.delete(page)
    }

    // Add pages
    for (id in addedIds) {
        val page = newPages[id]!!
        posts.write(page)
    }

    // Updated pages
    for (id in keptIds) {
        val oldPage = existingPages[id]!!
        val newPage = newPages[id]!!
        if (oldPage.file.file != newPage.file.file) {
            posts.delete(oldPage)
        }
        posts.write(newPage)
    }
}
