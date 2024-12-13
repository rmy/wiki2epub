package no.rmy.book

import no.rmy.mediawiki.MwTag

class BkRettelse(parent: BkPassage): BkPassage(parent) {
    var content = String()

    override fun append(tag: MwTag) {
        content = tag.content()
    }

    override fun renderHtml(): String = content
}
