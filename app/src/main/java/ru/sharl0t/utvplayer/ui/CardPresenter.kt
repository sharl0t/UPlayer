package ru.sharl0t.utvplayer.ui

import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import coil.dispose
import coil.load
import ru.sharl0t.utvplayer.R
import ru.sharl0t.utvplayer.data.Episode
import ru.sharl0t.utvplayer.data.Show

/** Карточка передачи или серии. Размер картинки в dp: Coil уменьшает её под карточку, приставке легче. */
class CardPresenter(private val widthDp: Int, private val heightDp: Int) : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val density = parent.resources.displayMetrics.density
        val card = ImageCardView(parent.context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setMainImageDimensions((widthDp * density).toInt(), (heightDp * density).toInt())
            setMainImageScaleType(ImageView.ScaleType.CENTER_CROP)
        }
        return ViewHolder(card)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val card = viewHolder.view as ImageCardView
        val imageUrl = when (item) {
            is Show -> {
                card.titleText = item.title
                card.contentText = null
                item.imageUrl
            }
            is Episode -> {
                card.titleText = item.title
                card.contentText = listOfNotNull(item.duration, item.description).joinToString(" · ")
                item.imageUrl
            }
            else -> null
        }
        val placeholder = ColorDrawable(ContextCompat.getColor(card.context, R.color.brand))
        card.mainImageView.load(imageUrl) {
            placeholder(placeholder)
            error(placeholder)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val card = viewHolder.view as ImageCardView
        card.mainImageView.dispose()
        // Не card.mainImage = null: ImageCardView тогда прячет картинку, и после переиспользования она не видна
        card.mainImageView.setImageDrawable(null)
    }
}
