package app.nudroidlabs.nustrim.tv.library

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.nustrim.core.library.LocalMediaEntry
import app.nudroidlabs.nustrim.core.library.LocalMediaStore
import app.nudroidlabs.nustrim.core.model.MediaType
import app.nudroidlabs.nustrim.tv.common.TvMediaGridCard
import app.nudroidlabs.nustrim.tv.home.TvHomeEntry
import app.nudroidlabs.nustrim.tv.theme.TvColors
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

private const val LIBRARY_FOCUS_RESTORE_MS = 70L
private const val LIBRARY_COLUMNS = 6

private data class TvLibraryItem(
    val local: LocalMediaEntry,
    val entry: TvHomeEntry,
    val watched: Boolean
)

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
    val preferences = remember(context) { TvLibraryPreferences(context) }
    val gridState = rememberLazyGridState()

    var localRevision by remember { mutableIntStateOf(0) }
    var focusRestoreRevision by remember { mutableIntStateOf(0) }
    var lastFocusedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingFocusKey by remember { mutableStateOf<String?>(null) }
    var optionsEntry by remember { mutableStateOf<TvHomeEntry?>(null) }
    var focusedBackdropEntry by remember { mutableStateOf<TvHomeEntry?>(null) }

    var typeFilter by remember {
        mutableStateOf(preferences.readTypeFilter())
    }
    var stateFilter by remember {
        mutableStateOf(preferences.readStateFilter())
    }
    var sortMode by remember {
        mutableStateOf(preferences.readSortMode())
    }

    val savedEntries = remember(refreshToken, localRevision) {
        mediaStore.saved().distinctBy { it.key }
    }
    val libraryItems = remember(savedEntries, localRevision) {
        savedEntries.map { local ->
            val item = local.toMediaItem()
            TvLibraryItem(
                local = local,
                entry = TvHomeEntry(
                    sourceUrl = local.sourceUrl,
                    session = null,
                    item = item,
                    catalogName = "Library",
                    continueEntry = local.takeIf { it.hasContinueState }
                ),
                watched = mediaStore.isWatched(local.sourceUrl, item)
            )
        }
    }

    val visibleItems = remember(
        libraryItems,
        typeFilter,
        stateFilter,
        sortMode
    ) {
        libraryItems
            .asSequence()
            .filter { libraryItem ->
                when (typeFilter) {
                    TvLibraryTypeFilter.ALL -> true
                    TvLibraryTypeFilter.MOVIES ->
                        libraryItem.local.type == MediaType.MOVIE
                    TvLibraryTypeFilter.SERIES ->
                        libraryItem.local.type == MediaType.SERIES ||
                            libraryItem.local.type == MediaType.TV
                }
            }
            .filter { libraryItem ->
                when (stateFilter) {
                    TvLibraryStateFilter.ALL -> true
                    TvLibraryStateFilter.CONTINUE ->
                        libraryItem.local.hasContinueState
                    TvLibraryStateFilter.WATCHED ->
                        libraryItem.watched
                    TvLibraryStateFilter.UNWATCHED ->
                        !libraryItem.watched
                }
            }
            .sortedWith(
                when (sortMode) {
                    TvLibrarySortMode.RECENT ->
                        compareByDescending<TvLibraryItem> { it.local.updatedAt }
                            .thenBy { it.local.title.lowercase() }
                    TvLibrarySortMode.TITLE ->
                        compareBy<TvLibraryItem> { it.local.title.lowercase() }
                            .thenByDescending { it.local.updatedAt }
                    TvLibrarySortMode.YEAR ->
                        compareByDescending<TvLibraryItem> {
                            extractLibraryYear(it.local.releaseInfo)
                        }.thenBy { it.local.title.lowercase() }
                }
            )
            .toList()
    }

    val visibleKeys = remember(visibleItems) {
        visibleItems.map { it.entry.stableKey }
    }
    val cardRequesters = remember(visibleKeys) {
        visibleKeys.associateWith { FocusRequester() }
    }
    val stateSelectorRequester = remember { FocusRequester() }
    val sortSelectorRequester = remember { FocusRequester() }
    val emptyStateRequester = remember { FocusRequester() }

    val continueCount = remember(libraryItems) {
        libraryItems.count { it.local.hasContinueState }
    }
    val watchedCount = remember(libraryItems) {
        libraryItems.count { it.watched }
    }

    fun restoreLibraryFocus(key: String?) {
        pendingFocusKey = key
        focusRestoreRevision += 1
    }

    fun moveToGrid() {
        val target = lastFocusedKey
            ?.takeIf { it in visibleKeys }
            ?: visibleKeys.firstOrNull()
        if (target != null) {
            restoreLibraryFocus(target)
        } else {
            runCatching { emptyStateRequester.requestFocus() }
        }
    }

    fun resetFilters() {
        typeFilter = TvLibraryTypeFilter.ALL
        stateFilter = TvLibraryStateFilter.ALL
        sortMode = TvLibrarySortMode.RECENT
        preferences.write(
            TvLibraryTypeFilter.ALL,
            TvLibraryStateFilter.ALL,
            TvLibrarySortMode.RECENT
        )
        val targetKey = libraryItems.firstOrNull()?.entry?.stableKey
        lastFocusedKey = targetKey
        pendingFocusKey = targetKey
        focusRestoreRevision += 1
    }

    LaunchedEffect(
        contentFocusRequestToken,
        focusRestoreRevision,
        visibleKeys
    ) {
        val explicitRestore = focusRestoreRevision > 0
        if (contentFocusRequestToken <= 0 && !explicitRestore) {
            return@LaunchedEffect
        }

        val preferredKey = pendingFocusKey
            ?: lastFocusedKey?.takeIf { it in visibleKeys }
        val restoreIndex = preferredKey
            ?.let { key -> visibleKeys.indexOf(key) }
            ?: -1

        if (restoreIndex >= 0 && preferredKey != null) {
            gridState.scrollToItem(restoreIndex)
            delay(LIBRARY_FOCUS_RESTORE_MS)
            val requester = cardRequesters[preferredKey]
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

    LaunchedEffect(visibleKeys) {
        if (
            lastFocusedKey != null &&
            lastFocusedKey !in visibleKeys
        ) {
            lastFocusedKey = null
        }
        if (
            focusedBackdropEntry != null &&
            focusedBackdropEntry?.stableKey !in visibleKeys
        ) {
            focusedBackdropEntry = visibleItems.firstOrNull()?.entry
        }
    }

    BackHandler(enabled = optionsEntry != null) {
        val key = optionsEntry?.stableKey
        optionsEntry = null
        restoreLibraryFocus(key)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background)
    ) {
        TvLibraryBackdrop(entry = focusedBackdropEntry)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 36.dp,
                    end = 34.dp,
                    top = 34.dp,
                    bottom = 28.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TvLibraryHeader(
                totalCount = libraryItems.size,
                visibleCount = visibleItems.size,
                continueCount = continueCount,
                watchedCount = watchedCount
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TvLibrarySelectorChip(
                    label = "Type",
                    value = typeFilter.displayLabel,
                    focusRequester = firstContentRequester,
                    onFocused = onContentFocused,
                    onMoveLeft = onMoveLeft,
                    onMoveDown = ::moveToGrid,
                    onSelect = {
                        val next = typeFilter.next()
                        typeFilter = next
                        preferences.write(
                            next,
                            stateFilter,
                            sortMode
                        )
                    }
                )
                TvLibrarySelectorChip(
                    label = "Status",
                    value = stateFilter.displayLabel,
                    focusRequester = stateSelectorRequester,
                    onFocused = onContentFocused,
                    onMoveDown = ::moveToGrid,
                    onSelect = {
                        val next = stateFilter.next()
                        stateFilter = next
                        preferences.write(
                            typeFilter,
                            next,
                            sortMode
                        )
                    }
                )
                TvLibrarySelectorChip(
                    label = "Sort",
                    value = sortMode.displayLabel,
                    focusRequester = sortSelectorRequester,
                    onFocused = onContentFocused,
                    onMoveDown = ::moveToGrid,
                    onSelect = {
                        val next = sortMode.next()
                        sortMode = next
                        preferences.write(
                            typeFilter,
                            stateFilter,
                            next
                        )
                    }
                )
                Text(
                    text = "OK cycles selection",
                    color = TvColors.TextSecondary.copy(alpha = 0.70f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            when {
                libraryItems.isEmpty() -> {
                    TvLibraryEmpty(
                        focusRequester = emptyStateRequester,
                        onFocused = onContentFocused,
                        onMoveLeft = onMoveLeft,
                        filtered = false,
                        onReset = null
                    )
                }
                visibleItems.isEmpty() -> {
                    TvLibraryEmpty(
                        focusRequester = emptyStateRequester,
                        onFocused = onContentFocused,
                        onMoveLeft = onMoveLeft,
                        filtered = true,
                        onReset = ::resetFilters
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(LIBRARY_COLUMNS),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = 4.dp,
                            end = 12.dp,
                            bottom = 40.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp)
                    ) {
                        itemsIndexed(
                            items = visibleItems,
                            key = { _, item -> item.entry.stableKey }
                        ) { index, libraryItem ->
                            val entry = libraryItem.entry
                            val requester = cardRequesters.getValue(
                                entry.stableKey
                            )
                            val continueEntry = entry.continueEntry

                            TvMediaGridCard(
                                entry = entry,
                                focusRequester = requester,
                                badgeText = when {
                                    libraryItem.watched -> "WATCHED"
                                    continueEntry?.nextUp == true -> "NEXT UP"
                                    continueEntry?.hasProgress == true -> "CONTINUE"
                                    else -> null
                                },
                                progressFraction = continueEntry
                                    ?.takeIf {
                                        !libraryItem.watched &&
                                            it.hasProgress
                                    }
                                    ?.progressFraction,
                                onFocused = { focusedRequester ->
                                    lastFocusedKey = entry.stableKey
                                    focusedBackdropEntry = entry
                                    onContentFocused(focusedRequester)
                                },
                                onMoveLeft = if (
                                    index % LIBRARY_COLUMNS == 0
                                ) {
                                    onMoveLeft
                                } else {
                                    null
                                },
                                onMoveUp = if (
                                    index < LIBRARY_COLUMNS
                                ) {
                                    {
                                        runCatching {
                                            firstContentRequester.requestFocus()
                                        }
                                    }
                                } else {
                                    null
                                },
                                onLongPress = {
                                    optionsEntry = it
                                },
                                onOpen = onOpen
                            )
                        }
                    }
                }
            }
        }

        optionsEntry?.let { entry ->
            val watched = remember(
                localRevision,
                entry.stableKey
            ) {
                mediaStore.isWatched(
                    entry.sourceUrl,
                    entry.item
                )
            }
            val hasContinue =
                entry.continueEntry?.hasContinueState == true

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
                    val currentIndex =
                        visibleItems.indexOfFirst {
                            it.entry.stableKey == entry.stableKey
                        }
                    val replacementKey = visibleItems
                        .getOrNull(currentIndex + 1)
                        ?.entry
                        ?.stableKey
                        ?: visibleItems
                            .getOrNull(currentIndex - 1)
                            ?.entry
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
}

@Composable
private fun TvLibraryBackdrop(entry: TvHomeEntry?) {
    val image = entry
        ?.item
        ?.backgroundUrl
        ?.takeIf { it.isNotBlank() }
        ?: entry
            ?.item
            ?.posterUrl
            ?.takeIf { it.isNotBlank() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!image.isNullOrBlank()) {
            AsyncImage(
                model = image,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.16f),
                contentScale = ContentScale.Crop
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            TvColors.Background.copy(alpha = 0.66f),
                            TvColors.Background.copy(alpha = 0.90f),
                            TvColors.Background
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            TvColors.Background.copy(alpha = 0.80f),
                            Color.Transparent,
                            TvColors.Background.copy(alpha = 0.30f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun TvLibraryHeader(
    totalCount: Int,
    visibleCount: Int,
    continueCount: Int,
    watchedCount: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = "Library",
            color = TvColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 31.sp
        )
        val summary = buildList {
            add(
                if (visibleCount == totalCount) {
                    "$totalCount saved"
                } else {
                    "$visibleCount of $totalCount shown"
                }
            )
            if (continueCount > 0) {
                add("$continueCount continue")
            }
            if (watchedCount > 0) {
                add("$watchedCount watched")
            }
        }.joinToString("  •  ")
        Text(
            text = summary,
            color = TvColors.TextSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun TvLibrarySelectorChip(
    label: String,
    value: String,
    focusRequester: FocusRequester,
    onFocused: (FocusRequester) -> Unit,
    onMoveLeft: (() -> Unit)? = null,
    onMoveDown: () -> Unit,
    onSelect: () -> Unit
) {
    var focused by remember(label) {
        mutableStateOf(false)
    }

    Surface(
        modifier = Modifier
            .height(44.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                focused = state.hasFocus
                if (state.hasFocus) {
                    onFocused(focusRequester)
                }
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
                        event.key == Key.DirectionDown -> {
                        onMoveDown()
                        true
                    }
                    event.type == KeyEventType.KeyDown &&
                        isLibrarySelectKey(event.key) -> {
                        onSelect()
                        true
                    }
                    else -> false
                }
            }
            .focusable(),
        color = if (focused) {
            TvColors.FocusBackground
        } else {
            TvColors.Surface.copy(alpha = 0.88f)
        },
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) {
                TvColors.FocusRing
            } else {
                Color.White.copy(alpha = 0.10f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 10.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.uppercase(),
                color = TvColors.TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )
            Text(
                text = value,
                color = TvColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
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
    val firstRequester = remember(entry.stableKey) {
        FocusRequester()
    }

    LaunchedEffect(entry.stableKey) {
        delay(40)
        runCatching { firstRequester.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.58f))
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 50.dp)
                .width(372.dp),
            color = TvColors.BackgroundElevated.copy(alpha = 0.98f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.12f)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = entry.item.title,
                    color = TvColors.TextPrimary,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        entry.continueEntry?.nextUp == true ->
                            "Next episode ready"
                        entry.continueEntry?.hasProgress == true ->
                            "Continue watching"
                        watched ->
                            "Watched"
                        else ->
                            "Saved to Library"
                    },
                    color = TvColors.TextSecondary,
                    fontSize = 12.sp
                )
                TvLibraryOptionButton(
                    label = "More info",
                    focusRequester = firstRequester,
                    onClick = onOpen
                )
                TvLibraryOptionButton(
                    label = if (watched) {
                        "Mark as unwatched"
                    } else {
                        "Mark as watched"
                    },
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
                    label = "Close",
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
    var focused by remember(label) {
        mutableStateOf(false)
    }
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
            .onFocusChanged {
                focused = it.hasFocus
            }
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    isLibrarySelectKey(event.key)
                ) {
                    onClick()
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
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) {
                TvColors.FocusRing
            } else {
                Color.White.copy(alpha = 0.08f)
            }
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
    onMoveLeft: () -> Unit,
    filtered: Boolean,
    onReset: (() -> Unit)?
) {
    var focused by remember(filtered) {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    focused = state.hasFocus
                    if (state.hasFocus) {
                        onFocused(focusRequester)
                    }
                }
                .onPreviewKeyEvent { event ->
                    when {
                        event.type == KeyEventType.KeyDown &&
                            event.key == Key.DirectionLeft -> {
                            onMoveLeft()
                            true
                        }
                        filtered &&
                            onReset != null &&
                            event.type == KeyEventType.KeyDown &&
                            isLibrarySelectKey(event.key) -> {
                            onReset()
                            true
                        }
                        else -> false
                    }
                }
                .focusable(),
            color = TvColors.Surface.copy(alpha = 0.92f),
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
                    horizontal = 44.dp,
                    vertical = 34.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Icon(
                    imageVector = if (filtered) {
                        Icons.Outlined.Info
                    } else {
                        Icons.Outlined.BookmarkBorder
                    },
                    contentDescription = null,
                    tint = TvColors.TextSecondary
                )
                Text(
                    text = if (filtered) {
                        "No titles match these filters"
                    } else {
                        "Nothing saved yet"
                    },
                    color = TvColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                )
                Text(
                    text = if (filtered) {
                        "Press OK to reset Library filters."
                    } else {
                        "Open a title and choose Add to Library."
                    },
                    color = TvColors.TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

private fun TvLibraryTypeFilter.next(): TvLibraryTypeFilter =
    when (this) {
        TvLibraryTypeFilter.ALL -> TvLibraryTypeFilter.MOVIES
        TvLibraryTypeFilter.MOVIES -> TvLibraryTypeFilter.SERIES
        TvLibraryTypeFilter.SERIES -> TvLibraryTypeFilter.ALL
    }

private fun TvLibraryStateFilter.next(): TvLibraryStateFilter =
    when (this) {
        TvLibraryStateFilter.ALL -> TvLibraryStateFilter.CONTINUE
        TvLibraryStateFilter.CONTINUE -> TvLibraryStateFilter.WATCHED
        TvLibraryStateFilter.WATCHED -> TvLibraryStateFilter.UNWATCHED
        TvLibraryStateFilter.UNWATCHED -> TvLibraryStateFilter.ALL
    }

private fun TvLibrarySortMode.next(): TvLibrarySortMode =
    when (this) {
        TvLibrarySortMode.RECENT -> TvLibrarySortMode.TITLE
        TvLibrarySortMode.TITLE -> TvLibrarySortMode.YEAR
        TvLibrarySortMode.YEAR -> TvLibrarySortMode.RECENT
    }

private fun extractLibraryYear(value: String): Int {
    val match = Regex("""\b(19|20)\d{2}\b""").find(value)
    return match?.value?.toIntOrNull() ?: 0
}

private fun isLibrarySelectKey(key: Key): Boolean =
    key == Key.DirectionCenter || key == Key.Enter
