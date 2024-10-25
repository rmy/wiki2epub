package no.rmy.book

import no.rmy.mediawiki.MwTag

class BkParagraph(parent: BkPassage?): BkPassage(parent) {
    val content = StringBuffer()

    override fun append(tag: MwTag) {
        when(tag.name) {
            "text" -> {
                content.append(tag.name)
                content.append(": ")
                content.append(tag.content())
            }
            else -> {
                content.append(tag.name)
                content.append(": ")
                content.append(tag.content())
            }
        }
    }

    override fun renderHtml(): String =
        "<p>\n${content.toString()}\n</p>"
}
