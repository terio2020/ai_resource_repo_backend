# Tencent Docs Publish Skill Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a skill (`tencent-docs-publish`) that manages project documentation on Tencent Docs — auto-syncing technical docs from code changes and supporting manual updates for product/planning docs.

**Architecture:** A single SKILL.md file at `~/.agents/skills/tencent-docs-publish/SKILL.md` organized in phases (Init -> Change Detection -> Mapping -> Content Generation -> Push -> Report). Config-driven via `.tencent-docs-sync.yml` in project root.

**Tech Stack:** Tencent Docs MCP tools, git, Markdown/MDX rendering

## Global Constraints

- Skill name: `tencent-docs-publish`
- Skill location: `~/.agents/skills/tencent-docs-publish/SKILL.md`
- Config file: `.tencent-docs-sync.yml` in project root
- DOC type documents use `insert_markdown` for content updates
- SmartCanvas documents use `create_smartcanvas_by_mdx` for content updates
- Space operations use `query_space_list`, `create_space`, `create_space_node`
- No external dependencies beyond Tencent Docs MCP and git
- Follow existing skill conventions from `~/.agents/skills/doc-sync/SKILL.md`

---

### Task 1: Create Skill Directory and Frontmatter

**Files:**
- Create: `~/.agents/skills/tencent-docs-publish/SKILL.md`

**Interfaces:**
- Consumes: nothing
- Produces: SKILL.md with frontmatter + overview + execution flow diagram

- [ ] **Step 1: Create directory and write frontmatter + overview**

```bash
mkdir -p ~/.agents/skills/tencent-docs-publish
```

Write to `~/.agents/skills/tencent-docs-publish/SKILL.md`:

```markdown
---
name: tencent-docs-publish
description: Use when code changes need to be reflected in Tencent Docs project documentation, or when manually updating product/planning/progress docs on Tencent Docs. Triggers: git diff detected with doc-relevant changes, or user says "update docs", "sync docs", "publish docs", "update product roadmap", "update sprint progress", "update architecture docs".
---

# Tencent Docs Publish — Project Documentation Sync

## Overview

A skill that manages project documentation on Tencent Docs. It maintains a mapping between code changes and Tencent Docs documents, automatically syncing technical docs when code changes and supporting manual updates for product/planning documents.

**Core principle:** Config-driven, idempotent, incremental-first.

## Execution Flow

```
User triggers
  |
  +- git diff detected ---> Phase 0 -> Phase 1 -> Phase 2 -> Phase 3 -> Phase 4 -> Phase 5
  |
  +- Manual trigger ---> Phase 0 -> (skip Phase 1-2) -> Phase 3 -> Phase 4 -> Phase 5
```

## When to Use

| Trigger | Example |
|---------|---------|
| Code changes detected | After implementing a feature, before committing |
| Manual doc update | "Update product roadmap", "Update sprint progress" |
| First-time setup | New project needs documentation structure |

## Config File: `.tencent-docs-sync.yml`

The config file defines the Tencent Docs space and document mappings:

```yaml
space:
  name: "ProjectName"
  description: "Project documentation"

docs:
  - id: api/user
    title: "User Module API"
    path: "/API文档/User模块"
    type: doc
    auto_sync: true
    source_patterns:
      - "UserController.java"
      - "UserService*.java"
```

**If no config file exists:** Prompt user to create one. Provide a template.

## Tencent Docs MCP Tools Reference

| Operation | Tool |
|-----------|------|
| List spaces | `tencent-docs_query_space_list` |
| Create space | `tencent-docs_create_space` |
| Create folder node | `tencent-docs_create_space_node` (node_type=wiki_folder) |
| Create doc file | `tencent-docs_manage_create_file` (file_type=doc) |
| Create smartcanvas | `tencent-docs_create_smartcanvas_by_mdx` |
| Query file info | `tencent-docs_manage_query_file_info` |
| Search files | `tencent-docs_manage_search_file` |
| DOC: get outline | `tencent-docs_doc_get_outline` |
| DOC: find text | `tencent-docs_doc_find` |
| DOC: insert markdown | `tencent-docs_doc_insert_markdown` |
| DOC: replace text | `tencent-docs_doc_replace_text` |
| DOC: get last pos | `tencent-docs_doc_get_last_operable_pos` |
| SmartCanvas: find | `tencent-docs_smartcanvas_find` |
| SmartCanvas: edit | `tencent-docs_smartcanvas_edit` |
```

