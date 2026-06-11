<script setup lang="ts">

import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart, BarChart, RadarChart } from 'echarts/charts'
import {
  TooltipComponent,
  GridComponent,
  LegendComponent,
  RadarComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { useThemeStore } from '@/stores/theme'

use([
  LineChart,
  BarChart,
  RadarChart,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  RadarComponent,
  CanvasRenderer,
])

const themeStore = useThemeStore()
const isDark = computed(() => themeStore.resolvedTheme === 'dark')

// ── Types (matching data.md schema) ───────────────────────────────
interface Site {
  siteId: string
  locationName: string
  riverName: string
  latitude: number
  longitude: number
}
interface WaterSample {
  sampleId: string
  siteId: string
  tripIdentifier: string
  collectionDate: string
  waterTemperature: number | null
  phLevel: number | null
  tds: number | null
  ec: number | null
  dissolvedOxygen: number | null
}
interface Isolate {
  isolateId: string
  sampleId: string
  organismIdentity: string
  sourceContext: string
  virulenceGenes: string | null
  binaryTypingProfile: {
    Intl1: boolean
    Intl2: boolean
    Intl3: boolean
    TEM: boolean
    SHV: boolean
  }
}
interface AmrSequence {
  isolateId: string
  geneSymbol: string
  elementType: string
  resistanceClass: string
  subclass: string
  identityPercentage: number
  coveragePercentage: number
}
interface WgsMetrics {
  isolateId: string
  qualityStatus: string
  genotype: string
  predictedPhenotype: string
  sirProfile: string
  plasmid: string
  genomeLength: number
  n50Value: number
}
interface SiteData {
  site: Site
  waterSamples: WaterSample[]
  isolates: Isolate[]
  amrSequences: AmrSequence[]
  wgsMetrics: WgsMetrics[]
}

// ── Constants ──────────────────────────────────────────────────────
const BASE = 'http://localhost:8080/api/v1'
const MAX_SLOTS = 5
const COLORS = ['#3B82F6', '#EF4444', '#10B981', '#F59E0B', '#8B5CF6']
const TRIP_OPTIONS = ['all', 'Trip 1', 'Trip 2'] as const
type TripFilter = typeof TRIP_OPTIONS[number]

const ALL_SITES = ref<SiteData[]>([])
const loading = ref(false)
const route = useRoute()

// ── Slot state — array instead of hardcoded A/B ────────────────────
interface Slot {
  siteId: string
  tripFilter: TripFilter
}

const slots = ref<Slot[]>([
  { siteId: '', tripFilter: 'all' },
  { siteId: '', tripFilter: 'all' },
])

// Safe accessor — avoids TS noUncheckedIndexedAccess errors on array[i]
function getSlot(i: number): Slot {
  return slots.value[i] ?? { siteId: '', tripFilter: 'all' }
}

function addSlot() {
  if (slots.value.length < MAX_SLOTS) {
    slots.value.push({ siteId: '', tripFilter: 'all' })
  }
}

function removeSlot(i: number) {
  if (slots.value.length > 2) {
    slots.value.splice(i, 1)
    wgsPages.value.splice(i, 1)
  }
}

// ── Derived data per slot ──────────────────────────────────────────
const slotData = computed(() =>
  slots.value.map(s => ALL_SITES.value.find(d => d.site.siteId === s.siteId) ?? null)
)

// ── WGS pagination — one page number per slot ─────────────────────
const WGS_PAGE_SIZE = 5
const wgsPages = ref<number[]>([1, 1])
const activeWgsTab = ref(0)

watch(
  () => slots.value.map(s => s.siteId),
  () => { wgsPages.value = slots.value.map(() => 1) },
  { deep: true }
)

watch(() => slots.value.length, (len) => {
  while (wgsPages.value.length < len) wgsPages.value.push(1)
  if (activeWgsTab.value >= len) activeWgsTab.value = len - 1
})

function wgsPaged(data: SiteData | null, slotIdx: number) {
  if (!data) return { rows: [] as WgsMetrics[], totalPages: 1, page: 1, total: 0 }
  const page = wgsPages.value[slotIdx] ?? 1
  const total = data.wgsMetrics.length
  const totalPages = Math.max(1, Math.ceil(total / WGS_PAGE_SIZE))
  const safe = Math.min(page, totalPages)
  const start = (safe - 1) * WGS_PAGE_SIZE
  return {
    rows: data.wgsMetrics.slice(start, start + WGS_PAGE_SIZE),
    totalPages,
    page: safe,
    total,
  }
}

function setWgsPage(slotIdx: number, page: number) {
  wgsPages.value[slotIdx] = page
}

// ── Water param selector ───────────────────────────────────────────
const selWaterParam = ref<keyof WaterSample>('phLevel')

// ── Data fetch ─────────────────────────────────────────────────────
const incomingSiteA = (route.query.siteA as string | undefined) ?? ''

onMounted(async () => {
  loading.value = true
  try {
    const sitesRes = await fetch(`${BASE}/sites`)
    const sites: Site[] = await sitesRes.json()

    const siteData = await Promise.all(
      sites.map(async (site) => {
        const [samplesRes, isolatesRes, amrRes, wgsRes] = await Promise.all([
          fetch(`${BASE}/sites/${site.siteId}/water-samples`),
          fetch(`${BASE}/sites/${site.siteId}/isolates`),
          fetch(`${BASE}/sites/${site.siteId}/amr-sequences`),
          fetch(`${BASE}/sites/${site.siteId}/wgs-metrics`),
        ])
        return {
          site,
          waterSamples: samplesRes.ok ? await samplesRes.json() : [],
          isolates: isolatesRes.ok ? await isolatesRes.json() : [],
          amrSequences: amrRes.ok ? await amrRes.json() : [],
          wgsMetrics: wgsRes.ok ? await wgsRes.json() : [],
        } as SiteData
      })
    )

    ALL_SITES.value = siteData

    const siteIds = siteData.map((s) => s.site.siteId)
    const a = incomingSiteA && siteIds.includes(incomingSiteA) ? incomingSiteA : (siteIds[0] ?? '')
    const b = siteIds.find((id) => id !== a) ?? (siteIds[1] ?? '')
    if (slots.value[0]) slots.value[0].siteId = a
    if (slots.value[1]) slots.value[1].siteId = b
  } catch (e) {
    console.error('Failed to load comparison data:', e)
  } finally {
    loading.value = false
  }
})

// ── Helpers ────────────────────────────────────────────────────────
function filteredSamples(data: SiteData | null, tripFilter: TripFilter) {
  if (!data) return []
  if (tripFilter === 'all') return data.waterSamples
  return data.waterSamples.filter((s) => s.tripIdentifier === tripFilter)
}

function avgWater(data: SiteData | null, key: keyof WaterSample, tripFilter: TripFilter) {
  const vals = filteredSamples(data, tripFilter)
    .map((s) => s[key] as number | null)
    .filter((v) => v !== null) as number[]
  if (!vals.length) return null
  return +(vals.reduce((a, b) => a + b, 0) / vals.length).toFixed(2)
}

function slotLabel(i: number): string {
  const data = slotData.value[i]
  const slot = getSlot(i)
  const name = data?.site.locationName ?? `Site ${i + 1}`
  const sameIds = slots.value.filter(s => s.siteId === slot.siteId)
  if (sameIds.length > 1) {
    return `${name} · ${slot.tripFilter === 'all' ? 'All Trips' : slot.tripFilter}`
  }
  return name
}

function intl1Rate(data: SiteData | null) {
  if (!data || !data.isolates.length) return 0
  return +(
    (data.isolates.filter((i) => i.binaryTypingProfile.Intl1).length / data.isolates.length) *
    100
  ).toFixed(0)
}

function resistantCount(data: SiteData | null) {
  if (!data) return 0
  return data.wgsMetrics.filter((w) => w.sirProfile === 'Resistant').length
}

function wgsPassRate(data: SiteData | null) {
  if (!data || !data.wgsMetrics.length) return 0
  return +(
    (data.wgsMetrics.filter((w) => w.qualityStatus === 'Passed').length / data.wgsMetrics.length) *
    100
  ).toFixed(0)
}

function sirColor(sir: string): string {
  if (sir === 'Resistant') return '#EF4444'
  if (sir === 'Intermediate') return '#FBBF24'
  return '#34D399'
}
function sirBg(sir: string): string {
  if (sir === 'Resistant') return 'var(--c-red-dim)'
  if (sir === 'Intermediate') return 'var(--c-amber-dim)'
  return 'var(--c-green-dim)'
}

// Computed for the currently active WGS tab data — avoids repeated undefined-unsafe indexing in template
const activeWgsData = computed((): SiteData | null => slotData.value[activeWgsTab.value] ?? null)

const axLabel = computed(() => ({
  color: isDark.value ? '#5C7A94' : '#9CA3AF',
  fontSize: 11,
  fontFamily: 'DM Sans, sans-serif',
}))
const splitLine = computed(() => ({
  lineStyle: {
    color: isDark.value ? 'rgba(255,255,255,0.06)' : '#F3F4F6',
    type: 'dashed' as const,
  },
}))
const axLine = computed(() => ({
  lineStyle: { color: isDark.value ? 'rgba(255,255,255,0.08)' : '#E5E7EB' },
}))

const PARAM_META: Record<string, { label: string; unit: string }> = {
  phLevel: { label: 'pH Level', unit: '' },
  waterTemperature: { label: 'Temperature', unit: '°C' },
  tds: { label: 'TDS', unit: 'mg/L' },
  ec: { label: 'EC', unit: 'μS/cm' },
  dissolvedOxygen: { label: 'Dissolved Oxygen', unit: 'mg/L' },
}

// ── Water quality line chart ───────────────────────────────────────
const waterChartOption = computed(() => {
  const meta = PARAM_META[selWaterParam.value as string]
  if (!meta) return {}

  const allDates = Array.from(
    new Set(
      slotData.value.flatMap((data, i) =>
        filteredSamples(data, getSlot(i).tripFilter).map(
          (s) => `${s.tripIdentifier}·${s.collectionDate}`
        )
      )
    )
  ).sort()

  const series = slotData.value.map((data, i) => {
    const samples = filteredSamples(data, getSlot(i).tripFilter)
    const vals = allDates.map((d) => {
      const s = samples.find((s) => `${s.tripIdentifier}·${s.collectionDate}` === d)
      return s ? s[selWaterParam.value as keyof WaterSample] : null
    })
    const color = COLORS[i]
    // only show area fills for ≤ 2 slots to avoid clutter
    const showArea = slots.value.length <= 2
    return {
      name: slotLabel(i),
      type: 'line',
      data: vals,
      smooth: true,
      symbol: 'circle',
      symbolSize: 7,
      lineStyle: { color, width: 2.5 },
      itemStyle: {
        color,
        borderColor: isDark.value ? '#101D2E' : '#fff',
        borderWidth: 2,
      },
      ...(showArea ? {
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: color + '44' },
              { offset: 1, color: color + '00' },
            ],
          },
        },
      } : {}),
    }
  })

  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis' },
    legend: {
      data: slots.value.map((_, i) => slotLabel(i)),
      textStyle: axLabel.value,
      bottom: 0,
    },
    grid: { left: 52, right: 20, top: 14, bottom: 48 },
    xAxis: {
      type: 'category',
      data: allDates,
      axisLabel: { ...axLabel.value, rotate: 20 },
      axisLine: axLine.value,
    },
    yAxis: {
      type: 'value',
      axisLabel: { ...axLabel.value, formatter: (v: number) => `${v}${meta.unit}` },
      splitLine: splitLine.value,
      axisLine: { show: false },
    },
    series,
  }
})

