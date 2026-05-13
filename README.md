# AI-powered-answers

一个面向技术问答场景的全栈 AI 对话项目，采用 **Spring Boot + Spring AI + Vue 3** 构建，提供流式聊天、Redis 会话记忆、RAG 检索增强和可扩展工具调用能力。

项目当前定位为“代码小助手”，主要用于代码优化、Bug 排查、技术概念解释和基础架构问答，也适合作为学习 Spring AI、RAG、SSE 流式输出与 Agent 工具集成的练手项目。

---

## 项目结构

```text
.
├── ai-server/      # Spring Boot + Spring AI 后端
├── ai-frontend/    # Vue 3 + Vite 前端
└── README.md
```

---

## 核心能力

- 基于 **SSE** 的流式对话输出
- 基于 **Redis** 的多轮会话记忆
- 基于 **Spring AI + VectorStore** 的 RAG 检索增强
- 可扩展的 **Tool Calling** 工具注册机制
- 前端支持历史会话、本地持久化、Markdown 渲染与代码高亮
- 预留 MCP 扩展能力

---

## 技术栈

### 后端 `ai-server`

- Java 21
- Spring Boot 3.5.13
- Spring AI 1.0.0
- Spring AI Alibaba DashScope
- Spring AI MCP Client
- Redis
- SimpleVectorStore
- Hutool
- Jsoup
- iText PDF
- Kryo

### 前端 `ai-frontend`

- Vue 3
- Vite 5
- Axios
- marked
- highlight.js
- DOMPurify

---

## 系统架构

```text
Vue 3 Frontend
   ↓ HTTP / SSE
AnswerController
   ↓
AnswerService
   ├── ChatClient
   ├── RedisChatMemory
   ├── QuestionAnswerAdvisor
   └── MyLoggerAdvisor
              ↓
     DashScope Chat Model
              ↓
     SimpleVectorStore / RAG
```

---

## 后端说明

后端核心逻辑集中在以下几个位置：

- `ai-server/src/main/java/com/server/controller/AnswerController.java`
  - 提供流式聊天接口和清除记忆接口
- `ai-server/src/main/java/com/server/service/AnswerService.java`
  - 负责组装 `ChatClient`、系统提示词、会话记忆和 RAG Advisor
- `ai-server/src/main/java/com/server/chatmemory/RedisChatMemory.java`
  - 使用 Redis 存储聊天消息，默认保留最近 20 条，TTL 为 7 天
- `ai-server/src/main/java/com/server/config/VectorStoreConfig.java`
  - 启动时加载文档、切分文档、补充关键词并写入 `SimpleVectorStore`
- `ai-server/src/main/java/com/server/config/ToolsConfig.java`
  - 统一注册工具能力

### 已注册工具

当前工具配置中已注册以下能力：

- 文件读写
- Web 搜索
- 网页抓取
- 资源下载
- PDF 生成
- 终止任务
- 基于向量检索的建议工具
- 智谱 MCP 搜索封装

说明：终端执行工具被明确注释为高风险，当前未注册到默认工具链中。

---

## 前端说明

前端主要界面位于 `ai-frontend/src/App.vue`，当前实现包含：

- 新建会话
- 历史会话切换与删除
- 本地 `localStorage` 持久化
- Markdown 渲染
- 代码高亮
- 输入框自动增高
- `Enter` 发送、`Shift + Enter` 换行
- 流式输出过程中手动停止生成
- 清除当前会话的后端记忆

流式请求封装位于 `ai-frontend/src/api/chat.js`，通过 `fetch + ReadableStream` 手动消费 SSE 数据。

---

## 当前接口

### 1. 流式聊天

```http
GET /ai/answer/sse?userMessage=xxx&chatId=xxx
```

参数：

| 参数 | 说明 |
|------|------|
| `userMessage` | 用户输入内容 |
| `chatId` | 会话 ID，用于隔离上下文 |

返回类型：

- `text/event-stream`
- 前端按 `data:` 逐段读取并实时渲染

### 2. 清除会话记忆

```http
DELETE /ai/answer/delmemory/{chatId}
```

作用：