- [ ] **Step 2: Verify file structure**

```bash
ls -la ~/.agents/skills/tencent-docs-publish/
```

Expected: `SKILL.md` exists, non-empty.

- [ ] **Step 3: Commit**

```bash
cd /Users/ifish/development/ai_resource_repo_backend
git add docs/superpowers/plans/2026-07-02-tencent-docs-publish.md
git commit -m "docs: add implementation plan for tencent-docs-publish skill"
```

---

### Task 2: Write Phase 0 — Initialization

**Files:**
- Modify: `~/.agents/skills/tencent-docs-publish/SKILL.md` (append after execution flow)

**Interfaces:**
- Consumes: config file path from project root
- Produces: validated config + space/document structure ready for updates

- [ ] **Step 1: Append Phase 0 to SKILL.md**

Append to SKILL.md:

```markdown
## Phase 0: Initialization

### 0.1 Load Config

Read `.tencent-docs-sync.yml` from project root. If missing:

> "No `.tencent-docs-sync.yml` found. I'll help you create one. What should the Tencent Docs space name be?"

Guide user through creating the config file. Write it to project root.

**Config validation rules:**
- `space.name` is required, non-empty
- Each `docs[]` entry requires: `id`, `title`, `path`, `type` (doc|smartcanvas)
- `auto_sync: true` entries require `source_patterns` (non-empty array)
- `path` must start with `/`
- No duplicate `id` values

### 0.2 Verify/Create Space

Call `tencent-docs_query_space_list` to find existing space by name.

- **Found**: Extract `space_id`, proceed to 0.3
- **Not found**: Call `tencent-docs_create_space(title=space_name, description=config.space.description)`, get `space_id`

### 0.3 Verify/Create Directory Structure

For each `doc.path` in config (e.g., `/API文档/User模块`):

1. Split path into segments: `["API文档", "User模块"]`
2. Call `tencent-docs_query_space_node(space_id=space_id)` to get root children
3. Walk segments, creating missing folders with `tencent-docs_create_space_node(space_id=space_id, node_type=wiki_folder, title=segment, parent_node_id=parent_id)`
4. At leaf, check if document already exists by searching for title match
5. If not exists: create document via `tencent-docs_manage_create_file(file_type=doc)` or `tencent-docs_create_smartcanvas_by_mdx(title=doc.title)` depending on `doc.type`
6. Store the resulting `file_id` for later updates

**Build a runtime map:**

```
RUNTIME_MAP:
  space_id: "abc123"
  docs:
    api/user:
      file_id: "doc_456"
      title: "User Module API"
      path: "/API文档/User模块"
      type: doc
      auto_sync: true
      source_patterns: ["UserController.java", ...]
```

### 0.4 Detect Trigger Type

```
Check git diff:
  git_status = git diff --name-status
  git_staged = git diff --staged --name-status
  git_untracked = git ls-files --others --exclude-standard

If any changes exist:
  trigger = "auto_sync"
Else:
  trigger = "manual"
```

**Output of Phase 0:**
- `RUNTIME_MAP` (space_id + doc file_ids)
- `trigger` ("auto_sync" | "manual")
- If manual: `user_intent` (parsed from user message)
```

- [ ] **Step 2: Verify content**

Read the file and confirm Phase 0 section is complete and coherent.

---

### Task 3: Write Phase 1 — Change Detection

**Files:**
- Modify: `~/.agents/skills/tencent-docs-publish/SKILL.md` (append after Phase 0)

**Interfaces:**
- Consumes: `trigger` from Phase 0
- Produces: `CHANGE_MANIFEST` (for auto_sync) or `target_doc` + `user_intent` (for manual)

- [ ] **Step 1: Append Phase 1 to SKILL.md**

```markdown
## Phase 1: Change Detection

### 1.1 Auto Sync — Git Diff Analysis

Run only when `trigger == "auto_sync"`. Skip entirely for manual triggers.

```bash
# Modified files (unstaged)
git diff --name-status

# Staged files
git diff --staged --name-status

# Untracked files
git ls-files --others --exclude-standard

