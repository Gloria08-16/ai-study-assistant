<template>
  <div class="chat-container">
    <!-- 左侧历史会话列表 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <h3>历史会话</h3>
        <el-button type="primary" size="small" @click="newConversation" class="new-chat-btn">
          <el-icon><Plus /></el-icon>
          新对话
        </el-button>
      </div>
      <div class="session-list">
        <div
          v-for="session in sessions"
          :key="session.id"
          :class="['session-item', { active: activeSessionId === session.id }]"
          @click="selectSession(session)"
        >
          <el-icon><ChatLineSquare /></el-icon>

          <!-- 改名模式 -->
          <el-input
            v-if="editingSessionId === session.id"
            v-model="editTitle"
            size="small"
            class="rename-input"
            @blur="confirmRename(session)"
            @keydown.enter="confirmRename(session)"
            @keydown.esc="cancelRename"
            @click.stop
            ref="renameInputRef"
          />

          <!-- 普通显示 + 双击改名 -->
          <span
            v-else
            class="session-title"
            @dblclick.stop="startRename(session)"
            :title="'双击修改名称'"
          >{{ session.title }}</span>

          <el-button
            type="danger"
            size="small"
            :icon="Delete"
            circle
            class="delete-session-btn"
            @click.stop="deleteSession(session.id)"
            title="删除会话"
          />
        </div>

        <!-- 无会话时的提示 -->
        <div v-if="sessions.length === 0" class="no-sessions">
          暂无会话，点击上方按钮创建
        </div>
      </div>
    </div>

    <!-- 右侧主聊天区域 -->
    <div class="main-chat">
      <div class="chat-header">
        <h2>AI 智能伴学</h2>
      </div>

      <!-- 消息列表 -->
      <div class="message-area" ref="messageArea">
        <div v-for="(msg, index) in currentMessages" :key="index"
          :class="['message-item', msg.role === 'user' ? 'user-msg' : 'ai-msg']">
          <div class="avatar">
            <el-avatar v-if="msg.role === 'user'" :icon="UserFilled" />
            <el-avatar v-else>
              <el-icon><ChatDotRound /></el-icon>
            </el-avatar>
          </div>
          <div class="bubble">
            <div class="role-label">{{ msg.role === 'user' ? '我' : 'AI助手' }}</div>
            <div class="content">{{ msg.content }}</div>
          </div>
        </div>

        <!-- 加载中 -->
        <div v-if="loading" class="message-item ai-msg">
          <div class="avatar">
            <el-avatar><el-icon><ChatDotRound /></el-icon></el-avatar>
          </div>
          <div class="bubble">
            <div class="role-label">AI助手</div>
            <div class="content">正在思考中...</div>
          </div>
        </div>
      </div>

      <!-- 底部输入区 -->
      <div class="input-area">
        <el-input v-model="inputText" placeholder="请输入你的问题... (Enter发送，Shift+Enter换行)"
          :rows="3" type="textarea" resize="none" @keydown="handleKeydown" />
        <el-button type="primary" @click="sendMessage" :loading="loading" class="send-btn">
          <el-icon><Promotion /></el-icon>
          发送
        </el-button>
      </div>
    </div>

    <!-- 删除确认对话框 -->
    <el-dialog v-model="deleteDialogVisible" title="确认删除" width="400px">
      <p>确定要删除会话「{{ deleteTargetTitle }}」吗？该会话下的所有消息也将被删除。</p>
      <template #footer>
        <el-button @click="deleteDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmDelete">确认删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import axios from 'axios'

const API_BASE = 'http://localhost:8080/api'

// ---------- 会话状态 ----------
const sessions = ref([])
const activeSessionId = ref(null)

// ---------- 消息状态 ----------
const currentMessages = ref([])

const inputText = ref('')
const loading = ref(false)
const messageArea = ref(null)

// ---------- 改名状态 ----------
const editingSessionId = ref(null)
const editTitle = ref('')
const renameInputRef = ref(null)

