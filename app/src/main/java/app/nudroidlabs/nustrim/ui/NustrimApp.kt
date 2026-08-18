package app.nudroidlabs.nustrim.ui

import app.nudroidlabs.nustrim.tv.TvApp

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.media.AudioManager
import android.graphics.Typeface
import android.util.TypedValue
import android.content.res.Configuration
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import app.nudroidlabs.nustrim.BuildConfig
import app.nudroidlabs.nustrim.core.diagnostics.NustrimDiagnostics
import app.nudroidlabs.nustrim.core.integrations.MdbListClient
import app.nudroidlabs.nustrim.core.integrations.MdbListRating
import app.nudroidlabs.nustrim.core.integrations.TmdbClient
import app.nudroidlabs.nustrim.core.integrations.TmdbMetadata
import app.nudroidlabs.nustrim.core.integrations.TraktClient
import app.nudroidlabs.nustrim.core.integrations.TraktDeviceCode
import app.nudroidlabs.nustrim.core.library.LocalMediaEntry
import app.nudroidlabs.nustrim.core.library.LocalMediaStore
import app.nudroidlabs.nustrim.core.model.MediaCatalog
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.StreamSource
import app.nudroidlabs.nustrim.core.player.PlayerFactory
import app.nudroidlabs.nustrim.core.source.CatalogSectionSourceSession
import app.nudroidlabs.nustrim.core.source.ChildSourceOpener
import app.nudroidlabs.nustrim.core.source.cloudstream.CloudStreamProviderStore
import app.nudroidlabs.nustrim.core.source.InstalledSource
import app.nudroidlabs.nustrim.core.source.InstalledSourceStore
import app.nudroidlabs.nustrim.core.source.SourceHealthStore
import app.nudroidlabs.nustrim.core.source.SearchableSourceSession
import app.nudroidlabs.nustrim.core.source.SourceEngine
import app.nudroidlabs.nustrim.core.source.SourceKind
import app.nudroidlabs.nustrim.core.source.SourcePreset
import app.nudroidlabs.nustrim.core.source.SourceSession
import app.nudroidlabs.nustrim.features.diagnostics.ProviderDiagnosticReport
import app.nudroidlabs.nustrim.features.diagnostics.ProviderDiagnosticRunner
import app.nudroidlabs.nustrim.features.diagnostics.ProviderDiagnosticStatus
import app.nudroidlabs.nustrim.core.update.AppUpdater
import app.nudroidlabs.nustrim.core.update.UpdateInfo
import app.nudroidlabs.nustrim.features.streams.StreamResolver
import app.nudroidlabs.nustrim.features.streams.StreamProviderProgress
import app.nudroidlabs.nustrim.features.streams.SubtitleResolver
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AppBackground = Color(0xFF090A0C)
private val AppSurface = Color(0xFF15171B)
private val AppSurface2 = Color(0xFF1E2025)
private val AppTextMuted = Color(0xFF9B9EA7)
private val AppAccent = Color(0xFF6554C0)
private val AppAccentSoft = Color(0xFF37304E)

private val LocalInterfaceMode = compositionLocalOf<InterfaceMode?> { null }
private val LocalTvSidebarFocusRequester = compositionLocalOf<FocusRequester?> { null }

private val AppColors = darkColorScheme(
    primary = AppAccent,
    onPrimary = Color.White,
    background = AppBackground,
    onBackground = Color.White,
    surface = AppSurface,
    onSurface = Color.White,
    surfaceVariant = AppSurface2,
    onSurfaceVariant = Color(0xFFE4E4E8),
    outline = Color(0xFF36383F)
)

private enum class MainSection(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    SEARCH("Search", Icons.Outlined.Search),
    LIBRARY("Library", Icons.Outlined.LibraryAdd),
    SETTINGS("Settings", Icons.Outlined.Settings)
}

@Composable
private fun rememberIsPhysicalTv(): Boolean {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(context, configuration.uiMode) {
        val packageManager = context.packageManager
        val televisionUiMode =
            (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
                Configuration.UI_MODE_TYPE_TELEVISION
        televisionUiMode ||
            packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
    }
}

private fun isTvActivateKey(event: androidx.compose.ui.input.key.KeyEvent): Boolean =
    event.key == Key.DirectionCenter || event.key == Key.Enter

private inline fun consumeTvActivateKey(
    event: androidx.compose.ui.input.key.KeyEvent,
    onActivate: () -> Unit
): Boolean {
    if (!isTvActivateKey(event)) return false
    if (event.type == KeyEventType.KeyDown) onActivate()
    return true
}

private fun requestTvFocus(requester: FocusRequester?): Boolean {
    if (requester == null) return false
    return runCatching {
        requester.requestFocus()
        true
    }.getOrDefault(false)
}

private object TvNavigationMemory {
    var homeListIndex: Int = 0
    var homeListOffset: Int = 0
    var homeRowKey: String? = null
    var homeItemId: String? = null
    val rowListIndex: MutableMap<String, Int> = mutableMapOf()
    val rowListOffset: MutableMap<String, Int> = mutableMapOf()

    // M10 Stage 3: preserve the last meaningful focus target on a details page.
    // This keeps TV navigation predictable after a source dialog or player round trip.
    var detailKey: String? = null
    var detailFocusTarget: String? = null
}

private const val TvLayoutPrefsName = "nustrim_tv_layout"
private const val SubtitleFontSizeKey = "subtitle_font_size"
private const val SubtitleBoldKey = "subtitle_bold"

private fun readDefaultSubtitleFontSize(context: Context): Int =
    context.getSharedPreferences(TvLayoutPrefsName, Context.MODE_PRIVATE)
        .getInt(SubtitleFontSizeKey, 18)
        .coerceIn(12, 32)

private fun writeDefaultSubtitleFontSize(context: Context, value: Int) {
    context.getSharedPreferences(TvLayoutPrefsName, Context.MODE_PRIVATE)
        .edit()
        .putInt(SubtitleFontSizeKey, value.coerceIn(12, 32))
        .apply()
}

private fun readDefaultSubtitleBold(context: Context): Boolean =
    context.getSharedPreferences(TvLayoutPrefsName, Context.MODE_PRIVATE)
        .getBoolean(SubtitleBoldKey, false)

private fun writeDefaultSubtitleBold(context: Context, value: Boolean) {
    context.getSharedPreferences(TvLayoutPrefsName, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(SubtitleBoldKey, value)
        .apply()
}

private data class SubtitleLanguageOption(val code: String, val label: String)

private val SubtitleLanguageOptions = listOf(
    SubtitleLanguageOption("ms", "Malay"),
    SubtitleLanguageOption("en", "English"),
    SubtitleLanguageOption("id", "Indonesian"),
    SubtitleLanguageOption("zh", "Chinese"),
    SubtitleLanguageOption("ja", "Japanese"),
    SubtitleLanguageOption("ko", "Korean"),
    SubtitleLanguageOption("th", "Thai"),
    SubtitleLanguageOption("ar", "Arabic"),
    SubtitleLanguageOption("es", "Spanish"),
    SubtitleLanguageOption("fr", "French"),
    SubtitleLanguageOption("de", "German")
)

private fun subtitleLanguageLabel(code: String): String =
    SubtitleLanguageOptions.firstOrNull { it.code == code }?.label ?: friendlySubtitleLanguage(code)

private fun normalizeSubtitleLanguageCode(code: String): String {
    val clean = code.trim().lowercase(Locale.ROOT).substringBefore('-').substringBefore('_')
    return when (clean) {
        "eng" -> "en"
        "msa", "may" -> "ms"
        "ind" -> "id"
        "zho", "chi" -> "zh"
        "jpn" -> "ja"
        "kor" -> "ko"
        "tha" -> "th"
        "ara" -> "ar"
        "spa" -> "es"
        "fra", "fre" -> "fr"
        "deu", "ger" -> "de"
        else -> clean
    }
}

private fun subtitleLanguageMatches(trackCode: String, trackLabel: String, preferredCode: String): Boolean {
    if (preferredCode.isBlank()) return false
    val preferred = normalizeSubtitleLanguageCode(preferredCode)
    val code = normalizeSubtitleLanguageCode(trackCode)
    if (code == preferred) return true
    val option = SubtitleLanguageOptions.firstOrNull { it.code == preferred }
    return option?.label?.let { trackLabel.equals(it, ignoreCase = true) } == true
}

private data class UiMediaEntry(
    val sourceUrl: String,
    val session: SourceSession,
    val item: MediaItem,
    val catalogName: String
)

private data class CatalogSlice(
    val sourceUrl: String,
    val session: SourceSession,
    val catalog: MediaCatalog
) {
    val preferenceKey: String
        get() = "$sourceUrl|${catalog.name.trim()}"
}

private data class CatalogOrderEntry(
    val key: String,
    val title: String,
    val sourceName: String
)

private data class SourceProbe(
    val url: String,
    val loading: Boolean = true,
    val name: String = "Checking...",
    val kind: SourceKind? = null,
    val description: String = "",
    val error: String = "",
    val capabilities: Set<String> = emptySet(),
    val searchable: Boolean = false,
    val configurable: Boolean = false,
    val configurationRequired: Boolean = false,
    val configureUrl: String = ""
)

private data class PlaybackRequest(
    val sourceUrl: String,
    val session: SourceSession,
    val item: MediaItem,
    val episode: MediaEpisode?,
    val nextEpisode: MediaEpisode?,
    val source: StreamSource,
    val availableSources: List<StreamSource>,
    val title: String,
    val forceStartAtZero: Boolean = false
)

private fun orderedSeasonNumbers(episodes: List<MediaEpisode>): List<Int> =
    episodes.mapNotNull { it.season }.distinct().sortedWith(
        compareBy<Int> { if (it == 0) Int.MAX_VALUE else it }
    )

private fun preferredSeason(episodes: List<MediaEpisode>): Int? {
    val seasons = orderedSeasonNumbers(episodes)
    return seasons.firstOrNull { it > 0 } ?: seasons.firstOrNull()
}

private fun preferredEpisode(episodes: List<MediaEpisode>): MediaEpisode? {
    val season = preferredSeason(episodes)
    return episodes
        .filter { season == null || it.season == season }
        .sortedBy { it.episode ?: Int.MAX_VALUE }
        .firstOrNull()
        ?: episodes.firstOrNull()
}

private fun seasonLabel(season: Int): String = if (season == 0) "Specials" else "Season $season"

private sealed interface Screen {
    data class Main(val section: MainSection) : Screen
    data class Addons(val parent: Screen) : Screen
    data class DiagnosticsLog(val parent: Screen) : Screen

    data class Catalog(
        val session: SourceSession,
        val catalog: MediaCatalog,
        val parent: Screen
    ) : Screen

    data class Detail(
        val sourceUrl: String,
        val session: SourceSession,
        val item: MediaItem,
        val parent: Screen,
        val autoPlayEpisodeId: String? = null,
        val autoOpenSources: Boolean = false,
        val autoOpenSourcesEpisodeId: String? = null,
        val forceStartAtZero: Boolean = false
    ) : Screen

    data class Player(
        val parent: Screen,
        val request: PlaybackRequest
    ) : Screen

    data class Working(val message: String, val parent: Screen) : Screen
    data class Failure(val message: String, val parent: Screen) : Screen
}


private enum class SettingsPage {
    ROOT,
    TRACKING,
    TRAKT,
    LAYOUT,
    CONTENT_DISCOVERY,
    CATALOG_ORDER,
    DOWNLOADS,
    PLAYBACK,
    INTEGRATIONS,
    TMDB,
    MDBLIST,
    LOCAL_DATA,
    DEVELOPER,
    UPDATES,
    ABOUT
}

@Composable
fun NustrimApp() {
    val context = LocalContext.current
    val preferences = remember(context) { UiPreferences(context) }
    val sourceStore = remember(context) { InstalledSourceStore(context) }
    val sourceEngine = remember(context) { SourceEngine(context) }
    var interfaceMode by remember { mutableStateOf<InterfaceMode?>(InterfaceMode.MOBILE) }
    var developerMode by remember { mutableStateOf(preferences.developerMode) }
    var remoteTestEnabled by remember {
        mutableStateOf(preferences.developerMode && preferences.remoteTestEnabled)
    }
    var screen: Screen by remember { mutableStateOf(Screen.Main(MainSection.HOME)) }
    val activity = context.findActivity()

    fun switchInterfaceMode(selected: InterfaceMode) {
        preferences.interfaceMode = InterfaceMode.MOBILE
        interfaceMode = InterfaceMode.MOBILE
        screen = Screen.Main(MainSection.HOME)
    }

    LaunchedEffect(interfaceMode, screen is Screen.Player) {
        activity?.requestedOrientation = when {
            interfaceMode == InterfaceMode.TV || screen is Screen.Player ->
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(activity) {
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    LaunchedEffect(Unit) {
        if (developerMode) sourceStore.ensureDeveloperDefaults()
        if (!developerMode && preferences.remoteTestEnabled) {
            preferences.remoteTestEnabled = false
            remoteTestEnabled = false
        }
    }

    fun openCatalogItem(parent: Screen.Catalog, item: MediaItem) {
        val childProvider = item.ref?.sourceKind in setOf(
            "cloudstream-plugin",
            "cloudstream-loaded-provider"
        )
        val opener = parent.session as? ChildSourceOpener
        if (!childProvider || opener == null) {
            screen = Screen.Detail("", parent.session, item, parent)
            return
        }

        screen = Screen.Working("Loading ${item.title} provider...", parent)
        opener.openChild(
            item = item,
            onSuccess = { child ->
                child.loadCatalog(
                    onSuccess = { catalog -> screen = Screen.Catalog(child, catalog, parent) },
                    onError = { error ->
                        screen = Screen.Failure(error.message ?: error.javaClass.simpleName, parent)
                    }
                )
            },
            onError = { error ->
                screen = Screen.Failure(error.message ?: error.javaClass.simpleName, parent)
            }
        )
    }

    fun openLocalEntry(
        parent: Screen.Main,
        entry: LocalMediaEntry,
        autoOpenSources: Boolean = false,
        forceStartAtZero: Boolean = false
    ) {
        screen = Screen.Working("Opening ${entry.title}...", parent)
        sourceEngine.open(
            entry.sourceUrl,
            onSuccess = { session ->
                val seed = entry.toMediaItem()
                fun open(target: MediaItem) {
                    screen = Screen.Detail(
                        sourceUrl = entry.sourceUrl,
                        session = session,
                        item = target,
                        parent = parent,
                        autoOpenSources = autoOpenSources,
                        autoOpenSourcesEpisodeId = entry.episodeId.takeIf { it.isNotBlank() },
                        forceStartAtZero = forceStartAtZero
                    )
                }
                session.loadDetails(
                    seed,
                    onSuccess = { detailed -> open(detailed) },
                    onError = { open(seed) }
                )
            },
            onError = { error ->
                screen = Screen.Failure(error.message ?: error.javaClass.simpleName, parent)
            }
        )
    }

    MaterialTheme(colorScheme = AppColors) {
        CompositionLocalProvider(LocalInterfaceMode provides interfaceMode) {
            val screenContent: @Composable () -> Unit = {
                when (val current = screen) {
                    is Screen.Main -> MainShell(
                        selected = current.section,
                        remoteTestActive = developerMode && remoteTestEnabled,
                        onRemoteTestClose = {
                            preferences.remoteTestEnabled = false
                            remoteTestEnabled = false
                        },
                        onSection = { screen = Screen.Main(it) }
                    ) { isTv, contentFocusRequestToken ->
                        when (current.section) {
                            MainSection.HOME -> HomeScreen(
                                isTv = isTv,
                                focusRequestToken = contentFocusRequestToken,
                                onOpen = { entry ->
                                    screen = Screen.Detail(entry.sourceUrl, entry.session, entry.item, current)
                                },
                                onContinue = { entry -> openLocalEntry(current, entry) },
                                onContinueManual = { entry ->
                                    openLocalEntry(current, entry, autoOpenSources = true)
                                },
                                onContinueStartFromBeginning = { entry ->
                                    openLocalEntry(
                                        current,
                                        entry,
                                        autoOpenSources = true,
                                        forceStartAtZero = true
                                    )
                                },
                                onAddons = { screen = Screen.Addons(current) }
                            )

                            MainSection.SEARCH -> SearchScreen(
                                isTv = isTv,
                                focusRequestToken = contentFocusRequestToken,
                                onOpen = { entry ->
                                    screen = Screen.Detail(entry.sourceUrl, entry.session, entry.item, current)
                                }
                            )

                            MainSection.LIBRARY -> LibraryScreen(
                                isTv = isTv,
                                focusRequestToken = contentFocusRequestToken,
                                onOpen = { entry -> openLocalEntry(current, entry) }
                            )

                            MainSection.SETTINGS -> SettingsScreen(
                                isTv = isTv,
                                focusRequestToken = contentFocusRequestToken,
                                interfaceMode = interfaceMode ?: if (isTv) InterfaceMode.TV else InterfaceMode.MOBILE,
                                developerMode = developerMode,
                                remoteTestEnabled = remoteTestEnabled,
                                onInterfaceMode = { switchInterfaceMode(it) },
                                onDeveloperMode = { enabled ->
                                    preferences.developerMode = enabled
                                    developerMode = enabled
                                    if (!enabled) {
                                        preferences.remoteTestEnabled = false
                                        remoteTestEnabled = false
                                    } else {
                                        sourceStore.ensureDeveloperDefaults()
                                    }
                                },
                                onRemoteTestEnabled = { enabled ->
                                    val active = enabled && developerMode
                                    preferences.remoteTestEnabled = active
                                    remoteTestEnabled = active
                                },
                                onAddons = { screen = Screen.Addons(current) },
                                onDiagnostics = { screen = Screen.DiagnosticsLog(current) }
                            )
                        }
                    }

                    is Screen.Addons -> AddonsScreen(
                        isTv = rememberIsTv(),
                        onBack = { screen = current.parent },
                        onDiagnostic = { session, catalog ->
                            screen = Screen.Catalog(session, catalog, current)
                        }
                    )

                    is Screen.DiagnosticsLog -> DiagnosticsLogScreen(
                    isTv = rememberIsTv(),
                    onBack = { screen = current.parent }
                )


                is Screen.Catalog -> DiagnosticCatalogScreen(
                        isTv = rememberIsTv(),
                        session = current.session,
                        catalog = current.catalog,
                        onItem = { openCatalogItem(current, it) },
                        onBack = { screen = current.parent },
                        onCatalogChanged = { screen = current.copy(catalog = it) }
                    )

                    is Screen.Detail -> DetailScreen(
                        isTv = rememberIsTv(),
                        sourceUrl = current.sourceUrl,
                        session = current.session,
                        item = current.item,
                        autoPlayEpisodeId = current.autoPlayEpisodeId,
                        autoOpenSources = current.autoOpenSources,
                        autoOpenSourcesEpisodeId = current.autoOpenSourcesEpisodeId,
                        onBack = { screen = current.parent },
                        onPlay = { source, allSources, title, detailed, episode ->
                            screen = Screen.Player(
                                parent = current.copy(
                                    autoPlayEpisodeId = null,
                                    autoOpenSources = false,
                                    autoOpenSourcesEpisodeId = null,
                                    forceStartAtZero = false
                                ),
                                request = PlaybackRequest(
                                    sourceUrl = current.sourceUrl,
                                    session = current.session,
                                    item = detailed,
                                    episode = episode,
                                    nextEpisode = detailed.nextEpisodeAfter(episode),
                                    source = source,
                                    availableSources = allSources,
                                    title = title,
                                    forceStartAtZero = current.forceStartAtZero
                                )
                            )
                        }
                    )

                    is Screen.Player -> PlayerScreen(
                        isTv = rememberIsTv(),
                        request = current.request,
                        onBack = { screen = current.parent }
                    )

                    is Screen.Working -> StatusScreen(
                        title = "Loading",
                        message = current.message,
                        loading = true,
                        onBack = { screen = current.parent }
                    )

                    is Screen.Failure -> StatusScreen(
                        title = "Source error",
                        message = current.message,
                        loading = false,
                        onBack = { screen = current.parent }
                    )
                }
            }
            val physicalTvDevice = rememberIsPhysicalTv()
            if (interfaceMode == InterfaceMode.TV) {
                TvApp(
                    onExit = {
                        if (physicalTvDevice) {
                            activity?.finish()
                        } else {
                            switchInterfaceMode(InterfaceMode.MOBILE)
                        }
                    }
                )
            } else {
                Surface(modifier = Modifier.fillMaxSize(), color = AppBackground) {
                                screenContent()
                            }
            }
            if (interfaceMode == null) {
                InterfaceModePicker(onSelect = { switchInterfaceMode(it) })
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun TvReferenceCanvas(content: @Composable () -> Unit) {
    val outerDensity = LocalDensity.current
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val widthPx = with(outerDensity) { maxWidth.toPx() }
        val heightPx = with(outerDensity) { maxHeight.toPx() }
        val referenceDensity = minOf(widthPx / 1280f, heightPx / 720f).coerceAtLeast(0.1f)
        CompositionLocalProvider(LocalDensity provides Density(referenceDensity, fontScale = 1f)) {
            Box(Modifier.requiredSize(1280.dp, 720.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun rememberIsTv(): Boolean {
    LocalInterfaceMode.current?.let { return it == InterfaceMode.TV }
    val configuration = LocalConfiguration.current
    return (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
        Configuration.UI_MODE_TYPE_TELEVISION || configuration.screenWidthDp >= 900
}

@Composable
private fun InterfaceModePicker(onSelect: (InterfaceMode) -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Choose interface") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose the interface for this device.")
                Text(
                    "TV mode can be used on a phone and touch remains enabled for testing. Remote focus and D-pad controls stay active in TV mode.",
                    color = AppTextMuted
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onSelect(InterfaceMode.MOBILE) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Mobile") }
                OutlinedButton(
                    onClick = { onSelect(InterfaceMode.TV) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("TV") }
            }
        }
    )
}

@Composable
private fun MainShell(
    selected: MainSection,
    remoteTestActive: Boolean,
    onRemoteTestClose: () -> Unit,
    onSection: (MainSection) -> Unit,
    content: @Composable (Boolean, Int) -> Unit
) {
    Scaffold(
                containerColor = AppBackground,
                bottomBar = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, AppBackground.copy(alpha = 0.98f))
                                )
                            )
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(34.dp),
                            color = Color(0xFF24262B),
                            tonalElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MainSection.entries.forEach { destination ->
                                    MobileNavItem(
                                        destination = destination,
                                        selected = destination == selected,
                                        onClick = { onSection(destination) }
                                    )
                                }
                            }
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) { content(false, 0) }
            }
}

private fun dispatchTvTestKey(activity: Activity?, keyCode: Int) {
    if (activity == null) return
    activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
    activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
}

@Composable
private fun TvTestRemote(
    activity: Activity?,
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    Surface(
        modifier = modifier
            .width(196.dp)
            .focusProperties { canFocus = false },
        shape = RoundedCornerShape(20.dp),
        color = Color(0xF01A1B20),
        border = BorderStroke(1.dp, AppAccent.copy(alpha = 0.7f)),
        tonalElevation = 14.dp
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Remote test", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Close",
                    color = AppTextMuted,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .focusProperties { canFocus = false }
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            TvTestRemoteKey("↑") { dispatchTvTestKey(activity, KeyEvent.KEYCODE_DPAD_UP) }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TvTestRemoteKey("←") { dispatchTvTestKey(activity, KeyEvent.KEYCODE_DPAD_LEFT) }
                TvTestRemoteKey("OK") { dispatchTvTestKey(activity, KeyEvent.KEYCODE_DPAD_CENTER) }
                TvTestRemoteKey("→") { dispatchTvTestKey(activity, KeyEvent.KEYCODE_DPAD_RIGHT) }
            }
            TvTestRemoteKey("↓") { dispatchTvTestKey(activity, KeyEvent.KEYCODE_DPAD_DOWN) }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TvTestRemoteKey(
                    label = "Vol−",
                    modifier = Modifier.weight(1f)
                ) { dispatchTvTestKey(activity, KeyEvent.KEYCODE_VOLUME_DOWN) }
                TvTestRemoteKey(
                    label = "Vol+",
                    modifier = Modifier.weight(1f)
                ) { dispatchTvTestKey(activity, KeyEvent.KEYCODE_VOLUME_UP) }
            }
            Spacer(Modifier.height(6.dp))
            TvTestRemoteKey(
                label = "Back",
                modifier = Modifier.fillMaxWidth()
            ) { dispatchTvTestKey(activity, KeyEvent.KEYCODE_BACK) }
        }
    }
}

@Composable
private fun TvTestRemoteKey(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .focusProperties { canFocus = false }
            .widthIn(min = 48.dp)
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2A2C32))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MobileNavItem(
    destination: MainSection,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (selected) Color(0xFFF1F1F3) else Color.Transparent,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                destination.icon,
                destination.label,
                tint = if (selected) Color(0xFF222328) else Color(0xFFC4C5CA)
            )
        }
        Text(
            destination.label,
            color = if (selected) Color.White else Color(0xFFB0B2B8),
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun TvNavItem(
    destination: MainSection,
    selected: Boolean,
    expanded: Boolean,
    requester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    onMoveRight: () -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(requester)
            .onFocusChanged {
                focused = it.isFocused
                onFocusChanged(it.isFocused)
            }
            .onPreviewKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight -> {
                        onMoveRight()
                        true
                    }
                    else -> consumeTvActivateKey(event, onClick)
                }
            }
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(28.dp),
        color = when {
            focused -> Color(0xFFE9EDF3)
            selected -> Color(0xFF34373E)
            else -> Color.Transparent
        },
        border = if (focused) BorderStroke(2.dp, Color.White) else null
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (expanded) 14.dp else 0.dp,
                vertical = 12.dp
            ),
            horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                destination.icon,
                destination.label,
                modifier = Modifier.size(24.dp),
                tint = when {
                    focused -> Color(0xFF181A1F)
                    selected -> Color.White
                    else -> Color(0xFFB7BAC2)
                }
            )
            if (expanded) {
                Spacer(Modifier.width(13.dp))
                Text(
                    destination.label,
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    fontWeight = if (focused || selected) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        focused -> Color(0xFF181A1F)
                        selected -> Color.White
                        else -> Color(0xFFB7BAC2)
                    }
                )
            }
        }
    }
}
@Composable
private fun NustrimMark(size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(AppAccent, RoundedCornerShape((size / 4).dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("N", fontWeight = FontWeight.Black, fontSize = (size / 2).sp)
    }
}

@Composable
private fun HomeScreen(
    isTv: Boolean,
    focusRequestToken: Int = 0,
    onOpen: (UiMediaEntry) -> Unit,
    onContinue: (LocalMediaEntry) -> Unit,
    onContinueManual: (LocalMediaEntry) -> Unit,
    onContinueStartFromBeginning: (LocalMediaEntry) -> Unit,
    onAddons: () -> Unit
) {
    val context = LocalContext.current
    val physicalTv = isTv && rememberIsPhysicalTv()
    val engine = remember(context) { SourceEngine(context) }
    val store = remember(context) { InstalledSourceStore(context) }
    val preferences = remember(context) { UiPreferences(context) }
    val mediaStore = remember(context) { LocalMediaStore(context) }
    val contextScope = rememberCoroutineScope()
    var localMediaRevision by remember { mutableIntStateOf(0) }
    val continueWatching = remember(localMediaRevision) { mediaStore.continueWatching() }
    var posterContextEntry by remember { mutableStateOf<UiMediaEntry?>(null) }
    var continueContextEntry by remember { mutableStateOf<LocalMediaEntry?>(null) }
    var manageListsEntry by remember { mutableStateOf<UiMediaEntry?>(null) }
    var contextReturnRequester by remember { mutableStateOf<FocusRequester?>(null) }

    fun restoreContextFocus() {
        val requester = contextReturnRequester
        contextScope.launch {
            delay(90)
            requestTvFocus(requester)
        }
    }
    val focusedTvEntryFlow = remember { MutableStateFlow<UiMediaEntry?>(null) }
    var displayedTvEntry by remember { mutableStateOf<UiMediaEntry?>(null) }

    var refresh by remember { mutableIntStateOf(0) }
    var loading by remember(refresh) { mutableStateOf(true) }
    var failureCount by remember(refresh) { mutableIntStateOf(0) }
    var sourceOrder by remember(refresh) { mutableStateOf<List<String>>(emptyList()) }
    val sectionMap = remember(refresh) { mutableStateMapOf<String, List<CatalogSlice>>() }

    LaunchedEffect(refresh) {
        sectionMap.clear()
        failureCount = 0
        loading = true
        val installed = store.enabledUrls(preferences.developerMode)
        sourceOrder = installed
        var pending = installed.size

        fun finish() {
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
                        finish()
                    } else {
                        val sectioned = session as? CatalogSectionSourceSession
                        if (sectioned != null) {
                            sectioned.loadCatalogSections(
                                onSuccess = { catalogs ->
                                    sectionMap[sourceUrl] = catalogs.map {
                                        CatalogSlice(sourceUrl, session, it)
                                    }
                                    finish()
                                },
                                onError = {
                                    failureCount += 1
                                    sectionMap[sourceUrl] = emptyList()
                                    finish()
                                }
                            )
                        } else {
                            session.loadCatalog(
                                onSuccess = { catalog ->
                                    sectionMap[sourceUrl] = if (catalog.items.isEmpty()) {
                                        emptyList()
                                    } else {
                                        listOf(CatalogSlice(sourceUrl, session, catalog))
                                    }
                                    finish()
                                },
                                onError = {
                                    failureCount += 1
                                    sectionMap[sourceUrl] = emptyList()
                                    finish()
                                }
                            )
                        }
                    }
                },
                onError = {
                    failureCount += 1
                    sectionMap[sourceUrl] = emptyList()
                    finish()
                }
            )
        }
    }

    val rawSections = sourceOrder.flatMap { sectionMap[it].orEmpty() }
    val hiddenCatalogs = preferences.hiddenCatalogKeys
    val savedCatalogOrder = preferences.catalogOrder
    val catalogRanks = savedCatalogOrder.withIndex().associate { it.value to it.index }
    val sections = rawSections
        .filterNot { it.preferenceKey in hiddenCatalogs }
        .sortedWith(
            compareBy<CatalogSlice> { catalogRanks[it.preferenceKey] ?: Int.MAX_VALUE }
                .thenBy { rawSections.indexOf(it) }
        )
    val heroEntries = remember(sections) {
        val seen = mutableSetOf<String>()
        buildList {
            sections.forEach { slice ->
                slice.catalog.items.forEach { media ->
                    val key = "${media.ref?.metaId.orEmpty()}|${media.id}|${media.title}"
                    if (seen.add(key)) {
                        add(UiMediaEntry(slice.sourceUrl, slice.session, media, slice.catalog.name))
                    }
                    if (size >= 6) return@buildList
                }
                if (size >= 6) return@buildList
            }
        }
    }

    LaunchedEffect(heroEntries, isTv) {
        if (isTv && displayedTvEntry == null) {
            displayedTvEntry = heroEntries.firstOrNull()
        }
    }
    LaunchedEffect(isTv, physicalTv) {
        if (isTv) {
            focusedTvEntryFlow.collectLatest { target ->
                target ?: return@collectLatest
                // Keep D-pad traversal independent from backdrop/image work on TV hardware.
                delay(440)
                if (displayedTvEntry?.item?.id != target.item.id) {
                    displayedTvEntry = target
                }
            }
        }
    }

    val pinnedTvHeader = isTv && heroEntries.isNotEmpty()
    // M10 Stage 2: keep the focused artwork as a full-screen backdrop while the
    // first catalog row sits near the visual midpoint, matching a TV-first home rhythm.
    val tvHeaderHeight: Dp = if (isTv) 720.dp else 0.dp
    val tvFirstRowTopInset: Dp = if (isTv) 326.dp else 0.dp
    val firstCatalogKey = sections.firstOrNull()?.preferenceKey

    val homeListState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (isTv) TvNavigationMemory.homeListIndex else 0,
        initialFirstVisibleItemScrollOffset = if (isTv) TvNavigationMemory.homeListOffset else 0
    )
    DisposableEffect(homeListState, isTv) {
        onDispose {
            if (isTv) {
                TvNavigationMemory.homeListIndex = homeListState.firstVisibleItemIndex
                TvNavigationMemory.homeListOffset = homeListState.firstVisibleItemScrollOffset
            }
        }
    }

    Box(Modifier.fillMaxSize()) {


        LazyColumn(
            state = homeListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (pinnedTvHeader) tvFirstRowTopInset else 0.dp),
            contentPadding = PaddingValues(bottom = if (isTv) 42.dp else 22.dp),
            verticalArrangement = Arrangement.spacedBy(if (isTv) 18.dp else 12.dp)
        ) {
            if (heroEntries.isNotEmpty() && !pinnedTvHeader) {
                item(key = "home-mobile-hero", contentType = "hero") {
                    HomeHero(
                        isTv = isTv,
                        entries = heroEntries,
                        onOpen = onOpen
                    )
                }
            }

            if (!loading && continueWatching.isNotEmpty()) {
                item(key = "home-continue", contentType = "media-row") {
                    ContinueWatchingHomeRow(
                        entries = continueWatching,
                        isTv = isTv,
                        reduceMotion = false,
                        autoFocusFirst = false,
                        restoreItemKey = if (isTv && TvNavigationMemory.homeRowKey == "__continue__") {
                            TvNavigationMemory.homeItemId
                        } else {
                            null
                        },
                        onFocused = { entry ->
                            if (isTv) {
                                TvNavigationMemory.homeRowKey = "__continue__"
                                TvNavigationMemory.homeItemId = entry.key
                            }
                        },
                        onLongPress = { entry, requester ->
                            contextReturnRequester = requester
                            continueContextEntry = entry
                        },
                        onOpen = onContinue
                    )
                }
            }

            sections.forEach { slice ->
                val entries = slice.catalog.items.map {
                    UiMediaEntry(slice.sourceUrl, slice.session, it, slice.catalog.name)
                }
                if (entries.isNotEmpty()) {
                    item(key = "catalog:${slice.preferenceKey}", contentType = "media-row") {
                        HomeCatalogRow(
                            title = slice.catalog.name,
                            memoryKey = slice.preferenceKey,
                            entries = entries,
                            isTv = isTv,
                            reduceMotion = false,
                            focusRequestToken = if (slice.preferenceKey == firstCatalogKey) focusRequestToken else 0,
                            autoFocusFirst = isTv &&
                                TvNavigationMemory.homeItemId == null &&
                                slice.preferenceKey == firstCatalogKey,
                            restoreItemId = if (isTv && TvNavigationMemory.homeRowKey == slice.preferenceKey) {
                                TvNavigationMemory.homeItemId
                            } else {
                                null
                            },
                            onOpen = onOpen,
                            onLongPress = { entry, requester ->
                                contextReturnRequester = requester
                                posterContextEntry = entry
                            },
                            onFocused = { entry ->
                                if (isTv) {
                                    TvNavigationMemory.homeRowKey = slice.preferenceKey
                                    TvNavigationMemory.homeItemId = entry.item.id
                                    focusedTvEntryFlow.value = entry
                                }
                            }
                        )
                    }
                }
            }

            if (preferences.developerDiagnostics && failureCount > 0) {
                item(key = "home-diagnostic", contentType = "status") {
                    DiagnosticBanner("$failureCount enabled source(s) failed while loading Home.")
                }
            }
        }

        if (loading && sections.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xD916181D),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text("Loading catalogs...", color = Color(0xFFD5D7DD), fontSize = 14.sp)
                    }
                }
            }
        }

        if (!loading && sections.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "No catalog available",
                    message = "Enable a catalog addon in Addons. Cinemeta is installed by default.",
                    action = "Open Addons",
                    onAction = onAddons
                )
            }
        }
    }

    posterContextEntry?.let { entry ->
        val watched = mediaStore.isWatched(entry.sourceUrl, entry.item)
        TvActionDialog(
            title = entry.item.title,
            subtitle = "Title actions",
            actions = listOf(
                TvDialogAction("Go to details") {
                    posterContextEntry = null
                    onOpen(entry)
                },
                TvDialogAction("Manage Lists") {
                    posterContextEntry = null
                    manageListsEntry = entry
                },
                TvDialogAction(if (watched) "Mark as unwatched" else "Mark as watched") {
                    mediaStore.setWatched(entry.sourceUrl, entry.item, !watched)
                    if (!watched) {
                        mediaStore.recordProgress(
                            entry.sourceUrl,
                            entry.item,
                            episode = null,
                            positionMs = 0L,
                            durationMs = 0L,
                            completed = true
                        )
                    }
                    localMediaRevision += 1
                    posterContextEntry = null
                    restoreContextFocus()
                }
            ),
            onDismiss = {
                posterContextEntry = null
                restoreContextFocus()
            }
        )
    }

    manageListsEntry?.let { entry ->
        val saved = mediaStore.isSaved(entry.sourceUrl, entry.item)
        TvActionDialog(
            title = entry.item.title,
            subtitle = "Manage Lists",
            actions = listOf(
                TvDialogAction(if (saved) "Remove from Library" else "Add to Library") {
                    mediaStore.setSaved(entry.sourceUrl, entry.item, !saved)
                    localMediaRevision += 1
                    manageListsEntry = null
                    restoreContextFocus()
                },
                TvDialogAction("Back") {
                    manageListsEntry = null
                    restoreContextFocus()
                }
            ),
            onDismiss = {
                manageListsEntry = null
                restoreContextFocus()
            }
        )
    }

    continueContextEntry?.let { entry ->
        TvActionDialog(
            title = entry.title,
            subtitle = "Choose what you want to do with this item",
            actions = listOf(
                TvDialogAction("Go to details") {
                    continueContextEntry = null
                    onContinue(entry)
                },
                TvDialogAction("Play manually") {
                    continueContextEntry = null
                    onContinueManual(entry)
                },
                TvDialogAction("Start from beginning") {
                    continueContextEntry = null
                    onContinueStartFromBeginning(entry)
                },
                TvDialogAction("Remove") {
                    mediaStore.remove(entry.sourceUrl, entry.toMediaItem())
                    localMediaRevision += 1
                    continueContextEntry = null
                    restoreContextFocus()
                }
            ),
            onDismiss = {
                continueContextEntry = null
                restoreContextFocus()
            }
        )
    }
}

