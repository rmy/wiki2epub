package no.rmy.wiki2epub

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.whileSelect
import kotlinx.serialization.json.*
import java.io.File


class Page(val page: Int, val source: String?) {
    val content get() = cleanedContent().lines().filter {
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

    override fun html(): String {
        return "<span epub:type=\"pagebreak\" id=\"page$text\">$text</span>"
    }
}


class Paragraph(val content: String) : Tag {
    override fun html(): String = content.trim().lines().joinToString(" <br/>\n").let {
        "<p>\n${it.trim()}\n</p>"
    }


    companion object {
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
                    //} else if (line.startsWith("{{page|")) {
                    //    PageNumber(line)
                } else {
                    var revisedLine = line
                    // println("Line: $revisedLine")
                    listOf("{{innfelt initial ppoem|", "{{page|", "{{Sperret|").forEach { searchFor ->
                        var tries = 5
                        while (--tries > 0 && revisedLine.contains(searchFor)) {
                            revisedLine.split(searchFor, limit = 2).last().split("}}").first().let { c ->
                                val oldValue = "$searchFor$c}}"
                                // println(oldValue)
                                when (searchFor) {
                                    "{{page|" -> {
                                        revisedLine = revisedLine.replace(oldValue, PageNumber(oldValue).html())
                                    }
                                    "{{Sperret|" -> {
                                        revisedLine = revisedLine.replace(oldValue, "<em>$c</em>")
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
                        Paragraph(p.joinToString("\n")).let {
                            queue.add(it)
                        }
                        p.clear()
                    }
                    queue.add(tag)
                }
            }
            if (p.isNotEmpty()) {
                Paragraph(p.joinToString("\n")).let {
                    queue.add(it)
                }
            }
            return queue
        }
    }
}


class Chapter(val content: String) {
    fun tags(): List<Tag> =
        content.split(Regex("\\{\\{gap\\|1em\\}\\}|\\{\\{Innrykk\\|1\\}\\}")).map {
            Paragraph.create(it)
        }.flatten()

    fun html(): String = tags().map {
        it.html()
    }.joinToString("\n\n")


    companion object {
        suspend fun create(firstPage: Int, lastPage: Int): Chapter {
            val httpClient = HttpClient(CIO)
            val jsonDecoder = Json {
                isLenient = true
            }

            val c = (firstPage until lastPage).mapNotNull { page ->
                val pageUrl = "https://api.wikimedia.org/core/v1/wikisource/no/page/Side%3AIliaden.djvu%2F$page"

                val path = "files"
                File(path).mkdirs()
                val filename = "$path/iliaden_$page.wikimedia"

                if(File(filename).exists()) {
                    File(filename).readText().let {
                        Page(page, it)
                    }
                }
                else {
                    val result = httpClient.request {
                        url(pageUrl)
                    }
                    delay(500)

                    val string = result.bodyAsText()
                    val source = jsonDecoder.parseToJsonElement(string).jsonObject.get("source")?.jsonPrimitive?.contentOrNull
                    if(source != null) {
                        File(filename).writeText(source)
                        Page(page, source)
                    }
                    else {
                        null
                    }
                }
            }.joinToString("\n") {
                it.toString()
            }


            return Chapter(c)
        }
    }
}


fun main() = runBlocking {
    val chapters = listOf(
        Chapter.create(11, 27),
        Chapter.create(28, 51),
        Chapter.create(52, 64),
        Chapter.create(65, 79),
        Chapter.create(80, 104),
        Chapter.create(105, 119),
        Chapter.create(120, 132),
        Chapter.create(133, 148),
        Chapter.create(341, 352),
    )

    chapters.forEach { ch ->
        println("-----")
        println(ch.html())
    }
    // val ch = chapters.drop(0).first()

    //println(ch.content)
}

