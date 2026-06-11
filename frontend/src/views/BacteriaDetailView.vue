<script setup lang="ts">
/* ── Imports ─────────────────────────────────────────────────── */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import VChart from 'vue-echarts'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'

import ResistanceProfileGrid from '@/components/amr/ResistanceProfileGrid.vue'
import WgsMetricsPanel from '@/components/amr/WgsMetricsPanel.vue'
import { useChartTheme } from '@/composables/useChartTheme'
import { ORGANISM_DB, DEFAULT_ORGANISM } from '@/data/organisms'
import type { RiskLevel } from '@/types/amr'

/* ── Setup ───────────────────────────────────────────────────── */
const route = useRoute()
const router = useRouter()
const visible = ref(false)
onMounted(() => {
  setTimeout(() => {
    visible.value = true
  }, 60)
})

/* ── Chart theme ─────────────────────────────────────────────── */
const { isDark, tooltipBase, axisLabel, splitLine, axisLine, blue } = useChartTheme()

/* ── Organism data ───────────────────────────────────────────── */
const organismName = computed(() => decodeURIComponent(route.params.name as string))
// Fallback guarantees organism is always defined; `!` is safe because DEFAULT_ORGANISM is a known key.
const organism = computed(() => (ORGANISM_DB[organismName.value] ?? ORGANISM_DB[DEFAULT_ORGANISM])!)

/* ── Derived counts ──────────────────────────────────────────── */
const rCount = computed(
  () => organism.value.resistanceProfile.filter((a) => a.level === 'R').length,
)
const iCount = computed(
  () => organism.value.resistanceProfile.filter((a) => a.level === 'I').length,
)
const sCount = computed(
  () => organism.value.resistanceProfile.filter((a) => a.level === 'S').length,
)

const classBreakdown = computed(() => {
  const map: Record<string, number> = {}
  for (const g of organism.value.genes) {
    map[g.resistanceClass] = (map[g.resistanceClass] ?? 0) + g.occurrenceCount
  }
  return Object.entries(map)
    .sort(([, a], [, b]) => b - a)
    .map(([name, count]) => ({ name, count }))
})

/* ── Colour helpers ──────────────────────────────────────────── */
function arCodeSeverity(code: string): 'danger' | 'warn' | 'secondary' {
  if (code === 'CRE' || code === 'VRE') return 'danger'
  if (code === 'ESBL' || code === 'MDR' || code === 'MDRO') return 'warn'
  return 'secondary'
}

function classSeverity(cls: string): 'danger' | 'warn' | 'secondary' | 'info' | 'success' {
  const c = cls.toUpperCase()
  if (c.includes('BETA') || c.includes('CARBAPENEM') || c.includes('GLYCO')) return 'danger'
  if (c.includes('AMINO') || c.includes('QUINOLONE') || c.includes('MULTI')) return 'warn'
  if (c.includes('TETRA') || c.includes('SULFO') || c.includes('MACRO')) return 'secondary'
  if (c.includes('TRIMETH') || c.includes('FOSFO')) return 'info'
  return 'success'
}

function riskSeverity(r: RiskLevel): 'danger' | 'warn' | 'success' {
  if (r === 'HIGH') return 'danger'
  if (r === 'MED') return 'warn'
  return 'success'
}

function classBarColor(cls: string): string {
  const c = cls.toUpperCase()
  if (c.includes('BETA') || c.includes('GLYCO')) return isDark.value ? '#F87171' : '#DC2626'
  if (c.includes('AMINO') || c.includes('QUINOLONE')) return isDark.value ? '#FBBF24' : '#D97706'
  if (c.includes('MULTI')) return isDark.value ? '#A78BFA' : '#7C3AED'
  if (c.includes('TETRA') || c.includes('SULFO')) return isDark.value ? '#5C7A94' : '#6B7280'
  return isDark.value ? '#34D399' : '#059669'
}

