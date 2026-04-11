<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart, BarChart } from 'echarts/charts'
import {
  TooltipComponent,
  GridComponent,
  LegendComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { useThemeStore } from '@/stores/theme'

use([LineChart, BarChart, TooltipComponent, GridComponent, LegendComponent, CanvasRenderer])

// ── Click-outside directive ────────────────────────────────────────
const vClickOutside = {
  mounted(el: HTMLElement & { _clickOutside?: (e: Event) => void }, binding: { value: () => void }) {
    el._clickOutside = (e: Event) => {
      if (!el.contains(e.target as Node)) binding.value()
    }
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

// All available sites for the selector
// TODO: replace with GET /api/sites when backend is ready
const ALL_SITES = [
  { siteId: 'A10', locationName: 'Pretoria',     riverName: 'Apies River' },
  { siteId: 'B26', locationName: 'Hammanskraal', riverName: 'Apies River' },
  { siteId: 'B27', locationName: 'Tshwane',      riverName: 'Apies River' },
]

const filteredSites = computed(() =>
  ALL_SITES.filter(s =>
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

function closeDropdown() {
  dropdownOpen.value = false
  searchQuery.value  = ''
}

// ── Types ──────────────────────────────────────────────────────────
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

interface BinaryTypingProfile {
  Intl1: boolean
  Intl2: boolean
  Intl3: boolean
  TEM: boolean
  SHV: boolean
}

interface Isolate {
  isolateId: string
  sampleId: string
  isolateNumber: string
  organismIdentity: string
  sourceContext: string
  arCode: string
  virulenceGenes: string | null
  binaryTypingProfile: BinaryTypingProfile
}

interface AmrSequence {
  isolateId: string
  geneSymbol: string
  sequenceName: string
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

interface SiteDetail {
  site: Site
  waterSamples: WaterSample[]
  isolates: Isolate[]
  amrSequences: AmrSequence[]
  wgsMetrics: WgsMetrics[]
}

// ── Mock data ──────────────────────────────────────────────────────
const SITE_DATA: Record<string, SiteDetail | undefined> = {
  A10: {
    site: { siteId: 'A10', locationName: 'Pretoria', riverName: 'Apies River', latitude: -25.747, longitude: 28.229 },
    waterSamples: [
      { sampleId: 'SAMP-001', siteId: 'A10', tripIdentifier: 'Trip 1', collectionDate: '2025-05-10', waterTemperature: 18.5, phLevel: 7.2, tds: 250, ec: 400, dissolvedOxygen: 6.5 },
      { sampleId: 'SAMP-004', siteId: 'A10', tripIdentifier: 'Trip 2', collectionDate: '2025-07-15', waterTemperature: 15.3, phLevel: 6.9, tds: 280, ec: 450, dissolvedOxygen: 5.8 },
    ],
    isolates: [
      { isolateId: 'ISO-101', sampleId: 'SAMP-001', isolateNumber: 'TSp1H', organismIdentity: 'Klebsiella pneumoniae', sourceContext: 'Spinach at harvest', arCode: 'B1', virulenceGenes: 'rmpA, iutA', binaryTypingProfile: { Intl1: true, Intl2: false, Intl3: true, TEM: true, SHV: true } },
    ],
    amrSequences: [
      { isolateId: 'ISO-101', geneSymbol: 'bla',  sequenceName: 'subclass B3 metallo-beta-lactamase', elementType: 'AMR',    resistanceClass: 'BETA-LACTAM', subclass: 'BETA-LACTAM', identityPercentage: 81.36, coveragePercentage: 58.33 },
      { isolateId: 'ISO-101', geneSymbol: 'erm',  sequenceName: '23S rRNA methyltransferase Erm',    elementType: 'AMR',    resistanceClass: 'MACROLIDE',   subclass: 'MACROLIDE',   identityPercentage: 87.54, coveragePercentage: 39.76 },
    ],
    wgsMetrics: [
      { isolateId: 'ISO-101', qualityStatus: 'Passed', genotype: "aph(3')-Ia, blaCTX-M-14", predictedPhenotype: 'kanamycin, ampicillin, ceftriaxone', sirProfile: 'Resistant', plasmid: 'IncFIB(K)', genomeLength: 5017831, n50Value: 156657 },
    ],
  },
  B26: {
    site: { siteId: 'B26', locationName: 'Hammanskraal', riverName: 'Apies River', latitude: -25.750, longitude: 28.230 },
    waterSamples: [
      { sampleId: 'SAMP-002', siteId: 'B26', tripIdentifier: 'Trip 1', collectionDate: '2025-05-10', waterTemperature: 19.1, phLevel: 7.4, tds: 260, ec: 410, dissolvedOxygen: 6.2 },
    ],
    isolates: [
      { isolateId: 'ISO-102', sampleId: 'SAMP-002', isolateNumber: 'Pi1', organismIdentity: 'Serratia fonticola', sourceContext: 'Irrigation pivot point', arCode: 'B26', virulenceGenes: null, binaryTypingProfile: { Intl1: false, Intl2: false, Intl3: true, TEM: false, SHV: false } },
    ],
    amrSequences: [
      { isolateId: 'ISO-102', geneSymbol: 'aac(3)-I', sequenceName: 'AAC(3)-I aminoglycoside', elementType: 'AMR', resistanceClass: 'AMINOGLYCOSIDE', subclass: 'GENTAMICIN', identityPercentage: 98.68, coveragePercentage: 62.00 },
    ],
    wgsMetrics: [
      { isolateId: 'ISO-102', qualityStatus: 'Failed', genotype: 'tet(A)', predictedPhenotype: 'tetracycline', sirProfile: 'Intermediate', plasmid: 'Col(BS512)', genomeLength: 6133820, n50Value: 1660 },
    ],
  },
  B27: {
    site: { siteId: 'B27', locationName: 'Tshwane', riverName: 'Apies River', latitude: -25.752, longitude: 28.231 },
    waterSamples: [
      { sampleId: 'SAMP-003', siteId: 'B27', tripIdentifier: 'Trip 2', collectionDate: '2025-07-15', waterTemperature: 15.3, phLevel: 6.9, tds: 280, ec: 450, dissolvedOxygen: 5.8 },
    ],
    isolates: [
      { isolateId: 'ISO-103', sampleId: 'SAMP-003', isolateNumber: 'Pi2', organismIdentity: 'Escherichia coli', sourceContext: 'Irrigation pivot point', arCode: 'B27', virulenceGenes: 'eae, bfpA', binaryTypingProfile: { Intl1: true, Intl2: true, Intl3: false, TEM: true, SHV: false } },
    ],
    amrSequences: [
      { isolateId: 'ISO-103', geneSymbol: 'arsN1', sequenceName: 'arsinothricin N-acetyltransferase', elementType: 'STRESS', resistanceClass: 'METAL', subclass: 'ARSENIC', identityPercentage: 90.86, coveragePercentage: 51.48 },
    ],
    wgsMetrics: [
      { isolateId: 'ISO-103', qualityStatus: 'Passed', genotype: 'blaTEM-1B, sul2', predictedPhenotype: 'ampicillin, sulfisoxazole', sirProfile: 'Susceptible', plasmid: 'IncX1', genomeLength: 5025249, n50Value: 125507 },
    ],
  },
}

const detail   = ref<SiteDetail | null>(null)
const selParam = ref<keyof WaterSample>('phLevel')

function loadSite(id: string) {
  loading.value = true
  // TODO: replace with real API calls:
  // GET /api/sites/{siteId}
  // GET /api/sites/{siteId}/water-samples
  // GET /api/sites/{siteId}/isolates
  // GET /api/sites/{siteId}/amr-sequences
  // GET /api/sites/{siteId}/wgs-metrics
  setTimeout(() => {
    detail.value = SITE_DATA[id] ?? SITE_DATA['A10'] ?? null
    loading.value = false
  }, 450)
}

onMounted(() => loadSite(siteId.value))

watch(siteId, (newId) => {
  selectedSite.value = newId
  loadSite(newId)
})

// ── Navigate to comparison page pre-loaded with this site ──────────
function goToCompare() {
  router.push({ path: '/compare', query: { siteA: siteId.value } })
}

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

// Water quality time-series
const waterChartOption = computed(() => {
  if (!detail.value) return {}
  const meta: ParamMeta | undefined = PARAM_META[selParam.value as string]
  if (!meta) return {}
  const samples = detail.value.waterSamples
  const dates   = samples.map(s => `${s.tripIdentifier} · ${s.collectionDate}`)
  const values  = samples.map(s => s[selParam.value] as number | null)
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', formatter: (p: any) => `${p[0].axisValue}<br/>${meta.label}: <b>${p[0].value ?? '—'} ${meta.unit}</b>` },
    grid: { left: 52, right: 20, top: 16, bottom: 40 },
    xAxis: { type: 'category', data: dates, axisLabel: { ...axLabel.value, rotate: 15 }, axisLine: axLine.value },
    yAxis: { type: 'value', axisLabel: { ...axLabel.value, formatter: (v: number) => `${v}${meta.unit}` }, splitLine: splitLine.value, axisLine: { show: false } },
    series: [{
      type: 'line', data: values, smooth: true, symbol: 'circle', symbolSize: 8,
      lineStyle: { color: meta.color, width: 2.5 },
      itemStyle: { color: meta.color, borderColor: isDark.value ? '#101D2E' : '#fff', borderWidth: 2 },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: meta.color + '44' }, { offset: 1, color: meta.color + '00' }] } },
    }],
  }
})

// AMR resistance class bar chart
const amrBarOption = computed(() => {
  if (!detail.value) return {}
  const classMap = new Map<string, number>()
  for (const seq of detail.value.amrSequences) {
    classMap.set(seq.resistanceClass, (classMap.get(seq.resistanceClass) ?? 0) + 1)
  }
  const entries = [...classMap.entries()].sort((a, b) => a[1] - b[1])
  const CLASS_COLORS: Record<string, string> = {
    'BETA-LACTAM': '#EF4444', 'MACROLIDE': '#F59E0B',
    'AMINOGLYCOSIDE': '#8B5CF6', 'METAL': '#6B7280',
    'SULFONAMIDE': '#3B82F6', 'TETRACYCLINE': '#FB923C',
  }
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis' },
    grid: { left: 130, right: 30, top: 10, bottom: 20 },
    xAxis: { type: 'value', axisLabel: axLabel.value, splitLine: splitLine.value },
    yAxis: { type: 'category', data: entries.map(e => e[0]), axisLabel: { ...axLabel.value, fontFamily: 'monospace', fontSize: 10.5 }, axisLine: axLine.value },
    series: [{ type: 'bar', barMaxWidth: 18, data: entries.map(e => ({ value: e[1], itemStyle: { color: CLASS_COLORS[e[0]] ?? '#3B82F6', borderRadius: [0, 3, 3, 0] } })) }],
  }
})

