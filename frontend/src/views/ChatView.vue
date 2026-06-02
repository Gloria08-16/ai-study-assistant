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
      <el-menu class="session-list" default-active="0">
        <el-menu-item v-for="(session, index) in sessions" :key="session.id" :index="String(index)"
          @click="selectSession(session)">
          <el-icon><ChatLineSquare /></el-icon>
          <span>{{ session.title }}</span>
        </el-menu-item>
      </el-menu>
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
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import axios from 'axios'

const API_BASE = 'http://localhost:8080/api'

// 模拟会话列表
const sessions = ref([
  { id: 1, title: '新对话' }
])

// 当前会话的消息
const currentMessages = ref([
  { role: 'assistant', content: '你好！我是AI智能伴学助手，有什么可以帮你的吗？' }
])

const inputText = ref('')
const loading = ref(false)
const messageArea = ref(null)

// 键盘事件：Enter 发送，Shift+Enter 换行
function handleKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

// 新建对话
function newConversation() {
  const newSession = { id: Date.now(), title: '新对话' }
  sessions.value.unshift(newSession)
  currentMessages.value = [
    { role: 'assistant', content: '你好！我是AI智能伴学助手，有什么可以帮你的吗？' }
  ]
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

  // 添加用户消息
  currentMessages.value.push({ role: 'user', content: text })
  inputText.value = ''
  scrollToBottom()

  loading.value = true
  try {
    const res = await axios.post(`${API_BASE}/chat`, { message: text })
    currentMessages.value.push({ role: 'assistant', content: res.data.reply })
  } catch (e) {
    currentMessages.value.push({ role: 'assistant', content: '请求失败，请检查后端服务是否已启动。' })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

// 选择历史会话（模拟）
function selectSession(session) {
  // 可后续扩展：从后端加载该会话的历史消息
  console.log('切换到会话:', session.title)
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
  border-right: none;
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
