package ru.sharl0t.utvplayer.ui

import android.view.View
import android.view.animation.DecelerateInterpolator

/** Лёгкие анимации на `ViewPropertyAnimator` (альфа и сдвиг): другого приставка Rombica Go не тянет. */
object Anim {
    private val decel = DecelerateInterpolator(1.5f)

    fun dp(view: View, value: Float) = value * view.resources.displayMetrics.density

    /** Появление: из прозрачности и со сдвигом (dx, dy в dp) на место. */
    fun enter(view: View, delayMs: Long = 0, dxDp: Float = 0f, dyDp: Float = 0f, durationMs: Long = 320) {
        view.animate().cancel()
        view.alpha = 0f
        view.translationX = dp(view, dxDp)
        view.translationY = dp(view, dyDp)
        view.animate().alpha(1f).translationX(0f).translationY(0f)
            .setStartDelay(delayMs).setDuration(durationMs).setInterpolator(decel).start()
    }

    /** Вернуть вид в обычное состояние (переиспользованная карточка списка). */
    fun reset(view: View) {
        view.animate().cancel()
        view.alpha = 1f
        view.translationX = 0f
        view.translationY = 0f
    }

    /** Черта растёт слева направо. */
    fun stretch(bar: View, delayMs: Long = 0, durationMs: Long = 320) {
        bar.animate().cancel()
        bar.pivotX = 0f
        bar.scaleX = 0f
        bar.animate().scaleX(1f).setStartDelay(delayMs).setDuration(durationMs).setInterpolator(decel).start()
    }

    /** Картинка уезжает влево и гаснет, после этого вызывается [then] (там меняют содержимое). */
    fun slideOut(view: View, then: () -> Unit) {
        view.animate().cancel()
        view.animate().alpha(0f).translationX(-dp(view, 40f)).setStartDelay(0).setDuration(140)
            .withEndAction { then() }.start()
    }

    /** Картинка приезжает справа на место. */
    fun slideIn(view: View) = enter(view, dxDp = 27f, durationMs = 260)

    /** Подъём и лёгкое увеличение карточки или возврат при потере фокуса. */
    fun lift(view: View, focused: Boolean, liftPx: Float, scale: Float = 1.07f) {
        val s = if (focused) scale else 1f
        // translationZ поднимает выбранную карточку над соседями при перекрытии
        view.animate().translationY(if (focused) -liftPx else 0f).scaleX(s).scaleY(s)
            .translationZ(if (focused) 1f else 0f).setStartDelay(0).setDuration(140).start()
    }
}
