package app.nudroidlabs.nustrim.core.integrations

import app.nudroidlabs.nustrim.core.diagnostics.NustrimDiagnostics
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.MediaType
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val CONNECT_TIMEOUT_MS = 12_000
private const val READ_TIMEOUT_MS = 18_000

private data class HttpResult(
    val code: Int,
    val body: String
)

private object IntegrationHttp {
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): HttpResult =
        request("GET", url, headers, null)

    suspend fun postJson(url: String, body: JSONObject, headers: Map<String, String> = emptyMap()): HttpResult =
        request("POST", url, headers + ("Content-Type" to "application/json"), body.toString())

    private suspend fun request(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?
    ): HttpResult = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Nustrim")
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
            if (body != null) {
                doOutput = true
                outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
            }
        }

        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.let {
                BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader -> reader.readText() }
            }.orEmpty()
            HttpResult(code, text)
        } finally {
            connection.disconnect()
        }
    }
}

data class TmdbRecommendation(
    val id: Int,
    val title: String,
    val posterUrl: String,
    val mediaType: MediaType
)

data class TmdbCastMember(
    val name: String,
    val character: String,
    val profileUrl: String
)

data class TmdbTrailer(
    val name: String,
    val key: String,
    val site: String,
    val type: String
)

data class TmdbMetadata(
    val tmdbId: Int,
    val mediaType: MediaType,
    val title: String,
    val overview: String,
    val posterUrl: String,
    val backdropUrl: String,
    val logoUrl: String,
    val releaseYear: String,
    val genres: List<String>,
    val runtimeMinutes: Int?,
    val cast: List<String>,
    val imdbId: String,
    val recommendations: List<TmdbRecommendation>,
    val castMembers: List<TmdbCastMember> = emptyList(),
    val trailers: List<TmdbTrailer> = emptyList(),
    val tagline: String = "",
    val status: String = "",
    val certification: String = "",
    val originCountries: List<String> = emptyList(),
    val originalLanguage: String = "",
    val networks: List<String> = emptyList()
)

data class MdbListRating(
    val source: String,
    val value: Double?,
    val score: Double?,
    val votes: Long?
)

data class TraktDeviceCode(
    val deviceCode: String,
    val userCode: String,
    val verificationUrl: String,
    val expiresIn: Int,
    val interval: Int,
    val createdAtMs: Long = System.currentTimeMillis()
) {
    val expiresAtMs: Long get() = createdAtMs + expiresIn.coerceAtLeast(0) * 1000L
}

data class TraktToken(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val createdAt: Long
) {
    val expiresAtSeconds: Long get() = createdAt + expiresIn
}

object TmdbClient {
    private const val API_ROOT = "https://api.themoviedb.org/3"
    private const val IMAGE_ROOT = "https://image.tmdb.org/t/p"

    suspend fun validate(credential: String): Result<Unit> = runCatching {
        require(credential.isNotBlank()) { "TMDB credential is empty." }
        val result = get("$API_ROOT/configuration", credential)
        ensureSuccess(result, "TMDB")
    }

    suspend fun metadata(item: MediaItem, credential: String): Result<TmdbMetadata> = runCatching {
        require(credential.isNotBlank()) { "TMDB credential is empty." }
        val resolved = resolveIdentity(item, credential)
            ?: error("TMDB could not match this title.")
        val typePath = if (resolved.second == MediaType.SERIES || resolved.second == MediaType.TV) "tv" else "movie"
        val detailsResult = get(
            "$API_ROOT/$typePath/${resolved.first}?append_to_response=credits,images,external_ids,recommendations,videos,release_dates,content_ratings",
            credential
        )
        ensureSuccess(detailsResult, "TMDB details")
        parseDetails(JSONObject(detailsResult.body), resolved.second)
    }.onFailure {
        NustrimDiagnostics.error("TMDB_ERROR", it, "title=${item.title} type=${item.type}")
    }

