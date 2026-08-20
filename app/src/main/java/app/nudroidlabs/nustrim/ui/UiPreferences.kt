package app.nudroidlabs.nustrim.ui

import android.content.Context

enum class InterfaceMode {
    MOBILE,
    TV
}

enum class SubtitleDisplayMode {
    PREFERRED_ONLY,
    SHOW_ALL
}

class UiPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("nustrim_ui", Context.MODE_PRIVATE)

    var interfaceMode: InterfaceMode?
        get() = preferences.getString(KEY_INTERFACE_MODE, null)
            ?.let { stored -> runCatching { InterfaceMode.valueOf(stored) }.getOrNull() }
        set(value) {
            if (value == null) {
                preferences.edit().remove(KEY_INTERFACE_MODE).commit()
            } else {
                preferences.edit().putString(KEY_INTERFACE_MODE, value.name).commit()
            }
        }

    var interfaceModeConfirmed: Boolean
        get() = preferences.getBoolean(KEY_INTERFACE_MODE_CONFIRMED, false)
        set(value) = preferences.edit().putBoolean(KEY_INTERFACE_MODE_CONFIRMED, value).apply()

    var remoteTestEnabled: Boolean
        get() = preferences.getBoolean(KEY_REMOTE_TEST_ENABLED, false)
        set(value) = preferences.edit().putBoolean(KEY_REMOTE_TEST_ENABLED, value).apply()

    var developerMode: Boolean
        get() = preferences.getBoolean(KEY_DEVELOPER_MODE, false)
        set(value) = preferences.edit().putBoolean(KEY_DEVELOPER_MODE, value).apply()

    var developerDiagnostics: Boolean
        get() = preferences.getBoolean(KEY_DEVELOPER_DIAGNOSTICS, false)
        set(value) = preferences.edit().putBoolean(KEY_DEVELOPER_DIAGNOSTICS, value).apply()

    var autoplayFirstSource: Boolean
        get() = preferences.getBoolean(KEY_AUTOPLAY_FIRST_SOURCE, false)
        set(value) = preferences.edit().putBoolean(KEY_AUTOPLAY_FIRST_SOURCE, value).apply()

    var autoplayNextEpisode: Boolean
        get() = preferences.getBoolean(KEY_AUTOPLAY_NEXT_EPISODE, true)
        set(value) = preferences.edit().putBoolean(KEY_AUTOPLAY_NEXT_EPISODE, value).apply()

    var subtitlePreferredLanguage: String
        get() = preferences.getString(KEY_SUBTITLE_PREFERRED_LANGUAGE, "ms").orEmpty().ifBlank { "ms" }
        set(value) = preferences.edit().putString(KEY_SUBTITLE_PREFERRED_LANGUAGE, value.trim()).apply()

    var subtitleSecondPreferredLanguage: String
        get() = preferences.getString(KEY_SUBTITLE_SECOND_PREFERRED_LANGUAGE, "en").orEmpty().ifBlank { "en" }
        set(value) = preferences.edit().putString(KEY_SUBTITLE_SECOND_PREFERRED_LANGUAGE, value.trim()).apply()

    var subtitleDisplayMode: SubtitleDisplayMode
        get() = preferences.getString(KEY_SUBTITLE_DISPLAY_MODE, SubtitleDisplayMode.SHOW_ALL.name)
            ?.let { stored -> runCatching { SubtitleDisplayMode.valueOf(stored) }.getOrNull() }
            ?: SubtitleDisplayMode.SHOW_ALL
        set(value) = preferences.edit().putString(KEY_SUBTITLE_DISPLAY_MODE, value.name).apply()

    var catalogOrder: List<String>
        get() = preferences.getString(KEY_CATALOG_ORDER, "")
            .orEmpty()
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .toList()
        set(value) = preferences.edit().putString(KEY_CATALOG_ORDER, value.distinct().joinToString("\n")).apply()

    val hiddenCatalogKeys: Set<String>
        get() = preferences.getStringSet(KEY_HIDDEN_CATALOGS, emptySet()).orEmpty().toSet()

    fun setCatalogHidden(key: String, hidden: Boolean) {
        val keys = hiddenCatalogKeys.toMutableSet()
        if (hidden) keys += key else keys -= key
        preferences.edit().putStringSet(KEY_HIDDEN_CATALOGS, keys).apply()
    }

    fun resetCatalogLayout() {
        preferences.edit()
            .remove(KEY_CATALOG_ORDER)
            .remove(KEY_HIDDEN_CATALOGS)
            .apply()
    }

    var tmdbApiKey: String
        get() = preferences.getString(KEY_TMDB_API_KEY, "").orEmpty()
        set(value) = preferences.edit().putString(KEY_TMDB_API_KEY, value.trim()).apply()

    var tmdbEnrichmentEnabled: Boolean
        get() = preferences.getBoolean(KEY_TMDB_ENRICHMENT_ENABLED, false)
        set(value) = preferences.edit().putBoolean(KEY_TMDB_ENRICHMENT_ENABLED, value).apply()

    var mdbListApiKey: String
        get() = preferences.getString(KEY_MDBLIST_API_KEY, "").orEmpty()
        set(value) = preferences.edit().putString(KEY_MDBLIST_API_KEY, value.trim()).apply()

    var mdbListRatingsEnabled: Boolean
        get() = preferences.getBoolean(KEY_MDBLIST_RATINGS_ENABLED, false)
        set(value) = preferences.edit().putBoolean(KEY_MDBLIST_RATINGS_ENABLED, value).apply()

    fun isMdbListProviderEnabled(providerId: String): Boolean {
        return preferences.getStringSet(KEY_MDBLIST_PROVIDERS, DEFAULT_MDBLIST_PROVIDERS)
            ?.contains(providerId)
            ?: DEFAULT_MDBLIST_PROVIDERS.contains(providerId)
    }

    fun setMdbListProviderEnabled(providerId: String, enabled: Boolean) {
        val providers = preferences
            .getStringSet(KEY_MDBLIST_PROVIDERS, DEFAULT_MDBLIST_PROVIDERS)
            .orEmpty()
            .toMutableSet()

        if (enabled) providers += providerId else providers -= providerId
        preferences.edit().putStringSet(KEY_MDBLIST_PROVIDERS, providers).apply()
    }

    var traktClientId: String
        get() = preferences.getString(KEY_TRAKT_CLIENT_ID, "").orEmpty()
        set(value) = preferences.edit().putString(KEY_TRAKT_CLIENT_ID, value.trim()).apply()

    var traktClientSecret: String
        get() = preferences.getString(KEY_TRAKT_CLIENT_SECRET, "").orEmpty()
        set(value) = preferences.edit().putString(KEY_TRAKT_CLIENT_SECRET, value.trim()).apply()

    var traktAccessToken: String
        get() = preferences.getString(KEY_TRAKT_ACCESS_TOKEN, "").orEmpty()
        set(value) = preferences.edit().putString(KEY_TRAKT_ACCESS_TOKEN, value.trim()).apply()

    var traktRefreshToken: String
        get() = preferences.getString(KEY_TRAKT_REFRESH_TOKEN, "").orEmpty()
        set(value) = preferences.edit().putString(KEY_TRAKT_REFRESH_TOKEN, value.trim()).apply()

    var traktExpiresAtSeconds: Long
        get() = preferences.getLong(KEY_TRAKT_EXPIRES_AT, 0L)
        set(value) = preferences.edit().putLong(KEY_TRAKT_EXPIRES_AT, value).apply()

    var traktUsername: String
        get() = preferences.getString(KEY_TRAKT_USERNAME, "").orEmpty()
        set(value) = preferences.edit().putString(KEY_TRAKT_USERNAME, value.trim()).apply()

    val traktConnected: Boolean
        get() = traktAccessToken.isNotBlank() && traktClientId.isNotBlank()

    fun saveTraktToken(accessToken: String, refreshToken: String, expiresAtSeconds: Long) {
        preferences.edit()
            .putString(KEY_TRAKT_ACCESS_TOKEN, accessToken.trim())
            .putString(KEY_TRAKT_REFRESH_TOKEN, refreshToken.trim())
            .putLong(KEY_TRAKT_EXPIRES_AT, expiresAtSeconds)
            .apply()
    }

    fun clearTraktConnection() {
        preferences.edit()
            .remove(KEY_TRAKT_ACCESS_TOKEN)
            .remove(KEY_TRAKT_REFRESH_TOKEN)
            .remove(KEY_TRAKT_EXPIRES_AT)
            .remove(KEY_TRAKT_USERNAME)
            .apply()
    }

    companion object {
        private const val KEY_INTERFACE_MODE = "interface_mode"
        private const val KEY_INTERFACE_MODE_CONFIRMED = "interface_mode_confirmed_v2"
        private const val KEY_REMOTE_TEST_ENABLED = "remote_test_enabled"
        private const val KEY_DEVELOPER_MODE = "developer_mode"
        private const val KEY_DEVELOPER_DIAGNOSTICS = "developer_diagnostics"
        private const val KEY_AUTOPLAY_FIRST_SOURCE = "autoplay_first_source"
        private const val KEY_AUTOPLAY_NEXT_EPISODE = "autoplay_next_episode"
        private const val KEY_SUBTITLE_PREFERRED_LANGUAGE = "subtitle_preferred_language"
        private const val KEY_SUBTITLE_SECOND_PREFERRED_LANGUAGE = "subtitle_second_preferred_language"
        private const val KEY_SUBTITLE_DISPLAY_MODE = "subtitle_display_mode"
        private const val KEY_CATALOG_ORDER = "catalog_order"
        private const val KEY_HIDDEN_CATALOGS = "hidden_catalogs"
        private const val KEY_TMDB_API_KEY = "tmdb_api_key"
        private const val KEY_TMDB_ENRICHMENT_ENABLED = "tmdb_enrichment_enabled"
        private const val KEY_MDBLIST_API_KEY = "mdblist_api_key"
        private const val KEY_MDBLIST_RATINGS_ENABLED = "mdblist_ratings_enabled"
        private const val KEY_MDBLIST_PROVIDERS = "mdblist_rating_providers"
        private const val KEY_TRAKT_CLIENT_ID = "trakt_client_id"
        private const val KEY_TRAKT_CLIENT_SECRET = "trakt_client_secret"
        private const val KEY_TRAKT_ACCESS_TOKEN = "trakt_access_token"
        private const val KEY_TRAKT_REFRESH_TOKEN = "trakt_refresh_token"
        private const val KEY_TRAKT_EXPIRES_AT = "trakt_expires_at"
        private const val KEY_TRAKT_USERNAME = "trakt_username"

        private val DEFAULT_MDBLIST_PROVIDERS = setOf(
            "imdb",
            "tmdb",
            "tomatoes",
            "metacritic",
            "trakt",
            "letterboxd",
            "audience",
            "mal",
            "metacriticuser",
            "rogerebert"
        )
    }
}