- 清除指定会话在 Redis 中的聊天上下文
- 前端会同步清空当前会话展示内容

---

## RAG 知识库机制

启动后端时，项目会自动读取 `ai-server/src/main/resources/document/` 下的文档，并执行以下流程：

```text
加载文档
→ 文本切分
→ 关键词增强
→ 向量化
→ 写入 SimpleVectorStore
```

当前特性：

- 使用 `SimpleVectorStore`，向量数据默认驻留内存
- 服务重启后会重新构建索引
- 关键词增强阶段会增加启动时模型调用成本

如果后续要把项目用于更真实的编程问答，建议把文档内容替换为：

- 项目开发文档
- 接口文档
- 常见故障手册
- 编码规范
- 部署手册

---

## 会话记忆机制

当前项目默认启用 Redis 记忆实现：

- Key 前缀：`chat:`
- 每个会话只保留最近 **20** 条消息
- 过期时间为 **7 天**

这意味着同一个 `chatId` 下可以持续多轮对话，但不会无限增长。

---

## 运行要求

### 后端

- JDK 21
- Maven 3.9+
- Redis
- DashScope 可用 API Key
- 额外搜索或 MCP 能力所需 API Key

### 前端

- Node.js 18+
- npm

---

## 配置说明

后端配置文件位于：

`ai-server/src/main/resources/application.yaml`

当前主要依赖以下环境变量：

```yaml
spring:
  ai:
    dashscope:
      api-key: ${SPRING_AI_DASH_SCOPE_API_KEY}
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST}
      port: ${SPRING_DATA_REDIS_PORT}
      database: ${SPRING_DATA_REDIS_DATABASE}

search-api:
  api-key: ${SPRING_SEARCH_API_KEY}

zhipu:
  api-key: ${ZHIPU_API_KEY}
```

你至少需要准备：

- `SPRING_AI_DASH_SCOPE_API_KEY`
- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT`
- `SPRING_DATA_REDIS_DATABASE`

如果要启用额外搜索能力，还需要：

- `SPRING_SEARCH_API_KEY`
- `ZHIPU_API_KEY`

建议不要把真实密钥直接提交到仓库。

---

## 快速启动

### 1. 启动 Redis

请确保本地或远程 Redis 可用，并与 `application.yaml` 中的配置一致。

### 2. 启动后端

```bash
cd ai-server
mvn spring-boot:run
```

默认端口：`8080`

### 3. 启动前端

```bash
cd ai-frontend
npm install
npm run dev
```

默认端口：`3000`

前端通过 Vite 代理将 `/ai` 请求转发到：

```text
http://localhost:8080
```

---

## 使用流程

1. 打开前端页面
2. 创建或进入一个会话
3. 输入技术问题并发送
4. 前端通过 SSE 实时接收模型输出
5. 同一 `chatId` 下的上下文会保存在 Redis 中
6. 如需重置上下文，可清除当前会话记忆

---

## 当前实现特点

### 优点

- 项目链路完整，适合学习全栈 AI 对话系统
- 流式返回体验自然
- Redis 会话记忆比单纯内存更实用
- 已具备 RAG 与工具扩展基础
- 前端交互已经覆盖常见聊天场景

### 当前限制

- 向量库仍是内存型，重启后会重新构建
- RAG 文档构建在启动阶段完成，文档多时会拖慢启动
- 工具虽然已注册，但主链路仍以对话 + RAG 为主
- 系统目前更偏技术问答助手，还不是完整自治 Agent

---

## 适用场景

这个项目适合：

- 学习 Spring AI 的基础用法
- 理解 SSE 流式对话实现
- 练习 Redis Chat Memory 集成
- 入门 RAG 文档检索流程
- 体验 Tool Calling / MCP 扩展方式
- 作为垂直领域 AI 助手的原型项目

---

## 后续可优化方向

- 将 `SimpleVectorStore` 替换为可持久化向量数据库
- 为 RAG 建立离线索引流程，减少启动成本
- 为工具调用设计更清晰的 Agent 执行链路
- 补充测试用例和更完整的异常处理
- 将知识库内容替换为更贴近“代码助手”定位的资料
