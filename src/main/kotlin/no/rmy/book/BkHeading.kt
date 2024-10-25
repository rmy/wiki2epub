package no.rmy.book

import no.rmy.mediawiki.MwParent
import no.rmy.mediawiki.MwTag

class BkHeading(parent: BkPassage): BkPassage(parent) {
    val content = StringBuffer()
    var level = 2

    override fun append(tag: MwTag) {
        (tag as? MwParent)?.let {
            if(it.children.any { it.hasProperty("stor") }) {
                --level
            }
        }

        content.append(tag.content())
    }


    override fun renderHtml(): String = "<h$level>$content</h$level>"
}