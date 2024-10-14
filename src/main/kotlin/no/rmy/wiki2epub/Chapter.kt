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

class Chapter(val content: String, val useStyle: Boolean) {
    val title: String
        get() =
            tags().mapNotNull { it as? Heading }.joinToString(" - ") { it.text }

    fun inputStream(): InputStream = html().byteInputStream()

    fun tags(): List<Tag> = if (useStyle)
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

    fun epub3(body: String): String = """
<?xml version="1.0" encoding="utf-8"?>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="nb-NO">
<head>
    <meta charset="UTF-8" />
	<title>$title</title>
    ${getStyle(useStyle)}
</head>
<body xmlns:epub="http://www.idpf.org/2007/ops" epub:type="bodymatter">
<section epub:type="chapter" class="chapter">
$body
</section>
</body>
</html>
""".trim()

    fun epub2(body: String): String = """
<?xml version="1.0" encoding="utf-8" standalone="yes"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="nb-NO" dir="ltr"
lang="nb-NO">
<head>
  <title>$title</title>
  ${getStyle(useStyle)}
</head>
<body>
${body}
</body>
</html>
""".trim()

    fun html(): String = when (Mode.current) {
        Mode.EPUB2 -> epub2html()
        Mode.EPUB3 -> epub3html()
    }


    fun epub2html(): String = tags().map {
        it.epub2html()
    }.joinToString("\n\n").let {
        epub2(it)
    }


    fun epub3html(): String = tags().map {
        it.epub3html()
    }.joinToString("\n\n").let {
        epub3(it)
    }


    companion object {
        fun getStyle(s: Boolean): String = if (s)
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

            val c = (firstPage..lastPage).mapNotNull { page ->
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