// ── AMR class grouped bar ──────────────────────────────────────────
const amrClassOption = computed(() => {
  const allClasses = Array.from(
    new Set(
      slotData.value.flatMap(d => d?.amrSequences.map(s => s.resistanceClass) ?? [])
    )
  )

  const series = slotData.value.map((data, i) => {
    const map = new Map<string, number>()
    data?.amrSequences.forEach(s => map.set(s.resistanceClass, (map.get(s.resistanceClass) ?? 0) + 1))
    return {
      name: slotLabel(i),
      type: 'bar',
      barMaxWidth: 14,
      barGap: '8%',
      data: allClasses.map(c => map.get(c) ?? 0),
      itemStyle: { color: COLORS[i], borderRadius: [0, 3, 3, 0] },
    }
  })

  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: {
      data: slots.value.map((_, i) => slotLabel(i)),
      textStyle: axLabel.value,
      bottom: 0,
    },
    grid: { left: 130, right: 16, top: 10, bottom: 48 },
    xAxis: { type: 'value', axisLabel: axLabel.value, splitLine: splitLine.value },
    yAxis: {
      type: 'category',
      data: allClasses,
      axisLabel: { ...axLabel.value, fontFamily: 'monospace', fontSize: 10 },
      axisLine: axLine.value,
    },
    series,
  }
})

