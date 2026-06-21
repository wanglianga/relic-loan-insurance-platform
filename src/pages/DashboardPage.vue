<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { useAuth } from '@/stores/auth'
import {
  Archive,
  Wrench,
  FileText,
  Shield,
  Package,
  RotateCcw,
  Clock,
  AlertTriangle,
  CheckCircle2,
} from 'lucide-vue-next'
import type { ApiResponse, PageResult, EvidenceLog } from '@/api'

const router = useRouter()
const { userName, roleLabel, userRole } = useAuth()

const loading = ref(true)
const totalArtifacts = ref(0)
const activeRestorations = ref(0)
const pendingLoans = ref(0)
const activeExhibitions = ref(0)
const recentEvidence = ref<EvidenceLog[]>([])

const ARTIFACT_STATUS_MAP: Record<string, string> = {
  REGISTERED: '已登记',
  UNDER_RESTORATION: '修复中',
  LOAN_PENDING: '借展待批',
  ON_LOAN: '借出中',
  RETURNED: '已归还',
}

const LOAN_STATUS_MAP: Record<string, string> = {
  PENDING: '待审批',
  APPROVED: '已批准',
  REJECTED: '已驳回',
  ACTIVE: '进行中',
  COMPLETED: '已完成',
}

const EVIDENCE_ACTION_MAP: Record<string, string> = {
  CREATE: '登记',
  RESTORATION_APPROVED: '修复审批',
  RESTORATION_COMPLETED: '修复完成',
  LOAN_APPLY: '借展申请',
  LOAN_APPROVED: '借展批准',
  INSURANCE_VERIFIED: '保险核验',
  TRANSPORT_SEALED: '运输封签',
  EXHIBITION_SETUP: '展厅布置',
  RETURN_RECEIVED: '归还接收',
}

const ROLE_ACTIONS: Record<string, { label: string; icon: any; path: string }[]> = {
  COLLECTION: [
    { label: '登记藏品', icon: Archive, path: '/artifacts' },
    { label: '审批借展', icon: FileText, path: '/loans/approval' },
  ],
  RESTORER: [
    { label: '创建修复', icon: Wrench, path: '/restorations' },
  ],
  BORROWER: [
    { label: '申请借展', icon: FileText, path: '/loans/apply' },
  ],
  INSURER: [
    { label: '核验保险', icon: Shield, path: '/insurance' },
  ],
  SECURITY: [
    { label: '包装出库', icon: Package, path: '/packaging' },
  ],
  TRANSPORT: [
    { label: '运输监管', icon: RotateCcw, path: '/transport' },
  ],
}

const quickActions = computed(() => ROLE_ACTIONS[userRole.value] || [])

const statsCards = computed(() => [
  { label: '藏品总数', value: totalArtifacts.value, icon: Archive, color: 'text-amber-600 dark:text-amber-400', bg: 'bg-amber-50 dark:bg-amber-900/20' },
  { label: '修复中', value: activeRestorations.value, icon: Wrench, color: 'text-blue-600 dark:text-blue-400', bg: 'bg-blue-50 dark:bg-blue-900/20' },
  { label: '待审批借展', value: pendingLoans.value, icon: FileText, color: 'text-orange-600 dark:text-orange-400', bg: 'bg-orange-50 dark:bg-orange-900/20' },
  { label: '进行中展览', value: activeExhibitions.value, icon: Shield, color: 'text-emerald-600 dark:text-emerald-400', bg: 'bg-emerald-50 dark:bg-emerald-900/20' },
])

