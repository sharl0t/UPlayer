package ru.sharl0t.utvplayer.ui

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.graphics.Outline
import android.view.KeyEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.leanback.widget.BaseGridView
import androidx.leanback.widget.VerticalGridView
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import coil.load
import ru.sharl0t.utvplayer.R
import ru.sharl0t.utvplayer.data.Show

/** Главный экран: шапка, верхний блок и сетка передач на 6 колонок. */
class MainFragment : Fragment(R.layout.fragment_main) {
    private lateinit var shows: List<Show>
    private val handler = Handler(Looper.getMainLooper())
    private var heroTask: Runnable? = null
    // Первые ~секунду после запуска карточки выезжают волной
    private var entering = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val count = view.findViewById<TextView>(R.id.count)
        // Счётчик передач набегает от нуля
        ValueAnimator.ofInt(0, shows.size).apply {
            duration = 700
            addUpdateListener { count.text = plural(it.animatedValue as Int) }
        }.start()
        entering = true
        handler.postDelayed({ entering = false }, ENTER_WINDOW_MS)
        val grid = view.findViewById<VerticalGridView>(R.id.grid)
        val settings = view.findViewById<View>(R.id.settings)
        settings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
        // На шестерёнке в фокусе значок поворачивается
        settings.setOnFocusChangeListener { v, focused ->
            v.animate().rotation(if (focused) 90f else 0f).setDuration(250).start()
        }
        // Вниз с шестерёнки: на карточку, где фокус был до этого (сетка помнит выбранную позицию)
        settings.setOnKeyListener { _, keyCode, event ->
            (keyCode == KeyEvent.KEYCODE_DPAD_DOWN).also {
                if (it && event.action == KeyEvent.ACTION_DOWN) grid.requestFocus()
            }
        }
        // Первый ряд стоит у верхнего края сетки и не уезжает под верхний блок
        grid.windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE
        grid.windowAlignmentOffsetPercent = 0f
        grid.windowAlignmentOffset = 0
        grid.itemAlignmentOffsetPercent = 0f
        val gap = (27 * resources.displayMetrics.density).toInt()
        grid.setHorizontalSpacing(gap)
        grid.setVerticalSpacing(gap)
        val heroImage = view.findViewById<ImageView>(R.id.hero_image)
        val heroTitle = view.findViewById<TextView>(R.id.hero_title)
        val heroBar = view.findViewById<View>(R.id.hero_bar)
        val heroPos = view.findViewById<TextView>(R.id.hero_pos)
        val bgs = arrayOf(view.findViewById<ImageView>(R.id.bg_a), view.findViewById<ImageView>(R.id.bg_b))
        var bgFront = 0
        // Фон: новая картинка загружается в скрытый слой и после загрузки плавно проявляется поверх прежней
        val setBackground = { url: String? ->
            val next = bgs[1 - bgFront]
            val prev = bgs[bgFront]
            next.load(url) {
                listener(onSuccess = { _, _ ->
                    next.animate().alpha(BG_ALPHA).setDuration(BG_FADE_MS).start()
                    prev.animate().alpha(0f).setDuration(BG_FADE_MS).start()
                    bgFront = 1 - bgFront
                })
            }
        }
        // Верхний блок обновляется через 260 мс после фокуса: при быстрой прокрутке лишние картинки не грузятся
        val onFocus: (Int) -> Unit = { position ->
            heroTask?.let { handler.removeCallbacks(it) }
            val task = Runnable {
                val show = shows[position]
                setBackground(show.imageUrl)
                // Картинка уезжает, на её месте приезжает новая; текст поднимается снизу, черта растёт
                val swap: () -> Unit = {
                    heroTitle.text = show.title
                    heroPos.text = "${position + 1} из ${shows.size}"
                    heroBar.visibility = View.VISIBLE
                    Anim.enter(heroTitle, dyDp = 18f)
                    Anim.enter(heroPos, delayMs = 80, dyDp = 18f)
                    Anim.stretch(heroBar, delayMs = 60)
                    heroImage.load(show.imageUrl) {
                        listener(onSuccess = { _, _ -> Anim.slideIn(heroImage) }, onError = { _, _ -> Anim.slideIn(heroImage) })
                    }
                }
                if (heroBar.visibility == View.VISIBLE) Anim.slideOut(heroImage, swap) else swap()
            }
            heroTask = task
            handler.postDelayed(task, HERO_DELAY_MS)
        }
        grid.adapter = ShowAdapter(shows, COLUMNS, settings, onFocus, { entering }) {
            startActivity(ShowActivity.intent(requireContext(), it))
        }
        grid.post { grid.requestFocus() }
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        heroTask?.let { handler.removeCallbacks(it) }
        heroTask = null
        super.onDestroyView()
    }

    private fun plural(n: Int): String {
        val word = when {
            n % 100 in 11..14 -> "передач"
            n % 10 == 1 -> "передача"
            n % 10 in 2..4 -> "передачи"
            else -> "передач"
        }
        return "$n $word"
    }

    private class ShowAdapter(
        private val items: List<Show>,
        private val columns: Int,
        private val settings: View,
        private val onFocus: (Int) -> Unit,
        private val isEntering: () -> Boolean,
        private val onClick: (Show) -> Unit,
    ) : RecyclerView.Adapter<ShowAdapter.Holder>() {
        class Holder(view: View, onFocus: (Int) -> Unit) : RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewById(R.id.image)
            val name: TextView = view.findViewById(R.id.name)

            init {
                // Скругление картинки
                val radius = view.resources.getDimension(R.dimen.card_radius)
                image.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(v: View, outline: Outline) =
                        outline.setRoundRect(0, 0, v.width, v.height, radius)
                }
                image.clipToOutline = true
                dimOutline(view.findViewById(R.id.dim), radius)
                val lift = view.resources.getDimension(R.dimen.focus_lift)
                view.setOnFocusChangeListener { v, focused ->
                    Anim.lift(v, focused, lift)
                    // Бегущая строка у длинного названия работает только у выделенного TextView
                    name.isSelected = focused
                    if (focused && adapterPosition != RecyclerView.NO_POSITION) onFocus(adapterPosition)
                }
            }

            private fun dimOutline(dim: View, radius: Float) {
                dim.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(v: View, outline: Outline) =
                        outline.setRoundRect(0, 0, v.width, v.height, radius)
                }
                dim.clipToOutline = true
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_show, parent, false), onFocus)

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val show = items[position]
            holder.name.text = show.title
            holder.image.load(show.imageUrl)
            holder.itemView.setOnClickListener { onClick(show) }
            // Вверх из первого ряда: на шестерёнку
            holder.itemView.setOnKeyListener { _, keyCode, event ->
                (keyCode == KeyEvent.KEYCODE_DPAD_UP && position < columns).also {
                    if (it && event.action == KeyEvent.ACTION_DOWN) settings.requestFocus()
                }
            }
        }

        override fun onViewRecycled(holder: Holder) = holder.image.dispose()

        // Волна: картинка и название появляются по диагонали (колонка + ряд), подъём фокуса на самой карточке не мешает
        override fun onViewAttachedToWindow(holder: Holder) {
            val pos = holder.adapterPosition
            val frame = (holder.itemView as ViewGroup)
            for (i in 0 until frame.childCount) {
                val child = frame.getChildAt(i)
                if (isEntering() && pos != RecyclerView.NO_POSITION) {
                    Anim.enter(child, delayMs = (pos % columns + pos / columns) * 60L, dyDp = 30f, durationMs = 380)
                } else Anim.reset(child)
            }
        }

        override fun getItemCount() = items.size
    }

    companion object {
        private const val COLUMNS = 6
        private const val HERO_DELAY_MS = 260L
        private const val BG_ALPHA = 0.35f
        private const val BG_FADE_MS = 600L
        private const val ENTER_WINDOW_MS = 1200L

        fun create(shows: List<Show>) = MainFragment().also { it.shows = shows }
    }
}
