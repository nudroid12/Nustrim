package app.nudroidlabs.nustrim.tv.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.nustrim.core.library.LocalMediaStore
import app.nudroidlabs.nustrim.tv.common.TvMediaGridCard
import app.nudroidlabs.nustrim.tv.home.TvHomeEntry
import app.nudroidlabs.nustrim.tv.theme.TvColors
import kotlinx.coroutines.delay

private const val LIBRARY_FOCUS_RESTORE_MS = 60L

@Composable
fun TvLibraryScreen(
    contentFocusRequestToken: Int,
    refreshToken: Int,
    firstContentRequester: FocusRequester,
    onContentFocused: (FocusRequester) -> Unit,
    onMoveLeft: () -> Unit,
    onOpen: (TvHomeEntry) -> Unit
) {
    val context = LocalContext.current
    val mediaStore = remember(context) { LocalMediaStore(context) }
    val gridState = rememberLazyGridState()
    val requesterByKey = remember { mutableMapOf<String, FocusRequester>() }

    var localRevision by remember { mutableIntStateOf(0) }
    var focusRestoreRevision by remember { mutableIntStateOf(0) }
    var lastFocusedKey by remember { mutableStateOf<String?>(null) }
    var pendingFocusKey by remember { mutableStateOf<String?>(null) }
    var optionsEntry by remember { mutableStateOf<TvHomeEntry?>(null) }

    val entries = remember(refreshToken, localRevision) {
        mediaStore.saved()
            .distinctBy { it.key }
            .map { local ->
                TvHomeEntry(
                    sourceUrl = local.sourceUrl,
                    session = null,
                    item = local.toMediaItem(),
                    catalogName = "Library",
                    continueEntry = local.takeIf { it.hasContinueState }
                )
            }
    }

    fun restoreLibraryFocus(key: String?) {
        pendingFocusKey = key
        focusRestoreRevision += 1
    }

    LaunchedEffect(
        contentFocusRequestToken,
        entries.size,
        focusRestoreRevision
    ) {
        val explicitRestore = focusRestoreRevision > 0
        if (contentFocusRequestToken <= 0 && !explicitRestore) return@LaunchedEffect

        val preferredKey = pendingFocusKey ?: lastFocusedKey
        val restoreIndex = preferredKey
            ?.let { key -> entries.indexOfFirst { it.stableKey == key } }
            ?: -1

        if (restoreIndex >= 0 && preferredKey != null) {
            gridState.scrollToItem(restoreIndex)
            delay(LIBRARY_FOCUS_RESTORE_MS)
            val requester = requesterByKey[preferredKey]
            if (requester != null) {
                runCatching { requester.requestFocus() }
            } else {
                runCatching { firstContentRequester.requestFocus() }
            }
        } else {
            delay(40)
            runCatching { firstContentRequester.requestFocus() }
        }

        pendingFocusKey = null
    }

    BackHandler(enabled = optionsEntry != null) {
        val key = optionsEntry?.stableKey
        optionsEntry = null
        restoreLibraryFocus(key)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background)
            .padding(
                start = 36.dp,
                end = 34.dp,
                top = 38.dp,
                bottom = 30.dp
            ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = "Library",
                color = TvColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp
            )
            Text(
                text = if (entries.isEmpty()) {
                    "Your saved titles will appear here."
                } else {
                    "${entries.size} saved title${if (entries.size == 1) "" else "s"}"
                },
                color = TvColors.TextSecondary,
                fontSize = 13.sp
            )
        }

        if (entries.isEmpty()) {
            TvLibraryEmpty(
                focusRequester = firstContentRequester,
                onFocused = onContentFocused,
                onMoveLeft = onMoveLeft
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 4.dp,
                    end = 10.dp,
                    bottom = 36.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                itemsIndexed(
                    items = entries,
                    key = { _, entry -> entry.stableKey }
                ) { index, entry ->
                    val requester = if (index == 0) {
                        firstContentRequester
                    } else {
                        remember(entry.stableKey) { FocusRequester() }
                    }
                    requesterByKey[entry.stableKey] = requester

                    val watched = remember(
                        localRevision,
                        entry.stableKey
                    ) {
                        mediaStore.isWatched(
                            entry.sourceUrl,
                            entry.item
                        )
                    }

                    val continueEntry = entry.continueEntry

                    TvMediaGridCard(
                        entry = entry,
                        focusRequester = requester,
                        badgeText = when {
                            watched -> "WATCHED"
                            continueEntry?.nextUp == true -> "NEXT UP"
                            else -> null
                        },
                        progressFraction = continueEntry
                            ?.takeIf { it.hasProgress }
                            ?.progressFraction,
                        onFocused = { focusedRequester ->
                            lastFocusedKey = entry.stableKey
                            onContentFocused(focusedRequester)
                        },
                        onMoveLeft = if (index % 6 == 0) onMoveLeft else null,
                        onLongPress = {
                            optionsEntry = it
                        },
                        onOpen = onOpen
                    )
                }
            }
        }
    }

    optionsEntry?.let { entry ->
        val watched = remember(localRevision, entry.stableKey) {
            mediaStore.isWatched(entry.sourceUrl, entry.item)
        }
        val hasContinue = entry.continueEntry?.hasContinueState == true

        TvLibraryOptionsOverlay(
            entry = entry,
            watched = watched,
            hasContinue = hasContinue,
            onDismiss = {
                optionsEntry = null
                restoreLibraryFocus(entry.stableKey)
            },
            onOpen = {
                optionsEntry = null
                onOpen(entry)
            },
            onToggleWatched = {
                mediaStore.setWatched(
                    sourceUrl = entry.sourceUrl,
                    item = entry.item,
                    watched = !watched
                )
                localRevision += 1
                optionsEntry = null
                restoreLibraryFocus(entry.stableKey)
            },
            onClearContinue = if (hasContinue) {
                {
                    mediaStore.clearContinueWatching(
                        sourceUrl = entry.sourceUrl,
                        item = entry.item
                    )
                    localRevision += 1
                    optionsEntry = null
                    restoreLibraryFocus(entry.stableKey)
                }
            } else {
                null
            },
            onRemoveLibrary = {
                val currentIndex = entries.indexOfFirst {
                    it.stableKey == entry.stableKey
                }
                val replacementKey = entries
                    .getOrNull(currentIndex + 1)
                    ?.stableKey
                    ?: entries
                        .getOrNull(currentIndex - 1)
                        ?.stableKey

                mediaStore.setSaved(
                    sourceUrl = entry.sourceUrl,
                    item = entry.item,
                    saved = false
                )
                localRevision += 1
                optionsEntry = null
                lastFocusedKey = replacementKey
                restoreLibraryFocus(replacementKey)
            }
        )
    }
}

