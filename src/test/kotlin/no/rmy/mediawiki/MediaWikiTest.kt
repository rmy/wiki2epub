package no.rmy.mediawiki

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MediaWikiTest : FunSpec({
    val logger = LoggerFactory.getLogger(this::class.java)

    test("parse some content") {
        logger.info(this.testCase.name.originalName)
        MediaWiki.iliaden(11)?.let {
            //logger.info(it.content())
        } ?: logger.info("No tags created")
    }

})