// Binary typing profile
const binaryProfileSummary = computed(() => {
  if (!detail.value) return []
  const keys: (keyof BinaryTypingProfile)[] = ['Intl1', 'Intl2', 'Intl3', 'TEM', 'SHV']
  return keys.map(k => ({
    marker: k,
    positive: detail.value!.isolates.filter(i => i.binaryTypingProfile[k]).length,
    total: detail.value!.isolates.length,
  }))
})

// ── Resistance matrix ──────────────────────────────────────────────
// Built from wgsMetrics.predictedPhenotype (comma-separated antibiotic names)
// and wgsMetrics.sirProfile, cross-referenced with isolate IDs.
// Columns = antibiotics found across all isolates at this site
// Rows    = each isolate
// Cell    = R (red) / I (amber) / S (green) derived from sirProfile + phenotype membership
const resistanceMatrix = computed(() => {
  if (!detail.value) return { antibiotics: [], rows: [] }

  // Collect all unique antibiotic names mentioned in predictedPhenotype fields
  const antibioticSet = new Set<string>()
  for (const wgs of detail.value.wgsMetrics) {
    if (wgs.predictedPhenotype) {
      wgs.predictedPhenotype.split(',').map(a => a.trim()).filter(Boolean).forEach(a => antibioticSet.add(a))
    }
  }
  const antibiotics = [...antibioticSet].sort()

  // Build one row per isolate
  const rows = detail.value.isolates.map(iso => {
    const wgs = detail.value!.wgsMetrics.find(w => w.isolateId === iso.isolateId)
    const phenotypeList = wgs?.predictedPhenotype
      ? wgs.predictedPhenotype.split(',').map(a => a.trim())
      : []

    const cells = antibiotics.map(ab => {
      if (!wgs) return 'NA'
      // If this antibiotic appears in the predicted phenotype, use the SIR profile
      // otherwise the isolate is considered susceptible to this drug
      if (phenotypeList.includes(ab)) return wgs.sirProfile  // 'Resistant' | 'Intermediate' | 'Susceptible'
      return 'Susceptible'
    })

    return { isolateId: iso.isolateId, organism: iso.organismIdentity, cells }
  })

  return { antibiotics, rows }
})

