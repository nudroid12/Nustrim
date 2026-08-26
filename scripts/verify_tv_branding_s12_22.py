#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


launch_background = read("app/src/main/res/drawable/nustrim_launch_background.xml")


checks = {
    "S12.22 or later version": any(
        name in read("app/build.gradle.kts")
        for name in (
            'versionName = "0.57.21-tv-cleanroom-s12.22-branding"',
            'versionName = "0.57.22-tv-cleanroom-s12.23-player-episodes"',
            'versionName = "0.57.23-tv-cleanroom-s12.24-sidebar-clean"',
            'versionName = "0.57.24-tv-cleanroom-s12.25-cloudstream-links-fix"',
        )
    ),
    "S12.22 or later version code": any(
        f"versionCode = {code}" in read("app/build.gradle.kts") for code in (144, 145, 146, 147)
    ),
    "branding marker": "branding-source=user-approved-blue-cyan-nustrim-logo" in read(".nustrim-tv"),
    "launcher resource": 'android:icon="@drawable/nustrim_app_icon"' in read("app/src/main/AndroidManifest.xml"),
    "launch background": "@drawable/nustrim_launch_background" in read("app/src/main/res/values/styles.xml"),
    "launch background colour shape": '<shape android:shape="rectangle">' in launch_background and '<solid android:color="#02030B"' in launch_background,
    "sidebar branding follows current contract": (
        "R.drawable.nustrim_brand_mark" in read("app/src/main/java/app/nudroidlabs/nustrim/tv/shell/TvSidebar.kt")
        or "branding-sidebar=removed-by-user-request" in read(".nustrim-tv")
    ),
    "mobile mark": "painterResource(R.drawable.nustrim_brand_mark)" in read("app/src/main/java/app/nudroidlabs/nustrim/ui/NustrimApp.kt"),
    "brand mark asset": (ROOT / "app/src/main/res/drawable-nodpi/nustrim_brand_mark.webp").stat().st_size > 10_000,
    "app icon asset": (ROOT / "app/src/main/res/drawable-nodpi/nustrim_app_icon.webp").stat().st_size > 10_000,
    "TV banner asset": (ROOT / "app/src/main/res/drawable-xhdpi/nustrim_tv_banner.png").stat().st_size > 10_000,
    "splash asset": (ROOT / "app/src/main/res/drawable-xhdpi/nustrim_splash_brand.webp").stat().st_size > 8_000,
}

failed = [name for name, passed in checks.items() if not passed]
for name, passed in checks.items():
    print(f"{'PASS' if passed else 'FAIL'}: {name}")
if failed:
    raise SystemExit("S12.22 branding contract failed: " + ", ".join(failed))
print(f"S12.22 branding contracts: {len(checks)}/{len(checks)}")
