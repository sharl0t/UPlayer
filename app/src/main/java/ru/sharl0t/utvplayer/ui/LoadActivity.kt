package ru.sharl0t.utvplayer.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.sharl0t.utvplayer.R

/** Экран, который грузит данные с сайта: крутилка, затем содержимое или ошибка с «Повторить». */
abstract class LoadActivity<T> : FragmentActivity() {
    private lateinit var progress: View
    private var job: Job? = null

    protected abstract suspend fun load(): T

    protected abstract fun contentFragment(data: T): Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        // Состояние фрагментов не восстанавливаем: после пересоздания экран просто загружается заново
        super.onCreate(null)
        setContentView(R.layout.activity_content)
        progress = findViewById(R.id.progress)
        reload()
    }

    fun reload() {
        job?.cancel()
        supportFragmentManager.findFragmentById(R.id.content)?.let { supportFragmentManager.commit { remove(it) } }
        progress.visibility = View.VISIBLE
        job = lifecycleScope.launch {
            val fragment = try {
                contentFragment(load())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MessageFragment.error(getString(R.string.load_error, e.message ?: e.javaClass.simpleName))
            }
            // Фрагмент можно ставить только на видимый экран, иначе транзакция падает после onSaveInstanceState
            withStarted {
                progress.visibility = View.GONE
                supportFragmentManager.commit { replace(R.id.content, fragment) }
            }
        }
    }
}