// ── Radar chart ────────────────────────────────────────────────────
const radarOption = computed(() => {
  const maxAmr = Math.max(...slotData.value.map(d => d?.amrSequences.length ?? 0), 1)

  const seriesData = slotData.value.map((data, i) => {
    const color = COLORS[i]
    const tf = getSlot(i).tripFilter
    const score = [
      +(((avgWater(data, 'phLevel', tf) ?? 0) / 14) * 100).toFixed(1),
      +(((avgWater(data, 'dissolvedOxygen', tf) ?? 0) / 15) * 100).toFixed(1),
      +(((data?.amrSequences.length ?? 0) / maxAmr) * 100).toFixed(1),
      +intl1Rate(data).toFixed(1),
      +wgsPassRate(data).toFixed(1),
    ]
    // drop area fills at 4+ slots to keep it readable
    const showArea = slots.value.length <= 3
    return {
      name: slotLabel(i),
      value: score,
      lineStyle: { color, width: 2 },
      itemStyle: { color },
      ...(showArea ? { areaStyle: { color: color + '33' } } : {}),
    }
  })

  return {
    backgroundColor: 'transparent',
    tooltip: {},
    legend: {
      data: slots.value.map((_, i) => slotLabel(i)),
      textStyle: axLabel.value,
      bottom: 0,
    },
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
      axisLine: { lineStyle: { color: isDark.value ? 'rgba(255,255,255,0.08)' : '#E5E7EB' } },
    },
    series: [{ type: 'radar', data: seriesData }],
  }
})

