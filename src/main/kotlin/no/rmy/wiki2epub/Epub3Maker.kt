package no.rmy.wiki2epub

import net.seeseekey.epubwriter.model.EpubBook
import net.seeseekey.epubwriter.model.Landmark
import net.seeseekey.epubwriter.model.TocLink
import java.io.File
import java.util.*


object Epub3Maker {
    fun create(chapters: List<Chapter>) {
        Mode.current = no.rmy.wiki2epub.Mode.EPUB3

        val path = when (Mode.current) {
            Mode.EPUB2 -> "files/epub2"
            Mode.EPUB3 -> "files/epub3"
        }
        File(path).mkdirs()

        chapters.forEachIndexed { index, ch ->
            val filename = "$path/chapter_$index.xhtml"
            File(filename).writeText(ch.html())
        }

        EpubBook(
            "nb-NO",
            UUID.randomUUID().toString(),
            "Iliaden",
            "Homer"
        ).also { book ->
            book.publisher = "H. ASCHEHOUG & CO. (W. NYGAARD)"
            // Add content


            File("iliaden_cover.jpg").inputStream().let {
                book.addCoverImage(
                    it.readAllBytes(),
                    "image/jpeg",
                    "images/cover.jpeg"
                );
            }

            listOf("styles.css", "innledning.css").forEach { filename ->
                File(filename).inputStream().let {
                    book.addContent(
                        it,
                        "text/css", filename,
                        false,
                        false
                    )
                }
            }

            // Create Landmarks
            val landmarks: MutableList<Landmark> = mutableListOf()

            // Create toc
            val tocLinks: MutableList<TocLink> = ArrayList<TocLink>()

            listOf("cover.xhtml", "tittelside3.xhtml").forEach { filename ->
                val href = filename.replace("3", "")
                File(filename).inputStream().let {
                    book.addContent(
                        it,
                        "application/xhtml+xml", href,
                        true,
                        true
                    ).setId(href.split(".").first())
                }
                val chapterToc: TocLink = TocLink(href, "KOLOFON.", null)
                tocLinks.add(chapterToc)

                val landmark = Landmark()
                landmark.setType("bodymatter")
                landmark.setHref(href)
                landmark.setTitle("KOLOFON.")

                landmarks.add(landmark)
            }

            chapters.forEachIndexed { index, ch ->
                val chIndex = index + 1

                val href = "chapter_$chIndex.xhtml"

                book.addContent(
                    ch.inputStream(),
                    "application/xhtml+xml", href,
                    true,
                    true
                ).setId(href.split(".").first())

                val chapterToc: TocLink = TocLink(href, ch.title, null)
                tocLinks.add(chapterToc)
            }

            // Set toc options
            book.setAutoToc(false)
            book.setTocLinks(tocLinks)

            // Set landmarks
            book.setLandmarks(landmarks)

            when (Mode.current) {
                Mode.EPUB2 -> "iliaden.epub"
                Mode.EPUB3 -> "iliaden_epub3.epub"
            }.let {
                book.writeToFile("files/$it")
            }
        }
    }

}
