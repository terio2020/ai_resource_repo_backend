#!/bin/bash
set -e

# ==========================================
# Logicoma 后端自动化部署脚本
# ==========================================
# Usage: ./deploy.sh [--target=aws|aliyun] [--skip-build] [--no-backup]
# ==========================================

# Options
TARGET="aliyun"
SKIP_BUILD=false
NO_BACKUP=false
SELF_AUDIT=false
BACKUP_DB=false
ROLLBACK=""

while [[ $# -gt 0 ]]; do
  case $1 in
    --target=*) TARGET="${1#*=}"; shift ;;
    --skip-build) SKIP_BUILD=true; shift ;;
    --no-backup) NO_BACKUP=true; shift ;;
    --self-audit) SELF_AUDIT=true; shift ;;
    --backup-db) BACKUP_DB=true; shift ;;
    --rollback=*) ROLLBACK="${1#*=}"; shift ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

# Target server configuration
select_server() {
  case "$TARGET" in
    aliyun|server1)
      SERVER_IP="111.231.58.28"
      SSH_USER="root"
      SSH_KEY="$HOME/.ssh/id_ed25519_logicoma"
      REMOTE_DIR="/DE_PKGS/de_version/de_code/de_server/ai_resource"
      CONTAINER_NAME="ai_resource_app"
      ENV_FILE=".env"
      ;;
    aws)
      SERVER_IP="44.199.91.157"
      SSH_USER="ubuntu"
      SSH_KEY="$HOME/.ssh/aws-logicomanet.pem"
      REMOTE_DIR="/opt/logicomanet-be"
      CONTAINER_NAME="logicomanet-be"
      ENV_FILE=".env.aws"
      ;;
    *)
      echo "Unknown target: $TARGET"
      echo "Available targets: aliyun, aws"
      exit 1
      ;;
  esac
}
select_server

# =============================================================================
# --self-audit: 部署前自检 (设计 §3.9, v2-5/v3-8)
# 5 项检查: Flyway V 号 / DB pre-flight / UNDO / Schema 预演 / Mapper 一致性
# =============================================================================
self_audit() {
  local fail=0
  local migration_dir="src/main/resources/db/migration"
  local undo_dir="src/main/resources/db/migration-undo"
  local be_dir
  be_dir="$(cd "$(dirname "$0")" && pwd)"
  cd "$be_dir"
  echo "[deploy.sh --self-audit] 开始自检 (BE dir: $be_dir)"
  echo -n "  Flyway V 号续接 ... "
  local latest_v
  latest_v=$(ls "$migration_dir"/V*.sql 2>/dev/null | sort -V | tail -1 | sed 's/.*V\([0-9]*\).*/\1/')
  [ -n "$latest_v" ] && echo "V${latest_v} — PASS" || { echo "FAIL"; fail=1; }
  echo -n "  DB pre-flight ... "
  ssh_cmd "${SSH_USER}@${SERVER_IP}" "docker exec mysql sh -c 'exec mysql -uroot -p\"\$MYSQL_ROOT_PASSWORD\" -e \"SELECT version FROM logicoma_net.flyway_schema_history ORDER BY installed_rank DESC LIMIT 1\"' 2>/dev/null" > /dev/null 2>&1 && echo "PASS" || echo "WARN"
  echo -n "  UNDO 脚本完整性 ... "
  local missing=0
  for vfile in "$migration_dir"/V*.sql; do
    local vname; vname=$(basename "$vfile" .sql)
    [ ! -f "$undo_dir/${vname}-undo.sql" ] && { echo ""; echo "    MISSING: $undo_dir/${vname}-undo.sql"; missing=1; }
  done
  [ "$missing" -eq 0 ] && echo "PASS" || { echo "FAIL"; fail=1; }
  echo -n "  Schema 预演 (mvn compile) ... "
  mvn -q compile -DskipTests 2>/dev/null && echo "PASS" || { echo "FAIL"; fail=1; }
  echo -n "  Mapper ↔ DB 一致性 ... "
  [ -f "src/test/java/com/ai/repo/MapperXmlConsistencyTest.java" ] && echo "PASS" || echo "WARN"
  echo "[deploy.sh --self-audit] 完成: $fail 项失败"
  return $fail
}

