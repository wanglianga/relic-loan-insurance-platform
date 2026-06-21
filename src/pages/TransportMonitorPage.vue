<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Truck, MapPin, Thermometer, Droplets, Activity, CheckCircle2 } from 'lucide-vue-next'
import { api, type Transport, type TransportMonitor, type Loan, type PageResult, type ApiResponse } from '@/api'
import { useAuth } from '@/stores/auth'

const { hasRole } = useAuth()

const STATUS_MAP: Record<string, string> = {
  PREPARING: '准备中',
  SEALED: '已封签',
  IN_TRANSIT: '运输中',
  ARRIVED: '已到达',
}

const STATUS_COLORS: Record<string, string> = {
  PREPARING: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  SEALED: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
  IN_TRANSIT: 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400',
  ARRIVED: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
}

const transports = ref<Transport[]>([])
const loading = ref(false)
const selectedTransport = ref<Transport | null>(null)
const selectedLoan = ref<Loan | null>(null)
const detailLoading = ref(false)
const operating = ref(false)

const monitorForm = ref({
  temperature: '' as number | '',
  humidity: '' as number | '',
  vibration: '' as number | '',
  gpsLocation: '',
})
const monitorSubmitting = ref(false)

async function loadTransports() {
  loading.value = true
  try {
    const [sealedRes, inTransitRes] = await Promise.allSettled([
      api.transports.list({ page: 0, size: 100, status: 'SEALED' }),
      api.transports.list({ page: 0, size: 100, status: 'IN_TRANSIT' }),
    ])
    const list: Transport[] = []
    if (sealedRes.status === 'fulfilled') list.push(...sealedRes.value.data.content)
    if (inTransitRes.status === 'fulfilled') list.push(...inTransitRes.value.data.content)
    transports.value = list
  } catch {
    transports.value = []
  } finally {
    loading.value = false
  }
}

async function selectTransport(t: Transport) {
  selectedTransport.value = t
  detailLoading.value = true
  try {
    const res = await api.transports.get(t.id)
    selectedTransport.value = res.data
    const loanRes = await api.loans.get(res.data.loanId)
    selectedLoan.value = loanRes.data
  } catch {
  } finally {
    detailLoading.value = false
  }
}

async function handleDepart() {
  if (!selectedTransport.value) return
  operating.value = true
  try {
    const res = await api.transports.depart(selectedTransport.value.id)
    selectedTransport.value = res.data
    await loadTransports()
  } catch {
  } finally {
    operating.value = false
  }
}

async function handleArrive() {
  if (!selectedTransport.value) return
  operating.value = true
  try {
    const res = await api.transports.arrive(selectedTransport.value.id)
    selectedTransport.value = res.data
    await loadTransports()
  } catch {
  } finally {
    operating.value = false
  }
}

async function submitMonitor() {
  if (!selectedTransport.value) return
  monitorSubmitting.value = true
  try {
    await api.transports.addMonitor(selectedTransport.value.id, {
      temperature: monitorForm.value.temperature === '' ? null : Number(monitorForm.value.temperature),
      humidity: monitorForm.value.humidity === '' ? null : Number(monitorForm.value.humidity),
      vibration: monitorForm.value.vibration === '' ? null : Number(monitorForm.value.vibration),
      gpsLocation: monitorForm.value.gpsLocation || null,
    })
    monitorForm.value = { temperature: '', humidity: '', vibration: '', gpsLocation: '' }
    const res = await api.transports.get(selectedTransport.value.id)
    selectedTransport.value = res.data
  } catch {
  } finally {
    monitorSubmitting.value = false
  }
}

function isTempAlert(temp: number | null): boolean {
  if (temp === null || !selectedLoan.value?.environment) return false
  const env = selectedLoan.value.environment
  if (env.temperatureMin !== null && temp < env.temperatureMin) return true
  if (env.temperatureMax !== null && temp > env.temperatureMax) return true
  return false
}

function isHumidityAlert(humidity: number | null): boolean {
  if (humidity === null || !selectedLoan.value?.environment) return false
  const env = selectedLoan.value.environment
  if (env.humidityMin !== null && humidity < env.humidityMin) return true
  if (env.humidityMax !== null && humidity > env.humidityMax) return true
  return false
}

