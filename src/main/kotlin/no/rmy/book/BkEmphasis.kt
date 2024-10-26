package no.rmy.book

import no.rmy.mediawiki.MwTag

class BkEmphasis(parent: BkPassage): BkPassage(parent) {
    var content = String()

    override fun append(tag: MwTag) {
        content = tag.content()
    }

    override fun renderHtml(): String =
        "<em>$content</em>"
}