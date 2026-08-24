package app.nudroidlabs.nustrim.tv.sources

import android.content.Context

/** Remembers providers that produced playable links so they can run first next time. */
class TvCloudStreamPerformanceStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "nustrim_tv_cloudstream_performance",
        Context.MODE_PRIVATE,
    )

    fun score(repositoryUrl: String, providerId: String): Int =
        preferences.getInt(key(repositoryUrl, providerId), 0)

    fun recordSuccess(repositoryUrl: String, providerId: String) {
        val key = key(repositoryUrl, providerId)
        preferences.edit().putInt(key, (preferences.getInt(key, 0) + 1).coerceAtMost(100)).apply()
    }

    private fun key(repositoryUrl: String, providerId: String): String =
        "${repositoryUrl.hashCode().toString(16)}:${providerId.hashCode().toString(16)}"
}
