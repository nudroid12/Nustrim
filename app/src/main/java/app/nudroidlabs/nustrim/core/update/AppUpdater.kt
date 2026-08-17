package app.nudroidlabs.nustrim.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.FileProvider
import app.nudroidlabs.nustrim.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class AppUpdater(private val context: Context) {
    suspend fun check(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val manifest = readText(UPDATE_MANIFEST_URL)
            val root = JSONObject(manifest)
            val info = UpdateInfo(
                versionCode = root.getInt("versionCode"),
                versionName = root.getString("versionName"),
                apkUrl = root.getString("apkUrl"),
                sha256 = root.getString("sha256").lowercase(),
                changelog = root.optString("changelog"),
                sizeBytes = root.optLong("sizeBytes", -1L),
                signingMode = root.optString("signingMode", "unknown")
            )
            info.takeIf { it.versionCode > BuildConfig.VERSION_CODE }
        }
    }

    suspend fun download(
        info: UpdateInfo,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val directory = File(context.cacheDir, "updates").apply { mkdirs() }
            val target = File(directory, "Nustrim-${info.versionName}.apk")
            val connection = open(info.apkUrl)
            val responseLength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      connection.contentLengthLong
  } else {
      connection.contentLength.toLong()
  }
  val total = responseLength.takeIf { it > 0L } ?: info.sizeBytes
            val mainHandler = Handler(Looper.getMainLooper())
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var copied = 0L
                    var lastProgress = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        if (total > 0L) {
                            val progress = ((copied * 100L) / total).toInt().coerceIn(0, 100)
                            if (progress != lastProgress) {
                                lastProgress = progress
                                mainHandler.post { onProgress(progress) }
                            }
                        }
                    }
                }
            }
            connection.disconnect()
            val digest = sha256(target)
            require(digest.equals(info.sha256, ignoreCase = true)) {
                "Update verification failed. Expected ${info.sha256.take(12)}…, got ${digest.take(12)}…"
            }
            Handler(Looper.getMainLooper()).post { onProgress(100) }
            target
        }
    }

    fun canRequestPackageInstall(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun install(file: File): Result<Unit> = runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.updateprovider",
            file
        )
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    private fun readText(url: String): String {
        val connection = open(url)
        return connection.inputStream.bufferedReader().use { it.readText() }.also { connection.disconnect() }
    }

    private fun open(url: String): HttpURLConnection {
        var current = url
        repeat(6) {
            val connection = (URL(current).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 60_000
                instanceFollowRedirects = false
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json, application/octet-stream")
                setRequestProperty("User-Agent", "Nustrim/${BuildConfig.VERSION_NAME}")
            }
            val code = connection.responseCode
            if (code in 300..399) {
                val location = connection.getHeaderField("Location")
                    ?: error("Update server redirected without a location")
                connection.disconnect()
                current = URL(URL(current), location).toString()
            } else {
                require(code in 200..299) { "Update server returned HTTP $code" }
                return connection
            }
        }
        error("Too many update redirects")
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val UPDATE_MANIFEST_URL =
            "https://github.com/nudroid12/Nustrim/releases/latest/download/update.json"
    }
}

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val changelog: String,
    val sizeBytes: Long,
    val signingMode: String
)
