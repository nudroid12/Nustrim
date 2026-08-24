package app.nudroidlabs.nustrim.tv.player

import app.nudroidlabs.nustrim.core.model.SubtitleSource
import java.util.Locale

/** Keeps Malay and Indonesian as separate languages even when a provider supplies a wrong code. */
internal object TvSubtitleLanguage {
    private const val PROVIDER_SEPARATOR = "|||NUSTRIM_PROVIDER|||"

    fun canonicalCode(rawCode: String, rawLabel: String = ""): String {
        val explicitLabelCode = explicitMalayOrIndonesianCode(trackLabel(rawLabel))
        if (explicitLabelCode != null) return explicitLabelCode

        val clean = rawCode.trim().lowercase(Locale.ROOT)
        explicitMalayOrIndonesianCode(clean)?.let { return it }
        val base = clean.substringBefore('-').substringBefore('_')
        return when (base) {
            "", "und", "unknown", "mul", "zxx" -> ""
            "eng", "english" -> "en"
            "ms", "msa", "may", "malay", "melayu" -> "ms"
            "id", "in", "ind", "indonesian", "indonesia" -> "id"
            "spa", "spanish" -> "es"
            "por", "portuguese" -> "pt"
            "fra", "fre", "french" -> "fr"
            "deu", "ger", "german" -> "de"
            "ita", "italian" -> "it"
            "jpn", "japanese" -> "ja"
            "kor", "korean" -> "ko"
            "zho", "chi", "chinese" -> "zh"
            "ara", "arabic" -> "ar"
            "tha", "thai" -> "th"
            "vie", "vietnamese" -> "vi"
            "rus", "russian" -> "ru"
            "hin", "hindi" -> "hi"
            else -> base.takeIf { it.length in 2..3 }.orEmpty()
        }
    }

    fun normalize(subtitle: SubtitleSource): SubtitleSource {
        val code = canonicalCode(subtitle.language, subtitle.label)
        return if (code == subtitle.language) subtitle else subtitle.copy(language = code)
    }

    fun displayName(code: String): String {
        val canonical = canonicalCode(code)
        return when (canonical) {
            "ms" -> "Malay"
            "id" -> "Indonesian"
            "" -> ""
            else -> Locale.forLanguageTag(canonical)
                .getDisplayLanguage(Locale.ENGLISH)
                .trim()
                .takeIf { it.isNotBlank() && !it.equals(canonical, ignoreCase = true) }
                ?: canonical.uppercase(Locale.ROOT)
        }
    }

    private fun trackLabel(raw: String): String = raw
        .split(PROVIDER_SEPARATOR, limit = 2)
        .let { parts -> if (parts.size == 2) parts[1] else parts[0] }
        .trim()

    private fun explicitMalayOrIndonesianCode(raw: String): String? {
        val value = raw.lowercase(Locale.ROOT)
            .replace('_', '-')
            .replace(Regex("[^a-z-]+"), " ")
            .trim()
        val words = value.replace('-', ' ')
        if (value.isBlank() || value == "bahasa") return null
        if (
            value == "id" || value == "id-id" || value == "in" || value == "ind" ||
            containsPhrase(words, "indonesian") || containsPhrase(words, "indonesia") ||
            containsPhrase(words, "bahasa indonesia") || containsPhrase(words, "indo")
        ) {
            return "id"
        }
        if (
            value == "ms" || value == "ms-my" || value == "msa" || value == "may" ||
            containsPhrase(words, "malay") || containsPhrase(words, "melayu") ||
            containsPhrase(words, "bahasa melayu")
        ) {
            return "ms"
        }
        return null
    }

    private fun containsPhrase(value: String, phrase: String): Boolean =
        Regex("(^|\\s)${Regex.escape(phrase)}(\\s|$)").containsMatchIn(value)
}
