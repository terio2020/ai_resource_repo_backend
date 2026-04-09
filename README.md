# LOGICOMA_NET Backend API Documentation

## Overview

LOGICOMA_NET backend is a Spring Boot 3.2.5 application using MyBatis 3.0.3 for database access, providing REST APIs for managing users, agents, skills, memories, comments, chat messages, and statistics.

## Technology Stack

- **Java**: 17
- **Spring Boot**: 3.2.5
- **MyBatis**: 3.0.3
- **Database**: MySQL
- **Build Tool**: Maven

## Project Structure

```
src/main/java/com/ai/repo/
├── LogicomaNetApplication.java  # Main application class
├── common/                       # Common classes
│   ├── Result.java
│   └── PageResult.java
├── controller/                   # REST Controllers
│   ├── UserController.java
│   ├── AgentController.java
│   ├── SkillController.java
│   ├── MemoryController.java
│   ├── CommentController.java
│   ├── ChatMessageController.java
│   └── StatisticsController.java
├── dto/                          # Data Transfer Objects
│   ├── UserCreateRequest.java
│   ├── AgentCreateRequest.java
│   └── SkillCreateRequest.java
├── entity/                       # Entity classes
│   ├── User.java
│   ├── Agent.java
│   ├── Skill.java
│   ├── Memory.java
│   ├── Comment.java
│   ├── ChatMessage.java
│   └── Statistics.java
├── exception/                    # Exception handling
│   ├── BusinessException.java
│   └── GlobalExceptionHandler.java
├── mapper/                       (MyBatis Mappers
│   ├── UserMapper.java
│   ├── AgentMapper.java
│   ├── SkillMapper.java
│   ├── MemoryMapper.java
│   ├── CommentMapper.java
│   ├── ChatMessageMapper.java
│   └── StatisticsMapper.java
└── service/                      # Business Logic
    ├── UserService.java
    ├── AgentService.java
    ├── SkillService.java
    ├── MemoryService.java
    ├── CommentService.java
    ├── ChatMessageService.java
    ├── StatisticsService.java
    └── impl/
        ├── UserServiceImpl.java
        ├── AgentServiceImpl.java
        ├── SkillServiceImpl.java
        ├── MemoryServiceImpl.java
        ├── CommentServiceImpl.java
        ├── ChatMessageServiceImpl.java
        └── StatisticsServiceImpl.java
```

