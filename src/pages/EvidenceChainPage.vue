<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Link2, Search, Filter, Clock, User, Camera, ArrowRight } from 'lucide-vue-next'
import { api, type EvidenceLog, type Artifact, type PageResult, type ApiResponse } from '@/api'
import { useAuth } from '@/stores/auth'

const router = useRouter()
const { hasRole } = useAuth()

const ACTION_LABEL: Record<string, string> = {
  CREATE: '登记入库',
  RESTORATION_APPROVED: '修复审批',
  RESTORATION_COMPLETED: '修复完成',
  LOAN_APPLY: '借展申请',
  LOAN_APPROVED: '借展批准',
  INSURANCE_VERIFIED: '保险核验',
  TRANSPORT_SEALED: '运输封签',
  EXHIBITION_SETUP: '展厅布置',
  RETURN_RECEIVED: '归还接收',
}

const ACTION_DOT_COLOR: Record<string, string> = {
  CREATE: 'bg-blue-500',
  RESTORATION_APPROVED: 'bg-amber-500',
  RESTORATION_COMPLETED: 'bg-amber-500',
  LOAN_APPLY: 'bg-purple-500',
  LOAN_APPROVED: 'bg-purple-500',
  INSURANCE_VERIFIED: 'bg-green-500',
  TRANSPORT_SEALED: 'bg-orange-500',
  EXHIBITION_SETUP: 'bg-cyan-500',
  RETURN_RECEIVED: 'bg-gray-400 dark:bg-gray-500',
}

const ACTION_BADGE_COLOR: Record<string, string> = {
  CREATE: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
  RESTORATION_APPROVED: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  RESTORATION_COMPLETED: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  LOAN_APPLY: 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400',
  LOAN_APPROVED: 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400',
  INSURANCE_VERIFIED: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
  TRANSPORT_SEALED: 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400',
  EXHIBITION_SETUP: 'bg-cyan-100 text-cyan-700 dark:bg-cyan-900/30 dark:text-cyan-400',
  RETURN_RECEIVED: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300',
}

const ENTITY_LABEL: Record<string, string> = {
  ARTIFACT: '藏品',
  RESTORATION: '修复',
  LOAN: '借展',
  INSURANCE: '保险',
  TRANSPORT: '运输',
  EXHIBITION: '展览',
  RETURN: '归还',
}

const ENTITY_PATH: Record<string, string> = {
  ARTIFACT: '/artifacts',
  RESTORATION: '/restorations',
  LOAN: '/loans',
  INSURANCE: '/insurance',
  TRANSPORT: '/transport',
  EXHIBITION: '/exhibitions',
  RETURN: '/returns',
}

const ACTION_OPTIONS = [
  { value: '', label: '全部操作' },
  { value: 'CREATE', label: '登记入库' },
  { value: 'RESTORATION_APPROVED', label: '修复审批' },
  { value: 'RESTORATION_COMPLETED', label: '修复完成' },
  { value: 'LOAN_APPLY', label: '借展申请' },
  { value: 'LOAN_APPROVED', label: '借展批准' },
  { value: 'INSURANCE_VERIFIED', label: '保险核验' },
  { value: 'TRANSPORT_SEALED', label: '运输封签' },
  { value: 'EXHIBITION_SETUP', label: '展厅布置' },
  { value: 'RETURN_RECEIVED', label: '归还接收' },
]

const keyword = ref('')
const artifactId = ref<number | ''>('')
const action = ref('')
const page = ref(0)
const size = ref(10)
const loading = ref(false)
const data = ref<PageResult<EvidenceLog> | null>(null)
const artifacts = ref<Artifact[]>([])

const totalPages = computed(() => data.value?.totalPages ?? 0)

