# Agent Package Manager — 功能设计文档

## 1. 功能背景

### 1.1 当前现状

现有系统中，Agent 的 **Skill** 和 **Memory** 的文件管理存在以下问题：

| 问题 | 说明 |
|------|------|
| 仅支持 `.md` 单文件 | `file.storage.allowed-extensions: .md` 硬性限制 |
| 无版本管理 | 同名文件直接覆盖，无法回退 |
| 无多文件支持 | 不能上传一个技能包（多个相关文件） |
| 无社区协作 | 没有 Fork/PR/Review 机制 |
| DB 记录缺失 | `FileUploadLog.insert()` 已被注释，下载/列表全部不可用 |
| 无公开/私有控制 | 上传即所有人可见，粒度不够 |
| Skill 走 JGit 协议 | 需要完整的 Git 客户端，门槛高 |

### 1.2 目标

构建一个 **Package Manager（包管理器）**，让用户可以将 Agent 的 Skill 或 Memory 以 **多文件包** 的形式上传、版本管理、公开/私有控制，并支持社区贡献与审核。

---

## 2. 功能设计

### 2.1 核心概念

| 概念 | 说明 |
|------|------|
| **Package（包）** | 一个 Skill 或 Memory 的集合，包含元数据 + 多版本文件 |
| **Version（版本）** | 包的一个不可变快照，包含一组文件；**一旦创建不可删除** |
| **Contribution（贡献PR）** | 外部用户基于某版本修改后提交的变更申请，需要原作者审核 |
| **Rollback（回退）** | 将包的 current_version 指向历史版本 |

### 2.2 功能要点

#### 2.2.1 文件上传
- 支持多文件同时上传（zip 或 multipart 批量）
- 支持文件类型：`.md`, `.json`, `.txt`, `.doc`, `.pdf`, `.py`, `.js`, `.yaml`, `.yml`, `.toml`, `.csv`, `.xml` 等常见文本/文档格式
- 单个文件 ≤ 50MB（沿用现有配置）
- 上传后自动计算 MD5 摘要

#### 2.2.2 存储路径

目录结构如下：

```
{file.storage.base-path}/packages/
├── skill/
│   └── {userId}/
│       └── {agentId}/
│           └── {packageName}/
│               ├── v1_20260622_120000/
│               │   ├── manifest.json
│               │   ├── main.py
│               │   └── README.md
│               └── v2_20260623_150000/
│                   ├── manifest.json
│                   ├── main.py
│                   └── README.md
└── memory/
    └── {userId}/
        └── {agentId}/
            └── {packageName}/
                └── v1_20260622_120000/
                    └── knowledge_base.md
```

路径规则：`{basePath}/packages/{type}/{userId}/{agentId}/{packageName}/{versionTag}/`

其中 `versionTag = v{序号}_{yyyyMMdd_HHmmss}`，保证全局唯一、按时间排序。

#### 2.2.3 公开/私有控制
- 新建包默认 **私有**（仅创建者自己和所属 Agent 可见/可下载）
- 创建者可随时切换为 **公开**（所有人可见/可下载）
- 切换回私有仅影响后续访问，之前已下载的不受影响
- 公开包才能在社区中被搜索和贡献

#### 2.2.4 版本管理
- 每次发布新版本生成一个 **不可变快照**（文件物理复制 + DB 记录）
- 所有历史版本 **永久保留**，不可删除
- 创建者可随时 **回退** 到任意历史版本（仅修改 current_version_id 指针）

#### 2.2.5 社区贡献流程

```
┌────────────────────────────────────────────────────────────┐
│                    贡献PR工作流                              │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  贡献者 (User B)             创建者 (User A)                │
│       │                            │                        │
│       ▼                            │                        │
│  下载包 (某版本 V1)                 │                        │
│       │                            │                        │
│       ▼                            │                        │
│  本地编辑文件                       │                        │
│       │                            │                        │
│       ▼                            │                        │
│  POST /contributions              │                        │
│  上传修改后的文件                   │                        │
│       │                            │                        │
│       │    ┌───────────────────────┤                        │
│       │    │  收到审核通知          │                        │
│       │    ▼                       │                        │
│       │  查看变更文件 Diff          │                        │
│       │    │                       │                        │
│       │    ├─ 通过 → 自动创建新版本 V2  → 贡献者收到通知    │
│       │    └─ 驳回 → 标记 rejected → 贡献者收到通知        │
│       │                            │                        │
└────────────────────────────────────────────────────────────┘
```

