<template>
  <div class="container">
    <!-- Wallet Overview -->
    <div class="pm-hero">
      <div style="position: relative; z-index: 1;">
        <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 16px;">
          <span style="font-size: 18px; font-weight: 700;">{{ store.user?.username }}</span>
          <span class="tag tag-active">Connected</span>
        </div>
        <div class="pm-stat-grid" style="margin: 0;">
          <div class="pm-stat-item" style="text-align: left;">
            <div class="stat-label">Available</div>
            <div class="stat-value up">{{ fmt(wallet?.balance) }}</div>
          </div>
          <div class="pm-stat-item" style="text-align: left;">
            <div class="stat-label">Frozen</div>
            <div class="stat-value" style="color: var(--gold);">{{ fmt(wallet?.frozenBalance) }}</div>
          </div>
          <div class="pm-stat-item" style="text-align: left;">
            <div class="stat-label">Total P&L</div>
            <div class="stat-value" :class="(wallet?.totalProfit || 0) >= 0 ? 'up' : 'down'">
              {{ (wallet?.totalProfit || 0) >= 0 ? '+' : '' }}{{ fmt(wallet?.totalProfit) }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- My Bets -->
    <div class="card">
      <div class="section-title">My Bets</div>
      <div v-if="bets.length === 0" class="empty">No bets yet</div>
      <div v-else class="pm-bet-list">
        <div v-for="b in bets" :key="b.id" style="padding: 14px 0; border-bottom: 1px solid var(--border); cursor: pointer;" @click="$router.push('/market/' + b.marketId)">
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <div style="display: flex; align-items: center; gap: 10px;">
              <span :class="['tag', b.direction === 'UP' ? 'tag-up' : 'tag-down']" style="min-width: 48px; text-align: center;">
                {{ b.direction === 'UP' ? 'UP' : 'DOWN' }}
              </span>
              <span style="font-weight: 500;">{{ b.marketTitle }}</span>
            </div>
            <span style="font-variant-numeric: tabular-nums; font-weight: 600;">{{ b.amount }} pts</span>
          </div>
          <div style="display: flex; justify-content: space-between; margin-top: 6px; font-size: 12px; color: var(--text-dim);">
            <span>{{ b.odds }}x &middot; {{ formatTime(b.createdAt) }}</span>
            <span v-if="b.result === 'WIN'" class="up" style="font-weight: 600;">WIN +{{ b.winAmount }}</span>
            <span v-else-if="b.result === 'LOSE'" class="down" style="font-weight: 600;">LOSE</span>
            <span v-else class="tag tag-pending" style="font-size: 11px;">Pending</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Transactions -->
    <div class="card">
      <div class="section-title">Transaction History</div>
      <div v-if="transactions.length === 0" class="empty">No transactions</div>
      <div v-else>
        <div v-for="t in transactions" :key="t.id" style="padding: 12px 0; border-bottom: 1px solid var(--border);">
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span style="font-weight: 500;">{{ typeText(t.type) }}</span>
            <span :class="t.direction === 1 ? 'up' : 'down'" style="font-weight: 600; font-variant-numeric: tabular-nums;">
              {{ t.direction === 1 ? '+' : '-' }}{{ t.amount }}
            </span>
          </div>
          <div style="display: flex; justify-content: space-between; margin-top: 4px; font-size: 12px; color: var(--text-dim);">
            <span>{{ t.remark || '-' }}</span>
            <span>{{ formatTime(t.createdAt) }}</span>
          </div>
        </div>
      </div>
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
  return Number(n).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatTime(t) {
  if (!t) return '--'
  return new Date(t).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function typeText(t) {
  return {
    BET_FREEZE: 'Bet Freeze',
    BET_WIN: 'Bet Win',
    BET_LOSS: 'Bet Loss',
    BET_REFUND: 'Refund',
    ADMIN_GRANT: 'Admin Grant',
    ADMIN_DEDUCT: 'Admin Deduct',
    REGISTER: 'Register Bonus'
  }[t] || t
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
