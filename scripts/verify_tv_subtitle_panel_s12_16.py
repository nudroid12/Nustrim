#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]


def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


checks: list[tuple[str, bool]] = []


def require(name: str, condition: bool) -> None:
    checks.append((name, condition))


panels = read("app/src/main/java/app/nudroidlabs/nustrim/tv/player/TvPlayerPanels.kt")
screen = read("app/src/main/java/app/nudroidlabs/nustrim/tv/player/TvPlayerScreen.kt")
runtime = read("app/src/main/java/app/nudroidlabs/nustrim/tv/player/TvPlayerRuntime.kt")
gradle = read("app/build.gradle.kts")
metadata = read(".nustrim-tv")

require(
    "target version",
    (
        'versionName = "0.57.15-tv-cleanroom-s12.16-subtitle-panel-polish"' in gradle
        and "versionCode = 138" in gradle
    )
    or (
        'versionName = "0.57.16-tv-cleanroom-s12.17-episode-titles"' in gradle
        and "versionCode = 139" in gradle
    )
    or (
        'versionName = "0.57.17-tv-cleanroom-s12.18-sources-live-tabs"' in gradle
        and "versionCode = 140" in gradle
    )
    or (
        'versionName = "0.57.18-tv-cleanroom-s12.19-cloudstream-speed"' in gradle
        and "versionCode = 141" in gradle
    )
    or (
        'versionName = "0.57.19-tv-cleanroom-s12.20-cloudstream-tv"' in gradle
        and "versionCode = 142" in gradle
    )
    or (
        'versionName = "0.57.20-tv-cleanroom-s12.21-subtitle-language-split"' in gradle
        and "versionCode = 143" in gradle
    )
    or (
        'versionName = "0.57.21-tv-cleanroom-s12.22-branding"' in gradle
        and "versionCode = 144" in gradle
    )
    or (
        'versionName = "0.57.22-tv-cleanroom-s12.23-player-episodes"' in gradle
        and "versionCode = 145" in gradle
    )
    or (
        'versionName = "0.57.23-tv-cleanroom-s12.24-sidebar-clean"' in gradle
        and "versionCode = 146" in gradle
    )
    or (
        'versionName = "0.57.24-tv-cleanroom-s12.25-cloudstream-links-fix"' in gradle
        and "versionCode = 147" in gradle
    )
    or (
        'versionName = "0.57.25-tv-cleanroom-s12.26-tv-integrations"' in gradle
        and "versionCode = 148" in gradle
    ),
)
require("safe subtitle panel width", 'width = 860.dp' in panels and 'width = 1_040.dp' not in panels)
require("strong subtitle scrim", "strongScrim = true" in panels and "Color.Black.copy(alpha = 0.95f)" in panels)
require("all languages scroll", "items(languages" in panels and "languages.take(" not in panels)
require("compact subtitle rows", "compact = true" in panels and "vertical = if (compact) 9.dp" in panels)
require(
    "single text size control",
    "TvSubtitleSizeControl" in panels
    and 'title = "Text size +"' not in panels
    and 'title = "Text size -"' not in panels,
)
require(
    "friendly ISO language labels",
    "subtitleTrackTitle" in panels
    and (
        ('"msa", "may" -> "ms"' in panels and '"ind" -> "id"' in panels)
        or "TvSubtitleLanguage.canonicalCode(raw)" in panels
    ),
)
require("track provider and language shown", "subtitleTrackDescription" in panels and "track.provider" in panels)
require(
    "rendered subtitles hidden only while panel is open",
    "activePanel == TvPlayerPanel.SUBTITLES" in screen
    and "View.INVISIBLE" in screen
    and "View.VISIBLE" in screen,
)
require("subtitle selection engine preserved", "runtime.selectSubtitle(track)" in screen and "selectSubtitle" in runtime)
require("S12.15 Settings preserved", "settings-content-manager=addons,catalog-order" in metadata)
require("S12.14 sidebar preserved", "sidebar-animation=single-transition" in metadata)

failed = [name for name, ok in checks if not ok]
for name, ok in checks:
    print(f"{'PASS' if ok else 'FAIL'} {name}")
print(f"RESULT {len(checks) - len(failed)}/{len(checks)}")
if failed:
    sys.exit(1)