关键约束：
- 贡献者不能自审自批
- 一个贡献PR一旦 merged/rejected 不可修改
- 被驳回的 PR，贡献者可以修改后重新提交

#### 2.2.6 安全与校验

| 检查项 | 说明 |
|--------|------|
| 文件类型白名单 | 仅允许预设的安全类型（防止 exe/bat/sh） |
| 文件大小限制 | 单文件 ≤ 50MB |
| MD5 校验 | 上传时记录，下载时校验完整性 |
| 路径穿越防护 | sanitizePath() 禁止 `..`、绝对路径、空字节 |
| 内容审核 | 复用现有 MarkdownSecurityService + OpenAI Moderation（可配置） |
| 所有权校验 | 仅包创建者可发布版本/审核 PR/修改可见性 |

---

## 3. 数据库表设计

### 3.1 agent_packages — 包主表

```sql
CREATE TABLE agent_packages (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL        COMMENT '创建者用户ID',
    agent_id        BIGINT          NOT NULL        COMMENT '关联的Agent ID',
    package_type    VARCHAR(10)     NOT NULL        COMMENT '类型: skill / memory',
    name            VARCHAR(100)    NOT NULL        COMMENT '包名称（字母数字下划线）',
    description     TEXT                            COMMENT '描述',
    tags            VARCHAR(500)                    COMMENT '逗号分隔的标签',
    is_public       TINYINT(1)      NOT NULL DEFAULT 0   COMMENT '是否公开: 0私有 1公开',
    current_version_id  BIGINT                      COMMENT '当前生效的版本ID',
    download_count  INT             NOT NULL DEFAULT 0   COMMENT '累计下载次数',
    created_at      DATETIME        NOT NULL        COMMENT '创建时间',
    updated_at      DATETIME                        COMMENT '最后更新时间',

    UNIQUE KEY uk_agent_type_name (agent_id, package_type, name),
    KEY idx_user_id (user_id),
    KEY idx_is_public (is_public),
    KEY idx_package_type (package_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent包记录';
```

### 3.2 package_versions — 版本记录表（不可删除）

```sql
CREATE TABLE package_versions (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    package_id      BIGINT          NOT NULL        COMMENT '关联包ID',
    version_tag     VARCHAR(50)     NOT NULL        COMMENT '版本标签: v1/v2/v3...',
    storage_path    VARCHAR(500)    NOT NULL        COMMENT '文件存储的磁盘路径',
    file_count      INT             NOT NULL DEFAULT 0   COMMENT '文件数量',
    total_size      BIGINT          NOT NULL DEFAULT 0   COMMENT '所有文件总大小(字节)',
    commit_message  TEXT                            COMMENT '版本发布说明',
    status          VARCHAR(20)     NOT NULL DEFAULT 'active' COMMENT '状态: active/superseded',
    source_contribution_id  BIGINT                  COMMENT '来源贡献PR ID(空=原始版本)',
    created_at      DATETIME        NOT NULL        COMMENT '创建时间',

    UNIQUE KEY uk_package_version (package_id, version_tag),
    KEY idx_package_id (package_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='包版本快照';
```

> `status = 'superseded'` 表示该版本已被新版本取代；`'active'` 表示当前最新。

### 3.3 package_files — 版本内文件清单

```sql
CREATE TABLE package_files (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    version_id      BIGINT          NOT NULL        COMMENT '关联版本ID',
    file_name       VARCHAR(255)    NOT NULL        COMMENT '文件名',
    file_path       VARCHAR(500)    NOT NULL        COMMENT '包内相对路径（含文件名）',
    file_size       BIGINT          NOT NULL        COMMENT '文件大小(字节)',
    mime_type       VARCHAR(100)                    COMMENT 'MIME类型',
    md5_hash        VARCHAR(64)     NOT NULL        COMMENT '文件MD5摘要(hex)',
    created_at      DATETIME        NOT NULL        COMMENT '创建时间',

    KEY idx_version_id (version_id),
    KEY idx_file_path (file_path(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='版本内文件记录';
```

### 3.4 package_contributions — 贡献PR记录

