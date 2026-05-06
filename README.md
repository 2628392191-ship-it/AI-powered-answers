# AI 代码小助手

一个基于 Spring Boot + Spring AI + Vue 3 的全栈 AI 对话项目，面向“代码优化、Bug 排查、技术问答”场景。

项目提供了流式对话、会话记忆、RAG 检索、工具注册、前端聊天界面等完整链路，适合作为 Spring AI Agent / RAG / MCP 集成的学习与实践项目。

---

## 项目概览

### 主要能力

- 基于 SSE 的流式对话输出
- 基于 Redis 的多轮会话记忆
- 基于 Spring AI 的 RAG 检索增强
- 可扩展的 Tool Calling 工具集
- 预留 MCP 集成能力
- Vue 3 聊天界面，支持历史对话、本地持久化、Markdown 渲染与代码高亮

### 当前定位

系统提示词将助手角色限定为“代码小助手”，主要回答以下技术问题：

- 编程语言
- 系统架构
- 数据库
- 运维部署

非技术类问题会被拒答。

---

## 项目结构

```text
.
├── ai-server/              # Spring Boot + Spring AI 后端
├── ai-frontend/            # Vue 3 + Vite 前端
└── README.md
```

### 后端模块概览

```text
ai-server/src/main/java/com/server/
├── advisor/                # ChatClient 日志增强
├── chatmemory/             # Redis / 文件会话记忆实现
├── config/                 # 向量库、工具、跨域、MCP 等配置
├── constant/               # 路径与常量定义
├── controller/             # REST 接口
├── mcp/                    # 自定义 MCP / JSON-RPC 调用封装
├── rag/                    # 文档加载、切分、关键词增强
├── service/                # 核心对话服务
└── tools/                  # Tool Calling 工具集
```

---

## 技术栈

### 后端 `ai-server`

| 技术 | 说明 |
|------|------|
| Java 21 | 运行环境 |
| Spring Boot 3.5.13 | 后端基础框架 |
| Spring AI | AI 对话、RAG、Advisor、Tool 能力 |
| Spring AI Alibaba DashScope | 接入通义千问模型 |
| Spring AI MCP Client | MCP 客户端能力支持 |
| Redis | 会话记忆持久化 |
| SimpleVectorStore | 内存向量存储 |
| Kryo | 文件型会话记忆序列化 |
| Jsoup | 网页抓取解析 |
| Hutool | HTTP / 文件 / JSON 工具 |
| iText PDF | PDF 生成 |

### 前端 `ai-frontend`

| 技术 | 说明 |
|------|------|
| Vue 3 | 前端框架 |
| Vite 5 | 构建与开发服务器 |
| marked | Markdown 渲染 |
| highlight.js | 代码高亮 |
| Axios | HTTP 请求 |

---

## 核心功能

## 1. 流式对话

后端通过 SSE 返回模型输出，前端按 chunk 实时渲染消息内容。

- 接口：`GET /ai/answer/sse`
- 参数：`UserMessage`、`chatId`
- 适合长文本回复、代码解释、排障问答等场景

## 2. 会话记忆

当前默认使用 **RedisChatMemory** 持久化多轮对话，按 `chat:{conversationId}` 存储消息列表。

### 当前启用方案

- 存储介质：Redis
- 优点：支持服务重启后保留上下文、实现简单、读写快
- 用途：支撑同一 `chatId` 下的连续对话

### 备用方案

项目中同时保留了 `FileBasedChatMemory`：

- 使用 Kryo 将消息序列化为文件
- 存储位置：`tmp/chat-memory/`
- 当前主流程未启用，但保留了文件记忆实现

## 3. RAG 知识库检索

后端启动时会自动加载 `ai-server/src/main/resources/document/` 目录下的 Markdown 文档，执行以下流程：

```text
Markdown 加载
→ 文本切分
→ 关键词元信息增强
→ 向量化
→ 写入 SimpleVectorStore
```

当前实现特点：

- 使用 `SimpleVectorStore`，数据保存在内存中
- 服务每次重启都会重新构建向量索引
- 文档切分后会做关键词增强，启动阶段会额外调用模型

> 注意：当前知识库内容是示例性质的 Markdown 文档，并非“代码助手专属知识库”。如果要用于真实编程问答，建议替换为项目文档、接口文档、技术规范或 FAQ 资料。

## 4. Tool Calling 工具集

项目中已经注册了多种工具，便于扩展 Agent 能力：

| 工具类 | 功能 |
|------|------|
| `FileOperationTool` | 读写本地文件 |
| `WebSearchTool` | Web 搜索 |
| `WebScrapingTool` | 抓取网页内容 |
| `ResourceDownloadTool` | 下载网络资源 |
| `TerminalOperationTool` | 执行终端命令 |
| `PDFGenerationTool` | 生成 PDF |
| `LovingAdviceTool` | 基于向量检索返回建议 |
| `TerminateTool` | 终止任务 |
| `ZhiPuMcp` | 通过智谱接口执行搜索 |

> 说明：这些工具已经在配置层注册，但当前默认对话主链路主要使用“系统提示词 + 会话记忆 + RAG Advisor”，工具调用能力仍可继续接入和增强。

## 5. MCP 集成预留

项目中存在两种 MCP 相关实现：

- 基于 Spring AI MCP Client 的标准接入方式
- 针对特定服务进行手动 JSON-RPC / SSE 调用的自定义封装

这部分说明项目已经具备继续扩展外部工具生态的基础，但是否启用取决于具体配置与接入方式。