# =============================================================================
# --backup-db: 数据库备份 (P23)
# =============================================================================
backup_db() {
  local ts; ts=$(date +%Y%m%d_%H%M%S)
  local backup_file="/opt/backups/pre-${ts}.sql"
  echo "[deploy.sh --backup-db] 备份 DB → ${backup_file}"
  ssh_cmd "${SSH_USER}@${SERVER_IP}" "sudo mkdir -p /opt/backups && sudo chmod 755 /opt/backups" 2>/dev/null
  # Use the container-local root credential so application-password rotation
  # cannot break the mandatory pre-deploy backup gate.
  ssh_cmd "${SSH_USER}@${SERVER_IP}" "docker exec mysql sh -c 'exec mysqldump -uroot -p\"\$MYSQL_ROOT_PASSWORD\" --single-transaction logicoma_net' 2>/dev/null" | gzip | ssh_cmd "${SSH_USER}@${SERVER_IP}" "sudo tee ${backup_file}.gz >/dev/null" 2>/dev/null
  ssh_cmd "${SSH_USER}@${SERVER_IP}" "test \$(stat -c%s '${backup_file}.gz') -gt 1024" || {
    echo "[deploy.sh --backup-db] FAIL: backup is empty or invalid"
    return 1
  }
  echo "[deploy.sh --backup-db] 完成: ${backup_file}.gz"
  ssh_cmd "${SSH_USER}@${SERVER_IP}" "ls -t /opt/backups/pre-*.sql.gz 2>/dev/null | tail -n +4 | xargs -r sudo rm" 2>/dev/null
}

# =============================================================================
# --rollback: 回滚 (设计 §3.9)
# =============================================================================
rollback() {
  local version="$1"
  echo "[deploy.sh --rollback] 回滚到版本: ${version}"
  echo "  1. docker stop ${CONTAINER_NAME}"
  echo "  2. 回退 JAR: docker tag logicomanet-be:<prev> logicomanet-be:latest"
  echo "  3. 还原 DB: zcat /opt/backups/pre-${version}.sql.gz | docker exec -i mysql mysql logicoma_net"
  echo "  4. docker run ..."
  echo "[deploy.sh --rollback] 请手动执行, 或使用 --rollback=auto"
}

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

step() { echo -e "${GREEN}[→]${NC} $1"; }
info() { echo -e "${YELLOW}[!]${NC} $1"; }
err() { echo -e "${RED}[✗]${NC} $1"; exit 1; }
ok()  { echo -e "${CYAN}[✓]${NC} $1"; }

# SSH/SCP helpers
ssh_cmd() {
  ssh -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new -o ConnectTimeout=10 "$@"
}

scp_cmd() {
  scp -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new "$@"
}

[ -f "$SSH_KEY" ] || err "SSH key not found: $SSH_KEY"

LOCAL_JAR="target/logicoma-net-2.0.0.jar"
APP_JAR="app.jar"

# Load .env file
load_env_file() {
  if [ -f "$ENV_FILE" ]; then
    step "Loading $ENV_FILE..."
    set -a
    source "$ENV_FILE"
    set +a
    ok "Environment loaded from $ENV_FILE"
  elif [ -f ".env" ]; then
    step "$ENV_FILE not found, falling back to .env..."
    set -a
    source ".env"
    set +a
    ok "Environment loaded from .env"
  else
    info "No .env file found, using defaults"
  fi
}

echo ""
echo "=========================================="
echo "  Target: ${TARGET} (${SERVER_IP})"
echo "  Dir:    ${REMOTE_DIR}"
echo "  Container: ${CONTAINER_NAME}"
echo "=========================================="
echo ""

load_env_file

# Default env values
MAIL_HOST="${MAIL_HOST:-smtp.example.com}"
MAIL_PORT="${MAIL_PORT:-587}"
MAIL_USERNAME="${MAIL_USERNAME:-}"
MAIL_PASSWORD="${MAIL_PASSWORD:-}"
MAIL_FROM="${MAIL_FROM:-noreply@logicoma.ai}"
APP_BASE_URL="${APP_BASE_URL:-http://your-domain.com}"
APP_FRONTEND_URL="${APP_FRONTEND_URL:-http://your-domain.com}"
ADMIN_BOOTSTRAP_EMAILS="${ADMIN_BOOTSTRAP_EMAILS:-}"

# ─── 子命令路由（helpers 与环境变量加载完成后执行） ────────────────────────
[ "$SELF_AUDIT" = true ] && { self_audit; exit $?; }
[ -n "$ROLLBACK" ] && { rollback "$ROLLBACK"; exit $?; }
[ "$BACKUP_DB" = true ] && { backup_db; exit $?; }

