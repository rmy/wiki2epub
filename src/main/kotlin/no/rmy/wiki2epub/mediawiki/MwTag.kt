package no.rmy.wiki2epub.mediawiki


interface MwTag {
    val parent: MwTag?

    val name: String

    fun content(): String
}


