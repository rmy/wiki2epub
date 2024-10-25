package no.rmy.book

import no.rmy.mediawiki.MwParent
import no.rmy.mediawiki.MwTag
import org.slf4j.LoggerFactory

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
            "midtstilt" -> {
                if(currentChild() is BkNone) {
                    children.removeLast()
                }
                BkHeading(this).also { heading ->
                    heading.append(tag)
                    children.add(heading)
                }
            }
            "unnamed" -> {
                (tag as? MwParent)?.let {
                    it.children.forEach {
                        append(it)
                    }
                }
            }
            else -> {
                logger.info(tag.name)
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


    companion object {
        val logger = LoggerFactory.getLogger("Book")
    }
}