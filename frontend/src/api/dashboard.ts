/* ── Dashboard API — typed fetch helpers ──────────────────────────
   All dashboard endpoints are public (no auth required).
   Requests hit /api/v1/... which Vite proxies to Spring Boot :8080.
────────────────────────────────────────────────────────────────── */

const BASE = '/api/v1/analytics/dashboard'

async function get<T>(path: string, params?: Record<string, string | number>): Promise<T> {
  const url = new URL(path, window.location.origin)
  if (params) {
    Object.entries(params).forEach(([k, v]) => url.searchParams.set(k, String(v)))
  }
  const res = await fetch(url.toString())
  if (!res.ok) throw new Error(`API ${res.status}: ${url.pathname}`)
  return res.json() as Promise<T>
}

/* ── Response shapes (§1–§8 of recommended-endpoints.md) ─────── */

export interface IncidentRateResponse {
  rate: number
  comparedToYear: number
  delta: number
  direction: 'up' | 'down' | 'stable'
}

export interface SampleCountResponse {
  total: number
  siteCount: number
}

export interface HighRiskSitesResponse {
  count: number
  newThisMonth: number
}

export interface MonthlyCasesResponse {
  year: number
  month: number
  caseCount: number
  previousMonthCount: number
  delta: number
  direction: 'up' | 'down' | 'stable'
}

export interface MonthlyTrendItem {
  month: number
  monthLabel: string
  caseCount: number
  alert: boolean
}

export interface MonthlyTrendResponse {
  year: number
  data: MonthlyTrendItem[]
}

export interface ProvinceRiskItem {
  name: string
  riskLevel: 'HIGH' | 'MED' | 'LOW'
  riskScore: number
  mdroCount: number
}

export interface ProvinceRiskResponse {
  provinces: ProvinceRiskItem[]
}

export interface TopOrganismItem {
  name: string
  arCode: string
  detectionCount: number
  siteCount: number
  yoyTrend: 'up' | 'down' | 'stable'
  resistanceRate: number
}

export interface TopOrganismsResponse {
  organisms: TopOrganismItem[]
}

export interface TopResistanceGeneItem {
  gene: string
  resistanceClass: string
  subclass: string
  isolates: number
  identity: number
}

export interface TopResistanceGenesResponse {
  genes: TopResistanceGeneItem[]
}

export interface AffectedRiverSiteItem {
  siteId: string
  river: string
  province: string
  lastSampled: string
  isolates: number
  risk: 'HIGH' | 'MED' | 'LOW'
}

export interface AffectedRiverSitesResponse {
  sites: AffectedRiverSiteItem[]
}

export interface RecentUploadItem {
  filename: string
  site: string
  siteId: string
  uploadedAt: string
  status: 'OK' | 'WARN' | 'ERROR'
  recordCount: number
}

export interface RecentUploadsResponse {
  uploads: RecentUploadItem[]
}

/* ── API functions ───────────────────────────────────────────── */

export const dashboardApi = {
  incidentRate: () =>
    get<IncidentRateResponse>(`${BASE}/incident-rate`),

  sampleCount: () =>
    get<SampleCountResponse>(`${BASE}/sample-count`),

  highRiskSites: () =>
    get<HighRiskSitesResponse>(`${BASE}/high-risk-sites`),

  monthlyCases: (year: number, month: number) =>
    get<MonthlyCasesResponse>(`${BASE}/monthly-cases`, { year, month }),

  availableYears: () =>
    get<{ years: number[] }>(`${BASE}/monthly-trend/available-years`),

  monthlyTrend: (year: number) =>
    get<MonthlyTrendResponse>(`${BASE}/monthly-trend`, { year }),

  monthlyTrendBestYear: () =>
    get<MonthlyTrendResponse>(`${BASE}/monthly-trend/best-year`),

  riskByProvince: () =>
    get<ProvinceRiskResponse>(`${BASE}/risk-by-province`),

  topOrganisms: (limit = 5) =>
    get<TopOrganismsResponse>(`${BASE}/top-organisms`, { limit }),

  topResistanceGenes: (limit = 8) =>
    get<TopResistanceGenesResponse>(`${BASE}/top-resistance-genes`, { limit }),

  affectedRiverSites: (limit = 6) =>
    get<AffectedRiverSitesResponse>(`${BASE}/affected-river-sites`, { limit }),

  recentUploads: (limit = 5) =>
    get<RecentUploadsResponse>(`${BASE}/recent-uploads`, { limit }),
}
