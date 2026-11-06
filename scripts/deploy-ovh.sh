#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

SKIP_BUILD=false
PREFLIGHT=false
DRY_RUN=false
BACKUP_ONLY=false
for option in "$@"; do
  case "$option" in
    --target=ovh|--target=ovhcloud|--reuse-remote-env) ;;
    --skip-build) SKIP_BUILD=true ;;
    --preflight-only) PREFLIGHT=true ;;
    --dry-run) DRY_RUN=true ;;
    --backup-db) BACKUP_ONLY=true ;;
    --no-backup) echo 'OVH backend requires a verified on-disk database backup before restart; --no-backup is not supported.' >&2; exit 1 ;;
    --help|-h)
      echo 'Usage: ./deploy.sh [--target=ovh] [--dry-run|--preflight-only|--backup-db] [--skip-build]'
      exit 0 ;;
    *) echo "Unsupported OVH option: $option (database rollback is manual; no automatic schema downgrade)." >&2; exit 1 ;;
  esac
done

HOST=144.217.27.52
KEY="${LOGICOMA_SSH_KEY:-$HOME/.ssh/id_ed25519_logicoma}"
KNOWN_HOSTS="${LOGICOMA_KNOWN_HOSTS:-$PWD/scripts/ovh_known_hosts}"
SSH=(ssh -i "$KEY" -o IdentitiesOnly=yes -o BatchMode=yes -o StrictHostKeyChecking=yes -o "UserKnownHostsFile=$KNOWN_HOSTS" -o ConnectTimeout=10 ubuntu@"$HOST")
fail() { echo "ERROR: $*" >&2; exit 1; }
if [ "$DRY_RUN" = true ]; then
  echo "OVH backend: ubuntu@$HOST /opt/logicomanet-be/app.jar; Compose service backend (logicomanet-be)"
  echo 'Plan: verify source/host, build JAR, stage and verify SHA-256, verify schema compatibility, back up DB on existing disk, atomically replace JAR, restart backend, verify public health/auth.'
  echo 'Existing server secrets, Compose networks, runtime image and resource limits are retained. No builds, SSH connections or uploads executed.'
  exit 0
fi
[ -f "$KEY" ] || fail "SSH key missing: $KEY"

"${SSH[@]}" 'sudo -n bash -s' <<'REMOTE'
set -euo pipefail
test -f /opt/logicomanet/release/active
test "$(stat -c %a /opt/logicomanet/secrets/backend.env)" = 600
docker compose -p logicomanet -f /opt/logicomanet/compose.yaml config --quiet
test -s /opt/logicomanet-be/app.jar
docker exec logicomanet-mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -Nse "SELECT MAX(installed_rank) FROM logicoma_net.flyway_schema_history WHERE success=1"' > /dev/null
curl -fsS --max-time 10 http://127.0.0.1/actuator/health | python3 -c 'import json,sys; assert json.load(sys.stdin)["status"] == "UP"'
echo 'OVH backend preflight passed'
REMOTE
[ "$PREFLIGHT" = false ] || exit 0

if [ "$BACKUP_ONLY" = false ]; then
  git diff --quiet && git diff --cached --quiet || fail 'Commit/review working-tree changes before deployment.'
  [ -z "$(git ls-files --others --exclude-standard)" ] || fail 'Review untracked files before deployment.'
  REMOTE_HEAD=$(git ls-remote origin refs/heads/main | awk '{print $1}')
  [ -n "$REMOTE_HEAD" ] && [ "$(git rev-parse HEAD)" = "$REMOTE_HEAD" ] || fail 'HEAD must match the current origin/main release.'
  if [ "$SKIP_BUILD" = false ]; then mvn clean package -DskipTests; fi
  JAR=target/logicoma-net-2.0.0.jar
  [ -s "$JAR" ] || fail "JAR missing: $JAR"
fi

WORK=$(mktemp -d)
UPLOAD=''
cleanup() {
  rm -rf "$WORK"
  if [ -n "$UPLOAD" ]; then "${SSH[@]}" "rm -rf -- '$UPLOAD'" || true; fi
}
trap cleanup EXIT
UPLOAD=$("${SSH[@]}" 'umask 077; mktemp -d /tmp/logicoma-be.XXXXXXXX')
[[ "$UPLOAD" =~ ^/tmp/logicoma-be\.[a-zA-Z0-9]+$ ]] || fail 'Unexpected staging path.'
if [ "$BACKUP_ONLY" = false ]; then
  cp "$JAR" "$WORK/app.jar"
  (cd "$WORK" && shasum -a 256 app.jar) > "$WORK/SHA256SUMS"
  # Read the artifact itself, rather than trusting migrations from another build.
  python3 - "$JAR" > "$WORK/versions" <<'PY'
