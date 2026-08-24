#!/usr/bin/env python3
"""Static contracts for the S12.8 repository cleanup."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]

OBSOLETE_WORKFLOWS = {
    "nustrim-clean-bootstrap-rc27.yml",
    "nustrim-tv-cleanroom-s1-v3.yml",
    "nustrim-tv-cleanroom-s10-global-polish.yml",
    "nustrim-tv-cleanroom-s11-1-subtitle-system-fixed-v2.yml",
    "nustrim-tv-cleanroom-s11-1-subtitle-system-fixed.yml",
    "nustrim-tv-cleanroom-s11-1-subtitle-system.yml",
    "nustrim-tv-cleanroom-s11-parity-qa.yml",
    "nustrim-tv-cleanroom-s12-1-sources-progressive.yml",
    "nustrim-tv-cleanroom-s12-2-details-episode-focus.yml",
    "nustrim-tv-cleanroom-s12-3-player-postplay.yml",
    "nustrim-tv-cleanroom-s12-4-subtitle-updater.yml",
    "nustrim-tv-cleanroom-s12-5-player-final-polish.yml",
    "nustrim-tv-cleanroom-s12-6-updater-focus.yml",
    "nustrim-tv-cleanroom-s12-7-home-poster-scale.yml",
    "nustrim-tv-cleanroom-s2-home-v2.yml",
    "nustrim-tv-cleanroom-s2-home.yml",
    "nustrim-tv-cleanroom-s3-episodes-v2.yml",
    "nustrim-tv-cleanroom-s3-episodes.yml",
    "nustrim-tv-cleanroom-s4-details.yml",
    "nustrim-tv-cleanroom-s4.1-episode-title-fix-v2.yml",
    "nustrim-tv-cleanroom-s4.1-episode-title-fix.yml",
    "nustrim-tv-cleanroom-s4.2-season-focus.yml",
    "nustrim-tv-cleanroom-s4.3-exact-episode-return.yml",
    "nustrim-tv-cleanroom-s5-sources-v2.yml",
    "nustrim-tv-cleanroom-s5-sources.yml",
    "nustrim-tv-cleanroom-s5.1-stream-resolution-v2.yml",
    "nustrim-tv-cleanroom-s5.1-stream-resolution.yml",
    "nustrim-tv-cleanroom-s6-player-v2.yml",
    "nustrim-tv-cleanroom-s6-player.yml",
    "nustrim-tv-cleanroom-s6.1-player-parity.yml",
    "nustrim-tv-cleanroom-s6.2-player-final-polish.yml",
    "nustrim-tv-cleanroom-s6.3-player-qa.yml",
    "nustrim-tv-cleanroom-s6.3-rollback-to-s6.2.yml",
    "nustrim-tv-cleanroom-s7-search.yml",
    "nustrim-tv-cleanroom-s8-library.yml",
    "nustrim-tv-cleanroom-s9-settings.yml",
}

OBSOLETE_SCRIPTS = {
    "verify_tv_details_episode_focus_s12_2.py",
    "verify_tv_home_poster_scale_s12_7.py",
    "verify_tv_player_final_polish_s12_5.py",
    "verify_tv_player_postplay_s12_3.py",
    "verify_tv_sources_progressive_s12_1.py",
    "verify_tv_subtitle_updater_s12_4.py",
    "verify_tv_updater_focus_s12_6.py",
}

OBSOLETE_KOTLIN = {
    "app/src/main/java/app/nudroidlabs/nustrim/core/model/EpisodeEngine.kt",
    "app/src/main/java/app/nudroidlabs/nustrim/core/provider/MediaProvider.kt",
    "app/src/main/java/app/nudroidlabs/nustrim/core/recommendation/MoreLikeThisRepository.kt",
    "app/src/main/java/app/nudroidlabs/nustrim/core/repository/JsonRepositoryProvider.kt",
    "app/src/main/java/app/nudroidlabs/nustrim/core/source/StreamSourceAggregator.kt",
    "app/src/main/java/app/nudroidlabs/nustrim/core/source/SubtitleSourceAggregator.kt",
}

REMOVED_SYMBOLS = {
    "EpisodeEngine",
    "MediaProvider",
    "MoreLikeThisEngine",
    "MoreLikeThisRepository",
    "JsonRepositoryProvider",
    "StreamSourceAggregator",
    "SubtitleSourceAggregator",
}

workflow_dir = ROOT / ".github/workflows"
script_dir = ROOT / "scripts"
app_dir = ROOT / "app"
build = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
marker = (ROOT / ".nustrim-tv").read_text(encoding="utf-8")
app_text = "\n".join(
    path.read_text(encoding="utf-8")
    for path in app_dir.rglob("*.kt")
)

existing_workflows = {path.name for path in workflow_dir.glob("*.yml")}
existing_scripts = {path.name for path in script_dir.glob("*.py")}

checks = {
    "36 obsolete workflows removed": not (OBSOLETE_WORKFLOWS & existing_workflows),
    "primary build workflow retained": (workflow_dir / "build.yml").is_file(),
    "only current and primary workflows remain": existing_workflows <= {
        "build.yml",
        "nustrim-tv-cleanroom-s12-8-repo-cleanup.yml",
    },
    "7 superseded stage verifiers removed": not (OBSOLETE_SCRIPTS & existing_scripts),
    "parity regression verifier retained": (script_dir / "verify_tv_parity_s11.py").is_file(),
    "6 unreferenced Kotlin files removed": all(
        not (ROOT / relative).exists() for relative in OBSOLETE_KOTLIN
    ),
    "removed Kotlin symbols have no live source references": all(
        symbol not in app_text for symbol in REMOVED_SYMBOLS
    ),
    "current SourceEngine retained": "class SourceEngine" in app_text,
    "current Nustrim JSON session retained": "class NustrimJsonSession" in app_text,
    "current progressive TV Sources retained": "class TvSourcesRepository" in app_text,
    "current subtitle repository retained": "class TvSubtitleRepository" in app_text,
    "target version name": (
        'versionName = "0.57.7-tv-cleanroom-s12.8-repo-cleanup"' in build
    ),
    "target version code": "versionCode = 130" in build,
    "cleanup stage marker": "subsystem=12.8-repo-cleanup" in marker,
    "removed file count marker": "repo-cleanup-removed-files=49" in marker,
    "device acceptance backlog retained": "backlog=device-parity-acceptance" in marker,
}

failed = [name for name, passed in checks.items() if not passed]
if failed:
    raise SystemExit("S12.8 repository cleanup invariant failed: " + ", ".join(failed))

print(f"S12.8 repository cleanup invariants passed: {len(checks)}/{len(checks)}")
