package no.rmy.wiki2epub.mediawiki

import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun Any.getLogableName() = if (this::class.isCompanion) {
    this::class.java.declaringClass.simpleName
} else {
    this::class.simpleName ?: this.javaClass.name
}

fun Any.getAutoNamedLogger() = getLogger(getLogableName())

fun getLogger(name: String): Logger = LoggerFactory.getLogger("tellusr.$name")