import re,sys,zipfile
with zipfile.ZipFile(sys.argv[1]) as z:
    versions=sorted({m.group(1).replace('_','.') for n in z.namelist()
        if (m:=re.fullmatch(r'BOOT-INF/classes/db/migration/V([0-9][0-9._]*)__.*\.sql',n))})
    if not versions: raise SystemExit('No versioned migrations in JAR')
    print('\n'.join(versions))
PY
  scp -i "$KEY" -o IdentitiesOnly=yes -o BatchMode=yes -o StrictHostKeyChecking=yes -o "UserKnownHostsFile=$KNOWN_HOSTS" "$WORK/app.jar" "$WORK/SHA256SUMS" "$WORK/versions" ubuntu@"$HOST":"$UPLOAD/"
fi

"${SSH[@]}" "sudo -n flock -n /opt/logicomanet/deploy.lock bash -s -- '$UPLOAD' '$BACKUP_ONLY'" <<'REMOTE'
set -euo pipefail
upload=$1
backup_only=$2
dc() { docker compose -p logicomanet -f /opt/logicomanet/compose.yaml "$@"; }
if [ "$backup_only" = false ]; then
  (cd "$upload" && sha256sum -c SHA256SUMS)
  docker exec logicomanet-mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -Nse "SELECT version FROM logicoma_net.flyway_schema_history WHERE success=1 AND version IS NOT NULL"' > "$upload/deployed-versions"
  python3 - "$upload/versions" "$upload/deployed-versions" <<'PY'
import sys
local=set(open(sys.argv[1]).read().split())
deployed=set(open(sys.argv[2]).read().split())
missing=deployed-local
if missing: raise SystemExit('Refusing an older/incomplete JAR; deployed migrations missing: '+','.join(sorted(missing)))
PY
fi

# A release safety copy on the purchased disk; no provider backup service is enabled.
install -d -m 700 /opt/logicomanet/backups
backup="/opt/logicomanet/backups/pre-deploy-$(date +%Y%m%d%H%M%S)-$$.sql.gz"
umask 077
docker exec logicomanet-mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot --single-transaction --routines --triggers --events --no-tablespaces logicoma_net' | gzip > "$backup"
test "$(stat -c%s "$backup")" -gt 1024
gzip -t "$backup"
echo "Verified database backup (mode 600): $backup"
[ "$backup_only" = false ] || exit 0

base=/opt/logicomanet-be
previous="$base/app.jar.previous.$(date +%Y%m%d%H%M%S).$$"
cp -p "$base/app.jar" "$previous"
install -m 644 "$upload/app.jar" "$base/app.jar.next"
mv "$base/app.jar.next" "$base/app.jar"
# Reuse the authoritative Compose configuration; never create a host-network container.
dc restart backend
healthy=false
for attempt in $(seq 1 45); do
  if curl -fsS --max-time 5 http://127.0.0.1/actuator/health | python3 -c 'import json,sys; assert json.load(sys.stdin)["status"] == "UP"' 2>/dev/null; then healthy=true; break; fi
  sleep 2
done
if [ "$healthy" = false ]; then
  echo "Backend health failed. Previous JAR: $previous; database backup: $backup" >&2
  echo 'No automatic JAR or schema downgrade: startup may already have applied migrations. Inspect before recovery.' >&2
  exit 1
fi
test "$(sha256sum "$base/app.jar" | cut -d' ' -f1)" = "$(cut -d' ' -f1 "$upload/SHA256SUMS")"
echo "OVH backend installed. Previous JAR: $previous"
REMOTE
[ "$BACKUP_ONLY" = false ] || exit 0

curl -fsS --max-time 20 -D "$WORK/headers" https://logicomanet.com/actuator/health > "$WORK/health"
python3 -c 'import json,sys; assert json.load(open(sys.argv[1]))["status"] == "UP"' "$WORK/health"
grep -qi '^x-logicoma-origin: ovh-20261104' "$WORK/headers" || fail 'Public origin marker mismatch.'
STATUS=$(curl -sS --max-time 20 -o "$WORK/auth" -w '%{http_code}' https://logicomanet.com/api/users/me)
[ "$STATUS" = 401 ] || fail "Anonymous auth boundary changed: HTTP $STATUS"
echo "OVH backend deployed and verified: $(shasum -a 256 "$JAR" | awk '{print $1}')"
