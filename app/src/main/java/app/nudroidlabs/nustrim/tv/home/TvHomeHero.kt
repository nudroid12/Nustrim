package app.nudroidlabs.nustrim.tv.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.nustrim.tv.theme.TvTokens
import coil3.compose.AsyncImage

@Composable
fun TvHomeHero(
    media: TvHomeMedia?,
    modifier: Modifier = Modifier,
) {
    val item = media?.item
    Box(modifier = modifier) {
        Crossfade(
            targetState = media?.stableKey to media?.backdropUrl,
            animationSpec = tween(TvTokens.MediumMotionMillis),
            label = "home-hero-backdrop",
        ) { (_, backdrop) ->
            if (!backdrop.isNullOrBlank()) {
                AsyncImage(
                    model = backdrop,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopEnd,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xFF08090B),
                            0.22f to Color(0xF208090B),
                            0.46f to Color(0xC708090B),
                            0.76f to Color(0x5808090B),
                            1.00f to Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.58f to Color.Transparent,
                            0.80f to Color(0x8808090B),
                            1.00f to Color(0xFF08090B),
                        ),
                    ),
                ),
        )

        if (item != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(0.44f)
                    .padding(start = 44.dp, bottom = 52.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                var logoFailed by remember(item.logoUrl) { mutableStateOf(false) }
                if (item.logoUrl.isNotBlank() && !logoFailed) {
                    AsyncImage(
                        model = item.logoUrl,
                        contentDescription = item.title,
                        onError = { logoFailed = true },
                        modifier = Modifier
                            .height(96.dp)
                            .widthIn(min = 110.dp, max = 230.dp),
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterStart,
                    )
                } else {
                    androidx.compose.material3.Text(
                        text = item.title,
                        color = Color(0xFFF5F5F6),
                        fontSize = 38.sp,
                        lineHeight = 42.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(14.dp))
                HeroMetadata(media)

                if (item.description.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    androidx.compose.material3.Text(
                        text = item.description,
                        color = Color(0xFFD2D3D6),
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroMetadata(media: TvHomeMedia) {
    val item = media.item
    val values = buildList {
        item.releaseInfo.trim().takeIf { it.isNotBlank() }?.let(::add)
        item.rating.trim().takeIf { it.isNotBlank() }?.let { add("★ $it") }
        item.runtime.trim().takeIf { it.isNotBlank() }?.let(::add)
        item.genres.take(2).map { it.trim() }.filter { it.isNotBlank() }.forEach(::add)
    }

    if (values.isEmpty()) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        values.take(5).forEachIndexed { index, value ->
            if (index > 0) {
                androidx.compose.material3.Text(
                    text = "•",
                    color = Color(0xFF8F9197),
                    fontSize = 12.sp,
                )
            }
            androidx.compose.material3.Text(
                text = value,
                color = Color(0xFFE4E5E7),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
