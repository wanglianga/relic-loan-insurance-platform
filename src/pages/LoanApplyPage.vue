<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { FileText, Plus, Search, Calendar, X } from 'lucide-vue-next'
import { api, type Loan, type Artifact, type PageResult } from '@/api'
import { useAuth } from '@/stores/auth'
const { user } = useAuth()

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

const showApplyModal = ref(false)
const submitting = ref(false)
const artifacts = ref<Artifact[]>([])

const form = ref({
  artifactId: null as number | null,
  borrowingInstitution: '',
  exhibitionName: '',
  loanStartDate: '',
  loanEndDate: '',
  temperatureMin: null as number | null,
  temperatureMax: null as number | null,
  humidityMin: null as number | null,
  humidityMax: null as number | null,
  illuminanceMax: null as number | null,
  vibrationMax: null as number | null,
  securityRoute: '',
  setupDate: '',
})

async function fetchLoans() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: page.value,
      size: size.value,
    }
    if (status.value) params.status = status.value
    if (user.value?.id) params.applicantId = user.value.id
    const res = await api.loans.list(params)
    data.value = res.data
  } catch {
    data.value = null
  } finally {
    loading.value = false
  }
}

async function fetchArtifacts() {
  try {
    const res = await api.artifacts.list({ page: 0, size: 200 })
    artifacts.value = res.data.content.filter((a: Artifact) => !a.restriction?.noLoan)
  } catch {
    artifacts.value = []
  }
}

function openApplyModal() {
  form.value = {
    artifactId: null,
    borrowingInstitution: '',
    exhibitionName: '',
    loanStartDate: '',
    loanEndDate: '',
    temperatureMin: null,
    temperatureMax: null,
    humidityMin: null,
    humidityMax: null,
    illuminanceMax: null,
    vibrationMax: null,
    securityRoute: '',
    setupDate: '',
  }
  showApplyModal.value = true
  fetchArtifacts()
}

async function submitApply() {
  if (!form.value.artifactId || !form.value.borrowingInstitution || !form.value.exhibitionName || !form.value.loanStartDate || !form.value.loanEndDate) return
  submitting.value = true
  try {
    const loanRes = await api.loans.create({
      artifactId: form.value.artifactId,
      borrowingInstitution: form.value.borrowingInstitution,
      exhibitionName: form.value.exhibitionName,
      loanStartDate: form.value.loanStartDate,
      loanEndDate: form.value.loanEndDate,
    })
    const loanId = loanRes.data.id
    const hasEnv = form.value.temperatureMin != null || form.value.temperatureMax != null ||
      form.value.humidityMin != null || form.value.humidityMax != null ||
      form.value.illuminanceMax != null || form.value.vibrationMax != null ||
      form.value.securityRoute || form.value.setupDate
    if (hasEnv) {
      await api.loans.updateEnvironment(loanId, {
        temperatureMin: form.value.temperatureMin,
        temperatureMax: form.value.temperatureMax,
        humidityMin: form.value.humidityMin,
        humidityMax: form.value.humidityMax,
        illuminanceMax: form.value.illuminanceMax,
        vibrationMax: form.value.vibrationMax,
        securityRoute: form.value.securityRoute || null,
        setupDate: form.value.setupDate || null,
      })
    }
    showApplyModal.value = false
    fetchLoans()
  } finally {
    submitting.value = false
  }
}

function goPage(p: number) {
  if (p < 0 || (data.value && p >= data.value.totalPages)) return
  page.value = p
}

