package ru.sharl0t.utvplayer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import ru.sharl0t.utvplayer.data.Net
import ru.sharl0t.utvplayer.data.Stats

class App : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        Net.init(this)
        Stats.pingIfDue(this)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient { Net.client }
            .crossfade(true)
            .build()
}
