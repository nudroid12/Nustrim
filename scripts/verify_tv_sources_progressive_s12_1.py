#!/usr/bin/env python3
"""Static contract checks for S12.1 progressive TV source loading."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCES = ROOT / "app/src/main/java/app/nudroidlabs/nustrim/tv/sources"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


repository = read(SOURCES / "TvSourcesRepository.kt")
entry = read(SOURCES / "TvSourcesEntry.kt")
models = read(SOURCES / "TvSourcesModels.kt")
screen = read(SOURCES / "TvSourcesScreen.kt")
player_entry = read(
    ROOT / "app/src/main/java/app/nudroidlabs/nustrim/tv/player/TvPlayerEntry.kt"
)
build = read(ROOT / "app/build.gradle.kts")
marker = read(ROOT / ".nustrim-tv")

checks = {
    "progressive repository flow": "fun loadProgressively(" in repository
    and "channelFlow" in repository,
    "equal initial loading state": "status = TvSourceAttemptStatus.LOADING" in repository,
    "parallel installed providers": "sourceEntries.map { installed ->" in repository
    and "launch {" in repository
    and "}.joinAll()" in repository,
    "per-provider publication": "resolveInstalledSource(route, installed, ::publish)" in repository,
    "parallel CloudStream children": "children.map { child ->" in repository
    and "onProgress(childResult)" in repository,
    "independent provider timeout": "withTimeout(PROVIDER_TIMEOUT_MS)" in repository,
    "partial streams retained": "streams[stream.stableKey] = stream" in repository,
    "unfinished providers normalised": "Provider did not complete before timeout" in repository,
    "loading model": "LOADING," in models and "loadingProviderCount" in models,
    "entry collects progressive snapshots": ".loadProgressively(" in entry
    and ".collect { snapshot ->" in entry,
    "player panel receives progressive snapshots": ".loadProgressively(" in player_entry
    and "sourceSnapshot = snapshot" in player_entry,
    "empty provider tabs removed":
        ".filter { it.status != TvSourceAttemptStatus.EMPTY }" in screen,
    "loading provider tabs visible": "TvSourceAttemptStatus.LOADING -> \"  …\"" in screen,
    "loading count visible": "providers still loading" in screen,
    "target version":
        'versionName = "0.57.0-tv-cleanroom-s12.1-sources-progressive"' in build,
    "target version code": "versionCode = 123" in build,
    "stage marker": "subsystem=12.1-sources-progressive" in marker,
    "parallel marker": "sources-provider-scheduling=parallel-equal-priority" in marker,
}

failed = [name for name, passed in checks.items() if not passed]
if failed:
    raise SystemExit("S12.1 progressive source invariant failed: " + ", ".join(failed))

print(f"S12.1 progressive source invariants passed: {len(checks)}/{len(checks)}")
