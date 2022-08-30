import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.kotlin.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import ru.gildor.coroutines.okhttp.*
import kotlin.reflect.jvm.*

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

val MediaTypeApplicationJson = "application/json".toMediaType()
val objectMapper = jacksonObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)

class NotionAPI(private val bearer: String) {
    private val client = OkHttpClient()
    val notionVersion = "2022-06-28"

    suspend fun pageGet(pageId: String): Page? {
        val body = request("pages/$pageId").body ?: return null
        return objectMapper.readValue(body.string())
    }

    suspend fun pageGetProperty(pageId: String, propertyId: String): Flow<PropertyItem> = paginate<PropertyItem> { cursor ->
        request("pages/$pageId/properties/$propertyId?page_size=1000" + (if (cursor != null) "&start_cursor=$cursor" else ""))
    }

    suspend fun databaseGet(id: String): Database? {
        val body = request("databases/$id").body ?: return null
        return objectMapper.readValue(body.string())
    }

    suspend fun databaseQuery(id: String, query: Map<String, Any?> = mapOf()): Flow<NObject> = paginate<NObject> { cursor ->
        val rquery = query + LinkedHashMap<String, Any?>().apply {
            this["page_size"] = 1000
            if (cursor != null) this["start_cursor"] = cursor
        }
        //println("QUERY: $rquery")
        request("databases/$id/query", body = objectMapper.writeValueAsString(rquery).toRequestBody(MediaTypeApplicationJson))
    }

    suspend fun blocksChildren(blockId: String): Flow<Block> = paginate<Block> { cursor ->
        request("blocks/$blockId/children?page_size=1000" + (if (cursor != null) "&start_cursor=$cursor" else ""))
    }

    inline suspend fun <reified T> paginate(noinline request: suspend (cursor: String?) -> Response): Flow<T> = flow {
        var cursor: String? = null
        while (true) {
            val res = request(cursor).body ?: return@flow
            val str = res.string()
            val obj = objectMapper.readValue<SimpleObject>(str)
            when (obj.`object`) {
                "error" -> {
                    throw NError(objectMapper.readValue<NErrorObject>(str))
                }
                "list" -> {
                    val result = objectMapper.readValue<NListObject<T>>(str)
                    res.close()
                    for (result in result.results) {
                        emit(result)
                    }
                    cursor = result.next_cursor
                    if (result.has_more) continue
                }
                else -> {
                    emit(objectMapper.readValue<T>(str))
                }
            }

            break
        }
    }

    data  class SimpleObject(val `object`: String? = null)

