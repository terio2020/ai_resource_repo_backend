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

while [[ $# -gt 0 ]]; do
  case $1 in
    --target=*) TARGET="${1#*=}"; shift ;;
    --skip-build) SKIP_BUILD=true; shift ;;
    --no-backup) NO_BACKUP=true; shift ;;
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
  ok "$ENV_FILE uploaded"
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
    -v ${REMOTE_DIR}:${REMOTE_DIR} \
    -v /data/git_repos:/data/git_repos \
    -e MAIL_HOST=${MAIL_HOST} \
    -e MAIL_PORT=${MAIL_PORT} \
    -e MAIL_USERNAME=${MAIL_USERNAME} \
    -e MAIL_PASSWORD=${MAIL_PASSWORD} \
    -e MAIL_FROM=${MAIL_FROM} \
    -e APP_BASE_URL=${APP_BASE_URL} \
    -e FRONTEND_URL=${APP_FRONTEND_URL} \
    -e OAUTH_GOOGLE_CLIENT_ID=${OAUTH_GOOGLE_CLIENT_ID:-} \
    -e OAUTH_GOOGLE_CLIENT_SECRET=${OAUTH_GOOGLE_CLIENT_SECRET:-} \
    -e OAUTH_GOOGLE_REDIRECT_URI=${OAUTH_GOOGLE_REDIRECT_URI:-} \
    -e OAUTH_GITHUB_CLIENT_ID=${OAUTH_GITHUB_CLIENT_ID:-} \
    -e OAUTH_GITHUB_CLIENT_SECRET=${OAUTH_GITHUB_CLIENT_SECRET:-} \
    -e OAUTH_GITHUB_REDIRECT_URI=${OAUTH_GITHUB_REDIRECT_URI:-} \
    -e OAUTH_APPLE_CLIENT_ID=${OAUTH_APPLE_CLIENT_ID:-} \
    -e OAUTH_APPLE_TEAM_ID=${OAUTH_APPLE_TEAM_ID:-} \
    -e OAUTH_APPLE_KEY_ID=${OAUTH_APPLE_KEY_ID:-} \
    -e OAUTH_APPLE_PRIVATE_KEY=${OAUTH_APPLE_PRIVATE_KEY:-} \
    -e OAUTH_APPLE_REDIRECT_URI=${OAUTH_APPLE_REDIRECT_URI:-} \
    -e OPENAI_API_KEY=${OPENAI_API_KEY:-} \
    -e JWT_SECRET=${JWT_SECRET:-} \
    -e APP_OAUTH_STATE_SECRET=${APP_OAUTH_STATE_SECRET:-} \
    -e TOKEN_ENCRYPTION_SECRET=${TOKEN_ENCRYPTION_SECRET:-} \
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
