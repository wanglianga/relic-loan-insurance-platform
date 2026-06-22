<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'
import { useTheme } from '@/composables/useTheme'
import {
  LayoutDashboard,
  Archive,
  Bug,
  Wrench,
  FileText,
  Shield,
  Package,
  Monitor,
  RotateCcw,
  Link2,
  Sun,
  Moon,
  LogOut,
  ChevronLeft,
  Menu,
  Camera,
  ClipboardCheck,
  Thermometer,
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const { user, userName, roleLabel, hasRole, logout } = useAuth()
const { theme, toggleTheme, isDark } = useTheme()

const collapsed = defineModel<boolean>('collapsed', { default: false })

const navItems = computed(() => {
  const items = [
    { path: '/dashboard', label: '工作台', icon: LayoutDashboard, roles: ['COLLECTION', 'RESTORER', 'BORROWER', 'INSURER', 'SECURITY', 'TRANSPORT'] },
    { path: '/artifacts', label: '藏品管理', icon: Archive, roles: ['COLLECTION', 'RESTORER', 'BORROWER', 'INSURER', 'SECURITY'] },
    { path: '/diseases', label: '病害图谱', icon: Bug, roles: ['COLLECTION', 'RESTORER'] },
    { path: '/diseases/review', label: '病害复核', icon: ClipboardCheck, roles: ['COLLECTION'] },
    { path: '/restorations', label: '修复方案', icon: Wrench, roles: ['COLLECTION', 'RESTORER'] },
    { path: '/loans/apply', label: '借展申请', icon: FileText, roles: ['BORROWER', 'COLLECTION'] },
    { path: '/loans/approval', label: '借展审批', icon: FileText, roles: ['COLLECTION'] },
    { path: '/loans/environment-precheck', label: '展厅环境预审', icon: Thermometer, roles: ['COLLECTION', 'BORROWER'] },
    { path: '/insurance', label: '保险核验', icon: Shield, roles: ['INSURER', 'COLLECTION'] },
    { path: '/packaging', label: '包装出库', icon: Package, roles: ['SECURITY', 'TRANSPORT', 'COLLECTION'] },
    { path: '/transport', label: '运输监管', icon: Package, roles: ['TRANSPORT', 'SECURITY'] },
    { path: '/exhibition/setup', label: '展厅布置', icon: Monitor, roles: ['SECURITY', 'BORROWER'] },
    { path: '/exhibition/monitor', label: '巡展监测', icon: Monitor, roles: ['SECURITY', 'BORROWER'] },
    { path: '/return', label: '归还验收', icon: RotateCcw, roles: ['COLLECTION', 'SECURITY'] },
    { path: '/evidence', label: '证据链', icon: Link2, roles: ['COLLECTION', 'RESTORER', 'BORROWER', 'INSURER', 'SECURITY', 'TRANSPORT'] },
  ]
  return items.filter(item => item.roles.some(r => hasRole(r)))
})

function isActive(path: string) {
  return route.path.startsWith(path)
}

function navigate(path: string) {
  router.push(path)
}

function handleLogout() {
  logout()
  router.push('/login')
}
</script>

<template>
  <div class="flex h-screen bg-gray-50 dark:bg-gray-900">
    <aside
      :class="[
        'flex flex-col bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700 transition-all duration-300 shrink-0',
        collapsed ? 'w-16' : 'w-60'
      ]"
    >
      <div class="h-14 flex items-center justify-between px-4 border-b border-gray-200 dark:border-gray-700">
        <div v-if="!collapsed" class="flex items-center gap-2">
          <Camera class="w-6 h-6 text-amber-600" />
          <span class="font-bold text-sm text-gray-900 dark:text-white truncate">文物修复展借平台</span>
        </div>
        <button
          @click="collapsed = !collapsed"
          class="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500"
        >
          <ChevronLeft v-if="!collapsed" class="w-4 h-4" />
          <Menu v-else class="w-4 h-4" />
        </button>
      </div>

      <nav class="flex-1 overflow-y-auto py-2">
        <button
          v-for="item in navItems"
          :key="item.path"
          @click="navigate(item.path)"
          :class="[
            'w-full flex items-center gap-3 px-4 py-2.5 text-sm transition-colors',
            isActive(item.path)
              ? 'bg-amber-50 dark:bg-amber-900/20 text-amber-700 dark:text-amber-400 border-r-2 border-amber-600'
              : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700'
          ]"
        >
          <component :is="item.icon" class="w-4.5 h-4.5 shrink-0" />
          <span v-if="!collapsed" class="truncate">{{ item.label }}</span>
        </button>
      </nav>

      <div class="border-t border-gray-200 dark:border-gray-700 p-3">
        <div v-if="!collapsed" class="mb-2 px-2">
          <p class="text-xs font-medium text-gray-900 dark:text-white truncate">{{ userName }}</p>
          <p class="text-xs text-gray-500 dark:text-gray-400 truncate">{{ roleLabel }}</p>
        </div>
        <button
          @click="handleLogout"
          class="w-full flex items-center gap-2 px-2 py-1.5 text-xs text-gray-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-colors"
        >
          <LogOut class="w-3.5 h-3.5 shrink-0" />
          <span v-if="!collapsed">退出登录</span>
        </button>
      </div>
    </aside>

    <div class="flex-1 flex flex-col min-w-0">
      <header class="h-14 flex items-center justify-between px-6 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 shrink-0">
        <h1 class="text-base font-semibold text-gray-900 dark:text-white">
          {{ route.meta.title || '文物修复展借与保险核验平台' }}
        </h1>
        <button
          @click="toggleTheme"
          class="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 transition-colors"
        >
          <Sun v-if="isDark" class="w-4 h-4" />
          <Moon v-else class="w-4 h-4" />
        </button>
      </header>

      <main class="flex-1 overflow-auto p-6">
        <slot />
      </main>
    </div>
  </div>
</template>
