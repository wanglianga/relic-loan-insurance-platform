<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, type Artifact, type ArtifactRestriction, type Disease, type EvidenceLog } from '@/api'
import { useAuth } from '@/stores/auth'
import { ArrowLeft, Edit, Save, X, ShieldAlert } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const { hasRole } = useAuth()

const id = Number(route.params.id)

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

const DISEASE_TYPE_MAP: Record<string, string> = {
  CRACK: '裂隙',
  DISCOLORATION: '变色',
  SURFACE_WEAR: '表面磨损',
  PEELING: '剥落',
  INSECT_DAMAGE: '虫蛀',
  PIGMENT_POWDERING: '颜料粉化',
}

const SEVERITY_MAP: Record<string, string> = {
  MILD: '轻度',
  MODERATE: '中度',
  SEVERE: '重度',
}

const SEVERITY_COLORS: Record<string, string> = {
  MILD: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
  MODERATE: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  SEVERE: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
}

const artifact = ref<Artifact | null>(null)
const diseases = ref<Disease[]>([])
const evidenceLogs = ref<EvidenceLog[]>([])
const restorationCount = ref(0)
const loading = ref(true)

const editingRestriction = ref(false)
const restrictionForm = ref<Partial<ArtifactRestriction>>({})

async function fetchAll() {
  loading.value = true
  try {
    const [artifactRes, diseaseRes, evidenceRes, restorationRes] = await Promise.all([
      api.artifacts.get(id),
      api.diseases.listByArtifact(id),
      api.evidence.listByArtifact(id),
      api.restorations.list({ artifactId: id, size: 1 }).catch(() => ({ data: { totalElements: 0 } })),
    ])
    artifact.value = artifactRes.data
    diseases.value = diseaseRes.data
    evidenceLogs.value = evidenceRes.data
    restorationCount.value = (restorationRes as any).data?.totalElements || 0
  } catch {
    artifact.value = null
  } finally {
    loading.value = false
  }
}

function startEditRestriction() {
  if (!artifact.value?.restriction) return
  const r = artifact.value.restriction
  restrictionForm.value = { ...r }
  editingRestriction.value = true
}

async function saveRestriction() {
  if (!restrictionForm.value) return
  try {
    const res = await api.artifacts.updateRestriction(id, restrictionForm.value)
    if (artifact.value) {
      artifact.value.restriction = res.data
    }
    editingRestriction.value = false
  } catch {}
}

function cancelEditRestriction() {
  editingRestriction.value = false
  restrictionForm.value = {}
}

function formatTime(ts: string | null) {
  if (!ts) return '-'
  return new Date(ts).toLocaleString('zh-CN')
}

onMounted(fetchAll)
</script>

