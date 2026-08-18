<template>
  <div class="profile-container">
    <div class="back-bar">
      <el-button :icon="Back" round class="back-btn" @click="goBack">
        返回上一页
      </el-button>
    </div>

    <el-card class="profile-card">
      <!-- 头部个人简讯卡片 -->
      <div class="user-header">
        <el-avatar
          :size="64"
          :src="getAvatarUrl(userInfo?.avatar)"
        />
        <div class="user-info-text">
          <div class="name-box">
            <h2>{{ userInfo?.username }}</h2>
            <el-tag :type="roleTagType" size="small">{{ roleName }}</el-tag>
          </div>
          <p class="phone-text">绑定手机：{{ userInfo?.phone || '暂未绑定' }}</p>
        </div>
      </div>

      <!-- 功能选项卡 -->
      <el-tabs v-model="activeTab" class="profile-tabs" @tab-click="handleTabClick">
        <!-- Tab 1: 我的订单 -->
        <el-tab-pane label="📦 我的订单" name="orders">
          <el-table :data="orderList" stripe style="width: 100%" v-loading="loadingOrders">
            <el-table-column prop="id" label="订单号" width="120" />
            <el-table-column prop="goodsName" label="商品名称" min-width="180" />
            <el-table-column prop="orderPrice" label="支付金额" width="120">
              <template #default="scope">
                <span class="price-text">￥{{ scope.row.orderPrice }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="下单时间" width="180" />
            <el-table-column prop="status" label="订单状态" width="120">
              <template #default="scope">
                <el-tag v-if="scope.row.status === 0" type="warning">待支付</el-tag>
                <el-tag v-else-if="scope.row.status === 1" type="success">已支付</el-tag>
                <el-tag v-else type="info">已取消</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="scope">
                <div v-if="scope.row.status === 0" class="order-btn-group">
                  <el-button
                    type="primary"
                    size="small"
                    @click="handlePayOrder(scope.row)"
                  >
                    去支付
                  </el-button>
                  <el-button
                    type="danger"
                    link
                    size="small"
                    @click="handleCancelOrder(scope.row)"
                  >
                    取消订单
                  </el-button>
                </div>
                <span v-else-if="scope.row.status === 1" class="text-gray">已完成</span>
                <span v-else class="text-gray">已失效</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab 2: 我的售卖  -->
        <el-tab-pane v-if="isMerchant" label="🏪 我的售卖" name="merchant">
          <div class="action-bar">
            <el-button type="primary" @click="openAddGoodsDialog">➕ 上架秒杀商品</el-button>
          </div>

          <el-table :data="merchantGoodsList" stripe style="width: 100%" v-loading="loadingGoods">
            <el-table-column prop="goodsName" label="商品名称" min-width="180" />
            <el-table-column prop="goodsPrice" label="原价(元)" width="100" />
            <el-table-column prop="seckillPrice" label="秒杀价(元)" width="110">
              <template #default="scope">
                <span class="price-text">￥{{ scope.row.seckillPrice }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="stockCount" label="剩余库存" width="90" />
            <el-table-column label="活动状态" width="110">
              <template #default="scope">
                <el-tag v-if="getGoodsStatus(scope.row) === 0" type="primary">即将开始</el-tag>
                <el-tag v-else-if="getGoodsStatus(scope.row) === 1" type="danger">秒杀中</el-tag>
                <el-tag v-else type="info">已结束</el-tag>
              </template>
            </el-table-column>

            <el-table-column label="开始时间" width="170">
              <template #default="scope">
                {{ scope.row.startDate ? new Date(scope.row.startDate).toLocaleString() : (scope.row.startTime || '-') }}
              </template>
            </el-table-column>

            <el-table-column label="结束时间" width="170">
              <template #default="scope">
                {{ scope.row.endDate ? new Date(scope.row.endDate).toLocaleString() : (scope.row.endTime || '-') }}
              </template>
            </el-table-column>

            <!-- 操作列：编辑与下架 -->
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="scope">
                <!-- 1. 编辑按钮（正在秒杀和秒杀结束的不可以编辑） -->
                <el-tooltip
                  :content="getGoodsStatus(scope.row) === 1 ? '正在秒杀中，无法编辑' : '活动已结束，无法编辑'"
                  :disabled="getGoodsStatus(scope.row) === 0"
                  placement="top"
                >
                  <span>
                    <el-button
                      type="primary"
                      link
                      size="small"
                      :disabled="getGoodsStatus(scope.row) !== 0"
                      @click="openEditGoodsDialog(scope.row)"
                    >
                      编辑
                    </el-button>
                  </span>
                </el-tooltip>

                <!-- 2. 下架按钮（正在秒杀的不能下架） -->
                <el-tooltip
                  content="正在秒杀中，无法下架"
                  :disabled="getGoodsStatus(scope.row) !== 1"
                  placement="top"
                >
                  <span>
                    <el-button
                      type="danger"
                      link
                      size="small"
                      :disabled="getGoodsStatus(scope.row) === 1"
                      @click="handleDeleteGoods(scope.row)"
                    >
                      下架
                    </el-button>
                  </span>
                </el-tooltip>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab 3: 我的收藏 -->
        <el-tab-pane label="⭐ 我的收藏" name="favorites">
          <el-table :data="favoriteGoodsList" stripe style="width: 100%" v-loading="loadingFavorites">
            <el-table-column label="商品图片" width="110">
              <template #default="scope">
                <img :src="scope.row.goodsImg" style="width: 50px; height: 50px; object-fit: contain; border-radius: 6px;" />
              </template>
            </el-table-column>
            <el-table-column prop="goodsName" label="商品名称" min-width="180" />
            <el-table-column prop="seckillPrice" label="秒杀价(元)" width="120">
              <template #default="scope">
                <span class="price-text">￥{{ scope.row.seckillPrice }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="stockCount" label="剩余库存" width="100" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <el-button type="danger" link size="small" @click="handleRemoveFavorite(scope.row.id)">
                  取消收藏
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab 4: 修改资料 -->
        <el-tab-pane label="⚙️ 修改资料" name="profile">
          <el-form :model="editForm" label-width="100px" class="edit-form">
            <el-form-item label="用户头像">
              <el-upload
                class="avatar-uploader"
                action="/api/user/uploadAvatar"
                :show-file-list="false"
                :on-success="handleAvatarSuccess"
                :before-upload="beforeAvatarUpload"
              >
                <img v-if="editForm.avatar" :src="getAvatarUrl(editForm.avatar)" class="avatar-preview" />
                <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
              </el-upload>
              <div class="avatar-tips">点击上方框选择并更换新头像</div>
            </el-form-item>

            <el-form-item label="用户名">
              <el-input v-model="editForm.username" placeholder="请输入新用户名" />
            </el-form-item>

            <el-form-item label="手机号">
              <el-input v-model="editForm.phone" placeholder="请输入手机号" />
            </el-form-item>

            <el-form-item label="原密码">
              <el-input v-model="editForm.oldPassword" type="password" placeholder="修改密码须验证原密码" show-password />
            </el-form-item>

            <el-form-item label="新密码">
              <el-input v-model="editForm.newPassword" type="password" placeholder="不修改请留空" show-password />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="handleUpdateProfile">
                保存修改
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 发布/编辑秒杀商品弹窗 -->
    <el-dialog v-model="addGoodsVisible" :title="isEditMode ? '✏️ 编辑秒杀商品' : '🔥 发布秒杀商品'" width="620px" destroy-on-close>
      <el-form :model="newGoods" label-width="100px">
        <el-form-item label="商品名称" required>
          <el-input v-model="newGoods.goodsName" placeholder="例如：iPhone 15 Pro 256GB 暗夜黑" />
        </el-form-item>

        <el-form-item label="商品图片" required>
          <div class="goods-img-input-box">
            <el-input v-model="newGoods.goodsImg" placeholder="请输入图片网络链接 URL 或点击右侧上传" clearable style="flex: 1;" />
            <el-upload
              class="goods-img-uploader"
              action="/api/goods/uploadImg"
              :show-file-list="false"
              :on-success="handleGoodsImgSuccess"
              :before-upload="beforeAvatarUpload"
            >
              <el-button type="primary" plain style="margin-left: 10px;">本地上传</el-button>
            </el-upload>
          </div>
          <div v-if="newGoods.goodsImg" class="goods-img-preview-container">
            <img :src="getAvatarUrl(newGoods.goodsImg)" class="goods-img-preview" alt="预览图" />
          </div>
        </el-form-item>

        <el-form-item label="商品详情">
          <el-input
            v-model="newGoods.goodsDetail"
            type="textarea"
            :rows="4"
            placeholder="请输入商品详细描述..."
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商品原价" required>
              <el-input-number v-model="newGoods.goodsPrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="秒杀价格" required>
              <el-input-number v-model="newGoods.seckillPrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="库存数量" required>
              <el-input-number v-model="newGoods.stockCount" :min="1" :precision="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="限购数量" required>
              <el-input-number v-model="newGoods.limitCount" :min="1" :precision="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="活动时间" required>
          <el-date-picker
            v-model="newGoods.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="addGoodsVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="handleSaveGoods">提交保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Back } from '@element-plus/icons-vue'
import axios from 'axios'
import { useRouter } from 'vue-router'

const router = useRouter()
const activeTab = ref('orders')
const userInfo = ref(null)

const loadingOrders = ref(false)
const loadingGoods = ref(false)
const loadingFavorites = ref(false)
const submitting = ref(false)
const publishing = ref(false)

const orderList = ref([])
const merchantGoodsList = ref([])
const favoriteGoodsList = ref([])

// 编辑/发布模式状态控制
const isEditMode = ref(false)
const editingGoodsId = ref(null)

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

// 计算商品当前的秒杀状态 (0:未开始, 1:秒杀中, 2:已结束)
const getGoodsStatus = (row) => {
  const now = Date.now()
  const startStr = row.startDate || row.startTime
  const endStr = row.endDate || row.endTime
  const start = startStr ? new Date(startStr).getTime() : 0
  const end = endStr ? new Date(endStr).getTime() : 0

  if (now < start) return 0 // 未开始
  if (now >= start && now <= end) return 1 // 正在秒杀
  return 2 // 已结束
}

const getAvatarUrl = (url) => {
  if (!url || url.trim() === '') {
    return 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
  }
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  if (url.startsWith('/uploads/')) {
    return '/api' + url
  }
  return url
}

const editForm = reactive({
  username: '',
  phone: '',
  avatar: '',
  oldPassword: '',
  newPassword: ''
})

const addGoodsVisible = ref(false)
const newGoods = reactive({
  goodsName: '',
  goodsImg: '',
  goodsDetail: '',
  goodsPrice: 4999,
  seckillPrice: 2999,
  stockCount: 100,
  limitCount: 1,
  timeRange: []
})

const handleAvatarSuccess = (response) => {
  if (response.code === 200) {
    editForm.avatar = response.data
    ElMessage.success('头像图片上传成功')
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

const beforeAvatarUpload = (rawFile) => {
  const isImage = rawFile.type.startsWith('image/')
  const isLt2M = rawFile.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件！')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('头像图片大小不能超过 5MB！')
    return false
  }
  return true
}

const handleGoodsImgSuccess = (response) => {
  if (response.code === 200) {
    newGoods.goodsImg = response.data
    ElMessage.success('商品图片上传成功')
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

onMounted(() => {
  const savedUser = localStorage.getItem('userInfo')
  if (!savedUser) {
    ElMessage.warning('请先登录账号')
    router.push('/login')
    return
  }
  userInfo.value = JSON.parse(savedUser)
  editForm.username = userInfo.value.username || ''
  editForm.phone = userInfo.value.phone || ''
  editForm.avatar = userInfo.value.avatar || ''

  fetchLatestUserInfo()
  fetchMyOrders()
})

const fetchLatestUserInfo = async () => {
  if (!userInfo.value?.userId) return
  try {
    const res = await axios.get('/api/user/info', {
      params: { userId: userInfo.value.userId }
    })
    if (res.data && res.data.code === 200 && res.data.data) {
      const latest = res.data.data
      userInfo.value.avatar = latest.avatar
      userInfo.value.username = latest.username
      userInfo.value.phone = latest.phone
      editForm.avatar = latest.avatar || ''
      localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    }
  } catch (e) {}
}

const isMerchant = computed(() => {
  return userInfo.value?.role === 1 || userInfo.value?.role === 2
})

const roleName = computed(() => {
  const role = userInfo.value?.role
  if (role === 1) return '🏪 认证商家'
  if (role === 2) return '👑 超级管理员'
  return '👤 普通买家'
})

const roleTagType = computed(() => {
  const role = userInfo.value?.role
  if (role === 1) return 'warning'
  if (role === 2) return 'danger'
  return 'info'
})

const handleTabClick = (tab) => {
  if (tab.paneName === 'orders') fetchMyOrders()
  if (tab.paneName === 'merchant' && isMerchant.value) fetchMerchantGoods()
  if (tab.paneName === 'favorites') fetchMyFavorites()
}

// 取消订单逻辑
const handleCancelOrder = (order) => {
  ElMessageBox.confirm(
    `确定要取消订单 #${order.id} 吗？取消后将释放商品库存。`,
    '⚠️ 取消订单提示',
    {
      confirmButtonText: '确定取消',
      cancelButtonText: '再想想',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await axios.post('/api/order/cancel', null, {
        params: { orderId: order.id }
      })
      if (res.data && res.data.code === 200) {
        ElMessage.success('订单已成功取消')
        fetchMyOrders() // 刷新订单列表
      } else {
        ElMessage.error(res.data.msg || '取消订单失败')
      }
    } catch (e) {
      ElMessage.error('网络请求异常，请稍后重试')
    }
  }).catch(() => {})
}

// 1. 获取我的订单
const fetchMyOrders = async () => {
  loadingOrders.value = true
  try {
    const res = await axios.get('/api/order/userList', {
      params: { userId: userInfo.value.userId }
    })
    if (res.data.code === 200) {
      orderList.value = res.data.data || []
    }
  } catch (e) {
    orderList.value = []
  } finally {
    loadingOrders.value = false
  }
}

// 2. 去支付
const handlePayOrder = async (order) => {
  try {
    const res = await axios.post('/api/order/pay', null, {
      params: { orderId: order.id }
    })

    if (res.data && res.data.code === 200) {
      ElMessage.success(`订单号 ${order.id} 支付成功！`)
      order.status = 1
      fetchMyOrders()
    } else {
      ElMessage.error(res.data.msg || '支付失败')
    }
  } catch (e) {
    ElMessage.error('支付接口调用异常')
  }
}

// 3. 获取商人售卖商品
const fetchMerchantGoods = async () => {
  loadingGoods.value = true
  try {
    const res = await axios.get('/api/goods/merchantList', {
      params: { merchantId: userInfo.value.userId }
    })
    if (res.data.code === 200) {
      merchantGoodsList.value = res.data.data || []
    }
  } catch (e) {
    merchantGoodsList.value = []
  } finally {
    loadingGoods.value = false
  }
}

// 4. 获取用户收藏列表
const fetchMyFavorites = async () => {
  if (!userInfo.value?.userId) return
  loadingFavorites.value = true
  try {
    const res = await axios.get('/api/favorite/list', {
      params: { userId: userInfo.value.userId }
    })
    if (res.data && res.data.code === 200) {
      favoriteGoodsList.value = res.data.data || []
    }
  } catch (e) {
    ElMessage.error('获取收藏列表失败')
  } finally {
    loadingFavorites.value = false
  }
}

// 5. 个人主页取消收藏功能
const handleRemoveFavorite = async (goodsId) => {
  try {
    const res = await axios.post('/api/favorite/toggle', null, {
      params: { userId: userInfo.value.userId, goodsId: goodsId }
    })
    if (res.data && res.data.code === 200) {
      ElMessage.success('已取消收藏')
      fetchMyFavorites()
    }
  } catch (e) {
    ElMessage.error('操作失败，请重试')
  }
}

// 6. 修改资料提交
const handleUpdateProfile = async () => {
  if (!editForm.username.trim()) {
    ElMessage.warning('用户名不能为空')
    return
  }

  submitting.value = true
  try {
    const res = await axios.post('/api/user/update', {
      id: userInfo.value.userId,
      username: editForm.username,
      phone: editForm.phone,
      avatar: editForm.avatar,
      oldPassword: editForm.oldPassword,
      newPassword: editForm.newPassword
    })

    if (res.data.code === 200) {
      ElMessage.success('资料修改成功！')
      userInfo.value.username = editForm.username
      userInfo.value.phone = editForm.phone
      userInfo.value.avatar = editForm.avatar
      localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
      window.dispatchEvent(new Event('user-info-updated'))
    } else {
      ElMessage.error(res.data.msg || '修改失败，请核对密码')
    }
  } catch (e) {
    ElMessage.error('服务异常，请重试')
  } finally {
    submitting.value = false
  }
}

// 打开发布商品弹窗
const openAddGoodsDialog = () => {
  isEditMode.value = false
  editingGoodsId.value = null
  newGoods.goodsName = ''
  newGoods.goodsImg = ''
  newGoods.goodsDetail = ''
  newGoods.goodsPrice = 4999
  newGoods.seckillPrice = 2999
  newGoods.stockCount = 100
  newGoods.limitCount = 1
  newGoods.timeRange = []
  addGoodsVisible.value = true
}

// 打开编辑商品弹窗
const openEditGoodsDialog = (row) => {
  isEditMode.value = true
  editingGoodsId.value = row.id
  newGoods.goodsName = row.goodsName || ''
  newGoods.goodsImg = row.goodsImg || ''
  newGoods.goodsDetail = row.goodsDetail || ''
  newGoods.goodsPrice = row.goodsPrice || 0
  newGoods.seckillPrice = row.seckillPrice || 0
  newGoods.stockCount = row.stockCount || 10
  newGoods.limitCount = row.limitCount || 1

  const start = row.startDate ? new Date(row.startDate).toLocaleString() : row.startTime
  const end = row.endDate ? new Date(row.endDate).toLocaleString() : row.endTime
  newGoods.timeRange = [start, end]

  addGoodsVisible.value = true
}

// 提交保存商品
const handleSaveGoods = async () => {
  if (!newGoods.goodsName.trim() || !newGoods.goodsImg.trim()) {
    ElMessage.warning('请填写完整的商品名称和图片链接')
    return
  }
  if (!newGoods.timeRange || newGoods.timeRange.length < 2) {
    ElMessage.warning('请选择秒杀开始时间与结束时间')
    return
  }

  publishing.value = true
  try {
    const url = isEditMode.value ? '/api/goods/update' : '/api/goods/publish'
    const payload = {
      merchantId: userInfo.value.userId,
      goodsId: editingGoodsId.value,
      goodsName: newGoods.goodsName,
      goodsImg: newGoods.goodsImg,
      goodsDetail: newGoods.goodsDetail,
      goodsPrice: newGoods.goodsPrice,
      seckillPrice: newGoods.seckillPrice,
      stockCount: newGoods.stockCount,
      limitCount: newGoods.limitCount,
      startTime: newGoods.timeRange[0],
      endTime: newGoods.timeRange[1]
    }

    const res = await axios.post(url, payload)

    if (res.data && res.data.code === 200) {
      ElMessage.success(isEditMode.value ? '秒杀商品修改成功！' : '秒杀商品发布成功！')
      addGoodsVisible.value = false
      fetchMerchantGoods()
    } else {
      ElMessage.error(res.data.msg || '操作失败')
    }
  } catch (e) {
    ElMessage.error('网络与后端通讯失败')
  } finally {
    publishing.value = false
  }
}

// 🌟 下架/删除秒杀商品
const handleDeleteGoods = (row) => {
  ElMessageBox.confirm(
    `确定要下架商品 “${row.goodsName}” 吗？下架后将无法重新进行秒杀。`,
    '⚠️ 下架确认',
    {
      confirmButtonText: '确认下架',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await axios.post('/api/goods/delete', null, {
        params: { id: row.id }
      })
      if (res.data && res.data.code === 200) {
        ElMessage.success('商品下架成功')
        fetchMerchantGoods()
      } else {
        ElMessage.error(res.data.msg || '下架失败')
      }
    } catch (e) {
      ElMessage.error('服务请求异常')
    }
  }).catch(() => {})
}
</script>

<style scoped>
.profile-container {
  max-width: 1000px;
  margin: 20px auto 30px auto;
  padding: 0 20px;
}

.back-bar {
  margin-bottom: 16px;
}

.back-btn {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  background-color: #ffffff;
  border-color: #e4e7ed;
  color: #606266;
  transition: all 0.2s ease;
}

.back-btn:hover {
  border-color: #409eff;
  color: #409eff;
  transform: translateX(-2px);
}

.profile-card {
  border-radius: 8px;
}

.user-header {
  display: flex;
  align-items: center;
  gap: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 20px;
}

.name-box {
  display: flex;
  align-items: center;
  gap: 12px;
}

.name-box h2 {
  margin: 0;
  color: #303133;
}

.phone-text {
  margin: 6px 0 0 0;
  color: #909399;
  font-size: 14px;
}

.profile-tabs {
  margin-top: 10px;
}

.price-text {
  color: #ff6700;
  font-weight: bold;
}

.text-gray {
  color: #909399;
  font-size: 13px;
}

.action-bar {
  margin-bottom: 15px;
}

.edit-form {
  max-width: 480px;
  margin-top: 20px;
}

.avatar-uploader :deep(.el-upload) {
  border: 1px dashed #dcdfe6;
  border-radius: 50%;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: border-color 0.2s;
  width: 90px;
  height: 90px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-uploader :deep(.el-upload:hover) {
  border-color: #409eff;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
}

.avatar-preview {
  width: 90px;
  height: 90px;
  object-fit: cover;
  border-radius: 50%;
}

.avatar-tips {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}

.goods-img-input-box {
  display: flex;
  align-items: center;
  width: 100%;
}

.goods-img-preview-container {
  margin-top: 8px;
}

.goods-img-preview {
  width: 60px;
  height: 60px;
  object-fit: contain;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background-color: #f7f8fa;
  padding: 2px;
}
</style>