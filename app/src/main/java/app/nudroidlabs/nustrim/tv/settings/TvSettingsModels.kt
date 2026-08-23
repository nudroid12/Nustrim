package app.nudroidlabs.nustrim.tv.settings

import app.nudroidlabs.nustrim.core.source.InstalledSource
import app.nudroidlabs.nustrim.core.update.UpdateInfo
import app.nudroidlabs.nustrim.ui.SubtitleDisplayMode

enum class TvSettingsCategory(val label: String, val subtitle: String) {
    PLAYBACK("Playback", "Source and episode behaviour"),
    SUBTITLES("Subtitles", "Language and visibility"),
    CONTENT("Content", "Installed source management"),
    INTEGRATIONS("Integrations", "Metadata, ratings and tracking"),
    ADVANCED("Advanced", "Developer and diagnostic controls"),
    ABOUT("About", "Version and update information"),
}

data class TvSettingsSnapshot(
    val autoplayFirstSource: Boolean,
    val autoplayNextEpisode: Boolean,
    val seekStepSeconds: Int,
    val controlsAutoHideSeconds: Int,
    val subtitlePreferredLanguage: String,
    val subtitleSecondPreferredLanguage: String,
    val subtitleDisplayMode: SubtitleDisplayMode,
    val sources: List<InstalledSource>,
    val developerMode: Boolean,
    val developerDiagnostics: Boolean,
    val tmdbConfigured: Boolean,
    val tmdbEnabled: Boolean,
    val mdbListConfigured: Boolean,
    val mdbListEnabled: Boolean,
    val traktConnected: Boolean,
)

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
    "ar" to "Arabic",
)
