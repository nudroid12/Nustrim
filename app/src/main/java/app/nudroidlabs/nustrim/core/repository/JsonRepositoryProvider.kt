package app.nudroidlabs.nustrim.core.repository

import android.os.Handler
import android.os.Looper
import app.nudroidlabs.nustrim.core.model.MediaCatalog
import app.nudroidlabs.nustrim.core.provider.MediaProvider
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.Executors

class JsonRepositoryProvider : MediaProvider {
    override val id: String = "json-repository"
    override val displayName: String = "JSON Repository"

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun loadCatalog(
        source: String,
        onSuccess: (MediaCatalog) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        executor.execute {
            try {
                validateUrl(source)
                val connection = URL(source).openConnection() as HttpURLConnection
                connection.connectTimeout = 12_000
                connection.readTimeout = 18_000
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("Accept", "application/json,text/plain,*/*")
                connection.setRequestProperty("User-Agent", "Nustrim/0.3")

                try {
                    val status = connection.responseCode
                    if (status !in 200..299) {
                        throw IllegalStateException("Repository request failed with HTTP $status")
                    }

                    val text = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                    val catalog = RepositoryParser.parse(text)
                    mainHandler.post { onSuccess(catalog) }
                } finally {
                    connection.disconnect()
                }
            } catch (t: Throwable) {
                mainHandler.post { onError(t) }
            }
        }
    }

    private fun validateUrl(source: String) {
        val uri = URI(source.trim())
        val scheme = uri.scheme?.lowercase()
        require(scheme == "https" || scheme == "http") {
            "Repository URL must use http or https"
        }
        require(!uri.host.isNullOrBlank()) { "Repository URL is invalid" }
    }
}
