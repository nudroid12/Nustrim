package app.nudroidlabs.nustrim.tv.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.nustrim.BuildConfig
import app.nudroidlabs.nustrim.core.source.InstalledSource
import app.nudroidlabs.nustrim.core.source.SourcePreset
import app.nudroidlabs.nustrim.tv.focus.TvFocusRegistry
import app.nudroidlabs.nustrim.tv.focus.TvFocusRestoreEffect
import app.nudroidlabs.nustrim.tv.focus.rememberTvFocusAnchor
import app.nudroidlabs.nustrim.tv.focus.tvFocusAnchor
import app.nudroidlabs.nustrim.tv.theme.TvTokens
import app.nudroidlabs.nustrim.tv.theme.animateTvFocusScale
import app.nudroidlabs.nustrim.ui.SubtitleDisplayMode

@Composable
internal fun TvSettingsScreen(
    snapshot: TvSettingsSnapshot,
    updateState: TvSettingsUpdateState,
    memory: TvSettingsMemory,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    onToggleAutoplayFirstSource: () -> Unit,
    onToggleAutoplayNextEpisode: () -> Unit,
    onCycleSeekStep: () -> Unit,
    onCycleControlsAutoHide: () -> Unit,
    onCyclePreferredLanguage: () -> Unit,
    onCycleSecondLanguage: () -> Unit,
    onToggleSubtitleDisplayMode: () -> Unit,
    onDecreaseSubtitleFontSize: () -> Unit,
    onIncreaseSubtitleFontSize: () -> Unit,
    onToggleSubtitleBold: () -> Unit,
    onToggleSource: (InstalledSource) -> Unit,
    onToggleCatalog: (TvSettingsCatalog) -> Unit,
    onMoveCatalog: (TvSettingsCatalog, Int) -> Unit,
    onResetCatalogs: () -> Unit,
    onToggleTmdb: () -> Unit,
    onEditTmdb: () -> Unit,
    onToggleMdbList: () -> Unit,
    onEditMdbList: () -> Unit,
    onToggleMdbListProvider: (String) -> Unit,
    onEditTrakt: () -> Unit,
    onDisconnectTrakt: () -> Unit,
    onCopyBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onToggleDeveloperMode: () -> Unit,
    onToggleDeveloperDiagnostics: () -> Unit,
    onCopyDiagnostics: () -> Unit,
    onClearDiagnostics: () -> Unit,
    onSwitchToMobile: () -> Unit,
    onUpdateAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var category by remember(scopeKey) { mutableStateOf(memory.selectedCategory) }
    val fallback = memory.lastDetailAnchor[category] ?: settingsRailAnchorKey(category)

    TvFocusRestoreEffect(
        registry = focusRegistry,
        scopeKey = scopeKey,
        fallbackAnchorKey = fallback,
        requestToken = focusRequestToken,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SETTINGS_BACKGROUND)
            .padding(start = 38.dp, end = 44.dp, top = 28.dp, bottom = 28.dp),
    ) {
        Text(
            text = "Settings",
            color = Color(0xFFF4F4F6),
            fontSize = 30.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF111216))
                .border(1.dp, Color(0xFF272A30), RoundedCornerShape(18.dp)),
        ) {
            SettingsRail(
                selected = category,
                scopeKey = scopeKey,
                focusRegistry = focusRegistry,
                onSelected = {
                    category = it
                    memory.selectedCategory = it
                },
                modifier = Modifier.width(238.dp).fillMaxHeight(),
            )
            Box(Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF272A30)))
            SettingsDetailPane(
                category = category,
                snapshot = snapshot,
                updateState = updateState,
                memory = memory,
                scopeKey = scopeKey,
                focusRegistry = focusRegistry,
                onToggleAutoplayFirstSource = onToggleAutoplayFirstSource,
                onToggleAutoplayNextEpisode = onToggleAutoplayNextEpisode,
                onCycleSeekStep = onCycleSeekStep,
                onCycleControlsAutoHide = onCycleControlsAutoHide,
                onCyclePreferredLanguage = onCyclePreferredLanguage,
                onCycleSecondLanguage = onCycleSecondLanguage,
                onToggleSubtitleDisplayMode = onToggleSubtitleDisplayMode,
                onDecreaseSubtitleFontSize = onDecreaseSubtitleFontSize,
                onIncreaseSubtitleFontSize = onIncreaseSubtitleFontSize,
                onToggleSubtitleBold = onToggleSubtitleBold,
                onToggleSource = onToggleSource,
                onToggleCatalog = onToggleCatalog,
                onMoveCatalog = onMoveCatalog,
                onResetCatalogs = onResetCatalogs,
                onToggleTmdb = onToggleTmdb,
                onEditTmdb = onEditTmdb,
                onToggleMdbList = onToggleMdbList,
                onEditMdbList = onEditMdbList,
                onToggleMdbListProvider = onToggleMdbListProvider,
                onEditTrakt = onEditTrakt,
                onDisconnectTrakt = onDisconnectTrakt,
                onCopyBackup = onCopyBackup,
                onRestoreBackup = onRestoreBackup,
                onToggleDeveloperMode = onToggleDeveloperMode,
                onToggleDeveloperDiagnostics = onToggleDeveloperDiagnostics,
                onCopyDiagnostics = onCopyDiagnostics,
                onClearDiagnostics = onClearDiagnostics,
                onSwitchToMobile = onSwitchToMobile,
                onUpdateAction = onUpdateAction,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun SettingsRail(
    selected: TvSettingsCategory,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    onSelected: (TvSettingsCategory) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(TvSettingsCategory.entries, key = { it.name }) { category ->
            SettingsRailRow(
                category = category,
                selected = category == selected,
                scopeKey = scopeKey,
                focusRegistry = focusRegistry,
                onFocused = { onSelected(category) },
                onOpenDetail = {
                    onSelected(category)
                    focusRegistry.requestAnchor(scopeKey, settingsFirstDetailAnchorKey(category))
                },
            )
        }
    }
}

