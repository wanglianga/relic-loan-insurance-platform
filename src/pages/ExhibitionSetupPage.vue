<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { Monitor, Check, Settings } from 'lucide-vue-next'
import { api, type Exhibition, type Loan, type PageResult, type ApiResponse } from '@/api'
import { useAuth } from '@/stores/auth'

const { hasRole } = useAuth()

const STATUS_LABELS: Record<string, string> = {
  PENDING: '待布置',
  ACTIVE: '展出中',
  COMPLETED: '已结束',
}

const STATUS_COLORS: Record<string, string> = {
  PENDING: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  ACTIVE: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
  COMPLETED: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300',
}

const STATUSES = [
  { value: '', label: '全部状态' },
  { value: 'PENDING', label: '待布置' },
  { value: 'ACTIVE', label: '展出中' },
  { value: 'COMPLETED', label: '已结束' },
]

const exhibitions = ref<Exhibition[]>([])
const loans = ref<Loan[]>([])
const totalElements = ref(0)
const page = ref(0)
const size = ref(10)
const loading = ref(false)
const filterStatus = ref('')
const showSetupModal = ref(false)
const submitting = ref(false)
const confirmingId = ref<number | null>(null)

const form = ref({
  loanId: '' as number | '',
  venue: '',
  showcaseCode: '',
})

const totalPages = computed(() => Math.ceil(totalElements.value / size.value))

const eligibleLoans = computed(() => {
  return loans.value.filter(l =>
    l.transport?.status === 'ARRIVED' && !l.exhibition
  )
})

async function loadExhibitions() {
  loading.value = true
  try {
    const params: Record<string, any> = { page: page.value, size: size.value }
    if (filterStatus.value) params.status = filterStatus.value
    const res = await api.exhibitions.list(params)
    exhibitions.value = res.data.content
    totalElements.value = res.data.totalElements
  } catch {
    exhibitions.value = []
    totalElements.value = 0
  } finally {
    loading.value = false
  }
}

async function loadLoans() {
  try {
    const res = await api.loans.list({ page: 0, size: 200 })
    loans.value = res.data.content
  } catch {
    loans.value = []
  }
}

function handleFilter() {
  page.value = 0
  loadExhibitions()
}

function changePage(delta: number) {
  const next = page.value + delta
  if (next >= 0 && next < totalPages.value) {
    page.value = next
    loadExhibitions()
  }
}

function openSetupModal() {
  form.value = { loanId: '', venue: '', showcaseCode: '' }
  showSetupModal.value = true
}

async function submitSetup() {
  if (form.value.loanId === '' || !form.value.venue || !form.value.showcaseCode) return
  submitting.value = true
  try {
    await api.exhibitions.setup({
      loanId: form.value.loanId as number,
      venue: form.value.venue,
      showcaseCode: form.value.showcaseCode,
    })
    showSetupModal.value = false
    await loadExhibitions()
    await loadLoans()
  } catch {
  } finally {
    submitting.value = false
  }
}

async function confirmSetup(id: number) {
  confirmingId.value = id
  try {
    await api.exhibitions.confirmSetup(id)
    await loadExhibitions()
  } catch {
  } finally {
    confirmingId.value = null
  }
}

function getLoanForExhibition(exhibition: Exhibition): Loan | undefined {
  return loans.value.find(l => l.id === exhibition.loanId)
}

onMounted(() => {
  loadLoans()
  loadExhibitions()
})

