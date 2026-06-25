<template>
  <div class="container" style="max-width: 440px; margin-top: 80px;">
    <div class="pm-hero" style="padding: 36px;">
      <div style="position: relative; z-index: 1;">
        <h2 style="font-size: 24px; margin-bottom: 4px;">{{ isLogin ? 'Welcome Back' : 'Create Account' }}</h2>
        <p class="dim" style="margin-bottom: 28px;">BTC 5-Min Prediction Market</p>

        <label class="field">
          <span>Username</span>
          <input class="input" v-model="username" placeholder="3-32 characters" @keyup.enter="submit" />
        </label>
        <label class="field">
          <span>Password</span>
          <input class="input" type="password" v-model="password" placeholder="6-32 characters" @keyup.enter="submit" />
        </label>

        <p v-if="err" class="down" style="margin-bottom: 12px; font-size: 13px;">{{ err }}</p>

        <button :class="['btn', 'btn-primary']" style="width: 100%; padding: 14px; font-size: 15px;" @click="submit" :disabled="loading">
          {{ loading ? 'Processing...' : (isLogin ? 'Sign In' : 'Sign Up') }}
        </button>

        <p style="text-align: center; margin-top: 20px;" class="dim">
          {{ isLogin ? "Don't have an account?" : 'Already have an account?' }}
          <a href="#" @click.prevent="isLogin = !isLogin">{{ isLogin ? 'Sign Up' : 'Sign In' }}</a>
        </p>

        <div style="margin-top: 24px; padding: 14px; background: var(--bg-input); border-radius: var(--radius-sm); font-size: 12px;" class="dim">
          Demo: demo / demo123<br/>
          Admin: admin / admin123
        </div>
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
