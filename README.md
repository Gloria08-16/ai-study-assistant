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
│       │   └── KnowledgeController.java
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

```bash
# 1. 进入后端目录
cd backend

# 2. Maven 编译打包
mvn clean package -DskipTests

# 3. 启动 Spring Boot 应用
mvn spring-boot:run
```

启动后，后端服务运行在 **http://localhost:8080**，提供的接口：

| 接口                    | 方法   | 说明         |
|------------------------|--------|--------------|
| `/api/chat`            | POST   | AI 对话      |
| `/api/knowledge/add`   | POST   | 新增知识资料 |
| `/api/knowledge/list`  | GET    | 查询知识列表 |

> 当前 AI 对话接口返回模拟数据，联调成功后可替换 `AiChatService.chat()` 方法接入真实大模型 API。

数据库连接配置在 `src/main/resources/application.yml` 中，默认：
- 地址：`localhost:3306`
- 数据库：`ai_study_db`
- 用户名：`root`
- 密码：`123456`

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

- **AI伴学** (`/chat`)：左侧历史会话列表 + 右侧聊天区域，支持发送问题和接收回复。
- **知识库** (`/knowledge`)：表格展示资料列表，支持新增资料。

## 四、技术栈

| 层级   | 技术                                         |
|--------|----------------------------------------------|
| 后端   | Spring Boot 3.2.x, MyBatis-Plus 3.5.7, MySQL |
| 前端   | Vue 3, Vue Router, Axios, Element Plus       |
| 数据库 | MySQL 8.0+, utf8mb4                           |
