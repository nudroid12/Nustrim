#!/usr/bin/env python3
"""Verify the Nustrim TV S11 static parity contract.

This verifier checks source-level evidence only. It deliberately does not claim
rendered visual parity, remote-control ergonomics, playback quality, or device
performance. Those require the manual device checklist in the S11 audit report.
"""

from __future__ import annotations

import argparse
import json
from dataclasses import asdict, dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
TV = ROOT / "app/src/main/java/app/nudroidlabs/nustrim/tv"


@dataclass(frozen=True)
class Check:
    surface: str
    contract: str
    passed: bool
    evidence: str


def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


def has(text: str, *tokens: str) -> bool:
    return all(token in text for token in tokens)


def check(surface: str, contract: str, passed: bool, evidence: str) -> Check:
    return Check(surface=surface, contract=contract, passed=passed, evidence=evidence)


def build_checks() -> list[Check]:
    route = read("app/src/main/java/app/nudroidlabs/nustrim/tv/navigation/TvRoute.kt")
    back = read("app/src/main/java/app/nudroidlabs/nustrim/tv/navigation/TvBackPolicy.kt")
    shell = read("app/src/main/java/app/nudroidlabs/nustrim/tv/shell/TvShell.kt")
    sidebar = read("app/src/main/java/app/nudroidlabs/nustrim/tv/shell/TvSidebar.kt")
    focus_registry = read("app/src/main/java/app/nudroidlabs/nustrim/tv/focus/TvFocusRegistry.kt")
    focus_restore = read("app/src/main/java/app/nudroidlabs/nustrim/tv/focus/TvFocusRestoreEffect.kt")
    theme = read("app/src/main/java/app/nudroidlabs/nustrim/tv/theme/NustrimTvTheme.kt")
    tokens = read("app/src/main/java/app/nudroidlabs/nustrim/tv/theme/TvTokens.kt")
    focus_motion = read("app/src/main/java/app/nudroidlabs/nustrim/tv/theme/TvFocusMotion.kt")

    home_screen = read("app/src/main/java/app/nudroidlabs/nustrim/tv/home/TvHomeScreen.kt")
    home_hero = read("app/src/main/java/app/nudroidlabs/nustrim/tv/home/TvHomeHero.kt")
    home_rows = read("app/src/main/java/app/nudroidlabs/nustrim/tv/home/TvHomeRows.kt")
    home_repo = read("app/src/main/java/app/nudroidlabs/nustrim/tv/home/TvHomeRepository.kt")

    search_entry = read("app/src/main/java/app/nudroidlabs/nustrim/tv/search/TvSearchEntry.kt")
    search_screen = read("app/src/main/java/app/nudroidlabs/nustrim/tv/search/TvSearchScreen.kt")
    search_rows = read("app/src/main/java/app/nudroidlabs/nustrim/tv/search/TvSearchRows.kt")
    search_history = read("app/src/main/java/app/nudroidlabs/nustrim/tv/search/TvSearchHistoryStore.kt")

    library_entry = read("app/src/main/java/app/nudroidlabs/nustrim/tv/library/TvLibraryEntry.kt")
    library_models = read("app/src/main/java/app/nudroidlabs/nustrim/tv/library/TvLibraryModels.kt")
    library_screen = read("app/src/main/java/app/nudroidlabs/nustrim/tv/library/TvLibraryScreen.kt")

    settings_entry = read("app/src/main/java/app/nudroidlabs/nustrim/tv/settings/TvSettingsEntry.kt")
    settings_models = read("app/src/main/java/app/nudroidlabs/nustrim/tv/settings/TvSettingsModels.kt")
    settings_screen = read("app/src/main/java/app/nudroidlabs/nustrim/tv/settings/TvSettingsScreen.kt")

    details_entry = read("app/src/main/java/app/nudroidlabs/nustrim/tv/details/TvDetailsEntry.kt")
    details_screen = read("app/src/main/java/app/nudroidlabs/nustrim/tv/details/TvDetailsScreen.kt")
    episode_builder = read("app/src/main/java/app/nudroidlabs/nustrim/tv/episode/TvEpisodeCatalogueBuilder.kt")

    sources_entry = read("app/src/main/java/app/nudroidlabs/nustrim/tv/sources/TvSourcesEntry.kt")
    sources_screen = read("app/src/main/java/app/nudroidlabs/nustrim/tv/sources/TvSourcesScreen.kt")
    sources_models = read("app/src/main/java/app/nudroidlabs/nustrim/tv/sources/TvSourcesModels.kt")

    player_entry = read("app/src/main/java/app/nudroidlabs/nustrim/tv/player/TvPlayerEntry.kt")
    player_runtime = read("app/src/main/java/app/nudroidlabs/nustrim/tv/player/TvPlayerRuntime.kt")
    player_screen = read("app/src/main/java/app/nudroidlabs/nustrim/tv/player/TvPlayerScreen.kt")
    player_controls = read("app/src/main/java/app/nudroidlabs/nustrim/tv/player/TvPlayerControls.kt")
    player_panels = read("app/src/main/java/app/nudroidlabs/nustrim/tv/player/TvPlayerPanels.kt")

    checks = [
        check("Shell", "Four product root destinations", has(route, "HOME", "SEARCH", "LIBRARY", "SETTINGS"), "TvRoute.kt"),
        check("Shell", "Collapsed and expanded sidebar", has(sidebar, "SidebarCollapsedWidth", "SidebarExpandedWidth", "expanded"), "TvSidebar.kt"),
        check("Shell", "DPAD opens and closes navigation", has(shell, "Key.DirectionLeft", "onOpenSidebar") and has(sidebar, "Key.DirectionRight", "onCloseToContent"), "TvShell.kt, TvSidebar.kt"),
        check("Shell", "Back hierarchy is deterministic", has(back, "POP_ROUTE", "OPEN_SIDEBAR", "EXIT_APP"), "TvBackPolicy.kt"),
        check("Shell", "Shared theme and bounded focus restore", has(theme, "TvColors", "TvTypography") and has(focus_motion, "animateTvFocusScale") and has(focus_restore, "FocusRestoreAttempts") and has(focus_registry, "candidates.any"), "theme and focus packages"),

        check("Home", "Hero backdrop and metadata", has(home_hero, "AsyncImage", "Brush.horizontalGradient", "HeroMetadata"), "TvHomeHero.kt"),
        check("Home", "Vertical catalogue with horizontal rails", has(home_rows, "LazyColumn", "LazyRow", "TvHomePosterCard"), "TvHomeRows.kt"),
        check("Home", "Loading, empty and error states", has(home_screen, "TvHomeLoading", "TvHomeUiState.Empty", "TvHomeUiState.Error", "Retry"), "TvHomeScreen.kt"),
        check("Home", "Exact row and card focus restoration", has(home_screen, "homeRowIndex", "TvFocusRestoreEffect") and has(home_rows, "rememberHomePosition", "rowItemIndex", "homeAnchorKey"), "TvHomeScreen.kt, TvHomeRows.kt"),
        check("Home", "Catalogue cards open Details and expose hold actions", has(home_repo, "catalog") and has(shell, "TvHomeEntry", "TvRoute.Details") and has(home_rows, "rememberTvHomeLongPressTracker", "onLongPress") and has(home_screen, "TvHomePosterActionsDialog"), "TvHomeRepository.kt, TvHomeRows.kt, TvHomeScreen.kt, TvShell.kt"),

        check("Search", "TV search input with bounded query", has(search_entry, "MIN_SEARCH_QUERY_LENGTH = 2", "SEARCH_DEBOUNCE_MS = 350L") and has(search_screen, "TvSearchInput"), "TvSearchEntry.kt, TvSearchScreen.kt"),
        check("Search", "Recent searches and clear history", has(search_screen, "TvRecentSearches", "SEARCH_CLEAR_HISTORY_ANCHOR", "onClearHistory") and has(search_history, "clear"), "TvSearchScreen.kt, TvSearchHistoryStore.kt"),
        check("Search", "Discover catalogue before query", has(search_entry, "DiscoverLoading", "Discover") and has(search_screen, "Loading Discover"), "TvSearchEntry.kt, TvSearchScreen.kt"),
        check("Search", "Loading, empty and error states", has(search_screen, "TvSearchLoading", "TvSearchUiState.Empty", "TvSearchUiState.Error", "TvSearchMessage"), "TvSearchScreen.kt"),
        check("Search", "Exact result focus and Details route", has(search_rows, "searchCardAnchorKey", "rememberTvFocusAnchor") and has(shell, "TvSearchEntry", "focusRegistry.lastFocused", "TvRoute.Details"), "TvSearchRows.kt, TvShell.kt"),

        check(
            "Library",
            "Continue Watching is first on Home and Library keeps Saved",
            has(home_repo, "mediaStore.continueWatching()", "continueRow?.let(::add)", "addAll(catalogRows)")
            and has(library_models, "SAVED")
            and "CONTINUE_WATCHING" not in library_models,
            "TvHomeRepository.kt, TvLibraryModels.kt",
        ),
        check("Library", "Media, watched and sort filters", has(library_models, "TvLibraryTypeFilter", "TvLibraryWatchedFilter", "TvLibrarySort") and has(library_entry, "filteredAndSorted"), "TvLibraryModels.kt, TvLibraryEntry.kt"),
        check("Library", "Poster grid presentation", has(library_screen, "LazyVerticalGrid", "LibraryPosterCard"), "TvLibraryScreen.kt"),
        check("Library", "Empty-state guidance", has(library_screen, "LibraryEmptyState", "Bookmark"), "TvLibraryScreen.kt"),
        check("Library", "Exact card focus and Details route", has(library_screen, "libraryMediaAnchorKey", "TvFocusRestoreEffect") and has(shell, "TvLibraryEntry", "focusRegistry.lastFocused", "TvRoute.Details"), "TvLibraryScreen.kt, TvShell.kt"),

        check("Settings", "Category rail and detail pane", has(settings_screen, "SettingsRail", "SettingsDetailPane", "LazyColumn"), "TvSettingsScreen.kt"),
        check("Settings", "Content Manager and catalogue order are separate", has(settings_models, "PLAYBACK", "SUBTITLES", "CONTENT", "CATALOG_ORDER", "INTEGRATIONS", "LOCAL_DATA", "ADVANCED", "ABOUT"), "TvSettingsModels.kt"),
        check("Settings", "Functional playback and subtitle preferences", has(settings_entry, "autoplayFirstSource", "autoplayNextEpisode", "tvSeekStepSeconds", "subtitlePreferredLanguage", "subtitleDisplayMode"), "TvSettingsEntry.kt"),
        check("Settings", "Source management and signed updater", has(settings_entry, "InstalledSourceStore", "sourceStore.setEnabled", "AppUpdater", "updater.check()"), "TvSettingsEntry.kt"),
        check("Settings", "Category and detail focus restoration", has(settings_screen, "settingsRailAnchorKey", "settingsFirstDetailAnchorKey", "TvFocusRestoreEffect"), "TvSettingsScreen.kt"),

        check("Details", "Backdrop hero and metadata", has(details_screen, "DetailsBackdrop", "DetailsHero", "MetaLine"), "TvDetailsScreen.kt"),
        check("Details", "Play and local save actions", has(details_screen, "PlayButton", "SaveButton", "onToggleSaved", "onPlayMovie"), "TvDetailsScreen.kt"),
        check("Details", "Canonical seasons and episodes", has(details_screen, "SeasonTabs", "EpisodeRow", "EpisodeCard") and has(episode_builder, "TvEpisodeCatalogue"), "TvDetailsScreen.kt, TvEpisodeCatalogueBuilder.kt"),
        check("Details", "Loading and retryable error states", has(details_screen, "DetailsLoading", "DetailsError", "Retry"), "TvDetailsScreen.kt"),
        check("Details", "Focus memory and Sources route", has(details_screen, "seasonAnchorKey", "episodeAnchorKey", "TvFocusRestoreEffect") and has(details_entry, "onPlayEpisode") and has(shell, "TvRoute.Sources"), "Details package, TvShell.kt"),

        check("Sources", "Backdrop identity and right stream pane", has(sources_screen, "SourcesBackdrop", "SourcesIdentity", "SourcesRightPane"), "TvSourcesScreen.kt"),
        check("Sources", "All and per-source filtering", has(sources_screen, "SourceFilterRow", "SourceChip", "selectedSource", "filtered"), "TvSourcesScreen.kt"),
        check("Sources", "Loading, empty and retryable error states", has(sources_screen, "SourcesLoading", "SourcesEmpty", "SourcesError", "Retry"), "TvSourcesScreen.kt"),
        check("Sources", "Exact chip and stream focus restoration", has(sources_screen, "sourceChipAnchorKey", "streamAnchorKey", "TvFocusRestoreEffect", "rememberedStreamKey"), "TvSourcesScreen.kt"),
        check("Sources", "Playable stream opens Player", has(sources_models, "playable") and has(sources_entry, "onStreamSelected") and has(shell, "TvRoute.Player"), "Sources package, TvShell.kt"),

        check("Player", "Real Media3 playback surface", has(player_runtime, "PlayerFactory.create(") and has(player_screen, "PlayerView(context)"), "TvPlayerRuntime.kt, TvPlayerScreen.kt"),
        check("Player", "Transport and seek controls", has(player_controls, "Play", "Pause", "TvPlayerProgressBar", "seekBy"), "TvPlayerControls.kt"),
        check("Player", "Episodes, sources, audio and subtitle panels", has(player_screen, "TvPlayerPanel.EPISODES", "TvPlayerPanel.SOURCES", "TvPlayerPanel.AUDIO", "TvPlayerPanel.SUBTITLES") and has(player_panels, "TvPlayerEpisodesPanel", "TvPlayerSourcesPanel", "TvPlayerAudioPanel", "TvPlayerSubtitlePanel"), "TvPlayerScreen.kt, TvPlayerPanels.kt"),
        check("Player", "Loading, pause, error and post-play overlays", has(player_screen, "TvPlayerLoadingOverlay", "TvPauseOverlay", "TvPlayerErrorOverlay", "TvPostPlayOverlay"), "TvPlayerScreen.kt"),
        check("Player", "Episode and source switching preserve Nustrim engines", has(player_entry, "onSwitchSource", "onEpisodeSelected") and has(player_runtime, "selectAudio", "selectSubtitle", "setSpeed"), "TvPlayerEntry.kt, TvPlayerRuntime.kt"),
    ]
    return checks


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--json", type=Path, help="Write the machine-readable report")
    parser.add_argument("--minimum-percent", type=float, default=95.0)
    args = parser.parse_args()

    checks = build_checks()
    passed = sum(item.passed for item in checks)
    total = len(checks)
    percent = passed * 100.0 / total if total else 0.0

    for item in checks:
        state = "PASS" if item.passed else "FAIL"
        print(f"{state}: {item.surface}: {item.contract} [{item.evidence}]")
    print(f"STATIC_CONTRACT_SCORE={passed}/{total} ({percent:.1f}%)")
    print("DEVICE_VISUAL_PARITY=NOT_EVALUATED")

    report = {
        "schemaVersion": 1,
        "stage": "S11",
        "reference": {
            "name": "Nuvio TV 0.8.6-beta",
            "commit": "082af4e29f4629873c185360638940ea42ba988e",
        },
        "scope": ["Shell", "Home", "Search", "Library", "Settings", "Details", "Sources", "Player"],
        "staticContract": {
            "passed": passed,
            "total": total,
            "percent": round(percent, 1),
            "minimumPercent": args.minimum_percent,
        },
        "deviceVisualParity": "not-evaluated",
        "checks": [asdict(item) for item in checks],
    }
    if args.json:
        args.json.parent.mkdir(parents=True, exist_ok=True)
        args.json.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")

    return 0 if percent >= args.minimum_percent else 1


if __name__ == "__main__":
    raise SystemExit(main())
