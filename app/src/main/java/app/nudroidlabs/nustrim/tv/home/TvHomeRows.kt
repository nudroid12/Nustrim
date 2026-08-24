package app.nudroidlabs.nustrim.tv.home

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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.nustrim.tv.focus.TvFocusRegistry
import app.nudroidlabs.nustrim.tv.focus.rememberTvFocusAnchor
import app.nudroidlabs.nustrim.tv.focus.tvFocusAnchor
import app.nudroidlabs.nustrim.tv.theme.animateTvFocusScale
import coil3.compose.AsyncImage

internal val HOME_POSTER_WIDTH = 116.dp
internal val HOME_POSTER_HEIGHT = 174.dp
internal val HOME_CONTINUE_WIDTH = 210.dp
internal val HOME_CONTINUE_HEIGHT = 118.dp

@Composable
fun TvHomeRows(
    rows: List<TvHomeRow>,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    verticalListState: LazyListState,
    onFocused: (media: TvHomeMedia, rowIndex: Int, itemIndex: Int) -> Unit,
    onOpen: (media: TvHomeMedia, rowIndex: Int, itemIndex: Int) -> Unit,
    onLongPress: (media: TvHomeMedia, rowIndex: Int, itemIndex: Int) -> Unit,
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
                onLongPress = onLongPress,
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
    onLongPress: (TvHomeMedia, Int, Int) -> Unit,
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
                    onLongPress = onLongPress,
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
    onLongPress: (TvHomeMedia, Int, Int) -> Unit,
) {
    val anchorKey = homeAnchorKey(rowKey, media.stableKey)
    val anchor = rememberTvFocusAnchor(focusRegistry, scopeKey, anchorKey)
    var focused by remember { mutableStateOf(false) }
    val scale = animateTvFocusScale(
        focused = focused,
        label = "home-card-scale",
    )
    val continueEntry = media.continueEntry
    val longPressTracker = rememberTvHomeLongPressTracker()
    val cardWidth = if (continueEntry != null) HOME_CONTINUE_WIDTH else HOME_POSTER_WIDTH
    val cardHeight = if (continueEntry != null) HOME_CONTINUE_HEIGHT else HOME_POSTER_HEIGHT

    Column(
        modifier = Modifier
            .width(cardWidth)
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
            .onPreviewKeyEvent { event ->
                longPressTracker.handle(
                    event = event.nativeKeyEvent,
                    onClick = { onOpen(media, rowIndex, itemIndex) },
                    onLongPress = { onLongPress(media, rowIndex, itemIndex) },
                )
            }
            .focusable(),
    ) {
        Box(
            modifier = Modifier
                .size(width = cardWidth, height = cardHeight)
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
            val poster = if (continueEntry != null) {
                media.item.backgroundUrl.ifBlank { media.item.posterUrl }
            } else {
                media.item.posterUrl.ifBlank { media.item.backgroundUrl }
            }
            if (poster.isNotBlank()) {
                AsyncImage(
                    model = poster,
                    contentDescription = media.item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            if (continueEntry != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.88f),
                                ),
                            ),
                        ),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 11.dp, end = 11.dp, bottom = 10.dp),
                ) {
                    Text(
                        text = media.item.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = continueWatchingLabel(continueEntry),
                        color = Color(0xFFD4D5D8),
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (continueEntry.progressFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.28f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(continueEntry.progressFraction.coerceIn(0f, 1f))
                                .background(Color(0xFFF2F2F4)),
                        )
                    }
                }
            }
        }
        if (continueEntry == null) {
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
}

private fun continueWatchingLabel(entry: app.nudroidlabs.nustrim.core.library.LocalMediaEntry): String {
    val episode = when {
        entry.season != null && entry.episode != null -> "S${entry.season} E${entry.episode}"
        entry.episode != null -> "Episode ${entry.episode}"
        else -> ""
    }
    val progress = (entry.progressFraction * 100f).toInt().coerceIn(0, 100)
    return when {
        entry.nextUp && episode.isNotBlank() -> "Next up  •  $episode"
        entry.nextUp -> "Next up"
        episode.isNotBlank() && progress > 0 -> "$episode  •  $progress%"
        episode.isNotBlank() -> episode
        progress > 0 -> "$progress% watched"
        else -> "Continue watching"
    }
}

fun homeAnchorKey(rowKey: String, mediaKey: String): String = "home-card|$rowKey|$mediaKey"
