package no.rmy.wiki2epub

import io.documentnode.epub4j.domain.Author
import io.documentnode.epub4j.domain.Book
import io.documentnode.epub4j.domain.Resource
import io.documentnode.epub4j.epub.EpubWriter
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


class Page(val page: Int, val source: String?) {
    val content
        get() = cleanedContent().lines().filter {
            it.trim().let {
                !it.startsWith("{{ppoem")
                        && !it.equals("}}")
            }
        }.joinToString("\n")

    constructor(page: Int, json: JsonElement) : this(
        page,
        json.jsonObject.get("source")?.jsonPrimitive?.contentOrNull
    )

    private val oldPageString: String = "<span epub:type=\"pagebreak\" id=\"page$page\">$page</span>"
    private val pageString: String get() = "{{page|$page}}"

    override fun toString(): String = "$pageString\n$content"


    private fun cleanedContent(): String {
        return source?.let { text ->
            text.split("</noinclude>").filter {
                !it.startsWith("<noinclude>")
            }.map {
                it.split("<noinclude>").first()
            }.joinToString("")
        } ?: ""
    }
}


interface Tag {
    fun html(): String
}

class Heading(val content: String, val level: Int = 1) : Tag {
    val text get() = content.trim().split("|").last().trimEnd('}')

    override fun html(): String = "<h$level>$text</h$level>"
}

class PageNumber(content: String) : Tag {
    val text = content.trim().split("|").last().trimEnd('}')

    fun html3(): String =
        "<span title=\"[Pg $text]\"><a id=\"Page_$text\" title=\"[Pg $text]\"></a></span>"

    fun html2(): String {
        return "<span epub:type=\"pagebreak\" id=\"page$text\">$text</span>x"
    }

    fun html4(): String {
        return "<span>$text</span>"
    }

    override fun html(): String = html3()
}


class Paragraph(val content: String, val isPoem: Boolean) : Tag {
    override fun html(): String = content.trim().lines().joinToString("\n").let {
        "<p>\n${
            it.trim()
                .replace("</span>x<br/>", "</span>")
                .replace("</span>x", "</span>")
        }\n</p>"
    }


    companion object {
        fun isPageNumber(s: String): Boolean = s.startsWith("{{page|")

        fun toTag(p: List<String>, isPoem: Boolean): Tag = // Paragraph(p.joinToString("\n"))
            if (p.size == 1 && isPageNumber(p.first())) {
                PageNumber(p.first())
            } else {
                p.filter { it.isNotBlank() }.mapIndexed { index, it ->
                    if (isPageNumber(it)) {
                        PageNumber(it.trim()).html() + "x"
                    } else {
                        if(isPoem) {
                            when(index) {
                                0 -> "<div class=\"one\">$it</div>"
                                else -> "<div class=\"follow\">$it</div>"
                            }
                            //"<div class=\"line\">$it</div>"
                            //"<span class=\"line\">$it</span>"
                            //it
                        } else {
                            it.split(Regex("\\s+")).chunked(10).map {
                                it.joinToString(" ")
                            }.joinToString("\n")
                        }
                    }
                }.let {
                    Paragraph(it.joinToString(
                        //"<br/>\n"
                        "\n"
                    ) { it.trim() }, isPoem)
                }
            }

        fun create(content: String, isPoem: Boolean = true): List<Tag> {
            val queue = mutableListOf<Tag>()

            val lines = content.trim().lines().toMutableList()
            val p = mutableListOf<String>()
            while (lines.isNotEmpty()) {
                val line = lines.first()
                lines.removeFirst()
                var tag: Tag? = if (line.startsWith("{{midtstilt|{{stor")) {
                    Heading(line, 1)
                } else if (line.startsWith("{{midtstilt|")) {
                    Heading(line, 2)
                } else {
                    var revisedLine = line
                    // println("Line: $revisedLine")
                    listOf(
                        "{{innfelt initial ppoem|",
                        "{{page|",
                        "{{Sperret|",
                        "{{nodent|{{innfelt initial|",
                        "{{Blank linje",
                        "{{høyre|''"

                    ).forEach { searchFor ->
                        var tries = 5
                        while (--tries > 0 && revisedLine.contains(searchFor)) {
                            revisedLine.split(searchFor, limit = 2).last().split("}}").first().let { c ->
                                val oldValue = "$searchFor$c}}"
                                // println(oldValue)
                                when (searchFor) {
                                    "{{page|" -> {
                                        if(!isPoem)
                                            revisedLine = revisedLine.replace(oldValue, PageNumber(oldValue).html())
                                    }

                                    "{{Sperret|" -> {
                                        revisedLine = revisedLine.replace(oldValue, "<em>$c</em>")
                                    }

                                    "{{Blank linje" -> {
                                        revisedLine = revisedLine.replace(oldValue, "<hr/>")
                                    }
                                    "{{innfelt initial ppoem|" -> {
                                        revisedLine = revisedLine.replace(oldValue, "<big>$c</big>")
                                    }
                                    "{{nodent|{{innfelt initial|" -> {
                                        revisedLine = revisedLine.replace(oldValue, "<big>$c</big>")
                                        revisedLine = revisedLine.replace("}}", "")
                                    }
                                    "{{høyre|''" -> {
                                        revisedLine = revisedLine.replace(oldValue, "$c")
                                        revisedLine = revisedLine.split("''").first().let { "<center>$it</center>" }
                                    }

                                    else -> {
                                        revisedLine = revisedLine.replace("$searchFor$c}}", c)
                                    }
                                }
                            }
                        }
                    }
                    p.add(revisedLine)
                    null
                }
                if (tag != null) {
                    if (p.isNotEmpty()) {
                        queue.add(toTag(p, isPoem))
                        p.clear()
                    }
                    queue.add(tag)
                }
            }
            if (p.isNotEmpty()) {
                queue.add(toTag(p, isPoem))
            }
            return queue
        }
    }
}


