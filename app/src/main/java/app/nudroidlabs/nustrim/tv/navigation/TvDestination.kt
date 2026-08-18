package app.nudroidlabs.nustrim.tv.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class TvDestination(
    val label: String,
    val icon: ImageVector
) {
    HOME("Home", Icons.Outlined.Home),
    SEARCH("Search", Icons.Outlined.Search),
    LIBRARY("Library", Icons.Outlined.LibraryAdd),
    SETTINGS("Settings", Icons.Outlined.Settings)
}
