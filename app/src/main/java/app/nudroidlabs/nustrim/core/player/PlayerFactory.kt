package app.nudroidlabs.nustrim.core.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import app.nudroidlabs.nustrim.BuildConfig
import app.nudroidlabs.nustrim.core.diagnostics.NustrimDiagnostics
import app.nudroidlabs.nustrim.core.model.StreamSource
import app.nudroidlabs.nustrim.core.model.SubtitleSource

@androidx.annotation.OptIn(UnstableApi::class)
object PlayerFactory {
    fun create(
        context: Context,
        title: String,
        source: StreamSource,
        startPositionMs: Long = 0L
    ): ExoPlayer {
        require(source.playable && source.url.isNotBlank()) {
            "This stream is not directly playable"
        }

        NustrimDiagnostics.log(
            "PLAYER_CREATE",
            "title=$title provider=${source.providerName} url=${source.url} " +
                "resumeMs=$startPositionMs subtitles=${source.subtitles.size} " +
                "headers=${NustrimDiagnostics.headers(source.headers)}"
        )

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Nustrim/${BuildConfig.VERSION_NAME}")
            .setAllowCrossProtocolRedirects(true)

        if (source.headers.isNotEmpty()) {
            httpFactory.setDefaultRequestProperties(source.headers)
        }

        val mediaSourceFactory = DefaultMediaSourceFactory(httpFactory)
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        val state = when (playbackState) {
                            Player.STATE_IDLE -> "IDLE"
                            Player.STATE_BUFFERING -> "BUFFERING"
                            Player.STATE_READY -> "READY"
                            Player.STATE_ENDED -> "ENDED"
                            else -> playbackState.toString()
                        }
                        NustrimDiagnostics.log("PLAYER_STATE", state)
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        NustrimDiagnostics.log("PLAYER_PLAYING", isPlaying.toString())
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        NustrimDiagnostics.error(
                            "PLAYER_ERROR",
                            error,
                            "code=${error.errorCode} name=${error.errorCodeName}"
                        )
                    }
                })

                val mediaItem = MediaItem.Builder()
                    .setUri(source.url)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(title)
                            .build()
                    )
                    .setSubtitleConfigurations(source.subtitles.map { subtitle -> subtitleConfiguration(subtitle, source.providerName) })
                    .build()

                setMediaItem(mediaItem)
                if (startPositionMs > 0L) seekTo(startPositionMs)
                prepare()
                playWhenReady = true
            }
    }

    private fun subtitleConfiguration(
        subtitle: SubtitleSource,
        fallbackProviderName: String
    ): MediaItem.SubtitleConfiguration {
        val baseLabel = subtitle.label.ifBlank { subtitle.language.ifBlank { "Subtitle" } }
        val providerName = fallbackProviderName.ifBlank { "Embedded" }
        val label = if (baseLabel.contains(PROVIDER_SEPARATOR)) {
            baseLabel
        } else {
            "$providerName$PROVIDER_SEPARATOR$baseLabel"
        }
        val builder = MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitle.url))
            .setMimeType(subtitleMimeType(subtitle.url))
            .setLabel(label)
        subtitle.language.takeIf { it.isNotBlank() }?.let(builder::setLanguage)
        return builder.build()
    }

    private const val PROVIDER_SEPARATOR = "|||NUSTRIM_PROVIDER|||"

    private fun subtitleMimeType(url: String): String {
        val clean = url.substringBefore('?').lowercase()
        return when {
            clean.endsWith(".srt") -> MimeTypes.APPLICATION_SUBRIP
            clean.endsWith(".ssa") || clean.endsWith(".ass") -> MimeTypes.TEXT_SSA
            clean.endsWith(".ttml") || clean.endsWith(".xml") -> MimeTypes.APPLICATION_TTML
            else -> MimeTypes.TEXT_VTT
        }
    }
}
