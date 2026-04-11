<script setup lang="ts">
/**
 * ComparisonView.vue
 *
 * DATA MODEL (from /mockdata files in repo — source of truth per data.md):
 *
 * Comparison entities = Sites (Epicollect_Metadata.xlsx):
 *   A10 (Pretoria), B26 (Hammanskraal), B27 (Tshwane) — all on Apies River
 *
 * Per-site data available for comparison:
 *   - Water quality per sample: Temp, pH, TDS, EC, DO (Epicollect)
 *   - Isolate organisms and binary typing (Binary_Information)
 *   - AMR gene hits by class (AMRFinderPlus_Results)
 *   - WGS quality, predicted phenotype, SIR profile (StarAMR_Metrics)
 *
 * Comparison filters (per data.md):
 *   - siteA, siteB (required)
 *   - metric filter: incidentRate, waterQuality, etc.
 *   - time period / trip filter
 *
 * NOTE: Photos not in any data file — not included.
 * NOTE: The /analytics/compare endpoint already referenced in design doc is used here.
 */

import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart, BarChart, RadarChart } from 'echarts/charts'
import { TooltipComponent, GridComponent, LegendComponent, RadarComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { useThemeStore } from '@/stores/theme'

use([LineChart, BarChart, RadarChart, TooltipComponent, GridComponent, LegendComponent, RadarComponent, CanvasRenderer])

const themeStore = useThemeStore()
const isDark     = computed(() => themeStore.resolvedTheme === 'dark')

// ── Types (matching data.md schema) ───────────────────────────────
interface Site {
  siteId: string; locationName: string; riverName: string
  latitude: number; longitude: number
}
interface WaterSample {
  sampleId: string; siteId: string; tripIdentifier: string; collectionDate: string
  waterTemperature: number | null; phLevel: number | null
  tds: number | null; ec: number | null; dissolvedOxygen: number | null
}
interface Isolate {
  isolateId: string; sampleId: string; organismIdentity: string; sourceContext: string
  virulenceGenes: string | null
  binaryTypingProfile: { Intl1: boolean; Intl2: boolean; Intl3: boolean; TEM: boolean; SHV: boolean }
}
interface AmrSequence {
  isolateId: string; geneSymbol: string; elementType: string
  resistanceClass: string; subclass: string
  identityPercentage: number; coveragePercentage: number
}
interface WgsMetrics {
  isolateId: string; qualityStatus: string; genotype: string
  predictedPhenotype: string; sirProfile: string; plasmid: string
  genomeLength: number; n50Value: number
}
interface SiteData {
  site: Site
  waterSamples: WaterSample[]
  isolates: Isolate[]
  amrSequences: AmrSequence[]
  wgsMetrics: WgsMetrics[]
}

// ── Mock data — exact values from repo /mockdata files ─────────────
const ALL_SITES: SiteData[] = [
  {
    site: { siteId: 'A10', locationName: 'Pretoria',     riverName: 'Apies River', latitude: -25.747, longitude: 28.229 },
    waterSamples: [
      { sampleId: 'SAMP-001', siteId: 'A10', tripIdentifier: 'Trip 1', collectionDate: '2025-05-10', waterTemperature: 18.5, phLevel: 7.2, tds: 250, ec: 400, dissolvedOxygen: 6.5 },
      { sampleId: 'SAMP-004', siteId: 'A10', tripIdentifier: 'Trip 2', collectionDate: '2025-07-15', waterTemperature: 15.3, phLevel: 6.9, tds: 280, ec: 450, dissolvedOxygen: 5.8 },
    ],
    isolates: [
      { isolateId: 'ISO-101', sampleId: 'SAMP-001', organismIdentity: 'Klebsiella pneumoniae', sourceContext: 'Spinach at harvest', virulenceGenes: 'rmpA, iutA', binaryTypingProfile: { Intl1: true, Intl2: false, Intl3: true, TEM: true, SHV: true } },
    ],
    amrSequences: [
      { isolateId: 'ISO-101', geneSymbol: 'bla', elementType: 'AMR', resistanceClass: 'BETA-LACTAM',  subclass: 'BETA-LACTAM', identityPercentage: 81.36, coveragePercentage: 58.33 },
      { isolateId: 'ISO-101', geneSymbol: 'erm', elementType: 'AMR', resistanceClass: 'MACROLIDE',    subclass: 'MACROLIDE',   identityPercentage: 87.54, coveragePercentage: 39.76 },
    ],
    wgsMetrics: [
      { isolateId: 'ISO-101', qualityStatus: 'Passed', genotype: "aph(3')-Ia, blaCTX-M-14", predictedPhenotype: 'kanamycin, ampicillin, ceftriaxone', sirProfile: 'Resistant', plasmid: 'IncFIB(K)', genomeLength: 5017831, n50Value: 156657 },
    ],
  },
  {
    site: { siteId: 'B26', locationName: 'Hammanskraal', riverName: 'Apies River', latitude: -25.750, longitude: 28.230 },
    waterSamples: [
      { sampleId: 'SAMP-002', siteId: 'B26', tripIdentifier: 'Trip 1', collectionDate: '2025-05-10', waterTemperature: 19.1, phLevel: 7.4, tds: 260, ec: 410, dissolvedOxygen: 6.2 },
    ],
    isolates: [
      { isolateId: 'ISO-102', sampleId: 'SAMP-002', organismIdentity: 'Serratia fonticola', sourceContext: 'Irrigation pivot point', virulenceGenes: null, binaryTypingProfile: { Intl1: false, Intl2: false, Intl3: true, TEM: false, SHV: false } },
    ],
    amrSequences: [
      { isolateId: 'ISO-102', geneSymbol: 'aac(3)-I', elementType: 'AMR', resistanceClass: 'AMINOGLYCOSIDE', subclass: 'GENTAMICIN', identityPercentage: 98.68, coveragePercentage: 62.00 },
    ],
    wgsMetrics: [
      { isolateId: 'ISO-102', qualityStatus: 'Failed', genotype: 'tet(A)', predictedPhenotype: 'tetracycline', sirProfile: 'Intermediate', plasmid: 'Col(BS512)', genomeLength: 6133820, n50Value: 1660 },
    ],
  },
  {
    site: { siteId: 'B27', locationName: 'Tshwane',      riverName: 'Apies River', latitude: -25.752, longitude: 28.231 },
    waterSamples: [
      { sampleId: 'SAMP-003', siteId: 'B27', tripIdentifier: 'Trip 2', collectionDate: '2025-07-15', waterTemperature: 15.3, phLevel: 6.9, tds: 280, ec: 450, dissolvedOxygen: 5.8 },
    ],
    isolates: [
      { isolateId: 'ISO-103', sampleId: 'SAMP-003', organismIdentity: 'Escherichia coli', sourceContext: 'Irrigation pivot point', virulenceGenes: 'eae, bfpA', binaryTypingProfile: { Intl1: true, Intl2: true, Intl3: false, TEM: true, SHV: false } },
    ],
    amrSequences: [
      { isolateId: 'ISO-103', geneSymbol: 'arsN1', elementType: 'STRESS', resistanceClass: 'METAL', subclass: 'ARSENIC', identityPercentage: 90.86, coveragePercentage: 51.48 },
    ],
    wgsMetrics: [
      { isolateId: 'ISO-103', qualityStatus: 'Passed', genotype: 'blaTEM-1B, sul2', predictedPhenotype: 'ampicillin, sulfisoxazole', sirProfile: 'Susceptible', plasmid: 'IncX1', genomeLength: 5025249, n50Value: 125507 },
    ],
  },
]

const route = useRoute()

// ── State ──────────────────────────────────────────────────────────
const COLORS = ['#3B82F6', '#EF4444'] as const

// Pre-populate Site A from the ?siteA= query param set by the River Detail
// "Compare this site" button. Falls back to A10 if not provided.
const incomingSiteA = (route.query.siteA as string | undefined) ?? 'A10'
const defaultSiteB  = ALL_SITES.find(s => s.site.siteId !== incomingSiteA)?.site.siteId ?? 'B26'

const selectedIds   = ref<[string, string]>([incomingSiteA, defaultSiteB])
const tripFilterA   = ref<'all' | 'Trip 1' | 'Trip 2'>('all')
const tripFilterB   = ref<'all' | 'Trip 1' | 'Trip 2'>('all')
const selWaterParam = ref<keyof WaterSample>('phLevel')

const dataA = computed(() => ALL_SITES.find(s => s.site.siteId === selectedIds.value[0]) ?? null)
const dataB = computed(() => ALL_SITES.find(s => s.site.siteId === selectedIds.value[1]) ?? null)

onMounted(() => {
  // TODO: replace with real API calls per data.md:
  // GET /api/sites  — to populate dropdowns
  // GET /api/analytics/compare?siteA={id}&siteB={id}&metric=incidentRate  — metrics table
  // GET /api/sites/{siteId}/water-samples  — per-site water quality (×2)
  // GET /api/sites/{siteId}/isolates       — per-site isolates (×2)
  // GET /api/sites/{siteId}/amr-sequences  — per-site AMR genes (×2)
})

// Helpers (moved here from second script block to avoid duplicate declarations)
function sirColor(sir: string): string {
  if (sir === 'Resistant')    return '#EF4444'
  if (sir === 'Intermediate') return '#FBBF24'
  return '#34D399'
}
function sirBg(sir: string): string {
  if (sir === 'Resistant')    return 'var(--c-red-dim)'
  if (sir === 'Intermediate') return 'var(--c-amber-dim)'
  return 'var(--c-green-dim)'
}

// ── Helpers ────────────────────────────────────────────────────────
function filteredSamples(data: SiteData | null, slot: 0 | 1 = 0) {
  if (!data) return []
  const tf = slot === 0 ? tripFilterA.value : tripFilterB.value
  if (tf === 'all') return data.waterSamples
  return data.waterSamples.filter(s => s.tripIdentifier === tf)
}

function avgWater(data: SiteData | null, key: keyof WaterSample, slot: 0 | 1 = 0) {
  const vals = filteredSamples(data, slot).map(s => s[key] as number | null).filter(v => v !== null) as number[]
  if (!vals.length) return null
  return +(vals.reduce((a, b) => a + b, 0) / vals.length).toFixed(2)
}

// ── Labels — append trip when same site is selected for both slots ──
const labelA = computed(() => {
  const name = dataA.value?.site.locationName ?? 'A'
  if (selectedIds.value[0] === selectedIds.value[1]) {
    return `${name} \u00b7 ${tripFilterA.value === 'all' ? 'All Trips' : tripFilterA.value}`
  }
  return name
})
const labelB = computed(() => {
  const name = dataB.value?.site.locationName ?? 'B'
  if (selectedIds.value[0] === selectedIds.value[1]) {
    return `${name} \u00b7 ${tripFilterB.value === 'all' ? 'All Trips' : tripFilterB.value}`
  }
  return name
})

function intl1Rate(data: SiteData | null) {
  if (!data || !data.isolates.length) return 0
  return +((data.isolates.filter(i => i.binaryTypingProfile.Intl1).length / data.isolates.length) * 100).toFixed(0)
}

function resistantCount(data: SiteData | null) {
  if (!data) return 0
  return data.wgsMetrics.filter(w => w.sirProfile === 'Resistant').length
}

function wgsPassRate(data: SiteData | null) {
  if (!data || !data.wgsMetrics.length) return 0
  return +((data.wgsMetrics.filter(w => w.qualityStatus === 'Passed').length / data.wgsMetrics.length) * 100).toFixed(0)
}

// ── Chart helpers ──────────────────────────────────────────────────
const axLabel   = computed(() => ({ color: isDark.value ? '#5C7A94' : '#9CA3AF', fontSize: 11, fontFamily: 'DM Sans, sans-serif' }))
const splitLine = computed(() => ({ lineStyle: { color: isDark.value ? 'rgba(255,255,255,0.06)' : '#F3F4F6', type: 'dashed' as const } }))
const axLine    = computed(() => ({ lineStyle: { color: isDark.value ? 'rgba(255,255,255,0.08)' : '#E5E7EB' } }))

const PARAM_META: Record<string, { label: string; unit: string }> = {
  phLevel:          { label: 'pH Level',        unit: '' },
  waterTemperature: { label: 'Temperature',      unit: '°C' },
  tds:              { label: 'TDS',              unit: 'mg/L' },
  ec:               { label: 'EC',               unit: 'μS/cm' },
  dissolvedOxygen:  { label: 'Dissolved Oxygen', unit: 'mg/L' },
}

// Overlaid water quality line chart
const waterChartOption = computed(() => {
  const meta = PARAM_META[selWaterParam.value as string]
  if (!meta) return {}
  const samplesA = filteredSamples(dataA.value, 0)
  const samplesB = filteredSamples(dataB.value, 1)
  const allDates = Array.from(new Set([
    ...samplesA.map(s => `${s.tripIdentifier}·${s.collectionDate}`),
    ...samplesB.map(s => `${s.tripIdentifier}·${s.collectionDate}`),
  ])).sort()

  const getVals = (samples: typeof samplesA) =>
    allDates.map(d => {
      const s = samples.find(s => `${s.tripIdentifier}·${s.collectionDate}` === d)
      return s ? s[selWaterParam.value as keyof WaterSample] : null
    })

  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis' },
    legend: { data: [labelA.value, labelB.value], textStyle: axLabel.value, bottom: 0 },
    grid: { left: 52, right: 20, top: 14, bottom: 48 },
    xAxis: { type: 'category', data: allDates, axisLabel: { ...axLabel.value, rotate: 20 }, axisLine: axLine.value },
    yAxis: { type: 'value', axisLabel: { ...axLabel.value, formatter: (v: number) => `${v}${meta.unit}` }, splitLine: splitLine.value, axisLine: { show: false } },
    series: [
      { name: labelA.value, type: 'line', data: getVals(samplesA), smooth: true, symbol: 'circle', symbolSize: 7, lineStyle: { color: COLORS[0], width: 2.5 }, itemStyle: { color: COLORS[0], borderColor: isDark.value ? '#101D2E' : '#fff', borderWidth: 2 }, areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: COLORS[0] + '44' }, { offset: 1, color: COLORS[0] + '00' }] } } },
      { name: labelB.value, type: 'line', data: getVals(samplesB), smooth: true, symbol: 'circle', symbolSize: 7, lineStyle: { color: COLORS[1], width: 2.5 }, itemStyle: { color: COLORS[1], borderColor: isDark.value ? '#101D2E' : '#fff', borderWidth: 2 }, areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: COLORS[1] + '44' }, { offset: 1, color: COLORS[1] + '00' }] } } },
    ],
  }
})

