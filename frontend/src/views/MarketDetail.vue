<template>
  <div class="container">
    <div style="margin-bottom: 16px;">
      <a href="#" @click.prevent="$router.back()" class="dim">← 返回</a>
    </div>

    <div v-if="loading" class="empty">加载中...</div>
    <template v-else-if="market">
      <!-- 市场信息 -->
      <div class="card">
        <div style="display: flex; justify-content: space-between; align-items: start; flex-wrap: wrap; gap: 12px;">
          <div>
            <h2 style="margin-bottom: 4px;">{{ market.title }}</h2>
            <p class="dim">{{ formatTime(market.openTime) }} → {{ formatTime(market.closeTime) }}</p>
          </div>
          <span :class="['tag', statusTag(market.status)]">{{ statusText(market.status) }}</span>
        </div>
        <div style="display: flex; gap: 32px; margin-top: 20px; flex-wrap: wrap;">
          <div>
            <div class="dim" style="font-size: 12px;">开盘价</div>
            <div style="font-size: 22px; font-weight: 600;">${{ fmt(market.openPrice) }}</div>
          </div>
          <div v-if="market.closePrice">
            <div class="dim" style="font-size: 12px;">收盘价</div>
            <div style="font-size: 22px; font-weight: 600;" :class="closeClass">{{ fmt(market.closePrice) }}</div>
          </div>
          <div>
            <div class="dim" style="font-size: 12px;">赔率</div>
            <div style="font-size: 22px; font-weight: 600; color: var(--accent);">{{ market.odds }}x</div>
          </div>
          <div v-if="market.result">
            <div class="dim" style="font-size: 12px;">结果</div>
            <div style="font-size: 22px; font-weight: 600;" :class="market.result === 'UP' ? 'up' : 'down'">
              {{ market.result === 'UP' ? '涨 ▲' : '跌 ▼' }}
            </div>
          </div>
        </div>
      </div>

      <!-- 押注面板 -->
      <div class="card" v-if="market.status === 'ACTIVE'">
        <h3 class="section-title">🎯 下注</h3>
        <div v-if="!store.token" class="empty">
          请先 <a href="#" @click.prevent="$router.push('/login')">登录</a> 后下注
        </div>
        <template v-else>
          <div style="display: flex; gap: 16px; margin-bottom: 16px;">
            <button
              :class="['btn', direction === 'UP' ? 'btn-up' : 'btn-ghost']"
              style="flex: 1; padding: 16px; font-size: 16px;"
              @click="direction = 'UP'"
            >涨 ▲</button>
            <button
              :class="['btn', direction === 'DOWN' ? 'btn-down' : 'btn-ghost']"
              style="flex: 1; padding: 16px; font-size: 16px;"
              @click="direction = 'DOWN'"
            >跌 ▼</button>
          </div>

          <label class="field">
            <span>下注金额（余额 {{ walletBalance }}）</span>
            <input class="input" type="number" v-model.number="amount" min="1" :max="walletBalance" placeholder="100" />
          </label>

          <div style="display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap;">
            <button class="btn btn-ghost" v-for="v in [100, 500, 1000]" :key="v" @click="amount = v" style="padding: 6px 14px; font-size: 13px;">{{ v }}</button>
            <button class="btn btn-ghost" @click="amount = walletBalance" style="padding: 6px 14px; font-size: 13px;">全部</button>
          </div>

          <div style="padding: 12px; background: var(--bg); border-radius: 8px; margin-bottom: 16px; font-size: 13px;">
            <div style="display: flex; justify-content: space-between; margin-bottom: 6px;">
              <span class="dim">方向</span>
              <span :class="direction === 'UP' ? 'up' : 'down'">{{ direction === 'UP' ? '涨 ▲' : '跌 ▼' }}</span>
            </div>
            <div style="display: flex; justify-content: space-between; margin-bottom: 6px;">
              <span class="dim">下注</span>
              <span>{{ amount || 0 }} 积分</span>
            </div>
            <div style="display: flex; justify-content: space-between;">
              <span class="dim">预测正确可赢</span>
              <span class="up">+{{ winAmount }} 积分</span>
            </div>
          </div>

          <button class="btn btn-primary" style="width: 100%;" @click="placeBet" :disabled="betting">
            {{ betting ? '下注中...' : '确认下注' }}
          </button>
        </template>
      </div>

      <!-- 市场注单 -->
      <div class="card">
        <h3 class="section-title">📋 市场注单（{{ bets.length }}）</h3>
        <div v-if="bets.length === 0" class="empty">暂无注单</div>
        <table v-else>
          <thead>
            <tr><th>用户</th><th>方向</th><th>金额</th><th>赔率</th><th>结果</th><th>时间</th></tr>
          </thead>
          <tbody>
            <tr v-for="b in bets" :key="b.id">
              <td>{{ b.username }}</td>
              <td><span :class="['tag', b.direction === 'UP' ? 'tag-up' : 'tag-down']">{{ b.direction === 'UP' ? '涨' : '跌' }}</span></td>
              <td>{{ b.amount }}</td>
              <td>{{ b.odds }}x</td>
              <td>
                <span v-if="b.result === 'WIN'" class="up">赢 +{{ b.winAmount }}</span>
                <span v-else-if="b.result === 'LOSE'" class="down">输</span>
                <span v-else class="tag tag-pending">待结算</span>
              </td>
              <td class="dim">{{ formatTime(b.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api.js'
import { store } from '../store.js'

const route = useRoute()
const market = ref(null)
const bets = ref([])
const loading = ref(true)
const direction = ref('UP')
const amount = ref(100)
const betting = ref(false)
const wallet = ref(null)

let timer = null

const walletBalance = computed(() => wallet.value?.balance ?? 0)
const winAmount = computed(() => ((amount.value || 0) * (market.value?.odds || 0)).toFixed(2))
const closeClass = computed(() => {
  if (!market.value?.closePrice || !market.value?.openPrice) return ''
  return Number(market.value.closePrice) >= Number(market.value.openPrice) ? 'up' : 'down'
})

function fmt(n) {
  if (n == null) return '--'
  return Number(n).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatTime(t) {
  if (!t) return '--'
  return new Date(t).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function statusText(s) {
  return { PENDING: '待开盘', ACTIVE: '进行中', CLOSED: '已收盘', SETTLED: '已结算' }[s] || s
}
function statusTag(s) {
  return { PENDING: 'tag-pending', ACTIVE: 'tag-active', CLOSED: 'tag-settled', SETTLED: 'tag-settled' }[s] || 'tag-settled'
}

async function fetchData() {
  try {
    const [m, b] = await Promise.all([
      api.marketDetail(route.params.id),
      api.marketBets(route.params.id)
    ])
    market.value = m
    bets.value = b || []
  } catch (e) {
    window.showToast(e.message, 'error')
  } finally {
    loading.value = false
  }
}

async function fetchWallet() {
  if (!store.token) return
  try { wallet.value = await api.wallet() } catch {}
}

async function placeBet() {
  if (!amount.value || amount.value < 1) {
    window.showToast('请输入有效金额', 'error')
    return
  }
  if (amount.value > walletBalance.value) {
    window.showToast('余额不足', 'error')
    return
  }
  betting.value = true
  try {
    await api.placeBet(market.value.id, direction.value, amount.value)
    window.showToast('下注成功！')
    await Promise.all([fetchData(), fetchWallet()])
    window.refreshWallet && window.refreshWallet()
  } catch (e) {
    window.showToast(e.message, 'error')
  } finally {
    betting.value = false
  }
}

onMounted(() => {
  fetchData()
  fetchWallet()
  timer = setInterval(fetchData, 5000)
})

onUnmounted(() => clearInterval(timer))
</script>
