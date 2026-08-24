#!/usr/bin/env python3
"""Static contracts for S12.10 compact catalogue posters and title actions."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "app/src/main/java/app/nudroidlabs/nustrim/tv/home"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


rows = read(HOME / "TvHomeRows.kt")
screen = read(HOME / "TvHomeScreen.kt")
entry = read(HOME / "TvHomeEntry.kt")
long_press = read(HOME / "TvHomeLongPress.kt")
actions = read(HOME / "TvHomePosterActions.kt")
store = read(ROOT / "app/src/main/java/app/nudroidlabs/nustrim/core/library/LocalMediaStore.kt")
build = read(ROOT / "app/build.gradle.kts")
marker = read(ROOT / ".nustrim-tv")

checks = {
    "catalogue poster width is compact": "HOME_POSTER_WIDTH = 116.dp" in rows,
    "catalogue poster height retains 2 by 3 ratio": "HOME_POSTER_HEIGHT = 174.dp" in rows,
    "Continue Watching landscape width is preserved": "HOME_CONTINUE_WIDTH = 210.dp" in rows,
    "Continue Watching landscape height is preserved": "HOME_CONTINUE_HEIGHT = 118.dp" in rows,
    "loading skeleton follows compact dimensions": (
        ".size(width = HOME_POSTER_WIDTH, height = HOME_POSTER_HEIGHT)" in screen
    ),
    "Home cards track select-key holds": "rememberTvHomeLongPressTracker()" in rows,
    "short select still opens the card": "onClick = { onOpen(media, rowIndex, itemIndex) }" in rows,
    "long select opens title actions": (
        "onLongPress = { onLongPress(media, rowIndex, itemIndex) }" in rows
    ),
    "remote Menu key also opens title actions": "KEYCODE_MENU" in long_press,
    "long press uses platform timeout": "ViewConfiguration.getLongPressTimeout()" in long_press,
    "long press is emitted only once per hold": "longPressHandled" in long_press,
    "DPAD centre is recognised": "KEYCODE_DPAD_CENTER" in long_press,
    "Enter is recognised": "KEYCODE_ENTER" in long_press,
    "dialog uses supplied title": "text = target.media.item.title" in actions,
    "dialog identifies Title actions": 'text = "Title actions"' in actions,
    "dialog provides Go to details": 'label = "Go to details"' in actions,
    "dialog provides Add and Remove library states": (
        '"Remove from library" else "Add to library"' in actions
    ),
    "dialog provides watched and unwatched states": (
        '"Mark as unwatched" else "Mark as watched"' in actions
    ),
    "first action receives initial focus": (
        "firstAction.requestFocus()" in actions and ".focusRequester(firstAction)" in actions
    ),
    "closing dialog restores the origin card": (
        "focusRegistry.requestAnchor(scopeKey, anchorKey)" in screen
        and "focusAfterDialog = actionTarget?.anchorKey" in screen
    ),
    "Go to details bypasses Continue Watching direct resume": (
        "target.media.copy(continueEntry = null)" in screen
    ),
    "Home reads saved state from local store": "mediaStore.isSaved" in entry,
    "Home writes saved state to local store": "mediaStore.setSaved" in entry,
    "Home reads watched state from local store": "mediaStore.isWatched" in entry,
    "Home writes watched state to local store": "mediaStore.setWatched" in entry,
    "mark watched clears Continue Watching state": "mediaStore.clearContinueWatching" in entry,
    "existing local storage operations remain available": (
        "fun setSaved(" in store and "fun setWatched(" in store and "fun clearContinueWatching(" in store
    ),
    "target version name": (
        'versionName = "0.57.9-tv-cleanroom-s12.10-catalog-actions"' in build
    ),
    "target version code": "versionCode = 132" in build,
    "stage marker": "subsystem=12.10-catalog-actions" in marker,
    "compact size marker": "home-poster-size=116x174dp" in marker,
    "title actions marker": "home-poster-hold=title-actions" in marker,
    "focus restoration marker": "home-poster-action-focus=return-to-origin-card" in marker,
    "device acceptance remains explicit": "backlog=device-parity-acceptance" in marker,
}

failed = [name for name, passed in checks.items() if not passed]
if failed:
    raise SystemExit("S12.10 catalogue action invariant failed: " + ", ".join(failed))

print(f"S12.10 catalogue action invariants passed: {len(checks)}/{len(checks)}")