private data class TvDialogAction(
    val label: String,
    val onClick: () -> Unit
)

@Composable
private fun TvActionDialog(
    title: String,
    subtitle: String,
    actions: List<TvDialogAction>,
    onDismiss: () -> Unit
) {
    val actionLabels = actions.map { it.label }
    val requesters = remember(actionLabels) { List(actions.size) { FocusRequester() } }
    val confirmationRequesters = remember { List(2) { FocusRequester() } }
    var pendingActionIndex by remember(actionLabels) { mutableStateOf<Int?>(null) }
    val pendingAction = pendingActionIndex?.let { actions.getOrNull(it) }

    fun selectAction(index: Int) {
        val action = actions.getOrNull(index) ?: return
        if (tvActionNeedsConfirmation(action.label)) {
            pendingActionIndex = index
        } else {
            action.onClick()
        }
    }

    Dialog(
        onDismissRequest = {
            if (pendingActionIndex != null) pendingActionIndex = null else onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .widthIn(max = 700.dp),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFA17191E),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                tonalElevation = 20.dp
            ) {
                if (pendingAction != null) {
                    Column(
                        modifier = Modifier
                            .focusGroup()
                            .padding(horizontal = 28.dp, vertical = 26.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Confirm action", fontSize = 25.sp, fontWeight = FontWeight.Black)
                        Text(
                            tvActionConfirmationMessage(pendingAction.label),
                            color = AppTextMuted,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TvDialogConfirmButton(
                                label = "Cancel",
                                requester = confirmationRequesters[0],
                                modifier = Modifier.weight(1f),
                                onLeft = { requestTvFocus(confirmationRequesters[1]) },
                                onRight = { requestTvFocus(confirmationRequesters[1]) },
                                onClick = { pendingActionIndex = null }
                            )
                            TvDialogConfirmButton(
                                label = pendingAction.label,
                                requester = confirmationRequesters[1],
                                modifier = Modifier.weight(1f),
                                onLeft = { requestTvFocus(confirmationRequesters[0]) },
                                onRight = { requestTvFocus(confirmationRequesters[0]) },
                                onClick = {
                                    pendingActionIndex = null
                                    pendingAction.onClick()
                                }
                            )
                        }
                        Text(
                            "Back to actions",
                            color = AppTextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .focusGroup()
                            .padding(horizontal = 28.dp, vertical = 26.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(title, fontSize = 25.sp, fontWeight = FontWeight.Black, maxLines = 2)
                        Text(subtitle, color = AppTextMuted, fontSize = 14.sp)
                        Spacer(Modifier.height(2.dp))
                        actions.forEachIndexed { index, action ->
                            var focused by remember(action.label) { mutableStateOf(false) }
                            val scale by animateFloatAsState(
                                targetValue = if (focused) 1.015f else 1f,
                                animationSpec = tween(120),
                                label = "tv_dialog_action_scale"
                            )
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer { scaleX = scale; scaleY = scale }
                                    .focusRequester(requesters[index])
                                    .onFocusChanged { focused = it.isFocused }
                                    .onPreviewKeyEvent { event ->
                                        when {
                                            isTvActivateKey(event) && event.type == KeyEventType.KeyDown -> true
                                            isTvActivateKey(event) && event.type == KeyEventType.KeyUp -> {
                                                selectAction(index)
                                                true
                                            }
                                            event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp -> {
                                                requestTvFocus(requesters.getOrNull((index - 1).coerceAtLeast(0)))
                                                true
                                            }
                                            event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown -> {
                                                requestTvFocus(requesters.getOrNull((index + 1).coerceAtMost(actions.lastIndex)))
                                                true
                                            }
                                            event.type == KeyEventType.KeyDown &&
                                                (event.key == Key.DirectionLeft || event.key == Key.DirectionRight) -> true
                                            else -> false
                                        }
                                    }
                                    .clickable { selectAction(index) }
                                    .focusable(),
                                shape = RoundedCornerShape(16.dp),
                                color = if (focused) Color.White else Color(0xFF25282E),
                                border = if (focused) BorderStroke(2.dp, Color.White) else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Text(
                                    action.label,
                                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 15.dp),
                                    color = if (focused) Color(0xFF17191E) else Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                        Text(
                            "Back to close",
                            color = AppTextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
    LaunchedEffect(actionLabels, pendingActionIndex) {
        delay(100)
        if (pendingActionIndex != null) requestTvFocus(confirmationRequesters.firstOrNull())
        else requestTvFocus(requesters.firstOrNull())
    }
}

private fun tvActionNeedsConfirmation(label: String): Boolean = when (label) {
    "Remove",
    "Remove from Library",
    "Start from beginning",
    "Mark as watched",
    "Mark as unwatched" -> true
    else -> false
}

private fun tvActionConfirmationMessage(label: String): String = when (label) {
    "Remove" -> "Remove this item from Continue Watching?"
    "Remove from Library" -> "Remove this title from your Library?"
    "Start from beginning" -> "Start playback from the beginning instead of resuming?"
    "Mark as watched" -> "Mark this title as watched?"
    "Mark as unwatched" -> "Mark this title as unwatched?"
    else -> "Continue with this action?"
}

@Composable
private fun TvDialogConfirmButton(
    label: String,
    requester: FocusRequester,
    modifier: Modifier = Modifier,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onClick: () -> Unit
) {
    var focused by remember(label) { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .focusRequester(requester)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft -> { onLeft(); true }
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight -> { onRight(); true }
                    event.type == KeyEventType.KeyDown &&
                        (event.key == Key.DirectionUp || event.key == Key.DirectionDown) -> true
                    isTvActivateKey(event) && event.type == KeyEventType.KeyDown -> true
                    isTvActivateKey(event) && event.type == KeyEventType.KeyUp -> { onClick(); true }
                    else -> false
                }
            }
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(14.dp),
        color = if (focused) Color.White else Color(0xFF25282E),
        border = if (focused) BorderStroke(2.dp, Color.White) else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
            color = if (focused) Color(0xFF17191E) else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HomeHeader(
    isTv: Boolean,
    loading: Boolean,
    onRefresh: () -> Unit,
    onAddons: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isTv) 34.dp else 18.dp,
                end = if (isTv) 34.dp else 10.dp,
                top = if (isTv) 24.dp else 16.dp,
                bottom = 4.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NustrimMark(if (isTv) 42 else 34)
        Spacer(Modifier.width(10.dp))
        Text(
            "Nustrim",
            fontSize = if (isTv) 28.sp else 23.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRefresh, enabled = !loading) {
            Icon(Icons.Outlined.Refresh, "Refresh")
        }
        IconButton(onClick = onAddons) {
            Icon(Icons.Outlined.Source, "Addons")
        }
    }
}

@Composable
private fun TvFocusDetailsHeader(
    entry: UiMediaEntry,
    onOpen: (UiMediaEntry) -> Unit,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val item = entry.item
    val artwork = item.backgroundUrl.ifBlank { item.posterUrl }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        Artwork(artwork, item.title, Modifier.fillMaxSize(), ContentScale.Crop)

        // TV-first horizontal fade keeps metadata readable without flattening the artwork.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.00f to AppBackground.copy(alpha = 0.98f),
                            0.20f to AppBackground.copy(alpha = 0.90f),
                            0.43f to AppBackground.copy(alpha = 0.62f),
                            0.68f to AppBackground.copy(alpha = 0.16f),
                            1.00f to Color.Transparent
                        )
                    )
                )
        )

        // Fade the lower half into the page background so catalog rows can visually
        // overlap the backdrop without appearing buried behind the hero.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.42f to Color.Transparent,
                            0.58f to AppBackground.copy(alpha = 0.50f),
                            0.74f to AppBackground.copy(alpha = 0.92f),
                            1.00f to AppBackground
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(0.43f)
                .padding(start = 34.dp, end = 18.dp, top = 48.dp)
        ) {
            Text(
                item.title,
                fontSize = 32.sp,
                lineHeight = 35.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Text(
                listOf(
                    item.type.name.lowercase().replaceFirstChar { it.uppercase() },
                    item.releaseInfo
                ).filter { it.isNotBlank() }.joinToString("  •  "),
                color = Color(0xFFE3E5EA),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.description.isNotBlank()) {
                Spacer(Modifier.height(9.dp))
                Text(
                    item.description,
                    color = Color(0xFFC9CBD1),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onOpen(entry) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.96f),
                    contentColor = Color(0xFF17181C)
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 7.dp)
            ) {
                Text("View Details", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HomeHero(
    isTv: Boolean,
    entries: List<UiMediaEntry>,
    onOpen: (UiMediaEntry) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { entries.size })
    val activeIndex = pagerState.currentPage.coerceIn(0, entries.lastIndex.coerceAtLeast(0))

    LaunchedEffect(pagerState.settledPage, entries.size) {
        if (entries.size > 1) {
            delay(6500)
            if (!pagerState.isScrollInProgress) {
                val settled = pagerState.settledPage.coerceIn(0, entries.lastIndex)
                val nextPage = (settled + 1) % entries.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }
    LaunchedEffect(pagerState.isScrollInProgress, entries.size) {
        if (
            entries.isNotEmpty() &&
            !pagerState.isScrollInProgress &&
            abs(pagerState.currentPageOffsetFraction) > 0.001f
        ) {
            pagerState.scrollToPage(pagerState.currentPage.coerceIn(0, entries.lastIndex))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isTv) 300.dp else 500.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { index ->
            val entry = entries[index]
            val item = entry.item
            val artwork = item.backgroundUrl.ifBlank { item.posterUrl }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onOpen(entry) }
            ) {
                Artwork(artwork, item.title, Modifier.fillMaxSize(), ContentScale.Crop)
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.04f),
                                Color.Black.copy(alpha = 0.08f),
                                Color.Black.copy(alpha = 0.42f),
                                AppBackground
                            )
                        )
                    )
                )
                if (isTv) {
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.78f),
                                    Color.Black.copy(alpha = 0.38f),
                                    Color.Transparent
                                )
                            )
                        )
                    )
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(if (isTv) 0.68f else 1f)
                        .padding(
                            horizontal = if (isTv) 40.dp else 24.dp,
                            vertical = if (isTv) 26.dp else 36.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        item.title,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = if (isTv) 30.sp else 38.sp,
                        lineHeight = if (isTv) 34.sp else 42.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        listOf(
                            item.type.name.lowercase().replaceFirstChar { it.uppercase() },
                            item.releaseInfo
                        ).filter { it.isNotBlank() }.joinToString("  •  "),
                        color = Color(0xFFE7E7EA),
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isTv) 14.sp else 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { onOpen(entry) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF191A1E)
                        ),
                        modifier = Modifier.widthIn(min = if (isTv) 150.dp else 190.dp)
                    ) {
                        Text("View Details", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (entries.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (isTv) 12.dp else 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                entries.indices.forEach { index ->
                    Box(
                        Modifier
                            .size(if (index == activeIndex) 12.dp else 8.dp)
                            .background(
                                if (index == activeIndex) Color.White else Color.White.copy(alpha = 0.48f),
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}
@Composable
private fun ContinueWatchingHomeRow(
    entries: List<LocalMediaEntry>,
    isTv: Boolean,
    reduceMotion: Boolean = false,
    autoFocusFirst: Boolean = false,
    restoreItemKey: String? = null,
    onFocused: (LocalMediaEntry) -> Unit = {},
    onLongPress: (LocalMediaEntry, FocusRequester?) -> Unit = { _, _ -> },
    onOpen: (LocalMediaEntry) -> Unit
) {
    val sidebarRequester = LocalTvSidebarFocusRequester.current
    val visibleEntries = remember(entries, isTv) { entries.take(if (isTv) 18 else 12) }
    val focusRequesters = remember(visibleEntries.map { it.key }) {
        visibleEntries.associate { it.key to FocusRequester() }
    }
    var initialFocusDone by remember { mutableStateOf(false) }
    val rowListState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (isTv) TvNavigationMemory.rowListIndex["__continue__"] ?: 0 else 0,
        initialFirstVisibleItemScrollOffset = if (isTv) TvNavigationMemory.rowListOffset["__continue__"] ?: 0 else 0
    )
    DisposableEffect(rowListState, isTv) {
        onDispose {
            if (isTv) {
                TvNavigationMemory.rowListIndex["__continue__"] = rowListState.firstVisibleItemIndex
                TvNavigationMemory.rowListOffset["__continue__"] = rowListState.firstVisibleItemScrollOffset
            }
        }
    }
    LaunchedEffect(isTv, restoreItemKey, autoFocusFirst, visibleEntries) {
        if (!isTv) return@LaunchedEffect
        val targetKey = when {
            restoreItemKey != null && visibleEntries.any { it.key == restoreItemKey } -> restoreItemKey
            autoFocusFirst -> visibleEntries.firstOrNull()?.key
            else -> null
        }
        if (targetKey != null && !initialFocusDone) {
            delay(240)
            runCatching { focusRequesters[targetKey]?.requestFocus() }
            delay(140)
            if (!initialFocusDone) {
                runCatching { focusRequesters[targetKey]?.requestFocus() }
            }
        }
    }

    Column {
        Text(
            "Continue Watching",
            fontSize = if (isTv) 18.sp else 22.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = if (isTv) 28.dp else 16.dp)
        )
        Spacer(Modifier.height(if (isTv) 7.dp else 10.dp))
        LazyRow(
            state = rowListState,
            modifier = if (isTv) Modifier.focusGroup() else Modifier,
            contentPadding = if (isTv) {
                PaddingValues(start = 28.dp, end = 32.dp, top = 5.dp, bottom = 9.dp)
            } else {
                PaddingValues(horizontal = 16.dp)
            },
            horizontalArrangement = Arrangement.spacedBy(if (isTv) 10.dp else 12.dp)
        ) {
            items(
                count = visibleEntries.size,
                key = { index -> visibleEntries[index].key }
            ) { index ->
                val entry = visibleEntries[index]
                val previousRequester = visibleEntries.getOrNull(index - 1)?.key?.let(focusRequesters::get)
                val nextRequester = visibleEntries.getOrNull(index + 1)?.key?.let(focusRequesters::get)
                var focused by remember { mutableStateOf(false) }
                val focusScale by animateFloatAsState(
                    targetValue = if (isTv && focused && !reduceMotion) 1.035f else 1f,
                    animationSpec = tween(durationMillis = 120),
                    label = "tv-continue-focus-scale"
                )
                var pressStartedAtMs by remember(entry.key) { mutableStateOf(0L) }
                val progress = entry.progressFraction.coerceIn(0f, 1f)
                val openEntry = { onOpen(entry) }
                val openContext = { onLongPress(entry, focusRequesters[entry.key]) }
                Column(
                    modifier = Modifier
                        .width(if (isTv) 180.dp else 220.dp)
                        .then(
                            if (isTv) Modifier.focusRequester(focusRequesters.getValue(entry.key))
                            else Modifier
                        )
                        .zIndex(if (focused) 1f else 0f)
                        .graphicsLayer {
                            scaleX = focusScale
                            scaleY = focusScale
                        }
                        .onFocusChanged {
                            focused = it.isFocused
                            if (it.isFocused) {
                                initialFocusDone = true
                                onFocused(entry)
                            } else {
                                pressStartedAtMs = 0L
                            }
                        }
                        .onPreviewKeyEvent { event ->
                            if (!isTv) {
                                false
                            } else when {
                                isTvActivateKey(event) && event.type == KeyEventType.KeyDown -> {
                                    if (pressStartedAtMs == 0L) {
                                        pressStartedAtMs = System.currentTimeMillis()
                                    }
                                    true
                                }
                                isTvActivateKey(event) && event.type == KeyEventType.KeyUp -> {
                                    val heldMs = if (pressStartedAtMs > 0L) {
                                        System.currentTimeMillis() - pressStartedAtMs
                                    } else {
                                        0L
                                    }
                                    pressStartedAtMs = 0L
                                    if (heldMs >= 650L) openContext() else openEntry()
                                    true
                                }
                                event.type == KeyEventType.KeyDown &&
                                    event.key == Key.DirectionLeft -> when {
                                        previousRequester != null -> requestTvFocus(previousRequester)
                                        sidebarRequester != null -> requestTvFocus(sidebarRequester)
                                        else -> false
                                    }
                                event.type == KeyEventType.KeyDown &&
                                    event.key == Key.DirectionRight &&
                                    nextRequester != null -> requestTvFocus(nextRequester)
                                else -> false
                            }
                        }
                        .then(
                            if (isTv) {
                                Modifier.pointerInput(entry.key) {
                                    detectTapGestures(
                                        onTap = { openEntry() },
                                        onLongPress = { openContext() }
                                    )
                                }
                            } else {
                                Modifier.clickable(onClick = openEntry)
                            }
                        )
                        .focusable()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                        shape = RoundedCornerShape(14.dp),
                        border = if (focused) BorderStroke(2.dp, Color.White) else null,
                        colors = CardDefaults.cardColors(containerColor = AppSurface2)
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            Artwork(
                                entry.backgroundUrl.ifBlank { entry.posterUrl },
                                entry.title,
                                Modifier.fillMaxSize(),
                                ContentScale.Crop
                            )
                            Box(
                                Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth(progress)
                                    .height(4.dp)
                                    .background(Color.White)
                            )
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(entry.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val episode = entry.toEpisode()?.displayTitle.orEmpty()
                    val percent = (progress * 100).toInt()
                    Text(
                        listOf(episode, if (percent > 0) "$percent%" else "").filter { it.isNotBlank() }.joinToString("  •  "),
                        color = AppTextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeCatalogRow(
    title: String,
    memoryKey: String = title,
    entries: List<UiMediaEntry>,
    isTv: Boolean,
    reduceMotion: Boolean = false,
    focusRequestToken: Int = 0,
    autoFocusFirst: Boolean = false,
    restoreItemId: String? = null,
    onOpen: (UiMediaEntry) -> Unit,
    onLongPress: (UiMediaEntry, FocusRequester?) -> Unit = { _, _ -> },
    onFocused: (UiMediaEntry) -> Unit = {}
) {
    // Keep the TV row working set compact. LazyRow still loads more as focus moves.
    val visibleEntries = remember(entries, isTv) { entries.take(18) }
    val focusRequesters = remember(visibleEntries.map { it.item.id }) {
        visibleEntries.associate { it.item.id to FocusRequester() }
    }
    var initialFocusDone by remember { mutableStateOf(false) }
    var handledFocusRequestToken by remember { mutableIntStateOf(0) }
    val rowListState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (isTv) TvNavigationMemory.rowListIndex[memoryKey] ?: 0 else 0,
        initialFirstVisibleItemScrollOffset = if (isTv) TvNavigationMemory.rowListOffset[memoryKey] ?: 0 else 0
    )
    DisposableEffect(rowListState, isTv, memoryKey) {
        onDispose {
            if (isTv) {
                TvNavigationMemory.rowListIndex[memoryKey] = rowListState.firstVisibleItemIndex
                TvNavigationMemory.rowListOffset[memoryKey] = rowListState.firstVisibleItemScrollOffset
            }
        }
    }

    LaunchedEffect(isTv, memoryKey, restoreItemId, autoFocusFirst, focusRequestToken, visibleEntries) {
        if (!isTv) return@LaunchedEffect
        val explicitRequest = focusRequestToken > handledFocusRequestToken
        val targetId = when {
            restoreItemId != null && visibleEntries.any { it.item.id == restoreItemId } -> restoreItemId
            autoFocusFirst || explicitRequest -> visibleEntries.firstOrNull()?.item?.id
            else -> null
        }
        if (targetId != null && (!initialFocusDone || explicitRequest)) {
            delay(if (explicitRequest) 120 else 240)
            runCatching { focusRequesters[targetId]?.requestFocus() }
            delay(120)
            runCatching { focusRequesters[targetId]?.requestFocus() }
            if (explicitRequest) handledFocusRequestToken = focusRequestToken
        }
    }

    Column {
        Text(
            title,
            color = Color.White,
            fontSize = if (isTv) 18.sp else 22.sp,
            fontWeight = if (isTv) FontWeight.SemiBold else FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = if (isTv) 28.dp else 16.dp)
        )
        Spacer(Modifier.height(if (isTv) 7.dp else 9.dp))
        LazyRow(
            state = rowListState,
            modifier = if (isTv) Modifier.focusGroup() else Modifier,
            contentPadding = if (isTv) {
                PaddingValues(start = 28.dp, end = 32.dp, top = 5.dp, bottom = 10.dp)
            } else {
                PaddingValues(horizontal = 16.dp)
            },
            horizontalArrangement = Arrangement.spacedBy(if (isTv) 10.dp else 10.dp)
        ) {
            items(
                count = visibleEntries.size,
                key = { index ->
                    val entry = visibleEntries[index]
                    "${entry.sourceUrl}:${entry.item.ref?.mediaType}:${entry.item.id}:${title}"
                }
            ) { index ->
                val entry = visibleEntries[index]
                HomePosterCard(
                    item = entry.item,
                    isTv = isTv,
                    reduceMotion = reduceMotion,
                    requester = focusRequesters[entry.item.id],
                    previousRequester = visibleEntries.getOrNull(index - 1)?.item?.id?.let(focusRequesters::get),
                    nextRequester = visibleEntries.getOrNull(index + 1)?.item?.id?.let(focusRequesters::get),
                    onFocused = {
                        initialFocusDone = true
                        onFocused(entry)
                    },
                    onLongPress = { onLongPress(entry, focusRequesters[entry.item.id]) },
                    onClick = { onOpen(entry) }
                )
            }
        }
    }
}

@Composable
private fun HomePosterCard(
    item: MediaItem,
    isTv: Boolean,
    reduceMotion: Boolean = false,
    requester: FocusRequester? = null,
    previousRequester: FocusRequester? = null,
    nextRequester: FocusRequester? = null,
    onFocused: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onClick: () -> Unit
) {


    val sidebarRequester = LocalTvSidebarFocusRequester.current
    var focused by remember { mutableStateOf(false) }
    var landscapePreview by remember(item.id) { mutableStateOf(false) }
    var pressStartedAtMs by remember(item.id) { mutableStateOf(0L) }

    LaunchedEffect(isTv, focused, item.id) {
        if (isTv && focused) {
            delay(3_000)
            if (focused) landscapePreview = true
        } else {
            landscapePreview = false
        }
    }

    val targetWidth = if (isTv && landscapePreview) 286.dp else if (isTv) 110.dp else 124.dp
    val width by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = tween(durationMillis = if (isTv) 180 else 220),
        label = "tv-poster-preview-width"
    )
    val focusScale by animateFloatAsState(
        targetValue = if (isTv && focused && !reduceMotion) 1.035f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "tv-poster-focus-scale"
    )
    val requesterModifier = requester?.let { Modifier.focusRequester(it) } ?: Modifier
    val openContext = {
        if (isTv) {
            landscapePreview = false
            onLongPress()
        }
    }

    Column(
        modifier = Modifier
            .width(width)
            .then(requesterModifier)
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                scaleX = focusScale
                scaleY = focusScale
            }
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) {
                    onFocused()
                } else {
                    landscapePreview = false
                    pressStartedAtMs = 0L
                }
            }
            .onPreviewKeyEvent { event ->
                if (!isTv) {
                    false
                } else when {
                    isTvActivateKey(event) && event.type == KeyEventType.KeyDown -> {
                        if (pressStartedAtMs == 0L) {
                            pressStartedAtMs = System.currentTimeMillis()
                        }
                        true
                    }
                    isTvActivateKey(event) && event.type == KeyEventType.KeyUp -> {
                        val heldMs = if (pressStartedAtMs > 0L) {
                            System.currentTimeMillis() - pressStartedAtMs
                        } else {
                            0L
                        }
                        pressStartedAtMs = 0L
                        if (heldMs >= 650L) openContext() else onClick()
                        true
                    }
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionLeft -> when {
                            previousRequester != null -> requestTvFocus(previousRequester)
                            sidebarRequester != null && requester != null -> requestTvFocus(sidebarRequester)
                            else -> false
                        }
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionRight &&
                        nextRequester != null -> requestTvFocus(nextRequester)
                    else -> false
                }
            }
            .then(
                if (isTv) {
                    Modifier.pointerInput(item.id) {
                        detectTapGestures(
                            onTap = { onClick() },
                            onLongPress = { openContext() }
                        )
                    }
                } else {
                    Modifier.clickable(onClick = onClick)
                }
            )
            .focusable()
    ) {
        Card(
            modifier = if (isTv) {
                Modifier.fillMaxWidth().height(165.dp)
            } else {
                Modifier.fillMaxWidth().aspectRatio(2f / 3f)
            },
            shape = RoundedCornerShape(if (isTv) 12.dp else 10.dp),
            border = if (focused) BorderStroke(2.dp, Color.White.copy(alpha = 0.96f)) else null,
            colors = CardDefaults.cardColors(containerColor = AppSurface2)
        ) {
            Artwork(
                if (isTv && landscapePreview) item.backgroundUrl.ifBlank { item.posterUrl } else item.posterUrl,
                item.title,
                Modifier.fillMaxSize(),
                ContentScale.Crop
            )
        }
        Spacer(Modifier.height(if (isTv) 8.dp else 7.dp))
        Text(
            item.title,
            fontSize = if (isTv) 12.sp else 14.sp,
            fontWeight = if (isTv && focused) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (item.releaseInfo.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                item.releaseInfo,
                color = AppTextMuted,
                fontSize = if (isTv) 10.sp else 12.sp,
                maxLines = 1
            )
        }
    }
}

private enum class DiscoverFilterSheet {
    TYPE,
    CATALOG,
    GENRE
}

private val CommonDiscoverGenres = listOf(
    "Action",
    "Adventure",
    "Animation",
    "Biography",
    "Comedy",
    "Crime",
    "Documentary",
    "Drama",
    "Family",
    "Fantasy",
    "History",
    "Horror",
    "Music",
    "Mystery",
    "Romance",
    "Science Fiction",
    "Thriller",
    "War",
    "Western"
)

private fun discoverEntryKey(entry: UiMediaEntry): String =
    "${entry.sourceUrl}|${entry.item.ref?.mediaType}|${entry.item.id}"

private fun discoverCatalogBucket(name: String): String? {
    val clean = name.trim()
    if (clean.isBlank() || clean.startsWith("[")) return null
    val normalized = clean.lowercase(Locale.ROOT)
    return when {
        "popular" in normalized -> "Popular"
        "new" in normalized || "latest" in normalized || "recent" in normalized -> "New"
        "featured" in normalized -> "Featured"
        else -> null
    }
}

@Composable
// M10 Stage 5: NuvioTV 0.7.20-beta inspired TV Search, Library and Settings workspaces.
private fun SearchScreen(
    isTv: Boolean,
    focusRequestToken: Int = 0,
    onOpen: (UiMediaEntry) -> Unit
) {
    val context = LocalContext.current
    val engine = remember(context) { SourceEngine(context) }
    val store = remember(context) { InstalledSourceStore(context) }
    val preferences = remember(context) { UiPreferences(context) }
    val searchFocusRequester = remember { FocusRequester() }
    val sidebarRequester = LocalTvSidebarFocusRequester.current

    LaunchedEffect(isTv, focusRequestToken) {
        if (isTv && focusRequestToken > 0) {
            delay(120)
            requestTvFocus(searchFocusRequester)
        }
    }

    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var discoverLoading by remember { mutableStateOf(true) }
    var searched by remember { mutableStateOf(false) }
    var failures by remember { mutableIntStateOf(0) }
    var typeFilter by remember { mutableStateOf("Movies") }
    var catalogFilter by remember { mutableStateOf("Popular") }
    var genreFilter by remember { mutableStateOf("All Genres") }
    var filterSheet by remember { mutableStateOf<DiscoverFilterSheet?>(null) }

    val results = remember { mutableStateListOf<UiMediaEntry>() }
    val discover = remember { mutableStateListOf<UiMediaEntry>() }
    val resultKeys = remember { mutableSetOf<String>() }
    val discoverKeys = remember { mutableSetOf<String>() }
    val discoverGenres = remember { mutableStateMapOf<String, Set<String>>() }

    LaunchedEffect(Unit) {
        discover.clear()
        discoverKeys.clear()
        discoverLoading = true
        val urls = store.enabledUrls(preferences.developerMode)
        var pending = urls.size
        fun finish() {
            pending -= 1
            if (pending <= 0) discoverLoading = false
        }
        if (urls.isEmpty()) {
            discoverLoading = false
            return@LaunchedEffect
        }
        urls.forEach { url ->
            engine.open(
                url,
                onSuccess = { session ->
                    if (session.kind == SourceKind.CLOUDSTREAM) {
                        finish()
                    } else {
                        val sectioned = session as? CatalogSectionSourceSession
                        if (sectioned != null) {
                            sectioned.loadCatalogSections(
                                onSuccess = { catalogs ->
                                    catalogs.take(8).forEach { catalog ->
                                        val bucket = discoverCatalogBucket(catalog.name)
                                        if (bucket != null) {
                                            catalog.items.take(30).forEach { item ->
                                                val key = "$bucket|${item.ref?.mediaType}:${item.id}"
                                                if (discoverKeys.add(key)) {
                                                    discover += UiMediaEntry(url, session, item, bucket)
                                                }
                                            }
                                        }
                                    }
                                    finish()
                                },
                                onError = { failures += 1; finish() }
                            )
                        } else {
                            session.loadCatalog(
                                onSuccess = { catalog ->
                                    val bucket = discoverCatalogBucket(catalog.name)
                                    if (bucket != null) {
                                        catalog.items.take(48).forEach { item ->
                                            val key = "$bucket|${item.ref?.mediaType}:${item.id}"
                                            if (discoverKeys.add(key)) {
                                                discover += UiMediaEntry(url, session, item, bucket)
                                            }
                                        }
                                    }
                                    finish()
                                },
                                onError = { failures += 1; finish() }
                            )
                        }
                    }
                },
                onError = { failures += 1; finish() }
            )
        }
    }

    LaunchedEffect(
        discoverLoading,
        preferences.tmdbEnrichmentEnabled,
        preferences.tmdbApiKey
    ) {
        if (
            discoverLoading ||
            !preferences.tmdbEnrichmentEnabled ||
            preferences.tmdbApiKey.isBlank()
        ) return@LaunchedEffect
        discover.take(36).forEach { entry ->
            val key = discoverEntryKey(entry)
            if (discoverGenres[key] == null) {
                TmdbClient.metadata(entry.item, preferences.tmdbApiKey)
                    .onSuccess { metadata ->
                        discoverGenres[key] = metadata.genres
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .toSet()
                    }
            }
        }
    }

    fun performSearch() {
        val clean = query.trim()
        if (clean.isBlank()) {
            searched = false
            results.clear()
            resultKeys.clear()
            return
        }
        results.clear()
        resultKeys.clear()
        failures = 0
        searched = true
        loading = true
        val urls = store.enabledUrls(preferences.developerMode)
        var pending = urls.size
        fun finish() {
            pending -= 1
            if (pending <= 0) loading = false
        }
        if (urls.isEmpty()) {
            loading = false
            return
        }
        urls.forEach { url ->
            engine.open(
                url,
                onSuccess = { session ->
                    val searchable = session as? SearchableSourceSession
                    if (searchable == null) {
                        finish()
                    } else {
                        searchable.search(
                            clean,
                            onSuccess = { catalog ->
                                catalog.items.forEach { item ->
                                    val key = "${item.ref?.mediaType}:${item.id}"
                                    if (resultKeys.add(key)) {
                                        results += UiMediaEntry(url, session, item, catalog.name)
                                    }
                                }
                                finish()
                            },
                            onError = { failures += 1; finish() }
                        )
                    }
                },
                onError = { failures += 1; finish() }
            )
        }
    }

    fun matchesType(entry: UiMediaEntry): Boolean {
        if (typeFilter == "All") return true
        val type = (entry.item.ref?.mediaType ?: entry.item.type.name).lowercase()
        return when (typeFilter) {
            "Movies" -> "movie" in type || "film" in type
            "Series" -> "series" in type || "tv" in type || "show" in type || "anime" in type
            else -> true
        }
    }

    fun matchesCatalog(entry: UiMediaEntry): Boolean = entry.catalogName == catalogFilter

    fun entryGenres(entry: UiMediaEntry): Set<String> {
        val enriched = discoverGenres[discoverEntryKey(entry)].orEmpty()
        if (enriched.isNotEmpty()) return enriched
        val haystack = buildString {
            append(entry.catalogName)
            append(' ')
            append(entry.item.description)
            append(' ')
            append(entry.item.releaseInfo)
        }.lowercase(Locale.ROOT)
        return CommonDiscoverGenres.filter { genre ->
            val needle = genre.lowercase(Locale.ROOT)
            needle in haystack ||
                (genre == "Science Fiction" && ("sci-fi" in haystack || "science fiction" in haystack))
        }.toSet()
    }

    fun matchesGenre(entry: UiMediaEntry): Boolean =
        genreFilter == "All Genres" ||
            entryGenres(entry).any { it.equals(genreFilter, ignoreCase = true) }

    val availableCatalogBuckets = discover.map { it.catalogName }.toSet()
    val catalogOptions = listOf("Popular", "New", "Featured")
        .filter { it in availableCatalogBuckets }
        .ifEmpty { listOf("Popular") }

    LaunchedEffect(discoverLoading, discover.size) {
        if (!discoverLoading && catalogFilter !in catalogOptions) {
            catalogFilter = catalogOptions.first()
        }
    }

    val genreOptions = listOf("All Genres") + CommonDiscoverGenres
    val filteredDiscover = discover.filter { entry ->
        matchesType(entry) && matchesCatalog(entry) && matchesGenre(entry)
    }
    val searchContentFocusRequester = remember { FocusRequester() }


        Column(Modifier.fillMaxSize()) {
            PageHeader(title = "Search", subtitle = "Search and discover", isTv = false)
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it.isBlank()) searched = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .focusRequester(searchFocusRequester),
                placeholder = { Text("Search movies, shows...") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                shape = RoundedCornerShape(22.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { performSearch() })
            )
            Spacer(Modifier.height(18.dp))

            if (!searched) {
                Text(
                    "Discover",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { DiscoverFilterButton(typeFilter) { filterSheet = DiscoverFilterSheet.TYPE } }
                    item { DiscoverFilterButton(catalogFilter) { filterSheet = DiscoverFilterSheet.CATALOG } }
                    item { DiscoverFilterButton(genreFilter) { filterSheet = DiscoverFilterSheet.GENRE } }
                }
                Spacer(Modifier.height(10.dp))
            }

            when {
                loading -> LoadingPanel("Searching...")
                searched && results.isEmpty() -> EmptyState(
                    title = "No results",
                    message = "No enabled searchable addon returned a match."
                )
                searched -> MediaGrid(entries = results.filter(::matchesType), isTv = false, onOpen = onOpen)
                discoverLoading -> LoadingPanel("Loading Discover...")
                discover.isEmpty() -> EmptyState(
                    title = "Discover is empty",
                    message = "Enable a catalog addon in Addons to populate Discover."
                )
                filteredDiscover.isEmpty() -> EmptyState(
                    title = "No Discover matches",
                    message = "Try another type, catalog or genre."
                )
                else -> MediaGrid(entries = filteredDiscover, isTv = false, onOpen = onOpen)
            }

            if (preferences.developerDiagnostics && failures > 0) {
                DiagnosticBanner("$failures source(s) failed while loading Search or Discover.")
            }
        }


    when (filterSheet) {
        DiscoverFilterSheet.TYPE -> DiscoverFilterBottomSheet(
            isTv = isTv,
            title = "Select Type",
            options = listOf("Movies", "Series"),
            selected = typeFilter,
            onSelect = {
                typeFilter = it
                filterSheet = null
            },
            onDismiss = { filterSheet = null }
        )
        DiscoverFilterSheet.CATALOG -> DiscoverFilterBottomSheet(
            isTv = isTv,
            title = "Select Catalog",
            options = catalogOptions,
            selected = catalogFilter,
            onSelect = {
                catalogFilter = it
                filterSheet = null
            },
            onDismiss = { filterSheet = null }
        )
        DiscoverFilterSheet.GENRE -> DiscoverFilterBottomSheet(
            isTv = isTv,
            title = "Select Genre",
            options = genreOptions,
            selected = genreFilter,
            onSelect = {
                genreFilter = it
                filterSheet = null
            },
            onDismiss = { filterSheet = null }
        )
        null -> Unit
    }
}

@Composable
private fun TvSearchFilterPill(
    label: String,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (consumeTvActivateKey(event, onClick)) true else false
            }
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(99.dp),
        color = if (focused) Color.White else AppSurface2,
        border = BorderStroke(1.dp, if (focused) Color.White else Color.White.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = if (focused) AppBackground else Color.White,
                fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(7.dp))
            Text("⌄", color = if (focused) AppBackground else AppTextMuted)
        }
    }
}

