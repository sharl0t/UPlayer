package ru.sharl0t.utvplayer.ui

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.count).text = plural(shows.size)
        val grid = view.findViewById<VerticalGridView>(R.id.grid)
        val settings = view.findViewById<View>(R.id.settings)
        settings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
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
        // Верхний блок обновляется через 260 мс после фокуса: при быстрой прокрутке лишние картинки не грузятся
        val onFocus: (Int) -> Unit = { position ->
            heroTask?.let { handler.removeCallbacks(it) }
            val task = Runnable {
                val show = shows[position]
                heroImage.load(show.imageUrl)
                heroTitle.text = show.title
                heroBar.visibility = View.VISIBLE
                heroPos.text = "${position + 1} из ${shows.size}"
            }
            heroTask = task
            handler.postDelayed(task, HERO_DELAY_MS)
        }
        grid.adapter = ShowAdapter(shows, COLUMNS, settings, onFocus) {
            startActivity(ShowActivity.intent(requireContext(), it))
        }
        grid.post { grid.requestFocus() }
    }

    override fun onDestroyView() {
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
                    v.translationY = if (focused) -lift else 0f
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

        override fun getItemCount() = items.size
    }

    companion object {
        private const val COLUMNS = 6
        private const val HERO_DELAY_MS = 260L

        fun create(shows: List<Show>) = MainFragment().also { it.shows = shows }
    }
}
