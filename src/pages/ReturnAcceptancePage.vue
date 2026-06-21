<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { RotateCcw, Check, AlertTriangle, Plus, X } from 'lucide-vue-next'
import { api, type ReturnRecord, type DamageRecord, type Loan, type PageResult, type ApiResponse } from '@/api'
import { useAuth } from '@/stores/auth'

const { hasRole } = useAuth()

const STATUS_LABELS: Record<string, string> = {
  PENDING: '待验收',
  INSPECTING: '检查中',
  ACCEPTED: '已接收',
  DAMAGED: '有损伤',
}

const STATUS_COLORS: Record<string, string> = {
  PENDING: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  INSPECTING: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
  ACCEPTED: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
  DAMAGED: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
}

const STATUSES = [
  { value: '', label: '全部状态' },
  { value: 'PENDING', label: '待验收' },
  { value: 'INSPECTING', label: '检查中' },
  { value: 'ACCEPTED', label: '已接收' },
  { value: 'DAMAGED', label: '有损伤' },
]

const DAMAGE_TYPES: Record<string, string> = {
  CRACK: '裂隙',
  DISCOLORATION: '变色',
  SURFACE_WEAR: '表面磨损',
  PEELING: '剥落',
  INSECT_DAMAGE: '虫蛀',
  PIGMENT_POWDERING: '颜料粉化',
}

const records = ref<ReturnRecord[]>([])
const loans = ref<Loan[]>([])
const detailRecord = ref<ReturnRecord | null>(null)
const totalElements = ref(0)
const page = ref(0)
const size = ref(10)
const loading = ref(false)
const filterStatus = ref('')
const showCreateModal = ref(false)
const showReceiveModal = ref(false)
const showDamageModal = ref(false)
const submitting = ref(false)

const createForm = ref({
  loanId: '' as number | '',
  returnNotes: '',
})

const receiveForm = ref({
  overallStatus: 'ACCEPTED',
  returnNotes: '',
})

const damageForm = ref({
  damageType: 'CRACK',
  description: '',
  responsibility: '',
})

const receiveId = ref<number | null>(null)
const damageId = ref<number | null>(null)

const totalPages = computed(() => Math.ceil(totalElements.value / size.value))

const completedLoans = computed(() => {
  return loans.value.filter(l => {
    const s = l.status
    return s === 'COMPLETED' || s === 'ACTIVE' || s === 'APPROVED'
  })
})

function getLoanForRecord(record: ReturnRecord): Loan | undefined {
  return loans.value.find(l => l.id === record.loanId)
}

