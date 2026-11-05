package ru.sharl0t.utvplayer.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.Transition
import android.view.View
import android.widget.ImageView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.IntentCompat
import androidx.fragment.app.Fragment
import coil.imageLoader
import coil.load
import coil.memory.MemoryCache
import coil.request.SuccessResult
import coil.result
import ru.sharl0t.utvplayer.R
import ru.sharl0t.utvplayer.data.Show
import ru.sharl0t.utvplayer.data.ShowDetails
import ru.sharl0t.utvplayer.data.UtvApi

/** Передача: загрузка через LoadActivity, содержимое `ShowFragment`. */
class ShowActivity : LoadActivity<ShowDetails>() {
    private val handler = Handler(Looper.getMainLooper())

    /** Картинка ещё разворачивается: содержимое ждёт конца, чтобы не мелькнуть под ней. */
    private var opening = false
    private var afterOpen: (() -> Unit)? = null
    private var contentShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(EXTRA_SHEET, false)) showSheet(savedInstanceState == null)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override suspend fun load() = UtvApi.show(intent.getStringExtra(EXTRA_SLUG).orEmpty())

    override fun contentFragment(data: ShowDetails): Fragment {
        return ShowFragment.create(data) { season, episode ->
            val title = "${data.title}, $season, ${episode.title}"
            startActivity(PlayerActivity.intent(this, episode.url, title))
        }
    }

    /** Картинка карточки на весь экран: цель «разворота» и фон, пока грузится передача. */
    private fun showSheet(firstStart: Boolean) {
        val sheet = findViewById<ImageView>(R.id.sheet)
        sheet.visibility = View.VISIBLE
        sheet.transitionName = intent.getStringExtra(EXTRA_TRANSITION)
        // Та же картинка, что у карточки, сразу из памяти: переход начинается с готового кадра
        val key = IntentCompat.getParcelableExtra(intent, EXTRA_MEMORY_KEY, MemoryCache.Key::class.java)
        val cached = key?.let { imageLoader.memoryCache?.get(it)?.bitmap }
        if (cached != null) sheet.setImageBitmap(cached) else sheet.load(intent.getStringExtra(EXTRA_IMAGE))

        val transition = window.sharedElementEnterTransition ?: return
        if (!firstStart) return
        opening = true
        // «Ю» не мелькает над каталогом до разворота: появится после него, если передача ещё грузится
        findViewById<View>(R.id.progress).visibility = View.INVISIBLE
        transition.addListener(object : Transition.TransitionListener {
            override fun onTransitionEnd(t: Transition) = opened()
            override fun onTransitionCancel(t: Transition) = opened()
            override fun onTransitionStart(t: Transition) {}
            override fun onTransitionPause(t: Transition) {}
            override fun onTransitionResume(t: Transition) {}
        })
        // Если переход так и не начался, содержимое всё равно появится
        handler.postDelayed(::opened, OPEN_TIMEOUT_MS)
    }

    private fun opened() {
        if (!opening) return
        opening = false
        if (!contentShown) findViewById<View>(R.id.progress).visibility = View.VISIBLE
        afterOpen?.invoke()
        afterOpen = null
    }

    override fun onContentShown(content: View) {
        contentShown = true
        if (!intent.getBooleanExtra(EXTRA_SHEET, false)) return
        // Поверх развёрнутой картинки содержимое проявляется, как .showscr в макете
        content.animate().cancel()
        content.alpha = 0f
        val fade = { content.animate().alpha(1f).setDuration(300).start(); Unit }
        if (opening) afterOpen = fade else fade()
    }

    companion object {
        private const val EXTRA_SLUG = "slug"
        private const val EXTRA_SHEET = "sheet"
        private const val EXTRA_TRANSITION = "transition"
        private const val EXTRA_IMAGE = "image"
        private const val EXTRA_MEMORY_KEY = "memory_key"
        private const val OPEN_TIMEOUT_MS = 1000L

        fun transitionName(show: Show) = "show:${show.slug}"

        fun intent(context: Context, show: Show): Intent =
            Intent(context, ShowActivity::class.java).putExtra(EXTRA_SLUG, show.slug)

        /**
         * Открыть передачу, развернув картинку карточки на весь экран.
         * Без картинки открывается как обычно.
         */
        fun open(activity: Activity, show: Show, image: ImageView) {
            val intent = intent(activity, show)
            if (image.drawable == null || !image.isAttachedToWindow) {
                activity.startActivity(intent)
                return
            }
            val name = transitionName(show)
            image.transitionName = name
            intent.putExtra(EXTRA_SHEET, true)
                .putExtra(EXTRA_TRANSITION, name)
                .putExtra(EXTRA_IMAGE, show.imageUrl)
                .putExtra(EXTRA_MEMORY_KEY, (image.result as? SuccessResult)?.memoryCacheKey)
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, image, name)
            activity.startActivity(intent, options.toBundle())
        }
    }
}
