<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart, BarChart } from 'echarts/charts'
import { TooltipComponent, GridComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { useThemeStore } from '@/stores/theme'

use([LineChart, BarChart, TooltipComponent, GridComponent, LegendComponent, CanvasRenderer])

interface EChartsTooltipItem {
  axisValue: string
  value: number | null | undefined
  seriesName: string
  dataIndex: number
  seriesIndex: number
}

const BASE = 'http://localhost:8080/api/v1'


const vClickOutside = {
  mounted(el: HTMLElement & { _clickOutside?: (e: Event) => void }, binding: { value: () => void }) {
    el._clickOutside = (e: Event) => { if (!el.contains(e.target as Node)) binding.value() }
    document.addEventListener('click', el._clickOutside)
  },
  unmounted(el: HTMLElement & { _clickOutside?: (e: Event) => void }) {
    if (el._clickOutside) document.removeEventListener('click', el._clickOutside)
  },
}

const route      = useRoute()
const router     = useRouter()
const themeStore = useThemeStore()
const isDark     = computed(() => themeStore.resolvedTheme === 'dark')
const loading    = ref(true)

const siteId       = computed(() => (route.query.siteId as string) ?? 'A10')
const selectedSite = ref(siteId.value)
const searchQuery  = ref('')
const dropdownOpen = ref(false)

// ── Site list for selector ─────────────────────────────────────────
const ALL_SITES = ref<{ siteId: string; locationName: string; riverName: string }[]>([])

async function loadAllSites() {
  try {
    const res = await fetch(`${BASE}/sites`)
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    ALL_SITES.value = await res.json()
  } catch (e) {
    console.error('Failed to load sites list:', e)
    ALL_SITES.value = [
      { siteId: 'A10', locationName: 'Pretoria',     riverName: 'Apies River' },
      { siteId: 'B26', locationName: 'Hammanskraal', riverName: 'Apies River' },
      { siteId: 'B27', locationName: 'Tshwane',      riverName: 'Apies River' },
    ]
  }
}

const filteredSites = computed(() =>
  ALL_SITES.value.filter(s =>
    s.locationName.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
    s.riverName.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
    s.siteId.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
)

function selectSite(id: string) {
  selectedSite.value = id
  dropdownOpen.value = false
  searchQuery.value  = ''
  router.push({ path: '/river', query: { siteId: id } })
}
function closeDropdown() { dropdownOpen.value = false; searchQuery.value = '' }

// ── Types ──────────────────────────────────────────────────────────
interface Site { siteId: string; locationName: string; riverName: string; latitude: number; longitude: number }
interface WaterSample {
  sampleId: string; siteId: string; tripIdentifier: string; collectionDate: string
  waterTemperature: number | null; phLevel: number | null; tds: number | null; ec: number | null; dissolvedOxygen: number | null
}
interface BinaryTypingProfile { Intl1: boolean; Intl2: boolean; Intl3: boolean; TEM: boolean; SHV: boolean }
interface Isolate {
  isolateId: string; sampleId: string; isolateNumber: string; organismIdentity: string
  sourceContext: string; arCode: string; virulenceGenes: string | null; binaryTypingProfile: BinaryTypingProfile
}
interface AmrSequence {
  isolateId: string; geneSymbol: string; sequenceName: string; elementType: string
  resistanceClass: string; resistanceSubclass: string; identityPercentage: number; coveragePercentage: number
}
interface WgsMetrics {
  isolateId: string; qualityStatus: string; genotype: string; predictedPhenotype: string
  predictedSirProfile: string; plasmid: string; genomeLength: number; n50Value: number
}
interface SiteDetail {
  site: Site; waterSamples: WaterSample[]; isolates: Isolate[]
  amrSequences: AmrSequence[]; wgsMetrics: WgsMetrics[]
}

const detail   = ref<SiteDetail | null>(null)
const selParam = ref<keyof WaterSample>('phLevel')

async function loadSite(id: string) {
  loading.value = true
  detail.value  = null
  try {
    const [siteRes, samplesRes, isolatesRes, amrRes, wgsRes] = await Promise.all([
      fetch(`${BASE}/sites/${id}`),
      fetch(`${BASE}/sites/${id}/water-samples`),
      fetch(`${BASE}/sites/${id}/isolates`),
      fetch(`${BASE}/sites/${id}/amr-sequences`),
      fetch(`${BASE}/sites/${id}/wgs-metrics`),
    ])
    if (!siteRes.ok || !samplesRes.ok || !isolatesRes.ok) throw new Error('Fetch failed')

    const site         = await siteRes.json()
    const waterSamples = await samplesRes.json()
    const isolatesRaw  = await isolatesRes.json()
    const amrSequences = amrRes.ok ? await amrRes.json() : []
    const wgsMetrics   = wgsRes.ok ? await wgsRes.json() : []

    const isolates = isolatesRaw.map((iso: any) => ({
      ...iso,
      binaryTypingProfile: {
        Intl1: iso.binaryTypingProfile?.Intl1 ?? false,
        Intl2: iso.binaryTypingProfile?.Intl2 ?? false,
        Intl3: iso.binaryTypingProfile?.Intl3 ?? false,
        TEM:   iso.binaryTypingProfile?.TEM   ?? false,
        SHV:   iso.binaryTypingProfile?.SHV   ?? false,
      },
    }))

    detail.value = { site, waterSamples, isolates, amrSequences, wgsMetrics }
  } catch (e) {
    console.error('Failed to load site:', e)
    detail.value = null
  } finally {
    loading.value = false
  }
}

onMounted(async () => { await loadAllSites(); loadSite(siteId.value) })
watch(siteId, (newId) => { selectedSite.value = newId; loadSite(newId) })
function goToCompare() { router.push({ path: '/compare', query: { siteA: siteId.value } }) }

// ── Param meta ─────────────────────────────────────────────────────
interface ParamMeta { label: string; unit: string; color: string }
const PARAM_META: Record<string, ParamMeta> = {
  phLevel:          { label: 'pH Level',        unit: '',       color: '#34D399' },
  waterTemperature: { label: 'Temperature',      unit: '°C',    color: '#FB923C' },
  tds:              { label: 'TDS',              unit: 'mg/L',  color: '#60A5FA' },
  ec:               { label: 'EC',               unit: 'μS/cm', color: '#A78BFA' },
  dissolvedOxygen:  { label: 'Dissolved Oxygen', unit: 'mg/L',  color: '#FBBF24' },
}

// ── Chart helpers ──────────────────────────────────────────────────
const axLabel   = computed(() => ({ color: isDark.value ? '#5C7A94' : '#9CA3AF', fontSize: 11, fontFamily: 'DM Sans, sans-serif' }))
const splitLine = computed(() => ({ lineStyle: { color: isDark.value ? 'rgba(255,255,255,0.06)' : '#F3F4F6', type: 'dashed' as const } }))
const axLine    = computed(() => ({ lineStyle: { color: isDark.value ? 'rgba(255,255,255,0.08)' : '#E5E7EB' } }))

const waterChartOption = computed(() => {
  if (!detail.value) return {}
  const meta = PARAM_META[selParam.value as string]
  if (!meta) return {}
  const samples = detail.value.waterSamples
  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      formatter: (params: EChartsTooltipItem | EChartsTooltipItem[]) => {
        const p = Array.isArray(params) ? params : [params]
        if (!p[0]) return ''
        return `${p[0].axisValue}<br/>${meta.label}: <b>${p[0].value ?? '—'} ${meta.unit}</b>`
      },
    },
    grid: { left: 52, right: 20, top: 16, bottom: 40 },
    xAxis: { type: 'category', data: samples.map(s => `${s.tripIdentifier} · ${s.collectionDate}`), axisLabel: { ...axLabel.value, rotate: 15 }, axisLine: axLine.value },
    yAxis: { type: 'value', axisLabel: { ...axLabel.value, formatter: (v: number) => `${v}${meta.unit}` }, splitLine: splitLine.value, axisLine: { show: false } },
    series: [{ type: 'line', data: samples.map(s => s[selParam.value] as number | null), smooth: true, symbol: 'circle', symbolSize: 8,
      lineStyle: { color: meta.color, width: 2.5 }, itemStyle: { color: meta.color, borderColor: isDark.value ? '#101D2E' : '#fff', borderWidth: 2 },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: meta.color + '44' }, { offset: 1, color: meta.color + '00' }] } } }],
  }
})

