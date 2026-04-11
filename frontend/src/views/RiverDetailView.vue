<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
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

const route      = useRoute()
const themeStore = useThemeStore()
const isDark     = computed(() => themeStore.resolvedTheme === 'dark')
const loading    = ref(true)

const siteId = computed(() => (route.query.siteId as string) ?? 'A10')

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
// FIX: typed as Record<string, SiteDetail | undefined> so TypeScript
// knows a lookup might return undefined
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

onMounted(() => {
  // TODO: replace with real API calls:
  // GET /api/sites/{siteId}
  // GET /api/sites/{siteId}/water-samples
  // GET /api/sites/{siteId}/isolates
  // GET /api/sites/{siteId}/amr-sequences
  // GET /api/sites/{siteId}/wgs-metrics
  setTimeout(() => {
    // FIX: added ?? null so the type is SiteDetail | null, not SiteDetail | undefined
    detail.value = SITE_DATA[siteId.value] ?? SITE_DATA['A10'] ?? null
    loading.value = false
  }, 450)
})

// ── Param meta ─────────────────────────────────────────────────────
interface ParamMeta {
  label: string
  unit: string
  color: string
}

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

// Water quality time-series chart
const waterChartOption = computed(() => {
  if (!detail.value) return {}
  // FIX: explicitly typed as ParamMeta | undefined and early-returned if missing
  const meta: ParamMeta | undefined = PARAM_META[selParam.value as string]
  if (!meta) return {}

  const samples = detail.value.waterSamples
  const dates   = samples.map(s => `${s.tripIdentifier} · ${s.collectionDate}`)
  const values  = samples.map(s => s[selParam.value] as number | null)

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      formatter: (p: any) => `${p[0].axisValue}<br/>${meta.label}: <b>${p[0].value ?? '—'} ${meta.unit}</b>`,
    },
    grid: { left: 52, right: 20, top: 16, bottom: 40 },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: { ...axLabel.value, rotate: 15 },
      axisLine: axLine.value,
    },
    yAxis: {
      type: 'value',
      axisLabel: { ...axLabel.value, formatter: (v: number) => `${v}${meta.unit}` },
      splitLine: splitLine.value,
      axisLine: { show: false },
    },
    series: [{
      type: 'line',
      data: values,
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { color: meta.color, width: 2.5 },
      itemStyle: { color: meta.color, borderColor: isDark.value ? '#101D2E' : '#fff', borderWidth: 2 },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: meta.color + '44' },
            { offset: 1, color: meta.color + '00' },
          ],
        },
      },
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
    'BETA-LACTAM':    '#EF4444',
    'MACROLIDE':      '#F59E0B',
    'AMINOGLYCOSIDE': '#8B5CF6',
    'METAL':          '#6B7280',
    'SULFONAMIDE':    '#3B82F6',
    'TETRACYCLINE':   '#FB923C',
  }
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis' },
    grid: { left: 130, right: 30, top: 10, bottom: 20 },
    xAxis: { type: 'value', axisLabel: axLabel.value, splitLine: splitLine.value },
    yAxis: {
      type: 'category',
      data: entries.map(e => e[0]),
      axisLabel: { ...axLabel.value, fontFamily: 'monospace', fontSize: 10.5 },
      axisLine: axLine.value,
    },
    series: [{
      type: 'bar',
      barMaxWidth: 18,
      data: entries.map(e => ({
        value: e[1],
        itemStyle: { color: CLASS_COLORS[e[0]] ?? '#3B82F6', borderRadius: [0, 3, 3, 0] },
      })),
    }],
  }
})

// Binary typing profile summary across all isolates
const binaryProfileSummary = computed(() => {
  if (!detail.value) return []
  const keys: (keyof BinaryTypingProfile)[] = ['Intl1', 'Intl2', 'Intl3', 'TEM', 'SHV']
  return keys.map(k => ({
    marker: k,
    positive: detail.value!.isolates.filter(i => i.binaryTypingProfile[k]).length,
    total: detail.value!.isolates.length,
  }))
})

// WGS quality counts
const wgsPassCount  = computed(() => detail.value?.wgsMetrics.filter(w => w.qualityStatus === 'Passed').length ?? 0)
const wgsTotalCount = computed(() => detail.value?.wgsMetrics.length ?? 0)