watch(filterStatus, () => {
  page.value = 0
  loadExhibitions()
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="p-2 rounded-lg bg-purple-100 dark:bg-purple-900/30">
          <Monitor class="w-5 h-5 text-purple-600 dark:text-purple-400" />
        </div>
        <div>
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white">展陈布置</h2>
          <p class="text-sm text-gray-500 dark:text-gray-400">展览布置与确认管理</p>
        </div>
      </div>
      <button
        v-if="hasRole('SECURITY')"
        @click="openSetupModal"
        class="flex items-center gap-2 px-4 py-2 rounded-lg bg-amber-600 hover:bg-amber-700 text-white text-sm font-medium transition-colors"
      >
        <Settings class="w-4 h-4" />
        布置展览
      </button>
    </div>

    <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
      <div class="flex items-center gap-4">
        <select
          v-model="filterStatus"
          class="px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
        >
          <option v-for="s in STATUSES" :key="s.value" :value="s.value">{{ s.label }}</option>
        </select>
      </div>
    </div>

    <div v-if="loading" class="text-center py-8 text-gray-400 dark:text-gray-500">加载中...</div>
    <div v-else-if="exhibitions.length === 0" class="text-center py-8 text-gray-400 dark:text-gray-500">暂无展陈记录</div>
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      <div
        v-for="ex in exhibitions"
        :key="ex.id"
        class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5 space-y-3"
      >
        <div class="flex items-center justify-between">
          <span :class="['inline-flex px-2.5 py-0.5 rounded-full text-xs font-medium', STATUS_COLORS[ex.status] || STATUS_COLORS.COMPLETED]">
            {{ STATUS_LABELS[ex.status] || ex.status }}
          </span>
          <span v-if="ex.setupAt" class="text-xs text-gray-400 dark:text-gray-500">{{ ex.setupAt.substring(0, 10) }}</span>
        </div>

        <div class="space-y-2">
          <div class="flex items-center gap-2">
            <span class="text-xs text-gray-500 dark:text-gray-400 w-16">场馆</span>
            <span class="text-sm text-gray-900 dark:text-white">{{ ex.venue || '-' }}</span>
          </div>
          <div class="flex items-center gap-2">
            <span class="text-xs text-gray-500 dark:text-gray-400 w-16">展柜号</span>
            <span class="text-sm font-mono text-gray-900 dark:text-white">{{ ex.showcaseCode || '-' }}</span>
          </div>
          <div class="flex items-center gap-2">
            <span class="text-xs text-gray-500 dark:text-gray-400 w-16">布置日期</span>
            <span class="text-sm text-gray-900 dark:text-white">{{ ex.setupAt ? ex.setupAt.substring(0, 10) : '-' }}</span>
          </div>
        </div>

        <div class="border-t border-gray-100 dark:border-gray-700 pt-3 space-y-1.5">
          <p class="text-xs font-medium text-gray-500 dark:text-gray-400">借展信息</p>
          <div class="flex items-center gap-2">
            <span class="text-xs text-gray-500 dark:text-gray-400 w-16">文物</span>
            <span class="text-sm text-gray-900 dark:text-white">{{ getLoanForExhibition(ex)?.artifactName || '-' }}</span>
          </div>
          <div class="flex items-center gap-2">
            <span class="text-xs text-gray-500 dark:text-gray-400 w-16">借入方</span>
            <span class="text-sm text-gray-900 dark:text-white">{{ getLoanForExhibition(ex)?.borrowingInstitution || '-' }}</span>
          </div>
        </div>

        <div v-if="hasRole('SECURITY') && ex.status === 'PENDING'" class="pt-2">
          <button
            :disabled="confirmingId === ex.id"
            @click="confirmSetup(ex.id)"
            class="w-full flex items-center justify-center gap-2 px-4 py-2 rounded-lg bg-green-600 hover:bg-green-700 disabled:bg-green-400 text-white text-sm font-medium transition-colors"
          >
            <Check class="w-4 h-4" />
            {{ confirmingId === ex.id ? '确认中...' : '确认布置' }}
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="totalPages > 1"
      class="flex items-center justify-between bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 px-4 py-3"
    >
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

    <div v-if="showSetupModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50" @click.self="showSetupModal = false">
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-lg p-6">
        <div class="flex items-center gap-2 mb-5">
          <Settings class="w-5 h-5 text-amber-600" />
          <h3 class="text-base font-semibold text-gray-900 dark:text-white">布置展览</h3>
        </div>
        <form @submit.prevent="submitSetup" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">借展记录</label>
            <select
              v-model="form.loanId"
              required
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
            >
              <option value="" disabled>请选择已到运输的借展记录</option>
              <option v-for="l in eligibleLoans" :key="l.id" :value="l.id">
                {{ l.artifactName || '文物#' + l.artifactId }} - {{ l.borrowingInstitution }}
              </option>
            </select>
            <p v-if="eligibleLoans.length === 0" class="mt-1 text-xs text-gray-400 dark:text-gray-500">暂无可布置的借展记录（需运输已到达且未布置展览）</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">场馆</label>
            <input
              v-model="form.venue"
              type="text"
              required
              placeholder="请输入场馆名称"
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">展柜编号</label>
            <input
              v-model="form.showcaseCode"
              type="text"
              required
              placeholder="请输入展柜编号"
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
            />
          </div>
          <div class="flex justify-end gap-3 pt-2">
            <button
              type="button"
              @click="showSetupModal = false"
              class="px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
            >
              取消
            </button>
            <button
              type="submit"
              :disabled="submitting || eligibleLoans.length === 0"
              class="px-4 py-2 rounded-lg bg-amber-600 hover:bg-amber-700 disabled:bg-amber-400 text-white text-sm font-medium transition-colors"
            >
              {{ submitting ? '提交中...' : '确认布置' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
