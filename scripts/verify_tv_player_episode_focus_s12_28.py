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
panels = read("app/src/main/java/app/nudroidlabs/nustrim/tv/player/TvPlayerPanels.kt")

check("S12.28 marker", "subsystem=12.28-player-episode-focus" in marker)
check("active season focus marker", "player-episode-first-up=active-season-tab" in marker)
check("stable season requester marker", "player-season-focus-requester=stable-per-season" in marker)
check("S12.28 version", version == "0.57.27-tv-cleanroom-s12.28-player-episode-focus")
check("S12.28 version name", 'versionName = "0.57.27-tv-cleanroom-s12.28-player-episode-focus"' in gradle)
check("S12.28 version code", "versionCode = 150" in gradle)
check("focus properties imported", "import androidx.compose.ui.focus.focusProperties" in panels)
check(
    "stable requester per season",
    "val seasonRequesters = remember(catalogue.parentKey, seasonKeys)" in panels
    and "catalogue.seasons.map { FocusRequester() }" in panels,
)
check(
    "season chips own requesters",
    "focusRequester = seasonRequesters[index]" in panels,
)
check(
    "only first episode targets active season",
    "upFocusRequester = seasonRequesters[selectedSeasonIndex].takeIf { index == 0 }" in panels,
)
check(
    "episode row exposes directional target",
    "upFocusRequester: FocusRequester?" in panels,
)
check(
    "focus up is explicitly assigned",
    "if (upFocusRequester != null) Modifier.focusProperties { up = upFocusRequester }" in panels,
)
check(
    "season selection remains focus driven",
    "onFocus = { pendingSeasonIndex = index }" in panels,
)
check(
    "season switching remains click driven",
    "onClick = { selectedSeasonIndex = index }" in panels,
)

failed = [name for name, passed in checks if not passed]
for name, passed in checks:
    print(f"{'PASS' if passed else 'FAIL'}: {name}")

if failed:
    raise SystemExit(f"{len(failed)} of {len(checks)} checks failed: {', '.join(failed)}")

print(f"S12.28 Player episode focus contracts: {len(checks)}/{len(checks)} passed")
