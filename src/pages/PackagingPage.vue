<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { Package, Check, Box, Lock } from 'lucide-vue-next'
import { api, type Transport, type Loan, type PageResult, type ApiResponse } from '@/api'
import { useAuth } from '@/stores/auth'

const { hasRole, user } = useAuth()

const STATUS_MAP: Record<string, string> = {
  PREPARING: '准备中',
  SEALED: '已封签',
  IN_TRANSIT: '运输中',
  ARRIVED: '已到达',
}

const STATUS_COLORS: Record<string, string> = {
  PREPARING: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  SEALED: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
  IN_TRANSIT: 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400',
  ARRIVED: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
}

const STATUSES = [
  { value: '', label: '全部状态' },
  { value: 'PREPARING', label: '准备中' },
  { value: 'SEALED', label: '已封签' },
  { value: 'IN_TRANSIT', label: '运输中' },
  { value: 'ARRIVED', label: '已到达' },
]

const transports = ref<Transport[]>([])
const totalElements = ref(0)
const page = ref(0)
const size = ref(12)
const loading = ref(false)
const statusFilter = ref('')

const showCreateModal = ref(false)
const createSubmitting = ref(false)
const createForm = ref({
  loanId: '' as number | '',
  packagingStandard: '',
  boxCode: '',
  boxSpec: '',
  innerMaterial: '',
  route: '',
})

const showSealModal = ref(false)
const sealSubmitting = ref(false)
const sealingTransportId = ref<number | null>(null)
const sealForm = ref({ sealCode: '' })

const approvedLoans = ref<Loan[]>([])
const loansMap = ref<Record<number, Loan>>({})

const totalPages = computed(() => Math.ceil(totalElements.value / size.value))

async function loadTransports() {
  loading.value = true
  try {
    const params: Record<string, any> = { page: page.value, size: size.value }
    if (statusFilter.value) params.status = statusFilter.value
    const res = await api.transports.list(params)
    transports.value = res.data.content
    totalElements.value = res.data.totalElements
  } catch {
    transports.value = []
    totalElements.value = 0
  } finally {
    loading.value = false
  }
}

async function loadApprovedLoans() {
  try {
    const res = await api.loans.list({ page: 0, size: 200, status: 'APPROVED' })
    approvedLoans.value = res.data.content
    const map: Record<number, Loan> = {}
    for (const loan of approvedLoans.value) {
      map[loan.id] = loan
    }
    const allRes = await api.loans.list({ page: 0, size: 200 })
    for (const loan of allRes.data.content) {
      map[loan.id] = loan
    }
    loansMap.value = map
  } catch {
    approvedLoans.value = []
  }
}

const loansWithoutTransport = computed(() => {
  const transportLoanIds = new Set(transports.value.map(t => t.loanId))
  return approvedLoans.value.filter(l => !transportLoanIds.has(l.id))
})

function openCreateModal() {
  createForm.value = {
    loanId: '',
    packagingStandard: '',
    boxCode: '',
    boxSpec: '',
    innerMaterial: '',
    route: '',
  }
  showCreateModal.value = true
}

async function submitCreate() {
  if (createForm.value.loanId === '' || !createForm.value.boxCode) return
  createSubmitting.value = true
  try {
    await api.transports.create({
      loanId: createForm.value.loanId as number,
      packagingStandard: createForm.value.packagingStandard || null,
      boxCode: createForm.value.boxCode,
      boxSpec: createForm.value.boxSpec || null,
      innerMaterial: createForm.value.innerMaterial || null,
      route: createForm.value.route || null,
    })
    showCreateModal.value = false
    await loadTransports()
  } catch {
  } finally {
    createSubmitting.value = false
  }
}

function openSealModal(id: number) {
  sealingTransportId.value = id
  sealForm.value = { sealCode: '' }
  showSealModal.value = true
}

async function submitSeal() {
  if (!sealForm.value.sealCode || sealingTransportId.value === null) return
  sealSubmitting.value = true
  try {
    await api.transports.seal(sealingTransportId.value, {
      sealCode: sealForm.value.sealCode,
      sealedBy: user.value?.id || null,
    })
    showSealModal.value = false
    await loadTransports()
  } catch {
  } finally {
    sealSubmitting.value = false
  }
}

function changePage(delta: number) {
  const next = page.value + delta
  if (next >= 0 && next < totalPages.value) {
    page.value = next
    loadTransports()
  }
}