// ── Metrics table ──────────────────────────────────────────────────
const metricRows = computed(() => [
  { label: 'River', values: slotData.value.map(d => d?.site.riverName ?? '—') },
  { label: 'Location', values: slotData.value.map(d => d?.site.locationName ?? '—') },
  {
    label: 'GPS',
    values: slotData.value.map(d =>
      d ? `${d.site.latitude}, ${d.site.longitude}` : '—'
    ),
  },
  {
    label: 'Trip Filter',
    values: slots.value.map((_, i) => getSlot(i).tripFilter === 'all' ? 'All Trips' : getSlot(i).tripFilter),
  },
  {
    label: 'Water Samples',
    values: slotData.value.map((d, i) => filteredSamples(d, getSlot(i).tripFilter).length),
  },
  { label: 'Isolates', values: slotData.value.map(d => d?.isolates.length ?? '—') },
  {
    label: 'Avg pH',
    values: slotData.value.map((d, i) => avgWater(d, 'phLevel', getSlot(i).tripFilter) ?? '—'),
  },
  {
    label: 'Avg Temp (°C)',
    values: slotData.value.map((d, i) => avgWater(d, 'waterTemperature', getSlot(i).tripFilter) ?? '—'),
  },
  {
    label: 'Avg TDS (mg/L)',
    values: slotData.value.map((d, i) => avgWater(d, 'tds', getSlot(i).tripFilter) ?? '—'),
  },
  {
    label: 'Avg EC (μS/cm)',
    values: slotData.value.map((d, i) => avgWater(d, 'ec', getSlot(i).tripFilter) ?? '—'),
  },
  {
    label: 'Avg DO (mg/L)',
    values: slotData.value.map((d, i) => avgWater(d, 'dissolvedOxygen', getSlot(i).tripFilter) ?? '—'),
  },
  {
    label: 'IntI1 Positive Rate',
    values: slotData.value.map(d => intl1Rate(d) + '%'),
  },
  {
    label: 'AMR Gene Hits',
    values: slotData.value.map(d => d?.amrSequences.length ?? '—'),
  },
  {
    label: 'WGS Passed QC',
    values: slotData.value.map(d => wgsPassRate(d) + '%'),
  },
  {
    label: 'Resistant Isolates',
    values: slotData.value.map(d => resistantCount(d)),
  },
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
      <p class="page-subtitle">
        Compare water quality and AMR profiles across Apies River sampling sites.
      </p>
    </header>

    <!-- ── Slot cards ── -->
    <div class="slots-section">
      <div class="slots-row">
        <div
          v-for="(slot, i) in slots"
          :key="i"
          class="hero-card"
          :style="{ borderTopColor: COLORS[i] }"
        >
          <!-- Remove button — only show when more than 2 slots -->
          <button
            v-if="slots.length > 2"
            class="slot-remove"
            :style="{ color: COLORS[i] }"
            @click="removeSlot(i)"
            title="Remove this site"
          >
            <i class="pi pi-times"></i>
          </button>

          <div class="hero-picker-area">
            <span class="hero-slot-label" :style="{ color: COLORS[i] }">
              <i class="pi pi-map-marker"></i> Site {{ String.fromCharCode(65 + i) }}
            </span>
            <select v-model="slot.siteId" class="hero-select">
              <option value="" disabled>Select a site…</option>
              <option v-for="s in ALL_SITES" :key="s.site.siteId" :value="s.site.siteId">
                {{ s.site.siteId }} — {{ s.site.locationName }}
              </option>
            </select>
            <div class="hero-meta" v-if="slotData[i]">
              {{ slotData[i]!.site.riverName }} · {{ slotData[i]!.site.latitude }},
              {{ slotData[i]!.site.longitude }}
            </div>
            <div class="hero-trip-row">
              <span class="hero-trip-label">Trip</span>
              <div class="trip-tabs">
                <button
                  v-for="t in TRIP_OPTIONS"
                  :key="t"
                  class="trip-tab"
                  :class="{ 'trip-tab--active': slot.tripFilter === t }"
                  :style="slot.tripFilter === t ? { borderColor: COLORS[i], color: COLORS[i] } : {}"
                  @click="slot.tripFilter = t"
                >
                  {{ t === 'all' ? 'All' : t }}
                </button>
              </div>
            </div>
          </div>

          <div class="hero-divider"></div>

          <div class="mini-stats" v-if="slotData[i]">
            <div class="mini-stat">
              <span class="mini-label">Samples</span>
              <span class="mini-val" :style="{ color: COLORS[i] }">
                {{ filteredSamples(slotData[i], slot.tripFilter).length }}
              </span>
            </div>
            <div class="mini-stat">
              <span class="mini-label">Isolates</span>
              <span class="mini-val" :style="{ color: COLORS[i] }">
                {{ slotData[i]!.isolates.length }}
              </span>
            </div>
            <div class="mini-stat">
              <span class="mini-label">AMR Hits</span>
              <span class="mini-val" :style="{ color: COLORS[i] }">
                {{ slotData[i]!.amrSequences.length }}
              </span>
            </div>
            <div class="mini-stat">
              <span class="mini-label">WGS Pass</span>
              <span class="mini-val" :style="{ color: COLORS[i] }">
                {{ wgsPassRate(slotData[i]) }}%
              </span>
            </div>
          </div>
        </div>

        <!-- Add site button -->
        <button
          v-if="slots.length < MAX_SLOTS"
          class="add-slot-btn"
          @click="addSlot"
        >
          <i class="pi pi-plus"></i>
          <span>Add Site</span>
        </button>
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
              <th v-for="(_, i) in slots" :key="i" :style="{ color: COLORS[i] }">
                <span class="th-dot" :style="{ background: COLORS[i] }"></span>
                {{ slotData[i]?.site.locationName ?? `Site ${String.fromCharCode(65 + i)}` }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in metricRows" :key="row.label">
              <td class="metric-name">{{ row.label }}</td>
              <td
                v-for="(val, i) in row.values"
                :key="i"
                class="metric-val"
                :style="{ color: COLORS[i] }"
              >
                {{ val }}
              </td>
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
          <button
            v-for="(meta, key) in PARAM_META"
            :key="key"
            class="param-tab"
            :class="{ 'param-tab--active': selWaterParam === key }"
            @click="selWaterParam = key as any"
          >
            {{ meta.label }}
          </button>
        </div>
      </div>
      <div class="chart-card">
        <VChart :option="waterChartOption" autoresize style="height: 280px" />
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
          <VChart :option="radarOption" autoresize style="height: 300px" />
        </div>
      </section>
      <section class="chart-section">
        <div class="section-header">
          <h2 class="section-title"><i class="pi pi-shield"></i> AMR Resistance Classes</h2>
          <span class="section-note">AMRFinderPlus · gene hits by class</span>
        </div>
        <div class="chart-card">
          <VChart :option="amrClassOption" autoresize style="height: 300px" />
        </div>
      </section>
    </div>

    <!-- ── WGS tabbed table ── -->
    <section class="chart-section">
      <div class="section-header">
        <h2 class="section-title"><i class="pi pi-dna"></i> WGS Metrics Comparison</h2>
        <span class="section-note">StarAMR · quality, genotype, SIR profile, plasmid</span>
      </div>

      <!-- Tab row — one tab per slot -->
      <div class="wgs-tabs">
        <button
          v-for="(_, i) in slots"
          :key="i"
          class="wgs-tab"
          :class="{ 'wgs-tab--active': activeWgsTab === i }"
          :style="activeWgsTab === i
            ? { borderColor: COLORS[i], color: COLORS[i], background: COLORS[i] + '18' }
            : {}"
          @click="activeWgsTab = i"
        >
          <span class="wgs-tab-dot" :style="{ background: COLORS[i] }"></span>
          {{ slotData[i]?.site.locationName ?? `Site ${String.fromCharCode(65 + i)}` }}
        </button>
      </div>

      <!-- Table for the active tab -->
      <div class="table-card" style="border-radius: 0 0 8px 8px; border-top: none">
        <template v-if="activeWgsData">
          <table class="data-table">
            <thead>
              <tr>
                <th>Isolate</th>
                <th>QC</th>
                <th>Predicted Phenotype</th>
                <th>SIR</th>
                <th>Plasmid</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!wgsPaged(activeWgsData, activeWgsTab).rows.length">
                <td colspan="5" class="dim" style="text-align: center; padding: 16px">
                  No WGS records.
                </td>
              </tr>
              <tr
                v-for="wgs in wgsPaged(activeWgsData, activeWgsTab).rows"
                :key="wgs.isolateId"
              >
                <td class="mono dim">{{ wgs.isolateId }}</td>
                <td>
                  <span
                    class="quality-badge"
                    :class="wgs.qualityStatus === 'Passed' ? 'quality--pass' : 'quality--fail'"
                  >{{ wgs.qualityStatus }}</span>
                </td>
                <td class="dim phenotype-cell" :title="wgs.predictedPhenotype">
                  {{ wgs.predictedPhenotype || '—' }}
                </td>
                <td>
                  <span
                    class="sir-badge"
                    :style="{ color: sirColor(wgs.sirProfile), background: sirBg(wgs.sirProfile) }"
                  >{{ wgs.sirProfile }}</span>
                </td>
                <td class="mono dim plasmid-cell" :title="wgs.plasmid">{{ wgs.plasmid || '—' }}</td>
              </tr>
            </tbody>
          </table>
          <div
            v-if="wgsPaged(activeWgsData, activeWgsTab).total > 0"
            class="wgs-pagination"
          >
            <span class="wgs-pagination-info">
              Page {{ wgsPaged(activeWgsData, activeWgsTab).page }} of
              {{ wgsPaged(activeWgsData, activeWgsTab).totalPages }} ·
              {{ wgsPaged(activeWgsData, activeWgsTab).total }} total
            </span>
            <div class="wgs-pagination-buttons">
              <button
                class="page-btn"
                :disabled="wgsPaged(activeWgsData, activeWgsTab).page <= 1"
                @click="setWgsPage(activeWgsTab, wgsPaged(activeWgsData, activeWgsTab).page - 1)"
              >
                <i class="pi pi-chevron-left"></i> Prev
              </button>
              <button
                class="page-btn"
                :disabled="wgsPaged(activeWgsData, activeWgsTab).page >= wgsPaged(activeWgsData, activeWgsTab).totalPages"
                @click="setWgsPage(activeWgsTab, wgsPaged(activeWgsData, activeWgsTab).page + 1)"
              >
                Next <i class="pi pi-chevron-right"></i>
              </button>
            </div>
          </div>
        </template>
        <div v-else class="dim" style="text-align: center; padding: 24px">
          No site selected for this slot.
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.compare-page {
  padding: 28px 32px 60px;
  max-width: 1280px;
  margin: 0 auto;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--c-text-muted);
}
.crumb-link {
  cursor: pointer;
  color: var(--c-brand);
}
.crumb-link:hover {
  text-decoration: underline;
}
.crumb-sep {
  font-size: 9px;
}
.page-header {
  margin-bottom: 22px;
}
.page-title {
  font-family: 'Inter', sans-serif;
  font-size: 22px;
  font-weight: 700;
  color: var(--c-heading);
  margin: 0 0 6px;
}
.page-subtitle {
  font-size: 13px;
  color: var(--c-text-muted);
  margin: 0;
}

/* ── Slots row ── */
.slots-section {
  margin-bottom: 24px;
}
.slots-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-start;
}

.hero-card {
  position: relative;
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-top: 3px solid transparent;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: var(--c-shadow);
  /* each card takes equal space, min 220px, max ~340px */
  flex: 1 1 220px;
  max-width: 340px;
}

.slot-remove {
  position: absolute;
  top: 8px;
  right: 8px;
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 11px;
  opacity: 0.6;
  transition: opacity 0.15s;
  padding: 2px 4px;
  border-radius: 4px;
  z-index: 1;
}
.slot-remove:hover {
  opacity: 1;
  background: var(--c-brand-dim);
}

.hero-picker-area {
  padding: 20px 22px 16px;
}
.hero-slot-label {
  font-size: 10.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  display: flex;
  align-items: center;
  gap: 5px;
  margin-bottom: 10px;
}
.hero-select {
  width: 100%;
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  border-radius: 7px;
  color: var(--c-heading);
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 700;
  padding: 9px 12px;
  cursor: pointer;
  outline: none;
  transition: border-color 0.15s;
  margin-bottom: 6px;
}
.hero-select:focus {
  border-color: var(--c-brand);
}
.hero-meta {
  font-size: 11px;
  color: var(--c-text-muted);
  margin-bottom: 12px;
}
.hero-trip-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.hero-trip-label {
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--c-text-muted);
  white-space: nowrap;
}
.hero-divider {
  height: 1px;
  background: var(--c-border);
}

