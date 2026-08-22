package app.nudroidlabs.nustrim.tv.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import java.util.Locale
import app.nudroidlabs.nustrim.core.player.PlayerFactory

@OptIn(UnstableApi::class)
class TvPlayerRuntime(
    context: Context,
    request: TvPlaybackRequest,
    startPositionMs: Long,
    preferredSubtitleLanguage: String = "",
) {
    val player: ExoPlayer = PlayerFactory.create(
        context = context.applicationContext,
        title = request.playerTitle,
        source = request.stream,
        startPositionMs = startPositionMs.coerceAtLeast(0L),
    )

    var isPlaying by mutableStateOf(false)
        private set
    var isBuffering by mutableStateOf(true)
        private set
    var playbackState by mutableStateOf(Player.STATE_IDLE)
        private set
    var positionMs by mutableStateOf(startPositionMs.coerceAtLeast(0L))
        private set
    var durationMs by mutableStateOf(0L)
        private set
    var bufferedPositionMs by mutableStateOf(0L)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var ended by mutableStateOf(false)
        private set
    var playbackSpeed by mutableStateOf(1f)
        private set
    var audioTracks by mutableStateOf(emptyList<TvPlayerTrack>())
        private set
    var subtitleTracks by mutableStateOf(emptyList<TvPlayerTrack>())
        private set

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            playbackState = state
            isBuffering = state == Player.STATE_BUFFERING
            ended = state == Player.STATE_ENDED
            syncTimeline()
        }

        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = playing
            syncTimeline()
        }

        override fun onPlayerError(error: PlaybackException) {
            errorMessage = error.message.orEmpty().ifBlank { error.errorCodeName }
            isBuffering = false
        }

        override fun onTracksChanged(tracks: Tracks) {
            refreshTracks(tracks)
        }
    }

    init {
        canonicalLanguageCode(preferredSubtitleLanguage)
            .takeIf { it.isNotBlank() }
            ?.let { language ->
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setPreferredTextLanguage(language)
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .build()
            }
        player.addListener(listener)
        isPlaying = player.isPlaying
        playbackState = player.playbackState
        isBuffering = player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_IDLE
        refreshTracks(player.currentTracks)
        syncTimeline()
    }

    val readyOrEnded: Boolean
        get() = playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED

    fun syncTimeline() {
        positionMs = player.currentPosition.coerceAtLeast(0L)
        durationMs = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
        bufferedPositionMs = player.bufferedPosition.coerceAtLeast(positionMs)
        playbackSpeed = player.playbackParameters.speed
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun play() {
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun seekBy(deltaMs: Long) {
        seekTo(positionMs + deltaMs)
    }

    fun seekTo(targetMs: Long) {
        val max = durationMs.takeIf { it > 0L } ?: Long.MAX_VALUE
        player.seekTo(targetMs.coerceIn(0L, max))
        syncTimeline()
    }

    fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed.coerceIn(0.25f, 3f))
        playbackSpeed = player.playbackParameters.speed
    }

    fun selectAudio(track: TvPlayerTrack) {
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            .setOverrideForType(TrackSelectionOverride(track.group, track.trackIndex))
            .build()
    }

    fun selectSubtitle(track: TvPlayerTrack?) {
        val builder = player.trackSelectionParameters.buildUpon()
        if (track == null) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        } else {
            builder
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(TrackSelectionOverride(track.group, track.trackIndex))
        }
        player.trackSelectionParameters = builder.build()
    }

    fun release() {
        player.removeListener(listener)
        player.release()
    }

    private fun refreshTracks(tracks: Tracks) {
        audioTracks = collectTracks(tracks, C.TRACK_TYPE_AUDIO, "Audio")
        subtitleTracks = collectTracks(tracks, C.TRACK_TYPE_TEXT, "Subtitle")
    }

    private fun collectTracks(
        tracks: Tracks,
        type: Int,
        fallback: String,
    ): List<TvPlayerTrack> {
        var ordinal = 0
        return buildList {
            tracks.groups
                .filter { it.type == type }
                .forEach { group ->
                    for (index in 0 until group.length) {
                        if (!group.isTrackSupported(index, true)) continue
                        ordinal += 1
                        val format = group.getTrackFormat(index)
                        val languageCode = canonicalLanguageCode(format.language.orEmpty())
                        val language = displayLanguageName(languageCode)
                        val rawLabel = format.label.orEmpty()
                            .replace(PROVIDER_SEPARATOR, " · ")
                            .trim()
                        val label = cleanTrackLabel(
                            rawLabel = rawLabel,
                            languageCode = languageCode,
                            languageLabel = language,
                            fallback = fallback,
                            ordinal = ordinal,
                        )
                        add(
                            TvPlayerTrack(
                                key = buildString {
                                    append(type)
                                    append(':')
                                    append(group.mediaTrackGroup.hashCode().toString(16))
                                    append(':')
                                    append(index)
                                },
                                label = label,
                                language = language,
                                selected = group.isTrackSelected(index),
                                group = group.mediaTrackGroup,
                                trackIndex = index,
                            ),
                        )
                    }
                }
        }
    }

    private fun canonicalLanguageCode(raw: String): String {
        val base = raw.trim().lowercase(Locale.ROOT).substringBefore('-').substringBefore('_')
        return when (base) {
            "", "und", "unknown", "mul", "zxx" -> ""
            "eng" -> "en"
            "msa", "may" -> "ms"
            "ind" -> "id"
            "spa" -> "es"
            "por" -> "pt"
            "fra", "fre" -> "fr"
            "deu", "ger" -> "de"
            "ita" -> "it"
            "jpn" -> "ja"
            "kor" -> "ko"
            "zho", "chi" -> "zh"
            "ara" -> "ar"
            "tha" -> "th"
            "vie" -> "vi"
            "rus" -> "ru"
            "hin" -> "hi"
            else -> base.takeIf { it.length in 2..3 }.orEmpty()
        }
    }

    private fun displayLanguageName(code: String): String {
        if (code.isBlank()) return ""
        val display = Locale.forLanguageTag(code).getDisplayLanguage(Locale.ENGLISH).trim()
        return display
            .takeIf { it.isNotBlank() && !it.equals(code, ignoreCase = true) }
            ?: code.uppercase(Locale.ROOT)
    }

    private fun cleanTrackLabel(
        rawLabel: String,
        languageCode: String,
        languageLabel: String,
        fallback: String,
        ordinal: Int,
    ): String {
        val normalizedRaw = rawLabel.trim()
        val rawKey = normalizedRaw.lowercase(Locale.ROOT)
        val languageKey = languageLabel.lowercase(Locale.ROOT)
        val codeKey = languageCode.lowercase(Locale.ROOT)
        val genericLanguageLabel = normalizedRaw.isBlank() ||
            rawKey == languageKey ||
            rawKey == codeKey ||
            rawKey == "unknown" ||
            rawKey == "und"

        if (genericLanguageLabel) {
            return languageLabel.ifBlank { "$fallback $ordinal" }
        }
        return normalizedRaw
    }

    private companion object {
        const val PROVIDER_SEPARATOR = "|||NUSTRIM_PROVIDER|||"
    }
}
