<template>
  <el-dialog
    v-model="visible"
    :title="isLogin ? '用户登录' : '账号注册'"
    width="400px"
    center
    destroy-on-close
  >
    <el-form :model="form" label-width="80px" class="auth-form">
      <el-form-item label="用户名">
        <el-input v-model="form.username" placeholder="请输入用户名" />
      </el-form-item>

      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
      </el-form-item>

      <!-- 注册专属字段 -->
      <template v-if="!isLogin">
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>

        <!-- 我要售卖（切换为商家角色） -->
        <el-form-item label="开店意向">
          <el-checkbox v-model="form.isMerchant" border size="default">
            🏪 我要售卖（注册为商家账号）
          </el-checkbox>
        </el-form-item>
      </template>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" class="submit-btn" :loading="loading" @click="handleSubmit">
          {{ isLogin ? '立 即 登 录' : '立 即 注 册' }}
        </el-button>

        <div class="switch-link">
          <span v-if="isLogin">还没有账号？<a @click="toggleMode">立即注册</a></span>
          <span v-else>已有账号？<a @click="toggleMode">返回登录</a></span>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const visible = ref(false)
const isLogin = ref(true)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  phone: '',
  isMerchant: false
})

// 暴露打开弹窗的方法
const open = (mode = 'login') => {
  isLogin.value = mode === 'login'
  visible.value = true
}

const toggleMode = () => {
  isLogin.value = !isLogin.value
}

const handleSubmit = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请填写完整的账号和密码')
    return
  }

  loading.value = true
  try {
    if (isLogin.value) {
      // 登录逻辑
      const res = await axios.post('/api/user/login', {
        username: form.username,
        password: form.password
      })

      if (res.data.code === 200) {
        const userInfo = res.data.data
        // 保存登录凭证与角色信息 (0:用户, 1:商家, 2:管理员)
        localStorage.setItem('token', userInfo.token)
        localStorage.setItem('userInfo', JSON.stringify(userInfo))

        let roleName = '普通用户'
        if (userInfo.role === 1) roleName = '商家'
        if (userInfo.role === 2) roleName = '超级管理员'

        ElMessage.success(`登录成功，欢迎您，${roleName}：${userInfo.username}`)
        visible.value = false
        window.location.reload() // 刷新页面同步状态
      } else {
        ElMessage.error(res.data.msg || '登录失败')
      }
    } else {
      // 注册逻辑
      const res = await axios.post('/api/user/register', {
        username: form.username,
        password: form.password,
        phone: form.phone,
        role: form.isMerchant ? 1 : 0 // 勾选“我要售卖”为 1 (商家)，否则为 0 (普通用户)
      })

      if (res.data.code === 200) {
        ElMessage.success(res.data.msg)
        isLogin.value = true // 注册成功后自动切换为登录
      } else {
        ElMessage.error(res.data.msg || '注册失败')
      }
    }
  } catch (error) {
    ElMessage.error('网络请求失败，请检查服务器连接')
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.auth-form {
  margin-top: 10px;
}
.submit-btn {
  width: 100%;
  height: 40px;
  font-size: 16px;
}
.switch-link {
  margin-top: 15px;
  text-align: center;
  font-size: 14px;
  color: #606266;
}
.switch-link a {
  color: #409eff;
  cursor: pointer;
  text-decoration: underline;
}
</style>