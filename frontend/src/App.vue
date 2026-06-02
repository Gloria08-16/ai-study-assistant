<template>
  <div id="app">
    <!-- 顶部导航栏 -->
    <el-menu mode="horizontal" :default-active="activeMenu" router class="top-nav">
      <div class="nav-brand">
        <el-icon :size="22"><Reading /></el-icon>
        <span>AI智能伴学系统</span>
      </div>
      <div class="nav-items">
        <el-menu-item index="/chat">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI伴学</span>
        </el-menu-item>
        <el-menu-item index="/knowledge">
          <el-icon><FolderOpened /></el-icon>
          <span>知识库</span>
        </el-menu-item>
      </div>
    </el-menu>

    <!-- 路由出口 -->
    <router-view />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style>
/* 全局重置（仅影响应用壳，不破坏 Element Plus 组件） */
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
}

body {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', Arial, sans-serif;
}

#app {
  display: flex;
  flex-direction: column;
}

.top-nav {
  display: flex;
  align-items: center;
  padding: 0 20px;
  flex-shrink: 0;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: 40px;
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.nav-items {
  display: flex;
}
</style>
