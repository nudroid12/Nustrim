package app.nudroidlabs.nustrim.tv.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.nustrim.core.library.LocalMediaStore
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.source.InstalledSourceStore
import app.nudroidlabs.nustrim.core.source.SearchableSourceSession
import app.nudroidlabs.nustrim.core.source.SourceEngine
import app.nudroidlabs.nustrim.tv.common.TvMediaGridCard
import app.nudroidlabs.nustrim.tv.home.TvHomeEntry
import app.nudroidlabs.nustrim.tv.theme.TvColors
import kotlinx.coroutines.delay

private const val SEARCH_FOCUS_RESTORE_MS = 60L

@Composable
fun TvSearchScreen(
    contentFocusRequestToken: Int,
    firstContentRequester: FocusRequester,
    onContentFocused: (FocusRequester) -> Unit,
    onMoveLeft: () -> Unit,
    onOpen: (TvHomeEntry) -> Unit
) {
    val context = LocalContext.current
    val engine = remember(context) { SourceEngine(context) }
    val sourceStore = remember(context) { InstalledSourceStore(context) }
    val mediaStore = remember(context) { LocalMediaStore(context) }
    val searchButtonRequester = remember { FocusRequester() }
    val resultGridState = rememberLazyGridState()
    val requesterByKey = remember { mutableMapOf<String, FocusRequester>() }

    var query by remember { mutableStateOf("") }
    var submittedQuery by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var searchableCount by remember { mutableIntStateOf(0) }
    var failureCount by remember { mutableIntStateOf(0) }
    var searchGeneration by remember { mutableIntStateOf(0) }
    var localMediaRevision by remember { mutableIntStateOf(0) }
    var focusRestoreRevision by remember { mutableIntStateOf(0) }
    var lastFocusedResultKey by remember { mutableStateOf<String?>(null) }
    var pendingFocusKey by remember { mutableStateOf<String?>(null) }
    var optionsEntry by remember { mutableStateOf<TvHomeEntry?>(null) }

    val results = remember { mutableStateListOf<TvHomeEntry>() }

    fun mediaIdentity(item: MediaItem): String {
        val id = item.ref?.metaId
            ?.takeIf { it.isNotBlank() }
            ?: item.id
        return "${item.type}|$id"
    }

    fun restoreResultFocus(key: String?) {
        pendingFocusKey = key
        focusRestoreRevision += 1
    }

    fun runSearch() {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return

        searchGeneration += 1
        val generation = searchGeneration
        submittedQuery = cleanQuery
        loading = true
        searchableCount = 0
        failureCount = 0
        lastFocusedResultKey = null
        requesterByKey.clear()
        results.clear()

        val installed = sourceStore.sources()
            .filter { it.enabled && !it.developerOnly }
            .map { it.url }
            .distinct()

        if (installed.isEmpty()) {
            loading = false
            return
        }

        var pending = installed.size

        fun finishOne() {
            if (generation != searchGeneration) return
            pending -= 1
            if (pending <= 0) loading = false
        }

        installed.forEach { sourceUrl ->
            engine.open(
                sourceUrl,
                onSuccess = success@{ session ->
                    if (generation != searchGeneration) return@success
                    val searchable = session as? SearchableSourceSession
                    if (
                        searchable == null ||
                        !session.capabilities.searchable
                    ) {
                        finishOne()
                    } else {
                        searchableCount += 1
                        searchable.search(
                            query = cleanQuery,
                            onSuccess = { catalog ->
                                if (generation == searchGeneration) {
                                    catalog.items.forEach { item ->
                                        val identity = mediaIdentity(item)
                                        if (
                                            results.none {
                                                mediaIdentity(it.item) == identity
                                            }
                                        ) {
                                            results += TvHomeEntry(
                                                sourceUrl = sourceUrl,
                                                session = session,
                                                item = item,
                                                catalogName = catalog.name
                                            )
                                        }
                                    }
                                }
                                finishOne()
                            },
                            onError = {
                                if (generation == searchGeneration) {
                                    failureCount += 1
                                }
                                finishOne()
                            }
                        )
                    }
                },
                onError = {
                    if (generation == searchGeneration) {
                        failureCount += 1
                    }
                    finishOne()
                }
            )
        }
    }

    LaunchedEffect(
        contentFocusRequestToken,
        results.size,
        focusRestoreRevision
    ) {
        val explicitRestore = focusRestoreRevision > 0 && pendingFocusKey != null
        if (contentFocusRequestToken <= 0 && !explicitRestore) return@LaunchedEffect

        val preferredKey = pendingFocusKey ?: lastFocusedResultKey
        val restoreIndex = preferredKey
            ?.let { key -> results.indexOfFirst { it.stableKey == key } }
            ?: -1

        if (restoreIndex >= 0 && preferredKey != null) {
            resultGridState.scrollToItem(restoreIndex)
            delay(SEARCH_FOCUS_RESTORE_MS)
            val restored = requesterByKey[preferredKey]
            if (restored != null) {
                runCatching { restored.requestFocus() }
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
        restoreResultFocus(key)
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
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = "Search",
                color = TvColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp
            )
            Text(
                text = "Search every enabled catalog addon.",
                color = TvColors.TextSecondary,
                fontSize = 13.sp
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = {
                    Text("Movie, series or anime")
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { runSearch() }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = TvColors.Surface,
                    unfocusedContainerColor = TvColors.Surface,
                    disabledContainerColor = TvColors.Surface,
                    focusedTextColor = TvColors.TextPrimary,
                    unfocusedTextColor = TvColors.TextPrimary,
                    focusedIndicatorColor = TvColors.FocusRing,
                    unfocusedIndicatorColor = Color.White.copy(alpha = 0.16f),
                    cursorColor = TvColors.FocusRing
                ),
                modifier = Modifier
                    .width(540.dp)
                    .height(56.dp)
                    .focusRequester(firstContentRequester)
                    .onFocusChanged { state ->
                        if (state.hasFocus) {
                            onContentFocused(firstContentRequester)
                        }
                    }
                    .onPreviewKeyEvent { event ->
                        if (
                            event.type == KeyEventType.KeyDown &&
                            event.key == Key.DirectionLeft &&
                            query.isBlank()
                        ) {
                            onMoveLeft()
                            true
                        } else {
                            false
                        }
                    }
            )

            TvSearchButton(
                enabled = query.isNotBlank() && !loading,
                focusRequester = searchButtonRequester,
                onFocused = onContentFocused,
                onClick = { runSearch() }
            )
        }

        when {
            loading -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(20.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Searching ${searchableCount.coerceAtLeast(1)} addon(s)...",
                        color = TvColors.TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            submittedQuery.isNotBlank() -> {
                Text(
                    text = buildString {
                        append("${results.size} result")
                        if (results.size != 1) append("s")
                        append(" for \"$submittedQuery\"")
                        if (failureCount > 0) {
                            append(" · $failureCount source failure")
                            if (failureCount != 1) append("s")
                        }
                    },
                    color = TvColors.TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        if (!loading && submittedQuery.isNotBlank() && results.isEmpty()) {
            TvSearchEmpty()
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                state = resultGridState,
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
                    items = results,
                    key = { _, entry -> entry.stableKey }
                ) { index, entry ->
                    val requester = remember(entry.stableKey) {
                        FocusRequester()
                    }
                    requesterByKey[entry.stableKey] = requester

                    val watched = remember(localMediaRevision, entry.stableKey) {
                        mediaStore.isWatched(entry.sourceUrl, entry.item)
                    }
                    val saved = remember(localMediaRevision, entry.stableKey) {
                        mediaStore.isSaved(entry.sourceUrl, entry.item)
                    }

                    TvMediaGridCard(
                        entry = entry,
                        focusRequester = requester,
                        badgeText = when {
                            watched -> "WATCHED"
                            saved -> "LIBRARY"
                            else -> null
                        },
                        onFocused = { focusedRequester ->
                            lastFocusedResultKey = entry.stableKey
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
        val saved = remember(localMediaRevision, entry.stableKey) {
            mediaStore.isSaved(entry.sourceUrl, entry.item)
        }
        val watched = remember(localMediaRevision, entry.stableKey) {
            mediaStore.isWatched(entry.sourceUrl, entry.item)
        }

        TvSearchOptionsOverlay(
            entry = entry,
            saved = saved,
            watched = watched,
            onDismiss = {
                optionsEntry = null
                restoreResultFocus(entry.stableKey)
            },
            onOpen = {
                optionsEntry = null
                onOpen(entry)
            },
            onToggleSaved = {
                mediaStore.setSaved(
                    sourceUrl = entry.sourceUrl,
                    item = entry.item,
                    saved = !saved
                )
                localMediaRevision += 1
                optionsEntry = null
                restoreResultFocus(entry.stableKey)
            },
            onToggleWatched = {
                mediaStore.setWatched(
                    sourceUrl = entry.sourceUrl,
                    item = entry.item,
                    watched = !watched
                )
                localMediaRevision += 1
                optionsEntry = null
                restoreResultFocus(entry.stableKey)
            }
        )
    }
}

@Composable
private fun TvSearchButton(
    enabled: Boolean,
    focusRequester: FocusRequester,
    onFocused: (FocusRequester) -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .height(54.dp)
            .width(126.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                focused = state.hasFocus
                if (state.hasFocus) onFocused(focusRequester)
            }
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    (
                        event.key == Key.DirectionCenter ||
                            event.key == Key.Enter
                        )
                ) {
                    if (enabled) onClick()
                    true
                } else {
                    false
                }
            }
            .focusable(),
        shape = RoundedCornerShape(12.dp),
        color = when {
            !enabled -> TvColors.Surface.copy(alpha = 0.55f)
            focused -> TvColors.FocusRing
            else -> TvColors.Surface
        },
        border = BorderStroke(
            1.dp,
            if (focused) {
                TvColors.FocusRing
            } else {
                Color.White.copy(alpha = 0.12f)
            }
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = if (focused) TvColors.Background else TvColors.TextPrimary
            )
            Text(
                text = "Search",
                color = if (focused) TvColors.Background else TvColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 7.dp)
            )
        }
    }
}

@Composable
private fun TvSearchOptionsOverlay(
    entry: TvHomeEntry,
    saved: Boolean,
    watched: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onToggleSaved: () -> Unit,
    onToggleWatched: () -> Unit
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
                    text = entry.catalogName,
                    color = TvColors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                TvSearchOptionButton(
                    label = "Open details",
                    focusRequester = firstRequester,
                    onClick = onOpen
                )
                TvSearchOptionButton(
                    label = if (saved) "Remove from Library" else "Add to Library",
                    onClick = onToggleSaved
                )
                TvSearchOptionButton(
                    label = if (watched) "Mark as unwatched" else "Mark as watched",
                    onClick = onToggleWatched
                )
                TvSearchOptionButton(
                    label = "Cancel",
                    onClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun TvSearchOptionButton(
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
private fun TvSearchEmpty() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 30.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = "No matches found",
                color = TvColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 19.sp
            )
            Text(
                text = "Try a shorter title or another spelling.",
                color = TvColors.TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}
