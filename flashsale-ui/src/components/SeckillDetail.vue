<template>
  <div class="seckill-app">
    <!-- 顶部 Banner 标题 & 搜索框 -->
    <div class="page-header">
      <h2 class="page-title">🔥 热门秒杀专区</h2>
      <p class="page-subtitle">限时超低价 · 限量抢购 · 破价热销中</p>

      <!-- 核心：秒杀商品搜索框 -->
      <div class="search-bar-container">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索秒杀商品名称或描述..."
          clearable
          size="large"
          class="search-input"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <span class="search-icon">🔍</span>
          </template>
          <template #append>
            <el-button type="primary" class="search-btn" @click="handleSearch">
              搜索
            </el-button>
          </template>
        </el-input>
      </div>
    </div>

    <!-- 1. 秒杀商品列表 -->
    <el-row v-if="goodsList.length > 0" :gutter="24" class="goods-list">
      <el-col
        v-for="goods in goodsList"
        :key="goods.id"
        :xs="24" :sm="12" :md="8" :lg="6"
      >
        <div class="goods-card" @click="openDetailDialog(goods)">
          <!-- 动态状态标签角标 -->
          <div class="status-badge" :class="getStatusClass(goods.status)">
            {{ getStatusText(goods) }}
          </div>

          <!-- 商品图片展示区 -->
          <div class="img-wrapper">
            <img :src="goods.goodsImg" :alt="goods.goodsName" class="goods-img" />
          </div>

          <!-- 商品信息 -->
          <div class="card-info">
            <h3 class="card-title" :title="goods.goodsName">{{ goods.goodsName }}</h3>
            <p class="goods-desc" :title="goods.goodsTitle">{{ goods.goodsTitle }}</p>

            <div class="price-container">
              <div class="price-main">
                <span class="price-symbol">￥</span>
                <span class="seckill-price">{{ goods.seckillPrice.toFixed(2) }}</span>
              </div>
              <span class="original-price">￥{{ goods.goodsPrice.toFixed(2) }}</span>
            </div>

            <button class="detail-btn" @click.stop="openDetailDialog(goods)">
              进入详情页
            </button>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 搜索无结果时的空状态 -->
    <el-empty
      v-else
      description="未找到相关的秒杀商品"
      class="empty-box"
    >
      <el-button v-if="searchKeyword" type="primary" plain @click="clearSearch">
        重置搜索
      </el-button>
    </el-empty>

    <!-- 2. 精美商品详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      width="560px"
      align-center
      destroy-on-close
      class="custom-detail-dialog"
    >
      <div v-if="currentGoods" class="detail-body">
        <!-- 弹窗头部：商品简图与标题价格 + 🌟 收藏按钮 -->
        <div class="detail-header"
          <img :src="currentGoods.goodsImg" class="detail-thumb" />
          <div class="detail-header-info">
            <div class="detail-title-row">
              <h2 class="detail-name">{{ currentGoods.goodsName }}</h2>
              <el-button
                :type="isFavorite ? 'warning' : 'default'"
                :icon="isFavorite ? StarFilled : Star"
                circle
                size="large"
                class="fav-btn"
                title="收藏/取消收藏"
                @click="handleToggleFavorite"
              />
            </div>
            <p class="detail-sub-title">{{ currentGoods.goodsTitle }}</p>

            <div class="detail-price-box">
              <span class="seckill-tag">秒杀价</span>
              <span class="price-symbol">￥</span>
              <span class="price-num">{{ currentGoods.seckillPrice }}</span>
              <span class="price-old">￥{{ currentGoods.goodsPrice }}</span>
            </div>
          </div>
        </div>

        <!-- 活动信息栅格矩阵 -->
        <div class="detail-meta-grid">
          <div class="meta-item">
            <span class="meta-label">⏰ 开始时间</span>
            <span class="meta-val">{{ currentGoods.startTime }}</span>
          </div>

          <div class="meta-item">
            <span class="meta-label">⌛ 结束时间</span>
            <span class="meta-val">{{ currentGoods.endTime }}</span>
          </div>

          <div class="meta-item">
            <span class="meta-label">📌 活动状态</span>
            <span class="meta-val">
              <el-tag :type="getElTagType(currentGoods.status)" effect="light">
                {{ getStatusText(currentGoods) }}
              </el-tag>
            </span>
          </div>

          <div class="meta-item">
            <span class="meta-label">📦 剩余库存</span>
            <span class="meta-val stock-num">{{ currentGoods.stockCount }} 件</span>
          </div>
        </div>

        <!-- 核心秒杀动作按钮 -->
        <button
          class="submit-seckill-btn"
          :disabled="currentGoods.status !== 1 || seckillLoading"
          @click="openCaptchaDialog"
        >
          {{ seckillLoading ? '⏳ 秒杀处理中...' : (currentGoods.status === 1 ? '🔥 立即抢购' : '非秒杀时段') }}
        </button>
      </div>
    </el-dialog>

    <!-- 3. 图形验证码校验弹窗 (后端防刷) -->
    <el-dialog
      v-model="captchaVisible"
      title="🔒 安全验证"
      width="380px"
      center
      align-center
      :close-on-click-modal="false"
      class="captcha-dialog"
    >
      <div class="captcha-box">
        <p class="captcha-tips">点击图片刷新，请输入上方 4 位计算结果/验证码</p>
        <img
          :src="captchaImgUrl"
          alt="验证码"
          class="captcha-img"
          @click="refreshCaptcha"
          title="点击刷新验证码"
        />
        <el-input
          v-model="captchaCode"
          placeholder="请输入验证码"
          maxlength="4"
          size="large"
          clearable
          @keyup.enter="handleStartSeckill"
        />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="captchaVisible = false">取消</el-button>
          <el-button type="danger" :loading="submitLoading" @click="handleStartSeckill">
            确认抢购
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import { Star, StarFilled } from '@element-plus/icons-vue'
import axios from 'axios'
import { useRouter } from 'vue-router'