async function loadRecords() {
  loading.value = true
  try {
    const params: Record<string, any> = { page: page.value, size: size.value }
    if (filterStatus.value) params.status = filterStatus.value
    const res = await api.returns.list(params)
    records.value = res.data.content
    totalElements.value = res.data.totalElements
  } catch {
    records.value = []
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
  loadRecords()
}

function changePage(delta: number) {
  const next = page.value + delta
  if (next >= 0 && next < totalPages.value) {
    page.value = next
    loadRecords()
  }
}

function openCreateModal() {
  createForm.value = { loanId: '', returnNotes: '' }
  showCreateModal.value = true
}

async function submitCreate() {
  if (createForm.value.loanId === '') return
  submitting.value = true
  try {
    await api.returns.create({
      loanId: createForm.value.loanId as number,
      returnNotes: createForm.value.returnNotes || null,
    })
    showCreateModal.value = false
    await loadRecords()
    await loadLoans()
  } catch {
  } finally {
    submitting.value = false
  }
}

function openReceiveModal(id: number) {
  receiveId.value = id
  receiveForm.value = { overallStatus: 'ACCEPTED', returnNotes: '' }
  showReceiveModal.value = true
}

async function submitReceive() {
  if (!receiveId.value) return
  submitting.value = true
  try {
    await api.returns.receive(receiveId.value, {
      overallStatus: receiveForm.value.overallStatus,
      returnNotes: receiveForm.value.returnNotes || null,
    })
    showReceiveModal.value = false
    receiveId.value = null
    await loadRecords()
  } catch {
  } finally {
    submitting.value = false
  }
}

function openDamageModal(id: number) {
  damageId.value = id
  damageForm.value = { damageType: 'CRACK', description: '', responsibility: '' }
  showDamageModal.value = true
}

async function submitDamage() {
  if (!damageId.value) return
  submitting.value = true
  try {
    await api.returns.addDamage(damageId.value, {
      damageType: damageForm.value.damageType,
      description: damageForm.value.description || null,
      responsibility: damageForm.value.responsibility || null,
    })
    showDamageModal.value = false
    damageId.value = null
    await loadRecords()
  } catch {
  } finally {
    submitting.value = false
  }
}

async function viewDetail(id: number) {
  try {
    const res = await api.returns.get(id)
    detailRecord.value = res.data
  } catch {
    detailRecord.value = null
  }
}

onMounted(() => {
  loadLoans()
  loadRecords()
})

watch(filterStatus, () => {
  page.value = 0
  loadRecords()
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="p-2 rounded-lg bg-indigo-100 dark:bg-indigo-900/30">
          <RotateCcw class="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
        </div>
        <div>
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white">归还验收</h2>
          <p class="text-sm text-gray-500 dark:text-gray-400">文物归还接收与损伤记录管理</p>
        </div>
      </div>
      <button
        v-if="hasRole('COLLECTION')"
        @click="openCreateModal"
        class="flex items-center gap-2 px-4 py-2 rounded-lg bg-amber-600 hover:bg-amber-700 text-white text-sm font-medium transition-colors"
      >
        <Plus class="w-4 h-4" />
        创建归还记录
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

    <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-700/50">
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">文物名称</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">借入方</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">状态</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400 hidden md:table-cell">归还备注</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400 hidden lg:table-cell">接收人</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400 hidden lg:table-cell">接收日期</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="7" class="px-4 py-8 text-center text-gray-400 dark:text-gray-500">加载中...</td>
            </tr>
            <tr v-else-if="records.length === 0">
              <td colspan="7" class="px-4 py-8 text-center text-gray-400 dark:text-gray-500">暂无归还记录</td>
            </tr>
            <tr
              v-for="r in records"
              :key="r.id"
              class="border-b border-gray-100 dark:border-gray-700/50 hover:bg-gray-50 dark:hover:bg-gray-700/30"
            >
              <td class="px-4 py-3 text-gray-900 dark:text-white">{{ getLoanForRecord(r)?.artifactName || '-' }}</td>
              <td class="px-4 py-3 text-gray-700 dark:text-gray-300">{{ getLoanForRecord(r)?.borrowingInstitution || '-' }}</td>
              <td class="px-4 py-3">
                <span :class="['inline-flex px-2 py-0.5 rounded-full text-xs font-medium', STATUS_COLORS[r.overallStatus] || STATUS_COLORS.PENDING]">
                  {{ STATUS_LABELS[r.overallStatus] || r.overallStatus }}
                </span>
              </td>
              <td class="px-4 py-3 text-gray-700 dark:text-gray-300 max-w-xs truncate hidden md:table-cell">{{ r.returnNotes || '-' }}</td>
              <td class="px-4 py-3 text-gray-700 dark:text-gray-300 hidden lg:table-cell">{{ r.receiverName || '-' }}</td>
              <td class="px-4 py-3 text-xs text-gray-500 dark:text-gray-400 hidden lg:table-cell">{{ r.receivedAt ? r.receivedAt.substring(0, 10) : '-' }}</td>
              <td class="px-4 py-3">
                <div class="flex items-center gap-1.5">
                  <button
                    @click="viewDetail(r.id)"
                    class="px-2.5 py-1 rounded-lg text-xs text-amber-600 dark:text-amber-400 hover:bg-amber-50 dark:hover:bg-amber-900/20 transition-colors"
                  >
                    详情
                  </button>
                  <button
                    v-if="hasRole('COLLECTION') && (r.overallStatus === 'PENDING' || r.overallStatus === 'INSPECTING')"
                    @click="openReceiveModal(r.id)"
                    class="px-2.5 py-1 rounded-lg text-xs bg-green-600 hover:bg-green-700 text-white transition-colors"
                  >
                    接收
                  </button>
                  <button
                    v-if="r.overallStatus === 'DAMAGED' || r.overallStatus === 'INSPECTING'"
                    @click="openDamageModal(r.id)"
                    class="px-2.5 py-1 rounded-lg text-xs bg-red-600 hover:bg-red-700 text-white transition-colors flex items-center gap-1"
                  >
                    <AlertTriangle class="w-3 h-3" />
                    损伤
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="totalPages > 1" class="flex items-center justify-between px-4 py-3 border-t border-gray-200 dark:border-gray-700">
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
    </div>

    <div v-if="detailRecord" class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5 space-y-4">
      <div class="flex items-center justify-between">
        <h3 class="text-sm font-semibold text-gray-900 dark:text-white">归还记录详情</h3>
        <button @click="detailRecord = null" class="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 transition-colors">
          <X class="w-4 h-4" />
        </button>
      </div>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
        <div>
          <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">状态</p>
          <span :class="['inline-flex px-2 py-0.5 rounded-full text-xs font-medium', STATUS_COLORS[detailRecord.overallStatus] || STATUS_COLORS.PENDING]">
            {{ STATUS_LABELS[detailRecord.overallStatus] || detailRecord.overallStatus }}
          </span>
        </div>
        <div>
          <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">接收人</p>
          <p class="text-gray-900 dark:text-white">{{ detailRecord.receiverName || '-' }}</p>
        </div>
        <div>
          <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">接收日期</p>
          <p class="text-gray-900 dark:text-white">{{ detailRecord.receivedAt ? detailRecord.receivedAt.substring(0, 10) : '-' }}</p>
        </div>
        <div>
          <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">归还备注</p>
          <p class="text-gray-900 dark:text-white">{{ detailRecord.returnNotes || '-' }}</p>
        </div>
      </div>

      <div v-if="detailRecord.damageRecords && detailRecord.damageRecords.length > 0">
        <p class="text-xs font-medium text-gray-500 dark:text-gray-400 mb-2">损伤记录</p>
        <div class="space-y-2">
          <div
            v-for="d in detailRecord.damageRecords"
            :key="d.id"
            class="flex items-start gap-3 p-3 rounded-lg bg-red-50 dark:bg-red-900/10 border border-red-100 dark:border-red-800/30"
          >
            <AlertTriangle class="w-4 h-4 text-red-500 dark:text-red-400 shrink-0 mt-0.5" />
            <div class="flex-1 space-y-1">
              <div class="flex items-center gap-2">
                <span class="inline-flex px-2 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400">
                  {{ DAMAGE_TYPES[d.damageType] || d.damageType }}
                </span>
              </div>
              <p v-if="d.description" class="text-sm text-gray-700 dark:text-gray-300">{{ d.description }}</p>
              <p v-if="d.responsibility" class="text-xs text-gray-500 dark:text-gray-400">责任人: {{ d.responsibility }}</p>
              <p class="text-xs text-gray-400 dark:text-gray-500">{{ d.recordedAt?.replace('T', ' ').substring(0, 19) }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showCreateModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50" @click.self="showCreateModal = false">
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-lg p-6">
        <div class="flex items-center gap-2 mb-5">
          <Plus class="w-5 h-5 text-amber-600" />
          <h3 class="text-base font-semibold text-gray-900 dark:text-white">创建归还记录</h3>
        </div>
        <form @submit.prevent="submitCreate" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">借展记录</label>
            <select
              v-model="createForm.loanId"
              required
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
            >
              <option value="" disabled>请选择借展记录</option>
              <option v-for="l in completedLoans" :key="l.id" :value="l.id">
                {{ l.artifactName || '文物#' + l.artifactId }} - {{ l.borrowingInstitution }}
              </option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">归还备注</label>
            <textarea
              v-model="createForm.returnNotes"
              rows="3"
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500 resize-none"
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
              :disabled="submitting"
              class="px-4 py-2 rounded-lg bg-amber-600 hover:bg-amber-700 disabled:bg-amber-400 text-white text-sm font-medium transition-colors"
            >
              {{ submitting ? '提交中...' : '创建' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="showReceiveModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50" @click.self="showReceiveModal = false">
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-lg p-6">
        <div class="flex items-center gap-2 mb-5">
          <Check class="w-5 h-5 text-green-600" />
          <h3 class="text-base font-semibold text-gray-900 dark:text-white">接收归还</h3>
        </div>
        <form @submit.prevent="submitReceive" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">验收状态</label>
            <select
              v-model="receiveForm.overallStatus"
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
            >
              <option value="ACCEPTED">已接收 - 无损伤</option>
              <option value="DAMAGED">有损伤 - 发现损伤</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">归还备注</label>
            <textarea
              v-model="receiveForm.returnNotes"
              rows="3"
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500 resize-none"
            />
          </div>
          <div class="flex justify-end gap-3 pt-2">
            <button
              type="button"
              @click="showReceiveModal = false"
              class="px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
            >
              取消
            </button>
            <button
              type="submit"
              :disabled="submitting"
              class="px-4 py-2 rounded-lg bg-green-600 hover:bg-green-700 disabled:bg-green-400 text-white text-sm font-medium transition-colors"
            >
              {{ submitting ? '提交中...' : '确认接收' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="showDamageModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50" @click.self="showDamageModal = false">
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-lg p-6">
        <div class="flex items-center gap-2 mb-5">
          <AlertTriangle class="w-5 h-5 text-red-600" />
          <h3 class="text-base font-semibold text-gray-900 dark:text-white">添加损伤记录</h3>
        </div>
        <form @submit.prevent="submitDamage" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">损伤类型</label>
            <select
              v-model="damageForm.damageType"
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
            >
              <option v-for="(label, key) in DAMAGE_TYPES" :key="key" :value="key">{{ label }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">描述</label>
            <textarea
              v-model="damageForm.description"
              rows="3"
              placeholder="请描述损伤详情"
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500 resize-none"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">责任人</label>
            <input
              v-model="damageForm.responsibility"
              type="text"
              placeholder="谁对此损伤负责"
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
            />
          </div>
          <div class="flex justify-end gap-3 pt-2">
            <button
              type="button"
              @click="showDamageModal = false"
              class="px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
            >
              取消
            </button>
            <button
              type="submit"
              :disabled="submitting"
              class="px-4 py-2 rounded-lg bg-red-600 hover:bg-red-700 disabled:bg-red-400 text-white text-sm font-medium transition-colors"
            >
              {{ submitting ? '提交中...' : '添加损伤' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
