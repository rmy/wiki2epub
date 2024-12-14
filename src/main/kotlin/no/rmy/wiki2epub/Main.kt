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
        Chapter.create(208, 220),
        Chapter.create(221, 243),
        Chapter.create(244, 258),
        Chapter.create(259, 278),
        Chapter.create(279, 302),
        Chapter.create(303, 323),
        Chapter.create(324, 340),
        Chapter.create(341, 352),
        Chapter.create(353, 366),
        Chapter.create(367, 383),
        Chapter.create(384, 397),
        Chapter.create(398, 421),
        Chapter.create(422, 443),
    )

    Mode.entries.forEach { currentMode ->
        Mode.current = currentMode
        when(currentMode) {
            Mode.EPUB2 -> Epub2Maker.create(chapters)
            Mode.EPUB3 -> Epub3Maker.create(chapters)
        }
    }
}


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
    fun epub2html(): String
    fun epub3html(): String
}

