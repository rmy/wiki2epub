package no.rmy.book

import no.rmy.mediawiki.MwTag
import no.rmy.mediawiki.MwText

class BkText(parent: BkParent) : BkPassage(parent) {
    val content = StringBuffer()

    override fun append(tag: MwTag) {
        when (tag) {
            is MwText -> content.append(tag.content())

            else -> {
                content.append("\n\n" + tag.name)
                content.append(": ")
                content.append(tag.content())
            }
        }
    }

    override fun renderHtml(): String = content.toString()
}