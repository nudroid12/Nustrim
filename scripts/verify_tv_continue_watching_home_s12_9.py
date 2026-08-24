#!/usr/bin/env python3
"""Static contracts for S12.9 Continue Watching Home placement."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
TV = ROOT / "app/src/main/java/app/nudroidlabs/nustrim/tv"
CORE = ROOT / "app/src/main/java/app/nudroidlabs/nustrim/core"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


repository = read(TV / "home/TvHomeRepository.kt")
models = read(TV / "home/TvHomeModels.kt")
rows = read(TV / "home/TvHomeRows.kt")
shell = read(TV / "shell/TvShell.kt")
library_models = read(TV / "library/TvLibraryModels.kt")
library_entry = read(TV / "library/TvLibraryEntry.kt")
local_store = read(CORE / "library/LocalMediaStore.kt")
build = read(ROOT / "app/build.gradle.kts")
marker = read(ROOT / ".nustrim-tv")

first_row_contract = (
    "val rows = buildList {\n"
    "            continueRow?.let(::add)\n"
    "            addAll(catalogRows)\n"
) in repository

checks = {
    "Home reads the existing local media store": "LocalMediaStore(appContext)" in repository,
    "Home loads Continue Watching state": "mediaStore.continueWatching()" in repository,
    "invalid local entries are excluded": (
        ".filter { it.sourceUrl.isNotBlank() && it.title.isNotBlank() }" in repository
    ),
    "Continue Watching is prepended before catalogues": first_row_contract,
    "empty Continue Watching row is omitted": "if (items.isEmpty()) return null" in repository,
    "Home works with Continue Watching and no providers": (
        "rows = listOfNotNull(continueRow)" in repository
    ),
    "Continue Watching cache follows progress changes": (
        "cachedContinueSignature == continueSignature" in repository
        and "entry.positionMs" in repository
        and "entry.updatedAt" in repository
    ),
    "Home media retains its local resume entry": "val continueEntry: LocalMediaEntry? = null" in models,
    "Continue Watching uses landscape cards": (
        "HOME_CONTINUE_WIDTH = 210.dp" in rows
        and "HOME_CONTINUE_HEIGHT = 118.dp" in rows
    ),
    "Continue Watching card has progress rail": (
        "continueEntry.progressFraction" in rows
        and ".fillMaxWidth(continueEntry.progressFraction.coerceIn(0f, 1f))" in rows
    ),
    "Continue Watching card identifies Next Up and episodes": (
        '"Next up  •  $episode"' in rows
        and '"S${entry.season} E${entry.episode}"' in rows
    ),
    "catalogue poster dimensions remain unchanged": (
        "HOME_POSTER_WIDTH = 126.dp" in rows
        and "HOME_POSTER_HEIGHT = 189.dp" in rows
    ),
    "Continue Watching opens a Details-backed resume stack": (
        "navigator.push(details)" in shell
        and "val continueEntry = media.continueEntry" in shell
        and "episode = continueEntry.toEpisode()" in shell
    ),
    "Library keeps Saved": 'SAVED("Saved")' in library_models,
    "Library no longer declares Continue Watching": "CONTINUE_WATCHING" not in library_models,
    "Library loads Saved only": "store.saved().map" in library_entry,
    "local progress ordering remains most recent first": (
        "fun continueWatching(): List<LocalMediaEntry>" in local_store
        and ".sortedByDescending { it.updatedAt }" in local_store
    ),
    "target version name": (
        'versionName = "0.57.8-tv-cleanroom-s12.9-continue-watching-home"' in build
    ),
    "target version code": "versionCode = 131" in build,
    "stage marker": "subsystem=12.9-continue-watching-home" in marker,
    "Home placement marker": "home-continue-watching=first-row" in marker,
    "Library marker is Saved only": "library-sections=saved\n" in marker,
    "device acceptance remains explicit": "backlog=device-parity-acceptance" in marker,
}

failed = [name for name, passed in checks.items() if not passed]
if failed:
    raise SystemExit("S12.9 Continue Watching invariant failed: " + ", ".join(failed))

print(f"S12.9 Continue Watching invariants passed: {len(checks)}/{len(checks)}")