// AMR class grouped bar
const amrClassOption = computed(() => {
  const getClassCounts = (data: SiteData | null) => {
    const map = new Map<string, number>()
    data?.amrSequences.forEach(s => map.set(s.resistanceClass, (map.get(s.resistanceClass) ?? 0) + 1))
    return map
  }
  const allClasses = Array.from(new Set([
    ...(dataA.value?.amrSequences.map(s => s.resistanceClass) ?? []),
    ...(dataB.value?.amrSequences.map(s => s.resistanceClass) ?? []),
  ]))
  const cA = getClassCounts(dataA.value)
  const cB = getClassCounts(dataB.value)

  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: [labelA.value, labelB.value], textStyle: axLabel.value, bottom: 0 },
    grid: { left: 130, right: 16, top: 10, bottom: 48 },
    xAxis: { type: 'value', axisLabel: axLabel.value, splitLine: splitLine.value },
    yAxis: { type: 'category', data: allClasses, axisLabel: { ...axLabel.value, fontFamily: 'monospace', fontSize: 10 }, axisLine: axLine.value },
    series: [
      { name: labelA.value, type: 'bar', barMaxWidth: 14, barGap: '8%', data: allClasses.map(c => cA.get(c) ?? 0), itemStyle: { color: COLORS[0], borderRadius: [0, 3, 3, 0] } },
      { name: labelB.value, type: 'bar', barMaxWidth: 14,                data: allClasses.map(c => cB.get(c) ?? 0), itemStyle: { color: COLORS[1], borderRadius: [0, 3, 3, 0] } },
    ],
  }
})