/* Add site button */
.add-slot-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex: 0 0 auto;
  width: 80px;
  min-height: 160px;
  background: transparent;
  border: 2px dashed var(--c-border);
  border-radius: 10px;
  color: var(--c-text-muted);
  font-size: 11px;
  font-family: 'DM Sans', sans-serif;
  cursor: pointer;
  transition: all 0.15s;
  align-self: stretch;
}
.add-slot-btn:hover {
  border-color: var(--c-brand);
  color: var(--c-brand);
  background: var(--c-brand-dim);
}
.add-slot-btn .pi {
  font-size: 18px;
}

/* Trip tabs */
.trip-tabs {
  display: flex;
  gap: 5px;
}
.trip-tab {
  padding: 5px 11px;
  border-radius: 6px;
  border: 1px solid var(--c-border);
  background: transparent;
  color: var(--c-text-muted);
  font-size: 11px;
  cursor: pointer;
  transition: all 0.15s;
  font-family: 'DM Sans', sans-serif;
}
.trip-tab:hover {
  background: var(--c-card);
  color: var(--c-text);
}
.trip-tab--active {
  font-weight: 600;
  background: var(--c-brand-dim);
}

/* Mini stats */
.mini-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  padding: 14px 22px 18px;
  gap: 14px;
}
.mini-stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.mini-label {
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 0.07em;
  color: var(--c-text-muted);
}
.mini-val {
  font-size: 22px;
  font-weight: 700;
  font-family: 'Inter', sans-serif;
  line-height: 1.1;
}

