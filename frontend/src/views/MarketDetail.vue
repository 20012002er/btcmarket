<template>
  <div class="container">
    <div style="margin-bottom: 16px;">
      <a href="#" @click.prevent="$router.back()" class="dim" style="font-size: 13px;">&larr; Back to Markets</a>
    </div>

    <div v-if="loading" class="empty">Loading...</div>
    <template v-else-if="market">
      <div class="pm-detail-layout">
        <!-- Left: Market Info + Bet Panel -->
        <div>
          <!-- Market Header -->
          <div class="pm-hero">
            <div style="position: relative; z-index: 1;">
              <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 12px;">
                <span v-if="market.status === 'ACTIVE'" class="pm-live-badge">LIVE</span>
                <span v-else-if="market.status === 'PENDING'" class="tag tag-pending">PENDING</span>
                <span v-else-if="market.status === 'SETTLED'" class="tag tag-settled">SETTLED</span>
                <span v-else class="tag tag-settled">{{ market.status }}</span>
                <span style="font-size: 12px; color: var(--text-muted);">{{ market.marketNo }}</span>
              </div>
              <h2 style="font-size: 22px; font-weight: 700; margin-bottom: 8px;">{{ market.title }}</h2>

              <!-- Price Row -->
              <div class="pm-price-row" style="margin-bottom: 12px;">
                <div>
                  <div style="font-size: 12px; color: var(--text-dim); margin-bottom: 2px;">目标价格</div>
                  <div class="price-big" style="font-size: 28px;">${{ fmt(market.openPrice) }}</div>
                </div>
                <div v-if="currentPrice">
                  <div style="font-size: 12px; color: var(--text-dim); margin-bottom: 2px;">最终价格</div>
                  <div class="price-big" style="font-size: 28px;" :class="priceChangeClass">
                    ${{ fmt(currentPrice.price) }}
                    <span style="font-size: 14px; margin-left: 6px;">{{ priceChangeSign }}{{ fmt(currentPrice.change24h) }}%</span>
                  </div>
                </div>
              </div>

              <!-- Target Price Dashed Line Indicator -->
              <div v-if="currentPrice" style="margin-top: 8px; display: flex; align-items: center; gap: 8px;">
                <span class="tag tag-pending" style="font-size: 11px;">Target ${{ fmt(market.openPrice) }}</span>
                <div style="flex: 1; height: 1px; border-top: 1px dashed var(--border-light);"></div>
              </div>

              <!-- Timeline -->
              <div class="pm-market-timeline">
                <span class="dot" :class="dotClass('start')"></span>
                <span class="line"></span>
                <span style="font-size: 12px; color: var(--text-dim);">{{ formatTime(market.startTime) }}</span>
                <span class="line"></span>
                <span class="dot" :class="dotClass('end')"></span>
                <span style="font-size: 12px; color: var(--text-dim);">{{ formatTime(market.endTime) }}</span>
              </div>
            </div>
          </div>

          <!-- Price Chart -->
          <div class="card" style="padding: 20px;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
              <div style="font-size: 14px; font-weight: 600; color: var(--text);">实时价格走势</div>
              <div class="pm-time-tabs">
                <button
                  v-for="t in timeTabs"
                  :key="t.value"
                  :class="{ active: timeRange === t.value }"
                  @click="timeRange = t.value"
                >{{ t.label }}</button>
              </div>
            </div>
            <div ref="chartRef" style="width: 100%; height: 280px;"></div>
          </div>

          <!-- Stats Grid -->
          <div class="pm-stat-grid">
            <div class="pm-stat-item">
              <div class="stat-label">Open Price</div>
              <div class="stat-value">${{ fmt(market.openPrice) }}</div>
            </div>
            <div class="pm-stat-item" v-if="market.closePrice">
              <div class="stat-label">Close Price</div>
              <div class="stat-value" :class="closeClass">${{ fmt(market.closePrice) }}</div>
            </div>
            <div class="pm-stat-item">
              <div class="stat-label">Total Bets</div>
              <div class="stat-value">{{ market.betCount || 0 }}</div>
            </div>
            <div class="pm-stat-item" v-if="market.result">
              <div class="stat-label">Result</div>
              <div class="stat-value" :class="market.result === 'UP' ? 'up' : 'down'">
                {{ market.result === 'UP' ? 'UP' : 'DOWN' }}
              </div>
            </div>
          </div>

          <!-- Pool Bar -->
          <div class="card" style="padding: 20px;">
            <div style="display: flex; justify-content: space-between; margin-bottom: 12px;">
              <div>
                <span class="up" style="font-weight: 700; font-size: 16px;">UP</span>
                <span style="color: var(--text-dim); font-size: 13px; margin-left: 8px;">{{ fmt(market.totalBetUp) }} pts</span>
              </div>
              <div>
                <span style="color: var(--text-dim); font-size: 13px; margin-right: 8px;">{{ fmt(market.totalBetDown) }} pts</span>
                <span class="down" style="font-weight: 700; font-size: 16px;">DOWN</span>
              </div>
            </div>
            <div class="pm-pool-bar">
              <div class="up-pool" :style="{ width: upPercent + '%' }"></div>
              <div class="down-pool" :style="{ width: downPercent + '%' }"></div>
            </div>
            <div style="display: flex; justify-content: space-between; font-size: 12px; color: var(--text-dim); margin-top: 8px;">
              <span>{{ upPercent.toFixed(1) }}%</span>
              <span>{{ downPercent.toFixed(1) }}%</span>
            </div>
          </div>

          <!-- Bet Panel (only for ACTIVE) -->
          <div class="card" v-if="market.status === 'ACTIVE'">
            <div class="section-title">Place Bet</div>
            <div v-if="!store.token" class="empty" style="padding: 24px;">
              请先 <a href="#" @click.prevent="$router.push('/login')">登录</a> 后下注
            </div>
            <template v-else>
              <!-- Direction Buttons -->
              <div class="pm-bet-buttons">
                <div :class="['pm-bet-btn', 'up-btn', { selected: direction === 'UP' }]" @click="direction = 'UP'">
                  <div class="direction-label up">UP</div>
                  <div class="direction-odds">{{ market.oddsUp || '--' }}x odds</div>
                </div>
                <div :class="['pm-bet-btn', 'down-btn', { selected: direction === 'DOWN' }]" @click="direction = 'DOWN'">
                  <div class="direction-label down">DOWN</div>
                  <div class="direction-odds">{{ market.oddsDown || '--' }}x odds</div>
                </div>
              </div>

              <!-- Amount -->
              <label class="field">
                <span>Bet Amount &middot; Balance: {{ walletBalance }}</span>
                <input class="input" type="number" v-model.number="amount" min="1" :max="walletBalance" placeholder="Enter amount" />
              </label>

              <div class="pm-quick-amounts">
                <button v-for="v in [100, 500, 1000]" :key="v" @click="amount = v">{{ v }}</button>
                <button @click="amount = walletBalance">MAX</button>
              </div>

              <!-- Confirm Panel -->
              <div class="pm-confirm-panel">
                <div class="row">
                  <span class="dim">Direction</span>
                  <span :class="direction === 'UP' ? 'up' : 'down'" style="font-weight: 600;">{{ direction }}</span>
                </div>
                <div class="row">
                  <span class="dim">Amount</span>
                  <span>{{ amount || 0 }} pts</span>
                </div>
                <div class="row">
                  <span class="dim">Odds</span>
                  <span>{{ direction === 'UP' ? (market.oddsUp || '--') : (market.oddsDown || '--') }}x</span>
                </div>
                <div class="row" style="border-top: 1px solid var(--border); padding-top: 8px; margin-top: 4px;">
                  <span class="dim">Potential Win</span>
                  <span class="up" style="font-weight: 700;">+{{ winAmount }} pts</span>
                </div>
              </div>

              <button :class="['btn', direction === 'UP' ? 'btn-up' : 'btn-down']" style="width: 100%; padding: 16px; font-size: 16px;" @click="placeBet" :disabled="betting">
                {{ betting ? 'Placing...' : `Bet ${direction}` }}
              </button>
            </template>
          </div>
        </div>

        <!-- Right: Outcome + Bet List -->
        <div>
          <!-- Outcome Card -->
          <div class="card pm-outcome-card" style="margin-bottom: 16px;">
            <div class="pm-outcome-icon" :class="outcomeClass">
              <svg v-if="outcome === 'UP'" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
              <svg v-else-if="outcome === 'DOWN'" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
              <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
            </div>
            <div class="pm-outcome-title" :class="outcomeClass">Outcome: {{ outcomeText }}</div>
            <div class="pm-outcome-desc">{{ market.title }} - {{ formatTimeShort(market.startTime) }}</div>
            <div style="font-size: 12px; color: var(--text-muted); margin-top: 8px;">交易即表示你同意 使用条款</div>
          </div>

          <!-- Related Markets -->
          <div class="card" style="padding: 16px; margin-bottom: 16px;">
            <div style="font-size: 13px; font-weight: 600; color: var(--text-dim); margin-bottom: 12px; text-transform: uppercase; letter-spacing: 0.5px;">Related Markets</div>
            <div class="pm-related-list">
              <div v-for="rm in relatedMarkets" :key="rm.id" class="pm-related-item" @click="$router.push('/market/' + rm.id)">
                <div class="pm-related-icon" :class="rm.asset === 'BTC' ? 'btc' : rm.asset === 'ETH' ? 'eth' : rm.asset === 'SOL' ? 'sol' : 'xrp'">
                  {{ rm.asset[0] }}
                </div>
                <div class="pm-related-info">
                  <div class="pm-related-title">{{ rm.title }}</div>
                  <div class="pm-related-time">{{ formatTimeShort(rm.startTime) }}</div>
                </div>
                <div class="pm-related-prob" :class="rm.prob >= 50 ? 'up' : 'down'">
                  <span class="prob-dot" :class="rm.prob >= 50 ? 'up' : 'down'"></span>
                  {{ rm.prob }}%
                </div>
                <div class="pm-related-dir" :class="rm.prob >= 50 ? 'up' : 'down'">{{ rm.prob >= 50 ? 'Up' : 'Down' }}</div>
              </div>
            </div>
          </div>

          <!-- Bet List -->
          <div class="card">
            <div class="section-title">Order Book &middot; {{ bets.length }}</div>
            <div v-if="bets.length === 0" class="empty" style="padding: 24px;">No bets yet</div>
            <div v-else class="pm-bet-list">
              <div v-for="b in bets" :key="b.id" style="padding: 12px 0; border-bottom: 1px solid var(--border);">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                  <div style="display: flex; align-items: center; gap: 8px;">
                    <span :class="['tag', b.direction === 'UP' ? 'tag-up' : 'tag-down']" style="min-width: 48px; text-align: center;">
                      {{ b.direction === 'UP' ? 'UP' : 'DOWN' }}
                    </span>
                    <span style="font-weight: 500;">{{ b.username }}</span>
                  </div>
                  <span style="font-variant-numeric: tabular-nums; font-weight: 600;">{{ b.amount }} pts</span>
                </div>
                <div style="display: flex; justify-content: space-between; margin-top: 4px; font-size: 12px; color: var(--text-dim);">
                  <span>{{ b.odds }}x</span>
                  <span v-if="b.result === 'WIN'" class="up">WIN +{{ b.winAmount }}</span>
                  <span v-else-if="b.result === 'LOSE'" class="down">LOSE</span>
                  <span v-else>Pending</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api.js'