// Radar: 5 AMR/quality dimensions from actual data
// Axes: avg pH (0-14), avg Temp (0-35), AMR gene count (0-5), IntI1 rate (0-100), WGS pass rate (0-100)
const radarOption = computed(() => {
  const toScore = (data: SiteData | null) => [
    +(((avgWater(data, 'phLevel') ?? 0) / 14) * 100).toFixed(1),
    +(((avgWater(data, 'dissolvedOxygen') ?? 0) / 14) * 100).toFixed(1),
    +(((data?.amrSequences.length ?? 0) / 5) * 100).toFixed(1),
    +intl1Rate(data).toFixed(1),
    +wgsPassRate(data).toFixed(1),
  ]
  return {
    backgroundColor: 'transparent',
    tooltip: {},
    legend: { data: [labelA.value, labelB.value], textStyle: axLabel.value, bottom: 0 },
    radar: {
      indicator: [
        { name: 'Avg pH', max: 100 },
        { name: 'Avg DO', max: 100 },
        { name: 'AMR Gene Hits', max: 100 },
        { name: 'IntI1 Rate', max: 100 },
        { name: 'WGS Pass %', max: 100 },
      ],
      axisName: { color: isDark.value ? '#5C7A94' : '#9CA3AF', fontSize: 11 },
      splitLine: { lineStyle: { color: isDark.value ? 'rgba(255,255,255,0.08)' : '#E5E7EB' } },
      splitArea: { show: false },
      axisLine:  { lineStyle: { color: isDark.value ? 'rgba(255,255,255,0.08)' : '#E5E7EB' } },
    },
    series: [{
      type: 'radar',
      data: [
        { name: labelA.value, value: toScore(dataA.value), lineStyle: { color: COLORS[0], width: 2 }, itemStyle: { color: COLORS[0] }, areaStyle: { color: COLORS[0] + '33' } },
        { name: labelB.value, value: toScore(dataB.value), lineStyle: { color: COLORS[1], width: 2 }, itemStyle: { color: COLORS[1] }, areaStyle: { color: COLORS[1] + '33' } },
      ],
    }],
  }
})

