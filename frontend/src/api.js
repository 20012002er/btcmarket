const BASE = ''

function getToken() {
  return localStorage.getItem('token') || ''
}

async function request(url, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }
  const token = getToken()
  if (token) headers['Authorization'] = 'Bearer ' + token
  try {
    const resp = await fetch(BASE + url, { ...options, headers })
    const data = await resp.json()
    if (data.code !== 0) {
      if (data.code === 401) {
        localStorage.removeItem('token')
      }
      throw new Error(data.message || '请求失败')
    }
    return data.data
  } catch (e) {
    if (e.message === 'Failed to fetch' || e.message.includes('Network')) {
      throw new Error('网络连接失败，请确认后端服务已启动')
    }
    throw e
  }
}

export const api = {
  // auth
  register: (username, password) => request('/api/auth/register', { method: 'POST', body: JSON.stringify({ username, password }) }),
  login: (username, password) => request('/api/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }),

  // price
  btcPrice: () => request('/api/price/btc'),

  // market
  activeMarkets: () => request('/api/market/active'),
  marketHistory: () => request('/api/market/history'),
  marketDetail: (id) => request('/api/market/' + id),
  marketBets: (id) => request('/api/bet/market/' + id),

  // bet
  placeBet: (marketId, direction, amount) => request('/api/bet/place', { method: 'POST', body: JSON.stringify({ marketId, direction, amount }) }),
  myBets: (page = 0, size = 20) => request('/api/bet/my?page=' + page + '&size=' + size),

  // wallet
  wallet: () => request('/api/wallet'),
  transactions: (page = 0, size = 20) => request('/api/wallet/transactions?page=' + page + '&size=' + size),

  // user
  profile: () => request('/api/user/profile'),
  stats: () => request('/api/user/stats'),

  // admin
  dashboard: () => request('/api/admin/dashboard'),
  adminUsers: () => request('/api/admin/users'),
  grant: (userId, amount, remark) => request('/api/admin/grant', { method: 'POST', body: JSON.stringify({ userId, amount, remark }) }),
  deduct: (userId, amount, remark) => request('/api/admin/deduct', { method: 'POST', body: JSON.stringify({ userId, amount, remark }) }),
}