import { store } from '../store.js'
import * as echarts from 'echarts'

const route = useRoute()
const market = ref(null)
const bets = ref([])
const loading = ref(true)
const direction = ref('UP')
const amount = ref(100)
const betting = ref(false)
const wallet = ref(null)
const currentPrice = ref(null)
const priceHistory = ref([])
const chartRef = ref(null)
let chart = null
const timeRange = ref('1H')

const timeTabs = [
  { label: '5分钟', value: '5M' },
  { label: '15分钟', value: '15M' },
  { label: '1小时', value: '1H' },
  { label: '1天', value: '1D' },
]

const walletBalance = computed(() => wallet.value?.balance ?? 0)
const winAmount = computed(() => {
  const odds = direction.value === 'UP' ? (market.value?.oddsUp || 0) : (market.value?.oddsDown || 0)
  return ((amount.value || 0) * Number(odds)).toFixed(2)
})
const closeClass = computed(() => {
  if (!market.value?.closePrice || !market.value?.openPrice) return ''
  return Number(market.value.closePrice) >= Number(market.value.openPrice) ? 'up' : 'down'
})
const upPercent = computed(() => {
  const up = Number(market.value?.totalBetUp) || 0
  const down = Number(market.value?.totalBetDown) || 0
  if (up + down === 0) return 50
  return (up / (up + down)) * 100
})
const downPercent = computed(() => 100 - upPercent.value)

