package no.rmy.mediawiki

import io.kotest.core.spec.style.FunSpec
import no.rmy.book.Book
import org.slf4j.LoggerFactory


class MediaWikiTest : FunSpec({
    val logger = LoggerFactory.getLogger(this::class.java)

    test("output html") {
        logger.info(this.testCase.name.originalName)
        MediaWiki.iliaden(11, 27)?.let {
            val book = Book()
            book.append(it)
            println(book.renderHtml())
            logger.info(book.renderHtml())
            //logger.info(it.content())
        } ?: logger.info("No tags created")
    }

    test("parse some content") {
        logger.info(this.testCase.name.originalName)
        MediaWiki.iliaden(11, 27)?.let {
            logger.info(it.render())
        } ?: logger.info("No tags created")
    }

})

