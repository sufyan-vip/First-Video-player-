#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
chmod +x gradlew
./gradlew testDebugUnitTest assembleDebug assembleRelease --stacktrace
mkdir -p dist
cp app/build/outputs/apk/debug/*.apk dist/Aether-debug.apk
cp app/build/outputs/apk/release/*.apk dist/Aether-release.apk
ls -lh dist