function trendIcon(t: string): string {
  if (t === 'up') return 'pi-arrow-up'
  if (t === 'down') return 'pi-arrow-down'
  return 'pi-minus'
}

function trendClass(t: string): string {
  if (t === 'up') return 'trend-danger'
  if (t === 'down') return 'trend-success'
  return 'trend-muted'
}

/* ── ECharts options ─────────────────────────────────────────── */
const MONTHS = [
  'Jan',
  'Feb',
  'Mar',
  'Apr',
  'May',
  'Jun',
  'Jul',
  'Aug',
  'Sep',
  'Oct',
  'Nov',
  'Dec',
] as const

const trendChartOption = computed(() => ({
  backgroundColor: 'transparent',
  animation: true,
  animationDuration: 1000,
  animationEasing: 'cubicOut' as const,
  grid: { left: 48, right: 16, top: 20, bottom: 36 },
  tooltip: {
    trigger: 'axis' as const,
    ...tooltipBase.value,
    formatter: (params: Array<{ name: string; color: string; value: number }>) => {
      const p = params[0]
      if (!p) return ''
      return `<div style="font-family:DM Sans,sans-serif;font-size:12px">
        <strong>${p.name}</strong><br/>
        <span style="color:${p.color}">●</span> Detections: <strong>${p.value}</strong>
      </div>`
    },
  },
  xAxis: {
    type: 'category' as const,
    data: MONTHS,
    axisTick: { show: false },
    axisLine: axisLine.value,
    axisLabel: axisLabel.value,
  },
  yAxis: {
    type: 'value' as const,
    splitLine: splitLine.value,
    axisLabel: axisLabel.value,
    axisLine: { show: false },
    axisTick: { show: false },
  },
  series: [
    {
      type: 'line' as const,
      data: organism.value.monthlyTrend,
      smooth: 0.3,
      symbol: 'circle',
      symbolSize: 5,
      lineStyle: { color: blue.value, width: 2.5 },
      itemStyle: {
        color: blue.value,
        borderColor: isDark.value ? '#0D1520' : '#FFFFFF',
        borderWidth: 2,
      },
      areaStyle: {
        color: {
          type: 'linear' as const,
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: isDark.value ? 'rgba(59,130,246,0.22)' : 'rgba(37,99,235,0.15)' },
            { offset: 1, color: isDark.value ? 'rgba(59,130,246,0.01)' : 'rgba(37,99,235,0.01)' },
          ],
        },
      },
    },
  ],
}))

const classChartOption = computed(() => ({
  backgroundColor: 'transparent',
  animation: true,
  animationDuration: 900,
  animationEasing: 'cubicOut' as const,
  grid: { left: 10, right: 60, top: 8, bottom: 8, containLabel: true },
  tooltip: {
    trigger: 'axis' as const,
    axisPointer: { type: 'none' as const },
    ...tooltipBase.value,
  },
  xAxis: {
    type: 'value' as const,
    axisLabel: { show: false },
    splitLine: { show: false },
    axisLine: { show: false },
    axisTick: { show: false },
  },
  yAxis: {
    type: 'category' as const,
    data: classBreakdown.value.map((c) => c.name).reverse(),
    axisTick: { show: false },
    axisLine: { show: false },
    axisLabel: { ...axisLabel.value, fontSize: 10, width: 120, overflow: 'truncate' as const },
  },
  series: [
    {
      type: 'bar' as const,
      data: classBreakdown.value.map((c) => c.count).reverse(),
      barMaxWidth: 18,
      itemStyle: {
        borderRadius: [0, 3, 3, 0],
        color: (params: { dataIndex: number }) => {
          const cls = classBreakdown.value.map((c) => c.name).reverse()[params.dataIndex] ?? ''
          return classBarColor(cls)
        },
      },
      label: {
        show: true,
        position: 'right' as const,
        color: isDark.value ? '#5C7A94' : '#9CA3AF',
        fontFamily: 'DM Mono, monospace',
        fontSize: 10,
      },
    },
  ],
}))
</script>

