<template>
  <div class="container">
    <h2 class="section-title">👤 用户中心</h2>

    <!-- 钱包概览 -->
    <div class="card">
      <div style="display: flex; gap: 32px; flex-wrap: wrap;">
        <div>
          <div class="dim" style="font-size: 12px;">用户名</div>
          <div style="font-size: 18px; font-weight: 600;">{{ store.user?.username }}</div>
        </div>
        <div>
          <div class="dim" style="font-size: 12px;">可用余额</div>
          <div style="font-size: 22px; font-weight: 700; color: var(--up);">{{ fmt(wallet?.balance) }}</div>
        </div>
        <div>
          <div class="dim" style="font-size: 12px;">冻结中</div>
          <div style="font-size: 22px; font-weight: 600; color: var(--accent);">{{ fmt(wallet?.frozenBalance) }}</div>
        </div>
        <div>
          <div class="dim" style="font-size: 12px;">累计盈亏</div>
          <div style="font-size: 22px; font-weight: 600;" :class="(wallet?.totalProfit || 0) >= 0 ? 'up' : 'down'">
            {{ (wallet?.totalProfit || 0) >= 0 ? '+' : '' }}{{ fmt(wallet?.totalProfit) }}
          </div>
        </div>
      </div>
    </div>

    <!-- 我的注单 -->
    <div class="card">
      <h3 class="section-title">🎯 我的注单</h3>
      <div v-if="bets.length === 0" class="empty">暂无注单记录</div>
      <table v-else>
        <thead>
          <tr><th>市场</th><th>方向</th><th>金额</th><th>赔率</th><th>结果</th><th>时间</th></tr>
        </thead>
        <tbody>
          <tr v-for="b in bets" :key="b.id" style="cursor: pointer;" @click="$router.push('/market/' + b.marketId)">
            <td>{{ b.marketTitle }}</td>
            <td><span :class="['tag', b.direction === 'UP' ? 'tag-up' : 'tag-down']">{{ b.direction === 'UP' ? '涨' : '跌' }}</span></td>
            <td>{{ b.amount }}</td>
            <td>{{ b.odds }}x</td>
            <td>
              <span v-if="b.result === 'WIN'" class="up">赢 +{{ b.winAmount }}</span>
              <span v-else-if="b.result === 'LOSE'" class="down">输 -{{ b.amount }}</span>
              <span v-else class="tag tag-pending">待结算</span>
            </td>
            <td class="dim">{{ formatTime(b.createdAt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 积分流水 -->
    <div class="card">
      <h3 class="section-title">💰 积分流水</h3>
      <div v-if="transactions.length === 0" class="empty">暂无流水记录</div>
      <table v-else>
        <thead>
          <tr><th>类型</th><th>金额</th><th>备注</th><th>时间</th></tr>
        </thead>
        <tbody>
          <tr v-for="t in transactions" :key="t.id">
            <td>{{ typeText(t.type) }}</td>
            <td :class="t.amount >= 0 ? 'up' : 'down'">{{ t.amount >= 0 ? '+' : '' }}{{ t.amount }}</td>
            <td class="dim">{{ t.remark || '-' }}</td>
            <td class="dim">{{ formatTime(t.createdAt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../api.js'
import { store } from '../store.js'

const wallet = ref(null)
const bets = ref([])
const transactions = ref([])

function fmt(n) {
  if (n == null) return '0.00'
  return Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatTime(t) {
  if (!t) return '--'
  return new Date(t).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function typeText(t) {
  return { REGISTER: '注册赠送', BET: '下注', WIN: '中奖', LOSE: '结算扣除', ADMIN_GRANT: '管理员发放', ADMIN_DEDUCT: '管理员扣除' }[t] || t
}

onMounted(async () => {
  try {
    const [w, b, tx] = await Promise.all([
      api.wallet(),
      api.myBets(0, 50),
      api.transactions(0, 50)
    ])
    wallet.value = w
    bets.value = b?.content || b || []
    transactions.value = tx?.content || tx || []
  } catch (e) {
    window.showToast(e.message, 'error')
  }
})
</script>
