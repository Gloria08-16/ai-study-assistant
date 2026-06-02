import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useChatStore = defineStore('chat', () => {
  // -------- 状态 --------
  const sessions = ref([])
  const activeSessionId = ref(null)
  // 缓存每个会话的消息: { [sessionId]: [{ role, content }, ...] }
  const messagesMap = ref({})

  // -------- 计算属性 --------
  const currentMessages = computed(() => {
    if (!activeSessionId.value) return []
    return messagesMap.value[activeSessionId.value] || []
  })

  // -------- 会话操作 --------
  function setSessions(list) {
    sessions.value = list
  }

  function addSession(session) {
    sessions.value.unshift(session)
  }

  function removeSession(id) {
    sessions.value = sessions.value.filter(s => s.id !== id)
    delete messagesMap.value[id]
  }

  function setActiveSession(id) {
    activeSessionId.value = id
  }

  function updateSessionTitle(id, title) {
    const s = sessions.value.find(s => s.id === id)
    if (s) s.title = title
  }

  // -------- 消息操作 --------
  function setMessages(sessionId, msgs) {
    messagesMap.value[sessionId] = msgs
  }

  function addMessage(sessionId, msg) {
    if (!messagesMap.value[sessionId]) {
      messagesMap.value[sessionId] = []
    }
    messagesMap.value[sessionId].push(msg)
  }

  function appendToLastAssistant(sessionId, chunk) {
    const msgs = messagesMap.value[sessionId]
    if (!msgs) return
    for (let i = msgs.length - 1; i >= 0; i--) {
      if (msgs[i].role === 'assistant') {
        msgs[i].content += chunk
        return
      }
    }
  }

  // -------- 持久化配置 --------
  return {
    sessions,
    activeSessionId,
    messagesMap,
    currentMessages,
    setSessions,
    addSession,
    removeSession,
    setActiveSession,
    updateSessionTitle,
    setMessages,
    addMessage,
    appendToLastAssistant
  }
}, {
  persist: {
    key: 'ai-study-chat',
    storage: localStorage,
    paths: ['sessions', 'activeSessionId', 'messagesMap']
  }
})
