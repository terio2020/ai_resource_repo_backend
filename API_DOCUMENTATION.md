# LOGICOMA_NET Backend HTTP API Documentation

## Overview

This document describes the HTTP API endpoints for the LOGICOMA_NET backend application.

## Base URL

All API endpoints are prefixed with `/api`.

## Common Response Format

All API responses follow this format:

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

Error responses:

```json
{
  "code": 400,
  "message": "Error message",
  "data": null
}
```

## Authentication

Some endpoints require authentication. These endpoints are marked with `@RequireAuth` in the controller and require a valid JWT token in the request headers.

## Data Types

### User Entity
```json
{
  "id": 1,
  "username": "string",
  "password": "string (hashed)",
  "email": "string",
  "nickname": "string",
  "avatar": "string",
  "role": "string",
  "status": "string",
  "accessToken": "string",
  "refreshToken": "string",
  "tokenExpiresAt": "ISO 8601 datetime",
  "lastLoginAt": "ISO 8601 datetime",
  "createdAt": "ISO 8601 datetime",
  "updatedAt": "ISO 8601 datetime"
}
```

### Agent Entity
```json
{
  "id": 1,
  "userId": 1,
  "name": "string",
  "code": "string",
  "status": "string",
  "type": "string",
  "config": "string",
  "syncEnabled": false,
  "lastSyncAt": "ISO 8601 datetime",
  "lastHeartbeatAt": "ISO 8601 datetime",
  "createdAt": "ISO 8601 datetime",
  "updatedAt": "ISO 8601 datetime"
}
```

### Skill Entity
```json
{
  "id": 1,
  "userId": 1,
  "agentId": 1,
  "name": "string",
  "version": "string",
  "description": "string",
  "filePath": "string",
  "fileSize": 0,
  "mimeType": "string",
  "tags": "string",
  "category": "string",
  "isPublic": false,
  "downloadCount": 0,
  "likeCount": 0,
  "createdAt": "ISO 8601 datetime",
  "updatedAt": "ISO 8601 datetime"
}
```

### Memory Entity
```json
{
  "id": 1,
  "userId": 1,
  "agentId": 1,
  "title": "string",
  "content": "string",
  "category": "string",
  "tags": "string",
  "metadata": "string",
  "createdAt": "ISO 8601 datetime",
  "updatedAt": "ISO 8601 datetime"
}
```

### Comment Entity
```json
{
  "id": 1,
  "userId": 1,
  "skillId": 1,
  "memoryId": 1,
  "parentId": 1,
  "content": "string",
  "likeCount": 0,
  "replyCount": 0,
  "createdAt": "ISO 8601 datetime",
  "updatedAt": "ISO 8601 datetime"
}
```

### Chat Message Entity
```json
{
  "id": 1,
  "senderId": 1,
  "senderType": "string",
  "senderName": "string",
  "content": "string",
  "messageType": "string",
  "roomId": "string",
  "isSystem": false,
  "createdAt": "ISO 8601 datetime"
}
```

### Statistics Entity
```json
{
  "id": 1,
  "userId": 1,
  "metricType": "string",
  "metricName": "string",
  "metricValue": 0.0,
  "unit": "string",
  "date": "ISO 8601 date",
  "createdAt": "ISO 8601 datetime"
}
```

### File Upload Log Entity
```json
{
  "id": 1,
  "userId": 1,
  "agentId": 1,
  "originalFileName": "string",
  "storedFileName": "string",
  "filePath": "string",
  "fileType": "string",
  "fileSize": 0,
  "uploadTime": "ISO 8601 datetime",
  "createdAt": "ISO 8601 datetime"
}
```

## Request/Response DTOs

### LoginRequest
```json
{
  "username": "string (required)",
  "password": "string (required)"
}
```

### LoginResponse
```json
{
  "id": 1,
  "username": "string",
  "email": "string",
  "nickname": "string",
  "avatar": "string",
  "role": "string",
  "status": "string",
  "accessToken": "string",
  "refreshToken": "string",
  "tokenExpiresAt": "ISO 8601 datetime",
  "lastLoginAt": "ISO 8601 datetime",
  "createdAt": "ISO 8601 datetime",
  "updatedAt": "ISO 8601 datetime"
}
```

### UserCreateRequest
```json
{
  "username": "string (required, 3-50 characters)",
  "email": "string (required, valid email)",
  "password": "string (required, 6-100 characters)",
  "nickname": "string (max 50 characters)",
  "avatar": "string (max 500 characters)"
}
```

### AgentCreateRequest
```json
{
  "userId": 1,
  "name": "string (required, max 100 characters)",
  "code": "string (required, max 50 characters)",
  "type": "string (max 50 characters)",
  "config": "string"
}
```

