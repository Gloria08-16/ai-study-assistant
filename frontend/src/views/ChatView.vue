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
      <div class="session-list">
        <div
          v-for="session in store.sessions"
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
        <div v-if="store.sessions.length === 0" class="no-sessions">
          暂无会话，点击 + 创建
        </div>
      </div>
    </aside>

    <!-- 右侧主聊天区域 -->
    <section class="main-chat">
      <div class="message-area" ref="messageArea">
        <div v-if="store.currentMessages.length === 0 && !loading" class="welcome">
          <div class="welcome-icon">✦</div>
          <h2>开始新的对话</h2>
          <p>在下方输入你的问题，AI 助手将竭诚为你解答</p>
        </div>

        <div v-for="(msg, index) in store.currentMessages" :key="index"
          :class="['message-row', msg.role === 'user' ? 'row-user' : 'row-ai']">
          <div :class="['bubble', msg.role === 'user' ? 'bubble-user' : 'bubble-ai']">
            <div v-if="msg.role === 'user'" class="bubble-text">{{ msg.content }}</div>
            <div v-else class="bubble-text markdown-body" v-html="renderMarkdown(msg.content)"></div>
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
        <div class="input-wrapper">
          <textarea
            v-model="inputText"
            class="chat-input"
            placeholder="输入你的问题..."
            rows="1"
            @keydown="handleKeydown"
          ></textarea>
          <button
            class="btn-send"
            :class="{ loading: loading }"
            :disabled="!inputText.trim() || loading"
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
import { ref, nextTick, onMounted } from 'vue'
import axios from 'axios'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import { useChatStore } from '../stores/chatStore'

const API_BASE = 'http://localhost:8080/api'
const store = useChatStore()

// ---------- Markdown 渲染器 ----------
const md = new MarkdownIt({
  html: false,
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

function renderMarkdown(text) {
  if (!text) return ''
  return md.render(text)
}

// ---------- 本地状态 ----------
const inputText = ref('')
const loading = ref(false)
const messageArea = ref(null)

const editingSessionId = ref(null)
const editTitle = ref('')

const deleteDialogVisible = ref(false)
const deleteTargetId = ref(null)
const deleteTargetTitle = ref('')

const DEFAULT_GREETING = '你好，我是你的AI智能助学助手，在学习的过程中，有什么不明白、不确定的问题都可以问我'

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

  try {
    const response = await fetch(`${API_BASE}/chat/stream`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: text })
    })

    if (!response.ok) {
      throw new Error('HTTP ' + response.status)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let fullReply = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (!line.startsWith('event:') && !line.startsWith('data:')) continue

        // SSE 事件名
        let eventType = 'chunk'
        let eventData = ''

        if (line.startsWith('event:')) {
          eventType = line.substring(6).trim()
          continue  // 需要下一行才是 data
        }
        if (line.startsWith('data:')) {
          eventData = line.substring(5).trim()
        }

        if (eventType === 'chunk' && eventData) {
          fullReply += eventData
          store.appendToLastAssistant(sid, eventData)
          scrollToBottom()
        } else if (eventType === 'done') {
          fullReply = eventData || fullReply
        } else if (eventType === 'error') {
          store.appendToLastAssistant(sid, eventData || '请求失败，请稍后重试')
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
    console.error('SSE 流式请求失败:', e)
    const msgs = store.currentMessages
    const last = msgs[msgs.length - 1]
    if (last && last.role === 'assistant' && last.content === '') {
      last.content = '请求失败，请检查后端服务是否已启动。'
    } else {
      store.addMessage(sid, { role: 'assistant', content: '请求失败，请检查后端服务是否已启动。' })
    }
  } finally {
    loading.value = false
    scrollToBottom()
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
  background: #fff;
  display: flex;
  flex-direction: column;
  border-right: none;
  box-shadow: 2px 0 24px rgba(0,0,0,0.03);
  z-index: 10;
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
  background: #f3f4f6;
}

.session-item.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
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

/* ========== 主聊天区 ========== */
.main-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: transparent;
  position: relative;
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
  font-size: 40px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
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
}

/* ========== 消息区域 ========== */
.message-area {
  flex: 1;
  overflow-y: auto;
  padding: 24px 20px;
  scroll-behavior: smooth;
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
}

.bubble-ai {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  color: #1d1d1f;
  border-bottom-left-radius: 6px;
  box-shadow: 0 1px 3px rgba(142, 197, 252, 0.12), 0 2px 12px rgba(142, 197, 252, 0.06);
  border: 1px solid rgba(142, 197, 252, 0.15);
}

.bubble-text {
  font-size: 14.5px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
  letter-spacing: -0.1px;
}

/* Markdown 内容样式 */
.markdown-body :deep(p) { margin: 0 0 8px; }
.markdown-body :deep(p:last-child) { margin-bottom: 0; }
.markdown-body :deep(ul), .markdown-body :deep(ol) { padding-left: 20px; margin: 4px 0; }
.markdown-body :deep(li) { margin-bottom: 2px; }
.markdown-body :deep(pre) {
  background: #1e1e1e;
  border-radius: 10px;
  padding: 14px 16px;
  overflow-x: auto;
  margin: 8px 0;
  font-size: 13px;
}
.markdown-body :deep(code) {
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
}
.markdown-body :deep(p code) {
  background: rgba(0,0,0,0.06);
  padding: 2px 6px;
  border-radius: 4px;
}
.markdown-body :deep(blockquote) {
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
  justify-content: center;
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
.btn-send.loading { background: linear-gradient(135deg, #a0c0f0 0%, #80d8e8 100%); pointer-events: none; }

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

<!-- 全局样式：毛玻璃弹窗 -->
<style>
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
