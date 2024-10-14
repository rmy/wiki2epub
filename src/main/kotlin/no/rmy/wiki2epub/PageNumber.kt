package no.rmy.wiki2epub

class PageNumber(content: String) : Tag {
    val text2 = content.trim().split("|").last().trimEnd('}')
    val number: Int? = text2.toIntOrNull()?.let {
        val page = it - 10
        if (page > 0)
            page
        else
            null
    }

    override fun epub2html(): String =
        "<span title=\"[Pg $number]\"><a id=\"Page_$number\" title=\"[Pg $number]\"></a></span>"

    override fun epub3html(): String {
        //return "<span epub:type=\"pagebreak\" id=\"page$number\">$number</span>x"
        return "<span epub:type=\"pagebreak\" title=\"$number\" id=\"side$number\"></span>"
    }

    fun spannedNumberHtml(): String {
        return "<span>$number</span>"
    }

    override fun html(): String = number?.let {
        when(Mode.current) {
            Mode.EPUB2 -> epub2html()
            Mode.EPUB3 -> epub3html()
        }
    } ?: ""
}

