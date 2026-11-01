package ru.sharl0t.utvplayer.data

import kotlinx.coroutines.runBlocking
import okhttp3.Request
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/**
 * Разбор на живом сайте u-tv.ru. Ходит в сеть, поэтому включается только так:
 * `UTV_LIVE=1 ./gradlew testDebugUnitTest`.
 */
class LiveSiteTest {
    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            assumeTrue("UTV_LIVE не задан", System.getenv("UTV_LIVE") == "1")
            File("src/main/res/raw/extra_roots.pem").inputStream().use { Net.init(it) }
        }
    }

    @Test
    fun catalogShowsSeasonsAndStreams() = runBlocking {
        val shows = UtvApi.shows()
        println("Передач: ${shows.size}")
        assertTrue("слишком мало передач", shows.size > 30)
        assertTrue("передачи без названия", shows.all { it.title.isNotBlank() })
        println("Без картинки: ${shows.filter { it.imageUrl == null }.map { it.slug }}")
        println("Хосты картинок: ${shows.mapNotNull { it.imageUrl?.substringAfter("://")?.substringBefore("/") }.toSet()}")

        val mismatches = mutableListOf<String>()
        var episodes = 0
        var firstEpisode: Episode? = null
        for (show in shows) {
            val details = UtvApi.show(show.slug)
            // data-total сверяем по сырому HTML, UtvApi его не читает
            val html = Jsoup.parse(
                Net.client.newCall(Request.Builder().url("https://www.u-tv.ru/shows/${show.slug}/").build())
                    .execute().use { it.body!!.string() }
            )
            for (season in html.select("div.defaultTabs__item[data-season]")) {
                val total = season.attr("data-total").toIntOrNull()
                val count = season.select("a.swiperGroup__slide").size
                if (total != count) mismatches += "${show.slug} «${season.attr("data-season")}»: data-total=$total, в HTML $count"
            }
            val n = details.seasons.sumOf { it.episodes.size }
            episodes += n
            if (firstEpisode == null) firstEpisode = details.seasons.firstOrNull()?.episodes?.firstOrNull()
            println("${show.slug}: «${details.title}», сезонов ${details.seasons.size} (${details.seasons.joinToString { "${it.title}: ${it.episodes.size}" }})")
            details.seasons.flatMap { it.episodes }.forEach {
                assertTrue("${show.slug}: серия без адреса", it.url.startsWith("https://"))
                assertTrue("${show.slug}: серия без названия", it.title.isNotBlank())
            }
            assertTrue("${show.slug}: остался анонс", details.seasons.none { it.title.contains("анонс", true) })
        }
        println("Серий всего: $episodes")
        println("Несовпадений data-total: ${mismatches.size}")
        mismatches.forEach(::println)

        val episode = firstEpisode!!
        println("Пример серии: $episode")
        val stream = UtvApi.streamUrl(episode.url)
        println("Поток: $stream")
        val playlist = Net.client.newCall(Request.Builder().url(stream).build()).execute().use { it.body!!.string() }
        assertTrue("это не HLS", playlist.startsWith("#EXTM3U"))
        println(playlist.lines().filter { it.startsWith("#EXT-X-STREAM-INF") }.joinToString("\n"))
        assertEquals(emptyList<String>(), mismatches)
    }
}