// WGS quality counts
const wgsPassCount  = computed(() => detail.value?.wgsMetrics.filter(w => w.qualityStatus === 'Passed').length ?? 0)
const wgsTotalCount = computed(() => detail.value?.wgsMetrics.length ?? 0)

// Unique organism list
const organisms = computed(() => {
  if (!detail.value) return []
  const map = new Map<string, number>()
  for (const iso of detail.value.isolates) map.set(iso.organismIdentity, (map.get(iso.organismIdentity) ?? 0) + 1)
  return [...map.entries()].sort((a, b) => b[1] - a[1])
})

function sirColor(sir: string): string {
  if (sir === 'Resistant')    return isDark.value ? '#EF4444' : '#C62828'
  if (sir === 'Intermediate') return isDark.value ? '#FBBF24' : '#D97706'
  return isDark.value ? '#34D399' : '#059669'
}
function sirBg(sir: string): string {
  if (sir === 'Resistant')    return 'var(--c-red-dim)'
  if (sir === 'Intermediate') return 'var(--c-amber-dim)'
  return 'var(--c-green-dim)'
}
function matrixColor(status: string): string {
  if (status === 'Resistant')    return isDark.value ? '#EF444422' : '#FEE2E2'
  if (status === 'Intermediate') return isDark.value ? '#FBBF2422' : '#FEF9C3'
  if (status === 'Susceptible')  return isDark.value ? '#34D39922' : '#D1FAE5'
  return isDark.value ? 'rgba(255,255,255,0.04)' : '#F9FAFB'
}
function matrixTextColor(status: string): string {
  if (status === 'Resistant')    return isDark.value ? '#F87171' : '#DC2626'
  if (status === 'Intermediate') return isDark.value ? '#FBBF24' : '#D97706'
  if (status === 'Susceptible')  return isDark.value ? '#34D399' : '#059669'
  return isDark.value ? '#374D61' : '#9CA3AF'
}
function matrixLabel(status: string): string {
  if (status === 'Resistant')    return 'R'
  if (status === 'Intermediate') return 'I'
  if (status === 'Susceptible')  return 'S'
  return '—'
}

