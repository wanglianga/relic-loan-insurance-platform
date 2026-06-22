<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Bug, Search, Plus, AlertTriangle, Eye, CheckCircle, XCircle, Clock } from 'lucide-vue-next'
import { api, type Disease, type Artifact, type PageResult, type ApiResponse } from '@/api'
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
const totalElements = ref(0)
const page = ref(0)
const size = ref(20)
const loading = ref(false)
const selectedArtifactId = ref<number | ''>('')
const selectedReviewStatus = ref('')
const showAddModal = ref(false)
const showDetailModal = ref(false)
const submitting = ref(false)
const selectedDisease = ref<Disease | null>(null)
const isAnnotating = ref(false)
const annotationStart = ref<{ x: number; y: number } | null>(null)
const currentAnnotation = ref<{ x: number; y: number; width: number; height: number } | null>(null)
const imageContainerRef = ref<HTMLDivElement | null>(null)

const form = ref({
  artifactId: '' as number | '',
  diseaseType: '',
  severity: '',
  location: '',
  description: '',
  hdPhotoUrl: '',
})

const typeDistribution = computed(() => {
  const map: Record<string, number> = {}
  for (const d of diseases.value) {
    const label = DISEASE_TYPE_LABELS[d.diseaseType] || d.diseaseType
    map[label] = (map[label] || 0) + 1
  }
  return Object.entries(map).map(([type, count]) => ({ type, count })).sort((a, b) => b.count - a.count)
})

const totalPages = computed(() => Math.ceil(totalElements.value / size.value))

const severityStats = computed(() => {
  let mild = 0, moderate = 0, severe = 0
  for (const d of diseases.value) {
    if (d.severity === 'MILD') mild++
    else if (d.severity === 'MODERATE') moderate++
    else if (d.severity === 'SEVERE') severe++
  }
  return { mild, moderate, severe }
})

const reviewStats = computed(() => {
  let pending = 0, reviewed = 0, impacted = 0
  for (const d of diseases.value) {
    if (d.reviewStatus === 'PENDING' || !d.reviewStatus) pending++
    else if (d.reviewStatus === 'REVIEWED') reviewed++
    else if (d.reviewStatus === 'REVIEWED_WITH_IMPACT') impacted++
  }
  return { pending, reviewed, impacted }
})

