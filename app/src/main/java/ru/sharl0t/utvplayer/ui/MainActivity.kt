package ru.sharl0t.utvplayer.ui

import androidx.fragment.app.Fragment
import ru.sharl0t.utvplayer.data.Show
import ru.sharl0t.utvplayer.data.UtvApi

/** Каталог: свой экран `MainFragment`, загрузка и ошибка из `LoadActivity`. */
class MainActivity : LoadActivity<List<Show>>() {
    override suspend fun load() = UtvApi.shows()

    override fun contentFragment(data: List<Show>): Fragment = MainFragment.create(data)
}