// ── Public safety rating ───────────────────────────────────────────
// Derived entirely from available data — no extra endpoint needed.
// Logic:
//   CAUTION  → any isolate has sirProfile === 'Resistant' (MDR bacteria present in water)
//   ADVISORY → any isolate has sirProfile === 'Intermediate' (reduced susceptibility detected)
//   LOW RISK → all isolates susceptible, or no WGS data yet
//
// We also factor in DO (dissolved oxygen) as a basic water health signal:
//   DO < 4 mg/L is considered hypoxic and unsafe for contact.
const safetyRating = computed(() => {
  if (!detail.value) return null

  const wgsResults = detail.value.wgsMetrics
  const hasResistant    = wgsResults.some(w => w.sirProfile === 'Resistant')
  const hasIntermediate = wgsResults.some(w => w.sirProfile === 'Intermediate')

  // Check latest dissolved oxygen reading
  const doValues = detail.value.waterSamples
    .map(s => s.dissolvedOxygen)
    .filter((v): v is number => v !== null)
  const latestDo = doValues.length ? doValues[doValues.length - 1] : null
  const lowOxygen = latestDo != null && latestDo < 4

  // Count distinct resistant antibiotic classes as a severity signal
  const resistantClasses = new Set(
    detail.value.amrSequences
      .filter(s => s.elementType === 'AMR')
      .map(s => s.resistanceClass)
  )
  const multidrugResistant = resistantClasses.size >= 2

  if (hasResistant && multidrugResistant) {
    return {
      level: 'HIGH RISK',
      icon: 'pi-ban',
      color: 'var(--c-red)',
      bg: 'var(--c-red-dim)',
      border: 'var(--c-red)',
      headline: 'Not recommended for recreational water contact.',
      detail: `Multidrug-resistant bacteria have been detected at this site (${resistantClasses.size} resistance classes). Avoid swimming, wading, or contact with this water. Boil or treat water before use.`,
    }
  }
  if (hasResistant || lowOxygen) {
    return {
      level: 'CAUTION',
      icon: 'pi-exclamation-triangle',
      color: 'var(--c-amber)',
      bg: 'var(--c-amber-dim)',
      border: 'var(--c-amber)',
      headline: 'Exercise caution near this water.',
      detail: lowOxygen
        ? `Low dissolved oxygen (${latestDo} mg/L) detected — water quality is reduced. ${hasResistant ? 'Antibiotic-resistant bacteria have also been found at this site.' : ''} Avoid prolonged contact with the water.`
        : 'Antibiotic-resistant bacteria have been detected at this site. Avoid swallowing water. Wash hands after contact. Not suitable for drinking without treatment.',
    }
  }
  if (hasIntermediate) {
    return {
      level: 'LOW ADVISORY',
      icon: 'pi-info-circle',
      color: 'var(--c-brand)',
      bg: 'var(--c-brand-dim)',
      border: 'var(--c-brand)',
      headline: 'Generally safe, with minor advisory.',
      detail: 'Bacteria with reduced antibiotic susceptibility have been detected but no confirmed resistant strains. Standard precautions apply — avoid swallowing water and wash hands after contact.',
    }
  }
  return {
    level: 'LOW RISK',
    icon: 'pi-check-circle',
    color: 'var(--c-green)',
    bg: 'var(--c-green-dim)',
    border: 'var(--c-green)',
    headline: 'No significant AMR threat detected at this site.',
    detail: 'Current data shows no antibiotic-resistant bacteria at this location. Standard hygiene practices are still recommended when near river water.',
  }
})
</script>

