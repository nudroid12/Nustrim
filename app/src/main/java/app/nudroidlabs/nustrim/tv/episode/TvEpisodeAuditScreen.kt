package app.nudroidlabs.nustrim.tv.episode

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.nustrim.tv.focus.TvFocusRegistry
import app.nudroidlabs.nustrim.tv.focus.TvFocusRestoreEffect
import app.nudroidlabs.nustrim.tv.focus.rememberTvFocusAnchor
import app.nudroidlabs.nustrim.tv.focus.tvFocusAnchor
import app.nudroidlabs.nustrim.tv.theme.TvTokens

@Composable
fun TvEpisodeAuditScreen(
    state: TvEpisodeUiState,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        TvEpisodeUiState.Loading -> EpisodeMessageScreen(
            title = "Loading episode catalogue",
            message = "Reading the provider's original episode metadata without the legacy Episode Engine.",
            loading = true,
            modifier = modifier,
        )

        is TvEpisodeUiState.Error -> EpisodeActionScreen(
            title = "Episode metadata unavailable",
            message = state.message,
            actionLabel = "Retry",
            scopeKey = scopeKey,
            focusRegistry = focusRegistry,
            focusRequestToken = focusRequestToken,
            onAction = onRetry,
            modifier = modifier,
        )

        is TvEpisodeUiState.Empty -> EpisodeActionScreen(
            title = state.snapshot.item.title.ifBlank { "No episodes" },
            message = state.message,
            actionLabel = "Retry",
            scopeKey = scopeKey,
            focusRegistry = focusRegistry,
            focusRequestToken = focusRequestToken,
            onAction = onRetry,
            modifier = modifier,
        )

        is TvEpisodeUiState.Ready -> EpisodeCatalogueScreen(
            snapshot = state.snapshot,
            scopeKey = scopeKey,
            focusRegistry = focusRegistry,
            focusRequestToken = focusRequestToken,
            modifier = modifier,
        )
    }
}

