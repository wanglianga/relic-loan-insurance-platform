<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Thermometer, Droplets, Sun, Activity, AlertTriangle, Plus } from 'lucide-vue-next'
import { api, type Exhibition, type EnvironmentMonitor, type LoanEnvironment, type ApiResponse } from '@/api'
import { useAuth } from '@/stores/auth'

const { hasRole } = useAuth()

const exhibitions = ref<Exhibition[]>([])
const selectedId = ref<number | null>(null)
const selectedExhibition = ref<Exhibition | null>(null)
const loanEnvironment = ref<LoanEnvironment | null>(null)
const loading = ref(false)
const showAddForm = ref(false)
const submitting = ref(false)

const monitorForm = ref({
  temperature: '' as number | '',
  humidity: '' as number | '',
  illuminance: '' as number | '',
  vibration: '' as number | '',
  isAlert: false,
})

const monitors = computed(() => {
  return selectedExhibition.value?.environmentMonitors || []
})

const latestMonitor = computed(() => {
  if (monitors.value.length === 0) return null
  return monitors.value[monitors.value.length - 1]
})

function checkAlert(
  monitor: EnvironmentMonitor | null,
  env: LoanEnvironment | null
): boolean {
  if (!monitor || !env) return false
  if (env.temperatureMin != null && monitor.temperature != null && monitor.temperature < env.temperatureMin) return true
  if (env.temperatureMax != null && monitor.temperature != null && monitor.temperature > env.temperatureMax) return true
  if (env.humidityMin != null && monitor.humidity != null && monitor.humidity < env.humidityMin) return true
  if (env.humidityMax != null && monitor.humidity != null && monitor.humidity > env.humidityMax) return true
  if (env.illuminanceMax != null && monitor.illuminance != null && monitor.illuminance > env.illuminanceMax) return true
  if (env.vibrationMax != null && monitor.vibration != null && monitor.vibration > env.vibrationMax) return true
  return false
}

const hasAlert = computed(() => checkAlert(latestMonitor.value, loanEnvironment.value))

function getTempColor(val: number | null, env: LoanEnvironment | null): string {
  if (val == null || !env) return 'bg-gray-100 dark:bg-gray-700'
  if ((env.temperatureMin != null && val < env.temperatureMin) || (env.temperatureMax != null && val > env.temperatureMax)) {
    return 'bg-red-100 dark:bg-red-900/30'
  }
  return 'bg-blue-100 dark:bg-blue-900/30'
}

function getHumidityColor(val: number | null, env: LoanEnvironment | null): string {
  if (val == null || !env) return 'bg-gray-100 dark:bg-gray-700'
  if ((env.humidityMin != null && val < env.humidityMin) || (env.humidityMax != null && val > env.humidityMax)) {
    return 'bg-red-100 dark:bg-red-900/30'
  }
  return 'bg-cyan-100 dark:bg-cyan-900/30'
}

function getIlluminanceColor(val: number | null, env: LoanEnvironment | null): string {
  if (val == null || !env) return 'bg-gray-100 dark:bg-gray-700'
  if (env.illuminanceMax != null && val > env.illuminanceMax) {
    return 'bg-red-100 dark:bg-red-900/30'
  }
  return 'bg-yellow-100 dark:bg-yellow-900/30'
}

function getVibrationColor(val: number | null, env: LoanEnvironment | null): string {
  if (val == null || !env) return 'bg-gray-100 dark:bg-gray-700'
  if (env.vibrationMax != null && val > env.vibrationMax) {
    return 'bg-red-100 dark:bg-red-900/30'
  }
  return 'bg-green-100 dark:bg-green-900/30'
}

async function loadExhibitions() {
  loading.value = true
  try {
    const res = await api.exhibitions.list({ page: 0, size: 100, status: 'ACTIVE' })
    exhibitions.value = res.data.content
  } catch {
    exhibitions.value = []
  } finally {
    loading.value = false
  }
}

async function selectExhibition(id: number) {
  selectedId.value = id
  try {
    const res = await api.exhibitions.get(id)
    selectedExhibition.value = res.data
    await loadLoanEnvironment(res.data.loanId)
  } catch {
    selectedExhibition.value = null
    loanEnvironment.value = null
  }
}

async function loadLoanEnvironment(loanId: number) {
  try {
    const res = await api.loans.get(loanId)
    loanEnvironment.value = res.data.environment || null
  } catch {
    loanEnvironment.value = null
  }
}

function openAddForm() {
  monitorForm.value = { temperature: '', humidity: '', illuminance: '', vibration: '', isAlert: false }
  showAddForm.value = true
}

async function submitMonitor() {
  if (!selectedId.value) return
  submitting.value = true
  try {
    await api.exhibitions.addEnvironmentMonitor(selectedId.value, {
      temperature: monitorForm.value.temperature === '' ? null : Number(monitorForm.value.temperature),
      humidity: monitorForm.value.humidity === '' ? null : Number(monitorForm.value.humidity),
      illuminance: monitorForm.value.illuminance === '' ? null : Number(monitorForm.value.illuminance),
      vibration: monitorForm.value.vibration === '' ? null : Number(monitorForm.value.vibration),
      isAlert: monitorForm.value.isAlert,
    })
    showAddForm.value = false
    await selectExhibition(selectedId.value)
  } catch {
  } finally {
    submitting.value = false
  }
}