// ---------- 删除状态 ----------
const deleteDialogVisible = ref(false)
const deleteTargetId = ref(null)
const deleteTargetTitle = ref('')

// 初始 AI 问候语
const DEFAULT_GREETING = '你好，我是你的AI智能助学助手，在学习的过程中，有什么不明白、不确定的问题都可以问我'

// ==================== 生命周期 ====================
onMounted(async () => {
  await loadSessions()
  if (sessions.value.length > 0) {
    // 默认选中第一个会话
    activeSessionId.value = sessions.value[0].id
    await loadMessages(sessions.value[0].id)
  } else {
    // 没有任何会话，自动创建一个
    await autoCreateSession()
  }
})

// ==================== 会话管理 ====================

// 从后端加载会话列表
async function loadSessions() {
  try {
    const res = await axios.get(`${API_BASE}/sessions`)
    sessions.value = res.data || []
  } catch (e) {
    console.error('加载会话列表失败:', e)
  }
}

// 自动创建第一个会话
async function autoCreateSession() {
  try {
    const res = await axios.post(`${API_BASE}/sessions`, {
      userId: 1,
      title: '新对话'
    })
    const session = res.data
    sessions.value = [session]
    activeSessionId.value = session.id

    // 保存 AI 初始问候语
    await axios.post(`${API_BASE}/messages`, {
      sessionId: session.id,
      role: 'assistant',
      content: DEFAULT_GREETING
    })
    currentMessages.value = [{ role: 'assistant', content: DEFAULT_GREETING }]
  } catch (e) {
    console.error('自动创建会话失败:', e)
    // 兜底：至少显示本地消息
    currentMessages.value = [{ role: 'assistant', content: DEFAULT_GREETING }]
  }
}

// 新建对话
async function newConversation() {
  try {
    const res = await axios.post(`${API_BASE}/sessions`, {
      userId: 1,
      title: '新对话'
    })
    const session = res.data
    sessions.value.unshift(session)
    activeSessionId.value = session.id

    // 保存 AI 初始问候语
    await axios.post(`${API_BASE}/messages`, {
      sessionId: session.id,
      role: 'assistant',
      content: DEFAULT_GREETING
    })
    currentMessages.value = [{ role: 'assistant', content: DEFAULT_GREETING }]
  } catch (e) {
    console.error('创建会话失败:', e)
  }
}

// 选择历史会话
async function selectSession(session) {
  if (activeSessionId.value === session.id) return
  activeSessionId.value = session.id
  await loadMessages(session.id)
}

// 删除会话
function deleteSession(id) {
  const session = sessions.value.find(s => s.id === id)
  if (!session) return
  deleteTargetId.value = id
  deleteTargetTitle.value = session.title
  deleteDialogVisible.value = true
}

