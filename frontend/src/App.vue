<template>
  <div class="app-layout">
    <nav class="navbar">
      <router-link to="/markets" class="logo">₿ BTC预测</router-link>
      <div class="nav-links">
        <router-link to="/markets">市场</router-link>
        <router-link to="/user" v-if="store.token">用户中心</router-link>
        <router-link to="/admin" v-if="store.isAdmin">管理后台</router-link>
      </div>
      <div class="nav-right">
        <span class="balance-chip" v-if="wallet">💰 {{ formatNum(wallet.balance) }}</span>
        <span class="dim" v-if="!store.token">未登录</span>
        <button class="btn btn-ghost" v-if="!store.token" @click="$router.push('/login')">登录</button>
        <template v-else>
          <span class="dim">{{ store.user?.username }}</span>
          <button class="btn btn-ghost" @click="logout">退出</button>
        </template>
      </div>
    </nav>
    <router-view />
    <div v-if="toast.show" :class="['toast', toast.type]">{{ toast.msg }}</div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { store } from './store.js'
import { api } from './api.js'
import { useRouter } from 'vue-router'

const router = useRouter()
const wallet = ref(null)
const toast = ref({ show: false, msg: '', type: 'success' })

let walletTimer = null
let priceTimer = null

function formatNum(n) {
  if (n == null) return '0'
  return Number(n).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

function showToast(msg, type = 'success') {
  toast.value = { show: true, msg, type }
  setTimeout(() => { toast.value.show = false }, 2500)
}

async function refreshWallet() {
  if (!store.token) { wallet.value = null; return }
  try { wallet.value = await api.wallet() } catch {}
}

function logout() {
  store.logout()
  wallet.value = null
  router.push('/markets')
}

onMounted(() => {
  refreshWallet()
  walletTimer = setInterval(refreshWallet, 5000)
})

onUnmounted(() => {
  clearInterval(walletTimer)
  clearInterval(priceTimer)
})

window.showToast = showToast
window.refreshWallet = refreshWallet
</script>
