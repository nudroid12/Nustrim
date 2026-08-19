package app.nudroidlabs.nustrim.tv.home

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import app.nudroidlabs.nustrim.core.source.CatalogSectionSourceSession
import app.nudroidlabs.nustrim.core.source.InstalledSourceStore
import app.nudroidlabs.nustrim.core.source.SourceEngine
import app.nudroidlabs.nustrim.core.source.SourceKind
import app.nudroidlabs.nustrim.tv.theme.TvColors
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

private const val HOME_FOCUS_SETTLE_MS = 140L
private const val HOME_LONG_PRESS_MS = 650L

@Composable
fun TvHomeScreen(
    contentFocusRequestToken: Int,
    refreshToken: Int = 0,
    firstContentRequester: FocusRequester,
    onContentFocused: (TvHomeEntry, FocusRequester) -> Unit,
    onMoveLeft: () -> Unit,
    onOpen: (TvHomeEntry) -> Unit
) {
    val context = LocalContext.current
    val engine = remember(context) { SourceEngine(context) }
    val sourceStore = remember(context) { InstalledSourceStore(context) }
    val mediaStore = remember(context) { LocalMediaStore(context) }

    var reloadToken by remember { mutableIntStateOf(0) }
    var localRevision by remember { mutableIntStateOf(0) }
    var loading by remember(reloadToken) { mutableStateOf(true) }
    var failureCount by remember(reloadToken) { mutableIntStateOf(0) }
    var sourceOrder by remember(reloadToken) { mutableStateOf<List<String>>(emptyList()) }
    val sectionMap = remember(reloadToken) {
        mutableStateMapOf<String, List<TvHomeSection>>()
    }
    val focusedIndexByRow = remember { mutableStateMapOf<String, Int>() }
    val requesterByKey = remember { mutableMapOf<String, FocusRequester>() }
    val homeListState = rememberLazyListState()

    var focusedEntry by remember { mutableStateOf<TvHomeEntry?>(null) }
    var heroEntry by remember { mutableStateOf<TvHomeEntry?>(null) }
    var lastFocusedKey by remember { mutableStateOf<String?>(null) }
    var pendingRestoreKey by remember { mutableStateOf<String?>(null) }
    var optionsEntry by remember { mutableStateOf<TvHomeEntry?>(null) }

    LaunchedEffect(reloadToken) {
        sectionMap.clear()
        failureCount = 0
        loading = true

        val installed = sourceStore.sources()
            .filter { it.enabled && !it.developerOnly }
            .map { it.url }

        sourceOrder = installed
        var pending = installed.size

        fun finishOne() {
            pending -= 1
            if (pending <= 0) loading = false
        }

        if (installed.isEmpty()) {
            loading = false
            return@LaunchedEffect
        }

        installed.forEach { sourceUrl ->
            engine.open(
                sourceUrl,
                onSuccess = { session ->
                    if (session.kind == SourceKind.CLOUDSTREAM) {
                        sectionMap[sourceUrl] = emptyList()
                        finishOne()
                    } else {
                        val sectioned = session as? CatalogSectionSourceSession
                        if (sectioned != null) {
                            sectioned.loadCatalogSections(
                                onSuccess = { catalogs ->
                                    sectionMap[sourceUrl] = catalogs.mapIndexedNotNull { index, catalog ->
                                        val entries = catalog.items.map { media ->
                                            TvHomeEntry(
                                                sourceUrl = sourceUrl,
                                                session = session,
                                                item = media,
                                                catalogName = catalog.name
                                            )
                                        }.distinctBy { it.stableKey }

                                        entries.takeIf { it.isNotEmpty() }?.let {
                                            TvHomeSection(
                                                key = "$sourceUrl|${catalog.name}|$index",
                                                title = catalog.name,
                                                entries = it
                                            )
                                        }
                                    }
                                    finishOne()
                                },
                                onError = {
                                    failureCount += 1
                                    sectionMap[sourceUrl] = emptyList()
                                    finishOne()
                                }
                            )
                        } else {
                            session.loadCatalog(
                                onSuccess = { catalog ->
                                    val entries = catalog.items.map { media ->
                                        TvHomeEntry(
                                            sourceUrl = sourceUrl,
                                            session = session,
                                            item = media,
                                            catalogName = catalog.name
                                        )
                                    }.distinctBy { it.stableKey }

                                    sectionMap[sourceUrl] = if (entries.isEmpty()) {
                                        emptyList()
                                    } else {
                                        listOf(
                                            TvHomeSection(
                                                key = "$sourceUrl|${catalog.name}",
                                                title = catalog.name,
                                                entries = entries
                                            )
                                        )
                                    }
                                    finishOne()
                                },
                                onError = {
                                    failureCount += 1
                                    sectionMap[sourceUrl] = emptyList()
                                    finishOne()
                                }
                            )
                        }
                    }
                },
                onError = {
                    failureCount += 1
                    sectionMap[sourceUrl] = emptyList()
                    finishOne()
                }
            )
        }
    }

    val catalogSections = sourceOrder
        .flatMap { sectionMap[it].orEmpty() }
        .map { section ->
            section.copy(entries = section.entries.distinctBy { it.stableKey })
        }
        .filter { it.entries.isNotEmpty() }

    val continueEntries = remember(loading, reloadToken, refreshToken, localRevision) {
        if (loading) {
            emptyList()
        } else {
            mediaStore.continueWatching()
                .map { local ->
                    TvHomeEntry(
                        sourceUrl = local.sourceUrl,
                        session = null,
                        item = local.toMediaItem(),
                        catalogName = "Continue Watching",
                        continueEntry = local
                    )
                }
                .distinctBy { it.stableKey }
        }
    }

    val firstCatalogEntries = catalogSections.firstOrNull()?.entries.orEmpty()
    val initialHero = continueEntries.firstOrNull()
        ?: firstCatalogEntries.firstOrNull()

    LaunchedEffect(initialHero?.stableKey) {
        if (heroEntry == null && initialHero != null) {
            heroEntry = initialHero
        }
    }

    LaunchedEffect(focusedEntry?.stableKey) {
        val target = focusedEntry ?: return@LaunchedEffect
        delay(HOME_FOCUS_SETTLE_MS)
        if (focusedEntry?.stableKey == target.stableKey) {
            heroEntry = target
        }
    }

    val hasContent = continueEntries.isNotEmpty() ||
        catalogSections.any { it.entries.isNotEmpty() }

    LaunchedEffect(contentFocusRequestToken, loading, hasContent) {
        if (contentFocusRequestToken > 0 && !loading && hasContent) {
            delay(40)
            val preferred = lastFocusedKey?.let(requesterByKey::get)
            val restored = preferred != null &&
                runCatching { preferred.requestFocus() }.isSuccess
            if (!restored) {
                runCatching { firstContentRequester.requestFocus() }
            }
        }
    }

    LaunchedEffect(refreshToken, loading, hasContent) {
        if (refreshToken > 0 && !loading && hasContent) {
            delay(90)
            val key = lastFocusedKey ?: return@LaunchedEffect
            requesterByKey[key]?.let { requester ->
                runCatching { requester.requestFocus() }
            }
        }
    }

    LaunchedEffect(pendingRestoreKey, continueEntries.size, catalogSections.size) {
        val key = pendingRestoreKey ?: return@LaunchedEffect
        delay(60)
        val restored = requesterByKey[key]?.let { requester ->
            runCatching { requester.requestFocus() }.isSuccess
        } == true
        if (restored) {
            pendingRestoreKey = null
        }
    }

    BackHandler(enabled = optionsEntry != null) {
        val key = optionsEntry?.stableKey
        optionsEntry = null
        pendingRestoreKey = key
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background)
    ) {
        TvHeroBackdrop(
            entry = heroEntry ?: initialHero,
            modifier = Modifier.fillMaxSize()
        )

        LazyColumn(
            state = homeListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 304.dp,
                bottom = 52.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (!loading && continueEntries.isNotEmpty()) {
                item(key = "continue-watching") {
                    TvContinueRow(
                        entries = continueEntries,
                        firstContentRequester = firstContentRequester,
                        autoAttachFirstRequester = true,
                        initialFocusedIndex = focusedIndexByRow["continue-watching"] ?: 0,
                        optionsStableKey = optionsEntry?.stableKey,
                        onFocused = { index, entry, requester ->
                            focusedIndexByRow["continue-watching"] = index
                            focusedEntry = entry
                            lastFocusedKey = entry.stableKey
                            onContentFocused(entry, requester)
                        },
                        onRequesterReady = { key, requester ->
                            requesterByKey[key] = requester
                        },
                        onRequesterDisposed = { key, requester ->
                            if (requesterByKey[key] === requester) {
                                requesterByKey.remove(key)
                            }
                        },
                        onMoveLeft = onMoveLeft,
                        onOpen = onOpen,
                        onLongPress = { optionsEntry = it }
                    )
                }
            }

            catalogSections.forEachIndexed { sectionIndex, section ->
                val rowKey = "catalog:${section.key}"
                item(key = rowKey) {
                    TvCatalogRow(
                        section = section,
                        firstContentRequester = firstContentRequester,
                        autoAttachFirstRequester = sectionIndex == 0 && continueEntries.isEmpty(),
                        initialFocusedIndex = focusedIndexByRow[rowKey] ?: 0,
                        optionsStableKey = optionsEntry?.stableKey,
                        isWatched = { entry ->
                            mediaStore.isWatched(entry.sourceUrl, entry.item)
                        },
                        onFocused = { index, entry, requester ->
                            focusedIndexByRow[rowKey] = index
                            focusedEntry = entry
                            lastFocusedKey = entry.stableKey
                            onContentFocused(entry, requester)
                        },
                        onRequesterReady = { key, requester ->
                            requesterByKey[key] = requester
                        },
                        onRequesterDisposed = { key, requester ->
                            if (requesterByKey[key] === requester) {
                                requesterByKey.remove(key)
                            }
                        },
                        onMoveLeft = onMoveLeft,
                        onOpen = onOpen,
                        onLongPress = { optionsEntry = it }
                    )
                }
            }

            if (!loading && failureCount > 0 && catalogSections.isNotEmpty()) {
                item(key = "partial-failure") {
                    Text(
                        text = "$failureCount source(s) could not be loaded.",
                        color = TvColors.TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 34.dp)
                    )
                }
            }
        }

        when {
            loading && catalogSections.isEmpty() -> {
                TvHomeLoading(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            !loading &&
                catalogSections.isEmpty() &&
                continueEntries.isEmpty() -> {
                TvHomeEmpty(
                    onReload = { reloadToken += 1 },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        optionsEntry?.let { entry ->
            val saved = remember(entry.stableKey, localRevision) {
                mediaStore.isSaved(entry.sourceUrl, entry.item)
            }
            val continueIndex = continueEntries.indexOfFirst { it.stableKey == entry.stableKey }

            TvHomeOptionsOverlay(
                entry = entry,
                saved = saved,
                onDismiss = {
                    val key = entry.stableKey
                    optionsEntry = null
                    pendingRestoreKey = key
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
                    localRevision += 1
                    optionsEntry = null
                    pendingRestoreKey = entry.stableKey
                },
                onClearContinue = if (entry.continueEntry != null) {
                    {
                        val restoreKey = continueEntries.getOrNull(continueIndex + 1)?.stableKey
                            ?: continueEntries.getOrNull(continueIndex - 1)?.stableKey
                            ?: firstCatalogEntries.firstOrNull()?.stableKey

                        mediaStore.clearContinueWatching(
                            sourceUrl = entry.sourceUrl,
                            item = entry.item
                        )
                        localRevision += 1
                        optionsEntry = null
                        pendingRestoreKey = restoreKey
                        if (focusedEntry?.stableKey == entry.stableKey) {
                            focusedEntry = null
                            heroEntry = initialHero
                        }
                    }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun TvHeroBackdrop(
    entry: TvHomeEntry?,
    modifier: Modifier = Modifier
) {
    val item = entry?.item

    Box(modifier = modifier) {
        val image = item?.backgroundUrl
            ?.takeIf { it.isNotBlank() }
            ?: item?.posterUrl.orEmpty()

        if (image.isNotBlank()) {
            AsyncImage(
                model = image,
                contentDescription = item?.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.00f to TvColors.Background.copy(alpha = 0.98f),
                            0.42f to TvColors.Background.copy(alpha = 0.68f),
                            0.74f to TvColors.Background.copy(alpha = 0.28f),
                            1.00f to TvColors.Background.copy(alpha = 0.08f)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.52f to Color.Transparent,
                            0.78f to TvColors.Background.copy(alpha = 0.58f),
                            1.00f to TvColors.Background
                        )
                    )
                )
        )

        if (item != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 44.dp, top = 52.dp)
                    .width(520.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = item.title,
                    color = TvColors.TextPrimary,
                    fontSize = 34.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item.releaseInfo
                        .takeIf { it.isNotBlank() }
                        ?.let {
                            Text(
                                text = it,
                                color = TvColors.TextPrimary.copy(alpha = 0.82f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                    entry.catalogName
                        .takeIf { it.isNotBlank() }
                        ?.let {
                            Text(
                                text = it,
                                color = TvColors.TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                }

                item.description
                    .takeIf { it.isNotBlank() }
                    ?.let {
                        Text(
                            text = it,
                            color = TvColors.TextPrimary.copy(alpha = 0.78f),
                            fontSize = 14.sp,
                            lineHeight = 19.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = TvColors.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = entry.continueEntry?.let { local ->
                            val status = homeContinueStatusLine(local)
                            when {
                                local.nextUp -> status
                                status.isNotBlank() -> "Resume · $status"
                                else -> "Press OK for details"
                            }
                        } ?: "Press OK for details",
                        color = TvColors.TextPrimary.copy(alpha = 0.88f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TvCatalogRow(
    section: TvHomeSection,
    firstContentRequester: FocusRequester,
    autoAttachFirstRequester: Boolean,
    initialFocusedIndex: Int,
    optionsStableKey: String?,
    isWatched: (TvHomeEntry) -> Boolean,
    onFocused: (Int, TvHomeEntry, FocusRequester) -> Unit,
    onRequesterReady: (String, FocusRequester) -> Unit,
    onRequesterDisposed: (String, FocusRequester) -> Unit,
    onMoveLeft: () -> Unit,
    onOpen: (TvHomeEntry) -> Unit,
    onLongPress: (TvHomeEntry) -> Unit
) {
    val entries = remember(section.key, section.entries) {
        section.entries.distinctBy { it.stableKey }
    }
    val rowState = rememberLazyListState(
        initialFirstVisibleItemIndex = (initialFocusedIndex - 1)
            .coerceIn(0, (entries.lastIndex).coerceAtLeast(0))
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = section.title,
            color = TvColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 19.sp,
            modifier = Modifier.padding(horizontal = 34.dp)
        )

        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = 34.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(
                items = entries,
                key = { _, entry -> entry.stableKey }
            ) { index, entry ->
                TvPosterCard(
                    entry = entry,
                    focusRequester = if (
                        autoAttachFirstRequester && index == 0
                    ) {
                        firstContentRequester
                    } else {
                        null
                    },
                    watched = isWatched(entry),
                    keepExpanded = optionsStableKey == entry.stableKey,
                    onFocused = { focusedEntry, requester ->
                        onFocused(index, focusedEntry, requester)
                    },
                    onRequesterReady = onRequesterReady,
                    onRequesterDisposed = onRequesterDisposed,
                    onMoveLeft = if (index == 0) onMoveLeft else null,
                    onOpen = onOpen,
                    onLongPress = onLongPress
                )
            }
        }
    }
}

@Composable
private fun TvContinueRow(
    entries: List<TvHomeEntry>,
    firstContentRequester: FocusRequester,
    autoAttachFirstRequester: Boolean,
    initialFocusedIndex: Int,
    optionsStableKey: String?,
    onFocused: (Int, TvHomeEntry, FocusRequester) -> Unit,
    onRequesterReady: (String, FocusRequester) -> Unit,
    onRequesterDisposed: (String, FocusRequester) -> Unit,
    onMoveLeft: () -> Unit,
    onOpen: (TvHomeEntry) -> Unit,
    onLongPress: (TvHomeEntry) -> Unit
) {
    val stableEntries = remember(entries) { entries.distinctBy { it.stableKey } }
    val rowState = rememberLazyListState(
        initialFirstVisibleItemIndex = (initialFocusedIndex - 1)
            .coerceIn(0, (stableEntries.lastIndex).coerceAtLeast(0))
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Continue Watching",
            color = TvColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 19.sp,
            modifier = Modifier.padding(horizontal = 34.dp)
        )

        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = 34.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(
                items = stableEntries,
                key = { _, entry -> entry.stableKey }
            ) { index, entry ->
                TvContinueCard(
                    entry = entry,
                    focusRequester = if (
                        autoAttachFirstRequester && index == 0
                    ) {
                        firstContentRequester
                    } else {
                        null
                    },
                    optionsActive = optionsStableKey == entry.stableKey,
                    onFocused = { focusedEntry, requester ->
                        onFocused(index, focusedEntry, requester)
                    },
                    onRequesterReady = onRequesterReady,
                    onRequesterDisposed = onRequesterDisposed,
                    onMoveLeft = if (index == 0) onMoveLeft else null,
                    onOpen = onOpen,
                    onLongPress = onLongPress
                )
            }
        }
    }
}

@Composable
private fun TvPosterCard(
    entry: TvHomeEntry,
    focusRequester: FocusRequester?,
    watched: Boolean,
    keepExpanded: Boolean,
    onFocused: (TvHomeEntry, FocusRequester) -> Unit,
    onRequesterReady: (String, FocusRequester) -> Unit,
    onRequesterDisposed: (String, FocusRequester) -> Unit,
    onMoveLeft: (() -> Unit)?,
    onOpen: (TvHomeEntry) -> Unit,
    onLongPress: (TvHomeEntry) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var selectDownAt by remember { mutableStateOf<Long?>(null) }
    val cardRequester = remember(entry.stableKey, focusRequester) {
        focusRequester ?: FocusRequester()
    }

    DisposableEffect(entry.stableKey, cardRequester) {
        onRequesterReady(entry.stableKey, cardRequester)
        onDispose {
            onRequesterDisposed(entry.stableKey, cardRequester)
        }
    }

    LaunchedEffect(focused, keepExpanded) {
        if (focused) {
            delay(900)
            if (focused) expanded = true
        } else if (!keepExpanded) {
            expanded = false
        }
    }

    val width by animateDpAsState(
        targetValue = if (expanded) 236.dp else 132.dp,
        label = "posterWidth"
    )
    val height by animateDpAsState(
        targetValue = if (expanded) 132.dp else 198.dp,
        label = "posterHeight"
    )
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.045f else 1f,
        label = "posterScale"
    )
    val image = if (expanded) {
        entry.item.backgroundUrl.takeIf { it.isNotBlank() }
            ?: entry.item.posterUrl
    } else {
        entry.item.posterUrl.takeIf { it.isNotBlank() }
            ?: entry.item.backgroundUrl
    }

    Surface(
        modifier = Modifier
            .width(width)
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .focusRequester(cardRequester)
            .onFocusChanged {
                focused = it.hasFocus
                if (!it.hasFocus) selectDownAt = null
                if (it.hasFocus) onFocused(entry, cardRequester)
            }
            .onPreviewKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionLeft &&
                        onMoveLeft != null -> {
                        onMoveLeft()
                        true
                    }

                    isHomeSelectKey(event.key) && event.type == KeyEventType.KeyDown -> {
                        if (selectDownAt == null) {
                            selectDownAt = SystemClock.uptimeMillis()
                        }
                        true
                    }

                    isHomeSelectKey(event.key) && event.type == KeyEventType.KeyUp -> {
                        val pressedAt = selectDownAt
                        selectDownAt = null
                        val heldMs = pressedAt?.let { SystemClock.uptimeMillis() - it } ?: 0L
                        if (heldMs >= HOME_LONG_PRESS_MS) {
                            onLongPress(entry)
                        } else {
                            onOpen(entry)
                        }
                        true
                    }

                    else -> false
                }
            }
            .focusable(),
        color = TvColors.Surface,
        shape = RoundedCornerShape(10.dp),
        border = if (focused) {
            BorderStroke(2.dp, TvColors.FocusRing)
        } else {
            BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (image.isNotBlank()) {
                AsyncImage(
                    model = image,
                    contentDescription = entry.item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            if (expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    TvColors.Background.copy(alpha = 0.86f)
                                )
                            )
                        )
                )
                Text(
                    text = entry.item.title,
                    color = TvColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                )
            }

            if (watched) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "WATCHED",
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TvContinueCard(
    entry: TvHomeEntry,
    focusRequester: FocusRequester?,
    optionsActive: Boolean,
    onFocused: (TvHomeEntry, FocusRequester) -> Unit,
    onRequesterReady: (String, FocusRequester) -> Unit,
    onRequesterDisposed: (String, FocusRequester) -> Unit,
    onMoveLeft: (() -> Unit)?,
    onOpen: (TvHomeEntry) -> Unit,
    onLongPress: (TvHomeEntry) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    var selectDownAt by remember { mutableStateOf<Long?>(null) }
    val cardRequester = remember(entry.stableKey, focusRequester) {
        focusRequester ?: FocusRequester()
    }

    DisposableEffect(entry.stableKey, cardRequester) {
        onRequesterReady(entry.stableKey, cardRequester)
        onDispose {
            onRequesterDisposed(entry.stableKey, cardRequester)
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (focused || optionsActive) 1.035f else 1f,
        label = "continueScale"
    )
    val local = entry.continueEntry
    val image = entry.item.backgroundUrl
        .takeIf { it.isNotBlank() }
        ?: entry.item.posterUrl
    val progress = local?.progressFraction ?: 0f
    val statusLine = local?.let(::homeContinueStatusLine).orEmpty()

    Surface(
        modifier = Modifier
            .width(260.dp)
            .height(146.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .focusRequester(cardRequester)
            .onFocusChanged {
                focused = it.hasFocus
                if (!it.hasFocus) selectDownAt = null
                if (it.hasFocus) onFocused(entry, cardRequester)
            }
            .onPreviewKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionLeft &&
                        onMoveLeft != null -> {
                        onMoveLeft()
                        true
                    }

                    isHomeSelectKey(event.key) && event.type == KeyEventType.KeyDown -> {
                        if (selectDownAt == null) {
                            selectDownAt = SystemClock.uptimeMillis()
                        }
                        true
                    }

                    isHomeSelectKey(event.key) && event.type == KeyEventType.KeyUp -> {
                        val pressedAt = selectDownAt
                        selectDownAt = null
                        val heldMs = pressedAt?.let { SystemClock.uptimeMillis() - it } ?: 0L
                        if (heldMs >= HOME_LONG_PRESS_MS) {
                            onLongPress(entry)
                        } else {
                            onOpen(entry)
                        }
                        true
                    }

                    else -> false
                }
            }
            .focusable(),
        color = TvColors.Surface,
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(
            if (focused || optionsActive) 2.dp else 1.dp,
            if (focused || optionsActive) {
                Color.White.copy(alpha = 0.92f)
            } else {
                Color.White.copy(alpha = 0.08f)
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (image.isNotBlank()) {
                AsyncImage(
                    model = image,
                    contentDescription = entry.item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Transparent,
                                TvColors.Background.copy(alpha = 0.94f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(
                        start = 11.dp,
                        end = 11.dp,
                        bottom = if (progress > 0f) 11.dp else 8.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = entry.item.title,
                    color = TvColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (statusLine.isNotBlank()) {
                    Text(
                        text = statusLine,
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (local?.nextUp == true) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "NEXT UP",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                }
            }
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.22f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .background(TvColors.Accent)
                    )
                }
            }
        }
    }
}

@Composable
private fun TvHomeOptionsOverlay(
    entry: TvHomeEntry,
    saved: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onToggleSaved: () -> Unit,
    onClearContinue: (() -> Unit)?
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
                    text = if (entry.continueEntry != null) {
                        homeContinueStatusLine(entry.continueEntry)
                    } else {
                        entry.catalogName
                    },
                    color = TvColors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                TvHomeOptionButton(
                    label = "Open details",
                    focusRequester = firstRequester,
                    onClick = onOpen
                )
                TvHomeOptionButton(
                    label = if (saved) "Remove from Library" else "Add to Library",
                    onClick = onToggleSaved
                )
                if (onClearContinue != null) {
                    TvHomeOptionButton(
                        label = "Remove from Continue Watching",
                        onClick = onClearContinue
                    )
                }
                TvHomeOptionButton(
                    label = "Cancel",
                    onClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun TvHomeOptionButton(
    label: String,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester)
                else Modifier
            )
            .onFocusChanged { focused = it.hasFocus }
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    isHomeSelectKey(event.key)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .focusable(),
        color = if (focused) {
            Color.White.copy(alpha = 0.94f)
        } else {
            Color.White.copy(alpha = 0.06f)
        },
        shape = RoundedCornerShape(10.dp),
        border = if (focused) null else BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.08f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = label,
                color = if (focused) TvColors.Background else TvColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

private fun isHomeSelectKey(key: Key): Boolean =
    key == Key.DirectionCenter || key == Key.Enter

private fun homeContinueStatusLine(local: LocalMediaEntry): String {
    val episodeLabel = buildString {
        local.season?.let { append("S$it") }
        local.episode?.let {
            if (isNotEmpty()) append("E$it") else append("E$it")
        }
    }

    if (local.nextUp) {
        return if (episodeLabel.isNotBlank()) "Next up · $episodeLabel" else "Next up"
    }

    if (!local.hasProgress) return ""

    val remainingMs = (local.durationMs - local.positionMs).coerceAtLeast(0L)
    val remainingMinutes = if (remainingMs >= 60_000L) {
        (remainingMs / 60_000L).coerceAtLeast(1L)
    } else {
        0L
    }
    val progressPercent = (local.progressFraction * 100f).toInt().coerceIn(0, 100)
    val progressText = if (remainingMinutes > 0L) {
        "${remainingMinutes}m left"
    } else {
        "$progressPercent% watched"
    }

    return if (episodeLabel.isNotBlank()) {
        "$episodeLabel · $progressText"
    } else {
        progressText
    }
}

@Composable
private fun TvHomeLoading(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = TvColors.BackgroundElevated.copy(alpha = 0.94f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.10f)
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 22.dp,
                vertical = 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp
            )
            Text(
                text = "Loading Home...",
                color = TvColors.TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun TvHomeEmpty(
    onReload: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .width(440.dp)
            .onFocusChanged { focused = it.hasFocus }
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    (
                        event.key == Key.DirectionCenter ||
                            event.key == Key.Enter
                        )
                ) {
                    onReload()
                    true
                } else {
                    false
                }
            }
            .focusable(),
        shape = RoundedCornerShape(18.dp),
        color = TvColors.BackgroundElevated,
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) {
                TvColors.FocusRing
            } else {
                Color.White.copy(alpha = 0.08f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = TvColors.TextSecondary,
                modifier = Modifier.size(30.dp)
            )
            Text(
                text = "No catalog available",
                color = TvColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp
            )
            Text(
                text = "Enable a catalog source, then press OK to retry.",
                color = TvColors.TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}