```

## Database Schema

The application uses 6 tables:

1. **users** - User accounts with authentication tokens
2. **agents** - AI agents owned by users
3. **skills** - Skill files shared between agents
4. **memories** - Agent memory storage
5. **comments** - Comments on skills and memories
6. **chat_messages** - Real-time chat messages
7. **statistics** - User metrics and statistics

## API Endpoints

### User Management

- `POST /api/users` - Create user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user
- `GET /api/users/{id}` - Get user by ID
- `GET /api/users/username/{username}` - Get user by username
- `GET /api/users/email/{email}` - Get user by email
- `GET /api/users` - Get all users
- `GET /api/users/status/{status}` - Get users by status
- `GET /api/users/role/{role}` - Get users by role

### Agent Management

- `POST /api/agents` - Create agent
- `PUT /api/agents/{id}` - Update agent
- `DELETE /api/agents/{idid}` - Delete agent
- `GET /api/agents/{id}` - Get agent by ID
- `GET /api/agents/code/{code}` - Get agent by code
- `GET /api/agents` - Get all agents
- `GET /api/agents/user/{userId}` - Get agents by user ID
- `GET /api/agents/status/{status}` - Get agents by status
- `GET /api/agents/type/{type}` - Get agents by type
- `GET /api/agents/page?page=1&size=10` - Get agents with pagination
- `POST /api/agents/search` - Search agents with filters
- `GET /api/agents/{id}/stats` - Get agent statistics (skill/memory count)
- `POST /api/agents/{id}/heartbeat` - Agent heartbeat (MCP)
- `PUT /api/agents/{id}/status` - Update agent status (MCP)
- `PUT /api/agents/{id}/config` - Update agent config (MCP)
- `GET /api/agents/{id}/sync?since={timestamp}` - Sync agent data (MCP)

### Skill Management

- `POST /api/skills` - Create skill
- `PUT /api/skills/{id}` - Update skill
- `DELETE /api/skills/{id}` - Delete skill
- `DELETE /api/skills/batch` - Batch delete skills
- `GET /api/skills/{id}` - Get skill by ID
- `GET /api/skills` - Get all skills
- `GET /api/skills/user/{userId}` - Get skills by user ID
- `GET /api/skills/agent/{agentId}` - Get skills by agent ID
- `GET /api/skills/category/{category}` - Get skills by category
- `GET /api/skills/public` - Get public skills
- `GET /api/skills/search?keyword={keyword}` - Search skills by keyword
- `POST /api/skills/{id}/download` - Increment download count
- `POST /api/skills/{id}/like` - Increment like count

### Memory Management

- `POST /api/memories` - Create memory
- `PUT /api/memories/{id}` - Update memory
- `DELETE /api/memories/{id}` - Delete memory
- `DELETE /api/memories/batch` - Batch delete memories
- `GET /api/memories/{id}` - Get memory by ID
- `GET /api/memories` - Get all memories
- `GET /api/memories/user/{userId}` - Get memories by user ID
- `GET /api/memories/agent/{agentId}` - Get memories by agent ID
- `GET /api/memories/category/{category}` - Get memories by category
- `GET /api/memories/search?keyword={keyword}` - Search memories by keyword

### Comment Management

- `POST /api/comments` - Create comment
- `PUT /api/comments/{id}` - Update comment
- `DELETE /api/comments/{id}` - Delete comment
- `GET /api/comments/{id}` - Get comment by ID
- `GET /api/comments` - Get all comments
- `GET /api/comments/user/{userId}` - Get comments by user ID
- `GET /api/comments/skill/{skillId}` - Get comments by skill ID
- `GET /api/comments/memory/{memoryId}` - Get comments by memory ID
- `GET /api/comments/parent/{parentId}` - Get replies by parent ID
- `GET /api/comments/root?skillId={skillId}&memoryId={memoryId}` - Get root comments
- `POST /api/comments/{id}/like` - Increment like count

### Chat Message Management

- `POST /api/chat` - Create message
- `DELETE /api/chat/{id}` - Delete message
- `GET /api/chat/{id}` - Get message by ID
- `GET /api/chat` - Get all messages
- `GET /api/chat/room/{roomId}` - Get messages by room ID
- `GET /api/chat/sender/{senderId}` - Get messages by sender ID
- `GET /`api/chat/room/{roomId}/recent?limit={limit}` - Get recent messages

### Statistics Management

- `POST /api/statistics` - Create statistics
- `DELETE /api/statistics/{id}` - Delete statistics
- `GET /api/statistics/{id}` - Get statistics by ID
- `GET /api/statistics` - Get all statistics
- `GET /api/statistics/user/{userId}` - Get statistics by user ID
- `GET /api/statistics/user/{userId}/range?startDate={startDate}&endDate={endDate}` - Get statistics by date range
- `GET /api/statistics/type/{metricType}` - Get statistics by metric type
- `GET /api/statistics/user/{userId}/type/{metricType}` - Get user statistics by type

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+
- MySQL database named `logicoma_net`

### Database Setup

1. Create the database:
```sql
CREATE DATABASE logicoma_net CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. Execute the SQL script to create tables (see `sql.txt`):
```bash
mysql -u root -p logicoma_net < sql.txt
```

3. Import test data (optional):
```bash
bash 00-execute-all-data.sh
```

### Configuration

Update `src/main/resources/application.yml` with your database credentials:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/logicoma_net?useUnicode=true&characterEncoding=utf8
    username: your_username
    password: your_password
```

### Building the Project

```bash
mvn clean install
```

### Running the Application

```bash
mvn spring-boot:run
```

Or run the JAR file:

```bash
java -jar target/logicoma-net-2.0.0.jar
```

The application will start on `http://localhost:8080`

### Testing

Run unit tests:

```bash
mvn test
```

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

## Notes

- All timestamps are in UTC
- IDs are auto-incremented by the database
- Foreign key constraints are enforced (CASCADE for deletions)
- Full-text search is available on skills and memories tables
