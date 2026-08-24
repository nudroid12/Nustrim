#!/usr/bin/env python3
"""Static contracts for S12.6 TV updater focus retention."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SETTINGS = ROOT / "app/src/main/java/app/nudroidlabs/nustrim/tv/settings"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


entry = read(SETTINGS / "TvSettingsEntry.kt")
screen = read(SETTINGS / "TvSettingsScreen.kt")
updater = read(ROOT / "app/src/main/java/app/nudroidlabs/nustrim/core/update/AppUpdater.kt")
manifest = read(ROOT / "app/src/main/AndroidManifest.xml")
build = read(ROOT / "app/build.gradle.kts")
marker = read(ROOT / ".nustrim-tv")

checks = {
    "update row remains focusable while busy": (
        'value = updateValue(updateState),\n'
        '                            enabled = true,\n'
        '                            loading = updateState is TvSettingsUpdateState.Checking ||\n'
        '                                updateState is TvSettingsUpdateState.Downloading,'
        in screen
    ),
    "busy actions remain no-op": (
        "TvSettingsUpdateState.Checking," in entry
        and "is TvSettingsUpdateState.Downloading -> Unit" in entry
    ),
    "update row keeps stable item key": 'item("about-update")' in screen,
    "update row keeps stable focus anchor": (
        "anchorKey = settingsFirstDetailAnchorKey(category)" in screen
    ),
    "available state downloads on second action": (
        "is TvSettingsUpdateState.Available ->" in entry
        and "updater.download(state.info)" in entry
    ),
    "download completion enters install flow": (
        "onSuccess = { apk -> installDownloaded(state.info, apk.absolutePath) }" in entry
    ),
    "permission return resumes installation": (
        "Lifecycle.Event.ON_RESUME" in entry
        and "updater.canRequestPackageInstall()" in entry
    ),
    "installer uses FileProvider": "FileProvider.getUriForFile" in updater,
    "manifest permits package installation": (
        "android.permission.REQUEST_INSTALL_PACKAGES" in manifest
    ),
    "target version": (
        'versionName = "0.57.5-tv-cleanroom-s12.6-updater-focus"' in build
    ),
    "target version code": "versionCode = 128" in build,
    "stage marker": "subsystem=12.6-updater-focus" in marker,
    "focus retention marker": (
        "settings-update-focus=retained-during-check-and-download" in marker
    ),
    "device acceptance remains explicit": (
        "backlog=device-parity-acceptance" in marker
    ),
}

failed = [name for name, passed in checks.items() if not passed]
if failed:
    raise SystemExit("S12.6 TV updater focus invariant failed: " + ", ".join(failed))

print(f"S12.6 TV updater focus invariants passed: {len(checks)}/{len(checks)}")
