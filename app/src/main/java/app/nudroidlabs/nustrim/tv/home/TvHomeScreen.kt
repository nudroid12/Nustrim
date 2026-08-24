package app.nudroidlabs.nustrim.tv.home

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.nustrim.tv.focus.TvFocusRegistry
import app.nudroidlabs.nustrim.tv.focus.TvFocusRestoreEffect
import app.nudroidlabs.nustrim.tv.focus.rememberTvFocusAnchor
import app.nudroidlabs.nustrim.tv.focus.tvFocusAnchor
import kotlinx.coroutines.delay

@Composable
fun TvHomeScreen(
    state: TvHomeUiState,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    onRetry: () -> Unit,
    onOpen: (TvHomeMedia, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        TvHomeUiState.Loading -> TvHomeLoading(modifier)
        is TvHomeUiState.Empty -> TvHomeMessage(
            title = "Nothing to show yet",
            message = state.message,
            actionLabel = "Retry",
            scopeKey = scopeKey,
            focusRegistry = focusRegistry,
            focusRequestToken = focusRequestToken,
            onAction = onRetry,
            modifier = modifier,
        )
        is TvHomeUiState.Error -> TvHomeMessage(
            title = "Home unavailable",
            message = state.message,
            actionLabel = "Retry",
            scopeKey = scopeKey,
            focusRegistry = focusRegistry,
            focusRequestToken = focusRequestToken,
            onAction = onRetry,
            modifier = modifier,
        )
        is TvHomeUiState.Ready -> TvHomeReady(
            snapshot = state.snapshot,
            scopeKey = scopeKey,
            focusRegistry = focusRegistry,
            focusRequestToken = focusRequestToken,
            onOpen = onOpen,
            modifier = modifier,
        )
    }
}

@Composable
private fun TvHomeReady(
    snapshot: TvHomeSnapshot,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    onOpen: (TvHomeMedia, Int, Int) -> Unit,
    modifier: Modifier,
) {
    val rows = snapshot.rows
    val savedRowIndex = focusRegistry.homeRowIndex(scopeKey)
        .coerceIn(0, (rows.size - 1).coerceAtLeast(0))
    val verticalState = rememberLazyListState(initialFirstVisibleItemIndex = savedRowIndex)
    val initialMedia = rows.getOrNull(savedRowIndex)?.let { row ->
        row.items.getOrNull(focusRegistry.rowItemIndex(scopeKey, row.key)) ?: row.items.firstOrNull()
    } ?: rows.firstOrNull()?.items?.firstOrNull()

    var focusedMedia by remember(rows) { mutableStateOf(initialMedia) }
    var heroMedia by remember(rows) { mutableStateOf(initialMedia) }

    LaunchedEffect(focusedMedia?.stableKey) {
        val candidate = focusedMedia ?: return@LaunchedEffect
        delay(HERO_FOCUS_SETTLE_MS)
        if (focusedMedia?.stableKey == candidate.stableKey) {
            heroMedia = candidate
        }
    }

    val fallback = rows.getOrNull(savedRowIndex)?.let { row ->
        val index = focusRegistry.rowItemIndex(scopeKey, row.key)
            .coerceIn(0, (row.items.size - 1).coerceAtLeast(0))
        row.items.getOrNull(index)?.let { homeAnchorKey(row.key, it.stableKey) }
            ?: row.items.firstOrNull()?.let { homeAnchorKey(row.key, it.stableKey) }
    } ?: rows.firstOrNull()?.items?.firstOrNull()?.let { first ->
        homeAnchorKey(rows.first().key, first.stableKey)
    }

    if (fallback != null) {
        TvFocusRestoreEffect(
            registry = focusRegistry,
            scopeKey = scopeKey,
            fallbackAnchorKey = fallback,
            requestToken = focusRequestToken,
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val heroHeight = maxHeight * 0.72f
        val rowsTop = maxHeight * 0.56f

        TvHomeHero(
            media = heroMedia,
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight),
        )

        TvHomeRows(
            rows = rows,
            scopeKey = scopeKey,
            focusRegistry = focusRegistry,
            verticalListState = verticalState,
            onFocused = { media, _, _ -> focusedMedia = media },
            onOpen = onOpen,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = rowsTop),
        )
    }
}

@Composable
private fun TvHomeLoading(modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF08090B)),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 44.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Box(
                Modifier
                    .size(width = 210.dp, height = 26.dp)
                    .background(Color(0xFF202228), RoundedCornerShape(6.dp)),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(6) {
                    Box(
                        Modifier
                            .size(width = HOME_POSTER_WIDTH, height = HOME_POSTER_HEIGHT)
                            .background(Color(0xFF17191E), RoundedCornerShape(10.dp)),
                    )
                }
            }
        }
        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.Center)
                .size(34.dp),
            color = Color(0xFFE4E5E7),
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun TvHomeMessage(
    title: String,
    message: String,
    actionLabel: String,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    onAction: () -> Unit,
    modifier: Modifier,
) {
    val anchorKey = "home-message-action"
    val anchor = rememberTvFocusAnchor(focusRegistry, scopeKey, anchorKey)
    var focused by remember { mutableStateOf(false) }

    TvFocusRestoreEffect(
        registry = focusRegistry,
        scopeKey = scopeKey,
        fallbackAnchorKey = anchorKey,
        requestToken = focusRequestToken,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 52.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = message,
            color = Color(0xFFB8BAC0),
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(26.dp))
        Box(
            modifier = Modifier
                .background(
                    if (focused) Color(0xFFF0F0F2) else Color(0xFF272A31),
                    RoundedCornerShape(8.dp),
                )
                .tvFocusAnchor(anchor)
                .onFocusChanged { focused = it.isFocused }
                .onKeyEvent { event ->
                    if (
                        event.type == KeyEventType.KeyDown &&
                        (event.key == Key.DirectionCenter || event.key == Key.Enter)
                    ) {
                        onAction()
                        true
                    } else false
                }
                .focusable()
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text(
                text = actionLabel,
                color = if (focused) Color(0xFF111216) else Color.White,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private const val HERO_FOCUS_SETTLE_MS = 150L