    private suspend fun resolveIdentity(item: MediaItem, credential: String): Pair<Int, MediaType>? {
        val externalId = sequenceOf(item.ref?.metaId, item.id)
            .filterNotNull()
            .firstOrNull { it.matches(Regex("tt\\d+", RegexOption.IGNORE_CASE)) }

        if (externalId != null) {
            val result = get(
                "$API_ROOT/find/${encode(externalId)}?external_source=imdb_id",
                credential
            )
            ensureSuccess(result, "TMDB find")
            val body = JSONObject(result.body)
            val movie = body.optJSONArray("movie_results")?.optJSONObject(0)
            val tv = body.optJSONArray("tv_results")?.optJSONObject(0)
            when {
                item.type == MediaType.SERIES || item.type == MediaType.TV -> tv?.let { return it.optInt("id") to MediaType.SERIES }
                item.type == MediaType.MOVIE -> movie?.let { return it.optInt("id") to MediaType.MOVIE }
                movie != null -> return movie.optInt("id") to MediaType.MOVIE
                tv != null -> return tv.optInt("id") to MediaType.SERIES
            }
        }

        val numericRef = item.ref?.metaId?.toIntOrNull()
        if (numericRef != null && item.ref?.sourceKind.orEmpty().contains("tmdb", ignoreCase = true)) {
            return numericRef to item.type.normalizedTmdbType()
        }

        val type = item.type.normalizedTmdbType()
        val typePath = if (type == MediaType.SERIES) "tv" else "movie"
        val year = Regex("\\b(?:19|20)\\d{2}\\b").find(item.releaseInfo)?.value
        val yearParam = when {
            year.isNullOrBlank() -> ""
            type == MediaType.SERIES -> "&first_air_date_year=$year"
            else -> "&year=$year"
        }
        val result = get(
            "$API_ROOT/search/$typePath?query=${encode(item.title)}$yearParam&include_adult=false&page=1",
            credential
        )
        ensureSuccess(result, "TMDB search")
        val first = JSONObject(result.body).optJSONArray("results")?.optJSONObject(0) ?: return null
        return first.optInt("id").takeIf { it > 0 }?.let { it to type }
    }

