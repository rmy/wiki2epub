package no.rmy.mediawiki

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

object MediaWiki {
    val httpClient = HttpClient(CIO)

    val jsonParser = Json {
        isLenient = true
    }


    fun parse(text: String): MwTag {
        return MwParent(null).apply {
            parse(text)
        }
    }



    suspend fun iliaden(page: Int): MwTag? {
        val path = "files"
        File(path).mkdirs()
        val filename = "$path/iliaden_$page.wikimedia"

        val text = if (File(filename).exists()) {
            File(filename).readText().let {
                it
            }
        } else {
            val pageUrl = "https://api.wikimedia.org/core/v1/wikisource/no/page/Side%3AIliaden.djvu%2F$page"
            val result = httpClient.request {
                url(pageUrl)
            }
            delay(500)

            val string = result.bodyAsText()
            val source =
                jsonParser.parseToJsonElement(string).jsonObject.get("source")?.jsonPrimitive?.contentOrNull
            if (source != null) {
                File(filename).writeText(source)
                source
            } else {
                null
            }
        }

        return text?.let { parse(it) }
    }
}