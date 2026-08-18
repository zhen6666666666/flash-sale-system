<template>
  <div class="app-container">
    <header v-if="route.path !== '/login'" class="header">
      <div class="logo" @click="router.push('/')">⚡ 秒杀商城系统</div>

      <!-- 右上角个人信息与下拉菜单 -->
      <div class="user-area">
        <!-- 1. 已登录：点击显示下拉菜单 -->
        <el-dropdown v-if="isLoggedIn" trigger="click" @command="handleCommand">
          <div class="avatar-wrapper is-online">
            <el-avatar
              :size="36"
              :src="getAvatarUrl(userInfo?.avatar)"
              class="user-avatar"
            />
            <span class="user-name-text">{{ username }}</span>
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </div>

          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon> 个人主页
              </el-dropdown-item>
              <el-dropdown-item command="logout" divided>
                <el-icon><SwitchButton /></el-icon> 退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <!-- 2. 未登录：点击头像或文字跳转登录页 -->
        <div v-else class="avatar-wrapper" @click="router.push('/login')">
          <el-avatar
            :size="36"
            :src="getAvatarUrl(null)"
            class="user-avatar"
          />
          <span class="user-name-text">未登录（点击登录/注册）</span>
        </div>
      </div>
    </header>

    <!-- 主体内容渲染出口 -->
    <main class="main-content" :class="{ 'full-screen': route.path === '/login' }">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, SwitchButton, ArrowDown } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const userInfo = ref(null)

// 智能处理头像图片 URL 格式
const getAvatarUrl = (url) => {
  if (!url || url.trim() === '') {
    return 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
  }
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  // 如果路径以 /uploads/ 开头，补全前端代理所需的 /api 前缀
  if (url.startsWith('/uploads/')) {
    return '/api' + url
  }
  return url
}

// 校验并从本地缓存提取用户信息
const checkUserInfo = () => {
  const savedUser = localStorage.getItem('userInfo')
  if (savedUser) {
    try {
      userInfo.value = JSON.parse(savedUser)
    } catch (e) {
      localStorage.removeItem('userInfo')
      userInfo.value = null
    }
  } else {
    userInfo.value = null
  }
}

const handleUserInfoUpdated = () => {
  checkUserInfo()
}

onMounted(() => {
  checkUserInfo()
  window.addEventListener('storage', handleUserInfoUpdated)
  window.addEventListener('user-info-updated', handleUserInfoUpdated)
})

onUnmounted(() => {
  window.removeEventListener('storage', handleUserInfoUpdated)
  window.removeEventListener('user-info-updated', handleUserInfoUpdated)
})

// 监听路由变化，实时更新头部登录状态
watch(() => route.path, () => {
  checkUserInfo()
})

const isLoggedIn = computed(() => {
  return !!userInfo.value && !!localStorage.getItem('token')
})

const username = computed(() => {
  return userInfo.value?.username || '用户'
})

// 处理下拉菜单点击指令
const handleCommand = (command) => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    userInfo.value = null
    ElMessage.success('已安全退出登录')
    router.push('/login')
  }
}
</script>

<style>
html, body {
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  background-color: #f5f7fa;
}

.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  height: 60px;
  background-color: #ffffff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  z-index: 100;
}

.logo {
  font-size: 20px;
  font-weight: bold;
  color: #f56c6c;
  cursor: pointer;
}

.user-area {
  display: flex;
  align-items: center;
}

.avatar-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 20px;
  transition: background-color 0.2s;
  user-select: none;
}

.avatar-wrapper:hover {
  background-color: #f0f2f5;
}

.user-avatar {
  border: 2px solid #dcdfe6;
  object-fit: cover;
}

.avatar-wrapper.is-online .user-avatar {
  border: 2px solid #67c23a;
}

.user-name-text {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.main-content {
  flex: 1;
}

.main-content.full-screen {
  padding: 0;
}
</style>