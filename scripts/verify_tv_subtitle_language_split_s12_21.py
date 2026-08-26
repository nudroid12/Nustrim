#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


checks: list[tuple[str, bool]] = []


def check(name: str, condition: bool) -> None:
    checks.append((name, condition))


marker = read(".nustrim-tv")
version = read(".nustrim-version").strip()
gradle = read("app/build.gradle.kts")
language = read("app/src/main/java/app/nudroidlabs/nustrim/tv/player/TvSubtitleLanguage.kt")
repository = read("app/src/main/java/app/nudroidlabs/nustrim/tv/player/TvSubtitleRepository.kt")
runtime = read("app/src/main/java/app/nudroidlabs/nustrim/tv/player/TvPlayerRuntime.kt")
panels = read("app/src/main/java/app/nudroidlabs/nustrim/tv/player/TvPlayerPanels.kt")
settings = read("app/src/main/java/app/nudroidlabs/nustrim/tv/settings/TvSettingsModels.kt")

check("S12.21 feature marker", "subtitle-language-panel=separate-malay-and-indonesian-rails" in marker)
check(
    "S12.21 or later version",
    version in {
        "0.57.20-tv-cleanroom-s12.21-subtitle-language-split",
        "0.57.21-tv-cleanroom-s12.22-branding",
        "0.57.22-tv-cleanroom-s12.23-player-episodes",
        "0.57.23-tv-cleanroom-s12.24-sidebar-clean",
        "0.57.24-tv-cleanroom-s12.25-cloudstream-links-fix",
        "0.57.25-tv-cleanroom-s12.26-tv-integrations",
        "0.57.26-tv-cleanroom-s12.27-cloudstream-runtime-links",
        "0.57.27-tv-cleanroom-s12.28-player-episode-focus",
    },
)
check("S12.21 or later versionCode", any(f"versionCode = {code}" in gradle for code in (143, 144, 145, 146, 147, 148, 149, 150)))
check("Malay and Indonesian remain separate settings", '"ms" to "Malay"' in settings and '"id" to "Indonesian"' in settings)
check("Malay ISO 639-1 maps to ms", '"ms", "msa", "may", "malay", "melayu" -> "ms"' in language)
check("Indonesian ISO 639-1 maps to id", '"id", "in", "ind", "indonesian", "indonesia" -> "id"' in language)
check("Malay BCP 47 tag supported", 'value == "ms-my"' in language)
check("Indonesian BCP 47 tag supported", 'value == "id-id"' in language)
check("Bahasa Melayu label supported", 'containsPhrase(words, "bahasa melayu")' in language)
check("Bahasa Indonesia label supported", 'containsPhrase(words, "bahasa indonesia")' in language)
check("Malay standalone label supported", 'containsPhrase(words, "malay")' in language)
check("Indonesian standalone label supported", 'containsPhrase(words, "indonesian")' in language)
check("Short Indo label supported", 'containsPhrase(words, "indo")' in language)
check("Bare Bahasa is not guessed", 'value == "bahasa") return null' in language)
check("Provider prefix excluded from classification", '.split(PROVIDER_SEPARATOR, limit = 2)' in language)
check("Explicit label checked before provider code", language.index("explicitLabelCode") < language.index("val clean = rawCode"))
check("Wrong ms code can be corrected by Indonesian label", "if (explicitLabelCode != null) return explicitLabelCode" in language)
check("Wrong id code can be corrected by Malay label", "if (explicitLabelCode != null) return explicitLabelCode" in language)
check("Malay display name is explicit", '"ms" -> "Malay"' in language)
check("Indonesian display name is explicit", '"id" -> "Indonesian"' in language)
check("Discovered subtitles are normalised", "TvSubtitleLanguage.normalize(subtitle)" in repository)
check("Preference matching uses code and label", "canonicalCode(subtitle.language, subtitle.label)" in repository)
check("Subtitle identity uses corrected language", "canonicalCode(subtitle.language, subtitle.label)" in repository)
check("Media3 runtime passes raw track label", "rawLabel = rawLabel" in runtime)
check("Media3 runtime uses shared classifier", "TvSubtitleLanguage.canonicalCode(" in runtime)
check("Panel receives explicit language names", "TvSubtitleLanguage.displayName(languageCode)" in runtime)
check("Panel title aliases use shared classifier", "TvSubtitleLanguage.canonicalCode(raw)" in panels)
check("Separate language rail marker", "subtitle-language-panel=separate-malay-and-indonesian-rails" in marker)
check("CloudStream TV preserved", "cloudstream-tv-search=enabled-provider-rows" in marker)
check("Subtitle panel polish preserved", "subtitle-overlay=language-track-style-rails" in marker)

failed = [name for name, passed in checks if not passed]
for name, passed in checks:
    print(f"{'PASS' if passed else 'FAIL'}: {name}")

if failed:
    raise SystemExit(f"{len(failed)} of {len(checks)} checks failed: {', '.join(failed)}")

print(f"S12.21 subtitle language contracts: {len(checks)}/{len(checks)} passed")
