package app.nudroidlabs.nustrim.tv2.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Tv2Destination(
    val id: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Outlined.Home),
    SEARCH("search", "Search", Icons.Outlined.Search),
    LIBRARY("library", "Library", Icons.Outlined.LibraryAdd),
    SETTINGS("settings", "Settings", Icons.Outlined.Settings);

    companion object {
        fun fromId(id: String): Tv2Destination =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: HOME
    }
}
