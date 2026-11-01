package ru.sharl0t.utvplayer.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import ru.sharl0t.utvplayer.R
import ru.sharl0t.utvplayer.data.Episode
import ru.sharl0t.utvplayer.data.Show
import ru.sharl0t.utvplayer.data.ShowDetails
import ru.sharl0t.utvplayer.data.UtvApi

/** Передача: сезоны слева, серии сезона рядом. */
class ShowActivity : LoadActivity<ShowDetails>() {
    override suspend fun load() = UtvApi.show(intent.getStringExtra(EXTRA_SLUG).orEmpty())

    override fun contentFragment(data: ShowDetails): Fragment {
        if (data.seasons.isEmpty()) return MessageFragment.info(getString(R.string.no_episodes))
        // Картинки серий на сайте 600×336
        val cards = CardPresenter(240, 135)
        val rows = ArrayObjectAdapter(ListRowPresenter())
        data.seasons.forEachIndexed { i, season ->
            rows.add(ListRow(HeaderItem(i.toLong(), season.title), ArrayObjectAdapter(cards).apply {
                addAll(0, season.episodes)
            }))
        }
        return BrowseSupportFragment().apply {
            title = data.title
            headersState = BrowseSupportFragment.HEADERS_ENABLED
            isHeadersTransitionOnBackEnabled = true
            brandColor = ContextCompat.getColor(this@ShowActivity, R.color.brand)
            adapter = rows
            onItemViewClickedListener = OnItemViewClickedListener { _, item, _, row ->
                if (item is Episode) {
                    val season = (row as ListRow).headerItem.name
                    val title = "${data.title}, $season, ${item.title}"
                    startActivity(PlayerActivity.intent(this@ShowActivity, item.url, title))
                }
            }
        }
    }

    companion object {
        private const val EXTRA_SLUG = "slug"

        fun intent(context: Context, show: Show): Intent =
            Intent(context, ShowActivity::class.java).putExtra(EXTRA_SLUG, show.slug)
    }
}
