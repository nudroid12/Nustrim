package app.nudroidlabs.nustrim.tv.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.nustrim.core.library.LocalMediaStore
import app.nudroidlabs.nustrim.core.source.CatalogSectionSourceSession
import app.nudroidlabs.nustrim.core.source.InstalledSourceStore
import app.nudroidlabs.nustrim.core.source.SourceEngine
import app.nudroidlabs.nustrim.core.source.SourceKind
import app.nudroidlabs.nustrim.tv.theme.TvColors
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun TvHomeScreen(
    contentFocusRequestToken: Int,
    refreshToken: Int = 0,
    firstContentRequester: FocusRequester,
    onContentFocused: (TvHomeEntry, FocusRequester) -> Unit,
    onMoveLeft: () -> Unit,
    onOpen: (TvHomeEntry) -> Unit
) {
    val context = LocalContext.current
    val engine = remember(context) { SourceEngine(context) }
    val sourceStore = remember(context) { InstalledSourceStore(context) }
    val mediaStore = remember(context) { LocalMediaStore(context) }

    var reloadToken by remember { mutableIntStateOf(0) }
    var loading by remember(reloadToken) { mutableStateOf(true) }
    var failureCount by remember(reloadToken) { mutableIntStateOf(0) }
    var sourceOrder by remember(reloadToken) { mutableStateOf<List<String>>(emptyList()) }
    val sectionMap = remember(reloadToken) {
        mutableStateMapOf<String, List<TvHomeSection>>()
    }

    LaunchedEffect(reloadToken) {
        sectionMap.clear()
        failureCount = 0
        loading = true

        val installed = sourceStore.sources()
            .filter { it.enabled && !it.developerOnly }
            .map { it.url }

        sourceOrder = installed
        var pending = installed.size

        fun finishOne() {
            pending -= 1
            if (pending <= 0) loading = false
        }

        if (installed.isEmpty()) {
            loading = false
            return@LaunchedEffect
        }

        installed.forEach { sourceUrl ->
            engine.open(
                sourceUrl,
                onSuccess = { session ->
                    if (session.kind == SourceKind.CLOUDSTREAM) {
                        sectionMap[sourceUrl] = emptyList()
                        finishOne()
                    } else {
                        val sectioned = session as? CatalogSectionSourceSession
                        if (sectioned != null) {
                            sectioned.loadCatalogSections(
                                onSuccess = { catalogs ->
                                    sectionMap[sourceUrl] = catalogs.mapIndexedNotNull { index, catalog ->
                                        val entries = catalog.items.map { media ->
                                            TvHomeEntry(
                                                sourceUrl = sourceUrl,
                                                session = session,
                                                item = media,
                                                catalogName = catalog.name
                                            )
                                        }
                                        entries.takeIf { it.isNotEmpty() }?.let {
                                            TvHomeSection(
                                                key = "$sourceUrl|${catalog.name}|$index",
                                                title = catalog.name,
                                                entries = it
                                            )
                                        }
                                    }
                                    finishOne()
                                },
                                onError = {
                                    failureCount += 1
                                    sectionMap[sourceUrl] = emptyList()
                                    finishOne()
                                }
                            )
                        } else {
                            session.loadCatalog(
                                onSuccess = { catalog ->
                                    val entries = catalog.items.map { media ->
                                        TvHomeEntry(
                                            sourceUrl = sourceUrl,
                                            session = session,
                                            item = media,
                                            catalogName = catalog.name
                                        )
                                    }
                                    sectionMap[sourceUrl] = if (entries.isEmpty()) {
                                        emptyList()
                                    } else {
                                        listOf(
                                            TvHomeSection(
                                                key = "$sourceUrl|${catalog.name}",
                                                title = catalog.name,
                                                entries = entries
                                            )
                                        )
                                    }
                                    finishOne()
                                },
                                onError = {
                                    failureCount += 1
                                    sectionMap[sourceUrl] = emptyList()
                                    finishOne()
                                }
                            )
                        }
                    }
                },
                onError = {
                    failureCount += 1
                    sectionMap[sourceUrl] = emptyList()
                    finishOne()
                }
            )
        }
    }

    val catalogSections = sourceOrder.flatMap { sectionMap[it].orEmpty() }
    val continueEntries = remember(loading, reloadToken, refreshToken) {
        if (loading) {
            emptyList()
        } else {
            mediaStore.continueWatching().map { local ->
                TvHomeEntry(
                    sourceUrl = local.sourceUrl,
                    session = null,
                    item = local.toMediaItem(),
                    catalogName = "Continue Watching",
                    continueEntry = local
                )
            }
        }
    }

    val firstCatalogEntries = catalogSections.firstOrNull()?.entries.orEmpty()
    val initialHero = firstCatalogEntries.firstOrNull()
        ?: continueEntries.firstOrNull()

    var focusedEntry by remember { mutableStateOf<TvHomeEntry?>(null) }
    var heroEntry by remember { mutableStateOf<TvHomeEntry?>(null) }

    LaunchedEffect(initialHero?.stableKey) {
        if (heroEntry == null && initialHero != null) {
            heroEntry = initialHero
        }
    }

    LaunchedEffect(focusedEntry?.stableKey) {
        val target = focusedEntry ?: return@LaunchedEffect
        delay(360)
        if (focusedEntry?.stableKey == target.stableKey) {
            heroEntry = target
        }
    }

    val hasContent = continueEntries.isNotEmpty() ||
        catalogSections.any { it.entries.isNotEmpty() }

    LaunchedEffect(contentFocusRequestToken, loading, hasContent) {
        if (contentFocusRequestToken > 0 && !loading && hasContent) {
            delay(40)
            runCatching { firstContentRequester.requestFocus() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background)
    ) {
        TvHeroBackdrop(
            entry = heroEntry ?: initialHero,
            modifier = Modifier.fillMaxSize()
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 304.dp,
                bottom = 52.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (!loading && continueEntries.isNotEmpty()) {
                item(key = "continue-watching") {
                    TvContinueRow(
                        entries = continueEntries,
                        firstContentRequester = firstContentRequester,
                        autoAttachFirstRequester = catalogSections.isEmpty(),
                        onFocused = { entry, requester ->
                            focusedEntry = entry
                            onContentFocused(entry, requester)
                        },
                        onMoveLeft = onMoveLeft,
                        onOpen = onOpen
                    )
                }
            }

            catalogSections.forEachIndexed { sectionIndex, section ->
                item(key = "catalog:${section.key}") {
                    TvCatalogRow(
                        section = section,
                        firstContentRequester = firstContentRequester,
                        autoAttachFirstRequester = sectionIndex == 0,
                        onFocused = { entry, requester ->
                            focusedEntry = entry
                            onContentFocused(entry, requester)
                        },
                        onMoveLeft = onMoveLeft,
                        onOpen = onOpen
                    )
                }
            }

            if (!loading && failureCount > 0 && catalogSections.isNotEmpty()) {
                item(key = "partial-failure") {
                    Text(
                        text = "$failureCount source(s) could not be loaded.",
                        color = TvColors.TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 34.dp)
                    )
                }
            }
        }

        when {
            loading && catalogSections.isEmpty() -> {
                TvHomeLoading(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            !loading &&
                catalogSections.isEmpty() &&
                continueEntries.isEmpty() -> {
                TvHomeEmpty(
                    onReload = { reloadToken += 1 },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun TvHeroBackdrop(
    entry: TvHomeEntry?,
    modifier: Modifier = Modifier
) {
    val item = entry?.item

    Box(modifier = modifier) {
        val image = item?.backgroundUrl
            ?.takeIf { it.isNotBlank() }
            ?: item?.posterUrl.orEmpty()

        if (image.isNotBlank()) {
            AsyncImage(
                model = image,
                contentDescription = item?.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.00f to TvColors.Background.copy(alpha = 0.98f),
                            0.42f to TvColors.Background.copy(alpha = 0.68f),
                            0.74f to TvColors.Background.copy(alpha = 0.28f),
                            1.00f to TvColors.Background.copy(alpha = 0.08f)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.52f to Color.Transparent,
                            0.78f to TvColors.Background.copy(alpha = 0.58f),
                            1.00f to TvColors.Background
                        )
                    )
                )
        )

        if (item != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 44.dp, top = 52.dp)
                    .width(520.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = item.title,
                    color = TvColors.TextPrimary,
                    fontSize = 34.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item.releaseInfo
                        .takeIf { it.isNotBlank() }
                        ?.let {
                            Text(
                                text = it,
                                color = TvColors.TextPrimary.copy(alpha = 0.82f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                    entry.catalogName
                        .takeIf { it.isNotBlank() }
                        ?.let {
                            Text(
                                text = it,
                                color = TvColors.TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                }

                item.description
                    .takeIf { it.isNotBlank() }
                    ?.let {
                        Text(
                            text = it,
                            color = TvColors.TextPrimary.copy(alpha = 0.78f),
                            fontSize = 14.sp,
                            lineHeight = 19.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = TvColors.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = entry.continueEntry?.let { local ->
                            val episodeLabel = buildString {
                                local.season?.let { append("S$it") }
                                local.episode?.let { append("E$it") }
                            }
                            when {
                                local.nextUp && episodeLabel.isNotBlank() -> {
                                    "Next up · $episodeLabel"
                                }
                                local.nextUp -> "Next up"
                                local.hasProgress && episodeLabel.isNotBlank() -> {
                                    "Resume · $episodeLabel · ${(local.progressFraction * 100).toInt()}%"
                                }
                                local.hasProgress -> {
                                    "Resume · ${(local.progressFraction * 100).toInt()}%"
                                }
                                else -> "Press OK for details"
                            }
                        } ?: "Press OK for details",
                        color = TvColors.TextPrimary.copy(alpha = 0.88f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TvCatalogRow(
    section: TvHomeSection,
    firstContentRequester: FocusRequester,
    autoAttachFirstRequester: Boolean,
    onFocused: (TvHomeEntry, FocusRequester) -> Unit,
    onMoveLeft: () -> Unit,
    onOpen: (TvHomeEntry) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = section.title,
            color = TvColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 19.sp,
            modifier = Modifier.padding(horizontal = 34.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 34.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(
                items = section.entries,
                key = { _, entry -> entry.stableKey }
            ) { index, entry ->
                TvPosterCard(
                    entry = entry,
                    focusRequester = if (
                        autoAttachFirstRequester && index == 0
                    ) {
                        firstContentRequester
                    } else {
                        null
                    },
                    onFocused = onFocused,
                    onMoveLeft = if (index == 0) onMoveLeft else null,
                    onOpen = onOpen
                )
            }
        }
    }
}

@Composable
private fun TvContinueRow(
    entries: List<TvHomeEntry>,
    firstContentRequester: FocusRequester,
    autoAttachFirstRequester: Boolean,
    onFocused: (TvHomeEntry, FocusRequester) -> Unit,
    onMoveLeft: () -> Unit,
    onOpen: (TvHomeEntry) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Continue Watching",
            color = TvColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 19.sp,
            modifier = Modifier.padding(horizontal = 34.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 34.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(
                items = entries,
                key = { _, entry -> entry.stableKey }
            ) { index, entry ->
                TvContinueCard(
                    entry = entry,
                    focusRequester = if (
                        autoAttachFirstRequester && index == 0
                    ) {
                        firstContentRequester
                    } else {
                        null
                    },
                    onFocused = onFocused,
                    onMoveLeft = if (index == 0) onMoveLeft else null,
                    onOpen = onOpen
                )
            }
        }
    }
}

@Composable
private fun TvPosterCard(
    entry: TvHomeEntry,
    focusRequester: FocusRequester?,
    onFocused: (TvHomeEntry, FocusRequester) -> Unit,
    onMoveLeft: (() -> Unit)?,
    onOpen: (TvHomeEntry) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val cardRequester = remember(entry.stableKey, focusRequester) {
        focusRequester ?: FocusRequester()
    }

    LaunchedEffect(focused) {
        if (focused) {
            delay(2400)
            if (focused) expanded = true
        } else {
            expanded = false
        }
    }

    val width by animateDpAsState(
        targetValue = if (expanded) 236.dp else 132.dp,
        label = "posterWidth"
    )
    val height by animateDpAsState(
        targetValue = if (expanded) 132.dp else 198.dp,
        label = "posterHeight"
    )
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.045f else 1f,
        label = "posterScale"
    )

    val image = if (expanded) {
        entry.item.backgroundUrl.takeIf { it.isNotBlank() }
            ?: entry.item.posterUrl
    } else {
        entry.item.posterUrl.takeIf { it.isNotBlank() }
            ?: entry.item.backgroundUrl
    }

    Surface(
        modifier = Modifier
            .width(width)
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .focusRequester(cardRequester)
            .onFocusChanged {
                focused = it.hasFocus
                if (it.hasFocus) onFocused(entry, cardRequester)
            }
            .onPreviewKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionLeft &&
                        onMoveLeft != null -> {
                        onMoveLeft()
                        true
                    }

                    event.type == KeyEventType.KeyDown &&
                        (
                            event.key == Key.DirectionCenter ||
                                event.key == Key.Enter
                            ) -> {
                        onOpen(entry)
                        true
                    }

                    else -> false
                }
            }
            .focusable(),
        color = TvColors.Surface,
        shape = RoundedCornerShape(10.dp),
        border = if (focused) {
            BorderStroke(2.dp, TvColors.FocusRing)
        } else {
            BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.08f)
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (image.isNotBlank()) {
                AsyncImage(
                    model = image,
                    contentDescription = entry.item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            if (expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    TvColors.Background.copy(alpha = 0.86f)
                                )
                            )
                        )
                )
                Text(
                    text = entry.item.title,
                    color = TvColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                )
            }
        }
    }
}

@Composable
private fun TvContinueCard(
    entry: TvHomeEntry,
    focusRequester: FocusRequester?,
    onFocused: (TvHomeEntry, FocusRequester) -> Unit,
    onMoveLeft: (() -> Unit)?,
    onOpen: (TvHomeEntry) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val cardRequester = remember(entry.stableKey, focusRequester) {
        focusRequester ?: FocusRequester()
    }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.035f else 1f,
        label = "continueScale"
    )
    val local = entry.continueEntry
    val image = entry.item.backgroundUrl
        .takeIf { it.isNotBlank() }
        ?: entry.item.posterUrl
    val progress = local?.progressFraction ?: 0f
    val episodeLabel = local?.let { state ->
        buildString {
            state.season?.let { append("S$it") }
            state.episode?.let { append("E$it") }
        }
    }.orEmpty()
    val statusLine = local?.let { state ->
        when {
            state.nextUp && episodeLabel.isNotBlank() -> {
                "Next up · $episodeLabel"
            }
            state.nextUp -> "Next up"
            state.hasProgress && episodeLabel.isNotBlank() -> {
                "$episodeLabel · ${(state.progressFraction * 100).toInt()}%"
            }
            state.hasProgress -> {
                "${(state.progressFraction * 100).toInt()}% watched"
            }
            else -> ""
        }
    }.orEmpty()

    Surface(
        modifier = Modifier
            .width(260.dp)
            .height(146.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .focusRequester(cardRequester)
            .onFocusChanged {
                focused = it.hasFocus
                if (it.hasFocus) onFocused(entry, cardRequester)
            }
            .onPreviewKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionLeft &&
                        onMoveLeft != null -> {
                        onMoveLeft()
                        true
                    }
                    event.type == KeyEventType.KeyDown &&
                        (
                            event.key == Key.DirectionCenter ||
                                event.key == Key.Enter
                            ) -> {
                        onOpen(entry)
                        true
                    }
                    else -> false
                }
            }
            .focusable(),
        color = TvColors.Surface,
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) {
                Color.White.copy(alpha = 0.92f)
            } else {
                Color.White.copy(alpha = 0.08f)
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (image.isNotBlank()) {
                AsyncImage(
                    model = image,
                    contentDescription = entry.item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Transparent,
                                TvColors.Background.copy(alpha = 0.94f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(
                        start = 11.dp,
                        end = 11.dp,
                        bottom = if (progress > 0f) 11.dp else 8.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = entry.item.title,
                    color = TvColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (statusLine.isNotBlank()) {
                    Text(
                        text = statusLine,
                        color = Color.White.copy(alpha = 0.68f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.22f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(TvColors.Accent)
                    )
                }
            }
        }
    }
}

@Composable
private fun TvHomeLoading(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = TvColors.BackgroundElevated.copy(alpha = 0.94f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.10f)
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 22.dp,
                vertical = 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp
            )
            Text(
                text = "Loading Home...",
                color = TvColors.TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun TvHomeEmpty(
    onReload: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .width(440.dp)
            .onFocusChanged { focused = it.hasFocus }
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    (
                        event.key == Key.DirectionCenter ||
                            event.key == Key.Enter
                        )
                ) {
                    onReload()
                    true
                } else {
                    false
                }
            }
            .focusable(),
        shape = RoundedCornerShape(18.dp),
        color = TvColors.BackgroundElevated,
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) {
                TvColors.FocusRing
            } else {
                Color.White.copy(alpha = 0.08f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = TvColors.TextSecondary,
                modifier = Modifier.size(30.dp)
            )
            Text(
                text = "No catalog available",
                color = TvColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp
            )
            Text(
                text = "Enable a catalog source, then press OK to retry.",
                color = TvColors.TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}
