package app.nudroidlabs.nustrim.tv.player

import android.content.Context
import app.nudroidlabs.nustrim.core.model.SubtitleSource
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object TvSubtitleShiftManager {
    suspend fun shift(
        context: Context,
        subtitles: List<SubtitleSource>,
        offsetMs: Long
    ): List<SubtitleSource> = withContext(Dispatchers.IO) {
        if (offsetMs == 0L || subtitles.isEmpty()) {
            return@withContext subtitles
        }

        val cacheDir = File(context.cacheDir, "subtitle-shift").apply { mkdirs() }
        subtitles.map { subtitle ->
            shiftOne(cacheDir, subtitle, offsetMs) ?: subtitle
        }
    }

    private fun shiftOne(
        cacheDir: File,
        subtitle: SubtitleSource,
        offsetMs: Long
    ): SubtitleSource? {
        val sourceUrl = subtitle.url.trim()
        if (
            !sourceUrl.startsWith("http://", ignoreCase = true) &&
            !sourceUrl.startsWith("https://", ignoreCase = true)
        ) {
            return null
        }

        val format = detectFormat(sourceUrl) ?: return null
        val key = sha256("$sourceUrl|$offsetMs|${subtitle.language}|${subtitle.label}")
        val file = File(cacheDir, "$key.${format.extension}")
        if (!file.exists() || file.length() == 0L) {
            val raw = download(subtitle) ?: return null
            val shifted = when (format) {
                SubtitleTextFormat.SRT -> shiftSrt(raw, offsetMs)
                SubtitleTextFormat.VTT -> shiftVtt(raw, offsetMs)
                SubtitleTextFormat.ASS -> shiftAss(raw, offsetMs)
            }
            file.writeText(shifted, Charsets.UTF_8)
        }

        return subtitle.copy(
            url = file.toURI().toString(),
            headers = emptyMap()
        )
    }

    private fun download(subtitle: SubtitleSource): String? {
        val connection = runCatching {
            URL(subtitle.url).openConnection() as HttpURLConnection
        }.getOrNull() ?: return null

        return try {
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 8_000
            connection.readTimeout = 12_000
            connection.setRequestProperty("Accept", "text/plain,text/vtt,*/*")
            connection.setRequestProperty("User-Agent", "Nustrim/TV")
            subtitle.headers.forEach { (name, value) ->
                if (name.isNotBlank() && value.isNotBlank()) {
                    connection.setRequestProperty(name, value)
                }
            }
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: Throwable) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun shiftSrt(raw: String, offsetMs: Long): String =
        SRT_RANGE.replace(raw) { match ->
            val start = shiftTimestamp(match.groupValues[1], offsetMs, ',')
            val end = shiftTimestamp(match.groupValues[2], offsetMs, ',')
            "$start --> $end"
        }

    private fun shiftVtt(raw: String, offsetMs: Long): String =
        VTT_RANGE.replace(raw) { match ->
            val start = shiftTimestamp(match.groupValues[1], offsetMs, '.')
            val end = shiftTimestamp(match.groupValues[2], offsetMs, '.')
            val settings = match.groupValues[3]
            "$start --> $end$settings"
        }

    private fun shiftAss(raw: String, offsetMs: Long): String =
        ASS_DIALOGUE.replace(raw) { match ->
            val prefix = match.groupValues[1]
            val start = shiftAssTimestamp(match.groupValues[2], offsetMs)
            val end = shiftAssTimestamp(match.groupValues[3], offsetMs)
            val suffix = match.groupValues[4]
            "$prefix$start,$end$suffix"
        }

    private fun shiftAssTimestamp(value: String, offsetMs: Long): String {
        val parts = value.split(':')
        if (parts.size != 3) return value
        val hours = parts[0].toLongOrNull() ?: return value
        val minutes = parts[1].toLongOrNull() ?: return value
        val secParts = parts[2].split('.')
        if (secParts.size != 2) return value
        val seconds = secParts[0].toLongOrNull() ?: return value
        val centiseconds = secParts[1].take(2).padEnd(2, '0').toLongOrNull() ?: return value
        val total = (
            hours * 3_600_000L +
                minutes * 60_000L +
                seconds * 1_000L +
                centiseconds * 10L +
                offsetMs
            ).coerceAtLeast(0L)
        val outHours = total / 3_600_000L
        val outMinutes = (total % 3_600_000L) / 60_000L
        val outSeconds = (total % 60_000L) / 1_000L
        val outCentiseconds = (total % 1_000L) / 10L
        return "%d:%02d:%02d.%02d".format(
            outHours,
            outMinutes,
            outSeconds,
            outCentiseconds
        )
    }

    private fun shiftTimestamp(
        value: String,
        offsetMs: Long,
        separator: Char
    ): String {
        val parts = value.split(separator)
        if (parts.size != 2) return value
        val clock = parts[0].split(':')
        if (clock.size !in 2..3) return value

        val hours: Long
        val minutes: Long
        val seconds: Long
        if (clock.size == 3) {
            hours = clock[0].toLongOrNull() ?: return value
            minutes = clock[1].toLongOrNull() ?: return value
            seconds = clock[2].toLongOrNull() ?: return value
        } else {
            hours = 0L
            minutes = clock[0].toLongOrNull() ?: return value
            seconds = clock[1].toLongOrNull() ?: return value
        }
        val millis = parts[1].take(3).padEnd(3, '0').toLongOrNull() ?: return value
        val total = (
            hours * 3_600_000L +
                minutes * 60_000L +
                seconds * 1_000L +
                millis +
                offsetMs
            ).coerceAtLeast(0L)

        val outHours = total / 3_600_000L
        val outMinutes = (total % 3_600_000L) / 60_000L
        val outSeconds = (total % 60_000L) / 1_000L
        val outMillis = total % 1_000L

        return if (separator == ',' || outHours > 0L || clock.size == 3) {
            "%02d:%02d:%02d%c%03d".format(
                outHours,
                outMinutes,
                outSeconds,
                separator,
                outMillis
            )
        } else {
            "%02d:%02d%c%03d".format(
                outMinutes,
                outSeconds,
                separator,
                outMillis
            )
        }
    }

    private fun detectFormat(url: String): SubtitleTextFormat? {
        val lower = url.substringBefore('?').lowercase()
        return when {
            lower.endsWith(".srt") -> SubtitleTextFormat.SRT
            lower.endsWith(".vtt") || lower.endsWith(".webvtt") -> SubtitleTextFormat.VTT
            lower.endsWith(".ass") || lower.endsWith(".ssa") -> SubtitleTextFormat.ASS
            else -> null
        }
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private enum class SubtitleTextFormat(val extension: String) {
        SRT("srt"),
        VTT("vtt"),
        ASS("ass")
    }

    private val SRT_RANGE = Regex(
        """(?m)(\d{2}:\d{2}:\d{2},\d{3})\s*-->\s*(\d{2}:\d{2}:\d{2},\d{3})"""
    )
    private val VTT_RANGE = Regex(
        """(?m)((?:\d{2}:)?\d{2}:\d{2}\.\d{3})\s*-->\s*((?:\d{2}:)?\d{2}:\d{2}\.\d{3})([^\r\n]*)"""
    )
    private val ASS_DIALOGUE = Regex(
        """(?m)^(Dialogue:\s*[^,]*,)(\d+:\d{2}:\d{2}\.\d{2}),(\d+:\d{2}:\d{2}\.\d{2})(,.*)$"""
    )
}
