package ru.sharl0t.utvplayer.data

data class Show(val slug: String, val title: String, val imageUrl: String?)

data class Season(val title: String, val episodes: List<Episode>)

data class Episode(
    val url: String,
    val title: String,
    val description: String?,
    val duration: String?,
    val imageUrl: String?,
    /** Номер серии из «1 сезон 3 серия», null если формат другой. */
    val number: Int? = null,
    /** Героиня и город из подписи; оба null, если подпись другого формата (тогда показывать description). */
    val heroine: String? = null,
    val city: String? = null,
)

data class ShowDetails(val title: String, val seasons: List<Season>)