    private fun parseDetails(body: JSONObject, fallbackType: MediaType): TmdbMetadata {
        val type = when {
            body.has("first_air_date") || body.has("number_of_seasons") -> MediaType.SERIES
            body.has("release_date") -> MediaType.MOVIE
            else -> fallbackType.normalizedTmdbType()
        }
        val title = body.optString(if (type == MediaType.SERIES) "name" else "title")
        val date = body.optString(if (type == MediaType.SERIES) "first_air_date" else "release_date")
        val runtime = if (type == MediaType.SERIES) {
            body.optJSONArray("episode_run_time")?.optInt(0)?.takeIf { it > 0 }
        } else {
            body.optInt("runtime").takeIf { it > 0 }
        }
        val images = body.optJSONObject("images")
        val logos = images?.optJSONArray("logos")
        var logoPath = ""
        if (logos != null) {
            for (index in 0 until logos.length()) {
                val logo = logos.optJSONObject(index) ?: continue
                val language = logo.optString("iso_639_1")
                if (language == "en" || language.isBlank() || language == "null") {
                    logoPath = logo.optString("file_path")
                    if (logoPath.isNotBlank()) break
                }
            }
            if (logoPath.isBlank()) logoPath = logos.optJSONObject(0)?.optString("file_path").orEmpty()
        }

        val genres = buildList {
            val array = body.optJSONArray("genres") ?: JSONArray()
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.optString("name")?.takeIf { it.isNotBlank() }?.let(::add)
            }
        }
        val castMembers = buildList {
            val array = body.optJSONObject("credits")?.optJSONArray("cast") ?: JSONArray()
            for (index in 0 until minOf(array.length(), 12)) {
                val member = array.optJSONObject(index) ?: continue
                val name = member.optString("name")
                if (name.isBlank()) continue
                add(
                    TmdbCastMember(
                        name = name,
                        character = member.optString("character"),
                        profileUrl = imageUrl(member.optString("profile_path"), "w185")
                    )
                )
            }
        }
        val cast = castMembers.map { it.name }
        val trailers = buildList {
            val array = body.optJSONObject("videos")?.optJSONArray("results") ?: JSONArray()
            for (index in 0 until array.length()) {
                val video = array.optJSONObject(index) ?: continue
                val site = video.optString("site")
                val key = video.optString("key")
                val videoType = video.optString("type")
                if (!site.equals("YouTube", ignoreCase = true) || key.isBlank()) continue
                if (videoType !in setOf("Trailer", "Teaser", "Clip")) continue
                add(
                    TmdbTrailer(
                        name = video.optString("name").ifBlank { videoType },
                        key = key,
                        site = site,
                        type = videoType
                    )
                )
                if (size >= 8) break
            }
        }
        val certification = if (type == MediaType.SERIES) {
            val results = body.optJSONObject("content_ratings")?.optJSONArray("results") ?: JSONArray()
            var fallback = ""
            var selected = ""
            for (index in 0 until results.length()) {
                val rating = results.optJSONObject(index) ?: continue
                val value = rating.optString("rating")
                if (value.isBlank()) continue
                if (fallback.isBlank()) fallback = value
                if (rating.optString("iso_3166_1") == "US") { selected = value; break }
            }
            selected.ifBlank { fallback }
        } else {
            val countries = body.optJSONObject("release_dates")?.optJSONArray("results") ?: JSONArray()
            var fallback = ""
            var selected = ""
            for (countryIndex in 0 until countries.length()) {
                val country = countries.optJSONObject(countryIndex) ?: continue
                val dates = country.optJSONArray("release_dates") ?: JSONArray()
                for (dateIndex in 0 until dates.length()) {
                    val value = dates.optJSONObject(dateIndex)?.optString("certification").orEmpty()
                    if (value.isBlank()) continue
                    if (fallback.isBlank()) fallback = value
                    if (country.optString("iso_3166_1") == "US") { selected = value; break }
                }
                if (selected.isNotBlank()) break
            }
            selected.ifBlank { fallback }
        }
        val originCountries = buildList {
            val array = if (type == MediaType.SERIES) {
                body.optJSONArray("origin_country") ?: JSONArray()
            } else {
                body.optJSONArray("production_countries") ?: JSONArray()
            }
            for (index in 0 until array.length()) {
                val value = if (type == MediaType.SERIES) {
                    array.optString(index)
                } else {
                    array.optJSONObject(index)?.optString("iso_3166_1").orEmpty()
                }
                if (value.isNotBlank() && value != "null") add(value)
            }
        }
        val networks = buildList {
            val array = body.optJSONArray("networks") ?: JSONArray()
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.optString("name")?.takeIf { it.isNotBlank() }?.let(::add)
            }
        }
        val recommendations = buildList {
            val array = body.optJSONObject("recommendations")?.optJSONArray("results") ?: JSONArray()
            for (index in 0 until minOf(array.length(), 12)) {
                val obj = array.optJSONObject(index) ?: continue
                val id = obj.optInt("id")
                if (id <= 0) continue
                val recommendationType = when (obj.optString("media_type")) {
                    "tv" -> MediaType.SERIES
                    "movie" -> MediaType.MOVIE
                    else -> type
                }
                val recommendationTitle = obj.optString(
                    if (recommendationType == MediaType.SERIES) "name" else "title"
                )
                if (recommendationTitle.isBlank()) continue
                add(
                    TmdbRecommendation(
                        id = id,
                        title = recommendationTitle,
                        posterUrl = imageUrl(obj.optString("poster_path"), "w342"),
                        mediaType = recommendationType
                    )
                )
            }
        }

        return TmdbMetadata(
            tmdbId = body.optInt("id"),
            mediaType = type,
            title = title,
            overview = body.optString("overview"),
            posterUrl = imageUrl(body.optString("poster_path"), "w500"),
            backdropUrl = imageUrl(body.optString("backdrop_path"), "w1280"),
            logoUrl = imageUrl(logoPath, "w500"),
            releaseYear = date.take(4).takeIf { it.length == 4 }.orEmpty(),
            genres = genres,
            runtimeMinutes = runtime,
            cast = cast,
            imdbId = body.optJSONObject("external_ids")?.optString("imdb_id").orEmpty(),
            recommendations = recommendations,
            castMembers = castMembers,
            trailers = trailers,
            tagline = body.optString("tagline"),
            status = body.optString("status"),
            certification = certification,
            originCountries = originCountries,
            originalLanguage = body.optString("original_language"),
            networks = networks
        )
    }

    private suspend fun get(url: String, credential: String): HttpResult {
        return if (credential.trim().startsWith("eyJ")) {
            IntegrationHttp.get(url, mapOf("Authorization" to "Bearer ${credential.trim()}"))
        } else {
            val separator = if (url.contains('?')) '&' else '?'
            IntegrationHttp.get("$url${separator}api_key=${encode(credential.trim())}")
        }
    }

    private fun imageUrl(path: String, size: String): String =
        path.takeIf { it.isNotBlank() && it != "null" }?.let { "$IMAGE_ROOT/$size$it" }.orEmpty()
}

