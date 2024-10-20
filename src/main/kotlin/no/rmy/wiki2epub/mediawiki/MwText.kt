package no.rmy.wiki2epub.mediawiki

class MwText(override val parent: MwTag, val value: String) : MwTag {
    override val name: String get() = "text"

    override fun content(): String = value
}