// Metrics table rows — pulling from all four data sources
const metricRows = computed(() => [
  { label: 'River',                a: dataA.value?.site.riverName ?? '—',                             b: dataB.value?.site.riverName ?? '—' },
  { label: 'Location',             a: dataA.value?.site.locationName ?? '—',                          b: dataB.value?.site.locationName ?? '—' },
  { label: 'GPS',                  a: dataA.value ? `${dataA.value.site.latitude}, ${dataA.value.site.longitude}` : '—', b: dataB.value ? `${dataB.value.site.latitude}, ${dataB.value.site.longitude}` : '—' },
  { label: 'Trip Filter',          a: tripFilterA.value === 'all' ? 'All Trips' : tripFilterA.value,  b: tripFilterB.value === 'all' ? 'All Trips' : tripFilterB.value },
  { label: 'Water Samples',        a: filteredSamples(dataA.value, 0).length,                         b: filteredSamples(dataB.value, 1).length },
  { label: 'Isolates',             a: dataA.value?.isolates.length ?? '—',                            b: dataB.value?.isolates.length ?? '—' },
  { label: 'Avg pH',               a: avgWater(dataA.value, 'phLevel', 0) ?? '—',                    b: avgWater(dataB.value, 'phLevel', 1) ?? '—' },
  { label: 'Avg Temp (°C)',        a: avgWater(dataA.value, 'waterTemperature', 0) ?? '—',            b: avgWater(dataB.value, 'waterTemperature', 1) ?? '—' },
  { label: 'Avg TDS (mg/L)',       a: avgWater(dataA.value, 'tds', 0) ?? '—',                        b: avgWater(dataB.value, 'tds', 1) ?? '—' },
  { label: 'Avg EC (μS/cm)',       a: avgWater(dataA.value, 'ec', 0) ?? '—',                         b: avgWater(dataB.value, 'ec', 1) ?? '—' },
  { label: 'Avg DO (mg/L)',        a: avgWater(dataA.value, 'dissolvedOxygen', 0) ?? '—',            b: avgWater(dataB.value, 'dissolvedOxygen', 1) ?? '—' },
  { label: 'IntI1 Positive Rate',  a: intl1Rate(dataA.value) + '%',                                  b: intl1Rate(dataB.value) + '%' },
  { label: 'AMR Gene Hits',        a: dataA.value?.amrSequences.length ?? '—',                       b: dataB.value?.amrSequences.length ?? '—' },
  { label: 'WGS Passed QC',        a: wgsPassRate(dataA.value) + '%',                                b: wgsPassRate(dataB.value) + '%' },
  { label: 'Resistant Isolates',   a: resistantCount(dataA.value),                                   b: resistantCount(dataB.value) },
])
</script>

