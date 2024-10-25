package no.rmy.book

import no.rmy.mediawiki.MwParent
import no.rmy.mediawiki.MwTag

class Book: BkParent(null) {

    override fun append(tag: MwTag) {
        when(tag.name) {
            "ppoem" -> {
                if(currentChild() is BkNone) {
                    children.removeLast()
                }
                if(currentChild() !is BkPoetic) {
                    children.add(BkPoetic(this))
                }
                (tag as? MwParent)?.children?.forEach { ch ->
                    currentChild()?.append(ch)
                }
                children.add(BkNone(this))
            }
            else -> {
                if(currentChild() is BkNone) {
                    children.removeLast()
                }
                if(currentChild() !is BkProse) {
                    children.add(BkProse(this))
                }
                currentChild()?.append(tag)
                children.add(BkNone(this))
            }
        }
    }
}