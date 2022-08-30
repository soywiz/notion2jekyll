import com.soywiz.notionapi.*

suspend fun main() {
    val notion = NotionAPI("secret_nss1EfFxsW2x5raz9TZ48VACglEK86YfqN8QqgxrbB0")
    notion.databaseQuery("38ceb3348ad54f64a743b3e2a3c5fd2c").collect {
        println("it=$it")
    }

    //for ((propName, prop) in notion.pageGet("283c5fc6-cf19-4cb9-b4de-27792181230d")!!.properties) {
    //    val content = notion.pageGetProperty("283c5fc6-cf19-4cb9-b4de-27792181230d", prop.id).toList().joinToString("") { it.toPlaintext() }
    //    println("$propName=$content")
    //}
    //return
    //return
    //val database = notion.databaseGet("38ceb3348ad54f64a743b3e2a3c5fd2c")
    //for (prop in database!!.properties) {
    //    println(prop)
    //}
    /*
    notion.blocksChildren("60bac1b7f3864e919cc74a00b1a0b7c8").collect {
        val markdown = it.toMarkdown().trim()
        if (markdown.isNotEmpty()) {
            //println("it=$it")
            println(markdown)
            println()
        }
    }
    notion.databaseQuery("38ceb3348ad54f64a743b3e2a3c5fd2c").collect {
        println("it=$it")
    }
     */
    println("--------")
    //println(notion.databaseQuery("38ceb3348ad54f64a743b3e2a3c5fd2c").toList())
}
