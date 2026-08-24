package app.nudroidlabs.nustrim.tv.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import app.nudroidlabs.nustrim.core.source.CatalogSectionSourceSession
import app.nudroidlabs.nustrim.core.source.SourceEngine
import app.nudroidlabs.nustrim.core.source.SourceKind
import app.nudroidlabs.nustrim.core.source.cloudstream.CloudStreamProviderStore
import app.nudroidlabs.nustrim.core.diagnostics.NustrimDiagnostics
import app.nudroidlabs.nustrim.core.integrations.MdbListClient
import app.nudroidlabs.nustrim.core.integrations.TmdbClient
import app.nudroidlabs.nustrim.core.integrations.TraktClient
import app.nudroidlabs.nustrim.core.library.LocalMediaStore
import app.nudroidlabs.nustrim.core.update.AppUpdater
import app.nudroidlabs.nustrim.tv.focus.TvFocusRegistry
import app.nudroidlabs.nustrim.ui.SubtitleDisplayMode
import app.nudroidlabs.nustrim.ui.UiPreferences
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

@Composable
fun TvSettingsEntry(
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    onSwitchToMobile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val preferences = remember(context) { UiPreferences(context) }
    val sourceStore = remember(context) { InstalledSourceStore(context) }
    val sourceEngine = remember(context) { SourceEngine(context) }
    val cloudStreamProviderStore = remember(context) { CloudStreamProviderStore(context) }
    val mediaStore = remember(context) { LocalMediaStore(context) }
    val updater = remember(context) { AppUpdater(context) }
    val coroutineScope = rememberCoroutineScope()
    val memory = remember(scopeKey) { TvSettingsSessionStore.memory(scopeKey) }
    var revision by remember { mutableStateOf(0) }
    var updateState by remember { mutableStateOf<TvSettingsUpdateState>(TvSettingsUpdateState.Idle) }
    var editor by remember { mutableStateOf<TvSettingsEditor?>(null) }
    var statusMessage by remember { mutableStateOf("") }
    var discoveredCatalogs by remember { mutableStateOf<List<TvSettingsCatalog>>(emptyList()) }
    var discoveredCloudStreamProviders by remember {
        mutableStateOf<List<TvSettingsCloudStreamProvider>>(emptyList())
    }
    var catalogsLoading by remember { mutableStateOf(true) }
    var cloudStreamProvidersLoading by remember { mutableStateOf(true) }
    val diagnosticEntries by NustrimDiagnostics.entries.collectAsState()

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

    fun reloadCatalogs() {
        catalogsLoading = true
        cloudStreamProvidersLoading = true
        discoveredCatalogs = emptyList()
        discoveredCloudStreamProviders = emptyList()
        val urls = sourceStore.enabledUrls(preferences.developerMode)
        if (urls.isEmpty()) {
            catalogsLoading = false
            cloudStreamProvidersLoading = false
            return
        }
        var pending = urls.size
        fun finish() {
            pending -= 1
            if (pending <= 0) {
                catalogsLoading = false
                cloudStreamProvidersLoading = false
            }
        }
        urls.forEach { url ->
            sourceEngine.open(
                url,
                onSuccess = { session ->
                    if (session.kind == SourceKind.CLOUDSTREAM) {
                        session.loadCatalog(
                            onSuccess = { catalog ->
                                discoveredCloudStreamProviders = discoveredCloudStreamProviders +
                                    catalog.items.map { item ->
                                        TvSettingsCloudStreamProvider(
                                            repositoryUrl = url,
                                            repositoryId = session.id,
                                            item = item,
                                            enabled = cloudStreamProviderStore.isEnabled(session.id, item),
                                        )
                                    }
                                finish()
                            },
                            onError = { finish() },
                        )
                    } else {
                        val sectioned = session as? CatalogSectionSourceSession
                        if (sectioned != null) {
                            sectioned.loadCatalogSections(
                                onSuccess = { sections ->
                                    discoveredCatalogs = discoveredCatalogs + sections.map { catalog ->
                                        val key = "$url|${catalog.name.trim()}"
                                        TvSettingsCatalog(
                                            key = key,
                                            title = catalog.name,
                                            sourceName = session.displayName,
                                            visible = key !in preferences.hiddenCatalogKeys,
                                        )
                                    }
                                    finish()
                                },
                                onError = { finish() },
                            )
                        } else {
                            session.loadCatalog(
                                onSuccess = { catalog ->
                                    val key = "$url|${catalog.name.trim()}"
                                    discoveredCatalogs = discoveredCatalogs + TvSettingsCatalog(
                                        key = key,
                                        title = catalog.name,
                                        sourceName = session.displayName,
                                        visible = key !in preferences.hiddenCatalogKeys,
                                    )
                                    finish()
                                },
                                onError = { finish() },
                            )
                        }
                    }
                },
                onError = { finish() },
            )
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) { reloadCatalogs() }

    val snapshot = remember(
        revision,
        discoveredCatalogs,
        discoveredCloudStreamProviders,
        catalogsLoading,
        cloudStreamProvidersLoading,
        diagnosticEntries,
        statusMessage,
    ) {
        val ranks = preferences.catalogOrder.withIndex().associate { it.value to it.index }
        val sortedCatalogs = discoveredCatalogs.distinctBy { it.key }.sortedWith(
            compareBy<TvSettingsCatalog> { ranks[it.key] ?: Int.MAX_VALUE }
                .thenBy { discoveredCatalogs.indexOfFirst { candidate -> candidate.key == it.key } },
        )
        TvSettingsSnapshot(
            autoplayFirstSource = preferences.autoplayFirstSource,
            autoplayNextEpisode = preferences.autoplayNextEpisode,
            seekStepSeconds = preferences.tvSeekStepSeconds,
            controlsAutoHideSeconds = preferences.tvControlsAutoHideSeconds,
            subtitlePreferredLanguage = preferences.subtitlePreferredLanguage,
            subtitleSecondPreferredLanguage = preferences.subtitleSecondPreferredLanguage,
            subtitleDisplayMode = preferences.subtitleDisplayMode,
            subtitleFontSize = preferences.subtitleFontSize,
            subtitleBold = preferences.subtitleBold,
            sources = sourceStore.visibleSources(preferences.developerMode),
            cloudStreamProviders = discoveredCloudStreamProviders.distinctBy {
                "${it.repositoryId}|${CloudStreamProviderStore.providerIdentityInternal(it.item)}"
            },
            cloudStreamProvidersLoading = cloudStreamProvidersLoading,
            catalogs = sortedCatalogs,
            catalogsLoading = catalogsLoading,
            developerMode = preferences.developerMode,
            developerDiagnostics = preferences.developerDiagnostics,
            tmdbConfigured = preferences.tmdbApiKey.isNotBlank(),
            tmdbEnabled = preferences.tmdbEnrichmentEnabled,
            mdbListConfigured = preferences.mdbListApiKey.isNotBlank(),
            mdbListEnabled = preferences.mdbListRatingsEnabled,
            mdbListProviders = TV_MDBLIST_PROVIDERS.associate { (id, _) ->
                id to preferences.isMdbListProviderEnabled(id)
            },
            traktConnected = preferences.traktConnected,
            traktUsername = preferences.traktUsername,
            diagnosticsLineCount = diagnosticEntries.size,
            statusMessage = statusMessage,
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
        onDecreaseSubtitleFontSize = {
            preferences.subtitleFontSize = (snapshot.subtitleFontSize - 2).coerceAtLeast(12)
            refresh()
        },
        onIncreaseSubtitleFontSize = {
            preferences.subtitleFontSize = (snapshot.subtitleFontSize + 2).coerceAtMost(32)
            refresh()
        },
        onToggleSubtitleBold = {
            preferences.subtitleBold = !snapshot.subtitleBold
            refresh()
        },
        onAddSource = { editor = TvSettingsEditor.Addon() },
        onToggleSource = { source ->
            sourceStore.setEnabled(source.url, !source.enabled)
            reloadCatalogs()
            refresh()
        },
        onToggleCloudStreamProvider = { provider ->
            cloudStreamProviderStore.setEnabled(
                provider.repositoryId,
                provider.item,
                !provider.enabled,
            )
            discoveredCloudStreamProviders = discoveredCloudStreamProviders.map {
                if (
                    it.repositoryId == provider.repositoryId &&
                    CloudStreamProviderStore.providerIdentityInternal(it.item) ==
                    CloudStreamProviderStore.providerIdentityInternal(provider.item)
                ) {
                    it.copy(enabled = !provider.enabled)
                } else {
                    it
                }
            }
            statusMessage = if (provider.enabled) {
                "${provider.item.title} disabled."
            } else {
                "${provider.item.title} enabled."
            }
            refresh()
        },
        onToggleCatalog = { catalog ->
            preferences.setCatalogHidden(catalog.key, catalog.visible)
            discoveredCatalogs = discoveredCatalogs.map {
                if (it.key == catalog.key) it.copy(visible = !catalog.visible) else it
            }
            refresh()
        },
        onMoveCatalog = { catalog, delta ->
            val keys = snapshot.catalogs.map { it.key }.toMutableList()
            val from = keys.indexOf(catalog.key)
            val to = (from + delta).coerceIn(0, keys.lastIndex)
            if (from >= 0 && from != to) {
                val moved = keys.removeAt(from)
                keys.add(to, moved)
                preferences.catalogOrder = keys
                refresh()
            }
        },
        onResetCatalogs = {
            preferences.resetCatalogLayout()
            discoveredCatalogs = discoveredCatalogs.map { it.copy(visible = true) }
            statusMessage = "Catalog order and visibility reset."
            refresh()
        },
        onToggleTmdb = {
            if (snapshot.tmdbConfigured) {
                preferences.tmdbEnrichmentEnabled = !snapshot.tmdbEnabled
                refresh()
            }
        },
        onEditTmdb = { editor = TvSettingsEditor.Tmdb(preferences.tmdbApiKey) },
        onToggleMdbList = {
            if (snapshot.mdbListConfigured) {
                preferences.mdbListRatingsEnabled = !snapshot.mdbListEnabled
                refresh()
            }
        },
        onEditMdbList = { editor = TvSettingsEditor.MdbList(preferences.mdbListApiKey) },
        onToggleMdbListProvider = { providerId ->
            preferences.setMdbListProviderEnabled(
                providerId,
                snapshot.mdbListProviders[providerId] != true,
            )
            refresh()
        },
        onEditTrakt = {
            editor = TvSettingsEditor.Trakt(preferences.traktClientId, preferences.traktClientSecret)
        },
        onDisconnectTrakt = {
            preferences.clearTraktConnection()
            statusMessage = "Trakt disconnected. Local watch progress is unchanged."
            refresh()
        },
        onCopyBackup = {
            val payload = JSONObject()
                .put("format", 1)
                .put("sources", JSONArray(sourceStore.exportJson()))
                .put("library", JSONArray(mediaStore.exportJson()))
                .put(
                    "ui",
                    JSONObject()
                        .put("developerMode", preferences.developerMode)
                        .put("developerDiagnostics", preferences.developerDiagnostics)
                        .put("autoplayFirstSource", preferences.autoplayFirstSource)
                        .put("autoplayNextEpisode", preferences.autoplayNextEpisode),
                )
                .toString()
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Nustrim backup", payload))
            statusMessage = "Backup copied. Integration API keys are excluded."
            refresh()
        },
        onRestoreBackup = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val raw = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
            runCatching {
                val payload = JSONObject(raw)
                sourceStore.importJson(payload.getJSONArray("sources").toString())
                mediaStore.importJson(payload.getJSONArray("library").toString())
                payload.optJSONObject("ui")?.let { ui ->
                    preferences.developerMode = ui.optBoolean("developerMode", preferences.developerMode)
                    preferences.developerDiagnostics = ui.optBoolean(
                        "developerDiagnostics",
                        preferences.developerDiagnostics,
                    )
                    preferences.autoplayFirstSource = ui.optBoolean(
                        "autoplayFirstSource",
                        preferences.autoplayFirstSource,
                    )
                    preferences.autoplayNextEpisode = ui.optBoolean(
                        "autoplayNextEpisode",
                        preferences.autoplayNextEpisode,
                    )
                    if (preferences.developerMode) sourceStore.ensureDeveloperDefaults()
                }
            }.onSuccess {
                statusMessage = "Backup restored from clipboard."
            }.onFailure {
                statusMessage = "Restore failed: ${it.message ?: "invalid backup"}"
            }
            refresh()
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
        onCopyDiagnostics = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(
                ClipData.newPlainText("Nustrim diagnostics", NustrimDiagnostics.snapshotText()),
            )
            statusMessage = "Diagnostics copied to clipboard."
            refresh()
        },
        onClearDiagnostics = {
            NustrimDiagnostics.clear()
            statusMessage = "Diagnostics log cleared."
            refresh()
        },
        onSwitchToMobile = onSwitchToMobile,
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

    editor?.let { active ->
        TvSettingsCredentialDialog(
            editor = active,
            onDismiss = { editor = null },
            onSave = { first, second ->
                when (active) {
                    is TvSettingsEditor.Addon -> Unit
                    is TvSettingsEditor.Tmdb -> {
                        preferences.tmdbApiKey = first
                        preferences.tmdbEnrichmentEnabled = first.isNotBlank()
                        statusMessage = "TMDB credential saved locally."
                    }
                    is TvSettingsEditor.MdbList -> {
                        preferences.mdbListApiKey = first
                        preferences.mdbListRatingsEnabled = first.isNotBlank()
                        statusMessage = "MDBList API key saved locally."
                    }
                    is TvSettingsEditor.Trakt -> {
                        preferences.traktClientId = first
                        preferences.traktClientSecret = second
                        statusMessage = "Trakt application credentials saved locally."
                    }
                }
                editor = null
                refresh()
            },
            onTestOrConnect = { first, second ->
                coroutineScope.launch {
                    when (active) {
                        is TvSettingsEditor.Addon -> {
                            editor = TvSettingsEditor.Addon(first, "Checking add-on...")
                            sourceEngine.open(
                                first,
                                onSuccess = {
                                    sourceStore.add(first)
                                    statusMessage = "Add-on added."
                                    editor = null
                                    reloadCatalogs()
                                    refresh()
                                },
                                onError = { error ->
                                    editor = TvSettingsEditor.Addon(
                                        first,
                                        error.message.orEmpty().ifBlank { "Unable to open this add-on URL." },
                                    )
                                },
                            )
                        }
                        is TvSettingsEditor.Tmdb -> {
                            statusMessage = "Testing TMDB..."
                            TmdbClient.validate(first).fold(
                                onSuccess = {
                                    preferences.tmdbApiKey = first
                                    preferences.tmdbEnrichmentEnabled = true
                                    statusMessage = "TMDB connection successful."
                                    editor = null
                                },
                                onFailure = { statusMessage = it.message ?: "TMDB connection failed." },
                            )
                        }
                        is TvSettingsEditor.MdbList -> {
                            statusMessage = "Testing MDBList..."
                            MdbListClient.validate(first).fold(
                                onSuccess = {
                                    preferences.mdbListApiKey = first
                                    preferences.mdbListRatingsEnabled = true
                                    statusMessage = "MDBList connection successful."
                                    editor = null
                                },
                                onFailure = { statusMessage = it.message ?: "MDBList connection failed." },
                            )
                        }
                        is TvSettingsEditor.Trakt -> {
                            preferences.traktClientId = first
                            preferences.traktClientSecret = second
                            if (first.isBlank() || second.isBlank()) {
                                statusMessage = "Enter both Trakt client ID and client secret."
                            } else {
                                statusMessage = "Requesting Trakt device code..."
                                TraktClient.createDeviceCode(first).fold(
                                    onSuccess = { device ->
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Trakt code", device.userCode))
                                        statusMessage = "Code ${device.userCode} copied. Open ${device.verificationUrl}. Waiting for approval..."
                                        editor = null
                                        var resolved = false
                                        while (System.currentTimeMillis() < device.expiresAtMs) {
                                            delay(device.interval.coerceAtLeast(2) * 1000L)
                                            val tokenResult = TraktClient.pollDeviceToken(
                                                device.deviceCode,
                                                first,
                                                second,
                                            )
                                            val token = tokenResult.getOrNull()
                                            if (token != null) {
                                                preferences.saveTraktToken(
                                                    token.accessToken,
                                                    token.refreshToken,
                                                    token.expiresAtSeconds,
                                                )
                                                preferences.traktUsername = TraktClient.username(token.accessToken, first)
                                                    .getOrNull().orEmpty()
                                                statusMessage = "Trakt connected."
                                                resolved = true
                                                refresh()
                                                break
                                            }
                                            tokenResult.exceptionOrNull()?.let {
                                                statusMessage = it.message ?: "Trakt connection failed."
                                                resolved = true
                                                break
                                            }
                                        }
                                        if (!resolved) statusMessage = "Trakt authorisation code expired. Start again."
                                    },
                                    onFailure = { statusMessage = it.message ?: "Could not start Trakt authorisation." },
                                )
                            }
                        }
                    }
                    refresh()
                }
            },
        )
    }
}
