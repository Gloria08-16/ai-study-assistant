<template>
  <div class="knowledge-container">
    <div class="knowledge-header">
      <div>
        <h2>知识库</h2>
        <p class="header-subtitle">{{ knowledgeList.length }} 篇笔记</p>
      </div>
      <button class="btn-add" @click="openAddDialog">
        <el-icon :size="18"><Plus /></el-icon>
        <span>新增笔记</span>
      </button>
    </div>

    <!-- 标签筛选栏 -->
    <div v-if="allTags.length > 0" class="tag-filter-bar">
      <button
        :class="['tag-filter', { active: activeFilterTag === '' }]"
        @click="activeFilterTag = ''; currentPage = 1"
      >全部</button>
      <button
        v-for="tag in allTags"
        :key="tag"
        :class="['tag-filter', { active: activeFilterTag === tag }]"
        @click="activeFilterTag = tag; currentPage = 1"
      >{{ tag }}</button>
    </div>

    <!-- 卡片网格 -->
    <div v-if="filteredList.length > 0" class="card-grid">
      <article
        v-for="(item, idx) in pagedData"
        :key="item.id"
        class="knowledge-card"
        @click="openDetail(item)"
      >
        <div class="card-index">{{ rowIndex(idx) }}</div>
        <h3 class="card-title">{{ item.title }}</h3>
        <p class="card-content">{{ item.content }}</p>
        <div class="card-footer">
          <div class="card-tags" v-if="parseTags(item.tags).length > 0">
            <span
              v-for="tag in parseTags(item.tags)"
              :key="tag"
              class="card-tag"
              @click.stop="activeFilterTag = tag; currentPage = 1"
            >{{ tag }}</span>
          </div>
          <span class="card-time">{{ formatTime(item.createTime) }}</span>
          <button class="card-delete" @click.stop="deleteKnowledge(item.id)" title="删除">
            <el-icon :size="15"><Delete /></el-icon>
          </button>
        </div>
      </article>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <div class="empty-icon">📝</div>
      <h3>{{ activeFilterTag ? '没有匹配的笔记' : '知识库还是空的' }}</h3>
      <p>{{ activeFilterTag ? '试试其他标签筛选' : '点击上方按钮添加你的第一篇笔记' }}</p>
    </div>

    <!-- 分页 -->
    <div v-if="filteredList.length > pageSize" class="pagination-wrap">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="filteredList.length"
        layout="prev, pager, next"
        background
      />
    </div>

    <!-- 新增/详情对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '新增笔记' : detailItem?.title"
      width="560px"
      custom-class="glass-dialog"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <!-- 查看模式 -->
      <template v-if="dialogMode === 'view'">
        <div v-if="parseTags(detailItem?.tags).length > 0" class="detail-tags">
          <span v-for="tag in parseTags(detailItem?.tags)" :key="tag" class="card-tag">{{ tag }}</span>
        </div>
        <div class="detail-content">{{ detailItem?.content }}</div>
        <div class="detail-meta">
          <span>创建于 {{ formatTime(detailItem?.createTime) }}</span>
          <button class="btn-cancel" @click="dialogVisible = false">关闭</button>
        </div>
      </template>

      <!-- 新增模式 -->
      <template v-else>
        <div class="form-group">
          <label class="form-label">标题</label>
          <input v-model="form.title" class="form-input" placeholder="输入笔记标题" />
        </div>
        <div class="form-group">
          <label class="form-label">标签</label>
          <el-select
            v-model="formTags"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="输入标签后回车添加"
            class="tag-select"
            :reserve-keyword="false"
          >
            <el-option
              v-for="tag in allTags"
              :key="tag"
              :label="tag"
              :value="tag"
            />
          </el-select>
        </div>
        <div class="form-group">
          <label class="form-label">内容</label>
          <textarea v-model="form.content" class="form-textarea" rows="10" placeholder="输入笔记内容"></textarea>
        </div>
        <div class="form-actions">
          <button class="btn-cancel" @click="dialogVisible = false">取消</button>
          <button class="btn-save" @click="submitKnowledge" :disabled="submitting">
            <template v-if="submitting">保存中...</template>
            <template v-else>保存笔记</template>
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const API_BASE = 'http://localhost:8080/api/knowledge'

