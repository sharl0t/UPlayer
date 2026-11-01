package ru.sharl0t.utvplayer.ui

import android.os.Bundle
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
        setDefaultBackground(true)
        setButtonClickListener {
            val activity = requireActivity()
            if (retry && activity is LoadActivity<*>) activity.reload() else activity.finish()
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
