<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ClipboardCheck, Search, AlertTriangle, CheckCircle, XCircle, Eye, FileText, Shield } from 'lucide-vue-next'
import { api, type Disease, type DiseaseReview, type ExhibitionCondition, type Artifact, type Loan } from '@/api'
import { useAuth } from '@/stores/auth'

const { hasRole } = useAuth()

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

const SEVERITY_LABELS: Record<string, string> = {
  MILD: '轻度',
  MODERATE: '中度',
  SEVERE: '重度',
}

const SEVERITY_COLORS: Record<string, string> = {
  MILD: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
  MODERATE: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  SEVERE: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
}

const REVIEW_STATUS_LABELS: Record<string, string> = {
  PENDING: '待复核',
  REVIEWED: '已复核-无影响',
  REVIEWED_WITH_IMPACT: '已复核-影响借展',
}

const REVIEW_STATUS_COLORS: Record<string, string> = {
  PENDING: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  REVIEWED: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
  REVIEWED_WITH_IMPACT: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
}

const diseases = ref<Disease[]>([])
const artifacts = ref<Artifact[]>([])
const loans = ref<Loan[]>([])
const totalElements = ref(0)
const page = ref(0)
const size = ref(20)
const loading = ref(false)
const selectedArtifactId = ref<number | ''>('')
const selectedLoanId = ref<number | ''>('')
const showReviewModal = ref(false)
const showConditionModal = ref(false)
const submitting = ref(false)
const generatingCondition = ref(false)
const selectedDisease = ref<Disease | null>(null)
const selectedReview = ref<DiseaseReview | null>(null)
const generatedCondition = ref<ExhibitionCondition | null>(null)

const reviewForm = ref({
  affectsLoanGrade: false,
  loanGradeImpact: '',
  reviewNotes: '',
})

const totalPages = computed(() => Math.ceil(totalElements.value / size.value))

const pendingCount = computed(() => diseases.value.filter(d => d.reviewStatus === 'PENDING' || !d.reviewStatus).length)

async function loadDiseases() {
  loading.value = true
  try {
    const params: Record<string, any> = { page: page.value, size: size.value, reviewStatus: 'PENDING' }
    if (selectedArtifactId.value !== '') {
      params.artifactId = selectedArtifactId.value
    }
    const res = await api.diseases.list(params)
    diseases.value = res.data.content
    totalElements.value = res.data.totalElements
  } catch {
    diseases.value = []
    totalElements.value = 0
  } finally {
    loading.value = false
  }
}

async function loadArtifacts() {
  try {
    const res = await api.artifacts.list({ page: 0, size: 100 })
    artifacts.value = res.data.content
  } catch {
    artifacts.value = []
  }
}

async function loadLoans() {
  try {
    const res = await api.loans.list({ page: 0, size: 100, status: 'PENDING' })
    loans.value = res.data.content
  } catch {
    loans.value = []
  }
}

function handleSearch() {
  page.value = 0
  loadDiseases()
}

function changePage(delta: number) {
  const next = page.value + delta
  if (next >= 0 && next < totalPages.value) {
    page.value = next
    loadDiseases()
  }
}

async function openReviewModal(disease: Disease) {
  selectedDisease.value = disease
  reviewForm.value = {
    affectsLoanGrade: false,
    loanGradeImpact: '',
    reviewNotes: '',
  }
  try {
    const res = await api.diseases.get(disease.id)
    selectedDisease.value = res.data
  } catch {
  }
  showReviewModal.value = true
}

async function submitReview() {
  if (!selectedDisease.value) return
  submitting.value = true
  try {
    const loanId = selectedLoanId.value !== '' ? Number(selectedLoanId.value) : null
    const res = await api.diseases.review(selectedDisease.value.id, {
      loanId,
      affectsLoanGrade: reviewForm.value.affectsLoanGrade,
      loanGradeImpact: reviewForm.value.loanGradeImpact || null,
      reviewNotes: reviewForm.value.reviewNotes || null,
    })
    selectedReview.value = res.data
    showReviewModal.value = false
    
    if (reviewForm.value.affectsLoanGrade && loanId) {
      await generateCondition(res.data.id, loanId)
    }
    
    await loadDiseases()
  } catch {
  } finally {
    submitting.value = false
  }
}

async function generateCondition(reviewId: number, loanId: number) {
  generatingCondition.value = true
  try {
    const res = await api.diseases.generateExhibitionCondition(reviewId, loanId)
    generatedCondition.value = res.data
    showConditionModal.value = true
  } catch {
  } finally {
    generatingCondition.value = false
  }
}