onMounted(() => {
  loadApprovedLoans()
  loadTransports()
})

watch(statusFilter, () => {
  page.value = 0
  loadTransports()
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="p-2 rounded-lg bg-amber-100 dark:bg-amber-900/30">
          <Package class="w-5 h-5 text-amber-600 dark:text-amber-400" />
        </div>
        <div>
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white">包装出库</h2>
          <p class="text-sm text-gray-500 dark:text-gray-400">文物包装封签与运输出库管理</p>
        </div>
      </div>
      <button
        v-if="hasRole('SECURITY')"
        @click="openCreateModal"
        class="flex items-center gap-2 px-4 py-2 rounded-lg bg-amber-600 hover:bg-amber-700 text-white text-sm font-medium transition-colors"
      >
        <Package class="w-4 h-4" />
        创建运输
      </button>
    </div>

    <div class="flex items-center gap-3">
      <select
        v-model="statusFilter"
        class="px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
      >
        <option v-for="s in STATUSES" :key="s.value" :value="s.value">{{ s.label }}</option>
      </select>
    </div>

    <div v-if="loading" class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
      <div v-for="i in 6" :key="i" class="animate-pulse rounded-xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 p-5">
        <div class="h-4 w-24 bg-gray-200 dark:bg-gray-700 rounded mb-3" />
        <div class="h-3 w-32 bg-gray-200 dark:bg-gray-700 rounded mb-2" />
        <div class="h-3 w-20 bg-gray-200 dark:bg-gray-700 rounded" />
      </div>
    </div>

    <div v-else-if="transports.length === 0" class="py-12 text-center text-sm text-gray-400 dark:text-gray-500">
      暂无运输记录
    </div>

    <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
      <div
        v-for="t in transports"
        :key="t.id"
        class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5 hover:shadow-md transition-shadow"
      >
        <div class="flex items-center justify-between mb-3">
          <span :class="['inline-flex px-2.5 py-0.5 rounded-full text-xs font-medium', STATUS_COLORS[t.status] || '']">
            {{ STATUS_MAP[t.status] || t.status }}
          </span>
          <span class="text-xs text-gray-400 dark:text-gray-500 font-mono">#{{ t.id }}</span>
        </div>

        <div class="space-y-2 mb-4">
          <div class="flex items-center gap-2">
            <Box class="w-4 h-4 text-gray-400" />
            <span class="text-sm font-medium text-gray-900 dark:text-white">{{ loansMap[t.loanId]?.artifactName || `文物#${t.loanId}` }}</span>
          </div>
          <p class="text-xs text-gray-500 dark:text-gray-400 ml-6">{{ loansMap[t.loanId]?.borrowingInstitution || '-' }}</p>
        </div>

        <div class="space-y-1.5 text-sm border-t border-gray-100 dark:border-gray-700 pt-3 mb-3">
          <div class="flex justify-between">
            <span class="text-gray-500 dark:text-gray-400">包装标准</span>
            <span class="text-gray-900 dark:text-white">{{ t.packagingStandard || '-' }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-500 dark:text-gray-400">箱号</span>
            <span class="text-gray-900 dark:text-white font-mono">{{ t.boxCode || '-' }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-500 dark:text-gray-400">箱体规格</span>
            <span class="text-gray-900 dark:text-white">{{ t.boxSpec || '-' }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-500 dark:text-gray-400">内衬材料</span>
            <span class="text-gray-900 dark:text-white">{{ t.innerMaterial || '-' }}</span>
          </div>
        </div>

        <div v-if="t.sealCode" class="space-y-1.5 text-sm border-t border-gray-100 dark:border-gray-700 pt-3 mb-3">
          <div class="flex items-center gap-2 mb-1">
            <Lock class="w-3.5 h-3.5 text-blue-500" />
            <span class="text-xs font-medium text-blue-600 dark:text-blue-400">封签信息</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-500 dark:text-gray-400">封签码</span>
            <span class="text-gray-900 dark:text-white font-mono">{{ t.sealCode }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-500 dark:text-gray-400">封签人</span>
            <span class="text-gray-900 dark:text-white">{{ t.sealerName || '-' }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-500 dark:text-gray-400">封签时间</span>
            <span class="text-gray-900 dark:text-white text-xs">{{ t.sealedAt?.substring(0, 16)?.replace('T', ' ') || '-' }}</span>
          </div>
        </div>

        <div v-if="t.escortName" class="space-y-1.5 text-sm border-t border-gray-100 dark:border-gray-700 pt-3 mb-3">
          <div class="flex justify-between">
            <span class="text-gray-500 dark:text-gray-400">押运员</span>
            <span class="text-gray-900 dark:text-white">{{ t.escortName }}</span>
          </div>
        </div>

        <div v-if="t.route" class="space-y-1.5 text-sm border-t border-gray-100 dark:border-gray-700 pt-3 mb-3">
          <div class="flex justify-between">
            <span class="text-gray-500 dark:text-gray-400">路线</span>
            <span class="text-gray-900 dark:text-white text-right max-w-[60%] truncate">{{ t.route }}</span>
          </div>
        </div>

        <div v-if="t.status === 'PREPARING' && hasRole('SECURITY')" class="pt-3 border-t border-gray-100 dark:border-gray-700">
          <button
            @click="openSealModal(t.id)"
            class="w-full flex items-center justify-center gap-2 px-3 py-2 rounded-lg bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium transition-colors"
          >
            <Lock class="w-4 h-4" />
            封签
          </button>
        </div>
      </div>
    </div>

    <div v-if="totalPages > 1" class="flex items-center justify-between px-1">
      <span class="text-sm text-gray-500 dark:text-gray-400">共 {{ totalElements }} 条</span>
      <div class="flex items-center gap-2">
        <button
          :disabled="page === 0"
          @click="changePage(-1)"
          class="px-3 py-1 rounded-lg border border-gray-300 dark:border-gray-600 text-sm disabled:opacity-40 hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300 transition-colors"
        >
          上一页
        </button>
        <span class="text-sm text-gray-700 dark:text-gray-300">{{ page + 1 }} / {{ totalPages }}</span>
        <button
          :disabled="page >= totalPages - 1"
          @click="changePage(1)"
          class="px-3 py-1 rounded-lg border border-gray-300 dark:border-gray-600 text-sm disabled:opacity-40 hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300 transition-colors"
        >
          下一页
        </button>
      </div>
    </div>

    <div v-if="showCreateModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50" @click.self="showCreateModal = false">
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-lg p-6">
        <div class="flex items-center gap-2 mb-5">
          <Package class="w-5 h-5 text-amber-600" />
          <h3 class="text-base font-semibold text-gray-900 dark:text-white">创建运输记录</h3>
        </div>
        <form @submit.prevent="submitCreate" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">借展申请</label>
            <select
              v-model="createForm.loanId"
              required
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
            >
              <option value="" disabled>请选择已批准的借展申请</option>
              <option v-for="l in loansWithoutTransport" :key="l.id" :value="l.id">{{ l.artifactName || `文物#${l.artifactId}` }} → {{ l.borrowingInstitution }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">包装标准</label>
            <input
              v-model="createForm.packagingStandard"
              type="text"
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
            />
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">箱号</label>
              <input
                v-model="createForm.boxCode"
                type="text"
                required
                class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">箱体规格</label>
              <input
                v-model="createForm.boxSpec"
                type="text"
                class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">内衬材料</label>
            <input
              v-model="createForm.innerMaterial"
              type="text"
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">运输路线</label>
            <input
              v-model="createForm.route"
              type="text"
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
            />
          </div>
          <div class="flex justify-end gap-3 pt-2">
            <button
              type="button"
              @click="showCreateModal = false"
              class="px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
            >
              取消
            </button>
            <button
              type="submit"
              :disabled="createSubmitting"
              class="px-4 py-2 rounded-lg bg-amber-600 hover:bg-amber-700 disabled:bg-amber-400 text-white text-sm font-medium transition-colors"
            >
              {{ createSubmitting ? '提交中...' : '创建' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="showSealModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50" @click.self="showSealModal = false">
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-md p-6">
        <div class="flex items-center gap-2 mb-5">
          <Lock class="w-5 h-5 text-blue-600" />
          <h3 class="text-base font-semibold text-gray-900 dark:text-white">运输封签</h3>
        </div>
        <form @submit.prevent="submitSeal" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">封签码</label>
            <input
              v-model="sealForm.sealCode"
              type="text"
              required
              placeholder="请输入封签码"
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div class="flex justify-end gap-3 pt-2">
            <button
              type="button"
              @click="showSealModal = false"
              class="px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
            >
              取消
            </button>
            <button
              type="submit"
              :disabled="sealSubmitting"
              class="px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white text-sm font-medium transition-colors"
            >
              {{ sealSubmitting ? '提交中...' : '确认封签' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