/* Sections */
.chart-section {
  margin-bottom: 22px;
}
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}
.section-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--c-heading);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  display: flex;
  align-items: center;
  gap: 7px;
}
.section-title .pi {
  color: var(--c-brand);
}
.section-note {
  font-size: 11px;
  color: var(--c-text-dim);
}
.chart-card {
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: 8px;
  padding: 16px;
  box-shadow: var(--c-shadow);
}

/* Param tabs */
.param-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.param-tab {
  padding: 4px 12px;
  border-radius: 20px;
  border: 1.5px solid var(--c-border);
  background: transparent;
  color: var(--c-text-muted);
  font-size: 11px;
  cursor: pointer;
  transition: all 0.15s;
  font-family: 'DM Sans', sans-serif;
}
.param-tab:hover {
  background: var(--c-card);
  color: var(--c-text);
}
.param-tab--active {
  border-color: var(--c-brand);
  color: var(--c-brand);
  font-weight: 600;
}

/* Two col */
.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 22px;
  margin-bottom: 22px;
}
@media (max-width: 900px) {
  .two-col {
    grid-template-columns: 1fr;
  }
}

/* WGS tabs */
.wgs-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
  border-bottom: none;
}
.wgs-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 16px;
  font-size: 12px;
  font-weight: 600;
  font-family: 'DM Sans', sans-serif;
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-bottom: none;
  border-radius: 8px 8px 0 0;
  color: var(--c-text-muted);
  cursor: pointer;
  transition: all 0.15s;
  margin-right: 4px;
}
.wgs-tab:hover {
  color: var(--c-text);
  background: var(--c-brand-dim);
}
.wgs-tab--active {
  font-weight: 700;
  border-bottom: 2px solid transparent;
  position: relative;
  z-index: 1;
}
.wgs-tab-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* Tables */
.table-card {
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: 8px;
  overflow: auto;
  box-shadow: var(--c-shadow);
}
.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}
.data-table thead th {
  background: var(--c-card);
  color: var(--c-text-muted);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.09em;
  text-transform: uppercase;
  padding: 10px 14px;
  border-bottom: 1px solid var(--c-border);
  text-align: left;
  white-space: nowrap;
}
.data-table tbody tr {
  border-bottom: 1px solid var(--c-border);
  transition: background 0.12s;
}
.data-table tbody tr:last-child {
  border-bottom: none;
}
.data-table tbody tr:hover {
  background: var(--c-brand-dim);
}
.data-table tbody td {
  padding: 9px 14px;
  color: var(--c-text);
}