async function loadDiseases() {
  loading.value = true
  try {
    const params: Record<string, any> = { page: page.value, size: size.value }
    if (selectedArtifactId.value !== '') {
      params.artifactId = selectedArtifactId.value
    }
    if (selectedReviewStatus.value !== '') {
      params.reviewStatus = selectedReviewStatus.value
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

function openAddModal() {
  form.value = { artifactId: '', diseaseType: '', severity: '', location: '', description: '', hdPhotoUrl: '' }
  showAddModal.value = true
}

async function submitDisease() {
  if (form.value.artifactId === '' || !form.value.diseaseType || !form.value.severity) return
  submitting.value = true
  try {
    await api.diseases.create({
      artifactId: form.value.artifactId as number,
      diseaseType: form.value.diseaseType,
      severity: form.value.severity,
      location: form.value.location || null,
      description: form.value.description || null,
      hdPhotoUrl: form.value.hdPhotoUrl || null,
    })
    showAddModal.value = false
    await loadDiseases()
  } catch {
  } finally {
    submitting.value = false
  }
}

async function openDetailModal(disease: Disease) {
  selectedDisease.value = disease
  currentAnnotation.value = null
  annotationStart.value = null
  isAnnotating.value = false
  try {
    const res = await api.diseases.get(disease.id)
    selectedDisease.value = res.data
    if (res.data.annotationX && res.data.annotationY && res.data.annotationWidth && res.data.annotationHeight) {
      currentAnnotation.value = {
        x: Number(res.data.annotationX),
        y: Number(res.data.annotationY),
        width: Number(res.data.annotationWidth),
        height: Number(res.data.annotationHeight),
      }
    }
  } catch {
  }
  showDetailModal.value = true
}

function startAnnotation(e: MouseEvent) {
  if (!isAnnotating.value || !imageContainerRef.value) return
  const rect = imageContainerRef.value.getBoundingClientRect()
  const x = ((e.clientX - rect.left) / rect.width) * 100
  const y = ((e.clientY - rect.top) / rect.height) * 100
  annotationStart.value = { x, y }
  currentAnnotation.value = { x, y, width: 0, height: 0 }
}

function updateAnnotation(e: MouseEvent) {
  if (!isAnnotating.value || !annotationStart.value || !imageContainerRef.value) return
  const rect = imageContainerRef.value.getBoundingClientRect()
  const x = ((e.clientX - rect.left) / rect.width) * 100
  const y = ((e.clientY - rect.top) / rect.height) * 100
  currentAnnotation.value = {
    x: Math.min(annotationStart.value.x, x),
    y: Math.min(annotationStart.value.y, y),
    width: Math.abs(x - annotationStart.value.x),
    height: Math.abs(y - annotationStart.value.y),
  }
}

function endAnnotation() {
  if (!isAnnotating.value) return
  annotationStart.value = null
  isAnnotating.value = false
}

async function saveAnnotation() {
  if (!selectedDisease.value || !currentAnnotation.value) return
  try {
    await api.diseases.updateAnnotation(selectedDisease.value.id, {
      annotationX: currentAnnotation.value.x,
      annotationY: currentAnnotation.value.y,
      annotationWidth: currentAnnotation.value.width,
      annotationHeight: currentAnnotation.value.height,
    })
    const res = await api.diseases.get(selectedDisease.value.id)
    selectedDisease.value = res.data
    loadDiseases()
  } catch {
  }
}

onMounted(() => {
  loadArtifacts()
  loadDiseases()
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="p-2 rounded-lg bg-amber-100 dark:bg-amber-900/30">
          <Bug class="w-5 h-5 text-amber-600 dark:text-amber-400" />
        </div>
        <div>
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white">病害图谱</h2>
          <p class="text-sm text-gray-500 dark:text-gray-400">文物病害记录、标注与分布统计</p>
        </div>
      </div>
      <button
        v-if="hasRole('RESTORER')"
        @click="openAddModal"
        class="flex items-center gap-2 px-4 py-2 rounded-lg bg-amber-600 hover:bg-amber-700 text-white text-sm font-medium transition-colors"
      >
        <Plus class="w-4 h-4" />
        新增病害
      </button>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
        <div class="flex items-center gap-2 mb-1">
          <div class="w-2 h-2 rounded-full bg-green-500" />
          <span class="text-sm text-gray-500 dark:text-gray-400">轻度</span>
        </div>
        <p class="text-2xl font-bold text-gray-900 dark:text-white">{{ severityStats.mild }}</p>
      </div>
      <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
        <div class="flex items-center gap-2 mb-1">
          <div class="w-2 h-2 rounded-full bg-amber-500" />
          <span class="text-sm text-gray-500 dark:text-gray-400">中度</span>
        </div>
        <p class="text-2xl font-bold text-gray-900 dark:text-white">{{ severityStats.moderate }}</p>
      </div>
      <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
        <div class="flex items-center gap-2 mb-1">
          <div class="w-2 h-2 rounded-full bg-red-500" />
          <span class="text-sm text-gray-500 dark:text-gray-400">重度</span>
        </div>
        <p class="text-2xl font-bold text-gray-900 dark:text-white">{{ severityStats.severe }}</p>
      </div>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
        <div class="flex items-center gap-2 mb-1">
          <Clock class="w-4 h-4 text-amber-500" />
          <span class="text-sm text-gray-500 dark:text-gray-400">待复核</span>
        </div>
        <p class="text-2xl font-bold text-gray-900 dark:text-white">{{ reviewStats.pending }}</p>
      </div>
      <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
        <div class="flex items-center gap-2 mb-1">
          <CheckCircle class="w-4 h-4 text-green-500" />
          <span class="text-sm text-gray-500 dark:text-gray-400">已复核-无影响</span>
        </div>
        <p class="text-2xl font-bold text-gray-900 dark:text-white">{{ reviewStats.reviewed }}</p>
      </div>
      <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
        <div class="flex items-center gap-2 mb-1">
          <XCircle class="w-4 h-4 text-red-500" />
          <span class="text-sm text-gray-500 dark:text-gray-400">已复核-影响借展</span>
        </div>
        <p class="text-2xl font-bold text-gray-900 dark:text-white">{{ reviewStats.impacted }}</p>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-4 gap-6">
      <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
        <h3 class="text-sm font-medium text-gray-900 dark:text-white mb-4">病害类型分布</h3>
        <div v-if="typeDistribution.length === 0" class="text-sm text-gray-400 dark:text-gray-500 text-center py-4">暂无数据</div>
        <div v-else class="space-y-3">
          <div v-for="item in typeDistribution" :key="item.type" class="flex items-center justify-between">
            <span class="text-sm text-gray-700 dark:text-gray-300">{{ item.type }}</span>
            <span class="text-sm font-medium text-amber-600 dark:text-amber-400">{{ item.count }}</span>
          </div>
        </div>
      </div>

      <div class="lg:col-span-3 space-y-4">
        <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
          <div class="flex flex-wrap items-center gap-4">
            <select
              v-model="selectedArtifactId"
              @change="handleSearch"
              class="flex-1 max-w-xs px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
            >
              <option value="">全部文物</option>
              <option v-for="a in artifacts" :key="a.id" :value="a.id">{{ a.name }} ({{ a.artifactCode }})</option>
            </select>
            <select
              v-model="selectedReviewStatus"
              @change="handleSearch"
              class="flex-1 max-w-xs px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
            >
              <option value="">全部复核状态</option>
              <option v-for="(label, key) in REVIEW_STATUS_LABELS" :key="key" :value="key">{{ label }}</option>
            </select>
            <button
              @click="handleSearch"
              class="flex items-center gap-2 px-4 py-2 rounded-lg bg-amber-600 hover:bg-amber-700 text-white text-sm font-medium transition-colors"
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
                  <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">复核状态</th>
                  <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">部位</th>
                  <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">报告人</th>
                  <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">日期</th>
                  <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="loading">
                  <td colspan="8" class="px-4 py-8 text-center text-gray-400 dark:text-gray-500">加载中...</td>
                </tr>
                <tr v-else-if="diseases.length === 0">
                  <td colspan="8" class="px-4 py-8 text-center text-gray-400 dark:text-gray-500">暂无病害记录</td>
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
                  <td class="px-4 py-3">
                    <span :class="['inline-flex px-2 py-0.5 rounded-full text-xs font-medium', REVIEW_STATUS_COLORS[d.reviewStatus || 'PENDING'] || '']">
                      {{ REVIEW_STATUS_LABELS[d.reviewStatus || 'PENDING'] || '待复核' }}
                    </span>
                  </td>
                  <td class="px-4 py-3 text-gray-700 dark:text-gray-300">{{ d.location || '-' }}</td>
                  <td class="px-4 py-3 text-gray-700 dark:text-gray-300">{{ d.reporterName || '-' }}</td>
                  <td class="px-4 py-3 text-gray-500 dark:text-gray-400 text-xs">{{ d.reportedAt?.substring(0, 10) }}</td>
                  <td class="px-4 py-3">
                    <button
                      @click="openDetailModal(d)"
                      class="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 hover:text-amber-600 transition-colors"
                      title="查看详情"
                    >
                      <Eye class="w-4 h-4" />
                    </button>
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
      </div>
    </div>

    <div v-if="showAddModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50" @click.self="showAddModal = false">
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-lg p-6">
        <div class="flex items-center gap-2 mb-5">
          <AlertTriangle class="w-5 h-5 text-amber-600" />
          <h3 class="text-base font-semibold text-gray-900 dark:text-white">新增病害记录</h3>
        </div>
        <form @submit.prevent="submitDisease" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">文物</label>
            <select
              v-model="form.artifactId"
              required
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
            >
              <option value="" disabled>请选择文物</option>
              <option v-for="a in artifacts" :key="a.id" :value="a.id">{{ a.name }} ({{ a.artifactCode }})</option>
            </select>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">病害类型</label>
              <select
                v-model="form.diseaseType"
                required
                class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
              >
                <option value="" disabled>请选择</option>
                <option v-for="(label, key) in DISEASE_TYPE_LABELS" :key="key" :value="key">{{ label }}</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">严重程度</label>
              <select
                v-model="form.severity"
                required
                class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
              >
                <option value="" disabled>请选择</option>
                <option v-for="(label, key) in SEVERITY_LABELS" :key="key" :value="key">{{ label }}</option>
              </select>
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">高清图URL</label>
            <input
              v-model="form.hdPhotoUrl"
              type="text"
              placeholder="https://example.com/hd-photo.jpg"
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">部位</label>
            <input
              v-model="form.location"
              type="text"
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">描述</label>
            <textarea
              v-model="form.description"
              rows="3"
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500 resize-none"
            />
          </div>
          <div class="flex justify-end gap-3 pt-2">
            <button
              type="button"
              @click="showAddModal = false"
              class="px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
            >
              取消
            </button>
            <button
              type="submit"
              :disabled="submitting"
              class="px-4 py-2 rounded-lg bg-amber-600 hover:bg-amber-700 disabled:bg-amber-400 text-white text-sm font-medium transition-colors"
            >
              {{ submitting ? '提交中...' : '提交' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="showDetailModal && selectedDisease" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" @click.self="showDetailModal = false">
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-4xl max-h-[90vh] overflow-y-auto">
        <div class="sticky top-0 bg-white dark:bg-gray-800 px-6 py-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between z-10">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white">病害详情 - 高清图标注</h3>
          <button @click="showDetailModal = false" class="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 transition-colors">
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
              <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">复核状态</p>
              <span :class="['inline-flex px-2 py-0.5 rounded-full text-xs font-medium', REVIEW_STATUS_COLORS[selectedDisease.reviewStatus || 'PENDING'] || '']">
                {{ REVIEW_STATUS_LABELS[selectedDisease.reviewStatus || 'PENDING'] || '待复核' }}
              </span>
            </div>
          </div>

          <div>
            <div class="flex items-center justify-between mb-3">
              <h4 class="text-sm font-semibold text-gray-900 dark:text-white">高清图标注</h4>
              <div class="flex gap-2">
                <button
                  v-if="hasRole('RESTORER')"
                  @click="isAnnotating = !isAnnotating"
                  :class="[
                    'px-3 py-1.5 rounded-lg text-sm font-medium transition-colors',
                    isAnnotating
                      ? 'bg-red-600 hover:bg-red-700 text-white'
                      : 'bg-amber-600 hover:bg-amber-700 text-white'
                  ]"
                >
                  {{ isAnnotating ? '取消标注' : '开始标注' }}
                </button>
                <button
                  v-if="currentAnnotation && hasRole('RESTORER')"
                  @click="saveAnnotation"
                  class="px-3 py-1.5 rounded-lg bg-green-600 hover:bg-green-700 text-white text-sm font-medium transition-colors"
                >
                  保存标注
                </button>
              </div>
            </div>
            <div
              ref="imageContainerRef"
              @mousedown="startAnnotation"
              @mousemove="updateAnnotation"
              @mouseup="endAnnotation"
              @mouseleave="endAnnotation"
              :class="[
                'relative w-full h-96 bg-gray-100 dark:bg-gray-700 rounded-lg overflow-hidden border-2 border-dashed',
                isAnnotating ? 'border-amber-500 cursor-crosshair' : 'border-gray-300 dark:border-gray-600'
              ]"
            >
              <div v-if="!selectedDisease.hdPhotoUrl && !selectedDisease.photoUrl" class="absolute inset-0 flex items-center justify-center text-gray-400">
                暂无高清图片
              </div>
              <img
                v-else
                :src="selectedDisease.hdPhotoUrl || selectedDisease.photoUrl || ''"
                :alt="selectedDisease.description || '病害图片'"
                class="w-full h-full object-contain"
                draggable="false"
              />
              <div
                v-if="currentAnnotation && currentAnnotation.width > 0 && currentAnnotation.height > 0"
                class="absolute border-2 border-amber-500 bg-amber-500/20 pointer-events-none"
                :style="{
                  left: currentAnnotation.x + '%',
                  top: currentAnnotation.y + '%',
                  width: currentAnnotation.width + '%',
                  height: currentAnnotation.height + '%',
                }"
              >
                <div class="absolute -top-6 left-0 bg-amber-600 text-white text-xs px-1.5 py-0.5 rounded whitespace-nowrap">
                  病害区域
                </div>
              </div>
              <div v-if="isAnnotating" class="absolute top-2 left-2 bg-black/70 text-white text-xs px-2 py-1 rounded">
                鼠标拖动框选病害区域
              </div>
            </div>
            <p v-if="isAnnotating" class="text-xs text-amber-600 dark:text-amber-400 mt-2">
              提示：按住鼠标左键拖动绘制矩形框，标注病害位置和范围
            </p>
          </div>

          <div v-if="selectedDisease.annotationX" class="bg-amber-50 dark:bg-amber-900/20 rounded-lg p-4">
            <h4 class="text-sm font-semibold text-amber-700 dark:text-amber-400 mb-2">已有标注信息</h4>
            <div class="grid grid-cols-4 gap-4 text-sm">
              <div>
                <p class="text-xs text-amber-600 dark:text-amber-500">X坐标</p>
                <p class="text-gray-900 dark:text-white">{{ selectedDisease.annotationX?.toFixed(2) }}%</p>
              </div>
              <div>
                <p class="text-xs text-amber-600 dark:text-amber-500">Y坐标</p>
                <p class="text-gray-900 dark:text-white">{{ selectedDisease.annotationY?.toFixed(2) }}%</p>
              </div>
              <div>
                <p class="text-xs text-amber-600 dark:text-amber-500">宽度</p>
                <p class="text-gray-900 dark:text-white">{{ selectedDisease.annotationWidth?.toFixed(2) }}%</p>
              </div>
              <div>
                <p class="text-xs text-amber-600 dark:text-amber-500">高度</p>
                <p class="text-gray-900 dark:text-white">{{ selectedDisease.annotationHeight?.toFixed(2) }}%</p>
              </div>
            </div>
          </div>

          <div v-if="selectedDisease.latestReview" class="border-t border-gray-200 dark:border-gray-700 pt-4">
            <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-3">最新复核记录</h4>
            <div class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-4">
              <div class="grid grid-cols-2 gap-4 mb-3">
                <div>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">复核人</p>
                  <p class="text-sm text-gray-900 dark:text-white">{{ selectedDisease.latestReview.reviewerName || '-' }}</p>
                </div>
                <div>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">复核时间</p>
                  <p class="text-sm text-gray-900 dark:text-white">{{ selectedDisease.latestReview.reviewedAt?.substring(0, 16) }}</p>
                </div>
                <div class="col-span-2">
                  <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">是否影响借展等级</p>
                  <p :class="['text-sm font-medium', selectedDisease.latestReview.affectsLoanGrade ? 'text-red-600 dark:text-red-400' : 'text-green-600 dark:text-green-400']">
                    {{ selectedDisease.latestReview.affectsLoanGrade ? '是' : '否' }}
                  </p>
                </div>
                <div v-if="selectedDisease.latestReview.loanGradeImpact" class="col-span-2">
                  <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">借展等级影响说明</p>
                  <p class="text-sm text-gray-900 dark:text-white">{{ selectedDisease.latestReview.loanGradeImpact }}</p>
                </div>
                <div v-if="selectedDisease.latestReview.reviewNotes" class="col-span-2">
                  <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">复核意见</p>
                  <p class="text-sm text-gray-900 dark:text-white">{{ selectedDisease.latestReview.reviewNotes }}</p>
                </div>
              </div>
              <div v-if="selectedDisease.latestReview.exhibitionCondition" class="border-t border-gray-200 dark:border-gray-600 pt-3 mt-3">
                <h5 class="text-xs font-semibold text-amber-600 dark:text-amber-400 mb-2">展示条件要求</h5>
                <div class="grid grid-cols-2 gap-3 text-sm">
                  <div>
                    <p class="text-xs text-gray-500 dark:text-gray-400">最小展示距离</p>
                    <p class="text-gray-900 dark:text-white">{{ selectedDisease.latestReview.exhibitionCondition.displayDistanceMin }} 米</p>
                  </div>
                  <div>
                    <p class="text-xs text-gray-500 dark:text-gray-400">照度上限</p>
                    <p class="text-gray-900 dark:text-white">{{ selectedDisease.latestReview.exhibitionCondition.illuminanceLimit }} lux</p>
                  </div>
                  <div class="col-span-2">
                    <p class="text-xs text-gray-500 dark:text-gray-400">支架要求</p>
                    <p class="text-gray-900 dark:text-white">{{ selectedDisease.latestReview.exhibitionCondition.bracketRequirements }}</p>
                  </div>
                  <div class="col-span-2">
                    <p class="text-xs text-gray-500 dark:text-gray-400">保险附加条件</p>
                    <p class="text-gray-900 dark:text-white">{{ selectedDisease.latestReview.exhibitionCondition.insuranceAdditionalTerms }}</p>
                  </div>
                  <div v-if="selectedDisease.latestReview.exhibitionCondition.otherRequirements" class="col-span-2">
                    <p class="text-xs text-gray-500 dark:text-gray-400">其他要求</p>
                    <p class="text-gray-900 dark:text-white">{{ selectedDisease.latestReview.exhibitionCondition.otherRequirements }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="selectedDisease.description" class="border-t border-gray-200 dark:border-gray-700 pt-4">
            <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-2">病害描述</h4>
            <p class="text-sm text-gray-700 dark:text-gray-300">{{ selectedDisease.description }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
