<template>
  <div class="container">
    <!-- Dashboard -->
    <div class="pm-hero">
      <div style="position: relative; z-index: 1;">
        <div class="section-title" style="margin-bottom: 20px;">Admin Dashboard</div>
        <div v-if="!dash" class="dim">Loading...</div>
        <div v-else class="pm-stat-grid" style="margin: 0;">
          <div class="pm-stat-item" v-for="item in dashItems" :key="item.label">
            <div class="stat-label">{{ item.label }}</div>
            <div class="stat-value">{{ item.value }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Points Management -->
    <div class="card">
      <div class="section-title">Points Management</div>
      <div style="display: flex; gap: 12px; flex-wrap: wrap; align-items: end;">
        <label class="field" style="flex: 1; min-width: 120px; margin-bottom: 0;">
          <span>User ID</span>
          <input class="input" type="number" v-model.number="form.userId" placeholder="2" />
        </label>
        <label class="field" style="flex: 1; min-width: 120px; margin-bottom: 0;">
          <span>Amount</span>
          <input class="input" type="number" v-model.number="form.amount" placeholder="1000" />
        </label>
        <label class="field" style="flex: 2; min-width: 200px; margin-bottom: 0;">
          <span>Remark</span>
          <input class="input" v-model="form.remark" placeholder="Bonus" />
        </label>
      </div>
      <div style="display: flex; gap: 12px; margin-top: 16px;">
        <button class="btn btn-up" @click="grant" :disabled="acting">Grant</button>
        <button class="btn btn-danger" @click="deduct" :disabled="acting">Deduct</button>
      </div>
    </div>

    <!-- User List -->
    <div class="card" style="padding: 0; overflow: hidden;">
      <div class="section-title" style="padding: 20px 24px 0;">Users</div>
      <table>
        <thead>
          <tr><th>ID</th><th>Username</th><th>Role</th><th>Balance</th><th>Frozen</th><th>P&L</th><th>Status</th></tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id">
            <td>{{ u.id }}</td>
            <td style="font-weight: 500;">{{ u.username }}</td>
            <td><span :class="['tag', u.role === 'ADMIN' ? 'tag-pending' : 'tag-active']">{{ u.role }}</span></td>
            <td style="font-variant-numeric: tabular-nums;">{{ fmt(u.balance) }}</td>
            <td style="font-variant-numeric: tabular-nums;">{{ fmt(u.frozenBalance) }}</td>
            <td :class="(u.totalProfit || 0) >= 0 ? 'up' : 'down'" style="font-variant-numeric: tabular-nums;">{{ (u.totalProfit || 0) >= 0 ? '+' : '' }}{{ fmt(u.totalProfit) }}</td>
            <td>{{ u.status === 1 ? 'Active' : 'Disabled' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from '../api.js'

const dash = ref(null)
const users = ref([])
const form = ref({ userId: 2, amount: 1000, remark: '' })
const acting = ref(false)

const dashItems = computed(() => {
  const d = dash.value || {}
  return [
    { label: 'Users', value: d.totalUsers ?? 0 },
    { label: 'Active Markets', value: d.activeMarkets ?? 0 },
    { label: 'Total Bets', value: d.totalBets ?? 0 },
    { label: 'Pending', value: d.pendingBets ?? 0 },
    { label: 'Volume', value: fmt(d.totalVolume) },
    { label: 'Platform P&L', value: fmt(d.platformProfit) },
  ]
})

function fmt(n) {
  if (n == null) return '0'
  return Number(n).toLocaleString('en-US', { maximumFractionDigits: 2 })
}

async function loadData() {
  try {
    const [d, u] = await Promise.all([api.dashboard(), api.adminUsers()])
    dash.value = d
    users.value = u || []
  } catch (e) {
    window.showToast(e.message, 'error')
  }
}

async function grant() {
  if (!form.value.userId || !form.value.amount) {
    window.showToast('请填写用户ID和金额', 'error')
    return
  }
  acting.value = true
  try {
    await api.grant(form.value.userId, form.value.amount, form.value.remark)
    window.showToast('积分发放成功')
    await loadData()
  } catch (e) {
    window.showToast(e.message, 'error')
  } finally {
    acting.value = false
  }
}

async function deduct() {
  if (!form.value.userId || !form.value.amount) {
    window.showToast('请填写用户ID和金额', 'error')
    return
  }
  acting.value = true
  try {
    await api.deduct(form.value.userId, form.value.amount, form.value.remark)
    window.showToast('积分扣除成功')
    await loadData()
  } catch (e) {
    window.showToast(e.message, 'error')
  } finally {
    acting.value = false
  }
}

onMounted(loadData)
</script>
