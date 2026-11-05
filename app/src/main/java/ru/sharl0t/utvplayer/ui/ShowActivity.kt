package ru.sharl0t.utvplayer.ui

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import ru.sharl0t.utvplayer.R
import ru.sharl0t.utvplayer.data.Show
import ru.sharl0t.utvplayer.data.ShowDetails
import ru.sharl0t.utvplayer.data.UtvApi

/** Передача: загрузка через LoadActivity, содержимое `ShowFragment`. */
class ShowActivity : LoadActivity<ShowDetails>() {
    override suspend fun load() = UtvApi.show(intent.getStringExtra(EXTRA_SLUG).orEmpty())

    override fun contentFragment(data: ShowDetails): Fragment {
        if (data.seasons.isEmpty()) return MessageFragment.info(getString(R.string.no_episodes))
        return ShowFragment.create(data) { season, episode ->
            val title = "${data.title}, $season, ${episode.title}"
            startActivity(PlayerActivity.intent(this, episode.url, title))
        }
    }

    companion object {
        private const val EXTRA_SLUG = "slug"

        fun intent(context: Context, show: Show): Intent =
            Intent(context, ShowActivity::class.java).putExtra(EXTRA_SLUG, show.slug)
    }
}