object MdbListClient {
    private const val API_ROOT = "https://api.mdblist.com"

    suspend fun validate(apiKey: String): Result<Unit> = runCatching {
        require(apiKey.isNotBlank()) { "MDBList API key is empty." }
        val result = IntegrationHttp.get("$API_ROOT/tmdb/movie/238?apikey=${encode(apiKey.trim())}")
        ensureSuccess(result, "MDBList")
        JSONObject(result.body)
    }.map { }

    suspend fun ratings(
        item: MediaItem,
        apiKey: String,
        tmdb: TmdbMetadata? = null
    ): Result<List<MdbListRating>> = runCatching {
        require(apiKey.isNotBlank()) { "MDBList API key is empty." }
        val typePath = if (item.type == MediaType.SERIES || item.type == MediaType.TV) "show" else "movie"
        val imdb = sequenceOf(item.ref?.metaId, item.id, tmdb?.imdbId)
            .filterNotNull()
            .firstOrNull { it.matches(Regex("tt\\d+", RegexOption.IGNORE_CASE)) }
        val endpoint = when {
            imdb != null -> "$API_ROOT/imdb/$typePath/${encode(imdb)}"
            tmdb?.tmdbId?.let { it > 0 } == true -> "$API_ROOT/tmdb/$typePath/${tmdb!!.tmdbId}"
            item.ref?.sourceKind.orEmpty().contains("tmdb", ignoreCase = true) && item.ref?.metaId?.toIntOrNull() != null ->
                "$API_ROOT/tmdb/$typePath/${item.ref!!.metaId}"
            else -> error("MDBList needs an IMDb or TMDB identifier for this title.")
        }
        val result = IntegrationHttp.get("$endpoint?apikey=${encode(apiKey.trim())}")
        ensureSuccess(result, "MDBList ratings")
        val array = JSONObject(result.body).optJSONArray("ratings") ?: JSONArray()
        buildList {
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val source = obj.optString("source").lowercase()
                if (source.isBlank()) continue
                add(
                    MdbListRating(
                        source = source,
                        value = obj.optDoubleOrNull("value"),
                        score = obj.optDoubleOrNull("score"),
                        votes = obj.optLongOrNull("votes")
                    )
                )
            }
        }
    }.onFailure {
        NustrimDiagnostics.error("MDBLIST_ERROR", it, "title=${item.title} type=${item.type}")
    }
}

object TraktClient {
    private const val API_ROOT = "https://api.trakt.tv"

    suspend fun createDeviceCode(clientId: String): Result<TraktDeviceCode> = runCatching {
        require(clientId.isNotBlank()) { "Trakt client ID is empty." }
        val result = IntegrationHttp.postJson(
            "$API_ROOT/oauth/device/code",
            JSONObject().put("client_id", clientId.trim())
        )
        ensureSuccess(result, "Trakt device code")
        val obj = JSONObject(result.body)
        TraktDeviceCode(
            deviceCode = obj.getString("device_code"),
            userCode = obj.getString("user_code"),
            verificationUrl = obj.optString("verification_url", "https://trakt.tv/activate"),
            expiresIn = obj.optInt("expires_in", 600),
            interval = obj.optInt("interval", 5).coerceAtLeast(2)
        )
    }

