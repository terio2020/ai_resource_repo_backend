#!/bin/bash

# ==========================================
# Logicoma 后端自动化本地编译 -> 远程部署脚本
# 特性：使用 SSH 密钥认证，全程无需输入密码
# ==========================================

# 1. 远程服务器配置
SERVER_IP="111.231.58.28"
USER="root"
REMOTE_DIR="/DE_PKGS/de_version/de_code/de_server/ai_resource"
CONTAINER_NAME="ai_resource_app"
APP_JAR="app.jar"

# 2. 本地项目配置
LOCAL_JAR="target/logicoma-net-2.0.0.jar"
SSH_KEY="$HOME/.ssh/id_ed25519_logicoma"

echo "=========================================="
echo " 1. 开始本地编译打包..."
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
echo " 2. 开始上传 Jar 包至服务器..."
echo "=========================================="
scp -o StrictHostKeyChecking=no -i "$SSH_KEY" "$LOCAL_JAR" ${USER}@${SERVER_IP}:${REMOTE_DIR}/logicoma-net-2.0.0.jar

if [ $? -ne 0 ]; then
    echo "❌ 上传失败！"
    exit 1
fi

echo "=========================================="
echo " 3. 远程执行替换与重启..."
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

    # 重启 Docker 容器
    echo "🚀 重启容器: ${CONTAINER_NAME}..."
    docker restart ${CONTAINER_NAME}

    echo "✅ 部署已完成！"
    echo "=========================================="
    echo " 正在打印后端服务最新日志 (按 Ctrl+C 退出):"
    echo "=========================================="

    # 查看日志
    sleep 2
    docker logs --tail 200 -f ${CONTAINER_NAME}
EOF
