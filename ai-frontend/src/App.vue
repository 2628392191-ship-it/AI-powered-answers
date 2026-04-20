<template>
  <div class="app">
    <!-- Sidebar -->
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <div class="agent-avatar">
          <AgentIcon />
        </div>
        <div class="agent-info">
          <h2>代码小助手</h2>
          <div>
            <span class="status-dot"></span>
            <span class="status-text">在线</span>
          </div>
        </div>
      </div>

      <button class="new-chat-btn" @click="createNewChat">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
        新建对话
      </button>

      <div class="history-label">历史对话</div>
      <div class="chat-history">
        <div v-if="chatSessions.length === 0" class="empty-history">
          暂无历史对话
        </div>
        <div
          v-for="session in sortedSessions"
          :key="session.id"
          class="history-item"
          :class="{ active: session.id === currentChatId }"
          @click="switchChat(session.id)"
        >
          <div class="history-item-icon">💬</div>
          <div class="history-item-content">
            <div class="history-item-title">{{ session.title }}</div>
            <div class="history-item-time">{{ formatTime(session.updatedAt) }}</div>
          </div>
          <button class="history-item-del" @click.stop="deleteChat(session.id)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
      </div>
    </aside>

    <!-- Main chat area -->
    <main class="chat-main">
      <header class="chat-header">
        <button class="sidebar-toggle" @click="sidebarCollapsed = !sidebarCollapsed">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/>
          </svg>
        </button>
        <div class="header-title">{{ currentChatTitle }}</div>
        <button class="clear-btn" @click="clearCurrentChatMemory" title="清除当前对话记忆">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4h6v2"/>
          </svg>
        </button>
      </header>

      <div class="messages" ref="messagesContainer">
        <!-- Welcome screen -->
        <div v-if="messages.length === 0" class="welcome-screen">
          <div class="welcome-avatar">
            <AgentIcon />
          </div>
          <h1>你好，我是代码小助手 👾</h1>
          <p>专注于代码优化 & Bug 排查，随时准备帮你 debug！</p>
          <div class="suggestions">
            <button
              v-for="(suggestion, idx) in suggestions"
              :key="idx"
              class="suggestion-chip"
              @click="sendSuggestion(suggestion.text)"
            >
              {{ suggestion.label }}
            </button>
          </div>
        </div>

        <!-- Messages -->
        <div v-for="(msg, idx) in messages" :key="idx" class="message" :class="msg.role">
          <div class="msg-avatar" :class="msg.role === 'ai' ? 'ai-avatar' : 'user-avatar'">
            <AgentIcon v-if="msg.role === 'ai'" />
            <span v-else>你</span>
          </div>
          <div class="msg-body">
            <div class="msg-name">{{ msg.role === 'ai' ? '代码小助手' : '你' }}</div>
            <div class="msg-content" v-html="msg.html"></div>
          </div>
        </div>

        <!-- Typing indicator: only show when streaming but no content yet -->
        <div v-if="isStreaming && streamingContent === ''" class="message ai">
          <div class="msg-avatar ai-avatar">
            <AgentIcon />
          </div>
          <div class="msg-body">
            <div class="msg-name">代码小助手</div>
            <div class="msg-content">
              <div class="typing-indicator">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="input-area">
        <div class="input-wrapper">
          <textarea
            v-model="userInput"
            @keydown="handleKeydown"
            placeholder="输入你的问题... (Shift+Enter 换行，Enter 发送)"
            rows="1"
            ref="textarea"
          ></textarea>
          <button class="send-btn" @click="sendMessage" :disabled="!userInput.trim() || isStreaming">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
            </svg>
          </button>
        </div>
        <div class="input-hint">代码小助手可能会犯错，重要代码请自行验证</div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, watch, onMounted } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import AgentIcon from './components/AgentIcon.vue'
import { streamChat, clearMemory } from './api/chat'

