package ru.sharl0t.utvplayer.ui

import android.graphics.Outline
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.leanback.widget.HorizontalGridView
import androidx.leanback.widget.VerticalGridView
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import coil.load
import ru.sharl0t.utvplayer.R
import ru.sharl0t.utvplayer.data.Episode
import ru.sharl0t.utvplayer.data.ShowDetails

/**
 * Экран передачи: шапка, колонка сезонов слева, блок серии и ряд серий.
 * Здесь только разметка и самое простое поведение; фокус и блок серии доводят задачи 10.11 до 10.13.
 */
class ShowFragment : Fragment(R.layout.fragment_show) {
    private lateinit var details: ShowDetails
    private lateinit var onPlay: (seasonTitle: String, Episode) -> Unit

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val total = details.seasons.sumOf { it.episodes.size }
        view.findViewById<TextView>(R.id.show_title).text = details.title
        view.findViewById<TextView>(R.id.show_meta).text =
            "${plural(details.seasons.size, "сезон", "сезона", "сезонов")} · ${plural(total, "серия", "серии", "серий")}"

        val seasons = view.findViewById<VerticalGridView>(R.id.seasons)
        val episodes = view.findViewById<HorizontalGridView>(R.id.episodes)
        var current = 0
        val episodeAdapter = EpisodeAdapter(
            onFocus = { showEpisode(view, details.seasons[current].title, it) },
            onClick = { onPlay(details.seasons[current].title, it) },
        )
        episodes.adapter = episodeAdapter
        episodes.setItemSpacing(0)
        val select: (Int) -> Unit = { index ->
            current = index
            val season = details.seasons[index]
            episodeAdapter.submit(season.episodes)
            season.episodes.firstOrNull()?.let { showEpisode(view, season.title, it) }
        }
        seasons.adapter = SeasonAdapter(details, select)
        select(0)
        seasons.post { seasons.requestFocus() }
    }

    private fun showEpisode(view: View, season: String, ep: Episode) {
        view.findViewById<ImageView>(R.id.ep_image).load(ep.imageUrl)
        view.findViewById<TextView>(R.id.ep_num).text = ep.number?.let { "$it серия" } ?: ep.title
        val who = if (ep.heroine != null) listOfNotNull(ep.heroine, ep.city).joinToString(", ") else ep.description
        view.findViewById<TextView>(R.id.ep_who).text = who.orEmpty()
        view.findViewById<TextView>(R.id.ep_meta).text = listOfNotNull(season, ep.duration).joinToString(" · ")
    }

    private fun plural(n: Int, one: String, few: String, many: String): String {
        val word = when {
            n % 100 in 11..14 -> many
            n % 10 == 1 -> one
            n % 10 in 2..4 -> few
            else -> many
        }
        return "$n $word"
    }

    private class SeasonAdapter(
        private val details: ShowDetails,
        private val onFocus: (Int) -> Unit,
    ) : RecyclerView.Adapter<SeasonAdapter.Holder>() {
        private var selected = 0
        private var list: RecyclerView? = null

        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            list = recyclerView
        }

        // Выбранный сезон: последний, на котором был фокус; activated рисует черту и подсветку
        private fun choose(position: Int) {
            selected = position
            val rv = list ?: return
            for (i in 0 until rv.childCount) {
                val h = rv.getChildViewHolder(rv.getChildAt(i))
                h.itemView.isActivated = h.adapterPosition == position
            }
            onFocus(position)
        }

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.season_name)
            val count: TextView = view.findViewById(R.id.season_count)

            init {
                view.setOnFocusChangeListener { _, focused ->
                    if (focused && adapterPosition != RecyclerView.NO_POSITION) choose(adapterPosition)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_season, parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val season = details.seasons[position]
            holder.name.text = season.title
            holder.count.text = season.episodes.size.toString()
            holder.itemView.isActivated = position == selected
        }

        override fun getItemCount() = details.seasons.size
    }

    private class EpisodeAdapter(
        private val onFocus: (Episode) -> Unit,
        private val onClick: (Episode) -> Unit,
    ) : RecyclerView.Adapter<EpisodeAdapter.Holder>() {
        private var items: List<Episode> = emptyList()

        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewById(R.id.ep_card_image)
            val name: TextView = view.findViewById(R.id.ep_card_name)
            val duration: TextView = view.findViewById(R.id.ep_card_duration)

            init {
                val radius = view.resources.getDimension(R.dimen.card_radius)
                image.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(v: View, outline: Outline) =
                        outline.setRoundRect(0, 0, v.width, v.height, radius)
                }
                image.clipToOutline = true
            }
        }

        fun submit(list: List<Episode>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_episode, parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val ep = items[position]
            holder.name.text = ep.title
            holder.duration.text = ep.duration.orEmpty()
            holder.duration.visibility = if (ep.duration.isNullOrEmpty()) View.GONE else View.VISIBLE
            holder.image.load(ep.imageUrl)
            // С первой серии влево фокус уходит на колонку сезонов
            holder.itemView.setOnKeyListener { v, code, event ->
                val first = holder.adapterPosition == 0
                if (first && code == KeyEvent.KEYCODE_DPAD_LEFT && event.action == KeyEvent.ACTION_DOWN) {
                    (v.rootView.findViewById<View>(R.id.seasons)).requestFocus()
                } else false
            }
            holder.itemView.setOnClickListener { onClick(ep) }
            holder.itemView.setOnFocusChangeListener { _, focused -> if (focused) onFocus(ep) }
        }

        override fun onViewRecycled(holder: Holder) = holder.image.dispose()

        override fun getItemCount() = items.size
    }

    companion object {
        fun create(details: ShowDetails, onPlay: (seasonTitle: String, Episode) -> Unit) =
            ShowFragment().also {
                it.details = details
                it.onPlay = onPlay
            }
    }
}
