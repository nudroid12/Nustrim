package app.nudroidlabs.nustrim.tv.focus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged

private data class TvFocusAddress(
    val scopeKey: String,
    val anchorKey: String,
)

private data class TvHomeFocusPosition(
    val rowKey: String,
    val rowIndex: Int,
    val itemIndex: Int,
)

@Stable
class TvFocusRegistry {
    private val requesters = mutableMapOf<TvFocusAddress, FocusRequester>()
    private val lastFocusedAnchor = mutableStateMapOf<String, String>()
    private val homePositions = mutableMapOf<String, TvHomeFocusPosition>()
    private val homeRowItemPositions = mutableMapOf<Pair<String, String>, Int>()

    fun register(scopeKey: String, anchorKey: String, requester: FocusRequester) {
        requesters[TvFocusAddress(scopeKey, anchorKey)] = requester
    }

    fun unregister(scopeKey: String, anchorKey: String, requester: FocusRequester) {
        val address = TvFocusAddress(scopeKey, anchorKey)
        if (requesters[address] === requester) requesters.remove(address)
    }

    fun rememberFocused(scopeKey: String, anchorKey: String) {
        lastFocusedAnchor[scopeKey] = anchorKey
    }

    fun lastFocused(scopeKey: String): String? = lastFocusedAnchor[scopeKey]

    fun rememberHomePosition(
        scopeKey: String,
        rowKey: String,
        rowIndex: Int,
        itemIndex: Int,
    ) {
        homePositions[scopeKey] = TvHomeFocusPosition(rowKey, rowIndex, itemIndex)
        homeRowItemPositions[scopeKey to rowKey] = itemIndex
    }

    fun homeRowIndex(scopeKey: String): Int = homePositions[scopeKey]?.rowIndex ?: 0

    fun rowItemIndex(scopeKey: String, rowKey: String): Int =
        homeRowItemPositions[scopeKey to rowKey] ?: 0

    fun requestAnchor(scopeKey: String, anchorKey: String): Boolean {
        val requester = requesters[TvFocusAddress(scopeKey, anchorKey)] ?: return false
        return runCatching {
            requester.requestFocus()
            true
        }.getOrDefault(false)
    }

    fun requestFocus(scopeKey: String, fallbackAnchorKey: String? = null): Boolean {
        val remembered = lastFocusedAnchor[scopeKey]
        val preferred = remembered?.let { requesters[TvFocusAddress(scopeKey, it)] }
        val fallback = fallbackAnchorKey?.let { requesters[TvFocusAddress(scopeKey, it)] }
        val requester = preferred ?: fallback ?: return false
        return runCatching {
            requester.requestFocus()
            true
        }.getOrDefault(false)
    }
}

@Composable
fun rememberTvFocusRegistry(): TvFocusRegistry = remember { TvFocusRegistry() }

@Stable
class TvFocusAnchor internal constructor(
    internal val requester: FocusRequester,
    internal val onFocused: () -> Unit,
)

@Composable
fun rememberTvFocusAnchor(
    registry: TvFocusRegistry,
    scopeKey: String,
    anchorKey: String,
): TvFocusAnchor {
    val requester = remember(scopeKey, anchorKey) { FocusRequester() }

    DisposableEffect(registry, scopeKey, anchorKey, requester) {
        registry.register(scopeKey, anchorKey, requester)
        onDispose { registry.unregister(scopeKey, anchorKey, requester) }
    }

    return remember(registry, scopeKey, anchorKey, requester) {
        TvFocusAnchor(
            requester = requester,
            onFocused = { registry.rememberFocused(scopeKey, anchorKey) },
        )
    }
}

fun Modifier.tvFocusAnchor(anchor: TvFocusAnchor): Modifier =
    focusRequester(anchor.requester)
        .onFocusChanged { state -> if (state.isFocused) anchor.onFocused() }