```sql
CREATE TABLE package_contributions (
    id                  BIGINT          PRIMARY KEY AUTO_INCREMENT,
    package_id          BIGINT          NOT NULL        COMMENT '关联包ID',
    source_version_id   BIGINT          NOT NULL        COMMENT '基于哪个版本修改',
    contributor_user_id BIGINT                          COMMENT '贡献者用户ID(与agent二选一)',
    contributor_agent_id BIGINT                         COMMENT '贡献者Agent ID(与user二选一)',
    commit_message      TEXT                            COMMENT '提交说明(为何修改)',
    status              VARCHAR(20)     NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected/merged',
    reviewed_by         BIGINT                          COMMENT '审核人用户ID',
    reviewed_at         DATETIME                        COMMENT '审核时间',
    review_comment      TEXT                            COMMENT '审核意见/驳回理由',
    target_version_id   BIGINT                          COMMENT '审核通过后产生的新版本ID',
    created_at          DATETIME        NOT NULL        COMMENT '创建时间',

    KEY idx_package_id (package_id),
    KEY idx_status (status),
    KEY idx_contributor (contributor_user_id, contributor_agent_id),
    KEY idx_reviewed_by (reviewed_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='贡献PR(提交+审核记录)';
```

约束逻辑（应用层）：
- `contributor_user_id` 和 `contributor_agent_id` 不能同时为空
- `reviewed_by` 不能等于 `contributor_user_id`（不能自审）
- 一个 PR 仅能通过/驳回一次

### 3.5 contribution_files — 贡献PR中修改的文件

```sql
CREATE TABLE contribution_files (
    id                  BIGINT          PRIMARY KEY AUTO_INCREMENT,
    contribution_id     BIGINT          NOT NULL        COMMENT '关联贡献PR ID',
    file_name           VARCHAR(255)    NOT NULL        COMMENT '文件名',
    file_path           VARCHAR(500)    NOT NULL        COMMENT '包内相对路径',
    file_size           BIGINT          NOT NULL        COMMENT '文件大小',
    md5_hash            VARCHAR(64)     NOT NULL        COMMENT '文件MD5摘要',
    storage_path        VARCHAR(500)    NOT NULL        COMMENT '暂存路径(审核期间临时存放)',
    action              VARCHAR(20)     NOT NULL DEFAULT 'modified' COMMENT 'added/modified/deleted',
    created_at          DATETIME        NOT NULL        COMMENT '创建时间',

    KEY idx_contribution_id (contribution_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='贡献PR中修改的文件列表';
```

因为用户说"其他用户也可以对下载后的进行编辑后进行commit，由原本用户/agent进行审核，同意修改后进行保存"，所以必须暂存贡献者的文件，直到审核通过才并入正式版本目录。

### 3.6 package_downloads — 下载记录

```sql
CREATE TABLE package_downloads (
    id                  BIGINT          PRIMARY KEY AUTO_INCREMENT,
    package_id          BIGINT          NOT NULL        COMMENT '关联包ID',
    version_id          BIGINT          NOT NULL        COMMENT '下载的版本ID',
    downloader_user_id  BIGINT                          COMMENT '下载者用户ID(与agent二选一)',
    downloader_agent_id BIGINT                          COMMENT '下载者Agent ID(与user二选一)',
    created_at          DATETIME        NOT NULL        COMMENT '下载时间',

    KEY idx_package_id (package_id),
    KEY idx_downloader (downloader_user_id, downloader_agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='下载记录';
```

---

## 4. 功能流程框架

### 4.1 首次创建包 + 上传文件

```
User/Agent
  │
  ├─ POST /api/packages
  │   Body: { agentId, type: "skill", name: "weather-skill", description, tags }
  │   → 创建 agent_packages 记录，返回 package_id
  │
  ├─ POST /api/packages/{id}/versions (multipart)
  │   Fields: files[] + commit_message
  │   → 1. 校验文件类型/大小
  │   → 2. 计算 MD5
  │   → 3. 创建存储目录 {basePath}/packages/skill/{userId}/{agentId}/weather-skill/v1_20260622_120000/
  │   → 4. 保存所有文件到该目录
  │   → 5. 创建 package_versions 记录 (version_tag="v1")
  │   → 6. 创建 package_files 记录（每个文件一条）
  │   → 7. 更新 agent_packages.current_version_id = 新 version_id
  │   → 8. 返回版本信息
```

### 4.2 发布新版本（owner）

```
POST /api/packages/{id}/versions (multipart)
  → 校验当前用户是包创建者
  → 1. 获取当前最大版本号，+1 生成 v2/v3...
  → 2. 创建新目录 v2_20260623_150000/
  → 3. 保存文件、创建 DB 记录
  → 4. 旧版本 status → 'superseded'
  → 5. 更新 current_version_id
```

### 4.3 切换可见性

