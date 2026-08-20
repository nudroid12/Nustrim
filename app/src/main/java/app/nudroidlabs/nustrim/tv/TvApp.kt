package app.nudroidlabs.nustrim.tv

import androidx.compose.runtime.Composable
import app.nudroidlabs.nustrim.tv.focus.rememberTvFocusRegistry
import app.nudroidlabs.nustrim.tv.navigation.rememberTvNavigator
import app.nudroidlabs.nustrim.tv.shell.TvShell
import app.nudroidlabs.nustrim.tv.theme.NustrimTvTheme

@Composable
fun TvApp(onExit: () -> Unit) {
    val navigator = rememberTvNavigator()
    val focusRegistry = rememberTvFocusRegistry()

    NustrimTvTheme {
        TvShell(
            navigator = navigator,
            focusRegistry = focusRegistry,
            onExit = onExit,
        )
    }
}
