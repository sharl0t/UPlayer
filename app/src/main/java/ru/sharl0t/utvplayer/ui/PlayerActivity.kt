package ru.sharl0t.utvplayer.ui

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import ru.sharl0t.utvplayer.data.UtvApi

/** Плеер: по странице серии находит адрес master.m3u8 и открывает [PlayerFragment]. */
class PlayerActivity : LoadActivity<String>() {
    override suspend fun load(): String = UtvApi.streamUrl(intent.getStringExtra(EXTRA_EPISODE_URL)!!)

    override fun contentFragment(data: String): Fragment =
        PlayerFragment.create(data, intent.getStringExtra(EXTRA_TITLE).orEmpty())

    companion object {
        const val EXTRA_EPISODE_URL = "episode_url"
        const val EXTRA_TITLE = "title"

        fun intent(context: Context, episodeUrl: String, title: String): Intent =
            Intent(context, PlayerActivity::class.java)
                .putExtra(EXTRA_EPISODE_URL, episodeUrl)
                .putExtra(EXTRA_TITLE, title)
    }
}
