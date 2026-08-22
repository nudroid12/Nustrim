package app.nudroidlabs.nustrim.tv.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import app.nudroidlabs.nustrim.tv.theme.animateTvFocusScale
import coil3.compose.AsyncImage

@Composable
fun TvSearchRows(
    rows: List<TvSearchRow>,
    memory: TvSearchMemory,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    onOpen: (TvSearchMedia, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialRow = memory.focusedRowIndex.coerceIn(0, (rows.size - 1).coerceAtLeast(0))
    val verticalState = rememberLazyListState(initialFirstVisibleItemIndex = initialRow)
    LazyColumn(
        state = verticalState,
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 58.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        itemsIndexed(
            items = rows,
            key = { _, row -> row.key },
        ) { rowIndex, row ->
            TvSearchRowContent(
                row = row,
                rowIndex = rowIndex,
                memory = memory,
                scopeKey = scopeKey,
                focusRegistry = focusRegistry,
                onOpen = onOpen,
            )
        }
    }
}

@Composable
private fun TvSearchRowContent(
    row: TvSearchRow,
    rowIndex: Int,
    memory: TvSearchMemory,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    onOpen: (TvSearchMedia, Int, Int) -> Unit,
) {
    val initialItem = memory.rowItemPositions[row.key]
        ?.coerceIn(0, (row.items.size - 1).coerceAtLeast(0))
        ?: 0
    val rowState = rememberLazyListState(initialFirstVisibleItemIndex = initialItem)
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 44.dp, end = 44.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = row.title,
                color = Color(0xFFF1F1F3),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!row.sourceName.equals(row.title, ignoreCase = true)) {
                Text(
                    text = row.sourceName,
                    color = Color(0xFF858890),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = 44.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(
                items = row.items,
                key = { _, media -> media.stableKey },
            ) { itemIndex, media ->
                TvSearchPosterCard(
                    media = media,
                    rowKey = row.key,
                    rowIndex = rowIndex,
                    itemIndex = itemIndex,
                    memory = memory,
                    scopeKey = scopeKey,
                    focusRegistry = focusRegistry,
                    onOpen = onOpen,
                )
            }
        }
    }
}

@Composable
private fun TvSearchPosterCard(
    media: TvSearchMedia,
    rowKey: String,
    rowIndex: Int,
    itemIndex: Int,
    memory: TvSearchMemory,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    onOpen: (TvSearchMedia, Int, Int) -> Unit,
) {
    val anchorKey = searchCardAnchorKey(rowKey, media.stableKey)
    val anchor = rememberTvFocusAnchor(focusRegistry, scopeKey, anchorKey)
    var focused by remember { mutableStateOf(false) }
    val scale = animateTvFocusScale(
        focused = focused,
        label = "search-card-scale",
    )
    Column(
        modifier = Modifier
            .width(146.dp)
            .scale(scale)
            .tvFocusAnchor(anchor)
            .onFocusChanged { state ->
                focused = state.isFocused
                if (state.isFocused) {
                    memory.focusedRowIndex = rowIndex
                    memory.lastFocusedRowKey = rowKey
                    memory.lastFocusedMediaKey = media.stableKey
                    memory.rowItemPositions[rowKey] = itemIndex
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
        Spacer(Modifier.height(8.dp))
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
        media.item.releaseInfo.takeIf { it.isNotBlank() }?.let { release ->
            Spacer(Modifier.height(2.dp))
            Text(
                text = release,
                color = Color(0xFF81848B),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

fun preferredSearchAnchor(rows: List<TvSearchRow>, memory: TvSearchMemory): String? {
    val rememberedRow = memory.lastFocusedRowKey
    val rememberedMedia = memory.lastFocusedMediaKey
    if (rememberedRow != null && rememberedMedia != null) {
        val exists = rows.firstOrNull { it.key == rememberedRow }
            ?.items
            ?.any { it.stableKey == rememberedMedia }
            ?: false
        if (exists) return searchCardAnchorKey(rememberedRow, rememberedMedia)
    }

    val row = rows.getOrNull(memory.focusedRowIndex.coerceIn(0, (rows.size - 1).coerceAtLeast(0)))
        ?: rows.firstOrNull()
        ?: return null
    val itemIndex = memory.rowItemPositions[row.key]
        ?.coerceIn(0, (row.items.size - 1).coerceAtLeast(0))
        ?: 0
    val media = row.items.getOrNull(itemIndex) ?: row.items.firstOrNull() ?: return null
    return searchCardAnchorKey(row.key, media.stableKey)
}

fun searchCardAnchorKey(rowKey: String, mediaKey: String): String =
    "search-card|$rowKey|$mediaKey"