const priceChangeClass = computed(() => {
  if (!currentPrice.value) return ''
  return Number(currentPrice.value.change24h) >= 0 ? 'up' : 'down'
})
const priceChangeSign = computed(() => {
  if (!currentPrice.value) return ''
  return Number(currentPrice.value.change24h) >= 0 ? '+' : ''
})

const outcome = computed(() => {
  if (!market.value) return null
  if (market.value.result) return market.value.result
  if (!currentPrice.value || !market.value.openPrice) return null
  return Number(currentPrice.value.price) >= Number(market.value.openPrice) ? 'UP' : 'DOWN'
})
const outcomeText = computed(() => {
  if (!outcome.value) return 'Pending'
  return outcome.value
})
const outcomeClass = computed(() => {
  if (!outcome.value) return 'pending'
  return outcome.value === 'UP' ? 'up' : 'down'
})

const relatedMarkets = computed(() => {
  const assets = ['BTC', 'ETH', 'SOL', 'XRP']
  const titles = [
    'Bitcoin Up or Down',
    'Ethereum Up or Down',
    'Solana Up or Down',
    'XRP Up or Down'
  ]
  return assets.map((asset, i) => ({
    id: i + 1,
    asset,
    title: titles[i] + ' - June 25, 2AM ET',
    startTime: market.value?.startTime,
    prob: Math.round(upPercent.value)
  }))
})

