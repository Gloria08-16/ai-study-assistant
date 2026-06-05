<template>
  <div class="chat-container">
    <!-- 左侧历史会话列表 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <span class="sidebar-title">会话</span>
        <button class="btn-new-chat" @click="newConversation" title="新建对话">
          <el-icon :size="18"><Plus /></el-icon>
        </button>
      </div>
      <!-- 会话搜索框 -->
      <div class="session-search-bar">
        <el-icon :size="14" class="search-icon"><Search /></el-icon>
        <input
          v-model="searchQuery"
          class="session-search-input"
          placeholder="搜索会话..."
          @click.stop
        />
        <button v-if="searchQuery" class="btn-clear-search" @click.stop="searchQuery = ''" title="清除">
          <el-icon :size="12"><Close /></el-icon>
        </button>
      </div>
      <div class="session-list">
        <div
          v-for="session in filteredSessions"
          :key="session.id"
          :class="['session-item', { active: store.activeSessionId === session.id }]"
          @click="selectSession(session)"
        >
          <input
            v-if="editingSessionId === session.id"
            v-model="editTitle"
            class="rename-input"
            @blur="confirmRename(session)"
            @keydown.enter="confirmRename(session)"
            @keydown.esc="cancelRename"
            @click.stop
          />
          <span
            v-else
            class="session-title"
            @dblclick.stop="startRename(session)"
            title="双击修改名称"
          >{{ session.title }}</span>

          <button class="btn-delete-session" @click.stop="deleteSession(session.id)" title="删除会话">
            <el-icon :size="13"><Close /></el-icon>
          </button>
        </div>
        <div v-if="filteredSessions.length === 0" class="no-sessions">
          {{ searchQuery ? '未找到匹配的会话' : '暂无会话，点击 + 创建' }}
        </div>
      </div>
    </aside>

    <!-- 右侧主聊天区域 -->
    <section class="main-chat">
      <div class="message-area" ref="messageArea" @click="handleCopyCode">
        <div v-if="store.currentMessages.length === 0 && !loading" class="welcome">
          <div class="welcome-icon">✦</div>
          <h2>开始新的对话</h2>
          <p>在下方输入你的问题，AI 助手将竭诚为你解答</p>
        </div>

        <div v-for="(msg, index) in store.currentMessages" :key="index"
          :class="['message-row', msg.role === 'user' ? 'row-user' : 'row-ai']">
          <div :class="['bubble', msg.role === 'user' ? 'bubble-user' : 'bubble-ai']">
            <div v-if="msg.role === 'user'" class="bubble-text">{{ msg.content }}</div>
            <div v-else class="bubble-text ai-reply-content" v-html="renderMarkdown(msg.content)"></div>
          </div>
        </div>

        <div v-if="loading" class="message-row row-ai">
          <div class="bubble bubble-ai bubble-loading">
            <span class="dot-pulse"></span>
          </div>
        </div>
      </div>

      <!-- 底部输入区 -->
      <div class="input-area">
        <div class="rag-toggle-row">
          <label class="rag-toggle-label">
            <el-switch v-model="useKnowledge" size="small" />
            <span class="rag-toggle-text">开启私人知识库增强</span>
          </label>
          <button class="btn-settings-toggle" :class="{ active: showSettings }"
            @click="showSettings = !showSettings" title="AI 参数设置">
            <el-icon :size="15"><Setting /></el-icon>
            <span>参数</span>
          </button>
        </div>

        <!-- AI 参数设置面板 -->
        <div v-show="showSettings" class="settings-panel">
          <div class="settings-row">
            <label class="settings-label">模型</label>
            <el-select v-model="model" size="small" class="settings-select">
              <el-option v-for="opt in modelOptions" :key="opt.value"
                :label="opt.label" :value="opt.value" />
            </el-select>
          </div>
          <div class="settings-row">
            <label class="settings-label">温度：{{ temperature }}</label>
            <input type="range" min="0" max="2" step="0.1" v-model.number="temperature"
              class="settings-slider" />
            <span class="settings-hint">低=严谨 高=创意</span>
          </div>
          <div class="settings-row">
            <label class="settings-label">最大长度：{{ maxTokens }}</label>
            <input type="range" min="256" max="8192" step="256" v-model.number="maxTokens"
              class="settings-slider" />
          </div>
        </div>

        <div class="input-wrapper">
          <textarea
            v-model="inputText"
            class="chat-input"
            placeholder="输入你的问题..."
            rows="1"
            @keydown="handleKeydown"
          ></textarea>
          <!-- 发送 / 停止按钮 -->
          <button v-if="loading" class="btn-stop" @click="stopGeneration" title="停止生成">
            <el-icon :size="18"><Close /></el-icon>
          </button>
          <button v-else
            class="btn-send"
            :disabled="!inputText.trim()"
            @click="sendMessage"
            title="发送"
          >
            <el-icon :size="18"><Promotion /></el-icon>
          </button>
        </div>
      </div>
    </section>

    <!-- 删除确认对话框 -->
    <el-dialog v-model="deleteDialogVisible" title="确认删除" width="380px"
      custom-class="glass-dialog" :close-on-click-modal="false">
      <p class="delete-hint">确定要删除会话「{{ deleteTargetTitle }}」吗？该会话下的所有消息也将被删除。</p>
      <template #footer>
        <button class="btn-cancel" @click="deleteDialogVisible = false">取消</button>
        <button class="btn-danger" @click="confirmDelete">确认删除</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import axios from 'axios'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import { useChatStore } from '../stores/chatStore'

