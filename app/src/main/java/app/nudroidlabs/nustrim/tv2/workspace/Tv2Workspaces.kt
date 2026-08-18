package app.nudroidlabs.nustrim.tv2.workspace

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.nustrim.core.library.LocalMediaEntry
import coil3.compose.AsyncImage

private object Tv2WorkspaceColors {
    val Background = Color(0xFF090A0D)
    val Surface = Color(0xFF15171C)
    val Raised = Color(0xFF1E2128)
    val Text = Color(0xFFF5F6F8)
    val Muted = Color(0xFFADB1BB)
    val FocusText = Color(0xFF17191E)
}

data class Tv2MediaTile(
    val key: String,
    val title: String,
    val posterUrl: String,
    val backgroundUrl: String,
    val releaseInfo: String
)

data class Tv2SettingsEntry(
    val id: String,
    val title: String,
    val subtitle: String
)

@Composable
fun Tv2SearchWorkspace(
    query: String,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    searchFocusRequester: FocusRequester,
    contentFocusRequester: FocusRequester,
    sidebarRequester: FocusRequester?,
    searched: Boolean,
    loading: Boolean,
    discoverLoading: Boolean,
    results: List<Tv2MediaTile>,
    discover: List<Tv2MediaTile>,
    filteredDiscover: List<Tv2MediaTile>,
    typeFilter: String,
    catalogFilter: String,
    genreFilter: String,
    onTypeFilter: () -> Unit,
    onCatalogFilter: () -> Unit,
    onGenreFilter: () -> Unit,
    onOpen: (String) -> Unit,
    diagnostics: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Tv2WorkspaceColors.Background)
            .padding(top = 22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 38.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Search",
                    color = Tv2WorkspaceColors.Text,
                    fontSize = 33.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    if (searched) "Results from your enabled addons" else "Find something to watch",
                    color = Tv2WorkspaceColors.Muted,
                    fontSize = 13.sp
                )
            }
            Text(
                if (searched) "${results.size} RESULT${if (results.size == 1) "" else "S"}" else "DISCOVER",
                color = Tv2WorkspaceColors.Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(15.dp))

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 38.dp)
                .focusRequester(searchFocusRequester)
                .onPreviewKeyEvent { event ->
                    when {
                        event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft ->
                            tv2RequestFocus(sidebarRequester)
                        event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown ->
                            tv2RequestFocus(contentFocusRequester)
                        else -> false
                    }
                },
            placeholder = { Text("Search movies, series, anime...") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            shape = RoundedCornerShape(18.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() })
        )

        if (!searched) {
            Spacer(Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 38.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                item { Tv2FilterPill(typeFilter, onTypeFilter) }
                item { Tv2FilterPill(catalogFilter, onCatalogFilter) }
                item { Tv2FilterPill(genreFilter, onGenreFilter) }
            }
        }

        Spacer(Modifier.height(13.dp))

        Box(Modifier.fillMaxWidth().weight(1f)) {
            when {
                loading -> Tv2WorkspaceState(
                    iconSearch = true,
                    title = "Searching...",
                    subtitle = "Checking enabled sources",
                    loading = true
                )
                searched && results.isEmpty() -> Tv2WorkspaceState(
                    iconSearch = true,
                    title = "No results",
                    subtitle = "No enabled searchable addon returned a match."
                )
                searched -> Tv2MediaGrid(
                    entries = results,
                    firstFocusRequester = contentFocusRequester,
                    sidebarRequester = sidebarRequester,
                    onOpen = onOpen
                )
                discoverLoading -> Tv2WorkspaceState(
                    iconSearch = true,
                    title = "Loading Discover...",
                    subtitle = "Building your catalog",
                    loading = true
                )
                discover.isEmpty() -> Tv2WorkspaceState(
                    iconSearch = true,
                    title = "Discover is empty",
                    subtitle = "Enable a catalog addon to populate Discover."
                )
                filteredDiscover.isEmpty() -> Tv2WorkspaceState(
                    iconSearch = true,
                    title = "No Discover matches",
                    subtitle = "Try another type, catalog or genre."
                )
                else -> Tv2MediaGrid(
                    entries = filteredDiscover,
                    firstFocusRequester = contentFocusRequester,
                    sidebarRequester = sidebarRequester,
                    onOpen = onOpen
                )
            }
        }

        diagnostics?.let {
            Text(
                it,
                color = Color(0xFFFFB4AB),
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 38.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun Tv2LibraryWorkspace(
    saved: List<LocalMediaEntry>,
    gridMode: Boolean,
    onGridMode: (Boolean) -> Unit,
    primaryFocusRequester: FocusRequester,
    sidebarRequester: FocusRequester?,
    onOpen: (LocalMediaEntry) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Tv2WorkspaceColors.Background)
            .padding(start = 38.dp, end = 38.dp, top = 22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Library",
                    color = Tv2WorkspaceColors.Text,
                    fontSize = 33.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    if (saved.isEmpty()) "Your saved titles"
                    else "${saved.size} saved title${if (saved.size == 1) "" else "s"}",
                    color = Tv2WorkspaceColors.Muted,
                    fontSize = 13.sp
                )
            }
            Text(
                "LOCAL",
                color = Tv2WorkspaceColors.Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Tv2LibraryControl(
                label = "Saved",
                selected = true,
                requester = primaryFocusRequester,
                onMoveLeft = { tv2RequestFocus(sidebarRequester) },
                onClick = {}
            )
            Spacer(Modifier.weight(1f))
            Tv2LibraryControl("Grid", gridMode, null, null) { onGridMode(true) }
            Tv2LibraryControl("List", !gridMode, null, null) { onGridMode(false) }
        }

        Spacer(Modifier.height(18.dp))

        when {
            saved.isEmpty() -> Tv2WorkspaceState(
                iconSearch = false,
                title = "Your library is empty",
                subtitle = "Save a title from its details screen and it will appear here."
            )
            gridMode -> LazyVerticalGrid(
                columns = GridCells.Adaptive(148.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(15.dp),
                verticalArrangement = Arrangement.spacedBy(19.dp)
            ) {
                items(saved, key = { it.key }) { entry ->
                    Tv2LibraryPoster(entry) { onOpen(entry) }
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                contentPadding = PaddingValues(bottom = 30.dp)
            ) {
                items(saved, key = { it.key }) { entry ->
                    Tv2LibraryRow(entry) { onOpen(entry) }
                }
            }
        }
    }
}

@Composable
fun Tv2SettingsWorkspace(
    settingsItems: List<Tv2SettingsEntry>,
    firstFocusRequester: FocusRequester,
    sidebarRequester: FocusRequester?,
    onOpen: (String) -> Unit
) {
    var selectedIndex by remember(settingsItems.map { it.id }) { mutableIntStateOf(0) }
    val requesters = remember(settingsItems.map { it.id }) {
        List(settingsItems.size) { FocusRequester() }
    }
    val selected = settingsItems.getOrNull(selectedIndex)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Tv2WorkspaceColors.Background)
            .padding(horizontal = 34.dp, vertical = 24.dp)
    ) {
        Column(Modifier.width(355.dp).fillMaxHeight()) {
            Text(
                "Settings",
                color = Tv2WorkspaceColors.Text,
                fontSize = 33.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                "Nustrim preferences and integrations",
                color = Tv2WorkspaceColors.Muted,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(17.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(7.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(settingsItems.size, key = { settingsItems[it].id }) { index ->
                    val entry = settingsItems[index]
                    var focused by remember(entry.id) { mutableStateOf(false) }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (index == 0) Modifier.focusRequester(firstFocusRequester)
                                else Modifier.focusRequester(requesters[index])
                            )
                            .onFocusChanged {
                                focused = it.isFocused
                                if (it.isFocused) selectedIndex = index
                            }
                            .onPreviewKeyEvent { event ->
                                when {
                                    tv2Activate(event) { onOpen(entry.id) } -> true
                                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft ->
                                        tv2RequestFocus(sidebarRequester)
                                    else -> false
                                }
                            }
                            .clickable { onOpen(entry.id) }
                            .focusable(),
                        shape = RoundedCornerShape(14.dp),
                        color = if (focused) Color.White else Tv2WorkspaceColors.Surface,
                        border = BorderStroke(
                            if (focused) 2.dp else 1.dp,
                            if (focused) Color.White else Color.White.copy(alpha = 0.07f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                entry.title,
                                modifier = Modifier.weight(1f),
                                color = if (focused) Tv2WorkspaceColors.FocusText else Tv2WorkspaceColors.Text,
                                fontSize = 14.sp,
                                fontWeight = if (focused) FontWeight.Bold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                Icons.Outlined.KeyboardArrowRight,
                                contentDescription = null,
                                tint = if (focused) Tv2WorkspaceColors.FocusText else Tv2WorkspaceColors.Muted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.width(28.dp))

        Surface(
            modifier = Modifier.fillMaxHeight().weight(1f),
            shape = RoundedCornerShape(24.dp),
            color = Tv2WorkspaceColors.Surface,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Tv2WorkspaceColors.Raised, Tv2WorkspaceColors.Surface)
                        )
                    )
                    .padding(38.dp)
            ) {
                if (selected != null) {
                    Column(
                        modifier = Modifier.align(Alignment.CenterStart).widthIn(max = 520.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.72f),
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(Modifier.height(18.dp))
                        Text(
                            selected.title,
                            color = Tv2WorkspaceColors.Text,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            selected.subtitle,
                            color = Tv2WorkspaceColors.Muted,
                            fontSize = 15.sp,
                            lineHeight = 21.sp
                        )
                        Spacer(Modifier.height(18.dp))
                        Text(
                            "Press OK to open",
                            color = Tv2WorkspaceColors.Text.copy(alpha = 0.76f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Tv2FilterPill(label: String, onClick: () -> Unit) {
    var focused by remember(label) { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event -> tv2Activate(event, onClick) }
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(18.dp),
        color = if (focused) Color.White else Tv2WorkspaceColors.Surface,
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) Color.White else Color.White.copy(alpha = 0.09f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = if (focused) Tv2WorkspaceColors.FocusText else Tv2WorkspaceColors.Text,
                fontSize = 12.sp,
                fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "⌄",
                color = if (focused) Tv2WorkspaceColors.FocusText else Tv2WorkspaceColors.Muted
            )
        }
    }
}

@Composable
private fun Tv2MediaGrid(
    entries: List<Tv2MediaTile>,
    firstFocusRequester: FocusRequester,
    sidebarRequester: FocusRequester?,
    onOpen: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(148.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 38.dp, end = 38.dp, top = 8.dp, bottom = 30.dp),
        horizontalArrangement = Arrangement.spacedBy(15.dp),
        verticalArrangement = Arrangement.spacedBy(19.dp)
    ) {
        items(entries, key = { it.key }) { entry ->
            val first = entry.key == entries.firstOrNull()?.key
            Tv2PosterTile(
                entry = entry,
                requester = if (first) firstFocusRequester else null,
                onMoveLeft = if (first) ({ tv2RequestFocus(sidebarRequester) }) else null,
                onClick = { onOpen(entry.key) }
            )
        }
    }
}

@Composable
private fun Tv2PosterTile(
    entry: Tv2MediaTile,
    requester: FocusRequester?,
    onMoveLeft: (() -> Boolean)?,
    onClick: () -> Unit
) {
    var focused by remember(entry.key) { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.045f else 1f,
        animationSpec = tween(140),
        label = "tv2-workspace-poster-scale"
    )

    Column(
        modifier = Modifier
            .width(148.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(requester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                when {
                    tv2Activate(event, onClick) -> true
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionLeft &&
                        onMoveLeft != null -> onMoveLeft()
                    else -> false
                }
            }
            .clickable(onClick = onClick)
            .focusable()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
            shape = RoundedCornerShape(12.dp),
            color = Tv2WorkspaceColors.Raised,
            border = if (focused) BorderStroke(2.dp, Color.White)
            else BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Tv2Artwork(entry.posterUrl, entry.title, Modifier.fillMaxSize())
        }

        Spacer(Modifier.height(8.dp))
        Text(
            entry.title,
            color = Tv2WorkspaceColors.Text,
            fontSize = 12.sp,
            fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (entry.releaseInfo.isNotBlank()) {
            Text(
                entry.releaseInfo,
                color = Tv2WorkspaceColors.Muted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun Tv2LibraryControl(
    label: String,
    selected: Boolean,
    requester: FocusRequester?,
    onMoveLeft: (() -> Boolean)?,
    onClick: () -> Unit
) {
    var focused by remember(label) { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .then(requester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                when {
                    tv2Activate(event, onClick) -> true
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionLeft &&
                        onMoveLeft != null -> onMoveLeft()
                    else -> false
                }
            }
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(18.dp),
        color = when {
            focused -> Color.White
            selected -> Color.White.copy(alpha = 0.13f)
            else -> Tv2WorkspaceColors.Surface
        },
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) Color.White else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (focused) Tv2WorkspaceColors.FocusText else Tv2WorkspaceColors.Text,
            fontSize = 12.sp,
            fontWeight = if (focused || selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun Tv2LibraryPoster(entry: LocalMediaEntry, onClick: () -> Unit) {
    Tv2PosterTile(
        entry = Tv2MediaTile(
            key = entry.key,
            title = entry.title,
            posterUrl = entry.posterUrl,
            backgroundUrl = entry.backgroundUrl,
            releaseInfo = entry.releaseInfo
        ),
        requester = null,
        onMoveLeft = null,
        onClick = onClick
    )
}

@Composable
private fun Tv2LibraryRow(entry: LocalMediaEntry, onClick: () -> Unit) {
    var focused by remember(entry.key) { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event -> tv2Activate(event, onClick) }
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(14.dp),
        color = if (focused) Color.White else Tv2WorkspaceColors.Surface,
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) Color.White else Color.White.copy(alpha = 0.06f)
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.width(84.dp).aspectRatio(16f / 9f),
                shape = RoundedCornerShape(9.dp),
                color = Tv2WorkspaceColors.Raised
            ) {
                Tv2Artwork(
                    entry.backgroundUrl.ifBlank { entry.posterUrl },
                    entry.title,
                    Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    entry.title,
                    color = if (focused) Tv2WorkspaceColors.FocusText else Tv2WorkspaceColors.Text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (entry.releaseInfo.isNotBlank()) {
                    Text(
                        entry.releaseInfo,
                        color = if (focused) Color(0xFF60656E) else Tv2WorkspaceColors.Muted,
                        fontSize = 11.sp
                    )
                }
            }
            Icon(
                Icons.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = if (focused) Tv2WorkspaceColors.FocusText else Tv2WorkspaceColors.Muted
            )
        }
    }
}

@Composable
private fun Tv2WorkspaceState(
    iconSearch: Boolean,
    title: String,
    subtitle: String,
    loading: Boolean = false
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Icon(
                    if (iconSearch) Icons.Outlined.Search else Icons.Outlined.LibraryAdd,
                    contentDescription = null,
                    tint = Tv2WorkspaceColors.Muted,
                    modifier = Modifier.size(44.dp)
                )
            }
            Text(
                title,
                color = Tv2WorkspaceColors.Text,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
            Text(subtitle, color = Tv2WorkspaceColors.Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun Tv2Artwork(url: String, title: String, modifier: Modifier) {
    if (url.isBlank()) {
        Box(
            modifier = modifier.background(
                Brush.linearGradient(
                    listOf(Tv2WorkspaceColors.Raised, Tv2WorkspaceColors.Surface)
                )
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                title.take(1).uppercase().ifBlank { "N" },
                color = Tv2WorkspaceColors.Muted,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = title,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

private fun tv2Activate(
    event: androidx.compose.ui.input.key.KeyEvent,
    onClick: () -> Unit
): Boolean {
    val activate = event.key == Key.DirectionCenter || event.key == Key.Enter
    if (!activate) return false
    if (event.type == KeyEventType.KeyUp) onClick()
    return true
}

private fun tv2RequestFocus(requester: FocusRequester?): Boolean {
    if (requester == null) return false
    return runCatching {
        requester.requestFocus()
        true
    }.getOrDefault(false)
}