const router = useRouter()

// 状态定义
const userId = ref(1)
const goodsList = ref([])
const searchKeyword = ref('') 
const detailVisible = ref(false)
const captchaVisible = ref(false)
const currentGoods = ref(null)
const isFavorite = ref(false)

const captchaCode = ref('')
const captchaImgUrl = ref('')
const seckillLoading = ref(false)
const submitLoading = ref(false)

// 校验登录通用函数
const checkLogin = () => {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录账号！')
    router.push('/login')
    return false
  }
  return true
}

// 打开详情弹窗
const openDetailDialog = async (goods) => {
  if (!checkLogin()) return
  currentGoods.value = goods
  detailVisible.value = true
  checkFavoriteStatus(goods.id)
}

// 查询收藏状态接口
const checkFavoriteStatus = async (goodsId) => {
  try {
    const res = await axios.get('/api/favorite/status', {
      params: { userId: userId.value, goodsId: goodsId }
    })
    if (res.data && res.data.code === 200) {
      isFavorite.value = res.data.data
    }
  } catch (e) {
    isFavorite.value = false
  }
}

// 点击收藏/取消收藏
const handleToggleFavorite = async () => {
  if (!currentGoods.value) return
  try {
    const res = await axios.post('/api/favorite/toggle', null, {
      params: { userId: userId.value, goodsId: currentGoods.value.id }
    })
    if (res.data && res.data.code === 200) {
      isFavorite.value = res.data.data
      ElMessage.success(res.data.msg || (isFavorite.value ? '收藏成功！' : '已取消收藏'))
    }
  } catch (e) {
    ElMessage.error('修改收藏状态失败')
  }
}

// 打开验证码弹窗
const openCaptchaDialog = () => {
  if (!checkLogin()) return
  captchaCode.value = ''
  refreshCaptcha()
  captchaVisible.value = true
}

// 动态加载后端商品列表
const fetchGoodsList = async () => {
  try {
    const res = await axios.get('/api/seckill/list', {
      params: {
        keyword: searchKeyword.value.trim()
      }
    })
    if (res.data && res.data.code === 200 && res.data.data) {
      const now = new Date().getTime()

      goodsList.value = res.data.data.map(item => {
        const start = item.startDate ? new Date(item.startDate).getTime() : 0
        const end = item.endDate ? new Date(item.endDate).getTime() : 0

        let status = 1 
        if (now < start) {
          status = 0 
        } else if (now > end) {
          status = 2 
        }

        return {
          id: item.id,
          goodsName: item.goodsName || '商品名称',
          goodsTitle: item.goodsDetail || '暂无描述',
          goodsImg: item.goodsImg || 'https://via.placeholder.com/200?text=No+Image',
          goodsPrice: Number(item.goodsPrice || 0),
          seckillPrice: Number(item.seckillPrice || 0),
          stockCount: item.stockCount ?? 0,
          startTime: item.startDate ? new Date(item.startDate).toLocaleString() : '暂无',
          endTime: item.endDate ? new Date(item.endDate).toLocaleString() : '暂无',
          status: status
        }
      })
    } else {
      ElMessage.error(res.data.msg || '获取列表失败')
    }
  } catch (error) {
    ElMessage.error('无法连接到后端服务，请检查服务器状态')
  }
}

const handleSearch = () => { fetchGoodsList() }
const clearSearch = () => { searchKeyword.value = ''; fetchGoodsList() }

