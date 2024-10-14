package no.rmy.wiki2epub

enum class Mode {
    EPUB2, EPUB3;

    companion object {
        var current = EPUB3
    }

}