<template>
  <div id="app">
    <!-- 顶部导航栏 -->
    <nav class="top-nav">
      <router-link to="/" class="nav-brand">
        <span class="brand-icon">✦</span>
        <span class="brand-text">智学</span>
      </router-link>
      <div class="nav-links">
        <router-link to="/chat" class="nav-link" active-class="nav-link--active">
          <el-icon :size="16"><ChatDotRound /></el-icon>
          <span>AI伴学</span>
        </router-link>
        <router-link to="/knowledge" class="nav-link" active-class="nav-link--active">
          <el-icon :size="16"><FolderOpened /></el-icon>
          <span>知识库</span>
        </router-link>
      </div>
    </nav>

    <!-- 路由出口 -->
    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<style>
/* ========== 全局重置 ========== */
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
  background: #f7f9fb;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #1d1d1f;
}

#app {
  display: flex;
  flex-direction: column;
}

/* ========== 路由过渡动画 ========== */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ========== 顶部导航 ========== */
.top-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 28px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-bottom: none;
  box-shadow: 0 1px 0 rgba(0,0,0,0.04);
  flex-shrink: 0;
  z-index: 100;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: #1d1d1f;
}

.brand-icon {
  font-size: 20px;
  color: #2c2c2c;
}

.brand-text {
  font-size: 17px;
  font-weight: 600;
  letter-spacing: -0.3px;
}

.nav-links {
  display: flex;
  gap: 4px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 10px;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  transition: all 0.2s ease;
}

.nav-link:hover {
  background: rgba(0, 0, 0, 0.04);
  color: #1d1d1f;
}

.nav-link--active {
  background: #1d1d1f;
  color: #fff;
}

.nav-link--active:hover {
  background: #2c2c2c;
  color: #fff;
}

/* ========== 主体区域 ========== */
.main-content {
  flex: 1;
  overflow: hidden;
}
</style>
