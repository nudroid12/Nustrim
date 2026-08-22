package app.nudroidlabs.nustrim.tv.search

import android.content.Context
import org.json.JSONArray

class TvSearchHistoryStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "nustrim_tv_search",
        Context.MODE_PRIVATE,
    )

    fun recent(): List<String> {
        val raw = preferences.getString(KEY_RECENT, "[]").orEmpty()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    array.optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
                }
            }
        }.getOrDefault(emptyList()).take(MAX_RECENT)
    }

    fun remember(query: String) {
        val clean = query.trim()
        if (clean.length < MIN_SEARCH_QUERY_LENGTH) return
        val updated = buildList {
            add(clean)
            recent().filterNot { it.equals(clean, ignoreCase = true) }.forEach(::add)
        }.take(MAX_RECENT)
        val array = JSONArray().apply { updated.forEach(::put) }
        preferences.edit().putString(KEY_RECENT, array.toString()).apply()
    }

    fun clear() {
        preferences.edit().remove(KEY_RECENT).apply()
    }

    private companion object {
        const val KEY_RECENT = "recent_queries"
        const val MAX_RECENT = 8
    }
}
