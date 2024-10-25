package no.rmy.book

import no.rmy.mediawiki.MwParent
import no.rmy.mediawiki.MwTag

class BkParagraph(parent: BkPassage?): BkPassage(parent) {
    val content = StringBuffer()

    override fun append(tag: MwTag) {
        when(tag.name) {
            "text" -> {
                content.append(tag.content())
            }

            else -> {
                (tag as? MwParent)?.children?.also {
                    it.forEach { tag ->
                        append(tag)
                    }
                } ?: {
                    content.append("\n\n" + tag.name)
                    content.append(": ")
                    content.append(tag.content())
                }
            }
        }
    }

    override fun renderHtml(): String =
        "<p>\n${content.toString().trim().lines().filter { it.isNotBlank() }.joinToString("\n")}\n</p>"
}
