package no.rmy.mediawiki


interface MwTag {
    val parent: MwTag?

    val name: String

    fun content(): String
}
