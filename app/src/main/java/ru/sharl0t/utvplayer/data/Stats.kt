package ru.sharl0t.utvplayer.data

import android.content.Context
import android.os.Build
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.UUID

/** Обезличенная статистика устройств: раз в сутки один фоновый GET на irlme.online (см. README). */
object Stats {
    private const val PREFS = "stats"
    private const val KEY_ID = "id"
    private const val KEY_LAST = "last_ping"
    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun pingIfDue(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val last = prefs.getLong(KEY_LAST, 0)
        if (now - last in 0 until DAY_MS) return

        val id = prefs.getString(KEY_ID, null)
            ?: UUID.randomUUID().toString().also { prefs.edit().putString(KEY_ID, it).apply() }
        val version = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        } catch (e: Exception) {
            ""
        }
        val url = "https://irlme.online/ping".toHttpUrl().newBuilder()
            .addQueryParameter("id", id)
            .addQueryParameter("m", Build.MANUFACTURER)
            .addQueryParameter("d", Build.MODEL)
            .addQueryParameter("a", Build.VERSION.SDK_INT.toString())
            .addQueryParameter("abi", Build.SUPPORTED_ABIS.firstOrNull().orEmpty())
            .addQueryParameter("v", version)
            .build()

        // Время пишем только после успешного ответа: без сети попробуем при следующем запуске.
        Net.client.newCall(Request.Builder().url(url).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = Unit

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) prefs.edit().putLong(KEY_LAST, now).apply()
                }
            }
        })
    }
}