const API_BASE = 'http://localhost:8080/api'
const store = useChatStore()

// ---------- Markdown 渲染器 ----------
const md = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true,
  highlight(str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return '<pre class="hljs"><code>' +
          hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
          '</code></pre>'
      } catch (_) {}
    }
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>'
  }
})

// 自定义围栏代码块渲染：包裹在容器中并添加语言标签与复制按钮
const defaultFence = md.renderer.rules.fence || function (tokens, idx, options, env, self) {
  return self.renderToken(tokens, idx, options)
}
md.renderer.rules.fence = function (tokens, idx, options, env, self) {
  const token = tokens[idx]
  const lang = token.info.trim()
  const code = token.content

  // 高亮处理
  let highlighted
  if (lang && hljs.getLanguage(lang)) {
    try {
      highlighted = hljs.highlight(code, { language: lang, ignoreIllegals: true }).value
    } catch (_) {
      highlighted = md.utils.escapeHtml(code)
    }
  } else {
    highlighted = md.utils.escapeHtml(code)
  }

  const langLabel = lang ? `<span class="code-lang">${lang}</span>` : ''

  return `<div class="code-block-wrapper">`
    + `<div class="code-block-header">${langLabel}<button class="btn-copy-code" title="复制代码"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg><span>复制</span></button></div>`
    + `<pre class="hljs"><code>${highlighted}</code></pre>`
    + `</div>`
}

function renderMarkdown(text) {
  if (!text) return ''
  return md.render(text)
}

// 代码块复制（通过事件委托）
function handleCopyCode(e) {
  const btn = e.target.closest('.btn-copy-code')
  if (!btn) return
  const wrapper = btn.closest('.code-block-wrapper')
  if (!wrapper) return
  const code = wrapper.querySelector('code')
  if (!code) return
  navigator.clipboard.writeText(code.textContent || '').then(() => {
    const span = btn.querySelector('span')
    if (span) {
      span.textContent = '已复制'
      setTimeout(() => { span.textContent = '复制' }, 2000)
    }
  }).catch(() => {
    // 降级方案
    const textarea = document.createElement('textarea')
    textarea.value = code.textContent || ''
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    const span = btn.querySelector('span')
    if (span) {
      span.textContent = '已复制'
      setTimeout(() => { span.textContent = '复制' }, 2000)
    }
  })
}

