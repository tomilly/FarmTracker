package pl.farmtracker.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/** Proste pobranie tekstu po HTTP GET. Wydzielone, żeby repozytoria dało się testować bez sieci. */
fun interface HttpGet {
    /** @throws IOException przy braku sieci, timeoucie albo kodzie innym niż 2xx. */
    suspend fun get(url: String): String
}

internal class UrlConnectionHttpGet @Inject constructor() : HttpGet {

    override suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("HTTP $code dla $url")
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 20_000
        const val USER_AGENT = "FarmTracker (Android)"
    }
}