## 6. 前端聊天界面

前端提供完整聊天交互体验：

- 新建对话 / 切换历史对话
- 本地 `localStorage` 持久化聊天记录
- Markdown 渲染
- 代码块高亮与复制
- 输入框自适应高度
- `Enter` 发送、`Shift + Enter` 换行
- 清除当前会话记忆

---

## 系统架构

```text
Vue 3 Frontend
   ↓ HTTP / SSE
Spring Boot Controller
   ↓
AnswerService
   ├── ChatClient
   ├── RedisChatMemory
   ├── QuestionAnswerAdvisor
   └── MyLoggerAdvisor
              ↓
        DashScope / Spring AI
```

---

## 快速启动

## 1. 环境要求

### 后端

- JDK 21
- Maven 3.9+
- Redis
- 可用的大模型 API Key

### 前端

- Node.js 18+
- npm / pnpm / yarn 均可（项目当前使用 npm 最直接）

## 2. 配置后端

编辑文件：`ai-server/src/main/resources/application.yaml`

至少需要准备以下配置：

```yaml
server:
  port: 8080

spring:
  ai:
    dashscope:
      api-key: your-dashscope-api-key
      chat:
        options:
          model: qwen-max
  data:
    redis:
      host: your-redis-host
      port: 6379
      database: 0

search-api:
  api-key: your-search-api-key

zhipu:
  api-key: your-zhipu-api-key
```

建议：

- 不要把真实密钥提交到仓库
- 开发环境可先直接写入配置文件
- 更推荐通过环境变量或外部配置注入敏感信息

## 3. 启动后端

```bash
cd ai-server
mvn spring-boot:run
```

默认端口：`8080`

## 4. 启动前端

```bash
cd ai-frontend
npm install
npm run dev
```

默认端口：`3000`

前端已通过 Vite 代理将 `/ai` 请求转发到：

```text
http://localhost:8080
```

---

## 接口说明

### 1. 流式对话

```http
GET /ai/answer/sse?UserMessage=xxx&chatId=xxx
```

参数说明：

| 参数 | 说明 |
|------|------|
| `UserMessage` | 用户输入内容 |
| `chatId` | 会话 ID，用于区分不同对话上下文 |

返回：

- `text/event-stream`
- 前端逐段读取 `data:` 内容并拼接展示

### 2. 清除会话记忆

```http
DELETE /ai/answer/delmemory/{chatId}
```

作用：

- 清除指定会话在后端的上下文记忆
- 前端会同时清空对应本地聊天记录

---

## 关键实现说明

## 1. `AnswerService`

项目核心对话逻辑集中在 `AnswerService`：

- 初始化 `ChatClient`
- 注入系统提示词
- 绑定 Redis 会话记忆
- 在流式调用时追加日志 Advisor 与向量检索 Advisor

## 2. `VectorStoreConfig`

负责：

- 加载 Markdown 文档
- 文本切分
- 关键词增强
- 初始化 `SimpleVectorStore`

这是 RAG 检索链路的入口。

## 3. `ToolsConfig`

负责统一注册项目内的工具回调，后续若要增强 Agent 能力，可在这里继续扩展。

## 4. 前端 `chat.js`

前端通过 `fetch + ReadableStream` 手动消费 SSE 数据流，而不是使用传统的整包响应方式，因此能实现更自然的“边生成边展示”体验。

---

## 运行产物与临时目录

项目运行过程中会使用以下临时目录：

```text
tmp/
├── chat-memory/    # 文件型会话记忆（备用实现）
├── file/           # 文件工具读写目录
├── pdf/            # PDF 输出目录
└── download/       # 资源下载目录
```

---

## 当前已知特点与改进建议

### 1. 向量库是内存型

当前使用 `SimpleVectorStore`，服务重启后索引会丢失并重新构建。

建议：

- 切换到可持久化的向量数据库
- 或将索引结果序列化保存，降低启动成本

### 2. 启动时 RAG 构建可能较慢

关键词增强阶段会调用模型，对文档 chunk 较多时启动耗时会增加。

建议：

- 在离线阶段完成切分和增强
- 对结果做缓存或持久化

### 3. 工具已注册但主链路仍以对话 + RAG 为主

当前项目已经具备工具扩展基础，但默认聊天服务还可以继续增强为更完整的 Agent 执行链路。

### 4. 知识库内容与“代码助手”定位暂未完全一致

当前知识库文档更偏示例数据，如果项目目标是面向程序员的实用助手，建议替换为：

- 项目开发文档
- 常见故障手册
- 接口说明
- 编码规范
- 部署手册

---

## 适用场景

这个项目适合用于：

- 学习 Spring AI 基础用法
- 实践 SSE 流式聊天
- 理解 Chat Memory 的实现方式
- 入门 RAG 文档检索流程
- 体验 Tool Calling / MCP 的扩展方式
- 构建自己的垂直领域 AI 助手

---

## 后续可扩展方向

- 将向量库切换到持久化方案
- 将工具真正接入主对话链路
- 增加会话列表接口，由后端统一管理历史会话
- 支持上传项目文档，动态构建代码知识库
- 增加用户身份体系与多用户隔离
- 补充测试与部署脚本
- 使用环境变量管理全部敏感配置

---

## 许可证

当前仓库未看到明确 License 文件。

如果你准备开源，建议补充：

- `MIT`
- `Apache-2.0`
- 或其他你希望采用的许可证