@Composable
private fun TvSearchState(title: String, subtitle: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Search, null, modifier = Modifier.size(44.dp), tint = AppTextMuted)
            Spacer(Modifier.height(12.dp))
            Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = AppTextMuted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun TvSearchMediaGrid(
    entries: List<UiMediaEntry>,
    firstFocusRequester: FocusRequester,
    onOpen: (UiMediaEntry) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(132.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 32.dp, end = 32.dp, top = 8.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(
            entries,
            key = { "tv-search:${it.sourceUrl}:${it.item.ref?.mediaType}:${it.item.id}" }
        ) { entry ->
            val isFirst = entry === entries.firstOrNull()
            HomePosterCard(
                item = entry.item,
                isTv = true,
                requester = if (isFirst) firstFocusRequester else null,
                onClick = { onOpen(entry) }
            )
        }
    }
}

@Composable
private fun DiscoverFilterButton(
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(6.dp))
        Text("⌄", color = AppTextMuted)
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DiscoverFilterBottomSheet(
    isTv: Boolean = false,
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (isTv) {
        TvDiscoverChoiceDialog(
            title = title,
            options = options,
            selected = selected,
            onSelect = onSelect,
            onDismiss = onDismiss
        )
        return
    }
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF191A1E),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 8.dp)
                    .width(56.dp)
                    .height(5.dp)
                    .background(Color(0xFF303238), RoundedCornerShape(99.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Text(
                title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
            HorizontalDivider(color = Color(0xFF2A2C31))
            LazyColumn(Modifier.heightIn(max = 520.dp)) {
                items(options.distinct()) { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            option,
                            modifier = Modifier.weight(1f),
                            fontSize = 18.sp,
                            fontWeight = if (option == selected) FontWeight.Bold else FontWeight.Medium
                        )
                        if (option == selected) {
                            Text("✓", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    HorizontalDivider(color = Color(0xFF26282D))
                }
            }
        }
    }
}

@Composable
private fun TvDiscoverChoiceDialog(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val unique = remember(options) { options.distinct() }
    val requesters = remember(unique) { List(unique.size) { FocusRequester() } }
    val selectedIndex = unique.indexOf(selected).takeIf { it >= 0 } ?: 0
    BackHandler(onBack = onDismiss)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.62f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.width(560.dp).heightIn(max = 560.dp),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFA17191E),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text(title, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(14.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(unique.size) { index ->
                            val option = unique[index]
                            var focused by remember(option) { mutableStateOf(false) }
                            val choose = { onSelect(option) }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(requesters[index])
                                    .onFocusChanged { focused = it.isFocused }
                                    .onPreviewKeyEvent { event ->
                                        when {
                                            consumeTvActivateKey(event, choose) -> true
                                            event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft -> true
                                            event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight -> true
                                            else -> false
                                        }
                                    }
                                    .clickable(onClick = choose)
                                    .focusable(),
                                shape = RoundedCornerShape(14.dp),
                                color = when {
                                    focused -> Color.White
                                    option == selected -> AppAccentSoft
                                    else -> AppSurface2
                                },
                                border = if (option == selected && !focused) BorderStroke(1.dp, AppAccent) else null
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        option,
                                        modifier = Modifier.weight(1f),
                                        color = if (focused) AppBackground else Color.White,
                                        fontWeight = if (option == selected || focused) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (option == selected) {
                                        Text("✓", color = if (focused) AppBackground else Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(unique, selectedIndex) {
        delay(90)
        requestTvFocus(requesters.getOrNull(selectedIndex))
    }
}

@Composable
private fun MediaGrid(
    entries: List<UiMediaEntry>,
    isTv: Boolean,
    onOpen: (UiMediaEntry) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(if (isTv) 142.dp else 118.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = if (isTv) 24.dp else 16.dp,
            end = if (isTv) 24.dp else 16.dp,
            top = 4.dp,
            bottom = if (isTv) 24.dp else 20.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(if (isTv) 14.dp else 18.dp)
    ) {
        items(
            entries,
            key = { "${it.sourceUrl}:${it.item.ref?.mediaType}:${it.item.id}" }
        ) { entry ->
            HomePosterCard(entry.item, isTv) { onOpen(entry) }
        }
    }
}

@Composable
private fun LibraryScreen(
    isTv: Boolean,
    focusRequestToken: Int = 0,
    onOpen: (LocalMediaEntry) -> Unit
) {
    val context = LocalContext.current
    val store = remember(context) { LocalMediaStore(context) }
    val saved = store.saved()
    var gridMode by remember { mutableStateOf(true) }
    val libraryFocusRequester = remember { FocusRequester() }
    val sidebarRequester = LocalTvSidebarFocusRequester.current

    LaunchedEffect(isTv, focusRequestToken) {
        if (isTv && focusRequestToken > 0) {
            delay(120)
            requestTvFocus(libraryFocusRequester)
        }
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 18.dp, end = 18.dp, top = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Library",
                modifier = Modifier.weight(1f),
                fontSize = 34.sp,
                fontWeight = FontWeight.Black
            )
            OutlinedButton(
                onClick = { gridMode = !gridMode },
                contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp)
            ) {
                Text(if (gridMode) "▦" else "☰", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = true, onClick = {}, label = { Text("Saved") })
            FilterChip(selected = false, onClick = {}, enabled = false, label = { Text("Cloud") })
        }
        Spacer(Modifier.height(16.dp))

        if (saved.isEmpty()) {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = AppSurface) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 17.dp)) {
                    Text("Your library is empty", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Saved titles will appear here after you tap Save on a details screen.",
                        color = AppTextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else if (gridMode) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(112.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(saved, key = { it.key }) { entry ->
                    LibrarySavedGridCard(entry = entry, isTv = false, onOpen = onOpen)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                contentPadding = PaddingValues(bottom = 18.dp)
            ) {
                items(saved, key = { it.key }) { entry ->
                    LibrarySavedListRow(entry = entry, onOpen = onOpen)
                }
            }
        }
    }
}

@Composable
private fun TvLibraryControl(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    requester: FocusRequester? = null,
    onMoveLeft: (() -> Boolean)? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .then(requester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .graphicsLayer(alpha = if (enabled) 1f else 0.45f)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                when {
                    enabled && consumeTvActivateKey(event, onClick) -> true
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft -> onMoveLeft?.invoke() ?: false
                    else -> false
                }
            }
            .then(if (enabled) Modifier.clickable(onClick = onClick).focusable() else Modifier),
        shape = RoundedCornerShape(99.dp),
        color = when {
            focused -> Color.White
            selected -> AppAccentSoft
            else -> AppSurface2
        },
        border = when {
            focused -> BorderStroke(2.dp, Color.White)
            selected -> BorderStroke(1.dp, AppAccent)
            else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
        }
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            color = if (focused) AppBackground else Color.White,
            fontWeight = if (focused || selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun LibrarySavedGridCard(
    entry: LocalMediaEntry,
    isTv: Boolean,
    onOpen: (LocalMediaEntry) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isTv && focused) 1.04f else 1f,
        label = "library-grid-focus"
    )
    Column(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable { onOpen(entry) }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
            border = if (focused) BorderStroke(2.dp, Color.White) else null,
            colors = CardDefaults.cardColors(containerColor = AppSurface2)
        ) {
            Artwork(
                entry.posterUrl.ifBlank { entry.backgroundUrl },
                entry.title,
                Modifier.fillMaxSize(),
                ContentScale.Crop
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            entry.title,
            fontWeight = FontWeight.Bold,
            fontSize = if (isTv) 12.sp else 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LibrarySavedListRow(
    entry: LocalMediaEntry,
    onOpen: (LocalMediaEntry) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable { onOpen(entry) },
        shape = RoundedCornerShape(15.dp),
        color = if (focused) AppSurface2 else AppSurface,
        border = if (focused) BorderStroke(2.dp, Color.White) else null
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Artwork(
                entry.posterUrl.ifBlank { entry.backgroundUrl },
                entry.title,
                Modifier.width(72.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(9.dp)),
                ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val episode = entry.toEpisode()?.displayTitle.orEmpty()
                if (episode.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(episode, color = AppTextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun LocalMediaRail(
    entries: List<LocalMediaEntry>,
    isTv: Boolean,
    showProgress: Boolean,
    onOpen: (LocalMediaEntry) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(if (isTv) 10.dp else 10.dp)) {
        items(entries, key = { it.key }) { entry ->
            var focused by remember { mutableStateOf(false) }
            val focusScale by animateFloatAsState(
                targetValue = if (isTv && focused) 1.045f else 1f,
                label = "library-focus"
            )
            Column(
                modifier = Modifier
                    .width(if (isTv) 146.dp else 118.dp)
                    .graphicsLayer { scaleX = focusScale; scaleY = focusScale }
                    .onFocusChanged { focused = it.isFocused }
                    .focusable()
                    .clickable { onOpen(entry) }
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
                    border = if (focused) BorderStroke(2.dp, Color.White) else null,
                    colors = CardDefaults.cardColors(containerColor = AppSurface2)
                ) {
                    Artwork(entry.posterUrl.ifBlank { entry.backgroundUrl }, entry.title, Modifier.fillMaxSize(), ContentScale.Crop)
                }
                Spacer(Modifier.height(6.dp))
                Text(entry.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (showProgress) {
                    val episodeLabel = entry.toEpisode()?.displayTitle.orEmpty()
                    val progress = (entry.progressFraction * 100).toInt().coerceIn(0, 100)
                    Text(
                        listOf(episodeLabel, if (progress > 0) "$progress%" else "").filter { it.isNotBlank() }.joinToString(" · "),
                        color = AppTextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AddonsScreen(
    isTv: Boolean,
    onBack: () -> Unit,
    onDiagnostic: (SourceSession, MediaCatalog) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val engine = remember(context) { SourceEngine(context) }
    val store = remember(context) { InstalledSourceStore(context) }
    val healthStore = remember(context) { SourceHealthStore(context) }
    val providerStore = remember(context) { CloudStreamProviderStore(context) }
    val diagnosticRunner = remember { ProviderDiagnosticRunner() }
    val preferences = remember(context) { UiPreferences(context) }

    var url by remember { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }
    var providerRevision by remember { mutableIntStateOf(0) }
    var adding by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val probes = remember { mutableStateMapOf<String, SourceProbe>() }
    val expandedRepositories = remember { mutableStateMapOf<String, Boolean>() }
    val repositoryLoading = remember { mutableStateMapOf<String, Boolean>() }
    val repositoryCatalogs = remember { mutableStateMapOf<String, MediaCatalog>() }
    val repositorySessions = remember { mutableStateMapOf<String, SourceSession>() }
    val repositoryErrors = remember { mutableStateMapOf<String, String>() }
    var diagnosticReport by remember { mutableStateOf<ProviderDiagnosticReport?>(null) }

    if (preferences.developerMode) store.ensureDeveloperDefaults()
    val installed = remember(revision, preferences.developerMode) {
        store.visibleSources(preferences.developerMode)
    }

    LaunchedEffect(revision, preferences.developerMode) {
        installed.forEach { source ->
            probes[source.url] = SourceProbe(source.url)
            engine.open(
                source.url,
                onSuccess = { session ->
                    probes[source.url] = SourceProbe(
                        url = source.url,
                        loading = false,
                        name = session.displayName,
                        kind = session.kind,
                        description = session.description,
                        capabilities = session.capabilities.resources,
                        searchable = session.capabilities.searchable,
                        configurable = session.capabilities.configurable,
                        configurationRequired = session.capabilities.configurationRequired,
                        configureUrl = session.capabilities.configureUrl
                    )
                },
                onError = { throwable ->
                    probes[source.url] = SourceProbe(
                        url = source.url,
                        loading = false,
                        name = source.label.ifBlank { "Unavailable addon" },
                        error = throwable.message ?: throwable.javaClass.simpleName
                    )
                }
            )
        }
    }

    fun addAddon() {
        val clean = url.trim()
        if (clean.isBlank()) return
        adding = true
        error = ""
        engine.open(
            clean,
            onSuccess = {
                store.add(clean)
                adding = false
                url = ""
                revision += 1
            },
            onError = {
                adding = false
                error = it.message ?: it.javaClass.simpleName
            }
        )
    }

    fun loadRepository(source: InstalledSource) {
        if (repositoryLoading[source.url] == true) return
        repositoryLoading[source.url] = true
        repositoryErrors.remove(source.url)
        engine.open(
            source.url,
            onSuccess = { session ->
                repositorySessions[source.url] = session
                session.loadCatalog(
                    onSuccess = { catalog ->
                        repositoryLoading[source.url] = false
                        repositoryCatalogs[source.url] = catalog
                        healthStore.recordSuccess(source.url)
                    },
                    onError = { throwable ->
                        repositoryLoading[source.url] = false
                        repositoryErrors[source.url] = throwable.message ?: throwable.javaClass.simpleName
                        healthStore.recordFailure(source.url, throwable)
                    }
                )
            },
            onError = { throwable ->
                repositoryLoading[source.url] = false
                repositoryErrors[source.url] = throwable.message ?: throwable.javaClass.simpleName
                healthStore.recordFailure(source.url, throwable)
            }
        )
    }

    fun diagnoseProvider(source: InstalledSource, providerItem: MediaItem) {
        val session = repositorySessions[source.url]
        if (session == null) {
            diagnosticReport = ProviderDiagnosticReport(
                providerName = providerItem.title,
                status = ProviderDiagnosticStatus.FAILED,
                primaryError = "Repository is not loaded.",
                stages = emptyList(),
                events = listOf("Open the provider list again and retry."),
                elapsedMs = 0L
            )
            return
        }
        diagnosticReport = ProviderDiagnosticReport(
            providerName = providerItem.title,
            status = ProviderDiagnosticStatus.RUNNING,
            primaryError = "",
            stages = emptyList(),
            events = listOf("Starting diagnostic..."),
            elapsedMs = 0L
        )
        diagnosticRunner.run(session, providerItem) { report ->
            diagnosticReport = report
        }
    }
    fun openRepositoryProvider(source: InstalledSource, providerItem: MediaItem) {
        val repository = repositorySessions[source.url]
        val opener = repository as? ChildSourceOpener
        if (repository == null || opener == null) {
            error = "Repository is not loaded."
            return
        }
        error = ""
        opener.openChild(
            providerItem,
            onSuccess = { child ->
                child.loadCatalog(
                    onSuccess = { catalog -> onDiagnostic(child, catalog) },
                    onError = { catalogError ->
                        val searchable = child as? SearchableSourceSession
                        if (searchable == null) {
                            error = catalogError.message ?: catalogError.javaClass.simpleName
                        } else {
                            searchable.search(
                                "One Piece",
                                onSuccess = { catalog -> onDiagnostic(child, catalog) },
                                onError = { searchError ->
                                    error = searchError.message ?: searchError.javaClass.simpleName
                                }
                            )
                        }
                    }
                )
            },
            onError = { throwable ->
                error = throwable.message ?: throwable.javaClass.simpleName
            }
        )
    }


    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            horizontal = if (isTv) 24.dp else 16.dp,
            vertical = if (isTv) 16.dp else 14.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                }
                Spacer(Modifier.width(4.dp))
                PageHeaderInline("Addons", "Manage installed addon sources")
            }
        }

        item {
            Text("Add Addon", style = MaterialTheme.typography.titleMedium, color = AppTextMuted)
            Spacer(Modifier.height(8.dp))
            SectionSurface {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Manifest or repository URL") },
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { addAddon() },
                    enabled = url.isNotBlank() && !adding,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (adding) "Checking..." else "Add Addon")
                }
                if (error.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        item {
            Text("Installed Addons", style = MaterialTheme.typography.titleMedium, color = AppTextMuted)
        }

        if (installed.isEmpty()) {
            item { EmptyState("No addons", "Add a Stremio manifest or repository.") }
        } else {
            items(installed, key = { it.url }) { source ->
                val isRepository = source.url.substringBefore('?').endsWith("repo.json", ignoreCase = true)
                val expanded = expandedRepositories[source.url] == true
                AddonCard(
                    source = source,
                    probe = probes[source.url],
                    developerMode = preferences.developerMode,
                    isRepository = isRepository,
                    repositoryExpanded = expanded,
                    onRepositoryToggle = {
                        val next = !expanded
                        expandedRepositories[source.url] = next
                        if (next && repositoryCatalogs[source.url] == null) loadRepository(source)
                    },
                    onRetry = { revision += 1 },
                    onEnabled = { enabled ->
                        store.setEnabled(source.url, enabled)
                        revision += 1
                    },
                    onRemove = {
                        store.remove(source.url)
                        SourceHealthStore(context).clear(source.url)
                        revision += 1
                    },
                    onMoveUp = {
                        store.move(source.url, -1)
                        revision += 1
                    },
                    onMoveDown = {
                        store.move(source.url, 1)
                        revision += 1
                    },
                    onOpen = {
                        engine.open(
                            source.url,
                            onSuccess = { session ->
                                session.loadCatalog(
                                    onSuccess = { catalog -> onDiagnostic(session, catalog) },
                                    onError = { throwable ->
                                        error = throwable.message ?: throwable.javaClass.simpleName
                                    }
                                )
                            },
                            onError = { throwable ->
                                error = throwable.message ?: throwable.javaClass.simpleName
                            }
                        )
                    }
                )

                if (isRepository && expanded) {
                    Spacer(Modifier.height(8.dp))
                    RepositoryProviderList(
                        source = source,
                        session = repositorySessions[source.url],
                        catalog = repositoryCatalogs[source.url],
                        loading = repositoryLoading[source.url] == true,
                        error = repositoryErrors[source.url].orEmpty(),
                        providerStore = providerStore,
                        revision = providerRevision,
                        onEnabled = { session, item, enabled ->
                            providerStore.setEnabled(session.id, item, enabled)
                            providerRevision += 1
                        },
                        onOpen = { item -> openRepositoryProvider(source, item) },
                        onDiagnose = { item -> diagnoseProvider(source, item) }
                    )
                }
            }
        }
    }

    diagnosticReport?.let { report ->
        ProviderDiagnosticDialog(
            report = report,
            onCopy = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Nustrim provider diagnose", report.copyText()))
            },
            onClose = { diagnosticReport = null }
        )
    }
}

@Composable
private fun RepositoryProviderList(
    source: InstalledSource,
    session: SourceSession?,
    catalog: MediaCatalog?,
    loading: Boolean,
    error: String,
    providerStore: CloudStreamProviderStore,
    revision: Int,
    onEnabled: (SourceSession, MediaItem, Boolean) -> Unit,
    onOpen: (MediaItem) -> Unit,
    onDiagnose: (MediaItem) -> Unit
) {
    SectionSurface {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Providers", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                catalog?.items?.size?.let { "$it installed" }.orEmpty(),
                color = AppTextMuted,
                fontSize = 11.sp
            )
        }
        if (loading) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Loading providers...", color = AppTextMuted, fontSize = 12.sp)
            }
        }
        if (error.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = Color(0xFFFF949B), fontSize = 12.sp)
        }
        val items = catalog?.items.orEmpty()
        if (!loading && error.isBlank() && items.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("No provider packages found.", color = AppTextMuted, fontSize = 12.sp)
        }
        if (session != null && items.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { providerItem ->
                    val enabled = providerStore.isEnabled(session.id, providerItem)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(13.dp),
                        color = AppSurface2
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        providerItem.title,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val summary = providerItem.description.lineSequence()
                                        .firstOrNull { it.isNotBlank() }
                                        .orEmpty()
                                    if (summary.isNotBlank()) {
                                        Text(
                                            summary,
                                            color = AppTextMuted,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Switch(
                                    checked = enabled,
                                    onCheckedChange = { onEnabled(session, providerItem, it) }
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    if (enabled) "Enabled" else "Disabled",
                                    color = if (enabled) Color(0xFF75D89B) else AppTextMuted,
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = { onOpen(providerItem) },
                                    enabled = enabled,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) { Text("Open", fontSize = 12.sp) }
                                OutlinedButton(
                                    onClick = { onDiagnose(providerItem) },
                                    enabled = enabled,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) { Text("Diagnose", fontSize = 12.sp) }
                            }
                        }
                    }
                }
            }
        }
        if (revision == Int.MIN_VALUE) Text(source.url)
    }
}

@Composable
private fun ProviderDiagnosticDialog(
    report: ProviderDiagnosticReport,
    onCopy: () -> Unit,
    onClose: () -> Unit
) {
    var showEvents by remember(report.providerName) { mutableStateOf(false) }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 860.dp)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF18191D),
            tonalElevation = 12.dp
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Provider Diagnose", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(report.providerName, color = AppTextMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF24262B)
                    ) {
                        Text(
                            when (report.status) {
                                ProviderDiagnosticStatus.RUNNING -> "RUNNING"
                                ProviderDiagnosticStatus.WORKING -> "WORKING"
                                ProviderDiagnosticStatus.PARTIAL -> "PARTIAL"
                                ProviderDiagnosticStatus.FAILED -> "FAILED"
                            },
                            color = when (report.status) {
                                ProviderDiagnosticStatus.WORKING -> Color(0xFF75D89B)
                                ProviderDiagnosticStatus.PARTIAL -> Color(0xFFF0C86E)
                                ProviderDiagnosticStatus.FAILED -> Color(0xFFFF949B)
                                else -> AppTextMuted
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                if (report.primaryError.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF3A2024)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(10.dp)) {
                            Text("Primary error", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(report.primaryError, color = Color(0xFFFFC5C9), fontSize = 11.sp)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(report.stages, key = { it.name }) { stage ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(11.dp),
                            color = Color(0xFF24262B)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stage.state,
                                    color = when (stage.state) {
                                        "OK" -> Color(0xFF75D89B)
                                        "FAILED" -> Color(0xFFFF949B)
                                        "RUNNING" -> Color(0xFFF0C86E)
                                        else -> AppTextMuted
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.width(72.dp)
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(stage.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(
                                        stage.detail,
                                        color = AppTextMuted,
                                        fontSize = 10.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (stage.durationMs > 0L) {
                                    Text("${stage.durationMs}ms", color = AppTextMuted, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                    if (showEvents) {
                        item {
                            Spacer(Modifier.height(4.dp))
                            Text("Diagnostic events", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        items(report.events.takeLast(40)) { event ->
                            Text("• $event", color = AppTextMuted, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showEvents = !showEvents },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text(if (showEvents) "Hide events" else "Events", maxLines = 1, softWrap = false)
                    }
                    OutlinedButton(
                        onClick = onCopy,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)
                    ) { Text("Copy Report", maxLines = 1, softWrap = false) }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.End).widthIn(min = 96.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                ) { Text("Close", maxLines = 1, softWrap = false) }
            }
        }
    }
}

@Composable
private fun AddonCard(
    source: InstalledSource,
    probe: SourceProbe?,
    developerMode: Boolean,
    isRepository: Boolean,
    repositoryExpanded: Boolean,
    onRepositoryToggle: () -> Unit,
    onRetry: () -> Unit,
    onEnabled: (Boolean) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onOpen: () -> Unit
) {
    val context = LocalContext.current
    val health = SourceHealthStore(context).status(source.url)
    val configureUrl = probe?.configureUrl.orEmpty()
    val title = probe?.name?.takeIf { it.isNotBlank() }
        ?: source.label.ifBlank { source.url.substringBefore("/manifest.json").substringAfterLast("/") }

    SectionSurface {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(AppSurface2, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    probe?.loading == true -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    probe?.error?.isNotBlank() == true -> Icon(Icons.Outlined.Info, null, tint = Color(0xFFFF858F))
                    source.enabled -> Icon(Icons.Outlined.CheckCircle, null, tint = Color(0xFF75D89B))
                    else -> Icon(Icons.Outlined.Source, null, tint = AppTextMuted)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when {
                        probe?.error?.isNotBlank() == true -> "Error"
                        probe?.kind != null -> probe.kind.name
                        else -> source.preset.name.replace('_', ' ')
                    },
                    color = AppTextMuted,
                    fontSize = 12.sp
                )
            }
            Switch(checked = source.enabled, onCheckedChange = onEnabled)
        }

        if (probe?.description?.isNotBlank() == true) {
            Spacer(Modifier.height(8.dp))
            Text(probe.description, color = AppTextMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            health.statusLabel,
            color = if (health.consecutiveFailures > 0) Color(0xFFFF949B) else AppTextMuted,
            fontSize = 12.sp
        )
        if (health.lastError.isNotBlank() && health.consecutiveFailures > 0) {
            Text(
                health.lastError,
                color = Color(0xFFFFC5C9),
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        val capabilityLabels = buildList {
            probe?.capabilities?.sorted()?.let { addAll(it) }
            if (probe?.searchable == true && "search" !in this) add("search")
        }
        if (capabilityLabels.isNotEmpty()) {
            Spacer(Modifier.height(7.dp))
            Text(
                capabilityLabels.joinToString(" · ") { it.replaceFirstChar(Char::uppercase) },
                color = Color(0xFFB9C8E8),
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (probe?.configurationRequired == true) {
            Spacer(Modifier.height(5.dp))
            Text("Configuration required", color = Color(0xFFF0C86E), fontSize = 12.sp)
        }

        Spacer(Modifier.height(8.dp))
        Text(source.url, color = Color(0xFF7F828C), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { OutlinedButton(onClick = onMoveUp) { Text("Up") } }
            item { OutlinedButton(onClick = onMoveDown) { Text("Down") } }
            if (probe?.error?.isNotBlank() == true || health.consecutiveFailures > 0) {
                item { OutlinedButton(onClick = onRetry) { Text("Retry") } }
            }
            if (configureUrl.isNotBlank()) {
                item {
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(configureUrl)))
                            }
                        }
                    ) { Text("Configure") }
                }
            }
            if (isRepository) {
                item {
                    OutlinedButton(onClick = onRepositoryToggle, enabled = source.enabled) {
                        Text(if (repositoryExpanded) "Providers ▲" else "Providers ▼")
                    }
                }
            } else if (developerMode) {
                item { OutlinedButton(onClick = onOpen) { Text("Open") } }
            }
            if (!source.isPinned) {
                item {
                    OutlinedButton(onClick = onRemove) {
                        Icon(Icons.Outlined.Delete, null)
                        Spacer(Modifier.width(5.dp))
                        Text("Remove")
                    }
                }
            }
        }
        if (source.isPinned) {
            Spacer(Modifier.height(8.dp))
            Text(
                when (source.preset) {
                    SourcePreset.CORE_DEFAULT -> "Default addon"
                    SourcePreset.DEVELOPER_DEFAULT -> "Developer default"
                    else -> "Pinned addon"
                },
                color = AppTextMuted,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    isTv: Boolean,
    focusRequestToken: Int = 0,
    interfaceMode: InterfaceMode,
    developerMode: Boolean,
    remoteTestEnabled: Boolean,
    onInterfaceMode: (InterfaceMode) -> Unit,
    onDeveloperMode: (Boolean) -> Unit,
    onRemoteTestEnabled: (Boolean) -> Unit,
    onAddons: () -> Unit,
    onDiagnostics: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember(context) { UiPreferences(context) }
    var page by remember { mutableStateOf(SettingsPage.ROOT) }
    var diagnostics by remember { mutableStateOf(preferences.developerDiagnostics) }

    BackHandler(enabled = page != SettingsPage.ROOT) {
        page = SettingsPage.ROOT
    }

    when (page) {
        SettingsPage.ROOT -> SettingsRootScreen(
            isTv = isTv,
            focusRequestToken = focusRequestToken,
            developerMode = developerMode,
            onTracking = { page = SettingsPage.TRACKING },
            onLayout = { page = SettingsPage.LAYOUT },
            onContentDiscovery = { page = SettingsPage.CONTENT_DISCOVERY },
            onDownloads = { page = SettingsPage.DOWNLOADS },
            onPlayback = { page = SettingsPage.PLAYBACK },
            onIntegrations = { page = SettingsPage.INTEGRATIONS },
            onLocalData = { page = SettingsPage.LOCAL_DATA },
            onDeveloper = { page = SettingsPage.DEVELOPER },
            onUpdates = { page = SettingsPage.UPDATES },
            onAbout = { page = SettingsPage.ABOUT },
            onDiagnostics = onDiagnostics
        )

        SettingsPage.TRACKING -> TrackingSettingsScreen(
            isTv = isTv,
            preferences = preferences,
            onTrakt = { page = SettingsPage.TRAKT },
            onBack = { page = SettingsPage.ROOT }
        )

        SettingsPage.TRAKT -> TraktIntegrationSettingsScreen(
            isTv = isTv,
            preferences = preferences,
            onBack = { page = SettingsPage.TRACKING }
        )

        SettingsPage.LAYOUT -> LayoutSettingsScreen(
            isTv = isTv,
            interfaceMode = interfaceMode,
            onInterfaceMode = onInterfaceMode,
            onBack = { page = SettingsPage.ROOT }
        )

        SettingsPage.CONTENT_DISCOVERY -> ContentDiscoverySettingsScreen(
            isTv = isTv,
            onAddons = onAddons,
            onCatalogOrder = { page = SettingsPage.CATALOG_ORDER },
            onBack = { page = SettingsPage.ROOT }
        )

        SettingsPage.CATALOG_ORDER -> CatalogOrderSettingsScreen(
            isTv = isTv,
            preferences = preferences,
            onBack = { page = SettingsPage.CONTENT_DISCOVERY }
        )

        SettingsPage.DOWNLOADS -> DownloadsSettingsScreen(
            isTv = isTv,
            onBack = { page = SettingsPage.ROOT }
        )

        SettingsPage.PLAYBACK -> PlaybackSettingsScreen(
            isTv = isTv,
            onBack = { page = SettingsPage.ROOT }
        )

        SettingsPage.INTEGRATIONS -> IntegrationsSettingsScreen(
            isTv = isTv,
            preferences = preferences,
            onTmdb = { page = SettingsPage.TMDB },
            onMdbList = { page = SettingsPage.MDBLIST },
            onBack = { page = SettingsPage.ROOT }
        )

        SettingsPage.TMDB -> TmdbIntegrationSettingsScreen(
            isTv = isTv,
            preferences = preferences,
            onBack = { page = SettingsPage.INTEGRATIONS }
        )

        SettingsPage.MDBLIST -> MdbListIntegrationSettingsScreen(
            isTv = isTv,
            preferences = preferences,
            onBack = { page = SettingsPage.INTEGRATIONS }
        )

        SettingsPage.LOCAL_DATA -> LocalDataSettingsScreen(
            isTv = isTv,
            interfaceMode = interfaceMode,
            onInterfaceMode = onInterfaceMode,
            developerMode = developerMode,
            diagnostics = diagnostics,
            onDeveloperModeRestored = onDeveloperMode,
            onDiagnosticsRestored = { diagnostics = it },
            onBack = { page = SettingsPage.ROOT }
        )

        SettingsPage.DEVELOPER -> DeveloperSettingsScreen(
            isTv = isTv,
            developerMode = developerMode,
            remoteTestEnabled = remoteTestEnabled,
            diagnostics = diagnostics,
            onDeveloperMode = onDeveloperMode,
            onRemoteTestEnabled = onRemoteTestEnabled,
            onDiagnostics = { diagnostics = it },
            onOpenDiagnostics = onDiagnostics,
            onBack = { page = SettingsPage.ROOT }
        )

        SettingsPage.UPDATES -> UpdateSettingsScreen(
            isTv = isTv,
            onBack = { page = SettingsPage.ROOT }
        )

        SettingsPage.ABOUT -> AboutNustrimSettingsScreen(
            isTv = isTv,
            onBack = { page = SettingsPage.ROOT }
        )
    }
}

@Composable
private fun SettingsRootScreen(
    isTv: Boolean,
    focusRequestToken: Int = 0,
    developerMode: Boolean,
    onTracking: () -> Unit,
    onLayout: () -> Unit,
    onContentDiscovery: () -> Unit,
    onDownloads: () -> Unit,
    onPlayback: () -> Unit,
    onIntegrations: () -> Unit,
    onLocalData: () -> Unit,
    onDeveloper: () -> Unit,
    onUpdates: () -> Unit,
    onAbout: () -> Unit,
    onDiagnostics: () -> Unit
) {
    val firstSettingsFocusRequester = remember { FocusRequester() }
    val sidebarRequester = LocalTvSidebarFocusRequester.current

    LaunchedEffect(isTv, focusRequestToken) {
        if (isTv && focusRequestToken > 0) {
            delay(120)
            requestTvFocus(firstSettingsFocusRequester)
        }
    }



    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { PageHeaderInline("Settings", "Nustrim preferences and integrations") }
        item {
            SettingsSectionTitle("Tracking")
            SectionSurface {
                SettingsNavigationRow(
                    icon = Icons.Outlined.CheckCircle,
                    title = "Tracking",
                    subtitle = "Local watch progress and Trakt sync"
                ) { onTracking() }
            }
        }
        item {
            SettingsSectionTitle("General")
            SectionSurface {
                SettingsNavigationRow(Icons.Outlined.Tv, "Layout", "Mobile and TV interface") { onLayout() }
                SettingsGroupDivider()
                SettingsNavigationRow(Icons.Outlined.Source, "Content Manager", "Addons and catalog management") { onContentDiscovery() }
                SettingsGroupDivider()
                SettingsNavigationRow(Icons.Outlined.LibraryAdd, "Downloads", "Download manager preferences") { onDownloads() }
                SettingsGroupDivider()
                SettingsNavigationRow(Icons.Outlined.PlayArrow, "Playback", "Source selection and player behaviour") { onPlayback() }
                SettingsGroupDivider()
                SettingsNavigationRow(Icons.Outlined.Build, "Integrations", "TMDB enrichment and MDBList ratings") { onIntegrations() }
            }
        }
        item {
            SettingsSectionTitle("About")
            SectionSurface {
                SettingsNavigationRow(Icons.Outlined.Refresh, "Updates", "Check, download and install Nustrim updates") { onUpdates() }
                SettingsGroupDivider()
                SettingsNavigationRow(Icons.Outlined.Info, "About Nustrim", "Version, package and project information") { onAbout() }
                SettingsGroupDivider()
                SettingsNavigationRow(Icons.Outlined.LibraryAdd, "Backup & Restore", "Backup and restore local settings and library") { onLocalData() }
                SettingsGroupDivider()
                SettingsNavigationRow(Icons.Outlined.Build, "Developer Tools", "Developer mode and diagnostics") { onDeveloper() }
                if (developerMode) {
                    SettingsGroupDivider()
                    SettingsNavigationRow(Icons.Outlined.Info, "Diagnostics Log", "Runtime log with Copy Log and Clear Log") { onDiagnostics() }
                }
            }
        }
    }
}
@Composable
private fun TvSettingsOpenButton(
    requester: FocusRequester,
    onMoveLeft: () -> Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .width(210.dp)
            .focusRequester(requester)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                when {
                    consumeTvActivateKey(event, onClick) -> true
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft -> onMoveLeft()
                    else -> false
                }
            }
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(14.dp),
        color = if (focused) Color.White else AppSurface2,
        border = BorderStroke(1.dp, if (focused) Color.White else Color.White.copy(alpha = 0.12f))
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Open settings",
                modifier = Modifier.weight(1f),
                color = if (focused) AppBackground else Color.White,
                fontWeight = FontWeight.Bold
            )
            Icon(
                Icons.Outlined.KeyboardArrowRight,
                null,
                tint = if (focused) AppBackground else AppTextMuted
            )
        }
    }
}

@Composable
private fun TrackingSettingsScreen(
    isTv: Boolean,
    preferences: UiPreferences,
    onTrakt: () -> Unit,
    onBack: () -> Unit
) {
    SettingsPageScaffold(
        isTv = isTv,
        title = "Tracking",
        subtitle = "Watch progress and external tracking",
        onBack = onBack
    ) {
        item {
            SettingsSectionTitle("Local Tracking")
            SectionSurface {
                SettingLine("Watch progress", "Enabled")
                Spacer(Modifier.height(6.dp))
                Text(
                    "Local progress stays available even when no external integration is configured.",
                    color = AppTextMuted
                )
            }
        }
        item {
            SettingsSectionTitle("Trakt")
            SectionSurface {
                SettingsNavigationRow(
                    icon = Icons.Outlined.CheckCircle,
                    title = "Trakt",
                    subtitle = if (preferences.traktConnected) {
                        preferences.traktUsername.takeIf { it.isNotBlank() }?.let { "Connected as $it" } ?: "Connected"
                    } else {
                        "Not connected"
                    }
                ) { onTrakt() }
            }
        }
    }
}

@Composable
private fun TraktIntegrationSettingsScreen(
    isTv: Boolean,
    preferences: UiPreferences,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var draftClientId by remember { mutableStateOf(preferences.traktClientId) }
    var draftClientSecret by remember { mutableStateOf(preferences.traktClientSecret) }
    var connected by remember { mutableStateOf(preferences.traktConnected) }
    var username by remember { mutableStateOf(preferences.traktUsername) }
    var device by remember { mutableStateOf<TraktDeviceCode?>(null) }
    var connecting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(device?.deviceCode) {
        val active = device ?: return@LaunchedEffect
        connecting = true
        message = "Waiting for Trakt authorisation..."
        while (System.currentTimeMillis() < active.expiresAtMs) {
            delay(active.interval.coerceAtLeast(2) * 1000L)
            val tokenResult = TraktClient.pollDeviceToken(
                deviceCode = active.deviceCode,
                clientId = preferences.traktClientId,
                clientSecret = preferences.traktClientSecret
            )
            val failure = tokenResult.exceptionOrNull()
            if (failure != null) {
                message = failure.message ?: "Trakt authorisation failed."
                connecting = false
                device = null
                break
            }
            val token = tokenResult.getOrNull()
            if (token != null) {
                preferences.saveTraktToken(
                    accessToken = token.accessToken,
                    refreshToken = token.refreshToken,
                    expiresAtSeconds = token.expiresAtSeconds
                )
                val name = TraktClient.username(token.accessToken, preferences.traktClientId)
                    .getOrNull()
                    .orEmpty()
                preferences.traktUsername = name
                username = name
                connected = true
                connecting = false
                device = null
                message = name.takeIf { it.isNotBlank() }
                    ?.let { "Connected to Trakt as $it." }
                    ?: "Trakt connected."
                break
            }
        }
        if (connecting && System.currentTimeMillis() >= active.expiresAtMs) {
            connecting = false
            device = null
            message = "Trakt authorisation code expired. Start again."
        }
    }

    SettingsPageScaffold(
        isTv = isTv,
        title = "Trakt",
        subtitle = "Connect your own Trakt account",
        onBack = onBack
    ) {
        item {
            SettingsSectionTitle("Status")
            SectionSurface {
                SettingLine(
                    "Trakt",
                    if (connected) username.takeIf { it.isNotBlank() }?.let { "Connected as $it" } ?: "Connected" else "Not connected"
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Nustrim keeps local tracking available when Trakt is disconnected.",
                    color = AppTextMuted,
                    fontSize = 12.sp
                )
            }
        }

        item {
            SettingsSectionTitle("Trakt API Application")
            SectionSurface {
                Text(
                    "Nustrim does not ship shared Trakt application credentials. Create your own Trakt API application, then enter its client ID and client secret here before connecting your account.",
                    color = AppTextMuted,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = draftClientId,
                    onValueChange = { draftClientId = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Trakt client ID") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = draftClientSecret,
                    onValueChange = { draftClientSecret = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Trakt client secret") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        preferences.traktClientId = draftClientId
                        preferences.traktClientSecret = draftClientSecret
                        if (draftClientId.isBlank() || draftClientSecret.isBlank()) {
                            message = "Enter both Trakt client ID and client secret."
                        } else {
                            message = "Trakt application credentials saved locally."
                        }
                    }
                ) { Text("Save") }
            }
        }

        if (!connected) {
            item {
                SettingsSectionTitle("Connect Trakt")
                SectionSurface {
                    Button(
                        onClick = {
                            preferences.traktClientId = draftClientId
                            preferences.traktClientSecret = draftClientSecret
                            scope.launch {
                                if (preferences.traktClientId.isBlank() || preferences.traktClientSecret.isBlank()) {
                                    message = "Save your Trakt client ID and client secret first."
                                    return@launch
                                }
                                connecting = true
                                message = "Requesting Trakt device code..."
                                TraktClient.createDeviceCode(preferences.traktClientId)
                                    .onSuccess {
                                        device = it
                                        message = "Open Trakt and enter code ${it.userCode}."
                                    }
                                    .onFailure {
                                        connecting = false
                                        message = it.message ?: "Could not start Trakt authorisation."
                                    }
                            }
                        },
                        enabled = !connecting
                    ) { Text(if (connecting) "Connecting..." else "Connect Trakt") }

                    device?.let { active ->
                        Spacer(Modifier.height(14.dp))
                        Text("Authorisation code", color = AppTextMuted, fontSize = 12.sp)
                        Text(active.userCode, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Trakt code", active.userCode))
                                }
                            ) { Text("Copy Code") }
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(active.verificationUrl)))
                                    }
                                }
                            ) { Text("Open Trakt") }
                        }
                    }
                }
            }
        } else {
            item {
                SectionSurface {
                    OutlinedButton(
                        onClick = {
                            preferences.clearTraktConnection()
                            connected = false
                            username = ""
                            message = "Trakt disconnected. Local tracking is unchanged."
                        }
                    ) { Text("Disconnect Trakt") }
                }
            }
        }

        if (message.isNotBlank()) {
            item {
                SectionSurface {
                    Text(message, color = AppTextMuted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun LayoutSettingsScreen(
    isTv: Boolean,
    interfaceMode: InterfaceMode,
    onInterfaceMode: (InterfaceMode) -> Unit,
    onBack: () -> Unit
) {
    var expandedPanel by remember { mutableStateOf<String?>(null) }

    fun openPanel(panel: String) {
        expandedPanel = panel
    }

    fun togglePanel(panel: String) {
        expandedPanel = if (expandedPanel == panel) null else panel
    }

    SettingsPageScaffold(
        isTv = isTv,
        title = "Layout",
        subtitle = "Choose how Nustrim is presented",
        onBack = onBack
    ) {
        item {
            LayoutAccordionSection(
                isTv = isTv,
                title = "Interface Mode",
                description = "Mobile or remote-first TV layout",
                expanded = expandedPanel == "interface",
                onOpen = { openPanel("interface") },
                onToggle = { togglePanel("interface") }
            ) {
                Text(
                    "TV mode can be forced on a phone for remote testing.",
                    color = AppTextMuted,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(10.dp))
                InterfaceModeOption(
                    title = "Mobile",
                    description = "Touch-first layout with bottom navigation",
                    selected = interfaceMode == InterfaceMode.MOBILE,
                    onClick = { onInterfaceMode(InterfaceMode.MOBILE) }
                )
                Spacer(Modifier.height(8.dp))
                InterfaceModeOption(
                    title = "TV",
                    description = "Remote-first landscape layout with D-pad focus",
                    selected = interfaceMode == InterfaceMode.TV,
                    onClick = { onInterfaceMode(InterfaceMode.TV) }
                )
            }
        }


        item {
            val context = LocalContext.current
            val preferences = remember(context) { UiPreferences(context) }
            var defaultSubtitleSize by remember { mutableIntStateOf(readDefaultSubtitleFontSize(context)) }
            var defaultSubtitleBold by remember { mutableStateOf(readDefaultSubtitleBold(context)) }
            var preferredLanguage by remember { mutableStateOf(preferences.subtitlePreferredLanguage) }
            var secondPreferredLanguage by remember { mutableStateOf(preferences.subtitleSecondPreferredLanguage) }
            var subtitleDisplayMode by remember { mutableStateOf(preferences.subtitleDisplayMode) }
            LayoutAccordionSection(
                isTv = isTv,
                title = "Subtitles",
                description = "Default player subtitle appearance",
                expanded = expandedPanel == "subtitles",
                onOpen = { openPanel("subtitles") },
                onToggle = { togglePanel("subtitles") }
            ) {
                Text("Preferred language", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(7.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(SubtitleLanguageOptions, key = { "preferred:${it.code}" }) { language ->
                        FilterChip(
                            selected = preferredLanguage == language.code,
                            onClick = {
                                preferredLanguage = language.code
                                preferences.subtitlePreferredLanguage = language.code
                                if (secondPreferredLanguage == language.code) {
                                    secondPreferredLanguage = if (language.code == "en") "ms" else "en"
                                    preferences.subtitleSecondPreferredLanguage = secondPreferredLanguage
                                }
                            },
                            label = { Text(language.label) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Second preferred language", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(7.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(SubtitleLanguageOptions.filter { it.code != preferredLanguage }, key = { "second:${it.code}" }) { language ->
                        FilterChip(
                            selected = secondPreferredLanguage == language.code,
                            onClick = {
                                secondPreferredLanguage = language.code
                                preferences.subtitleSecondPreferredLanguage = language.code
                            },
                            label = { Text(language.label) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Languages shown in player", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = subtitleDisplayMode == SubtitleDisplayMode.PREFERRED_ONLY,
                        onClick = {
                            subtitleDisplayMode = SubtitleDisplayMode.PREFERRED_ONLY
                            preferences.subtitleDisplayMode = subtitleDisplayMode
                        },
                        label = { Text("Preferred only") }
                    )
                    FilterChip(
                        selected = subtitleDisplayMode == SubtitleDisplayMode.SHOW_ALL,
                        onClick = {
                            subtitleDisplayMode = SubtitleDisplayMode.SHOW_ALL
                            preferences.subtitleDisplayMode = subtitleDisplayMode
                        },
                        label = { Text("Show all") }
                    )
                }
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Font size", fontWeight = FontWeight.Medium)
                        Text("$defaultSubtitleSize sp", color = AppTextMuted, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            defaultSubtitleSize = (defaultSubtitleSize - 2).coerceAtLeast(12)
                            writeDefaultSubtitleFontSize(context, defaultSubtitleSize)
                        },
                        enabled = defaultSubtitleSize > 12
                    ) { Text("−") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            defaultSubtitleSize = (defaultSubtitleSize + 2).coerceAtMost(32)
                            writeDefaultSubtitleFontSize(context, defaultSubtitleSize)
                        },
                        enabled = defaultSubtitleSize < 32
                    ) { Text("+") }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Bold", fontWeight = FontWeight.Medium)
                        Text("Use bold subtitles by default", color = AppTextMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = defaultSubtitleBold,
                        onCheckedChange = { value ->
                            defaultSubtitleBold = value
                            writeDefaultSubtitleBold(context, value)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LayoutAccordionSection(
    isTv: Boolean,
    title: String,
    description: String,
    expanded: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    SectionSurface {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    if (!isTv) {
                        false
                    } else if (
                        event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionDown &&
                        !expanded
                    ) {
                        onOpen()
                        true
                    } else {
                        consumeTvActivateKey(event, onToggle)
                    }
                }
                .clickable(onClick = onToggle)
                .focusable()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium
                )
                Text(description, color = AppTextMuted, fontSize = 12.sp)
            }
            Text(
                if (expanded) "⌃" else "⌄",
                color = if (focused) Color.White else AppTextMuted,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            SettingsGroupDivider()
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun InterfaceModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val isTv = rememberIsTv()
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (isTv) consumeTvActivateKey(event, onClick) else false
            }
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(12.dp),
        color = when {
            isTv && focused -> AppSurface2
            selected -> AppAccentSoft
            else -> Color.Transparent
        },
        border = when {
            isTv && focused -> BorderStroke(2.dp, Color.White)
            selected -> BorderStroke(1.dp, AppAccent)
            else -> BorderStroke(1.dp, Color(0xFF2A2C31))
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(description, color = AppTextMuted, fontSize = 12.sp)
            }
            Text(
                if (selected) "Active" else "Select",
                color = if (selected) Color.White else AppTextMuted,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ContentDiscoverySettingsScreen(
    isTv: Boolean,
    onAddons: () -> Unit,
    onCatalogOrder: () -> Unit,
    onBack: () -> Unit
) {
    SettingsPageScaffold(
        isTv = isTv,
        title = "Content Manager",
        subtitle = "Catalog and source management",
        onBack = onBack
    ) {
        item {
            SettingsSectionTitle("Content Sources")
            SectionSurface {
                SettingsNavigationRow(
                    icon = Icons.Outlined.Source,
                    title = "Addons",
                    subtitle = "Install, enable, disable and test source addons"
                ) { onAddons() }
                SettingsGroupDivider()
                SettingsNavigationRow(
                    icon = Icons.Outlined.LibraryAdd,
                    title = "Catalog Order",
                    subtitle = "Reorder or hide Home catalog rows"
                ) { onCatalogOrder() }
            }
        }
    }
}

@Composable
private fun CatalogOrderSettingsScreen(
    isTv: Boolean,
    preferences: UiPreferences,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val engine = remember(context) { SourceEngine(context) }
    val store = remember(context) { InstalledSourceStore(context) }
    var sourceOrder by remember { mutableStateOf<List<String>>(emptyList()) }
    val catalogMap = remember { mutableStateMapOf<String, List<CatalogOrderEntry>>() }
    var loading by remember { mutableStateOf(true) }
    var order by remember { mutableStateOf(preferences.catalogOrder) }
    var hidden by remember { mutableStateOf(preferences.hiddenCatalogKeys) }

    LaunchedEffect(Unit) {
        catalogMap.clear()
        loading = true
        val urls = store.enabledUrls(preferences.developerMode)
        sourceOrder = urls
        var pending = urls.size
        fun finish() {
            pending -= 1
            if (pending <= 0) loading = false
        }
        if (urls.isEmpty()) {
            loading = false
            return@LaunchedEffect
        }
        urls.forEach { url ->
            engine.open(
                url,
                onSuccess = { session ->
                    if (session.kind == SourceKind.CLOUDSTREAM) {
                        catalogMap[url] = emptyList()
                        finish()
                    } else {
                        val sectioned = session as? CatalogSectionSourceSession
                        if (sectioned != null) {
                            sectioned.loadCatalogSections(
                                onSuccess = { catalogs ->
                                    catalogMap[url] = catalogs.map { catalog ->
                                        CatalogOrderEntry(
                                            key = "$url|${catalog.name.trim()}",
                                            title = catalog.name,
                                            sourceName = session.displayName
                                        )
                                    }
                                    finish()
                                },
                                onError = { catalogMap[url] = emptyList(); finish() }
                            )
                        } else {
                            session.loadCatalog(
                                onSuccess = { catalog ->
                                    catalogMap[url] = listOf(
                                        CatalogOrderEntry(
                                            key = "$url|${catalog.name.trim()}",
                                            title = catalog.name,
                                            sourceName = session.displayName
                                        )
                                    )
                                    finish()
                                },
                                onError = { catalogMap[url] = emptyList(); finish() }
                            )
                        }
                    }
                },
                onError = { catalogMap[url] = emptyList(); finish() }
            )
        }
    }

    val rawEntries = sourceOrder.flatMap { catalogMap[it].orEmpty() }
    val ranks = order.withIndex().associate { it.value to it.index }
    val entries = rawEntries.sortedWith(
        compareBy<CatalogOrderEntry> { ranks[it.key] ?: Int.MAX_VALUE }
            .thenBy { rawEntries.indexOf(it) }
    )

    fun move(entry: CatalogOrderEntry, delta: Int) {
        val keys = entries.map { it.key }.toMutableList()
        val from = keys.indexOf(entry.key)
        val to = (from + delta).coerceIn(0, keys.lastIndex)
        if (from < 0 || from == to) return
        val moved = keys.removeAt(from)
        keys.add(to, moved)
        order = keys
        preferences.catalogOrder = keys
    }

    SettingsPageScaffold(
        isTv = isTv,
        title = "Catalog Order",
        subtitle = "Arrange the Home rows shared by Mobile and TV",
        onBack = onBack
    ) {
        item {
            SectionSurface {
                Text(
                    "Use Up and Down to change position. Turn Visible off to hide a row without disabling its addon.",
                    color = AppTextMuted,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = {
                    preferences.resetCatalogLayout()
                    order = emptyList()
                    hidden = emptySet()
                }) { Text("Reset default order") }
            }
        }
        if (loading && entries.isEmpty()) {
            item { LoadingPanel("Loading catalogs...") }
        } else if (entries.isEmpty()) {
            item { EmptyState("No catalogs", "Enable a catalog addon first.") }
        } else {
            items(entries, key = { it.key }) { entry ->
                SectionSurface {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(entry.sourceName, color = AppTextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Visible", color = AppTextMuted, fontSize = 10.sp)
                            Switch(
                                checked = entry.key !in hidden,
                                onCheckedChange = { visible ->
                                    hidden = if (visible) hidden - entry.key else hidden + entry.key
                                    preferences.setCatalogHidden(entry.key, !visible)
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { move(entry, -1) }) { Text("Up") }
                        OutlinedButton(onClick = { move(entry, 1) }) { Text("Down") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadsSettingsScreen(
    isTv: Boolean,
    onBack: () -> Unit
) {
    SettingsPageScaffold(
        isTv = isTv,
        title = "Downloads",
        subtitle = "Offline media controls",
        onBack = onBack
    ) {
        item {
            SettingsSectionTitle("Download Manager")
            SectionSurface {
                SettingLine("Status", "Planned")
                Spacer(Modifier.height(6.dp))
                Text(
                    "The M2 baseline does not expose a download manager yet. This page reserves the settings location without pretending downloads are active.",
                    color = AppTextMuted
                )
            }
        }
    }
}

@Composable
private fun PlaybackSettingsScreen(
    isTv: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember(context) { UiPreferences(context) }
    var autoplayFirstSource by remember { mutableStateOf(preferences.autoplayFirstSource) }
    var autoplayNextEpisode by remember { mutableStateOf(preferences.autoplayNextEpisode) }

    SettingsPageScaffold(
        isTv = isTv,
        title = "Playback",
        subtitle = "Source selection and player behaviour",
        onBack = onBack
    ) {
        item {
            SettingsSectionTitle("Source Selection")
            SectionSurface {
                SettingsSwitchRow(
                    icon = Icons.Outlined.PlayArrow,
                    title = "Autoplay first source",
                    subtitle = "Play the first enabled source instead of opening the picker",
                    checked = autoplayFirstSource,
                    onChecked = {
                        autoplayFirstSource = it
                        preferences.autoplayFirstSource = it
                    }
                )
                SettingsGroupDivider()
                SettingsSwitchRow(
                    icon = Icons.Outlined.PlayArrow,
                    title = "Autoplay next episode",
                    subtitle = "Start the next episode automatically when the current episode ends",
                    checked = autoplayNextEpisode,
                    onChecked = {
                        autoplayNextEpisode = it
                        preferences.autoplayNextEpisode = it
                    }
                )
            }
        }
    }
}

@Composable
private fun IntegrationsSettingsScreen(
    isTv: Boolean,
    preferences: UiPreferences,
    onTmdb: () -> Unit,
    onMdbList: () -> Unit,
    onBack: () -> Unit
) {
    SettingsPageScaffold(
        isTv = isTv,
        title = "Integrations",
        subtitle = "Third-party metadata and ratings",
        onBack = onBack
    ) {
        item {
            SettingsSectionTitle("Integrations")
            SectionSurface {
                SettingsNavigationRow(
                    icon = Icons.Outlined.Info,
                    title = "TMDB Enrichment",
                    subtitle = if (preferences.tmdbEnrichmentEnabled) {
                        "Enabled"
                    } else {
                        "Metadata, artwork and discovery enrichment"
                    }
                ) { onTmdb() }
                SettingsGroupDivider()
                SettingsNavigationRow(
                    icon = Icons.Outlined.CheckCircle,
                    title = "MDBList Ratings",
                    subtitle = if (preferences.mdbListRatingsEnabled) {
                        "Enabled"
                    } else {
                        "IMDb, TMDB, Rotten Tomatoes and more"
                    }
                ) { onMdbList() }
            }
        }
        item {
            SectionSurface {
                Text(
                    "Trakt remains under Tracking because it synchronises watch state rather than enriching metadata.",
                    color = AppTextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun TmdbIntegrationSettingsScreen(
    isTv: Boolean,
    preferences: UiPreferences,
    onBack: () -> Unit
) {
    var draftKey by remember { mutableStateOf(preferences.tmdbApiKey) }
    var savedKey by remember { mutableStateOf(preferences.tmdbApiKey) }
    var enabled by remember { mutableStateOf(preferences.tmdbEnrichmentEnabled) }
    var message by remember { mutableStateOf("") }
    var testing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    SettingsPageScaffold(
        isTv = isTv,
        title = "TMDB Enrichment",
        subtitle = "Metadata and artwork enrichment",
        onBack = onBack
    ) {
        item {
            SettingsSectionTitle("TMDB")
            SectionSurface {
                SettingsSwitchRow(
                    icon = Icons.Outlined.Info,
                    title = "Enable TMDB enrichment",
                    subtitle = if (savedKey.isBlank()) {
                        "Add an API key first"
                    } else {
                        "Use TMDB as an enrichment layer while keeping addon IDs canonical"
                    },
                    checked = enabled,
                    enabled = savedKey.isNotBlank(),
                    onChecked = {
                        enabled = it
                        preferences.tmdbEnrichmentEnabled = it
                    }
                )
            }
        }
        item {
            SettingsSectionTitle("API Key")
            SectionSurface {
                OutlinedTextField(
                    value = draftKey,
                    onValueChange = { draftKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("TMDB API key or read token") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            savedKey = draftKey.trim()
                            preferences.tmdbApiKey = savedKey
                            if (savedKey.isBlank()) {
                                enabled = false
                                preferences.tmdbEnrichmentEnabled = false
                            }
                            message = if (savedKey.isBlank()) "TMDB credential cleared." else "TMDB credential saved locally."
                        }
                    ) {
                        Text("Save")
                    }
                    OutlinedButton(
                        onClick = {
                            val credential = draftKey.trim()
                            if (credential.isBlank()) {
                                message = "Enter a TMDB API key or API Read Access Token first."
                            } else {
                                testing = true
                                scope.launch {
                                    TmdbClient.validate(credential)
                                        .onSuccess {
                                            savedKey = credential
                                            preferences.tmdbApiKey = credential
                                            message = "TMDB connection successful."
                                        }
                                        .onFailure { message = it.message ?: "TMDB connection failed." }
                                    testing = false
                                }
                            }
                        },
                        enabled = !testing
                    ) { Text(if (testing) "Testing..." else "Test") }
                }
                if (message.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(message, color = AppTextMuted, fontSize = 12.sp)
                }
            }
        }
        item {
            SectionSurface {
                Text(
                    "When enabled, Nustrim uses your own TMDB credential to enrich Details with artwork, overview, genres, runtime, cast and recommendations. Addon metadata remains the canonical playback identity.",
                    color = AppTextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun MdbListIntegrationSettingsScreen(
    isTv: Boolean,
    preferences: UiPreferences,
    onBack: () -> Unit
) {
    val providers = remember {
        listOf(
            "imdb" to "IMDb",
            "tmdb" to "TMDB",
            "tomatoes" to "Rotten Tomatoes",
            "metacritic" to "Metacritic",
            "metacriticuser" to "Metacritic User",
            "trakt" to "Trakt",
            "letterboxd" to "Letterboxd",
            "audience" to "Rotten Tomatoes Audience",
            "mal" to "MyAnimeList",
            "rogerebert" to "RogerEbert"
        )
    }
    var draftKey by remember { mutableStateOf(preferences.mdbListApiKey) }
    var savedKey by remember { mutableStateOf(preferences.mdbListApiKey) }
    var enabled by remember { mutableStateOf(preferences.mdbListRatingsEnabled) }
    var message by remember { mutableStateOf("") }
    var testing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val providerState = remember {
        mutableStateMapOf<String, Boolean>().apply {
            providers.forEach { (id, _) ->
                this[id] = preferences.isMdbListProviderEnabled(id)
            }
        }
    }

    SettingsPageScaffold(
        isTv = isTv,
        title = "MDBList Ratings",
        subtitle = "Choose rating providers for details screens",
        onBack = onBack
    ) {
        item {
            SettingsSectionTitle("MDBList")
            SectionSurface {
                SettingsSwitchRow(
                    icon = Icons.Outlined.CheckCircle,
                    title = "Enable MDBList ratings",
                    subtitle = if (savedKey.isBlank()) "Add an API key first" else "Enable external rating enrichment",
                    checked = enabled,
                    enabled = savedKey.isNotBlank(),
                    onChecked = {
                        enabled = it
                        preferences.mdbListRatingsEnabled = it
                    }
                )
            }
        }

        item {
            SettingsSectionTitle("API Key")
            SectionSurface {
                OutlinedTextField(
                    value = draftKey,
                    onValueChange = { draftKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("MDBList API key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            savedKey = draftKey.trim()
                            preferences.mdbListApiKey = savedKey
                            if (savedKey.isBlank()) {
                                enabled = false
                                preferences.mdbListRatingsEnabled = false
                            }
                            message = if (savedKey.isBlank()) "MDBList API key cleared." else "MDBList API key saved locally."
                        }
                    ) {
                        Text("Save")
                    }
                    OutlinedButton(
                        onClick = {
                            val key = draftKey.trim()
                            if (key.isBlank()) {
                                message = "Enter an MDBList API key first."
                            } else {
                                testing = true
                                scope.launch {
                                    MdbListClient.validate(key)
                                        .onSuccess {
                                            savedKey = key
                                            preferences.mdbListApiKey = key
                                            message = "MDBList connection successful."
                                        }
                                        .onFailure { message = it.message ?: "MDBList connection failed." }
                                    testing = false
                                }
                            }
                        },
                        enabled = !testing
                    ) { Text(if (testing) "Testing..." else "Test") }
                }
                if (message.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(message, color = AppTextMuted, fontSize = 12.sp)
                }
            }
        }

        item {
            SettingsSectionTitle("Rating Providers")
            SectionSurface {
                providers.forEachIndexed { index, (id, label) ->
                    SettingsSwitchRow(
                        icon = Icons.Outlined.Info,
                        title = label,
                        subtitle = "",
                        checked = providerState[id] == true,
                        enabled = enabled && savedKey.isNotBlank(),
                        onChecked = { checked ->
                            providerState[id] = checked
                            preferences.setMdbListProviderEnabled(id, checked)
                        }
                    )
                    if (index < providers.lastIndex) SettingsGroupDivider()
                }
            }
        }

        item {
            SectionSurface {
                Text(
                    "When enabled, Nustrim requests ratings from MDBList only for Details screens and only with your own API key. Provider switches control which returned scores are displayed.",
                    color = AppTextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun LocalDataSettingsScreen(
    isTv: Boolean,
    interfaceMode: InterfaceMode,
    onInterfaceMode: (InterfaceMode) -> Unit,
    developerMode: Boolean,
    diagnostics: Boolean,
    onDeveloperModeRestored: (Boolean) -> Unit,
    onDiagnosticsRestored: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember(context) { UiPreferences(context) }
    val store = remember(context) { InstalledSourceStore(context) }
    val mediaStore = remember(context) { LocalMediaStore(context) }
    var backupMessage by remember { mutableStateOf("") }

    fun copyBackup() {
        val payload = JSONObject()
            .put("format", 1)
            .put("sources", JSONArray(store.exportJson()))
            .put("library", JSONArray(mediaStore.exportJson()))
            .put(
                "ui",
                JSONObject()
                    .put("interfaceMode", interfaceMode.name)
                    .put("developerMode", developerMode)
                    .put("developerDiagnostics", diagnostics)
                    .put("autoplayFirstSource", preferences.autoplayFirstSource)
                    .put("autoplayNextEpisode", preferences.autoplayNextEpisode)
            )
            .toString()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Nustrim backup", payload))
        backupMessage = "Backup copied to clipboard. Integration API keys are excluded."
    }

    fun restoreBackup() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val raw = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
        runCatching {
            val payload = JSONObject(raw)
            store.importJson(payload.getJSONArray("sources").toString())
            mediaStore.importJson(payload.getJSONArray("library").toString())
            payload.optJSONObject("ui")?.let { ui ->
                runCatching { InterfaceMode.valueOf(ui.optString("interfaceMode")) }
                    .getOrNull()
                    ?.let(onInterfaceMode)
                val restoredDeveloperMode = ui.optBoolean("developerMode", developerMode)
                val restoredDiagnostics = ui.optBoolean("developerDiagnostics", diagnostics)
                preferences.developerMode = restoredDeveloperMode
                preferences.developerDiagnostics = restoredDiagnostics
                preferences.autoplayFirstSource = ui.optBoolean(
                    "autoplayFirstSource",
                    preferences.autoplayFirstSource
                )
                preferences.autoplayNextEpisode = ui.optBoolean(
                    "autoplayNextEpisode",
                    preferences.autoplayNextEpisode
                )
                onDeveloperModeRestored(restoredDeveloperMode)
                onDiagnosticsRestored(restoredDiagnostics)
                if (restoredDeveloperMode) store.ensureDeveloperDefaults()
            }
        }.onSuccess {
            backupMessage = "Backup restored from clipboard."
        }.onFailure {
            backupMessage = "Restore failed: ${it.message ?: "invalid backup"}"
        }
    }

    SettingsPageScaffold(
        isTv = isTv,
        title = "Backup & Restore",
        subtitle = "Backup and restore local Nustrim data",
        onBack = onBack
    ) {
        item {
            SettingsSectionTitle("Backup & Restore")
            SectionSurface {
                Text(
                    "Includes addons, local library, watch progress and interface settings. Integration API keys are intentionally excluded.",
                    color = AppTextMuted
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { copyBackup() }) { Text("Copy backup") }
                    OutlinedButton(onClick = { restoreBackup() }) { Text("Restore") }
                }
                if (backupMessage.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(backupMessage, color = AppTextMuted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun DeveloperSettingsScreen(
    isTv: Boolean,
    developerMode: Boolean,
    remoteTestEnabled: Boolean,
    diagnostics: Boolean,
    onDeveloperMode: (Boolean) -> Unit,
    onRemoteTestEnabled: (Boolean) -> Unit,
    onDiagnostics: (Boolean) -> Unit,
    onOpenDiagnostics: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember(context) { UiPreferences(context) }
    val store = remember(context) { InstalledSourceStore(context) }

    SettingsPageScaffold(
        isTv = isTv,
        title = "Developer Tools",
        subtitle = "Diagnostics and provider development",
        onBack = onBack
    ) {
        item {
            SettingsSectionTitle("Developer")
            SectionSurface {
                SettingsSwitchRow(
                    icon = Icons.Outlined.Build,
                    title = "Developer Mode",
                    subtitle = "Expose Kurato CloudStream and Yastream test addons",
                    checked = developerMode,
                    onChecked = { enabled ->
                        preferences.developerMode = enabled
                        onDeveloperMode(enabled)
                        if (enabled) {
                            store.ensureDeveloperDefaults()
                        } else {
                            preferences.remoteTestEnabled = false
                            onRemoteTestEnabled(false)
                        }
                    }
                )
                SettingsGroupDivider()
                SettingsSwitchRow(
                    icon = Icons.Outlined.Info,
                    title = "Developer diagnostics",
                    subtitle = "Show runtime and media references on details screens",
                    checked = diagnostics,
                    onChecked = {
                        preferences.developerDiagnostics = it
                        onDiagnostics(it)
                    }
                )
                if (developerMode) {
                    SettingsGroupDivider()
                    SettingsSwitchRow(
                        icon = Icons.Outlined.Tv,
                        title = "Remote Test",
                        subtitle = "Show the floating TV test remote. Touch input is blocked while active.",
                        checked = remoteTestEnabled,
                        onChecked = { enabled ->
                            preferences.remoteTestEnabled = enabled
                            onRemoteTestEnabled(enabled)
                        }
                    )
                    SettingsGroupDivider()
                    SettingsNavigationRow(
                        icon = Icons.Outlined.Info,
                        title = "Diagnostics Log",
                        subtitle = "Copy or clear runtime diagnostics"
                    ) { onOpenDiagnostics() }
                }
            }
        }
    }
}

@Composable
private fun UpdateSettingsScreen(
    isTv: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val updater = remember(context) { AppUpdater(context) }
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var downloadedApk by remember { mutableStateOf<java.io.File?>(null) }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    fun formatSize(bytes: Long): String = when {
        bytes <= 0L -> ""
        bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
        bytes >= 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
        else -> "$bytes B"
    }

    fun checkForUpdate() {
        if (checking || downloading) return
        checking = true
        message = "Checking for updates..."
        error = ""
        scope.launch {
            updater.check()
                .onSuccess { info ->
                    updateInfo = info
                    downloadedApk = null
                    message = if (info == null) {
                        "You're up to date."
                    } else {
                        "Nustrim ${info.versionName} is available."
                    }
                }
                .onFailure { throwable ->
                    error = throwable.message ?: throwable.javaClass.simpleName
                    message = ""
                }
            checking = false
        }
    }

    fun downloadUpdate(info: UpdateInfo) {
        if (downloading) return
        downloading = true
        progress = 0
        error = ""
        message = "Downloading ${info.versionName}..."
        scope.launch {
            updater.download(info) { progress = it }
                .onSuccess { file ->
                    downloadedApk = file
                    message = "Update downloaded and verified."
                }
                .onFailure { throwable ->
                    error = throwable.message ?: throwable.javaClass.simpleName
                    message = ""
                }
            downloading = false
        }
    }

    SettingsPageScaffold(
        isTv = isTv,
        title = "Updates",
        subtitle = "Update Nustrim without removing app data",
        onBack = onBack
    ) {
        item {
            SettingsSectionTitle("Installed")
            SectionSurface {
                SettingLine("Current version", BuildConfig.VERSION_NAME)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Android installs updates over the current app when the new APK uses the same package name and signing certificate.",
                    color = AppTextMuted,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { checkForUpdate() },
                    enabled = !checking && !downloading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (checking) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (checking) "Checking..." else "Check for Updates")
                }
            }
        }

        if (message.isNotBlank() || error.isNotBlank()) {
            item {
                DiagnosticBanner(
                    message = error.ifBlank { message },
                    error = error.isNotBlank()
                )
            }
        }

        updateInfo?.let { info ->
            item {
                SettingsSectionTitle("Available Update")
                SectionSurface {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Nustrim ${info.versionName}", fontWeight = FontWeight.Bold)
                            val size = formatSize(info.sizeBytes)
                            if (size.isNotBlank()) {
                                Text(size, color = AppTextMuted, fontSize = 11.sp)
                            }
                        }
                        Text("NEW", color = Color(0xFF75D89B), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    if (info.changelog.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            info.changelog,
                            color = AppTextMuted,
                            fontSize = 12.sp,
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (info.signingMode != "persistent") {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Update signing is not locked yet. Android may reject an in-place update if this APK was signed with a different build key.",
                            color = Color(0xFFF0C86E),
                            fontSize = 11.sp
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    if (downloading) {
                        Text("Download $progress%", color = AppTextMuted, fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        Slider(
                            value = progress.toFloat(),
                            onValueChange = {},
                            enabled = false,
                            valueRange = 0f..100f
                        )
                    } else if (downloadedApk == null) {
                        Button(
                            onClick = { downloadUpdate(info) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Download Update") }
                    }
                }
            }
        }

        downloadedApk?.let { apk ->
            item {
                SettingsSectionTitle("Install")
                SectionSurface {
                    Text("APK checksum verified.", color = Color(0xFF75D89B), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    if (!updater.canRequestPackageInstall()) {
                        Text(
                            "Allow Nustrim to install app updates, then return here and tap Install Update.",
                            color = AppTextMuted,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { updater.openInstallPermission() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Allow Installation") }
                        Spacer(Modifier.height(8.dp))
                    }
                    Button(
                        onClick = {
                            if (!updater.canRequestPackageInstall()) {
                                updater.openInstallPermission()
                            } else {
                                updater.install(apk)
                                    .onFailure { error = it.message ?: it.javaClass.simpleName }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Install Update") }
                }
            }
        }
    }
}

@Composable
private fun AboutNustrimSettingsScreen(
    isTv: Boolean,
    onBack: () -> Unit
) {
    SettingsPageScaffold(
        isTv = isTv,
        title = "About Nustrim",
        subtitle = "Project and build information",
        onBack = onBack
    ) {
        item {
            SettingsSectionTitle("Nustrim")
            SectionSurface {
                SettingLine("Version", BuildConfig.VERSION_NAME)
                SettingLine("Package", BuildConfig.APPLICATION_ID)
                SettingLine("Developer", "NudroidLabs")
                SettingLine("Player", "AndroidX Media3")
            }
        }
    }
}

@Composable
private fun SettingsPageScaffold(
    isTv: Boolean,
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    val backRequester = remember { FocusRequester() }
    LaunchedEffect(isTv, title) {
        if (isTv) {
            delay(90)
            requestTvFocus(backRequester)
        }
    }
    val list: @Composable () -> Unit = {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = if (isTv) 28.dp else 16.dp,
                vertical = if (isTv) 22.dp else 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (isTv) 18.dp else 16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = if (isTv) Modifier.focusRequester(backRequester) else Modifier
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            title,
                            fontSize = if (isTv) 27.sp else 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(subtitle, color = AppTextMuted, fontSize = if (isTv) 13.sp else 13.sp)
                    }
                }
            }
            content()
        }
    }
    if (isTv) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 22.dp),
            shape = RoundedCornerShape(24.dp),
            color = AppSurface,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
        ) { list() }
    } else {
        list()
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = AppTextMuted,
        fontWeight = FontWeight.Medium
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SettingsGroupDivider() {
    HorizontalDivider(color = Color(0xFF2A2C31))
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    requester: FocusRequester? = null,
    onMoveLeft: (() -> Boolean)? = null,
    onClick: () -> Unit
) {
    val isTv = rememberIsTv()
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(requester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                when {
                    isTv && event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionLeft -> onMoveLeft?.invoke() ?: false
                    isTv -> consumeTvActivateKey(event, onClick)
                    else -> false
                }
            }
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(12.dp),
        color = if (isTv && focused) AppSurface2 else Color.Transparent,
        border = if (isTv && focused) BorderStroke(2.dp, Color.White) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (isTv) 12.dp else 0.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(AppSurface2, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = AppTextMuted, fontSize = 12.sp)
                }
            }
            Icon(Icons.Outlined.KeyboardArrowRight, null, tint = AppTextMuted)
        }
    }
}

@Composable
private fun SettingsEntry(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val isTv = rememberIsTv()
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (isTv) consumeTvActivateKey(event, onClick) else false
            }
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(18.dp),
        color = if (isTv && focused) AppSurface2 else AppSurface,
        border = if (isTv && focused) BorderStroke(2.dp, Color.White) else null
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).background(AppSurface2, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = AppTextMuted, fontSize = 13.sp)
            }
            Icon(Icons.Outlined.KeyboardArrowRight, null, tint = AppTextMuted)
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChecked: (Boolean) -> Unit
) {
    val isTv = rememberIsTv()
    var focused by remember { mutableStateOf(false) }
    val toggle = { if (enabled) onChecked(!checked) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = if (enabled) 1f else 0.55f)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                isTv && enabled && consumeTvActivateKey(event, toggle)
            }
            .then(
                if (isTv && enabled) Modifier.clickable(onClick = toggle).focusable()
                else Modifier
            ),
        shape = RoundedCornerShape(if (isTv) 12.dp else 0.dp),
        color = if (isTv && focused) AppSurface2 else Color.Transparent,
        border = if (isTv && focused) BorderStroke(2.dp, Color.White) else null
    ) {
        Row(
            Modifier.padding(horizontal = if (isTv) 12.dp else 0.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = AppTextMuted)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = AppTextMuted, fontSize = 12.sp)
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = if (isTv) null else onChecked,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun SettingLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = AppTextMuted)
        Spacer(Modifier.width(12.dp))
        Text(value, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DiagnosticsLogScreen(
    isTv: Boolean,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val entries by NustrimDiagnostics.entries.collectAsState()
    var copied by remember { mutableStateOf(false) }

    fun copyLog() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText("Nustrim diagnostics", NustrimDiagnostics.snapshotText())
        )
        copied = true
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTv) 20.dp else 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Diagnostics Log",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Android Logcat tag: NustrimDiag · ${entries.size} line(s)",
                    color = AppTextMuted
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTv) 24.dp else 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { copyLog() }, enabled = entries.isNotEmpty()) {
                Text(if (copied) "Copied" else "Copy Log")
            }
            OutlinedButton(
                onClick = {
                    NustrimDiagnostics.clear()
                    copied = false
                },
                enabled = entries.isNotEmpty()
            ) {
                Text("Clear Log")
            }
        }

        Spacer(Modifier.height(10.dp))

        if (entries.isEmpty()) {
            EmptyState(
                title = "No diagnostic events yet",
                message = "Try a catalog, provider, stream or playback action, then return here."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = if (isTv) 34.dp else 16.dp,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(entries.asReversed()) { line ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = AppSurface
                    ) {
                        Text(
                            line,
                            modifier = Modifier.padding(10.dp),
                            fontSize = 11.sp,
                            color = Color(0xFFD8DAE0)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticCatalogScreen(
    isTv: Boolean,
    session: SourceSession,
    catalog: MediaCatalog,
    onItem: (MediaItem) -> Unit,
    onBack: () -> Unit,
    onCatalogChanged: (MediaCatalog) -> Unit
) {
    BackHandler(onBack = onBack)
    var query by remember(session.id) { mutableStateOf("") }
    var searching by remember(session.id) { mutableStateOf(false) }
    var error by remember(session.id) { mutableStateOf("") }
    val searchable = session as? SearchableSourceSession

    fun search() {
        val searchSession = searchable ?: return
        val clean = query.trim()
        searching = true
        error = ""
        if (clean.isBlank()) {
            session.loadCatalog(
                onSuccess = {
                    searching = false
                    onCatalogChanged(it)
                },
                onError = {
                    searching = false
                    error = it.message ?: it.javaClass.simpleName
                }
            )
        } else {
            searchSession.search(
                clean,
                onSuccess = {
                    searching = false
                    onCatalogChanged(it)
                },
                onError = {
                    searching = false
                    error = it.message ?: it.javaClass.simpleName
                }
            )
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTv) 20.dp else 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
            }
            Column(Modifier.weight(1f)) {
                Text(catalog.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${session.displayName} · ${catalog.items.size} item(s)", color = AppTextMuted)
            }
        }

        if (searchable != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isTv) 24.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search provider") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { search() })
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { search() }, enabled = !searching) { Text("Search") }
            }
        }

        if (error.isNotBlank()) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        when {
            searching -> LoadingPanel("Loading provider...")
            catalog.items.isEmpty() -> EmptyState(
                "No browseable items",
                "This provider may be search-only."
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(if (isTv) 142.dp else 118.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = if (isTv) 24.dp else 16.dp,
                    vertical = 14.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    catalog.items,
                    key = { "${it.type}:${it.id}:${it.ref?.metaId.orEmpty()}" }
                ) { item ->
                    DiagnosticMediaCard(item, isTv) { onItem(item) }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticMediaCard(
    item: MediaItem,
    isTv: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.035f else 1f, label = "diagnostic")
    Column(
        modifier = Modifier
            .width(if (isTv) 142.dp else 118.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
            border = if (focused) BorderStroke(2.dp, Color.White) else null,
            colors = CardDefaults.cardColors(containerColor = AppSurface2)
        ) {
            Artwork(item.posterUrl.ifBlank { item.backgroundUrl }, item.title, Modifier.fillMaxSize(), ContentScale.Crop)
        }
        Spacer(Modifier.height(6.dp))
        Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DetailScreen(
    isTv: Boolean,
    sourceUrl: String,
    session: SourceSession,
    item: MediaItem,
    autoPlayEpisodeId: String?,
    autoOpenSources: Boolean,
    autoOpenSourcesEpisodeId: String?,
    onBack: () -> Unit,
    onPlay: (StreamSource, List<StreamSource>, String, MediaItem, MediaEpisode?) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val preferences = remember(context) { UiPreferences(context) }
    val streamResolver = remember(context) { StreamResolver(context) }
    val subtitleResolver = remember(context) { SubtitleResolver(context) }
    val mediaStore = remember(context) { LocalMediaStore(context) }
    val integrationScope = rememberCoroutineScope()
    var tmdbMetadata by remember(item.id) { mutableStateOf<TmdbMetadata?>(null) }
    var mdbRatings by remember(item.id) { mutableStateOf<List<MdbListRating>>(emptyList()) }
    var integrationLoading by remember(item.id) { mutableStateOf(false) }
    var integrationMessage by remember(item.id) { mutableStateOf("") }
    var detailed by remember(item.id) { mutableStateOf(item) }
    var streams by remember(item.id) { mutableStateOf(item.streams) }
    var selectedEpisode by remember(item.id) { mutableStateOf<MediaEpisode?>(null) }
    var selectedSeason by remember(item.id) { mutableStateOf<Int?>(null) }
    var loadingDetails by remember(item.id) { mutableStateOf(true) }
    var loadingStreams by remember(item.id) { mutableStateOf(false) }
    var sourceChoices by remember(item.id) { mutableStateOf<List<StreamSource>>(emptyList()) }
    var sourcePickerVisible by remember(item.id) { mutableStateOf(false) }
    var sourceError by remember(item.id) { mutableStateOf("") }
    var streamProgress by remember(item.id) { mutableStateOf("") }
    var providerProgress by remember(item.id) { mutableStateOf<List<StreamProviderProgress>>(emptyList()) }
    var pendingPlayTitle by remember(item.id) { mutableStateOf(item.title) }
    var pendingEpisode by remember(item.id) { mutableStateOf<MediaEpisode?>(null) }
    var saved by remember(sourceUrl, item.id) { mutableStateOf(mediaStore.isSaved(sourceUrl, item)) }
    var error by remember(item.id) { mutableStateOf("") }
    var autoPlayConsumed by remember(item.id, autoPlayEpisodeId) { mutableStateOf(false) }
    var autoOpenSourcesConsumed by remember(item.id, autoOpenSources, autoOpenSourcesEpisodeId) {
        mutableStateOf(false)
    }

    // M10 Stage 3 TV detail focus foundation, implemented natively in Nustrim.
    val detailKey = remember(sourceUrl, item.id) { "$sourceUrl|${item.id}" }
    val primaryFocusRequester = remember(detailKey) { FocusRequester() }
    val saveFocusRequester = remember(detailKey) { FocusRequester() }
    val trailerFocusRequester = remember(detailKey) { FocusRequester() }
    val seasonFocusRequesters = remember(detailKey) { mutableMapOf<Int, FocusRequester>() }
    val episodeFocusRequesters = remember(detailKey) { mutableMapOf<String, FocusRequester>() }
    var detailFocusRestoreToken by remember(detailKey) { mutableIntStateOf(0) }

    fun rememberDetailFocus(target: String) {
        if (!isTv) return
        TvNavigationMemory.detailKey = detailKey
        TvNavigationMemory.detailFocusTarget = target
    }

    fun loadIntegrations(target: MediaItem) {
        val wantsTmdb = preferences.tmdbEnrichmentEnabled && preferences.tmdbApiKey.isNotBlank()
        val wantsRatings = preferences.mdbListRatingsEnabled && preferences.mdbListApiKey.isNotBlank()
        if (!wantsTmdb && !wantsRatings) {
            tmdbMetadata = null
            mdbRatings = emptyList()
            integrationLoading = false
            integrationMessage = ""
            return
        }

        integrationLoading = true
        integrationMessage = ""
        integrationScope.launch {
            var metadata: TmdbMetadata? = null
            val errors = mutableListOf<String>()

            if (wantsTmdb) {
                TmdbClient.metadata(target, preferences.tmdbApiKey)
                    .onSuccess { metadata = it; tmdbMetadata = it }
                    .onFailure { errors += "TMDB: ${it.message ?: "request failed"}" }
            }

            if (wantsRatings) {
                MdbListClient.ratings(target, preferences.mdbListApiKey, metadata)
                    .onSuccess { ratings ->
                        mdbRatings = ratings.filter { preferences.isDisplayedMdbRating(it.source) }
                    }
                    .onFailure { errors += "MDBList: ${it.message ?: "request failed"}" }
            }

            integrationLoading = false
            integrationMessage = errors.joinToString(" · ")
        }
    }

    fun playbackTitle(episode: MediaEpisode?): String =
        episode?.let { "${detailed.title} · ${it.displayTitle}" } ?: detailed.title

    fun resolveAndPlay(requestedEpisode: MediaEpisode?, autoStart: Boolean = false) {
        if (loadingStreams) return
        val episode = if (detailed.episodes.isEmpty()) {
            null
        } else {
            requestedEpisode ?: selectedEpisode ?: preferredEpisode(detailed.episodes)
        }

        selectedEpisode = episode
        loadingStreams = true
        sourceChoices = emptyList()
        sourceError = ""
        streamProgress = "Starting providers..."
        providerProgress = emptyList()
        if (!autoStart) sourcePickerVisible = true
        error = ""
        pendingPlayTitle = playbackTitle(episode)
        pendingEpisode = episode

        NustrimDiagnostics.log(
            "PLAY_REQUEST",
            "source=${session.displayName} sourceId=${session.id} type=${detailed.type} " +
                "mediaId=${detailed.id} metaId=${detailed.ref?.metaId.orEmpty()} " +
                "episode=${episode?.id.orEmpty()} title=$pendingPlayTitle"
        )

        var latestStreams = emptyList<StreamSource>()
        var latestSubtitles = emptyList<app.nudroidlabs.nustrim.core.model.SubtitleSource>()
        var streamDone = false
        var subtitleDone = false
        var autoStarted = false

        fun publish() {
            val enriched = latestStreams.map { source ->
                source.copy(
                    subtitles = (source.subtitles + latestSubtitles)
                        .distinctBy { "${it.url}|${it.language}" }
                )
            }
            streams = enriched
            sourceChoices = enriched.filter { it.playable && it.url.isNotBlank() }

            if (streamDone && subtitleDone) {
                loadingStreams = false
                streamProgress = "${sourceChoices.size} playable source(s)"
                if (sourceChoices.isEmpty() && sourceError.isBlank()) {
                    sourceError = "No playable streams found from enabled addons."
                }
                if (autoStart && sourceChoices.isNotEmpty() && !autoStarted) {
                    autoStarted = true
                    onPlay(sourceChoices.first(), sourceChoices, pendingPlayTitle, detailed, episode)
                }
            }
        }

        subtitleResolver.resolve(
            originSession = session,
            item = detailed,
            episode = episode,
            developerMode = preferences.developerMode,
            onSuccess = { subtitles ->
                integrationScope.launch {
                    latestSubtitles = subtitles
                    subtitleDone = true
                    publish()
                }
            }
        )

        streamResolver.resolve(
            originSession = session,
            item = detailed,
            episode = episode,
            developerMode = preferences.developerMode,
            onSuccess = { resolved ->
                integrationScope.launch {
                    latestStreams = resolved
                    streamDone = true
                    publish()
                }
            },
            onError = { throwable ->
                integrationScope.launch {
                    streamDone = true
                    sourceError = throwable.message ?: throwable.javaClass.simpleName
                    publish()
                }
            },
            onProgress = { partial, completed, total, lastError ->
                integrationScope.launch {
                    latestStreams = partial
                    streamProgress = "Checking providers $completed/$total"
                    if (lastError != null && partial.isEmpty()) {
                        sourceError = lastError.message ?: lastError.javaClass.simpleName
                    }
                    publish()
                }
            },
            onProviderProgress = { states ->
                integrationScope.launch { providerProgress = states }
            }
        )
    }

    LaunchedEffect(item.id) {
        session.loadDetails(
            item,
            onSuccess = { resolved ->
                detailed = resolved
                loadIntegrations(resolved)
                loadingDetails = false
                NustrimDiagnostics.log(
                    "DETAIL_READY",
                    "provider=${session.displayName} type=${resolved.type} mediaId=${resolved.id} " +
                        "metaId=${resolved.ref?.metaId.orEmpty()} episodes=${resolved.episodes.size} " +
                        "embeddedStreams=${resolved.streams.size}"
                )
                val resumeEntry = mediaStore.continueWatching()
                    .firstOrNull { it.matchesMedia(sourceUrl, resolved) }
                val resumeEpisode = resumeEntry?.episodeId
                    ?.takeIf { it.isNotBlank() }
                    ?.let { episodeId -> resolved.episodes.firstOrNull { it.id == episodeId } }

                val rememberedEpisode = if (isTv && TvNavigationMemory.detailKey == detailKey) {
                    TvNavigationMemory.detailFocusTarget
                        ?.takeIf { it.startsWith("episode:") }
                        ?.removePrefix("episode:")
                        ?.let { episodeId -> resolved.episodes.firstOrNull { it.id == episodeId } }
                } else {
                    null
                }

                selectedEpisode = rememberedEpisode ?: resumeEpisode
                selectedSeason = rememberedEpisode?.season
                    ?: resumeEpisode?.season
                    ?: preferredSeason(resolved.episodes)

                if (resolved.episodes.isEmpty() && resolved.streams.isNotEmpty()) {
                    streams = resolved.streams
                }
            },
            onError = {
                loadingDetails = false
                loadIntegrations(item)
                error = it.message ?: it.javaClass.simpleName
                NustrimDiagnostics.error(
                    "DETAIL_ERROR",
                    it,
                    "provider=${session.displayName} mediaId=${item.id}"
                )
            }
        )
    }

    val resumeEntry = mediaStore.continueWatching()
        .firstOrNull { it.matchesMedia(sourceUrl, detailed) }

    LaunchedEffect(autoPlayEpisodeId, loadingDetails, detailed.episodes) {
        if (!autoPlayConsumed && !loadingDetails && !autoPlayEpisodeId.isNullOrBlank()) {
            val target = detailed.episodes.firstOrNull { it.id == autoPlayEpisodeId }
            if (target != null) {
                autoPlayConsumed = true
                selectedEpisode = target
                selectedSeason = target.season
                resolveAndPlay(target, autoStart = true)
            }
        }
    }

    LaunchedEffect(autoOpenSources, autoOpenSourcesEpisodeId, loadingDetails, detailed.episodes) {
        if (!autoOpenSourcesConsumed && autoOpenSources && !loadingDetails) {
            val target = autoOpenSourcesEpisodeId
                ?.let { episodeId -> detailed.episodes.firstOrNull { it.id == episodeId } }
                ?: selectedEpisode
                ?: preferredEpisode(detailed.episodes)
            autoOpenSourcesConsumed = true
            if (target != null) {
                selectedEpisode = target
                selectedSeason = target.season
            }
            resolveAndPlay(target, autoStart = false)
        }
    }

    val seasons = orderedSeasonNumbers(detailed.episodes)
    val visibleEpisodes = if (selectedSeason == null) {
        detailed.episodes
    } else {
        detailed.episodes.filter { it.season == selectedSeason }
    }
    val displayItem = detailed.withTmdbMetadata(tmdbMetadata)

    LaunchedEffect(
        isTv,
        loadingDetails,
        sourcePickerVisible,
        detailFocusRestoreToken,
        selectedSeason,
        visibleEpisodes.map { it.id }
    ) {
        if (!isTv || loadingDetails || sourcePickerVisible) return@LaunchedEffect
        delay(90)
        val rememberedTarget = TvNavigationMemory.detailFocusTarget
            .takeIf { TvNavigationMemory.detailKey == detailKey }

        val restored = when {
            rememberedTarget == "save" -> requestTvFocus(saveFocusRequester)
            rememberedTarget == "trailer" -> requestTvFocus(trailerFocusRequester)
            rememberedTarget?.startsWith("season:") == true -> {
                val season = rememberedTarget.removePrefix("season:").toIntOrNull()
                requestTvFocus(season?.let(seasonFocusRequesters::get))
            }
            rememberedTarget?.startsWith("episode:") == true -> {
                val episodeId = rememberedTarget.removePrefix("episode:")
                requestTvFocus(episodeFocusRequesters[episodeId])
            }
            else -> requestTvFocus(primaryFocusRequester)
        }
        if (!restored) requestTvFocus(primaryFocusRequester)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = if (isTv) 28.dp else 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DetailsHero(
                isTv = isTv,
                item = displayItem,
                logoUrl = tmdbMetadata?.logoUrl.orEmpty(),
                onBack = onBack,
                onPrimary = { resolveAndPlay(selectedEpisode) },
                primaryLabel = when {
                    resumeEntry?.hasProgress == true -> "Resume ${formatPlaybackTime(resumeEntry.positionMs)}"
                    detailed.episodes.isNotEmpty() && selectedEpisode != null -> "Play ${selectedEpisode!!.displayTitle}"
                    else -> "Play"
                },
                playbackHint = resumeEntry?.episodeTitle
                    ?.takeIf { it.isNotBlank() }
                    ?.let { "Continue $it" }
                    .orEmpty(),
                loading = loadingStreams,
                saved = saved,
                canSave = sourceUrl.isNotBlank(),
                primaryFocusRequester = if (isTv) primaryFocusRequester else null,
                saveFocusRequester = if (isTv) saveFocusRequester else null,
                trailerFocusRequester = if (isTv) trailerFocusRequester else null,
                onPrimaryFocused = { rememberDetailFocus("primary") },
                onSaveFocused = { rememberDetailFocus("save") },
                onTrailerFocused = { rememberDetailFocus("trailer") },
                onTrailer = tmdbMetadata?.trailers?.firstOrNull()?.let { trailer ->
                    {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${trailer.key}"))
                            )
                        }
                    }
                },
                onToggleSaved = {
                    val next = !saved
                    mediaStore.setSaved(sourceUrl, displayItem, next)
                    saved = next
                }
            )
        }
        if (loadingDetails) {
            item {
                if (isTv) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 34.dp, vertical = 8.dp)) {
                        LoadingPanel("Loading details...")
                    }
                } else {
                    LoadingPanel("Loading details...")
                }
            }
        }
        if (error.isNotBlank()) item { DiagnosticBanner(error, error = true) }

        // M10 Stage 3: TV keeps episodes near the hero and uses a horizontal, D-pad-first rail.


        item {
            RichDetailsSections(
                isTv = isTv,
                item = displayItem,
                metadata = tmdbMetadata,
                ratings = mdbRatings,
                loading = integrationLoading,
                message = integrationMessage
            )
        }

        if (!isTv && detailed.episodes.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text("Episodes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (seasons.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(seasons) { season ->
                                FilterChip(
                                    selected = selectedSeason == season,
                                    onClick = { selectedSeason = season },
                                    label = { Text(seasonLabel(season)) }
                                )
                            }
                        }
                    }
                }
            }
            items(visibleEpisodes, key = { it.id }) { episode ->
                EpisodeRow(
                    episode = episode,
                    selected = selectedEpisode?.id == episode.id,
                    isTv = false,
                    progressFraction = resumeEntry
                        ?.takeIf { it.episodeId == episode.id }
                        ?.progressFraction
                        ?: 0f,
                    resumePositionMs = resumeEntry
                        ?.takeIf { it.episodeId == episode.id }
                        ?.positionMs
                        ?: 0L,
                    onClick = {
                        if (!loadingStreams) resolveAndPlay(episode)
                    }
                )
            }
        }

        if (preferences.developerDiagnostics) {
            item { DeveloperDetails(session, detailed, selectedEpisode, streams) }
        }
    }

    if (sourcePickerVisible) {
        StreamSourcePicker(
            title = pendingPlayTitle,
            item = displayItem,
            episode = pendingEpisode,
            resumePositionMs = resumeEntry
                ?.takeIf { entry ->
                    pendingEpisode?.let { entry.episodeId == it.id } ?: entry.episodeId.isBlank()
                }
                ?.positionMs
                ?: 0L,
            streams = sourceChoices,
            providerProgress = providerProgress,
            loading = loadingStreams,
            progress = streamProgress,
            error = sourceError,
            onDismiss = {
                sourcePickerVisible = false
                detailFocusRestoreToken += 1
            },
            onRefresh = {
                if (!loadingStreams) {
                    sourcePickerVisible = false
                    resolveAndPlay(pendingEpisode)
                }
            },
            onSelect = { source ->
                sourcePickerVisible = false
                onPlay(source, sourceChoices, pendingPlayTitle, detailed, pendingEpisode)
            }
        )
    }
}

private fun UiPreferences.isDisplayedMdbRating(source: String): Boolean {
    val providerId = when (source.trim().lowercase()) {
        "imdb" -> "imdb"
        "tmdb" -> "tmdb"
        "tomatoes", "rottentomatoes" -> "tomatoes"
        "popcorn", "tomatoesaudience", "rottentomatoesaudience", "audience" -> "audience"
        "metacritic" -> "metacritic"
        "metacriticuser", "metacritic_user" -> "metacriticuser"
        "trakt" -> "trakt"
        "letterboxd" -> "letterboxd"
        "myanimelist", "mal" -> "mal"
        "rogerebert", "roger_ebert" -> "rogerebert"
        else -> source.trim().lowercase()
    }
    return isMdbListProviderEnabled(providerId)
}

private fun MediaItem.withTmdbMetadata(metadata: TmdbMetadata?): MediaItem {
    metadata ?: return this
    val facts = buildList {
        metadata.releaseYear.takeIf { it.isNotBlank() }?.let(::add)
        metadata.genres.take(2).forEach(::add)
        metadata.runtimeMinutes?.takeIf { it > 0 }?.let { add("$it min") }
    }
    return copy(
        title = metadata.title.ifBlank { title },
        description = metadata.overview.ifBlank { description },
        posterUrl = metadata.posterUrl.ifBlank { posterUrl },
        backgroundUrl = metadata.backdropUrl.ifBlank { backgroundUrl },
        releaseInfo = facts.joinToString(" · ").ifBlank { releaseInfo }
    )
}

private fun mdbRatingLabel(source: String): String = when (source.trim().lowercase()) {
    "imdb" -> "IMDb"
    "tmdb" -> "TMDB"
    "tomatoes", "rottentomatoes" -> "Rotten Tomatoes"
    "popcorn", "tomatoesaudience", "rottentomatoesaudience", "audience" -> "RT Audience"
    "metacritic" -> "Metacritic"
    "metacriticuser", "metacritic_user" -> "Metacritic User"
    "trakt" -> "Trakt"
    "letterboxd" -> "Letterboxd"
    "myanimelist", "mal" -> "MyAnimeList"
    "rogerebert", "roger_ebert" -> "RogerEbert"
    else -> source.replaceFirstChar { it.uppercase() }
}

private fun mdbRatingValue(rating: MdbListRating): String {
    val raw = rating.value ?: rating.score ?: return "N/A"
    return when (rating.source.trim().lowercase()) {
        "tomatoes", "rottentomatoes", "popcorn", "tomatoesaudience", "rottentomatoesaudience", "audience",
        "metacritic", "trakt" -> "${raw.toInt()}%"
        else -> if (raw % 1.0 == 0.0) raw.toInt().toString() else String.format(java.util.Locale.US, "%.1f", raw)
    }
}

@Composable
private fun RichDetailsSections(
    isTv: Boolean,
    item: MediaItem,
    metadata: TmdbMetadata?,
    ratings: List<MdbListRating>,
    loading: Boolean,
    message: String
) {
    val context = LocalContext.current
    var showFullOverview by remember(item.id) { mutableStateOf(false) }
    val horizontalPadding = if (isTv) 24.dp else 16.dp
    val overview = metadata?.overview?.takeIf { it.isNotBlank() } ?: item.description

    Column(
        modifier = Modifier.padding(horizontal = horizontalPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("About", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        SectionSurface {
            metadata?.tagline?.takeIf { it.isNotBlank() }?.let { tagline ->
                Text(tagline, fontWeight = FontWeight.SemiBold, color = Color(0xFFE5E1F8))
                Spacer(Modifier.height(8.dp))
            }
            if (overview.isNotBlank()) {
                Text(
                    overview,
                    color = Color(0xFFD0D2D8),
                    maxLines = if (showFullOverview) Int.MAX_VALUE else if (isTv) 5 else 4,
                    overflow = TextOverflow.Ellipsis
                )
                if (overview.length > 220) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { showFullOverview = !showFullOverview }) {
                        Text(if (showFullOverview) "Show Less" else "Show More")
                    }
                }
            } else {
                Text("No overview available.", color = AppTextMuted)
            }

            metadata?.genres?.takeIf { it.isNotEmpty() }?.let { genres ->
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(genres, key = { it }) { genre ->
                        Surface(shape = RoundedCornerShape(20.dp), color = AppSurface2) {
                            Text(genre, modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(9.dp))
                Text("Loading configured TMDB / MDBList enrichment...", color = AppTextMuted, fontSize = 12.sp)
            }
        }

        if (ratings.isNotEmpty()) {
            Text("Ratings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ratings, key = { "${it.source}|${it.value}|${it.score}" }) { rating ->
                    Surface(shape = RoundedCornerShape(14.dp), color = AppSurface2) {
                        Column(Modifier.padding(horizontal = 13.dp, vertical = 10.dp)) {
                            Text(mdbRatingLabel(rating.source), color = AppTextMuted, fontSize = 11.sp)
                            Text(mdbRatingValue(rating), fontWeight = FontWeight.Black, fontSize = 17.sp)
                        }
                    }
                }
            }
        }

        metadata?.castMembers?.takeIf { it.isNotEmpty() }?.let { cast ->
            Text("Cast", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(if (isTv) 14.dp else 10.dp)) {
                items(cast, key = { "${it.name}|${it.character}" }) { member ->
                    Column(Modifier.width(if (isTv) 130.dp else 96.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
                            shape = RoundedCornerShape(13.dp),
                            colors = CardDefaults.cardColors(containerColor = AppSurface2)
                        ) {
                            Artwork(member.profileUrl, member.name, Modifier.fillMaxSize(), ContentScale.Crop)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(member.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (member.character.isNotBlank()) {
                            Text(member.character, color = AppTextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        metadata?.trailers?.takeIf { it.isNotEmpty() }?.let { trailers ->
            Text("Trailers", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                items(trailers, key = { "${it.site}|${it.key}" }) { trailer ->
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${trailer.key}"))
                                )
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.PlayArrow, null)
                        Spacer(Modifier.width(5.dp))
                        Text(trailer.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        val hasShowDetails = metadata != null || item.releaseInfo.isNotBlank()
        if (hasShowDetails) {
            Text("Show Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            SectionSurface {
                metadata?.status?.takeIf { it.isNotBlank() }?.let { SettingLine("Status", it) }
                val release = metadata?.releaseYear?.takeIf { it.isNotBlank() } ?: item.releaseInfo
                if (release.isNotBlank()) SettingLine("Release", release)
                metadata?.runtimeMinutes?.takeIf { it > 0 }?.let { SettingLine("Runtime", "$it min") }
                metadata?.certification?.takeIf { it.isNotBlank() }?.let { SettingLine("Certification", it) }
                metadata?.originCountries?.takeIf { it.isNotEmpty() }?.let { SettingLine("Country", it.joinToString(", ")) }
                metadata?.originalLanguage?.takeIf { it.isNotBlank() }?.let { SettingLine("Language", it.uppercase()) }
                metadata?.networks?.takeIf { it.isNotEmpty() }?.let { SettingLine("Network", it.joinToString(", ")) }
            }
        }

        metadata?.recommendations?.takeIf { it.isNotEmpty() }?.let { recommendations ->
            Text("More Like This", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Recommendations are metadata only. Playback still comes from your installed Nustrim addons.",
                color = AppTextMuted,
                fontSize = 11.sp
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(recommendations, key = { "${it.mediaType}|${it.id}" }) { recommendation ->
                    Column(Modifier.width(if (isTv) 150.dp else 112.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = AppSurface2)
                        ) {
                            Artwork(recommendation.posterUrl, recommendation.title, Modifier.fillMaxSize(), ContentScale.Crop)
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(recommendation.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                    }
                }
            }
        }

        if (message.isNotBlank()) {
            Text(message, color = Color(0xFFFFC5C9), fontSize = 12.sp)
        }
    }
}

@Composable
private fun StreamSourcePicker(
    title: String,
    item: MediaItem,
    episode: MediaEpisode?,
    resumePositionMs: Long = 0L,
    streams: List<StreamSource>,
    providerProgress: List<StreamProviderProgress> = emptyList(),
    loading: Boolean = false,
    progress: String = "",
    error: String = "",
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onSelect: (StreamSource) -> Unit
) {

        MobileStreamSourcePicker(
            title = title,
            item = item,
            episode = episode,
            resumePositionMs = resumePositionMs,
            streams = streams,
            providerProgress = providerProgress,
            loading = loading,
            progress = progress,
            error = error,
            onDismiss = onDismiss,
            onRefresh = onRefresh,
            onSelect = onSelect
        )

}

private fun streamSourceStableKey(source: StreamSource): String =
    "${source.providerId}|${source.url}|${source.name}|${source.headers.hashCode()}"

@Composable
private fun TvStreamSourcePickerDialog(
    title: String,
    item: MediaItem,
    episode: MediaEpisode?,
    resumePositionMs: Long,
    streams: List<StreamSource>,
    providerProgress: List<StreamProviderProgress>,
    loading: Boolean,
    progress: String,
    error: String,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onSelect: (StreamSource) -> Unit
) {
    BackHandler(onBack = onDismiss)
    val sortedStreams = remember(streams) {
        streams.sortedWith(
            compareBy<StreamSource> { it.providerName.lowercase() }
                .thenBy { it.name.lowercase() }
        )
    }
    val activeProviderStates = remember(providerProgress) {
        providerProgress
            .filter { it.loading || it.hasSources || it.failed }
            .groupBy { it.name.ifBlank { "Other" } }
            .mapValues { (_, states) ->
                states.reduce { a, b ->
                    a.copy(
                        loading = a.loading || b.loading,
                        hasSources = a.hasSources || b.hasSources,
                        failed = a.failed && b.failed
                    )
                }
            }
    }
    val providers = remember(sortedStreams, activeProviderStates) {
        listOf("All") + (
            sortedStreams.map { it.providerName.ifBlank { "Other" } } + activeProviderStates.keys
        ).distinct().sorted()
    }
    var selectedProvider by remember { mutableStateOf("All") }
    LaunchedEffect(providers) {
        if (selectedProvider !in providers) selectedProvider = "All"
    }
    val visibleStreams = remember(sortedStreams, selectedProvider) {
        if (selectedProvider == "All") sortedStreams
        else sortedStreams.filter { it.providerName.ifBlank { "Other" } == selectedProvider }
    }

    val cancelRequester = remember { FocusRequester() }
    val refreshRequester = remember { FocusRequester() }
    val retryRequester = remember { FocusRequester() }
    val providerRequesters = remember(providers) { List(providers.size) { FocusRequester() } }
    val sourceKeys = remember(visibleStreams) { visibleStreams.map(::streamSourceStableKey) }
    val sourceRequesters = remember(sourceKeys) { List(sourceKeys.size) { FocusRequester() } }
    var wasLoading by remember { mutableStateOf(loading) }
    var completedLoadFocusHandled by remember { mutableStateOf(false) }

    val selectedProviderIndex = providers.indexOf(selectedProvider).coerceAtLeast(0)
    fun controlDownTarget(): FocusRequester? = when {
        providers.size > 1 -> providerRequesters.getOrNull(selectedProviderIndex)
        sourceRequesters.isNotEmpty() -> sourceRequesters.firstOrNull()
        error.isNotBlank() && !loading -> retryRequester
        else -> cancelRequester
    }
    fun listUpTarget(): FocusRequester? =
        if (providers.size > 1) providerRequesters.getOrNull(selectedProviderIndex) else cancelRequester

    val episodeCode = episode?.let { ep ->
        when {
            ep.season != null && ep.episode != null -> "S${ep.season} E${ep.episode}"
            else -> ""
        }
    }.orEmpty()
    val heroTitle = episode?.title?.ifBlank { item.title } ?: item.title
    val artwork = item.backgroundUrl.ifBlank { item.posterUrl }

    LaunchedEffect(Unit) {
        delay(120)
        when {
            visibleStreams.isNotEmpty() -> requestTvFocus(sourceRequesters.firstOrNull())
            error.isNotBlank() && !loading -> requestTvFocus(retryRequester)
            else -> requestTvFocus(cancelRequester)
        }
    }
    LaunchedEffect(loading, visibleStreams.size, selectedProvider) {
        if (wasLoading && !loading && visibleStreams.isNotEmpty() && !completedLoadFocusHandled) {
            completedLoadFocusHandled = true
            delay(80)
            requestTvFocus(sourceRequesters.firstOrNull())
        }
        if (loading) completedLoadFocusHandled = false
        wasLoading = loading
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.64f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .fillMaxHeight(0.86f)
                    .widthIn(max = 1040.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFA14161A),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                tonalElevation = 24.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusGroup()
                ) {
                    // M10 Stage 4: compact media context on the left, source surface on the right.
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.34f)
                            .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                    ) {
                        Artwork(artwork, heroTitle, Modifier.fillMaxSize(), ContentScale.Crop)
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.18f),
                                            Color.Black.copy(alpha = 0.52f),
                                            Color(0xFF14161A)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (episodeCode.isNotBlank()) {
                                Text(episodeCode, color = Color.White.copy(alpha = 0.86f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(heroTitle, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            if (episode != null) {
                                Text(item.title, color = Color.White.copy(alpha = 0.68f), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            if (resumePositionMs > 8_000L) {
                                Text(
                                    "Resume from ${formatPlaybackTime(resumePositionMs)}",
                                    color = Color.White.copy(alpha = 0.82f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.66f)
                            .padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Choose source", fontSize = 24.sp, fontWeight = FontWeight.Black)
                                Text(
                                    if (loading) progress.ifBlank { "Finding playable sources..." }
                                    else "${visibleStreams.size} playable source${if (visibleStreams.size == 1) "" else "s"}",
                                    color = AppTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                            TvSourcePickerControl(
                                label = "Refresh",
                                icon = Icons.Outlined.Refresh,
                                requester = refreshRequester,
                                enabled = !loading,
                                onClick = onRefresh,
                                onLeft = { requestTvFocus(cancelRequester) },
                                onRight = { requestTvFocus(cancelRequester) },
                                onDown = { requestTvFocus(controlDownTarget()) }
                            )
                            Spacer(Modifier.width(8.dp))
                            TvSourcePickerControl(
                                label = "Close",
                                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                                requester = cancelRequester,
                                onClick = onDismiss,
                                onLeft = { requestTvFocus(refreshRequester) },
                                onRight = { requestTvFocus(refreshRequester) },
                                onDown = { requestTvFocus(controlDownTarget()) }
                            )
                        }

                        if (providers.size > 1) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                            ) {
                                items(providers.indices.toList(), key = { providers[it] }) { index ->
                                    val provider = providers[index]
                                    val state = activeProviderStates[provider]
                                    TvSourceFilterChip(
                                        label = provider,
                                        selected = selectedProvider == provider,
                                        loading = state?.loading == true,
                                        failed = state?.let { it.failed && !it.hasSources } == true,
                                        requester = providerRequesters[index],
                                        onClick = { selectedProvider = provider },
                                        onLeft = {
                                            requestTvFocus(providerRequesters.getOrNull((index - 1).coerceAtLeast(0)))
                                        },
                                        onRight = {
                                            requestTvFocus(providerRequesters.getOrNull((index + 1).coerceAtMost(providerRequesters.lastIndex)))
                                        },
                                        onUp = { requestTvFocus(cancelRequester) },
                                        onDown = {
                                            if (visibleStreams.isNotEmpty()) requestTvFocus(sourceRequesters.firstOrNull())
                                            else if (error.isNotBlank() && !loading) requestTvFocus(retryRequester)
                                        }
                                    )
                                }
                            }
                        }

                        if (loading) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.055f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Text(progress.ifBlank { "Finding playable sources..." }, color = Color(0xFFD5D7DD), fontSize = 12.sp)
                                }
                            }
                        }

                        if (error.isNotBlank() && visibleStreams.isNotEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF321F24),
                                border = BorderStroke(1.dp, Color(0xFFFF9EA8).copy(alpha = 0.35f))
                            ) {
                                Text(
                                    error,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    color = Color(0xFFFFC0C6),
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White.copy(alpha = 0.045f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
                        ) {
                            when {
                                !loading && visibleStreams.isEmpty() && error.isNotBlank() -> {
                                    TvSourcePickerState(
                                        title = "Could not load sources",
                                        message = error,
                                        actionLabel = "Retry",
                                        requester = retryRequester,
                                        onAction = onRefresh,
                                        onUp = { requestTvFocus(listUpTarget()) }
                                    )
                                }
                                !loading && visibleStreams.isEmpty() -> {
                                    TvSourcePickerState(
                                        title = "No playable sources",
                                        message = "The enabled addons did not return a playable source.",
                                        actionLabel = "Try again",
                                        requester = retryRequester,
                                        onAction = onRefresh,
                                        onUp = { requestTvFocus(listUpTarget()) }
                                    )
                                }
                                visibleStreams.isEmpty() -> {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                                            Spacer(Modifier.height(12.dp))
                                            Text("Waiting for sources...", color = AppTextMuted, fontSize = 13.sp)
                                        }
                                    }
                                }
                                else -> {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(visibleStreams.indices.toList(), key = { sourceKeys[it] }) { index ->
                                            TvStreamSourcePickerRow(
                                                source = visibleStreams[index],
                                                requester = sourceRequesters[index],
                                                onSelect = onSelect,
                                                onUp = {
                                                    if (index > 0) requestTvFocus(sourceRequesters[index - 1])
                                                    else requestTvFocus(listUpTarget())
                                                },
                                                onDown = {
                                                    requestTvFocus(sourceRequesters.getOrNull((index + 1).coerceAtMost(sourceRequesters.lastIndex)))
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvSourcePickerControl(
    label: String,
    icon: ImageVector,
    requester: FocusRequester,
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onDown: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .focusRequester(requester)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft -> { onLeft(); true }
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight -> { onRight(); true }
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown -> { onDown(); true }
                    enabled && isTvActivateKey(event) && event.type == KeyEventType.KeyDown -> true
                    enabled && isTvActivateKey(event) && event.type == KeyEventType.KeyUp -> { onClick(); true }
                    else -> false
                }
            }
            .clickable(enabled = enabled, onClick = onClick)
            .focusable(enabled),
        shape = RoundedCornerShape(14.dp),
        color = when {
            !enabled -> Color.White.copy(alpha = 0.04f)
            focused -> Color.White
            else -> Color.White.copy(alpha = 0.08f)
        },
        border = if (focused) BorderStroke(2.dp, Color.White) else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, Modifier.size(18.dp), tint = if (focused) Color(0xFF17191E) else if (enabled) Color.White else AppTextMuted)
            Spacer(Modifier.width(7.dp))
            Text(label, color = if (focused) Color(0xFF17191E) else if (enabled) Color.White else AppTextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TvSourceFilterChip(
    label: String,
    selected: Boolean,
    loading: Boolean,
    failed: Boolean,
    requester: FocusRequester,
    onClick: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit
) {
    var focused by remember(label) { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .focusRequester(requester)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft -> { onLeft(); true }
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight -> { onRight(); true }
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp -> { onUp(); true }
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown -> { onDown(); true }
                    isTvActivateKey(event) && event.type == KeyEventType.KeyDown -> true
                    isTvActivateKey(event) && event.type == KeyEventType.KeyUp -> { onClick(); true }
                    else -> false
                }
            }
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(18.dp),
        color = when {
            focused -> Color.White
            selected -> AppAccentSoft
            else -> Color.White.copy(alpha = 0.07f)
        },
        border = when {
            focused -> BorderStroke(2.dp, Color.White)
            selected -> BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
            else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = if (focused) Color(0xFF17191E) else if (failed) Color(0xFFFFB7BC) else Color.White,
                fontSize = 12.sp,
                fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.Medium
            )
            if (loading) {
                Spacer(Modifier.width(7.dp))
                CircularProgressIndicator(
                    Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = if (focused) Color(0xFF17191E) else Color.White
                )
            }
        }
    }
}

@Composable
private fun TvSourcePickerState(
    title: String,
    message: String,
    actionLabel: String,
    requester: FocusRequester,
    onAction: () -> Unit,
    onUp: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth(0.76f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Outlined.Source, null, Modifier.size(34.dp), tint = AppTextMuted)
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(message, color = AppTextMuted, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 4, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            var focused by remember { mutableStateOf(false) }
            Surface(
                modifier = Modifier
                    .focusRequester(requester)
                    .onFocusChanged { focused = it.isFocused }
                    .onPreviewKeyEvent { event ->
                        when {
                            event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp -> { onUp(); true }
                            event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown -> true
                            event.type == KeyEventType.KeyDown && (event.key == Key.DirectionLeft || event.key == Key.DirectionRight) -> true
                            isTvActivateKey(event) && event.type == KeyEventType.KeyDown -> true
                            isTvActivateKey(event) && event.type == KeyEventType.KeyUp -> { onAction(); true }
                            else -> false
                        }
                    }
                    .clickable(onClick = onAction)
                    .focusable(),
                shape = RoundedCornerShape(14.dp),
                color = if (focused) Color.White else AppAccentSoft,
                border = if (focused) BorderStroke(2.dp, Color.White) else BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
            ) {
                Text(
                    actionLabel,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
                    color = if (focused) Color(0xFF17191E) else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TvStreamSourcePickerRow(
    source: StreamSource,
    requester: FocusRequester,
    onSelect: (StreamSource) -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit
) {
    var focused by remember(streamSourceStableKey(source)) { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.012f else 1f,
        animationSpec = tween(110),
        label = "tv_source_row_scale"
    )
    val selectSource = { onSelect(source) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .focusRequester(requester)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp -> { onUp(); true }
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown -> { onDown(); true }
                    event.type == KeyEventType.KeyDown && (event.key == Key.DirectionLeft || event.key == Key.DirectionRight) -> true
                    isTvActivateKey(event) && event.type == KeyEventType.KeyDown -> true
                    isTvActivateKey(event) && event.type == KeyEventType.KeyUp -> { selectSource(); true }
                    else -> false
                }
            }
            .clickable(onClick = selectSource)
            .focusable(),
        shape = RoundedCornerShape(14.dp),
        color = if (focused) Color.White else Color(0xFF22252B),
        border = if (focused) BorderStroke(2.dp, Color.White) else BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (focused) Color(0xFF1B1D22) else AppAccentSoft, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.PlayArrow, null, tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        source.providerName.ifBlank { "Stream" },
                        color = if (focused) Color(0xFF17191E) else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        source.type.uppercase(),
                        color = if (focused) Color(0xFF555A63) else AppTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (source.name.isNotBlank() && source.name != source.providerName) {
                    Text(
                        source.name,
                        color = if (focused) Color(0xFF343840) else Color(0xFFD8D9DE),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 13.sp
                    )
                }
                val meta = buildList {
                    if (source.subtitles.isNotEmpty()) add("${source.subtitles.size} subtitles")
                    if (source.note.isNotBlank()) add(source.note)
                }.joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(
                        meta,
                        color = if (focused) Color(0xFF656A73) else AppTextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileStreamSourcePicker(
    title: String,
    item: MediaItem,
    episode: MediaEpisode?,
    resumePositionMs: Long = 0L,
    streams: List<StreamSource>,
    providerProgress: List<StreamProviderProgress> = emptyList(),
    loading: Boolean = false,
    progress: String = "",
    error: String = "",
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onSelect: (StreamSource) -> Unit
) {
    BackHandler(onBack = onDismiss)
    val sortedStreams = remember(streams) {
        streams.sortedWith(
            compareBy<StreamSource> { it.providerName.lowercase() }
                .thenBy { it.name.lowercase() }
        )
    }
    val activeProviderStates = remember(providerProgress) {
        providerProgress
            .filter { it.loading || it.hasSources }
            .groupBy { it.name.ifBlank { "Other" } }
            .mapValues { (_, states) ->
                states.reduce { a, b ->
                    a.copy(
                        loading = a.loading || b.loading,
                        hasSources = a.hasSources || b.hasSources,
                        failed = a.failed && b.failed
                    )
                }
            }
    }
    val providers = remember(sortedStreams, activeProviderStates) {
        listOf("All") + (
            sortedStreams.map { it.providerName.ifBlank { "Other" } } + activeProviderStates.keys
        ).distinct().sorted()
    }
    var selectedProvider by remember { mutableStateOf("All") }
    LaunchedEffect(providers) {
        if (selectedProvider !in providers) selectedProvider = "All"
    }
    val visibleStreams = remember(sortedStreams, selectedProvider) {
        if (selectedProvider == "All") sortedStreams
        else sortedStreams.filter { it.providerName.ifBlank { "Other" } == selectedProvider }
    }
    val portrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val backdrop = item.backgroundUrl.ifBlank { item.posterUrl }
    val heroTitle = episode?.title?.ifBlank { item.title } ?: item.title
    val episodeCode = episode?.let { ep ->
        when {
            ep.season != null && ep.episode != null -> "S${ep.season} E${ep.episode}"
            else -> ""
        }
    }.orEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (portrait) 330.dp else 190.dp)
            ) {
                Artwork(
                    backdrop,
                    heroTitle,
                    Modifier.fillMaxSize(),
                    ContentScale.Crop
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.10f),
                                    Color.Black.copy(alpha = 0.34f),
                                    AppBackground
                                )
                            )
                        )
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.48f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    if (episodeCode.isNotBlank()) {
                        Text(
                            episodeCode,
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        heroTitle,
                        fontWeight = FontWeight.Black,
                        fontSize = if (portrait) 27.sp else 24.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (episode != null) {
                        Text(
                            item.title,
                            color = Color.White.copy(alpha = 0.68f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        Brush.verticalGradient(
                            listOf(AppBackground, Color(0xFF100E0B))
                        )
                    )
                    .padding(horizontal = 20.dp)
            ) {
                if (resumePositionMs > 8_000L) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = AppSurface2
                    ) {
                        Text(
                            "Resume from ${formatPlaybackTime(resumePositionMs)}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onRefresh,
                        enabled = !loading,
                        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(providers, key = { it }) { provider ->
                            val state = activeProviderStates[provider]
                            FilterChip(
                                selected = selectedProvider == provider,
                                onClick = { selectedProvider = provider },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(provider)
                                        if (state?.loading == true) {
                                            Spacer(Modifier.width(6.dp))
                                            CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                if (loading) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(9.dp))
                        Text(
                            progress.ifBlank { "Finding sources..." },
                            color = AppTextMuted,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Spacer(Modifier.height(10.dp))
                }

                if (error.isNotBlank()) {
                    Text(
                        error,
                        color = Color(0xFFFFB7BC),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                val selectedProviderLoading = activeProviderStates[selectedProvider]?.loading == true
                if (selectedProviderLoading && visibleStreams.isEmpty()) {
                    LoadingPanel("$selectedProvider is still loading...")
                } else if (!loading && visibleStreams.isEmpty() && error.isBlank()) {
                    EmptyState("No sources", "No playable sources were returned by the enabled addons.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            visibleStreams,
                            key = { source ->
                                "${source.providerId}|${source.url}|${source.name}|${source.headers.hashCode()}"
                            }
                        ) { source ->
                            StreamSourcePickerRow(source = source, onSelect = onSelect)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamSourcePickerRow(
    source: StreamSource,
    onSelect: (StreamSource) -> Unit
) {
    val isTv = rememberIsTv()
    var focused by remember { mutableStateOf(false) }
    val selectSource = { onSelect(source) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (isTv) consumeTvActivateKey(event, selectSource) else false
            }
            .clickable(onClick = selectSource)
            .focusable(),
        shape = RoundedCornerShape(14.dp),
        color = if (focused) AppAccentSoft else AppSurface2,
        border = if (focused) BorderStroke(2.dp, Color.White) else null
    ) {
        Row(
            modifier = Modifier.padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(AppAccent, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.PlayArrow, null)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    source.providerName.ifBlank { "Stream" },
                    fontWeight = FontWeight.Bold
                )
                if (source.name.isNotBlank() && source.name != source.providerName) {
                    Text(
                        source.name,
                        color = Color(0xFFD8D9DE),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val sourceMeta = buildList {
                    add(source.type.uppercase())
                    if (source.subtitles.isNotEmpty()) add("${source.subtitles.size} subtitles")
                }.joinToString(" · ")
                Text(sourceMeta, color = AppTextMuted, fontSize = 11.sp)
                if (source.note.isNotBlank()) {
                    Text(
                        source.note,
                        color = AppTextMuted,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsHero(
    isTv: Boolean,
    item: MediaItem,
    logoUrl: String,
    onBack: () -> Unit,
    onPrimary: () -> Unit,
    primaryLabel: String,
    playbackHint: String,
    loading: Boolean,
    saved: Boolean,
    canSave: Boolean,
    primaryFocusRequester: FocusRequester? = null,
    saveFocusRequester: FocusRequester? = null,
    trailerFocusRequester: FocusRequester? = null,
    onPrimaryFocused: () -> Unit = {},
    onSaveFocused: () -> Unit = {},
    onTrailerFocused: () -> Unit = {},
    onTrailer: (() -> Unit)? = null,
    onToggleSaved: () -> Unit
) {



    val backdrop = item.backgroundUrl.ifBlank { item.posterUrl }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isTv) 360.dp else 470.dp)
    ) {
        Artwork(backdrop, item.title, Modifier.fillMaxSize(), ContentScale.Crop)

        // M10 Stage 3: Nustrim's own TV detail composition uses a strong left readability
        // field plus a lower fade, following the same TV-first principles as the locked reference.
        if (isTv) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0.00f to AppBackground.copy(alpha = 0.98f),
                                0.34f to AppBackground.copy(alpha = 0.84f),
                                0.62f to AppBackground.copy(alpha = 0.30f),
                                1.00f to Color.Transparent
                            )
                        )
                    )
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Black.copy(alpha = 0.05f),
                                0.55f to Color.Transparent,
                                1.00f to AppBackground
                            )
                        )
                    )
            )
        } else {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = 0.36f),
                            AppBackground
                        )
                    )
                )
            )
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(if (isTv) 18.dp else 12.dp)
                .background(Color.Black.copy(alpha = 0.52f), CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
        }

        if (isTv) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .widthIn(max = 650.dp)
                    .padding(start = 36.dp, end = 24.dp, bottom = 28.dp)
            ) {
                if (logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = item.title,
                        modifier = Modifier.widthIn(max = 285.dp).height(84.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        item.title,
                        fontSize = 34.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (item.releaseInfo.isNotBlank()) {
                    Spacer(Modifier.height(7.dp))
                    Text(
                        item.releaseInfo,
                        color = Color(0xFFD8DADE),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (item.description.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        item.description,
                        color = Color(0xFFD0D2D7),
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvDetailPrimaryAction(
                        label = if (loading) "Loading..." else primaryLabel,
                        enabled = !loading,
                        focusRequester = primaryFocusRequester,
                        onFocused = onPrimaryFocused,
                        onClick = onPrimary
                    )
                    if (onTrailer != null) {
                        TvDetailIconAction(
                            icon = Icons.Outlined.PlayArrow,
                            contentDescription = "Trailer",
                            selected = false,
                            focusRequester = trailerFocusRequester,
                            onFocused = onTrailerFocused,
                            onClick = onTrailer
                        )
                    }
                    if (canSave) {
                        TvDetailIconAction(
                            icon = if (saved) Icons.Outlined.CheckCircle else Icons.Outlined.LibraryAdd,
                            contentDescription = if (saved) "Saved" else "Save",
                            selected = saved,
                            focusRequester = saveFocusRequester,
                            onFocused = onSaveFocused,
                            onClick = onToggleSaved
                        )
                    }
                }
                if (playbackHint.isNotBlank()) {
                    Spacer(Modifier.height(9.dp))
                    Text(playbackHint, color = Color(0xFFD2D4D9), fontSize = 12.sp)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 26.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(Modifier.weight(1f).widthIn(max = 650.dp)) {
                    if (logoUrl.isNotBlank()) {
                        AsyncImage(
                            model = logoUrl,
                            contentDescription = item.title,
                            modifier = Modifier.width(240.dp).height(76.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(
                            item.title,
                            fontSize = 31.sp,
                            lineHeight = 35.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (item.releaseInfo.isNotBlank()) {
                        Spacer(Modifier.height(5.dp))
                        Text(item.releaseInfo, color = Color(0xFFD2D4D9))
                    }
                    if (item.description.isNotBlank()) {
                        Spacer(Modifier.height(9.dp))
                        Text(
                            item.description,
                            color = Color(0xFFD1D2D6),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(15.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onPrimary,
                            enabled = !loading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF18191D)
                            )
                        ) {
                            Icon(Icons.Outlined.PlayArrow, null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (loading) "Loading..." else primaryLabel, fontWeight = FontWeight.Bold)
                        }
                        if (canSave) {
                            OutlinedButton(onClick = onToggleSaved) {
                                Icon(if (saved) Icons.Outlined.CheckCircle else Icons.Outlined.LibraryAdd, null)
                                Spacer(Modifier.width(5.dp))
                                Text(if (saved) "Saved" else "Save")
                            }
                        }
                    }
                    if (playbackHint.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(playbackHint, color = Color(0xFFD2D4D9), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TvDetailPrimaryAction(
    label: String,
    enabled: Boolean,
    focusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val activate = { if (enabled) onClick() }
    Surface(
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .focusProperties { up = FocusRequester.Cancel }
            .onPreviewKeyEvent { event -> consumeTvActivateKey(event, activate) }
            .clickable(enabled = enabled, onClick = activate)
            .focusable(enabled),
        shape = RoundedCornerShape(28.dp),
        color = when {
            !enabled -> Color.White.copy(alpha = 0.48f)
            focused -> Color(0xFFF4F5F7)
            else -> Color.White
        },
        border = if (focused) BorderStroke(3.dp, Color.White.copy(alpha = 0.92f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(Icons.Outlined.PlayArrow, null, tint = Color(0xFF17181B), modifier = Modifier.size(21.dp))
            Text(label, color = Color(0xFF17181B), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun TvDetailIconAction(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    focusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .size(46.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onPreviewKeyEvent { event -> consumeTvActivateKey(event, onClick) }
            .clickable(onClick = onClick)
            .focusable(),
        shape = CircleShape,
        color = if (focused) Color(0xFFE6E8ED) else Color.Black.copy(alpha = 0.54f),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) Color.White else Color.White.copy(alpha = 0.34f)
        )
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription,
                tint = if (focused) Color(0xFF16171A) else if (selected) Color.White else Color(0xFFE7E8EC),
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

@Composable
private fun TvSeasonPill(
    label: String,
    selected: Boolean,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onPreviewKeyEvent { event -> consumeTvActivateKey(event, onClick) }
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(22.dp),
        color = when {
            focused -> Color(0xFFE8EAF0)
            selected -> Color(0xFF353840)
            else -> AppSurface
        },
        border = if (focused) BorderStroke(2.dp, Color.White) else if (selected) BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)) else null
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
            color = if (focused) Color(0xFF17181B) else Color.White,
            fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun TvEpisodeCard(
    episode: MediaEpisode,
    selected: Boolean,
    progressFraction: Float,
    resumePositionMs: Long,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.035f else 1f,
        animationSpec = tween(150),
        label = "detailEpisodeFocus"
    )
    Surface(
        modifier = Modifier
            .width(258.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .zIndex(if (focused) 1f else 0f)
            .focusRequester(focusRequester)
            .then(
                if (upFocusRequester != null) Modifier.focusProperties { up = upFocusRequester }
                else Modifier
            )
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onPreviewKeyEvent { event -> consumeTvActivateKey(event, onClick) }
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Color(0xFF252831) else AppSurface,
        border = when {
            focused -> BorderStroke(2.dp, Color.White)
            selected -> BorderStroke(1.dp, AppAccent)
            else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(AppSurface2)
            ) {
                Artwork(episode.thumbnailUrl, episode.displayTitle, Modifier.fillMaxSize(), ContentScale.Crop)
                if (progressFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color.Black.copy(alpha = 0.42f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(Color.White)
                        )
                    }
                }
            }
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    episode.displayTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (resumePositionMs > 0L) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Resume ${formatPlaybackTime(resumePositionMs)}",
                        color = Color(0xFFD9D0FF),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                } else if (episode.overview.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        episode.overview,
                        color = AppTextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: MediaEpisode,
    selected: Boolean,
    isTv: Boolean,
    progressFraction: Float,
    resumePositionMs: Long,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTv) 24.dp else 16.dp)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (isTv) consumeTvActivateKey(event, onClick) else false
            }
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(15.dp),
        color = if (selected) AppAccentSoft else AppSurface,
        border = when {
            focused -> BorderStroke(2.dp, Color.White)
            selected -> BorderStroke(1.dp, AppAccent)
            else -> null
        }
    ) {
        Column {
            Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(if (isTv) 150.dp else 116.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(AppSurface2)
                ) {
                    Artwork(episode.thumbnailUrl, episode.displayTitle, Modifier.fillMaxSize(), ContentScale.Crop)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        episode.displayTitle,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (resumePositionMs > 0L) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "Resume ${formatPlaybackTime(resumePositionMs)}",
                            color = Color(0xFFD9D0FF),
                            fontSize = 12.sp
                        )
                    }
                    if (episode.overview.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            episode.overview,
                            color = AppTextMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(Icons.Outlined.PlayArrow, null, tint = if (selected) Color.White else AppTextMuted)
            }
            if (progressFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(AppAccent)
                    )
                }
            }
        }
    }
}

@Composable
private fun SourcesPanel(
    isTv: Boolean,
    title: String,
    streams: List<StreamSource>,
    loading: Boolean,
    resolved: Boolean,
    onResolve: () -> Unit,
    onPlay: (StreamSource) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTv) 24.dp else 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onResolve, enabled = !loading) {
                Icon(Icons.Outlined.Refresh, null)
                Spacer(Modifier.width(5.dp))
                Text("Refresh")
            }
        }
        Spacer(Modifier.height(10.dp))

        when {
            loading -> LoadingPanel("Resolving streams...")
            !resolved -> Text("Tap Play or choose an episode to load streams.", color = AppTextMuted)
            streams.isEmpty() -> SectionSurface {
                Text("No stream returned", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(
                    "This metadata source did not return a direct stream.",
                    color = AppTextMuted
                )
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                streams.forEach { stream ->
                    StreamRow(stream, onPlay)
                }
            }
        }
    }
}

@Composable
private fun StreamRow(
    source: StreamSource,
    onPlay: (StreamSource) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .then(if (source.playable) Modifier.clickable { onPlay(source) } else Modifier),
        shape = RoundedCornerShape(14.dp),
        color = AppSurface,
        border = if (focused) BorderStroke(2.dp, Color.White) else null
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(if (source.playable) AppAccent else AppSurface2, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(if (source.playable) Icons.Outlined.PlayArrow else Icons.Outlined.Info, null)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(source.name, fontWeight = FontWeight.Bold)
                Text(source.type.uppercase(), color = AppTextMuted, fontSize = 11.sp)
                if (source.note.isNotBlank()) {
                    Text(source.note, color = AppTextMuted, fontSize = 12.sp, maxLines = 2)
                }
            }
        }
    }
}

@Composable
private fun DeveloperDetails(
    session: SourceSession,
    item: MediaItem,
    episode: MediaEpisode?,
    streams: List<StreamSource>
) {
    SectionSurface(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Build, null, tint = Color(0xFFF0C86E))
            Spacer(Modifier.width(7.dp))
            Text("Developer diagnostics", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(9.dp))
        DiagnosticLine("Session", "${session.kind} · ${session.displayName}")
        DiagnosticLine("Session ID", session.id)
        DiagnosticLine("Media ID", item.id)
        DiagnosticLine(
            "Media ref",
            item.ref?.let { "${it.sourceKind} / ${it.mediaType} / ${it.metaId}" } ?: "none"
        )
        DiagnosticLine("Episode", episode?.id ?: "none")
DiagnosticLine("Streams", streams.size.toString())
if (streams.isNotEmpty()) {
    DiagnosticLine(
        "Providers",
        streams.map { it.providerName.ifBlank { "unknown" } }.distinct().joinToString()
    )
}
    }
}

@Composable
private fun DiagnosticLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, modifier = Modifier.width(92.dp), color = AppTextMuted, fontSize = 12.sp)
        Text(value, modifier = Modifier.weight(1f), fontSize = 12.sp)
    }
}

@Composable
private fun PageHeader(
    title: String,
    subtitle: String,
    isTv: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isTv) 24.dp else 16.dp,
                vertical = if (isTv) 14.dp else 16.dp
            )
    ) {
        Text(
            title,
            style = if (isTv) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(3.dp))
        Text(subtitle, color = AppTextMuted)
    }
}

@Composable
private fun PageHeaderInline(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, color = AppTextMuted)
    }
}

@Composable
private fun SectionSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = AppSurface
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun EmptyState(
    title: String,
    message: String,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 460.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Outlined.Info, null, tint = AppTextMuted, modifier = Modifier.size(34.dp))
            Spacer(Modifier.height(9.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(message, color = AppTextMuted)
            if (action != null && onAction != null) {
                Spacer(Modifier.height(13.dp))
                Button(onClick = onAction) { Text(action) }
            }
        }
    }
}

@Composable
private fun LoadingPanel(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(23.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
        Text(message, color = AppTextMuted)
    }
}

@Composable
private fun DiagnosticBanner(message: String, error: Boolean = false) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (error) Color(0xFF31191D) else AppSurface
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Info,
                null,
                tint = if (error) Color(0xFFFF949B) else Color(0xFFB9C8E8)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                message,
                color = if (error) Color(0xFFFFC5C9) else Color(0xFFD5DBE8)
            )
        }
    }
}

@Composable
private fun Artwork(
    url: String,
    title: String,
    modifier: Modifier,
    contentScale: ContentScale
) {
    if (url.isBlank()) {
        Box(
            modifier = modifier.background(
                Brush.linearGradient(listOf(Color(0xFF282A30), Color(0xFF15171B)))
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                title.take(1).uppercase().ifBlank { "T" },
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = title,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

@Composable
private fun StatusScreen(
    title: String,
    message: String,
    loading: Boolean,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        SectionSurface(modifier = Modifier.widthIn(max = 500.dp).padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            if (loading) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
            }
            Text(message, color = if (loading) AppTextMuted else MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = onBack) { Text("Back") }
        }
    }
}

private data class PlayerSubtitleTrack(
    val group: Tracks.Group,
    val trackIndex: Int,
    val languageCode: String,
    val languageLabel: String,
    val providerName: String,
    val label: String,
    val selected: Boolean
)

private const val SubtitleProviderSeparator = "|||NUSTRIM_PROVIDER|||"

private fun splitSubtitleProviderLabel(raw: String): Pair<String, String> {
    val parts = raw.split(SubtitleProviderSeparator, limit = 2)
    return if (parts.size == 2) {
        parts[0].ifBlank { "Subtitle" } to parts[1].ifBlank { "Subtitle" }
    } else {
        "Embedded" to raw.ifBlank { "Subtitle" }
    }
}

private data class PlayerAudioTrack(
    val group: Tracks.Group,
    val trackIndex: Int,
    val languageLabel: String,
    val label: String,
    val channelLabel: String,
    val codecLabel: String,
    val selected: Boolean
)

private fun friendlySubtitleLanguage(code: String): String {
    val clean = code.trim()
    if (clean.isBlank() || clean.equals("und", ignoreCase = true)) return "Unknown"
    return runCatching {
        Locale.forLanguageTag(clean).getDisplayLanguage(Locale.getDefault())
            .takeIf { it.isNotBlank() }
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            ?: clean
    }.getOrDefault(clean)
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun PlayerScreen(
    isTv: Boolean,
    request: PlaybackRequest,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val observableConfiguration = LocalConfiguration.current
    val clockLocale = remember(observableConfiguration) {
        ConfigurationCompat.getLocales(observableConfiguration)[0] ?: Locale.US
    }
    val preferences = remember(context) { UiPreferences(context) }
    val mediaStore = remember(context) { LocalMediaStore(context) }
    val streamResolver = remember(context) { StreamResolver(context) }
    val subtitleResolver = remember(context) { SubtitleResolver(context) }
    val scope = rememberCoroutineScope()
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val playerControlRequesters = remember(request.item.id) { List(6) { FocusRequester() } }
    val tvPlayFocusRequester = remember(request.item.id) { FocusRequester() }
    var controlsActivityToken by remember(request.item.id) { mutableIntStateOf(0) }

    var currentEpisode by remember(request.item.id) { mutableStateOf(request.episode) }
    var currentSource by remember(request.item.id) { mutableStateOf(request.source) }
    var availableSources by remember(request.item.id) {
        mutableStateOf(request.availableSources.ifEmpty { listOf(request.source) })
    }
    var startPositionMs by remember(request.item.id, request.forceStartAtZero) {
        mutableStateOf(
            if (request.forceStartAtZero) 0L
            else mediaStore.resumePosition(request.sourceUrl, request.item, request.episode)
        )
    }
    var retryToken by remember(request.item.id) { mutableIntStateOf(0) }
    var playerError by remember(request.item.id) { mutableStateOf("") }
    var playbackEnded by remember(request.item.id) { mutableStateOf(false) }
    var controlsVisible by remember(request.item.id) { mutableStateOf(true) }
    var isPlaying by remember(request.item.id) { mutableStateOf(false) }
    var isBuffering by remember(request.item.id) { mutableStateOf(true) }
    var positionMs by remember(request.item.id) { mutableStateOf(0L) }
    var durationMs by remember(request.item.id) { mutableStateOf(0L) }
    var scrubbing by remember(request.item.id) { mutableStateOf(false) }
    var scrubPositionMs by remember(request.item.id) { mutableStateOf(0L) }
    var resizeIndex by remember(request.item.id) { mutableIntStateOf(0) }
    var speedIndex by remember(request.item.id) { mutableIntStateOf(2) }
    var sourcePanelVisible by remember(request.item.id) { mutableStateOf(false) }
    var subtitlePanelVisible by remember(request.item.id) { mutableStateOf(false) }
    var audioPanelVisible by remember(request.item.id) { mutableStateOf(false) }
    var episodePanelVisible by remember(request.item.id) { mutableStateOf(false) }
    var sourceLoading by remember(request.item.id) { mutableStateOf(false) }
    var sourceError by remember(request.item.id) { mutableStateOf("") }
    var sourceProgress by remember(request.item.id) { mutableStateOf("") }
    var sourceProviderProgress by remember(request.item.id) { mutableStateOf<List<StreamProviderProgress>>(emptyList()) }
    var sourceChoices by remember(request.item.id) { mutableStateOf(availableSources) }
    var pendingEpisode by remember(request.item.id) { mutableStateOf(request.episode) }
    var hudMessage by remember(request.item.id) { mutableStateOf("") }
    var hudToken by remember(request.item.id) { mutableIntStateOf(0) }
    var trackRevision by remember(request.item.id) { mutableIntStateOf(0) }
    var subtitleFontSp by remember(request.item.id) {
        mutableIntStateOf(readDefaultSubtitleFontSize(context))
    }
    var subtitleBold by remember(request.item.id) {
        mutableStateOf(readDefaultSubtitleBold(context))
    }
    var playerViewRef by remember(request.item.id) { mutableStateOf<PlayerView?>(null) }
    var pauseClockText by remember(request.item.id) { mutableStateOf("") }

    val playingEpisode = currentEpisode
    val playingSource = currentSource
    val playingTitle = playingEpisode?.let { "${request.item.title} · ${it.displayTitle}" } ?: request.item.title
    val player = remember(playingSource.url, playingEpisode?.id, startPositionMs, retryToken) {
        PlayerFactory.create(context, playingTitle, playingSource, startPositionMs)
    }

    fun focusPlayerControl(index: Int = 2) {
        controlsVisible = true
        controlsActivityToken += 1
        scope.launch {
            delay(24)
            runCatching { playerControlRequesters[index.coerceIn(0, 5)].requestFocus() }
        }
    }

    fun focusTvPrimaryControl() {
        controlsVisible = true
        controlsActivityToken += 1
        scope.launch {
            delay(24)
            runCatching { tvPlayFocusRequester.requestFocus() }
        }
    }

    val resizeModes = remember {
        listOf(
            AspectRatioFrameLayout.RESIZE_MODE_FIT,
            AspectRatioFrameLayout.RESIZE_MODE_FILL,
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        )
    }
    val resizeLabels = remember { listOf("Fit", "Fill", "Zoom") }
    val speeds = remember { listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f) }

    fun applySubtitleStyle(sizeSp: Int = subtitleFontSp, bold: Boolean = subtitleBold) {
        playerViewRef?.subtitleView?.apply {
            setApplyEmbeddedFontSizes(false)
            setApplyEmbeddedStyles(false)
            setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp.toFloat())
            setStyle(
                CaptionStyleCompat(
                    android.graphics.Color.WHITE,
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                    CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                    android.graphics.Color.BLACK,
                    if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                )
            )
        }
    }

    fun showHud(message: String) {
        hudMessage = message
        hudToken += 1
    }

    fun adjustVolume(direction: Int) {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (direction > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            0
        )
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        showHud("Volume ${(current * 100 / max).coerceIn(0, 100)}%")
    }

    fun seekBy(deltaMs: Long) {
        val duration = player.duration.takeIf { it > 0L }
        val target = (player.currentPosition + deltaMs).coerceAtLeast(0L)
        player.seekTo(duration?.let { target.coerceAtMost(it) } ?: target)
        showHud(if (deltaMs < 0) "-10s" else "+10s")
    }

    fun persistProgress(completed: Boolean = false) {
        mediaStore.recordProgress(
            sourceUrl = request.sourceUrl,
            item = request.item,
            episode = playingEpisode,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.takeIf { it > 0L } ?: 0L,
            completed = completed
        )
    }

    fun selectSource(source: StreamSource, episode: MediaEpisode? = pendingEpisode) {
        val sameEpisode = episode?.id == playingEpisode?.id
        val nextPosition = if (sameEpisode) {
            player.currentPosition.coerceAtLeast(0L)
        } else {
            mediaStore.resumePosition(request.sourceUrl, request.item, episode)
        }
        persistProgress()
        currentEpisode = episode
        currentSource = source
        availableSources = sourceChoices.ifEmpty { listOf(source) }
        startPositionMs = nextPosition
        sourcePanelVisible = false
        playerError = ""
        playbackEnded = false
        controlsVisible = true
    }

    fun resolveSourcesFor(episode: MediaEpisode?, autoStart: Boolean) {
        if (sourceLoading) return
        pendingEpisode = episode
        sourceChoices = emptyList()
        sourceError = ""
        sourceProgress = "Starting providers..."
        sourceProviderProgress = emptyList()
        sourceLoading = true
        if (!autoStart) sourcePanelVisible = true

        var latestStreams = emptyList<StreamSource>()
        var latestSubtitles = emptyList<app.nudroidlabs.nustrim.core.model.SubtitleSource>()
        var streamsDone = false
        var subtitlesDone = false
        var autoStarted = false

        fun publish() {
            val enriched = latestStreams.map { stream ->
                stream.copy(
                    subtitles = (stream.subtitles + latestSubtitles)
                        .distinctBy { "${it.url}|${it.language}" }
                )
            }
            sourceChoices = enriched.filter { it.playable && it.url.isNotBlank() }
            if (streamsDone && subtitlesDone) {
                sourceLoading = false
                sourceProgress = "${sourceChoices.size} playable source(s)"
                if (sourceChoices.isEmpty() && sourceError.isBlank()) {
                    sourceError = "No playable sources found."
                }
                if (autoStart && sourceChoices.isNotEmpty() && !autoStarted) {
                    autoStarted = true
                    selectSource(sourceChoices.first(), episode)
                }
            }
        }

        subtitleResolver.resolve(
            originSession = request.session,
            item = request.item,
            episode = episode,
            developerMode = preferences.developerMode,
            onSuccess = { subtitles ->
                scope.launch {
                    latestSubtitles = subtitles
                    subtitlesDone = true
                    publish()
                }
            }
        )

        streamResolver.resolve(
            originSession = request.session,
            item = request.item,
            episode = episode,
            developerMode = preferences.developerMode,
            onSuccess = { resolved ->
                scope.launch {
                    latestStreams = resolved
                    streamsDone = true
                    publish()
                }
            },
            onError = { throwable ->
                scope.launch {
                    streamsDone = true
                    sourceError = throwable.message ?: throwable.javaClass.simpleName
                    publish()
                }
            },
            onProgress = { partial, completed, total, lastError ->
                scope.launch {
                    latestStreams = partial
                    sourceProgress = "Checking providers $completed/$total"
                    if (lastError != null && partial.isEmpty()) {
                        sourceError = lastError.message ?: lastError.javaClass.simpleName
                    }
                    publish()
                }
            },
            onProviderProgress = { states ->
                scope.launch { sourceProviderProgress = states }
            }
        )
    }

    BackHandler {
        when {
            subtitlePanelVisible -> { subtitlePanelVisible = false; focusPlayerControl(2) }
            audioPanelVisible -> { audioPanelVisible = false; focusPlayerControl(3) }
            sourcePanelVisible -> { sourcePanelVisible = false; focusPlayerControl(4) }
            episodePanelVisible -> { episodePanelVisible = false; focusPlayerControl(5) }
            controlsVisible -> {
                controlsVisible = false
                playerViewRef?.requestFocus()
            }
            else -> {
                persistProgress()
                onBack()
            }
        }
    }

    LaunchedEffect(player, speedIndex) {
        player.setPlaybackSpeed(speeds[speedIndex])
    }

    LaunchedEffect(player) {
        var lastSavedPositionMs = startPositionMs
        while (true) {
            delay(500)
            val duration = player.duration.takeIf { it > 0L } ?: 0L
            val position = player.currentPosition.coerceAtLeast(0L)
            positionMs = position
            durationMs = duration
            if (position > 0L && abs(position - lastSavedPositionMs) >= 10_000L) {
                mediaStore.recordProgress(
                    sourceUrl = request.sourceUrl,
                    item = request.item,
                    episode = playingEpisode,
                    positionMs = position,
                    durationMs = duration
                )
                lastSavedPositionMs = position
            }
        }
    }

    LaunchedEffect(
        controlsVisible,
        controlsActivityToken,
        isPlaying,
        isBuffering,
        playbackEnded,
        subtitlePanelVisible,
        audioPanelVisible,
        sourcePanelVisible,
        episodePanelVisible
    ) {
        if (
            controlsVisible &&
            !isBuffering &&
            !playbackEnded &&
            !subtitlePanelVisible &&
            !audioPanelVisible &&
            !sourcePanelVisible &&
            !episodePanelVisible
        ) {
            val activityAtStart = controlsActivityToken
            delay(if (isTv) 4_000 else 5_000)
            if (controlsActivityToken == activityAtStart) {
                controlsVisible = false
                if (isTv) playerViewRef?.requestFocus()
            }
        }
    }

    LaunchedEffect(hudToken) {
        if (hudMessage.isNotBlank()) {
            delay(1_200)
            hudMessage = ""
        }
    }

    LaunchedEffect(isTv) {
        if (isTv) {
            while (true) {
                pauseClockText = SimpleDateFormat("h:mm a", clockLocale).format(Date())
                delay(30_000)
            }
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                when (playbackState) {
                    Player.STATE_READY -> {
                        playerError = ""
                        playbackEnded = false
                    }
                    Player.STATE_ENDED -> {
                        persistProgress(completed = true)
                        val next = request.item.nextEpisodeAfter(playingEpisode)
                        if (next != null && preferences.autoplayNextEpisode) {
                            resolveSourcesFor(next, autoStart = true)
                        } else {
                            playbackEnded = true
                            controlsVisible = true
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(value: Boolean) {
                isPlaying = value
            }

            override fun onTracksChanged(tracks: Tracks) {
                trackRevision += 1
            }

            override fun onPlayerError(error: PlaybackException) {
                playerError = error.message ?: error.errorCodeName
                controlsVisible = true
                NustrimDiagnostics.error(
                    "PLAYER_UI_ERROR",
                    error,
                    "title=$playingTitle provider=${playingSource.providerName}"
                )
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            if (player.playbackState != Player.STATE_ENDED) persistProgress()
            player.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    keepScreenOn = true
                    resizeMode = resizeModes[resizeIndex]
                    this.player = player
                    playerViewRef = this
                    subtitleView?.apply {
                        visibility = android.view.View.VISIBLE
                        setApplyEmbeddedFontSizes(false)
                        setApplyEmbeddedStyles(false)
                        setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleFontSp.toFloat())
                    }
                    if (isTv) {
                        isFocusable = true
                        isFocusableInTouchMode = true
                        requestFocus()
                        setOnKeyListener { _, keyCode, event ->
                            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
                            if (subtitlePanelVisible || audioPanelVisible || sourcePanelVisible || episodePanelVisible) {
                                return@setOnKeyListener false
                            }
                            when (keyCode) {
                                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                    focusTvPrimaryControl()
                                    true
                                }
                                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_SPACE -> {
                                    if (player.isPlaying) {
                                        player.pause()
                                        if (isTv) controlsVisible = false else showHud("Pause")
                                    } else {
                                        player.play()
                                        if (isTv) controlsVisible = false else showHud("Play")
                                    }
                                    true
                                }
                                KeyEvent.KEYCODE_DPAD_LEFT -> {
                                    if (controlsVisible) focusPlayerControl(1) else seekBy(-10_000L)
                                    true
                                }
                                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                    if (controlsVisible) focusPlayerControl(3) else seekBy(10_000L)
                                    true
                                }
                                KeyEvent.KEYCODE_MEDIA_REWIND -> {
                                    seekBy(-10_000L)
                                    true
                                }
                                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                                    seekBy(10_000L)
                                    true
                                }
                                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                                    focusTvPrimaryControl()
                                    true
                                }
                                KeyEvent.KEYCODE_VOLUME_UP -> {
                                    adjustVolume(1)
                                    true
                                }
                                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                                    adjustVolume(-1)
                                    true
                                }
                                else -> false
                            }
                        }
                    }
                }
            },
            update = { view ->
                playerViewRef = view
                view.player = player
                view.resizeMode = resizeModes[resizeIndex]
                view.subtitleView?.visibility = android.view.View.VISIBLE
                applySubtitleStyle()
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(player, durationMs) {
                    var totalX = 0f
                    var totalY = 0f
                    var gestureMode = 0
                    var gestureStartPosition = 0L
                    var gestureTargetPosition = 0L
                    var gestureStartVolume = 0
                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)

                    detectDragGestures(
                        onDragStart = {
                            if (!subtitlePanelVisible && !audioPanelVisible && !sourcePanelVisible && !episodePanelVisible) {
                                totalX = 0f
                                totalY = 0f
                                gestureMode = 0
                                gestureStartPosition = player.currentPosition.coerceAtLeast(0L)
                                gestureTargetPosition = gestureStartPosition
                                gestureStartVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                controlsVisible = true
                            }
                        },
                        onDrag = { change, dragAmount ->
                            if (!subtitlePanelVisible && !audioPanelVisible && !sourcePanelVisible && !episodePanelVisible) {
                                change.consume()
                                totalX += dragAmount.x
                                totalY += dragAmount.y
                            if (gestureMode == 0 && maxOf(abs(totalX), abs(totalY)) > 16f) {
                                gestureMode = if (abs(totalX) >= abs(totalY)) 1 else 2
                            }
                            when (gestureMode) {
                                1 -> {
                                    val duration = player.duration.takeIf { it > 0L } ?: durationMs
                                    val seekWindow = if (duration > 0L) {
                                        (duration / 8L).coerceIn(120_000L, 900_000L)
                                    } else {
                                        300_000L
                                    }
                                    val width = size.width.toFloat().coerceAtLeast(1f)
                                    val delta = (totalX / width * seekWindow).toLong()
                                    gestureTargetPosition = (gestureStartPosition + delta)
                                        .coerceAtLeast(0L)
                                        .let { target -> if (duration > 0L) target.coerceAtMost(duration) else target }
                                    scrubbing = true
                                    scrubPositionMs = gestureTargetPosition
                                    val signed = if (delta >= 0L) "+${formatPlaybackTime(abs(delta))}" else "-${formatPlaybackTime(abs(delta))}"
                                    hudMessage = "$signed  •  ${formatPlaybackTime(gestureTargetPosition)}"
                                }
                                2 -> {
                                    val height = size.height.toFloat().coerceAtLeast(1f)
                                    val deltaVolume = (-totalY / height * maxVolume).toInt()
                                    val targetVolume = (gestureStartVolume + deltaVolume).coerceIn(0, maxVolume)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
                                    hudMessage = "Volume ${(targetVolume * 100 / maxVolume).coerceIn(0, 100)}%"
                                }
                            }
                            }
                        },
                        onDragEnd = {
                            if (gestureMode == 1) {
                                player.seekTo(gestureTargetPosition)
                                scrubbing = false
                            }
                            if (gestureMode != 0) hudToken += 1
                        },
                        onDragCancel = {
                            scrubbing = false
                            if (gestureMode != 0) hudToken += 1
                        }
                    )
                }
                .clickable {
                    if (!subtitlePanelVisible && !audioPanelVisible && !sourcePanelVisible && !episodePanelVisible) {
                        controlsVisible = !controlsVisible
                    }
                }
        )

        if (isBuffering && playerError.isBlank()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(44.dp),
                color = Color.White,
                strokeWidth = 3.dp
            )
        }

        if (hudMessage.isNotBlank()) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = RoundedCornerShape(18.dp),
                color = Color.Black.copy(alpha = 0.72f)
            ) {
                Text(
                    hudMessage,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isTv) 18.sp else 16.sp
                )
            }
        }

        if (
            isTv &&
            !isPlaying &&
            !isBuffering &&
            !playbackEnded &&
            playerError.isBlank() &&
            !subtitlePanelVisible &&
            !audioPanelVisible &&
            !sourcePanelVisible &&
            !episodePanelVisible
        ) {
            val remainingMs = (durationMs - positionMs).coerceAtLeast(0L)
            val endsText = if (durationMs > 0L && remainingMs > 0L) {
                SimpleDateFormat("h:mm a", clockLocale)
                    .format(Date(System.currentTimeMillis() + remainingMs))
            } else {
                ""
            }
            TvPauseInfoOverlay(
                item = request.item,
                episode = playingEpisode,
                clockText = pauseClockText,
                endsText = endsText
            )
        }


        AnimatedVisibility(
            visible = controlsVisible && !subtitlePanelVisible && !audioPanelVisible && !sourcePanelVisible && !episodePanelVisible,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(animationSpec = tween(120)),
            exit = fadeOut(animationSpec = tween(180))
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .onPreviewKeyEvent { event ->
                            if (isTv && event.type == KeyEventType.KeyDown) controlsActivityToken += 1
                            false
                        }
                        .background(Color.Black.copy(alpha = 0.20f))
                )
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 28.dp, end = 20.dp, top = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        playingTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isTv) 20.sp else 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val provider = playingSource.providerName.ifBlank { playingSource.name }
                    Text(
                        listOf(provider, playingSource.type.uppercase()).filter { it.isNotBlank() }.joinToString("  •  "),
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = if (isTv) 12.sp else 11.sp
                    )
                }
                if (isTv) {
                    val remainingMs = (durationMs - positionMs).coerceAtLeast(0L)
                    val endsText = if (durationMs > 0L && remainingMs > 0L) {
                        SimpleDateFormat("h:mm a", clockLocale)
                            .format(Date(System.currentTimeMillis() + remainingMs))
                    } else {
                        ""
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            pauseClockText,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.96f)
                        )
                        if (endsText.isNotBlank()) {
                            Text(
                                "Ends $endsText",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.68f)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.54f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                    ) {
                        IconButton(
                            onClick = {
                                persistProgress()
                                onBack()
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                        }
                    }
                }
            }

            if (!isTv) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(if (isTv) 54.dp else 34.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            seekBy(-10_000L)
                            controlsVisible = true
                        },
                        modifier = Modifier.size(if (isTv) 68.dp else 60.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("↶10", fontSize = if (isTv) 19.sp else 17.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            if (player.isPlaying) {
                                player.pause()
                                showHud("Pause")
                            } else {
                                player.play()
                                showHud("Play")
                            }
                            controlsVisible = true
                        },
                        modifier = Modifier.size(if (isTv) 82.dp else 72.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.72f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        if (isPlaying) {
                            Text("Ⅱ", fontSize = if (isTv) 32.sp else 28.sp, fontWeight = FontWeight.Black)
                        } else {
                            Icon(Icons.Outlined.PlayArrow, "Play", modifier = Modifier.size(if (isTv) 42.dp else 36.dp))
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            seekBy(10_000L)
                            controlsVisible = true
                        },
                        modifier = Modifier.size(if (isTv) 68.dp else 60.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("10↷", fontSize = if (isTv) 19.sp else 17.sp, fontWeight = FontWeight.Bold)
                    }
                }

            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(
                        start = if (isTv) 30.dp else 18.dp,
                        end = if (isTv) 30.dp else 18.dp,
                        bottom = if (isTv) 10.dp else 8.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val sliderDuration = durationMs.coerceAtLeast(1L)
                val sliderPosition = if (scrubbing) scrubPositionMs else positionMs
                Slider(
                    value = sliderPosition.coerceIn(0L, sliderDuration).toFloat(),
                    onValueChange = {
                        scrubbing = true
                        scrubPositionMs = it.toLong()
                    },
                    onValueChangeFinished = {
                        player.seekTo(scrubPositionMs)
                        scrubbing = false
                        controlsVisible = true
                    },
                    valueRange = 0f..sliderDuration.toFloat(),
                    modifier = Modifier.fillMaxWidth().height(18.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(formatPlaybackTime(sliderPosition), fontSize = 11.sp, color = Color.White.copy(alpha = 0.92f))
                    Spacer(Modifier.weight(1f))
                    Text(formatPlaybackTime(durationMs), fontSize = 11.sp, color = Color.White.copy(alpha = 0.92f))
                }
                Spacer(Modifier.height(5.dp))

                Surface(
                    modifier = Modifier.onPreviewKeyEvent { event ->
                        if (isTv && event.type == KeyEventType.KeyDown) controlsActivityToken += 1
                        false
                    },
                    shape = RoundedCornerShape(30.dp),
                    color = Color.Black.copy(alpha = 0.70f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isTv) {
                            OutlinedButton(
                                onClick = {
                                    if (player.isPlaying) {
                                        player.pause()
                                    } else {
                                        player.play()
                                    }
                                    controlsVisible = false
                                },
                                modifier = Modifier
                                    .focusRequester(tvPlayFocusRequester),
                                border = null,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 13.dp, vertical = 7.dp)
                            ) {
                                if (isPlaying) {
                                    Text("Pause", fontWeight = FontWeight.Bold)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.PlayArrow, "Play", modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Play", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                resizeIndex = (resizeIndex + 1) % resizeModes.size
                                controlsVisible = true
                            },
                            modifier = Modifier
                                .focusRequester(playerControlRequesters[0]),
                            border = null,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 13.dp, vertical = 7.dp)
                        ) { Text(resizeLabels[resizeIndex], fontWeight = FontWeight.Bold) }

                        OutlinedButton(
                            onClick = {
                                speedIndex = (speedIndex + 1) % speeds.size
                                player.setPlaybackSpeed(speeds[speedIndex])
                                controlsVisible = true
                            },
                            modifier = Modifier
                                .focusRequester(playerControlRequesters[1]),
                            border = null,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 13.dp, vertical = 7.dp)
                        ) {
                            val speed = speeds[speedIndex]
                            Text(if (speed == 1f) "1x" else "${speed}x", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                subtitlePanelVisible = true
                                controlsVisible = true
                            },
                            modifier = Modifier
                                .focusRequester(playerControlRequesters[2]),
                            border = null,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 13.dp, vertical = 7.dp)
                        ) { Text("Subs", fontWeight = FontWeight.Bold) }

                        OutlinedButton(
                            onClick = {
                                audioPanelVisible = true
                                controlsVisible = true
                            },
                            modifier = Modifier
                                .focusRequester(playerControlRequesters[3]),
                            border = null,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 13.dp, vertical = 7.dp)
                        ) { Text("Audio", fontWeight = FontWeight.Bold) }

                        OutlinedButton(
                            onClick = {
                                pendingEpisode = playingEpisode
                                sourceChoices = availableSources
                                sourceProviderProgress = availableSources
                                    .groupBy { it.providerName.ifBlank { "Other" } }
                                    .map { (name, providerSources) ->
                                        StreamProviderProgress(
                                            id = "existing:$name",
                                            name = name,
                                            loading = false,
                                            hasSources = providerSources.any { it.playable && it.url.isNotBlank() }
                                        )
                                    }
                                sourceError = ""
                                sourceProgress = "${availableSources.size} source(s)"
                                sourceLoading = false
                                sourcePanelVisible = true
                            },
                            modifier = Modifier
                                .focusRequester(playerControlRequesters[4]),
                            border = null,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 13.dp, vertical = 7.dp)
                        ) { Text("Sources", fontWeight = FontWeight.Bold) }

                        if (request.item.episodes.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { episodePanelVisible = true },
                                modifier = Modifier
                                    .focusRequester(playerControlRequesters[5]),
                                border = null,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 13.dp, vertical = 7.dp)
                            ) { Text("Episodes", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
        }


        if (playerError.isNotBlank()) {
            SectionSurface(
                modifier = Modifier.align(Alignment.Center).widthIn(max = 520.dp).padding(18.dp)
            ) {
                Text("Playback failed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(playerError, color = Color(0xFFFFB7BC))
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        startPositionMs = player.currentPosition.coerceAtLeast(0L)
                        playerError = ""
                        retryToken += 1
                    }) {
                        Icon(Icons.Outlined.Refresh, null)
                        Spacer(Modifier.width(5.dp))
                        Text("Retry")
                    }
                    val currentIndex = availableSources.indexOfFirst { it.url == playingSource.url }
                    val nextSource = availableSources.getOrNull(currentIndex + 1)
                    if (nextSource != null) {
                        OutlinedButton(onClick = {
                            sourceChoices = availableSources
                            pendingEpisode = playingEpisode
                            selectSource(nextSource, playingEpisode)
                        }) { Text("Try next source") }
                    }
                    OutlinedButton(onClick = {
                        persistProgress()
                        onBack()
                    }) { Text("Back") }
                }
            }
        } else if (playbackEnded) {
            val next = request.item.nextEpisodeAfter(playingEpisode)
            if (next != null) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Black.copy(alpha = 0.78f)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Up next", color = AppTextMuted, fontSize = 12.sp)
                        Text(next.displayTitle, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = { resolveSourcesFor(next, autoStart = false) }) {
                            Icon(Icons.Outlined.PlayArrow, null)
                            Spacer(Modifier.width(5.dp))
                            Text("Choose source")
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = sourcePanelVisible,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            PlayerSourcesPanel(
                title = playingEpisode?.displayTitle ?: request.item.title,
                streams = sourceChoices,
                providerProgress = sourceProviderProgress,
                playingSource = playingSource,
                loading = sourceLoading,
                progress = sourceProgress,
                error = sourceError,
                onClose = { sourcePanelVisible = false; focusPlayerControl(4) },
                onReload = { resolveSourcesFor(pendingEpisode ?: playingEpisode, autoStart = false) },
                onPlay = { source -> selectSource(source, pendingEpisode ?: playingEpisode) }
            )
        }

        AnimatedVisibility(
            visible = episodePanelVisible,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            PlayerEpisodesPanel(
                title = request.item.title,
                fallbackArtwork = request.item.backgroundUrl.ifBlank { request.item.posterUrl },
                episodes = request.item.episodes,
                currentEpisode = playingEpisode,
                onClose = { episodePanelVisible = false; focusPlayerControl(5) },
                onSelect = { episode ->
                    episodePanelVisible = false
                    resolveSourcesFor(episode, autoStart = false)
                }
            )
        }

        if (subtitlePanelVisible) {
            PlayerSubtitlesPanel(
                player = player,
                trackRevision = trackRevision,
                fontSizeSp = subtitleFontSp,
                bold = subtitleBold,
                preferredLanguage = preferences.subtitlePreferredLanguage,
                secondPreferredLanguage = preferences.subtitleSecondPreferredLanguage,
                displayMode = preferences.subtitleDisplayMode,
                onFontSize = { next ->
                    subtitleFontSp = next
                    writeDefaultSubtitleFontSize(context, next)
                    applySubtitleStyle(sizeSp = next)
                },
                onBold = { next ->
                    subtitleBold = next
                    writeDefaultSubtitleBold(context, next)
                    applySubtitleStyle(bold = next)
                },
                onClose = { subtitlePanelVisible = false; focusPlayerControl(2) },
                onTrackChanged = {
                    playerViewRef?.subtitleView?.visibility = android.view.View.VISIBLE
                    applySubtitleStyle()
                    trackRevision += 1
                }
            )
        }

        if (audioPanelVisible) {
            PlayerAudioPanel(
                player = player,
                trackRevision = trackRevision,
                onClose = { audioPanelVisible = false; focusPlayerControl(3) },
                onTrackChanged = { trackRevision += 1 }
            )
        }
    }
}

@Composable
private fun TvPauseInfoOverlay(
    item: MediaItem,
    episode: MediaEpisode?,
    clockText: String,
    endsText: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.86f),
                        Color.Black.copy(alpha = 0.58f),
                        Color.Black.copy(alpha = 0.18f),
                        Color.Transparent
                    )
                )
            )
    ) {
        if (clockText.isNotBlank()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 18.dp, end = 30.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    clockText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.94f)
                )
                if (endsText.isNotBlank()) {
                    Text(
                        "Ends $endsText",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.68f)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.58f)
                .padding(start = 42.dp, end = 28.dp)
        ) {
            Text(
                item.title,
                fontSize = 38.sp,
                lineHeight = 41.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val metadata = listOf(
                item.type.name.lowercase().replaceFirstChar { it.uppercase() },
                item.releaseInfo,
                episode?.displayTitle.orEmpty()
            ).filter { it.isNotBlank() }.joinToString("  •  ")
            if (metadata.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    metadata,
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (item.description.isNotBlank()) {
                Spacer(Modifier.height(15.dp))
                Text(
                    item.description,
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PlayerSourcesPanel(
    title: String,
    streams: List<StreamSource>,
    providerProgress: List<StreamProviderProgress> = emptyList(),
    playingSource: StreamSource,
    loading: Boolean,
    progress: String,
    error: String,
    onClose: () -> Unit,
    onReload: () -> Unit,
    onPlay: (StreamSource) -> Unit
) {
    BackHandler(onBack = onClose)
    val isTv = rememberIsTv()
    val sortedStreams = remember(streams) {
        streams.sortedWith(
            compareBy<StreamSource> { it.providerName.lowercase() }
                .thenBy { it.name.lowercase() }
        )
    }
    val activeProviderStates = remember(providerProgress) {
        providerProgress
            .filter { it.loading || it.hasSources }
            .groupBy { it.name.ifBlank { "Other" } }
            .mapValues { (_, states) ->
                states.reduce { a, b ->
                    a.copy(loading = a.loading || b.loading, hasSources = a.hasSources || b.hasSources)
                }
            }
    }
    val providers = remember(sortedStreams, activeProviderStates) {
        listOf("All") + (
            sortedStreams.map { it.providerName.ifBlank { "Other" } } + activeProviderStates.keys
        ).distinct().sorted()
    }
    var provider by remember { mutableStateOf("All") }
    LaunchedEffect(providers) { if (provider !in providers) provider = "All" }
    val visible = remember(sortedStreams, provider) {
        if (provider == "All") sortedStreams
        else sortedStreams.filter { it.providerName.ifBlank { "Other" } == provider }
    }
    val sourceFocusRequesters = remember(
        visible.map { "${it.providerId}|${it.url}|${it.name}|${it.headers.hashCode()}" }
    ) { List(visible.size) { FocusRequester() } }
    val closeFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { closeFocus.requestFocus() } }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.58f)
            .background(Color(0xFF171717))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(18.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Sources", fontSize = 27.sp, fontWeight = FontWeight.Medium)
                    Text(
                        title,
                        color = AppTextMuted,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(onClick = onReload, enabled = !loading) {
                    Text(if (loading) "Loading" else "Reload")
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.focusRequester(closeFocus)
                ) { Text("Close") }
            }
            Spacer(Modifier.height(14.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(providers, key = { it }) { value ->
                    val state = activeProviderStates[value]
                    FilterChip(
                        selected = provider == value,
                        onClick = { provider = value },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(value)
                                if (state?.loading == true) {
                                    Spacer(Modifier.width(6.dp))
                                    CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                }
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(Modifier.height(10.dp))

            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(progress.ifBlank { "Checking providers..." }, color = AppTextMuted)
                }
                Spacer(Modifier.height(10.dp))
            }
            if (error.isNotBlank()) {
                Text(error, color = Color(0xFFFFB7BC), fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                contentPadding = PaddingValues(bottom = 10.dp)
            ) {
                items(
                    count = visible.size,
                    key = { index ->
                        val source = visible[index]
                        "${source.providerId}|${source.url}|${source.name}|${source.headers.hashCode()}"
                    }
                ) { index ->
                    val source = visible[index]
                    var focused by remember { mutableStateOf(false) }
                    val playing = source.url == playingSource.url
                    val playSource = { onPlay(source) }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(sourceFocusRequesters[index])
                            .onFocusChanged { focused = it.isFocused }
                            .onPreviewKeyEvent { event ->
                                if (!isTv) {
                                    false
                                } else when {
                                    consumeTvActivateKey(event, playSource) -> true
                                    event.type == KeyEventType.KeyDown &&
                                        event.key == Key.DirectionUp &&
                                        index > 0 -> requestTvFocus(sourceFocusRequesters[index - 1])
                                    event.type == KeyEventType.KeyDown &&
                                        event.key == Key.DirectionDown &&
                                        index < visible.lastIndex ->
                                        requestTvFocus(sourceFocusRequesters[index + 1])
                                    else -> false
                                }
                            }
                            .clickable(onClick = playSource)
                            .focusable(),
                        shape = RoundedCornerShape(14.dp),
                        color = if (playing || focused) Color(0xFF3A3A3A) else Color(0xFF252525),
                        border = when {
                            focused -> BorderStroke(2.dp, Color.White)
                            playing -> BorderStroke(1.dp, Color.White.copy(alpha = 0.60f))
                            else -> null
                        }
                    ) {
                        Column(Modifier.padding(13.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    source.providerName.ifBlank { "Stream" },
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.weight(1f)
                                )
                                if (playing) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White
                                    ) {
                                        Text(
                                            "Playing",
                                            color = Color.Black,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            if (source.name.isNotBlank() && source.name != source.providerName) {
                                Text(
                                    source.name,
                                    color = Color.White.copy(alpha = 0.86f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            val meta = buildList {
                                source.type.takeIf { it.isNotBlank() }?.let { add(it.uppercase()) }
                                if (source.subtitles.isNotEmpty()) add("${source.subtitles.size} subs")
                            }.joinToString("  •  ")
                            if (meta.isNotBlank()) {
                                Text(meta, color = AppTextMuted, fontSize = 11.sp)
                            }
                            if (source.note.isNotBlank()) {
                                Text(
                                    source.note,
                                    color = AppTextMuted,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerEpisodesPanel(
    title: String,
    fallbackArtwork: String,
    episodes: List<MediaEpisode>,
    currentEpisode: MediaEpisode?,
    onClose: () -> Unit,
    onSelect: (MediaEpisode) -> Unit
) {
    BackHandler(onBack = onClose)
    val isTv = rememberIsTv()
    val seasons = remember(episodes) { orderedSeasonNumbers(episodes) }
    var season by remember(currentEpisode?.id, seasons) {
        mutableStateOf(currentEpisode?.season ?: seasons.firstOrNull { it > 0 } ?: seasons.firstOrNull())
    }
    val visible = remember(episodes, season) {
        if (season == null) episodes else episodes.filter { it.season == season }
    }
    val episodeFocusRequesters = remember(visible.map { it.id }) {
        List(visible.size) { FocusRequester() }
    }
    val closeFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { closeFocus.requestFocus() } }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.60f)
            .background(Color(0xFF171717))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(18.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Episodes", fontSize = 27.sp, fontWeight = FontWeight.Medium)
                    Text(title, color = AppTextMuted, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.focusRequester(closeFocus)
                ) { Text("Close") }
            }
            if (seasons.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(seasons, key = { it }) { value ->
                        FilterChip(
                            selected = season == value,
                            onClick = { season = value },
                            label = { Text(seasonLabel(value)) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 10.dp)
            ) {
                items(
                    count = visible.size,
                    key = { index -> visible[index].id }
                ) { index ->
                    val episode = visible[index]
                    var focused by remember { mutableStateOf(false) }
                    val selected = currentEpisode?.id == episode.id
                    val selectEpisode = { onSelect(episode) }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(episodeFocusRequesters[index])
                            .onFocusChanged { focused = it.isFocused }
                            .onPreviewKeyEvent { event ->
                                if (!isTv) {
                                    false
                                } else when {
                                    consumeTvActivateKey(event, selectEpisode) -> true
                                    event.type == KeyEventType.KeyDown &&
                                        event.key == Key.DirectionUp &&
                                        index > 0 -> requestTvFocus(episodeFocusRequesters[index - 1])
                                    event.type == KeyEventType.KeyDown &&
                                        event.key == Key.DirectionDown &&
                                        index < visible.lastIndex ->
                                        requestTvFocus(episodeFocusRequesters[index + 1])
                                    else -> false
                                }
                            }
                            .clickable(onClick = selectEpisode)
                            .focusable(),
                        shape = RoundedCornerShape(15.dp),
                        color = if (selected || focused) Color(0xFF313131) else Color(0xFF242424),
                        border = when {
                            focused -> BorderStroke(2.dp, Color.White)
                            selected -> BorderStroke(1.dp, Color.White.copy(alpha = 0.56f))
                            else -> null
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(180.dp)
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                Artwork(
                                    episode.thumbnailUrl.ifBlank { fallbackArtwork },
                                    episode.title,
                                    Modifier.fillMaxSize(),
                                    ContentScale.Crop
                                )
                                val code = when {
                                    episode.season != null && episode.episode != null -> "S${episode.season}E${episode.episode}"
                                    else -> ""
                                }
                                if (code.isNotBlank()) {
                                    Surface(
                                        modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                                        shape = RoundedCornerShape(7.dp),
                                        color = Color.Black.copy(alpha = 0.72f)
                                    ) {
                                        Text(
                                            code,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(13.dp))
                            Column(Modifier.weight(1f)) {
                                val detailCode = when {
                                    episode.season != null && episode.episode != null -> "S${episode.season}E${episode.episode}"
                                    episode.episode != null -> "Episode ${episode.episode}"
                                    else -> "Episode"
                                }
                                Text(
                                    detailCode,
                                    color = AppTextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    episode.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    episode.overview.ifBlank { "No episode synopsis is available from the current metadata source." },
                                    color = AppTextMuted,
                                    fontSize = 12.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerAudioPanel(
    player: Player,
    trackRevision: Int,
    onClose: () -> Unit,
    onTrackChanged: () -> Unit
) {
    BackHandler(onBack = onClose)
    val isTv = rememberIsTv()
    val tracks = remember(player, trackRevision) {
        buildList {
            player.currentTracks.groups
                .filter { it.type == C.TRACK_TYPE_AUDIO }
                .forEach { group ->
                    repeat(group.length) { index ->
                        val format = group.getTrackFormat(index)
                        val language = friendlySubtitleLanguage(format.language.orEmpty())
                        val channel = when (format.channelCount) {
                            1 -> "Mono"
                            2 -> "Stereo"
                            6 -> "5.1"
                            8 -> "7.1"
                            in 3..32 -> "${format.channelCount}ch"
                            else -> ""
                        }
                        val codec = format.codecs
                            ?.substringBefore(',')
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?: format.sampleMimeType
                                ?.substringAfter('/')
                                ?.uppercase()
                                .orEmpty()
                        add(
                            PlayerAudioTrack(
                                group = group,
                                trackIndex = index,
                                languageLabel = language,
                                label = format.label?.takeIf { it.isNotBlank() } ?: language,
                                channelLabel = channel,
                                codecLabel = codec,
                                selected = group.isTrackSelected(index)
                            )
                        )
                    }
                }
        }
    }
    val audioFocusRequesters = remember(
        tracks.map { "${it.group.mediaTrackGroup.hashCode()}|${it.trackIndex}|${it.label}" }
    ) { List(tracks.size + 1) { FocusRequester() } }
    val closeFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { closeFocus.requestFocus() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Audio", fontSize = 28.sp, fontWeight = FontWeight.Medium)
                    Text("Choose an audio track", color = AppTextMuted, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.focusRequester(closeFocus)
                ) { Text("Close") }
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier.widthIn(max = 760.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    var focused by remember { mutableStateOf(false) }
                    val selectAuto = {
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                            .build()
                        onTrackChanged()
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(audioFocusRequesters[0])
                            .onFocusChanged { focused = it.isFocused }
                            .onPreviewKeyEvent { event ->
                                if (!isTv) {
                                    false
                                } else when {
                                    consumeTvActivateKey(event, selectAuto) -> true
                                    event.type == KeyEventType.KeyDown &&
                                        event.key == Key.DirectionDown &&
                                        tracks.isNotEmpty() -> requestTvFocus(audioFocusRequesters[1])
                                    else -> false
                                }
                            }
                            .clickable(onClick = selectAuto)
                            .focusable(),
                        shape = RoundedCornerShape(14.dp),
                        color = if (focused) Color(0xFFF4F4F4) else Color(0xFF242424),
                        border = if (focused) BorderStroke(2.dp, Color.White) else null
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Auto", color = if (focused) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                            Text(
                                "Use the stream's preferred audio track.",
                                color = if (focused) Color.Black.copy(alpha = 0.65f) else AppTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                items(
                    count = tracks.size,
                    key = { index ->
                        val track = tracks[index]
                        "${track.group.mediaTrackGroup.hashCode()}|${track.trackIndex}|${track.label}"
                    }
                ) { index ->
                    val track = tracks[index]
                    var focused by remember { mutableStateOf(false) }
                    val selectTrack = {
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                            .addOverride(
                                TrackSelectionOverride(
                                    track.group.mediaTrackGroup,
                                    track.trackIndex
                                )
                            )
                            .build()
                        onTrackChanged()
                    }
                    val requesterIndex = index + 1
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(audioFocusRequesters[requesterIndex])
                            .onFocusChanged { focused = it.isFocused }
                            .onPreviewKeyEvent { event ->
                                if (!isTv) {
                                    false
                                } else when {
                                    consumeTvActivateKey(event, selectTrack) -> true
                                    event.type == KeyEventType.KeyDown &&
                                        event.key == Key.DirectionUp ->
                                        requestTvFocus(audioFocusRequesters[requesterIndex - 1])
                                    event.type == KeyEventType.KeyDown &&
                                        event.key == Key.DirectionDown &&
                                        requesterIndex < audioFocusRequesters.lastIndex ->
                                        requestTvFocus(audioFocusRequesters[requesterIndex + 1])
                                    else -> false
                                }
                            }
                            .clickable(onClick = selectTrack)
                            .focusable(),
                        shape = RoundedCornerShape(14.dp),
                        color = if (track.selected || focused) Color(0xFFF4F4F4) else Color(0xFF242424),
                        border = if (focused) BorderStroke(2.dp, Color.White) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    track.label,
                                    color = if (track.selected || focused) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                val detail = listOf(track.languageLabel, track.channelLabel, track.codecLabel)
                                    .filter { it.isNotBlank() }
                                    .distinct()
                                    .joinToString(" · ")
                                if (detail.isNotBlank()) {
                                    Text(
                                        detail,
                                        color = if (track.selected || focused) Color.Black.copy(alpha = 0.65f) else AppTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            if (track.selected) {
                                Text("✓", color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerSubtitlesPanel(
    player: Player,
    trackRevision: Int,
    fontSizeSp: Int,
    bold: Boolean,
    preferredLanguage: String,
    secondPreferredLanguage: String,
    displayMode: SubtitleDisplayMode,
    onFontSize: (Int) -> Unit,
    onBold: (Boolean) -> Unit,
    onClose: () -> Unit,
    onTrackChanged: () -> Unit
) {
    BackHandler(onBack = onClose)
    val tracks = remember(player, trackRevision) {
        buildList {
            player.currentTracks.groups
                .filter { it.type == C.TRACK_TYPE_TEXT }
                .forEach { group ->
                    repeat(group.length) { index ->
                        val format = group.getTrackFormat(index)
                        val code = format.language.orEmpty()
                        val rawLabel = format.label
                            ?.takeIf { it.isNotBlank() }
                            ?: friendlySubtitleLanguage(code)
                        val (providerName, displayLabel) = splitSubtitleProviderLabel(rawLabel)
                        add(
                            PlayerSubtitleTrack(
                                group = group,
                                trackIndex = index,
                                languageCode = code,
                                languageLabel = friendlySubtitleLanguage(code),
                                providerName = providerName,
                                label = displayLabel,
                                selected = group.isTrackSelected(index)
                            )
                        )
                    }
                }
        }
    }
    val preferredTracks = remember(tracks, preferredLanguage, secondPreferredLanguage, displayMode) {
        if (displayMode == SubtitleDisplayMode.PREFERRED_ONLY) {
            tracks.filter { track ->
                subtitleLanguageMatches(track.languageCode, track.languageLabel, preferredLanguage) ||
                    subtitleLanguageMatches(track.languageCode, track.languageLabel, secondPreferredLanguage)
            }
        } else {
            tracks
        }
    }
    val languages = remember(preferredTracks, preferredLanguage, secondPreferredLanguage) {
        preferredTracks.groupBy { it.languageLabel }
            .toList()
            .sortedWith(
                compareBy<Pair<String, List<PlayerSubtitleTrack>>> { entry ->
                    val sample = entry.second.firstOrNull()
                    when {
                        sample != null && subtitleLanguageMatches(sample.languageCode, sample.languageLabel, preferredLanguage) -> 0
                        sample != null && subtitleLanguageMatches(sample.languageCode, sample.languageLabel, secondPreferredLanguage) -> 1
                        else -> 2
                    }
                }.thenBy { it.first.lowercase() }
            )
    }
    val activeLanguage = tracks.firstOrNull { it.selected }?.languageLabel
    var selectedLanguage by remember(trackRevision, activeLanguage, languages) {
        mutableStateOf(activeLanguage ?: languages.firstOrNull()?.first ?: "None")
    }
    val visibleTracks = remember(preferredTracks, selectedLanguage) {
        preferredTracks.filter { it.languageLabel == selectedLanguage }
    }
    val isTv = rememberIsTv()
    val visibleTrackRequesters = remember(
        visibleTracks.map { "${it.group.mediaTrackGroup.hashCode()}|${it.trackIndex}|${it.label}" }
    ) {
        List(visibleTracks.size) { FocusRequester() }
    }
    fun selectSubtitleTrack(track: PlayerSubtitleTrack) {
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .addOverride(
                TrackSelectionOverride(
                    track.group.mediaTrackGroup,
                    track.trackIndex
                )
            )
            .build()
        onTrackChanged()
    }
    val closeFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { closeFocus.requestFocus() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Subtitles", fontSize = 28.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.focusRequester(closeFocus)
                ) { Text("Close") }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column(Modifier.weight(0.27f).fillMaxHeight()) {
                    Text("Languages", color = AppTextMuted, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    if (displayMode == SubtitleDisplayMode.PREFERRED_ONLY && languages.isEmpty()) {
                        Text(
                            "No preferred subtitles found for ${subtitleLanguageLabel(preferredLanguage)} or ${subtitleLanguageLabel(secondPreferredLanguage)}.",
                            color = AppTextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        item {
                            SubtitleLanguageRow(
                                label = "None",
                                count = 0,
                                selected = tracks.none { it.selected },
                                onClick = {
                                    player.trackSelectionParameters = player.trackSelectionParameters
                                        .buildUpon()
                                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                        .build()
                                    selectedLanguage = "None"
                                    onTrackChanged()
                                }
                            )
                        }
                        items(languages, key = { it.first }) { entry ->
                            SubtitleLanguageRow(
                                label = entry.first,
                                count = entry.second.size,
                                selected = selectedLanguage == entry.first,
                                onClick = { selectedLanguage = entry.first }
                            )
                        }
                    }
                }

                Column(Modifier.weight(0.40f).fillMaxHeight()) {
                    Text("Subtitles", color = AppTextMuted, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    if (selectedLanguage == "None") {
                        Text(
                            "Subtitles are disabled.",
                            color = AppTextMuted,
                            modifier = Modifier.padding(vertical = 14.dp)
                        )
                    } else if (visibleTracks.isEmpty()) {
                        Text(
                            if (displayMode == SubtitleDisplayMode.PREFERRED_ONLY) {
                                "No preferred subtitles found for ${subtitleLanguageLabel(preferredLanguage)} or ${subtitleLanguageLabel(secondPreferredLanguage)}."
                            } else {
                                "No subtitle tracks for this language."
                            },
                            color = AppTextMuted,
                            modifier = Modifier.padding(vertical = 14.dp)
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(
                                count = visibleTracks.size,
                                key = { index ->
                                    val track = visibleTracks[index]
                                    "${track.group.mediaTrackGroup.hashCode()}|${track.trackIndex}|${track.label}"
                                }
                            ) { index ->
                                val track = visibleTracks[index]
                                var focused by remember { mutableStateOf(false) }
                                val selectTrack = { selectSubtitleTrack(track) }
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(visibleTrackRequesters[index])
                                        .onFocusChanged { focused = it.isFocused }
                                        .onPreviewKeyEvent { event ->
                                            if (!isTv) {
                                                false
                                            } else when {
                                                consumeTvActivateKey(event, selectTrack) -> true
                                                event.type == KeyEventType.KeyDown &&
                                                    event.key == Key.DirectionUp &&
                                                    index > 0 -> requestTvFocus(visibleTrackRequesters[index - 1])
                                                event.type == KeyEventType.KeyDown &&
                                                    event.key == Key.DirectionDown &&
                                                    index < visibleTracks.lastIndex ->
                                                    requestTvFocus(visibleTrackRequesters[index + 1])
                                                else -> false
                                            }
                                        }
                                        .clickable(onClick = selectTrack)
                                        .focusable(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (track.selected || focused) Color(0xFFF4F4F4) else Color(0xFF242424),
                                    border = if (focused) BorderStroke(2.dp, Color.White) else null
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                track.providerName,
                                                color = if (track.selected || focused) Color.Black else Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                track.label,
                                                color = if (track.selected || focused) Color.Black.copy(alpha = 0.82f) else Color.White.copy(alpha = 0.86f),
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                track.languageLabel,
                                                color = if (track.selected || focused) Color.Black.copy(alpha = 0.66f) else AppTextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                        if (track.selected) {
                                            Text("✓", color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Column(Modifier.weight(0.33f).fillMaxHeight()) {
                    Text("Style", color = AppTextMuted, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    Text("Font Size", fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onFontSize((fontSizeSp - 1).coerceAtLeast(12)) },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) { Text("−", fontSize = 20.sp) }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF242424)
                        ) {
                            Text(
                                "${fontSizeSp}sp",
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                            )
                        }
                        OutlinedButton(
                            onClick = { onFontSize((fontSizeSp + 1).coerceAtMost(34)) },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) { Text("+", fontSize = 20.sp) }
                    }
                    Spacer(Modifier.height(18.dp))
                    FilterChip(
                        selected = bold,
                        onClick = { onBold(!bold) },
                        label = { Text("Bold") }
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Track selection applies immediately.",
                        color = AppTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SubtitleLanguageRow(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val isTv = rememberIsTv()
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (isTv) consumeTvActivateKey(event, onClick) else false
            }
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(12.dp),
        color = if (selected || focused) Color(0xFFF4F4F4) else Color.Transparent,
        border = if (focused && !selected) BorderStroke(1.dp, Color.White) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = if (selected || focused) Color.Black else Color.White,
                modifier = Modifier.weight(1f)
            )
            if (count > 0) {
                Surface(
                    shape = CircleShape,
                    color = if (selected || focused) Color.Black.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.12f)
                ) {
                    Text(
                        count.toString(),
                        color = if (selected || focused) Color.Black else Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

private fun MediaItem.nextEpisodeAfter(current: MediaEpisode?): MediaEpisode? {
    if (current == null || episodes.isEmpty()) return null
    val index = episodes.indexOfFirst { it.id == current.id }
    return if (index >= 0) episodes.getOrNull(index + 1) else null
}

private fun LocalMediaEntry.matchesMedia(sourceUrl: String, item: MediaItem): Boolean {
    if (this.sourceUrl != sourceUrl) return false
    val metaId = item.ref?.metaId.orEmpty()
    return if (metaId.isNotBlank()) {
        refMetaId == metaId
    } else {
        mediaId == item.id
    }
}

private fun formatPlaybackTime(positionMs: Long): String {
    val totalSeconds = positionMs.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
