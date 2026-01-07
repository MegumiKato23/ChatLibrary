# Chat Library Backend

基于 Spring Boot 3 + Spring AI + WebFlux 构建的 RAG (Retrieval-Augmented Generation) 智能问答后端服务。

## 🏗️ 架构与技术栈

- **核心框架**: Java 17, Spring Boot 3.4.12
- **响应式编程**: Spring WebFlux, Project Reactor
- **AI 框架**: Spring AI 1.1.0
- **LLM 模型**: Ollama (DeepSeek-R1)
- **向量数据库**: Qdrant
- **关系型数据库**: MySQL (R2DBC 响应式驱动)
- **文档解析**: Apache Tika (支持 PDF, DOC, DOCX, TXT 等)
- **文档转换**: Apache POI (DOC 转 HTML)

## ✨ 主要功能

1.  **用户管理**: 响应式 JWT 认证、注册登录、信息管理。
2.  **文档处理**:
    - 支持多格式文件上传与解析。
    - 文本切片 (TokenTextSplitter) 与向量化 (Embedding)。
    - 原始文档预览支持 (DOC 转换为 HTML)。
3.  **智能对话**:
    - 基于 RAG 的上下文检索。
    - 向量相似度搜索 (Vector Search)。
    - SSE (Server-Sent Events) 流式回复。
    - 对话历史持久化。

## 🚀 快速开始

### 前置要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Ollama (运行中)
- Qdrant (运行中)

### 1. 环境准备

#### 启动 Ollama
确保 Ollama 已安装并运行，且已拉取 `deepseek-r1:14b` 模型（或在配置中修改）：
```bash
ollama pull deepseek-r1:14b
ollama serve
```

#### 启动 Qdrant
使用 Docker 启动 Qdrant：
```bash
docker run -p 6333:6333 -p 6334:6334 \
    -v $(pwd)/qdrant_storage:/qdrant/storage:z \
    qdrant/qdrant
```

#### 创建数据库
在 MySQL 中创建数据库 `chat_ai_db`。
```sql
CREATE DATABASE chat_ai_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 配置应用

修改 `src/main/resources/application.yaml`：

```yaml
spring:
  r2dbc:
    url: r2dbc:mysql://localhost:3306/chat_ai_db
    username: your_username
    password: your_password
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: deepseek-r1:14b
    vectorstore:
      qdrant:
        host: localhost
        port: 6334
```

### 3. 运行应用

```bash
./mvnw spring-boot:run
```

应用将在 `http://localhost:8080` 启动。

## 🔌 API 文档

### 👤 用户模块 (`/user`)

| 方法 | 路径 | 描述 | 参数 |
| :--- | :--- | :--- | :--- |
| POST | `/user/login` | 用户登录 | JSON: `{username, password}` |
| POST | `/user/register` | 用户注册 | JSON: `{username, email, password}` |
| POST | `/user/update/{userId}` | 更新信息 | JSON: `{username, email, phone}` |
| POST | `/user/change-password/{userId}` | 修改密码 | JSON: `{oldPassword, newPassword, confirmPassword}` |
| DELETE | `/user/delete/{userId}` | 删除用户 | Path: `userId` |

### 📄 文档模块 (`/document`)

| 方法 | 路径 | 描述 | 参数 |
| :--- | :--- | :--- | :--- |
| POST | `/document/upload/{userId}` | 上传文档 | Multipart: `file`, Path: `userId` |
| GET | `/document/list` | 获取文档列表 | Query: `userId` |
| GET | `/document/preview/{id}` | 预览文件资源 | Path: `id` (返回二进制流) |
| GET | `/document/preview/content/{id}` | 预览解析内容 | Path: `id` (返回 HTML/Text) |
| DELETE | `/document/{id}` | 删除文档 | Path: `id` |

### 💬 对话模块 (`/ai`)

| 方法 | 路径 | 描述 | 参数 |
| :--- | :--- | :--- | :--- |
| POST | `/ai/chat` | 发起对话 (SSE) | JSON: `{chatId, userId, prompt}` |
| POST | `/ai/conversation` | 创建会话 | Query: `userId`, `title` |
| GET | `/ai/conversations` | 获取会话列表 | Query: `userId` |
| GET | `/ai/conversation/history/{historyId}` | 获取历史消息 | Path: `historyId` |
| DELETE | `/ai/conversation/history/{historyId}` | 删除会话 | Path: `historyId` |

## 🧪 测试

运行单元测试：
```bash
./mvnw test
```

## 📄 License

MIT
