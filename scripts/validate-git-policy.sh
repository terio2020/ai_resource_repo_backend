#!/usr/bin/env bash
set -euo pipefail

fail() { printf 'git-policy: %s\n' "$*" >&2; exit 1; }
validate_branch() { [[ "$1" =~ ^codex/LCM-[0-9]+-[a-z0-9]+(-[a-z0-9]+)*$ ]] || fail "branch must match codex/LCM-<number>-<kebab-case-slug>; got: $1"; }
validate_subject() {
  [[ "$1" =~ ^(feat|fix|refactor|test|docs|chore|ci|perf|build|style)(\([a-z0-9][a-z0-9-]*\))?!?:\ .+ ]] ||
    [[ "$1" =~ ^revert:\ .+ ]] || [[ "$1" =~ ^Merge\  ]] || fail "invalid Conventional Commit subject: $1"
}
validate_message() {
  local file="$1" subject; subject="$(sed -n '1p' "$file")"; validate_subject "$subject"
  [[ "$subject" =~ ^(Merge\ |revert:) ]] && return 0
  grep -Eq '^Change: LCM-[0-9]+$' "$file" || fail "missing Change: LCM-<number>"
  grep -Eq '^Contract: (none|additive|breaking)$' "$file" || fail "missing Contract: none|additive|breaking"
  grep -Eq '^Tests: .+' "$file" || fail "missing non-empty Tests: line"
}
case "${1:-}" in
  branch) validate_branch "${2:-$(git branch --show-current)}" ;;
  message) [[ -n "${2:-}" ]] || fail "message file is required"; validate_message "$2" ;;
  range)
    [[ -n "${2:-}" && -n "${3:-}" ]] || fail "base and head refs are required"
    while IFS= read -r commit; do file="$(mktemp)"; git show -s --format=%B "$commit" > "$file"; validate_message "$file" || { rm -f "$file"; exit 1; }; rm -f "$file"; done < <(git rev-list --reverse "${2}..${3}")
    ;;
  pr-title) [[ "${2:-}" =~ ^\[LCM-[0-9]+\]\ .+ ]] || fail "PR title must match [LCM-<number>] <summary>" ;;
  *) fail "usage: $0 branch|message|range|pr-title ..." ;;
esac
printf 'git-policy: OK\n'
