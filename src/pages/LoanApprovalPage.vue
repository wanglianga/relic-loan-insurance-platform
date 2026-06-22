<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { FileText, Check, X, Eye, AlertTriangle, Shield, ClipboardCheck, FileWarning } from 'lucide-vue-next'
import { api, type Loan, type Artifact, type PageResult, type EnvironmentPreCheck, type EnvironmentRisk, type DiseaseReview, type ExhibitionCondition } from '@/api'

const STATUS_MAP: Record<string, string> = {
  PENDING: '待审批',
  APPROVED: '已批准',
  REJECTED: '已驳回',
  ACTIVE: '进行中',
  COMPLETED: '已完成',
}

const STATUS_COLORS: Record<string, string> = {
  PENDING: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  APPROVED: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
  REJECTED: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
  ACTIVE: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
  COMPLETED: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300',
}

const RISK_LEVEL_LABELS: Record<string, string> = {
  LOW: '低风险',
  MEDIUM: '中风险',
  HIGH: '高风险',
  CRITICAL: '极高风险',
}

const RISK_LEVEL_COLORS: Record<string, string> = {
  LOW: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
  MEDIUM: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  HIGH: 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400',
  CRITICAL: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
}

const DISEASE_TYPE_LABELS: Record<string, string> = {
  CRACK: '裂隙',
  DISCOLORATION: '变色',
  SURFACE_WEAR: '表面磨损',
  PEELING: '剥落',
  INSECT_DAMAGE: '虫蛀',
  PIGMENT_POWDERING: '颜料粉化',
  GLAZE_CRACKING: '釉面开片',
  PAINT_PEELING: '彩绘脱落',
  WOOD_DEFORMATION: '木胎变形',
  METAL_CORROSION: '金属锈蚀',
}

const STATUSES = [
  { value: '', label: '全部状态' },
  { value: 'PENDING', label: '待审批' },
  { value: 'APPROVED', label: '已批准' },
  { value: 'REJECTED', label: '已驳回' },
  { value: 'ACTIVE', label: '进行中' },
  { value: 'COMPLETED', label: '已完成' },
]

const status = ref('')
const page = ref(0)
const size = ref(10)
const loading = ref(false)
const data = ref<PageResult<Loan> | null>(null)

const showDetail = ref(false)
const detailLoading = ref(false)
const detail = ref<Loan | null>(null)
const detailArtifact = ref<Artifact | null>(null)
const latestPreCheck = ref<EnvironmentPreCheck | null>(null)
const latestRisk = ref<EnvironmentRisk | null>(null)
const diseaseReviews = ref<DiseaseReview[]>([])
const exhibitionConditions = ref<ExhibitionCondition[]>([])
const loadingRisk = ref(false)
const loadingReviews = ref(false)

const showRejectModal = ref(false)
const rejectLoanId = ref<number | null>(null)
const rejectReason = ref('')
const rejecting = ref(false)

const approving = ref<number | null>(null)

async function fetchLoans() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: page.value,
      size: size.value,
    }
    if (status.value) params.status = status.value
    const res = await api.loans.list(params)
    data.value = res.data
  } catch {
    data.value = null
  } finally {
    loading.value = false
  }
}

async function loadRiskAndReviews(loanId: number) {
  loadingRisk.value = true
  loadingReviews.value = true
  try {
    const preCheckRes = await api.loans.getLatestPreCheck(loanId)
    latestPreCheck.value = preCheckRes.data
    latestRisk.value = preCheckRes.data.latestRisk || null
  } catch {
    latestPreCheck.value = null
    latestRisk.value = null
  } finally {
    loadingRisk.value = false
  }

  try {
    const reviewsRes = await api.diseases.listReviewsByLoan(loanId)
    diseaseReviews.value = reviewsRes.data
  } catch {
    diseaseReviews.value = []
  } finally {
    loadingReviews.value = false
  }

  try {
    const conditionsRes = await api.diseases.listExhibitionConditionsByLoan(loanId)
    exhibitionConditions.value = conditionsRes.data
  } catch {
    exhibitionConditions.value = []
  }
}

