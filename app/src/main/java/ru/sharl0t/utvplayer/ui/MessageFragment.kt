package ru.sharl0t.utvplayer.ui

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.leanback.app.ErrorSupportFragment
import ru.sharl0t.utvplayer.R

/** Сообщение на весь экран с одной кнопкой: «Повторить» загрузку или «Назад». Фокус сразу на кнопке. */
class MessageFragment : ErrorSupportFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val retry = requireArguments().getBoolean(ARG_RETRY)
        message = requireArguments().getString(ARG_MESSAGE)
        buttonText = getString(if (retry) R.string.retry else R.string.back)
        setDefaultBackground(false)
        backgroundDrawable = ColorDrawable(ContextCompat.getColor(requireContext(), R.color.brand))
        setButtonClickListener {
            val activity = requireActivity()
            if (retry && activity is LoadActivity<*>) activity.reload() else activity.finish()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Кнопка в новом фокусе: светлая плашка, тёмный текст
        view.findViewById<Button>(androidx.leanback.R.id.button)?.apply {
            background = ContextCompat.getDrawable(context, R.drawable.focus_plate_selector)
            setTextColor(ContextCompat.getColorStateList(context, R.color.text_focus_selector))
        }
    }

    companion object {
        private const val ARG_MESSAGE = "message"
        private const val ARG_RETRY = "retry"

        fun error(message: String) = create(message, retry = true)

        fun info(message: String) = create(message, retry = false)

        private fun create(message: String, retry: Boolean) = MessageFragment().apply {
            arguments = bundleOf(ARG_MESSAGE to message, ARG_RETRY to retry)
        }
    }
}