const refreshCaptcha = () => {
  if (!currentGoods.value) return
  captchaImgUrl.value = `/api/seckill/captcha?userId=${userId.value}&goodsId=${currentGoods.value.id}&t=${Date.now()}`
}

const handleStartSeckill = async () => {
  if (!captchaCode.value.trim()) {
    ElMessage.warning('请输入验证码')
    return
  }
  submitLoading.value = true
  seckillLoading.value = true

  try {
    const pathRes = await axios.get('/api/seckill/getPath', {
      params: {
        userId: userId.value,
        goodsId: currentGoods.value.id,
        captchaCode: captchaCode.value.trim()
      }
    })

    if (pathRes.data.code !== 200) {
      ElMessage.error(pathRes.data.msg || '验证码错误')
      refreshCaptcha()
      submitLoading.value = false
      seckillLoading.value = false
      return
    }

    captchaVisible.value = false
    const pathToken = pathRes.data.data

    const requestId = 'req_' + Date.now()
    const doRes = await axios.post(`/api/seckill/${pathToken}/doSeckill`, null, {
      params: {
        userId: userId.value,
        goodsId: currentGoods.value.id,
        requestId: requestId
      }
    })

    if (doRes.data.code === 200) {
      ElMessage.info('排队处理中，请稍候...')
      pollSeckillResult(currentGoods.value.id)
    } else {
      ElMessage.error(doRes.data.msg || '系统拦截，秒杀失败')
      seckillLoading.value = false
    }
  } catch (error) {
    ElMessage.error('网络拥堵或服务繁忙')
    seckillLoading.value = false
  } finally {
    submitLoading.value = false
  }
}

const pollSeckillResult = (goodsId) => {
  let count = 0
  const interval = setInterval(async () => {
    count++
    try {
      const res = await axios.get('/api/seckill/result', {
        params: { userId: userId.value, goodsId: goodsId }
      })

      const orderId = res.data.data
      if (orderId > 0) {
        clearInterval(interval)
        seckillLoading.value = false
        detailVisible.value = false
        if (currentGoods.value) currentGoods.value.stockCount--
        ElNotification({
          title: '抢购成功',
          message: `订单创建成功！订单号：${orderId}`,
          type: 'success'
        })
      } else if (orderId === -1) {
        clearInterval(interval)
        seckillLoading.value = false
        ElMessage.error('库存不足或已抢购过！')
      } else if (count >= 10) {
        clearInterval(interval)
        seckillLoading.value = false
        ElMessage.warning('排队超时，请前往订单中心查看')
      }
    } catch (e) {
      clearInterval(interval)
      seckillLoading.value = false
    }
  }, 1000)
}

const getStatusText = (goods) => {
  if (!goods) return ''
  if (goods.status === 0) return '即将开始'
  if (goods.status === 1) return '秒杀进行中'
  return '活动已结束'
}

const getStatusClass = (status) => {
  if (status === 0) return 'is-upcoming'
  if (status === 1) return 'is-ongoing'
  return 'is-ended'
}

const getElTagType = (status) => {
  if (status === 0) return 'warning'
  if (status === 1) return 'danger'
  return 'info'
}

onMounted(() => {
  const savedUser = localStorage.getItem('userInfo')
  if (savedUser) {
    try {
      const u = JSON.parse(savedUser)
      if (u.userId) userId.value = u.userId
    } catch (e) {}
  }
  fetchGoodsList()
})
</script>

<style scoped>
.seckill-app {
  max-width: 1200px;
  margin: 30px auto;
  padding: 0 20px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", sans-serif;
}

