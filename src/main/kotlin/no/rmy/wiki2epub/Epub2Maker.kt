package no.rmy.wiki2epub

import io.documentnode.epub4j.domain.Author
import io.documentnode.epub4j.domain.Book
import io.documentnode.epub4j.domain.Resource
import io.documentnode.epub4j.epub.EpubWriter
import java.io.File
import java.io.FileOutputStream


object Epub2Maker {
    fun create(chapters: List<Chapter>) {
        Mode.current = Mode.EPUB2

        val path = when (Mode.current) {
            Mode.EPUB2 -> "files/epub2"
            Mode.EPUB3 -> "files/epub3"
        }
        File(path).mkdirs()

        chapters.forEachIndexed { index, ch ->
            val filename = "$path/chapter_$index.xhtml"
            File(filename).writeText(ch.html())
        }

        val ebook = Book().apply {
            metadata.apply {
                titles.add("Iliaden")
                contributors.add(Author("Homer"))
                contributors.add(Author("Peder", "Østbye (oversetter)"))
                contributors.add(Author("Øystein", "Tvede (digital utgave)"))
                publishers.add("H. ASCHEHOUG & CO. (W. NYGAARD)")
            }

            Resource(File("iliaden_cover.jpg").inputStream(), "iliaden_cover.jpg").let {
                setCoverImage(it)
            }
            Resource(File("styles.css").inputStream(), "styles.css").let {
                addResource(it)
            }
            Resource(File("innledning.css").inputStream(), "innledning.css").let {
                addResource(it)
            }


            when (Mode.current) {
                Mode.EPUB2 -> "kolofon.xhtml"
                Mode.EPUB3 -> "kolofon3.xhtml"
            }.let { filename ->
                Resource(File(filename).inputStream(), "tittelside.xhtml").let {
                    addResource(it)
                    spine.addResource(it)
                }
            }
            chapters.forEachIndexed { index, ch ->
                val chIndex = index + 1
                val chapterResource = Resource(ch.inputStream(), "chapter_$chIndex.xhtml")

                this.addSection(ch.title, chapterResource)
            }

        }


        val ebookWriter = EpubWriter()
        when (Mode.current) {
            Mode.EPUB2 -> "iliaden_epub2.epub"
            Mode.EPUB3 -> "iliaden.epub"
        }.let {
            ebookWriter.write(ebook, FileOutputStream("docs/download/$it"))
        }

    }
}