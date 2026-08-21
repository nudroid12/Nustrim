package app.nudroidlabs.nustrim.tv.player

import androidx.annotation.OptIn
import androidx.media3.common.TrackGroup
import androidx.media3.common.util.UnstableApi

enum class TvPlayerPanel {
    EPISODES,
    SOURCES,
    AUDIO,
    SUBTITLES,
    SPEED,
}

@OptIn(UnstableApi::class)
enum class TvPlayerAspectMode(
    val label: String,
    val resizeMode: Int,
) {
    FIT("Fit", androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT),
    ZOOM("Zoom", androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    FILL("Fill", androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL),
}

data class TvPlayerTrack(
    val key: String,
    val label: String,
    val language: String,
    val selected: Boolean,
    val group: TrackGroup,
    val trackIndex: Int,
)