onMounted(loadExhibitions)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center gap-3">
      <div class="p-2 rounded-lg bg-teal-100 dark:bg-teal-900/30">
        <Activity class="w-5 h-5 text-teal-600 dark:text-teal-400" />
      </div>
      <div>
        <h2 class="text-lg font-semibold text-gray-900 dark:text-white">展陈环境监控</h2>
        <p class="text-sm text-gray-500 dark:text-gray-400">展览环境监测与告警管理</p>
      </div>
    </div>

    <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
      <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">选择展览</label>
      <select
        :value="selectedId"
        @change="selectExhibition(Number(($event.target as HTMLSelectElement).value))"
        class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
      >
        <option value="" disabled>请选择展出中的展览</option>
        <option v-for="ex in exhibitions" :key="ex.id" :value="ex.id">
          {{ ex.venue || '场馆' }} - {{ ex.showcaseCode || '展柜' }}
        </option>
      </select>
    </div>

    <div v-if="loading" class="text-center py-8 text-gray-400 dark:text-gray-500">加载中...</div>
    <div v-else-if="!selectedExhibition" class="text-center py-8 text-gray-400 dark:text-gray-500">请选择一个展览查看监控数据</div>
    <template v-else>
      <div
        v-if="hasAlert"
        class="flex items-center gap-3 p-4 rounded-xl bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800"
      >
        <AlertTriangle class="w-5 h-5 text-red-500 dark:text-red-400 shrink-0" />
        <p class="text-sm text-red-700 dark:text-red-300">当前环境数据超出借展环境限值，请及时处理！</p>
      </div>

      <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div :class="['rounded-xl border border-gray-200 dark:border-gray-700 p-4', getTempColor(latestMonitor?.temperature ?? null, loanEnvironment)]">
          <div class="flex items-center gap-2 mb-2">
            <Thermometer class="w-4 h-4 text-blue-600 dark:text-blue-400" />
            <span class="text-xs font-medium text-gray-600 dark:text-gray-300">温度</span>
          </div>
          <p class="text-2xl font-bold text-gray-900 dark:text-white">
            {{ latestMonitor?.temperature != null ? latestMonitor.temperature + '°C' : '-' }}
          </p>
          <p v-if="loanEnvironment" class="text-xs text-gray-500 dark:text-gray-400 mt-1">
            限值: {{ loanEnvironment.temperatureMin ?? '-' }} ~ {{ loanEnvironment.temperatureMax ?? '-' }}°C
          </p>
        </div>

        <div :class="['rounded-xl border border-gray-200 dark:border-gray-700 p-4', getHumidityColor(latestMonitor?.humidity ?? null, loanEnvironment)]">
          <div class="flex items-center gap-2 mb-2">
            <Droplets class="w-4 h-4 text-cyan-600 dark:text-cyan-400" />
            <span class="text-xs font-medium text-gray-600 dark:text-gray-300">湿度</span>
          </div>
          <p class="text-2xl font-bold text-gray-900 dark:text-white">
            {{ latestMonitor?.humidity != null ? latestMonitor.humidity + '%' : '-' }}
          </p>
          <p v-if="loanEnvironment" class="text-xs text-gray-500 dark:text-gray-400 mt-1">
            限值: {{ loanEnvironment.humidityMin ?? '-' }} ~ {{ loanEnvironment.humidityMax ?? '-' }}%
          </p>
        </div>

        <div :class="['rounded-xl border border-gray-200 dark:border-gray-700 p-4', getIlluminanceColor(latestMonitor?.illuminance ?? null, loanEnvironment)]">
          <div class="flex items-center gap-2 mb-2">
            <Sun class="w-4 h-4 text-yellow-600 dark:text-yellow-400" />
            <span class="text-xs font-medium text-gray-600 dark:text-gray-300">照度</span>
          </div>
          <p class="text-2xl font-bold text-gray-900 dark:text-white">
            {{ latestMonitor?.illuminance != null ? latestMonitor.illuminance + 'lux' : '-' }}
          </p>
          <p v-if="loanEnvironment" class="text-xs text-gray-500 dark:text-gray-400 mt-1">
            上限: {{ loanEnvironment.illuminanceMax ?? '-' }}lux
          </p>
        </div>

        <div :class="['rounded-xl border border-gray-200 dark:border-gray-700 p-4', getVibrationColor(latestMonitor?.vibration ?? null, loanEnvironment)]">
          <div class="flex items-center gap-2 mb-2">
            <Activity class="w-4 h-4 text-green-600 dark:text-green-400" />
            <span class="text-xs font-medium text-gray-600 dark:text-gray-300">振动</span>
          </div>
          <p class="text-2xl font-bold text-gray-900 dark:text-white">
            {{ latestMonitor?.vibration != null ? latestMonitor.vibration + 'mm/s' : '-' }}
          </p>
          <p v-if="loanEnvironment" class="text-xs text-gray-500 dark:text-gray-400 mt-1">
            上限: {{ loanEnvironment.vibrationMax ?? '-' }}mm/s
          </p>
        </div>
      </div>

      <div class="flex items-center justify-between">
        <h3 class="text-sm font-semibold text-gray-900 dark:text-white">环境要求参考</h3>
        <button
          @click="openAddForm"
          class="flex items-center gap-2 px-4 py-2 rounded-lg bg-amber-600 hover:bg-amber-700 text-white text-sm font-medium transition-colors"
        >
          <Plus class="w-4 h-4" />
          添加监测记录
        </button>
      </div>

      <div v-if="loanEnvironment" class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
        <div class="grid grid-cols-2 md:grid-cols-3 gap-4 text-sm">
          <div>
            <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">温度范围</p>
            <p class="text-gray-900 dark:text-white">{{ loanEnvironment.temperatureMin ?? '-' }} ~ {{ loanEnvironment.temperatureMax ?? '-' }} °C</p>
          </div>
          <div>
            <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">湿度范围</p>
            <p class="text-gray-900 dark:text-white">{{ loanEnvironment.humidityMin ?? '-' }} ~ {{ loanEnvironment.humidityMax ?? '-' }} %</p>
          </div>
          <div>
            <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">照度上限</p>
            <p class="text-gray-900 dark:text-white">{{ loanEnvironment.illuminanceMax ?? '-' }} lux</p>
          </div>
          <div>
            <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">振动上限</p>
            <p class="text-gray-900 dark:text-white">{{ loanEnvironment.vibrationMax ?? '-' }} mm/s</p>
          </div>
          <div>
            <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">安防路线</p>
            <p class="text-gray-900 dark:text-white">{{ loanEnvironment.securityRoute || '-' }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">布置日期</p>
            <p class="text-gray-900 dark:text-white">{{ loanEnvironment.setupDate ? loanEnvironment.setupDate.substring(0, 10) : '-' }}</p>
          </div>
        </div>
      </div>

      <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-700/50">
                <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">温度</th>
                <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">湿度</th>
                <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">照度</th>
                <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">振动</th>
                <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">告警</th>
                <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="monitors.length === 0">
                <td colspan="6" class="px-4 py-8 text-center text-gray-400 dark:text-gray-500">暂无监测记录</td>
              </tr>
              <tr
                v-for="m in [...monitors].reverse()"
                :key="m.id"
                class="border-b border-gray-100 dark:border-gray-700/50 hover:bg-gray-50 dark:hover:bg-gray-700/30"
              >
                <td class="px-4 py-3 text-gray-900 dark:text-white">{{ m.temperature != null ? m.temperature + '°C' : '-' }}</td>
                <td class="px-4 py-3 text-gray-900 dark:text-white">{{ m.humidity != null ? m.humidity + '%' : '-' }}</td>
                <td class="px-4 py-3 text-gray-900 dark:text-white">{{ m.illuminance != null ? m.illuminance + 'lux' : '-' }}</td>
                <td class="px-4 py-3 text-gray-900 dark:text-white">{{ m.vibration != null ? m.vibration + 'mm/s' : '-' }}</td>
                <td class="px-4 py-3">
                  <span v-if="m.isAlert" class="inline-flex px-2 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400">告警</span>
                  <span v-else class="inline-flex px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400">正常</span>
                </td>
                <td class="px-4 py-3 text-xs text-gray-500 dark:text-gray-400">{{ m.recordedAt?.replace('T', ' ').substring(0, 19) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <div v-if="showAddForm && selectedId" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50" @click.self="showAddForm = false">
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-lg p-6">
        <div class="flex items-center gap-2 mb-5">
          <Plus class="w-5 h-5 text-amber-600" />
          <h3 class="text-base font-semibold text-gray-900 dark:text-white">添加监测记录</h3>
        </div>
        <form @submit.prevent="submitMonitor" class="space-y-4">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">温度 (°C)</label>
              <input
                v-model="monitorForm.temperature"
                type="number"
                step="0.1"
                placeholder="例: 22.5"
                class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">湿度 (%)</label>
              <input
                v-model="monitorForm.humidity"
                type="number"
                step="0.1"
                placeholder="例: 55.0"
                class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">照度 (lux)</label>
              <input
                v-model="monitorForm.illuminance"
                type="number"
                step="1"
                placeholder="例: 300"
                class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">振动 (mm/s)</label>
              <input
                v-model="monitorForm.vibration"
                type="number"
                step="0.01"
                placeholder="例: 0.5"
                class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
          </div>
          <div class="flex items-center gap-2">
            <input
              v-model="monitorForm.isAlert"
              type="checkbox"
              id="isAlert"
              class="w-4 h-4 rounded border-gray-300 dark:border-gray-600 text-amber-600 focus:ring-amber-500"
            />
            <label for="isAlert" class="text-sm text-gray-700 dark:text-gray-300">标记为告警</label>
          </div>
          <div class="flex justify-end gap-3 pt-2">
            <button
              type="button"
              @click="showAddForm = false"
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