const amrBarOption = computed(() => {
  if (!detail.value) return {}
  const classMap = new Map<string, number>()
  for (const seq of detail.value.amrSequences) classMap.set(seq.resistanceClass, (classMap.get(seq.resistanceClass) ?? 0) + 1)
  const entries = [...classMap.entries()].sort((a, b) => a[1] - b[1])
  const CLASS_COLORS: Record<string, string> = { 'BETA-LACTAM': '#EF4444', 'MACROLIDE': '#F59E0B', 'AMINOGLYCOSIDE': '#8B5CF6', 'METAL': '#6B7280', 'SULFONAMIDE': '#3B82F6', 'TETRACYCLINE': '#FB923C' }
  return {
    backgroundColor: 'transparent', tooltip: { trigger: 'axis' },
    grid: { left: 130, right: 30, top: 10, bottom: 20 },
    xAxis: { type: 'value', axisLabel: axLabel.value, splitLine: splitLine.value },
    yAxis: { type: 'category', data: entries.map(e => e[0]), axisLabel: { ...axLabel.value, fontFamily: 'monospace', fontSize: 10.5 }, axisLine: axLine.value },
    series: [{ type: 'bar', barMaxWidth: 18, data: entries.map(e => ({ value: e[1], itemStyle: { color: CLASS_COLORS[e[0]] ?? '#3B82F6', borderRadius: [0, 3, 3, 0] } })) }],
  }
})

const binaryProfileSummary = computed(() => {
  if (!detail.value) return []
  const keys: (keyof BinaryTypingProfile)[] = ['Intl1', 'Intl2', 'Intl3', 'TEM', 'SHV']
  return keys.map(k => ({ marker: k, positive: detail.value!.isolates.filter(i => i.binaryTypingProfile[k]).length, total: detail.value!.isolates.length }))
})

const resistanceMatrix = computed(() => {
  if (!detail.value) return { antibiotics: [], rows: [] }
  const antibioticSet = new Set<string>()
  for (const wgs of detail.value.wgsMetrics) {
    if (wgs.predictedPhenotype) wgs.predictedPhenotype.split(',').map(a => a.trim()).filter(Boolean).forEach(a => antibioticSet.add(a))
  }
  const antibiotics = [...antibioticSet].sort()
  const rows = detail.value.isolates.map(iso => {
    const wgs = detail.value!.wgsMetrics.find(w => w.isolateId === iso.isolateId)
    const phenotypeList = wgs?.predictedPhenotype ? wgs.predictedPhenotype.split(',').map(a => a.trim()) : []
    return { isolateId: iso.isolateId, organism: iso.organismIdentity, cells: antibiotics.map(ab => !wgs ? 'NA' : phenotypeList.includes(ab) ? wgs.predictedSirProfile : 'Susceptible') }
  })
  return { antibiotics, rows }
})

const wgsPassCount  = computed(() => detail.value?.wgsMetrics.filter(w => w.qualityStatus?.toUpperCase() === 'PASS').length ?? 0)
const wgsTotalCount = computed(() => detail.value?.wgsMetrics.length ?? 0)
const organisms     = computed(() => {
  if (!detail.value) return []
  const map = new Map<string, number>()
  for (const iso of detail.value.isolates) map.set(iso.organismIdentity, (map.get(iso.organismIdentity) ?? 0) + 1)
  return [...map.entries()].sort((a, b) => b[1] - a[1])
})