async function viewDetail(id: number) {
  showDetail.value = true
  detailLoading.value = true
  detail.value = null
  detailArtifact.value = null
  latestPreCheck.value = null
  latestRisk.value = null
  diseaseReviews.value = []
  exhibitionConditions.value = []
  try {
    const res = await api.loans.get(id)
    detail.value = res.data
    if (detail.value.artifactId) {
      try {
        const artifactRes = await api.artifacts.get(detail.value.artifactId)
        detailArtifact.value = artifactRes.data
      } catch {
        detailArtifact.value = null
      }
    }
    await loadRiskAndReviews(id)
  } catch {
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

function hasUnresolvedRisk(): boolean {
  return latestRisk.value?.requiresApproval === true
}

function hasAffectedDisease(): boolean {
  return diseaseReviews.value.some(r => r.affectsLoanGrade === true)
}

function hasApprovalBlocker(): boolean {
  return hasUnresolvedRisk() || hasAffectedDisease()
}

function getBlockerMessages(): string[] {
  const messages: string[] = []
  if (hasUnresolvedRisk()) {
    messages.push('存在未处理的环境高风险，请先完成风险 mitigation')
  }
  if (hasAffectedDisease()) {
    messages.push('存在影响借展等级的病害，请确认展示条件已落实')
  }
  return messages
}

async function approveLoan(id: number) {
  if (hasApprovalBlocker()) {
    alert('存在审批拦截项，请先处理风险和病害问题后再批准')
    return
  }
  approving.value = id
  try {
    await api.loans.approve(id)
    fetchLoans()
    if (showDetail.value && detail.value?.id === id) {
      viewDetail(id)
    }
  } finally {
    approving.value = null
  }
}

function openRejectModal(id: number) {
  rejectLoanId.value = id
  rejectReason.value = ''
  showRejectModal.value = true
}

async function submitReject() {
  if (!rejectLoanId.value || !rejectReason.value.trim()) return
  rejecting.value = true
  try {
    await api.loans.reject(rejectLoanId.value, rejectReason.value.trim())
    showRejectModal.value = false
    fetchLoans()
    if (showDetail.value && detail.value?.id === rejectLoanId.value) {
      viewDetail(rejectLoanId.value)
    }
  } finally {
    rejecting.value = false
  }
}

function goPage(p: number) {
  if (p < 0 || (data.value && p >= data.value.totalPages)) return
  page.value = p
}

function formatDate(d: string | null | undefined) {
  return d ? d.split('T')[0] : '-'
}

function getLoanDays(): number {
  if (!detail.value?.loanStartDate || !detail.value?.loanEndDate) return 0
  const start = new Date(detail.value.loanStartDate)
  const end = new Date(detail.value.loanEndDate)
  return Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24))
}

function hasViolation(): boolean {
  if (!detailArtifact.value?.restriction || !detail.value) return false
  const r = detailArtifact.value.restriction
  if (r.noLoan) return true
  const days = getLoanDays()
  if (r.maxLoanDays && days > r.maxLoanDays) return true
  return false
}

function getViolationMessages(): string[] {
  const messages: string[] = []
  if (!detailArtifact.value?.restriction || !detail.value) return messages
  const r = detailArtifact.value.restriction
  if (r.noLoan) messages.push('该文物禁止外借')
  const days = getLoanDays()
  if (r.maxLoanDays && days > r.maxLoanDays) {
    messages.push(`借展天数(${days}天)超过最大允许天数(${r.maxLoanDays}天)`)
  }
  return messages
}

onMounted(fetchLoans)

watch([status], () => {
  page.value = 0
  fetchLoans()
})