class Chapter(val content: String, val useStyle: Boolean) {
    val title: String get() =
        tags().mapNotNull { it as? Heading }.joinToString(" - ") { it.text }

    fun inputStream(): InputStream = html().byteInputStream()

    fun tags(): List<Tag> = if(useStyle)
        tagsPoem()
    else
        tagsNormal()

    fun tagsPoem(): List<Tag> =
        content.split(Regex("\\{\\{gap\\|1em\\}\\}|\\{\\{Innrykk\\|1\\}\\}")).map {
            Paragraph.create(it)
        }.flatten()

   fun tagsNormal(): List<Tag> =
       content.split("\n\n").map {
           it.replace("\n", " ")
       }.map {
           Paragraph.create(it, false)
       }.flatten()

    fun html(): String = tags().map {
        it.html()
    }.joinToString("\n\n").let {
        """
<?xml version="1.0" encoding="utf-8" standalone="yes"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="no" dir="ltr"
lang="no">
<head>
  <title>$title</title>
  ${getStyle(useStyle)}
</head>
<body>
${it}
</body>
</html>        
        """.trimIndent()
    }


    companion object {
        fun getStyle(s: Boolean): String = if(s)
            "<link rel=\"stylesheet\" href=\"styles.css\" />"
        else
            "<link rel=\"stylesheet\" href=\"innledning.css\" />"

        val style = """
  <style>
  </style>
            
        """.trimIndent()


        suspend fun create(firstPage: Int, lastPage: Int, style: Boolean = true): Chapter {
            val httpClient = HttpClient(CIO)
            val jsonDecoder = Json {
                isLenient = true
            }

            val c = (firstPage .. lastPage).mapNotNull { page ->
                val pageUrl = "https://api.wikimedia.org/core/v1/wikisource/no/page/Side%3AIliaden.djvu%2F$page"

                val path = "files"
                File(path).mkdirs()
                val filename = "$path/iliaden_$page.wikimedia"

                if (File(filename).exists()) {
                    File(filename).readText().let {
                        Page(page, it)
                    }
                } else {
                    val result = httpClient.request {
                        url(pageUrl)
                    }
                    delay(500)

                    val string = result.bodyAsText()
                    val source =
                        jsonDecoder.parseToJsonElement(string).jsonObject.get("source")?.jsonPrimitive?.contentOrNull
                    if (source != null) {
                        File(filename).writeText(source)
                        Page(page, source)
                    } else {
                        null
                    }
                }
            }.joinToString("\n") {
                it.toString()
            }


            return Chapter(c, style)
        }

    }
}


fun main() = runBlocking {
    val chapters = listOf(
        // Chapter.create(1, 6, false),
        Chapter.create(7, 10, false),
        Chapter.create(11, 27),
        Chapter.create(28, 51),
        Chapter.create(52, 64),
        Chapter.create(65, 79),
        Chapter.create(80, 104),
        Chapter.create(105, 119),
        Chapter.create(120, 132),
        Chapter.create(133, 148),
        Chapter.create(149, 168),
        Chapter.create(169, 184),
        Chapter.create(185, 207),
        Chapter.create(185, 207),
        Chapter.create(208, 220),
        Chapter.create(221, 243),
        Chapter.create(244, 258),
        Chapter.create(259, 278),
        Chapter.create(303, 323),
        Chapter.create(324, 340),
        Chapter.create(341, 352),
        Chapter.create(353, 366),
        Chapter.create(367, 383),
        Chapter.create(384, 397),
        Chapter.create(398, 421),
        Chapter.create(422, 443),
    )

    val path = "files/html"
    File(path).mkdirs()

    chapters.forEachIndexed { index, ch ->
        println("-----")
        println(ch.html())

        val filename = "$path/chapter_$index.xhtml"
        File(filename).writeText(ch.html())
    }

    val ebook = Book().apply {
        metadata.apply {
            titles.add("Iliaden")
            contributors.add(Author("Peder", "Østbye"))
            contributors.add(Author("Homer"))
            contributors.add(Author("Øystein", "Tvede"))
            publishers.add("H. ASCHEHOUG & CO. (W. NYGAARD)")
        }

        Resource(File("iliaden_cover.jpg").inputStream(), "iliaden_cover.jpg").let {
            setCoverImage(it)
        }
        Resource(File("styles.css").inputStream(), "styles.css").let {
            addResource(it)
        }
        Resource(File("innledning.css").inputStream(), "innledning.css").let {
            addResource(it)
        }

        Resource(File("tittelside.xhtml").inputStream(), "tittelside.xhtml").let {
            addResource(it)
            spine.addResource(it)
        }
        chapters.forEachIndexed { index, ch ->
            val chIndex = index + 1
            val chapterResource = Resource(ch.inputStream(), "chapter_$chIndex.xhtml")

            this.addSection(ch.title, chapterResource)
            spine.addResource(chapterResource)
        }

    }



    val ebookWriter = EpubWriter()
    ebookWriter.write(ebook, FileOutputStream("files/iliaden.epub"))
    // val ch = chapters.drop(0).first()

    //println(ch.content)
}

