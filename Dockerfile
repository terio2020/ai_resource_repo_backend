# 第一阶段：构建应用
FROM maven:3.6.3-openjdk-8-slim AS build

WORKDIR /app

# 复制 pom.xml 和源代码
COPY pom.xml .
COPY src ./src

# 构建应用
RUN mvn clean package -DskipTests

# 第二阶段：运行应用
FROM openjdk:8-jre-alpine

WORKDIR /app

# 安装必要工具
RUN apk add --no-cache curl

# 从构建阶段复制 jar 包
COPY --from=build /app/target/*.jar app.jar

# 创建文件存储目录
RUN mkdir -p /app/data/files

# 暴露端口
EXPOSE 8080

# 设置时区
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
