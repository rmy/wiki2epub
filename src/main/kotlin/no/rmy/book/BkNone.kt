package no.rmy.book

import no.rmy.mediawiki.MwTag

class BkNone(parent: BkPassage): BkPassage(parent) {
    override fun append(tag: MwTag) {}

    override fun renderHtml(): String = ""
}