    suspend fun pollDeviceToken(
        deviceCode: String,
        clientId: String,
        clientSecret: String
    ): Result<TraktToken?> = runCatching {
        require(deviceCode.isNotBlank()) { "Trakt device code is empty." }
        require(clientId.isNotBlank()) { "Trakt client ID is empty." }
        require(clientSecret.isNotBlank()) { "Trakt client secret is empty." }
        val result = IntegrationHttp.postJson(
            "$API_ROOT/oauth/device/token",
            JSONObject()
                .put("code", deviceCode)
                .put("client_id", clientId.trim())
                .put("client_secret", clientSecret.trim())
        )
        when (result.code) {
            200 -> {
                val obj = JSONObject(result.body)
                TraktToken(
                    accessToken = obj.getString("access_token"),
                    refreshToken = obj.optString("refresh_token"),
                    expiresIn = obj.optLong("expires_in", 7_776_000L),
                    createdAt = obj.optLong("created_at", System.currentTimeMillis() / 1000L)
                )
            }
            400, 404 -> null
            409 -> error("Trakt code has already been approved.")
            410 -> error("Trakt device code expired.")
            418 -> error("Trakt device code was denied.")
            429 -> error("Trakt asked Nustrim to slow down. Try again shortly.")
            else -> {
                ensureSuccess(result, "Trakt device token")
                null
            }
        }
    }

    suspend fun username(accessToken: String, clientId: String): Result<String> = runCatching {
        require(accessToken.isNotBlank()) { "Trakt access token is empty." }
        require(clientId.isNotBlank()) { "Trakt client ID is empty." }
        val result = IntegrationHttp.get(
            "$API_ROOT/users/settings",
            traktHeaders(accessToken, clientId)
        )
        ensureSuccess(result, "Trakt settings")
        val user = JSONObject(result.body).optJSONObject("user")
        user?.optString("username")
            ?.takeIf { it.isNotBlank() }
            ?: user?.optString("name")?.takeIf { it.isNotBlank() }
            ?: "Connected"
    }

    suspend fun refresh(
        refreshToken: String,
        clientId: String,
        clientSecret: String
    ): Result<TraktToken> = runCatching {
        require(refreshToken.isNotBlank()) { "Trakt refresh token is empty." }
        val result = IntegrationHttp.postJson(
            "$API_ROOT/oauth/token",
            JSONObject()
                .put("refresh_token", refreshToken)
                .put("client_id", clientId.trim())
                .put("client_secret", clientSecret.trim())
                .put("redirect_uri", "urn:ietf:wg:oauth:2.0:oob")
                .put("grant_type", "refresh_token")
        )
        ensureSuccess(result, "Trakt token refresh")
        val obj = JSONObject(result.body)
        TraktToken(
            accessToken = obj.getString("access_token"),
            refreshToken = obj.optString("refresh_token", refreshToken),
            expiresIn = obj.optLong("expires_in", 7_776_000L),
            createdAt = obj.optLong("created_at", System.currentTimeMillis() / 1000L)
        )
    }

    private fun traktHeaders(accessToken: String, clientId: String): Map<String, String> = mapOf(
        "Authorization" to "Bearer ${accessToken.trim()}",
        "trakt-api-key" to clientId.trim(),
        "trakt-api-version" to "2"
    )
}

private fun MediaType.normalizedTmdbType(): MediaType = when (this) {
    MediaType.SERIES, MediaType.TV -> MediaType.SERIES
    else -> MediaType.MOVIE
}

private fun ensureSuccess(result: HttpResult, label: String) {
    if (result.code !in 200..299) {
        val detail = runCatching {
            JSONObject(result.body).optString("status_message")
                .ifBlank { JSONObject(result.body).optString("error") }
                .ifBlank { JSONObject(result.body).optString("message") }
        }.getOrDefault("")
        val suffix = detail.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty()
        error("$label request failed with HTTP ${result.code}$suffix")
    }
}

private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

private fun JSONObject.optDoubleOrNull(name: String): Double? {
    if (!has(name) || isNull(name)) return null
    return runCatching { getDouble(name) }.getOrNull()
}

private fun JSONObject.optLongOrNull(name: String): Long? {
    if (!has(name) || isNull(name)) return null
    return runCatching { getLong(name) }.getOrNull()
}
