package no.rmy.book

import no.rmy.mediawiki.MwTag

abstract class BkPassage(val parent: BkPassage? = null) {
    enum class Properties {
        Poetic
    }

    open fun hasProperty(property: Properties): Boolean = parent?.hasProperty(property) ?: false

    abstract fun append(tag: MwTag)

    abstract fun renderHtml(): String
}
