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
repository = read(
    "app/src/main/java/app/nudroidlabs/nustrim/tv/sources/TvSourcesRepository.kt"
)
provider = read(
    "app/src/main/java/app/nudroidlabs/nustrim/core/source/cloudstream/CloudStreamProviderSession.kt"
)

check("S12.25 feature marker", "subsystem=12.25-cloudstream-links-fix" in marker)
check(
    "S12.25 version",
    version == "0.57.24-tv-cleanroom-s12.25-cloudstream-links-fix",
)
check("S12.25 versionCode", "versionCode = 147" in gradle)
check("provider data ID match", '"provider-data-id"' in repository)
check("exact season and episode match", '"exact-season-episode"' in repository)
check("catalogue title match", '"catalogue-episode-title"' in repository)
check(
    "missing provider season fallback",
    '"episode-number-provider-season-missing"' in repository,
)
check("equal-count ordinal fallback", '"catalogue-ordinal-equal-count"' in repository)
check("generic episode titles rejected", "GENERIC_EPISODE_TITLE_REGEX" in repository)
check("unmatched episodes retain reason", "No provider episode match" in repository)
check("episode match diagnostics", '"CLOUDSTREAM_EPISODE_MATCH"' in repository)
check("episode no-match diagnostics", '"CLOUDSTREAM_EPISODE_NO_MATCH"' in repository)
check("stream failures preserved", "ProviderStreamsResolution" in repository)
check("all failed candidates become error", "failures.size == results.size" in repository)
check("repository loadLinks result diagnostics", '"CLOUDSTREAM_LINKS_RESULT"' in repository)
check("provider loadLinks start diagnostics", '"CLOUDSTREAM_LOADLINKS_START"' in provider)
check("provider loadLinks result diagnostics", '"CLOUDSTREAM_LOADLINKS_RESULT"' in provider)
check("provider loadLinks exception diagnostics", '"CLOUDSTREAM_LOADLINKS_ERROR"' in provider)
check("missing loadLinks data diagnostics", '"CLOUDSTREAM_LOADLINKS_NO_DATA"' in provider)
check(
    "playable-only Sources contract preserved",
    "val playableStreams = resolvedStreams.filter { it.playable && it.url.isNotBlank() }"
    in repository,
)
check(
    "S12.24 sidebar cleanup preserved",
    "branding-sidebar=removed-by-user-request" in marker,
)
check(
    "S12.23 player Episodes preserved",
    "player-episodes-tab=visible-for-episode-playback" in marker,
)

failed = [name for name, passed in checks if not passed]
for name, passed in checks:
    print(f"{'PASS' if passed else 'FAIL'}: {name}")

if failed:
    raise SystemExit(
        f"{len(failed)} of {len(checks)} checks failed: {', '.join(failed)}"
    )

print(f"S12.25 CloudStream links contracts: {len(checks)}/{len(checks)} passed")