.page-header { text-align: center; margin-bottom: 35px; }
.page-title { font-size: 28px; font-weight: 700; color: #1f2329; margin: 0 0 8px 0; }
.page-subtitle { font-size: 14px; color: #8f959e; margin: 0 0 20px 0; }

.search-bar-container { max-width: 520px; margin: 0 auto; }
.search-input :deep(.el-input-group__append) { background-color: #f53d3d; border-color: #f53d3d; color: #ffffff; }
.search-input :deep(.el-input-group__append button.el-button) { color: #ffffff; }
.search-icon { font-size: 16px; margin-right: 4px; }
.empty-box { margin: 50px 0; background: #ffffff; border-radius: 12px; padding: 40px; }

.goods-card {
  position: relative; background: #ffffff; border-radius: 12px; padding: 20px;
  margin-bottom: 24px; border: 1px solid #f0f2f5; box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1); cursor: pointer; display: flex; flex-direction: column;
}
.goods-card:hover { transform: translateY(-6px); box-shadow: 0 12px 28px rgba(0, 0, 0, 0.1); border-color: #ffd8d8; }

.status-badge { position: absolute; top: 12px; right: 12px; padding: 4px 10px; border-radius: 20px; font-size: 12px; font-weight: 600; z-index: 2; box-shadow: 0 2px 6px rgba(0,0,0,0.08); }
.status-badge.is-ongoing { background: linear-gradient(135deg, #f53d3d, #ff7d00); color: #fff; }
.status-badge.is-upcoming { background: #e8f3ff; color: #165dff; }
.status-badge.is-ended { background: #f2f3f5; color: #86909c; }

.img-wrapper { height: 200px; display: flex; align-items: center; justify-content: center; margin-bottom: 16px; border-radius: 8px; overflow: hidden; background-color: #fafafa; }
.goods-img { max-height: 88%; max-width: 88%; object-fit: contain; transition: transform 0.4s ease; }
.goods-card:hover .goods-img { transform: scale(1.08); }

.card-info { display: flex; flex-direction: column; flex: 1; }
.card-title { font-size: 16px; font-weight: 600; color: #1d2129; margin: 0 0 6px 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.goods-desc { font-size: 13px; color: #86909c; line-height: 1.5; height: 38px; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; margin-bottom: 16px; }

.price-container { display: flex; align-items: baseline; gap: 8px; margin-bottom: 16px; margin-top: auto; }
.price-main { color: #f53d3d; font-weight: 700; }
.price-symbol { font-size: 14px; }
.seckill-price { font-size: 22px; font-weight: 700; font-family: DINAlternate-Bold, "Arial", sans-serif; }
.original-price { font-size: 13px; color: #c9cdd4; text-decoration: line-through; }

.detail-btn { width: 100%; height: 40px; background: #f7f8fa; border: 1px solid #e5e6eb; color: #4e5969; font-size: 14px; font-weight: 500; border-radius: 8px; cursor: pointer; transition: all 0.2s ease; }
.goods-card:hover .detail-btn { background: linear-gradient(135deg, #f53d3d, #ff7d00); border-color: transparent; color: #ffffff; box-shadow: 0 4px 12px rgba(245, 61, 61, 0.3); }

.detail-body { padding: 8px 4px; }
.detail-header { display: flex; gap: 20px; align-items: center; padding-bottom: 20px; border-bottom: 1px solid #f2f3f5; margin-bottom: 20px; }
.detail-thumb { width: 110px; height: 110px; object-fit: contain; border-radius: 8px; background: #f7f8fa; padding: 6px; }
.detail-header-info { flex: 1; }

/* 🌟 详情弹窗标题与收藏按钮布局 */
.detail-title-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.detail-name { font-size: 20px; font-weight: 700; color: #1d2129; margin: 0 0 6px 0; }
.fav-btn { box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08); flex-shrink: 0; }

.detail-sub-title { font-size: 13px; color: #86909c; margin: 0 0 12px 0; }
.detail-price-box { display: flex; align-items: baseline; gap: 6px; }
.seckill-tag { background: #ffece8; color: #f53d3d; font-size: 12px; padding: 2px 6px; border-radius: 4px; font-weight: 600; margin-right: 4px; }
.price-num { font-size: 26px; color: #f53d3d; font-weight: 700; }
.price-old { font-size: 14px; color: #86909c; text-decoration: line-through; margin-left: 6px; }

.detail-meta-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 24px; }
.meta-item { background: #f7f8fa; padding: 12px 14px; border-radius: 8px; display: flex; flex-direction: column; gap: 4px; }
.meta-label { font-size: 12px; color: #86909c; }
.meta-val { font-size: 13px; font-weight: 600; color: #1d2129; }
.stock-num { color: #ff7d00; }

.submit-seckill-btn { width: 100%; height: 48px; background: linear-gradient(135deg, #f53d3d, #ff7d00); border: none; color: #ffffff; font-size: 16px; font-weight: 600; border-radius: 8px; cursor: pointer; transition: all 0.2s ease; box-shadow: 0 4px 16px rgba(245, 61, 61, 0.35); }
.submit-seckill-btn:hover:not(:disabled) { opacity: 0.92; transform: translateY(-1px); }
.submit-seckill-btn:disabled { background: #e5e6eb; color: #c9cdd4; box-shadow: none; cursor: not-allowed; }

.captcha-box { display: flex; flex-direction: column; align-items: center; gap: 16px; padding: 10px 0; }
.captcha-tips { font-size: 13px; color: #86909c; margin: 0; }
.captcha-img { width: 150px; height: 50px; cursor: pointer; border: 1px solid #e5e6eb; border-radius: 6px; transition: opacity 0.2s; }
.captcha-img:hover { opacity: 0.85; }
</style>