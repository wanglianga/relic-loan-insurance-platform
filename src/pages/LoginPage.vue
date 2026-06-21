<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'
import { Camera, AlertCircle } from 'lucide-vue-next'

const router = useRouter()
const { login, ROLE_LABELS } = useAuth()

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

const accounts = [
  { username: 'zhangwei', role: 'COLLECTION', label: '藏品部' },
  { username: 'liuna', role: 'RESTORER', label: '修复师' },
  { username: 'wangqiang', role: 'BORROWER', label: '外借展馆' },
  { username: 'zhaoli', role: 'INSURER', label: '保险公估' },
  { username: 'chenming', role: 'SECURITY', label: '安防负责人' },
  { username: 'sunjie', role: 'TRANSPORT', label: '运输监管' },
]

async function handleLogin() {
  if (!username.value || !password.value) {
    error.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await login(username.value, password.value)
    router.push('/dashboard')
  } catch (e: any) {
    error.value = e.message || '登录失败，请检查用户名和密码'
  } finally {
    loading.value = false
  }
}

function quickLogin(u: string) {
  username.value = u
  password.value = '123456'
  handleLogin()
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-amber-50 to-orange-100 dark:from-gray-900 dark:to-gray-800 p-4">
    <div class="w-full max-w-md">
      <div class="text-center mb-8">
        <div class="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-amber-600 text-white mb-4">
          <Camera class="w-8 h-8" />
        </div>
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">文物修复展借与保险核验平台</h1>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-2">藏品管理 · 修复追踪 · 展借审批 · 保险核验 · 运输监管</p>
      </div>

      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-xl p-8">
        <form @submit.prevent="handleLogin" class="space-y-5">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">用户名</label>
            <input
              v-model="username"
              type="text"
              placeholder="请输入用户名"
              class="w-full px-4 py-2.5 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none transition"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">密码</label>
            <input
              v-model="password"
              type="password"
              placeholder="请输入密码"
              class="w-full px-4 py-2.5 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none transition"
            />
          </div>

          <div v-if="error" class="flex items-center gap-2 text-red-600 text-sm">
            <AlertCircle class="w-4 h-4 shrink-0" />
            <span>{{ error }}</span>
          </div>

          <button
            type="submit"
            :disabled="loading"
            class="w-full py-2.5 rounded-lg bg-amber-600 hover:bg-amber-700 disabled:bg-amber-400 text-white font-medium transition-colors"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </button>
        </form>

        <div class="mt-6 pt-5 border-t border-gray-200 dark:border-gray-700">
          <p class="text-xs text-gray-500 dark:text-gray-400 mb-3">快捷登录（演示账号）</p>
          <div class="grid grid-cols-3 gap-2">
            <button
              v-for="acc in accounts"
              :key="acc.username"
              @click="quickLogin(acc.username)"
              class="px-2 py-1.5 text-xs rounded-lg border border-gray-200 dark:border-gray-600 hover:bg-amber-50 dark:hover:bg-amber-900/20 hover:border-amber-300 text-gray-700 dark:text-gray-300 transition-colors"
            >
              {{ acc.label }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
