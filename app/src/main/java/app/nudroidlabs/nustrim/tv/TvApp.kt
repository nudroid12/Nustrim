package app.nudroidlabs.nustrim.tv

import androidx.compose.runtime.Composable
import app.nudroidlabs.nustrim.tv.shell.TvShell
import app.nudroidlabs.nustrim.tv.theme.NustrimTvTheme

@Composable
fun TvApp(
    onExit: () -> Unit
) {
    NustrimTvTheme {
        TvShell(onExit = onExit)
    }
}
