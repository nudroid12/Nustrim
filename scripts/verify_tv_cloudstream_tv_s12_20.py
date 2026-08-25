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
models = read("app/src/main/java/app/nudroidlabs/nustrim/core/model/MediaModels.kt")
local = read("app/src/main/java/app/nudroidlabs/nustrim/core/library/LocalMediaStore.kt")
locator = read(
    "app/src/main/java/app/nudroidlabs/nustrim/core/source/cloudstream/CloudStreamProviderLocator.kt"
)
provider = read(
    "app/src/main/java/app/nudroidlabs/nustrim/core/source/cloudstream/CloudStreamProviderSession.kt"
)
bridge = read("app/src/main/java/app/nudroidlabs/nustrim/tv/cloudstream/TvCloudStreamBridge.kt")
home = read("app/src/main/java/app/nudroidlabs/nustrim/tv/home/TvHomeRepository.kt")
search = read("app/src/main/java/app/nudroidlabs/nustrim/tv/search/TvSearchRepository.kt")
details = read("app/src/main/java/app/nudroidlabs/nustrim/tv/details/TvDetailsRepository.kt")
sources = read("app/src/main/java/app/nudroidlabs/nustrim/tv/sources/TvSourcesRepository.kt")
settings_entry = read("app/src/main/java/app/nudroidlabs/nustrim/tv/settings/TvSettingsEntry.kt")
settings_screen = read("app/src/main/java/app/nudroidlabs/nustrim/tv/settings/TvSettingsScreen.kt")
subtitle = read("app/src/main/java/app/nudroidlabs/nustrim/tv/player/TvSubtitleRepository.kt")

check("S12.20 feature marker", "cloudstream-tv-search=enabled-provider-rows" in marker)
check("post-S12.20 version", version in {
    "0.57.19-tv-cleanroom-s12.20-cloudstream-tv",
    "0.57.20-tv-cleanroom-s12.21-subtitle-language-split",
    "0.57.21-tv-cleanroom-s12.22-branding",
    "0.57.22-tv-cleanroom-s12.23-player-episodes",
})
check("post-S12.20 versionCode", any(f"versionCode = {code}" in gradle for code in (142, 143, 144, 145)))
check("Media references carry provider locator", "val providerLocator: String = \"\"" in models)
check("Provider locator is URL-safe", "Base64.URL_SAFE" in locator and "providerName" in locator)
check("Provider locator persists in Library", "refProviderLocator" in local)
check("Library keys are provider aware", "providerLocator" in local and "toString(16)" in local)
check("CloudStream containers expose provider names", "val providerNames" in provider)
check("A named provider can be opened", "fun openProvider(" in provider)
check("Extracted CloudStream subtitles attach to streams", "subtitles = mappedSubtitles" in provider)
check("CloudStream bridge opens installed repository", "engine.awaitOpen(repositoryUrl)" in bridge)
check("CloudStream bridge honours provider enable state", "providerStore.isEnabled" in bridge)
check("CloudStream TV search is concurrency bounded", "Semaphore(MAX_PARALLEL_PROVIDERS)" in bridge)
check("CloudStream TV search queries every loaded provider", "container.providerNames.mapNotNull" in bridge)
check("Search results receive provider routes", "items = catalog.items.map(locator::attach)" in bridge)
check("Located results reopen exact cs3 package", "runtime.awaitOpen(locator)" in bridge)
check("Global TV Search includes CloudStream groups", "cloudStreamBridge.search(url, query)" in search)
check("Provider results use separate TV rows", "group.providerName" in search)
check("CloudStream repository cards are hidden from Home", "session.kind == SourceKind.CLOUDSTREAM" in home)
check("Details follows provider locator", "cloudStreamBridge.openLocated(route.media)" in details)
check("Details cache identity is provider aware", "providerIdentity" in details)
check("Sources prioritise exact plugin", "locatedProvider?.pluginUrl" in sources)
check("Sources select exact multi-provider child", "locatedProvider.providerName" in sources)
check("Sources can resolve exact located provider directly", "locator?.providerName == session.displayName" in sources)
check("Selected repository bypasses repository traversal", "cloudStreamBridge.openLocated(route.media)" in sources)
check("Sources publish only playable provider links", "val playableStreams = resolvedStreams.filter" in sources)
check("TV Settings discovers CloudStream providers", "discoveredCloudStreamProviders" in settings_entry)
check("TV Settings persists provider toggles", "cloudStreamProviderStore.setEnabled" in settings_entry)
check("Provider controls remain under Add-ons", "CloudStream providers" in settings_screen)
check("Catalogue order remains separate inside Content Manager", "TvContentManagerSection.CATALOG_ORDER" in settings_screen)
check("Subtitle cache identity is provider aware", "providerLocator" in subtitle)
check("S12.19 cache remains", "cloudstream-repository-cache=24h-stale-while-refresh" in marker)
check("S12.18 playable-only tabs remain", "sources-terminal-tabs=playable-links-only" in marker)

failed = [name for name, passed in checks if not passed]
for name, passed in checks:
    print(f"{'PASS' if passed else 'FAIL'}: {name}")

if failed:
    raise SystemExit(f"{len(failed)} of {len(checks)} checks failed: {', '.join(failed)}")

print(f"S12.20 CloudStream TV contracts: {len(checks)}/{len(checks)} passed")