// ---------- 本地状态 ----------
const inputText = ref('')
const loading = ref(false)
const useKnowledge = ref(false)
const messageArea = ref(null)

const editingSessionId = ref(null)
const editTitle = ref('')

const deleteDialogVisible = ref(false)
const deleteTargetId = ref(null)
const deleteTargetTitle = ref('')

// AI 参数设置
const showSettings = ref(false)
const temperature = ref(0.7)
const maxTokens = ref(2048)
const model = ref('deepseek-chat')
const modelOptions = [
  { value: 'deepseek-chat', label: 'DeepSeek Chat（通用对话）' },
  { value: 'deepseek-reasoner', label: 'DeepSeek Reasoner（深度推理）' }
]

// 会话搜索
const searchQuery = ref('')

// SSE 中断控制器
const abortController = ref(null)

const DEFAULT_GREETING = '你好，我是你的AI智能助学助手，在学习的过程中，有什么不明白、不确定的问题都可以问我'

// 计算属性：根据搜索词过滤会话
const filteredSessions = computed(() => {
  if (!searchQuery.value.trim()) return store.sessions
  const q = searchQuery.value.trim().toLowerCase()
  return store.sessions.filter(s => s.title.toLowerCase().includes(q))
})

// ==================== 生命周期 ====================
onMounted(async () => {
  await loadSessions()
  if (store.sessions.length > 0) {
    store.setActiveSession(store.sessions[0].id)
    await loadMessages(store.sessions[0].id)
  } else {
    await autoCreateSession()
  }
  scrollToBottom()
})

// ==================== 会话管理 ====================
async function loadSessions() {
  try {
    const res = await axios.get(`${API_BASE}/sessions`)
    store.setSessions(res.data || [])
  } catch (e) {
    console.error('加载会话列表失败:', e)
  }
}

async function autoCreateSession() {
  try {
    const res = await axios.post(`${API_BASE}/sessions`, { userId: 1, title: '新对话' })
    const session = res.data
    store.addSession(session)
    store.setActiveSession(session.id)
    store.setMessages(session.id, [{ role: 'assistant', content: DEFAULT_GREETING }])
    await axios.post(`${API_BASE}/messages`, {
      sessionId: session.id, role: 'assistant', content: DEFAULT_GREETING
    })
  } catch (e) {
    console.error('自动创建会话失败:', e)
    store.setMessages(0, [{ role: 'assistant', content: DEFAULT_GREETING }])
  }
}

async function newConversation() {
  try {
    const res = await axios.post(`${API_BASE}/sessions`, { userId: 1, title: '新对话' })
    const session = res.data
    store.addSession(session)
    store.setActiveSession(session.id)
    store.setMessages(session.id, [{ role: 'assistant', content: DEFAULT_GREETING }])
    await axios.post(`${API_BASE}/messages`, {
      sessionId: session.id, role: 'assistant', content: DEFAULT_GREETING
    })
  } catch (e) {
    console.error('创建会话失败:', e)
  }
}

async function selectSession(session) {
  if (store.activeSessionId === session.id) return
  store.setActiveSession(session.id)
  await loadMessages(session.id)
  scrollToBottom()
}

function deleteSession(id) {
  const session = store.sessions.find(s => s.id === id)
  if (!session) return
  deleteTargetId.value = id
  deleteTargetTitle.value = session.title
  deleteDialogVisible.value = true
}

async function confirmDelete() {
  try {
    await axios.delete(`${API_BASE}/sessions/${deleteTargetId.value}`)
    const wasActive = store.activeSessionId === deleteTargetId.value
    store.removeSession(deleteTargetId.value)
    if (wasActive) {
      if (store.sessions.length > 0) {
        store.setActiveSession(store.sessions[0].id)
        await loadMessages(store.sessions[0].id)
      } else {
        store.setActiveSession(null)
        await autoCreateSession()
      }
    }
  } catch (e) {
    console.error('删除会话失败:', e)
  } finally {
    deleteDialogVisible.value = false
  }
}

