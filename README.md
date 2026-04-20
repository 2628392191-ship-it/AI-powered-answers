# AI 代码小助手

一个基于 Spring AI + Vue 3 构建的全栈 AI 对话应用，专注于为程序员提供代码优化和 Bug 排查服务。

---

## 项目结构

```
.
├── ai-server/          # 后端 Spring Boot 服务
└── ai-frontend/        # 前端 Vue 3 应用
```

---

## 技术栈

### 后端 `ai-server`

| 技术 | 说明 |
|------|------|
| Spring Boot 3.5 | 基础框架 |
| Spring AI 1.0.0 | AI 能力集成框架 |
| Spring AI Alibaba (DashScope) | 阿里云通义千问模型接入 |
| Ollama | 本地模型支持 |
| Spring AI MCP Client | Model Context Protocol 客户端 |
| Kryo 5.6 | 对话记忆文件序列化 |
| iText PDF 9.1 | PDF 文件生成 |
| Hutool | 工具库（HTTP、文件、JSON） |
| Jsoup | HTML 网页解析 |
| Java 21 | 运行环境 |

### 前端 `ai-frontend`

| 技术 | 说明 |
|------|------|
| Vue 3 | 前端框架 |
| Vite 5 | 构建工具 |
| marked | Markdown 渲染 |
| highlight.js | 代码高亮 |
| Axios | HTTP 请求 |

---

## 核心功能

### 1. 流式对话（SSE）

通过 Server-Sent Events 实现 AI 回复的实时流式输出，前端逐 chunk 渲染，体验流畅。

- 接口：`GET /ai/answer/sse?UserMessage=&chatId=`
- 支持多轮对话，通过 `chatId` 区分会话

### 2. 对话记忆持久化

自定义 `FileBasedChatMemory`，使用 Kryo 将每个会话的消息序列化为 `.kryo` 文件存储在 `tmp/chat-memory/` 目录，服务重启后历史记忆不丢失。

### 3. RAG 知识库检索

内置三份恋爱问答 Markdown 文档（单身篇、恋爱篇、已婚篇），启动时经过以下流程构建向量索引：

```
加载 Markdown → Token 切割 → KeywordMetadataEnricher 关键词补充 → Embedding 向量化 → SimpleVectorStore
```

> ⚠️ 注意：`KeywordMetadataEnricher` 会对每个文档 chunk 调用 LLM，`SimpleVectorStore` 为纯内存存储，**每次重启都会重建索引**，导致启动较慢。建议将向量数据持久化到文件或替换为持久化向量数据库。

### 4. Function Calling 工具集

AI 可自主调用以下工具完成复杂任务：

| 工具 | 功能 |
|------|------|
| `FileOperationTool` | 读写本地文件 |
| `WebSearchTool` | 调用 SearchAPI 搜索百度 |
| `WebScrapingTool` | 抓取网页内容（Jsoup） |
| `ResourceDownloadTool` | 下载网络资源到本地 |
| `TerminalOperationTool` | 执行终端命令（cmd） |
| `PDFGenerationTool` | 生成 PDF 文件（支持中文） |
| `LovingAdviceTool` | 基于 RAG 知识库的恋爱建议 |
| `TerminateTool` | 终止当前 Agent 任务 |

### 5. 前端聊天界面

- 侧边栏历史对话管理（新建、切换、删除）
- 对话记录持久化到 `localStorage`
- Markdown + 代码高亮渲染，代码块一键复制
- 输入框自适应高度，`Enter` 发送，`Shift+Enter` 换行
- 流式打字机效果

---

## 快速启动

### 后端

1. 配置 `ai-server/src/main/resources/application.yaml`，填入你的 API Key：

```yaml
spring:
  ai:
    dashscope:
      api-key: your-dashscope-api-key

search-api:
  api-key: your-searchapi-key
```

2. 启动服务（默认端口 `8080`）：

```bash
cd ai-server
mvn spring-boot:run
```

### 前端

```bash
cd ai-frontend
npm install
npm run dev
```

前端默认通过 Vite 代理将 `/ai` 请求转发到后端 `http://localhost:8080`。

---

## 项目配置

### 系统提示词

`ai-server/src/main/resources/SystemPrompt.txt` — 定义 AI 角色为"代码小助手"，限定服务范围为编程、架构、数据库、运维，拒绝非技术类问题。

### 对话记忆存储路径

```
tmp/chat-memory/{chatId}.kryo
```

### 工具输出文件路径

```
tmp/file/     # 文件读写
tmp/pdf/      # PDF 生成
```

---

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/ai/answer/sse` | 流式对话（SSE），参数：`UserMessage`、`chatId` |
| DELETE | `/ai/answer/delmemory/{chatId}` | 清除指定会话的对话记忆 |
