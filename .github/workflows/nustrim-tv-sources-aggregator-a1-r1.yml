name: Nustrim TV Sources Aggregator A1 R1

on:
  workflow_dispatch:

permissions:
  contents: write
  actions: write

concurrency:
  group: nustrim-tv-sources-aggregator-a1-r1
  cancel-in-progress: false

env:
  TARGET_VERSION: "0.24.1-tv-sources-aggregator-r1"

jobs:
  api23-fix:
    runs-on: ubuntu-latest
    timeout-minutes: 90

    steps:
      - name: Checkout exact main
        uses: actions/checkout@v7
        with:
          ref: main
          fetch-depth: 0

      - name: Sync origin main
        shell: bash
        run: |
          set -euo pipefail
          git fetch --no-tags origin main
          git checkout -B main origin/main
          git reset --hard origin/main

      - name: Set up Java
        uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: "17"

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v6
        with:
          gradle-version: "9.5.0"

      - name: Capture protected baseline
        shell: bash
        run: |
          set -euo pipefail
          DIR="$RUNNER_TEMP/tv-sources-a1-r1"
          mkdir -p "$DIR"

          AGG="app/src/main/java/app/nudroidlabs/nustrim/core/source/StreamSourceAggregator.kt"
          DETAILS="app/src/main/java/app/nudroidlabs/nustrim/tv/details/TvDetailsScreen.kt"

          test -f "$AGG"
          test -f "$DETAILS"
          test -f .github/workflows/build.yml
          test -d app/src/main/java/app/nudroidlabs/nustrim/core/update

          grep -q 'class StreamSourceAggregator(' "$AGG"
          grep -q 'StreamSourceAggregator(context)' "$DETAILS"
          grep -q 'aggregator.load(' "$DETAILS"

          cp "$DETAILS" "$DIR/TvDetailsScreen.before.kt"
          cp app/src/main/java/app/nudroidlabs/nustrim/tv/player/TvPlayerScreen.kt             "$DIR/TvPlayerScreen.before.kt"
          cp app/src/main/java/app/nudroidlabs/nustrim/ui/NustrimApp.kt             "$DIR/NustrimApp.before.kt"
          cp app/src/main/java/app/nudroidlabs/nustrim/ui/UiPreferences.kt             "$DIR/UiPreferences.before.kt"

          sha256sum .github/workflows/build.yml > "$DIR/build.before"
          find app/src/main/java/app/nudroidlabs/nustrim/core/update             -type f -print0 | sort -z | xargs -0 sha256sum             > "$DIR/updater.before"

          {
            echo "TV SOURCES AGGREGATOR A1 R1"
            echo "==========================="
            echo "sha=$(git rev-parse HEAD)"
            grep -m1 'minSdk' app/build.gradle.kts
            grep -m1 'versionCode' app/build.gradle.kts
            grep -m1 'versionName' app/build.gradle.kts
            echo
            echo "[offending API usage]"
            grep -n 'putIfAbsent' "$AGG" || true
          } | tee "$DIR/preflight.txt"

      - name: Apply API 23 compatibility fix
        shell: bash
        run: |
          set -euo pipefail
          python3 - <<'PY'
          from pathlib import Path
          
          path = Path(
              "app/src/main/java/app/nudroidlabs/nustrim/core/source/StreamSourceAggregator.kt"
          )
          text = path.read_text(encoding="utf-8")
          
          old = """                    if (supportsStreams(session)) {
                                  streamSessions.putIfAbsent(session.id, session)
                              }
          """
          
          new = """                    if (
                                  supportsStreams(session) &&
                                  !streamSessions.containsKey(session.id)
                              ) {
                                  streamSessions[session.id] = session
                              }
          """
          
          if old not in text:
              if "putIfAbsent(" in text:
                  raise SystemExit(
                      "putIfAbsent still exists, but expected A1 block does not match live source."
                  )
              if (
                  "!streamSessions.containsKey(session.id)" in text
                  and "streamSessions[session.id] = session" in text
              ):
                  print("API 23 compatibility fix is already present; no source rewrite needed.")
              else:
                  raise SystemExit(
                      "Expected A1 putIfAbsent block is missing and compatible replacement was not found."
                  )
          else:
              text = text.replace(old, new, 1)
              path.write_text(text, encoding="utf-8")
              print("Replaced putIfAbsent with API 23-compatible containsKey + assignment.")
          
          updated = path.read_text(encoding="utf-8")
          
          checks = {
              "no putIfAbsent": "putIfAbsent(" not in updated,
              "containsKey guard": "!streamSessions.containsKey(session.id)" in updated,
              "assignment retained": "streamSessions[session.id] = session" in updated,
              "aggregator retained": "class StreamSourceAggregator(" in updated,
              "stream capability retained":
                  'resource.equals("stream", ignoreCase = true)' in updated,
          }
          failed = [name for name, ok in checks.items() if not ok]
          if failed:
              raise SystemExit("R1 source audit failed: " + ", ".join(failed))
          
          for name in checks:
              print("PASS:", name)
          PY

      - name: Set R1 version
        shell: bash
        run: |
          set -euo pipefail
          python3 - <<'PY'
          from pathlib import Path
          import re
          import os
          
          path = Path("app/build.gradle.kts")
          text = path.read_text(encoding="utf-8")
          target = os.environ["TARGET_VERSION"]
          
          code_match = re.search(r'versionCode\s*=\s*(\d+)', text)
          name_match = re.search(r'versionName\s*=\s*"([^"]+)"', text)
          if not code_match or not name_match:
              raise SystemExit("Could not parse app version metadata.")
          
          old_code = int(code_match.group(1))
          old_name = name_match.group(1)
          
          if old_name == target:
              new_code = old_code
          else:
              new_code = old_code + 1
          
          text = re.sub(
              r'versionCode\s*=\s*\d+',
              f'versionCode = {new_code}',
              text,
              count=1,
          )
          text = re.sub(
              r'versionName\s*=\s*"[^"]+"',
              f'versionName = "{target}"',
              text,
              count=1,
          )
          
          path.write_text(text, encoding="utf-8")
          print(f"versionName: {old_name} -> {target}")
          print(f"versionCode: {old_code} -> {new_code}")
          PY

      - name: R1 architecture audit
        shell: bash
        run: |
          set -euo pipefail
          DIR="$RUNNER_TEMP/tv-sources-a1-r1"
          AGG="app/src/main/java/app/nudroidlabs/nustrim/core/source/StreamSourceAggregator.kt"

          echo "AUDIT VERSION: TV-SOURCES-AGGREGATOR-A1-R1"

          if grep -R -n 'putIfAbsent('             app/src/main/java/app/nudroidlabs/nustrim/core/source/StreamSourceAggregator.kt; then
            echo "::error::API 24 putIfAbsent still exists."
            exit 1
          fi

          grep -q '!streamSessions.containsKey(session.id)' "$AGG"
          grep -q 'streamSessions\[session.id\] = session' "$AGG"
          grep -q 'class StreamSourceAggregator(' "$AGG"
          grep -q 'resource.equals("stream", ignoreCase = true)' "$AGG"

          grep -q 'minSdk = 23' app/build.gradle.kts
          grep -q 'versionName = "0.24.1-tv-sources-aggregator-r1"'             app/build.gradle.kts

          cmp "$DIR/TvDetailsScreen.before.kt"             app/src/main/java/app/nudroidlabs/nustrim/tv/details/TvDetailsScreen.kt
          cmp "$DIR/TvPlayerScreen.before.kt"             app/src/main/java/app/nudroidlabs/nustrim/tv/player/TvPlayerScreen.kt
          cmp "$DIR/NustrimApp.before.kt"             app/src/main/java/app/nudroidlabs/nustrim/ui/NustrimApp.kt
          cmp "$DIR/UiPreferences.before.kt"             app/src/main/java/app/nudroidlabs/nustrim/ui/UiPreferences.kt

          sha256sum .github/workflows/build.yml > "$DIR/build.after"
          find app/src/main/java/app/nudroidlabs/nustrim/core/update             -type f -print0 | sort -z | xargs -0 sha256sum             > "$DIR/updater.after"

          cmp "$DIR/build.before" "$DIR/build.after"
          cmp "$DIR/updater.before" "$DIR/updater.after"

          changed="$(
            git diff --name-only | sort
          )"
          printf '%s\n' "$changed" | tee "$DIR/changed-files.txt"

          unexpected="$(
            printf '%s\n' "$changed" |
            grep -v -E               '^(app/build.gradle.kts|app/src/main/java/app/nudroidlabs/nustrim/core/source/StreamSourceAggregator.kt)$'               || true
          )"
          if [[ -n "$unexpected" ]]; then
            echo "::error::Unexpected files changed:"
            printf '%s\n' "$unexpected"
            exit 1
          fi

          git diff --check
          git diff > "$DIR/r1.patch"

          echo "PASS: minSdk remains 23"
          echo "PASS: API 24 putIfAbsent removed"
          echo "PASS: aggregator behavior preserved"
          echo "PASS: TV Details/Player untouched"
          echo "PASS: updater/build workflow untouched"

      - name: Compile debug
        shell: bash
        run: |
          set -euo pipefail
          gradle --no-daemon --build-cache             :app:compileDebugKotlin             --stacktrace             --console=plain             2>&1 | tee "$RUNNER_TEMP/tv-sources-a1-r1/compile.log"

      - name: Run release lint
        shell: bash
        run: |
          set -euo pipefail
          gradle --no-daemon --build-cache             :app:lintRelease             --stacktrace             --console=plain             2>&1 | tee "$RUNNER_TEMP/tv-sources-a1-r1/lint-release.log"

      - name: Assemble debug
        shell: bash
        run: |
          set -euo pipefail
          gradle --no-daemon --build-cache             :app:assembleDebug             --stacktrace             --console=plain             2>&1 | tee "$RUNNER_TEMP/tv-sources-a1-r1/assemble.log"

      - name: Commit and push R1
        shell: bash
        run: |
          set -euo pipefail
          exec > >(tee "$RUNNER_TEMP/tv-sources-a1-r1/push.log") 2>&1

          git config user.name "github-actions[bot]"
          git config user.email "41898282+github-actions[bot]@users.noreply.github.com"

          git add             app/src/main/java/app/nudroidlabs/nustrim/core/source/StreamSourceAggregator.kt             app/build.gradle.kts

          git diff --cached --check

          if ! git diff --cached --quiet; then
            git commit -m "fix(tv): keep source aggregator compatible with API 23"
            git fetch --no-tags origin main
            git rebase origin/main
            git push origin HEAD:main
          fi

      - name: Verify actual origin main
        shell: bash
        run: |
          set -euo pipefail
          git fetch --no-tags origin main
          echo "origin_main_sha=$(git rev-parse origin/main)"             | tee "$RUNNER_TEMP/tv-sources-a1-r1/remote-main.txt"
          python3 - <<'PY'
          import subprocess
          
          agg = subprocess.check_output(
              [
                  "git",
                  "show",
                  "origin/main:app/src/main/java/app/nudroidlabs/nustrim/core/source/StreamSourceAggregator.kt",
              ],
              text=True,
          )
          gradle = subprocess.check_output(
              ["git", "show", "origin/main:app/build.gradle.kts"],
              text=True,
          )
          
          checks = {
              "remote putIfAbsent removed": "putIfAbsent(" not in agg,
              "remote API23 guard":
                  "!streamSessions.containsKey(session.id)" in agg,
              "remote assignment":
                  "streamSessions[session.id] = session" in agg,
              "remote aggregator retained":
                  "class StreamSourceAggregator(" in agg,
              "remote version":
                  'versionName = "0.24.1-tv-sources-aggregator-r1"' in gradle,
          }
          
          failed = [name for name, ok in checks.items() if not ok]
          if failed:
              raise SystemExit(
                  "REMOTE R1 VERIFICATION FAILED: " + ", ".join(failed)
              )
          
          for name in checks:
              print("PASS:", name)
          PY

      - name: Dispatch signed updater build
        shell: bash
        env:
          GH_TOKEN: ${{ github.token }}
        run: |
          set -euo pipefail

          HEAD_SHA="$(git rev-parse origin/main)"
          echo "HEAD_SHA=$HEAD_SHA" >> "$GITHUB_ENV"

          gh workflow run build.yml             --repo "$GITHUB_REPOSITORY"             --ref main             -f publish_release=true

          sleep 8

          RUN_ID=""
          for attempt in $(seq 1 24); do
            RUN_ID="$(
              gh run list                 --repo "$GITHUB_REPOSITORY"                 --workflow build.yml                 --branch main                 --event workflow_dispatch                 --limit 20                 --json databaseId,headSha                 --jq ".[] | select(.headSha == \"$HEAD_SHA\") | .databaseId"                 | head -n1
            )"
            if [[ -n "$RUN_ID" ]]; then
              break
            fi
            sleep 5
          done

          test -n "$RUN_ID"
          echo "SIGNED_RUN_ID=$RUN_ID" >> "$GITHUB_ENV"
          echo "signed_run_id=$RUN_ID"             | tee "$RUNNER_TEMP/tv-sources-a1-r1/signed-run.txt"

      - name: Wait signed updater build
        shell: bash
        env:
          GH_TOKEN: ${{ github.token }}
        run: |
          set -euo pipefail
          gh run watch "$SIGNED_RUN_ID"             --repo "$GITHUB_REPOSITORY"             --exit-status             2>&1 | tee "$RUNNER_TEMP/tv-sources-a1-r1/signed-build-watch.log"

      - name: Verify published R1
        shell: bash
        run: |
          set -euo pipefail

          MANIFEST="$RUNNER_TEMP/tv-sources-a1-r1/update.json"

          for attempt in $(seq 1 24); do
            curl               --fail               --location               --silent               --show-error               --header 'Cache-Control: no-cache'               "https://github.com/$GITHUB_REPOSITORY/releases/latest/download/update.json?ts=$(date +%s)"               --output "$MANIFEST" || true

            if grep -q '0.24.1-tv-sources-aggregator-r1' "$MANIFEST"               2>/dev/null; then
              break
            fi
            sleep 5
          done

          test -s "$MANIFEST"
          grep -q '0.24.1-tv-sources-aggregator-r1' "$MANIFEST"
          cat "$MANIFEST"             | tee "$RUNNER_TEMP/tv-sources-a1-r1/published-update.json"

      - name: Compact failure summary
        if: failure()
        shell: bash
        run: |
          set +e
          DIR="$RUNNER_TEMP/tv-sources-a1-r1"
          echo "========== TV Sources Aggregator A1 R1 =========="
          echo "Result: FAILED"

          for f in preflight.txt changed-files.txt remote-main.txt signed-run.txt                    published-update.json; do
            if [[ -f "$DIR/$f" ]]; then
              echo
              echo "===== $f ====="
              cat "$DIR/$f" || true
            fi
          done

          for f in compile.log lint-release.log assemble.log push.log                    signed-build-watch.log; do
            if [[ -f "$DIR/$f" ]]; then
              echo
              echo "===== $f ====="
              tail -n 260 "$DIR/$f" || true
            fi
          done

      - name: Upload diagnose
        if: always()
        uses: actions/upload-artifact@v7
        with:
          name: nustrim-tv-sources-aggregator-a1-r1-diagnose
          path: ${{ runner.temp }}/tv-sources-a1-r1
          if-no-files-found: ignore
          retention-days: 7
