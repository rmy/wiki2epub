package no.rmy.book

import no.rmy.mediawiki.MwParent
import no.rmy.mediawiki.MwTag

class BkParagraph(parent: BkPassage?): BkParent(parent) {

    override fun append(tag: MwTag) {
        when(tag.name) {
            "text" -> {
                val txt = children.lastOrNull() as? BkText ?: BkText(this).also {
                    children.add(it)
                }

                txt.append(tag)
            }

            "sperret" -> {
                BkEmphasis(this).also {
                    it.append(tag)
                    children.add(it)
                }
            }

            else -> {
                val txt = children.lastOrNull() as? BkText ?: BkText(this).also {
                    children.add(it)
                }

                (tag as? MwParent)?.children?.also {
                    it.forEach { tag ->
                        txt.append(tag)
                    }
                } ?: {
                    txt.append(tag)
                }
            }
        }
    }

    override fun renderHtml(): String =
        "<p>\n${children.joinToString("") { it.renderHtml() }.toString().trim().lines().filter { it.isNotBlank() }.joinToString("\n")}\n</p>"
}
