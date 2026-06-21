<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Archive, Search, Plus, Eye, Edit, Filter } from 'lucide-vue-next'
import { api, type Artifact, type PageResult, type ApiResponse } from '@/api'
import { useAuth } from '@/stores/auth'

const router = useRouter()
const { hasRole } = useAuth()

const STATUS_MAP: Record<string, string> = {
  REGISTERED: '已登记',
  UNDER_RESTORATION: '修复中',
  LOAN_PENDING: '借展待批',
  ON_LOAN: '借出中',
  RETURNED: '已归还',
}

const STATUS_COLORS: Record<string, string> = {
  REGISTERED: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
  UNDER_RESTORATION: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  LOAN_PENDING: 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400',
  ON_LOAN: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
  RETURNED: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300',
}

const STATUSES = [
  { value: '', label: '全部状态' },
  { value: 'REGISTERED', label: '已登记' },
  { value: 'UNDER_RESTORATION', label: '修复中' },
  { value: 'LOAN_PENDING', label: '借展待批' },
  { value: 'ON_LOAN', label: '借出中' },
  { value: 'RETURNED', label: '已归还' },
]

const keyword = ref('')
const status = ref('')
const page = ref(0)
const size = ref(10)
const loading = ref(false)
const data = ref<PageResult<Artifact> | null>(null)

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: page.value,
      size: size.value,
    }
    if (status.value) params.status = status.value
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    const res = await api.artifacts.list(params)
    data.value = res.data
  } catch {
    data.value = null
  } finally {
    loading.value = false
  }
}

function goDetail(id: number) {
  router.push(`/artifacts/${id}`)
}

function goPage(p: number) {
  if (p < 0 || (data.value && p >= data.value.totalPages)) return
  page.value = p
}

onMounted(fetchData)

watch([keyword, status], () => {
  page.value = 0
  fetchData()
})

watch(page, fetchData)
</script>

<template>
  <div class="space-y-4">
    <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
      <div class="flex items-center gap-2">
        <Archive class="w-5 h-5 text-amber-600" />
        <h2 class="text-lg font-semibold text-gray-900 dark:text-white">藏品列表</h2>
      </div>
      <button
        v-if="hasRole('COLLECTION')"
        @click="router.push('/artifacts/new')"
        class="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg bg-amber-600 hover:bg-amber-700 text-white text-sm font-medium transition-colors"
      >
        <Plus class="w-4 h-4" />
        登记新藏品
      </button>
    </div>

    <div class="flex flex-col sm:flex-row gap-3">
      <div class="relative flex-1">
        <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <input
          v-model="keyword"
          type="text"
          placeholder="按编号或名称搜索..."
          class="w-full pl-9 pr-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none transition text-sm"
        />
      </div>
      <div class="relative">
        <Filter class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <select
          v-model="status"
          class="pl-9 pr-8 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none transition text-sm appearance-none cursor-pointer"
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
              <th class="text-left px-4 py-3 font-medium text-gray-500 dark:text-gray-400">编号</th>
              <th class="text-left px-4 py-3 font-medium text-gray-500 dark:text-gray-400">名称</th>
              <th class="text-left px-4 py-3 font-medium text-gray-500 dark:text-gray-400 hidden md:table-cell">年代</th>
              <th class="text-left px-4 py-3 font-medium text-gray-500 dark:text-gray-400 hidden lg:table-cell">材质</th>
              <th class="text-left px-4 py-3 font-medium text-gray-500 dark:text-gray-400">状态</th>
              <th class="text-right px-4 py-3 font-medium text-gray-500 dark:text-gray-400">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="6" class="px-4 py-8 text-center text-gray-400 dark:text-gray-500">加载中...</td>
            </tr>
            <tr v-else-if="!data?.content?.length">
              <td colspan="6" class="px-4 py-8 text-center text-gray-400 dark:text-gray-500">暂无数据</td>
            </tr>
            <tr
              v-for="item in data?.content"
              :key="item.id"
              @click="goDetail(item.id)"
              class="border-b border-gray-100 dark:border-gray-700/50 hover:bg-amber-50/50 dark:hover:bg-amber-900/10 cursor-pointer transition-colors"
            >
              <td class="px-4 py-3 font-mono text-xs text-gray-600 dark:text-gray-300">{{ item.artifactCode }}</td>
              <td class="px-4 py-3 font-medium text-gray-900 dark:text-white">{{ item.name }}</td>
              <td class="px-4 py-3 text-gray-600 dark:text-gray-300 hidden md:table-cell">{{ item.era || '-' }}</td>
              <td class="px-4 py-3 text-gray-600 dark:text-gray-300 hidden lg:table-cell">{{ item.material || '-' }}</td>
              <td class="px-4 py-3">
                <span :class="['inline-flex px-2 py-0.5 rounded-full text-xs font-medium', STATUS_COLORS[item.status] || STATUS_COLORS.RETURNED]">
                  {{ STATUS_MAP[item.status] || item.status }}
                </span>
              </td>
              <td class="px-4 py-3 text-right">
                <div class="inline-flex items-center gap-1">
                  <button
                    @click.stop="goDetail(item.id)"
                    class="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 hover:text-amber-600 transition-colors"
                    title="查看"
                  >
                    <Eye class="w-4 h-4" />
                  </button>
                  <button
                    v-if="hasRole('COLLECTION')"
                    @click.stop="router.push(`/artifacts/${item.id}/edit`)"
                    class="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 hover:text-amber-600 transition-colors"
                    title="编辑"
                  >
                    <Edit class="w-4 h-4" />
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
  </div>
</template>
