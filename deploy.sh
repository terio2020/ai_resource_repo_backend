#!/bin/bash

# ==========================================
# Logicoma 后端自动化本地编译 -> 远程部署脚本
# 特性：启用 SSH 连接复用，全程只需输入一次密码
# ==========================================

# 1. 远程服务器配置 [cite: 109, 110, 111]
SERVER_IP="111.231.58.28" 
USER="root" 
REMOTE_DIR="/DE_PKGS/de_version/de_code/de_server/ai_resource" 
CONTAINER_NAME="ai_resource_app" 
APP_JAR="app.jar" 

# 2. 本地项目配置
LOCAL_JAR="target/logicoma-net-2.0.0.jar" 

# 3. SSH 连接复用配置
SSH_SOCKET="/tmp/logicoma_deploy_socket_$$"

# 确保脚本退出时（无论成功还是异常），自动关闭 SSH 后台主连接
trap 'ssh -S "$SSH_SOCKET" -O exit ${USER}@${SERVER_IP} >/dev/null 2>&1; echo "🔒 SSH 连接已安全关闭。"' EXIT

echo "=========================================="
echo " 1. 开始本地编译打包..."
echo "=========================================="
# [cite: 119]
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
echo " 2. 建立与服务器的共享连接 (这里需要输入密码)"
echo "=========================================="
# 创建主连接：-M 开启主模式，-S 指定套接字，-f 后台运行，-N 不执行命令
ssh -M -S "$SSH_SOCKET" -f -N ${USER}@${SERVER_IP}

if [ $? -ne 0 ]; then
    echo "❌ 无法连接到服务器，请检查密码或网络！"
    exit 1
fi
echo "✅ 连接已建立！接下来的操作将自动免密执行。"

echo "=========================================="
echo " 3. 开始上传 Jar 包至服务器..."
echo "=========================================="
# 使用 -o "ControlPath=..." 复用刚才建立的通道
scp -o "ControlPath=$SSH_SOCKET" "$LOCAL_JAR" ${USER}@${SERVER_IP}:${REMOTE_DIR}/logicoma-net-2.0.0.jar

if [ $? -ne 0 ]; then
    echo "❌ 上传失败！"
    exit 1
fi

echo "=========================================="
echo " 4. 远程执行替换与重启..."
echo "=========================================="
# 同样复用通道执行远程命令
ssh -o "ControlPath=$SSH_SOCKET" ${USER}@${SERVER_IP} << EOF
    # [cite: 128]
    cd ${REMOTE_DIR} || { echo "❌ 找不到远程目录 ${REMOTE_DIR}"; exit 1; }

    # 备份旧的 app.jar [cite: 129]
    if [ -f "${APP_JAR}" ]; then
        BACKUP_NAME="${APP_JAR}.bak.\$(date +"%Y%m%d_%H%M%S")"
        mv ${APP_JAR} \${BACKUP_NAME}
        echo "✅ 已将旧版本备份为: \${BACKUP_NAME}"
    fi

    # 重命名新上传的包 [cite: 130, 131]
    mv logicoma-net-2.0.0.jar ${APP_JAR}
    echo "✅ 新包已重命名为 ${APP_JAR}"

    # 重启 Docker 容器 [cite: 142]
    echo "🚀 重启容器: ${CONTAINER_NAME}..."
    docker restart ${CONTAINER_NAME}
    
    echo "✅ 部署已完成！"
    echo "=========================================="
    echo " 正在打印后端服务最新日志 (按 Ctrl+C 退出):"
    echo "=========================================="
    
    # 查看日志 [cite: 146]
    sleep 2
    docker logs --tail 200 -f ${CONTAINER_NAME}
EOF
