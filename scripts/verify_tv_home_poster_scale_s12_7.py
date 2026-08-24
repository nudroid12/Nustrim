#!/usr/bin/env python3
"""Static contracts for S12.7 Home poster scaling."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "app/src/main/java/app/nudroidlabs/nustrim/tv/home"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


rows = read(HOME / "TvHomeRows.kt")
screen = read(HOME / "TvHomeScreen.kt")
build = read(ROOT / "app/build.gradle.kts")
marker = read(ROOT / ".nustrim-tv")

checks = {
    "poster width follows locked Nuvio default": "internal val HOME_POSTER_WIDTH = 126.dp" in rows,
    "poster height follows locked Nuvio default": "internal val HOME_POSTER_HEIGHT = 189.dp" in rows,
    "poster column uses shared width": ".width(HOME_POSTER_WIDTH)" in rows,
    "poster image uses shared dimensions": (
        ".size(width = HOME_POSTER_WIDTH, height = HOME_POSTER_HEIGHT)" in rows
    ),
    "loading skeleton uses shared dimensions": (
        ".size(width = HOME_POSTER_WIDTH, height = HOME_POSTER_HEIGHT)" in screen
    ),
    "old oversized width removed from Home": "146.dp" not in rows and "146.dp" not in screen,
    "old oversized height removed from Home": "214.dp" not in rows and "214.dp" not in screen,
    "horizontal card spacing retained": "Arrangement.spacedBy(16.dp)" in rows,
    "poster focus anchor retained": ".tvFocusAnchor(anchor)" in rows,
    "poster focus memory retained": "focusRegistry.rememberHomePosition(" in rows,
    "exact Home focus restoration retained": "TvFocusRestoreEffect(" in screen,
    "target version name": (
        'versionName = "0.57.6-tv-cleanroom-s12.7-home-poster-scale"' in build
    ),
    "target version code": "versionCode = 129" in build,
    "stage marker": "subsystem=12.7-home-poster-scale" in marker,
    "poster size marker": "home-poster-size=126x189dp" in marker,
    "locked reference marker": "home-poster-reference=nuvio-default" in marker,
    "device acceptance remains explicit": "backlog=device-parity-acceptance" in marker,
}

failed = [name for name, passed in checks.items() if not passed]
if failed:
    raise SystemExit("S12.7 Home poster invariant failed: " + ", ".join(failed))

print(f"S12.7 Home poster invariants passed: {len(checks)}/{len(checks)}")