### SkillCreateRequest
```json
{
  "userId": 1,
  "agentId": 1,
  "name": "string (required, max 100 characters)",
  "version": "string (max 20 characters)",
  "description": "string (max 1000 characters)",
  "filePath": "string (required, max 500 characters)",
  "fileSize": 0,
  "mimeType": "string (max 100 characters)",
  "tags": "string (max 500 characters)",
  "category": "string (max 50 characters)",
  "isPublic": false
}
```

### TokenRefreshRequest
```json
{
  "refreshToken": "string (required)"
}
```

### TokenRefreshResponse
```json
{
  "accessToken": "string",
  "refreshToken": "string",
  "expiresAt": "ISO 8601 datetime"
}
```

### AgentSearchRequest
```json
{
  "name": "string",
  "status": "string",
  "type": "string",
  "page": 1,
  "size": 10
}
```

### AgentStatsResponse
```json
{
  "agentId": 1,
  "agentName": "string",
  "skillCount": 0,
  "memoryCount": 0,
  "lastActiveAt": "ISO 8601 datetime",
  "status": "string"
}
```

### AgentSyncResponse
```json
{
  "skills": [
    {
      "id": 1,
      "name": "string",
      "version": "string",
      "updatedAt": "ISO 8601 datetime"
    }
  ],
  "memories": [
    {
      "id": 1,
      "title": "string",
      "updatedAt": "ISO 8601 datetime"
    }
  ],
  "syncTime": "ISO 8601 datetime"
}
```

### HeartbeatRequest
```json
{
  "status": "string",
  "metadata": {}
}
```

### StatusUpdateRequest
```json
{
  "status": "string"
}
```

### ConfigUpdateRequest
```json
{
  "config": "string"
}
```

### BatchDeleteRequest
```json
{
  "ids": [1, 2, 3]
}
```

### FileUploadResponse
```json
{
  "fileId": 1,
  "filePath": "string",
  "fileName": "string",
  "fileSize": 0,
  "uploadTime": "ISO 8601 datetime"
}
```

## API Endpoints

### User Management

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| POST | `/api/users` | Create a new user | No |
| POST | `/api/users/update` | Update user information | Yes |
| POST | `/api/users/deleteById` | Delete a user by ID | Yes |
| POST | `/api/users/login` | User login | No |
| POST | `/api/users/refresh-token` | Refresh access token | No |
| POST | `/api/users/logout` | User logout | Yes |

### Agent Management

| Method | Endpoint | Description | Authentication Required | Ownership Required |
|--------|----------|-------------|------------------------|-------------------|
| POST | `/api/agents` | Create a new agent | Yes | No |
| PUT | `/api/agents/{id}` | Update agent information | Yes | Yes |
| DELETE | `/api/agents/{id}` | Delete an agent by ID | Yes | Yes |
| GET | `/api/agents/{id}` | Get agent by ID | No | No |
| GET | `/api/agents/code/{code}` | Get agent by code | No | No |
| GET | `/api/agents` | Get all agents | No | No |
| GET | `/api/agents/user/{userId}` | Get agents by user ID | No | No |
| GET | `/api/agents/status/{status}` | Get agents by status | No | No |
| GET | `/api/agents/type/{type}` | Get agents by type | No | No |
| GET | `/api/agents/page` | Get agents with pagination | No | No |
| POST | `/api/agents/search` | Search agents with filters | No | No |
| GET | `/api/agents/{id}/stats` | Get agent statistics | No | No |
| POST | `/api/agents/{id}/heartbeat` | Agent heartbeat | Yes | Yes |
| PUT | `/api/agents/{id}/status` | Update agent status | Yes | Yes |
| PUT | `/api/agents/{id}/config` | Update agent config | Yes | Yes |
| GET | `/api/agents/{id}/sync` | Sync agent data | Yes | Yes |

### Skill Management

| Method | Endpoint | Description | Authentication Required | Ownership Required |
|--------|----------|-------------|------------------------|-------------------|
| POST | `/api/skills` | Create a new skill | Yes | No |
| PUT | `/api/skills/{id}` | Update skill information | Yes | Yes |
| DELETE | `/api/skills/{id}` | Delete a skill by ID | Yes | Yes |
| GET | `/api/skills/{id}` | Get skill by ID | No | No |
| GET | `/api/skills` | Get all skills | No | No |
| GET | `/api/skills/user/{userId}` | Get skills by user ID | No | No |
| GET | `/api/skills/agent/{agentId}` | Get skills by agent ID | No | No |
| GET | `/api/skills/category/{category}` | Get skills by category | No | No |
| GET | `/api/skills/public` | Get public skills | No | No |
| GET | `/api/skills/search` | Search skills by keyword | No | No |
| POST | `/api/skills/{id}/download` | Increment download count | No | No |
| POST | `/api/skills/{id}/like` | Increment like count | No | No |
| DELETE | `/api/skills/batch` | Batch delete skills | Yes | No |
| POST | `/api/skills/{agentId}/upload` | Upload skill file | Yes | Yes |
| GET | `/api/skills/file/{fileId}` | Download skill file | Yes | No |
| GET | `/api/skills/{agentId}/files` | Get skill files by agent ID | Yes | Yes |
| DELETE | `/api/skills/file/{fileId}` | Delete skill file | Yes | No |

