package app.nudroidlabs.nustrim.tv.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.nustrim.tv.focus.TvFocusRegistry
import app.nudroidlabs.nustrim.tv.focus.TvFocusRestoreEffect
import app.nudroidlabs.nustrim.tv.focus.rememberTvFocusAnchor
import app.nudroidlabs.nustrim.tv.focus.tvFocusAnchor

@Composable
fun TvSearchScreen(
    query: String,
    state: TvSearchUiState,
    recentSearches: List<String>,
    memory: TvSearchMemory,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    onQueryChanged: (String) -> Unit,
    onConfirmSearch: () -> Unit,
    onRecentSelected: (String) -> Unit,
    onClearHistory: () -> Unit,
    onRetry: () -> Unit,
    onOpen: (TvSearchMedia, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = when (state) {
        is TvSearchUiState.Discover -> state.snapshot.rows
        is TvSearchUiState.Results -> state.snapshot.rows
        is TvSearchUiState.Searching -> state.previous?.rows.orEmpty()
        else -> emptyList()
    }
    val shortQuery = query.trim().length < MIN_SEARCH_QUERY_LENGTH
    val recentAnchor = if (state is TvSearchUiState.Discover) {
        recentSearches.firstOrNull()?.let(::searchRecentAnchorKey)
    } else {
        null
    }
    val resultAnchor = preferredSearchAnchor(rows, memory)
    val messageAnchor = when (state) {
        is TvSearchUiState.Empty, is TvSearchUiState.Error -> SEARCH_RETRY_ANCHOR
        else -> null
    }
    val contentAnchor = when {
        shortQuery && recentAnchor != null -> recentAnchor
        resultAnchor != null -> resultAnchor
        else -> messageAnchor
    }

    TvFocusRestoreEffect(
        registry = focusRegistry,
        scopeKey = scopeKey,
        fallbackAnchorKey = SEARCH_INPUT_ANCHOR,
        requestToken = focusRequestToken,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF08090B))
            .padding(top = 30.dp),
    ) {
        Text(
            text = "Search",
            color = Color(0xFFF4F4F6),
            fontSize = 30.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 44.dp),
        )
        Spacer(Modifier.height(18.dp))
        TvSearchInput(
            query = query,
            scopeKey = scopeKey,
            focusRegistry = focusRegistry,
            canMoveDown = contentAnchor != null,
            onMoveDown = {
                val anchor = contentAnchor
                if (anchor == null) {
                    false
                } else {
                    val moved = focusRegistry.requestAnchor(scopeKey, anchor)
                    if (moved && !shortQuery) onConfirmSearch()
                    moved
                }
            },
            onQueryChanged = onQueryChanged,
            onSubmit = onConfirmSearch,
            onClear = { onQueryChanged("") },
            modifier = Modifier.padding(horizontal = 44.dp),
        )
        Spacer(Modifier.height(22.dp))

        when (state) {
            TvSearchUiState.DiscoverLoading -> TvSearchLoading(
                title = "Loading Discover",
                modifier = Modifier.fillMaxSize(),
            )

            is TvSearchUiState.Discover -> {
                if (recentSearches.isNotEmpty()) {
                    TvRecentSearches(
                        queries = recentSearches,
                        scopeKey = scopeKey,
                        focusRegistry = focusRegistry,
                        onSelected = onRecentSelected,
                        onClear = onClearHistory,
                    )
                    Spacer(Modifier.height(22.dp))
                }
                Text(
                    text = "Discover",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 44.dp),
                )
                Spacer(Modifier.height(12.dp))
                TvSearchRows(
                    rows = state.snapshot.rows,
                    memory = memory,
                    scopeKey = scopeKey,
                    focusRegistry = focusRegistry,
                    onOpen = onOpen,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is TvSearchUiState.Searching -> {
                val previousRows = state.previous?.rows.orEmpty()
                if (previousRows.isEmpty()) {
                    TvSearchLoading(
                        title = "Searching for \"${state.query}\"",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    TvSearchResultHeading(
                        title = "Results for \"${state.query}\"",
                        trailing = "Searching...",
                    )
                    TvSearchRows(
                        rows = previousRows,
                        memory = memory,
                        scopeKey = scopeKey,
                        focusRegistry = focusRegistry,
                        onOpen = onOpen,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            is TvSearchUiState.Results -> {
                TvSearchResultHeading(
                    title = "Results for \"${state.query}\"",
                    trailing = if (state.snapshot.failedSources > 0) {
                        "${state.snapshot.failedSources} source(s) unavailable"
                    } else {
                        "${state.snapshot.rows.sumOf { it.items.size }} results"
                    },
                )
                TvSearchRows(
                    rows = state.snapshot.rows,
                    memory = memory,
                    scopeKey = scopeKey,
                    focusRegistry = focusRegistry,
                    onOpen = onOpen,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is TvSearchUiState.Empty -> TvSearchMessage(
                title = if (state.query.isBlank()) "Nothing to discover" else "No results",
                message = state.message,
                actionLabel = "Retry",
                scopeKey = scopeKey,
                focusRegistry = focusRegistry,
                onAction = onRetry,
                modifier = Modifier.fillMaxSize(),
            )

            is TvSearchUiState.Error -> TvSearchMessage(
                title = if (state.query.isBlank()) "Discover unavailable" else "Search unavailable",
                message = state.message,
                actionLabel = "Retry",
                scopeKey = scopeKey,
                focusRegistry = focusRegistry,
                onAction = onRetry,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun TvSearchInput(
    query: String,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    canMoveDown: Boolean,
    onMoveDown: () -> Boolean,
    onQueryChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val inputAnchor = rememberTvFocusAnchor(focusRegistry, scopeKey, SEARCH_INPUT_ANCHOR)
    var inputFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier
                .width(720.dp)
                .tvFocusAnchor(inputAnchor)
                .onFocusChanged { inputFocused = it.isFocused }
                .onKeyEvent { event ->
                    if (
                        canMoveDown &&
                        event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionDown
                    ) {
                        onMoveDown()
                    } else {
                        false
                    }
                },
            placeholder = { Text("Search movies and series") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSubmit()
                    keyboard?.hide()
                },
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF191B20),
                unfocusedContainerColor = Color(0xFF15171B),
                focusedIndicatorColor = Color(0xFFF1F1F3),
                unfocusedIndicatorColor = Color(0xFF3A3D45),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color(0xFFE1E2E5),
                focusedPlaceholderColor = Color(0xFF8F929A),
                unfocusedPlaceholderColor = Color(0xFF777A82),
                focusedLeadingIconColor = Color.White,
                unfocusedLeadingIconColor = Color(0xFF9DA0A8),
            ),
        )

        if (query.isNotEmpty()) {
            TvSearchAction(
                label = "Clear",
                icon = Icons.Default.Close,
                onClick = {
                    onClear()
                    focusRegistry.requestAnchor(scopeKey, SEARCH_INPUT_ANCHOR)
                },
            )
        } else if (inputFocused) {
            Text(
                text = "Type at least 2 characters",
                color = Color(0xFF979AA2),
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun TvRecentSearches(
    queries: List<String>,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    onSelected: (String) -> Unit,
    onClear: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 44.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Recent searches",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            TvRecentChip(
                label = "Clear history",
                anchorKey = SEARCH_CLEAR_HISTORY_ANCHOR,
                scopeKey = scopeKey,
                focusRegistry = focusRegistry,
                onClick = onClear,
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(end = 44.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(
                items = queries.take(8),
                key = { query -> query.lowercase() },
            ) { query ->
                TvRecentChip(
                    label = query,
                    anchorKey = searchRecentAnchorKey(query),
                    scopeKey = scopeKey,
                    focusRegistry = focusRegistry,
                    onClick = { onSelected(query) },
                )
            }
        }
    }
}

@Composable
private fun TvRecentChip(
    label: String,
    anchorKey: String,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    onClick: () -> Unit,
) {
    val anchor = rememberTvFocusAnchor(focusRegistry, scopeKey, anchorKey)
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .tvFocusAnchor(anchor)
            .onFocusChanged { focused = it.isFocused }
            .background(
                if (focused) Color(0xFFF0F0F2) else Color(0xFF24272D),
                RoundedCornerShape(99.dp),
            )
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            text = label,
            color = if (focused) Color(0xFF111216) else Color.White,
            fontSize = 13.sp,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TvSearchResultHeading(title: String, trailing: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 44.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(trailing, color = Color(0xFF92959D), fontSize = 12.sp)
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun TvSearchLoading(title: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = Color(0xFFE8E8EA),
                strokeWidth = 2.dp,
                modifier = Modifier.size(34.dp),
            )
            Spacer(Modifier.height(14.dp))
            Text(title, color = Color(0xFFB8BAC0), fontSize = 15.sp)
        }
    }
}

@Composable
private fun TvSearchMessage(
    title: String,
    message: String,
    actionLabel: String,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val anchor = rememberTvFocusAnchor(focusRegistry, scopeKey, SEARCH_RETRY_ANCHOR)
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = modifier.padding(horizontal = 52.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = Color(0xFF8F929A),
            modifier = Modifier.size(42.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(message, color = Color(0xFFB4B6BD), fontSize = 15.sp)
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .tvFocusAnchor(anchor)
                .onFocusChanged { focused = it.isFocused }
                .background(
                    if (focused) Color(0xFFF0F0F2) else Color(0xFF272A31),
                    RoundedCornerShape(9.dp),
                )
                .clickable(onClick = onAction)
                .focusable()
                .padding(horizontal = 22.dp, vertical = 11.dp),
        ) {
            Text(
                text = actionLabel,
                color = if (focused) Color(0xFF111216) else Color.White,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun TvSearchAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .background(
                if (focused) Color(0xFFF0F0F2) else Color(0xFF24272D),
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (focused) Color(0xFF111216) else Color.White,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            color = if (focused) Color(0xFF111216) else Color.White,
            fontWeight = FontWeight.Medium,
        )
    }
}

fun searchRecentAnchorKey(query: String): String =
    "search-recent|${query.trim().lowercase()}"

private const val SEARCH_INPUT_ANCHOR = "search-input"
private const val SEARCH_RETRY_ANCHOR = "search-retry"
private const val SEARCH_CLEAR_HISTORY_ANCHOR = "search-clear-history"