function sirColor(sir: string) { return sir === 'Resistant' ? (isDark.value ? '#EF4444' : '#C62828') : sir === 'Intermediate' ? (isDark.value ? '#FBBF24' : '#D97706') : (isDark.value ? '#34D399' : '#059669') }
function sirBg(sir: string)    { return sir === 'Resistant' ? 'var(--c-red-dim)' : sir === 'Intermediate' ? 'var(--c-amber-dim)' : 'var(--c-green-dim)' }
function matrixColor(s: string) { return s === 'Resistant' ? (isDark.value ? '#EF444422' : '#FEE2E2') : s === 'Intermediate' ? (isDark.value ? '#FBBF2422' : '#FEF9C3') : s === 'Susceptible' ? (isDark.value ? '#34D39922' : '#D1FAE5') : (isDark.value ? 'rgba(255,255,255,0.04)' : '#F9FAFB') }
function matrixTextColor(s: string) { return s === 'Resistant' ? (isDark.value ? '#F87171' : '#DC2626') : s === 'Intermediate' ? (isDark.value ? '#FBBF24' : '#D97706') : s === 'Susceptible' ? (isDark.value ? '#34D399' : '#059669') : (isDark.value ? '#374D61' : '#9CA3AF') }
function matrixLabel(s: string) { return s === 'Resistant' ? 'R' : s === 'Intermediate' ? 'I' : s === 'Susceptible' ? 'S' : '—' }

// ── Pagination ─────────────────────────────────────────────────────
const PAGE_SIZE = 10
const matrixPage   = ref(1)
const isolatesPage = ref(1)
const amrPage      = ref(1)
const wgsPage      = ref(1)
const samplesPage  = ref(1)

watch(() => detail.value?.site.siteId, () => {
  matrixPage.value = 1
  isolatesPage.value = 1
  amrPage.value = 1
  wgsPage.value = 1
  samplesPage.value = 1
})

function pageSlice<T>(arr: T[], page: number): T[] {
  const start = (page - 1) * PAGE_SIZE
  return arr.slice(start, start + PAGE_SIZE)
}
function totalPages(len: number): number {
  return Math.max(1, Math.ceil(len / PAGE_SIZE))
}
function clampPage(pageRef: { value: number }, len: number) {
  const max = totalPages(len)
  if (pageRef.value > max) pageRef.value = max
  if (pageRef.value < 1)   pageRef.value = 1
}

const pagedMatrixRows = computed(() => {
  clampPage(matrixPage, resistanceMatrix.value.rows.length)
  return pageSlice(resistanceMatrix.value.rows, matrixPage.value)
})
const pagedIsolates = computed(() => {
  if (!detail.value) return []
  clampPage(isolatesPage, detail.value.isolates.length)
  return pageSlice(detail.value.isolates, isolatesPage.value)
})
const pagedAmr = computed(() => {
  if (!detail.value) return []
  clampPage(amrPage, detail.value.amrSequences.length)
  return pageSlice(detail.value.amrSequences, amrPage.value)
})
const pagedWgs = computed(() => {
  if (!detail.value) return []
  clampPage(wgsPage, detail.value.wgsMetrics.length)
  return pageSlice(detail.value.wgsMetrics, wgsPage.value)
})
const pagedSamples = computed(() => {
  if (!detail.value) return []
  clampPage(samplesPage, detail.value.waterSamples.length)
  return pageSlice(detail.value.waterSamples, samplesPage.value)
})

const safetyRating = computed(() => {
  if (!detail.value) return null
  const wgs = detail.value.wgsMetrics
  const hasResistant    = wgs.some(w => w.predictedSirProfile === 'Resistant')
  const hasIntermediate = wgs.some(w => w.predictedSirProfile === 'Intermediate')
  const doVals   = detail.value.waterSamples.map(s => s.dissolvedOxygen).filter((v): v is number => v !== null)
  const latestDo = doVals.length ? doVals[doVals.length - 1] : null
  const lowO2    = latestDo != null && latestDo < 4
  const rClasses = new Set(detail.value.amrSequences.filter(s => s.elementType === 'AMR').map(s => s.resistanceClass))
  const mdr      = rClasses.size >= 2
  if (hasResistant && mdr)   return { level: 'HIGH RISK',    icon: 'pi-ban',                 color: 'var(--c-red)',   bg: 'var(--c-red-dim)',   border: 'var(--c-red)',   headline: 'Not recommended for recreational water contact.',  detail: `Multidrug-resistant bacteria detected (${rClasses.size} resistance classes). Avoid contact. Boil or treat water before use.` }
  if (hasResistant || lowO2) return { level: 'CAUTION',      icon: 'pi-exclamation-triangle', color: 'var(--c-amber)', bg: 'var(--c-amber-dim)', border: 'var(--c-amber)', headline: 'Exercise caution near this water.',                 detail: lowO2 ? `Low dissolved oxygen (${latestDo} mg/L) detected. ${hasResistant ? 'Resistant bacteria also found.' : ''} Avoid prolonged contact.` : 'Antibiotic-resistant bacteria detected. Avoid swallowing water.' }
  if (hasIntermediate)       return { level: 'LOW ADVISORY', icon: 'pi-info-circle',          color: 'var(--c-brand)', bg: 'var(--c-brand-dim)', border: 'var(--c-brand)', headline: 'Generally safe, with minor advisory.',              detail: 'Bacteria with reduced antibiotic susceptibility detected. Standard precautions apply.' }
  return                            { level: 'LOW RISK',     icon: 'pi-check-circle',         color: 'var(--c-green)', bg: 'var(--c-green-dim)', border: 'var(--c-green)', headline: 'No significant AMR threat detected at this site.', detail: 'No antibiotic-resistant bacteria found. Standard hygiene practices still recommended.' }
})
</script>

