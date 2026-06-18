<template>
  <div class="container">
    <!-- BTC 实时价格 -->
    <div class="card" style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 16px;">
      <div>
        <div class="dim" style="font-size: 13px;">BTC/USDT 实时价格</div>
        <div class="price-big" :class="priceDir">
          ${{ formatPrice(price) }}
          <span v-if="change24h !== null" style="font-size: 16px; margin-left: 8px;">
            {{ change24h >= 0 ? '▲' : '▼' }} {{ Math.abs(change24h).toFixed(2) }}%
          </span>
        </div>
        <div class="dim" style="font-size: 12px; margin-top: 4px;">
          24h 高 {{ formatPrice(high24h) }} · 低 {{ formatPrice(low24h) }} · 来源: {{ source }}
        </div>
      </div>
      <div style="text-align: right;">
        <div class="dim" style="font-size: 12px;">下次开奖倒计时</div>
        <div style="font-size: 24px; font-weight: 700; color: var(--accent); font-variant-numeric: tabular-nums;">
          {{ countdown }}
        </div>
      </div>
    </div>

    <!-- 活跃市场 -->
    <h3 class="section-title">🔥 进行中的市场</h3>
    <div v-if="loading" class="empty">加载中...</div>
    <div v-else-if="activeMarkets.length === 0" class="empty">暂无活跃市场，请稍候...</div>
    <div v-else class="market-grid">
      <div v-for="m in activeMarkets" :key="m.id" class="market-card" @click="$router.push('/market/' + m.id)">
        <div style="display: flex; justify-content: space-between; align-items: start;">
          <div>
            <div style="font-weight: 600;">{{ m.title }}</div>
            <div class="dim" style="font-size: 12px; margin-top: 2px;">{{ formatTime(m.openTime) }} → {{ formatTime(m.closeTime) }}</div>
          </div>
          <span class="tag tag-active">进行中</span>
        </div>
        <div style="margin: 14px 0;">
          <div class="dim" style="font-size: 12px;">开盘价</div>
          <div style="font-size: 20px; font-weight: 600;">${{ formatPrice(m.openPrice) }}</div>
        </div>
        <div class="countdown-bar">
          <div :style="{ width: progress(m) + '%' }"></div>
        </div>
        <div style="display: flex; justify-content: space-between; font-size: 12px;" class="dim">
          <span>赔率 {{ m.odds }}x</span>
          <span>剩余 {{ remainMin(m) }}分钟</span>
        </div>
      </div>
    </div>

    <!-- 历史市场 -->
    <h3 class="section-title" style="margin-top: 32px;">📊 最近结算</h3>
    <div v-if="history.length === 0" class="empty">暂无历史记录</div>
    <table v-else>
      <thead>
        <tr>
          <th>市场</th>
          <th>开盘价</th>
          <th>收盘价</th>
          <th>结果</th>
          <th>时间</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="m in history" :key="m.id" style="cursor: pointer;" @click="$router.push('/market/' + m.id)">
          <td>{{ m.title }}</td>
          <td>${{ formatPrice(m.openPrice) }}</td>
          <td>${{ formatPrice(m.closePrice) }}</td>
          <td>
            <span :class="['tag', m.result === 'UP' ? 'tag-up' : 'tag-down']">
              {{ m.result === 'UP' ? '涨 ▲' : '跌 ▼' }}
            </span>
          </td>
          <td class="dim">{{ formatTime(m.closeTime) }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { api } from '../api.js'

const price = ref(null)
const change24h = ref(null)
const high24h = ref(null)
const low24h = ref(null)
const source = ref('')
const priceDir = ref('')

const activeMarkets = ref([])
const history = ref([])
const loading = ref(true)
const countdown = ref('--:--')

let priceTimer = null
let marketTimer = null
let countdownTimer = null

function formatPrice(n) {
  if (n == null) return '--'
  return Number(n).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatTime(t) {
  if (!t) return '--'
  const d = new Date(t)
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function progress(m) {
  if (!m.openTime || !m.closeTime) return 0
  const open = new Date(m.openTime).getTime()
  const close = new Date(m.closeTime).getTime()
  const now = Date.now()
  return Math.min(100, Math.max(0, ((now - open) / (close - open)) * 100))
}

function remainMin(m) {
  if (!m.closeTime) return 0
  return Math.max(0, Math.ceil((new Date(m.closeTime).getTime() - Date.now()) / 60000))
}

async function fetchPrice() {
  try {
    const d = await api.btcPrice()
    const old = price.value
    price.value = d.price
    change24h.value = d.changePercent
    high24h.value = d.highPrice
    low24h.value = d.lowPrice
    source.value = d.source === 'BINANCE' ? 'Binance' : '模拟'
    if (old !== null) {
      priceDir.value = d.price >= old ? 'up' : 'down'
    }
  } catch {}
}

async function fetchMarkets() {
  loading.value = true
  try {
    const [active, hist] = await Promise.all([api.activeMarkets(), api.marketHistory()])
    activeMarkets.value = active || []
    history.value = hist || []
  } catch (e) {
    window.showToast(e.message, 'error')
  } finally {
    loading.value = false
  }
}

function updateCountdown() {
  const now = new Date()
  const sec = 60 - now.getSeconds()
  countdown.value = '00:' + String(sec).padStart(2, '0')
}

onMounted(() => {
  fetchPrice()
  fetchMarkets()
  updateCountdown()
  priceTimer = setInterval(fetchPrice, 3000)
  marketTimer = setInterval(fetchMarkets, 5000)
  countdownTimer = setInterval(updateCountdown, 1000)
})

onUnmounted(() => {
  clearInterval(priceTimer)
  clearInterval(marketTimer)
  clearInterval(countdownTimer)
})
</script>
