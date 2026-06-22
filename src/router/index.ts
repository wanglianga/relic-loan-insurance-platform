import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuth } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/pages/LoginPage.vue'),
    meta: { title: '登录', public: true },
  },
  {
    path: '/',
    component: () => import('@/components/AppLayout.vue'),
    children: [
      {
        path: '',
        redirect: '/dashboard',
      },
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/pages/DashboardPage.vue'),
        meta: { title: '工作台' },
      },
      {
        path: 'artifacts',
        name: 'artifact-list',
        component: () => import('@/pages/ArtifactListPage.vue'),
        meta: { title: '藏品管理', roles: ['COLLECTION', 'RESTORER', 'BORROWER', 'INSURER', 'SECURITY'] },
      },
      {
        path: 'artifacts/:id',
        name: 'artifact-detail',
        component: () => import('@/pages/ArtifactDetailPage.vue'),
        meta: { title: '藏品详情', roles: ['COLLECTION', 'RESTORER', 'BORROWER', 'INSURER', 'SECURITY'] },
      },
      {
        path: 'diseases',
        name: 'disease-map',
        component: () => import('@/pages/DiseaseMapPage.vue'),
        meta: { title: '病害图谱', roles: ['COLLECTION', 'RESTORER'] },
      },
      {
        path: 'diseases/review',
        name: 'disease-review',
        component: () => import('@/pages/DiseaseReviewPage.vue'),
        meta: { title: '病害复核', roles: ['COLLECTION'] },
      },
      {
        path: 'loans/environment-precheck',
        name: 'environment-precheck',
        component: () => import('@/pages/EnvironmentPreCheckPage.vue'),
        meta: { title: '展厅环境预审', roles: ['COLLECTION', 'BORROWER'] },
      },
      {
        path: 'restorations',
        name: 'restoration-list',
        component: () => import('@/pages/RestorationPlanPage.vue'),
        meta: { title: '修复方案', roles: ['COLLECTION', 'RESTORER'] },
      },
      {
        path: 'restorations/:id',
        name: 'restoration-detail',
        component: () => import('@/pages/RestorationDetailPage.vue'),
        meta: { title: '修复详情', roles: ['COLLECTION', 'RESTORER'] },
      },
      {
        path: 'loans/apply',
        name: 'loan-apply',
        component: () => import('@/pages/LoanApplyPage.vue'),
        meta: { title: '借展申请', roles: ['BORROWER', 'COLLECTION'] },
      },
      {
        path: 'loans/approval',
        name: 'loan-approval',
        component: () => import('@/pages/LoanApprovalPage.vue'),
        meta: { title: '借展审批', roles: ['COLLECTION'] },
      },
      {
        path: 'insurance',
        name: 'insurance-verify',
        component: () => import('@/pages/InsuranceVerifyPage.vue'),
        meta: { title: '保险核验', roles: ['INSURER', 'COLLECTION'] },
      },
      {
        path: 'packaging',
        name: 'packaging',
        component: () => import('@/pages/PackagingPage.vue'),
        meta: { title: '包装出库', roles: ['SECURITY', 'TRANSPORT', 'COLLECTION'] },
      },
      {
        path: 'transport',
        name: 'transport-monitor',
        component: () => import('@/pages/TransportMonitorPage.vue'),
        meta: { title: '运输监管', roles: ['TRANSPORT', 'SECURITY'] },
      },
      {
        path: 'exhibition/setup',
        name: 'exhibition-setup',
        component: () => import('@/pages/ExhibitionSetupPage.vue'),
        meta: { title: '展厅布置', roles: ['SECURITY', 'BORROWER'] },
      },
      {
        path: 'exhibition/monitor',
        name: 'exhibition-monitor',
        component: () => import('@/pages/ExhibitionMonitorPage.vue'),
        meta: { title: '巡展监测', roles: ['SECURITY', 'BORROWER'] },
      },
      {
        path: 'return',
        name: 'return-acceptance',
        component: () => import('@/pages/ReturnAcceptancePage.vue'),
        meta: { title: '归还验收', roles: ['COLLECTION', 'SECURITY'] },
      },
      {
        path: 'evidence',
        name: 'evidence-chain',
        component: () => import('@/pages/EvidenceChainPage.vue'),
        meta: { title: '证据链', roles: ['COLLECTION', 'RESTORER', 'BORROWER', 'INSURER', 'SECURITY', 'TRANSPORT'] },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  if (to.meta.public) {
    next()
    return
  }
  const { isLoggedIn, hasRole } = useAuth()
  if (!isLoggedIn.value) {
    next('/login')
    return
  }
  const requiredRoles = to.meta.roles
  if (requiredRoles && requiredRoles.length > 0 && !hasRole(...requiredRoles)) {
    next('/dashboard')
    return
  }
  next()
})

export default router
