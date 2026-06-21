<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Plus, CheckCircle, ArrowLeft } from 'lucide-vue-next'
import { api, type Restoration, type RestorationStep, type ApiResponse } from '@/api'
import { useAuth } from '@/stores/auth'

const route = useRoute()
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

const restorationId = computed(() => Number(route.params.id))
const restoration = ref<Restoration | null>(null)
const steps = ref<RestorationStep[]>([])
const loading = ref(false)
const showAddStepModal = ref(false)
const submitting = ref(false)
const completing = ref(false)

const stepForm = ref({
  action: '',
  description: '',
  materialUsed: '',
})

const canAddStep = computed(() => {
  if (!hasRole('RESTORER')) return false
  return restoration.value?.status === 'APPROVED' || restoration.value?.status === 'IN_PROGRESS'
})

const canComplete = computed(() => {
  if (!hasRole('RESTORER')) return false
  return restoration.value?.status === 'APPROVED' || restoration.value?.status === 'IN_PROGRESS'
})

async function loadData() {
  loading.value = true
  try {
    const res = await api.restorations.get(restorationId.value)
    restoration.value = res.data
    steps.value = res.data.steps || []
  } catch {
    restoration.value = null
    steps.value = []
  } finally {
    loading.value = false
  }
}

function openAddStepModal() {
  stepForm.value = { action: '', description: '', materialUsed: '' }
  showAddStepModal.value = true
}

async function submitStep() {
  if (!stepForm.value.action) return
  submitting.value = true
  try {
    await api.restorations.addStep(restorationId.value, {
      action: stepForm.value.action,
      description: stepForm.value.description || null,
      materialUsed: stepForm.value.materialUsed || null,
    })
    showAddStepModal.value = false
    await loadData()
  } catch {
  } finally {
    submitting.value = false
  }
}

async function completeRestoration() {
  completing.value = true
  try {
    await api.restorations.complete(restorationId.value)
    await loadData()
  } catch {
  } finally {
    completing.value = false
  }
}

function goBack() {
  router.push('/restorations')
}