const pageNumbers = computed(() => {
  const total = totalPages.value
  const current = page.value
  const pages: number[] = []
  const start = Math.max(0, current - 2)
  const end = Math.min(total - 1, current + 2)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

function formatTime(dateStr: string) {
  const d = new Date(dateStr)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function getDotColor(act: string) {
  return ACTION_DOT_COLOR[act] || 'bg-gray-400 dark:bg-gray-500'
}

function getBadgeColor(act: string) {
  return ACTION_BADGE_COLOR[act] || 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300'
}

function getActionLabel(act: string) {
  return ACTION_LABEL[act] || act
}

function getEntityLabel(type: string | null) {
  if (!type) return ''
  return ENTITY_LABEL[type] || type
}

function goEntity(type: string | null, id: number | null) {
  if (!type || !id) return
  const basePath = ENTITY_PATH[type]
  if (basePath) router.push(`${basePath}/${id}`)
}

async function fetchArtifacts() {
  try {
    const res = await api.artifacts.list({ page: 0, size: 200 })
    artifacts.value = res.data.content
  } catch {
    artifacts.value = []
  }
}

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: page.value,
      size: size.value,
    }
    if (artifactId.value !== '') params.artifactId = artifactId.value
    if (action.value) params.action = action.value
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    const res = await api.evidence.list(params)
    data.value = res.data
  } catch {
    data.value = null
  } finally {
    loading.value = false
  }
}

function goPage(p: number) {
  if (p < 0 || p >= totalPages.value) return
  page.value = p
}

function onFilterChange() {
  page.value = 0
  fetchData()
}

onMounted(() => {
  fetchArtifacts()
  fetchData()
})
</script>