@Composable
private fun SettingsRailRow(
    category: TvSettingsCategory,
    selected: Boolean,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    onFocused: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    val anchor = rememberTvFocusAnchor(focusRegistry, scopeKey, settingsRailAnchorKey(category))
    var focused by remember { mutableStateOf(false) }
    val foreground = if (focused) Color(0xFF101114) else Color.White
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    focused -> Color.White
                    selected -> Color(0xFF292B31)
                    else -> Color.Transparent
                },
            )
            .tvFocusAnchor(anchor)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight -> {
                        onOpenDetail()
                        true
                    }
                    event.type == KeyEventType.KeyDown &&
                        (event.key == Key.DirectionCenter || event.key == Key.Enter) -> {
                        onOpenDetail()
                        true
                    }
                    else -> false
                }
            }
            .clickable(onClick = onOpenDetail)
            .focusable()
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = settingsCategoryIcon(category),
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = category.label,
            color = foreground,
            fontSize = 14.sp,
            fontWeight = if (focused || selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun SettingsDetailPane(
    category: TvSettingsCategory,
    snapshot: TvSettingsSnapshot,
    updateState: TvSettingsUpdateState,
    memory: TvSettingsMemory,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    onToggleAutoplayFirstSource: () -> Unit,
    onToggleAutoplayNextEpisode: () -> Unit,
    onCycleSeekStep: () -> Unit,
    onCycleControlsAutoHide: () -> Unit,
    onCyclePreferredLanguage: () -> Unit,
    onCycleSecondLanguage: () -> Unit,
    onToggleSubtitleDisplayMode: () -> Unit,
    onDecreaseSubtitleFontSize: () -> Unit,
    onIncreaseSubtitleFontSize: () -> Unit,
    onToggleSubtitleBold: () -> Unit,
    onToggleSource: (InstalledSource) -> Unit,
    onToggleCatalog: (TvSettingsCatalog) -> Unit,
    onMoveCatalog: (TvSettingsCatalog, Int) -> Unit,
    onResetCatalogs: () -> Unit,
    onToggleTmdb: () -> Unit,
    onEditTmdb: () -> Unit,
    onToggleMdbList: () -> Unit,
    onEditMdbList: () -> Unit,
    onToggleMdbListProvider: (String) -> Unit,
    onEditTrakt: () -> Unit,
    onDisconnectTrakt: () -> Unit,
    onCopyBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onToggleDeveloperMode: () -> Unit,
    onToggleDeveloperDiagnostics: () -> Unit,
    onCopyDiagnostics: () -> Unit,
    onClearDiagnostics: () -> Unit,
    onSwitchToMobile: () -> Unit,
    onUpdateAction: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier.padding(horizontal = 28.dp, vertical = 24.dp)) {
        Text(category.label, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(category.subtitle, color = Color(0xFF8D9099), fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))

        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            when (category) {
                TvSettingsCategory.PLAYBACK -> {
                    item("playback-source") {
                        SettingsActionRow(
                            title = "Autoplay first source",
                            subtitle = "Play the first enabled result instead of opening the source picker.",
                            value = onOff(snapshot.autoplayFirstSource),
                            checked = snapshot.autoplayFirstSource,
                            anchorKey = settingsFirstDetailAnchorKey(category),
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = onToggleAutoplayFirstSource,
                        )
                    }
                    item("playback-next") {
                        SettingsActionRow(
                            title = "Autoplay next episode",
                            subtitle = "Continue to the next episode when playback finishes.",
                            value = onOff(snapshot.autoplayNextEpisode),
                            checked = snapshot.autoplayNextEpisode,
                            anchorKey = "settings:playback:next",
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = onToggleAutoplayNextEpisode,
                        )
                    }
                    item("playback-seek") {
                        SettingsActionRow(
                            title = "Seek step",
                            subtitle = "Preferred jump interval for TV playback.",
                            value = "${snapshot.seekStepSeconds} seconds",
                            anchorKey = "settings:playback:seek",
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = onCycleSeekStep,
                        )
                    }
                    item("playback-hide") {
                        SettingsActionRow(
                            title = "Controls auto-hide",
                            subtitle = "Preferred delay before player controls disappear.",
                            value = "${snapshot.controlsAutoHideSeconds} seconds",
                            anchorKey = "settings:playback:hide",
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = onCycleControlsAutoHide,
                        )
                    }
                }

                TvSettingsCategory.SUBTITLES -> {
                    item("subtitle-primary") {
                        SettingsActionRow(
                            title = "Preferred language",
                            subtitle = "Primary language used when subtitle tracks are available.",
                            value = subtitleLanguageLabel(snapshot.subtitlePreferredLanguage),
                            anchorKey = settingsFirstDetailAnchorKey(category),
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = onCyclePreferredLanguage,
                        )
                    }
                    item("subtitle-secondary") {
                        SettingsActionRow(
                            title = "Second language",
                            subtitle = "Fallback language when the preferred track is unavailable.",
                            value = subtitleLanguageLabel(snapshot.subtitleSecondPreferredLanguage),
                            anchorKey = "settings:subtitles:secondary",
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = onCycleSecondLanguage,
                        )
                    }
                    item("subtitle-display") {
                        val showAll = snapshot.subtitleDisplayMode == SubtitleDisplayMode.SHOW_ALL
                        SettingsActionRow(
                            title = "Languages shown",
                            subtitle = "Choose whether the player lists every track or preferred languages only.",
                            value = if (showAll) "Show all" else "Preferred only",
                            checked = showAll,
                            anchorKey = "settings:subtitles:display",
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = onToggleSubtitleDisplayMode,
                        )
                    }
                    item("subtitle-size-down") {
                        SettingsActionRow(
                            title = "Subtitle font size",
                            subtitle = "Decrease the default subtitle size used by the player.",
                            value = "${snapshot.subtitleFontSize} sp  −",
                            enabled = snapshot.subtitleFontSize > 12,
                            anchorKey = "settings:subtitles:size-down",
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = onDecreaseSubtitleFontSize,
                        )
                    }
                    item("subtitle-size-up") {
                        SettingsActionRow(
                            title = "Subtitle font size",
                            subtitle = "Increase the default subtitle size used by the player.",
                            value = "${snapshot.subtitleFontSize} sp  +",
                            enabled = snapshot.subtitleFontSize < 32,
                            anchorKey = "settings:subtitles:size-up",
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = onIncreaseSubtitleFontSize,
                        )
                    }
                    item("subtitle-bold") {
                        SettingsActionRow(
                            title = "Bold subtitles",
                            subtitle = "Use bold text for subtitles by default.",
                            value = onOff(snapshot.subtitleBold),
                            checked = snapshot.subtitleBold,
                            anchorKey = "settings:subtitles:bold",
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = onToggleSubtitleBold,
                        )
                    }
                }

                TvSettingsCategory.CONTENT -> {
                    val visible = snapshot.sources
                    items(visible, key = { it.url }) { source ->
                        val first = source === visible.firstOrNull()
                        SettingsActionRow(
                            title = sourceDisplayName(source),
                            subtitle = source.url,
                            value = onOff(source.enabled),
                            checked = source.enabled,
                            anchorKey = if (first) settingsFirstDetailAnchorKey(category) else "settings:content:${source.url}",
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = { onToggleSource(source) },
                        )
                    }
                }

                TvSettingsCategory.CATALOG_ORDER -> {
                    item("catalog-layout-heading") {
                        SettingsInfoCard(
                            title = "Home catalogue order",
                            lines = listOf(
                                "Rows" to if (snapshot.catalogsLoading) "Loading..." else snapshot.catalogs.size.toString(),
                                "Control" to "Visibility and order",
                            ),
                        )
                    }
                    item("catalog-layout-reset") {
                        SettingsActionRow(
                            title = "Reset catalogue order",
                            subtitle = "Restore the provider order and show every catalogue row.",
                            value = "Reset",
                            anchorKey = settingsFirstDetailAnchorKey(category),
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = onResetCatalogs,
                        )
                    }
                    items(snapshot.catalogs, key = { "catalog:${it.key}" }) { catalog ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SettingsActionRow(
                                title = catalog.title,
                                subtitle = catalog.sourceName,
                                value = if (catalog.visible) "Visible" else "Hidden",
                                checked = catalog.visible,
                                anchorKey = "settings:catalog-order:${catalog.key}",
                                category = category,
                                memory = memory,
                                scopeKey = scopeKey,
                                focusRegistry = focusRegistry,
                                onClick = { onToggleCatalog(catalog) },
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SettingsCompactAction(
                                    label = "Move up",
                                    onClick = { onMoveCatalog(catalog, -1) },
                                    modifier = Modifier.weight(1f),
                                )
                                SettingsCompactAction(
                                    label = "Move down",
                                    onClick = { onMoveCatalog(catalog, 1) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                TvSettingsCategory.INTEGRATIONS -> {
                    item("integration-tmdb-credential") {
                        SettingsActionRow(
                            title = "TMDB credential",
                            subtitle = "Enter or replace the API key or read token on this TV.",
                            value = if (snapshot.tmdbConfigured) "Configured" else "Set up",
                            anchorKey = settingsFirstDetailAnchorKey(category),
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = onEditTmdb,
                        )
                    }
                    item("integration-tmdb") {
                        SettingsActionRow(
                            title = "TMDB enrichment",
                            subtitle = if (snapshot.tmdbConfigured) "Metadata credential is configured." else "Set the credential above first.",
                            value = if (!snapshot.tmdbConfigured) "Not configured" else onOff(snapshot.tmdbEnabled),
                            checked = snapshot.tmdbEnabled,
                            enabled = snapshot.tmdbConfigured,
                            anchorKey = "settings:integrations:tmdb-toggle",
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = onToggleTmdb,
                        )
                    }
                    item("integration-mdblist-credential") {
                        SettingsActionRow(
                            title = "MDBList API key",
                            subtitle = "Enter or replace the API key on this TV.",
                            value = if (snapshot.mdbListConfigured) "Configured" else "Set up",
                            anchorKey = "settings:integrations:mdblist-key",
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = onEditMdbList,
                        )
                    }
                    item("integration-mdblist") {
                        SettingsActionRow(
                            title = "MDBList ratings",
                            subtitle = if (snapshot.mdbListConfigured) "Ratings credential is configured." else "Set the API key above first.",
                            value = if (!snapshot.mdbListConfigured) "Not configured" else onOff(snapshot.mdbListEnabled),
                            checked = snapshot.mdbListEnabled,
                            enabled = snapshot.mdbListConfigured,
                            anchorKey = "settings:integrations:mdblist",
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = onToggleMdbList,
                        )
                    }
                    items(TV_MDBLIST_PROVIDERS, key = { "mdblist:${it.first}" }) { (id, label) ->
                        val enabled = snapshot.mdbListProviders[id] == true
                        SettingsActionRow(
                            title = "MDBList: $label",
                            subtitle = "Choose whether this rating is shown on Details.",
                            value = onOff(enabled),
                            checked = enabled,
                            enabled = snapshot.mdbListConfigured && snapshot.mdbListEnabled,
                            anchorKey = "settings:integrations:mdblist:$id",
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = { onToggleMdbListProvider(id) },
                        )
                    }
                    item("integration-trakt") {
                        SettingsActionRow(
                            title = "Trakt",
                            subtitle = if (snapshot.traktConnected) {
                                snapshot.traktUsername.takeIf { it.isNotBlank() }?.let { "Connected as $it" } ?: "Connected"
                            } else {
                                "Enter your Trakt application credentials and connect with a device code."
                            },
                            value = if (snapshot.traktConnected) "Disconnect" else "Set up",
                            anchorKey = "settings:integrations:trakt",
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = if (snapshot.traktConnected) onDisconnectTrakt else onEditTrakt,
                        )
                    }
                    if (snapshot.statusMessage.isNotBlank()) {
                        item("integration-status") {
                            SettingsInfoCard("Status", listOf("Result" to snapshot.statusMessage))
                        }
                    }
                }

                TvSettingsCategory.LOCAL_DATA -> {
                    item("data-copy") {
                        SettingsActionRow(
                            title = "Copy backup",
                            subtitle = "Copy addons, library, watch progress and UI settings. API keys are excluded.",
                            value = "Copy",
                            anchorKey = settingsFirstDetailAnchorKey(category),
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = onCopyBackup,
                        )
                    }
                    item("data-restore") {
                        SettingsActionRow(
                            title = "Restore backup",
                            subtitle = "Restore a Nustrim backup currently held in the Android clipboard.",
                            value = "Restore",
                            anchorKey = "settings:data:restore",
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = onRestoreBackup,
                        )
                    }
                    if (snapshot.statusMessage.isNotBlank()) {
                        item("data-status") {
                            SettingsInfoCard("Status", listOf("Result" to snapshot.statusMessage))
                        }
                    }
                }

                TvSettingsCategory.ADVANCED -> {
                    item("advanced-developer") {
                        SettingsActionRow(
                            title = "Developer mode",
                            subtitle = "Expose developer sources and diagnostic controls.",
                            value = onOff(snapshot.developerMode),
                            checked = snapshot.developerMode,
                            anchorKey = settingsFirstDetailAnchorKey(category),
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = onToggleDeveloperMode,
                        )
                    }
                    item("advanced-diagnostics") {
                        SettingsActionRow(
                            title = "Diagnostic logging",
                            subtitle = "Keep additional runtime details for troubleshooting.",
                            value = onOff(snapshot.developerDiagnostics),
                            checked = snapshot.developerDiagnostics,
                            enabled = snapshot.developerMode,
                            anchorKey = "settings:advanced:diagnostics",
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = onToggleDeveloperDiagnostics,
                        )
                    }
                    item("advanced-copy-log") {
                        SettingsActionRow(
                            title = "Copy diagnostics log",
                            subtitle = "${snapshot.diagnosticsLineCount} runtime diagnostic line(s).",
                            value = "Copy",
                            enabled = snapshot.developerMode && snapshot.diagnosticsLineCount > 0,
                            anchorKey = "settings:advanced:copy-log",
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = onCopyDiagnostics,
                        )
                    }
                    item("advanced-clear-log") {
                        SettingsActionRow(
                            title = "Clear diagnostics log",
                            subtitle = "Remove the in-memory troubleshooting log.",
                            value = "Clear",
                            enabled = snapshot.developerMode && snapshot.diagnosticsLineCount > 0,
                            anchorKey = "settings:advanced:clear-log",
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = onClearDiagnostics,
                        )
                    }
                }

                TvSettingsCategory.ABOUT -> {
                    item("about-interface") {
                        SettingsActionRow(
                            title = "Switch to Mobile interface",
                            subtitle = "Leave the remote-first TV layout and open the touch-first interface.",
                            value = "Switch",
                            anchorKey = settingsFirstDetailAnchorKey(category),
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = onSwitchToMobile,
                        )
                    }
                    item("about-version") {
                        SettingsInfoCard(
                            title = "Nustrim",
                            lines = listOf(
                                "Version" to BuildConfig.VERSION_NAME,
                                "Package" to BuildConfig.APPLICATION_ID,
                                "Developer" to "NudroidLabs",
                                "Player" to "AndroidX Media3",
                            ),
                        )
                    }
                    item("about-update") {
                        SettingsActionRow(
                            title = updateTitle(updateState),
                            subtitle = updateSubtitle(updateState),
                            value = updateValue(updateState),
                            enabled = true,
                            loading = updateState is TvSettingsUpdateState.Checking ||
                                updateState is TvSettingsUpdateState.Downloading,
                            anchorKey = "settings:about:update",
                            category = category,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onClick = onUpdateAction,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    value: String,
    anchorKey: String,
    category: TvSettingsCategory,
    memory: TvSettingsMemory,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    onClick: () -> Unit,
    checked: Boolean? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val anchor = rememberTvFocusAnchor(focusRegistry, scopeKey, anchorKey)
    var focused by remember { mutableStateOf(false) }
    val scale = animateTvFocusScale(
        focused = focused,
        focusedScale = TvTokens.SubtleFocusScale,
        label = "settings-row-scale",
    )
    val foreground = when {
        !enabled -> Color(0xFF71747C)
        focused -> Color(0xFF101114)
        else -> Color.White
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(13.dp))
            .background(if (focused) Color.White else Color(0xFF1B1D22))
            .border(1.dp, if (focused) Color.White else Color(0xFF30333A), RoundedCornerShape(13.dp))
            .tvFocusAnchor(anchor)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) memory.lastDetailAnchor[category] = anchorKey
            }
            .onKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft -> {
                        focusRegistry.requestAnchor(scopeKey, settingsRailAnchorKey(category))
                        true
                    }
                    enabled && event.type == KeyEventType.KeyDown &&
                        (event.key == Key.DirectionCenter || event.key == Key.Enter) -> {
                        onClick()
                        true
                    }
                    else -> false
                }
            }
            .clickable(enabled = enabled, onClick = onClick)
            .focusable()
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = foreground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = if (focused && enabled) Color(0xFF4D5057) else Color(0xFF8D9099),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = foreground, strokeWidth = 2.dp)
        } else if (checked != null) {
            SettingsToggleIndicator(checked = checked, focused = focused, enabled = enabled)
        }
        if (value.isNotBlank()) {
            Text(
                text = value,
                color = foreground,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SettingsToggleIndicator(checked: Boolean, focused: Boolean, enabled: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 22.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(
                when {
                    !enabled -> Color(0xFF393B41)
                    checked && focused -> Color(0xFF101114)
                    checked -> Color(0xFFE8E8EA)
                    else -> Color(0xFF4A4D54)
                },
            )
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .size(16.dp)
                .background(
                    if (checked && !focused) Color(0xFF101114) else Color.White,
                    RoundedCornerShape(99.dp),
                ),
        )
    }
}

@Composable
private fun SettingsCompactAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Text(label, maxLines = 1)
    }
}

@Composable
private fun SettingsInfoCard(title: String, lines: List<Pair<String, String>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0xFF1B1D22))
            .border(1.dp, Color(0xFF30333A), RoundedCornerShape(13.dp))
            .padding(18.dp),
    ) {
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        lines.forEach { (label, value) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(label, color = Color(0xFF8D9099), fontSize = 12.sp, modifier = Modifier.width(100.dp))
                Text(value, color = Color(0xFFE5E5E7), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun settingsCategoryIcon(category: TvSettingsCategory): ImageVector = when (category) {
    TvSettingsCategory.PLAYBACK -> Icons.Default.PlayArrow
    TvSettingsCategory.SUBTITLES -> Icons.Default.ClosedCaption
    TvSettingsCategory.CONTENT -> Icons.Default.GridView
    TvSettingsCategory.CATALOG_ORDER -> Icons.Default.Reorder
    TvSettingsCategory.INTEGRATIONS -> Icons.Default.Link
    TvSettingsCategory.LOCAL_DATA -> Icons.Default.Save
    TvSettingsCategory.ADVANCED -> Icons.Default.Build
    TvSettingsCategory.ABOUT -> Icons.Default.Info
}

private fun settingsRailAnchorKey(category: TvSettingsCategory): String = "settings:rail:${category.name}"
private fun settingsFirstDetailAnchorKey(category: TvSettingsCategory): String = "settings:detail:${category.name}:first"

private fun subtitleLanguageLabel(code: String): String =
    TV_SUBTITLE_LANGUAGES.firstOrNull { it.first == code }?.second ?: code.uppercase()

private fun sourceDisplayName(source: InstalledSource): String = when {
    source.label.isNotBlank() -> source.label
    source.preset == SourcePreset.CORE_DEFAULT -> "Core source"
    source.preset == SourcePreset.DEVELOPER_DEFAULT -> "Developer source"
    else -> source.url.substringAfter("://").substringBefore('/').ifBlank { "Source" }
}

private fun onOff(value: Boolean): String = if (value) "On" else "Off"

private fun updateValue(state: TvSettingsUpdateState): String = when (state) {
    TvSettingsUpdateState.Idle -> "Check"
    TvSettingsUpdateState.Checking -> ""
    TvSettingsUpdateState.UpToDate -> "Up to date"
    is TvSettingsUpdateState.Available -> state.info.versionName
    is TvSettingsUpdateState.Downloading -> "${state.progress}%"
    is TvSettingsUpdateState.PermissionRequired -> "Allow install"
    is TvSettingsUpdateState.ReadyToInstall -> "Install"
    is TvSettingsUpdateState.Error -> "Try again"
}

private fun updateTitle(state: TvSettingsUpdateState): String = when (state) {
    is TvSettingsUpdateState.Available -> "Download update"
    is TvSettingsUpdateState.Downloading -> "Downloading update"
    is TvSettingsUpdateState.PermissionRequired -> "Allow app installs"
    is TvSettingsUpdateState.ReadyToInstall -> "Install update"
    else -> "Check for updates"
}

private fun updateSubtitle(state: TvSettingsUpdateState): String = when (state) {
    TvSettingsUpdateState.Idle -> "Compare this build with the latest signed GitHub release."
    TvSettingsUpdateState.Checking -> "Checking the Nustrim release manifest..."
    TvSettingsUpdateState.UpToDate -> "This is the latest available build."
    is TvSettingsUpdateState.Available -> state.info.changelog.ifBlank { "A newer signed build is available." }
    is TvSettingsUpdateState.Downloading -> "Downloading and verifying the APK SHA-256 digest."
    is TvSettingsUpdateState.PermissionRequired -> "Allow Nustrim to install updates, then return here."
    is TvSettingsUpdateState.ReadyToInstall -> "The verified APK is ready. Open the Android installer."
    is TvSettingsUpdateState.Error -> state.message
}

private val SETTINGS_BACKGROUND = Color(0xFF08090B)
