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
sidebar = read("app/src/main/java/app/nudroidlabs/nustrim/tv/shell/TvSidebar.kt")

check("S12.24 marker", "subsystem=12.24-sidebar-clean" in marker)
check("target version", version == "0.57.23-tv-cleanroom-s12.24-sidebar-clean")
check("target version name", 'versionName = "0.57.23-tv-cleanroom-s12.24-sidebar-clean"' in gradle)
check("target version code", "versionCode = 146" in gradle)
check("sidebar branding removal marker", "branding-sidebar=removed-by-user-request" in marker)
check("sidebar does not reference brand drawable", "R.drawable.nustrim_brand_mark" not in sidebar)
check("sidebar does not render brand name", 'text = "NUSTRIM"' not in sidebar)
check("sidebar does not import image composable", "import androidx.compose.foundation.Image" not in sidebar)
check("sidebar does not import brand resources", "import app.nudroidlabs.nustrim.R" not in sidebar)
check("Home navigation remains", 'SidebarItem(TvRootDestination.HOME, "Home"' in sidebar)
check("Search navigation remains", 'SidebarItem(TvRootDestination.SEARCH, "Search"' in sidebar)
check("Library navigation remains", 'SidebarItem(TvRootDestination.LIBRARY, "Library"' in sidebar)
check("Settings navigation remains", 'SidebarItem(TvRootDestination.SETTINGS, "Settings"' in sidebar)
check("single sidebar transition remains", 'label = "tv-sidebar-transition"' in sidebar)
check("navigation label animation remains", ".alpha(labelAlpha)" in sidebar and ".offset(x = labelOffset)" in sidebar)
check("S12.23 Episodes fix preserved", "player-episodes-tab=visible-for-episode-playback" in marker)
check("launcher branding preserved", "branding-launcher=blue-cyan-n-mark-on-near-black" in marker)
check("TV banner branding preserved", "branding-tv-banner=approved-lockup" in marker)
check("launch screen branding preserved", "branding-launch-screen=approved-lockup" in marker)
check("Mobile header branding preserved", "branding-mobile-header=transparent-n-mark" in marker)

failed = [name for name, passed in checks if not passed]
for name, passed in checks:
    print(f"{'PASS' if passed else 'FAIL'}: {name}")

if failed:
    raise SystemExit(f"{len(failed)} of {len(checks)} checks failed: {', '.join(failed)}")

print(f"S12.24 clean sidebar contracts: {len(checks)}/{len(checks)} passed")