<template>
  <div class="space-y-4">
    <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
      <div class="flex items-center gap-2">
        <Link2 class="w-5 h-5 text-amber-600 dark:text-amber-400" />
        <h2 class="text-lg font-semibold text-gray-900 dark:text-white">证据链追溯</h2>
      </div>
      <button
        class="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-200 text-sm font-medium hover:bg-gray-50 dark:hover:bg-gray-600 transition-colors"
      >
        <ArrowRight class="w-4 h-4" />
        导出摘要
      </button>
    </div>

    <div class="flex flex-col sm:flex-row gap-3">
      <div class="relative flex-1">
        <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <input
          v-model="keyword"
          type="text"
          placeholder="按描述或操作人搜索..."
          class="w-full pl-9 pr-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none transition text-sm"
          @keyup.enter="onFilterChange"
        />
      </div>
      <select
        v-model="artifactId"
        @change="onFilterChange"
        class="px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none transition text-sm appearance-none cursor-pointer"
      >
        <option value="">全部藏品</option>
        <option v-for="a in artifacts" :key="a.id" :value="a.id">{{ a.artifactCode }} - {{ a.name }}</option>
      </select>
      <div class="relative">
        <Filter class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <select
          v-model="action"
          @change="onFilterChange"
          class="pl-9 pr-8 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none transition text-sm appearance-none cursor-pointer"
        >
          <option v-for="opt in ACTION_OPTIONS" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
      </div>
    </div>

    <div v-if="loading" class="space-y-4 py-4">
      <div v-for="i in 3" :key="i" class="animate-pulse flex gap-4">
        <div class="flex flex-col items-center">
          <div class="w-3 h-3 rounded-full bg-gray-200 dark:bg-gray-700"></div>
          <div class="w-0.5 h-16 bg-gray-200 dark:bg-gray-700"></div>
        </div>
        <div class="flex-1 space-y-2 py-1">
          <div class="h-4 w-32 bg-gray-200 dark:bg-gray-700 rounded"></div>
          <div class="h-3 w-48 bg-gray-200 dark:bg-gray-700 rounded"></div>
          <div class="h-3 w-24 bg-gray-200 dark:bg-gray-700 rounded"></div>
        </div>
      </div>
    </div>

    <div v-else-if="!data?.content?.length" class="py-16 text-center text-gray-400 dark:text-gray-500">
      暂无证据链记录
    </div>

    <div v-else class="relative pl-6">
      <div class="absolute left-[9px] top-2 bottom-2 w-0.5 bg-gray-200 dark:bg-gray-700"></div>

      <div
        v-for="(log, idx) in data.content"
        :key="log.id"
        class="relative pb-8 last:pb-0"
      >
        <div
          :class="['absolute left-[-15px] top-1.5 w-3 h-3 rounded-full ring-2 ring-white dark:ring-gray-900', getDotColor(log.action)]"
        ></div>

        <div class="rounded-xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 p-4 hover:shadow-md transition-shadow">
          <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-3">
            <div class="flex items-center gap-2 flex-wrap">
              <span :class="['inline-flex items-center px-2 py-0.5 rounded text-xs font-medium', getBadgeColor(log.action)]">
                {{ getActionLabel(log.action) }}
              </span>
              <span v-if="log.fromStatus || log.toStatus" class="inline-flex items-center gap-1 text-xs">
                <span v-if="log.fromStatus" class="px-1.5 py-0.5 rounded bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300">{{ log.fromStatus }}</span>
                <ArrowRight v-if="log.fromStatus && log.toStatus" class="w-3 h-3 text-gray-400" />
                <span v-if="log.toStatus" class="px-1.5 py-0.5 rounded bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400">{{ log.toStatus }}</span>
              </span>
            </div>
            <div class="flex items-center gap-1.5 text-xs text-gray-500 dark:text-gray-400">
              <Clock class="w-3.5 h-3.5" />
              {{ formatTime(log.createdAt) }}
            </div>
          </div>

          <div class="flex items-center gap-1.5 text-sm text-gray-700 dark:text-gray-300 mb-2">
            <User class="w-3.5 h-3.5 text-gray-400" />
            <span>{{ log.operatorName || `用户#${log.operatorId}` }}</span>
          </div>

          <p v-if="log.description" class="text-sm text-gray-600 dark:text-gray-400 mb-2">{{ log.description }}</p>

          <div class="flex flex-wrap items-center gap-3 text-xs">
            <button
              v-if="log.relatedEntityType && log.relatedEntityId"
              @click="goEntity(log.relatedEntityType, log.relatedEntityId)"
              class="inline-flex items-center gap-1 px-2 py-1 rounded-md bg-gray-50 dark:bg-gray-700/50 text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/20 transition-colors"
            >
              <Link2 class="w-3 h-3" />
              {{ getEntityLabel(log.relatedEntityType) }} #{{ log.relatedEntityId }}
            </button>

            <span
              v-if="log.responsibilityImpact"
              class="inline-flex items-center gap-1 px-2 py-1 rounded-md bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400"
            >
              责任影响：{{ log.responsibilityImpact }}
            </span>

            <span
              v-if="log.photoUrl"
              class="inline-flex items-center gap-1 px-2 py-1 rounded-md bg-gray-50 dark:bg-gray-700/50 text-gray-500 dark:text-gray-400"
            >
              <Camera class="w-3 h-3" />
              含照片记录
            </span>
          </div>
        </div>
      </div>
    </div>

    <div
      v-if="data && data.totalPages > 1"
      class="flex items-center justify-between px-1 py-3"
    >
      <p class="text-xs text-gray-500 dark:text-gray-400">
        共 {{ data.totalElements }} 条，第 {{ data.number + 1 }} / {{ data.totalPages }} 页
      </p>
      <div class="flex items-center gap-1">
        <button
          @click="goPage(page - 1)"
          :disabled="page === 0"
          class="px-3 py-1.5 rounded-lg text-xs border border-gray-300 dark:border-gray-600 hover:bg-gray-100 dark:hover:bg-gray-700 disabled:opacity-40 disabled:cursor-not-allowed text-gray-700 dark:text-gray-300 transition-colors"
        >
          上一页
        </button>
        <button
          v-for="p in pageNumbers"
          :key="p"
          @click="goPage(p)"
          :class="[
            'px-3 py-1.5 rounded-lg text-xs border transition-colors',
            p === page
              ? 'bg-amber-600 border-amber-600 text-white'
              : 'border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'
          ]"
        >
          {{ p + 1 }}
        </button>
        <button
          @click="goPage(page + 1)"
          :disabled="page >= totalPages - 1"
          class="px-3 py-1.5 rounded-lg text-xs border border-gray-300 dark:border-gray-600 hover:bg-gray-100 dark:hover:bg-gray-700 disabled:opacity-40 disabled:cursor-not-allowed text-gray-700 dark:text-gray-300 transition-colors"
        >
          下一页
        </button>
      </div>
    </div>
  </div>
</template>
