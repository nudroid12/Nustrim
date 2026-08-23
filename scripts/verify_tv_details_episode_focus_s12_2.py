#!/usr/bin/env python3
"""Static contract checks for S12.2 TV Details and episode focus closure."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DETAILS = ROOT / "app/src/main/java/app/nudroidlabs/nustrim/tv/details/TvDetailsScreen.kt"
SHELL = ROOT / "app/src/main/java/app/nudroidlabs/nustrim/tv/shell/TvShell.kt"
BUILD = ROOT / "app/build.gradle.kts"
MARKER = ROOT / ".nustrim-tv"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


details = read(DETAILS)
shell = read(SHELL)
build = read(BUILD)
marker = read(MARKER)

checks = {
    "restore anchor is captured once per return request":
        "val requestedRestoreAnchor = remember(scopeKey, focusRequestToken)" in details,
    "restore anchor is validated against current catalogue":
        "takeIf { it in validRestoreAnchors }" in details,
    "episode row is composed before exact focus request":
        "listState.scrollToItem(EPISODE_ROW_INDEX)" in details,
    "season row is composed before exact focus request":
        "listState.scrollToItem(SEASON_TAB_ROW_INDEX)" in details,
    "focus restoration remains bounded":
        "repeat(TvTokens.FocusRestoreAttempts)" in details,
    "hero fallback remains available":
        "focusRegistry.requestAnchor(scopeKey, HERO_PLAY_ANCHOR)" in details,
    "remembered episode drives hero play":
        "selectedEpisode = rememberedEpisode" in details
        and "val episode = rememberedEpisode" in details,
    "episode activation commits exact cursor":
        "Key.DirectionCenter, Key.Enter -> {\n                        onFocused()\n                        onOpen()" in details,
    "provider cast metadata is rendered":
        'text = "Cast: " + item.cast.take(5).joinToString(", ")' in details,
    "details to sources preserves exact anchor":
        "anchorKey = focusRegistry.lastFocused(route.focusScope)" in shell,
    "back action still pops one route":
        "TvBackAction.POP_ROUTE -> {\n                navigator.pop()" in shell,
    "target version":
        'versionName = "0.57.1-tv-cleanroom-s12.2-details-episode-focus"' in build,
    "target version code": "versionCode = 124" in build,
    "stage marker": "subsystem=12.2-details-episode-focus" in marker,
}

failed = [name for name, passed in checks.items() if not passed]
if failed:
    raise SystemExit("S12.2 Details invariant failed: " + ", ".join(failed))

print(f"S12.2 Details and episode focus invariants passed: {len(checks)}/{len(checks)}")
