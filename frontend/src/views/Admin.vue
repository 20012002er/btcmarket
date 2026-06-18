<template>
  <div class="container">
    <h2 class="section-title">⚙️ 管理后台</h2>

    <!-- 仪表盘 -->
    <div class="card">
      <h3 class="section-title">📊 平台数据</h3>
      <div v-if="!dash" class="empty">加载中...</div>
      <div v-else style="display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 16px;">
        <div v-for="item in dashItems" :key="item.label" style="padding: 16px; background: var(--bg); border-radius: 8px;">
          <div class="dim" style="font-size: 12px;">{{ item.label }}</div>
          <div style="font-size: 22px; font-weight: 700; margin-top: 4px;">{{ item.value }}</div>
        </div>
      </div>
    </div>

    <!-- 积分管理 -->
    <div class="card">
      <h3 class="section-title">💸 积分发放/扣除</h3>
      <div style="display: flex; gap: 12px; flex-wrap: wrap; align-items: end;">
        <label class="field" style="flex: 1; min-width: 120px; margin-bottom: 0;">
          <span>用户ID</span>
          <input class="input" type="number" v-model.number="form.userId" placeholder="2" />
        </label>
        <label class="field" style="flex: 1; min-width: 120px; margin-bottom: 0;">
          <span>金额</span>
          <input class="input" type="number" v-model.number="form.amount" placeholder="1000" />
        </label>
        <label class="field" style="flex: 2; min-width: 200px; margin-bottom: 0;">
          <span>备注</span>
          <input class="input" v-model="form.remark" placeholder="活动奖励" />
        </label>
      </div>
      <div style="display: flex; gap: 12px; margin-top: 16px;">
        <button class="btn btn-up" @click="grant" :disabled="acting">发放积分</button>
        <button class="btn btn-danger" @click="deduct" :disabled="acting">扣除积分</button>
      </div>
    </div>

    <!-- 用户列表 -->
    <div class="card">
      <h3 class="section-title">👥 用户列表</h3>
      <table>
        <thead>
          <tr><th>ID</th><th>用户名</th><th>角色</th><th>余额</th><th>冻结</th><th>盈亏</th><th>状态</th><th>注册时间</th></tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id">
            <td>{{ u.id }}</td>
            <td>{{ u.username }}</td>
            <td><span :class="['tag', u.role === 'ADMIN' ? 'tag-pending' : 'tag-active']">{{ u.role === 'ADMIN' ? '管理员' : '用户' }}</span></td>
            <td>{{ fmt(u.balance) }}</td>
            <td>{{ fmt(u.frozenBalance) }}</td>
            <td :class="(u.totalProfit || 0) >= 0 ? 'up' : 'down'">{{ (u.totalProfit || 0) >= 0 ? '+' : '' }}{{ fmt(u.totalProfit) }}</td>
            <td>{{ u.status === 1 ? '正常' : '禁用' }}</td>
            <td class="dim">{{ formatTime(u.createdAt) }}</td>
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
    { label: '总用户数', value: d.totalUsers ?? 0 },
    { label: '活跃市场', value: d.activeMarkets ?? 0 },
    { label: '总注单', value: d.totalBets ?? 0 },
    { label: '待结算', value: d.pendingBets ?? 0 },
    { label: '平台流水', value: fmt(d.totalVolume) },
    { label: '平台盈亏', value: fmt(d.platformProfit) },
  ]
})

function fmt(n) {
  if (n == null) return '0'
  return Number(n).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

function formatTime(t) {
  if (!t) return '--'
  return new Date(t).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
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