<template>
  <div class="bacteria-detail">
    <!-- ── Header ───────────────────────────────────────── -->
    <div class="detail-header">
      <!-- ── Breadcrumb ───────────────────────────────────── -->
      <div class="breadcrumb-row">
        <div class="breadcrumb">
          <span class="crumb-link" @click="$router.push('/map')">Map</span>
          <i class="pi pi-chevron-right crumb-sep"></i>
          <span class="crumb-current">Bacteria Detail</span>
        </div>
      </div>

      <button class="back-btn" @click="router.push('/')">
        <i class="pi pi-arrow-left"></i>
        <span>Dashboard</span>
      </button>

      <div class="header-identity" :class="{ 'header-identity--visible': visible }">
        <div class="organism-name-row">
          <h1 class="organism-name">
            <em>{{ organismName }}</em>
          </h1>
          <Tag
            :value="organism.arCode"
            :severity="arCodeSeverity(organism.arCode)"
            class="mdro-badge"
          />
        </div>
        <div class="organism-meta">
          <span class="meta-pill gram-pill">{{ organism.gramStain }}</span>
          <span class="meta-pill">Common: {{ organism.commonName }}</span>
          <span class="meta-pill"
            >Primary gene: <code>{{ organism.topGene }}</code></span
          >
        </div>
        <p class="organism-desc">{{ organism.description }}</p>
      </div>
    </div>

    <!-- ── KPI Row ──────────────────────────────────────── -->
    <section class="kpi-row">
      <div
        class="kpi-card"
        :class="{ 'kpi-card--visible': visible }"
        style="transition-delay: 60ms"
      >
        <p class="kpi-label">Total Detections</p>
        <p class="kpi-value">{{ organism.detectionCount.toLocaleString() }}</p>
        <div class="kpi-sub" :class="trendClass(organism.yoyTrend)">
          <i :class="`pi ${trendIcon(organism.yoyTrend)}`"></i>
          <span>year-on-year</span>
        </div>
      </div>

      <div
        class="kpi-card"
        :class="{ 'kpi-card--visible': visible }"
        style="transition-delay: 110ms"
      >
        <p class="kpi-label">River Sites Affected</p>
        <p class="kpi-value kpi-value--blue">{{ organism.siteCount }}</p>
        <div class="kpi-sub trend-muted"><span>across SA provinces</span></div>
      </div>

      <div
        class="kpi-card"
        :class="{ 'kpi-card--visible': visible }"
        style="transition-delay: 160ms"
      >
        <p class="kpi-label">MDRO Rate</p>
        <p class="kpi-value kpi-value--red">{{ organism.resistanceRate }}%</p>
        <div class="kpi-sub trend-muted"><span>of sampled isolates</span></div>
      </div>

      <div
        class="kpi-card"
        :class="{ 'kpi-card--visible': visible }"
        style="transition-delay: 210ms"
      >
        <p class="kpi-label">Resistance Profile</p>
        <div class="kpi-ris">
          <span class="ris-pill ris-r">R {{ rCount }}</span>
          <span class="ris-pill ris-i">I {{ iCount }}</span>
          <span class="ris-pill ris-s">S {{ sCount }}</span>
        </div>
        <div class="kpi-sub trend-muted"><span>Resistant / Intermediate / Susceptible</span></div>
      </div>
    </section>

    <!-- ── Charts Row ───────────────────────────────────── -->
    <section class="charts-row">
      <div class="panel">
        <h2 class="panel-title">Detection Trend — 12 Months (2025)</h2>
        <VChart class="chart-main" :option="trendChartOption" :autoresize="true" />
      </div>
      <div class="panel">
        <h2 class="panel-title">Resistance by Antibiotic Class</h2>
        <VChart class="chart-classes" :option="classChartOption" :autoresize="true" />
      </div>
    </section>

    <!-- ── Antibiotic Resistance Profile ────────────────── -->
    <section class="panel">
      <div class="panel-header-row">
        <h2 class="panel-title">Antibiotic Resistance Profile</h2>
        <div class="profile-legend">
          <span class="legend-dot legend-r">R = Resistant</span>
          <span class="legend-dot legend-i">I = Intermediate</span>
          <span class="legend-dot legend-s">S = Susceptible</span>
        </div>
      </div>
      <ResistanceProfileGrid :entries="organism.resistanceProfile" />
    </section>

    <!-- ── AMR Genes Table ──────────────────────────────── -->
    <section class="panel">
      <div class="panel-header-row">
        <h2 class="panel-title">AMR Resistance Genes</h2>
        <span class="panel-subtitle"
          >AMRFinderPlus · {{ organism.genes.length }} genes detected</span
        >
      </div>
      <DataTable :value="organism.genes" class="amr-table" size="small">
        <Column field="geneSymbol" header="Gene Symbol">
          <template #body="{ data }">
            <code class="gene-name">{{ data.geneSymbol }}</code>
          </template>
        </Column>
        <Column field="resistanceClass" header="Class">
          <template #body="{ data }">
            <Tag :value="data.resistanceClass" :severity="classSeverity(data.resistanceClass)" />
          </template>
        </Column>
        <Column field="subclass" header="Subclass">
          <template #body="{ data }">
            <span class="subclass-text">{{ data.subclass }}</span>
          </template>
        </Column>
        <Column field="elementType" header="Type" style="width: 64px">
          <template #body="{ data }">
            <span class="type-text">{{ data.elementType }}</span>
          </template>
        </Column>
        <Column field="occurrenceCount" header="Occurrences" style="width: 90px; text-align: right">
          <template #body="{ data }">
            <span class="num-cell">{{ data.occurrenceCount }}</span>
          </template>
        </Column>
        <Column
          field="avgIdentityPct"
          header="Avg Identity %"
          style="width: 110px; text-align: right"
        >
          <template #body="{ data }">
            <span :class="data.avgIdentityPct >= 99 ? 'identity-high' : 'identity-cell'">
              {{ data.avgIdentityPct.toFixed(1) }}%
            </span>
          </template>
        </Column>
        <Column
          field="avgCoveragePct"
          header="Avg Coverage %"
          style="width: 110px; text-align: right"
        >
          <template #body="{ data }">
            <span class="identity-cell">{{ data.avgCoveragePct.toFixed(1) }}%</span>
          </template>
        </Column>
      </DataTable>
    </section>

    <!-- ── Bottom Row: Sites + WGS ──────────────────────── -->
    <section class="bottom-row">
      <div class="panel">
        <div class="panel-header-row">
          <h2 class="panel-title">Affected River Sites</h2>
          <span class="panel-subtitle">Epicollect · water quality parameters</span>
        </div>
        <DataTable :value="organism.sites" class="amr-table" size="small">
          <Column field="siteId" header="Site" style="width: 52px">
            <template #body="{ data }">
              <span class="site-id">{{ data.siteId }}</span>
            </template>
          </Column>
          <Column field="river" header="River"></Column>
          <Column field="province" header="Province"></Column>
          <Column field="isolates" header="Isolates" style="width: 64px; text-align: right">
            <template #body="{ data }">
              <span class="num-cell">{{ data.isolates }}</span>
            </template>
          </Column>
          <Column field="ph" header="pH" style="width: 52px; text-align: right">
            <template #body="{ data }">
              <span class="param-cell">{{ data.ph.toFixed(1) }}</span>
            </template>
          </Column>
          <Column field="tds" header="TDS" style="width: 60px; text-align: right">
            <template #body="{ data }">
              <span class="param-cell">{{ data.tds }}</span>
            </template>
          </Column>
          <Column field="temp" header="°C" style="width: 52px; text-align: right">
            <template #body="{ data }">
              <span class="param-cell">{{ data.temp.toFixed(1) }}</span>
            </template>
          </Column>
          <Column field="dissolvedOxygen" header="DO" style="width: 52px; text-align: right">
            <template #body="{ data }">
              <span class="param-cell">{{ data.dissolvedOxygen.toFixed(1) }}</span>
            </template>
          </Column>
          <Column field="risk" header="Risk" style="width: 64px">
            <template #body="{ data }">
              <Tag :value="data.risk" :severity="riskSeverity(data.risk)" />
            </template>
          </Column>
        </DataTable>
        <p class="table-note">TDS = Total Dissolved Solids (mg/L) · DO = Dissolved Oxygen (mg/L)</p>
      </div>

      <div class="panel">
        <div class="panel-header-row">
          <h2 class="panel-title">WGS / Genomic Metrics</h2>
          <span class="panel-subtitle">StarAMR · MLST</span>
        </div>
        <WgsMetricsPanel
          :wgs="organism.wgs"
          :r-count="rCount"
          :i-count="iCount"
          :s-count="sCount"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
