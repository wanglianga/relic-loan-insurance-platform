import { ref, computed } from 'vue'
import { api, type UserInfo, type LoginResponse } from '@/api'

const ROLE_LABELS: Record<string, string> = {
  COLLECTION: '藏品部',
  RESTORER: '修复师',
  BORROWER: '外借展馆',
  INSURER: '保险公估',
  SECURITY: '安防负责人',
  TRANSPORT: '运输监管',
}

const user = ref<UserInfo | null>(null)
const token = ref<string | null>(null)

function init() {
  const savedToken = localStorage.getItem('token')
  const savedUser = localStorage.getItem('user')
  if (savedToken && savedUser) {
    token.value = savedToken
    try {
      user.value = JSON.parse(savedUser)
    } catch {
      logout()
    }
  }
}

function setAuth(loginData: LoginResponse) {
  token.value = loginData.token
  user.value = loginData.user
  localStorage.setItem('token', loginData.token)
  localStorage.setItem('user', JSON.stringify(loginData.user))
}

function logout() {
  user.value = null
  token.value = null
  localStorage.removeItem('token')
  localStorage.removeItem('user')
}

const isLoggedIn = computed(() => !!token.value && !!user.value)
const userRole = computed(() => user.value?.role || '')
const userName = computed(() => user.value?.name || '')
const roleLabel = computed(() => ROLE_LABELS[userRole.value] || userRole.value)

function hasRole(...roles: string[]): boolean {
  return roles.includes(userRole.value)
}

async function login(username: string, password: string) {
  const res = await api.auth.login({ username, password })
  setAuth(res.data)
  return res.data
}

init()

export function useAuth() {
  return {
    user,
    token,
    isLoggedIn,
    userRole,
    userName,
    roleLabel,
    hasRole,
    login,
    logout,
    ROLE_LABELS,
  }
}
