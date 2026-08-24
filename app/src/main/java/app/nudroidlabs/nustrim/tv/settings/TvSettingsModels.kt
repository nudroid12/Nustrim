package app.nudroidlabs.nustrim.tv.settings

import app.nudroidlabs.nustrim.core.source.InstalledSource
import app.nudroidlabs.nustrim.core.update.UpdateInfo
import app.nudroidlabs.nustrim.ui.SubtitleDisplayMode

enum class TvSettingsCategory(val label: String, val subtitle: String) {
    PLAYBACK("Playback", "Source and episode behaviour"),
    SUBTITLES("Subtitles", "Language and visibility"),
    CONTENT("Content Manager", "Add-ons and Home catalogue order"),
    INTEGRATIONS("Integrations", "Metadata, ratings and tracking"),
    LOCAL_DATA("Local data", "Backup and restore"),
    ADVANCED("Advanced", "Developer and diagnostic controls"),
    ABOUT("About", "Version and update information"),
}

enum class TvContentManagerSection(val label: String) {
    ADDONS("Add-ons"),
    CATALOG_ORDER("Catalogue order"),
}

data class TvSettingsSnapshot(
    val autoplayFirstSource: Boolean,
    val autoplayNextEpisode: Boolean,
    val seekStepSeconds: Int,
    val controlsAutoHideSeconds: Int,
    val subtitlePreferredLanguage: String,
    val subtitleSecondPreferredLanguage: String,
    val subtitleDisplayMode: SubtitleDisplayMode,
    val subtitleFontSize: Int,
    val subtitleBold: Boolean,
    val sources: List<InstalledSource>,
    val catalogs: List<TvSettingsCatalog>,
    val catalogsLoading: Boolean,
    val developerMode: Boolean,
    val developerDiagnostics: Boolean,
    val tmdbConfigured: Boolean,
    val tmdbEnabled: Boolean,
    val mdbListConfigured: Boolean,
    val mdbListEnabled: Boolean,
    val mdbListProviders: Map<String, Boolean>,
    val traktConnected: Boolean,
    val traktUsername: String,
    val diagnosticsLineCount: Int,
    val statusMessage: String,
)

data class TvSettingsCatalog(
    val key: String,
    val title: String,
    val sourceName: String,
    val visible: Boolean,
)

sealed interface TvSettingsEditor {
    data class Addon(val url: String = "", val message: String = "") : TvSettingsEditor
    data class Tmdb(val credential: String) : TvSettingsEditor
    data class MdbList(val apiKey: String) : TvSettingsEditor
    data class Trakt(val clientId: String, val clientSecret: String) : TvSettingsEditor
}

sealed interface TvSettingsUpdateState {
    data object Idle : TvSettingsUpdateState
    data object Checking : TvSettingsUpdateState
    data object UpToDate : TvSettingsUpdateState
    data class Available(val info: UpdateInfo) : TvSettingsUpdateState
    data class Downloading(val info: UpdateInfo, val progress: Int) : TvSettingsUpdateState
    data class PermissionRequired(val info: UpdateInfo, val apkPath: String) : TvSettingsUpdateState
    data class ReadyToInstall(val info: UpdateInfo, val apkPath: String) : TvSettingsUpdateState
    data class Error(val message: String) : TvSettingsUpdateState
}

internal val TV_SUBTITLE_LANGUAGES = listOf(
    "ms" to "Malay",
    "en" to "English",
    "id" to "Indonesian",
    "zh" to "Chinese",
    "ja" to "Japanese",
    "ko" to "Korean",
    "th" to "Thai",
    "ar" to "Arabic",
    "es" to "Spanish",
    "fr" to "French",
    "de" to "German",
)

internal val TV_MDBLIST_PROVIDERS = listOf(
    "imdb" to "IMDb",
    "tmdb" to "TMDB",
    "tomatoes" to "Rotten Tomatoes",
    "metacritic" to "Metacritic",
    "trakt" to "Trakt",
    "letterboxd" to "Letterboxd",
    "audience" to "Audience",
    "mal" to "MyAnimeList",
    "metacriticuser" to "Metacritic User",
    "rogerebert" to "Roger Ebert",
)
