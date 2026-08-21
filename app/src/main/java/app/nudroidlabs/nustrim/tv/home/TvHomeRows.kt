package app.nudroidlabs.nustrim.tv.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import app.nudroidlabs.nustrim.tv.focus.rememberTvFocusAnchor
import app.nudroidlabs.nustrim.tv.focus.tvFocusAnchor
import app.nudroidlabs.nustrim.tv.theme.TvTokens
import coil3.compose.AsyncImage

@Composable
fun TvHomeRows(
    rows: List<TvHomeRow>,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    verticalListState: LazyListState,
    onFocused: (media: TvHomeMedia, rowIndex: Int, itemIndex: Int) -> Unit,
    onOpen: (media: TvHomeMedia, rowIndex: Int, itemIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = verticalListState,
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 68.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        itemsIndexed(
            items = rows,
            key = { _, row -> row.key },
        ) { rowIndex, row ->
            TvHomeRowContent(
                row = row,
                rowIndex = rowIndex,
                scopeKey = scopeKey,
                focusRegistry = focusRegistry,
                onFocused = onFocused,
                onOpen = onOpen,
            )
        }
    }
}

@Composable
private fun TvHomeRowContent(
    row: TvHomeRow,
    rowIndex: Int,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    onFocused: (TvHomeMedia, Int, Int) -> Unit,
    onOpen: (TvHomeMedia, Int, Int) -> Unit,
) {
    val initialIndex = focusRegistry.rowItemIndex(scopeKey, row.key)
        .coerceIn(0, (row.items.size - 1).coerceAtLeast(0))
    val rowState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    Column {
        Text(
            text = row.title,
            color = Color(0xFFF2F2F4),
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 44.dp, bottom = 11.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = 44.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(
                items = row.items,
                key = { _, media -> media.stableKey },
            ) { itemIndex, media ->
                TvHomePosterCard(
                    media = media,
                    scopeKey = scopeKey,
                    rowKey = row.key,
                    rowIndex = rowIndex,
                    itemIndex = itemIndex,
                    focusRegistry = focusRegistry,
                    onFocused = onFocused,
                    onOpen = onOpen,
                )
            }
        }
    }
}

@Composable
private fun TvHomePosterCard(
    media: TvHomeMedia,
    scopeKey: String,
    rowKey: String,
    rowIndex: Int,
    itemIndex: Int,
    focusRegistry: TvFocusRegistry,
    onFocused: (TvHomeMedia, Int, Int) -> Unit,
    onOpen: (TvHomeMedia, Int, Int) -> Unit,
) {
    val anchorKey = homeAnchorKey(rowKey, media.stableKey)
    val anchor = rememberTvFocusAnchor(focusRegistry, scopeKey, anchorKey)
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) TvTokens.FocusScale else 1f,
        animationSpec = androidx.compose.animation.core.tween(TvTokens.FastMotionMillis),
        label = "home-card-scale",
    )

    Column(
        modifier = Modifier
            .width(146.dp)
            .scale(scale)
            .tvFocusAnchor(anchor)
            .onFocusChanged { state ->
                focused = state.isFocused
                if (state.isFocused) {
                    focusRegistry.rememberHomePosition(
                        scopeKey = scopeKey,
                        rowKey = rowKey,
                        rowIndex = rowIndex,
                        itemIndex = itemIndex,
                    )
                    onFocused(media, rowIndex, itemIndex)
                }
            }
            .onKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    onOpen(media, rowIndex, itemIndex)
                    true
                } else {
                    false
                }
            }
            .focusable(),
    ) {
        Box(
            modifier = Modifier
                .size(width = 146.dp, height = 214.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF181A1F))
                .then(
                    if (focused) {
                        Modifier.border(
                            BorderStroke(2.dp, Color(0xFFF1F1F3)),
                            RoundedCornerShape(10.dp),
                        )
                    } else {
                        Modifier
                    },
                ),
        ) {
            val poster = media.item.posterUrl.ifBlank { media.item.backgroundUrl }
            if (poster.isNotBlank()) {
                AsyncImage(
                    model = poster,
                    contentDescription = media.item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(Modifier.height(9.dp))
        Text(
            text = media.item.title,
            color = if (focused) Color.White else Color(0xFFD4D5D8),
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

fun homeAnchorKey(rowKey: String, mediaKey: String): String = "home-card|$rowKey|$mediaKey"