function fmt(n) {
  if (n == null) return '0'
  return Number(n).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatTime(t) {
  if (!t) return '--'
  return new Date(t).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function formatTimeShort(t) {
  if (!t) return '--'
  const d = new Date(t)
  return `${d.getMonth() + 1}月${d.getDate()}日, ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

function dotClass(pos) {
  if (!market.value) return 'pending'
  if (market.value.status === 'SETTLED') return 'settled'
  if (market.value.status === 'ACTIVE') return pos === 'start' ? 'active' : 'pending'
  return 'pending'
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

async function fetchPrice() {
  try {
    const p = await api.btcPrice()
    currentPrice.value = p
  } catch {}
}

async function fetchPriceHistory() {
  try {
    const data = await api.btcPriceHistory(120)
    priceHistory.value = (data || []).reverse()
    // 如果 chart 已初始化直接更新；否则等 nextTick initChart 里会调用
    if (chart) updateChart()
  } catch {}
}

function initChart() {
  if (!chartRef.value) return
  if (chart) chart.dispose()
  chart = echarts.init(chartRef.value)
  updateChart()
  window.addEventListener('resize', () => chart && chart.resize())
}

function updateChart() {
  if (!chart) return
  if (priceHistory.value.length === 0) {
    chart.setOption({
      title: { text: '暂无价格数据', left: 'center', top: 'center', textStyle: { color: '#9aa0a6', fontSize: 14, fontWeight: 400 } },
      xAxis: { show: false }, yAxis: { show: false }, series: []
    }, true)
    return
  }

  const data = priceHistory.value.map(t => ({
    time: new Date(t.timestamp).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
    value: Number(t.price)
  }))

  const values = data.map(d => d.value)
  const min = Math.min(...values)
  const max = Math.max(...values)
  const pad = (max - min) * 0.1 || max * 0.01

  const openPrice = market.value?.openPrice ? Number(market.value.openPrice) : null

  const option = {
    grid: { left: 16, right: 16, top: 24, bottom: 24 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.98)',
      borderColor: '#dfe1e6',
      textStyle: { color: '#1a1d23', fontSize: 12 },
      formatter: (params) => {
        const p = params[0]
        return `<div style="font-weight:600">$${Number(p.value).toLocaleString('en-US',{minimumFractionDigits:2,maximumFractionDigits:2})}</div><div style="color:#5f6368;font-size:11px">${p.name}</div>`
      }
    },
    xAxis: {
      type: 'category',
      data: data.map(d => d.time),
      axisLine: { lineStyle: { color: '#e8eaed' } },
      axisLabel: { color: '#9aa0a6', fontSize: 11, interval: Math.floor(data.length / 6) },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      min: openPrice ? Math.min(min - pad, openPrice * 0.998) : min - pad,
      max: openPrice ? Math.max(max + pad, openPrice * 1.002) : max + pad,
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#e8eaed', type: 'dashed' } },
      axisLabel: {
        color: '#9aa0a6',
        fontSize: 11,
        formatter: (v) => '$' + (v >= 1000 ? (v/1000).toFixed(1)+'k' : v.toFixed(0))
      }
    },
    series: [
      {
        type: 'line',
        data: data.map(d => d.value),
        smooth: true,
        symbol: 'none',
        lineStyle: {
          color: '#f9ab00',
          width: 2
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(249,171,0,0.12)' },
            { offset: 1, color: 'rgba(249,171,0,0)' }
          ])
        }
      },
      ...(openPrice ? [{
        type: 'line',
        data: data.map(() => openPrice),
        symbol: 'none',
        lineStyle: {
          color: '#9aa0a6',
          width: 1,
          type: 'dashed'
        },
        silent: true,
        tooltip: { show: false }
      }] : [])
    ]
  }

  chart.setOption(option, true)
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

let timer = null
let priceTimer = null
let chartInitialized = false

// 当 market 数据加载完成后，chart 容器才会渲染，此时再初始化 chart
watch(market, (val) => {
  if (val && !chartInitialized) {
    chartInitialized = true
    nextTick(() => initChart())
  }
})

onMounted(() => {
  fetchData()
  fetchWallet()
  fetchPrice()
  fetchPriceHistory()
  timer = setInterval(fetchData, 5000)
  priceTimer = setInterval(() => { fetchPrice(); fetchPriceHistory() }, 5000)
})

onUnmounted(() => {
  clearInterval(timer)
  clearInterval(priceTimer)
  if (chart) {
    chart.dispose()
    chart = null
  }
})

watch(timeRange, () => {
  fetchPriceHistory()
})
</script>

<style scoped>
.pm-time-tabs {
  display: flex;
  gap: 4px;
  background: var(--bg-input);
  padding: 3px;
  border-radius: 8px;
}
.pm-time-tabs button {
  padding: 5px 12px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: var(--text-dim);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all .15s;
}
.pm-time-tabs button.active {
  background: var(--bg-card);
  color: var(--text);
  font-weight: 600;
}
.pm-time-tabs button:hover:not(.active) {
  color: var(--text);
}

.pm-outcome-card {
  text-align: center;
  padding: 28px 20px;
}
.pm-outcome-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}
.pm-outcome-icon.up {
  background: var(--up-bg-strong);
  color: var(--up);
}
.pm-outcome-icon.down {
  background: var(--down-bg-strong);
  color: var(--down);
}
.pm-outcome-icon.pending {
  background: var(--gold-bg);
  color: var(--gold);
}
.pm-outcome-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 6px;
}
.pm-outcome-desc {
  font-size: 12px;
  color: var(--text-dim);
}

.pm-related-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.pm-related-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 10px;
  cursor: pointer;
  transition: background .15s;
}
.pm-related-item:hover {
  background: var(--bg-card-hover);
}
.pm-related-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}
.pm-related-icon.btc {
  background: linear-gradient(135deg, #f0b90b, #f5d76e);
  color: #000;
}
.pm-related-icon.eth {
  background: linear-gradient(135deg, #627eea, #8aa0f5);
  color: #fff;
}
.pm-related-icon.sol {
  background: linear-gradient(135deg, #9945ff, #14f195);
  color: #fff;
}
.pm-related-icon.xrp {
  background: linear-gradient(135deg, #23292f, #4a5568);
  color: #fff;
}
.pm-related-info {
  flex: 1;
  min-width: 0;
}
.pm-related-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.pm-related-time {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 1px;
}
.pm-related-prob {
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 4px;
}
.prob-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
}
.prob-dot.up { background: var(--up); }
.prob-dot.down { background: var(--down); }
.pm-related-dir {
  font-size: 11px;
  font-weight: 600;
  min-width: 32px;
  text-align: right;
}
</style>