<template>
  <div class="river-detail">

    <!-- Skeleton -->
    <div v-if="loading" class="skeleton-wrap">
      <div class="skel" style="height:72px"></div>
      <div class="skel-row"><div class="skel" style="height:72px" v-for="n in 4" :key="n"></div></div>
      <div class="skel" style="height:280px"></div>
      <div class="skel-row"><div class="skel" style="height:240px"></div><div class="skel" style="height:240px"></div></div>
      <div class="skel" style="height:220px"></div>
    </div>

    <template v-else-if="detail">

      <!-- ── Page header hero card ── -->
      <header class="page-header">

        <!-- Breadcrumb row -->
        <div class="breadcrumb-row">
          <div class="breadcrumb">
            <span class="crumb-link" @click="$router.push('/map')">Map</span>
            <i class="pi pi-chevron-right crumb-sep"></i>
            <span class="crumb-current">River Detail</span>
          </div>
        </div>

        <!-- Hero card: site name IS the heading, selector embedded inside -->
        <div class="site-hero-card" v-click-outside="closeDropdown">

          <!-- Top row: site label + compare button -->
          <div class="hero-top-row">
            <span class="hero-site-label">
              <i class="pi pi-map-marker"></i> Site {{ detail.site.siteId }}
            </span>
            <button class="compare-btn" @click="goToCompare">
              <i class="pi pi-sliders-h"></i>
              Compare sites
            </button>
          </div>

          <!-- Site name as heading + chevron to open selector -->
          <button class="hero-name-btn" @click="dropdownOpen = !dropdownOpen">
            <h1 class="page-title">{{ detail.site.locationName }}</h1>
            <i class="pi pi-chevron-down hero-chevron" :class="{ 'hero-chevron--open': dropdownOpen }"></i>
          </button>

          <!-- Inline dropdown -->
          <div v-if="dropdownOpen" class="hero-dropdown">
            <div class="selector-search">
              <i class="pi pi-search search-icon"></i>
              <input
                v-model="searchQuery"
                class="search-input"
                placeholder="Search by name, river or ID…"
                autofocus
              />
            </div>
            <div class="selector-list">
              <button
                v-for="s in filteredSites"
                :key="s.siteId"
                class="selector-option"
                :class="{ 'selector-option--active': s.siteId === selectedSite }"
                @click="selectSite(s.siteId)"
              >
                <div class="option-main">
                  <span class="option-name">{{ s.locationName }}</span>
                  <span class="option-river">{{ s.riverName }}</span>
                </div>
                <span class="option-id">{{ s.siteId }}</span>
              </button>
              <div v-if="filteredSites.length === 0" class="selector-empty">
                No sites match "{{ searchQuery }}"
              </div>
            </div>
          </div>

          <!-- Meta pills inside the card -->
          <div class="meta-pills">
            <span class="pill"><i class="pi pi-waves"></i> {{ detail.site.riverName }}</span>
            <span class="pill"><i class="pi pi-compass"></i> {{ detail.site.latitude }}, {{ detail.site.longitude }}</span>
            <span class="pill"><i class="pi pi-database"></i> {{ detail.waterSamples.length }} samples</span>
            <span class="pill"><i class="pi pi-microscope"></i> {{ detail.isolates.length }} isolates</span>
          </div>

        </div>
      </header>

      <!-- ── Public safety banner ── -->
      <div v-if="safetyRating" class="safety-banner"
        :style="{ background: safetyRating.bg, borderColor: safetyRating.border }">
        <div class="safety-icon-wrap" :style="{ color: safetyRating.color }">
          <i :class="`pi ${safetyRating.icon} safety-icon`"></i>
        </div>
        <div class="safety-body">
          <div class="safety-header-row">
            <span class="safety-level" :style="{ color: safetyRating.color }">{{ safetyRating.level }}</span>
            <span class="safety-tag">Public Safety Assessment</span>
          </div>
          <p class="safety-headline">{{ safetyRating.headline }}</p>
          <p class="safety-detail">{{ safetyRating.detail }}</p>
        </div>
      </div>

      <!-- ── Stat cards ── -->
      <div class="stat-grid">
        <div class="stat-card">
          <div class="stat-icon" style="background:var(--c-brand-dim);color:var(--c-brand)"><i class="pi pi-calendar"></i></div>
          <div class="stat-body">
            <span class="stat-label">Sampling Trips</span>
            <span class="stat-value">{{ new Set(detail.waterSamples.map(s => s.tripIdentifier)).size }}</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background:var(--c-red-dim);color:var(--c-red)"><i class="pi pi-shield"></i></div>
          <div class="stat-body">
            <span class="stat-label">AMR Gene Hits</span>
            <span class="stat-value">{{ detail.amrSequences.length }}</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background:var(--c-green-dim);color:var(--c-green)"><i class="pi pi-check-circle"></i></div>
          <div class="stat-body">
            <span class="stat-label">WGS Passed QC</span>
            <span class="stat-value">{{ wgsPassCount }}<span class="stat-denom">/{{ wgsTotalCount }}</span></span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background:var(--c-amber-dim);color:var(--c-amber)"><i class="pi pi-tag"></i></div>
          <div class="stat-body">
            <span class="stat-label">Unique Organisms</span>
            <span class="stat-value">{{ organisms.length }}</span>
          </div>
        </div>
      </div>

      <!-- ── Water quality time-series ── -->
      <section class="chart-section">
        <div class="section-header">
          <h2 class="section-title"><i class="pi pi-chart-line"></i> Water Quality Over Time</h2>
          <div class="param-tabs">
            <button
              v-for="(meta, key) in PARAM_META" :key="key"
              class="param-tab"
              :class="{ 'param-tab--active': selParam === key }"
              :style="selParam === key ? { borderColor: meta.color, color: meta.color } : {}"
              @click="selParam = key as typeof selParam"
            >{{ meta.label }}</button>
          </div>
        </div>
        <div class="chart-card">
          <VChart :option="waterChartOption" autoresize style="height:260px" />
        </div>
      </section>

      <!-- ── AMR classes + Binary profile ── -->
      <div class="two-col">
        <section class="chart-section">
          <div class="section-header">
            <h2 class="section-title"><i class="pi pi-shield"></i> AMR Resistance Classes</h2>
            <span class="section-note">AMRFinderPlus · gene hits by class</span>
          </div>
          <div class="chart-card">
            <VChart :option="amrBarOption" autoresize style="height:200px" />
          </div>
        </section>
        <section class="chart-section">
          <div class="section-header">
            <h2 class="section-title"><i class="pi pi-sliders-h"></i> Binary Typing Profile</h2>
            <span class="section-note">Integron &amp; gene markers · per isolate</span>
          </div>
          <div class="chart-card binary-profile">
            <div v-for="marker in binaryProfileSummary" :key="marker.marker" class="marker-row">
              <span class="marker-name">{{ marker.marker }}</span>
              <div class="marker-dots">
                <span
                  v-for="iso in detail.isolates" :key="iso.isolateId"
                  class="marker-dot"
                  :class="iso.binaryTypingProfile[marker.marker as keyof BinaryTypingProfile] ? 'dot--pos' : 'dot--neg'"
                  :title="iso.isolateId"
                ></span>
              </div>
              <span class="marker-count" :class="marker.positive > 0 ? 'count--pos' : 'count--neg'">
                {{ marker.positive }}/{{ marker.total }}
              </span>
            </div>
          </div>
        </section>
      </div>

      <!-- ── Antibiotic Resistance Matrix ── -->
      <section class="chart-section" v-if="resistanceMatrix.antibiotics.length > 0">
        <div class="section-header">
          <h2 class="section-title"><i class="pi pi-table"></i> Antibiotic Resistance Profile</h2>
          <span class="section-note">StarAMR · predicted phenotype per isolate</span>
        </div>
        <div class="chart-card matrix-card">
          <div class="matrix-legend">
            <span class="mleg mleg--r">R Resistant</span>
            <span class="mleg mleg--i">I Intermediate</span>
            <span class="mleg mleg--s">S Susceptible</span>
          </div>
          <div class="matrix-scroll">
            <table class="matrix-table">
              <thead>
                <tr>
                  <th class="matrix-th-isolate">Isolate</th>
                  <th
                    v-for="ab in resistanceMatrix.antibiotics"
                    :key="ab"
                    class="matrix-th-drug"
                    :title="ab"
                  >
                    <span class="drug-label">{{ ab }}</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in resistanceMatrix.rows" :key="row.isolateId">
                  <td class="matrix-isolate-cell">
                    <span class="matrix-isolate-id">{{ row.isolateId }}</span>
                    <span class="matrix-organism">{{ row.organism }}</span>
                  </td>
                  <td
                    v-for="(cell, ci) in row.cells"
                    :key="ci"
                    class="matrix-cell"
                    :style="{ background: matrixColor(cell) }"
                    :title="`${row.isolateId} — ${resistanceMatrix.antibiotics[ci]}: ${cell}`"
                  >
                    <span class="matrix-cell-label" :style="{ color: matrixTextColor(cell) }">
                      {{ matrixLabel(cell) }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <!-- ── Isolates table ── -->
      <section class="chart-section">
        <div class="section-header">
          <h2 class="section-title"><i class="pi pi-table"></i> Isolates</h2>
          <span class="section-note">Binary_Information · linked to water samples</span>
        </div>
        <div class="table-card">
          <table class="data-table">
            <thead>
              <tr>
                <th>Isolate ID</th><th>Sample</th><th>Organism</th>
                <th>Source Context</th><th>Virulence Genes</th>
                <th>Intl1</th><th>TEM</th><th>SHV</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="iso in detail.isolates" :key="iso.isolateId">
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
        </div>
      </section>

      <!-- ── AMR Sequences table ── -->
      <section class="chart-section">
        <div class="section-header">
          <h2 class="section-title"><i class="pi pi-code"></i> AMR Gene Sequences</h2>
          <span class="section-note">AMRFinderPlus · gene-level hits per isolate</span>
        </div>
        <div class="table-card">
          <table class="data-table">
            <thead>
              <tr>
                <th>Isolate ID</th><th>Gene</th><th>Element Type</th>
                <th>Class</th><th>Subclass</th>
                <th style="text-align:right">Identity %</th><th style="text-align:right">Coverage %</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(seq, i) in detail.amrSequences" :key="i">
                <td class="mono dim">{{ seq.isolateId }}</td>
                <td class="mono gene-name">{{ seq.geneSymbol }}</td>
                <td><span class="type-badge" :class="seq.elementType === 'AMR' ? 'type--amr' : 'type--stress'">{{ seq.elementType }}</span></td>
                <td class="dim">{{ seq.resistanceClass }}</td>
                <td class="dim">{{ seq.subclass }}</td>
                <td style="text-align:right" class="pct-cell">{{ seq.identityPercentage.toFixed(2) }}%</td>
                <td style="text-align:right" class="pct-cell">{{ seq.coveragePercentage.toFixed(2) }}%</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- ── WGS Metrics table ── -->
      <section class="chart-section">
        <div class="section-header">
          <h2 class="section-title"><i class="pi pi-server"></i> WGS Metrics</h2>
          <span class="section-note">StarAMR · whole genome sequencing results</span>
        </div>
        <div class="table-card">
          <table class="data-table">
            <thead>
              <tr>
                <th>Isolate ID</th><th>Quality</th><th>Genotype</th>
                <th>Predicted Phenotype</th><th>SIR Profile</th><th>Plasmid</th>
                <th style="text-align:right">Genome (bp)</th><th style="text-align:right">N50</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="wgs in detail.wgsMetrics" :key="wgs.isolateId">
                <td class="mono dim">{{ wgs.isolateId }}</td>
                <td><span class="quality-badge" :class="wgs.qualityStatus === 'Passed' ? 'quality--pass' : 'quality--fail'">{{ wgs.qualityStatus }}</span></td>
                <td class="mono dim genotype-cell">{{ wgs.genotype }}</td>
                <td class="dim pheno-cell">{{ wgs.predictedPhenotype }}</td>
                <td><span class="sir-badge" :style="{ color: sirColor(wgs.sirProfile), background: sirBg(wgs.sirProfile) }">{{ wgs.sirProfile }}</span></td>
                <td class="mono dim">{{ wgs.plasmid }}</td>
                <td style="text-align:right" class="num-cell">{{ wgs.genomeLength.toLocaleString() }}</td>
                <td style="text-align:right" class="num-cell">{{ wgs.n50Value.toLocaleString() }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- ── Sampling history table ── -->
      <section class="chart-section">
        <div class="section-header">
          <h2 class="section-title"><i class="pi pi-list"></i> Sampling History</h2>
          <span class="section-note">Epicollect · physicochemical readings</span>
        </div>
        <div class="table-card">
          <table class="data-table">
            <thead>
              <tr>
                <th>Sample ID</th><th>Trip</th><th>Date</th>
                <th style="text-align:right">Temp (°C)</th><th style="text-align:right">pH</th>
                <th style="text-align:right">TDS (mg/L)</th><th style="text-align:right">EC (μS/cm)</th><th style="text-align:right">DO (mg/L)</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="s in detail.waterSamples" :key="s.sampleId">
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
        </div>
      </section>

    </template>

    <div v-else class="error-state">
      <i class="pi pi-exclamation-triangle"></i>
      <p>Site not found. Check the siteId parameter.</p>
    </div>

  </div>
</template>

<style scoped>
.river-detail { padding: 28px 32px 60px; max-width: 1280px; margin: 0 auto; }

/* ── Header ── */
.page-header { margin-bottom: 20px; }

.breadcrumb-row {
  display: flex; align-items: center; justify-content: space-between;
  gap: 12px; margin-bottom: 12px; flex-wrap: wrap;
}
.breadcrumb { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--c-text-muted); }
.crumb-link { cursor: pointer; color: var(--c-brand); }
.crumb-link:hover { text-decoration: underline; }
.crumb-current { color: var(--c-text-muted); }
.crumb-sep { font-size: 9px; }

/* Hero card */
.site-hero-card {
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-top: 3px solid var(--c-brand);
  border-radius: 10px;
  padding: 20px 24px 18px;
  box-shadow: var(--c-shadow);
  position: relative;
}

.hero-top-row {
  display: flex; align-items: center; justify-content: space-between;
  gap: 12px; margin-bottom: 6px;
}

.hero-site-label {
  font-size: 10.5px; font-weight: 700; text-transform: uppercase;
  letter-spacing: 0.1em; color: var(--c-brand);
  display: flex; align-items: center; gap: 5px;
}

/* Clicking the site name opens the dropdown */
.hero-name-btn {
  display: flex; align-items: center; gap: 12px;
  background: none; border: none; padding: 0; cursor: pointer;
  text-align: left; width: 100%; margin-bottom: 14px;
}
.hero-name-btn:hover .page-title { color: var(--c-brand); }
.hero-name-btn:hover .hero-chevron { color: var(--c-brand); }

.page-title {
  font-family: 'Zen Dots', sans-serif; font-size: 30px; font-weight: 700;
  color: var(--c-heading); margin: 0; line-height: 1.1;
  transition: color 0.15s;
}

.hero-chevron {
  font-size: 14px; color: var(--c-text-muted);
  transition: transform 0.2s, color 0.15s; flex-shrink: 0;
}
.hero-chevron--open { transform: rotate(180deg); }

/* Inline dropdown inside the hero card */
.hero-dropdown {
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  border-radius: 8px;
  margin-bottom: 14px;
  overflow: hidden;
}

/* Shared dropdown internals */
.selector-search {
  display: flex; align-items: center; gap: 8px;
  padding: 9px 12px; border-bottom: 1px solid var(--c-border);
}
.search-icon { font-size: 12px; color: var(--c-text-muted); flex-shrink: 0; }
.search-input {
  flex: 1; background: transparent; border: none; outline: none;
  color: var(--c-text); font-family: 'DM Sans', sans-serif; font-size: 13px;
}
.search-input::placeholder { color: var(--c-text-muted); }
.selector-list { max-height: 200px; overflow-y: auto; padding: 4px; }
.selector-option {
  display: flex; align-items: center; justify-content: space-between;
  width: 100%; padding: 9px 10px; border-radius: 6px; border: none;
  background: transparent; cursor: pointer; text-align: left;
  transition: background 0.12s; gap: 8px;
}
.selector-option:hover { background: var(--c-brand-dim); }
.selector-option--active { background: var(--c-brand-dim); }
.option-main { display: flex; flex-direction: column; gap: 2px; }
.option-name { font-size: 13px; font-weight: 500; color: var(--c-text); }
.option-river { font-size: 10.5px; color: var(--c-text-muted); }
.option-id { font-family: monospace; font-size: 10.5px; color: var(--c-brand); background: var(--c-brand-dim); padding: 1px 6px; border-radius: 4px; flex-shrink: 0; }
.selector-empty { padding: 16px; text-align: center; font-size: 12px; color: var(--c-text-muted); }

/* Compare button */
.compare-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 14px; border-radius: 6px;
  border: 1px solid var(--c-brand); background: var(--c-brand-dim);
  color: var(--c-brand); font-family: 'DM Sans', sans-serif;
  font-size: 12.5px; font-weight: 600;
  cursor: pointer; transition: background 0.15s; white-space: nowrap;
}
.compare-btn:hover { background: var(--c-brand); color: #fff; }
.compare-btn .pi { font-size: 12px; }

.meta-pills { display: flex; flex-wrap: wrap; gap: 7px; }
.pill { display: flex; align-items: center; gap: 5px; background: var(--c-bg); border: 1px solid var(--c-border); border-radius: 20px; padding: 3px 10px; font-size: 11.5px; color: var(--c-text-muted); }
.pill .pi { font-size: 10px; }
.pill--id { font-family: monospace; color: var(--c-brand); border-color: var(--c-brand); background: var(--c-brand-dim); }

/* ── Safety banner ── */
.safety-banner {
  display: flex; align-items: flex-start; gap: 16px;
  border: 1.5px solid; border-radius: 10px; padding: 16px 20px;
  margin-bottom: 22px;
}
.safety-icon-wrap { flex-shrink: 0; padding-top: 2px; }
.safety-icon { font-size: 24px; }
.safety-body { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.safety-header-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 2px; }
.safety-level { font-family: 'Zen Dots', sans-serif; font-size: 13px; font-weight: 700; letter-spacing: 0.04em; }
.safety-tag {
  font-size: 10px; font-weight: 600; text-transform: uppercase;
  letter-spacing: 0.08em; color: var(--c-text-muted);
  background: var(--c-card); border: 1px solid var(--c-border);
  border-radius: 4px; padding: 1px 7px;
}
.safety-headline { font-size: 14px; font-weight: 600; color: var(--c-heading); margin: 0; }
.safety-detail { font-size: 12.5px; color: var(--c-text-muted); margin: 0; line-height: 1.55; }

/* ── Stat cards ── */
.stat-grid { display: grid; grid-template-columns: repeat(auto-fit,minmax(180px,1fr)); gap: 14px; margin-bottom: 24px; }
.stat-card { display: flex; align-items: center; gap: 14px; background: var(--c-card); border: 1px solid var(--c-border); border-radius: 8px; padding: 16px 18px; box-shadow: var(--c-shadow); }
.stat-icon { width: 40px; height: 40px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 16px; flex-shrink: 0; }
.stat-body { display: flex; flex-direction: column; gap: 2px; }
.stat-label { font-size: 10px; color: var(--c-text-muted); text-transform: uppercase; letter-spacing: 0.07em; }
.stat-value { font-size: 24px; font-weight: 700; color: var(--c-heading); line-height: 1.1; font-family: 'Zen Dots', sans-serif; }
.stat-denom { font-size: 14px; color: var(--c-text-muted); font-family: 'DM Sans', sans-serif; font-weight: 400; }

/* ── Sections ── */
.chart-section { margin-bottom: 22px; }
.section-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; margin-bottom: 10px; }
.section-title { font-size: 12px; font-weight: 600; color: var(--c-heading); text-transform: uppercase; letter-spacing: 0.08em; display: flex; align-items: center; gap: 7px; }
.section-title .pi { color: var(--c-brand); }
.section-note { font-size: 11px; color: var(--c-text-dim); }
.chart-card { background: var(--c-card); border: 1px solid var(--c-border); border-radius: 8px; padding: 16px; box-shadow: var(--c-shadow); }

