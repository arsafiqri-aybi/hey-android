#!/usr/bin/env bash
set -euo pipefail
repo_dir="$(cd "$(dirname "$0")/.." && pwd)"
qa_dir="${HEY_QA_TOOLS:-$repo_dir/app/build/qa-tools}"
if [[ -z "${HEY_CHROMIUM:-}" ]]; then
  for candidate in google-chrome google-chrome-stable chromium chromium-browser; do
    if command -v "$candidate" >/dev/null 2>&1; then export HEY_CHROMIUM="$(command -v "$candidate")"; break; fi
  done
fi
if [[ -z "${HEY_CHROMIUM:-}" ]]; then
  printf '%s\n' 'A real Chromium executable is required; DOM/render checks cannot be marked PASS without it.' >&2
  exit 1
fi
if [[ -z "${NODE_PATH:-}" ]]; then
  npm install --prefix "$qa_dir" --no-save --package-lock=false --no-audit --no-fund playwright@1.58.2
  export NODE_PATH="$qa_dir/node_modules"
fi
node "$repo_dir/evaluation/browser-fixtures.cjs"