onMounted(loadTransports)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center gap-3">
      <div class="p-2 rounded-lg bg-purple-100 dark:bg-purple-900/30">
        <Truck class="w-5 h-5 text-purple-600 dark:text-purple-400" />
      </div>
      <div>
        <h2 class="text-lg font-semibold text-gray-900 dark:text-white">运输监管</h2>
        <p class="text-sm text-gray-500 dark:text-gray-400">运输途中监控与环境数据记录</p>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div class="lg:col-span-1 space-y-3">
        <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300 px-1">进行中的运输</h3>

        <div v-if="loading" class="space-y-3">
          <div v-for="i in 3" :key="i" class="animate-pulse rounded-xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 p-4">
            <div class="h-4 w-24 bg-gray-200 dark:bg-gray-700 rounded mb-2" />
            <div class="h-3 w-32 bg-gray-200 dark:bg-gray-700 rounded" />
          </div>
        </div>

        <div v-else-if="transports.length === 0" class="py-8 text-center text-sm text-gray-400 dark:text-gray-500">
          暂无进行中的运输
        </div>

        <div v-else class="space-y-3">
          <div
            v-for="t in transports"
            :key="t.id"
            @click="selectTransport(t)"
            :class="[
              'rounded-xl border p-4 cursor-pointer transition-all',
              selectedTransport?.id === t.id
                ? 'bg-purple-50 dark:bg-purple-900/20 border-purple-300 dark:border-purple-700 ring-1 ring-purple-300 dark:ring-purple-700'
                : 'bg-white dark:bg-gray-800 border-gray-200 dark:border-gray-700 hover:border-purple-200 dark:hover:border-purple-700'
            ]"
          >
            <div class="flex items-center justify-between mb-2">
              <span :class="['inline-flex px-2 py-0.5 rounded-full text-xs font-medium', STATUS_COLORS[t.status] || '']">
                {{ STATUS_MAP[t.status] || t.status }}
              </span>
              <span class="text-xs text-gray-400 dark:text-gray-500 font-mono">#{{ t.id }}</span>
            </div>
            <div class="flex items-center gap-2">
              <Truck class="w-4 h-4 text-gray-400" />
              <span class="text-sm font-medium text-gray-900 dark:text-white">运输 #{{ t.id }}</span>
            </div>
            <p v-if="t.route" class="text-xs text-gray-500 dark:text-gray-400 mt-1 ml-6 truncate">{{ t.route }}</p>
            <p v-if="t.boxCode" class="text-xs text-gray-500 dark:text-gray-400 ml-6">箱号: {{ t.boxCode }}</p>
          </div>
        </div>
      </div>

      <div class="lg:col-span-2">
        <div v-if="!selectedTransport" class="py-16 text-center text-sm text-gray-400 dark:text-gray-500">
          请从左侧选择一条运输记录查看详情
        </div>

        <div v-else class="space-y-5">
          <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
            <div class="flex items-center justify-between mb-4">
              <div class="flex items-center gap-3">
                <div class="p-2 rounded-lg bg-purple-100 dark:bg-purple-900/30">
                  <Truck class="w-5 h-5 text-purple-600 dark:text-purple-400" />
                </div>
                <div>
                  <h3 class="text-base font-semibold text-gray-900 dark:text-white">运输 #{{ selectedTransport.id }}</h3>
                  <span :class="['inline-flex px-2 py-0.5 rounded-full text-xs font-medium mt-1', STATUS_COLORS[selectedTransport.status] || '']">
                    {{ STATUS_MAP[selectedTransport.status] || selectedTransport.status }}
                  </span>
                </div>
              </div>
              <div v-if="hasRole('TRANSPORT')" class="flex items-center gap-2">
                <button
                  v-if="selectedTransport.status === 'SEALED'"
                  @click="handleDepart"
                  :disabled="operating"
                  class="flex items-center gap-2 px-4 py-2 rounded-lg bg-purple-600 hover:bg-purple-700 disabled:bg-purple-400 text-white text-sm font-medium transition-colors"
                >
                  <Truck class="w-4 h-4" />
                  发车
                </button>
                <button
                  v-if="selectedTransport.status === 'IN_TRANSIT'"
                  @click="handleArrive"
                  :disabled="operating"
                  class="flex items-center gap-2 px-4 py-2 rounded-lg bg-green-600 hover:bg-green-700 disabled:bg-green-400 text-white text-sm font-medium transition-colors"
                >
                  <CheckCircle2 class="w-4 h-4" />
                  到达
                </button>
              </div>
            </div>

            <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-3">
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">借展文物</p>
                <p class="text-sm font-medium text-gray-900 dark:text-white">{{ selectedLoan?.artifactName || '-' }}</p>
              </div>
              <div class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-3">
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">路线</p>
                <p class="text-sm font-medium text-gray-900 dark:text-white truncate">{{ selectedTransport.route || '-' }}</p>
              </div>
              <div class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-3">
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">箱号</p>
                <p class="text-sm font-medium text-gray-900 dark:text-white font-mono">{{ selectedTransport.boxCode || '-' }}</p>
              </div>
              <div class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-3">
                <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">封签码</p>
                <p class="text-sm font-medium text-gray-900 dark:text-white font-mono">{{ selectedTransport.sealCode || '-' }}</p>
              </div>
            </div>
          </div>

          <div v-if="selectedLoan?.environment" class="bg-amber-50 dark:bg-amber-900/20 rounded-xl border border-amber-200 dark:border-amber-800 p-4">
            <div class="flex items-center gap-2 mb-3">
              <Thermometer class="w-4 h-4 text-amber-600 dark:text-amber-400" />
              <h4 class="text-sm font-medium text-amber-800 dark:text-amber-300">环境要求范围</h4>
            </div>
            <div class="grid grid-cols-2 md:grid-cols-3 gap-3 text-sm">
              <div v-if="selectedLoan.environment.temperatureMin !== null || selectedLoan.environment.temperatureMax !== null">
                <span class="text-amber-600 dark:text-amber-400">温度：</span>
                <span class="text-amber-900 dark:text-amber-200">{{ selectedLoan.environment.temperatureMin ?? '-' }} ~ {{ selectedLoan.environment.temperatureMax ?? '-' }}°C</span>
              </div>
              <div v-if="selectedLoan.environment.humidityMin !== null || selectedLoan.environment.humidityMax !== null">
                <span class="text-amber-600 dark:text-amber-400">湿度：</span>
                <span class="text-amber-900 dark:text-amber-200">{{ selectedLoan.environment.humidityMin ?? '-' }} ~ {{ selectedLoan.environment.humidityMax ?? '-' }}%</span>
              </div>
              <div v-if="selectedLoan.environment.vibrationMax !== null">
                <span class="text-amber-600 dark:text-amber-400">振动上限：</span>
                <span class="text-amber-900 dark:text-amber-200">{{ selectedLoan.environment.vibrationMax }}</span>
              </div>
            </div>
          </div>

          <div v-if="hasRole('TRANSPORT') && (selectedTransport.status === 'SEALED' || selectedTransport.status === 'IN_TRANSIT')" class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
            <div class="flex items-center gap-2 mb-4">
              <Activity class="w-4 h-4 text-purple-600 dark:text-purple-400" />
              <h4 class="text-sm font-medium text-gray-900 dark:text-white">添加监控记录</h4>
            </div>
            <form @submit.prevent="submitMonitor" class="grid grid-cols-2 md:grid-cols-4 gap-3">
              <div>
                <label class="block text-xs text-gray-500 dark:text-gray-400 mb-1">温度 (°C)</label>
                <input
                  v-model="monitorForm.temperature"
                  type="number"
                  step="0.1"
                  class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-purple-500"
                />
              </div>
              <div>
                <label class="block text-xs text-gray-500 dark:text-gray-400 mb-1">湿度 (%)</label>
                <input
                  v-model="monitorForm.humidity"
                  type="number"
                  step="0.1"
                  class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-purple-500"
                />
              </div>
              <div>
                <label class="block text-xs text-gray-500 dark:text-gray-400 mb-1">振动</label>
                <input
                  v-model="monitorForm.vibration"
                  type="number"
                  step="0.01"
                  class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-purple-500"
                />
              </div>
              <div>
                <label class="block text-xs text-gray-500 dark:text-gray-400 mb-1">GPS 位置</label>
                <input
                  v-model="monitorForm.gpsLocation"
                  type="text"
                  class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-purple-500"
                />
              </div>
              <div class="col-span-2 md:col-span-4 flex justify-end">
                <button
                  type="submit"
                  :disabled="monitorSubmitting"
                  class="px-4 py-2 rounded-lg bg-purple-600 hover:bg-purple-700 disabled:bg-purple-400 text-white text-sm font-medium transition-colors"
                >
                  {{ monitorSubmitting ? '提交中...' : '提交记录' }}
                </button>
              </div>
            </form>
          </div>

          <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
            <div class="flex items-center gap-2 px-5 py-4 border-b border-gray-200 dark:border-gray-700">
              <Activity class="w-4 h-4 text-purple-600 dark:text-purple-400" />
              <h4 class="text-sm font-medium text-gray-900 dark:text-white">监控记录</h4>
            </div>
            <div class="overflow-x-auto">
              <table class="w-full text-sm">
                <thead>
                  <tr class="border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-700/50">
                    <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">温度</th>
                    <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">湿度</th>
                    <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">振动</th>
                    <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">GPS</th>
                    <th class="px-4 py-3 text-left font-medium text-gray-500 dark:text-gray-400">时间</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="detailLoading">
                    <td colspan="5" class="px-4 py-8 text-center text-gray-400 dark:text-gray-500">加载中...</td>
                  </tr>
                  <tr v-else-if="!selectedTransport.monitors?.length">
                    <td colspan="5" class="px-4 py-8 text-center text-gray-400 dark:text-gray-500">暂无监控记录</td>
                  </tr>
                  <tr
                    v-for="m in selectedTransport.monitors"
                    :key="m.id"
                    class="border-b border-gray-100 dark:border-gray-700/50 hover:bg-gray-50 dark:hover:bg-gray-700/30"
                  >
                    <td class="px-4 py-3">
                      <div class="flex items-center gap-1.5">
                        <Thermometer class="w-3.5 h-3.5" :class="isTempAlert(m.temperature) ? 'text-red-500' : 'text-gray-400'" />
                        <span :class="isTempAlert(m.temperature) ? 'text-red-600 dark:text-red-400 font-medium' : 'text-gray-900 dark:text-white'">
                          {{ m.temperature !== null ? `${m.temperature}°C` : '-' }}
                        </span>
                        <span v-if="isTempAlert(m.temperature)" class="text-xs text-red-500 dark:text-red-400">⚠ 超标</span>
                      </div>
                    </td>
                    <td class="px-4 py-3">
                      <div class="flex items-center gap-1.5">
                        <Droplets class="w-3.5 h-3.5" :class="isHumidityAlert(m.humidity) ? 'text-red-500' : 'text-gray-400'" />
                        <span :class="isHumidityAlert(m.humidity) ? 'text-red-600 dark:text-red-400 font-medium' : 'text-gray-900 dark:text-white'">
                          {{ m.humidity !== null ? `${m.humidity}%` : '-' }}
                        </span>
                        <span v-if="isHumidityAlert(m.humidity)" class="text-xs text-red-500 dark:text-red-400">⚠ 超标</span>
                      </div>
                    </td>
                    <td class="px-4 py-3 text-gray-900 dark:text-white">{{ m.vibration !== null ? m.vibration : '-' }}</td>
                    <td class="px-4 py-3">
                      <div class="flex items-center gap-1.5">
                        <MapPin class="w-3.5 h-3.5 text-gray-400" />
                        <span class="text-gray-700 dark:text-gray-300 text-xs">{{ m.gpsLocation || '-' }}</span>
                      </div>
                    </td>
                    <td class="px-4 py-3 text-xs text-gray-500 dark:text-gray-400">{{ m.recordedAt?.substring(0, 16)?.replace('T', ' ') }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