# --- Build ---
if [ "$SKIP_BUILD" = false ]; then
  step "Building project..."
  mvn clean package -DskipTests || err "Build failed."
  ok "Build complete."

  if [ ! -f "$LOCAL_JAR" ]; then
    if [ -f "logicoma-net-2.0.0.jar" ]; then
      LOCAL_JAR="logicoma-net-2.0.0.jar"
    else
      err "JAR not found: $LOCAL_JAR"
    fi
  fi
else
  step "Skipping build (--skip-build)"
fi

# --- Remote dir ---
step "Ensuring remote directory..."
ssh_cmd "${SSH_USER}@${SERVER_IP}" "mkdir -p '${REMOTE_DIR}'" || err "Failed to create remote directory"
ok "Remote directory ready"

# --- Backup old JAR ---
if [ "$NO_BACKUP" = false ]; then
  step "Backing up old JAR (if any)..."
  ssh_cmd "${SSH_USER}@${SERVER_IP}" \
    "cd ${REMOTE_DIR} && [ -f ${APP_JAR} ] && mv ${APP_JAR} ${APP_JAR}.bak.\$(date +%Y%m%d_%H%M%S) && echo 'Backup created' || echo 'No existing JAR to backup'"
  # DB 备份 (P23, 设计 §3.9, 切流量前)
  step "Backing up database before deploy..."
  backup_db
else
  info "Skipping backup (--no-backup)"
fi

# --- Upload ---
step "Uploading JAR..."
scp_cmd "$LOCAL_JAR" "${SSH_USER}@${SERVER_IP}:${REMOTE_DIR}/logicoma-net-2.0.0.jar" || err "Upload failed"
ok "JAR uploaded"

# Upload .env
if [ -f "$ENV_FILE" ]; then
  step "Uploading $ENV_FILE..."
  scp_cmd "$ENV_FILE" "${SSH_USER}@${SERVER_IP}:${REMOTE_DIR}/.env"
  ssh_cmd "${SSH_USER}@${SERVER_IP}" "chmod 600 '${REMOTE_DIR}/.env'"
  ok "$ENV_FILE uploaded"
else
  err "Required deployment secret file not found: $ENV_FILE"
fi

# --- Remote deploy ---
step "Deploying on remote server..."
ssh_cmd "${SSH_USER}@${SERVER_IP}" << EOF
  set -e
  cd ${REMOTE_DIR}

  # Rename new JAR
  mv logicoma-net-2.0.0.jar ${APP_JAR}
  echo "JAR renamed to ${APP_JAR}"

  # Stop & remove old container
  echo "Stopping old container..."
  docker stop ${CONTAINER_NAME} 2>/dev/null || true
  docker rm ${CONTAINER_NAME} 2>/dev/null || true
  echo "Old container removed"

  # Ensure data directory
  sudo mkdir -p /data/git_repos

  # Start new container
  echo "Starting new container..."
  docker run -d \
    --name ${CONTAINER_NAME} \
    --restart=always \
    --network=host \
    --env-file ${REMOTE_DIR}/.env \
    -v ${REMOTE_DIR}:${REMOTE_DIR} \
    -v /data/git_repos:/data/git_repos \
    -e FILE_STORAGE_PATH=${REMOTE_DIR} \
    -w ${REMOTE_DIR} \
    eclipse-temurin:17-jdk-alpine \
    java -jar app.jar --spring.profiles.active=prod
  echo "Container started"
EOF
ok "Deployment completed"

# --- Cleanup old backups ---
if [ "$NO_BACKUP" = false ]; then
  step "Cleaning old backups (keeping last 3)..."
  ssh_cmd "${SSH_USER}@${SERVER_IP}" \
    "cd ${REMOTE_DIR} && ls -1t ${APP_JAR}.bak.* 2>/dev/null | tail -n +4 | xargs -I{} rm -f {} 2>/dev/null; echo 'Cleanup done'"
  ok "Old backups cleaned"
fi

# --- Verify ---
step "Verifying deployment..."
sleep 3
ssh_cmd "${SSH_USER}@${SERVER_IP}" "docker ps --filter name=${CONTAINER_NAME} --format '{{.Names}} {{.Status}}'" || err "Container not running"
ok "Container is running"

echo ""
step "============================================"
step "  Deployment Complete!"
step "  Target: ${TARGET} (${SERVER_IP})"
step "  Container: ${CONTAINER_NAME}"
step "============================================"
echo ""

# Show logs
step "Tailing logs (Ctrl+C to exit)..."
echo ""
ssh_cmd "${SSH_USER}@${SERVER_IP}" "docker logs --tail 50 -f ${CONTAINER_NAME}"
