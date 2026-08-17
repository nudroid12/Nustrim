package app.nudroidlabs.nustrim.core.source

import android.content.Context
import java.security.MessageDigest

data class SourceHealth(
    val consecutiveFailures: Int = 0,
    val lastSuccessMs: Long = 0L,
    val lastFailureMs: Long = 0L,
    val lastError: String = ""
) {
    val statusLabel: String
        get() = when {
            consecutiveFailures > 0 -> "Failing · $consecutiveFailures consecutive"
            lastSuccessMs > 0L -> "Healthy"
            else -> "Not checked"
        }
}

class SourceHealthStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "nustrim_source_health",
        Context.MODE_PRIVATE
    )

    fun status(url: String): SourceHealth {
        val key = key(url)
        return SourceHealth(
            consecutiveFailures = preferences.getInt("${key}_failures", 0),
            lastSuccessMs = preferences.getLong("${key}_success", 0L),
            lastFailureMs = preferences.getLong("${key}_failure", 0L),
            lastError = preferences.getString("${key}_error", "").orEmpty()
        )
    }

    fun recordSuccess(url: String) {
        val key = key(url)
        preferences.edit()
            .putInt("${key}_failures", 0)
            .putLong("${key}_success", System.currentTimeMillis())
            .putString("${key}_error", "")
            .apply()
    }

    fun recordFailure(url: String, throwable: Throwable) {
        val key = key(url)
        val failures = preferences.getInt("${key}_failures", 0) + 1
        preferences.edit()
            .putInt("${key}_failures", failures)
            .putLong("${key}_failure", System.currentTimeMillis())
            .putString("${key}_error", throwable.message ?: throwable.javaClass.simpleName)
            .apply()
    }

    fun clear(url: String) {
        val key = key(url)
        preferences.edit()
            .remove("${key}_failures")
            .remove("${key}_success")
            .remove("${key}_failure")
            .remove("${key}_error")
            .apply()
    }

    private fun key(url: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(url.trim().toByteArray())
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
            .take(24)
}
