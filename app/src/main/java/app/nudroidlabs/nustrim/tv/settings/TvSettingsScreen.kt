package app.nudroidlabs.nustrim.tv.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.nustrim.BuildConfig
import app.nudroidlabs.nustrim.core.source.InstalledSourceStore
import app.nudroidlabs.nustrim.tv.theme.TvColors
import app.nudroidlabs.nustrim.ui.SubtitleDisplayMode
import app.nudroidlabs.nustrim.ui.UiPreferences
import kotlinx.coroutines.delay

@Composable
fun TvSettingsScreen(
    contentFocusRequestToken: Int,
    firstContentRequester: FocusRequester,
    onContentFocused: (FocusRequester) -> Unit,
    onMoveLeft: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember(context) { UiPreferences(context) }
    val sourceStore = remember(context) { InstalledSourceStore(context) }

    var autoplayFirst by remember {
        mutableStateOf(preferences.autoplayFirstSource)
    }
    var autoplayNext by remember {
        mutableStateOf(preferences.autoplayNextEpisode)
    }
    var showAllSubtitles by remember {
        mutableStateOf(
            preferences.subtitleDisplayMode == SubtitleDisplayMode.SHOW_ALL
        )
    }

    val installedSources = remember {
        sourceStore.sources()
    }
    var sourceStates by remember {
        mutableStateOf(
            installedSources.associate { source ->
                source.url to source.enabled
            }
        )
    }

    LaunchedEffect(contentFocusRequestToken) {
        if (contentFocusRequestToken > 0) {
            delay(40)
            runCatching { firstContentRequester.requestFocus() }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 36.dp,
            end = 50.dp,
            top = 38.dp,
            bottom = 42.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            Column(
                modifier = Modifier.padding(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "Settings",
                    color = TvColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp
                )
                Text(
                    text = "TV playback and subtitle preferences.",
                    color = TvColors.TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        item(key = "playback-title") {
            TvSettingsSectionTitle("Playback")
        }

        item(key = "autoplay-source") {
            TvSettingsToggleRow(
                title = "Autoplay first source",
                summary = "Start the first playable result automatically.",
                icon = Icons.Outlined.PlayArrow,
                checked = autoplayFirst,
                focusRequester = firstContentRequester,
                onFocused = onContentFocused,
                onMoveLeft = onMoveLeft,
                onToggle = {
                    autoplayFirst = !autoplayFirst
                    preferences.autoplayFirstSource = autoplayFirst
                }
            )
        }

        item(key = "autoplay-next") {
            TvSettingsToggleRow(
                title = "Autoplay next episode",
                summary = "Continue to the next episode when available.",
                icon = Icons.Outlined.PlayArrow,
                checked = autoplayNext,
                onFocused = onContentFocused,
                onMoveLeft = onMoveLeft,
                onToggle = {
                    autoplayNext = !autoplayNext
                    preferences.autoplayNextEpisode = autoplayNext
                }
            )
        }

        item(key = "subtitle-title") {
            TvSettingsSectionTitle("Subtitles")
        }

        item(key = "subtitle-mode") {
            TvSettingsToggleRow(
                title = "Show all subtitle languages",
                summary = if (showAllSubtitles) {
                    "All available subtitles are shown."
                } else {
                    "Prefer ${preferences.subtitlePreferredLanguage.uppercase()} then ${preferences.subtitleSecondPreferredLanguage.uppercase()}."
                },
                icon = Icons.Outlined.Info,
                checked = showAllSubtitles,
                onFocused = onContentFocused,
                onMoveLeft = onMoveLeft,
                onToggle = {
                    showAllSubtitles = !showAllSubtitles
                    preferences.subtitleDisplayMode = if (showAllSubtitles) {
                        SubtitleDisplayMode.SHOW_ALL
                    } else {
                        SubtitleDisplayMode.PREFERRED_ONLY
                    }
                }
            )
        }

        item(key = "source-title") {
            TvSettingsSectionTitle("Sources")
        }

        installedSources.forEachIndexed { index, source ->
            item(key = "source:${source.url}") {
                val checked = sourceStates[source.url] ?: source.enabled
                TvSettingsToggleRow(
                    title = source.label.ifBlank {
                        "Source ${index + 1}"
                    },
                    summary = buildString {
                        if (source.developerOnly) {
                            append("Developer · ")
                        }
                        append(source.url)
                    },
                    icon = Icons.Outlined.Settings,
                    checked = checked,
                    onFocused = onContentFocused,
                    onMoveLeft = onMoveLeft,
                    onToggle = {
                        val next = !checked
                        sourceStore.setEnabled(source.url, next)
                        sourceStates = sourceStates + (source.url to next)
                    }
                )
            }
        }

        if (installedSources.isEmpty()) {
            item(key = "source-empty") {
                TvSettingsInfoRow(
                    title = "No sources installed",
                    summary = "Install a source from the mobile source manager.",
                    icon = Icons.Outlined.Info,
                    onFocused = onContentFocused,
                    onMoveLeft = onMoveLeft
                )
            }
        }

        item(key = "app-title") {
            TvSettingsSectionTitle("About")
        }

        item(key = "about") {
            TvSettingsInfoRow(
                title = "Nustrim TV",
                summary = "Version ${BuildConfig.VERSION_NAME} · Remote-first interface",
                icon = Icons.Outlined.Info,
                onFocused = onContentFocused,
                onMoveLeft = onMoveLeft
            )
        }
    }
}

@Composable
private fun TvSettingsSectionTitle(
    title: String
) {
    Text(
        text = title.uppercase(),
        color = TvColors.TextSecondary.copy(alpha = 0.72f),
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.1.sp,
        modifier = Modifier.padding(
            top = 8.dp,
            bottom = 2.dp
        )
    )
}

@Composable
private fun TvSettingsToggleRow(
    title: String,
    summary: String,
    icon: ImageVector,
    checked: Boolean,
    focusRequester: FocusRequester? = null,
    onFocused: (FocusRequester) -> Unit,
    onMoveLeft: (() -> Unit)? = null,
    onToggle: () -> Unit
) {
    val requester = remember(title, focusRequester) {
        focusRequester ?: FocusRequester()
    }
    var focused by remember(title) { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(requester)
            .onFocusChanged { state ->
                focused = state.hasFocus
                if (state.hasFocus) onFocused(requester)
            }
            .onPreviewKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionLeft &&
                        onMoveLeft != null -> {
                        onMoveLeft()
                        true
                    }

                    event.type == KeyEventType.KeyDown &&
                        (
                            event.key == Key.DirectionCenter ||
                                event.key == Key.Enter
                            ) -> {
                        onToggle()
                        true
                    }

                    else -> false
                }
            }
            .focusable(),
        color = if (focused) {
            TvColors.FocusBackground
        } else {
            TvColors.Surface
        },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) {
                TvColors.FocusRing
            } else {
                Color.White.copy(alpha = 0.08f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 18.dp,
                    vertical = 14.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TvColors.TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    color = TvColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    text = summary,
                    color = TvColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = null
            )
        }
    }
}

@Composable
private fun TvSettingsInfoRow(
    title: String,
    summary: String,
    icon: ImageVector,
    onFocused: (FocusRequester) -> Unit,
    onMoveLeft: () -> Unit
) {
    val requester = remember(title) { FocusRequester() }
    var focused by remember(title) { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(requester)
            .onFocusChanged { state ->
                focused = state.hasFocus
                if (state.hasFocus) onFocused(requester)
            }
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionLeft
                ) {
                    onMoveLeft()
                    true
                } else {
                    false
                }
            }
            .focusable(),
        color = if (focused) {
            TvColors.FocusBackground
        } else {
            TvColors.Surface
        },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) {
                TvColors.FocusRing
            } else {
                Color.White.copy(alpha = 0.08f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 18.dp,
                    vertical = 15.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TvColors.TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    color = TvColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    text = summary,
                    color = TvColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
