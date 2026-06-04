# AI 智能伴学与私人知识库系统

基于 **Spring Boot 3 + Vue 3** 的前后端分离全栈项目，接入 DeepSeek 大模型实现 AI 对话与 RAG 知识库增强检索。

## 核心特性

- **SSE 流式对话** — Server-Sent Events 实现打字机效果，实时逐字输出
- **RAG 知识库增强** — MySQL Ngram 全文索引检索私人笔记，自动注入 AI 上下文
- **Markdown 渲染** — markdown-it + highlight.js，代码语法高亮，支持围栏代码块
- **多会话管理** — 历史会话切换、重命名、删除，Pinia 持久化存储
- **知识库标签** — 支持按标签筛选资料，CRUD 管理

---

## 项目结构

```
├── README.md
├── schema.sql                    # 数据库初始化脚本
├── backend/                      # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com/study/ai/
│       ├── AiStudyApplication.java
│       ├── config/CorsConfig.java
│       ├── controller/
│       │   ├── AiChatController.java      # AI 对话接口（含 SSE 流式）
│       │   ├── KnowledgeController.java   # 知识库 CRUD
│       │   ├── SessionController.java     # 会话管理
│       │   └── MessageController.java     # 消息存取
│       ├── entity/                        # 实体类
│       ├── mapper/                        # MyBatis-Plus Mapper
│       └── service/AiChatService.java     # DeepSeek 调用 + RAG 检索
└── frontend/                     # Vue 3 前端
    ├── vite.config.js
    ├── package.json
    └── src/
        ├── main.js                       # 入口：highlight.js 主题
        ├── App.vue
        ├── router/index.js
        ├── stores/chatStore.js           # Pinia 状态持久化
        └── views/
            ├── ChatView.vue              # AI 对话页
            └── KnowledgeView.vue         # 知识库管理页
```

---

## 一、数据库初始化

1. 确保本地已安装 **MySQL 8.0+**，且服务已启动。
2. 执行项目根目录下的 `schema.sql`：

```bash
mysql -u root -p < schema.sql
```

脚本自动创建数据库 `ai_study_db`（utf8mb4）及三张表：

| 表名 | 说明 |
|------|------|
| `chat_session` | 聊天会话 |
| `chat_message` | 聊天消息 |
| `knowledge_base` | 知识库笔记 |

> ⚠️ **全文索引**：RAG 知识库检索依赖 MySQL Ngram 全文索引。若表已存在但未建索引，请手动执行：
> ```sql
> ALTER TABLE knowledge_base ADD FULLTEXT INDEX ft_index_title_content (title, content) WITH PARSER ngram;
> ```

---

## 二、后端启动

**环境要求**：JDK 17+ / Maven 3.8+ / MySQL 8.0+

### 1. 配置 `application.yml`

编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_study_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 你的密码          # ← 改成你的 MySQL 密码

ai:
  api-key: sk-xxxxxxxxxxxxx     # ← 改成你的 DeepSeek API Key
  api-url: https://api.deepseek.com/chat/completions
```

> API Key 在 [platform.deepseek.com](https://platform.deepseek.com) 申请，新用户有免费额度。

### 2. 启动

```bash
cd backend
mvn spring-boot:run
```

后端运行在 **http://localhost:8080**。

---

## 三、前端启动

**环境要求**：Node.js 18+

```bash
cd frontend
npm install      # 首次运行
npm run dev
```

浏览器访问 **http://localhost:5173**。

---

## 四、API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/chat` | POST | 非流式 AI 对话 |
| `/api/chat/stream` | POST | **SSE 流式 AI 对话**（打字机效果） |
| `/api/sessions` | GET | 查询会话列表 |
| `/api/sessions` | POST | 创建新会话 |
| `/api/sessions/{id}` | PUT | 重命名会话 |
| `/api/sessions/{id}` | DELETE | 删除会话（联级删除消息） |
| `/api/messages/{sessionId}` | GET | 查询会话历史消息 |
| `/api/messages` | POST | 保存一条消息 |
| `/api/knowledge/list` | GET | 查询知识列表（支持 `?tag=` 筛选） |
| `/api/knowledge/add` | POST | 新增知识资料 |
| `/api/knowledge/delete/{id}` | DELETE | 删除知识资料 |

### `/api/chat` / `/api/chat/stream` 请求体

```json
{
  "message": "用户提问内容",
  "useKnowledge": true
}
```

`useKnowledge` 设为 `true` 时，系统会从知识库中全文检索最相关的 3 条笔记，作为系统提示注入 DeepSeek 请求。

---

## 五、技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2.5 |
| ORM | MyBatis-Plus 3.5.7 |
| 数据库 | MySQL 8.4（Ngram 全文索引） |
| AI 模型 | DeepSeek Chat API（SSE 流式） |
| 前端框架 | Vue 3（Composition API） |
| 路由 | Vue Router 4 |
| UI 组件 | Element Plus |
| 状态管理 | Pinia + pinia-plugin-persistedstate |
| Markdown | markdown-it + highlight.js（github-dark 主题） |
| 构建工具 | Vite 8 |

---

## 六、页面功能

### AI 伴学 (`/chat`)

- 左侧会话列表，支持**新建 / 切换 / 重命名（双击）/ 删除**
- 右侧聊天区，Enter 发送，Shift+Enter 换行
- **知识库增强开关**：开启后自动检索私人笔记辅助回答
- AI 回复支持 **Markdown 渲染 + 代码语法高亮**
- SSE 流式输出，**逐字打字机效果**
- 会话和消息 **自动持久化**至 MySQL

### 知识库 (`/knowledge`)

- 表格展示笔记列表，支持**按标签筛选**、分页浏览
- 新增笔记时可填写标题、内容、标签
- 标签字段支持自由输入多个标签
