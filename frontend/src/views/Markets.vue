<template>
  <div class="container">
    <!-- BTC 实时价格 Hero -->
    <div class="pm-hero">
      <div style="display: flex; justify-content: space-between; align-items: start; flex-wrap: wrap; gap: 20px; position: relative; z-index: 1;">
        <div>
          <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 8px;">
            <span style="font-size: 13px; color: var(--text-dim); font-weight: 500;">BTC / USDT</span>
            <span v-if="source === 'Binance'" style="font-size: 11px; padding: 2px 8px; border-radius: 4px; background: var(--accent-bg); color: var(--accent); font-weight: 600;">LIVE</span>
            <span v-else style="font-size: 11px; padding: 2px 8px; border-radius: 4px; background: var(--gold-bg); color: var(--gold); font-weight: 600;">SIM</span>
          </div>
          <div class="pm-price-row">
            <span class="price-big" :class="priceDir">${{ formatPrice(price) }}</span>
            <span v-if="change24h !== null" :class="change24h >= 0 ? 'up' : 'down'" style="font-size: 18px; font-weight: 600;">
              {{ change24h >= 0 ? '+' : '' }}{{ change24h.toFixed(2) }}%
            </span>
          </div>
          <div style="font-size: 12px; color: var(--text-muted); margin-top: 6px;">
            24h High ${{ formatPrice(high24h) }} &middot; Low ${{ formatPrice(low24h) }}
          </div>
        </div>
        <div style="text-align: right;">
          <div style="font-size: 12px; color: var(--text-dim); margin-bottom: 4px;">下次开奖</div>
          <div style="font-size: 32px; font-weight: 700; color: var(--accent); font-variant-numeric: tabular-nums; letter-spacing: 1px;">
            {{ countdown }}
          </div>
        </div>
      </div>
    </div>

    <!-- 实时价格折线图 -->
    <div class="card" style="padding: 20px;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
        <div style="font-size: 14px; font-weight: 600; color: var(--text);">BTC 实时价格走势</div>
        <div class="pm-time-tabs">
          <button
            v-for="t in timeTabs"
            :key="t.value"
            :class="{ active: timeRange === t.value }"
            @click="timeRange = t.value"
          >{{ t.label }}</button>
        </div>
      </div>
      <div ref="chartRef" style="width: 100%; height: 260px;"></div>
    </div>

    <!-- 活跃市场 -->
    <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;">
      <h3 class="section-title" style="margin-bottom: 0;">Active Markets</h3>
      <span v-if="activeMarkets.length > 0" class="pm-live-badge">{{ activeMarkets.length }} LIVE</span>
    </div>

    <div v-if="loading" class="empty">Loading...</div>
    <div v-else-if="activeMarkets.length === 0" class="empty">暂无活跃市场，请稍候...</div>
    <div v-else class="market-grid">
      <div v-for="m in activeMarkets" :key="m.id" class="market-card" @click="$router.push('/market/' + m.id)">
        <!-- Header -->
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
          <div style="font-weight: 600; font-size: 15px;">{{ m.title }}</div>
          <span v-if="m.status === 'ACTIVE'" class="pm-live-badge">LIVE</span>
          <span v-else class="tag tag-pending">PENDING</span>
        </div>

        <!-- Price + Time -->
        <div style="display: flex; justify-content: space-between; align-items: end; margin-bottom: 16px;">
          <div>
            <div style="font-size: 11px; color: var(--text-muted); margin-bottom: 2px;">Price to Beat</div>
            <div style="font-size: 24px; font-weight: 700; font-variant-numeric: tabular-nums;">${{ formatPrice(m.openPrice) }}</div>
          </div>
          <div style="text-align: right;">
            <div style="font-size: 11px; color: var(--text-muted); margin-bottom: 2px;">Ends</div>
            <div style="font-size: 14px; font-weight: 500; font-variant-numeric: tabular-nums;">{{ formatTime(m.endTime) }}</div>
          </div>
        </div>

        <!-- Pool Bar -->
        <div class="pm-pool-bar">
          <div class="up-pool" :style="{ width: upPoolPercent(m) + '%' }"></div>
          <div class="down-pool" :style="{ width: downPoolPercent(m) + '%' }"></div>
        </div>
        <div style="display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 14px;">
          <span class="up" style="font-weight: 600;">UP {{ upPoolPercent(m).toFixed(0) }}%</span>
          <span class="down" style="font-weight: 600;">DOWN {{ downPoolPercent(m).toFixed(0) }}%</span>
        </div>

        <!-- Bet Buttons Preview -->
        <div class="pm-bet-buttons" style="margin: 0;">
          <div class="pm-bet-btn up-btn" style="padding: 12px; cursor: pointer;" @click.stop="goBet(m.id, 'UP')">
            <div class="direction-label up" style="font-size: 16px;">UP</div>
            <div class="direction-odds">{{ m.oddsUp || '--' }}x</div>
          </div>
          <div class="pm-bet-btn down-btn" style="padding: 12px; cursor: pointer;" @click.stop="goBet(m.id, 'DOWN')">
            <div class="direction-label down" style="font-size: 16px;">DOWN</div>
            <div class="direction-odds">{{ m.oddsDown || '--' }}x</div>
          </div>
        </div>

        <!-- Countdown -->
        <div style="margin-top: 12px;">
          <div class="countdown-bar"><div :style="{ width: progress(m) + '%' }"></div></div>
          <div style="display: flex; justify-content: space-between; font-size: 11px; color: var(--text-muted);">
            <span>{{ formatTime(m.startTime) }}</span>
            <span>{{ remainMin(m) }}min left</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 历史市场 -->
    <h3 class="section-title" style="margin-top: 36px;">Recent Results</h3>
    <div v-if="history.length === 0" class="empty">暂无历史记录</div>
    <div v-else class="card" style="padding: 0; overflow: hidden;">
      <table>
        <thead>
          <tr>
            <th>Market</th>
            <th>Open</th>
            <th>Close</th>
            <th>Result</th>
            <th>Time</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in history" :key="m.id" style="cursor: pointer;" @click="$router.push('/market/' + m.id)">
            <td style="font-weight: 500;">{{ m.title }}</td>
            <td style="font-variant-numeric: tabular-nums;">${{ formatPrice(m.openPrice) }}</td>
            <td style="font-variant-numeric: tabular-nums;">${{ formatPrice(m.closePrice) }}</td>
            <td>
              <span :class="['tag', m.result === 'UP' ? 'tag-up' : 'tag-down']">
                {{ m.result === 'UP' ? 'UP' : 'DOWN' }}
              </span>
            </td>
            <td class="dim">{{ formatTime(m.endTime) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api.js'
import * as echarts from 'echarts'

const router = useRouter()
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

// Chart
const chartRef = ref(null)
let chart = null
const priceHistory = ref([])
const timeRange = ref('1H')

const timeTabs = [
  { label: '5分钟', value: '5M' },
  { label: '15分钟', value: '15M' },
  { label: '1小时', value: '1H' },
  { label: '1天', value: '1D' },
]

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
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

function progress(m) {
  if (!m.startTime || !m.endTime) return 0
  const open = new Date(m.startTime).getTime()
  const close = new Date(m.endTime).getTime()
  const now = Date.now()
  return Math.min(100, Math.max(0, ((now - open) / (close - open)) * 100))
}

function remainMin(m) {
  if (!m.endTime) return 0
  return Math.max(0, Math.ceil((new Date(m.endTime).getTime() - Date.now()) / 60000))
}

function upPoolPercent(m) {
  const up = Number(m.totalBetUp) || 0
  const down = Number(m.totalBetDown) || 0
  if (up + down === 0) return 50
  return (up / (up + down)) * 100
}

function downPoolPercent(m) {
  return 100 - upPoolPercent(m)
}

function goBet(id, dir) {
  router.push('/market/' + id)
}

async function fetchPrice() {
  try {
    const d = await api.btcPrice()
    const old = price.value
    price.value = d.price
    change24h.value = d.change24h
    high24h.value = d.highPrice
    low24h.value = d.lowPrice
    source.value = d.source === 'BINANCE' ? 'Binance' : 'Simulated'
    if (old !== null) {
      priceDir.value = d.price >= old ? 'up' : 'down'
    }
  } catch {}
}

async function fetchPriceHistory() {
  try {
    const data = await api.btcPriceHistory(120)
    priceHistory.value = (data || []).reverse()
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

  // 根据涨跌决定颜色
  const firstVal = values[0]
  const lastVal = values[values.length - 1]
  const isUp = lastVal >= firstVal
  const lineColor = isUp ? '#00c853' : '#ff1744'
  const areaColorTop = isUp ? 'rgba(0,200,83,0.12)' : 'rgba(255,23,68,0.12)'

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
      min: min - pad,
      max: max + pad,
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#e8eaed', type: 'dashed' } },
      axisLabel: {
        color: '#9aa0a6',
        fontSize: 11,
        formatter: (v) => '$' + (v >= 1000 ? (v/1000).toFixed(1)+'k' : v.toFixed(0))
      }
    },
    series: [{
      type: 'line',
      data: data.map(d => d.value),
      smooth: true,
      symbol: 'none',
      lineStyle: {
        color: lineColor,
        width: 2
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: areaColorTop },
          { offset: 1, color: 'rgba(0,0,0,0)' }
        ])
      }
    }]
  }

  chart.setOption(option, true)
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
  fetchPriceHistory()
  updateCountdown()
  nextTick(() => initChart())
  priceTimer = setInterval(() => { fetchPrice(); fetchPriceHistory() }, 5000)
  marketTimer = setInterval(fetchMarkets, 5000)
  countdownTimer = setInterval(updateCountdown, 1000)
})

onUnmounted(() => {
  clearInterval(priceTimer)
  clearInterval(marketTimer)
  clearInterval(countdownTimer)
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
</style>
