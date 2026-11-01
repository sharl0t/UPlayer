package ru.sharl0t.utvplayer.data

data class Show(val slug: String, val title: String, val imageUrl: String?)

data class Season(val title: String, val episodes: List<Episode>)

data class Episode(
    val url: String,
    val title: String,
    val description: String?,
    val duration: String?,
    val imageUrl: String?,
)

data class ShowDetails(val title: String, val seasons: List<Season>)
