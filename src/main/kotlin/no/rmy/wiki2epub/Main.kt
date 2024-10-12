package no.rmy.wiki2epub

import com.sun.java.accessibility.util.Translator
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

class PageNumber(val content: String) : Tag {
    val text get() = content.trim().split("|").last().trimEnd('}')

    fun html3(): String =
        "<span title=\"[Pg $text]\" id=\"pgepubid00259\"><a id=\"Page_$text\" title=\"[Pg $text]\"></a></span>"

    fun html2(): String {
        return "<span epub:type=\"pagebreak\" id=\"page$text\">$text</span>"
    }

    fun html4(): String {
        return "<span>$text</span>"
    }

    override fun html(): String = html3()
}


class Paragraph(val content: String) : Tag {
    override fun html(): String = content.trim().lines().joinToString("\n").let {
        "<p>\n${it.trim().replace("</span> <br/>", "</span>")}\n</p>"
    }


    companion object {
        fun isPageNumber(s: String): Boolean = s.startsWith("{{page|")

        fun toTag(p: List<String>): Tag = // Paragraph(p.joinToString("\n"))
            if (p.size == 1 && isPageNumber(p.first())) {
                PageNumber(p.first())
            } else {
                p.mapIndexed { index, it ->
                    if (isPageNumber(it)) {
                        PageNumber(it.trim()).html()
                    } else {
                        if (index == 0) {
                            "<div class=\"first\">$it</div>"
                        } else {
                            "<div>$it</div>"
                        }
                    }
                }.let {
                    Paragraph(it.joinToString("\n"))
                }
            }

        fun create(content: String): List<Tag> {
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
                        "{{Blank linje"
                    ).forEach { searchFor ->
                        var tries = 5
                        while (--tries > 0 && revisedLine.contains(searchFor)) {
                            revisedLine.split(searchFor, limit = 2).last().split("}}").first().let { c ->
                                val oldValue = "$searchFor$c}}"
                                // println(oldValue)
                                when (searchFor) {
                                    "{{page|" -> {
                                        //revisedLine = revisedLine.replace(oldValue, PageNumber(oldValue).html())
                                    }

                                    "{{Sperret|" -> {
                                        revisedLine = revisedLine.replace(oldValue, "<em>$c</em>")
                                    }

                                    "{{Blank linje" -> {
                                        revisedLine = revisedLine.replace(oldValue, "<hr/>")
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
                        queue.add(toTag(p))
                        p.clear()
                    }
                    queue.add(tag)
                }
            }
            if (p.isNotEmpty()) {
                queue.add(toTag(p))
            }
            return queue
        }
    }
}


class Chapter(val content: String, style: Boolean) {
    val title: String get() = tags().mapNotNull { it as? Heading }.joinToString(" - ") { it.text }

    fun inputStream(): InputStream = html().byteInputStream()

    fun tags(): List<Tag> =
        content.split(Regex("\\{\\{gap\\|1em\\}\\}|\\{\\{Innrykk\\|1\\}\\}")).map {
            Paragraph.create(it)
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
  ${getStyle(style)}
</head>
<body>
${it}
</body>
</html>        
        """.trimIndent()
    }


    companion object {
        fun getStyle(s: Boolean) = if(s)
            style
        else
            ""

        val style = """
  <style>
  h1 {
    text-align: center;
    font-size: 1em;
  }
  
  h2 {
    text-align: center;
    font-size: 1em;
  }

  em {
    letter-spacing: 0.2em;
    font-style: normal;
  }

  p {
    padding: 2em;
    margin: 0;
  }

/*
  div.first:first-letter {
      font-size: 2.5em;
      vertical-align: text-top;
  }
 */
  
  .first {
    text-indent; 2em
  }
        

  
  div {
    margin-left: 3em;
    text-indent: -3em;
    padding: 0;
  }
  
  
  </style>
            
        """.trimIndent()


        suspend fun create(firstPage: Int, lastPage: Int, style: Boolean = false): Chapter {
            val httpClient = HttpClient(CIO)
            val jsonDecoder = Json {
                isLenient = true
            }

            val c = (firstPage until lastPage).mapNotNull { page ->
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
        Chapter.create(1, 6, false),
        Chapter.create(7, 9, false),
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

    val path = "files"
    File(path).mkdirs()

    chapters.forEachIndexed { index, ch ->
        println("-----")
        println(ch.html())

        val filename = "$path/chapter_$index.xhtml"
        File(filename)
    }

    val ebook = Book().apply {
        metadata.titles.add("Iliaden")
        metadata.apply {
            titles.add("Iliaden")
            contributors.add(Author("Oversetter", "P. Østbye"))
            contributors.add(Author("Homer"))
            contributors.add(Author("Digitalisering", "Øystein Tvede"))
            publishers.add("H. ASCHEHOUG & CO. (W. NYGAARD)")
        }


        chapters.forEachIndexed { index, ch ->
            val chapterResource = Resource(ch.inputStream(), "chapter_$index.xhtml")

            this.addSection(ch.title, chapterResource)
            spine.addResource(chapterResource)
        }
    }

    val ebookWriter = EpubWriter()
    ebookWriter.write(ebook, FileOutputStream("files/iliaden.epub"))
    // val ch = chapters.drop(0).first()

    //println(ch.content)
}

