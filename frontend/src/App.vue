<template>
  <div class="app-layout">
    <nav class="navbar">
      <router-link to="/markets" class="logo">
        <span class="logo-icon">B</span>
        <span>BTC Predict</span>
      </router-link>
      <div class="nav-links">
        <router-link to="/markets">Markets</router-link>
        <router-link to="/user" v-if="store.token">Portfolio</router-link>
        <router-link to="/admin" v-if="store.isAdmin">Admin</router-link>
      </div>
      <div class="nav-right">
        <span class="balance-chip" v-if="wallet">{{ formatNum(wallet.balance) }} pts</span>
        <span class="dim" v-if="!store.token" style="font-size: 13px;">Not connected</span>
        <button class="btn btn-ghost" v-if="!store.token" @click="$router.push('/login')" style="padding: 7px 18px; font-size: 13px;">Connect</button>
        <template v-else>
          <span style="font-size: 13px; color: var(--text-dim);">{{ store.user?.username }}</span>
          <button class="btn btn-ghost" @click="logout" style="padding: 7px 14px; font-size: 13px;">Exit</button>
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

function formatNum(n) {
  if (n == null) return '0'
  return Number(n).toLocaleString('en-US', { maximumFractionDigits: 2 })
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
})

window.showToast = showToast
window.refreshWallet = refreshWallet
</script>
