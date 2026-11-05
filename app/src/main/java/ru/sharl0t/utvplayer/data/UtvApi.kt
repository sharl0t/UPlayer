package ru.sharl0t.utvplayer.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException

/** Разбор страниц u-tv.ru. Как устроен сайт, описано в README. */
object UtvApi {
    private const val BASE = "https://www.u-tv.ru"
    private const val MAX_PAGES = 20

    suspend fun shows(): List<Show> = withContext(Dispatchers.IO) {
        val doc = get("$BASE/shows/")
        val result = LinkedHashMap<String, Show>()
        parseShows(doc, result)

        // Первые 30 передач есть в HTML, остальные подгружаются кнопкой «Показать ещё»
        val token = doc.selectFirst("meta[name=csrf-token]")?.attr("content")
        var page = doc.selectFirst("#allProjects__btn")?.attr("data-page")?.toIntOrNull()
        var requests = 0
        while (token != null && page != null && requests++ < MAX_PAGES) {
            val body = FormBody.Builder()
                .add("tab", "all")
                .add("page", page.toString())
                .add("sort", "new")
                .add("slug", "shows")
                .build()
            val request = Request.Builder()
                .url("$BASE/projects/loading/")
                .post(body)
                .header("X-CSRF-TOKEN", token)
                .header("X-Requested-With", "XMLHttpRequest")
                .build()
            val json = JSONObject(fetch(request))
            parseShows(Jsoup.parseBodyFragment(json.optString("html"), BASE), result)
            page = if (json.optBoolean("btnNextLoad")) json.optInt("btnNumberPage") else null
        }
        result.values.toList()
    }

    suspend fun show(slug: String): ShowDetails = withContext(Dispatchers.IO) {
        val doc = get("$BASE/shows/$slug/")
        val title = doc.selectFirst("h1.project__title")?.text().orEmpty().ifBlank { slug }
        val seasons = doc.select("div.defaultTabs__item[data-season]")
            .filterNot { it.attr("data-season").contains("анонс", ignoreCase = true) }
            .map { season ->
                val seasonTitle = season.attr("data-season").trim()
                Season(
                    title = seasonTitle,
                    episodes = season.select("a.swiperGroup__slide").map { parseEpisode(it, title, seasonTitle) },
                )
            }
            .filter { it.episodes.isNotEmpty() }
        ShowDetails(title, seasons)
    }

    /** Страница серии → id видео → адрес master.m3u8. */
    suspend fun streamUrl(episodeUrl: String): String = withContext(Dispatchers.IO) {
        val player = get(episodeUrl).selectFirst("[data-edge-player]")?.attr("data-edge-player")
            ?: throw IOException("На странице серии нет плеера")
        val id = Regex("/videos/([^/?#]+)").find(player)?.groupValues?.get(1)
            ?: throw IOException("Не удалось найти id видео")
        val fromMediaPage = runCatching {
            Regex("""https://[^"'\s]+?\.m3u8""").find(fetch(Request.Builder().url(player).build()))?.value
        }.getOrNull()
        fromMediaPage ?: "https://cdn.media1.ru/videos/$id/master.m3u8"
    }

    private fun parseShows(doc: Document, into: MutableMap<String, Show>) {
        for (a in doc.select("a.projects__item[href]")) {
            val slug = Regex("/shows/([^/]+)/").find(a.attr("abs:href"))?.groupValues?.get(1) ?: continue
            if (slug in into) continue
            val title = a.selectFirst(".projects__name")?.text()?.trim().orEmpty()
                .ifBlank { a.selectFirst("img")?.attr("alt").orEmpty() }
            into[slug] = Show(slug, title, imageUrl(a))
        }
    }

    private fun parseEpisode(a: Element, showTitle: String, seasonTitle: String): Episode {
        val name = a.selectFirst(".swiperGroup__name")?.text()?.trim().orEmpty()
        // alt вида «Мама в 16. 3 серия. Анна, Ростов-на-Дону»: оставляем только описание после номера серии
        val description = EpisodeText.description(a.selectFirst("img")?.attr("alt").orEmpty(), showTitle, name)
        val person = description?.let(EpisodeText::heroineAndCity)
        return Episode(
            url = a.attr("abs:href"),
            title = name.removePrefix(seasonTitle).trim().ifBlank { name },
            description = description,
            duration = a.selectFirst(".timeVideo")?.text()?.trim()?.takeIf { it.isNotEmpty() },
            imageUrl = imageUrl(a),
            number = EpisodeText.number(name),
            heroine = person?.first,
            city = person?.second,
        )
    }

    /** Первая картинка из srcset подходящего размера; у img src бывает огромный оригинал. */
    private fun imageUrl(e: Element): String? {
        val srcset = e.selectFirst("picture source[srcset]")?.attr("srcset")
        val fromSrcset = srcset?.split(",")?.firstOrNull()?.trim()?.substringBefore(" ")
        return fromSrcset?.takeIf { it.startsWith("http") } ?: e.selectFirst("img[src]")?.attr("abs:src")
    }

    private fun get(url: String): Document = Jsoup.parse(fetch(Request.Builder().url(url).build()), url)

    private fun fetch(request: Request): String =
        Net.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}: ${request.url}")
            response.body?.string() ?: throw IOException("Пустой ответ: ${request.url}")
        }
}
