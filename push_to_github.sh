#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

export GIT="git -c safe.directory=${ROOT}"
export GIT_USER="git -c safe.directory=${ROOT} -c user.name=durant789990-tech -c user.email=durant789990-tech@users.noreply.github.com"

echo "[1/5] Gradle assembleDebug ..."
./gradlew assembleDebug

echo "[2/5] Copy APK to release_apk ..."
mkdir -p release_apk
SRC="app/build/outputs/apk/debug/app-debug.apk"
DEST="release_apk/KSongAssistant_latest.apk"
if [[ ! -f "$SRC" ]]; then
  echo "APK not found: $SRC" >&2
  exit 1
fi
cp -f "$SRC" "$DEST"

TS="$(date '+%Y-%m-%d %H:%M:%S')"
MSG="Auto commit: update feature and build APK [${TS}]"
BRANCH="$($GIT rev-parse --abbrev-ref HEAD)"

echo "[3/5] git add ..."
$GIT add .

echo "[4/5] git commit ..."
if ! $GIT_USER commit -m "$MSG"; then
  echo "Nothing to commit or commit failed."
fi

echo "[5/5] git push origin ${BRANCH} ..."
$GIT push origin "$BRANCH"

echo "Done. APK: ${DEST}"