// ==================== 改名 ====================
function startRename(session) {
  editingSessionId.value = session.id
  editTitle.value = session.title
  nextTick(() => {
    const el = document.querySelector('.rename-input')
    if (el) { el.focus(); el.select() }
  })
}

async function confirmRename(session) {
  const newTitle = editTitle.value.trim()
  if (newTitle && newTitle !== session.title) {
    try {
      await axios.put(`${API_BASE}/sessions/${session.id}`, { title: newTitle })
      store.updateSessionTitle(session.id, newTitle)
    } catch (e) {
      console.error('重命名失败:', e)
    }
  }
  editingSessionId.value = null
}

function cancelRename() {
  editingSessionId.value = null
}

// ==================== 消息 ====================
async function loadMessages(sessionId) {
  try {
    const res = await axios.get(`${API_BASE}/messages/${sessionId}`)
    if (res.data && res.data.length > 0) {
      store.setMessages(sessionId, res.data.map(m => ({ role: m.role, content: m.content })))
    } else {
      store.setMessages(sessionId, [{ role: 'assistant', content: DEFAULT_GREETING }])
      await axios.post(`${API_BASE}/messages`, {
        sessionId, role: 'assistant', content: DEFAULT_GREETING
      })
    }
  } catch (e) {
    console.error('加载消息失败:', e)
    store.setMessages(sessionId, [{ role: 'assistant', content: DEFAULT_GREETING }])
  }
}

function handleKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (messageArea.value) {
      messageArea.value.scrollTop = messageArea.value.scrollHeight
    }
  })
}

// ==================== SSE 流式发送 ====================
async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || loading.value) return
  if (!store.activeSessionId) await autoCreateSession()

  const sid = store.activeSessionId

  // 添加用户消息
  store.addMessage(sid, { role: 'user', content: text })
  inputText.value = ''
  scrollToBottom()

  // 保存用户消息到后端
  axios.post(`${API_BASE}/messages`, { sessionId: sid, role: 'user', content: text })
    .catch(e => console.error('保存用户消息失败:', e))

  // 添加占位 AI 消息
  store.addMessage(sid, { role: 'assistant', content: '' })
  loading.value = true
  scrollToBottom()

  // 构建对话历史上下文：取当前会话中最后20条消息（10轮对话）传给后端
  const allMsgs = store.currentMessages
  const recentHistory = allMsgs.slice(-20).map(m => ({ role: m.role, content: m.content }))

  // 创建 AbortController 用于中断请求
  const controller = new AbortController()
  abortController.value = controller

  try {
    const response = await fetch(`${API_BASE}/chat/stream`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        message: text,
        useKnowledge: useKnowledge.value,
        messages: recentHistory,          // 对话历史上下文
        temperature: temperature.value,   // 温度参数
        maxTokens: maxTokens.value,       // 最大输出 token 数
        model: model.value                // 模型选择
      }),
      signal: controller.signal
    })

    if (!response.ok) {
      throw new Error('HTTP ' + response.status)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let fullReply = ''
    let eventType = 'chunk'       // 当前事件类型
    let dataLines = []            // 多行 data 缓冲区（SSE 标准：多行 data 用 \n 拼接）

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        // SSE 事件边界：空行表示当前事件结束
        if (line.length === 0) {
          if (dataLines.length > 0) {
            // 将多行 data 用 \n 拼接还原（关键：\n 被 SSE 协议拆成了多行）
            const eventData = dataLines.join('\n')
            dataLines = []

            if (eventType === 'chunk' && eventData) {
              fullReply += eventData
              store.appendToLastAssistant(sid, eventData)
              scrollToBottom()
            } else if (eventType === 'done') {
              fullReply = eventData || fullReply
            } else if (eventType === 'error') {
              store.appendToLastAssistant(sid, eventData || '请求失败，请稍后重试')
            }
            eventType = 'chunk'
          }
          continue
        }

        if (line.startsWith('event:')) {
          eventType = line.substring(6).trim()
          dataLines = []
          continue
        }
        if (line.startsWith('data:')) {
          dataLines.push(line.substring(5))
          continue
        }
      }
    }

    // 保存完整 AI 回复
    if (fullReply) {
      axios.post(`${API_BASE}/messages`, {
        sessionId: sid, role: 'assistant', content: fullReply
      }).catch(e => console.error('保存AI回复失败:', e))
    }
  } catch (e) {
    if (e.name === 'AbortError') {
      // 用户主动中断：保留已生成的部分内容
      console.log('用户中断了 AI 回答')
      const msgs = store.currentMessages
      const last = msgs[msgs.length - 1]
      if (last && last.role === 'assistant' && last.content) {
        // 已有部分内容，追加中断提示
        store.appendToLastAssistant(sid, '\n\n---\n_（已停止生成）_')
      } else if (last && last.role === 'assistant' && last.content === '') {
        last.content = '_（已停止生成）_'
      }
    } else {
      console.error('SSE 流式请求失败:', e)
      const msgs = store.currentMessages
      const last = msgs[msgs.length - 1]
      if (last && last.role === 'assistant' && last.content === '') {
        last.content = '请求失败，请检查后端服务是否已启动。'
      } else {
        store.addMessage(sid, { role: 'assistant', content: '请求失败，请检查后端服务是否已启动。' })
      }
    }
  } finally {
    loading.value = false
    abortController.value = null
    scrollToBottom()
  }
}

