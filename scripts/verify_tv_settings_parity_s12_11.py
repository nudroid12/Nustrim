#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]


def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


checks = []


def require(name: str, condition: bool) -> None:
    checks.append((name, condition))


entry = read("app/src/main/java/app/nudroidlabs/nustrim/tv/settings/TvSettingsEntry.kt")
screen = read("app/src/main/java/app/nudroidlabs/nustrim/tv/settings/TvSettingsScreen.kt")
models = read("app/src/main/java/app/nudroidlabs/nustrim/tv/settings/TvSettingsModels.kt")
dialog = read("app/src/main/java/app/nudroidlabs/nustrim/tv/settings/TvSettingsCredentialDialog.kt")
session = read("app/src/main/java/app/nudroidlabs/nustrim/tv/settings/TvSettingsSessionStore.kt")
preferences = read("app/src/main/java/app/nudroidlabs/nustrim/ui/UiPreferences.kt")
shell = read("app/src/main/java/app/nudroidlabs/nustrim/tv/shell/TvShell.kt")
app = read("app/src/main/java/app/nudroidlabs/nustrim/ui/NustrimApp.kt")
metadata = read(".nustrim-tv")
gradle = read("app/build.gradle.kts")

require("target version name", 'versionName = "0.57.15-tv-cleanroom-s12.16-subtitle-panel-polish"' in gradle)
require("target version code", "versionCode = 138" in gradle)
category_block = models.split("enum class TvSettingsCategory", 1)[1].split("}", 1)[0]
require("seven TV categories", len(re.findall(r'^    [A-Z_]+\("', category_block, re.MULTILINE)) == 7)
require(
    "Content Manager contains add-ons and catalogue order",
    'CONTENT("Content Manager"' in models
    and "enum class TvContentManagerSection" in models
    and 'ADDONS("Add-ons")' in models
    and 'CATALOG_ORDER("Catalogue order")' in models
    and "contentManagerSection" in session
    and "ContentManagerTabs" in screen,
)
require("local data category", "LOCAL_DATA" in models and "TvSettingsCategory.LOCAL_DATA" in screen)
require("complete subtitle languages", all(code in models for code in ['"th"', '"es"', '"fr"', '"de"']))
require("subtitle font size shared", "var subtitleFontSize" in preferences and "onIncreaseSubtitleFontSize" in entry)
require("subtitle bold shared", "var subtitleBold" in preferences and "onToggleSubtitleBold" in entry)
require("source toggles preserved", "sourceStore.setEnabled" in entry)
require(
    "add-on URL is first add-on row and validated",
    screen.index('item("content-add-url")') < screen.index('items(snapshot.sources')
    and "TvSettingsEditor.Addon" in entry
    and "sourceEngine.open" in entry
    and "sourceStore.add(first)" in entry
    and "Manifest or repository URL" in dialog,
)
require("catalogs load in parallel", "urls.forEach" in entry and "sourceEngine.open" in entry)
require("catalog order", "preferences.catalogOrder = keys" in entry)
require("catalog visibility", "preferences.setCatalogHidden" in entry)
require("catalog reset", "preferences.resetCatalogLayout" in entry)
require("catalog order nested under Content Manager", "TvContentManagerSection.CATALOG_ORDER" in screen)
require("TMDB editor", "TvSettingsEditor.Tmdb" in entry and "TmdbClient.validate" in entry)
require("MDBList editor", "TvSettingsEditor.MdbList" in entry and "MdbListClient.validate" in entry)
require("MDBList providers", "TV_MDBLIST_PROVIDERS" in models and "setMdbListProviderEnabled" in entry)
require("Trakt credentials", "TvSettingsEditor.Trakt" in entry)
require("Trakt device auth", "TraktClient.createDeviceCode" in entry and "TraktClient.pollDeviceToken" in entry)
require("credential keyboard dialog", "OutlinedTextField" in dialog and "FocusRequester" in dialog)
require("credentials masked", "PasswordVisualTransformation" in dialog)
require("backup excludes API keys", "Integration API keys are excluded" in entry)
require("backup sources", "sourceStore.exportJson" in entry and "sourceStore.importJson" in entry)
require("backup library", "mediaStore.exportJson" in entry and "mediaStore.importJson" in entry)
require("diagnostics copy", "NustrimDiagnostics.snapshotText" in entry)
require("diagnostics clear", "NustrimDiagnostics.clear" in entry)
require(
    "TV to mobile callback",
    shell.count("onSwitchToMobile: () -> Unit") == 2
    and shell.count("onSwitchToMobile = onSwitchToMobile") == 2
    and "InterfaceMode.MOBILE" in app,
)
require("D-pad action activation", "Key.DirectionCenter" in screen and "Key.Enter" in screen)
require("focus restoration preserved", "TvFocusRestoreEffect" in screen and "tvFocusAnchor" in screen)
require("updater preserved", "updater.check()" in entry and "updater.download" in entry)
require("metadata scope", "settings=cleanroom-s12.11-mobile-feature-parity" in metadata)
require("downloads not faked", "Downloads" not in screen)
require("remote test not exposed", "Remote Test" not in screen)

failed = [name for name, ok in checks if not ok]
for name, ok in checks:
    print(f"{'PASS' if ok else 'FAIL'} {name}")
print(f"RESULT {len(checks) - len(failed)}/{len(checks)}")
if failed:
    sys.exit(1)