/* ── Shell ─────────────────────────────────────────── */
.bacteria-detail {
  padding: 0 28px 48px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ── Header ────────────────────────────────────────── */
.detail-header {
  padding: 20px 0 12px;
  border-bottom: 1px solid var(--c-border);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 7px;
  background: none;
  border: 1px solid var(--c-border);
  border-radius: 6px;
  color: var(--c-text-muted);
  font-family: 'DM Sans', sans-serif;
  font-size: 12px;
  padding: 5px 12px;
  cursor: pointer;
  width: fit-content;
  transition:
    background 0.15s,
    color 0.15s,
    border-color 0.15s;
}

.back-btn:hover {
  background: var(--c-brand-dim);
  color: var(--c-brand);
  border-color: var(--c-brand);
}

.back-btn .pi {
  font-size: 11px;
}

.header-identity {
  opacity: 0;
  transform: translateY(8px);
  transition:
    opacity 0.4s ease,
    transform 0.4s ease;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.header-identity--visible {
  opacity: 1;
  transform: translateY(0);
}

.organism-name-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.organism-name {
  font-family: 'DM Sans', sans-serif;
  font-size: 26px;
  font-weight: 500;
  font-style: italic;
  color: var(--c-heading);
  letter-spacing: -0.3px;
}

.mdro-badge {
  font-size: 12px !important;
  padding: 3px 10px !important;
}

.organism-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.meta-pill {
  font-size: 11px;
  font-weight: 500;
  color: var(--c-text-muted);
  background: var(--c-brand-dim);
  border: 1px solid var(--c-border);
  border-radius: 20px;
  padding: 2px 10px;
}

.meta-pill code {
  font-family: 'DM Mono', monospace;
  font-size: 11px;
  color: var(--c-brand);
}

.gram-pill {
  background: var(--c-green-dim);
  color: var(--c-green);
  border-color: var(--c-green);
  opacity: 0.8;
}

.organism-desc {
  font-size: 13px;
  line-height: 1.65;
  color: var(--c-text-muted);
  max-width: 900px;
}

/* ── KPI Row ───────────────────────────────────────── */
.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.kpi-card {
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: 8px;
  padding: 16px 18px 14px;
  box-shadow: var(--c-shadow);
  opacity: 0;
  transform: translateY(10px);
  transition:
    opacity 0.35s ease,
    transform 0.35s ease;
}

.kpi-card--visible {
  opacity: 1;
  transform: translateY(0);
}

.kpi-label {
  font-size: 9.5px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--c-text-muted);
  margin-bottom: 8px;
}

.kpi-value {
  font-family: 'Inter', sans-serif;
  font-size: 32px;
  font-weight: 400;
  color: var(--c-heading);
  line-height: 1;
  letter-spacing: -1px;
  margin-bottom: 6px;
}

.kpi-value--blue {
  color: var(--c-brand);
}
.kpi-value--red {
  color: var(--c-red);
}

.kpi-sub {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10.5px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.kpi-sub .pi {
  font-size: 10px;
}

.trend-danger {
  color: var(--c-red);
}
.trend-success {
  color: var(--c-green);
}
.trend-muted {
  color: var(--c-text-dim);
}

.kpi-ris {
  display: flex;
  gap: 6px;
  margin-bottom: 6px;
}

.ris-pill {
  font-family: 'DM Mono', monospace;
  font-size: 13px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 5px;
}

.ris-r {
  background: var(--c-red-dim);
  color: var(--c-red);
}
.ris-i {
  background: var(--c-amber-dim);
  color: var(--c-amber);
}
.ris-s {
  background: var(--c-green-dim);
  color: var(--c-green);
}

/* ── Panels ────────────────────────────────────────── */
.panel {
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: 8px;
  padding: 18px 20px;
  box-shadow: var(--c-shadow);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.panel-header-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.panel-title {
  font-family: 'DM Sans', sans-serif;
  font-size: 13px;
  font-weight: 600;
  color: var(--c-heading);
}

.panel-subtitle {
  font-size: 10.5px;
  color: var(--c-text-dim);
  white-space: nowrap;
}

/* ── Charts Row ────────────────────────────────────── */
.charts-row {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 14px;
}

.chart-main {
  height: 210px;
  width: 100%;
}
.chart-classes {
  height: 210px;
  width: 100%;
}

/* ── Resistance legend ─────────────────────────────── */
.profile-legend {
  display: flex;
  gap: 14px;
}

.legend-dot {
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.04em;
}
.legend-r {
  color: var(--c-red);
}
.legend-i {
  color: var(--c-amber);
}
.legend-s {
  color: var(--c-green);
}

/* ── AMR Genes Table ───────────────────────────────── */
.amr-table {
  font-family: 'DM Sans', sans-serif;
}

.gene-name {
  font-family: 'DM Mono', 'JetBrains Mono', monospace;
  font-size: 12px;
  color: var(--c-brand);
  background: var(--c-brand-dim);
  padding: 1px 6px;
  border-radius: 3px;
}

.subclass-text {
  font-size: 11.5px;
  color: var(--c-text-muted);
  text-transform: capitalize;
  font-style: italic;
}

.type-text {
  font-size: 11px;
  font-weight: 600;
  color: var(--c-text-dim);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.num-cell {
  display: block;
  text-align: right;
  font-size: 13px;
  font-weight: 600;
  color: var(--c-heading);
  font-family: 'DM Mono', monospace;
}

.identity-cell {
  display: block;
  text-align: right;
  font-size: 12px;
  color: var(--c-text-muted);
  font-family: 'DM Mono', monospace;
}

.identity-high {
  display: block;
  text-align: right;
  font-size: 12px;
  font-weight: 600;
  color: var(--c-green);
  font-family: 'DM Mono', monospace;
}

/* ── Bottom Row ────────────────────────────────────── */
.bottom-row {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 14px;
}

.site-id {
  font-family: 'DM Mono', monospace;
  font-size: 12px;
  font-weight: 600;
  color: var(--c-brand);
  background: var(--c-brand-dim);
  padding: 1px 6px;
  border-radius: 3px;
}

.param-cell {
  display: block;
  text-align: right;
  font-family: 'DM Mono', monospace;
  font-size: 11.5px;
  color: var(--c-text-muted);
}

.table-note {
  font-size: 10px;
  color: var(--c-text-dim);
  margin-top: -4px;
}

/* ── Responsive ────────────────────────────────────── */
@media (max-width: 1100px) {
  .kpi-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .charts-row {
    grid-template-columns: 1fr;
  }
  .bottom-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .bacteria-detail {
    padding: 0 14px 32px;
  }
  .kpi-row {
    grid-template-columns: 1fr 1fr;
  }
}

/* ── Breadcrumb ────────────────────────────────────── */
.breadcrumb-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
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

.crumb-current {
  color: var(--c-text-muted);
}

.crumb-sep {
  font-size: 9px;
}
</style>
