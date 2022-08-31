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

    println("existingPages=$existingPages")
    println("newPages=$newPages")

    /*
                val oldPage = existingPages[npage.page.id]
            //println(page.props)
            if (oldPage != null && oldPage.file.file != page.file.file) {
                println("Deleted old file. Moved from '${oldPage.file.file}' to '${page.file.file}'")
                posts.delete(oldPage)
            }
            posts.write(page)

     */

    println("--------")
}
