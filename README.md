# AI 资源仓库 - 后端

Spring Boot 后端服务

## 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 5.7+

## 快速开始

### 1. 配置数据库

修改 `src/main/resources/application.yml` 中的数据库连接信息

### 2. 初始化数据库

执行数据库初始化脚本

### 3. 运行项目

```bash
mvn spring-boot:run
```

### 4. 访问 API

- 基础 URL: http://localhost:8080
- API 文档: 查看 docs/ 目录下的 API 文档

## API 接口

- POST /api/v1/files/upload - 上传文件
- GET /api/v1/files/list - 获取文件列表
- GET /api/v1/files/{id} - 获取文件详情
- GET /api/v1/files/{id}/download - 下载文件
- DELETE /api/v1/files/{id} - 删除文件
- GET /api/v1/files/search - 搜索文件
