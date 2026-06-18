<template>
  <div class="container" style="max-width: 420px; margin-top: 60px;">
    <div class="card">
      <h2 style="margin-bottom: 6px;">{{ isLogin ? '登录' : '注册' }}</h2>
      <p class="dim" style="margin-bottom: 20px;">BTC 涨跌预测市场</p>

      <label class="field">
        <span>用户名</span>
        <input class="input" v-model="username" placeholder="3-32位" @keyup.enter="submit" />
      </label>
      <label class="field">
        <span>密码</span>
        <input class="input" type="password" v-model="password" placeholder="6-32位" @keyup.enter="submit" />
      </label>

      <p v-if="err" class="down" style="margin-bottom: 12px; font-size: 13px;">{{ err }}</p>

      <button class="btn btn-primary" style="width: 100%;" @click="submit" :disabled="loading">
        {{ loading ? '处理中...' : (isLogin ? '登录' : '注册') }}
      </button>

      <p style="text-align: center; margin-top: 16px;" class="dim">
        {{ isLogin ? '没有账号？' : '已有账号？' }}
        <a href="#" @click.prevent="isLogin = !isLogin">{{ isLogin ? '去注册' : '去登录' }}</a>
      </p>

      <div style="margin-top: 20px; padding: 12px; background: var(--bg); border-radius: 8px; font-size: 12px;" class="dim">
        演示账号：demo / demo123<br/>
        管理员：admin / admin123
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api.js'
import { store } from '../store.js'

const router = useRouter()
const isLogin = ref(true)
const username = ref('')
const password = ref('')
const err = ref('')
const loading = ref(false)

async function submit() {
  err.value = ''
  if (!username.value || !password.value) {
    err.value = '请填写用户名和密码'
    return
  }
  loading.value = true
  try {
    const data = isLogin.value
      ? await api.login(username.value, password.value)
      : await api.register(username.value, password.value)
    store.setLogin(data)
    window.showToast('登录成功')
    window.refreshWallet && window.refreshWallet()
    router.push('/markets')
  } catch (e) {
    err.value = e.message
  } finally {
    loading.value = false
  }
}
</script>
