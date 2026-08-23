#!/usr/bin/env python3
"""Static contract checks for S12.4 subtitle overlay and TV updater closure."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PLAYER = ROOT / "app/src/main/java/app/nudroidlabs/nustrim/tv/player"
SETTINGS = ROOT / "app/src/main/java/app/nudroidlabs/nustrim/tv/settings"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


entry = read(PLAYER / "TvPlayerEntry.kt")
models = read(PLAYER / "TvPlayerModels.kt")
panels = read(PLAYER / "TvPlayerPanels.kt")
runtime = read(PLAYER / "TvPlayerRuntime.kt")
screen = read(PLAYER / "TvPlayerScreen.kt")
subtitles = read(PLAYER / "TvSubtitleRepository.kt")
style = read(PLAYER / "TvSubtitleStyleStore.kt")
settings_entry = read(SETTINGS / "TvSettingsEntry.kt")
settings_models = read(SETTINGS / "TvSettingsModels.kt")
settings_screen = read(SETTINGS / "TvSettingsScreen.kt")
updater = read(ROOT / "app/src/main/java/app/nudroidlabs/nustrim/core/update/AppUpdater.kt")
build = read(ROOT / "app/build.gradle.kts")
marker = read(ROOT / ".nustrim-tv")

checks = {
    "player receives primary subtitle preference":
        "preferredSubtitleLanguage = preferences.subtitlePreferredLanguage" in entry,
    "player receives secondary subtitle preference":
        "secondPreferredSubtitleLanguage = preferences.subtitleSecondPreferredLanguage" in entry,
    "player receives subtitle visibility preference":
        "subtitleDisplayMode = preferences.subtitleDisplayMode" in entry,
    "track model keeps language code": "val languageCode: String" in models,
    "track model keeps provider": "val provider: String" in models,
    "runtime preserves provider label": "splitProviderLabel" in runtime and "provider = provider" in runtime,
    "repository keeps all discovered tracks":
        "Keep every discovered track attached to Media3" in subtitles,
    "repository still ranks preferred languages": "languageRank(subtitle, preference)" in subtitles,
    "subtitle panel supports preferred only":
        "displayMode == SubtitleDisplayMode.PREFERRED_ONLY" in panels,
    "subtitle panel has language rail": 'Text("Languages"' in panels,
    "subtitle panel has track rail": 'Text("Tracks"' in panels,
    "subtitle panel exposes provider": "subtitle = track.provider" in panels,
    "subtitle panel has style rail": 'Text("Style"' in panels,
    "subtitle font size persists": "subtitleStyleStore.fontSizeSp = next" in screen,
    "subtitle bold persists": "subtitleStyleStore.bold = next" in screen,
    "player view applies subtitle font size": "setFixedTextSize" in screen,
    "player view applies subtitle outline style": "CaptionStyleCompat.EDGE_TYPE_OUTLINE" in screen,
    "updater state includes download progress": "data class Downloading" in settings_models,
    "updater state includes install permission": "data class PermissionRequired" in settings_models,
    "TV updater downloads APK": "updater.download(state.info)" in settings_entry,
    "TV updater retries after permission":
        "Lifecycle.Event.ON_RESUME" in settings_entry and "updater.canRequestPackageInstall()" in settings_entry,
    "TV updater launches installer": "updater.install(apk)" in settings_entry,
    "TV updater verifies SHA-256": "MessageDigest.getInstance(\"SHA-256\")" in updater,
    "update UI reports digest verification": "verifying the APK SHA-256 digest" in settings_screen,
    "target version": 'versionName = "0.57.3-tv-cleanroom-s12.4-subtitle-updater"' in build,
    "target version code": "versionCode = 126" in build,
    "stage marker": "subsystem=12.4-subtitle-updater" in marker,
    "subtitle updater removed from backlog": "backlog=s6-player-final-polish" in marker,
}

failed = [name for name, passed in checks.items() if not passed]
if failed:
    raise SystemExit("S12.4 invariant failed: " + ", ".join(failed))

print(f"S12.4 subtitle overlay and updater invariants passed: {len(checks)}/{len(checks)}")
