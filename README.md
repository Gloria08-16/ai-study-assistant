# AI智能伴学与私人知识库系统

基于 Spring Boot 3 + Vue 3 的前后端分离全栈项目。

## 项目结构

```
├── schema.sql          # 数据库初始化脚本
├── backend/            # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com/study/ai/
│       ├── AiStudyApplication.java
│       ├── config/CorsConfig.java
│       ├── controller/
│       │   ├── AiChatController.java
│       │   ├── KnowledgeController.java
│       │   ├── SessionController.java
│       │   └── MessageController.java
│       ├── entity/
│       │   ├── ChatSession.java
│       │   ├── ChatMessage.java
│       │   └── KnowledgeBase.java
│       ├── mapper/
│       │   ├── ChatSessionMapper.java
│       │   ├── ChatMessageMapper.java
│       │   └── KnowledgeBaseMapper.java
│       └── service/AiChatService.java
└── frontend/           # Vue 3 前端
    ├── src/
    │   ├── main.js
    │   ├── App.vue
    │   ├── router/index.js
    │   └── views/
    │       ├── ChatView.vue
    │       └── KnowledgeView.vue
    └── package.json
```

## 一、数据库初始化

1. 确保本地已安装 MySQL（推荐 8.0+），且服务已启动。
2. 使用 root 用户登录 MySQL，执行项目根目录下的 `schema.sql`：

```bash
mysql -u root -p < schema.sql
```

或者登录 MySQL 后在命令行中执行：

```sql
source /path/to/schema.sql;
```

脚本会自动创建数据库 `ai_study_db`（字符集 utf8mb4）及三张表：
- `chat_session`   — 聊天会话
- `chat_message`   — 聊天消息
- `knowledge_base`  — 知识库

## 二、后端启动步骤

**前提**：已安装 JDK 17+ 和 Maven 3.8+。

### 1. 配置数据库连接

编辑 `backend/src/main/resources/application.yml`，将数据库密码改为你自己的 MySQL root 密码：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_study_db?...
    username: root
    password: 你的密码   # ← 改这里
```

### 2. 配置 DeepSeek API Key（必须）

在同一个文件中，填入你的 DeepSeek API Key：

```yaml
ai:
  api-key: 你的API-Key    # ← 改这里
  api-url: https://api.deepseek.com/chat/completions
```

> API Key 可在 [platform.deepseek.com](https://platform.deepseek.com) 申请，新用户有免费额度。

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

启动后，后端服务运行在 **http://localhost:8080**，提供的接口：

| 接口                         | 方法   | 说明             |
|-----------------------------|--------|------------------|
| `/api/chat`                 | POST   | AI 对话          |
| `/api/sessions`             | GET    | 查询会话列表     |
| `/api/sessions`             | POST   | 创建新会话       |
| `/api/sessions/{id}`        | PUT    | 重命名会话       |
| `/api/sessions/{id}`        | DELETE | 删除会话（联级删除消息） |
| `/api/messages/{sessionId}` | GET    | 查询会话历史消息 |
| `/api/messages`             | POST   | 保存一条消息     |
| `/api/knowledge/list`       | GET    | 查询知识列表     |
| `/api/knowledge/add`        | POST   | 新增知识资料     |
| `/api/knowledge/delete/{id}`| DELETE | 删除知识资料     |

## 三、前端启动步骤

**前提**：已安装 Node.js 18+。

```bash
# 1. 进入前端目录
cd frontend

# 2. 安装依赖（首次）
npm install

# 3. 启动开发服务器
npm run dev
```

启动后，浏览器访问控制台输出的地址（默认 **http://localhost:5173**）。

### 页面说明

- **AI伴学** (`/chat`)：左侧历史会话列表 + 右侧聊天区域。双击会话标题可重命名，悬停可删除。Enter 发送消息，Shift+Enter 换行。
- **知识库** (`/knowledge`)：表格展示资料列表，支持新增、删除，分页浏览。

> 前端通过 `http://localhost:8080` 访问后端 API。如果后端地址不同，请修改 `ChatView.vue` 和 `KnowledgeView.vue` 中的 `API_BASE` 常量。

## 四、技术栈

| 层级   | 技术                                         |
|--------|----------------------------------------------|
| 后端   | Spring Boot 3.2.x, MyBatis-Plus 3.5.7, MySQL |
| 前端   | Vue 3, Vue Router, Axios, Element Plus       |
| 数据库 | MySQL 8.0+, utf8mb4                           |
