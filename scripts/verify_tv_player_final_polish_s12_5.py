#!/usr/bin/env python3
"""Static contracts for S12.5 TV Player final polish."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PLAYER = ROOT / "app/src/main/java/app/nudroidlabs/nustrim/tv/player"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


entry = read(PLAYER / "TvPlayerEntry.kt")
screen = read(PLAYER / "TvPlayerScreen.kt")
store = read(ROOT / "app/src/main/java/app/nudroidlabs/nustrim/core/library/LocalMediaStore.kt")
build = read(ROOT / "app/build.gradle.kts")
marker = read(ROOT / ".nustrim-tv")

checks = {
    "player creates local media store": "LocalMediaStore(context.applicationContext)" in entry,
    "player restores saved position": "mediaStore.resumePosition(" in entry,
    "resume uses current source": "sourceUrl = route.request.sourceUrl" in entry,
    "resume uses current media": "item = route.request.media" in entry,
    "resume uses current episode": "episode = route.request.episode" in entry,
    "progress saves periodically": "PROGRESS_SAVE_INTERVAL_MS" in entry,
    "progress interval is ten seconds": "PROGRESS_SAVE_INTERVAL_MS = 10_000L" in entry,
    "progress saves on disposal": "if (!runtime.ended)" in entry and "mediaStore.recordProgress(" in entry,
    "natural completion is recorded": "completed = true" in entry,
    "completion queues next episode": "nextEpisode = nextEpisode" in entry,
    "next episode lookup rejects unknown current episode": "currentIndex.takeIf { it >= 0 }" in entry,
    "runtime retry has a unique key": 'retry/$retryToken' in entry,
    "playback retry preserves position": "resumePositionMs = runtime.positionMs" in entry,
    "runtime errors expose retry": "onRetry = onRetryPlayback" in screen,
    "player creation errors expose retry": "onRetry = { retryToken += 1 }" in entry,
    "error overlay has retry action": 'Text("Try again")' in screen,
    "backend resume threshold remains bounded": "MIN_PROGRESS_MS" in store,
    "backend completion threshold remains bounded": "COMPLETE_FRACTION" in store,
    "target version": 'versionName = "0.57.4-tv-cleanroom-s12.5-player-final-polish"' in build,
    "target version code": "versionCode = 127" in build,
    "stage marker": "subsystem=12.5-player-final-polish" in marker,
    "device acceptance remains explicit": "backlog=device-parity-acceptance" in marker,
}

failed = [name for name, passed in checks.items() if not passed]
if failed:
    raise SystemExit("S12.5 invariant failed: " + ", ".join(failed))

print(f"S12.5 Player final polish invariants passed: {len(checks)}/{len(checks)}")
