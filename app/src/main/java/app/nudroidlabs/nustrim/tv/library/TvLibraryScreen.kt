package app.nudroidlabs.nustrim.tv.library

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.nustrim.tv.focus.TvFocusRegistry
import app.nudroidlabs.nustrim.tv.focus.TvFocusRestoreEffect
import app.nudroidlabs.nustrim.tv.focus.rememberTvFocusAnchor
import app.nudroidlabs.nustrim.tv.focus.tvFocusAnchor
import app.nudroidlabs.nustrim.tv.theme.animateTvFocusScale
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun TvLibraryScreen(
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    memory: TvLibraryMemory,
    loadEntries: (TvLibrarySection) -> List<TvLibraryMedia>,
    onOpen: (TvLibraryMedia) -> Unit,
    modifier: Modifier = Modifier,
) {
    var section by remember(scopeKey) { mutableStateOf(memory.section) }
    var typeFilter by remember(scopeKey) { mutableStateOf(memory.typeFilter) }
    var sort by remember(scopeKey) { mutableStateOf(memory.sort) }
    var watchedFilter by remember(scopeKey) { mutableStateOf(memory.watchedFilter) }
    val allEntries = remember(section, focusRequestToken) { loadEntries(section) }
    val entries = remember(allEntries, typeFilter, watchedFilter, sort) {
        allEntries.filteredAndSorted(typeFilter, watchedFilter, sort)
    }
    val fallbackAnchor = memory.lastMediaKey
        ?.takeIf { key -> entries.any { it.stableKey == key } }
        ?.let(::libraryMediaAnchorKey)
        ?: librarySectionAnchorKey(section)

    TvFocusRestoreEffect(
        registry = focusRegistry,
        scopeKey = scopeKey,
        fallbackAnchorKey = fallbackAnchor,
        requestToken = focusRequestToken,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LIBRARY_BACKGROUND)
            .padding(top = 28.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Library",
                color = Color(0xFFF4F4F6),
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text("LOCAL", color = Color(0xFF858891), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.padding(horizontal = 44.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TvLibrarySection.entries.forEach { option ->
                LibraryPill(
                    label = option.label,
                    selected = section == option,
                    anchorKey = librarySectionAnchorKey(option),
                    scopeKey = scopeKey,
                    focusRegistry = focusRegistry,
                    onClick = {
                        section = option
                        memory.section = option
                        memory.firstVisibleItemIndex = 0
                    },
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.padding(horizontal = 44.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TvLibraryTypeFilter.entries.forEach { option ->
                LibraryPill(
                    label = option.label,
                    selected = typeFilter == option,
                    anchorKey = libraryTypeAnchorKey(option),
                    scopeKey = scopeKey,
                    focusRegistry = focusRegistry,
                    onClick = {
                        typeFilter = option
                        memory.typeFilter = option
                    },
                )
            }
            LibraryPill(
                label = "Sort: ${sort.label}",
                selected = false,
                anchorKey = LIBRARY_SORT_ANCHOR,
                scopeKey = scopeKey,
                focusRegistry = focusRegistry,
                onClick = {
                    sort = TvLibrarySort.entries[(sort.ordinal + 1) % TvLibrarySort.entries.size]
                    memory.sort = sort
                },
            )
            LibraryPill(
                label = watchedFilter.label,
                selected = watchedFilter != TvLibraryWatchedFilter.ALL,
                anchorKey = LIBRARY_WATCHED_ANCHOR,
                scopeKey = scopeKey,
                focusRegistry = focusRegistry,
                onClick = {
                    watchedFilter = TvLibraryWatchedFilter.entries[
                        (watchedFilter.ordinal + 1) % TvLibraryWatchedFilter.entries.size
                    ]
                    memory.watchedFilter = watchedFilter
                },
            )
        }
        Spacer(Modifier.height(18.dp))

        if (entries.isEmpty()) {
            LibraryEmptyState(
                section = section,
                filtersActive = typeFilter != TvLibraryTypeFilter.ALL ||
                    watchedFilter != TvLibraryWatchedFilter.ALL,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LibraryGrid(
                entries = entries,
                memory = memory,
                scopeKey = scopeKey,
                focusRegistry = focusRegistry,
                onOpen = onOpen,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun LibraryPill(
    label: String,
    selected: Boolean,
    anchorKey: String,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    onClick: () -> Unit,
) {
    val anchor = rememberTvFocusAnchor(focusRegistry, scopeKey, anchorKey)
    var focused by remember { mutableStateOf(false) }
    val bright = focused || selected
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(if (bright) Color.White else Color(0xFF181A1F))
            .border(1.dp, if (focused) Color.White else Color(0xFF30333A), RoundedCornerShape(99.dp))
            .tvFocusAnchor(anchor)
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    onClick()
                    true
                } else false
            }
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            text = label,
            color = if (bright) Color(0xFF101114) else Color(0xFFD5D6DA),
            fontSize = 13.sp,
            fontWeight = if (bright) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun LibraryGrid(
    entries: List<TvLibraryMedia>,
    memory: TvLibraryMemory,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    onOpen: (TvLibraryMedia) -> Unit,
    modifier: Modifier,
) {
    val initialIndex = memory.firstVisibleItemIndex.coerceIn(0, entries.lastIndex.coerceAtLeast(0))
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = initialIndex)

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { memory.firstVisibleItemIndex = it }
    }
    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty() && gridState.firstVisibleItemIndex > entries.lastIndex) {
            gridState.scrollToItem(entries.lastIndex)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(142.dp),
        state = gridState,
        modifier = modifier,
        contentPadding = PaddingValues(start = 44.dp, end = 44.dp, top = 6.dp, bottom = 38.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        itemsIndexed(entries, key = { _, media -> media.stableKey }) { index, media ->
            LibraryPosterCard(
                media = media,
                scopeKey = scopeKey,
                focusRegistry = focusRegistry,
                onFocused = {
                    memory.lastMediaKey = media.stableKey
                    if (index < gridState.firstVisibleItemIndex) memory.firstVisibleItemIndex = index
                },
                onClick = { onOpen(media) },
            )
        }
    }
}

@Composable
private fun LibraryPosterCard(
    media: TvLibraryMedia,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    val anchor = rememberTvFocusAnchor(focusRegistry, scopeKey, libraryMediaAnchorKey(media.stableKey))
    var focused by remember { mutableStateOf(false) }
    val scale = animateTvFocusScale(focused = focused, label = "library-card-scale")
    Column(
        modifier = Modifier
            .scale(scale)
            .tvFocusAnchor(anchor)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    onClick()
                    true
                } else false
            }
            .clickable(onClick = onClick)
            .focusable(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFF202228))
                .border(
                    width = if (focused) 3.dp else 1.dp,
                    color = if (focused) Color.White else Color(0xFF30333A),
                    shape = RoundedCornerShape(9.dp),
                ),
        ) {
            if (media.entry.posterUrl.isNotBlank()) {
                AsyncImage(
                    model = media.entry.posterUrl,
                    contentDescription = media.entry.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = media.entry.title.take(1).uppercase(),
                    color = Color(0xFF858891),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            if (media.watched) {
                Text(
                    text = "WATCHED",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .background(Color(0xCC111216), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
            if (media.entry.hasProgress) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(5.dp)
                        .background(Color(0xB3000000)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(media.entry.progressFraction)
                            .height(5.dp)
                            .background(Color.White),
                    )
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = media.entry.title,
            color = Color(0xFFF0F0F2),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val detail = libraryMediaDetail(media)
        if (detail.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = detail,
                color = Color(0xFF858891),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LibraryEmptyState(
    section: TvLibrarySection,
    filtersActive: Boolean,
    modifier: Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color(0xFF62656D), modifier = Modifier.size(42.dp))
            Spacer(Modifier.height(13.dp))
            Text(
                text = if (filtersActive) "No matching titles" else "Your Library is empty",
                color = Color(0xFFF0F0F2),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (filtersActive) "Try another filter." else "Save a title from Details to find it here.",
                color = Color(0xFF858891),
                fontSize = 13.sp,
            )
        }
    }
}

private fun libraryMediaDetail(media: TvLibraryMedia): String = when {
    media.entry.nextUp && media.entry.season != null && media.entry.episode != null ->
        "Up next S${media.entry.season} E${media.entry.episode}"
    media.entry.hasProgress && media.entry.season != null && media.entry.episode != null ->
        "S${media.entry.season} E${media.entry.episode}  •  ${(media.entry.progressFraction * 100).toInt()}%"
    media.entry.hasProgress -> "${(media.entry.progressFraction * 100).toInt()}% watched"
    else -> media.entry.releaseInfo
}

private fun librarySectionAnchorKey(section: TvLibrarySection): String = "library:section:${section.name}"
private fun libraryTypeAnchorKey(type: TvLibraryTypeFilter): String = "library:type:${type.name}"
private fun libraryMediaAnchorKey(key: String): String = "library:media:$key"

private const val LIBRARY_SORT_ANCHOR = "library:sort"
private const val LIBRARY_WATCHED_ANCHOR = "library:watched"
private val LIBRARY_BACKGROUND = Color(0xFF08090B)
