package ru.sharl0t.utvplayer.data

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import ru.sharl0t.utvplayer.R
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/** Общий HTTP-клиент для страниц, картинок и видео. */
object Net {
    private const val USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36"

    lateinit var client: OkHttpClient
        private set

    fun init(context: Context) = context.resources.openRawResource(R.raw.extra_roots).use { init(it) }

    /** [extraRoots]: PEM с дополнительными корнями; отдельно от Context, чтобы проверять на JVM. */
    fun init(extraRoots: InputStream) {
        val trustManager = trustManager(extraRoots)
        val sslContext = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trustManager), null) }
        client = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .cookieJar(MemoryCookieJar())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
            }
            .build()
    }

    /**
     * Сайт подписан GlobalSign Root R46, видео-CDN ведёт к ISRG Root X2. Этих корней нет в старых Android,
     * поэтому к системным сертификатам добавляем свои из res/raw/extra_roots.pem.
     */
    private fun trustManager(extraRoots: InputStream): X509TrustManager {
        val system = defaultTrustManager(null)
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null, null) }
        CertificateFactory.getInstance("X.509").generateCertificates(extraRoots).forEachIndexed { i, cert ->
            keyStore.setCertificateEntry("extra$i", cert)
        }
        val extra = defaultTrustManager(keyStore)
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) =
                system.checkClientTrusted(chain, authType)

            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                try {
                    system.checkServerTrusted(chain, authType)
                } catch (e: CertificateException) {
                    extra.checkServerTrusted(chain, authType)
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> =
                system.acceptedIssuers + extra.acceptedIssuers
        }
    }

    private fun defaultTrustManager(keyStore: KeyStore?): X509TrustManager {
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(keyStore)
        return factory.trustManagers.filterIsInstance<X509TrustManager>().first()
    }

    /** Cookie нужны только для CSRF-токена списка передач, хранить их между запусками незачем. */
    private class MemoryCookieJar : CookieJar {
        private val cookies = mutableMapOf<String, Cookie>()

        @Synchronized
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookies.forEach { this.cookies["${it.domain}|${it.name}"] = it }
        }

        @Synchronized
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val now = System.currentTimeMillis()
            cookies.values.removeAll { it.expiresAt < now }
            return cookies.values.filter { it.matches(url) }
        }
    }
}
