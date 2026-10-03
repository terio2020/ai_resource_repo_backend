#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "$0")/.." && pwd)"
backup_code="$(sed -n '1,2p; /^backup_db() {/,/^}/p' "$repo_dir/deploy.sh")"

# Exercise the real backup function without connecting to a host or database.
# The fake dump emits enough compressed bytes to pass the size check, then fails.
mock_code='
  SSH_USER=test
  SERVER_IP=example.invalid
  ssh_cmd() {
    case "$2" in
      *"sudo mkdir"*) return 0 ;;
      *"mysqldump"*) head -c 4096 /dev/urandom; return "$MOCK_DUMP_EXIT" ;;
      *"sudo tee"*)
        bytes=$(wc -c)
        [ "$bytes" -gt 1024 ]
        ;;
      *"test "*) return 0 ;;
      *) printf "Unexpected mock command: %s\n" "$2" >&2; return 99 ;;
    esac
  }
  backup_db
'

set +e
output=$(MOCK_DUMP_EXIT=7 bash -c "$backup_code"$'\n'"$mock_code" 2>&1)
status=$?
set -e

if [ "$status" -ne 7 ] || [[ "$output" == *"完成:"* ]]; then
  printf 'FAIL: expected dump exit 7 to block backup, got %s\n%s\n' "$status" "$output" >&2
  exit 1
fi

success_output=$(MOCK_DUMP_EXIT=0 bash -c "$backup_code"$'\n'"$mock_code" 2>&1)
if [[ "$success_output" != *"完成:"* ]]; then
  printf 'FAIL: a successful dump did not complete\n%s\n' "$success_output" >&2
  exit 1
fi

# Releases must preserve both database and JAR rollback points. Retention is
# a separate, explicitly approved operation, never part of the deploy path.
if grep -Eq 'xargs.*rm|find.*-delete|Cleaning old backups' "$repo_dir/deploy.sh"; then
  printf 'FAIL: deploy path must not remove old backups\n' >&2
  exit 1
fi

printf 'PASS: partial dump blocks deployment; successful dump completes\n'
