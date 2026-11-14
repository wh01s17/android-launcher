#!/usr/bin/env bash
# Measures cold start and frame timing on a HyperOS phone (plan.md §7 Hito 11, docs/performance.md).
#
# HyperOS drops the profileinstaller broadcasts Macrobenchmark relies on, so this script compiles the
# release app itself (`cmd package compile`) and runs each benchmark with
# `wh01s17.manualCompilation=true`. Install first:
#   ./gradlew :app:installBenchmarkRelease :benchmark:installBenchmarkRelease
# Then: ANDROID_SERIAL=<serial> tools/run-benchmarks.sh [output-dir]
set -euo pipefail

readonly APP=com.wh01s17.launcher
readonly TESTS=com.wh01s17.launcher.benchmark
readonly RUNNER="$TESTS/androidx.test.runner.AndroidJUnitRunner"
readonly DEVICE_DIR="/sdcard/Android/media/$TESTS"
readonly OUT="${1:-build/benchmarks}"

die() { echo "error: $*" >&2; exit 1; }

adb get-state >/dev/null 2>&1 || die "no device (set ANDROID_SERIAL)"
adb shell pm list instrumentation | grep -q "$RUNNER" || die "benchmark APK not installed"
adb shell pm path "$APP" >/dev/null 2>&1 || die "$APP (benchmarkRelease) not installed"

# Leaves the app compiled as asked: `verify` (no AOT code, like a fresh sideload without a profile) or
# `speed-profile` with the Baseline Profile that profileinstaller writes on the first launch.
compile() {
  local mode=$1
  adb shell am force-stop "$APP"
  if [[ "$mode" == speed-profile ]]; then
    adb shell cmd package compile -f -m verify "$APP" >/dev/null
    adb shell monkey -p "$APP" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
    sleep 12  # ProfileInstallerInitializer writes the profile a few seconds after the first frame.
    adb shell am force-stop "$APP"
  fi
  adb shell cmd package compile -f -m "$mode" "$APP" >/dev/null
  adb shell dumpsys package dexopt | grep -A3 "\[$APP\]" | grep -o "status=[a-z-]*" | head -1
}

# Runs one test and keeps its JSON (each run overwrites the one on the device) as <Class#test>.json.
run() {
  local test=$1
  echo "== $test"
  adb shell rm -rf "$DEVICE_DIR"
  adb shell am instrument -w \
    -e class "$TESTS.$test" \
    -e wh01s17.manualCompilation true \
    -e androidx.benchmark.dropShaders.enable false \
    "$RUNNER" | grep -E "^  (timeTo|frame)|^OK \(|FAILURES|Error" | sed 's/\[\([a-z]* [0-9.]*\)\]([^)]*)/\1/g' | awk '!seen[$0]++' || true
  adb shell cat "$DEVICE_DIR/$TESTS-benchmarkData.json" > "$OUT/${test/\#/_}.json"
}

mkdir -p "$OUT"

compile verify
run "StartupBenchmark#coldStartWithoutProfile"

compile speed-profile
run "StartupBenchmark#coldStartWithBaselineProfile"
run "FrameBenchmark#workspacePager"
run "FrameBenchmark#menuOpen"

echo "results: $OUT"
