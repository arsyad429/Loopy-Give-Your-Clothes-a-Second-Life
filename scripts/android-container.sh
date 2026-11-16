#!/bin/sh
set -eu
# Snapshot baru tiap build, tanpa SDK path/cache/output host dan tanpa source sisa build lama.
build_dir="$(mktemp -d "${TMPDIR:-/tmp}/loopy-android.XXXXXX")"
trap 'rm -rf "$build_dir"' EXIT HUP INT TERM
python3 - "$build_dir" <<'PY'
import shutil
import sys
shutil.copytree('/workspace/android', sys.argv[1], dirs_exist_ok=True,
                ignore=shutil.ignore_patterns('local.properties', '.gradle', '.kotlin', 'build'))
PY
cd "$build_dir"
result=0
sh ./gradlew --no-daemon "-PLOOPY_API_BASE_URL=${LOOPY_API_BASE_URL:-http://10.0.2.2:8080/}" "$@" || result=$?
mkdir -p /workspace/artifacts
if [ -d app/build/outputs ]; then cp -R app/build/outputs /workspace/artifacts/; fi
if [ -d app/build/reports ]; then cp -R app/build/reports /workspace/artifacts/; fi
if [ -d app/schemas ]; then cp -R app/schemas/. /workspace/android/app/schemas/; fi
exit "$result"
