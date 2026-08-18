<template>
  <div class="login-container">
    <el-card class="auth-card" shadow="always">
      <div class="brand-header">
        <h2>⚡ 秒杀商城系统</h2>
        <p class="sub-title">欢迎使用高并发秒杀交易平台</p>
      </div>

      <!-- 切换 登录 / 注册 页签 -->
      <el-tabs v-model="activeTab" class="auth-tabs" stretch>
        <!-- 1. 登录表单 -->
        <el-tab-pane label="账号登录" name="login">
          <el-form :model="loginForm" class="auth-form" size="large">
            <el-form-item>
              <el-input v-model="loginForm.username" placeholder="请输入用户名">
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item>
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                show-password
                @keyup.enter="handleLogin"
              >
                <template #prefix><el-icon><Lock /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-button type="primary" class="submit-btn" :loading="loading" @click="handleLogin">
              立 即 登 录
            </el-button>
          </el-form>
        </el-tab-pane>

        <!-- 2. 注册表单 -->
        <el-tab-pane label="新用户注册" name="register">
          <el-form :model="regForm" class="auth-form" size="large">
            <el-form-item>
              <el-input v-model="regForm.username" placeholder="设置用户名">
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item>
              <el-input
                v-model="regForm.password"
                type="password"
                placeholder="设置密码"
                show-password
              >
                <template #prefix><el-icon><Lock /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item>
              <el-input v-model="regForm.phone" placeholder="绑定手机号">
                <template #prefix><el-icon><Iphone /></el-icon></template>
              </el-input>
            </el-form-item>

            <!-- 我要售卖（切换为商家角色） -->
            <el-form-item>
              <el-checkbox v-model="regForm.isMerchant" border class="merchant-checkbox">
                🏪 我要售卖（注册为商家账号）
              </el-checkbox>
            </el-form-item>

            <el-button type="success" class="submit-btn" :loading="loading" @click="handleRegister">
              立 即 注 册
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="back-home">
        <el-link type="info" @click="goHome">← 返回商品列表页</el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const route = useRoute()

const activeTab = ref('login')
const loading = ref(false)

onMounted(() => {
  if (route.query.type === 'register') {
    activeTab.value = 'register'
  }
})

// 登录表单
const loginForm = reactive({
  username: '',
  password: ''
})

// 注册表单
const regForm = reactive({
  username: '',
  password: '',
  phone: '',
  isMerchant: false
})

// 统一登录提交
const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  loading.value = true
  try {
    const res = await axios.post('/api/user/login', {
      username: loginForm.username,
      password: loginForm.password
    })

    if (res.data.code === 200) {
      const userInfo = res.data.data
      localStorage.setItem('token', userInfo.token)
      localStorage.setItem('userInfo', JSON.stringify(userInfo))

      ElMessage.success(`登录成功，欢迎登录！`)
      router.push('/') // 登录成功跳转首页
    } else {
      ElMessage.error(res.data.msg || '用户名或密码错误')
    }
  } catch (e) {
    ElMessage.error('无法连接后端服务')
  } finally {
    loading.value = false
  }
}

// 统一注册提交
const handleRegister = async () => {
  if (!regForm.username || !regForm.password) {
    ElMessage.warning('请补全注册信息')
    return
  }

  loading.value = true
  try {
    const res = await axios.post('/api/user/register', {
      username: regForm.username,
      password: regForm.password,
      phone: regForm.phone,
      role: regForm.isMerchant ? 1 : 0 // 1: 商家, 0: 普通用户
    })

    if (res.data.code === 200) {
      ElMessage.success(res.data.msg)
      activeTab.value = 'login' // 注册成功后自动切到登录页
      loginForm.username = regForm.username
    } else {
      ElMessage.error(res.data.msg || '注册失败')
    }
  } catch (e) {
    ElMessage.error('注册服务请求异常')
  } finally {
    loading.value = false
  }
}

const goHome = () => {
  router.push('/')
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.auth-card {
  width: 420px;
  border-radius: 12px;
  padding: 10px 20px;
}

.brand-header {
  text-align: center;
  margin-bottom: 20px;
}

.brand-header h2 {
  color: #303133;
  margin-bottom: 6px;
}

.sub-title {
  color: #909399;
  font-size: 13px;
  margin: 0;
}

.auth-tabs {
  margin-top: 10px;
}

.auth-form {
  margin-top: 20px;
}

.merchant-checkbox {
  width: 100%;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: bold;
  margin-top: 10px;
}

.back-home {
  text-align: center;
  margin-top: 20px;
}
</style>