package app.nudroidlabs.nustrim.tv.library

import android.content.Context

internal enum class TvLibraryTypeFilter(
    val displayLabel: String
) {
    ALL("All"),
    MOVIES("Movies"),
    SERIES("Series")
}

internal enum class TvLibraryStateFilter(
    val displayLabel: String
) {
    ALL("All"),
    CONTINUE("Continue"),
    WATCHED("Watched"),
    UNWATCHED("Unwatched")
}

internal enum class TvLibrarySortMode(
    val displayLabel: String
) {
    RECENT("Recent"),
    TITLE("A-Z"),
    YEAR("Year")
}

internal class TvLibraryPreferences(
    context: Context
) {
    private val preferences = context.applicationContext
        .getSharedPreferences(
            "nustrim_tv_library",
            Context.MODE_PRIVATE
        )

    fun readTypeFilter(): TvLibraryTypeFilter =
        enumValueOrDefault(
            preferences.getString(KEY_TYPE_FILTER, null),
            TvLibraryTypeFilter.ALL
        )

    fun readStateFilter(): TvLibraryStateFilter =
        enumValueOrDefault(
            preferences.getString(KEY_STATE_FILTER, null),
            TvLibraryStateFilter.ALL
        )

    fun readSortMode(): TvLibrarySortMode =
        enumValueOrDefault(
            preferences.getString(KEY_SORT_MODE, null),
            TvLibrarySortMode.RECENT
        )

    fun write(
        typeFilter: TvLibraryTypeFilter,
        stateFilter: TvLibraryStateFilter,
        sortMode: TvLibrarySortMode
    ) {
        preferences.edit()
            .putString(KEY_TYPE_FILTER, typeFilter.name)
            .putString(KEY_STATE_FILTER, stateFilter.name)
            .putString(KEY_SORT_MODE, sortMode.name)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(
        raw: String?,
        fallback: T
    ): T {
        return enumValues<T>()
            .firstOrNull { it.name == raw }
            ?: fallback
    }

    private companion object {
        const val KEY_TYPE_FILTER = "type_filter"
        const val KEY_STATE_FILTER = "state_filter"
        const val KEY_SORT_MODE = "sort_mode"
    }
}
