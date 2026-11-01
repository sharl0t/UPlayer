package ru.sharl0t.utvplayer.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity

/** Плеер. Пока заглушка: сам плеер делается в задаче 4. */
class PlayerActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(this, intent.getStringExtra(EXTRA_TITLE), Toast.LENGTH_LONG).show()
        finish()
    }

    companion object {
        const val EXTRA_EPISODE_URL = "episode_url"
        const val EXTRA_TITLE = "title"

        fun intent(context: Context, episodeUrl: String, title: String): Intent =
            Intent(context, PlayerActivity::class.java)
                .putExtra(EXTRA_EPISODE_URL, episodeUrl)
                .putExtra(EXTRA_TITLE, title)
    }
}