watch(page, fetchLoans)
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-2">
        <FileText class="w-5 h-5 text-amber-600" />
        <h2 class="text-lg font-semibold text-gray-900 dark:text-white">借展审批</h2>
      </div>
    </div>

    <div class="flex flex-col sm:flex-row gap-3">
      <div class="relative">
        <select
          v-model="status"
          class="pl-4 pr-8 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none transition text-sm appearance-none cursor-pointer"
        >
          <option v-for="s in STATUSES" :key="s.value" :value="s.value">{{ s.label }}</option>
        </select>
      </div>
    </div>

    <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/80">
              <th class="text-left px-4 py-3 font-medium text-gray-500 dark:text-gray-400">文物编号</th>
              <th class="text-left px-4 py-3 font-medium text-gray-500 dark:text-gray-400">文物名称</th>
              <th class="text-left px-4 py-3 font-medium text-gray-500 dark:text-gray-400 hidden md:table-cell">申请人</th>
              <th class="text-left px-4 py-3 font-medium text-gray-500 dark:text-gray-400 hidden lg:table-cell">借入机构</th>
              <th class="text-left px-4 py-3 font-medium text-gray-500 dark:text-gray-400 hidden lg:table-cell">展览名称</th>
              <th class="text-left px-4 py-3 font-medium text-gray-500 dark:text-gray-400 hidden md:table-cell">起止日期</th>
              <th class="text-left px-4 py-3 font-medium text-gray-500 dark:text-gray-400">状态</th>
              <th class="text-right px-4 py-3 font-medium text-gray-500 dark:text-gray-400">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="8" class="px-4 py-8 text-center text-gray-400 dark:text-gray-500">加载中...</td>
            </tr>
            <tr v-else-if="!data?.content?.length">
              <td colspan="8" class="px-4 py-8 text-center text-gray-400 dark:text-gray-500">暂无数据</td>
            </tr>
            <tr
              v-for="item in data?.content"
              :key="item.id"
              class="border-b border-gray-100 dark:border-gray-700/50 hover:bg-amber-50/50 dark:hover:bg-amber-900/10 transition-colors"
            >
              <td class="px-4 py-3 font-mono text-xs text-gray-600 dark:text-gray-300">{{ item.artifactCode || '-' }}</td>
              <td class="px-4 py-3 font-medium text-gray-900 dark:text-white">{{ item.artifactName || '-' }}</td>
              <td class="px-4 py-3 text-gray-600 dark:text-gray-300 hidden md:table-cell">{{ item.applicantName || '-' }}</td>
              <td class="px-4 py-3 text-gray-600 dark:text-gray-300 hidden lg:table-cell">{{ item.borrowingInstitution }}</td>
              <td class="px-4 py-3 text-gray-600 dark:text-gray-300 hidden lg:table-cell">{{ item.exhibitionName }}</td>
              <td class="px-4 py-3 text-gray-600 dark:text-gray-300 hidden md:table-cell text-xs">{{ formatDate(item.loanStartDate) }} ~ {{ formatDate(item.loanEndDate) }}</td>
              <td class="px-4 py-3">
                <span :class="['inline-flex px-2 py-0.5 rounded-full text-xs font-medium', STATUS_COLORS[item.status] || STATUS_COLORS.COMPLETED]">
                  {{ STATUS_MAP[item.status] || item.status }}
                </span>
              </td>
              <td class="px-4 py-3 text-right">
                <div class="inline-flex items-center gap-1">
                  <button
                    @click="viewDetail(item.id)"
                    class="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 hover:text-amber-600 transition-colors"
                    title="查看"
                  >
                    <Eye class="w-4 h-4" />
                  </button>
                  <button
                    v-if="item.status === 'PENDING'"
                    @click="approveLoan(item.id)"
                    :disabled="approving === item.id"
                    class="p-1.5 rounded-lg hover:bg-green-100 dark:hover:bg-green-900/20 text-gray-500 hover:text-green-600 dark:hover:text-green-400 transition-colors disabled:opacity-50"
                    title="批准"
                  >
                    <Check class="w-4 h-4" />
                  </button>
                  <button
                    v-if="item.status === 'PENDING'"
                    @click="openRejectModal(item.id)"
                    class="p-1.5 rounded-lg hover:bg-red-100 dark:hover:bg-red-900/20 text-gray-500 hover:text-red-600 dark:hover:text-red-400 transition-colors"
                    title="驳回"
                  >
                    <X class="w-4 h-4" />
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

    <teleport to="body">
      <div
        v-if="showDetail"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
      >
        <div class="fixed inset-0 bg-black/50" @click="showDetail = false"></div>
        <div class="relative bg-white dark:bg-gray-800 rounded-xl shadow-xl w-full max-w-3xl max-h-[90vh] overflow-y-auto border border-gray-200 dark:border-gray-700">
          <div class="sticky top-0 bg-white dark:bg-gray-800 px-6 py-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between z-10">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white">借展详情</h3>
            <button @click="showDetail = false" class="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 transition-colors">
              <X class="w-5 h-5" />
            </button>
          </div>

          <div v-if="detailLoading" class="p-8 text-center text-gray-400 dark:text-gray-500">加载中...</div>
          <div v-else-if="detail" class="p-6 space-y-5">
            <div v-if="hasViolation()" class="rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 p-4">
              <div class="flex items-center gap-2 mb-2">
                <X class="w-4 h-4 text-red-600 dark:text-red-400" />
                <span class="text-sm font-semibold text-red-700 dark:text-red-400">违规警告</span>
              </div>
              <ul class="space-y-1">
                <li v-for="(msg, i) in getViolationMessages()" :key="i" class="text-sm text-red-600 dark:text-red-400">· {{ msg }}</li>
              </ul>
            </div>

            <div v-if="hasApprovalBlocker()" class="rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 p-4">
              <div class="flex items-center gap-2 mb-2">
                <FileWarning class="w-4 h-4 text-red-600 dark:text-red-400" />
                <span class="text-sm font-semibold text-red-700 dark:text-red-400">审批拦截</span>
              </div>
              <ul class="space-y-1">
                <li v-for="(msg, i) in getBlockerMessages()" :key="i" class="text-sm text-red-600 dark:text-red-400">· {{ msg }}</li>
              </ul>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">文物编号</p>
                <p class="text-sm font-medium text-gray-900 dark:text-white">{{ detail.artifactCode || '-' }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">文物名称</p>
                <p class="text-sm font-medium text-gray-900 dark:text-white">{{ detail.artifactName || '-' }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">申请人</p>
                <p class="text-sm text-gray-900 dark:text-white">{{ detail.applicantName || '-' }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">借入机构</p>
                <p class="text-sm text-gray-900 dark:text-white">{{ detail.borrowingInstitution }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">展览名称</p>
                <p class="text-sm text-gray-900 dark:text-white">{{ detail.exhibitionName }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">状态</p>
                <span :class="['inline-flex px-2 py-0.5 rounded-full text-xs font-medium', STATUS_COLORS[detail.status] || STATUS_COLORS.COMPLETED]">
                  {{ STATUS_MAP[detail.status] || detail.status }}
                </span>
              </div>
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">借展开始日期</p>
                <p class="text-sm text-gray-900 dark:text-white">{{ formatDate(detail.loanStartDate) }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">借展结束日期</p>
                <p class="text-sm text-gray-900 dark:text-white">{{ formatDate(detail.loanEndDate) }}</p>
              </div>
              <div v-if="detail.rejectionReason" class="col-span-2">
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">驳回原因</p>
                <p class="text-sm text-red-600 dark:text-red-400">{{ detail.rejectionReason }}</p>
              </div>
            </div>

            <div v-if="loadingRisk" class="text-sm text-gray-500 dark:text-gray-400 py-4 text-center">
              加载环境风险信息...
            </div>
            <div v-else-if="latestRisk" class="border-t border-gray-200 dark:border-gray-700 pt-4">
              <div class="flex items-center gap-2 mb-3">
                <Shield class="w-4 h-4 text-amber-600" />
                <h4 class="text-sm font-semibold text-gray-900 dark:text-white">环境风险评估</h4>
                <span :class="['inline-flex px-2 py-0.5 rounded-full text-xs font-medium', RISK_LEVEL_COLORS[latestRisk.riskLevel] || '']">
                  {{ RISK_LEVEL_LABELS[latestRisk.riskLevel] || latestRisk.riskLevel }}
                </span>
              </div>
              <div class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-4 space-y-3">
                <div v-if="latestRisk.requiresApproval" class="bg-red-50 dark:bg-red-900/20 rounded-lg p-3 border border-red-200 dark:border-red-800">
                  <p class="text-sm text-red-600 dark:text-red-400">⚠️ 该申请存在高风险，无法直接通过审批</p>
                </div>
                <div v-if="latestRisk.riskFactors">
                  <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">识别风险因素</p>
                  <p class="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-line">{{ latestRisk.riskFactors }}</p>
                </div>
                <div v-if="latestRisk.mitigationSuggestions">
                  <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">风险缓解建议</p>
                  <p class="text-sm text-amber-700 dark:text-amber-400 whitespace-pre-line">{{ latestRisk.mitigationSuggestions }}</p>
                </div>
                <div class="grid grid-cols-1 md:grid-cols-2 gap-3 pt-2">
                  <div v-if="latestRisk.showcaseSuggestion" class="bg-white dark:bg-gray-700 rounded p-3">
                    <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">展柜建议</p>
                    <p class="text-sm text-gray-900 dark:text-white">{{ latestRisk.showcaseSuggestion }}</p>
                  </div>
                  <div v-if="latestRisk.exhibitionDurationSuggestion" class="bg-white dark:bg-gray-700 rounded p-3">
                    <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">展期建议</p>
                    <p class="text-sm text-gray-900 dark:text-white">不超过 {{ latestRisk.exhibitionDurationSuggestion }} 天</p>
                  </div>
                  <div v-if="latestRisk.monitoringEquipment" class="bg-white dark:bg-gray-700 rounded p-3 md:col-span-2">
                    <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">监测设备</p>
                    <p class="text-sm text-gray-900 dark:text-white">{{ latestRisk.monitoringEquipment }}</p>
                  </div>
                </div>
              </div>
            </div>

            <div v-else class="border-t border-gray-200 dark:border-gray-700 pt-4">
              <div class="flex items-center gap-2 mb-2">
                <Shield class="w-4 h-4 text-gray-400" />
                <p class="text-sm text-gray-500 dark:text-gray-400">暂无环境风险评估记录</p>
              </div>
            </div>

            <div v-if="loadingReviews" class="text-sm text-gray-500 dark:text-gray-400 py-4 text-center">
              加载病害复核信息...
            </div>
            <div v-else-if="diseaseReviews.length > 0" class="border-t border-gray-200 dark:border-gray-700 pt-4">
              <div class="flex items-center gap-2 mb-3">
                <ClipboardCheck class="w-4 h-4 text-blue-600" />
                <h4 class="text-sm font-semibold text-gray-900 dark:text-white">病害复核记录</h4>
              </div>
              <div class="space-y-3">
                <div v-for="(review, idx) in diseaseReviews" :key="idx" class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-4">
                  <div class="flex items-center justify-between mb-2">
                    <div class="flex items-center gap-2">
                      <span class="text-sm font-medium text-gray-900 dark:text-white">{{ DISEASE_TYPE_LABELS[review.diseaseType] || review.diseaseType }}</span>
                      <span v-if="review.affectsLoanGrade" class="inline-flex px-2 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400">
                        影响借展等级
                      </span>
                      <span v-else class="inline-flex px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400">
                        不影响借展
                      </span>
                    </div>
                    <span class="text-xs text-gray-500 dark:text-gray-400">{{ review.reviewedAt?.substring(0, 16) }}</span>
                  </div>
                  <p v-if="review.loanGradeImpact" class="text-sm text-gray-700 dark:text-gray-300 mb-2">
                    <span class="text-gray-500 dark:text-gray-400">影响说明：</span>{{ review.loanGradeImpact }}
                  </p>
                  <p v-if="review.reviewNotes" class="text-sm text-gray-700 dark:text-gray-300">
                    <span class="text-gray-500 dark:text-gray-400">复核意见：</span>{{ review.reviewNotes }}
                  </p>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mt-2">复核人：{{ review.reviewerName || '-' }}</p>
                </div>
              </div>
            </div>

            <div v-if="exhibitionConditions.length > 0" class="border-t border-gray-200 dark:border-gray-700 pt-4">
              <div class="flex items-center gap-2 mb-3">
                <AlertTriangle class="w-4 h-4 text-amber-600" />
                <h4 class="text-sm font-semibold text-gray-900 dark:text-white">展示条件要求</h4>
              </div>
              <div class="space-y-3">
                <div v-for="(cond, idx) in exhibitionConditions" :key="idx" class="bg-amber-50 dark:bg-amber-900/20 rounded-lg p-4 border border-amber-200 dark:border-amber-800">
                  <div class="grid grid-cols-2 gap-4 mb-3">
                    <div>
                      <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">最小展示距离</p>
                      <p class="text-lg font-bold text-gray-900 dark:text-white">{{ cond.displayDistanceMin }} 米</p>
                    </div>
                    <div>
                      <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">照度上限</p>
                      <p class="text-lg font-bold text-gray-900 dark:text-white">{{ cond.illuminanceLimit }} lux</p>
                    </div>
                  </div>
                  <div class="space-y-2">
                    <div>
                      <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">支架要求</p>
                      <p class="text-sm text-gray-900 dark:text-white">{{ cond.bracketRequirements }}</p>
                    </div>
                    <div>
                      <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">保险附加条件</p>
                      <p class="text-sm text-gray-900 dark:text-white">{{ cond.insuranceAdditionalTerms }}</p>
                    </div>
                    <div v-if="cond.otherRequirements">
                      <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">其他要求</p>
                      <p class="text-sm text-gray-900 dark:text-white">{{ cond.otherRequirements }}</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="detailArtifact?.restriction" class="border-t border-gray-200 dark:border-gray-700 pt-4">
              <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-3">文物限制信息</h4>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">禁止外借</p>
                  <p :class="['text-sm font-medium', detailArtifact.restriction.noLoan ? 'text-red-600 dark:text-red-400' : 'text-green-600 dark:text-green-400']">
                    {{ detailArtifact.restriction.noLoan ? '是' : '否' }}
                  </p>
                </div>
                <div v-if="detailArtifact.restriction.noLoanReason">
                  <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">禁止原因</p>
                  <p class="text-sm text-gray-900 dark:text-white">{{ detailArtifact.restriction.noLoanReason }}</p>
                </div>
                <div>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">最大借展天数</p>
                  <p class="text-sm text-gray-900 dark:text-white">{{ detailArtifact.restriction.maxLoanDays || '无限制' }}</p>
                </div>
                <div>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">最小借展间隔天数</p>
                  <p class="text-sm text-gray-900 dark:text-white">{{ detailArtifact.restriction.minLoanIntervalDays || '无限制' }}</p>
                </div>
                <div v-if="detailArtifact.restriction.loanConditions" class="col-span-2">
                  <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">借展条件</p>
                  <p class="text-sm text-gray-900 dark:text-white">{{ detailArtifact.restriction.loanConditions }}</p>
                </div>
              </div>
            </div>

            <div v-if="detail.environment" class="border-t border-gray-200 dark:border-gray-700 pt-4">
              <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-3">环境要求</h4>
              <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
                <div>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">温度范围</p>
                  <p class="text-sm text-gray-900 dark:text-white">
                    {{ detail.environment.temperatureMin ?? '-' }} ~ {{ detail.environment.temperatureMax ?? '-' }} ℃
                  </p>
                </div>
                <div>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">湿度范围</p>
                  <p class="text-sm text-gray-900 dark:text-white">
                    {{ detail.environment.humidityMin ?? '-' }} ~ {{ detail.environment.humidityMax ?? '-' }} %
                  </p>
                </div>
                <div>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">照度上限</p>
                  <p class="text-sm text-gray-900 dark:text-white">{{ detail.environment.illuminanceMax ?? '-' }} lux</p>
                </div>
                <div>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">振动上限</p>
                  <p class="text-sm text-gray-900 dark:text-white">{{ detail.environment.vibrationMax ?? '-' }} g</p>
                </div>
                <div v-if="detail.environment.securityRoute" class="col-span-2">
                  <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">安防路线</p>
                  <p class="text-sm text-gray-900 dark:text-white">{{ detail.environment.securityRoute }}</p>
                </div>
                <div v-if="detail.environment.setupDate">
                  <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">布展日期</p>
                  <p class="text-sm text-gray-900 dark:text-white">{{ formatDate(detail.environment.setupDate) }}</p>
                </div>
              </div>
            </div>

            <div v-if="detail.status === 'PENDING'" class="border-t border-gray-200 dark:border-gray-700 pt-4 flex justify-end gap-3">
              <button
                @click="openRejectModal(detail.id); showDetail = false"
                class="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg border border-red-300 dark:border-red-700 text-red-700 dark:text-red-400 text-sm font-medium hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
              >
                <X class="w-4 h-4" />
                驳回
              </button>
              <button
                @click="approveLoan(detail.id)"
                :disabled="approving === detail.id || hasApprovalBlocker()"
                :title="hasApprovalBlocker() ? '存在审批拦截项，请先处理' : ''"
                class="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg bg-green-600 hover:bg-green-700 text-white text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <Check class="w-4 h-4" />
                {{ approving === detail.id ? '审批中...' : '批准' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </teleport>

    <teleport to="body">
      <div
        v-if="showRejectModal"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
      >
        <div class="fixed inset-0 bg-black/50" @click="showRejectModal = false"></div>
        <div class="relative bg-white dark:bg-gray-800 rounded-xl shadow-xl w-full max-w-md border border-gray-200 dark:border-gray-700">
          <div class="px-6 py-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white">驳回借展申请</h3>
            <button @click="showRejectModal = false" class="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 transition-colors">
              <X class="w-5 h-5" />
            </button>
          </div>
          <div class="p-6">
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">驳回原因 <span class="text-red-500">*</span></label>
            <textarea
              v-model="rejectReason"
              rows="4"
              placeholder="请输入驳回原因..."
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none text-sm resize-none"
            ></textarea>
          </div>
          <div class="px-6 py-4 border-t border-gray-200 dark:border-gray-700 flex justify-end gap-3">
            <button
              @click="showRejectModal = false"
              class="px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 text-sm font-medium hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
            >
              取消
            </button>
            <button
              @click="submitReject"
              :disabled="rejecting || !rejectReason.trim()"
              class="px-4 py-2 rounded-lg bg-red-600 hover:bg-red-700 text-white text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {{ rejecting ? '提交中...' : '确认驳回' }}
            </button>
          </div>
        </div>
      </div>
    </teleport>
  </div>
</template>
