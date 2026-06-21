<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Plus, AlertTriangle } from 'lucide-vue-next'
import { api, type Restoration, type Artifact, type PageResult, type ApiResponse } from '@/api'
import { useAuth } from '@/stores/auth'

const router = useRouter()
const { hasRole } = useAuth()

const STATUS_LABELS: Record<string, string> = {
  PROPOSED: '待审批',
  APPROVED: '已批准',
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
}

const STATUS_COLORS: Record<string, string> = {
  PROPOSED: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400',
  APPROVED: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
  IN_PROGRESS: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  COMPLETED: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
}

const restorations = ref<Restoration[]>([])
const artifacts = ref<Artifact[]>([])
const totalElements = ref(0)
const page = ref(0)
const size = ref(20)
const loading = ref(false)
const filterStatus = ref('')
const showAddModal = ref(false)
const showDetailModal = ref(false)
const submitting = ref(false)
const approvingId = ref<number | null>(null)
const detailData = ref<Restoration | null>(null)

const form = ref({
  artifactId: '' as number | '',
  title: '',
  plan: '',
  cleaningMethod: '',
  reinforcementMaterial: '',
})

const totalPages = computed(() => Math.ceil(totalElements.value / size.value))

async function loadRestorations() {
  loading.value = true
  try {
    const params: Record<string, any> = { page: page.value, size: size.value }
    if (filterStatus.value) {
      params.status = filterStatus.value
    }
    const res = await api.restorations.list(params)
    restorations.value = res.data.content
    totalElements.value = res.data.totalElements
  } catch {
    restorations.value = []
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
  loadRestorations()
}

function changePage(delta: number) {
  const next = page.value + delta
  if (next >= 0 && next < totalPages.value) {
    page.value = next
    loadRestorations()
  }
}

function openAddModal() {
  form.value = { artifactId: '', title: '', plan: '', cleaningMethod: '', reinforcementMaterial: '' }
  showAddModal.value = true
}

async function submitRestoration() {
  if (form.value.artifactId === '' || !form.value.title) return
  submitting.value = true
  try {
    await api.restorations.create({
      artifactId: form.value.artifactId as number,
      title: form.value.title,
      plan: form.value.plan || null,
      cleaningMethod: form.value.cleaningMethod || null,
      reinforcementMaterial: form.value.reinforcementMaterial || null,
    })
    showAddModal.value = false
    await loadRestorations()
  } catch {
  } finally {
    submitting.value = false
  }
}

async function approveRestoration(id: number) {
  approvingId.value = id
  try {
    await api.restorations.approve(id)
    await loadRestorations()
  } catch {
  } finally {
    approvingId.value = null
  }
}

async function viewDetail(id: number) {
  try {
    const res = await api.restorations.get(id)
    detailData.value = res.data
    showDetailModal.value = true
  } catch {
  }
}

function goToDetail(id: number) {
  router.push(`/restorations/${id}`)
}

onMounted(() => {
  loadArtifacts()
  loadRestorations()
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="p-2 rounded-lg bg-amber-100 dark:bg-amber-900/30">
          <AlertTriangle class="w-5 h-5 text-amber-600 dark:text-amber-400" />
        </div>
        <div>
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white">修复方案</h2>
          <p class="text-sm text-gray-500 dark:text-gray-400">文物修复方案管理与审批</p>
        </div>
      </div>
      <button
        v-if="hasRole('RESTORER')"
        @click="openAddModal"
        class="flex items-center gap-2 px-4 py-2 rounded-lg bg-amber-600 hover:bg-amber-700 text-white text-sm font-medium transition-colors"
      >
        <Plus class="w-4 h-4" />
        新建方案
      </button>
    </div>

    <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
      <div class="flex items-center gap-4">
        <select
          v-model="filterStatus"
          @change="handleSearch"
          class="px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
        >
          <option value="">全部状态</option>
          <option v-for="(label, key) in STATUS_LABELS" :key="key" :value="key">{{ label }}</option>
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
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">文物名称</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">方案标题</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">清洁方法</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">加固材料</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">状态</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">提议人</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">日期</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="8" class="px-4 py-8 text-center text-gray-400 dark:text-gray-500">加载中...</td>
            </tr>
            <tr v-else-if="restorations.length === 0">
              <td colspan="8" class="px-4 py-8 text-center text-gray-400 dark:text-gray-500">暂无修复方案</td>
            </tr>
            <tr
              v-for="r in restorations"
              :key="r.id"
              class="border-b border-gray-100 dark:border-gray-700/50 hover:bg-gray-50 dark:hover:bg-gray-700/30"
            >
              <td class="px-4 py-3 text-gray-900 dark:text-white">{{ r.artifactName || '-' }}</td>
              <td class="px-4 py-3 text-gray-900 dark:text-white">{{ r.title }}</td>
              <td class="px-4 py-3 text-gray-700 dark:text-gray-300 max-w-xs truncate">{{ r.cleaningMethod || '-' }}</td>
              <td class="px-4 py-3 text-gray-700 dark:text-gray-300 max-w-xs truncate">{{ r.reinforcementMaterial || '-' }}</td>
              <td class="px-4 py-3">
                <span :class="['inline-flex px-2 py-0.5 rounded-full text-xs font-medium', STATUS_COLORS[r.status] || '']">
                  {{ STATUS_LABELS[r.status] || r.status }}
                </span>
              </td>
              <td class="px-4 py-3 text-gray-700 dark:text-gray-300">{{ r.proposerName || '-' }}</td>
              <td class="px-4 py-3 text-gray-500 dark:text-gray-400 text-xs">{{ r.proposedAt?.substring(0, 10) }}</td>
              <td class="px-4 py-3">
                <div class="flex items-center gap-2">
                  <button
                    @click="viewDetail(r.id)"
                    class="px-2.5 py-1 rounded-lg text-xs text-amber-600 dark:text-amber-400 hover:bg-amber-50 dark:hover:bg-amber-900/20 transition-colors"
                  >
                    详情
                  </button>
                  <button
                    v-if="hasRole('COLLECTION') && r.status === 'PROPOSED'"
                    :disabled="approvingId === r.id"
                    @click="approveRestoration(r.id)"
                    class="px-2.5 py-1 rounded-lg text-xs bg-green-600 hover:bg-green-700 disabled:bg-green-400 text-white transition-colors"
                  >
                    {{ approvingId === r.id ? '审批中...' : '审批' }}
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

    <div v-if="showDetailModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50" @click.self="showDetailModal = false">
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-2xl p-6 max-h-[80vh] overflow-y-auto">
        <h3 class="text-base font-semibold text-gray-900 dark:text-white mb-5">修复方案详情</h3>
        <template v-if="detailData">
          <div class="space-y-3">
            <div class="grid grid-cols-2 gap-4">
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">文物名称</p>
                <p class="text-sm text-gray-900 dark:text-white">{{ detailData.artifactName || '-' }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">状态</p>
                <span :class="['inline-flex px-2 py-0.5 rounded-full text-xs font-medium', STATUS_COLORS[detailData.status] || '']">
                  {{ STATUS_LABELS[detailData.status] || detailData.status }}
                </span>
              </div>
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">方案标题</p>
                <p class="text-sm text-gray-900 dark:text-white">{{ detailData.title }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">提议人</p>
                <p class="text-sm text-gray-900 dark:text-white">{{ detailData.proposerName || '-' }}</p>
              </div>
            </div>
            <div>
              <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">修复方案</p>
              <p class="text-sm text-gray-900 dark:text-white">{{ detailData.plan || '-' }}</p>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">清洁方法</p>
                <p class="text-sm text-gray-900 dark:text-white">{{ detailData.cleaningMethod || '-' }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">加固材料</p>
                <p class="text-sm text-gray-900 dark:text-white">{{ detailData.reinforcementMaterial || '-' }}</p>
              </div>
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
          <button
            @click="goToDetail(detailData!.id); showDetailModal = false"
            class="px-4 py-2 rounded-lg bg-amber-600 hover:bg-amber-700 text-white text-sm font-medium transition-colors"
          >
            查看完整详情
          </button>
        </div>
      </div>
    </div>

    <div v-if="showAddModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50" @click.self="showAddModal = false">
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-lg p-6">
        <div class="flex items-center gap-2 mb-5">
          <Plus class="w-5 h-5 text-amber-600" />
          <h3 class="text-base font-semibold text-gray-900 dark:text-white">新建修复方案</h3>
        </div>
        <form @submit.prevent="submitRestoration" class="space-y-4">
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
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">方案标题</label>
            <input
              v-model="form.title"
              type="text"
              required
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">修复方案</label>
            <textarea
              v-model="form.plan"
              rows="3"
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500 resize-none"
            />
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">清洁方法</label>
              <input
                v-model="form.cleaningMethod"
                type="text"
                class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">加固材料</label>
              <input
                v-model="form.reinforcementMaterial"
                type="text"
                class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
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