    data class NListObject<T>(
        override var `object`: String,
        val results: List<T>,
        val next_cursor: String?,
        val has_more: Boolean,
        val type: String,
    ) : NObject()

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "object",
        visible = true,
        defaultImpl = NObject::class,
    )
    @JsonSubTypes(
        JsonSubTypes.Type(value = NListObject::class, name = "list"),
        JsonSubTypes.Type(value = NErrorObject::class, name = "error"),
        JsonSubTypes.Type(value = Page::class, name = "page"),
        JsonSubTypes.Type(value = Database::class, name = "database"),
        JsonSubTypes.Type(value = Block::class, name = "block"),
        JsonSubTypes.Type(value = PartialUser::class, name = "user"),
        JsonSubTypes.Type(value = ExternalImage::class, name = "external"),
    )
    open class NObject {
        open var `object`: String = ""

        @JsonAnySetter
        var extra: LinkedHashMap<String, Any?> = LinkedHashMap()

        override fun toString(): String = "${this::class.jvmName}(${`object`}, $extra)"
    }

    data class NErrorObject(val status: Int, val code: String, val message: String) : NObject()

    class NError(val error: NErrorObject) : Throwable("$error")

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
        visible = true,
        defaultImpl = PropInfo::class,
    )
    @JsonSubTypes(
        JsonSubTypes.Type(value = RichTextPropInfo::class, name = "rich_text"),
        JsonSubTypes.Type(value = SelectPropInfo::class, name = "select"),
        JsonSubTypes.Type(value = DatePropInfo::class, name = "date"),
        JsonSubTypes.Type(value = LastEditedTimeDatePropInfo::class, name = "last_edited_time"),
        JsonSubTypes.Type(value = CreatedDatePropInfo::class, name = "created_time"),
        JsonSubTypes.Type(value = TitlePropInfo::class, name = "title"),
        JsonSubTypes.Type(value = MultiSelectPropInfo::class, name = "multi_select"),
    )
    open class PropInfo() {
        open var id: String = ""
        var name: String = ""
        var type: String = ""

        @JsonAnySetter
        var extra: Map<String, Any?> = LinkedHashMap()

        override fun toString(): String = "PropInfo(name=$name, type=$type, id=$id)"
    }

    abstract class BaseSelectPropInfo() : PropInfo() {
        data class Option(val id: String?, val name: String?, val color: String?)
        data class Select(val options: List<Option>)
    }

    data class RichTextPropInfo(override var id: String, val rich_text: Any?) : PropInfo()
    abstract class BaseDatePropInfo() : PropInfo()
    data class DatePropInfo(override var id: String, val date: Any?) : BaseDatePropInfo()
    data class CreatedDatePropInfo(override var id: String, val created_time: Any?) : BaseDatePropInfo()
    data class LastEditedTimeDatePropInfo(override var id: String, val last_edited_time: Any?) : BaseDatePropInfo()
    data class TitlePropInfo(override var id: String, val title: Any?) : PropInfo()
    data class MultiSelectPropInfo(override var id: String, val multi_select: Select?) : BaseSelectPropInfo()
    data class SelectPropInfo(override var id: String, val select: Select?) : BaseSelectPropInfo()

    data class Page(
        val id: String,
        val created_time: String,
        val last_edited_time: String,
        val created_by: PartialUser,
        val last_edited_by: PartialUser,
        val cover: Image?,
        val archived: Boolean,
        val url: String,
        val properties: Map<String, PropInfo>,
    ) : NObject() {
    }

    data class Database(
        val id: String,
        val cover: Image,
        val icon: Image?,
        val created_time: String,
        val created_by: PartialUser,
        val last_edited_time: String?,
        val last_edited_by: PartialUser?,
        val title: List<RichTextEntry>,
        val description: List<RichTextEntry>,
        val is_inline: Boolean,
        val properties: Map<String, PropInfo>,
        val parent: Any?, // Reference
        val url: String,
        val archived: Boolean,
    ) : NObject()

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
        defaultImpl = PropertyItem::class,
        visible = true
    )
    @JsonSubTypes(
        JsonSubTypes.Type(value = RichTextPropertyItem::class, name = "rich_text"),
        JsonSubTypes.Type(value = TitlePropertyItem::class, name = "title"),
        JsonSubTypes.Type(value = MultiSelectPropertyItem::class, name = "multi_select"),
        JsonSubTypes.Type(value = LastEditedTimePropertyItem::class, name = "last_edited_time"),
        JsonSubTypes.Type(value = CreatedTimePropertyItem::class, name = "created_time"),
        JsonSubTypes.Type(value = DatePropertyItem::class, name = "date"),
        JsonSubTypes.Type(value = SelectPropertyItem::class, name = "select"),

    )
    open class PropertyItem(
    ) : NObject() {
        var id: String = ""
        var type: String = ""

        open fun toMarkdown(): String = "PropertyItem(id=$id, type=$type, extra=$extra)"
        open fun toPlaintext(): String = toMarkdown()
    }

    open class BaseSelectPropertyItem(
    ) : PropertyItem() {
        class Select(val id: String, val name: String, val color: String)
    }

    data class SelectPropertyItem(
        val select: Select
    ) : BaseSelectPropertyItem() {
        override fun toMarkdown(): String = select.name
    }

    data class MultiSelectPropertyItem(
        val multi_select: List<Select>
    ) : BaseSelectPropertyItem() {
        override fun toMarkdown(): String = multi_select.joinToString(", ") { it.name }
    }

    data class RichTextPropertyItem(
        val rich_text: RichTextEntry
    ) : PropertyItem() {
        override fun toMarkdown(): String = rich_text.toMarkdown()
        override fun toPlaintext(): String = rich_text.toPlaintext()
    }

    data class TitlePropertyItem(
        val title: RichTextEntry
    ) : PropertyItem() {
        override fun toMarkdown(): String = title.toMarkdown()
        override fun toPlaintext(): String = title.toPlaintext()
    }

    data class LastEditedTimePropertyItem(
        val last_edited_time: String
    ) : PropertyItem() {
        override fun toMarkdown(): String = last_edited_time
    }

    data class CreatedTimePropertyItem(
        val created_time: String
    ) : PropertyItem() {
        override fun toMarkdown(): String = created_time
    }

    data class DatePropertyItem(
        val date: DateInfo,
    ) : PropertyItem() {
        data class DateInfo(
            val start: String,
            val end: String? = null,
            val time_zone: String? = null
        )
        override fun toMarkdown(): String = when {
            date.end != null -> "${date.start}-${date.end}"
            else -> date.start
        }
    }


    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
        defaultImpl = Block::class,
        visible = true
    )
    @JsonSubTypes(
        JsonSubTypes.Type(value = ParagraphBlock::class, name = "paragraph"),
        JsonSubTypes.Type(value = DividerBlock::class, name = "divider"),
        JsonSubTypes.Type(value = ImageBlock::class, name = "image"),
        JsonSubTypes.Type(value = VideoBlock::class, name = "video"),
        JsonSubTypes.Type(value = Heading1Block::class, name = "heading_1"),
        JsonSubTypes.Type(value = Heading2Block::class, name = "heading_2"),
        JsonSubTypes.Type(value = Heading3Block::class, name = "heading_3"),
        JsonSubTypes.Type(value = Heading4Block::class, name = "heading_4"),
        JsonSubTypes.Type(value = Heading5Block::class, name = "heading_5"),
        JsonSubTypes.Type(value = Heading6Block::class, name = "heading_6"),
        JsonSubTypes.Type(value = TableOfContentsBlock::class, name = "table_of_contents"),
        JsonSubTypes.Type(value = CodeBlock::class, name = "code"),
    )
    open class Block(
    ) : NObject() {
        var id: String = ""
        var created_time: String = ""
        var last_edited_time: String = ""
        var created_by: PartialUser = PartialUser("")
        var last_edited_by: PartialUser = PartialUser("")
        var has_children: Boolean = false
        var archived: Boolean = false
        var type: String = ""
        override fun toString(): String = "Block(type=$type, id=$id)"
        open fun toMarkdown(): String = "Block($type)"
    }

    data class TableOfContentsBlock(val table_of_contents: TableOfContents) : Block() {
        data class TableOfContents(val color: String)

        override fun toMarkdown(): String = "{:toc}"
    }

    data class ParagraphBlock(val paragraph: Paragraph) : Block() {
        data class Paragraph(
            val rich_text: List<RichTextEntry>,
            val color: String,
        ) {
            fun toMarkdown() = rich_text.toMarkdown()
        }

        override fun toMarkdown(): String = paragraph.rich_text.toMarkdown()
    }

    abstract class HeadingBlock(val count: Int) : Block() {
        abstract val heading: Heading

        data class Heading(
            val rich_text: List<RichTextEntry>,
            val is_toggleable: Boolean,
            val color: String,
        ) {
            fun toMarkdown() = RichTextEntry.toMarkdown(rich_text)
        }

        override fun toMarkdown(): String = "#".repeat(count) + " " + heading.toMarkdown()
    }

    data class DividerBlock(val divider: Divider) : Block() {
        class Divider

        override fun toMarkdown(): String = "---"
    }

    data class VideoBlock(val video: Video) : Block() {
        data class Video(
            val caption: List<RichTextEntry>,
            val type: String,
            val external: External?
        )

        data class External(val url: String)

        override fun toMarkdown(): String = "<iframe width=\"420\" height=\"315\" src=\"${video.external?.url}\"></iframe>"
    }

    data class ImageBlock(val image: Image) : Block() {
        data class Image(
            val caption: List<RichTextEntry>,
            val type: String,
            val file: File?
        )

        data class File(var url: String, val expiry_time: String)

        override fun toMarkdown(): String = "[${image.caption.toMarkdown()}](${image.file?.url})"
    }

    data class CodeBlock(
        val code: Code
    ) : Block() {
        data class Code(
            val caption: List<Any?>,
            val rich_text: List<RichTextEntry>,
            val language: String,
        )

        override fun toMarkdown(): String = buildString {
            append("```${code.language}\n")
            append(code.rich_text.toMarkdown())
            append("```")
        }
    }

    data class Heading1Block(val heading_1: Heading) : HeadingBlock(1) {
        override val heading get() = heading_1
    }

    data class Heading2Block(val heading_2: Heading) : HeadingBlock(2) {
        override val heading get() = heading_2
    }

    data class Heading3Block(val heading_3: Heading) : HeadingBlock(3) {
        override val heading get() = heading_3
    }

    data class Heading4Block(val heading_4: Heading) : HeadingBlock(4) {
        override val heading get() = heading_4
    }

    data class Heading5Block(val heading_5: Heading) : HeadingBlock(5) {
        override val heading get() = heading_5
    }

    data class Heading6Block(val heading_6: Heading) : HeadingBlock(6) {
        override val heading get() = heading_6
    }

    data class RichTextEntry(
        val plain_text: String,
        val href: String?,
        val annotations: Annotations,
        val type: String, // text, mention, equation,
        val text: Text?,
    ) {
        data class Text(val content: String, val link: String?)
        data class Annotations(
            val bold: Boolean,
            val italic: Boolean,
            val strikethrough: Boolean,
            val underline: Boolean,
            val code: Boolean,
            val color: String,
        )

        companion object {
            fun toMarkdown(items: List<RichTextEntry>) = items.joinToString("") { it.toMarkdown() }
        }

        fun toPlaintext(): String = plain_text
        fun toMarkdown(): String = buildString {
            if (annotations.strikethrough) append("~~")
            if (annotations.bold) append("**")
            if (annotations.italic) append("_")
            if (annotations.underline) append("<ins>")
            if (annotations.color != "default") append("<span style='color:${annotations.color}' markdown=1>")
            if (annotations.code) append("<code>")
            append(plain_text)
            if (annotations.code) append("</code>")
            if (annotations.color != "default") append("</span>")
            if (annotations.underline) append("</ins>")
            if (annotations.italic) append("_")
            if (annotations.bold) append("**")
            if (annotations.strikethrough) append("~~")
        }
    }

    data class PartialUser(
        val id: String,
    ) : NObject() {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes(
        JsonSubTypes.Type(value = ExternalImage::class, name = "external"),
    )
    open class Image {
        var type: String = ""

        @JsonAnySetter
        var extra: Map<String, Any?> = LinkedHashMap()
    }

    data class ExternalImage(
        val external: External,
    ) : Image() {
        data class External(val url: String)
    }

    suspend fun request(path: String, body: RequestBody? = null): Response {
        val request = Request.Builder()
            .url("https://api.notion.com/v1/$path")
            .header("Authorization", "Bearer $bearer")
            .header("Notion-Version", notionVersion)
            .also { if (body != null) it.post(body) }
            .build()
        return client.newCall(request).await()
    }

    companion object {
        fun List<RichTextEntry>.toMarkdown(): String = map { it.toMarkdown() }.joinToString("")
    }
}
