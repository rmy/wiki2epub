package no.rmy.wiki2epub

class Heading(val content: String, val level: Int = 1) : Tag {
    val text get() = content.trim().split("|").last().trimEnd('}')

    override fun html(): String = "<h$level>$text</h$level>"
    override fun epub2html(): String = html()
    override fun epub3html(): String = html()
}

