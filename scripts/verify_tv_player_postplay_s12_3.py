#!/usr/bin/env python3
"""Static contract checks for S12.3 TV Player overlay and post-play closure."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PLAYER = ROOT / "app/src/main/java/app/nudroidlabs/nustrim/tv/player"
SOURCES = ROOT / "app/src/main/java/app/nudroidlabs/nustrim/tv/sources"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


entry = read(PLAYER / "TvPlayerEntry.kt")
screen = read(PLAYER / "TvPlayerScreen.kt")
controls = read(PLAYER / "TvPlayerControls.kt")
sources = read(SOURCES / "TvSourcesEntry.kt")
build = read(ROOT / "app/build.gradle.kts")
marker = read(ROOT / ".nustrim-tv")

checks = {
    "autoplay next preference reaches player":
        "autoplayNextEpisode = preferences.autoplayNextEpisode" in entry,
    "seek step preference reaches player":
        "seekStepMs = preferences.tvSeekStepSeconds * 1_000L" in entry,
    "control hide preference reaches player":
        "controlsAutoHideMs = preferences.tvControlsAutoHideSeconds * 1_000L" in entry,
    "control hide uses preference":
        "delay(controlsAutoHideMs.coerceAtLeast(MIN_CONTROL_HIDE_MS))" in screen,
    "hidden seek uses preference":
        "seekStepForRepeat(event.nativeKeyEvent.repeatCount, seekStepMs)" in screen,
    "focused progress seek uses preference":
        "runtime.seekBy(-seekStepMs)" in controls and "runtime.seekBy(seekStepMs)" in controls,
    "post-play receives autoplay preference":
        "autoplayEnabled = autoplayNextEpisode" in screen,
    "post-play countdown is bounded":
        "for (remaining in POST_PLAY_COUNTDOWN_SECONDS downTo 1)" in screen,
    "post-play countdown can be cancelled":
        "if (it.isFocused) autoplayCancelled = true" in screen,
    "post-play keeps explicit next action":
        '"Press OK to play"' in screen and "onNext = { nextEpisode?.let(onEpisodeSelected) }" in screen,
    "post-play keeps back to details":
        'text = "Back to details"' in screen and ".clickable(onClick = onBackToDetails)" in screen,
    "first source autoplay honours preference":
        "UiPreferences(context.applicationContext).autoplayFirstSource" in sources,
    "first source autoplay is one-shot":
        "if (!autoplayFirstSource || autoplayConsumed)" in sources
        and "autoplayConsumed = true" in sources,
    "first source autoplay requires playable result":
        "ready.snapshot.streams.firstOrNull { it.playable }" in sources,
    "target version":
        'versionName = "0.57.2-tv-cleanroom-s12.3-player-postplay"' in build,
    "target version code": "versionCode = 125" in build,
    "stage marker": "subsystem=12.3-player-overlay-postplay" in marker,
}

failed = [name for name, passed in checks.items() if not passed]
if failed:
    raise SystemExit("S12.3 Player invariant failed: " + ", ".join(failed))

print(f"S12.3 Player overlay and post-play invariants passed: {len(checks)}/{len(checks)}")
