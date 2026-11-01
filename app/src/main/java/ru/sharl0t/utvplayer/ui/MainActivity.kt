package ru.sharl0t.utvplayer.ui

import androidx.fragment.app.Fragment
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.VerticalGridPresenter
import ru.sharl0t.utvplayer.R
import ru.sharl0t.utvplayer.data.Show
import ru.sharl0t.utvplayer.data.UtvApi

/** Каталог: сетка всех передач. */
class MainActivity : LoadActivity<List<Show>>() {
    override suspend fun load() = UtvApi.shows()

    override fun contentFragment(data: List<Show>): Fragment = VerticalGridSupportFragment().apply {
        title = this@MainActivity.getString(R.string.catalog_title)
        gridPresenter = VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM).apply { numberOfColumns = 5 }
        // Картинки передач на сайте 720×616
        adapter = ArrayObjectAdapter(CardPresenter(160, 137)).apply { addAll(0, data) }
        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            if (item is Show) startActivity(ShowActivity.intent(this@MainActivity, item))
        }
    }
}
