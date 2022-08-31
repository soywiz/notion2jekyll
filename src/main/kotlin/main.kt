import com.soywiz.notionapi.*

suspend fun main() {
    val notion = NotionCachedAPI(NotionAPI("secret_nss1EfFxsW2x5raz9TZ48VACglEK86YfqN8QqgxrbB0"))

    for (page in notion.getDatabase("38ceb3348ad54f64a743b3e2a3c5fd2c").pages) {
        val page = notion.getFullPage(page)
        println("title: '${page.title}'")
        println("permalink: '${page.permalink}'")
        println("sponsor: '${page.sponsor}'")
        println("category: '${page.category}'")
        println("created: '${page.created}'")
        println("published: '${page.published}'")
        println("tags: '${page.tags}'")
        println("-")
        //println(page.contentMarkdown)
    }

    println("--------")
}