// 停止 AI 生成
function stopGeneration() {
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
  }
}
</script>

<style scoped>
/* ========== 容器 ========== */
.chat-container {
  display: flex;
  height: 100%;
  background: transparent;
}

/* ========== 侧边栏 ========== */
.sidebar {
  width: 260px;
  background: linear-gradient(180deg,
    rgba(255,255,255,0.95) 0%,
    rgba(248,249,255,0.95) 40%,
    rgba(245,247,252,0.95) 100%);
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(142, 197, 252, 0.12);
  box-shadow: 2px 0 24px rgba(102, 126, 234, 0.06);
  z-index: 10;
  position: relative;
}

/* 侧边栏顶部装饰线 */
.sidebar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #667eea, #764ba2, #f093fb, #4facfe);
  opacity: 0.7;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 20px 12px;
}

.sidebar-title {
  font-size: 13px;
  font-weight: 600;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.8px;
}

.btn-new-chat {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.btn-new-chat:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 15px rgba(79, 172, 254, 0.4);
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 12px 12px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  margin-bottom: 2px;
  border-radius: 12px;
  cursor: pointer;
  color: #4b5563;
  font-size: 14px;
  transition: all 0.15s ease;
  border: none;
  position: relative;
}

.session-item:hover {
  background: linear-gradient(135deg, rgba(102,126,234,0.08) 0%, rgba(118,75,162,0.06) 100%);
}

.session-item.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
}

.session-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  user-select: none;
  font-weight: 450;
}

.rename-input {
  flex: 1;
  border: none;
  background: rgba(255,255,255,0.15);
  color: #fff;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  font-family: inherit;
}

.btn-delete-session {
  width: 22px;
  height: 22px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #d1d5db;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.15s ease;
  flex-shrink: 0;
}

.session-item:hover .btn-delete-session { opacity: 1; }
.session-item.active .btn-delete-session { color: rgba(255,255,255,0.5); }
.session-item.active .btn-delete-session:hover { background: rgba(255,255,255,0.15); color: #fff; }
.btn-delete-session:hover { background: #fee2e2; color: #ef4444; }

.no-sessions {
  padding: 40px 16px;
  text-align: center;
  color: #d1d5db;
  font-size: 13px;
}

/* ========== 会话搜索框 ========== */
.session-search-bar {
  display: flex;
  align-items: center;
  margin: 0 12px 4px;
  padding: 7px 10px;
  background: rgba(102, 126, 234, 0.06);
  border-radius: 10px;
  border: 1px solid rgba(102, 126, 234, 0.08);
  gap: 6px;
}

.search-icon {
  color: #9ca3af;
  flex-shrink: 0;
}

.session-search-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  color: #4b5563;
  font-family: inherit;
}

.session-search-input::placeholder {
  color: #c4c4c4;
}

.btn-clear-search {
  width: 18px;
  height: 18px;
  border: none;
  border-radius: 50%;
  background: rgba(0,0,0,0.06);
  color: #9ca3af;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  padding: 0;
  transition: all 0.15s ease;
}

.btn-clear-search:hover {
  background: rgba(0,0,0,0.12);
  color: #6b7280;
}

/* ========== 主聊天区 ========== */
.main-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: transparent;
  position: relative;
}