/* Param tabs */
.param-tabs { display: flex; flex-wrap: wrap; gap: 6px; }
.param-tab { padding: 4px 12px; border-radius: 20px; border: 1.5px solid var(--c-border); background: transparent; color: var(--c-text-muted); font-size: 11px; cursor: pointer; transition: all 0.15s; font-family: 'DM Sans', sans-serif; }
.param-tab:hover { background: var(--c-card); color: var(--c-text); }
.param-tab--active { font-weight: 600; }

/* Two col */
.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 22px; margin-bottom: 22px; }
@media (max-width:900px) { .two-col { grid-template-columns: 1fr; } }

/* Binary profile */
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

/* ── Resistance matrix ── */
.matrix-card { padding: 14px 16px; }
.matrix-legend { display: flex; gap: 16px; margin-bottom: 14px; }
.mleg { display: flex; align-items: center; gap: 5px; font-size: 11px; font-weight: 600; }
.mleg::before { content: ''; width: 12px; height: 12px; border-radius: 3px; display: inline-block; }
.mleg--r { color: var(--c-red); }
.mleg--r::before { background: var(--c-red-dim); border: 1.5px solid var(--c-red); }
.mleg--i { color: var(--c-amber); }
.mleg--i::before { background: var(--c-amber-dim); border: 1.5px solid var(--c-amber); }
.mleg--s { color: var(--c-green); }
.mleg--s::before { background: var(--c-green-dim); border: 1.5px solid var(--c-green); }

