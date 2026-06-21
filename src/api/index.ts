const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api'

interface RequestOptions {
  method?: string
  body?: any
  params?: Record<string, any>
}

function getToken(): string | null {
  return localStorage.getItem('token')
}

function buildUrl(path: string, params?: Record<string, any>): string {
  const url = new URL(`${API_BASE}${path}`)
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        url.searchParams.append(key, String(value))
      }
    })
  }
  return url.toString()
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, params } = options
  const token = getToken()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  const url = buildUrl(path, method === 'GET' ? params : undefined)
  const fetchOptions: RequestInit = {
    method,
    headers,
  }
  if (body && method !== 'GET') {
    fetchOptions.body = JSON.stringify(body)
  }
  const response = await fetch(url, fetchOptions)
  if (response.status === 401) {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    window.location.href = '/login'
    throw new Error('Unauthorized')
  }
  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}))
    throw new Error(errorData.message || `HTTP ${response.status}`)
  }
  return response.json()
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  user: UserInfo
}

export interface UserInfo {
  id: number
  username: string
  name: string
  role: string
  phone: string | null
}

export interface Artifact {
  id: number
  artifactCode: string
  name: string
  era: string | null
  material: string | null
  dimensions: string | null
  source: string | null
  acquisitionDate: string | null
  status: string
  description: string | null
  photoUrl: string | null
  createdAt: string
  updatedAt: string
  restriction?: ArtifactRestriction
}

export interface ArtifactRestriction {
  id: number
  artifactId: number
  noExhibition: boolean
  noExhibitionReason: string | null
  noLoan: boolean
  noLoanReason: string | null
  minLoanIntervalDays: number
  maxLoanDays: number
  loanConditions: string | null
}

export interface Disease {
  id: number
  artifactId: number
  diseaseType: string
  severity: string
  description: string | null
  location: string | null
  photoUrl: string | null
  reportedBy: number
  reportedAt: string
  reporterName?: string
}

export interface Restoration {
  id: number
  artifactId: number
  title: string
  plan: string | null
  cleaningMethod: string | null
  reinforcementMaterial: string | null
  status: string
  proposedBy: number
  approvedBy: number | null
  proposedAt: string
  approvedAt: string | null
  completedAt: string | null
  steps?: RestorationStep[]
  proposerName?: string
  approverName?: string
  artifactName?: string
}

export interface RestorationStep {
  id: number
  restorationId: number
  stepOrder: number
  action: string
  description: string | null
  materialUsed: string | null
  beforePhotoUrl: string | null
  afterPhotoUrl: string | null
  operatorId: number
  operatedAt: string
  operatorName?: string
}

export interface Loan {
  id: number
  artifactId: number
  applicantId: number
  borrowingInstitution: string
  exhibitionName: string
  loanStartDate: string
  loanEndDate: string
  status: string
  rejectionReason: string | null
  createdAt: string
  approvedAt: string | null
  applicantName?: string
  artifactName?: string
  artifactCode?: string
  environment?: LoanEnvironment
  insurance?: Insurance
  transport?: Transport
  exhibition?: Exhibition
  returnRecord?: ReturnRecord
}

export interface LoanEnvironment {
  id: number
  loanId: number
  temperatureMin: number | null
  temperatureMax: number | null
  humidityMin: number | null
  humidityMax: number | null
  illuminanceMax: number | null
  vibrationMax: number | null
  securityRoute: string | null
  setupDate: string | null
}

export interface Insurance {
  id: number
  loanId: number
  appraisedValue: number
  deductible: number | null
  deductibleClause: string | null
  transportLiability: string | null
  status: string
  verifiedBy: number | null
  effectiveDate: string | null
  expiryDate: string | null
  verifiedAt: string | null
  verifierName?: string
}

export interface Transport {
  id: number
  loanId: number
  packagingStandard: string | null
  boxCode: string | null
  boxSpec: string | null
  innerMaterial: string | null
  sealCode: string | null
  sealedAt: string | null
  sealedBy: number | null
  escortId: number | null
  route: string | null
  status: string
  departedAt: string | null
  arrivedAt: string | null
  monitors?: TransportMonitor[]
  sealerName?: string
  escortName?: string
}