<template>
  <div class="river-detail">

    <div v-if="loading" class="skeleton-wrap">
      <div class="skel" style="height:72px"></div>
      <div class="skel-row"><div class="skel" style="height:72px" v-for="n in 4" :key="n"></div></div>
      <div class="skel" style="height:280px"></div>
      <div class="skel-row"><div class="skel" style="height:240px"></div><div class="skel" style="height:240px"></div></div>
      <div class="skel" style="height:220px"></div>
    </div>

    <template v-else-if="detail">

      <header class="page-header">
        <div class="breadcrumb-row">
          <div class="breadcrumb">
            <span class="crumb-link" @click="$router.push('/map')">Map</span>
            <i class="pi pi-chevron-right crumb-sep"></i>
            <span class="crumb-current">River Detail</span>
          </div>
        </div>
        <div class="site-hero-card" v-click-outside="closeDropdown">
          <div class="hero-top-row">
            <span class="hero-site-label"><i class="pi pi-map-marker"></i> Site {{ detail.site.siteId }}</span>
            <button class="compare-btn" @click="goToCompare"><i class="pi pi-sliders-h"></i> Compare sites</button>
          </div>
          <button class="hero-name-btn" @click="dropdownOpen = !dropdownOpen">
            <h1 class="page-title">{{ detail.site.locationName }}</h1>
            <i class="pi pi-chevron-down hero-chevron" :class="{ 'hero-chevron--open': dropdownOpen }"></i>
          </button>
          <div v-if="dropdownOpen" class="hero-dropdown">
            <div class="selector-search">
              <i class="pi pi-search search-icon"></i>
              <input v-model="searchQuery" class="search-input" placeholder="Search by name, river or ID…" autofocus />
            </div>
            <div class="selector-list">
              <button v-for="s in filteredSites" :key="s.siteId" class="selector-option" :class="{ 'selector-option--active': s.siteId === selectedSite }" @click="selectSite(s.siteId)">
                <div class="option-main">
                  <span class="option-name">{{ s.locationName }}</span>
                  <span class="option-river">{{ s.riverName }}</span>
                </div>
                <span class="option-id">{{ s.siteId }}</span>
              </button>
              <div v-if="filteredSites.length === 0" class="selector-empty">No sites match "{{ searchQuery }}"</div>
            </div>
          </div>
          <div class="meta-pills">
            <span class="pill"><i class="pi pi-waves"></i> {{ detail.site.riverName }}</span>
            <span class="pill"><i class="pi pi-compass"></i> {{ detail.site.latitude }}, {{ detail.site.longitude }}</span>
            <span class="pill"><i class="pi pi-database"></i> {{ detail.waterSamples.length }} samples</span>
            <span class="pill"><i class="pi pi-microscope"></i> {{ detail.isolates.length }} isolates</span>
          </div>
        </div>
      </header>

      <div v-if="safetyRating" class="safety-banner" :style="{ background: safetyRating.bg, borderColor: safetyRating.border }">
        <div class="safety-icon-wrap" :style="{ color: safetyRating.color }"><i :class="`pi ${safetyRating.icon} safety-icon`"></i></div>
        <div class="safety-body">
          <div class="safety-header-row">
            <span class="safety-level" :style="{ color: safetyRating.color }">{{ safetyRating.level }}</span>
            <span class="safety-tag">Public Safety Assessment</span>
          </div>
          <p class="safety-headline">{{ safetyRating.headline }}</p>
          <p class="safety-detail">{{ safetyRating.detail }}</p>
        </div>
      </div>

      <div class="stat-grid">
        <div class="stat-card">
          <div class="stat-icon" style="background:var(--c-brand-dim);color:var(--c-brand)"><i class="pi pi-calendar"></i></div>
          <div class="stat-body"><span class="stat-label">Sampling Trips</span><span class="stat-value">{{ new Set(detail.waterSamples.map(s => s.tripIdentifier)).size }}</span></div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background:var(--c-red-dim);color:var(--c-red)"><i class="pi pi-shield"></i></div>
          <div class="stat-body"><span class="stat-label">AMR Gene Hits</span><span class="stat-value">{{ detail.amrSequences.length }}</span></div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background:var(--c-green-dim);color:var(--c-green)"><i class="pi pi-check-circle"></i></div>
          <div class="stat-body"><span class="stat-label">WGS Passed QC</span><span class="stat-value">{{ wgsPassCount }}<span class="stat-denom">/{{ wgsTotalCount }}</span></span></div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background:var(--c-amber-dim);color:var(--c-amber)"><i class="pi pi-tag"></i></div>
          <div class="stat-body"><span class="stat-label">Unique Organisms</span><span class="stat-value">{{ organisms.length }}</span></div>
        </div>
      </div>

      <section class="chart-section">
        <div class="section-header">
          <h2 class="section-title"><i class="pi pi-chart-line"></i> Water Quality Over Time</h2>
          <div class="param-tabs">
            <button v-for="(meta, key) in PARAM_META" :key="key" class="param-tab" :class="{ 'param-tab--active': selParam === key }" :style="selParam === key ? { borderColor: meta.color, color: meta.color } : {}" @click="selParam = key as typeof selParam">{{ meta.label }}</button>
          </div>
        </div>
        <div class="chart-card"><VChart :option="waterChartOption" autoresize style="height:260px" /></div>
      </section>

      <div class="two-col">
        <section class="chart-section">
          <div class="section-header"><h2 class="section-title"><i class="pi pi-shield"></i> AMR Resistance Classes</h2><span class="section-note">AMRFinderPlus · gene hits by class</span></div>
          <div class="chart-card"><VChart :option="amrBarOption" autoresize style="height:200px" /></div>
        </section>
        <section class="chart-section">
          <div class="section-header"><h2 class="section-title"><i class="pi pi-sliders-h"></i> Binary Typing Profile</h2><span class="section-note">Integron &amp; gene markers · per isolate</span></div>
          <div class="chart-card binary-profile">
            <div v-for="marker in binaryProfileSummary" :key="marker.marker" class="marker-row">
              <span class="marker-name">{{ marker.marker }}</span>
              <div class="marker-dots">
                <span v-for="iso in detail.isolates" :key="iso.isolateId" class="marker-dot" :class="iso.binaryTypingProfile[marker.marker as keyof BinaryTypingProfile] ? 'dot--pos' : 'dot--neg'" :title="iso.isolateId"></span>
              </div>
              <span class="marker-count" :class="marker.positive > 0 ? 'count--pos' : 'count--neg'">{{ marker.positive }}/{{ marker.total }}</span>
            </div>
          </div>
        </section>
      </div>

      <section class="chart-section" v-if="resistanceMatrix.antibiotics.length > 0">
        <div class="section-header"><h2 class="section-title"><i class="pi pi-table"></i> Antibiotic Resistance Profile</h2><span class="section-note">StarAMR · predicted phenotype per isolate</span></div>
        <div class="chart-card matrix-card">
          <div class="matrix-legend"><span class="mleg mleg--r">R Resistant</span><span class="mleg mleg--i">I Intermediate</span><span class="mleg mleg--s">S Susceptible</span></div>
          <div class="matrix-scroll">
            <table class="matrix-table">
              <thead><tr><th class="matrix-th-isolate">Isolate</th><th v-for="ab in resistanceMatrix.antibiotics" :key="ab" class="matrix-th-drug" :title="ab"><span class="drug-label">{{ ab }}</span></th></tr></thead>
              <tbody>
                <tr v-for="row in pagedMatrixRows" :key="row.isolateId">
                  <td class="matrix-isolate-cell"><span class="matrix-isolate-id">{{ row.isolateId }}</span><span class="matrix-organism">{{ row.organism }}</span></td>
                  <td v-for="(cell, ci) in row.cells" :key="ci" class="matrix-cell" :style="{ background: matrixColor(cell) }" :title="`${row.isolateId} — ${resistanceMatrix.antibiotics[ci]}: ${cell}`">
                    <span class="matrix-cell-label" :style="{ color: matrixTextColor(cell) }">{{ matrixLabel(cell) }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="pager">
            <span class="pager-info">{{ resistanceMatrix.rows.length }} isolates · page {{ matrixPage }} of {{ totalPages(resistanceMatrix.rows.length) }}</span>
            <div class="pager-btns">
              <button class="pager-btn" :disabled="matrixPage <= 1" @click="matrixPage--"><i class="pi pi-chevron-left"></i> Prev</button>
              <button class="pager-btn" :disabled="matrixPage >= totalPages(resistanceMatrix.rows.length)" @click="matrixPage++">Next <i class="pi pi-chevron-right"></i></button>
            </div>
          </div>
        </div>
      </section>

      <section class="chart-section">
        <div class="section-header"><h2 class="section-title"><i class="pi pi-table"></i> Isolates</h2><span class="section-note">Binary_Information · linked to water samples</span></div>
        <div class="table-card">
          <table class="data-table">
            <thead><tr><th>Isolate ID</th><th>Sample</th><th>Organism</th><th>Source Context</th><th>Virulence Genes</th><th>Intl1</th><th>TEM</th><th>SHV</th></tr></thead>
            <tbody>
              <tr v-for="iso in pagedIsolates" :key="iso.isolateId">
                <td class="mono">{{ iso.isolateId }}</td>
                <td class="mono dim">{{ iso.sampleId }}</td>
                <td class="organism-cell"><em>{{ iso.organismIdentity }}</em></td>
                <td class="dim">{{ iso.sourceContext }}</td>
                <td class="mono dim">{{ iso.virulenceGenes ?? '—' }}</td>
                <td><span class="bool-badge" :class="iso.binaryTypingProfile.Intl1 ? 'bool--pos' : 'bool--neg'">{{ iso.binaryTypingProfile.Intl1 ? '✓' : '✗' }}</span></td>
                <td><span class="bool-badge" :class="iso.binaryTypingProfile.TEM  ? 'bool--pos' : 'bool--neg'">{{ iso.binaryTypingProfile.TEM  ? '✓' : '✗' }}</span></td>
                <td><span class="bool-badge" :class="iso.binaryTypingProfile.SHV  ? 'bool--pos' : 'bool--neg'">{{ iso.binaryTypingProfile.SHV  ? '✓' : '✗' }}</span></td>
              </tr>
            </tbody>
          </table>
          <div class="pager">
            <span class="pager-info">{{ detail.isolates.length }} isolates · page {{ isolatesPage }} of {{ totalPages(detail.isolates.length) }}</span>
            <div class="pager-btns">
              <button class="pager-btn" :disabled="isolatesPage <= 1" @click="isolatesPage--"><i class="pi pi-chevron-left"></i> Prev</button>
              <button class="pager-btn" :disabled="isolatesPage >= totalPages(detail.isolates.length)" @click="isolatesPage++">Next <i class="pi pi-chevron-right"></i></button>
            </div>
          </div>
        </div>
      </section>

      <section class="chart-section">
        <div class="section-header"><h2 class="section-title"><i class="pi pi-code"></i> AMR Gene Sequences</h2><span class="section-note">AMRFinderPlus · gene-level hits per isolate</span></div>
        <div class="table-card">
          <table class="data-table">
            <thead><tr><th>Isolate ID</th><th>Gene</th><th>Element Type</th><th>Class</th><th>Subclass</th><th style="text-align:right">Identity %</th><th style="text-align:right">Coverage %</th></tr></thead>
            <tbody>
              <tr v-for="(seq, i) in pagedAmr" :key="i">
                <td class="mono dim">{{ seq.isolateId }}</td>
                <td class="mono gene-name">{{ seq.geneSymbol }}</td>
                <td><span class="type-badge" :class="seq.elementType === 'AMR' ? 'type--amr' : 'type--stress'">{{ seq.elementType }}</span></td>
                <td class="dim">{{ seq.resistanceClass }}</td>
                <td class="dim">{{ seq.resistanceSubclass }}</td>
                <td style="text-align:right" class="pct-cell">{{ seq.identityPercentage.toFixed(2) }}%</td>
                <td style="text-align:right" class="pct-cell">{{ seq.coveragePercentage.toFixed(2) }}%</td>
              </tr>
            </tbody>
          </table>
          <div class="pager">
            <span class="pager-info">{{ detail.amrSequences.length }} gene hits · page {{ amrPage }} of {{ totalPages(detail.amrSequences.length) }}</span>
            <div class="pager-btns">
              <button class="pager-btn" :disabled="amrPage <= 1" @click="amrPage--"><i class="pi pi-chevron-left"></i> Prev</button>
              <button class="pager-btn" :disabled="amrPage >= totalPages(detail.amrSequences.length)" @click="amrPage++">Next <i class="pi pi-chevron-right"></i></button>
            </div>
          </div>
        </div>
      </section>

      <section class="chart-section">
        <div class="section-header"><h2 class="section-title"><i class="pi pi-server"></i> WGS Metrics</h2><span class="section-note">StarAMR · whole genome sequencing results</span></div>
        <div class="table-card">
          <table class="data-table">
            <thead><tr><th>Isolate ID</th><th>Quality</th><th>Genotype</th><th>Predicted Phenotype</th><th>SIR Profile</th><th>Plasmid</th><th style="text-align:right">Genome (bp)</th><th style="text-align:right">N50</th></tr></thead>
            <tbody>
              <tr v-for="wgs in pagedWgs" :key="wgs.isolateId">
                <td class="mono dim">{{ wgs.isolateId }}</td>
                <td><span class="quality-badge" :class="wgs.qualityStatus?.toUpperCase() === 'PASS' ? 'quality--pass' : 'quality--fail'">{{ wgs.qualityStatus }}</span></td>
                <td class="mono dim genotype-cell">{{ wgs.genotype }}</td>
                <td class="dim pheno-cell">{{ wgs.predictedPhenotype }}</td>
                <td><span class="sir-badge" :style="{ color: sirColor(wgs.predictedSirProfile), background: sirBg(wgs.predictedSirProfile) }">{{ wgs.predictedSirProfile }}</span></td>
                <td class="mono dim">{{ wgs.plasmid }}</td>
                <td style="text-align:right" class="num-cell">{{ wgs.genomeLength?.toLocaleString() }}</td>
                <td style="text-align:right" class="num-cell">{{ wgs.n50Value?.toLocaleString() }}</td>
              </tr>
            </tbody>
          </table>
          <div class="pager">
            <span class="pager-info">{{ detail.wgsMetrics.length }} records · page {{ wgsPage }} of {{ totalPages(detail.wgsMetrics.length) }}</span>
            <div class="pager-btns">
              <button class="pager-btn" :disabled="wgsPage <= 1" @click="wgsPage--"><i class="pi pi-chevron-left"></i> Prev</button>
              <button class="pager-btn" :disabled="wgsPage >= totalPages(detail.wgsMetrics.length)" @click="wgsPage++">Next <i class="pi pi-chevron-right"></i></button>
            </div>
          </div>
        </div>
      </section>

      <section class="chart-section">
        <div class="section-header"><h2 class="section-title"><i class="pi pi-list"></i> Sampling History</h2><span class="section-note">Epicollect · physicochemical readings</span></div>
        <div class="table-card">
          <table class="data-table">
            <thead><tr><th>Sample ID</th><th>Trip</th><th>Date</th><th style="text-align:right">Temp (°C)</th><th style="text-align:right">pH</th><th style="text-align:right">TDS (mg/L)</th><th style="text-align:right">EC (μS/cm)</th><th style="text-align:right">DO (mg/L)</th></tr></thead>
            <tbody>
              <tr v-for="s in pagedSamples" :key="s.sampleId">
                <td class="mono">{{ s.sampleId }}</td>
                <td class="dim">{{ s.tripIdentifier }}</td>
                <td class="dim">{{ s.collectionDate }}</td>
                <td style="text-align:right">{{ s.waterTemperature ?? '—' }}</td>
                <td style="text-align:right">{{ s.phLevel ?? '—' }}</td>
                <td style="text-align:right">{{ s.tds ?? '—' }}</td>
                <td style="text-align:right">{{ s.ec ?? '—' }}</td>
                <td style="text-align:right">{{ s.dissolvedOxygen ?? '—' }}</td>
              </tr>
            </tbody>
          </table>
          <div class="pager">
            <span class="pager-info">{{ detail.waterSamples.length }} samples · page {{ samplesPage }} of {{ totalPages(detail.waterSamples.length) }}</span>
            <div class="pager-btns">
              <button class="pager-btn" :disabled="samplesPage <= 1" @click="samplesPage--"><i class="pi pi-chevron-left"></i> Prev</button>
              <button class="pager-btn" :disabled="samplesPage >= totalPages(detail.waterSamples.length)" @click="samplesPage++">Next <i class="pi pi-chevron-right"></i></button>
            </div>
          </div>
        </div>
      </section>

    </template>

    <div v-else class="error-state">
      <i class="pi pi-exclamation-triangle"></i>
      <p>Upload data to view metrics on this page.</p>
    </div>

  </div>
</template>

<style scoped>
.river-detail { padding: 0 28px 40px; display: flex; flex-direction: column; gap: 18px; }
.pager { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 10px 14px; border-top: 1px solid var(--c-border); background: var(--c-bg); flex-wrap: wrap; }
.pager-info { font-size: 11px; color: var(--c-text-muted); }
.pager-btns { display: flex; gap: 6px; }
.pager-btn { display: inline-flex; align-items: center; gap: 4px; padding: 4px 10px; font-size: 11.5px; font-weight: 600; background: var(--c-card); color: var(--c-text); border: 1px solid var(--c-border); border-radius: 5px; cursor: pointer; font-family: 'DM Sans', sans-serif; transition: background 0.12s, border-color 0.12s; }
.pager-btn:hover:not(:disabled) { background: var(--c-brand-dim); border-color: var(--c-brand); color: var(--c-brand); }
.pager-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.pager-btn .pi { font-size: 10px; }
.page-header { margin-bottom: 20px; }
.breadcrumb-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; flex-wrap: wrap; }
.breadcrumb { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--c-text-muted); }
.crumb-link { cursor: pointer; color: var(--c-brand); }
.crumb-link:hover { text-decoration: underline; }
.crumb-current { color: var(--c-text-muted); }
.crumb-sep { font-size: 9px; }
.site-hero-card { background: var(--c-card); border: 1px solid var(--c-border); border-top: 3px solid var(--c-brand); border-radius: 10px; padding: 20px 24px 18px; box-shadow: var(--c-shadow); position: relative; }
.hero-top-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 6px; }
.hero-site-label { font-size: 10.5px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.1em; color: var(--c-brand); display: flex; align-items: center; gap: 5px; }
.hero-name-btn { display: flex; align-items: center; gap: 12px; background: none; border: none; padding: 0; cursor: pointer; text-align: left; width: 100%; margin-bottom: 14px; }
.hero-name-btn:hover .page-title { color: var(--c-brand); }
.hero-name-btn:hover .hero-chevron { color: var(--c-brand); }
.page-title { font-family: 'Inter', sans-serif; font-size: 30px; font-weight: 700; color: var(--c-heading); margin: 0; line-height: 1.1; transition: color 0.15s; }
.hero-chevron { font-size: 14px; color: var(--c-text-muted); transition: transform 0.2s, color 0.15s; flex-shrink: 0; }
.hero-chevron--open { transform: rotate(180deg); }
.hero-dropdown { background: var(--c-bg); border: 1px solid var(--c-border); border-radius: 8px; margin-bottom: 14px; overflow: hidden; }
.selector-search { display: flex; align-items: center; gap: 8px; padding: 9px 12px; border-bottom: 1px solid var(--c-border); }
.search-icon { font-size: 12px; color: var(--c-text-muted); flex-shrink: 0; }
.search-input { flex: 1; background: transparent; border: none; outline: none; color: var(--c-text); font-family: 'DM Sans', sans-serif; font-size: 13px; }
.search-input::placeholder { color: var(--c-text-muted); }
.selector-list { max-height: 200px; overflow-y: auto; padding: 4px; }
.selector-option { display: flex; align-items: center; justify-content: space-between; width: 100%; padding: 9px 10px; border-radius: 6px; border: none; background: transparent; cursor: pointer; text-align: left; transition: background 0.12s; gap: 8px; }
.selector-option:hover { background: var(--c-brand-dim); }
.selector-option--active { background: var(--c-brand-dim); }
.option-main { display: flex; flex-direction: column; gap: 2px; }
.option-name { font-size: 13px; font-weight: 500; color: var(--c-text); }
.option-river { font-size: 10.5px; color: var(--c-text-muted); }
.option-id { font-family: monospace; font-size: 10.5px; color: var(--c-brand); background: var(--c-brand-dim); padding: 1px 6px; border-radius: 4px; flex-shrink: 0; }
.selector-empty { padding: 16px; text-align: center; font-size: 12px; color: var(--c-text-muted); }
.compare-btn { display: flex; align-items: center; gap: 6px; padding: 6px 14px; border-radius: 6px; border: 1px solid var(--c-brand); background: var(--c-brand-dim); color: var(--c-brand); font-family: 'DM Sans', sans-serif; font-size: 12.5px; font-weight: 600; cursor: pointer; transition: background 0.15s; white-space: nowrap; }
.compare-btn:hover { background: var(--c-brand); color: #fff; }
.compare-btn .pi { font-size: 12px; }
.meta-pills { display: flex; flex-wrap: wrap; gap: 7px; }
.pill { display: flex; align-items: center; gap: 5px; background: var(--c-bg); border: 1px solid var(--c-border); border-radius: 20px; padding: 3px 10px; font-size: 11.5px; color: var(--c-text-muted); }
.pill .pi { font-size: 10px; }
.safety-banner { display: flex; align-items: flex-start; gap: 16px; border: 1.5px solid; border-radius: 10px; padding: 16px 20px; margin-bottom: 22px; }
.safety-icon-wrap { flex-shrink: 0; padding-top: 2px; }
.safety-icon { font-size: 24px; }
.safety-body { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.safety-header-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 2px; }
.safety-level { font-family: 'Inter', sans-serif; font-size: 13px; font-weight: 700; letter-spacing: 0.04em; }
.safety-tag { font-size: 10px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.08em; color: var(--c-text-muted); background: var(--c-card); border: 1px solid var(--c-border); border-radius: 4px; padding: 1px 7px; }
.safety-headline { font-size: 14px; font-weight: 600; color: var(--c-heading); margin: 0; }
.safety-detail { font-size: 12.5px; color: var(--c-text-muted); margin: 0; line-height: 1.55; }
.stat-grid { display: grid; grid-template-columns: repeat(auto-fit,minmax(180px,1fr)); gap: 14px; margin-bottom: 24px; }
.stat-card { display: flex; align-items: center; gap: 14px; background: var(--c-card); border: 1px solid var(--c-border); border-radius: 8px; padding: 16px 18px; box-shadow: var(--c-shadow); }
.stat-icon { width: 40px; height: 40px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 16px; flex-shrink: 0; }
.stat-body { display: flex; flex-direction: column; gap: 2px; }
.stat-label { font-size: 10px; color: var(--c-text-muted); text-transform: uppercase; letter-spacing: 0.07em; }
.stat-value { font-size: 24px; font-weight: 700; color: var(--c-heading); line-height: 1.1; font-family: 'Inter', sans-serif; }
.stat-denom { font-size: 14px; color: var(--c-text-muted); font-family: 'DM Sans', sans-serif; font-weight: 400; }
.chart-section { margin-bottom: 22px; }
.section-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; margin-bottom: 10px; }
.section-title { font-size: 12px; font-weight: 600; color: var(--c-heading); text-transform: uppercase; letter-spacing: 0.08em; display: flex; align-items: center; gap: 7px; }
.section-title .pi { color: var(--c-brand); }
.section-note { font-size: 11px; color: var(--c-text-dim); }
.chart-card { background: var(--c-card); border: 1px solid var(--c-border); border-radius: 8px; padding: 16px; box-shadow: var(--c-shadow); }
.param-tabs { display: flex; flex-wrap: wrap; gap: 6px; }
.param-tab { padding: 4px 12px; border-radius: 20px; border: 1.5px solid var(--c-border); background: transparent; color: var(--c-text-muted); font-size: 11px; cursor: pointer; transition: all 0.15s; font-family: 'DM Sans', sans-serif; }
.param-tab:hover { background: var(--c-card); color: var(--c-text); }
.param-tab--active { font-weight: 600; }
.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 22px; margin-bottom: 22px; }
@media (max-width:900px) { .two-col { grid-template-columns: 1fr; } }
.binary-profile { display: flex; flex-direction: column; gap: 12px; padding: 18px; }
.marker-row { display: grid; grid-template-columns: 48px 1fr 48px; align-items: center; gap: 10px; }
.marker-name { font-family: monospace; font-size: 12px; font-weight: 600; color: var(--c-text); }
.marker-dots { display: flex; gap: 6px; }
.marker-dot { width: 14px; height: 14px; border-radius: 50%; flex-shrink: 0; }
.dot--pos { background: var(--c-red); }
.dot--neg { background: var(--c-border); }
.marker-count { font-size: 11px; text-align: right; font-weight: 600; }
.count--pos { color: var(--c-red); }
.count--neg { color: var(--c-text-dim); }
.matrix-card { padding: 14px 16px; }
.matrix-legend { display: flex; gap: 16px; margin-bottom: 14px; }
.mleg { display: flex; align-items: center; gap: 5px; font-size: 11px; font-weight: 600; }
.mleg::before { content: ''; width: 12px; height: 12px; border-radius: 3px; display: inline-block; }
.mleg--r { color: var(--c-red); } .mleg--r::before { background: var(--c-red-dim); border: 1.5px solid var(--c-red); }
.mleg--i { color: var(--c-amber); } .mleg--i::before { background: var(--c-amber-dim); border: 1.5px solid var(--c-amber); }
.mleg--s { color: var(--c-green); } .mleg--s::before { background: var(--c-green-dim); border: 1.5px solid var(--c-green); }
.matrix-scroll { overflow-x: auto; }
.matrix-table { border-collapse: collapse; width: 100%; }
.matrix-th-isolate { text-align: left; padding: 8px 12px 8px 0; font-size: 10px; font-weight: 600; letter-spacing: 0.08em; text-transform: uppercase; color: var(--c-text-muted); white-space: nowrap; min-width: 160px; border-bottom: 1px solid var(--c-border); }
.matrix-th-drug { padding: 4px 3px 8px; border-bottom: 1px solid var(--c-border); min-width: 38px; }
.drug-label { display: block; writing-mode: vertical-rl; transform: rotate(180deg); font-size: 10px; font-weight: 600; color: var(--c-text-muted); letter-spacing: 0.04em; white-space: nowrap; max-height: 90px; overflow: hidden; }
.matrix-isolate-cell { padding: 6px 12px 6px 0; border-bottom: 1px solid var(--c-border); vertical-align: middle; }
.matrix-isolate-id { display: block; font-family: monospace; font-size: 11px; color: var(--c-brand); }
.matrix-organism { display: block; font-size: 10px; color: var(--c-text-muted); font-style: italic; }
.matrix-cell { padding: 6px 3px; text-align: center; border-bottom: 1px solid var(--c-border); border-left: 1px solid var(--c-border); transition: filter 0.12s; cursor: default; }
.matrix-cell:hover { filter: brightness(1.15); }
.matrix-cell-label { font-size: 11px; font-weight: 700; }
.table-card { background: var(--c-card); border: 1px solid var(--c-border); border-radius: 8px; overflow: auto; box-shadow: var(--c-shadow); }
.data-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.data-table thead th { background: var(--c-card); color: var(--c-text-muted); font-size: 10px; font-weight: 600; letter-spacing: 0.09em; text-transform: uppercase; padding: 10px 14px; border-bottom: 1px solid var(--c-border); text-align: left; white-space: nowrap; }
.data-table tbody tr { border-bottom: 1px solid var(--c-border); transition: background 0.12s; }
.data-table tbody tr:last-child { border-bottom: none; }
.data-table tbody tr:hover { background: var(--c-brand-dim); }
.data-table tbody td { padding: 9px 14px; color: var(--c-text); }
.mono { font-family: monospace; font-size: 11px; }
.dim { color: var(--c-text-muted); }
.organism-cell { font-size: 12px; color: var(--c-text); }
.gene-name { color: var(--c-brand); font-size: 11.5px; }
.genotype-cell { font-size: 10.5px; max-width: 160px; }
.pheno-cell { max-width: 180px; font-size: 11px; }
.pct-cell { font-variant-numeric: tabular-nums; }
.num-cell { font-variant-numeric: tabular-nums; color: var(--c-text-muted); }
.bool-badge { display: inline-flex; align-items: center; justify-content: center; width: 20px; height: 20px; border-radius: 4px; font-size: 11px; font-weight: 700; }
.bool--pos { background: var(--c-red-dim); color: var(--c-red); }
.bool--neg { background: var(--c-border); color: var(--c-text-dim); }
.type-badge { display: inline-block; padding: 2px 7px; border-radius: 4px; font-size: 10px; font-weight: 700; letter-spacing: 0.05em; }
.type--amr    { background: var(--c-red-dim); color: var(--c-red); }
.type--stress { background: var(--c-amber-dim); color: var(--c-amber); }
.quality-badge { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 10.5px; font-weight: 600; }
.quality--pass { background: var(--c-green-dim); color: var(--c-green); }
.quality--fail { background: var(--c-red-dim); color: var(--c-red); }
.sir-badge { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 10.5px; font-weight: 600; }
.skeleton-wrap { display: flex; flex-direction: column; gap: 18px; }
.skel-row { display: grid; grid-template-columns: repeat(4,1fr); gap: 14px; }
.skel { background: var(--c-card); border-radius: 8px; animation: pulse 1.4s infinite; width: 100%; }
@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:.5} }
.error-state { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 40vh; gap: 12px; color: var(--c-text-muted); }
.error-state .pi { font-size: 40px; color: var(--c-red); }
</style>