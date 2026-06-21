<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Shield, Check, Search, Eye, AlertTriangle, DollarSign } from 'lucide-vue-next'
import { api, type Insurance, type Loan, type PageResult, type ApiResponse } from '@/api'
import { useAuth } from '@/stores/auth'

const { hasRole } = useAuth()

const STATUS_LABELS: Record<string, string> = {
  PENDING: '待核验',
  VERIFIED: '已核验',
  ACTIVE: '已生效',
  EXPIRED: '已过期',
}

const STATUS_COLORS: Record<string, string> = {
  PENDING: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  VERIFIED: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
  ACTIVE: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
  EXPIRED: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300',
}

const STATUSES = [
  { value: '', label: '全部状态' },
  { value: 'PENDING', label: '待核验' },
  { value: 'VERIFIED', label: '已核验' },
  { value: 'ACTIVE', label: '已生效' },
  { value: 'EXPIRED', label: '已过期' },
]

const filterStatus = ref('')
const page = ref(0)
const size = ref(10)
const loading = ref(false)
const data = ref<PageResult<Insurance> | null>(null)

const showVerifyModal = ref(false)
const showDetailModal = ref(false)
const submitting = ref(false)
const activatingId = ref<number | null>(null)
const detailData = ref<Insurance | null>(null)

const verifyForm = ref({
  appraisedValue: '' as number | '',
  deductible: '' as number | '',
  deductibleClause: '',
  transportLiability: '',
  effectiveDate: '',
  expiryDate: '',
})

const totalPages = computed(() => data.value?.totalPages ?? 0)

function formatCurrency(val: number | null | undefined): string {
  if (val == null) return '-'
  return '¥' + val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = { page: page.value, size: size.value }
    if (filterStatus.value) params.status = filterStatus.value
    const res = await api.insurances.list(params)
    data.value = res.data
  } catch {
    data.value = null
  } finally {
    loading.value = false
  }
}

function goPage(p: number) {
  if (p < 0 || (data.value && p >= data.value.totalPages)) return
  page.value = p
}

function openVerifyModal(item: Insurance) {
  verifyForm.value = {
    appraisedValue: item.appraisedValue || '',
    deductible: item.deductible || '',
    deductibleClause: item.deductibleClause || '',
    transportLiability: item.transportLiability || '',
    effectiveDate: item.effectiveDate?.substring(0, 10) || '',
    expiryDate: item.expiryDate?.substring(0, 10) || '',
  }
  showVerifyModal.value = true
}

async function submitVerify() {
  if (verifyForm.value.appraisedValue === '' || !verifyForm.value.effectiveDate || !verifyForm.value.expiryDate) return
  submitting.value = true
  try {
    await api.insurances.verify(detailData.value!.id, {
      appraisedValue: verifyForm.value.appraisedValue,
      deductible: verifyForm.value.deductible || null,
      deductibleClause: verifyForm.value.deductibleClause || null,
      transportLiability: verifyForm.value.transportLiability || null,
      effectiveDate: verifyForm.value.effectiveDate,
      expiryDate: verifyForm.value.expiryDate,
    })
    showVerifyModal.value = false
    await fetchData()
  } catch {
  } finally {
    submitting.value = false
  }
}

async function activateInsurance(id: number) {
  activatingId.value = id
  try {
    await api.insurances.activate(id)
    await fetchData()
  } catch {
  } finally {
    activatingId.value = null
  }
}

async function viewDetail(id: number) {
  try {
    const res = await api.insurances.get(id)
    detailData.value = res.data
    showDetailModal.value = true
  } catch {
  }
}