# Deleted files
git diff --name-status | grep '^D'
git diff --staged --name-status | grep '^D'
```

Build CHANGE_MANIFEST:

```
CHANGE_MANIFEST:
  modified:
    - src/main/java/.../UserController.java  (added: 12, removed: 3)
    - src/main/java/.../UserService.java     (added: 5, removed: 1)
  deleted: []
  untracked: []
```

### 1.2 Manual Trigger — Parse User Intent

Run only when `trigger == "manual"`.

Parse user message to extract:
- **Document target**: Match against `config.docs[].title` or `config.docs[].id`
- **Content intent**: What the user wants to update

**Matching logic:**

```
user_msg = user_message.lower()
matched_docs = []

for doc in config.docs:
    if doc.title.lower() in user_msg or doc.id.lower() in user_msg:
        matched_docs.append(doc)

if len(matched_docs) == 0:
    Show available documents list, ask which one to update
elif len(matched_docs) == 1:
    target_doc = matched_docs[0]
elif len(matched_docs) > 1:
    Show matches, ask which one
```

**Output of Phase 1:**
- For auto_sync: `CHANGE_MANIFEST`
- For manual: `target_doc` + `user_intent`
```

- [ ] **Step 2: Verify content**

---

### Task 4: Write Phase 2 — Mapping

**Files:**
- Modify: `~/.agents/skills/tencent-docs-publish/SKILL.md` (append after Phase 1)

**Interfaces:**
- Consumes: `CHANGE_MANIFEST` or `target_doc`
- Produces: `UPDATE_PLAN` — list of (doc_id, file_id, update_strategy)

- [ ] **Step 1: Append Phase 2 to SKILL.md**

```markdown
## Phase 2: Mapping

### 2.1 Auto Sync — Match Changes to Documents

For each file in CHANGE_MANIFEST, match against `config.docs[].source_patterns`:

```
UPDATE_PLAN = []

for doc in config.docs:
    if not doc.auto_sync:
        continue
    matched_files = []
    for pattern in doc.source_patterns:
        for changed_file in CHANGE_MANIFEST.modified + CHANGE_MANIFEST.deleted:
            if fnmatch(changed_file, pattern):
                matched_files.append(changed_file)
    if matched_files:
        UPDATE_PLAN.append({
            "doc_id": doc.id,
            "file_id": RUNTIME_MAP.docs[doc.id].file_id,
            "title": doc.title,
            "type": doc.type,
            "matched_files": matched_files,
            "strategy": "incremental" if doc.type == "doc" else "full_rebuild"
        })
```

**Update strategy by doc type:**

| Type | Strategy | Reason |
|------|----------|--------|
| doc | incremental | Insert/update specific sections via `insert_markdown` at target position |
| smartcanvas | full_rebuild | Re-generate full content via `create_smartcanvas_by_mdx` |

### 2.2 Manual Trigger — Single Document

```
UPDATE_PLAN = [{
    "doc_id": target_doc.id,
    "file_id": RUNTIME_MAP.docs[target_doc.id].file_id,
    "title": target_doc.title,
    "type": target_doc.type,
    "matched_files": [],
    "strategy": "full_rebuild",
    "user_intent": user_intent
}]
```

**Output of Phase 2:** `UPDATE_PLAN` (list of documents to update)
```

- [ ] **Step 2: Verify content**

---

### Task 5: Write Phase 3 — Content Generation

**Files:**
- Modify: `~/.agents/skills/tencent-docs-publish/SKILL.md` (append after Phase 2)

**Interfaces:**
- Consumes: `UPDATE_PLAN` from Phase 2
- Produces: `CONTENT_MAP` — map of doc_id to rendered content string

- [ ] **Step 1: Append Phase 3 to SKILL.md**

```markdown
## Phase 3: Content Generation

### 3.1 Content Sources

For each entry in UPDATE_PLAN, gather content from:

1. **Source code** (auto_sync only): Read matched files to extract:
   - Controller: `@RequestMapping`, `@GetMapping`/`@PostMapping`/etc, method signatures, `@Operation`/`@Parameter` annotations
   - Service: method signatures, `@Transactional`, return types
   - Entity: field names, types, `@Table`/`@Column` annotations
   - Config: key configuration properties
2. **User input** (manual only): Content from user message or follow-up dialog
3. **Project docs**: README.md, AGENTS.md, API_DOCUMENTATION.md for context

### 3.2 Render DOC Content (Markdown)

For `type == "doc"`, render as Markdown:

```markdown
## User Module API

