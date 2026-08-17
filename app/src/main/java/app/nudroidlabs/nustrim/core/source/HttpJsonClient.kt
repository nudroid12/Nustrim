package app.nudroidlabs.nustrim.core.source

import android.os.Handler
import android.os.Looper
import app.nudroidlabs.nustrim.BuildConfig
import app.nudroidlabs.nustrim.core.diagnostics.NustrimDiagnostics
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HttpJsonClient(
    private val executor: ExecutorService = SHARED_EXECUTOR
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun getText(
        url: String,
        onSuccess: (String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        executor.execute {
            try {
                val text = getTextBlocking(url)
                mainHandler.post { onSuccess(text) }
            } catch (t: Throwable) {
                NustrimDiagnostics.error("HTTP_ERROR", t, url)
                mainHandler.post { onError(t) }
            }
        }
    }

    fun run(
        block: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        executor.execute {
            try {
                block()
            } catch (t: Throwable) {
                NustrimDiagnostics.error("ASYNC_ERROR", t)
                mainHandler.post { onError(t) }
            }
        }
    }

    fun postToMain(block: () -> Unit) {
        mainHandler.post(block)
    }

    fun getTextBlocking(url: String): String {
        validateUrl(url)
        val started = System.nanoTime()
        NustrimDiagnostics.log("HTTP_GET", url)
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 12_000
        connection.readTimeout = 20_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("Accept", "application/json,text/plain,*/*")
        connection.setRequestProperty("User-Agent", "Nustrim/${BuildConfig.VERSION_NAME}")
        try {
            val status = connection.responseCode
            val elapsedMs = (System.nanoTime() - started) / 1_000_000
            NustrimDiagnostics.log("HTTP_RESPONSE", "status=$status ms=$elapsedMs url=$url")
            if (status !in 200..299) {
                throw IllegalStateException("Request failed with HTTP $status: $url")
            }
            return BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun validateUrl(source: String) {
        val uri = URI(source.trim())
        val scheme = uri.scheme?.lowercase()
        require(scheme == "https" || scheme == "http") { "Source URL must use http or https" }
        require(!uri.host.isNullOrBlank()) { "Source URL is invalid" }
    }

    private companion object {
        // SourceEngine instances are created by several screens. Sharing a bounded
        // network pool avoids creating a new unbounded cached thread pool per screen.
        val SHARED_EXECUTOR: ExecutorService = Executors.newFixedThreadPool(8)
    }
}