const knowledgeList = ref([])
const loadingTable = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref('add')
const detailItem = ref(null)
const submitting = ref(false)
const activeFilterTag = ref('')

const form = ref({
  title: '',
  content: '',
  userId: 1
})
const formTags = ref([])

const currentPage = ref(1)
const pageSize = ref(6)

// 所有标签（从数据中提取）
const allTags = computed(() => {
  const set = new Set()
  knowledgeList.value.forEach(item => {
    parseTags(item.tags).forEach(t => set.add(t))
  })
  return [...set].sort()
})

// 按标签筛选
const filteredList = computed(() => {
  if (!activeFilterTag.value) return knowledgeList.value
  return knowledgeList.value.filter(item => {
    const tags = parseTags(item.tags)
    return tags.includes(activeFilterTag.value)
  })
})

const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

function rowIndex(index) {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

function parseTags(tags) {
  if (!tags) return []
  if (Array.isArray(tags)) return tags
  return tags.split(',').map(t => t.trim()).filter(Boolean)
}

function formatTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 19)
}

async function loadList() {
  loadingTable.value = true
  try {
    const url = activeFilterTag.value
      ? `${API_BASE}/list?tag=${encodeURIComponent(activeFilterTag.value)}`
      : `${API_BASE}/list`
    const res = await axios.get(url)
    knowledgeList.value = res.data || []
  } catch (e) {
    knowledgeList.value = []
  } finally {
    loadingTable.value = false
  }
}

function openAddDialog() {
  dialogMode.value = 'add'
  form.value = { title: '', content: '', userId: 1 }
  formTags.value = []
  dialogVisible.value = true
}

function openDetail(item) {
  dialogMode.value = 'view'
  detailItem.value = item
  dialogVisible.value = true
}

function resetForm() {
  dialogMode.value = 'add'
  detailItem.value = null
}

async function submitKnowledge() {
  if (!form.value.title.trim() || !form.value.content.trim()) {
    ElMessage.warning('标题和内容不能为空')
    return
  }
  submitting.value = true
  try {
    const payload = { ...form.value, tags: formTags.value.join(',') }
    await axios.post(`${API_BASE}/add`, payload)
    ElMessage.success('笔记添加成功')
    dialogVisible.value = false
    currentPage.value = 1
    loadList()
  } catch (e) {
    ElMessage.error('添加失败，请检查后端服务')
  } finally {
    submitting.value = false
  }
}

async function deleteKnowledge(id) {
  try {
    await axios.delete(`${API_BASE}/delete/${id}`)
    ElMessage.success('删除成功')
    if (pagedData.value.length === 1 && currentPage.value > 1) {
      currentPage.value--
    }
    loadList()
  } catch (e) {
    ElMessage.error('删除失败，请检查后端服务')
  }
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
/* ========== 容器 ========== */
.knowledge-container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 32px 28px;
  height: 100%;
  overflow-y: auto;
}

/* ========== 头部 ========== */
.knowledge-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.knowledge-header h2 {
  margin: 0;
  font-size: 26px;
  font-weight: 650;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.5px;
}

.header-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #9ca3af;
  font-weight: 450;
}

.btn-add {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 11px 22px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: #fff;
  border: none;
  border-radius: 14px;
  font-size: 14px;
  font-weight: 550;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.3s ease;
  letter-spacing: -0.2px;
}

.btn-add:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 22px rgba(79, 172, 254, 0.4);
}