async function viewReviews(disease: Disease) {
  selectedDisease.value = disease
  try {
    const res = await api.diseases.listReviews(disease.id)
    if (res.data.length > 0) {
      selectedReview.value = res.data[0]
      if (res.data[0].exhibitionCondition) {
        generatedCondition.value = res.data[0].exhibitionCondition
      }
      showConditionModal.value = true
    }
  } catch {
  }
}

onMounted(() => {
  loadArtifacts()
  loadLoans()
  loadDiseases()
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="p-2 rounded-lg bg-blue-100 dark:bg-blue-900/30">
          <ClipboardCheck class="w-5 h-5 text-blue-600 dark:text-blue-400" />
        </div>
        <div>
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white">病害图谱复核</h2>
          <p class="text-sm text-gray-500 dark:text-gray-400">藏品部病害复核与借展等级评估</p>
        </div>
      </div>
      <div class="flex items-center gap-2 px-4 py-2 rounded-lg bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800">
        <AlertTriangle class="w-4 h-4 text-amber-600 dark:text-amber-400" />
        <span class="text-sm text-amber-700 dark:text-amber-400">待复核: {{ pendingCount }} 条</span>
      </div>
    </div>

    <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
      <div class="flex flex-wrap items-center gap-4">
        <select
          v-model="selectedArtifactId"
          @change="handleSearch"
          class="flex-1 max-w-xs px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">全部文物</option>
          <option v-for="a in artifacts" :key="a.id" :value="a.id">{{ a.name }} ({{ a.artifactCode }})</option>
        </select>
        <button
          @click="handleSearch"
          class="flex items-center gap-2 px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium transition-colors"
        >
          <Search class="w-4 h-4" />
          查询
        </button>
      </div>
    </div>

    <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-700/50">
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">文物编号</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">病害类型</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">严重程度</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">部位</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">报告人</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">日期</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="7" class="px-4 py-8 text-center text-gray-400 dark:text-gray-500">加载中...</td>
            </tr>
            <tr v-else-if="diseases.length === 0">
              <td colspan="7" class="px-4 py-8 text-center text-gray-400 dark:text-gray-500">暂无待复核病害</td>
            </tr>
            <tr
              v-for="d in diseases"
              :key="d.id"
              class="border-b border-gray-100 dark:border-gray-700/50 hover:bg-gray-50 dark:hover:bg-gray-700/30"
            >
              <td class="px-4 py-3 text-gray-900 dark:text-white font-mono text-xs">{{ artifacts.find(a => a.id === d.artifactId)?.artifactCode || '-' }}</td>
              <td class="px-4 py-3 text-gray-900 dark:text-white">{{ DISEASE_TYPE_LABELS[d.diseaseType] || d.diseaseType }}</td>
              <td class="px-4 py-3">
                <span :class="['inline-flex px-2 py-0.5 rounded-full text-xs font-medium', SEVERITY_COLORS[d.severity] || '']">
                  {{ SEVERITY_LABELS[d.severity] || d.severity }}
                </span>
              </td>
              <td class="px-4 py-3 text-gray-700 dark:text-gray-300">{{ d.location || '-' }}</td>
              <td class="px-4 py-3 text-gray-700 dark:text-gray-300">{{ d.reporterName || '-' }}</td>
              <td class="px-4 py-3 text-gray-500 dark:text-gray-400 text-xs">{{ d.reportedAt?.substring(0, 10) }}</td>
              <td class="px-4 py-3">
                <div class="flex items-center gap-1">
                  <button
                    @click="openReviewModal(d)"
                    v-if="hasRole('COLLECTION') || hasRole('ADMIN')"
                    class="p-1.5 rounded-lg hover:bg-blue-100 dark:hover:bg-blue-900/20 text-gray-500 hover:text-blue-600 transition-colors"
                    title="复核"
                  >
                    <ClipboardCheck class="w-4 h-4" />
                  </button>
                  <button
                    @click="viewReviews(d)"
                    v-if="d.latestReview"
                    class="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 hover:text-amber-600 transition-colors"
                    title="查看复核记录"
                  >
                    <Eye class="w-4 h-4" />
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
            class="px-3 py-1 rounded-lg border border-gray-300 dark:border-gray-600 text-sm disabled:opacity-40 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
          >
            上一页
          </button>
          <span class="text-sm text-gray-700 dark:text-gray-300">{{ page + 1 }} / {{ totalPages }}</span>
          <button
            :disabled="page >= totalPages - 1"
            @click="changePage(1)"
            class="px-3 py-1 rounded-lg border border-gray-300 dark:border-gray-600 text-sm disabled:opacity-40 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
          >
            下一页
          </button>
        </div>
      </div>
    </div>

    <div v-if="showReviewModal && selectedDisease" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" @click.self="showReviewModal = false">
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div class="sticky top-0 bg-white dark:bg-gray-800 px-6 py-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between z-10">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white">病害复核</h3>
          <button @click="showReviewModal = false" class="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 transition-colors">
            <XCircle class="w-5 h-5" />
          </button>
        </div>
        <div class="p-6 space-y-6">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">文物</p>
              <p class="text-sm font-medium text-gray-900 dark:text-white">{{ artifacts.find(a => a.id === selectedDisease.artifactId)?.name || '-' }}</p>
            </div>
            <div>
              <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">病害类型</p>
              <p class="text-sm font-medium text-gray-900 dark:text-white">{{ DISEASE_TYPE_LABELS[selectedDisease.diseaseType] || selectedDisease.diseaseType }}</p>
            </div>
            <div>
              <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">严重程度</p>
              <span :class="['inline-flex px-2 py-0.5 rounded-full text-xs font-medium', SEVERITY_COLORS[selectedDisease.severity] || '']">
                {{ SEVERITY_LABELS[selectedDisease.severity] || selectedDisease.severity }}
              </span>
            </div>
            <div>
              <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">部位</p>
              <p class="text-sm text-gray-900 dark:text-white">{{ selectedDisease.location || '-' }}</p>
            </div>
          </div>

          <div v-if="selectedDisease.hdPhotoUrl || selectedDisease.photoUrl" class="relative w-full h-64 bg-gray-100 dark:bg-gray-700 rounded-lg overflow-hidden">
            <img
              :src="selectedDisease.hdPhotoUrl || selectedDisease.photoUrl || ''"
              :alt="selectedDisease.description || '病害图片'"
              class="w-full h-full object-contain"
            />
            <div
              v-if="selectedDisease.annotationX && selectedDisease.annotationWidth && selectedDisease.annotationWidth > 0"
              class="absolute border-2 border-amber-500 bg-amber-500/20 pointer-events-none"
              :style="{
                left: selectedDisease.annotationX + '%',
                top: selectedDisease.annotationY + '%',
                width: selectedDisease.annotationWidth + '%',
                height: selectedDisease.annotationHeight + '%',
              }"
            >
              <div class="absolute -top-6 left-0 bg-amber-600 text-white text-xs px-1.5 py-0.5 rounded whitespace-nowrap">
                病害区域
              </div>
            </div>
          </div>

          <div v-if="selectedDisease.description" class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-4">
            <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-2">病害描述</h4>
            <p class="text-sm text-gray-700 dark:text-gray-300">{{ selectedDisease.description }}</p>
          </div>

          <div class="border-t border-gray-200 dark:border-gray-700 pt-4">
            <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-4">复核意见</h4>
            <form @submit.prevent="submitReview" class="space-y-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">关联借展申请</label>
                <select
                  v-model="selectedLoanId"
                  class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">不关联</option>
                  <option v-for="l in loans" :key="l.id" :value="l.id">
                    {{ l.artifactName }} - {{ l.borrowingInstitution }} ({{ l.exhibitionName }})
                  </option>
                </select>
              </div>

              <div class="flex items-center gap-3">
                <label class="flex items-center gap-2 cursor-pointer">
                  <input
                    v-model="reviewForm.affectsLoanGrade"
                    type="radio"
                    :value="true"
                    class="w-4 h-4 text-red-600 focus:ring-red-500"
                  />
                  <span class="text-sm text-gray-700 dark:text-gray-300">影响借展等级</span>
                </label>
                <label class="flex items-center gap-2 cursor-pointer">
                  <input
                    v-model="reviewForm.affectsLoanGrade"
                    type="radio"
                    :value="false"
                    class="w-4 h-4 text-green-600 focus:ring-green-500"
                  />
                  <span class="text-sm text-gray-700 dark:text-gray-300">不影响借展等级</span>
                </label>
              </div>

              <div v-if="reviewForm.affectsLoanGrade">
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">借展等级影响说明 <span class="text-red-500">*</span></label>
                <textarea
                  v-model="reviewForm.loanGradeImpact"
                  rows="2"
                  required
                  placeholder="请说明对借展等级的具体影响..."
                  class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-blue-500 resize-none"
                />
              </div>

              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">复核意见</label>
                <textarea
                  v-model="reviewForm.reviewNotes"
                  rows="3"
                  placeholder="请输入复核意见..."
                  class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-blue-500 resize-none"
                />
              </div>

              <div v-if="reviewForm.affectsLoanGrade && selectedLoanId !== ''" class="bg-amber-50 dark:bg-amber-900/20 rounded-lg p-4 border border-amber-200 dark:border-amber-800">
                <div class="flex items-start gap-2">
                  <AlertTriangle class="w-4 h-4 text-amber-600 dark:text-amber-400 flex-shrink-0 mt-0.5" />
                  <div>
                    <p class="text-sm font-medium text-amber-700 dark:text-amber-400">系统提示</p>
                    <p class="text-xs text-amber-600 dark:text-amber-500 mt-1">
                      标记为影响借展等级后，系统将自动生成展示条件要求，包括展示距离、照度上限、支架要求和保险附加条件。
                      如果外借展馆坚持参展，这些条件将作为借展合同的必要条款。
                    </p>
                  </div>
                </div>
              </div>

              <div class="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  @click="showReviewModal = false"
                  class="px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                >
                  取消
                </button>
                <button
                  type="submit"
                  :disabled="submitting || (reviewForm.affectsLoanGrade && !reviewForm.loanGradeImpact.trim())"
                  class="px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white text-sm font-medium transition-colors"
                >
                  {{ submitting ? '提交中...' : '提交复核' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showConditionModal && generatedCondition" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" @click.self="showConditionModal = false">
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div class="sticky top-0 bg-white dark:bg-gray-800 px-6 py-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between z-10">
          <div class="flex items-center gap-2">
            <Shield class="w-5 h-5 text-amber-600" />
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white">展示条件要求</h3>
          </div>
          <button @click="showConditionModal = false" class="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 transition-colors">
            <XCircle class="w-5 h-5" />
          </button>
        </div>
        <div class="p-6 space-y-6">
          <div class="bg-amber-50 dark:bg-amber-900/20 rounded-lg p-4 border border-amber-200 dark:border-amber-800">
            <p class="text-sm text-amber-700 dark:text-amber-400">
              根据病害情况，系统自动生成以下展示条件要求。如外借展馆坚持参展，必须满足以下全部条件。
            </p>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-4">
              <div class="flex items-center gap-2 mb-2">
                <FileText class="w-4 h-4 text-blue-600" />
                <p class="text-xs text-gray-500 dark:text-gray-400">最小展示距离</p>
              </div>
              <p class="text-xl font-bold text-gray-900 dark:text-white">{{ generatedCondition.displayDistanceMin }} 米</p>
            </div>
            <div class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-4">
              <div class="flex items-center gap-2 mb-2">
                <FileText class="w-4 h-4 text-yellow-600" />
                <p class="text-xs text-gray-500 dark:text-gray-400">照度上限</p>
              </div>
              <p class="text-xl font-bold text-gray-900 dark:text-white">{{ generatedCondition.illuminanceLimit }} lux</p>
            </div>
          </div>

          <div class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-4">
            <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-2">支架要求</h4>
            <p class="text-sm text-gray-700 dark:text-gray-300">{{ generatedCondition.bracketRequirements }}</p>
          </div>

          <div class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-4">
            <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-2">保险附加条件</h4>
            <p class="text-sm text-gray-700 dark:text-gray-300">{{ generatedCondition.insuranceAdditionalTerms }}</p>
          </div>

          <div v-if="generatedCondition.otherRequirements" class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-4">
            <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-2">其他要求</h4>
            <p class="text-sm text-gray-700 dark:text-gray-300">{{ generatedCondition.otherRequirements }}</p>
          </div>

          <div v-if="selectedReview" class="border-t border-gray-200 dark:border-gray-700 pt-4">
            <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-3">复核信息</h4>
            <div class="grid grid-cols-2 gap-4 text-sm">
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400">复核人</p>
                <p class="text-gray-900 dark:text-white">{{ selectedReview.reviewerName || '-' }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400">复核时间</p>
                <p class="text-gray-900 dark:text-white">{{ selectedReview.reviewedAt?.substring(0, 16) }}</p>
              </div>
              <div v-if="selectedReview.loanGradeImpact" class="col-span-2">
                <p class="text-xs text-gray-500 dark:text-gray-400">借展等级影响</p>
                <p class="text-gray-900 dark:text-white">{{ selectedReview.loanGradeImpact }}</p>
              </div>
              <div v-if="selectedReview.reviewNotes" class="col-span-2">
                <p class="text-xs text-gray-500 dark:text-gray-400">复核意见</p>
                <p class="text-gray-900 dark:text-white">{{ selectedReview.reviewNotes }}</p>
              </div>
            </div>
          </div>

          <div class="flex justify-end">
            <button
              @click="showConditionModal = false"
              class="px-4 py-2 rounded-lg bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600 text-gray-700 dark:text-gray-300 text-sm font-medium transition-colors"
            >
              关闭
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