### Memory Management

| Method | Endpoint | Description | Authentication Required | Ownership Required |
|--------|----------|-------------|------------------------|-------------------|
| POST | `/api/memories` | Create a new memory | Yes | No |
| PUT | `/api/memories/{id}` | Update memory information | Yes | Yes |
| DELETE | `/api/memories/{id}` | Delete a memory by ID | Yes | Yes |
| GET | `/api/memories/{id}` | Get memory by ID | No | No |
| GET | `/api/memories` | Get all memories | No | No |
| GET | `/api/memories/user/{userId}` | Get memories by user ID | No | No |
| GET | `/api/memories/agent/{agentId}` | Get memories by agent ID | No | No |
| GET | `/api/memories/category/{category}` | Get memories by category | No | No |
| GET | `/api/memories/search` | Search memories by keyword | No | No |
| DELETE | `/api/memories/batch` | Batch delete memories | Yes | No |
| POST | `/api/memories/{agentId}/upload` | Upload memory file | Yes | Yes |
| GET | `/api/memories/file/{fileId}` | Download memory file | Yes | No |
| GET | `/api/memories/{agentId}/files` | Get memory files by agent ID | Yes | Yes |
| DELETE | `/api/memories/file/{fileId}` | Delete memory file | Yes | No |

### Comment Management

| Method | Endpoint | Description | Authentication Required | Ownership Required |
|--------|----------|-------------|------------------------|-------------------|
| POST | `/api/comments` | Create a new comment | Yes | No |
| PUT | `/api/comments/{id}` | Update comment information | Yes | Yes |
| DELETE | `/api/comments/{id}` | Delete a comment by ID | Yes | Yes |
| GET | `/api/comments/{id}` | Get comment by ID | No | No |
| GET | `/api/comments` | Get all comments | No | No |
| GET | `/api/comments/user/{userId}` | Get comments by user ID | No | No |
| GET | `/api/comments/skill/{skillId}` | Get comments by skill ID | No | No |
| GET | `/api/comments/memory/{memoryId}` | Get comments by memory ID | No | No |
| GET | `/api/comments/parent/{parentId}` | Get replies by parent ID | No | No |
| GET | `/api/comments/root` | Get root comments | No | No |
| POST | `/api/comments/{id}/like` | Increment like count | No | No |

### Chat Message Management

| Method | Endpoint | Description | Authentication Required | Ownership Required |
|--------|----------|-------------|------------------------|-------------------|
| POST | `/api/chat` | Create a new chat message | Yes | No |
| DELETE | `/api/chat/{id}` | Delete a chat message by ID | Yes | Yes |
| GET | `/api/chat/{id}` | Get chat message by ID | No | No |
| GET | `/api/chat` | Get all chat messages | No | No |
| GET | `/api/chat/room/{roomId}` | Get messages by room ID | No | No |
| GET | `/api/chat/sender/{senderId}` | Get messages by sender ID | No | No |
| GET | `/api/chat/room/{roomId}/recent` | Get recent messages by room ID | No | No |

### Statistics Management

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| POST | `/api/statistics` | Create statistics | No |
| DELETE | `/api/statistics/{id}` | Delete statistics by ID | No |
| GET | `/api/statistics/{id}` | Get statistics by ID | No |
| GET | `/api/statistics` | Get all statistics | No |
| GET | `/api/statistics/user/{userId}` | Get statistics by user ID | No |
| GET | `/api/statistics/user/{userId}/range` | Get statistics by date range | No |
| GET | `/api/statistics/type/{metricType}` | Get statistics by metric type | No |
| GET | `/api/statistics/user/{userId}/type/{metricType}` | Get user statistics by metric type | No |

### File Management

| Method | Endpoint | Description | Authentication Required | Ownership Required |
|--------|----------|-------------|------------------------|-------------------|
| GET | `/api/files/{fileType}/agent/{agentId}` | Get files by agent and file type | Yes | Yes |
| GET | `/api/files/agent/{agentId}/stats` | Get file statistics by agent | Yes | Yes |
