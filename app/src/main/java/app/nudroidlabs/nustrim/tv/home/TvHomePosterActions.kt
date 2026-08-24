package app.nudroidlabs.nustrim.tv.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

internal data class TvHomePosterActionTarget(
    val media: TvHomeMedia,
    val rowIndex: Int,
    val itemIndex: Int,
    val anchorKey: String,
    val isSaved: Boolean,
    val isWatched: Boolean,
)

@Composable
internal fun TvHomePosterActionsDialog(
    target: TvHomePosterActionTarget,
    onDismiss: () -> Unit,
    onDetails: () -> Unit,
    onToggleLibrary: () -> Unit,
    onToggleWatched: () -> Unit,
) {
    val firstAction = remember(target.media.stableKey) { FocusRequester() }

    LaunchedEffect(target.media.stableKey) {
        delay(60L)
        runCatching { firstAction.requestFocus() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.60f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .width(560.dp)
                    .background(Color(0xF225272C), RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFF8A8D94), RoundedCornerShape(24.dp))
                    .padding(horizontal = 30.dp, vertical = 28.dp),
            ) {
                Text(
                    text = target.media.item.title,
                    color = Color.White,
                    fontSize = 25.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Title actions",
                    color = Color(0xFFD1D3D8),
                    fontSize = 16.sp,
                )
                Spacer(Modifier.height(22.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    TvHomePosterActionButton(
                        label = "Go to details",
                        onClick = onDetails,
                        modifier = Modifier.focusRequester(firstAction),
                    )
                    TvHomePosterActionButton(
                        label = if (target.isSaved) "Remove from library" else "Add to library",
                        onClick = onToggleLibrary,
                    )
                    TvHomePosterActionButton(
                        label = if (target.isWatched) "Mark as unwatched" else "Mark as watched",
                        onClick = onToggleWatched,
                    )
                }
            }
        }
    }
}

@Composable
private fun TvHomePosterActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (focused) Color(0xFFF1F2F4) else Color(0xFF3B3E45),
                shape = RoundedCornerShape(30.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    event.nativeKeyEvent.repeatCount == 0 &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .focusable()
            .padding(horizontal = 24.dp, vertical = 15.dp),
    ) {
        Text(
            text = label,
            color = if (focused) Color(0xFF141518) else Color.White,
            fontSize = 17.sp,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