.matrix-scroll { overflow-x: auto; }
.matrix-table { border-collapse: collapse; width: 100%; }
.matrix-th-isolate {
  text-align: left; padding: 8px 12px 8px 0;
  font-size: 10px; font-weight: 600; letter-spacing: 0.08em;
  text-transform: uppercase; color: var(--c-text-muted);
  white-space: nowrap; min-width: 160px;
  border-bottom: 1px solid var(--c-border);
}
.matrix-th-drug {
  padding: 4px 3px 8px;
  border-bottom: 1px solid var(--c-border);
  min-width: 38px;
}
.drug-label {
  display: block;
  writing-mode: vertical-rl;
  transform: rotate(180deg);
  font-size: 10px; font-weight: 600;
  color: var(--c-text-muted); letter-spacing: 0.04em;
  white-space: nowrap;
  max-height: 90px; overflow: hidden;
}
.matrix-isolate-cell {
  padding: 6px 12px 6px 0;
  border-bottom: 1px solid var(--c-border);
  vertical-align: middle;
}
.matrix-isolate-id { display: block; font-family: monospace; font-size: 11px; color: var(--c-brand); }
.matrix-organism { display: block; font-size: 10px; color: var(--c-text-muted); font-style: italic; }
.matrix-cell {
  padding: 6px 3px;
  text-align: center;
  border-bottom: 1px solid var(--c-border);
  border-left: 1px solid var(--c-border);
  transition: filter 0.12s;
  cursor: default;
}
.matrix-cell:hover { filter: brightness(1.15); }
.matrix-cell-label { font-size: 11px; font-weight: 700; }

/* ── Tables ── */
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

/* Badges */
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

/* Skeleton */
.skeleton-wrap { display: flex; flex-direction: column; gap: 18px; }
.skel-row { display: grid; grid-template-columns: repeat(4,1fr); gap: 14px; }
.skel { background: var(--c-card); border-radius: 8px; animation: pulse 1.4s infinite; width: 100%; }
@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:.5} }

/* Error */
.error-state { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 40vh; gap: 12px; color: var(--c-text-muted); }
.error-state .pi { font-size: 40px; color: var(--c-red); }
</style>