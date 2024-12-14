package no.rmy.mediawiki


class MwParent(override val parent: MwTag?) : MwTag {
    override fun content(): String = children.joinToString("\n") {
        it.content()
    }

    val children: MutableList<MwTag> = mutableListOf()

    val properties: MutableList<String> = mutableListOf()

    override fun hasProperty(name: String) =
        properties.any { it.equals(name, true) }


    override val name: String get() = properties.firstOrNull()?.lowercase() ?: "unnamed"

    override fun render(): String = listOf(
        properties,
        children.map { it.render() }
    ).flatten().joinToString("|").let {
        "{{$it}}"
    }


    fun parse(text: String): Int {
        var valueStart: Int = 0
        var offset: Int = 0
        val length = text.length

        logger.info("Begin")

        // {{ppoem|start=follow|end=follow|
        while (offset < length) {
            when (text[offset]) {
                '{' -> {
                    ++offset
                    if (text[offset] == '{') {
                        ++offset
                        text.substring(valueStart, offset - 2).ifBlank { null }?.let {
                            logger.info("Text: {}", it)
                            children.add(MwText(this, it))
                        }
                        valueStart = offset

                        MwParent(this).also {
                            offset += it.parse(text.substring(valueStart))
                            children.add(it)
                            valueStart = offset
                        }
                    }
                }

                '}' -> {
                    ++offset
                    if (text[offset] == '}') {
                        ++offset
                        text.substring(valueStart, offset - 2).ifBlank { null }?.let {
                            logger.info("Text: {}", it)
                            children.add(MwText(this, it))
                        }
                        valueStart = offset
                        break
                    }
                }

                '|' -> {
                    text.substring(valueStart until offset).let {
                        logger.info("Value: {}", it)
                        properties.add(it)
                    }
                    ++offset
                    valueStart = offset
                }

                else -> ++offset
            }
        }

        if (offset > valueStart) {
            text.substring(valueStart, offset).ifBlank { null }?.let {
                logger.info("TextEnd: {}", it)
                children.add(MwText(this, it))
            }
        }

        logger.info("End")

        return offset
    }

    companion object {
        val logger = getAutoNamedLogger()
    }
}