// Configure marked
marked.setOptions({
  highlight: (code, lang) => {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true,
  gfm: true
})

// State
const sidebarCollapsed = ref(false)
const currentChatId = ref(generateChatId())
const chatSessions = ref([])
const messages = ref([])
const userInput = ref('')
const isStreaming = ref(false)
const streamingContent = ref('')
const messagesContainer = ref(null)
const textarea = ref(null)

const suggestions = [
  { label: '⚡ 代码性能优化', text: '帮我优化这段 Python 代码的性能' },
  { label: '🐛 Bug 排查', text: '我的 Spring Boot 项目启动报错，帮我排查' },
  { label: '📖 概念解释', text: '解释一下 Java 中的 volatile 关键字' },
  { label: '🏗️ 架构设计', text: '帮我设计一个高并发的数据库架构' }
]

const currentChatTitle = computed(() => {
  const session = chatSessions.value.find(s => s.id === currentChatId.value)
  return session?.title || '代码小助手'
})

const sortedSessions = computed(() => {
  return [...chatSessions.value].sort((a, b) => b.updatedAt - a.updatedAt)
})

// Lifecycle
onMounted(() => {
  loadChatSessions()
  loadCurrentChat()
})

watch(userInput, () => {
  nextTick(() => autoResizeTextarea())
})

// Functions
function generateChatId() {
  return Date.now().toString(36) + Math.random().toString(36).substr(2)
}

function loadChatSessions() {
  const saved = localStorage.getItem('chatSessions')
  if (saved) {
    chatSessions.value = JSON.parse(saved)
  }
}

function saveChatSessions() {
  localStorage.setItem('chatSessions', JSON.stringify(chatSessions.value))
}

function getCurrentSession() {
  return chatSessions.value.find(s => s.id === currentChatId.value)
}

function loadCurrentChat() {
  const session = getCurrentSession()
  if (session && session.messages.length > 0) {
    messages.value = session.messages.map(msg => ({
      ...msg,
      html: msg.role === 'ai' ? marked.parse(msg.content) : escapeHtml(msg.content)
    }))
    nextTick(() => scrollToBottom())
  }
}

function createNewChat() {
  currentChatId.value = generateChatId()
  messages.value = []
  userInput.value = ''
}

function switchChat(chatId) {
  currentChatId.value = chatId
  messages.value = []
  loadCurrentChat()
}

function deleteChat(chatId) {
  if (!confirm('确定要删除这个对话吗？')) return
  
  chatSessions.value = chatSessions.value.filter(s => s.id !== chatId)
  saveChatSessions()
  
  if (currentChatId.value === chatId) {
    createNewChat()
  }
}

async function clearCurrentChatMemory() {
  if (!confirm('确定要清除当前对话的记忆吗？')) return
  
  try {
    await clearMemory(currentChatId.value)
    
    const session = getCurrentSession()
    if (session) {
      session.messages = []
      saveChatSessions()
    }
    messages.value = []
    showToast('✅ 对话记忆已清除')
  } catch (error) {
    console.error('Clear memory error:', error)
    showToast('❌ 清除失败')
  }
}

function sendSuggestion(text) {
  userInput.value = text
  sendMessage()
}

async function sendMessage() {
  const text = userInput.value.trim()
  if (!text || isStreaming.value) return
  
  userInput.value = ''
  
  // Add user message
  const userMsg = { role: 'user', content: text, html: escapeHtml(text) }
  messages.value.push(userMsg)
  
  // Create or update session
  let session = getCurrentSession()
  if (!session) {
    session = {
      id: currentChatId.value,
      title: text.slice(0, 30) + (text.length > 30 ? '...' : ''),
      messages: [],
      createdAt: Date.now(),
      updatedAt: Date.now()
    }
    chatSessions.value.push(session)
  }
  
  session.messages.push({ role: 'user', content: text })
  session.updatedAt = Date.now()
  saveChatSessions()
  
  nextTick(() => scrollToBottom())
  
  // Stream AI response
  isStreaming.value = true
  streamingContent.value = ''

  // Push a reactive placeholder for the AI message
  messages.value.push({ role: 'ai', content: '', html: '' })
  const aiMsgIndex = messages.value.length - 1

  try {
    await streamChat(text, currentChatId.value, (chunk) => {
      streamingContent.value += chunk
      // Directly set on the reactive array element so Vue tracks the change
      messages.value[aiMsgIndex] = {
        role: 'ai',
        content: streamingContent.value,
        html: marked.parse(streamingContent.value)
      }
      nextTick(() => {
        highlightCodeBlocks()
        scrollToBottom()
      })
    })

    // Save AI response
    session.messages.push({ role: 'ai', content: streamingContent.value })
    session.updatedAt = Date.now()
    saveChatSessions()
  } catch (error) {
    console.error('Stream error:', error)
    messages.value.splice(aiMsgIndex, 1)
    messages.value.push({
      role: 'ai',
      content: '抱歉，服务出现了问题，请稍后再试 😢',
      html: '<p>抱歉，服务出现了问题，请稍后再试 😢</p>'
    })
  } finally {
    isStreaming.value = false
    streamingContent.value = ''
  }
}

function handleKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

function autoResizeTextarea() {
  if (!textarea.value) return
  textarea.value.style.height = 'auto'
  textarea.value.style.height = Math.min(textarea.value.scrollHeight, 160) + 'px'
}

function scrollToBottom() {
  if (!messagesContainer.value) return
  messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
}

function highlightCodeBlocks() {
  if (!messagesContainer.value) return
  messagesContainer.value.querySelectorAll('pre code').forEach((block) => {
    if (!block.dataset.highlighted) {
      hljs.highlightElement(block)
      block.dataset.highlighted = 'yes'
      
      // Add copy button
      if (!block.parentElement.querySelector('.copy-code-btn')) {
        const copyBtn = document.createElement('button')
        copyBtn.className = 'copy-code-btn'
        copyBtn.textContent = '复制'
        copyBtn.onclick = () => {
          navigator.clipboard.writeText(block.textContent)
          copyBtn.textContent = '已复制!'
          setTimeout(() => copyBtn.textContent = '复制', 2000)
        }
        block.parentElement.style.position = 'relative'
        block.parentElement.appendChild(copyBtn)
      }
    }
  })
}

function formatTime(timestamp) {
  const now = Date.now()
  const diff = now - timestamp
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  
  if (diff < minute) return '刚刚'
  if (diff < hour) return Math.floor(diff / minute) + '分钟前'
  if (diff < day) return Math.floor(diff / hour) + '小时前'
  if (diff < 7 * day) return Math.floor(diff / day) + '天前'
  
  const date = new Date(timestamp)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

function escapeHtml(text) {
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}

function showToast(message) {
  const toast = document.createElement('div')
  toast.textContent = message
  toast.style.cssText = `
    position: fixed;
    top: 20px;
    left: 50%;
    transform: translateX(-50%);
    background: var(--bg-tertiary);
    color: var(--text-primary);
    padding: 12px 20px;
    border-radius: 8px;
    border: 1px solid var(--border);
    z-index: 1000;
  `
  document.body.appendChild(toast)
  setTimeout(() => toast.remove(), 2000)
}
</script>
