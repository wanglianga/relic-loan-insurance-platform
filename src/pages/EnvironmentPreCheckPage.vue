<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Thermometer, Droplets, Lightbulb, Users, Shield, AlertTriangle, CheckCircle, XCircle, Upload, Search, Eye, FileText, ClipboardList } from 'lucide-vue-next'
import { api, type Loan, type EnvironmentPreCheck, type EnvironmentRisk, type Artifact } from '@/api'
import { useAuth } from '@/stores/auth'

const { hasRole } = useAuth()

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

const MATERIAL_CATEGORY_LABELS: Record<string, string> = {
  METAL: '金属器',
  CERAMIC: '陶瓷器',
  PAINTING: '书画',
  TEXTILE: '丝织品',
  WOOD: '木质文物',
  LACQUER: '漆器',
  PAPER: '纸质文物',
  OTHER: '其他',
}

const loans = ref<Loan[]>([])
const artifacts = ref<Artifact[]>([])
const preChecks = ref<EnvironmentPreCheck[]>([])
const totalElements = ref(0)
const page = ref(0)
const size = ref(20)
const loading = ref(false)
const selectedLoanId = ref<number | ''>('')
const showPreCheckModal = ref(false)
const showRiskModal = ref(false)
const showDetailModal = ref(false)
const submitting = ref(false)
const assessing = ref(false)
const selectedLoan = ref<Loan | null>(null)
const selectedPreCheck = ref<EnvironmentPreCheck | null>(null)
const selectedRisk = ref<EnvironmentRisk | null>(null)
const selectedArtifact = ref<Artifact | null>(null)

const preCheckForm = ref({
  temperatureData: '',
  humidityData: '',
  lightingLayout: '',
  visitorFlow: '',
  securityPatrolPlan: '',
})

const mitigateForm = ref({
  actions: '',
})

const highRiskCount = computed(() => preChecks.value.filter(p => p.latestRisk && (p.latestRisk.riskLevel === 'HIGH' || p.latestRisk.riskLevel === 'CRITICAL')).length)

const pendingCount = computed(() => preChecks.value.filter(p => !p.latestRisk).length)