/* 主聊天区右上角装饰光斑 */
.main-chat::after {
  content: '';
  position: absolute;
  top: -60px;
  right: -40px;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(240, 147, 251, 0.12) 0%, transparent 70%);
  pointer-events: none;
  z-index: 0;
}

.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  padding: 40px;
}

.welcome-icon {
  font-size: 56px;
  margin-bottom: 20px;
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 30%, #d4b5f0 60%, #8ec5fc 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: welcomePulse 3s ease-in-out infinite;
}

@keyframes welcomePulse {
  0%, 100% { filter: brightness(1); }
  50% { filter: brightness(1.2); }
}

.welcome h2 {
  font-size: 22px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0 0 8px;
  letter-spacing: -0.4px;
}

.welcome p {
  font-size: 14px;
  color: #9ca3af;
  margin: 0;
  max-width: 320px;
  line-height: 1.6;
}

/* ========== 消息区域 ========== */
.message-area {
  flex: 1;
  overflow-y: auto;
  padding: 24px 20px;
  scroll-behavior: smooth;
  /* 微妙点阵纹理 */
  background-image:
    radial-gradient(circle, rgba(142, 197, 252, 0.06) 1px, transparent 1px);
  background-size: 20px 20px;
}

.message-row {
  display: flex;
  margin-bottom: 16px;
  padding: 0 40px;
}

.row-user { justify-content: flex-end; }
.row-ai { justify-content: flex-start; }

/* ========== 聊天气泡 ========== */
.bubble {
  max-width: 68%;
  padding: 14px 18px;
  border-radius: 18px;
  border: none;
}

.bubble-user {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-bottom-right-radius: 6px;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.25);
}

.bubble-ai {
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(12px);
  color: #1d1d1f;
  border-bottom-left-radius: 6px;
  box-shadow: 0 1px 4px rgba(142, 197, 252, 0.10), 0 3px 14px rgba(142, 197, 252, 0.06);
  border: 1px solid rgba(142, 197, 252, 0.12);
}

.bubble-text {
  font-size: 14.5px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
  letter-spacing: -0.1px;
}

/* Markdown 内容样式 —— 保留块级元素布局 */
.ai-reply-content :deep(ul), .ai-reply-content :deep(ol) { padding-left: 20px; margin: 4px 0; }
.ai-reply-content :deep(li) { margin-bottom: 2px; }
.ai-reply-content :deep(p) {
  white-space: pre-wrap !important;
  margin-bottom: 8px;
}
.ai-reply-content :deep(p:last-child) { margin-bottom: 0; }
.ai-reply-content :deep(p code) {
  background: rgba(0,0,0,0.06);
  padding: 2px 6px;
  border-radius: 4px;
}
.ai-reply-content :deep(blockquote) {
  border-left: 3px solid #667eea;
  padding-left: 12px;
  color: #6b7280;
  margin: 8px 0;
}

/* 加载动画 */
.bubble-loading { padding: 16px 24px; }

.dot-pulse {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4facfe, #00f2fe);
  animation: dotPulse 1.4s infinite ease-in-out both;
  position: relative;
}

.dot-pulse::before, .dot-pulse::after {
  content: '';
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4facfe, #00f2fe);
  position: absolute;
  top: 0;
  animation: dotPulse 1.4s infinite ease-in-out both;
}

