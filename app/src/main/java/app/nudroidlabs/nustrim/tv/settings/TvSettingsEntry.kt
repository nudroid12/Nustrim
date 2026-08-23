package app.nudroidlabs.nustrim.tv.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.nudroidlabs.nustrim.core.source.InstalledSourceStore
import app.nudroidlabs.nustrim.core.update.AppUpdater
import app.nudroidlabs.nustrim.tv.focus.TvFocusRegistry
import app.nudroidlabs.nustrim.ui.SubtitleDisplayMode
import app.nudroidlabs.nustrim.ui.UiPreferences
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun TvSettingsEntry(
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val preferences = remember(context) { UiPreferences(context) }
    val sourceStore = remember(context) { InstalledSourceStore(context) }
    val updater = remember(context) { AppUpdater(context) }
    val coroutineScope = rememberCoroutineScope()
    val memory = remember(scopeKey) { TvSettingsSessionStore.memory(scopeKey) }
    var revision by remember { mutableStateOf(0) }
    var updateState by remember { mutableStateOf<TvSettingsUpdateState>(TvSettingsUpdateState.Idle) }

    fun installDownloaded(info: app.nudroidlabs.nustrim.core.update.UpdateInfo, apkPath: String) {
        val apk = File(apkPath)
        if (!apk.isFile) {
            updateState = TvSettingsUpdateState.Error("Downloaded update is missing. Download it again.")
            return
        }
        if (!updater.canRequestPackageInstall()) {
            updateState = TvSettingsUpdateState.PermissionRequired(info, apkPath)
            updater.openInstallPermission()
            return
        }
        updater.install(apk).fold(
            onSuccess = {
                updateState = TvSettingsUpdateState.ReadyToInstall(info, apkPath)
            },
            onFailure = { error ->
                updateState = TvSettingsUpdateState.Error(
                    error.message.orEmpty().ifBlank { error::class.java.simpleName },
                )
            },
        )
    }

    DisposableEffect(lifecycleOwner, updateState) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val state = updateState
                if (state is TvSettingsUpdateState.PermissionRequired && updater.canRequestPackageInstall()) {
                    installDownloaded(state.info, state.apkPath)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun refresh() {
        revision += 1
    }

    val snapshot = remember(revision) {
        TvSettingsSnapshot(
            autoplayFirstSource = preferences.autoplayFirstSource,
            autoplayNextEpisode = preferences.autoplayNextEpisode,
            seekStepSeconds = preferences.tvSeekStepSeconds,
            controlsAutoHideSeconds = preferences.tvControlsAutoHideSeconds,
            subtitlePreferredLanguage = preferences.subtitlePreferredLanguage,
            subtitleSecondPreferredLanguage = preferences.subtitleSecondPreferredLanguage,
            subtitleDisplayMode = preferences.subtitleDisplayMode,
            sources = sourceStore.visibleSources(preferences.developerMode),
            developerMode = preferences.developerMode,
            developerDiagnostics = preferences.developerDiagnostics,
            tmdbConfigured = preferences.tmdbApiKey.isNotBlank(),
            tmdbEnabled = preferences.tmdbEnrichmentEnabled,
            mdbListConfigured = preferences.mdbListApiKey.isNotBlank(),
            mdbListEnabled = preferences.mdbListRatingsEnabled,
            traktConnected = preferences.traktConnected,
        )
    }

    TvSettingsScreen(
        snapshot = snapshot,
        updateState = updateState,
        memory = memory,
        scopeKey = scopeKey,
        focusRegistry = focusRegistry,
        focusRequestToken = focusRequestToken,
        onToggleAutoplayFirstSource = {
            preferences.autoplayFirstSource = !snapshot.autoplayFirstSource
            refresh()
        },
        onToggleAutoplayNextEpisode = {
            preferences.autoplayNextEpisode = !snapshot.autoplayNextEpisode
            refresh()
        },
        onCycleSeekStep = {
            val values = listOf(10, 15, 30)
            preferences.tvSeekStepSeconds = values[(values.indexOf(snapshot.seekStepSeconds) + 1) % values.size]
            refresh()
        },
        onCycleControlsAutoHide = {
            val values = listOf(3, 5, 8)
            preferences.tvControlsAutoHideSeconds = values[(values.indexOf(snapshot.controlsAutoHideSeconds) + 1) % values.size]
            refresh()
        },
        onCyclePreferredLanguage = {
            val codes = TV_SUBTITLE_LANGUAGES.map { it.first }
            val next = codes[(codes.indexOf(snapshot.subtitlePreferredLanguage).coerceAtLeast(0) + 1) % codes.size]
            preferences.subtitlePreferredLanguage = next
            if (next == snapshot.subtitleSecondPreferredLanguage) {
                preferences.subtitleSecondPreferredLanguage = if (next == "en") "ms" else "en"
            }
            refresh()
        },
        onCycleSecondLanguage = {
            val codes = TV_SUBTITLE_LANGUAGES.map { it.first }
                .filterNot { it == snapshot.subtitlePreferredLanguage }
            val next = codes[(codes.indexOf(snapshot.subtitleSecondPreferredLanguage).coerceAtLeast(0) + 1) % codes.size]
            preferences.subtitleSecondPreferredLanguage = next
            refresh()
        },
        onToggleSubtitleDisplayMode = {
            preferences.subtitleDisplayMode = if (snapshot.subtitleDisplayMode == SubtitleDisplayMode.SHOW_ALL) {
                SubtitleDisplayMode.PREFERRED_ONLY
            } else {
                SubtitleDisplayMode.SHOW_ALL
            }
            refresh()
        },
        onToggleSource = { source ->
            sourceStore.setEnabled(source.url, !source.enabled)
            refresh()
        },
        onToggleTmdb = {
            if (snapshot.tmdbConfigured) {
                preferences.tmdbEnrichmentEnabled = !snapshot.tmdbEnabled
                refresh()
            }
        },
        onToggleMdbList = {
            if (snapshot.mdbListConfigured) {
                preferences.mdbListRatingsEnabled = !snapshot.mdbListEnabled
                refresh()
            }
        },
        onToggleDeveloperMode = {
            preferences.developerMode = !snapshot.developerMode
            if (preferences.developerMode) sourceStore.ensureDeveloperDefaults()
            refresh()
        },
        onToggleDeveloperDiagnostics = {
            preferences.developerDiagnostics = !snapshot.developerDiagnostics
            refresh()
        },
        onUpdateAction = {
            when (val state = updateState) {
                TvSettingsUpdateState.Idle,
                TvSettingsUpdateState.UpToDate,
                is TvSettingsUpdateState.Error -> {
                    updateState = TvSettingsUpdateState.Checking
                    coroutineScope.launch {
                        updater.check().fold(
                            onSuccess = { info ->
                                updateState = if (info == null) {
                                    TvSettingsUpdateState.UpToDate
                                } else {
                                    TvSettingsUpdateState.Available(info)
                                }
                            },
                            onFailure = { error ->
                                updateState = TvSettingsUpdateState.Error(
                                    error.message.orEmpty().ifBlank { error::class.java.simpleName },
                                )
                            },
                        )
                    }
                }
                is TvSettingsUpdateState.Available -> {
                    updateState = TvSettingsUpdateState.Downloading(state.info, 0)
                    coroutineScope.launch {
                        updater.download(state.info) { progress ->
                            updateState = TvSettingsUpdateState.Downloading(state.info, progress)
                        }.fold(
                            onSuccess = { apk -> installDownloaded(state.info, apk.absolutePath) },
                            onFailure = { error ->
                                updateState = TvSettingsUpdateState.Error(
                                    error.message.orEmpty().ifBlank { error::class.java.simpleName },
                                )
                            },
                        )
                    }
                }
                is TvSettingsUpdateState.PermissionRequired -> {
                    installDownloaded(state.info, state.apkPath)
                }
                is TvSettingsUpdateState.ReadyToInstall -> {
                    installDownloaded(state.info, state.apkPath)
                }
                TvSettingsUpdateState.Checking,
                is TvSettingsUpdateState.Downloading -> Unit
            }
        },
        modifier = modifier,
    )
}