async function loadLoans() {
  try {
    const res = await api.loans.list({ page: 0, size: 100, status: 'PENDING' })
    loans.value = res.data.content
  } catch {
    loans.value = []
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

async function loadPreChecks() {
  loading.value = true
  try {
    const params: Record<string, any> = { page: page.value, size: size.value }
    if (selectedLoanId.value !== '') {
      const res = await api.loans.listPreChecks(Number(selectedLoanId.value))
      preChecks.value = res.data
      totalElements.value = res.data.length
    } else {
      preChecks.value = []
      totalElements.value = 0
    }
  } catch {
    preChecks.value = []
    totalElements.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 0
  loadPreChecks()
}

async function openPreCheckModal(loan: Loan) {
  selectedLoan.value = loan
  preCheckForm.value = {
    temperatureData: '',
    humidityData: '',
    lightingLayout: '',
    visitorFlow: '',
    securityPatrolPlan: '',
  }
  showPreCheckModal.value = true
}

async function submitPreCheck() {
  if (!selectedLoan.value) return
  submitting.value = true
  try {
    const res = await api.loans.submitEnvironmentPreCheck(selectedLoan.value.id, {
      temperatureData: preCheckForm.value.temperatureData || null,
      humidityData: preCheckForm.value.humidityData || null,
      lightingLayout: preCheckForm.value.lightingLayout || null,
      visitorFlow: preCheckForm.value.visitorFlow || null,
      securityPatrolPlan: preCheckForm.value.securityPatrolPlan || null,
    })
    selectedPreCheck.value = res.data
    selectedRisk.value = res.data.latestRisk || null
    showPreCheckModal.value = false
    showRiskModal.value = true
    await loadPreChecks()
  } catch {
  } finally {
    submitting.value = false
  }
}

async function viewPreCheckDetail(preCheck: EnvironmentPreCheck) {
  selectedPreCheck.value = preCheck
  selectedRisk.value = preCheck.latestRisk || null
  if (preCheck.loanId) {
    try {
      const res = await api.loans.get(preCheck.loanId)
      selectedLoan.value = res.data
      if (res.data.artifactId) {
        const artifactRes = await api.artifacts.get(res.data.artifactId)
        selectedArtifact.value = artifactRes.data
      }
    } catch {
    }
  }
  showDetailModal.value = true
}

async function viewLatestRisk(loan: Loan) {
  selectedLoan.value = loan
  if (loan.artifactId) {
    try {
      const artifactRes = await api.artifacts.get(loan.artifactId)
      selectedArtifact.value = artifactRes.data
    } catch {
    }
  }
  try {
    const res = await api.loans.getLatestPreCheck(loan.id)
    selectedPreCheck.value = res.data
    selectedRisk.value = res.data.latestRisk || null
    showRiskModal.value = true
  } catch {
    selectedPreCheck.value = null
    selectedRisk.value = null
  }
}

async function mitigateRisk() {
  if (!selectedPreCheck.value || !selectedPreCheck.value.latestRisk) return
  assessing.value = true
  try {
    const res = await api.loans.mitigateRisk(selectedPreCheck.value.id, mitigateForm.value.actions)
    selectedRisk.value = res.data
    showRiskModal.value = false
    await loadPreChecks()
  } catch {
  } finally {
    assessing.value = false
  }
}

function fillExampleData() {
  preCheckForm.value = {
    temperatureData: '2024-01-01 00:00:00,22.5;2024-01-01 01:00:00,22.3;2024-01-01 02:00:00,22.4;2024-01-01 03:00:00,22.2;2024-01-01 04:00:00,22.1',
    humidityData: '2024-01-01 00:00:00,55;2024-01-01 01:00:00,56;2024-01-01 02:00:00,54;2024-01-01 03:00:00,57;2024-01-01 04:00:00,53',
    lightingLayout: '展厅共设8个照明点位，点位1-4采用LED轨道灯，间距2.5米，照度范围150-200lux；点位5-8采用嵌入式射灯，照度范围100-150lux。所有灯具距展品最小距离1.2米。',
    visitorFlow: '展厅总面积120平米，观众入口设单向通道，预计日均参观人数300-500人，高峰期每小时80人。观众动线采用U型设计，展品距通道最小距离0.8米。',
    securityPatrolPlan: '安保人员每小时巡更1次，重点区域每30分钟巡查1次。展厅内设8个监控摄像头，24小时录像，监控盲区小于5%。夜间设红外报警系统，与110联动。',
  }
}

onMounted(() => {
  loadLoans()
  loadArtifacts()
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="p-2 rounded-lg bg-green-100 dark:bg-green-900/30">
          <Shield class="w-5 h-5 text-green-600 dark:text-green-400" />
        </div>
        <div>
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white">展厅环境预审</h2>
          <p class="text-sm text-gray-500 dark:text-gray-400">外借场馆环境条件评估与风险管控</p>
        </div>
      </div>
      <div class="flex items-center gap-2">
        <div class="flex items-center gap-2 px-4 py-2 rounded-lg bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800">
          <AlertTriangle class="w-4 h-4 text-amber-600 dark:text-amber-400" />
          <span class="text-sm text-amber-700 dark:text-amber-400">待评估: {{ pendingCount }} 条</span>
        </div>
        <div v-if="highRiskCount > 0" class="flex items-center gap-2 px-4 py-2 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800">
          <XCircle class="w-4 h-4 text-red-600 dark:text-red-400" />
          <span class="text-sm text-red-700 dark:text-red-400">高风险: {{ highRiskCount }} 条</span>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <div v-for="loan in loans" :key="loan.id" class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
        <div class="flex items-start justify-between mb-3">
          <div>
            <p class="text-sm font-semibold text-gray-900 dark:text-white">{{ loan.artifactName }}</p>
            <p class="text-xs text-gray-500 dark:text-gray-400">{{ loan.borrowingInstitution }}</p>
          </div>
          <span v-if="loan.artifactId" :class="['inline-flex px-2 py-0.5 rounded-full text-xs', 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400']">
            {{ MATERIAL_CATEGORY_LABELS[artifacts.find(a => a.id === loan.artifactId)?.materialCategory || 'OTHER'] }}
          </span>
        </div>
        <p class="text-xs text-gray-500 dark:text-gray-400 mb-3">展览: {{ loan.exhibitionName }}</p>
        <p class="text-xs text-gray-500 dark:text-gray-400 mb-4">展期: {{ loan.loanStartDate?.substring(0, 10) }} ~ {{ loan.loanEndDate?.substring(0, 10) }}</p>
        <div class="flex items-center gap-2">
          <button
            @click="openPreCheckModal(loan)"
            v-if="hasRole('BORROWER') || hasRole('ADMIN')"
            class="flex-1 flex items-center justify-center gap-1 px-3 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-700 text-white text-xs font-medium transition-colors"
          >
            <Upload class="w-3 h-3" />
            上传预审
          </button>
          <button
            @click="viewLatestRisk(loan)"
            class="flex-1 flex items-center justify-center gap-1 px-3 py-1.5 rounded-lg border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 text-xs hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
          >
            <Eye class="w-3 h-3" />
            查看风险
          </button>
        </div>
      </div>
    </div>

    <div v-if="selectedLoanId !== ''" class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
      <div class="flex flex-wrap items-center gap-4 mb-4">
        <select
          v-model="selectedLoanId"
          @change="handleSearch"
          class="flex-1 max-w-md px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">选择借展申请</option>
          <option v-for="l in loans" :key="l.id" :value="l.id">{{ l.artifactName }} - {{ l.borrowingInstitution }}</option>
        </select>
        <button
          @click="handleSearch"
          class="flex items-center gap-2 px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium transition-colors"
        >
          <Search class="w-4 h-4" />
          查询
        </button>
      </div>

      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-700/50">
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">提交时间</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">提交人</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">风险等级</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">是否需审批</th>
              <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="5" class="px-4 py-8 text-center text-gray-400 dark:text-gray-500">加载中...</td>
            </tr>
            <tr v-else-if="preChecks.length === 0">
              <td colspan="5" class="px-4 py-8 text-center text-gray-400 dark:text-gray-500">暂无预审记录</td>
            </tr>
            <tr
              v-for="pc in preChecks"
              :key="pc.id"
              class="border-b border-gray-100 dark:border-gray-700/50 hover:bg-gray-50 dark:hover:bg-gray-700/30"
            >
              <td class="px-4 py-3 text-gray-700 dark:text-gray-300">{{ pc.createdAt?.substring(0, 16) }}</td>
              <td class="px-4 py-3 text-gray-900 dark:text-white">{{ pc.submitterName || '-' }}</td>
              <td class="px-4 py-3">
                <span v-if="pc.latestRisk" :class="['inline-flex px-2 py-0.5 rounded-full text-xs font-medium', RISK_LEVEL_COLORS[pc.latestRisk.riskLevel] || '']">
                  {{ RISK_LEVEL_LABELS[pc.latestRisk.riskLevel] || pc.latestRisk.riskLevel }}
                </span>
                <span v-else class="text-gray-400 dark:text-gray-500">未评估</span>
              </td>
              <td class="px-4 py-3">
                <CheckCircle v-if="pc.latestRisk?.requiresApproval" class="w-4 h-4 text-red-600" />
                <XCircle v-else-if="pc.latestRisk" class="w-4 h-4 text-green-600" />
                <span v-else class="text-gray-400 dark:text-gray-500">-</span>
              </td>
              <td class="px-4 py-3">
                <button
                  @click="viewPreCheckDetail(pc)"
                  class="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 hover:text-blue-600 transition-colors"
                  title="查看详情"
                >
                  <Eye class="w-4 h-4" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="showPreCheckModal && selectedLoan" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" @click.self="showPreCheckModal = false">
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-3xl max-h-[90vh] overflow-y-auto">
        <div class="sticky top-0 bg-white dark:bg-gray-800 px-6 py-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between z-10">
          <div class="flex items-center gap-2">
            <ClipboardList class="w-5 h-5 text-green-600" />
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white">提交环境预审</h3>
          </div>
          <button @click="showPreCheckModal = false" class="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 transition-colors">
            <XCircle class="w-5 h-5" />
          </button>
        </div>
        <div class="p-6 space-y-5">
          <div class="bg-blue-50 dark:bg-blue-900/20 rounded-lg p-4 border border-blue-200 dark:border-blue-800">
            <div class="flex items-start gap-2">
              <FileText class="w-4 h-4 text-blue-600 dark:text-blue-400 flex-shrink-0 mt-0.5" />
              <div>
                <p class="text-sm font-medium text-blue-700 dark:text-blue-400">{{ selectedLoan.artifactName }}</p>
                <p class="text-xs text-blue-600 dark:text-blue-500 mt-0.5">
                  借展机构: {{ selectedLoan.borrowingInstitution }} | 展览: {{ selectedLoan.exhibitionName }}
                </p>
              </div>
            </div>
          </div>

          <form @submit.prevent="submitPreCheck" class="space-y-5">
            <div>
              <label class="flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                <Thermometer class="w-4 h-4 text-red-500" />
                连续温湿度数据 - 温度
              </label>
              <textarea
                v-model="preCheckForm.temperatureData"
                rows="3"
                placeholder="格式：时间,温度值;时间,温度值... 例如：2024-01-01 00:00:00,22.5;2024-01-01 01:00:00,22.3"
                class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-blue-500 font-mono text-xs resize-none"
              />
            </div>

            <div>
              <label class="flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                <Droplets class="w-4 h-4 text-blue-500" />
                连续温湿度数据 - 湿度
              </label>
              <textarea
                v-model="preCheckForm.humidityData"
                rows="3"
                placeholder="格式：时间,湿度值;时间,湿度值... 例如：2024-01-01 00:00:00,55;2024-01-01 01:00:00,56"
                class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-blue-500 font-mono text-xs resize-none"
              />
            </div>

            <div>
              <label class="flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                <Lightbulb class="w-4 h-4 text-yellow-500" />
                照明布点方案
              </label>
              <textarea
                v-model="preCheckForm.lightingLayout"
                rows="3"
                placeholder="请详细描述照明点位分布、灯具类型、照度范围、与展品距离等信息..."
                class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-blue-500 resize-none"
              />
            </div>

            <div>
              <label class="flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                <Users class="w-4 h-4 text-purple-500" />
                观众动线规划
              </label>
              <textarea
                v-model="preCheckForm.visitorFlow"
                rows="3"
                placeholder="请详细描述展厅面积、预计参观人数、观众动线设计、展品与通道距离等信息..."
                class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-blue-500 resize-none"
              />
            </div>

            <div>
              <label class="flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                <Shield class="w-4 h-4 text-green-500" />
                安防巡更计划
              </label>
              <textarea
                v-model="preCheckForm.securityPatrolPlan"
                rows="3"
                placeholder="请详细描述安保巡更频率、监控覆盖范围、报警系统、夜间安保措施等信息..."
                class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-blue-500 resize-none"
              />
            </div>

            <div class="bg-amber-50 dark:bg-amber-900/20 rounded-lg p-4 border border-amber-200 dark:border-amber-800">
              <div class="flex items-start gap-2">
                <AlertTriangle class="w-4 h-4 text-amber-600 dark:text-amber-400 flex-shrink-0 mt-0.5" />
                <div>
                  <p class="text-sm font-medium text-amber-700 dark:text-amber-400">风险评估说明</p>
                  <p class="text-xs text-amber-600 dark:text-amber-500 mt-1">
                    系统将根据藏品材质自动计算环境风险。若丝织品遇到高照度或漆器遇到湿度波动，
                    审批不会直接通过，而是要求改展柜、缩短展期或增加监测设备。
                  </p>
                </div>
              </div>
            </div>

            <div class="flex justify-between pt-2">
              <button
                type="button"
                @click="fillExampleData"
                class="px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
              >
                填充示例数据
              </button>
              <div class="flex gap-3">
                <button
                  type="button"
                  @click="showPreCheckModal = false"
                  class="px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                >
                  取消
                </button>
                <button
                  type="submit"
                  :disabled="submitting"
                  class="px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white text-sm font-medium transition-colors"
                >
                  {{ submitting ? '提交中...' : '提交并评估' }}
                </button>
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>

    <div v-if="showRiskModal && selectedRisk" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" @click.self="showRiskModal = false">
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-3xl max-h-[90vh] overflow-y-auto">
        <div class="sticky top-0 bg-white dark:bg-gray-800 px-6 py-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between z-10">
          <div class="flex items-center gap-2">
            <AlertTriangle class="w-5 h-5 text-amber-600" />
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white">环境风险评估结果</h3>
          </div>
          <button @click="showRiskModal = false" class="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 transition-colors">
            <XCircle class="w-5 h-5" />
          </button>
        </div>
        <div class="p-6 space-y-6">
          <div v-if="selectedLoan && selectedArtifact" class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-4">
            <div class="grid grid-cols-2 gap-4 text-sm">
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">文物</p>
                <p class="font-medium text-gray-900 dark:text-white">{{ selectedLoan.artifactName }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">材质类别</p>
                <span :class="['inline-flex px-2 py-0.5 rounded-full text-xs', 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400']">
                  {{ MATERIAL_CATEGORY_LABELS[selectedArtifact.materialCategory] || selectedArtifact.materialCategory }}
                </span>
              </div>
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">借展机构</p>
                <p class="text-gray-900 dark:text-white">{{ selectedLoan.borrowingInstitution }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">风险等级</p>
                <span :class="['inline-flex px-2 py-0.5 rounded-full text-xs font-medium', RISK_LEVEL_COLORS[selectedRisk.riskLevel] || '']">
                  {{ RISK_LEVEL_LABELS[selectedRisk.riskLevel] || selectedRisk.riskLevel }}
                </span>
              </div>
            </div>
          </div>

          <div v-if="selectedRisk.requiresApproval" class="bg-red-50 dark:bg-red-900/20 rounded-lg p-4 border border-red-200 dark:border-red-800">
            <div class="flex items-start gap-2">
              <XCircle class="w-5 h-5 text-red-600 dark:text-red-400 flex-shrink-0 mt-0.5" />
              <div>
                <p class="text-sm font-medium text-red-700 dark:text-red-400">审批拦截</p>
                <p class="text-xs text-red-600 dark:text-red-500 mt-1">
                  该申请存在高风险因素，无法直接通过审批。请先完成风险 mitigation 后再提交审批。
                </p>
              </div>
            </div>
          </div>
          <div v-else class="bg-green-50 dark:bg-green-900/20 rounded-lg p-4 border border-green-200 dark:border-green-800">
            <div class="flex items-start gap-2">
              <CheckCircle class="w-5 h-5 text-green-600 dark:text-green-400 flex-shrink-0 mt-0.5" />
              <div>
                <p class="text-sm font-medium text-green-700 dark:text-green-400">风险可控</p>
                <p class="text-xs text-green-600 dark:text-green-500 mt-1">
                  环境风险在可接受范围内，可正常进入审批流程。
                </p>
              </div>
            </div>
          </div>

          <div v-if="selectedRisk.riskFactors" class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-4">
            <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-3">识别的风险因素</h4>
            <p class="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-line">{{ selectedRisk.riskFactors }}</p>
          </div>

          <div v-if="selectedRisk.mitigationSuggestions" class="bg-amber-50 dark:bg-amber-900/20 rounded-lg p-4 border border-amber-200 dark:border-amber-800">
            <h4 class="text-sm font-semibold text-amber-800 dark:text-amber-400 mb-3">风险缓解建议</h4>
            <p class="text-sm text-amber-700 dark:text-amber-300 whitespace-pre-line">{{ selectedRisk.mitigationSuggestions }}</p>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div v-if="selectedRisk.showcaseSuggestion" class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-4">
              <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-2">展柜建议</h4>
              <p class="text-sm text-gray-700 dark:text-gray-300">{{ selectedRisk.showcaseSuggestion }}</p>
            </div>
            <div v-if="selectedRisk.exhibitionDurationSuggestion" class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-4">
              <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-2">展期建议</h4>
              <p class="text-sm text-gray-700 dark:text-gray-300">建议不超过 {{ selectedRisk.exhibitionDurationSuggestion }} 天</p>
            </div>
            <div v-if="selectedRisk.monitoringEquipment" class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-4 md:col-span-2">
              <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-2">监测设备要求</h4>
              <p class="text-sm text-gray-700 dark:text-gray-300">{{ selectedRisk.monitoringEquipment }}</p>
            </div>
          </div>

          <div v-if="selectedRisk.requiresApproval && (hasRole('COLLECTION') || hasRole('ADMIN'))" class="border-t border-gray-200 dark:border-gray-700 pt-4">
            <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-3">记录风险处理措施</h4>
            <form @submit.prevent="mitigateRisk" class="space-y-3">
              <textarea
                v-model="mitigateForm.actions"
                rows="3"
                placeholder="请描述已采取的风险处理措施，如改展柜、缩短展期、增加监测设备等..."
                class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-blue-500 resize-none"
              />
              <div class="flex justify-end">
                <button
                  type="submit"
                  :disabled="assessing || !mitigateForm.actions.trim()"
                  class="px-4 py-2 rounded-lg bg-green-600 hover:bg-green-700 disabled:bg-green-400 text-white text-sm font-medium transition-colors"
                >
                  {{ assessing ? '处理中...' : '标记为已处理' }}
                </button>
              </div>
            </form>
          </div>

          <div class="flex justify-end pt-2">
            <button
              @click="showRiskModal = false"
              class="px-4 py-2 rounded-lg bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600 text-gray-700 dark:text-gray-300 text-sm font-medium transition-colors"
            >
              关闭
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showDetailModal && selectedPreCheck" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" @click.self="showDetailModal = false">
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-3xl max-h-[90vh] overflow-y-auto">
        <div class="sticky top-0 bg-white dark:bg-gray-800 px-6 py-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between z-10">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white">预审详情</h3>
          <button @click="showDetailModal = false" class="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 transition-colors">
            <XCircle class="w-5 h-5" />
          </button>
        </div>
        <div class="p-6 space-y-5">
          <div class="grid grid-cols-2 gap-4 text-sm">
            <div>
              <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">提交人</p>
              <p class="text-gray-900 dark:text-white">{{ selectedPreCheck.submitterName || '-' }}</p>
            </div>
            <div>
              <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">提交时间</p>
              <p class="text-gray-900 dark:text-white">{{ selectedPreCheck.createdAt?.substring(0, 16) }}</p>
            </div>
          </div>

          <div v-if="selectedPreCheck.temperatureData" class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-4">
            <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-2 flex items-center gap-2">
              <Thermometer class="w-4 h-4 text-red-500" />
              温度数据
            </h4>
            <p class="text-xs text-gray-700 dark:text-gray-300 font-mono">{{ selectedPreCheck.temperatureData }}</p>
          </div>

          <div v-if="selectedPreCheck.humidityData" class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-4">
            <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-2 flex items-center gap-2">
              <Droplets class="w-4 h-4 text-blue-500" />
              湿度数据
            </h4>
            <p class="text-xs text-gray-700 dark:text-gray-300 font-mono">{{ selectedPreCheck.humidityData }}</p>
          </div>

          <div v-if="selectedPreCheck.lightingLayout" class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-4">
            <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-2 flex items-center gap-2">
              <Lightbulb class="w-4 h-4 text-yellow-500" />
              照明布点方案
            </h4>
            <p class="text-sm text-gray-700 dark:text-gray-300">{{ selectedPreCheck.lightingLayout }}</p>
          </div>

          <div v-if="selectedPreCheck.visitorFlow" class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-4">
            <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-2 flex items-center gap-2">
              <Users class="w-4 h-4 text-purple-500" />
              观众动线规划
            </h4>
            <p class="text-sm text-gray-700 dark:text-gray-300">{{ selectedPreCheck.visitorFlow }}</p>
          </div>

          <div v-if="selectedPreCheck.securityPatrolPlan" class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-4">
            <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-2 flex items-center gap-2">
              <Shield class="w-4 h-4 text-green-500" />
              安防巡更计划
            </h4>
            <p class="text-sm text-gray-700 dark:text-gray-300">{{ selectedPreCheck.securityPatrolPlan }}</p>
          </div>

          <div class="flex justify-end">
            <button
              @click="showDetailModal = false"
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
