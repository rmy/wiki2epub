package no.rmy.book

import no.rmy.mediawiki.MwTag

class BkPoetic(parent: BkPassage?): BkParent(parent) {
    val content: MutableList<BkPassage> = mutableListOf()

    override fun hasProperty(property: Properties) = when(property) {
        Properties.Poetic -> true
        else -> super.hasProperty(property)
    }
}