export interface TransportMonitor {
  id: number
  transportId: number
  temperature: number | null
  humidity: number | null
  vibration: number | null
  gpsLocation: string | null
  recordedAt: string
}

export interface Exhibition {
  id: number
  loanId: number
  venue: string | null
  showcaseCode: string | null
  setupAt: string | null
  setupConfirmedBy: number | null
  status: string
  setupConfirmerName?: string
  environmentMonitors?: EnvironmentMonitor[]
}

export interface EnvironmentMonitor {
  id: number
  exhibitionId: number
  temperature: number | null
  humidity: number | null
  illuminance: number | null
  vibration: number | null
  isAlert: boolean
  recordedAt: string
}

export interface ReturnRecord {
  id: number
  loanId: number
  returnTransportId: number | null
  overallStatus: string
  returnNotes: string | null
  receivedBy: number | null
  receivedAt: string | null
  receiverName?: string
  damageRecords?: DamageRecord[]
}

export interface DamageRecord {
  id: number
  returnId: number
  damageType: string
  description: string | null
  photoUrl: string | null
  responsibility: string | null
  recordedAt: string
}

export interface EvidenceLog {
  id: number
  artifactId: number
  action: string
  fromStatus: string | null
  toStatus: string | null
  operatorId: number
  photoUrl: string | null
  description: string | null
  responsibilityImpact: string | null
  relatedEntityId: number | null
  relatedEntityType: string | null
  createdAt: string
  operatorName?: string
  artifactName?: string
}

export interface DashboardStats {
  artifactTotal: number
  artifactOnLoan: number
  artifactUnderRestoration: number
  artifactRegistered: number
  loanPending: number
  loanApproved: number
  insurancePending: number
  transportInTransit: number
  exhibitionActive: number
  restorationPending: number
  diseaseTotal: number
}