function formatTime(dateStr: string) {
  const d = new Date(dateStr)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function getActionLabel(action: string) {
  return EVIDENCE_ACTION_MAP[action] || action
}

function getStatusColor(status: string) {
  const map: Record<string, string> = {
    REGISTERED: 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300',
    UNDER_RESTORATION: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
    LOAN_PENDING: 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400',
    ON_LOAN: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
    RETURNED: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400',
  }
  return map[status] || 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300'
}

function getLoanStatusColor(status: string) {
  const map: Record<string, string> = {
    PENDING: 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400',
    APPROVED: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400',
    REJECTED: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
    ACTIVE: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
    COMPLETED: 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300',
  }
  return map[status] || 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300'
}

onMounted(async () => {
  loading.value = true
  try {
    const [artifactsRes, restorationsRes, loansRes, exhibitionsRes, evidenceRes] = await Promise.allSettled([
      api.artifacts.list({ page: 0, size: 1 }),
      api.restorations.list({ page: 0, size: 1 }),
      api.loans.list({ page: 0, size: 1, status: 'PENDING' }),
      api.exhibitions.list({ page: 0, size: 1, status: 'ACTIVE' }),
      api.evidence.list({ page: 0, size: 5, sort: 'createdAt,desc' }),
    ])

    if (artifactsRes.status === 'fulfilled') totalArtifacts.value = artifactsRes.value.data.totalElements
    if (restorationsRes.status === 'fulfilled') activeRestorations.value = restorationsRes.value.data.totalElements
    if (loansRes.status === 'fulfilled') pendingLoans.value = loansRes.value.data.totalElements
    if (exhibitionsRes.status === 'fulfilled') activeExhibitions.value = exhibitionsRes.value.data.totalElements
    if (evidenceRes.status === 'fulfilled') recentEvidence.value = evidenceRes.value.data.content
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-bold text-gray-900 dark:text-white">
          欢迎回来，{{ userName }}
        </h2>
        <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
          当前角色：{{ roleLabel }} · 文物修复展借与保险核验平台
        </p>
      </div>
      <div class="flex items-center gap-2 text-amber-600 dark:text-amber-400">
        <Archive class="w-5 h-5" />
        <span class="text-sm font-medium">工作台</span>
      </div>
    </div>

    <div v-if="loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <div v-for="i in 4" :key="i" class="animate-pulse rounded-xl bg-white dark:bg-gray-800 p-5 border border-gray-200 dark:border-gray-700">
        <div class="h-4 w-20 bg-gray-200 dark:bg-gray-700 rounded mb-3"></div>
        <div class="h-8 w-16 bg-gray-200 dark:bg-gray-700 rounded"></div>
      </div>
    </div>

    <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <div
        v-for="card in statsCards"
        :key="card.label"
        class="rounded-xl bg-white dark:bg-gray-800 p-5 border border-gray-200 dark:border-gray-700 hover:shadow-md transition-shadow"
      >
        <div class="flex items-center justify-between mb-3">
          <span class="text-sm text-gray-500 dark:text-gray-400">{{ card.label }}</span>
          <div :class="[card.bg, 'p-2 rounded-lg']">
            <component :is="card.icon" :class="['w-4 h-4', card.color]" />
          </div>
        </div>
        <p :class="['text-2xl font-bold', card.color]">{{ card.value }}</p>
      </div>
    </div>

    <div v-if="quickActions.length > 0" class="rounded-xl bg-white dark:bg-gray-800 p-5 border border-gray-200 dark:border-gray-700">
      <h3 class="text-sm font-semibold text-gray-900 dark:text-white mb-4">快捷操作</h3>
      <div class="flex flex-wrap gap-3">
        <button
          v-for="action in quickActions"
          :key="action.label"
          @click="router.push(action.path)"
          class="inline-flex items-center gap-2 px-4 py-2.5 rounded-lg bg-amber-50 dark:bg-amber-900/20 text-amber-700 dark:text-amber-400 text-sm font-medium hover:bg-amber-100 dark:hover:bg-amber-900/30 border border-amber-200 dark:border-amber-800 transition-colors"
        >
          <component :is="action.icon" class="w-4 h-4" />
          {{ action.label }}
        </button>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div class="rounded-xl bg-white dark:bg-gray-800 p-5 border border-gray-200 dark:border-gray-700">
        <div class="flex items-center gap-2 mb-4">
          <Clock class="w-4 h-4 text-amber-600 dark:text-amber-400" />
          <h3 class="text-sm font-semibold text-gray-900 dark:text-white">最近操作记录</h3>
        </div>
        <div v-if="loading" class="space-y-3">
          <div v-for="i in 5" :key="i" class="animate-pulse flex items-center gap-3">
            <div class="w-8 h-8 bg-gray-200 dark:bg-gray-700 rounded-full shrink-0"></div>
            <div class="flex-1">
              <div class="h-3 w-24 bg-gray-200 dark:bg-gray-700 rounded mb-2"></div>
              <div class="h-3 w-40 bg-gray-200 dark:bg-gray-700 rounded"></div>
            </div>
          </div>
        </div>
        <div v-else-if="recentEvidence.length === 0" class="py-8 text-center text-sm text-gray-400 dark:text-gray-500">
          暂无操作记录
        </div>
        <div v-else class="space-y-3">
          <div
            v-for="log in recentEvidence"
            :key="log.id"
            class="flex items-start gap-3 p-3 rounded-lg bg-gray-50 dark:bg-gray-700/50"
          >
            <div class="mt-0.5 p-1.5 rounded-full bg-amber-100 dark:bg-amber-900/30">
              <CheckCircle2 class="w-3.5 h-3.5 text-amber-600 dark:text-amber-400" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <span class="text-sm font-medium text-gray-900 dark:text-white">{{ log.artifactName || `藏品#${log.artifactId}` }}</span>
                <span class="inline-flex items-center px-1.5 py-0.5 rounded text-xs font-medium bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400">
                  {{ getActionLabel(log.action) }}
                </span>
              </div>
              <div class="mt-1 flex items-center gap-2 text-xs text-gray-500 dark:text-gray-400">
                <span>{{ log.operatorName || `用户#${log.operatorId}` }}</span>
                <span>·</span>
                <span>{{ formatTime(log.createdAt) }}</span>
              </div>
              <div v-if="log.fromStatus || log.toStatus" class="mt-1 flex items-center gap-1 text-xs">
                <span v-if="log.fromStatus" :class="['px-1.5 py-0.5 rounded', getStatusColor(log.fromStatus)]">{{ ARTIFACT_STATUS_MAP[log.fromStatus] || log.fromStatus }}</span>
                <span v-if="log.fromStatus && log.toStatus" class="text-gray-400">→</span>
                <span v-if="log.toStatus" :class="['px-1.5 py-0.5 rounded', getStatusColor(log.toStatus)]">{{ ARTIFACT_STATUS_MAP[log.toStatus] || log.toStatus }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="space-y-6">
        <div class="rounded-xl bg-white dark:bg-gray-800 p-5 border border-gray-200 dark:border-gray-700">
          <div class="flex items-center gap-2 mb-4">
            <AlertTriangle class="w-4 h-4 text-amber-600 dark:text-amber-400" />
            <h3 class="text-sm font-semibold text-gray-900 dark:text-white">藏品状态分布</h3>
          </div>
          <div class="space-y-2">
            <div
              v-for="(label, key) in ARTIFACT_STATUS_MAP"
              :key="key"
              class="flex items-center justify-between py-1.5"
            >
              <span :class="['inline-flex items-center px-2 py-0.5 rounded text-xs font-medium', getStatusColor(key)]">{{ label }}</span>
              <div class="w-32 h-2 bg-gray-100 dark:bg-gray-700 rounded-full overflow-hidden">
                <div
                  class="h-full rounded-full transition-all duration-500"
                  :class="{
                    'bg-gray-400 dark:bg-gray-500': key === 'REGISTERED',
                    'bg-blue-500': key === 'UNDER_RESTORATION',
                    'bg-orange-500': key === 'LOAN_PENDING',
                    'bg-amber-500': key === 'ON_LOAN',
                    'bg-emerald-500': key === 'RETURNED',
                  }"
                  :style="{ width: totalArtifacts > 0 ? '30%' : '0%' }"
                ></div>
              </div>
            </div>
          </div>
        </div>

        <div class="rounded-xl bg-white dark:bg-gray-800 p-5 border border-gray-200 dark:border-gray-700">
          <div class="flex items-center gap-2 mb-4">
            <FileText class="w-4 h-4 text-amber-600 dark:text-amber-400" />
            <h3 class="text-sm font-semibold text-gray-900 dark:text-white">借展状态分布</h3>
          </div>
          <div class="space-y-2">
            <div
              v-for="(label, key) in LOAN_STATUS_MAP"
              :key="key"
              class="flex items-center justify-between py-1.5"
            >
              <span :class="['inline-flex items-center px-2 py-0.5 rounded text-xs font-medium', getLoanStatusColor(key)]">{{ label }}</span>
              <div class="w-32 h-2 bg-gray-100 dark:bg-gray-700 rounded-full overflow-hidden">
                <div
                  class="h-full rounded-full transition-all duration-500"
                  :class="{
                    'bg-orange-500': key === 'PENDING',
                    'bg-emerald-500': key === 'APPROVED',
                    'bg-red-500': key === 'REJECTED',
                    'bg-amber-500': key === 'ACTIVE',
                    'bg-gray-400 dark:bg-gray-500': key === 'COMPLETED',
                  }"
                  :style="{ width: pendingLoans > 0 ? '30%' : '0%' }"
                ></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
