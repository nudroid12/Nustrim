package app.nudroidlabs.nustrim.tv.search

import android.content.Context

internal class TvSearchHistoryStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "nustrim_tv_search_history",
        Context.MODE_PRIVATE
    )

    fun items(): List<String> = preferences
        .getString(KEY_HISTORY, "")
        .orEmpty()
        .split(SEPARATOR)
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
        .take(MAX_ITEMS)

    fun add(query: String) {
        val clean = sanitize(query)
        if (clean.isBlank()) return
        val updated = buildList {
            add(clean)
            addAll(items().filterNot { it.equals(clean, ignoreCase = true) })
        }.take(MAX_ITEMS)
        preferences.edit().putString(KEY_HISTORY, updated.joinToString(SEPARATOR)).apply()
    }

    fun clear() {
        preferences.edit().remove(KEY_HISTORY).apply()
    }

    private fun sanitize(value: String): String = value
        .replace(SEPARATOR, " ")
        .replace('\n', ' ')
        .trim()

    private companion object {
        const val KEY_HISTORY = "recent_queries"
        const val SEPARATOR = "\u001F"
        const val MAX_ITEMS = 8
    }
}