.dot-pulse::before { left: -14px; animation-delay: -0.32s; }
.dot-pulse::after { left: 14px; animation-delay: 0.32s; }

@keyframes dotPulse {
  0%, 80%, 100% { opacity: 0.2; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1); }
}

/* ========== 底部输入区 ========== */
.input-area {
  padding: 16px 24px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* RAG 知识库增强开关 */
.rag-toggle-row {
  width: 100%;
  max-width: 720px;
  margin-bottom: 8px;
  padding: 0 4px;
  display: flex;
  justify-content: flex-start;
}

.rag-toggle-label {
  display: flex;
  align-items: center;
  gap: 9px;
  cursor: pointer;
  user-select: none;
}

.rag-toggle-text {
  font-size: 12.5px;
  font-weight: 480;
  color: #8e8e93;
  letter-spacing: -0.1px;
  transition: color 0.2s ease;
}

.rag-toggle-label:has(.el-switch.is-checked) .rag-toggle-text,
.rag-toggle-label:has(.is-checked) .rag-toggle-text {
  color: #667eea;
}

/* 穿透 el-switch 样式微调 */
.rag-toggle-row :deep(.el-switch) {
  --el-switch-on-color: #667eea;
  --el-switch-off-color: #d1d5db;
}

.rag-toggle-row :deep(.el-switch.is-checked .el-switch__core) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: transparent;
}

.rag-toggle-row :deep(.el-switch__core) {
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1.2);
}

/* AI 参数设置按钮 */
.btn-settings-toggle {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
  font-size: 12px;
  font-family: inherit;
  transition: all 0.2s ease;
  margin-left: auto;
}

.btn-settings-toggle:hover { color: #667eea; background: rgba(102, 126, 234, 0.06); }
.btn-settings-toggle.active { color: #667eea; background: rgba(102, 126, 234, 0.1); }

/* AI 参数设置面板 */
.settings-panel {
  width: 100%;
  max-width: 720px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(12px);
  border-radius: 12px;
  padding: 12px 16px;
  margin-bottom: 8px;
  border: 1px solid rgba(102, 126, 234, 0.1);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.settings-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.settings-label {
  font-size: 13px;
  font-weight: 500;
  color: #4b5563;
  min-width: 90px;
  flex-shrink: 0;
}

.settings-select {
  width: 220px;
}

.settings-slider {
  flex: 1;
  accent-color: #667eea;
  height: 4px;
  cursor: pointer;
}

.settings-hint {
  font-size: 11px;
  color: #9ca3af;
  flex-shrink: 0;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  width: 100%;
  max-width: 720px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-radius: 20px;
  padding: 8px 8px 8px 18px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.04), 0 1px 3px rgba(0,0,0,0.03);
  border: 1px solid rgba(0,0,0,0.04);
  transition: box-shadow 0.3s ease, border-color 0.3s ease;
}

.input-wrapper:focus-within {
  box-shadow: 0 4px 24px rgba(79, 172, 254, 0.15), 0 1px 3px rgba(79, 172, 254, 0.06);
  border-color: rgba(79, 172, 254, 0.25);
}

.chat-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 15px;
  line-height: 1.5;
  color: #1d1d1f;
  resize: none;
  font-family: inherit;
  padding: 4px 0;
  max-height: 120px;
}

.chat-input::placeholder { color: #c4c4c4; }

.btn-send {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.3s ease;
}

.btn-send:hover:not(:disabled) {
  transform: scale(1.06);
  box-shadow: 0 4px 18px rgba(79, 172, 254, 0.45);
}

.btn-send:disabled { background: #e5e7eb; color: #d1d5db; cursor: not-allowed; box-shadow: none; }

/* 停止生成按钮 */
.btn-stop {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.3s ease;
  animation: stopPulse 1.4s ease-in-out infinite;
}

.btn-stop:hover {
  transform: scale(1.06);
  box-shadow: 0 4px 18px rgba(239, 68, 68, 0.45);
}

@keyframes stopPulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.4); }
  50% { box-shadow: 0 0 0 8px rgba(239, 68, 68, 0); }
}

