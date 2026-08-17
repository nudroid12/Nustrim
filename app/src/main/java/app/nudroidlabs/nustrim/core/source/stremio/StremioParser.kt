package app.nudroidlabs.nustrim.core.source.stremio

import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.MediaRef
import app.nudroidlabs.nustrim.core.model.MediaType
import app.nudroidlabs.nustrim.core.model.StreamSource
import app.nudroidlabs.nustrim.core.model.SubtitleSource
import org.json.JSONArray
import org.json.JSONObject

object StremioParser {
    fun parseCatalog(text: String): List<MediaItem> {
        val root = JSONObject(text)
        val metas = root.optJSONArray("metas") ?: JSONArray()
        return buildList {
            for (i in 0 until metas.length()) {
                val meta = metas.optJSONObject(i) ?: continue
                parseMetaObject(meta)?.let(::add)
            }
        }
    }

    fun parseMetaResponse(text: String): MediaItem? {
        val root = JSONObject(text)
        val meta = root.optJSONObject("meta") ?: return null
        return parseMetaObject(meta)
    }

    fun parseStreams(text: String): List<StreamSource> {
        val root = JSONObject(text)
        val streams = root.optJSONArray("streams") ?: JSONArray()
        return buildList {
            for (i in 0 until streams.length()) {
                val stream = streams.optJSONObject(i) ?: continue
                add(parseStream(stream, i))
            }
        }
    }

    private fun parseMetaObject(meta: JSONObject): MediaItem? {
        val id = meta.optString("id").trim()
        val type = meta.optString("type").trim()
        val name = meta.optString("name").trim()
        if (id.isBlank() || type.isBlank() || name.isBlank()) return null

        val episodes = buildList {
            val videos = meta.optJSONArray("videos") ?: JSONArray()
            for (i in 0 until videos.length()) {
                val video = videos.optJSONObject(i) ?: continue
                val videoId = video.optString("id").trim()
                if (videoId.isBlank()) continue
                add(
                    MediaEpisode(
                        id = videoId,
                        title = video.optString("title", "Episode ${i + 1}"),
                        season = video.optIntOrNull("season"),
                        episode = video.optIntOrNull("episode"),
                        thumbnailUrl = video.optString("thumbnail", ""),
                        overview = video.optString("overview", "")
                    )
                )
            }
        }

        return MediaItem(
            id = id,
            title = name,
            description = meta.optString("description", ""),
            type = MediaType.from(type),
            posterUrl = meta.optString("poster", ""),
            backgroundUrl = meta.optString("background", ""),
            releaseInfo = meta.optString("releaseInfo", ""),
            episodes = episodes,
            ref = MediaRef(
                sourceKind = "stremio",
                mediaType = type,
                metaId = id
            )
        )
    }

    private fun parseStream(stream: JSONObject, index: Int): StreamSource {
        val title = listOf(
            stream.optString("name", "").trim(),
            stream.optString("description", stream.optString("title", "")).trim()
        ).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Source ${index + 1}" }

        val directUrl = stream.optString("url", "").trim()
        if (directUrl.isNotBlank()) {
            val requestHeaders = buildMap {
                val request = stream.optJSONObject("behaviorHints")
                    ?.optJSONObject("proxyHeaders")
                    ?.optJSONObject("request")
                if (request != null) {
                    val keys = request.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val value = request.optString(key)
                        if (value.isNotBlank()) put(key, value)
                    }
                }
            }
            return StreamSource(
                name = title,
                url = directUrl,
                type = inferType(directUrl),
                headers = requestHeaders,
                playable = true,
                subtitles = parseSubtitles(stream.optJSONArray("subtitles"))
            )
        }

        val infoHash = stream.optString("infoHash", "").trim()
        if (infoHash.isNotBlank()) {
            return StreamSource(
                name = title,
                playable = false,
                note = "Torrent stream detected. Torrent transport is not enabled in Universal Source v1."
            )
        }

        val ytId = stream.optString("ytId", "").trim()
        if (ytId.isNotBlank()) {
            return StreamSource(
                name = title,
                playable = false,
                note = "YouTube stream detected. Native YouTube transport is not enabled yet."
            )
        }

        val externalUrl = stream.optString("externalUrl", "").trim()
        if (externalUrl.isNotBlank()) {
            return StreamSource(
                name = title,
                playable = false,
                note = "External webpage stream detected."
            )
        }

        return StreamSource(
            name = title,
            playable = false,
            note = "This Stremio transport is not supported by the current player engine."
        )
    }

    private fun parseSubtitles(array: JSONArray?): List<SubtitleSource> = buildList {
        val subtitles = array ?: return@buildList
        for (index in 0 until subtitles.length()) {
            val item = subtitles.optJSONObject(index) ?: continue
            val url = item.optString("url").trim()
            if (url.isBlank()) continue
            val lang = item.optString("lang").trim()
            add(
                SubtitleSource(
                    id = item.optString("id"),
                    url = url,
                    language = lang,
                    label = item.optString("label").ifBlank { lang }
                )
            )
        }
    }.distinctBy { "${it.url}|${it.language}" }

    private fun inferType(url: String): String {
        val clean = url.substringBefore('?').lowercase()
        return when {
            clean.endsWith(".m3u8") -> "hls"
            clean.endsWith(".mpd") -> "dash"
            clean.endsWith(".mp4") -> "mp4"
            clean.endsWith(".mkv") -> "mkv"
            else -> "auto"
        }
    }

    private fun JSONObject.optIntOrNull(name: String): Int? =
        if (has(name) && !isNull(name)) optInt(name) else null
}
