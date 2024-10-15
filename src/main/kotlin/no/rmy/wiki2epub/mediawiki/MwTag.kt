package no.rmy.wiki2epub.mediawiki


interface MwTag {
    val parent: MwTag?

    val name: String

    fun content(): String
}

class MwText(override val parent: MwTag, val value: String): MwTag {
    override val name: String get() = "text"

    override fun content(): String = value
}


class MwParent(override val parent: MwTag?): MwTag {
    override fun content(): String = children.joinToString("\n") {
        it.content()
    }

    val children: MutableList<MwTag> = mutableListOf()

    val values: MutableList<String> = mutableListOf()

    override val name: String get() = values.firstOrNull() ?: ""


    fun parse(text: String): Int {
        var valueStart: Int = 0
        var offset: Int = 0
        val length = text.length

        // {{ppoem|start=follow|end=follow|
        while(offset < length) {
            when(text[offset]) {
                '{' -> {
                    ++offset
                    if(text[offset] == '{') {
                        text.substring(valueStart until offset).let {
                            children.add(MwText(this, it))
                        }
                        ++offset
                        valueStart = offset


                        ++offset
                        offset += MwParent(this).let {
                            children.add(it)
                            it.parse(text.substring(offset))
                        }
                    }
                }

                '}' -> {
                    ++offset
                    if(text[offset] == '}') {
                        ++offset
                        break
                    }
                }

                '|' -> {
                    text.substring(valueStart until offset).let {
                        values.add(it)
                    }
                    ++offset
                    valueStart = offset
                }

                else -> ++offset
            }
        }
        return offset
    }
}