function formatDate(d: string) {
  return d ? d.split('T')[0] : '-'
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
    <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
      <div class="flex items-center gap-2">
        <FileText class="w-5 h-5 text-amber-600" />
        <h2 class="text-lg font-semibold text-gray-900 dark:text-white">我的借展申请</h2>
      </div>
      <button
        @click="openApplyModal"
        class="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg bg-amber-600 hover:bg-amber-700 text-white text-sm font-medium transition-colors"
      >
        <Plus class="w-4 h-4" />
        申请借展
      </button>
    </div>

    <div class="flex flex-col sm:flex-row gap-3">
      <div class="relative flex-1">
        <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <input
          :value="STATUS_MAP[status] || ''"
          type="text"
          disabled
          class="w-full pl-9 pr-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 outline-none text-sm opacity-0 h-0 p-0 m-0"
        />
      </div>
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
              <th class="text-left px-4 py-3 font-medium text-gray-500 dark:text-gray-400 hidden md:table-cell">借入机构</th>
              <th class="text-left px-4 py-3 font-medium text-gray-500 dark:text-gray-400 hidden lg:table-cell">展览名称</th>
              <th class="text-left px-4 py-3 font-medium text-gray-500 dark:text-gray-400 hidden md:table-cell">起止日期</th>
              <th class="text-left px-4 py-3 font-medium text-gray-500 dark:text-gray-400">状态</th>
              <th class="text-right px-4 py-3 font-medium text-gray-500 dark:text-gray-400">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="7" class="px-4 py-8 text-center text-gray-400 dark:text-gray-500">加载中...</td>
            </tr>
            <tr v-else-if="!data?.content?.length">
              <td colspan="7" class="px-4 py-8 text-center text-gray-400 dark:text-gray-500">暂无数据</td>
            </tr>
            <tr
              v-for="item in data?.content"
              :key="item.id"
              class="border-b border-gray-100 dark:border-gray-700/50 hover:bg-amber-50/50 dark:hover:bg-amber-900/10 transition-colors"
            >
              <td class="px-4 py-3 font-mono text-xs text-gray-600 dark:text-gray-300">{{ item.artifactCode || '-' }}</td>
              <td class="px-4 py-3 font-medium text-gray-900 dark:text-white">{{ item.artifactName || '-' }}</td>
              <td class="px-4 py-3 text-gray-600 dark:text-gray-300 hidden md:table-cell">{{ item.borrowingInstitution }}</td>
              <td class="px-4 py-3 text-gray-600 dark:text-gray-300 hidden lg:table-cell">{{ item.exhibitionName }}</td>
              <td class="px-4 py-3 text-gray-600 dark:text-gray-300 hidden md:table-cell">
                <div class="flex items-center gap-1">
                  <Calendar class="w-3.5 h-3.5 text-gray-400 shrink-0" />
                  <span class="text-xs">{{ formatDate(item.loanStartDate) }} ~ {{ formatDate(item.loanEndDate) }}</span>
                </div>
              </td>
              <td class="px-4 py-3">
                <span :class="['inline-flex px-2 py-0.5 rounded-full text-xs font-medium', STATUS_COLORS[item.status] || STATUS_COLORS.COMPLETED]">
                  {{ STATUS_MAP[item.status] || item.status }}
                </span>
              </td>
              <td class="px-4 py-3 text-right">
                <div v-if="item.status === 'APPROVED' || item.status === 'ACTIVE'" class="inline-flex items-center gap-1">
                  <router-link
                    :to="`/insurance?loanId=${item.id}`"
                    class="px-2 py-1 rounded text-xs font-medium bg-blue-50 text-blue-700 dark:bg-blue-900/20 dark:text-blue-400 hover:bg-blue-100 dark:hover:bg-blue-900/30 transition-colors"
                  >
                    保险
                  </router-link>
                  <router-link
                    :to="`/packaging?loanId=${item.id}`"
                    class="px-2 py-1 rounded text-xs font-medium bg-emerald-50 text-emerald-700 dark:bg-emerald-900/20 dark:text-emerald-400 hover:bg-emerald-100 dark:hover:bg-emerald-900/30 transition-colors"
                  >
                    包装
                  </router-link>
                  <router-link
                    :to="`/transport?loanId=${item.id}`"
                    class="px-2 py-1 rounded text-xs font-medium bg-purple-50 text-purple-700 dark:bg-purple-900/20 dark:text-purple-400 hover:bg-purple-100 dark:hover:bg-purple-900/30 transition-colors"
                  >
                    运输
                  </router-link>
                </div>
                <span v-else class="text-xs text-gray-400 dark:text-gray-500">-</span>
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
        v-if="showApplyModal"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
      >
        <div class="fixed inset-0 bg-black/50" @click="showApplyModal = false"></div>
        <div class="relative bg-white dark:bg-gray-800 rounded-xl shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto border border-gray-200 dark:border-gray-700">
          <div class="sticky top-0 bg-white dark:bg-gray-800 px-6 py-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between z-10">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white">申请借展</h3>
            <button @click="showApplyModal = false" class="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 transition-colors">
              <X class="w-5 h-5" />
            </button>
          </div>

          <div class="p-6 space-y-5">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">选择文物 <span class="text-red-500">*</span></label>
              <select
                v-model="form.artifactId"
                class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none text-sm"
              >
                <option :value="null" disabled>请选择文物</option>
                <option v-for="a in artifacts" :key="a.id" :value="a.id">{{ a.artifactCode }} - {{ a.name }}</option>
              </select>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">借入机构 <span class="text-red-500">*</span></label>
                <input
                  v-model="form.borrowingInstitution"
                  type="text"
                  placeholder="请输入借入机构"
                  class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none text-sm"
                />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">展览名称 <span class="text-red-500">*</span></label>
                <input
                  v-model="form.exhibitionName"
                  type="text"
                  placeholder="请输入展览名称"
                  class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none text-sm"
                />
              </div>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">借展开始日期 <span class="text-red-500">*</span></label>
                <input
                  v-model="form.loanStartDate"
                  type="date"
                  class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none text-sm"
                />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">借展结束日期 <span class="text-red-500">*</span></label>
                <input
                  v-model="form.loanEndDate"
                  type="date"
                  class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none text-sm"
                />
              </div>
            </div>

            <div class="border-t border-gray-200 dark:border-gray-700 pt-4">
              <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-3">环境要求</h4>
              <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
                <div>
                  <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">温度下限(℃)</label>
                  <input
                    v-model.number="form.temperatureMin"
                    type="number"
                    step="0.1"
                    class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none text-sm"
                  />
                </div>
                <div>
                  <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">温度上限(℃)</label>
                  <input
                    v-model.number="form.temperatureMax"
                    type="number"
                    step="0.1"
                    class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none text-sm"
                  />
                </div>
                <div>
                  <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">湿度下限(%)</label>
                  <input
                    v-model.number="form.humidityMin"
                    type="number"
                    step="0.1"
                    class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none text-sm"
                  />
                </div>
                <div>
                  <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">湿度上限(%)</label>
                  <input
                    v-model.number="form.humidityMax"
                    type="number"
                    step="0.1"
                    class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none text-sm"
                  />
                </div>
                <div>
                  <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">照度上限(lux)</label>
                  <input
                    v-model.number="form.illuminanceMax"
                    type="number"
                    class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none text-sm"
                  />
                </div>
                <div>
                  <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">振动上限(g)</label>
                  <input
                    v-model.number="form.vibrationMax"
                    type="number"
                    step="0.01"
                    class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none text-sm"
                  />
                </div>
                <div class="col-span-2">
                  <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">布展日期</label>
                  <input
                    v-model="form.setupDate"
                    type="date"
                    class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none text-sm"
                  />
                </div>
              </div>
              <div class="mt-4">
                <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">安防路线</label>
                <textarea
                  v-model="form.securityRoute"
                  rows="3"
                  placeholder="请描述安防路线..."
                  class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-amber-500 focus:border-amber-500 outline-none text-sm resize-none"
                ></textarea>
              </div>
            </div>
          </div>

          <div class="sticky bottom-0 bg-white dark:bg-gray-800 px-6 py-4 border-t border-gray-200 dark:border-gray-700 flex justify-end gap-3">
            <button
              @click="showApplyModal = false"
              class="px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 text-sm font-medium hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
            >
              取消
            </button>
            <button
              @click="submitApply"
              :disabled="submitting || !form.artifactId || !form.borrowingInstitution || !form.exhibitionName || !form.loanStartDate || !form.loanEndDate"
              class="px-4 py-2 rounded-lg bg-amber-600 hover:bg-amber-700 text-white text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {{ submitting ? '提交中...' : '提交申请' }}
            </button>
          </div>
        </div>
      </div>
    </teleport>
  </div>
</template>
