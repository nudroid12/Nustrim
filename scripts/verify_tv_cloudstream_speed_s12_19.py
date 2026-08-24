#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


checks: list[tuple[str, bool]] = []


def check(name: str, condition: bool) -> None:
    checks.append((name, condition))


engine = read("app/src/main/java/app/nudroidlabs/nustrim/core/source/SourceEngine.kt")
cache = read(
    "app/src/main/java/app/nudroidlabs/nustrim/core/source/cloudstream/CloudStreamRepositoryCache.kt"
)
repository = read(
    "app/src/main/java/app/nudroidlabs/nustrim/core/source/cloudstream/CloudStreamRepositorySession.kt"
)
sources = read("app/src/main/java/app/nudroidlabs/nustrim/tv/sources/TvSourcesRepository.kt")
history = read("app/src/main/java/app/nudroidlabs/nustrim/tv/sources/TvCloudStreamPerformanceStore.kt")
marker = read(".nustrim-tv")
version = read(".nustrim-version").strip()
gradle = read("app/build.gradle.kts")

check("S12.19 feature marker", "cloudstream-repository-cache=24h-stale-while-refresh" in marker)
check("post-S12.19 version", version in {
    "0.57.18-tv-cleanroom-s12.19-cloudstream-speed",
    "0.57.19-tv-cleanroom-s12.20-cloudstream-tv",
    "0.57.20-tv-cleanroom-s12.21-subtitle-language-split",
})
check("post-S12.19 versionCode", any(f"versionCode = {code}" in gradle for code in (141, 142, 143)))
check("Repository cache is app-private", "context.applicationContext.cacheDir" in cache)
check("Repository cache is URL keyed", 'MessageDigest.getInstance("SHA-256")' in cache)
check("Repository cache uses a temporary commit file", 'target.name + ".tmp"' in cache and "renameTo(target)" in cache)
check("Repository cache TTL is 24 hours", "24L * 60L * 60L * 1_000L" in cache)
check("Repository stale fallback is bounded", "7L * 24L * 60L * 60L * 1_000L" in cache)
check("SourceEngine serves repository cache", "CLOUDSTREAM_REPO_CACHE_HIT" in engine)
check("SourceEngine refreshes cache in background", "refreshCloudStreamRepository(normalized)" in engine)
check("Refresh bypasses SourceEngine cache", "if (!forceRefresh) cachedSession" in engine)
check("Refresh bypasses plugin-list cache", "if (forceRefresh) null else cache.readFresh" in repository)
check("Plugin-list network result is cached", "cache.write(pluginListUrl, it)" in repository)
check("Four-provider concurrency limit", "MAX_PARALLEL_CLOUDSTREAM_PROVIDERS = 4" in sources)
check("Semaphore enforces provider concurrency", "cloudStreamProviderSlots.withPermit" in sources)
check("Nested provider deadlock is avoided", "if (depth == 0)" in sources)
check("Successful provider history persists", "getSharedPreferences" in history and "recordSuccess" in history)
check(
    "Successful providers are prioritised",
    ("sortedByDescending" in sources or "sortedWith" in sources) and "providerPerformance.score" in sources,
)
check("Positive result cache is 30 minutes", "POSITIVE_CACHE_TTL_MS = 30L * 60L * 1_000L" in sources)
check("Negative result cache is 10 minutes", "NEGATIVE_CACHE_TTL_MS = 10L * 60L * 1_000L" in sources)
check("Per-provider no-match cache is 10 minutes", "NEGATIVE_PROVIDER_CACHE_TTL_MS = 10L * 60L * 1_000L" in sources)
check("Manual refresh clears negative cache", "negativeProviderCache.keys" in sources)
check("Progressive provider publishing remains", "onProgress(childResult)" in sources)
check("S12.18 playable-only tabs remain", "sources-terminal-tabs=playable-links-only" in marker)

failed = [name for name, passed in checks if not passed]
for name, passed in checks:
    print(f"{'PASS' if passed else 'FAIL'}: {name}")

if failed:
    raise SystemExit(f"{len(failed)} of {len(checks)} checks failed: {', '.join(failed)}")

print(f"S12.19 CloudStream speed contracts: {len(checks)}/{len(checks)} passed")