<template>
  <div class="space-y-5">
    <div class="flex items-center gap-3">
      <button
        @click="router.push('/artifacts')"
        class="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 transition-colors"
      >
        <ArrowLeft class="w-5 h-5" />
      </button>
      <h2 class="text-lg font-semibold text-gray-900 dark:text-white">藏品详情</h2>
    </div>

    <div v-if="loading" class="py-12 text-center text-gray-400 dark:text-gray-500">加载中...</div>

    <template v-else-if="artifact">
      <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-base font-semibold text-gray-900 dark:text-white">基本信息</h3>
          <span :class="['inline-flex px-2.5 py-1 rounded-full text-xs font-medium', STATUS_COLORS[artifact.status] || STATUS_COLORS.RETURNED]">
            {{ STATUS_MAP[artifact.status] || artifact.status }}
          </span>
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-x-6 gap-y-3">
          <div>
            <p class="text-xs text-gray-500 dark:text-gray-400">编号</p>
            <p class="text-sm font-mono text-gray-900 dark:text-white">{{ artifact.artifactCode }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-500 dark:text-gray-400">名称</p>
            <p class="text-sm text-gray-900 dark:text-white">{{ artifact.name }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-500 dark:text-gray-400">年代</p>
            <p class="text-sm text-gray-900 dark:text-white">{{ artifact.era || '-' }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-500 dark:text-gray-400">材质</p>
            <p class="text-sm text-gray-900 dark:text-white">{{ artifact.material || '-' }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-500 dark:text-gray-400">尺寸</p>
            <p class="text-sm text-gray-900 dark:text-white">{{ artifact.dimensions || '-' }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-500 dark:text-gray-400">来源</p>
            <p class="text-sm text-gray-900 dark:text-white">{{ artifact.source || '-' }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-500 dark:text-gray-400">入藏日期</p>
            <p class="text-sm text-gray-900 dark:text-white">{{ artifact.acquisitionDate || '-' }}</p>
          </div>
          <div class="sm:col-span-2 lg:col-span-3">
            <p class="text-xs text-gray-500 dark:text-gray-400">描述</p>
            <p class="text-sm text-gray-900 dark:text-white whitespace-pre-wrap">{{ artifact.description || '-' }}</p>
          </div>
        </div>
      </div>

      <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-2">
            <ShieldAlert class="w-4 h-4 text-amber-600" />
            <h3 class="text-base font-semibold text-gray-900 dark:text-white">限制信息</h3>
          </div>
          <button
            v-if="hasRole('COLLECTION') && !editingRestriction"
            @click="startEditRestriction"
            class="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-medium border border-gray-300 dark:border-gray-600 hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300 transition-colors"
          >
            <Edit class="w-3.5 h-3.5" />
            编辑
          </button>
          <div v-if="editingRestriction" class="flex items-center gap-1">
            <button
              @click="saveRestriction"
              class="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-medium bg-amber-600 hover:bg-amber-700 text-white transition-colors"
            >
              <Save class="w-3.5 h-3.5" />
              保存
            </button>
            <button
              @click="cancelEditRestriction"
              class="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-medium border border-gray-300 dark:border-gray-600 hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300 transition-colors"
            >
              <X class="w-3.5 h-3.5" />
              取消
            </button>
          </div>
        </div>

        <template v-if="!editingRestriction">
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-3">
            <div>
              <p class="text-xs text-gray-500 dark:text-gray-400">禁止参展</p>
              <p class="text-sm" :class="artifact.restriction?.noExhibition ? 'text-red-600 dark:text-red-400' : 'text-gray-900 dark:text-white'">
                {{ artifact.restriction?.noExhibition ? '是' : '否' }}
                <span v-if="artifact.restriction?.noExhibition && artifact.restriction.noExhibitionReason" class="text-gray-500 dark:text-gray-400 ml-1">
                  ({{ artifact.restriction.noExhibitionReason }})
                </span>
              </p>
            </div>
            <div>
              <p class="text-xs text-gray-500 dark:text-gray-400">禁止借出</p>
              <p class="text-sm" :class="artifact.restriction?.noLoan ? 'text-red-600 dark:text-red-400' : 'text-gray-900 dark:text-white'">
                {{ artifact.restriction?.noLoan ? '是' : '否' }}
                <span v-if="artifact.restriction?.noLoan && artifact.restriction.noLoanReason" class="text-gray-500 dark:text-gray-400 ml-1">
                  ({{ artifact.restriction.noLoanReason }})
                </span>
              </p>
            </div>
            <div>
              <p class="text-xs text-gray-500 dark:text-gray-400">最短借展间隔（天）</p>
              <p class="text-sm text-gray-900 dark:text-white">{{ artifact.restriction?.minLoanIntervalDays ?? '-' }}</p>
            </div>
            <div>
              <p class="text-xs text-gray-500 dark:text-gray-400">最长借展天数</p>
              <p class="text-sm text-gray-900 dark:text-white">{{ artifact.restriction?.maxLoanDays ?? '-' }}</p>
            </div>
            <div class="sm:col-span-2">
              <p class="text-xs text-gray-500 dark:text-gray-400">借展条件</p>
              <p class="text-sm text-gray-900 dark:text-white whitespace-pre-wrap">{{ artifact.restriction?.loanConditions || '-' }}</p>
            </div>
          </div>
        </template>

        <template v-else>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-3">
            <div>
              <label class="text-xs text-gray-500 dark:text-gray-400">禁止参展</label>
              <select
                v-model="restrictionForm.noExhibition"
                class="w-full mt-1 px-3 py-1.5 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
              >
                <option :value="false">否</option>
                <option :value="true">是</option>
              </select>
            </div>
            <div v-if="restrictionForm.noExhibition">
              <label class="text-xs text-gray-500 dark:text-gray-400">禁止参展原因</label>
              <input
                v-model="restrictionForm.noExhibitionReason"
                type="text"
                class="w-full mt-1 px-3 py-1.5 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
            <div>
              <label class="text-xs text-gray-500 dark:text-gray-400">禁止借出</label>
              <select
                v-model="restrictionForm.noLoan"
                class="w-full mt-1 px-3 py-1.5 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
              >
                <option :value="false">否</option>
                <option :value="true">是</option>
              </select>
            </div>
            <div v-if="restrictionForm.noLoan">
              <label class="text-xs text-gray-500 dark:text-gray-400">禁止借出原因</label>
              <input
                v-model="restrictionForm.noLoanReason"
                type="text"
                class="w-full mt-1 px-3 py-1.5 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
            <div>
              <label class="text-xs text-gray-500 dark:text-gray-400">最短借展间隔（天）</label>
              <input
                v-model.number="restrictionForm.minLoanIntervalDays"
                type="number"
                min="0"
                class="w-full mt-1 px-3 py-1.5 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
            <div>
              <label class="text-xs text-gray-500 dark:text-gray-400">最长借展天数</label>
              <input
                v-model.number="restrictionForm.maxLoanDays"
                type="number"
                min="0"
                class="w-full mt-1 px-3 py-1.5 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
            <div class="sm:col-span-2">
              <label class="text-xs text-gray-500 dark:text-gray-400">借展条件</label>
              <textarea
                v-model="restrictionForm.loanConditions"
                rows="2"
                class="w-full mt-1 px-3 py-1.5 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm outline-none focus:ring-2 focus:ring-amber-500 resize-none"
              />
            </div>
          </div>
        </template>
      </div>

      <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-base font-semibold text-gray-900 dark:text-white">病害记录</h3>
          <span class="text-xs text-gray-500 dark:text-gray-400">{{ diseases.length }} 条</span>
        </div>
        <div v-if="!diseases.length" class="py-4 text-center text-sm text-gray-400 dark:text-gray-500">暂无病害记录</div>
        <div v-else class="space-y-3">
          <div
            v-for="d in diseases"
            :key="d.id"
            class="flex items-start gap-3 p-3 rounded-lg bg-gray-50 dark:bg-gray-700/50"
          >
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <span class="text-sm font-medium text-gray-900 dark:text-white">
                  {{ DISEASE_TYPE_MAP[d.diseaseType] || d.diseaseType }}
                </span>
                <span :class="['inline-flex px-2 py-0.5 rounded-full text-xs font-medium', SEVERITY_COLORS[d.severity] || '']">
                  {{ SEVERITY_MAP[d.severity] || d.severity }}
                </span>
              </div>
              <p v-if="d.location" class="text-xs text-gray-500 dark:text-gray-400 mt-0.5">位置：{{ d.location }}</p>
              <p v-if="d.description" class="text-xs text-gray-600 dark:text-gray-300 mt-1">{{ d.description }}</p>
              <p class="text-xs text-gray-400 dark:text-gray-500 mt-1">上报于 {{ formatTime(d.reportedAt) }}</p>
            </div>
          </div>
        </div>
      </div>

      <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-base font-semibold text-gray-900 dark:text-white">关联修复</h3>
          <button
            @click="router.push(`/restorations?artifactId=${id}`)"
            class="text-xs text-amber-600 dark:text-amber-400 hover:underline"
          >
            查看 {{ restorationCount }} 条修复记录
          </button>
        </div>
      </div>

      <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
        <h3 class="text-base font-semibold text-gray-900 dark:text-white mb-4">证据链时间线</h3>
        <div v-if="!evidenceLogs.length" class="py-4 text-center text-sm text-gray-400 dark:text-gray-500">暂无证据记录</div>
        <div v-else class="relative pl-6">
          <div class="absolute left-2 top-2 bottom-2 w-px bg-gray-200 dark:bg-gray-700"></div>
          <div
            v-for="(log, idx) in evidenceLogs"
            :key="log.id"
            class="relative pb-5 last:pb-0"
          >
            <div
              :class="[
                'absolute left-[-18px] top-1.5 w-3 h-3 rounded-full border-2',
                idx === 0 ? 'border-amber-500 bg-amber-500' : 'border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800'
              ]"
            ></div>
            <div class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-3">
              <div class="flex items-center gap-2 flex-wrap">
                <span class="text-sm font-medium text-gray-900 dark:text-white">{{ log.action }}</span>
                <span v-if="log.fromStatus || log.toStatus" class="text-xs text-gray-500 dark:text-gray-400">
                  {{ STATUS_MAP[log.fromStatus || ''] || log.fromStatus || '-' }}
                  →
                  {{ STATUS_MAP[log.toStatus || ''] || log.toStatus || '-' }}
                </span>
              </div>
              <p v-if="log.description" class="text-xs text-gray-600 dark:text-gray-300 mt-1">{{ log.description }}</p>
              <div class="flex items-center gap-3 mt-1.5 text-xs text-gray-400 dark:text-gray-500">
                <span>操作人：{{ log.operatorName || log.operatorId }}</span>
                <span>{{ formatTime(log.createdAt) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="py-12 text-center text-gray-400 dark:text-gray-500">未找到该藏品</div>
  </div>
</template>