.compare-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12.5px;
}
.compare-table thead th {
  background: var(--c-card);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.07em;
  text-transform: uppercase;
  padding: 10px 16px;
  border-bottom: 1px solid var(--c-border);
  text-align: left;
  white-space: nowrap;
}
.th-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 5px;
  vertical-align: middle;
}
.compare-table tbody tr {
  border-bottom: 1px solid var(--c-border);
  transition: background 0.12s;
}
.compare-table tbody tr:last-child {
  border-bottom: none;
}
.compare-table tbody tr:hover {
  background: var(--c-brand-dim);
}
.compare-table tbody td {
  padding: 9px 16px;
}
.metric-col {
  width: 200px;
}
.metric-name {
  color: var(--c-text-muted);
  font-weight: 500;
}
.metric-val {
  font-weight: 600;
}

.mono {
  font-family: monospace;
  font-size: 11px;
}
.dim {
  color: var(--c-text-muted);
}
.quality-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 10.5px;
  font-weight: 600;
}
.quality--pass {
  background: var(--c-green-dim);
  color: var(--c-green);
}
.quality--fail {
  background: var(--c-red-dim);
  color: var(--c-red);
}
.sir-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 10.5px;
  font-weight: 600;
}
.phenotype-cell {
  font-size: 11px;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.plasmid-cell {
  font-size: 10.5px;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wgs-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-top: 1px solid var(--c-border);
  background: var(--c-card);
  font-size: 11px;
}
.wgs-pagination-info {
  color: var(--c-text-muted);
  font-family: 'DM Sans', sans-serif;
}
.wgs-pagination-buttons {
  display: flex;
  gap: 6px;
}
.page-btn {
  padding: 5px 10px;
  border-radius: 6px;
  border: 1px solid var(--c-border);
  background: transparent;
  color: var(--c-text);
  font-size: 11px;
  font-family: 'DM Sans', sans-serif;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: all 0.15s;
}
.page-btn:hover:not(:disabled) {
  border-color: var(--c-brand);
  color: var(--c-brand);
}
.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.page-btn .pi {
  font-size: 9px;
}

@media (max-width: 700px) {
  .slots-row {
    flex-direction: column;
  }
  .hero-card {
    max-width: 100%;
  }
  .add-slot-btn {
    width: 100%;
    min-height: 56px;
    flex-direction: row;
  }
}
</style>
