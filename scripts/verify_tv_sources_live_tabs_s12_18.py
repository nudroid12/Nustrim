#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


checks: list[tuple[str, bool]] = []


def check(name: str, condition: bool) -> None:
    checks.append((name, condition))


models = read("app/src/main/java/app/nudroidlabs/nustrim/tv/sources/TvSourcesModels.kt")
screen = read("app/src/main/java/app/nudroidlabs/nustrim/tv/sources/TvSourcesScreen.kt")
entry = read("app/src/main/java/app/nudroidlabs/nustrim/tv/sources/TvSourcesEntry.kt")
marker = read(".nustrim-tv")
version = read(".nustrim-version").strip()
gradle = read("app/build.gradle.kts")

check("S12.18 feature preserved", "sources-terminal-tabs=playable-links-only" in marker)
check(
    "post-S12.18 version",
    version in {
        "0.57.17-tv-cleanroom-s12.18-sources-live-tabs",
        "0.57.18-tv-cleanroom-s12.19-cloudstream-speed",
        "0.57.19-tv-cleanroom-s12.20-cloudstream-tv",
        "0.57.20-tv-cleanroom-s12.21-subtitle-language-split",
        "0.57.21-tv-cleanroom-s12.22-branding",
    },
)
check("post-S12.18 versionCode", any(f"versionCode = {code}" in gradle for code in (140, 141, 142, 143, 144)))
check("Playable streams are explicit", "val playableStreams" in models and "streams.filter { it.playable }" in models)
check("Source labels come from playable links", "val sourceLabels: List<String> = playableStreams" in models)
check("All filter returns playable links only", "if (sourceLabel == null) playableStreams" in models)
check("Ready requires a playable link", "snapshot.playableStreams.isNotEmpty()" in entry)
check("Loading tabs remain temporary", "it.status == TvSourceAttemptStatus.LOADING" in screen)
check("Empty terminal tabs are absent", "it.status != TvSourceAttemptStatus.EMPTY" not in screen)
check("Error terminal tabs are absent", 'TvSourceAttemptStatus.ERROR -> "  ×"' not in screen)
check("Successful provider tabs derive from streams", "snapshot?.sourceLabels.orEmpty()" in screen)
check("Removed selection returns to All", "selectedSource !in availableSources" in screen and "selectedSource = null" in screen)
check("Refresh remains available", 'item(key = "refresh")' in screen)
check("All tab remains available", 'item(key = "all")' in screen)
check("Playable-only marker", "sources-terminal-tabs=playable-links-only" in marker)
check("Error tab removal marker", "sources-error-tabs=removed-after-completion" in marker)

failed = [name for name, passed in checks if not passed]
for name, passed in checks:
    print(f"{'PASS' if passed else 'FAIL'}: {name}")

if failed:
    raise SystemExit(f"{len(failed)} of {len(checks)} checks failed: {', '.join(failed)}")

print(f"S12.18 Sources live-tab contracts: {len(checks)}/{len(checks)} passed")
