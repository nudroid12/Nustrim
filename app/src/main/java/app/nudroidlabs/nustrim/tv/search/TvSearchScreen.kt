package app.nudroidlabs.nustrim.tv.search

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
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
import app.nudroidlabs.nustrim.core.model.MediaType
import app.nudroidlabs.nustrim.core.source.InstalledSourceStore
import app.nudroidlabs.nustrim.core.source.SearchableSourceSession
import app.nudroidlabs.nustrim.core.source.SourceEngine
import app.nudroidlabs.nustrim.tv.common.TvMediaGridCard
import app.nudroidlabs.nustrim.tv.home.TvHomeEntry
import app.nudroidlabs.nustrim.tv.theme.TvColors
import kotlinx.coroutines.delay

private const val SEARCH_FOCUS_RESTORE_MS = 70L
private const val SEARCH_RESULT_FOCUS_MS = 110L
private const val SEARCH_TIMEOUT_MS = 9_000L
private const val SEARCH_COLUMNS = 6

private enum class TvSearchTypeFilter(val label: String) {
    ALL("All"),
    MOVIES("Movies"),
    SERIES("Series")
}

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
    val historyStore = remember(context) { TvSearchHistoryStore(context) }
    val searchButtonRequester = remember { FocusRequester() }
    val voiceButtonRequester = remember { FocusRequester() }
    val clearButtonRequester = remember { FocusRequester() }
    val filterRequesters = remember {
        TvSearchTypeFilter.entries.associateWith { FocusRequester() }
    }
    val resultGridState = rememberLazyGridState()
    val requesterByKey = remember { mutableMapOf<String, FocusRequester>() }

    var query by remember { mutableStateOf("") }
    var submittedQuery by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var timedOut by remember { mutableStateOf(false) }
    var searchableCount by remember { mutableIntStateOf(0) }
    var failureCount by remember { mutableIntStateOf(0) }
    var searchGeneration by remember { mutableIntStateOf(0) }
    var localMediaRevision by remember { mutableIntStateOf(0) }
    var focusRestoreRevision by remember { mutableIntStateOf(0) }
    var inputFocusRevision by remember { mutableIntStateOf(0) }
    var lastFocusedResultKey by remember { mutableStateOf<String?>(null) }
    var pendingFocusKey by remember { mutableStateOf<String?>(null) }
    var optionsEntry by remember { mutableStateOf<TvHomeEntry?>(null) }
    var selectedType by remember { mutableStateOf(TvSearchTypeFilter.ALL) }
    var recentSearches by remember { mutableStateOf(historyStore.items()) }
    var moveFocusToResults by remember { mutableStateOf(false) }
    var voiceMessage by remember { mutableStateOf<String?>(null) }

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

    fun clearSearchState(keepQuery: Boolean = false) {
        searchGeneration += 1
        loading = false
        timedOut = false
        searchableCount = 0
        failureCount = 0
        submittedQuery = ""
        selectedType = TvSearchTypeFilter.ALL
        lastFocusedResultKey = null
        pendingFocusKey = null
        requesterByKey.clear()
        results.clear()
        moveFocusToResults = false
        if (!keepQuery) query = ""
        inputFocusRevision += 1
    }

    fun runSearch(searchText: String = query) {
        val cleanQuery = searchText.trim()
        if (cleanQuery.isBlank()) return

        query = cleanQuery
        historyStore.add(cleanQuery)
        recentSearches = historyStore.items()
        searchGeneration += 1
        val generation = searchGeneration
        submittedQuery = cleanQuery
        loading = true
        timedOut = false
        searchableCount = 0
        failureCount = 0
        selectedType = TvSearchTypeFilter.ALL
        lastFocusedResultKey = null
        requesterByKey.clear()
        results.clear()
        moveFocusToResults = true

        val installed = sourceStore.sources()
            .filter { it.enabled && !it.developerOnly }
            .map { it.url }
            .distinct()

        if (installed.isEmpty()) {
            loading = false
            return
        }

        var pending = installed.size

        fun finishOne(searchable: Boolean) {
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
                    if (searchable == null || !session.capabilities.searchable) {
                        finishOne(searchable = false)
                    } else {
                        searchableCount += 1
                        searchable.search(
                            query = cleanQuery,
                            onSuccess = { catalog ->
                                if (generation == searchGeneration) {
                                    catalog.items.forEach { item ->
                                        val identity = mediaIdentity(item)
                                        if (results.none { mediaIdentity(it.item) == identity }) {
                                            results += TvHomeEntry(
                                                sourceUrl = sourceUrl,
                                                session = session,
                                                item = item,
                                                catalogName = catalog.name
                                            )
                                        }
                                    }
                                }
                                finishOne(searchable = true)
                            },
                            onError = {
                                if (generation == searchGeneration) failureCount += 1
                                finishOne(searchable = true)
                            }
                        )
                    }
                },
                onError = {
                    if (generation == searchGeneration) failureCount += 1
                    finishOne(searchable = false)
                }
            )
        }
    }

    val visibleResults = results
        .asSequence()
        .filter { entry -> matchesTypeFilter(entry.item.type, selectedType) }
        .sortedWith(
            compareBy<TvHomeEntry> { searchRank(it.item, submittedQuery) }
                .thenByDescending { parseRating(it.item.rating) }
                .thenBy { it.item.title.lowercase() }
        )
        .toList()

    LaunchedEffect(searchGeneration, loading) {
        if (!loading) return@LaunchedEffect
        val watchedGeneration = searchGeneration
        delay(SEARCH_TIMEOUT_MS)
        if (loading && searchGeneration == watchedGeneration) {
            timedOut = true
            loading = false
        }
    }

    LaunchedEffect(inputFocusRevision) {
        if (inputFocusRevision > 0) {
            delay(50)
            runCatching { firstContentRequester.requestFocus() }
        }
    }

    LaunchedEffect(
        contentFocusRequestToken,
        visibleResults.size,
        focusRestoreRevision
    ) {
        val explicitRestore = focusRestoreRevision > 0 && pendingFocusKey != null
        if (contentFocusRequestToken <= 0 && !explicitRestore) return@LaunchedEffect

        val preferredKey = pendingFocusKey ?: lastFocusedResultKey
        val restoreIndex = preferredKey
            ?.let { key -> visibleResults.indexOfFirst { it.stableKey == key } }
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
            delay(50)
            runCatching { firstContentRequester.requestFocus() }
        }
        pendingFocusKey = null
    }

    LaunchedEffect(moveFocusToResults, visibleResults.size) {
        if (!moveFocusToResults || visibleResults.isEmpty()) return@LaunchedEffect
        val first = visibleResults.first()
        delay(SEARCH_RESULT_FOCUS_MS)
        val requester = requesterByKey[first.stableKey]
        if (requester != null && runCatching { requester.requestFocus() }.isSuccess) {
            moveFocusToResults = false
            lastFocusedResultKey = first.stableKey
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val recognised = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            .orEmpty()
            .trim()
        if (recognised.isBlank()) {
            voiceMessage = "No speech recognised. Try again."
        } else {
            voiceMessage = null
            runSearch(recognised)
        }
    }

    fun requestVoiceSearch() {
        voiceMessage = null
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Search Nustrim")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        runCatching { voiceLauncher.launch(intent) }
            .onFailure { voiceMessage = "Voice search is not available on this device." }
    }

    BackHandler(
        enabled = optionsEntry != null ||
            submittedQuery.isNotBlank() ||
            query.isNotBlank()
    ) {
        when {
            optionsEntry != null -> {
                val key = optionsEntry?.stableKey
                optionsEntry = null
                restoreResultFocus(key)
            }
            submittedQuery.isNotBlank() || query.isNotBlank() -> {
                clearSearchState()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background)
            .padding(
                start = 42.dp,
                end = 40.dp,
                top = 34.dp,
                bottom = 28.dp
            ),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Search",
                color = TvColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            )
            Text(
                text = "Find movies and series across every enabled source.",
                color = TvColors.TextSecondary,
                fontSize = 13.sp
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    voiceMessage = null
                },
                singleLine = true,
                placeholder = { Text("Movie, series or anime") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { runSearch() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = TvColors.Surface,
                    unfocusedContainerColor = TvColors.Surface,
                    disabledContainerColor = TvColors.Surface,
                    focusedTextColor = TvColors.TextPrimary,
                    unfocusedTextColor = TvColors.TextPrimary,
                    focusedIndicatorColor = TvColors.FocusRing,
                    unfocusedIndicatorColor = Color.White.copy(alpha = 0.14f),
                    cursorColor = TvColors.FocusRing
                ),
                modifier = Modifier
                    .width(600.dp)
                    .height(58.dp)
                    .focusRequester(firstContentRequester)
                    .onFocusChanged { state ->
                        if (state.hasFocus) onContentFocused(firstContentRequester)
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

            TvSearchActionButton(
                label = "Search",
                enabled = query.isNotBlank() && !loading,
                focusRequester = searchButtonRequester,
                icon = Icons.Outlined.Search,
                onFocused = onContentFocused,
                onClick = { runSearch() }
            )

            TvSearchActionButton(
                label = "Voice",
                enabled = !loading,
                focusRequester = voiceButtonRequester,
                icon = Icons.Outlined.Mic,
                onFocused = onContentFocused,
                onClick = { requestVoiceSearch() }
            )

            if (query.isNotBlank() || submittedQuery.isNotBlank()) {
                TvSearchActionButton(
                    label = "Clear",
                    enabled = true,
                    focusRequester = clearButtonRequester,
                    icon = Icons.Outlined.Close,
                    onFocused = onContentFocused,
                    onClick = { clearSearchState() }
                )
            }
        }

        voiceMessage?.let { message ->
            Text(
                text = message,
                color = TvColors.TextSecondary,
                fontSize = 12.sp
            )
        }

        if (submittedQuery.isBlank() && recentSearches.isNotEmpty()) {
            TvRecentSearches(
                searches = recentSearches.take(5),
                firstContentRequester = firstContentRequester,
                onMoveLeft = onMoveLeft,
                onSelected = { recent -> runSearch(recent) },
                onClear = {
                    historyStore.clear()
                    recentSearches = emptyList()
                    inputFocusRevision += 1
                }
            )
        }

        if (submittedQuery.isNotBlank()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TvSearchTypeFilter.entries.forEach { filter ->
                    TvSearchFilterChip(
                        label = filter.label,
                        selected = selectedType == filter,
                        focusRequester = filterRequesters.getValue(filter),
                        onMoveUp = { runCatching { firstContentRequester.requestFocus() } },
                        onMoveLeft = if (filter == TvSearchTypeFilter.ALL) onMoveLeft else null,
                        onFocused = onContentFocused,
                        onClick = {
                            selectedType = filter
                            lastFocusedResultKey = null
                            moveFocusToResults = visibleResults.isNotEmpty()
                        }
                    )
                }

                Spacer(Modifier.width(8.dp))
                Text(
                    text = buildString {
                        append("${visibleResults.size} result")
                        if (visibleResults.size != 1) append("s")
                        append(" for \"")
                        append(submittedQuery)
                        append("\"")
                        if (loading) append("  |  searching...")
                        if (timedOut) append("  |  scan timed out")
                        if (failureCount > 0) append("  |  $failureCount source failure")
                    },
                    color = TvColors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        when {
            loading && results.isEmpty() -> {
                TvSearchSkeletonGrid()
            }
            submittedQuery.isBlank() -> {
                TvSearchIdleState(hasRecentSearches = recentSearches.isNotEmpty())
            }
            visibleResults.isEmpty() -> {
                TvSearchEmpty(
                    timedOut = timedOut,
                    failures = failureCount,
                    searchedSources = searchableCount
                )
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(SEARCH_COLUMNS),
                    state = resultGridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 3.dp,
                        end = 10.dp,
                        bottom = 36.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    itemsIndexed(
                        items = visibleResults,
                        key = { _, entry -> entry.stableKey }
                    ) { index, entry ->
                        val requester = remember(entry.stableKey) { FocusRequester() }
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
                                moveFocusToResults = false
                                onContentFocused(focusedRequester)
                            },
                            onMoveLeft = if (index % SEARCH_COLUMNS == 0) onMoveLeft else null,
                            onMoveUp = if (index < SEARCH_COLUMNS) {
                                {
                                    runCatching {
                                        filterRequesters.getValue(selectedType).requestFocus()
                                    }
                                }
                            } else {
                                null
                            },
                            onLongPress = { optionsEntry = it },
                            onOpen = onOpen
                        )
                    }
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
private fun TvSearchActionButton(
    label: String,
    enabled: Boolean,
    focusRequester: FocusRequester,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onFocused: (FocusRequester) -> Unit,
    onClick: () -> Unit
) {
    var focused by remember(label) { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .height(54.dp)
            .width(128.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                focused = state.hasFocus
                if (state.hasFocus) onFocused(focusRequester)
            }
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
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
            !enabled -> TvColors.Surface.copy(alpha = 0.45f)
            focused -> TvColors.FocusRing
            else -> TvColors.Surface
        },
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) TvColors.FocusRing else Color.White.copy(alpha = 0.10f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (focused) TvColors.Background else TvColors.TextPrimary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                color = if (focused) TvColors.Background else TvColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 7.dp)
            )
        }
    }
}

@Composable
private fun TvRecentSearches(
    searches: List<String>,
    firstContentRequester: FocusRequester,
    onMoveLeft: () -> Unit,
    onSelected: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent searches",
                color = TvColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier.width(140.dp)
            )
            searches.forEachIndexed { index, value ->
                TvSearchTextChip(
                    label = value,
                    width = 170.dp,
                    onMoveUp = { runCatching { firstContentRequester.requestFocus() } },
                    onMoveLeft = if (index == 0) onMoveLeft else null,
                    onClick = { onSelected(value) }
                )
            }
            TvSearchTextChip(
                label = "Clear history",
                width = 132.dp,
                onMoveUp = { runCatching { firstContentRequester.requestFocus() } },
                onClick = onClear
            )
        }
    }
}

