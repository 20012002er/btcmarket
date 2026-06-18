import { createRouter, createWebHistory } from 'vue-router'
import { store } from './store.js'

const routes = [
  { path: '/', redirect: '/markets' },
  { path: '/login', component: () => import('./views/Login.vue'), meta: { guest: true } },
  { path: '/markets', component: () => import('./views/Markets.vue') },
  { path: '/market/:id', component: () => import('./views/MarketDetail.vue') },
  { path: '/user', component: () => import('./views/UserCenter.vue'), meta: { auth: true } },
  { path: '/admin', component: () => import('./views/Admin.vue'), meta: { auth: true, admin: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  if (to.meta.auth && !store.token) return '/login'
  if (to.meta.admin && !store.isAdmin) return '/markets'
  if (to.meta.guest && store.token) return '/markets'
})

export default router
