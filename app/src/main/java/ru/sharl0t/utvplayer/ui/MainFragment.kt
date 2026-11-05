package ru.sharl0t.utvplayer.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.leanback.widget.VerticalGridView
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import coil.load
import ru.sharl0t.utvplayer.R
import ru.sharl0t.utvplayer.data.Show

/** Главный экран: шапка, верхний блок и сетка передач на 6 колонок. */
class MainFragment : Fragment(R.layout.fragment_main) {
    private lateinit var shows: List<Show>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.count).text = plural(shows.size)
        view.findViewById<View>(R.id.settings).setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
        val grid = view.findViewById<VerticalGridView>(R.id.grid)
        val gap = (27 * resources.displayMetrics.density).toInt()
        grid.setHorizontalSpacing(gap)
        grid.setVerticalSpacing(gap)
        grid.adapter = ShowAdapter(shows) { startActivity(ShowActivity.intent(requireContext(), it)) }
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
        private val onClick: (Show) -> Unit,
    ) : RecyclerView.Adapter<ShowAdapter.Holder>() {
        class Holder(view: View) : RecyclerView.ViewHolder(view) {
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
            Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_show, parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val show = items[position]
            holder.name.text = show.title
            holder.image.load(show.imageUrl)
            holder.itemView.setOnClickListener { onClick(show) }
        }

        override fun onViewRecycled(holder: Holder) = holder.image.dispose()

        override fun getItemCount() = items.size
    }

    companion object {
        fun create(shows: List<Show>) = MainFragment().also { it.shows = shows }
    }
}