@Composable
private fun TvSearchFilterChip(
    label: String,
    selected: Boolean,
    focusRequester: FocusRequester,
    onMoveUp: () -> Unit,
    onMoveLeft: (() -> Unit)?,
    onFocused: (FocusRequester) -> Unit,
    onClick: () -> Unit
) {
    var focused by remember(label) { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .width(108.dp)
            .height(42.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                focused = state.hasFocus
                if (state.hasFocus) onFocused(focusRequester)
            }
            .onPreviewKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp -> {
                        onMoveUp()
                        true
                    }
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionLeft &&
                        onMoveLeft != null -> {
                        onMoveLeft()
                        true
                    }
                    event.type == KeyEventType.KeyDown &&
                        (event.key == Key.DirectionCenter || event.key == Key.Enter) -> {
                        onClick()
                        true
                    }
                    else -> false
                }
            }
            .focusable(),
        color = when {
            focused -> TvColors.FocusRing
            selected -> TvColors.FocusBackground
            else -> TvColors.Surface
        },
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            when {
                focused -> TvColors.FocusRing
                selected -> Color.White.copy(alpha = 0.18f)
                else -> Color.White.copy(alpha = 0.08f)
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (focused) TvColors.Background else TvColors.TextPrimary,
                fontWeight = if (selected || focused) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun TvSearchTextChip(
    label: String,
    width: androidx.compose.ui.unit.Dp,
    onMoveUp: () -> Unit,
    onMoveLeft: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    var focused by remember(label) { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .width(width)
            .height(42.dp)
            .onFocusChanged { focused = it.hasFocus }
            .onPreviewKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp -> {
                        onMoveUp()
                        true
                    }
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionLeft &&
                        onMoveLeft != null -> {
                        onMoveLeft()
                        true
                    }
                    event.type == KeyEventType.KeyDown &&
                        (event.key == Key.DirectionCenter || event.key == Key.Enter) -> {
                        onClick()
                        true
                    }
                    else -> false
                }
            }
            .focusable(),
        color = if (focused) TvColors.FocusRing else TvColors.Surface,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) TvColors.FocusRing else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (focused) TvColors.Background else TvColors.TextPrimary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TvSearchSkeletonGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(SEARCH_COLUMNS),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, end = 10.dp, bottom = 36.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(12) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(
                    modifier = Modifier
                        .width(142.dp)
                        .height(206.dp)
                        .background(
                            TvColors.Surface.copy(alpha = 0.72f),
                            RoundedCornerShape(11.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .width(118.dp)
                        .height(12.dp)
                        .background(
                            TvColors.Surface.copy(alpha = 0.58f),
                            RoundedCornerShape(6.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun TvSearchIdleState(hasRecentSearches: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (hasRecentSearches) 8.dp else 22.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (hasRecentSearches) "Ready when you are" else "Search Nustrim",
                color = TvColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 19.sp
            )
            Text(
                text = "Use the keyboard or voice search. Results appear progressively as sources respond.",
                color = TvColors.TextSecondary,
                fontSize = 13.sp
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
            .background(Color.Black.copy(alpha = 0.64f))
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 54.dp)
                .width(370.dp),
            color = TvColors.BackgroundElevated.copy(alpha = 0.98f),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
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
                TvSearchOptionButton(label = "Cancel", onClick = onDismiss)
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
                if (focusRequester != null) Modifier.focusRequester(focusRequester)
                else Modifier
            )
            .onFocusChanged { focused = it.hasFocus }
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
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
            if (focused) TvColors.FocusRing else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 12.dp),
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
private fun TvSearchEmpty(
    timedOut: Boolean,
    failures: Int,
    searchedSources: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "No matches found",
                color = TvColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 19.sp
            )
            Text(
                text = when {
                    timedOut -> "Some sources did not finish in time. Try again or use a shorter title."
                    searchedSources == 0 -> "No enabled source currently supports search."
                    failures > 0 -> "Some sources failed. Try another spelling or search again."
                    else -> "Try a shorter title, another spelling, or a different content filter."
                },
                color = TvColors.TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

private fun matchesTypeFilter(
    type: MediaType,
    filter: TvSearchTypeFilter
): Boolean = when (filter) {
    TvSearchTypeFilter.ALL -> true
    TvSearchTypeFilter.MOVIES -> type == MediaType.MOVIE
    TvSearchTypeFilter.SERIES -> type == MediaType.SERIES || type == MediaType.TV
}

private fun searchRank(item: MediaItem, query: String): Int {
    val cleanQuery = query.trim().lowercase()
    if (cleanQuery.isBlank()) return 5
    val title = item.title.trim().lowercase()
    return when {
        title == cleanQuery -> 0
        title.startsWith(cleanQuery) -> 1
        title.contains(cleanQuery) -> 2
        item.genres.any { it.lowercase().contains(cleanQuery) } -> 3
        item.cast.any { it.lowercase().contains(cleanQuery) } -> 4
        else -> 5
    }
}

private fun parseRating(value: String): Double = value
    .trim()
    .replace(',', '.')
    .toDoubleOrNull()
    ?: 0.0
