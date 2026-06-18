import { createApp } from 'vue'
import App from './App.vue'
import router from './router.js'
import { store } from './store.js'
import './style.css'

const app = createApp(App)
app.use(router)
app.provide('store', store)

store.fetchProfile()

app.mount('#app')