### Endpoints

#### POST /api/users
- **Description:** Create a new user
- **Auth:** @RequireAuth
- **Request body:** UserCreateRequest { username, email, password }
- **Response:** Result<User>
- **Controller:** UserController.createUser()

#### GET /api/users/{id}
- **Description:** Get user by ID
- **Auth:** @RequireAuth
- **Path params:** id (Long, @Min(1))
- **Response:** Result<User>
- **Controller:** UserController.getUserById()
```

**Extraction approach:** Read the Java source files, parse annotations and method signatures. For each controller method, extract:
- HTTP method + path (from `@RequestMapping`/`@GetMapping` etc.)
- `@Operation(summary=...)` description
- `@Parameter` descriptions
- Method parameter names and types
- Return type

### 3.3 Render SmartCanvas Content (MDX)

For `type == "smartcanvas"`, render as MDX:

**Architecture document:**

```mdx
---
title: System Architecture
---

<Heading level={2}>Tech Stack</Heading>

| Component | Technology |
|-----------|-----------|
| Framework | Spring Boot 3.2.5 |
| Language | Java 17 |
| Database | MySQL |
| ORM | MyBatis 3.0.3 |

<Heading level={2}>Module Architecture</Heading>

<ColumnList>
  <Column width={1/2}>
    <Callout type="info" title="Controller Layer">
      REST endpoints, 15 Controllers
    </Callout>
  </Column>
  <Column width={1/2}>
    <Callout type="info" title="Service Layer">
      Business logic interfaces + implementations
    </Callout>
  </Column>
</ColumnList>
```

**Product roadmap document:**

```mdx
---
title: Product Roadmap
---

<Heading level={2}>Current Version: v2.0.0</Heading>

<ColumnList>
  <Column width={1/3}>
    <Callout type="info" title="Completed">
      - User auth system
      - Agent CRUD
    </Callout>
  </Column>
  <Column width={1/3}>
    <Callout type="warning" title="In Progress">
      - Skill repository module
    </Callout>
  </Column>
  <Column width={1/3}>
    <Callout type="danger" title="Planned">
      - Comment system
      - Notification system
    </Callout>
  </Column>
</ColumnList>
```

**Output of Phase 3:** `CONTENT_MAP` — map of doc_id to rendered content string
```

- [ ] **Step 2: Verify content**

---

### Task 6: Write Phase 4 — Push to Tencent Docs

**Files:**
- Modify: `~/.agents/skills/tencent-docs-publish/SKILL.md` (append after Phase 3)

**Interfaces:**
- Consumes: `CONTENT_MAP` + `UPDATE_PLAN` from previous phases
- Produces: updated documents on Tencent Docs

- [ ] **Step 1: Append Phase 4 to SKILL.md**

```markdown
## Phase 4: Push to Tencent Docs

### 4.1 Update DOC Documents

For each entry where `type == "doc"`:

**Incremental update strategy:**

1. Call `tencent-docs_doc_get_outline(file_id=file_id)` to get document structure
2. Call `tencent-docs_doc_get_last_operable_pos(file_id=file_id)` to find end position
3. Call `tencent-docs_doc_insert_markdown(file_id=file_id, idx=end_pos, markdown=content)` to append new content
4. If replacing existing section: use `tencent-docs_doc_find` to locate old text, then `tencent-docs_doc_replace_text` to replace

**For first-time content (empty doc):**
1. Call `tencent-docs_doc_insert_markdown(file_id=file_id, idx=0, markdown=content)`

### 4.2 Update SmartCanvas Documents

For each entry where `type == "smartcanvas"`:

**Full rebuild strategy:**

1. Call `tencent-docs_smartcanvas_find(file_id=file_id, query="")` to get current blocks
2. For each existing block, call `tencent-docs_smartcanvas_edit(file_id=file_id, action=DELETE, id=block_id)`
3. Call `tencent-docs_smartcanvas_edit(file_id=file_id, action=INSERT_AFTER, id="", content=mdx_content)`

**Alternative (simpler):** If the document was created via `create_smartcanvas_by_mdx`, just call it again with updated content — it creates a new version.

### 4.3 Error Handling

| Error | Action |
|-------|--------|
| Document not found | Re-create via Phase 0.3 |
| API rate limit | Wait and retry (exponential backoff) |
| Network failure | Report to user, suggest retry |
| Permission denied | Report to user, check Tencent Docs access |
```

