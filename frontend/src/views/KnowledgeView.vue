<template>
  <div class="knowledge-container">
    <div class="knowledge-header">
      <h2>我的知识库</h2>
      <el-button type="primary" @click="dialogVisible = true">
        <el-icon><Plus /></el-icon>
        新增资料
      </el-button>
    </div>

    <!-- 资料列表 -->
    <el-table :data="pagedData" stripe style="width: 100%" v-loading="loadingTable">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" min-width="150" />
      <el-table-column label="内容" min-width="300">
        <template #default="{ row }">
          <span class="content-preview" @click="showContent(row)" style="cursor:pointer;">{{ row.content }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" size="small" @click="deleteKnowledge(row.id)" :icon="Delete">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="knowledgeList.length"
        layout="total, prev, pager, next"
        background
      />
    </div>

    <!-- 新增对话框 -->
    <el-dialog v-model="dialogVisible" title="新增资料" width="600px" :close-on-click-modal="false">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入资料标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="请输入资料内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitKnowledge" :loading="submitting">确定添加</el-button>
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
const submitting = ref(false)

const form = ref({
  title: '',
  content: '',
  userId: 1 // 模拟用户ID
})

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return knowledgeList.value.slice(start, start + pageSize.value)
})

// 加载列表
async function loadList() {
  loadingTable.value = true
  try {
    const res = await axios.get(`${API_BASE}/list`)
    knowledgeList.value = res.data
  } catch (e) {
    knowledgeList.value = []
  } finally {
    loadingTable.value = false
  }
}

// 提交新增
async function submitKnowledge() {
  if (!form.value.title.trim() || !form.value.content.trim()) {
    ElMessage.warning('标题和内容不能为空')
    return
  }
  submitting.value = true
  try {
    await axios.post(`${API_BASE}/add`, form.value)
    ElMessage.success('资料添加成功')
    dialogVisible.value = false
    form.value = { title: '', content: '', userId: 1 }
    loadList()
  } catch (e) {
    ElMessage.error('添加失败，请检查后端服务')
  } finally {
    submitting.value = false
  }
}

// 删除资料
async function deleteKnowledge(id) {
  try {
    await axios.delete(`${API_BASE}/delete/${id}`)
    ElMessage.success('删除成功')
    loadList()
  } catch (e) {
    ElMessage.error('删除失败，请检查后端服务')
  }
}

// 查看完整内容
function showContent(row) {
  ElMessage.info(`【${row.title}】\n${row.content}`)
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.knowledge-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.knowledge-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.knowledge-header h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.content-preview {
  display: block;
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
