# Tencent Docs Publish — Skill Design Document

## Overview

A skill that manages project documentation on Tencent Docs. It maintains a mapping between code changes and Tencent Docs documents, automatically syncing technical docs when code changes and supporting manual updates for product/planning documents.

## Trigger Conditions

- **Auto sync**: Code changes detected via git diff (modified/staged/deleted/untracked)
- **Manual trigger**: User explicitly requests updating a specific document (e.g., "update product roadmap", "update sprint progress")

## Configuration

Project root `.tencent-docs-sync.yml`:

```yaml
space:
  name: "Logicoma-Net"
  description: "Logicoma-Net 后端项目文档"

docs:
  - id: api/user
    title: "用户模块 API 文档"
    path: "/API文档/用户模块"
    type: doc
    auto_sync: true
    source_patterns:
      - "UserController.java"
      - "UserService*.java"
      - "UserMapper.java"
      - "entity/User.java"

  - id: api/agent
    title: "Agent 模块 API 文档"
    path: "/API文档/Agent模块"
    type: doc
    auto_sync: true
    source_patterns:
      - "AgentController.java"
      - "AgentService*.java"
      - "AgentMapper.java"
      - "entity/Agent.java"

  - id: architecture
    title: "系统架构"
    path: "/架构设计/系统架构"
    type: smartcanvas
    auto_sync: true
    source_patterns:
      - "pom.xml"
      - "config/*.java"
      - "SecurityConfig.java"

  - id: product/roadmap
    title: "产品路线图"
    path: "/产品设计规划/产品路线图"
    type: smartcanvas
    auto_sync: false

  - id: project/sprint
    title: "当前 Sprint"
    path: "/项目进度管理/当前Sprint"
    type: smartcanvas
    auto_sync: false

  - id: project/changelog
    title: "变更日志"
    path: "/项目进度管理/变更日志"
    type: smartcanvas
    auto_sync: true
```

## Document Space Structure

```
/{space_name}/
├── API文档/
│   ├── 用户模块
│   ├── Agent模块
│   ├── Memory模块
│   ├── Comment模块
│   ├── Skill仓库模块
│   └── 通知/认证模块
├── 架构设计/
│   ├── 系统架构
│   ├── 数据库设计
│   └── 部署架构
├── 开发指南/
│   ├── 快速开始
│   ├── 开发规范
│   └── 测试指南
├── 产品设计规划/
│   ├── 产品路线图
│   ├── 功能规划
│   └── 需求文档
├── 项目进度管理/
│   ├── 当前Sprint
│   ├── 迭代计划
│   └── 变更日志
└── 数据库/
    ├── 表结构说明
    └── 迁移记录
```

## Skill File Structure

```
~/.agents/skills/tencent-docs-publish/
└── SKILL.md
```

## SKILL.md Internal Structure

| Section | Content |
|---------|---------|
| Frontmatter | name + description (trigger conditions) |
| Overview | Core principle + applicable scenarios |
| Phase 0: Init | Read config → create/verify space structure |
| Phase 1: Change Detection | git diff analysis + manual trigger identification |
| Phase 2: Mapping | source_patterns → document list |
| Phase 3: Content Generation | Render Markdown/MDX by doc_type |
| Phase 4: Push | Tencent Docs API calls (create/update) |
| Phase 5: Report | Output update summary |
| Appendix | Config template + FAQ |

## Execution Flow

```
User triggers
  │
  ├─ git diff detected ──→ Phase 0 → Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5
  │
  └─ Manual trigger ──→ Phase 0 → (skip Phase 1-2) → Phase 3 → Phase 4 → Phase 5
```

## Key Design Decisions

1. **Zero external dependencies** — only relies on Tencent Docs MCP tools and git
2. **Config-driven** — `.tencent-docs-sync.yml` is the single configuration point
3. **Idempotent updates** — multiple runs produce no duplicate content
4. **Incremental-first** — technical docs use incremental updates, product docs use full rebuilds
5. **Space-first init** — first run creates the full directory tree in Tencent Docs space

## Tencent Docs MCP Tools Used

| Operation | Tool |
|-----------|------|
| Space management | `query_space_list`, `create_space` |
| Folder creation | `create_space_node` (wiki_folder) |
| Document creation | `manage_create_file` (doc), `create_smartcanvas_by_mdx` (smartcanvas) |
| Content update (DOC) | `doc_get_outline`, `doc_find`, `doc_replace_text`, `doc_insert_markdown`, `doc_get_last_operable_pos` |
| Content update (SmartCanvas) | `smartcanvas_find`, `smartcanvas_edit` (INSERT_AFTER/UPDATE) |
| Document query | `manage_query_file_info`, `manage_search_file` |

## Edge Cases

| Scenario | Behavior |
|----------|----------|
| First run (no space) | Create space + full directory tree + initial docs |
| Config missing | Prompt user to create `.tencent-docs-sync.yml` |
| Source file deleted | Mark corresponding doc section as deprecated/removed |
| No code changes | Skip auto-sync, proceed only if manual trigger |
| Network failure | Retry with exponential backoff, report failure |
| Document already exists | Update in place, do not duplicate |
| New module added (no mapping) | Prompt user to add to config |