// Unique organism list
const organisms = computed(() => {
  if (!detail.value) return []
  const map = new Map<string, number>()
  for (const iso of detail.value.isolates) {
    map.set(iso.organismIdentity, (map.get(iso.organismIdentity) ?? 0) + 1)
  }
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
</script>

<template>
  <div class="river-detail">

    <!-- Skeleton while loading -->
    <div v-if="loading" class="skeleton-wrap">
      <div class="skel" style="height:72px"></div>
      <div class="skel-row">
        <div class="skel" style="height:72px" v-for="n in 4" :key="n"></div>
      </div>
      <div class="skel" style="height:280px"></div>
      <div class="skel-row">
        <div class="skel" style="height:240px"></div>
        <div class="skel" style="height:240px"></div>
      </div>
      <div class="skel" style="height:220px"></div>
    </div>

    <template v-else-if="detail">

      <!-- ── Page header ── -->
      <header class="page-header">
        <div class="breadcrumb">
          <span class="crumb-link" @click="$router.push('/map')">Map</span>
          <i class="pi pi-chevron-right crumb-sep"></i>
          <span class="crumb-current">River Detail</span>
        </div>
        <h1 class="page-title">{{ detail.site.locationName }}</h1>
        <div class="meta-pills">
          <span class="pill"><i class="pi pi-map-marker"></i> {{ detail.site.riverName }}</span>
          <span class="pill"><i class="pi pi-compass"></i> {{ detail.site.latitude }}, {{ detail.site.longitude }}</span>
          <span class="pill"><i class="pi pi-database"></i> {{ detail.waterSamples.length }} samples</span>
          <span class="pill"><i class="pi pi-microscope"></i> {{ detail.isolates.length }} isolates</span>
          <span class="pill pill--id">Site {{ detail.site.siteId }}</span>
        </div>
      </header>

      <!-- ── Stat cards ── -->
      <div class="stat-grid">
        <div class="stat-card">
          <div class="stat-icon" style="background:var(--c-brand-dim);color:var(--c-brand)">
            <i class="pi pi-calendar"></i>
          </div>
          <div class="stat-body">
            <span class="stat-label">Sampling Trips</span>
            <span class="stat-value">{{ new Set(detail.waterSamples.map(s => s.tripIdentifier)).size }}</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background:var(--c-red-dim);color:var(--c-red)">
            <i class="pi pi-shield"></i>
          </div>
          <div class="stat-body">
            <span class="stat-label">AMR Gene Hits</span>
            <span class="stat-value">{{ detail.amrSequences.length }}</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background:var(--c-green-dim);color:var(--c-green)">
            <i class="pi pi-check-circle"></i>
          </div>
          <div class="stat-body">
            <span class="stat-label">WGS Passed QC</span>
            <span class="stat-value">
              {{ wgsPassCount }}<span class="stat-denom">/{{ wgsTotalCount }}</span>
            </span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background:var(--c-amber-dim);color:var(--c-amber)">
            <i class="pi pi-tag"></i>
          </div>
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
              v-for="(meta, key) in PARAM_META"
              :key="key"
              class="param-tab"
              :class="{ 'param-tab--active': selParam === key }"
              :style="selParam === key ? { borderColor: meta.color, color: meta.color } : {}"
              @click="selParam = key as typeof selParam"
            >
              {{ meta.label }}
            </button>
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
                  v-for="iso in detail.isolates"
                  :key="iso.isolateId"
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
                <th>Isolate ID</th>
                <th>Sample</th>
                <th>Organism</th>
                <th>Source Context</th>
                <th>Virulence Genes</th>
                <th>Intl1</th>
                <th>TEM</th>
                <th>SHV</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="iso in detail.isolates" :key="iso.isolateId">
                <td class="mono">{{ iso.isolateId }}</td>
                <td class="mono dim">{{ iso.sampleId }}</td>
                <td class="organism-cell"><em>{{ iso.organismIdentity }}</em></td>
                <td class="dim">{{ iso.sourceContext }}</td>
                <td class="mono dim">{{ iso.virulenceGenes ?? '—' }}</td>
                <td>
                  <span class="bool-badge" :class="iso.binaryTypingProfile.Intl1 ? 'bool--pos' : 'bool--neg'">
                    {{ iso.binaryTypingProfile.Intl1 ? '✓' : '✗' }}
                  </span>
                </td>
                <td>
                  <span class="bool-badge" :class="iso.binaryTypingProfile.TEM ? 'bool--pos' : 'bool--neg'">
                    {{ iso.binaryTypingProfile.TEM ? '✓' : '✗' }}
                  </span>
                </td>
                <td>
                  <span class="bool-badge" :class="iso.binaryTypingProfile.SHV ? 'bool--pos' : 'bool--neg'">
                    {{ iso.binaryTypingProfile.SHV ? '✓' : '✗' }}
                  </span>
                </td>
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
                <th>Isolate ID</th>
                <th>Gene</th>
                <th>Element Type</th>
                <th>Class</th>
                <th>Subclass</th>
                <th style="text-align:right">Identity %</th>
                <th style="text-align:right">Coverage %</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(seq, i) in detail.amrSequences" :key="i">
                <td class="mono dim">{{ seq.isolateId }}</td>
                <td class="mono gene-name">{{ seq.geneSymbol }}</td>
                <td>
                  <span class="type-badge" :class="seq.elementType === 'AMR' ? 'type--amr' : 'type--stress'">
                    {{ seq.elementType }}
                  </span>
                </td>
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
                <th>Isolate ID</th>
                <th>Quality</th>
                <th>Genotype</th>
                <th>Predicted Phenotype</th>
                <th>SIR Profile</th>
                <th>Plasmid</th>
                <th style="text-align:right">Genome (bp)</th>
                <th style="text-align:right">N50</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="wgs in detail.wgsMetrics" :key="wgs.isolateId">
                <td class="mono dim">{{ wgs.isolateId }}</td>
                <td>
                  <span class="quality-badge" :class="wgs.qualityStatus === 'Passed' ? 'quality--pass' : 'quality--fail'">
                    {{ wgs.qualityStatus }}
                  </span>
                </td>
                <td class="mono dim genotype-cell">{{ wgs.genotype }}</td>
                <td class="dim pheno-cell">{{ wgs.predictedPhenotype }}</td>
                <td>
                  <span
                    class="sir-badge"
                    :style="{ color: sirColor(wgs.sirProfile), background: sirBg(wgs.sirProfile) }"
                  >
                    {{ wgs.sirProfile }}
                  </span>
                </td>
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
                <th>Sample ID</th>
                <th>Trip</th>
                <th>Date</th>
                <th style="text-align:right">Temp (°C)</th>
                <th style="text-align:right">pH</th>
                <th style="text-align:right">TDS (mg/L)</th>
                <th style="text-align:right">EC (μS/cm)</th>
                <th style="text-align:right">DO (mg/L)</th>
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

/* Header */
.breadcrumb { display:flex; align-items:center; gap:6px; margin-bottom:8px; font-size:12px; color:var(--c-text-muted); }
.crumb-link { cursor:pointer; color:var(--c-brand); }
.crumb-link:hover { text-decoration:underline; }
.crumb-sep { font-size:9px; }
.page-header { margin-bottom:22px; }
.page-title { font-family:'Zen Dots',sans-serif; font-size:22px; font-weight:700; color:var(--c-heading); margin:0 0 10px; }
.meta-pills { display:flex; flex-wrap:wrap; gap:7px; }
.pill { display:flex; align-items:center; gap:5px; background:var(--c-card); border:1px solid var(--c-border); border-radius:20px; padding:3px 10px; font-size:11.5px; color:var(--c-text-muted); }
.pill .pi { font-size:10px; }
.pill--id { font-family:monospace; color:var(--c-brand); border-color:var(--c-brand); background:var(--c-brand-dim); }

/* Stat cards */
.stat-grid { display:grid; grid-template-columns:repeat(auto-fit,minmax(180px,1fr)); gap:14px; margin-bottom:24px; }
.stat-card { display:flex; align-items:center; gap:14px; background:var(--c-card); border:1px solid var(--c-border); border-radius:8px; padding:16px 18px; box-shadow:var(--c-shadow); }
.stat-icon { width:40px; height:40px; border-radius:8px; display:flex; align-items:center; justify-content:center; font-size:16px; flex-shrink:0; }
.stat-body { display:flex; flex-direction:column; gap:2px; }
.stat-label { font-size:10px; color:var(--c-text-muted); text-transform:uppercase; letter-spacing:0.07em; }
.stat-value { font-size:24px; font-weight:700; color:var(--c-heading); line-height:1.1; font-family:'Zen Dots',sans-serif; }
.stat-denom { font-size:14px; color:var(--c-text-muted); font-family:'DM Sans',sans-serif; font-weight:400; }

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
.param-tab--active { font-weight:600; }

/* Two col */
.two-col { display:grid; grid-template-columns:1fr 1fr; gap:22px; margin-bottom:22px; }
@media (max-width:900px) { .two-col { grid-template-columns:1fr; } }

/* Binary profile */
.binary-profile { display:flex; flex-direction:column; gap:12px; padding:18px; }
.marker-row { display:grid; grid-template-columns:48px 1fr 48px; align-items:center; gap:10px; }
.marker-name { font-family:monospace; font-size:12px; font-weight:600; color:var(--c-text); }
.marker-dots { display:flex; gap:6px; }
.marker-dot { width:14px; height:14px; border-radius:50%; flex-shrink:0; }
.dot--pos { background:var(--c-red); }
.dot--neg { background:var(--c-border); }
.marker-count { font-size:11px; text-align:right; font-weight:600; }
.count--pos { color:var(--c-red); }
.count--neg { color:var(--c-text-dim); }

/* Tables */
.table-card { background:var(--c-card); border:1px solid var(--c-border); border-radius:8px; overflow:auto; box-shadow:var(--c-shadow); }
.data-table { width:100%; border-collapse:collapse; font-size:12px; }
.data-table thead th { background:var(--c-card); color:var(--c-text-muted); font-size:10px; font-weight:600; letter-spacing:0.09em; text-transform:uppercase; padding:10px 14px; border-bottom:1px solid var(--c-border); text-align:left; white-space:nowrap; }
.data-table tbody tr { border-bottom:1px solid var(--c-border); transition:background 0.12s; }
.data-table tbody tr:last-child { border-bottom:none; }
.data-table tbody tr:hover { background:var(--c-brand-dim); }
.data-table tbody td { padding:9px 14px; color:var(--c-text); }
.mono { font-family:monospace; font-size:11px; }
.dim { color:var(--c-text-muted); }
.organism-cell { font-size:12px; color:var(--c-text); }
.gene-name { color:var(--c-brand); font-size:11.5px; }
.genotype-cell { font-size:10.5px; max-width:160px; }
.pheno-cell { max-width:180px; font-size:11px; }
.pct-cell { font-variant-numeric:tabular-nums; }
.num-cell { font-variant-numeric:tabular-nums; color:var(--c-text-muted); }

/* Badges */
.bool-badge { display:inline-flex; align-items:center; justify-content:center; width:20px; height:20px; border-radius:4px; font-size:11px; font-weight:700; }
.bool--pos { background:var(--c-red-dim); color:var(--c-red); }
.bool--neg { background:var(--c-border); color:var(--c-text-dim); }
.type-badge { display:inline-block; padding:2px 7px; border-radius:4px; font-size:10px; font-weight:700; letter-spacing:0.05em; }
.type--amr    { background:var(--c-red-dim); color:var(--c-red); }
.type--stress { background:var(--c-amber-dim); color:var(--c-amber); }
.quality-badge { display:inline-block; padding:2px 8px; border-radius:4px; font-size:10.5px; font-weight:600; }
.quality--pass { background:var(--c-green-dim); color:var(--c-green); }
.quality--fail { background:var(--c-red-dim); color:var(--c-red); }
.sir-badge { display:inline-block; padding:2px 8px; border-radius:4px; font-size:10.5px; font-weight:600; }

/* Skeleton */
.skeleton-wrap { display:flex; flex-direction:column; gap:18px; }
.skel-row { display:grid; grid-template-columns:repeat(4,1fr); gap:14px; }
.skel { background:var(--c-card); border-radius:8px; animation:pulse 1.4s infinite; width:100%; }
@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:.5} }

/* Error */
.error-state { display:flex; flex-direction:column; align-items:center; justify-content:center; min-height:40vh; gap:12px; color:var(--c-text-muted); }
.error-state .pi { font-size:40px; color:var(--c-red); }
</style>