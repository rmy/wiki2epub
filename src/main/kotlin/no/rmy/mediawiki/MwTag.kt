package no.rmy.mediawiki


interface MwTag {
    val parent: MwTag?

    val name: String get() = "Undefined"

    fun content(): String

    fun render(): String
}