@Composable
private fun EpisodeCatalogueScreen(
    snapshot: TvEpisodeSnapshot,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    modifier: Modifier,
) {
    val seasons = snapshot.catalogue.seasons
    var selectedSeasonIndex by remember(snapshot.catalogue.parentKey) {
        mutableIntStateOf(snapshot.catalogue.firstRegularSeasonIndex.coerceIn(0, (seasons.size - 1).coerceAtLeast(0)))
    }
    val selectedSeason = seasons.getOrNull(selectedSeasonIndex) ?: return
    val fallbackAnchorKey = "season:${selectedSeason.stableKey}"

    TvFocusRestoreEffect(
        registry = focusRegistry,
        scopeKey = scopeKey,
        fallbackAnchorKey = fallbackAnchorKey,
        requestToken = focusRequestToken,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 58.dp, vertical = 36.dp),
    ) {
        Text(
            text = snapshot.item.title.ifBlank { "Episode catalogue" },
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 38.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${snapshot.sourceName} · clean episode domain QA",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(18.dp))
        EpisodeDiagnostics(snapshot.catalogue.diagnostics)
        Spacer(Modifier.height(22.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(
                items = seasons,
                key = { _, season -> season.stableKey },
            ) { index, season ->
                SeasonChip(
                    season = season,
                    selected = index == selectedSeasonIndex,
                    scopeKey = scopeKey,
                    focusRegistry = focusRegistry,
                    onFocused = { selectedSeasonIndex = index },
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        Text(
            text = selectedSeason.label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(TvTokens.CardGap),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(
                items = selectedSeason.episodes,
                key = { _, episode -> episode.identity.stableKey },
            ) { _, episode ->
                EpisodeAuditCard(
                    episode = episode,
                    scopeKey = scopeKey,
                    focusRegistry = focusRegistry,
                )
            }
        }
    }
}

@Composable
private fun EpisodeDiagnostics(diagnostics: TvEpisodeDiagnostics) {
    val text = buildString {
        append("Provider ").append(diagnostics.providerEntries)
        append(" · Canonical ").append(diagnostics.canonicalEntries)
        append(" · Duplicate IDs removed ").append(diagnostics.duplicateProviderIdsRemoved)
        append(" · Same S/E slots retained ").append(diagnostics.coordinateCollisionsRetained)
        if (diagnostics.unknownSeasonCount > 0) append(" · Unknown season ").append(diagnostics.unknownSeasonCount)
        if (diagnostics.unknownEpisodeCount > 0) append(" · Unknown episode ").append(diagnostics.unknownEpisodeCount)
        if (diagnostics.invalidCoordinateCount > 0) append(" · Invalid coordinates ").append(diagnostics.invalidCoordinateCount)
    }
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
private fun SeasonChip(
    season: TvEpisodeSeason,
    selected: Boolean,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    onFocused: () -> Unit,
) {
    val anchorKey = "season:${season.stableKey}"
    val anchor = rememberTvFocusAnchor(focusRegistry, scopeKey, anchorKey)
    var focused by remember(anchorKey) { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, label = "season-chip-scale")

    Box(
        modifier = Modifier
            .scale(scale)
            .background(
                color = when {
                    focused -> Color(0xFFF1F1F3)
                    selected -> Color(0xFF2A2C32)
                    else -> Color(0xFF17191E)
                },
                shape = RoundedCornerShape(22.dp),
            )
            .tvFocusAnchor(anchor)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .focusable()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = season.label,
            color = if (focused) Color(0xFF101114) else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected || focused) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun EpisodeAuditCard(
    episode: TvCanonicalEpisode,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
) {
    val anchorKey = "episode:${episode.identity.stableKey}"
    val anchor = rememberTvFocusAnchor(focusRegistry, scopeKey, anchorKey)
    var focused by remember(anchorKey) { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.045f else 1f, label = "episode-card-scale")

    Column(
        modifier = Modifier
            .width(300.dp)
            .scale(scale)
            .background(
                color = if (focused) Color(0xFFF1F1F3) else Color(0xFF17191E),
                shape = RoundedCornerShape(12.dp),
            )
            .tvFocusAnchor(anchor)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(16.dp),
    ) {
        Text(
            text = episode.coordinateLabel,
            color = if (focused) Color(0xFF101114) else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = episode.title,
            color = if (focused) Color(0xFF101114) else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = when (episode.coordinateKind) {
                TvEpisodeCoordinateKind.PROVIDER -> "Provider coordinates"
                TvEpisodeCoordinateKind.PARTIAL_PROVIDER -> "Partial provider coordinates"
                TvEpisodeCoordinateKind.VIRTUAL_DISPLAY_ONLY -> "Virtual display order only"
                TvEpisodeCoordinateKind.UNKNOWN -> "No provider coordinates"
            },
            color = if (focused) Color(0xFF303238) else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        if (focused) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "ID: ${episode.providerEpisodeId.ifBlank { "<missing>" }}",
                color = Color(0xFF4B4E55),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EpisodeMessageScreen(
    title: String,
    message: String,
    loading: Boolean,
    modifier: Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(18.dp))
            }
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(text = message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EpisodeActionScreen(
    title: String,
    message: String,
    actionLabel: String,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    onAction: () -> Unit,
    modifier: Modifier,
) {
    val anchorKey = "episode-action"
    val anchor = rememberTvFocusAnchor(focusRegistry, scopeKey, anchorKey)
    var focused by remember { mutableStateOf(false) }

    TvFocusRestoreEffect(
        registry = focusRegistry,
        scopeKey = scopeKey,
        fallbackAnchorKey = anchorKey,
        requestToken = focusRequestToken,
    )

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(text = message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(22.dp))
            Box(
                modifier = Modifier
                    .background(
                        if (focused) Color(0xFFF1F1F3) else Color(0xFF202228),
                        RoundedCornerShape(10.dp),
                    )
                    .tvFocusAnchor(anchor)
                    .onFocusChanged { focused = it.isFocused }
                    .clickable(onClick = onAction)
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            ) {
                Text(
                    text = actionLabel,
                    color = if (focused) Color(0xFF101114) else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
