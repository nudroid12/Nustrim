#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


checks: list[tuple[str, bool]] = []


def check(name: str, condition: bool) -> None:
    checks.append((name, condition))


details = read("app/src/main/java/app/nudroidlabs/nustrim/tv/details/TvDetailsRepository.kt")
episode_enrichment = details.split("private suspend fun enrichFromCatalogMetadata", 1)[1]
stremio_parser = read("app/src/main/java/app/nudroidlabs/nustrim/core/source/stremio/StremioParser.kt")
builder = read("app/src/main/java/app/nudroidlabs/nustrim/tv/episode/TvEpisodeCatalogueBuilder.kt")
local_store = read("app/src/main/java/app/nudroidlabs/nustrim/core/library/LocalMediaStore.kt")
shell = read("app/src/main/java/app/nudroidlabs/nustrim/tv/shell/TvShell.kt")
controls = read("app/src/main/java/app/nudroidlabs/nustrim/tv/player/TvPlayerControls.kt")
player = read("app/src/main/java/app/nudroidlabs/nustrim/tv/player/TvPlayerScreen.kt")
marker = read(".nustrim-tv")
version = read(".nustrim-version").strip()
gradle = read("app/build.gradle.kts")

check("S12.17 feature preserved", "episode-title-source=catalog-metadata-first,cinemeta-catalog-fallback" in marker)
check(
    "post-S12.17 version",
    version in {
        "0.57.17-tv-cleanroom-s12.18-sources-live-tabs",
        "0.57.18-tv-cleanroom-s12.19-cloudstream-speed",
        "0.57.19-tv-cleanroom-s12.20-cloudstream-tv",
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
check("post-S12.17 versionCode", any(f"versionCode = {code}" in gradle for code in (140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150)))
check("Stremio catalog parses video title", 'title = video.optString("title"' in stremio_parser)
check("Catalog metadata remains first authority", "item.episodes.none(::needsCatalogTitle)" in details)
check("Cinemeta is catalog fallback", "InstalledSourceStore.CINEMETA_URL" in details)
check("Cinemeta direct IMDb identity", "directCinemetaSeed" in details and "IMDB_ID" in details)
check("Cinemeta title search fallback", "findCinemetaMatch" in details and "searchable.search(" in details)
check("Catalog metadata has bounded wait", "CATALOG_METADATA_TIMEOUT_MS = 8_000L" in details)
check("Catalog episodes merge by coordinates", "catalogByCoordinate[season to number]" in details)
check("Catalog title replaces generic title", "title = catalogEpisode.title.ifBlank" in details)
check(
    "TMDB is not used for episode titles",
    "TmdbClient" not in episode_enrichment
    and "tmdbEnrichmentEnabled" not in episode_enrichment,
)
check("Canonical title reaches provider episode", "providerEpisode = canonicalProviderEpisode" in builder)
check("Duplicate merge keeps canonical provider title", "providerEpisode = first.providerEpisode.copy(" in builder)
check("Generic provider mismatch is corrected", "genericNumber != episodeNumber" in builder)
check("Stored Continue Watching mismatch is corrected", "genericNumber != episode" in local_store)
check("Details routes canonical episode to Sources", "episode = episode.providerEpisode" in shell)
check("Player controls render routed episode title", "append(episode.title)" in controls)
check("Pause overlay renders routed episode title", "episode?.title?.takeIf" in player)
check("Canonical routing contract", "episode-title-canonical-routing=details,sources,player,continue-watching,episode-panel" in marker)
check("Catalog-first marker", "episode-title-source=catalog-metadata-first,cinemeta-catalog-fallback" in marker)

failed = [name for name, passed in checks if not passed]
for name, passed in checks:
    print(f"{'PASS' if passed else 'FAIL'}: {name}")

if failed:
    raise SystemExit(f"{len(failed)} of {len(checks)} checks failed: {', '.join(failed)}")

print(f"S12.17 episode title contracts: {len(checks)}/{len(checks)} passed")
