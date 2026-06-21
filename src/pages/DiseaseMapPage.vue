<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Bug, Search, Plus, AlertTriangle } from 'lucide-vue-next'
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

const diseases = ref<Disease[]>([])
const artifacts = ref<Artifact[]>([])
const totalElements = ref(0)
const page = ref(0)
const size = ref(20)
const loading = ref(false)
const selectedArtifactId = ref<number | ''>('')
const showAddModal = ref(false)
const submitting = ref(false)

const form = ref({
  artifactId: '' as number | '',
  diseaseType: '',
  severity: '',
  location: '',
  description: '',
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

async function loadDiseases() {
  loading.value = true
  try {
    const params: Record<string, any> = { page: page.value, size: size.value }
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
  form.value = { artifactId: '', diseaseType: '', severity: '', location: '', description: '' }
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
    })
    showAddModal.value = false
    await loadDiseases()
  } catch {
  } finally {
    submitting.value = false
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
          <p class="text-sm text-gray-500 dark:text-gray-400">文物病害记录与分布统计</p>
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
          <div class="flex items-center gap-4">
            <select
              v-model="selectedArtifactId"
              @change="handleSearch"
              class="flex-1 max-w-xs px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
            >
              <option value="">全部文物</option>
              <option v-for="a in artifacts" :key="a.id" :value="a.id">{{ a.name }} ({{ a.artifactCode }})</option>
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
                  <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">部位</th>
                  <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">描述</th>
                  <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">报告人</th>
                  <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">日期</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="loading">
                  <td colspan="7" class="px-4 py-8 text-center text-gray-400 dark:text-gray-500">加载中...</td>
                </tr>
                <tr v-else-if="diseases.length === 0">
                  <td colspan="7" class="px-4 py-8 text-center text-gray-400 dark:text-gray-500">暂无病害记录</td>
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
                  <td class="px-4 py-3 text-gray-700 dark:text-gray-300 max-w-xs truncate">{{ d.description || '-' }}</td>
                  <td class="px-4 py-3 text-gray-700 dark:text-gray-300">{{ d.reporterName || '-' }}</td>
                  <td class="px-4 py-3 text-gray-500 dark:text-gray-400 text-xs">{{ d.reportedAt?.substring(0, 10) }}</td>
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
  </div>
</template>