/* ========== 标签筛选栏 ========== */
.tag-filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.tag-filter {
  padding: 6px 16px;
  border: 1.5px solid #e5e7eb;
  border-radius: 20px;
  background: #fff;
  color: #6b7280;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.tag-filter:hover {
  border-color: #667eea;
  color: #667eea;
}

.tag-filter.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-color: transparent;
}

/* ========== 卡片网格 ========== */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.knowledge-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  border: none;
  box-shadow: 0 2px 12px rgba(0,0,0,0.03), 0 0 0 1px rgba(0,0,0,0.03);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1.2);
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 180px;
}

.knowledge-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(142, 197, 252, 0.2), 0 0 0 1px rgba(142, 197, 252, 0.12);
}

.card-index {
  position: absolute;
  top: 16px;
  right: 20px;
  font-size: 11px;
  font-weight: 600;
  color: #d1d5db;
}

.card-title {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 600;
  color: #1d1d1f;
  letter-spacing: -0.3px;
  line-height: 1.4;
  padding-right: 30px;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-content {
  flex: 1;
  margin: 0 0 16px;
  font-size: 13.5px;
  line-height: 1.65;
  color: #6b7280;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}

.card-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  border-top: 1px solid #f3f4f6;
  padding-top: 14px;
  flex-wrap: wrap;
}

.card-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  flex: 1;
}

.card-tag {
  display: inline-block;
  padding: 3px 10px;
  font-size: 11px;
  font-weight: 550;
  border-radius: 6px;
  background: rgba(142, 197, 252, 0.18);
  color: #4a7bbf;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
}

.card-tag:hover {
  background: rgba(142, 197, 252, 0.35);
}

.card-time {
  font-size: 11px;
  color: #4a7bbf;
  font-weight: 500;
  background: rgba(142, 197, 252, 0.18);
  padding: 4px 10px;
  border-radius: 6px;
  white-space: nowrap;
}

.card-delete {
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #d1d5db;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
  flex-shrink: 0;
  margin-left: auto;
}

.card-delete:hover {
  background: #fee2e2;
  color: #ef4444;
}

/* ========== 空状态 ========== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 6px;
}

.empty-state p {
  font-size: 14px;
  color: #9ca3af;
  margin: 0;
}

/* ========== 分页 ========== */
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}

/* ========== 表单 ========== */
.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.form-input,
.form-textarea {
  width: 100%;
  border: 1.5px solid #e5e7eb;
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 14px;
  font-family: inherit;
  color: #1d1d1f;
  background: #f9fafb;
  outline: none;
  transition: all 0.2s ease;
  box-sizing: border-box;
  resize: vertical;
  line-height: 1.6;
}

.form-input:focus,
.form-textarea:focus {
  border-color: #4facfe;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(79, 172, 254, 0.08);
}

.form-input::placeholder,
.form-textarea::placeholder {
  color: #c4c4c4;
}

.tag-select {
  width: 100%;
}

/* 穿透样式覆盖 el-select */
.tag-select :deep(.el-select__tags) {
  max-height: none;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
}

.btn-cancel {
  padding: 10px 22px;
  background: #f3f4f6;
  color: #4b5563;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
  font-family: inherit;
}

.btn-cancel:hover { background: #e5e7eb; }

.btn-save {
  padding: 10px 24px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: #fff;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 550;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: inherit;
}

.btn-save:hover:not(:disabled) {
  box-shadow: 0 4px 18px rgba(79, 172, 254, 0.45);
  transform: translateY(-1px);
}

.btn-save:disabled { opacity: 0.5; cursor: not-allowed; }

/* ========== 详情弹窗 ========== */
.detail-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.detail-content {
  font-size: 15px;
  line-height: 1.75;
  color: #1d1d1f;
  white-space: pre-wrap;
  word-break: break-word;
  padding: 4px 0;
}

.detail-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f3f4f6;
  font-size: 13px;
  color: #c4c4c4;
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .card-grid { grid-template-columns: 1fr; }
  .knowledge-container { padding: 20px 16px; }
}
</style>
