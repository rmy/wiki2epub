package no.rmy.wiki2epub

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*


class Page(val page: Int, val source: String?) {
    val content = cleanedContent().lines().filter {
        it.trim().let {
            !it.startsWith("{{ppoem")
                    && !it.equals("}}")
        }
    }.joinToString("\n")

    constructor(page: Int, json: JsonElement) : this(
        page,
        json.jsonObject.get("source")?.jsonPrimitive?.contentOrNull
    )

    override fun toString(): String = "{{page|$page}}\n$content"

    fun cleanedContent(): String {
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

class Heading(val content: String, val level: Int = 1): Tag {
    val text get() = content.trim().split("|").last().trimEnd('}')

    override fun html(): String = "<h$level>$text</h$level>"
}

class PageNumber(val content: String): Tag {
    val text get() = content.trim().split("|").last().trimEnd('}')

    override fun html(): String {
        return "<span epub:type=\"pagebreak\" id=\"page$text\">$text</span>\n"
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
            while(lines.isNotEmpty()) {
                val line = lines.first()
                lines.removeFirst()
                var tag: Tag? = if (line.startsWith("{{midtstilt|{{stor")) {
                    Heading(line, 1)
                } else if (line.startsWith("{{midtstilt|")) {
                    Heading(line, 2)
                } else if (line.startsWith("{{page|")) {
                    PageNumber(line)
                } else {
                    var revisedLine = line
                    listOf("{{innfelt initial ppoem|").mapNotNull { searchFor ->
                        if(revisedLine.contains(searchFor)) {
                            line.split(searchFor).last().split("}}").first().let { c ->
                                revisedLine = revisedLine.replace("$searchFor$c}}", c)
                            }
                        }
                    }
                    p.add(revisedLine)
                    null
                }
                if (tag != null) {
                    if(p.isNotEmpty()) {
                        Paragraph(p.joinToString("\n")).let {
                            queue.add(it)
                        }
                        p.clear()
                    }
                    queue.add(tag)
                }
            }
            if(p.isNotEmpty()) {
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
        content.split("{{gap|1em}}").map {
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

            val c = (firstPage until lastPage).map { page ->
                val pageUrl = "https://api.wikimedia.org/core/v1/wikisource/no/page/Side%3AIliaden.djvu%2F$page"
                val result = httpClient.request {
                    url(pageUrl)
                }

                val string = result.bodyAsText()
                val json = jsonDecoder.parseToJsonElement(string)
                Page(page, json)
            }.joinToString("\n") {
                it.toString()
            }

            return Chapter(c)
        }
    }
}


fun main() = runBlocking {
    val chapter = Chapter.create(341, 352)
    println(chapter.html())
}

