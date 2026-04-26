import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useDashboardStore } from '@/stores/dashboard'

/* ── Mock the API layer so no real HTTP requests are made ──────── */
vi.mock('@/api/dashboard', () => ({
  dashboardApi: {
    incidentRate:        vi.fn(),
    sampleCount:         vi.fn(),
    highRiskSites:       vi.fn(),
    monthlyCases:        vi.fn(),
    monthlyTrendBestYear:vi.fn(),
    riskByProvince:      vi.fn(),
    topOrganisms:        vi.fn(),
    topResistanceGenes:  vi.fn(),
    affectedRiverSites:  vi.fn(),
    availableYears:      vi.fn(),
    monthlyTrend:        vi.fn(),
  },
}))

import { dashboardApi } from '@/api/dashboard'

// vi.mocked gives us properly-typed mock instances
const api = vi.mocked(dashboardApi)

describe('useDashboardStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    // Default: all API calls reject (simulates backend unavailable)
    ;(Object.values(api) as ReturnType<typeof vi.fn>[]).forEach((fn) =>
      fn.mockRejectedValue(new Error('Network error')),
    )
  })

  /* ── Initial state ───────────────────────────────────────────── */

  it('starts with loading false', () => {
    const store = useDashboardStore()
    expect(store.loading).toBe(false)
  })

  it('starts with trendLoading false', () => {
    const store = useDashboardStore()
    expect(store.trendLoading).toBe(false)
  })

  it('starts with no error', () => {
    const store = useDashboardStore()
    expect(store.error).toBeNull()
  })

  it('starts with empty availableYears', () => {
    const store = useDashboardStore()
    expect(store.availableYears).toEqual([])
  })

  it('statCards returns null before any fetch', () => {
    const store = useDashboardStore()
    expect(store.statCards).toBeNull()
  })

  it('provinces returns null before any fetch', () => {
    const store = useDashboardStore()
    expect(store.provinces).toBeNull()
  })

  /* ── fetchAll with all failures ──────────────────────────────── */

  it('sets error message when all API calls fail', async () => {
    const store = useDashboardStore()
    await store.fetchAll()
    expect(store.error).toBe('Dashboard data unavailable — showing cached data.')
  })

  it('sets loading back to false after fetchAll (all failures)', async () => {
    const store = useDashboardStore()
    await store.fetchAll()
    expect(store.loading).toBe(false)
  })

  it('maps topOrganisms API response into topOrganisms computed', async () => {
    api.topOrganisms.mockResolvedValue({
      organisms: [
        { name: 'Klebsiella pneumoniae', arCode: 'CRE', detectionCount: 800, siteCount: 9, yoyTrend: 'down', resistanceRate: 52.0 },
      ],
    })

    const store = useDashboardStore()
    await store.fetchAll()

    expect(store.topOrganisms).toHaveLength(1)
    expect(store.topOrganisms![0]).toMatchObject({
      name: 'Klebsiella pneumoniae',
      arCode: 'CRE',
      detectionCount: 800,
      siteCount: 9,
      yoyTrend: 'down',
      resistanceRate: 52.0,
    })
  })

  /* ── statCards computed mapping ──────────────────────────────── */

  it('maps incidentRate response into statCards[0]', async () => {
    api.incidentRate.mockResolvedValue({ rate: 55.5, comparedToYear: 2024, delta: 2.1, direction: 'up' })

    const store = useDashboardStore()
    await store.fetchAll()

    const card = store.statCards?.find((c) => c.id === 'incident-rate')
    expect(card?.value).toBe('55.5%')
    expect(card?.trendClass).toBe('trend-danger')
  })

  it('maps sampleCount response into statCards[1]', async () => {
    api.sampleCount.mockResolvedValue({ total: 200, siteCount: 20 })

    const store = useDashboardStore()
    await store.fetchAll()

    const card = store.statCards?.find((c) => c.id === 'sample-count')
    expect(card?.value).toBe('200')
    expect(card?.trendText).toBe('Across 20 sites')
  })

  it('maps highRiskSites response into statCards[2]', async () => {
    api.highRiskSites.mockResolvedValue({ count: 8, newThisMonth: 3 })

    const store = useDashboardStore()
    await store.fetchAll()

    const card = store.statCards?.find((c) => c.id === 'high-risk-sites')
    expect(card?.value).toBe('8')
    expect(card?.trendText).toBe('3 new this month')
    expect(card?.trendClass).toBe('trend-danger') // newThisMonth > 0
  })

  it('maps monthlyCases response into statCards[3]', async () => {
    api.monthlyCases.mockResolvedValue({ year: 2025, month: 4, caseCount: 45, previousMonthCount: 42, delta: 3, direction: 'up' })

    const store = useDashboardStore()
    await store.fetchAll()

    const card = store.statCards?.find((c) => c.id === 'monthly-cases')
    expect(card?.value).toBe('45')
    expect(card?.trendText).toBe('+3 from previous month')
  })

  /* ── provinces computed mapping ──────────────────────────────── */

  it('maps riskByProvince into provinces computed', async () => {
    api.riskByProvince.mockResolvedValue({
      provinces: [
        { name: 'Gauteng', riskLevel: 'HIGH', riskScore: 88, mdroCount: 42 },
        { name: 'Western Cape', riskLevel: 'MED', riskScore: 48, mdroCount: 12 },
      ],
    })

    const store = useDashboardStore()
    await store.fetchAll()

    expect(store.provinces).toHaveLength(2)
    expect(store.provinces![0]).toMatchObject({ name: 'Gauteng', risk: 'HIGH', percent: 88 })
    expect(store.provinces![1]).toMatchObject({ name: 'Western Cape', risk: 'MED', percent: 48 })
  })

  /* ── resistanceGenes and affectedRiverSites ───────────────────── */

  it('maps topResistanceGenes into resistanceGenes computed', async () => {
    api.topResistanceGenes.mockResolvedValue({
      genes: [{ gene: 'blaCTX-M-14', resistanceClass: 'BETA-LACTAM', subclass: 'CEPHALOSPORIN', isolates: 412, identity: 98.7 }],
    })

    const store = useDashboardStore()
    await store.fetchAll()

    expect(store.resistanceGenes).toHaveLength(1)
    expect(store.resistanceGenes![0]!.gene).toBe('blaCTX-M-14')
    expect(store.resistanceGenes![0]!.isolates).toBe(412)
  })

  it('maps affectedRiverSites into affectedRiverSites computed', async () => {
    api.affectedRiverSites.mockResolvedValue({
      sites: [{ siteId: 'B26', river: 'Apies River', province: 'Gauteng', lastSampled: 'Jan 30, 2026', isolates: 16, risk: 'HIGH' }],
    })

    const store = useDashboardStore()
    await store.fetchAll()

    expect(store.affectedRiverSites).toHaveLength(1)
    expect(store.affectedRiverSites![0]!.siteId).toBe('B26')
    expect(store.affectedRiverSites![0]!.risk).toBe('HIGH')
  })

  /* ── monthly trend chart data ────────────────────────────────── */

  it('populates months, monthlyNormal, monthlyAlert from monthlyTrendBestYear', async () => {
    api.monthlyTrendBestYear.mockResolvedValue({
      year: 2025,
      data: [
        { month: 1, monthLabel: 'Jan', caseCount: 10, alert: false },
        { month: 2, monthLabel: 'Feb', caseCount: 25, alert: true },
      ],
    })

    const store = useDashboardStore()
    await store.fetchAll()

    expect(store.months).toEqual(['Jan', 'Feb'])
    expect(store.monthlyNormal).toEqual([10, null])  // non-alert → count, alert → null
    expect(store.monthlyAlert).toEqual([null, 25])   // non-alert → null, alert → count
    expect(store.trendYear).toBe(2025)
    expect(store.selectedYear).toBe(2025)
  })

  it('populates availableYears from availableYears endpoint', async () => {
    api.availableYears.mockResolvedValue({ years: [2023, 2024, 2025] })

    const store = useDashboardStore()
    await store.fetchAll()

    expect(store.availableYears).toEqual([2023, 2024, 2025])
  })

  /* ── selectYear action ───────────────────────────────────────── */

  it('does not call API when selecting the already-selected year', async () => {
    api.monthlyTrendBestYear.mockResolvedValue({ year: 2025, data: [] })

    const store = useDashboardStore()
    await store.fetchAll()            // selectedYear → 2025

    vi.clearAllMocks()
    await store.selectYear(2025)      // same year — should bail out early

    expect(api.monthlyTrend).not.toHaveBeenCalled()
  })

  it('fetches trend data when a different year is selected', async () => {
    api.monthlyTrendBestYear.mockResolvedValue({ year: 2025, data: [] })
    api.monthlyTrend.mockResolvedValue({ year: 2024, data: [] })

    const store = useDashboardStore()
    await store.fetchAll()

    await store.selectYear(2024)

    expect(api.monthlyTrend).toHaveBeenCalledWith(2024)
    expect(store.selectedYear).toBe(2024)
  })

  it('keeps previous trend data if selectYear API call fails', async () => {
    api.monthlyTrendBestYear.mockResolvedValue({
      year: 2025,
      data: [{ month: 1, monthLabel: 'Jan', caseCount: 10, alert: false }],
    })
    api.monthlyTrend.mockRejectedValue(new Error('Network error'))

    const store = useDashboardStore()
    await store.fetchAll()

    const prevMonths = store.months
    await store.selectYear(2024)

    expect(store.months).toEqual(prevMonths)      // unchanged
    expect(store.trendLoading).toBe(false)        // loading cleared even on failure
  })
})
