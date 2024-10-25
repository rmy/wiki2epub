package no.rmy.book

import no.rmy.mediawiki.MwTag

abstract class BkParent(parent: BkPassage?): BkPassage(parent) {
    val children = mutableListOf<BkPassage>()

    fun currentChild(): BkPassage? = children.lastOrNull()

    override fun renderHtml(): String = children.joinToString("\n") {
        it.renderHtml()
    }

    override fun append(tag: MwTag) {
        when(tag.name) {
            "indent", "innrykk", "gap" -> {
                children.add(BkParagraph(this))
            }
            else -> {
                if(children.isEmpty()) {
                    children.add(BkParagraph(this))
                }
                children.last().append(tag)
            }
        }
    }
}