async function confirmDelete() {
  try {
    await axios.delete(`${API_BASE}/sessions/${deleteTargetId.value}`)
    sessions.value = sessions.value.filter(s => s.id !== deleteTargetId.value)

    // 如果删除的是当前活跃会话
    if (activeSessionId.value === deleteTargetId.value) {
      if (sessions.value.length > 0) {
        activeSessionId.value = sessions.value[0].id
        await loadMessages(sessions.value[0].id)
      } else {
        activeSessionId.value = null
        currentMessages.value = []
        // 如果没有会话了，自动创建一个
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
    // 聚焦到输入框
    const input = document.querySelector('.rename-input input')
    if (input) input.focus()
  })
}

async function confirmRename(session) {
  const newTitle = editTitle.value.trim()
  if (newTitle && newTitle !== session.title) {
    try {
      await axios.put(`${API_BASE}/sessions/${session.id}`, { title: newTitle })
      session.title = newTitle
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

// 加载某个会话的历史消息
async function loadMessages(sessionId) {
  currentMessages.value = []
  try {
    const res = await axios.get(`${API_BASE}/messages/${sessionId}`)
    if (res.data && res.data.length > 0) {
      currentMessages.value = res.data.map(m => ({
        role: m.role,
        content: m.content
      }))
    } else {
      // 无历史消息，显示问候语
      currentMessages.value = [{ role: 'assistant', content: DEFAULT_GREETING }]
      // 将该问候语也保存到数据库
      await axios.post(`${API_BASE}/messages`, {
        sessionId: sessionId,
        role: 'assistant',
        content: DEFAULT_GREETING
      })
    }
  } catch (e) {
    console.error('加载消息失败:', e)
    currentMessages.value = [{ role: 'assistant', content: DEFAULT_GREETING }]
  }
  scrollToBottom()
}

// 键盘事件：Enter 发送，Shift+Enter 换行
function handleKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

// 滚动到底部
function scrollToBottom() {
  nextTick(() => {
    if (messageArea.value) {
      messageArea.value.scrollTop = messageArea.value.scrollHeight
    }
  })
}

// 发送消息
async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || loading.value) return
  if (!activeSessionId.value) {
    // 无活跃会话时自动创建一个
    await autoCreateSession()
  }

  // 添加用户消息到界面
  currentMessages.value.push({ role: 'user', content: text })
  inputText.value = ''
  scrollToBottom()

  // 保存用户消息到后端
  const sid = activeSessionId.value
  axios.post(`${API_BASE}/messages`, {
    sessionId: sid,
    role: 'user',
    content: text
  }).catch(e => console.error('保存用户消息失败:', e))

  // 调用 AI
  loading.value = true
  try {
    const res = await axios.post(`${API_BASE}/chat`, { message: text })
    const reply = res.data.reply
    currentMessages.value.push({ role: 'assistant', content: reply })

    // 保存 AI 回复到后端
    axios.post(`${API_BASE}/messages`, {
      sessionId: sid,
      role: 'assistant',
      content: reply
    }).catch(e => console.error('保存AI回复失败:', e))
  } catch (e) {
    const fallback = '请求失败，请检查后端服务是否已启动。'
    currentMessages.value.push({ role: 'assistant', content: fallback })
    axios.post(`${API_BASE}/messages`, {
      sessionId: sid,
      role: 'assistant',
      content: fallback
    }).catch(() => {})
  } finally {
    loading.value = false
    scrollToBottom()
  }
}
</script>

<style scoped>
.chat-container {
  display: flex;
  height: calc(100vh - 60px);
}

/* 左侧栏 */
.sidebar {
  width: 260px;
  background: #f5f7fa;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.new-chat-btn {
  flex-shrink: 0;
}

.session-list {
  flex: 1;
  overflow-y: auto;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  cursor: pointer;
  color: #606266;
  font-size: 14px;
  border-bottom: 1px solid #ebeef5;
  transition: background 0.2s;
}

.session-item:hover {
  background: #ecf5ff;
}

.session-item.active {
  background: #d9ecff;
  color: #409eff;
  font-weight: 500;
}

.session-item .el-icon {
  flex-shrink: 0;
}

.session-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  user-select: none;
}

.rename-input {
  flex: 1;
}

.delete-session-btn {
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.2s;
}

.session-item:hover .delete-session-btn {
  opacity: 1;
}

.no-sessions {
  padding: 40px 16px;
  text-align: center;
  color: #909399;
  font-size: 13px;
}

/* 右侧聊天区 */
.main-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.chat-header {
  padding: 12px 20px;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
}

.chat-header h2 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

/* 消息区域 */
.message-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #fafafa;
}

.message-item {
  display: flex;
  margin-bottom: 20px;
  gap: 12px;
}

.user-msg {
  flex-direction: row-reverse;
}

.avatar {
  flex-shrink: 0;
}

.bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.user-msg .bubble {
  background: #409eff;
  color: #fff;
}

.role-label {
  font-size: 12px;
  margin-bottom: 4px;
  opacity: 0.7;
}

.content {
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 底部输入区 */
.input-area {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #e4e7ed;
  background: #fff;
  align-items: flex-end;
}

.input-area .el-textarea {
  flex: 1;
}

.send-btn {
  height: 40px;
}
</style>