<template>
  <div class="compare-page">

    <!-- ── Header ── -->
    <header class="page-header">
      <div class="breadcrumb">
        <span class="crumb-link" @click="$router.push('/river')">River Detail</span>
        <i class="pi pi-chevron-right crumb-sep"></i>
        <span class="crumb-current">Site Comparison</span>
      </div>
      <h1 class="page-title">Site Comparison</h1>
      <p class="page-subtitle">Compare water quality and AMR profiles across Apies River sampling sites.</p>
    </header>

    <!-- ── Hero slot cards (picker + stats unified) ── -->
    <div class="hero-row">

      <!-- Slot A -->
      <div class="hero-card" :style="{ borderTopColor: COLORS[0] }">
        <div class="hero-picker-area">
          <span class="hero-slot-label" :style="{ color: COLORS[0] }">
            <i class="pi pi-map-marker"></i> Site A
          </span>
          <select v-model="selectedIds[0]" class="hero-select" :style="{ '--accent': COLORS[0] }">
            <option v-for="s in ALL_SITES" :key="s.site.siteId" :value="s.site.siteId">
              {{ s.site.siteId }} — {{ s.site.locationName }}
            </option>
          </select>
          <div class="hero-meta" v-if="dataA">{{ dataA.site.riverName }} · {{ dataA.site.latitude }}, {{ dataA.site.longitude }}</div>
          <div class="hero-trip-row">
            <span class="hero-trip-label">Trip</span>
            <div class="trip-tabs">
              <button v-for="t in (['all', 'Trip 1', 'Trip 2'] as const)" :key="t"
                class="trip-tab" :class="{ 'trip-tab--active': tripFilterA === t }"
                :style="tripFilterA === t ? { borderColor: COLORS[0], color: COLORS[0] } : {}"
                @click="tripFilterA = t">
                {{ t === 'all' ? 'All' : t }}
              </button>
            </div>
          </div>
        </div>
        <div class="hero-divider"></div>
        <div class="mini-stats" v-if="dataA">
          <div class="mini-stat">
            <span class="mini-label">Samples</span>
            <span class="mini-val" :style="{ color: COLORS[0] }">{{ filteredSamples(dataA, 0).length }}</span>
          </div>
          <div class="mini-stat">
            <span class="mini-label">Isolates</span>
            <span class="mini-val" :style="{ color: COLORS[0] }">{{ dataA.isolates.length }}</span>
          </div>
          <div class="mini-stat">
            <span class="mini-label">AMR Hits</span>
            <span class="mini-val" :style="{ color: COLORS[0] }">{{ dataA.amrSequences.length }}</span>
          </div>
          <div class="mini-stat">
            <span class="mini-label">WGS Pass</span>
            <span class="mini-val" :style="{ color: COLORS[0] }">{{ wgsPassRate(dataA) }}%</span>
          </div>
        </div>
      </div>

      <!-- VS badge -->
      <div class="vs-badge">VS</div>

      <!-- Slot B -->
      <div class="hero-card" :style="{ borderTopColor: COLORS[1] }">
        <div class="hero-picker-area">
          <span class="hero-slot-label" :style="{ color: COLORS[1] }">
            <i class="pi pi-map-marker"></i> Site B
          </span>
          <select v-model="selectedIds[1]" class="hero-select">
            <option v-for="s in ALL_SITES" :key="s.site.siteId" :value="s.site.siteId">
              {{ s.site.siteId }} — {{ s.site.locationName }}
            </option>
          </select>
          <div class="hero-meta" v-if="dataB">{{ dataB.site.riverName }} · {{ dataB.site.latitude }}, {{ dataB.site.longitude }}</div>
          <div class="hero-trip-row">
            <span class="hero-trip-label">Trip</span>
            <div class="trip-tabs">
              <button v-for="t in (['all', 'Trip 1', 'Trip 2'] as const)" :key="t"
                class="trip-tab" :class="{ 'trip-tab--active': tripFilterB === t }"
                :style="tripFilterB === t ? { borderColor: COLORS[1], color: COLORS[1] } : {}"
                @click="tripFilterB = t">
                {{ t === 'all' ? 'All' : t }}
              </button>
            </div>
          </div>
        </div>
        <div class="hero-divider"></div>
        <div class="mini-stats" v-if="dataB">
          <div class="mini-stat">
            <span class="mini-label">Samples</span>
            <span class="mini-val" :style="{ color: COLORS[1] }">{{ filteredSamples(dataB, 1).length }}</span>
          </div>
          <div class="mini-stat">
            <span class="mini-label">Isolates</span>
            <span class="mini-val" :style="{ color: COLORS[1] }">{{ dataB.isolates.length }}</span>
          </div>
          <div class="mini-stat">
            <span class="mini-label">AMR Hits</span>
            <span class="mini-val" :style="{ color: COLORS[1] }">{{ dataB.amrSequences.length }}</span>
          </div>
          <div class="mini-stat">
            <span class="mini-label">WGS Pass</span>
            <span class="mini-val" :style="{ color: COLORS[1] }">{{ wgsPassRate(dataB) }}%</span>
          </div>
        </div>
      </div>

    </div>

    <!-- ── Metrics table ── -->
    <section class="chart-section">
      <div class="section-header">
        <h2 class="section-title"><i class="pi pi-list"></i> Head-to-Head Metrics</h2>
        <span class="section-note">All four data sources · Epicollect, Binary Info, AMRFinderPlus, StarAMR</span>
      </div>
      <div class="table-card">
        <table class="compare-table">
          <thead>
            <tr>
              <th class="metric-col">Metric</th>
              <th :style="{ color: COLORS[0] }"><span class="th-dot" :style="{ background: COLORS[0] }"></span>{{ dataA?.site.locationName }}</th>
              <th :style="{ color: COLORS[1] }"><span class="th-dot" :style="{ background: COLORS[1] }"></span>{{ dataB?.site.locationName }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in metricRows" :key="row.label">
              <td class="metric-name">{{ row.label }}</td>
              <td class="metric-val" :style="{ color: COLORS[0] }">{{ row.a }}</td>
              <td class="metric-val" :style="{ color: COLORS[1] }">{{ row.b }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- ── Water quality overlay ── -->
    <section class="chart-section">
      <div class="section-header">
        <h2 class="section-title"><i class="pi pi-chart-line"></i> Water Quality Over Time</h2>
        <div class="param-tabs">
          <button v-for="(meta, key) in PARAM_META" :key="key"
            class="param-tab" :class="{ 'param-tab--active': selWaterParam === key }"
            @click="selWaterParam = key as any">{{ meta.label }}</button>
        </div>
      </div>
      <div class="chart-card">
        <VChart :option="waterChartOption" autoresize style="height:280px" />
      </div>
    </section>

    <!-- ── Radar + AMR class bar ── -->
    <div class="two-col">
      <section class="chart-section">
        <div class="section-header">
          <h2 class="section-title"><i class="pi pi-sliders-h"></i> Site Profile Radar</h2>
          <span class="section-note">Normalised across water quality &amp; AMR dimensions</span>
        </div>
        <div class="chart-card">
          <VChart :option="radarOption" autoresize style="height:300px" />
        </div>
      </section>
      <section class="chart-section">
        <div class="section-header">
          <h2 class="section-title"><i class="pi pi-shield"></i> AMR Resistance Classes</h2>
          <span class="section-note">AMRFinderPlus · gene hits by class</span>
        </div>
        <div class="chart-card">
          <VChart :option="amrClassOption" autoresize style="height:300px" />
        </div>
      </section>
    </div>

    <!-- ── WGS side-by-side tables ── -->
    <section class="chart-section">
      <div class="section-header">
        <h2 class="section-title"><i class="pi pi-dna"></i> WGS Metrics Comparison</h2>
        <span class="section-note">StarAMR · quality, genotype, SIR profile, plasmid</span>
      </div>
      <div class="wgs-cols">
        <template v-for="(data, idx) in [dataA, dataB]" :key="idx">
          <div class="wgs-col" v-if="data">
            <div class="wgs-col-header" :style="{ borderBottomColor: COLORS[idx], color: COLORS[idx] }">
              {{ data.site.locationName }}
            </div>
            <div class="table-card" style="border-radius:0 0 8px 8px; border-top:none;">
              <table class="data-table">
                <thead>
                  <tr><th>Isolate</th><th>QC</th><th>Predicted Phenotype</th><th>SIR</th><th>Plasmid</th></tr>
                </thead>
                <tbody>
                  <tr v-for="wgs in data.wgsMetrics" :key="wgs.isolateId">
                    <td class="mono dim">{{ wgs.isolateId }}</td>
                    <td><span class="quality-badge" :class="wgs.qualityStatus === 'Passed' ? 'quality--pass' : 'quality--fail'">{{ wgs.qualityStatus }}</span></td>
                    <td class="dim" style="font-size:11px;max-width:160px">{{ wgs.predictedPhenotype }}</td>
                    <td><span class="sir-badge" :style="{ color: sirColor(wgs.sirProfile), background: sirBg(wgs.sirProfile) }">{{ wgs.sirProfile }}</span></td>
                    <td class="mono dim" style="font-size:10.5px">{{ wgs.plasmid }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </template>
      </div>
    </section>

  </div>
</template>

<style scoped>
.compare-page { padding:28px 32px 60px; max-width:1280px; margin:0 auto; }

.breadcrumb { display:flex; align-items:center; gap:6px; margin-bottom:8px; font-size:12px; color:var(--c-text-muted); }
.crumb-link { cursor:pointer; color:var(--c-brand); }
.crumb-link:hover { text-decoration:underline; }
.crumb-sep { font-size:9px; }
.page-header { margin-bottom:22px; }
.page-title { font-family:'Zen Dots',sans-serif; font-size:22px; font-weight:700; color:var(--c-heading); margin:0 0 6px; }
.page-subtitle { font-size:13px; color:var(--c-text-muted); margin:0; }

/* Hero slot cards */
.hero-row { display:grid; grid-template-columns:1fr auto 1fr; align-items:center; gap:0; margin-bottom:24px; }
@media (max-width:700px) { .hero-row { grid-template-columns:1fr; } }

.hero-card { background:var(--c-card); border:1px solid var(--c-border); border-top:3px solid transparent; border-radius:10px; overflow:hidden; box-shadow:var(--c-shadow); }

.hero-picker-area { padding:20px 22px 16px; }

.hero-slot-label { font-size:10.5px; font-weight:700; text-transform:uppercase; letter-spacing:0.1em; display:flex; align-items:center; gap:5px; margin-bottom:10px; }

.hero-select { width:100%; background:var(--c-bg); border:1px solid var(--c-border); border-radius:7px; color:var(--c-heading); font-family:'Zen Dots',sans-serif; font-size:15px; font-weight:700; padding:9px 12px; cursor:pointer; outline:none; transition:border-color 0.15s; margin-bottom:6px; }
.hero-select:focus { border-color:var(--c-brand); }

.hero-meta { font-size:11px; color:var(--c-text-muted); margin-bottom:12px; }

.hero-trip-row { display:flex; align-items:center; gap:10px; }
.hero-trip-label { font-size:10px; font-weight:600; text-transform:uppercase; letter-spacing:0.08em; color:var(--c-text-muted); white-space:nowrap; }

.hero-divider { height:1px; background:var(--c-border); margin:0; }

.vs-badge { font-family:'Zen Dots',sans-serif; font-size:15px; font-weight:700; color:var(--c-text-muted); text-align:center; padding:0 18px; }

/* Shared trip tabs (used in hero cards) */
.trip-tabs { display:flex; gap:5px; }
.trip-tab { padding:5px 11px; border-radius:6px; border:1px solid var(--c-border); background:transparent; color:var(--c-text-muted); font-size:11px; cursor:pointer; transition:all 0.15s; font-family:'DM Sans',sans-serif; }
.trip-tab:hover { background:var(--c-card); color:var(--c-text); }
.trip-tab--active { font-weight:600; background:var(--c-brand-dim); }

/* Mini stats (shared) */
.mini-stats { display:grid; grid-template-columns:1fr 1fr; padding:14px 22px 18px; gap:14px; }
.mini-stat { display:flex; flex-direction:column; gap:2px; }
.mini-label { font-size:10px; text-transform:uppercase; letter-spacing:0.07em; color:var(--c-text-muted); }
.mini-val { font-size:22px; font-weight:700; font-family:'Zen Dots',sans-serif; line-height:1.1; }

/* (keep old col-* classes for any remaining usage) */
.col-dot { width:10px; height:10px; border-radius:50%; flex-shrink:0; }
.col-name { font-size:13px; font-weight:600; color:var(--c-heading); }
.col-sub { font-size:10.5px; color:var(--c-text-muted); margin-top:2px; }

/* Sections */
.chart-section { margin-bottom:22px; }
.section-header { display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:8px; margin-bottom:10px; }
.section-title { font-size:12px; font-weight:600; color:var(--c-heading); text-transform:uppercase; letter-spacing:0.08em; display:flex; align-items:center; gap:7px; }
.section-title .pi { color:var(--c-brand); }
.section-note { font-size:11px; color:var(--c-text-dim); }
.chart-card { background:var(--c-card); border:1px solid var(--c-border); border-radius:8px; padding:16px; box-shadow:var(--c-shadow); }

/* Param tabs */
.param-tabs { display:flex; flex-wrap:wrap; gap:6px; }
.param-tab { padding:4px 12px; border-radius:20px; border:1.5px solid var(--c-border); background:transparent; color:var(--c-text-muted); font-size:11px; cursor:pointer; transition:all 0.15s; font-family:'DM Sans',sans-serif; }
.param-tab:hover { background:var(--c-card); color:var(--c-text); }
.param-tab--active { border-color:var(--c-brand); color:var(--c-brand); font-weight:600; }

/* Two col */
.two-col { display:grid; grid-template-columns:1fr 1fr; gap:22px; margin-bottom:22px; }
@media (max-width:900px) { .two-col { grid-template-columns:1fr; } }

/* WGS side by side */
.wgs-cols { display:grid; grid-template-columns:1fr 1fr; gap:16px; }
@media (max-width:900px) { .wgs-cols { grid-template-columns:1fr; } }
.wgs-col { display:flex; flex-direction:column; }
.wgs-col-header { font-size:12px; font-weight:700; text-transform:uppercase; letter-spacing:0.07em; padding:9px 14px; background:var(--c-card); border:1px solid var(--c-border); border-radius:8px 8px 0 0; border-bottom-width:2px; }

/* Tables */
.table-card { background:var(--c-card); border:1px solid var(--c-border); border-radius:8px; overflow:auto; box-shadow:var(--c-shadow); }
.data-table { width:100%; border-collapse:collapse; font-size:12px; }
.data-table thead th { background:var(--c-card); color:var(--c-text-muted); font-size:10px; font-weight:600; letter-spacing:0.09em; text-transform:uppercase; padding:10px 14px; border-bottom:1px solid var(--c-border); text-align:left; white-space:nowrap; }
.data-table tbody tr { border-bottom:1px solid var(--c-border); transition:background 0.12s; }
.data-table tbody tr:last-child { border-bottom:none; }
.data-table tbody tr:hover { background:var(--c-brand-dim); }
.data-table tbody td { padding:9px 14px; color:var(--c-text); }

.compare-table { width:100%; border-collapse:collapse; font-size:12.5px; }
.compare-table thead th { background:var(--c-card); font-size:11px; font-weight:600; letter-spacing:0.07em; text-transform:uppercase; padding:10px 16px; border-bottom:1px solid var(--c-border); text-align:left; white-space:nowrap; }
.th-dot { display:inline-block; width:8px; height:8px; border-radius:50%; margin-right:5px; vertical-align:middle; }
.compare-table tbody tr { border-bottom:1px solid var(--c-border); transition:background 0.12s; }
.compare-table tbody tr:last-child { border-bottom:none; }
.compare-table tbody tr:hover { background:var(--c-brand-dim); }
.compare-table tbody td { padding:9px 16px; }
.metric-col { width:200px; }
.metric-name { color:var(--c-text-muted); font-weight:500; }
.metric-val { font-weight:600; }

.mono { font-family:monospace; font-size:11px; }
.dim { color:var(--c-text-muted); }
.quality-badge { display:inline-block; padding:2px 8px; border-radius:4px; font-size:10.5px; font-weight:600; }
.quality--pass { background:var(--c-green-dim); color:var(--c-green); }
.quality--fail { background:var(--c-red-dim); color:var(--c-red); }
.sir-badge { display:inline-block; padding:2px 8px; border-radius:4px; font-size:10.5px; font-weight:600; }
</style>