- [ ] **Step 2: Verify content**

---

### Task 7: Write Phase 5 — Report + Edge Cases + Appendix

**Files:**
- Modify: `~/.agents/skills/tencent-docs-publish/SKILL.md` (append after Phase 4)

**Interfaces:**
- Consumes: results from Phase 4
- Produces: final summary to user

- [ ] **Step 1: Append Phase 5 + Edge Cases + Appendix to SKILL.md**

```markdown
## Phase 5: Report

### 5.1 Final Summary

Present a concise summary:

```
TENCENT DOCS PUBLISH COMPLETE
=============================
Trigger: auto_sync (git diff detected)

Documents updated:
  - User Module API       Updated (2 new endpoints)
  - System Architecture   Updated (config changes)
  - Current Sprint        Skipped (no changes)

Documents skipped: 3 (no matching changes)

Space: Logicoma-Net
```

### 5.2 Present Document Links

For each updated document, present its Tencent Docs URL:

```
Updated documents:
  - User Module API: https://docs.qq.com/doc/xxx
  - System Architecture: https://docs.qq.com/smartcanvas/xxx
```

## Edge Cases

| Scenario | Behavior |
|----------|----------|
| No config file | Prompt user to create one with guided questions |
| Space not found | Create space automatically |
| Document already exists | Update in place, do not duplicate |
| Source file deleted | Mark corresponding doc section as deprecated |
| No code changes (manual trigger) | Skip git diff, proceed to content generation |
| Multiple docs match same source file | Update all matching docs |
| Network failure | Retry with exponential backoff, report failure |
| API rate limit hit | Wait 5 seconds, retry up to 3 times |
| New module with no mapping | Prompt user to add to config |

## Appendix: Config File Template

```yaml
space:
  name: "YourProjectName"
  description: "Project documentation on Tencent Docs"

docs:
  - id: api/example
    title: "Example Module API"
    path: "/API文档/Example模块"
    type: doc
    auto_sync: true
    source_patterns:
      - "ExampleController.java"
      - "ExampleService*.java"
```

## Self-Check Before Completion

- [ ] Frontmatter has correct name and description
- [ ] All 6 phases are present (0-5)
- [ ] Execution flow diagram matches phase logic
- [ ] Config template is complete and valid
- [ ] MCP tools reference table covers all needed tools
- [ ] Edge cases table covers failure scenarios
- [ ] Report format is clear and actionable
```

- [ ] **Step 2: Verify content**

---

### Task 8: Self-Review and Final Verification

- [ ] **Step 1: Self-review the plan**

Check against spec:

| Spec Requirement | Covered In |
|-----------------|-----------|
| Auto-sync from git diff | Task 3 (Phase 1) + Task 4 (Phase 2) |
| Manual trigger | Task 3 (Phase 1.2) |
| Config-driven setup | Task 2 (Phase 0) |
| Space + directory init | Task 2 (Phase 0.2-0.3) |
| DOC content generation | Task 5 (Phase 3.2) |
| SmartCanvas content generation | Task 5 (Phase 3.3) |
| Push to Tencent Docs | Task 6 (Phase 4) |
| Report output | Task 7 (Phase 5) |
| Edge cases | Task 7 |

**Placeholder scan:** No TBD, TODO, or vague requirements found.

**Type consistency:** All references to `RUNTIME_MAP`, `UPDATE_PLAN`, `CONTENT_MAP`, `CHANGE_MANIFEST` are consistent across tasks.

- [ ] **Step 2: Verify plan file exists and is valid**

```bash
wc -l docs/superpowers/plans/2026-07-02-tencent-docs-publish.md
head -5 docs/superpowers/plans/2026-07-02-tencent-docs-publish.md
```

Expected: File has content, starts with correct header.

- [ ] **Step 3: Commit**

```bash
cd /Users/ifish/development/ai_resource_repo_backend
git add docs/superpowers/plans/2026-07-02-tencent-docs-publish.md
git commit -m "docs: add implementation plan for tencent-docs-publish skill"
```
