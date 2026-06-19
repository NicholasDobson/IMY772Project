import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { dashboardApi } from '@/api/dashboard'
import type {
  IncidentRateResponse,
  SampleCountResponse,
  HighRiskSitesResponse,
  MonthlyCasesResponse,
  MonthlyTrendResponse,
  ProvinceRiskResponse,
  TopOrganismsResponse,
  TopResistanceGenesResponse,
  AffectedRiverSitesResponse,
} from '@/api/dashboard'
import type { StatCardData, Province, TopOrganism, ResistanceGene, RiverSite } from '@/types/amr'
import {
  STAT_CARDS,
  MONTHS,
  MONTHLY_NORMAL,
  MONTHLY_ALERT,
  PROVINCES,
  RESISTANCE_GENES,
  RIVER_SITES,
  TOP_ORGANISMS,
} from '@/data/dashboard'

/* ── Dashboard Store ──────────────────────────────────────────────
   Fetches all dashboard endpoints in parallel. Each section is
   independently nullable — a partial outage still shows what loaded.
   Outputs data shaped to match the existing dashboard types so
   DashboardView only swaps the data source, not its structure.
────────────────────────────────────────────────────────────────── */
export const useDashboardStore = defineStore('dashboard', () => {
  /* ── Loading / error ────────────────────────────────────────── */
  const loading        = ref(false)
  const trendLoading   = ref(false)
  const error          = ref<string | null>(null)

  /* ── Year selector ──────────────────────────────────────────── */
  const availableYears = ref<number[]>([])
  const selectedYear   = ref<number>(new Date().getFullYear())

  /* ── Raw responses ──────────────────────────────────────────── */
  const _incidentRate       = ref<IncidentRateResponse | null>(null)
  const _sampleCount        = ref<SampleCountResponse | null>(null)
  const _highRiskSites      = ref<HighRiskSitesResponse | null>(null)
  const _monthlyCases       = ref<MonthlyCasesResponse | null>(null)
  const _monthlyTrend       = ref<MonthlyTrendResponse | null>(null)
  const _riskByProvince     = ref<ProvinceRiskResponse | null>(null)
  const _topOrganisms       = ref<TopOrganismsResponse | null>(null)
  const _resistanceGenes    = ref<TopResistanceGenesResponse | null>(null)
  const _affectedRiverSites = ref<AffectedRiverSitesResponse | null>(null)

  /* ── Helpers ────────────────────────────────────────────────── */
  function directionIcon(d: 'up' | 'down' | 'stable'): string | null {
    if (d === 'up')   return 'pi-sort-up-fill'
    if (d === 'down') return 'pi-sort-down-fill'
    return null
  }

  function directionClass(d: 'up' | 'down' | 'stable', badIsUp = true): string {
    if (d === 'up')   return badIsUp ? 'trend-danger'  : 'trend-success'
    if (d === 'down') return badIsUp ? 'trend-success' : 'trend-danger'
    return 'trend-muted'
  }

  function trendSign(d: 'up' | 'down' | 'stable'): string {
    if (d === 'up')   return '+'
    if (d === 'down') return '-'
    return ''
  }

  /* ── Computed: 4 stat cards (StatCardData[]) ────────────────── */
  const statCards = computed<StatCardData[]>(() => {
    const ir  = _incidentRate.value
    const sc  = _sampleCount.value
    const hrs = _highRiskSites.value
    const mc  = _monthlyCases.value

    if (!ir && !sc && !hrs && !mc) return [...STAT_CARDS]

    return [
      {
        id: 'incident-rate',
        label: 'MDRO Incident Rate',
        value: ir ? `${ir.rate}%` : '—',
        trendIcon: ir ? directionIcon(ir.direction) : null,
        trendText: ir
          ? `${trendSign(ir.direction)}${Math.abs(ir.delta)}% from ${ir.comparedToYear}`
          : '—',
        valueClass: '',
        trendClass: ir ? directionClass(ir.direction, true) : 'trend-muted',
      },
      {
        id: 'sample-count',
        label: 'Total Samples Collected',
        value: sc ? sc.total.toLocaleString() : '—',
        trendIcon: null,
        trendText: sc ? `Across ${sc.siteCount} sites` : '—',
        valueClass: '',
        trendClass: 'trend-muted',
      },
      {
        id: 'high-risk-sites',
        label: 'High-Risk Sites',
        value: hrs ? String(hrs.count) : '—',
        trendIcon: hrs && hrs.newThisMonth > 0 ? 'pi-sort-up-fill' : null,
        trendText: hrs ? `${hrs.newThisMonth} new this month` : '—',
        valueClass: '',
        trendClass: hrs && hrs.newThisMonth > 0 ? 'trend-danger' : 'trend-muted',
      },
      {
        id: 'monthly-cases',
        label: 'Total Cases Per Month',
        value: mc ? mc.caseCount.toLocaleString() : '—',
        trendIcon: mc ? directionIcon(mc.direction) : null,
        trendText: mc
          ? `${trendSign(mc.direction)}${Math.abs(mc.delta)} from previous month`
          : '—',
        valueClass: '',
        trendClass: mc ? directionClass(mc.direction, true) : 'trend-muted',
      },
    ]
  })

  /* ── Computed: province risk ────────────────────────────────── */
  const provinces = computed<Province[]>(() => {
    const rbp = _riskByProvince.value
    if (!rbp) return [...PROVINCES]
    return rbp.provinces.map(p => ({
      name:    p.name,
      risk:    p.riskLevel,
      percent: p.riskScore,
    }))
  })

  /* ── Computed: top organisms ────────────────────────────────── */
  const topOrganisms = computed<TopOrganism[]>(() => {
    const to = _topOrganisms.value
    if (!to) return [...TOP_ORGANISMS]
    return to.organisms.map(o => ({
      name:           o.name,
      arCode:         o.arCode,
      detectionCount: o.detectionCount,
      siteCount:      o.siteCount,
      yoyTrend:       o.yoyTrend,
      resistanceRate: o.resistanceRate ?? 0,
    }))
  })

  /* ── Computed: top resistance genes ─────────────────────────── */
  const resistanceGenes = computed<ResistanceGene[]>(() => {
    const rg = _resistanceGenes.value
    if (!rg) return [...RESISTANCE_GENES]
    return rg.genes.map(g => ({
      gene:            g.gene,
      resistanceClass: g.resistanceClass,
      subclass:        g.subclass,
      isolates:        g.isolates,
      identity:        g.identity,
    }))
  })

  /* ── Computed: affected river sites ─────────────────────────── */
  const affectedRiverSites = computed<RiverSite[]>(() => {
    const rs = _affectedRiverSites.value
    if (!rs) return [...RIVER_SITES]
    return rs.sites.map(s => ({
      siteId:      s.siteId,
      river:       s.river,
      location:    '',
      province:    s.province,
      lastSampled: s.lastSampled,
      isolates:    s.isolates,
      risk:        s.risk,
    }))
  })

  /* ── Computed: chart series ──────────────────────────────────── */
  const trendYear = computed<number>(() =>
    _monthlyTrend.value?.year ?? selectedYear.value
  )
  const months = computed<string[]>(() =>
    _monthlyTrend.value?.data.map(d => d.monthLabel) ?? [...MONTHS]
  )
  const monthlyNormal = computed<(number | null)[]>(() =>
    _monthlyTrend.value?.data.map(d => (d.alert ? null : d.caseCount)) ?? [...MONTHLY_NORMAL]
  )
  const monthlyAlert = computed<(number | null)[]>(() =>
    _monthlyTrend.value?.data.map(d => (d.alert ? d.caseCount : null)) ?? [...MONTHLY_ALERT]
  )

  /* ── Action: switch chart year ───────────────────────────────── */
  async function selectYear(year: number) {
    if (year === trendYear.value) return
    selectedYear.value = year
    trendLoading.value = true
    try {
      _monthlyTrend.value = await dashboardApi.monthlyTrend(year)
    } catch {
      // keep previous data on failure
    } finally {
      trendLoading.value = false
    }
  }

  /* ── Fetch all dashboard data in parallel ────────────────────── */
  async function fetchAll() {
    const now   = new Date()
    const year  = now.getFullYear()
    const month = now.getMonth() + 1

    loading.value = true
    error.value   = null

    const results = await Promise.allSettled([
      dashboardApi.incidentRate(),
      dashboardApi.sampleCount(),
      dashboardApi.highRiskSites(),
      dashboardApi.monthlyCases(year, month),
      dashboardApi.monthlyTrendBestYear(),
      dashboardApi.riskByProvince(),
      dashboardApi.topOrganisms(5),
      dashboardApi.topResistanceGenes(8),
      dashboardApi.affectedRiverSites(6),
      dashboardApi.availableYears(),
    ])

    const [ir, sc, hrs, mc, mt, rbp, to, rg, ars, ay] = results

    if (ir.status  === 'fulfilled') _incidentRate.value        = ir.value
    if (sc.status  === 'fulfilled') _sampleCount.value         = sc.value
    if (hrs.status === 'fulfilled') _highRiskSites.value       = hrs.value
    if (mc.status  === 'fulfilled') _monthlyCases.value        = mc.value
    if (mt.status  === 'fulfilled') {
      _monthlyTrend.value = mt.value
      selectedYear.value  = mt.value.year   // sync selector to best-year
    }
    if (rbp.status === 'fulfilled') _riskByProvince.value      = rbp.value
    if (to.status  === 'fulfilled') _topOrganisms.value        = to.value
    if (rg.status  === 'fulfilled') _resistanceGenes.value     = rg.value
    if (ars.status === 'fulfilled') _affectedRiverSites.value  = ars.value
    if (ay.status  === 'fulfilled') availableYears.value       = ay.value.years
    else if (availableYears.value.length === 0) availableYears.value = [new Date().getFullYear()]

    if (results.every(r => r.status === 'rejected')) {
      error.value = 'Dashboard data unavailable — showing cached data.'
    }

    loading.value = false
  }

  return {
    loading,
    trendLoading,
    error,
    availableYears,
    selectedYear,
    statCards,
    provinces,
    topOrganisms,
    resistanceGenes,
    affectedRiverSites,
    trendYear,
    months,
    monthlyNormal,
    monthlyAlert,
    selectYear,
    fetchAll,
  }
})