export const api = {
  dashboard: {
    stats: () => request<ApiResponse<DashboardStats>>('/dashboard'),
  },
  auth: {
    login: (data: LoginRequest) => request<ApiResponse<LoginResponse>>('/auth/login', { method: 'POST', body: data }),
    me: () => request<ApiResponse<UserInfo>>('/auth/me'),
  },
  artifacts: {
    list: (params?: Record<string, any>) => request<ApiResponse<PageResult<Artifact>>>('/artifacts', { params }),
    get: (id: number) => request<ApiResponse<Artifact>>(`/artifacts/${id}`),
    create: (data: Partial<Artifact>) => request<ApiResponse<Artifact>>('/artifacts', { method: 'POST', body: data }),
    update: (id: number, data: Partial<Artifact>) => request<ApiResponse<Artifact>>(`/artifacts/${id}`, { method: 'PUT', body: data }),
    updateRestriction: (id: number, data: Partial<ArtifactRestriction>) => request<ApiResponse<ArtifactRestriction>>(`/artifacts/${id}/restriction`, { method: 'PUT', body: data }),
  },
  diseases: {
    list: (params?: Record<string, any>) => request<ApiResponse<PageResult<Disease>>>('/diseases', { params }),
    listByArtifact: (artifactId: number) => request<ApiResponse<Disease[]>>(`/diseases/artifact/${artifactId}`),
    create: (data: Partial<Disease>) => request<ApiResponse<Disease>>('/diseases', { method: 'POST', body: data }),
  },
  restorations: {
    list: (params?: Record<string, any>) => request<ApiResponse<PageResult<Restoration>>>('/restorations', { params }),
    get: (id: number) => request<ApiResponse<Restoration>>(`/restorations/${id}`),
    create: (data: Partial<Restoration>) => request<ApiResponse<Restoration>>('/restorations', { method: 'POST', body: data }),
    approve: (id: number) => request<ApiResponse<Restoration>>(`/restorations/${id}/approve`, { method: 'PUT' }),
    complete: (id: number) => request<ApiResponse<Restoration>>(`/restorations/${id}/complete`, { method: 'PUT' }),
    addStep: (id: number, data: Partial<RestorationStep>) => request<ApiResponse<RestorationStep>>(`/restorations/${id}/steps`, { method: 'POST', body: data }),
  },
  loans: {
    list: (params?: Record<string, any>) => request<ApiResponse<PageResult<Loan>>>('/loans', { params }),
    get: (id: number) => request<ApiResponse<Loan>>(`/loans/${id}`),
    create: (data: Partial<Loan>) => request<ApiResponse<Loan>>('/loans', { method: 'POST', body: data }),
    approve: (id: number) => request<ApiResponse<Loan>>(`/loans/${id}/approve`, { method: 'PUT' }),
    reject: (id: number, reason: string) => request<ApiResponse<Loan>>(`/loans/${id}/reject`, { method: 'PUT', body: { rejectionReason: reason } }),
    updateEnvironment: (id: number, data: Partial<LoanEnvironment>) => request<ApiResponse<LoanEnvironment>>(`/loans/${id}/environment`, { method: 'PUT', body: data }),
  },
  insurances: {
    list: (params?: Record<string, any>) => request<ApiResponse<PageResult<Insurance>>>('/insurances', { params }),
    get: (id: number) => request<ApiResponse<Insurance>>(`/insurances/${id}`),
    create: (data: Partial<Insurance>) => request<ApiResponse<Insurance>>('/insurances', { method: 'POST', body: data }),
    verify: (id: number, data?: Record<string, any>) => request<ApiResponse<Insurance>>(`/insurances/${id}/verify`, { method: 'PUT', body: data }),
    activate: (id: number) => request<ApiResponse<Insurance>>(`/insurances/${id}/activate`, { method: 'PUT' }),
  },
  transports: {
    list: (params?: Record<string, any>) => request<ApiResponse<PageResult<Transport>>>('/transports', { params }),
    get: (id: number) => request<ApiResponse<Transport>>(`/transports/${id}`),
    create: (data: Partial<Transport>) => request<ApiResponse<Transport>>('/transports', { method: 'POST', body: data }),
    seal: (id: number, data: Partial<Transport>) => request<ApiResponse<Transport>>(`/transports/${id}/seal`, { method: 'PUT', body: data }),
    depart: (id: number) => request<ApiResponse<Transport>>(`/transports/${id}/depart`, { method: 'PUT' }),
    arrive: (id: number) => request<ApiResponse<Transport>>(`/transports/${id}/arrive`, { method: 'PUT' }),
    addMonitor: (id: number, data: Partial<TransportMonitor>) => request<ApiResponse<TransportMonitor>>(`/transports/${id}/monitors`, { method: 'POST', body: data }),
  },
  exhibitions: {
    list: (params?: Record<string, any>) => request<ApiResponse<PageResult<Exhibition>>>('/exhibitions', { params }),
    get: (id: number) => request<ApiResponse<Exhibition>>(`/exhibitions/${id}`),
    setup: (data: Partial<Exhibition>) => request<ApiResponse<Exhibition>>('/exhibitions', { method: 'POST', body: data }),
    confirmSetup: (id: number) => request<ApiResponse<Exhibition>>(`/exhibitions/${id}/confirm-setup`, { method: 'PUT' }),
    addEnvironmentMonitor: (id: number, data: Partial<EnvironmentMonitor>) => request<ApiResponse<EnvironmentMonitor>>(`/exhibitions/${id}/environment-monitors`, { method: 'POST', body: data }),
  },
  returns: {
    list: (params?: Record<string, any>) => request<ApiResponse<PageResult<ReturnRecord>>>('/returns', { params }),
    get: (id: number) => request<ApiResponse<ReturnRecord>>(`/returns/${id}`),
    create: (data: Partial<ReturnRecord>) => request<ApiResponse<ReturnRecord>>('/returns', { method: 'POST', body: data }),
    receive: (id: number, data: Partial<ReturnRecord>) => request<ApiResponse<ReturnRecord>>(`/returns/${id}/receive`, { method: 'PUT', body: data }),
    addDamage: (id: number, data: Partial<DamageRecord>) => request<ApiResponse<DamageRecord>>(`/returns/${id}/damages`, { method: 'POST', body: data }),
  },
  evidence: {
    list: (params?: Record<string, any>) => request<ApiResponse<PageResult<EvidenceLog>>>('/evidence', { params }),
    listByArtifact: (artifactId: number) => request<ApiResponse<EvidenceLog[]>>(`/evidence/artifact/${artifactId}`),
  },
}

export default api
