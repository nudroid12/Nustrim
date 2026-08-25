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
entry = read("app/src/main/java/app/nudroidlabs/nustrim/tv/player/TvPlayerEntry.kt")
screen = read("app/src/main/java/app/nudroidlabs/nustrim/tv/player/TvPlayerScreen.kt")
panels = read("app/src/main/java/app/nudroidlabs/nustrim/tv/player/TvPlayerPanels.kt")

check("S12.23 marker", "subsystem=12.23-player-episodes" in marker)
check("target version", version == "0.57.22-tv-cleanroom-s12.23-player-episodes")
check("target version name", 'versionName = "0.57.22-tv-cleanroom-s12.23-player-episodes"' in gradle)
check("target version code", "versionCode = 145" in gradle)
check("details repository hydrates shallow series metadata", "TvDetailsRepository" in entry)
check("hydration is limited to episode playback", "if (activeRequest.episode == null)" in entry)
check("complete media replaces shallow episode media", "episodeMedia = detailedMedia!!" in entry)
check("current episode provides immediate fallback", "episodeMedia.episodes.ifEmpty { listOfNotNull(activeRequest.episode) }" in entry)
check("full catalogue uses hydrated entries", "providerEpisodes = episodeEntries" in entry)
check("episode metadata loading state reaches screen", "episodesLoading = episodeMetadataLoading" in entry)
check("episode control no longer requires preloaded catalogue", "hasEpisodes = request.episode != null || episodeCatalogue.episodes.isNotEmpty()" in screen)
check("movies remain excluded from immediate episode control", "hasEpisodes = true" not in screen)
check("episode panel receives loading state", "loading = episodesLoading" in screen)
check("episode panel exposes loading parameter", "loading: Boolean" in panels)
check("episode panel reports full-list loading", 'text = "Loading full episode list..."' in panels)
check("current season is restored after hydration", "remember(catalogue.parentKey, seasonKeys)" in panels)
check("S12.22 branding preserved", "branding-source=user-approved-blue-cyan-nustrim-logo" in marker)
check("catalog episode titles preserved", "episode-title-source=catalog-metadata-first,cinemeta-catalog-fallback" in marker)
check("Continue Watching restoration contract", "player-episodes-continue-watching=full-catalogue-restored" in marker)

failed = [name for name, passed in checks if not passed]
for name, passed in checks:
    print(f"{'PASS' if passed else 'FAIL'}: {name}")

if failed:
    raise SystemExit(f"{len(failed)} of {len(checks)} checks failed: {', '.join(failed)}")

print(f"S12.23 player episode contracts: {len(checks)}/{len(checks)} passed")
