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

            "rettelse" -> {
                BkRettelse(this).also {
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

    override fun renderHtml(): String = if(hasProperty(Properties.Poetic)) {
        val content = children.joinToString("") { it.renderHtml() }.trim()
        content.lines().mapIndexed { index, it ->
            when(index) {
                0 -> "&emsp; ${it.trim()} <br/>"
                else -> "${it.trim()} <br/>"
            }
        }.joinToString("\n").let {
            "<p>\n$it\n</p>"
        }
    }
    else {
        "<p>\n${children.joinToString("") { it.renderHtml() }.trim().lines().filter { it.isNotBlank() }.joinToString("\n")}\n</p>"
    }
}
