package no.rmy.mediawiki


interface MwTag {
    val parent: MwTag?

    val name: String get() = "Undefined"

    fun hasProperty(name: String): Boolean = false

    fun content(): String

    fun render(): String
}
