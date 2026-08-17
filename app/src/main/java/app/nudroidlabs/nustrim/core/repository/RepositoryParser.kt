package app.nudroidlabs.nustrim.core.repository

import app.nudroidlabs.nustrim.core.model.MediaCatalog
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.MediaType
import app.nudroidlabs.nustrim.core.model.StreamSource
import org.json.JSONObject

object RepositoryParser {
    fun parse(json: String): MediaCatalog {
        val root = JSONObject(json)
        val itemsJson = root.optJSONArray("items")
            ?: throw IllegalArgumentException("Repository does not contain an items array")

        val items = buildList {
            for (i in 0 until itemsJson.length()) {
                val item = itemsJson.getJSONObject(i)
                val streamsJson = item.optJSONArray("streams")
                val streams = buildList {
                    if (streamsJson != null) {
                        for (j in 0 until streamsJson.length()) {
                            val stream = streamsJson.getJSONObject(j)
                            val headersObject = stream.optJSONObject("headers")
                            val headers = buildMap {
                                if (headersObject != null) {
                                    val keys = headersObject.keys()
                                    while (keys.hasNext()) {
                                        val key = keys.next()
                                        put(key, headersObject.optString(key))
                                    }
                                }
                            }

                            val url = stream.optString("url").trim()
                            if (url.isNotBlank()) {
                                add(
                                    StreamSource(
                                        name = stream.optString("name", "Source ${j + 1}"),
                                        url = url,
                                        type = stream.optString("type", "auto"),
                                        headers = headers,
                                        playable = true
                                    )
                                )
                            }
                        }
                    }
                }

                add(
                    MediaItem(
                        id = item.optString("id", "item-$i"),
                        title = item.optString("title", "Untitled"),
                        description = item.optString("description", ""),
                        type = MediaType.from(item.optString("type")),
                        streams = streams,
                        posterUrl = item.optString("poster", "")
                    )
                )
            }
        }

        return MediaCatalog(
            name = root.optString("name", "Repository"),
            version = root.optInt("version", 1),
            items = items
        )
    }
}
