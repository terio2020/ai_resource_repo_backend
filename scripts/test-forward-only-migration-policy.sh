#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "$0")/.." && pwd)"
migration_dir="$repo_dir/src/main/resources/db/migration"
undo_dir="$repo_dir/src/main/resources/db/migration-undo"
forward_dir="$repo_dir/src/main/resources/db/migration-forward-only"

bash -n "$repo_dir/deploy.sh"
for migration in "$migration_dir"/V*.sql; do
  name="$(basename "$migration" .sql)"
  if [ ! -f "$undo_dir/${name}-undo.sql" ] && [ ! -s "$forward_dir/${name}.md" ]; then
    printf 'FAIL: %s lacks an undo or forward-only policy\n' "$name" >&2
    exit 1
  fi
done

set +e
rejection="$(bash "$repo_dir/deploy.sh" --target=aws --no-backup --reuse-remote-env 2>&1)"
status=$?
set -e
if [ "$status" -ne 0 ] && [[ "$rejection" == *'Forward-only migrations require a verified database backup'* ]]; then
  printf 'PASS: forward-only migrations are documented and cannot skip backup\n'
else
  printf 'FAIL: --no-backup was not rejected for forward-only migrations\n' >&2
  exit 1
fi