/* ========== 对话框按钮 ========== */
.btn-cancel, .btn-danger {
  padding: 10px 22px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
  border: none;
}

.btn-cancel { background: #f3f4f6; color: #4b5563; }
.btn-cancel:hover { background: #e5e7eb; }
.btn-danger { background: #ef4444; color: #fff; }
.btn-danger:hover { background: #dc2626; }

.delete-hint {
  color: #6b7280;
  font-size: 14px;
  line-height: 1.6;
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .sidebar { width: 72px; }
  .sidebar-title, .session-title, .btn-delete-session { display: none; }
  .session-item { justify-content: center; padding: 12px; }
  .message-row { padding: 0 8px; }
  .bubble { max-width: 88%; }
}
</style>

<!-- 全局样式：毛玻璃弹窗 & 代码块换行 & 代码复制按钮（非 scoped 穿透 v-html）-->
<style>
/* ===== 代码块容器与复制按钮 ===== */
.code-block-wrapper {
  position: relative;
  margin: 10px 0;
  border-radius: 8px;
  overflow: hidden;
}

.code-block-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  background: #1e1e1e;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}

.code-lang {
  font-size: 12px;
  color: #888;
  font-family: Consolas, Monaco, monospace;
}

.btn-copy-code {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border: 1px solid rgba(255,255,255,0.14);
  border-radius: 5px;
  background: transparent;
  color: #aaa;
  cursor: pointer;
  font-size: 12px;
  font-family: inherit;
  transition: all 0.2s ease;
}

.btn-copy-code:hover {
  background: rgba(255,255,255,0.08);
  color: #fff;
  border-color: rgba(255,255,255,0.25);
}

.code-block-wrapper .hljs {
  margin: 0 !important;
  border-radius: 0 0 8px 8px !important;
}

/* ===== Markdown 代码块强制换行 ===== */
.ai-reply-content pre {
  white-space: pre-wrap !important;
  word-wrap: break-word !important;
  word-break: break-all !important;
  overflow-x: auto !important;
  background-color: #2d2d2d !important;
  color: #ccc !important;
  padding: 12px 16px !important;
  border-radius: 8px !important;
  margin: 10px 0 !important;
  font-family: Consolas, Monaco, "Courier New", monospace !important;
  line-height: 1.5 !important;
}
.ai-reply-content pre code {
  white-space: pre-wrap !important;
  word-wrap: break-word !important;
  word-break: break-all !important;
  font-family: Consolas, Monaco, "Courier New", monospace !important;
  display: block !important;
}
.ai-reply-content pre code.hljs {
  white-space: pre-wrap !important;
  word-break: break-all !important;
  display: block !important;
}
.ai-reply-content code {
  font-family: Consolas, Monaco, "Courier New", monospace !important;
}
.ai-reply-content p {
  white-space: pre-wrap !important;
}

/* ===== 毛玻璃弹窗 ===== */
.glass-dialog {
  border-radius: 20px !important;
  overflow: hidden;
  border: none !important;
  box-shadow: 0 20px 60px rgba(0,0,0,0.12), 0 0 0 1px rgba(0,0,0,0.04) !important;
}

.glass-dialog .el-dialog__header { padding: 24px 28px 0; border-bottom: none; margin: 0; }
.glass-dialog .el-dialog__title { font-size: 17px; font-weight: 600; color: #1d1d1f; letter-spacing: -0.3px; }
.glass-dialog .el-dialog__body { padding: 12px 28px 20px; }
.glass-dialog .el-dialog__footer { padding: 0 28px 24px; }

.el-overlay {
  backdrop-filter: blur(6px) saturate(120%);
  -webkit-backdrop-filter: blur(6px) saturate(120%);
  background-color: rgba(0, 0, 0, 0.2) !important;
}
</style>
