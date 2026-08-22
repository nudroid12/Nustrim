package app.nudroidlabs.nustrim.tv.common

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.nustrim.tv.focus.TvFocusRegistry
import app.nudroidlabs.nustrim.tv.focus.TvFocusRestoreEffect
import app.nudroidlabs.nustrim.tv.focus.rememberTvFocusAnchor
import app.nudroidlabs.nustrim.tv.focus.tvFocusAnchor
import app.nudroidlabs.nustrim.tv.theme.TvTokens
import app.nudroidlabs.nustrim.tv.theme.animateTvFocusScale

@Composable
fun TvFoundationScreen(
    title: String,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    modifier: Modifier = Modifier,
) {
    val firstAnchorKey = "foundation-primary"

    TvFocusRestoreEffect(
        registry = focusRegistry,
        scopeKey = scopeKey,
        fallbackAnchorKey = firstAnchorKey,
        requestToken = focusRequestToken,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = TvTokens.ScreenHorizontalPadding,
                vertical = TvTokens.ScreenVerticalPadding,
            ),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 38.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Fresh TV foundation. This screen is intentionally temporary until its subsystem rebuild begins.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(30.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(TvTokens.CardGap)) {
            FoundationFocusCard(
                label = "Primary",
                anchorKey = firstAnchorKey,
                scopeKey = scopeKey,
                focusRegistry = focusRegistry,
            )
            FoundationFocusCard(
                label = "Secondary",
                anchorKey = "foundation-secondary",
                scopeKey = scopeKey,
                focusRegistry = focusRegistry,
            )
            FoundationFocusCard(
                label = "Focus restore",
                anchorKey = "foundation-restore",
                scopeKey = scopeKey,
                focusRegistry = focusRegistry,
            )
        }
    }
}

@Composable
private fun FoundationFocusCard(
    label: String,
    anchorKey: String,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
) {
    val anchor = rememberTvFocusAnchor(focusRegistry, scopeKey, anchorKey)
    var focused by remember { mutableStateOf(false) }
    val scale = animateTvFocusScale(
        focused = focused,
        label = "foundation-card-scale",
    )

    Box(
        modifier = Modifier
            .width(190.dp)
            .height(104.dp)
            .scale(scale)
            .background(
                color = if (focused) Color(0xFFF1F1F3) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(TvTokens.CardCornerRadius),
            )
            .tvFocusAnchor(anchor)
            .onFocusChanged { focused = it.isFocused }
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (focused) Color(0xFF101114) else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}
