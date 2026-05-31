#!/bin/bash

# ==========================================
# Logicoma 后端自动化本地编译 -> 远程部署脚本
# 特性：使用 SSH 密钥认证，全程无需输入密码
# 支持 .env 文件配置敏感信息
# ==========================================

# 1. 远程服务器配置
SERVER_IP="111.231.58.28"
USER="root"
REMOTE_DIR="/DE_PKGS/de_version/de_code/de_server/ai_resource"
CONTAINER_NAME="ai_resource_app"
APP_JAR="app.jar"
ENV_FILE=".env"

# 2. 本地项目配置
LOCAL_JAR="target/logicoma-net-2.0.0.jar"
SSH_KEY="$HOME/.ssh/id_ed25519_logicoma"

# ==========================================
# 从 .env 文件加载环境变量
# ==========================================
load_env_file() {
    if [ -f "$ENV_FILE" ]; then
        echo "📁 发现 .env 文件，加载配置..."
        set -a  # 自动导出
        source "$ENV_FILE"
        set +a
    else
        echo "⚠️  未找到 .env 文件，使用环境变量或默认值"
    fi
}

echo "=========================================="
echo " 1. 加载配置..."
echo "=========================================="
load_env_file

# 设置默认值（如果 .env 中未定义）
MAIL_HOST="${MAIL_HOST:-smtp.example.com}"
MAIL_PORT="${MAIL_PORT:-587}"
MAIL_USERNAME="${MAIL_USERNAME:-}"
MAIL_PASSWORD="${MAIL_PASSWORD:-}"
MAIL_FROM="${MAIL_FROM:-noreply@logicoma.ai}"
APP_BASE_URL="${APP_BASE_URL:-http://your-domain.com}"
APP_FRONTEND_URL="${APP_FRONTEND_URL:-http://your-domain.com}"

echo "=========================================="
echo " 2. 开始本地编译打包..."
echo "=========================================="
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ 本地编译失败，请检查您的 Java/Maven 代码！"
    exit 1
fi

if [ ! -f "$LOCAL_JAR" ]; then
    if [ -f "logicoma-net-2.0.0.jar" ]; then
        LOCAL_JAR="logicoma-net-2.0.0.jar"
    else
        echo "❌ 未找到打包后的 logicoma-net-2.0.0.jar 文件！请检查目录。"
        exit 1
    fi
fi

echo "=========================================="
echo " 3. 上传文件至服务器..."
echo "=========================================="

# 上传 JAR 包
scp -o StrictHostKeyChecking=no -i "$SSH_KEY" "$LOCAL_JAR" ${USER}@${SERVER_IP}:${REMOTE_DIR}/logicoma-net-2.0.0.jar

if [ $? -ne 0 ]; then
    echo "❌ 上传 JAR 包失败！"
    exit 1
fi

# 上传 .env 文件（如果存在）
if [ -f "$ENV_FILE" ]; then
    scp -o StrictHostKeyChecking=no -i "$SSH_KEY" "$ENV_FILE" ${USER}@${SERVER_IP}:${REMOTE_DIR}/.env
    if [ $? -eq 0 ]; then
        echo "✅ .env 文件已上传"
    fi
fi

echo "=========================================="
echo " 4. 远程执行替换与重启..."
echo "=========================================="
ssh -o StrictHostKeyChecking=no -i "$SSH_KEY" ${USER}@${SERVER_IP} << EOF
    cd ${REMOTE_DIR} || { echo "❌ 找不到远程目录 ${REMOTE_DIR}"; exit 1; }

    # 备份旧的 app.jar
    if [ -f "${APP_JAR}" ]; then
        BACKUP_NAME="${APP_JAR}.bak.\$(date +"%Y%m%d_%H%M%S")"
        mv ${APP_JAR} \${BACKUP_NAME}
        echo "✅ 已将旧版本备份为: \${BACKUP_NAME}"
    fi

    # 重命名新上传的包
    mv logicoma-net-2.0.0.jar ${APP_JAR}
    echo "✅ 新包已重命名为 ${APP_JAR}"

    # 停止并删除旧容器（保留镜像）
    echo "🛑 停止旧容器..."
    docker stop ${CONTAINER_NAME} || true
    docker rm ${CONTAINER_NAME} || true

    # 启动新容器（带环境变量）
    echo "🚀 启动新容器..."
    docker run -d \
        --name ${CONTAINER_NAME} \
        --restart=always \
        --network=host \
        -v /DE_PKGS/de_version/de_code/de_server/ai_resource:/DE_PKGS/de_version/de_code/de_server/ai_resource \
        -e MAIL_HOST=${MAIL_HOST} \
        -e MAIL_PORT=${MAIL_PORT} \
        -e MAIL_USERNAME=${MAIL_USERNAME} \
        -e MAIL_PASSWORD=${MAIL_PASSWORD} \
        -e MAIL_FROM=${MAIL_FROM} \
        -e APP_BASE_URL=${APP_BASE_URL} \
        -e APP_FRONTEND_URL=${APP_FRONTEND_URL} \
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
        -w /DE_PKGS/de_version/de_code/de_server/ai_resource \
        openjdk:17-jdk-alpine \
        java -jar app.jar

    echo "✅ 部署已完成！"
    echo "=========================================="
    echo " 正在打印后端服务最新日志 (按 Ctrl+C 退出):"
    echo "=========================================="

    # 查看日志
    sleep 3
    docker logs --tail 100 -f ${CONTAINER_NAME}
EOF