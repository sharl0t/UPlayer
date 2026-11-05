package ru.sharl0t.utvplayer.data

/** Разбор подписей серий: номер, описание, героиня и город. */
object EpisodeText {
    /** «1 сезон 3 серия» → 3. */
    fun number(name: String): Int? =
        Regex("""(\d+)\s*серия""", RegexOption.IGNORE_CASE).find(name)?.groupValues?.get(1)?.toIntOrNull()

    /** alt «Мама в 16. 3 серия. Анна, Ростов-на-Дону» → «Анна, Ростов-на-Дону»; null, если описания нет. */
    fun description(alt: String, showTitle: String, name: String): String? =
        alt.removePrefix(showTitle).trimStart('.', ' ')
            .replace(Regex("""^\d+\s+серия\.?\s*"""), "")
            .trim()
            .takeIf { it.isNotEmpty() && it != name }

    /** «Анна, Ростов-на-Дону» → («Анна», «Ростов-на-Дону»); null, если формат другой. */
    fun heroineAndCity(description: String): Pair<String, String>? {
        val i = description.lastIndexOf(',')
        if (i <= 0) return null
        val heroine = description.substring(0, i).trim()
        val city = description.substring(i + 1).trim()
        if (heroine.isEmpty() || city.isEmpty() || heroine.length > 40 || city.length > 40) return null
        if (heroine.contains('.') || city.contains('.')) return null
        return heroine to city
    }
}