onMounted(fetchData)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center gap-3">
      <div class="p-2 rounded-lg bg-amber-100 dark:bg-amber-900/30">
        <Shield class="w-5 h-5 text-amber-600 dark:text-amber-400" />
      </div>
      <div>
        <h2 class="text-lg font-semibold text-gray-900 dark:text-white">保险核验</h2>
        <p class="text-sm text-gray-500 dark:text-gray-400">展借文物保险记录核验与管理</p>
      </div>
    </div>

    <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
      <div class="flex items-center gap-4">
        <div class="relative">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <select
            v-model="filterStatus"
            class="pl-9 pr-8 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500 appearance-none cursor-pointer"
          >
            <option v-for="s in STATUSES" :key="s.value" :value="s.value">{{ s.label }}</option>
          </select>
        </div>
      </div>
    </div>

    <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-700/50">
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">借展ID</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">文物</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">评估价值</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400 hidden md:table-cell">免赔额</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">状态</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400 hidden lg:table-cell">核验人</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400 hidden md:table-cell">生效日期</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400 hidden md:table-cell">到期日期</th>
              <th class="px-4 py-3 text-right font-medium text-gray-500 dark:text-gray-400">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="9" class="px-4 py-8 text-center text-gray-400 dark:text-gray-500">加载中...</td>
            </tr>
            <tr v-else-if="!data?.content?.length">
              <td colspan="9" class="px-4 py-8 text-center text-gray-400 dark:text-gray-500">暂无保险记录</td>
            </tr>
            <tr
              v-for="item in data?.content"
              :key="item.id"
              class="border-b border-gray-100 dark:border-gray-700/50 hover:bg-gray-50 dark:hover:bg-gray-700/30"
            >
              <td class="px-4 py-3 font-mono text-xs text-gray-600 dark:text-gray-300">{{ item.loanId }}</td>
              <td class="px-4 py-3 text-gray-900 dark:text-white">{{ (item as any).artifactName || '-' }}</td>
              <td class="px-4 py-3 text-gray-900 dark:text-white">
                <div class="flex items-center gap-1">
                  <DollarSign class="w-3.5 h-3.5 text-gray-400" />
                  {{ formatCurrency(item.appraisedValue) }}
                </div>
              </td>
              <td class="px-4 py-3 text-gray-700 dark:text-gray-300 hidden md:table-cell">{{ formatCurrency(item.deductible) }}</td>
              <td class="px-4 py-3">
                <div class="flex items-center gap-1.5">
                  <span :class="['inline-flex px-2 py-0.5 rounded-full text-xs font-medium', STATUS_COLORS[item.status] || '']">
                    {{ STATUS_LABELS[item.status] || item.status }}
                  </span>
                  <AlertTriangle v-if="item.status === 'EXPIRED'" class="w-3.5 h-3.5 text-red-500" />
                </div>
              </td>
              <td class="px-4 py-3 text-gray-700 dark:text-gray-300 hidden lg:table-cell">{{ item.verifierName || '-' }}</td>
              <td class="px-4 py-3 text-gray-500 dark:text-gray-400 text-xs hidden md:table-cell">{{ item.effectiveDate?.substring(0, 10) || '-' }}</td>
              <td class="px-4 py-3 text-gray-500 dark:text-gray-400 text-xs hidden md:table-cell">{{ item.expiryDate?.substring(0, 10) || '-' }}</td>
              <td class="px-4 py-3 text-right">
                <div class="inline-flex items-center gap-1">
                  <button
                    @click="viewDetail(item.id)"
                    class="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 hover:text-amber-600 transition-colors"
                    title="详情"
                  >
                    <Eye class="w-4 h-4" />
                  </button>
                  <button
                    v-if="hasRole('INSURER') && item.status === 'PENDING'"
                    @click="detailData = item; openVerifyModal(item)"
                    class="px-2.5 py-1 rounded-lg text-xs bg-blue-600 hover:bg-blue-700 text-white transition-colors"
                  >
                    核验
                  </button>
                  <button
                    v-if="hasRole('INSURER') && item.status === 'VERIFIED'"
                    :disabled="activatingId === item.id"
                    @click="activateInsurance(item.id)"
                    class="px-2.5 py-1 rounded-lg text-xs bg-green-600 hover:bg-green-700 disabled:bg-green-400 text-white transition-colors"
                  >
                    {{ activatingId === item.id ? '生效中...' : '生效' }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div
        v-if="data && data.totalPages > 1"
        class="flex items-center justify-between px-4 py-3 border-t border-gray-200 dark:border-gray-700"
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
            v-for="p in data.totalPages"
            :key="p"
            @click="goPage(p - 1)"
            :class="[
              'px-3 py-1.5 rounded-lg text-xs border transition-colors',
              p - 1 === page
                ? 'bg-amber-600 border-amber-600 text-white'
                : 'border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'
            ]"
          >
            {{ p }}
          </button>
          <button
            @click="goPage(page + 1)"
            :disabled="page >= data.totalPages - 1"
            class="px-3 py-1.5 rounded-lg text-xs border border-gray-300 dark:border-gray-600 hover:bg-gray-100 dark:hover:bg-gray-700 disabled:opacity-40 disabled:cursor-not-allowed text-gray-700 dark:text-gray-300 transition-colors"
          >
            下一页
          </button>
        </div>
      </div>
    </div>

    <div v-if="showDetailModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50" @click.self="showDetailModal = false">
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-2xl p-6 max-h-[80vh] overflow-y-auto">
        <div class="flex items-center gap-2 mb-5">
          <Shield class="w-5 h-5 text-amber-600" />
          <h3 class="text-base font-semibold text-gray-900 dark:text-white">保险详情</h3>
        </div>
        <template v-if="detailData">
          <div v-if="detailData.status === 'EXPIRED'" class="mb-4 p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800">
            <div class="flex items-center gap-2 text-red-700 dark:text-red-400 text-sm">
              <AlertTriangle class="w-4 h-4" />
              <span class="font-medium">该保险已过期</span>
            </div>
          </div>
          <div class="space-y-3">
            <div class="grid grid-cols-2 gap-4">
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">借展ID</p>
                <p class="text-sm text-gray-900 dark:text-white">{{ detailData.loanId }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">状态</p>
                <span :class="['inline-flex px-2 py-0.5 rounded-full text-xs font-medium', STATUS_COLORS[detailData.status] || '']">
                  {{ STATUS_LABELS[detailData.status] || detailData.status }}
                </span>
              </div>
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">评估价值</p>
                <p class="text-sm text-gray-900 dark:text-white">{{ formatCurrency(detailData.appraisedValue) }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">免赔额</p>
                <p class="text-sm text-gray-900 dark:text-white">{{ formatCurrency(detailData.deductible) }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">核验人</p>
                <p class="text-sm text-gray-900 dark:text-white">{{ detailData.verifierName || '-' }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">核验时间</p>
                <p class="text-sm text-gray-900 dark:text-white">{{ detailData.verifiedAt?.substring(0, 10) || '-' }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">生效日期</p>
                <p class="text-sm text-gray-900 dark:text-white">{{ detailData.effectiveDate?.substring(0, 10) || '-' }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">到期日期</p>
                <p class="text-sm text-gray-900 dark:text-white">{{ detailData.expiryDate?.substring(0, 10) || '-' }}</p>
              </div>
            </div>
            <div>
              <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">免赔条款</p>
              <p class="text-sm text-gray-900 dark:text-white whitespace-pre-wrap">{{ detailData.deductibleClause || '-' }}</p>
            </div>
            <div>
              <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">运输责任</p>
              <p class="text-sm text-gray-900 dark:text-white whitespace-pre-wrap">{{ detailData.transportLiability || '-' }}</p>
            </div>
          </div>
        </template>
        <div class="flex justify-end gap-3 mt-6 pt-4 border-t border-gray-200 dark:border-gray-700">
          <button
            @click="showDetailModal = false"
            class="px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
          >
            关闭
          </button>
        </div>
      </div>
    </div>

    <div v-if="showVerifyModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50" @click.self="showVerifyModal = false">
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-lg p-6 max-h-[80vh] overflow-y-auto">
        <div class="flex items-center gap-2 mb-5">
          <Check class="w-5 h-5 text-amber-600" />
          <h3 class="text-base font-semibold text-gray-900 dark:text-white">保险核验</h3>
        </div>
        <form @submit.prevent="submitVerify" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">评估价值 <span class="text-red-500">*</span></label>
            <div class="relative">
              <DollarSign class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                v-model="verifyForm.appraisedValue"
                type="number"
                step="0.01"
                min="0"
                required
                class="w-full pl-9 pr-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">免赔金额</label>
            <div class="relative">
              <DollarSign class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                v-model="verifyForm.deductible"
                type="number"
                step="0.01"
                min="0"
                class="w-full pl-9 pr-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">免赔条款</label>
            <textarea
              v-model="verifyForm.deductibleClause"
              rows="3"
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500 resize-none"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">运输责任</label>
            <textarea
              v-model="verifyForm.transportLiability"
              rows="3"
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500 resize-none"
            />
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">生效日期 <span class="text-red-500">*</span></label>
              <input
                v-model="verifyForm.effectiveDate"
                type="date"
                required
                class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">到期日期 <span class="text-red-500">*</span></label>
              <input
                v-model="verifyForm.expiryDate"
                type="date"
                required
                class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
          </div>
          <div class="flex justify-end gap-3 pt-2">
            <button
              type="button"
              @click="showVerifyModal = false"
              class="px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
            >
              取消
            </button>
            <button
              type="submit"
              :disabled="submitting"
              class="px-4 py-2 rounded-lg bg-amber-600 hover:bg-amber-700 disabled:bg-amber-400 text-white text-sm font-medium transition-colors"
            >
              {{ submitting ? '提交中...' : '提交核验' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
