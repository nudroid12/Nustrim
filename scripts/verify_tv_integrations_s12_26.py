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
models = read("app/src/main/java/app/nudroidlabs/nustrim/tv/details/TvDetailsModels.kt")
entry = read("app/src/main/java/app/nudroidlabs/nustrim/tv/details/TvDetailsEntry.kt")
repository = read("app/src/main/java/app/nudroidlabs/nustrim/tv/details/TvDetailsRepository.kt")
screen = read("app/src/main/java/app/nudroidlabs/nustrim/tv/details/TvDetailsScreen.kt")
settings = read("app/src/main/java/app/nudroidlabs/nustrim/tv/settings/TvSettingsEntry.kt")
clients = read("app/src/main/java/app/nudroidlabs/nustrim/core/integrations/IntegrationClients.kt")

check("S12.26 marker", "subsystem=12.26-tv-integrations" in marker)
check("S12.26 version", version == "0.57.25-tv-cleanroom-s12.26-tv-integrations")
check("S12.26 versionCode", "versionCode = 148" in gradle)
check("TV snapshot carries TMDB metadata", "val tmdbMetadata: TmdbMetadata?" in models)
check("TV snapshot carries MDBList ratings", "val mdbListRatings: List<MdbListRating>" in models)
check("TV snapshot exposes progressive loading", "val integrationsLoading: Boolean" in models)
check("TV snapshot exposes integration errors", "val integrationMessage: String" in models)
check("TV repository calls TMDB metadata", "TmdbClient.metadata(snapshot.item" in repository)
check("TV repository calls MDBList ratings", "MdbListClient.ratings(" in repository)
check("TMDB respects configured toggle", "preferences.tmdbEnrichmentEnabled" in repository)
check("MDBList respects configured toggle", "preferences.mdbListRatingsEnabled" in repository)
check("TMDB uses saved credential", "preferences.tmdbApiKey" in repository)
check("MDBList uses saved API key", "preferences.mdbListApiKey" in repository)
check("base Details publish before enrichment", entry.index("Ready(baseSnapshot)") < entry.index("enrichIntegrations("))
check("integration refresh follows Details refresh", "forceRefresh = reloadToken > 0" in entry)
check("TMDB enrichment is timeout bounded", "TMDB_TIMEOUT_MS" in repository and "withTimeoutOrNull(TMDB_TIMEOUT_MS)" in repository)
check("MDBList enrichment is timeout bounded", "MDBLIST_TIMEOUT_MS" in repository and "withTimeoutOrNull(MDBLIST_TIMEOUT_MS)" in repository)
check("integration cache is bounded", "INTEGRATION_CACHE_TTL_MS" in repository and "size > 32" in repository)
check("cache changes with credentials", "tmdbApiKey.hashCode()" in repository and "mdbListApiKey.hashCode()" in repository)
check("cache changes with rating providers", "MDBLIST_PROVIDER_IDS.filter" in repository)
check("source playback identity remains untouched", all(token not in repository.split("private fun MediaItem.withTmdbMetadata", 1)[1].split("}", 1)[0] for token in ("id =", "ref =", "episodes =", "streams =")))
check("TMDB artwork reaches TV item", "backgroundUrl = metadata.backdropUrl" in repository and "logoUrl = metadata.logoUrl" in repository)
check("TMDB metadata reaches TV item", "genres = metadata.genres" in repository and "cast = metadata.cast" in repository)
check("MDBList provider preferences filter results", "filter { preferences.isDisplayedMdbRating(it.source) }" in repository)
check("TV Details renders rating chips", "RatingChip(rating)" in screen)
check("TV Details renders all returned selected ratings", "items = snapshot.mdbListRatings" in screen)
check("TV Details shows integration loading", '"Loading TMDB and MDBList..."' in screen)
check("TV Details shows integration failure", "snapshot.integrationMessage" in screen)
check("TV Settings still validates TMDB", "TmdbClient.validate(first)" in settings)
check("TV Settings still validates MDBList", "MdbListClient.validate(first)" in settings)
check("TMDB client accepts API key or bearer", '"Authorization" to "Bearer ${credential.trim()}"' in clients and "api_key=" in clients)
check("MDBList client uses apikey", "?apikey=" in clients)
check("S12.25 CloudStream links fix preserved", "cloudstream-loadlinks-errors=diagnostics-preserved" in marker)
check("S12.24 sidebar cleanup preserved", "branding-sidebar=removed-by-user-request" in marker)
check("S12.23 Player Episodes preserved", "player-episodes-tab=visible-for-episode-playback" in marker)

failed = [name for name, passed in checks if not passed]
for name, passed in checks:
    print(f"{'PASS' if passed else 'FAIL'}: {name}")

if failed:
    raise SystemExit(
        f"{len(failed)} of {len(checks)} checks failed: {', '.join(failed)}"
    )

print(f"S12.26 TV integration contracts: {len(checks)}/{len(checks)} passed")
