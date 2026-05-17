#!/bin/bash
# 快速部署脚本 (跳过编译，直接上传已构建的 jar)
# 使用: ./deploy-ready.sh <ssh-password>

PASSWORD=$1
if [ -z "$PASSWORD" ]; then
    echo "用法: ./deploy-ready.sh <ssh-password>"
    exit 1
fi

SERVER_IP="111.231.58.28"
USER="root"
REMOTE_DIR="/DE_PKGS/de_version/de_code/de_server/ai_resource"
CONTAINER_NAME="ai_resource_app"
APP_JAR="app.jar"
LOCAL_JAR="target/logicoma-net-2.0.0.jar"

echo "开始快速部署..."

# 上传
echo "上传 jar 包..."
sshpass -p "$PASSWORD" scp -o StrictHostKeyChecking=no "$LOCAL_JAR" ${USER}@${SERVER_IP}:${REMOTE_DIR}/logicoma-net-2.0.0.jar

if [ $? -ne 0 ]; then
    echo "❌ 上传失败！"
    exit 1
fi

# 远程执行
echo "重启容器..."
sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no ${USER}@${SERVER_IP} << 'REMOTE'
cd /DE_PKGS/de_version/de_code/de_server/ai_resource

# 备份
if [ -f "app.jar" ]; then
    mv app.jar app.jar.bak.$(date +"%Y%m%d_%H%M%S")
fi

# 替换
mv logicoma-net-2.0.0.jar app.jar

# 重启
docker restart ai_resource_app
echo "✅ 部署完成"
docker logs --tail 20 ai_resource_app
REMOTE

echo "=== 部署完成 ==="