```
PATCH /api/packages/{id}/visibility?isPublic=true
  → 仅包创建者可操作
  → 更新 agent_packages.is_public
  → 公开时可在公共列表中被搜索
```

### 4.4 下载包

```
GET /api/packages/{id}/versions/{versionId}/files/{fileId}/download
  → 如果是私有包，校验下载者是创建者
  → 读取磁盘文件，返回流
  → 记录 package_downloads
  → 递增 agent_packages.download_count
```

批量下载：
```
GET /api/packages/{id}/versions/{versionId}/download
  → 打包所有文件为 ZIP 流返回
```

### 4.5 提交贡献PR

```
POST /api/packages/{id}/contributions (multipart)
  → 校验包是公开的
  → 校验贡献者不是包创建者（不能自提PR）
  │
  ├─ 1. 暂存文件到 {basePath}/contributions/tmp_{contributionId}/
  ├─ 2. 创建 package_contributions 记录（status='pending'）
  ├─ 3. 创建 contribution_files 记录
  ├─ 4. 通知包创建者（Notification）
  └─ 5. 返回 contribution_id
```

### 4.6 审核贡献PR

```
PUT /api/packages/{id}/contributions/{cid}
  Body: { status: "approved"|"rejected", reviewComment: "..." }
  → 仅包创建者可审核
  → 校验 status 合法性
  │
  ├─ 如果 approved:
  │   ├─ 1. 创建新版本目录 v{n+1}_{timestamp}/
  │   ├─ 2. 复制基础版本的文件（未被修改的）
  │   ├─ 3. 合并 contribution_files 中的修改
  │   ├─ 4. 创建 package_versions 记录
  │   ├─ 5. 创建 package_files 记录（所有文件）
  │   ├─ 6. 旧版本 status → 'superseded'
  │   ├─ 7. 更新 current_version_id
  │   ├─ 8. contribution.source_version_id → 新版本ID
  │   └─ 9. contribution.status → 'merged'
  │
  └─ 如果 rejected:
      ├─ contribution.status → 'rejected'
      ├─ 清理暂存的 contribution_files
      └─ 通知贡献者
```

### 4.7 回退版本

```
POST /api/packages/{id}/rollback
  Body: { versionId: 回退到的版本ID }
  → 仅包创建者可操作
  → 1. 校验目标版本存在且属于该包
  → 2. 更新 current_version_id = 目标版本ID
  → 3. 记录回退事件（可选通过 contribution 记录或新增日志）
  → 注意：不会删除或覆盖任何已有版本
```

### 4.8 搜索与浏览

```
GET  /api/packages/public?page=&size=         → 浏览公开包
GET  /api/packages/search?q=keyword&page=&size= → 搜索（匹配 name/description/tags）
GET  /api/packages/agent/{agentId}            → Agent 的所有包
GET  /api/packages/user/{userId}              → 用户的所有包
GET  /api/packages/{id}/versions              → 版本列表（含版本号/时间/文件数）
GET  /api/packages/{id}/contributions         → 贡献PR列表
```

---

## 5. 与现存模块的关系

| 现存模块 | 关系 |
|---------|------|
| `MemoryController` | 废弃旧 upload/download 端点，新功能统一走 Package API |
| `FileStorageServiceImpl` | 废弃或重构，新功能用 `PackageStorageService` 替代 |
| `FileUploadLog` | 废弃，改用 `package_files` 表 |
| `FileController` | 保留查询功能或废弃 |
| `SkillRepositoryController` / JGit | **保持独立**，Package 是轻量替代方案，不冲突 |
| `ContentModerationService` | 复用，新上传也走内容审核 |
| `RateLimit` | 复用，上传/下载可加限流 |

---

## 6. 配置变更

`application.yml` 新增：

```yaml
package:
  storage:
    base-path: ${file.storage.base-path}/packages
    allowed-extensions: .md,.json,.txt,.doc,.pdf,.py,.js,.yaml,.yml,.toml,.csv,.xml,.html,.css,.svg,.png,.jpg,.jpeg,.gif
    max-file-size-mb: 50
    max-files-per-version: 100
```

---

## 7. 后续可扩展

| 方向 | 说明 |
|------|------|
| **依赖管理** | 包可以声明依赖其他包，发布时自动解析 |
| **语义版本号** | 从 v1/v2 升级到 semver (1.0.0, 2.1.0) |
| **版本对比API** | 提供两个版本之间的文件 Diff |
| **WebHook** | 版本发布/PR审核时推送通知到外部 URL |
| **评分/评论** | 复用现有 RepoRating 逻辑到包 |