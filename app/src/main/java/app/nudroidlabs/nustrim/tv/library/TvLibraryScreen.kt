package app.nudroidlabs.nustrim.tv.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.nustrim.core.library.LocalMediaStore
import app.nudroidlabs.nustrim.tv.common.TvMediaGridCard
import app.nudroidlabs.nustrim.tv.home.TvHomeEntry
import app.nudroidlabs.nustrim.tv.theme.TvColors
import kotlinx.coroutines.delay

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

    val entries = remember(refreshToken) {
        mediaStore.saved().map { local ->
            TvHomeEntry(
                sourceUrl = local.sourceUrl,
                session = null,
                item = local.toMediaItem(),
                catalogName = "Library",
                continueEntry = local.takeIf { it.hasProgress }
            )
        }
    }

    LaunchedEffect(contentFocusRequestToken, entries.size) {
        if (contentFocusRequestToken > 0) {
            delay(40)
            runCatching { firstContentRequester.requestFocus() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background)
            .padding(
                start = 36.dp,
                end = 34.dp,
                top = 38.dp,
                bottom = 30.dp
            ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = "Library",
                color = TvColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp
            )
            Text(
                text = if (entries.isEmpty()) {
                    "Your saved titles will appear here."
                } else {
                    "${entries.size} saved title${if (entries.size == 1) "" else "s"}"
                },
                color = TvColors.TextSecondary,
                fontSize = 13.sp
            )
        }

        if (entries.isEmpty()) {
            TvLibraryEmpty(
                focusRequester = firstContentRequester,
                onFocused = onContentFocused,
                onMoveLeft = onMoveLeft
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 4.dp,
                    end = 10.dp,
                    bottom = 36.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                itemsIndexed(
                    items = entries,
                    key = { _, entry -> entry.stableKey }
                ) { index, entry ->
                    TvMediaGridCard(
                        entry = entry,
                        focusRequester = if (index == 0) {
                            firstContentRequester
                        } else {
                            null
                        },
                        onFocused = onContentFocused,
                        onMoveLeft = if (index % 6 == 0) onMoveLeft else null,
                        onOpen = onOpen
                    )
                }
            }
        }
    }
}

@Composable
private fun TvLibraryEmpty(
    focusRequester: FocusRequester,
    onFocused: (FocusRequester) -> Unit,
    onMoveLeft: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    focused = state.hasFocus
                    if (state.hasFocus) onFocused(focusRequester)
                }
                .onPreviewKeyEvent { event ->
                    if (
                        event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionLeft
                    ) {
                        onMoveLeft()
                        true
                    } else {
                        false
                    }
                }
                .focusable(),
            color = TvColors.Surface,
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
                    horizontal = 42.dp,
                    vertical = 34.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = TvColors.TextSecondary
                )
                Text(
                    text = "Nothing saved yet",
                    color = TvColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                )
                Text(
                    text = "Open a title and choose Add to Library.",
                    color = TvColors.TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}
