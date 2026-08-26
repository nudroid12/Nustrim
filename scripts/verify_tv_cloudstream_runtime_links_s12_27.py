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
repository = read(
    "app/src/main/java/app/nudroidlabs/nustrim/tv/sources/TvSourcesRepository.kt"
)
provider = read(
    "app/src/main/java/app/nudroidlabs/nustrim/core/source/cloudstream/CloudStreamProviderSession.kt"
)

check(
    "S12.27 feature marker",
    "cloudstream-provider-entry=search-without-main-page-preflight" in marker,
)
check(
    "S12.27 version",
    version in {
        "0.57.26-tv-cleanroom-s12.27-cloudstream-runtime-links",
        "0.57.27-tv-cleanroom-s12.28-player-episode-focus",
    },
)
check("S12.27 versionCode", any(f"versionCode = {code}" in gradle for code in (149, 150)))
check(
    "loaded plugin containers bypass main page",
    "if (session is CloudStreamProviderContainerSession)" in repository
    and repository.index("if (session is CloudStreamProviderContainerSession)")
    < repository.index("val catalog = runCatching { session.awaitCatalog() }"),
)
check(
    "provider names open directly",
    "container.providerNames" in repository
    and "container.awaitProvider(providerName)" in repository,
)
check(
    "direct provider uses search path",
    "resolveLoadedPluginContainer" in repository
    and "resolveSearchableProvider(" in repository,
)
check(
    "provider main page is not required by direct entry",
    "search-without-main-page-preflight" in marker,
)
check(
    "repository provider toggles are honoured",
    "CloudStreamProviderStore(appContext)" in repository
    and "cloudStreamProviderStore.isEnabled(session.id, it)" in repository,
)
check(
    "repository selection cap expanded",
    "MAX_CHILDREN_PER_CONTAINER = 48" in repository
    and "MAX_CHILDREN_PER_CONTAINER = 18" not in repository,
)
check(
    "provider history remains prioritised",
    "providerPerformance.score(sourceUrl, providerIdentity(child))" in repository,
)
check(
    "media type contributes to priority",
    "typeCompatible(route.media.type, child.type)" in repository,
)
check(
    "repository plan diagnostics",
    '"CLOUDSTREAM_REPOSITORY_PLAN"' in repository,
)
check(
    "four-provider concurrency remains",
    "MAX_PARALLEL_CLOUDSTREAM_PROVIDERS = 4" in repository
    and "cloudStreamProviderSlots.withPermit" in repository,
)
check(
    "per-provider timeout remains bounded",
    "withTimeout(PROVIDER_TIMEOUT_MS)" in repository,
)
check(
    "late callback wait requires accepted loadLinks",
    "if (accepted && links.isEmpty())" in provider,
)
check(
    "late callback wait is bounded to 1500ms",
    "LATE_CALLBACK_WAIT_STEPS = 6" in provider
    and "LATE_CALLBACK_WAIT_STEP_MS = 250L" in provider,
)
check(
    "late callback diagnostics",
    '"CLOUDSTREAM_LOADLINKS_LATE_CALLBACK"' in provider,
)
check(
    "loadLinks result diagnostics remain",
    '"CLOUDSTREAM_LOADLINKS_RESULT"' in provider
    and '"CLOUDSTREAM_LINKS_RESULT"' in repository,
)
check(
    "provider errors remain visible in diagnostics",
    '"CLOUDSTREAM_LOADLINKS_ERROR"' in provider
    and '"CLOUDSTREAM_LOADLINKS_ERROR"' in repository,
)
check(
    "only playable nonblank links reach Sources",
    "val playableStreams = resolvedStreams.filter { it.playable && it.url.isNotBlank() }"
    in repository,
)
check(
    "terminal empty provider tabs remain removed",
    "cloudstream-empty-tabs=removed-after-terminal-result" in marker,
)
check(
    "S12.26 TV integrations preserved",
    "integrations-tv-tmdb=progressive-details-enrichment" in marker
    and "integrations-tv-mdblist=selected-provider-rating-chips" in marker,
)
check(
    "S12.25 episode link matching preserved",
    '"provider-data-id"' in repository
    and '"exact-season-episode"' in repository,
)
check(
    "S12.24 clean sidebar preserved",
    "branding-sidebar=removed-by-user-request" in marker,
)
check(
    "S12.23 player Episodes preserved",
    "player-episodes-tab=visible-for-episode-playback" in marker,
)

failed = [name for name, passed in checks if not passed]
for name, passed in checks:
    print(f"{'PASS' if passed else 'FAIL'}: {name}")

if failed:
    raise SystemExit(
        f"{len(failed)} of {len(checks)} checks failed: {', '.join(failed)}"
    )

print(f"S12.27 CloudStream runtime link contracts: {len(checks)}/{len(checks)} passed")