@Composable
private fun TvLibraryOptionsOverlay(
    entry: TvHomeEntry,
    watched: Boolean,
    hasContinue: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onToggleWatched: () -> Unit,
    onClearContinue: (() -> Unit)?,
    onRemoveLibrary: () -> Unit
) {
    val firstRequester = remember(entry.stableKey) { FocusRequester() }

    LaunchedEffect(entry.stableKey) {
        delay(40)
        runCatching { firstRequester.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 54.dp)
                .width(360.dp),
            color = TvColors.BackgroundElevated.copy(alpha = 0.98f),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = entry.item.title,
                    color = TvColors.TextPrimary,
                    fontSize = 19.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        entry.continueEntry?.nextUp == true -> "Next up"
                        entry.continueEntry?.hasProgress == true -> "Continue watching"
                        else -> "Library"
                    },
                    color = TvColors.TextSecondary,
                    fontSize = 12.sp
                )

                TvLibraryOptionButton(
                    label = "Open details",
                    focusRequester = firstRequester,
                    onClick = onOpen
                )
                TvLibraryOptionButton(
                    label = if (watched) "Mark as unwatched" else "Mark as watched",
                    onClick = onToggleWatched
                )
                if (hasContinue && onClearContinue != null) {
                    TvLibraryOptionButton(
                        label = "Remove from Continue Watching",
                        onClick = onClearContinue
                    )
                }
                TvLibraryOptionButton(
                    label = "Remove from Library",
                    onClick = onRemoveLibrary
                )
                TvLibraryOptionButton(
                    label = "Cancel",
                    onClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun TvLibraryOptionButton(
    label: String,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var focused by remember(label) { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                }
            )
            .onFocusChanged { focused = it.hasFocus }
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    (
                        event.key == Key.DirectionCenter ||
                            event.key == Key.Enter
                        )
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .focusable(),
        color = if (focused) TvColors.FocusBackground else TvColors.Surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) TvColors.FocusRing
            else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 15.dp,
                    vertical = 12.dp
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = label,
                color = TvColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TvLibraryEmpty(
    focusRequester: FocusRequester,
    onFocused: (FocusRequester) -> Unit,
    onMoveLeft: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    focused = state.hasFocus
                    if (state.hasFocus) onFocused(focusRequester)
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
            color = TvColors.Surface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(
                if (focused) 2.dp else 1.dp,
                if (focused) {
                    TvColors.FocusRing
                } else {
                    Color.White.copy(alpha = 0.10f)
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 42.dp,
                    vertical = 34.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = TvColors.TextSecondary
                )
                Text(
                    text = "Nothing saved yet",
                    color = TvColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                )
                Text(
                    text = "Open a title and choose Add to Library.",
                    color = TvColors.TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}