onMounted(loadData)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <button
          @click="goBack"
          class="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 transition-colors"
        >
          <ArrowLeft class="w-5 h-5" />
        </button>
        <div>
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white">修复详情</h2>
          <p class="text-sm text-gray-500 dark:text-gray-400">查看修复方案详情与步骤记录</p>
        </div>
      </div>
      <div class="flex items-center gap-3">
        <button
          v-if="canAddStep"
          @click="openAddStepModal"
          class="flex items-center gap-2 px-4 py-2 rounded-lg bg-amber-600 hover:bg-amber-700 text-white text-sm font-medium transition-colors"
        >
          <Plus class="w-4 h-4" />
          添加步骤
        </button>
        <button
          v-if="canComplete"
          :disabled="completing"
          @click="completeRestoration"
          class="flex items-center gap-2 px-4 py-2 rounded-lg bg-green-600 hover:bg-green-700 disabled:bg-green-400 text-white text-sm font-medium transition-colors"
        >
          <CheckCircle class="w-4 h-4" />
          {{ completing ? '完成中...' : '完成修复' }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="py-12 text-center text-gray-400 dark:text-gray-500">加载中...</div>

    <template v-else-if="restoration">
      <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
        <div class="flex items-center justify-between mb-5">
          <h3 class="text-base font-semibold text-gray-900 dark:text-white">{{ restoration.title }}</h3>
          <span :class="['inline-flex px-2.5 py-1 rounded-full text-xs font-medium', STATUS_COLORS[restoration.status] || '']">
            {{ STATUS_LABELS[restoration.status] || restoration.status }}
          </span>
        </div>
        <div class="grid grid-cols-2 md:grid-cols-3 gap-5">
          <div>
            <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">文物名称</p>
            <p class="text-sm text-gray-900 dark:text-white">{{ restoration.artifactName || '-' }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">提议人</p>
            <p class="text-sm text-gray-900 dark:text-white">{{ restoration.proposerName || '-' }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">审批人</p>
            <p class="text-sm text-gray-900 dark:text-white">{{ restoration.approverName || '-' }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">提议日期</p>
            <p class="text-sm text-gray-900 dark:text-white">{{ restoration.proposedAt?.substring(0, 10) || '-' }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">审批日期</p>
            <p class="text-sm text-gray-900 dark:text-white">{{ restoration.approvedAt?.substring(0, 10) || '-' }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">完成日期</p>
            <p class="text-sm text-gray-900 dark:text-white">{{ restoration.completedAt?.substring(0, 10) || '-' }}</p>
          </div>
        </div>
        <div class="mt-5 pt-5 border-t border-gray-200 dark:border-gray-700 space-y-4">
          <div>
            <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">修复方案</p>
            <p class="text-sm text-gray-900 dark:text-white whitespace-pre-wrap">{{ restoration.plan || '-' }}</p>
          </div>
          <div class="grid grid-cols-2 gap-5">
            <div>
              <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">清洁方法</p>
              <p class="text-sm text-gray-900 dark:text-white">{{ restoration.cleaningMethod || '-' }}</p>
            </div>
            <div>
              <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">加固材料</p>
              <p class="text-sm text-gray-900 dark:text-white">{{ restoration.reinforcementMaterial || '-' }}</p>
            </div>
          </div>
        </div>
      </div>

      <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
        <h3 class="text-base font-semibold text-gray-900 dark:text-white mb-5">修复步骤</h3>

        <div v-if="steps.length === 0" class="py-8 text-center text-gray-400 dark:text-gray-500 text-sm">暂无步骤记录</div>

        <div v-else class="relative pl-6">
          <div class="absolute left-2 top-0 bottom-0 w-0.5 bg-gray-200 dark:bg-gray-700" />

          <div v-for="(step, index) in steps" :key="step.id" class="relative pb-6 last:pb-0">
            <div
              :class="[
                'absolute -left-4 w-4 h-4 rounded-full border-2',
                index === steps.length - 1
                  ? 'bg-amber-600 border-amber-600'
                  : 'bg-white dark:bg-gray-800 border-gray-300 dark:border-gray-600'
              ]"
            />

            <div class="bg-gray-50 dark:bg-gray-700/30 rounded-lg p-4 ml-4">
              <div class="flex items-center justify-between mb-2">
                <div class="flex items-center gap-2">
                  <span class="inline-flex items-center justify-center w-5 h-5 rounded-full bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400 text-xs font-medium">
                    {{ step.stepOrder }}
                  </span>
                  <span class="text-sm font-medium text-gray-900 dark:text-white">{{ step.action }}</span>
                </div>
                <div class="flex items-center gap-3 text-xs text-gray-500 dark:text-gray-400">
                  <span>{{ step.operatorName || '-' }}</span>
                  <span>{{ step.operatedAt?.substring(0, 10) }}</span>
                </div>
              </div>

              <p v-if="step.description" class="text-sm text-gray-700 dark:text-gray-300 mb-2">{{ step.description }}</p>

              <div v-if="step.materialUsed" class="mb-2">
                <span class="text-xs text-gray-500 dark:text-gray-400">使用材料：</span>
                <span class="text-xs text-gray-700 dark:text-gray-300">{{ step.materialUsed }}</span>
              </div>

              <div v-if="step.beforePhotoUrl || step.afterPhotoUrl" class="flex gap-3 mt-2">
                <div v-if="step.beforePhotoUrl" class="text-center">
                  <img :src="step.beforePhotoUrl" alt="修复前" class="w-24 h-24 object-cover rounded-lg border border-gray-200 dark:border-gray-600" />
                  <span class="text-xs text-gray-400 mt-1">修复前</span>
                </div>
                <div v-if="step.afterPhotoUrl" class="text-center">
                  <img :src="step.afterPhotoUrl" alt="修复后" class="w-24 h-24 object-cover rounded-lg border border-gray-200 dark:border-gray-600" />
                  <span class="text-xs text-gray-400 mt-1">修复后</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="py-12 text-center text-gray-400 dark:text-gray-500">未找到修复方案</div>

    <div v-if="showAddStepModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50" @click.self="showAddStepModal = false">
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-lg p-6">
        <div class="flex items-center gap-2 mb-5">
          <Plus class="w-5 h-5 text-amber-600" />
          <h3 class="text-base font-semibold text-gray-900 dark:text-white">添加修复步骤</h3>
        </div>
        <form @submit.prevent="submitStep" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">操作名称</label>
            <input
              v-model="stepForm.action"
              type="text"
              required
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">操作描述</label>
            <textarea
              v-model="stepForm.description"
              rows="3"
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500 resize-none"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">使用材料</label>
            <input
              v-model="stepForm.materialUsed"
              type="text"
              class="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
            />
          </div>
          <div class="flex justify-end gap-3 pt-2">
            <button
              type="button"
              @click="showAddStepModal = false"
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
