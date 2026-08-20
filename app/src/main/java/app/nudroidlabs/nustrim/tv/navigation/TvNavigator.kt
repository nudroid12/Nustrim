package app.nudroidlabs.nustrim.tv.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Stable
class TvNavigator internal constructor(
    startDestination: TvRootDestination,
) {
    private val backStack = mutableStateListOf<TvRoute>(TvRoute.Root(startDestination))

    var activeRoot by mutableStateOf(startDestination)
        private set

    val currentRoute: TvRoute
        get() = backStack.last()

    val canPop: Boolean
        get() = backStack.size > 1

    fun navigateRoot(destination: TvRootDestination) {
        activeRoot = destination
        backStack.clear()
        backStack += TvRoute.Root(destination)
    }

    fun push(route: TvRoute) {
        if (route is TvRoute.Root) {
            navigateRoot(route.destination)
            return
        }
        if (backStack.lastOrNull() == route) return
        backStack += route
    }

    fun replace(route: TvRoute) {
        if (backStack.isNotEmpty()) {
            backStack.removeAt(backStack.lastIndex)
        }
        if (route is TvRoute.Root) {
            activeRoot = route.destination
        }
        backStack += route
    }

    fun pop(): Boolean {
        if (!canPop) return false
        backStack.removeAt(backStack.lastIndex)
        val root = backStack.firstOrNull() as? TvRoute.Root
        if (root != null) activeRoot = root.destination
        return true
    }

    fun popToRoot() {
        val root = backStack.firstOrNull() as? TvRoute.Root ?: TvRoute.Root(activeRoot)
        backStack.clear()
        backStack += root
        activeRoot = root.destination
    }
}

@Composable
fun rememberTvNavigator(
    startDestination: TvRootDestination = TvRootDestination.HOME,
): TvNavigator = remember(startDestination) {
    TvNavigator(startDestination)
}
