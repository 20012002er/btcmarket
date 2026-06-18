import { reactive } from 'vue'
import { api } from './api.js'

export const store = reactive({
  user: null,
  token: localStorage.getItem('token') || '',

  setLogin(data) {
    this.token = data.token
    this.user = data.user
    localStorage.setItem('token', data.token)
  },

  logout() {
    this.token = ''
    this.user = null
    localStorage.removeItem('token')
  },

  async fetchProfile() {
    if (!this.token) return
    try {
      this.user = await api.profile()
    } catch {
      this.logout()
    }
  },

  get isAdmin() {
    return this.user && this.user.role === 'ADMIN'
  }
})
