<script setup lang="ts">
/* ── Imports ─────────────────────────────────────────────────── */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import VChart from 'vue-echarts'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'

import StatCard from '@/components/amr/StatCard.vue'
import { useChartTheme } from '@/composables/useChartTheme'
import { useDashboardStore } from '@/stores/dashboard'
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
import type { RiskLevel } from '@/types/amr'

/* ── Store — live data, falls back to mock when API is unavailable */
const store = useDashboardStore()

// When the DB has no isolate data (e.g. only Epicollect was uploaded),
// all resistance-related panels show the static mockdata-derived values
// instead of misleading zeros returned by the live API.
const displayStatCards = computed(() =>
  store.statCards && store.hasIsolateData ? store.statCards : STAT_CARDS
)
const displayProvinces = computed(() =>
  store.hasIsolateData ? (store.provinces ?? PROVINCES) : PROVINCES
)
const displayOrganisms = computed(() =>
  store.hasIsolateData ? (store.topOrganisms ?? TOP_ORGANISMS) : TOP_ORGANISMS
)
const displayResistanceGenes = computed(() =>
  store.hasIsolateData ? (store.resistanceGenes ?? RESISTANCE_GENES) : RESISTANCE_GENES
)
const displayRiverSites = computed(() =>
  store.hasIsolateData ? (store.affectedRiverSites ?? RIVER_SITES) : RIVER_SITES
)
const displayMonths = computed(() =>
  store.hasIsolateData && store.months.length ? store.months : MONTHS
)
const displayNormal = computed(() =>
  store.hasIsolateData && store.monthlyNormal.length ? store.monthlyNormal : MONTHLY_NORMAL
)
const displayAlert = computed(() =>
  store.hasIsolateData && store.monthlyAlert.length ? store.monthlyAlert : MONTHLY_ALERT
)

/* ── State ───────────────────────────────────────────────────── */
const router = useRouter()
const visible = ref(false)
onMounted(async () => {
  await store.fetchAll()
  setTimeout(() => { visible.value = true }, 60)
})

/* ── Chart theme ─────────────────────────────────────────────── */
const { isDark, tooltipBase, axisLabel, splitLine, axisLine, blue, blueHover, red, redHover } =
  useChartTheme()

/* ── Helpers ─────────────────────────────────────────────────── */
function riskColor(risk: RiskLevel): string {
  if (risk === 'HIGH') return 'var(--c-risk-high)'
  if (risk === 'MED') return 'var(--c-risk-med)'
  return 'var(--c-risk-low)'
}

function riskBadgeClass(risk: RiskLevel): string {
  if (risk === 'HIGH') return 'badge-high'
  if (risk === 'MED') return 'badge-med'
  return 'badge-low'
}

function classSeverity(cls: string): 'danger' | 'warn' | 'secondary' | 'info' {
  if (cls === 'BETA-LACTAM' || cls === 'COLISTIN' || cls === 'CARBAPENEM') return 'danger'
  if (cls === 'AMINOGLYCOSIDE' || cls === 'QUINOLONE') return 'warn'
  if (cls === 'TETRACYCLINE' || cls === 'PHENICOL' || cls === 'TRIMETHOPRIM' || cls === 'SULFONAMIDE') return 'secondary'
  return 'info'  // MACROLIDE, GLYCOPEPTIDE, etc.
}

function riskSeverity(r: RiskLevel): 'danger' | 'warn' | 'success' {
  if (r === 'HIGH') return 'danger'
  if (r === 'MED') return 'warn'
  return 'success'
}

function arCodeSeverity(code: string): 'danger' | 'warn' | 'secondary' {
  if (code === 'CRE' || code === 'VRE') return 'danger'
  if (code === 'ESBL' || code === 'MDR' || code === 'MDRO') return 'warn'
  return 'secondary'
}

function trendIcon(t: string): string {
  if (t === 'up') return 'pi pi-arrow-up'
  if (t === 'down') return 'pi pi-arrow-down'
  return 'pi pi-minus'
}

function trendStyle(t: string): Record<string, string> {
  if (t === 'up') return { color: 'var(--c-red)' }
  if (t === 'down') return { color: 'var(--c-green)' }
  return { color: 'var(--c-text-dim)' }
}

/* ── Navigation ──────────────────────────────────────────────── */
function goToBacteria(name: string): void {
  router.push({ name: 'bacteria-detail', params: { name } })
}

function onChartClick(params: Record<string, unknown>): void {
  if (params?.componentType === 'series') {
    goToBacteria('Escherichia coli')
  }
}

/* ── ECharts option ──────────────────────────────────────────── */
const chartOption = computed(() => ({
  backgroundColor: 'transparent',
  animation: true,
  animationDuration: 800,
  animationEasing: 'cubicOut' as const,
  grid: { left: 48, right: 16, top: 16, bottom: 36 },
  tooltip: {
    trigger: 'axis' as const,
    ...tooltipBase.value,
    formatter: (params: Array<{ name: string; color: string; value: number | null }>) => {
      const p = params.find((x) => x.value != null)
      if (!p) return ''
      return `<div style="font-family:DM Sans,sans-serif;font-size:12px">
        <strong>${p.name}</strong><br/>
        <span style="color:${p.color}">●</span> Isolates: <strong>${p.value}</strong>
      </div>`
    },
  },
  xAxis: {
    type: 'category' as const,
    data: displayMonths.value,
    axisTick: { show: false },
    axisLine: axisLine.value,
    axisLabel: axisLabel.value,
  },
  yAxis: {
    type: 'value' as const,
    min: 0,
    splitLine: splitLine.value,
    axisLabel: axisLabel.value,
    axisLine: { show: false },
    axisTick: { show: false },
  },
  series: [
    {
      name: 'Normal',
      type: 'bar' as const,
      data: displayNormal.value,
      barMaxWidth: 28,
      itemStyle: { color: blue.value, borderRadius: [3, 3, 0, 0] },
      emphasis: { itemStyle: { color: blueHover.value } },
    },
    {
      name: 'Elevated',
      type: 'bar' as const,
      data: displayAlert.value,
      barMaxWidth: 28,
      itemStyle: { color: red.value, borderRadius: [3, 3, 0, 0] },
      emphasis: { itemStyle: { color: redHover.value } },
    },
  ],
  legend: {
    show: true,
    bottom: 0,
    itemWidth: 10,
    itemHeight: 10,
    borderRadius: 2,
    textStyle: {
      color: isDark.value ? '#5C7A94' : '#9CA3AF',
      fontFamily: 'DM Sans, sans-serif',
      fontSize: 11,
    },
  },
}))
</script>

<template>
  <div class="dashboard">
    <!-- Page Title -->
    <div class="page-header">
      <h1 class="page-title">Dashboard</h1>
    </div>

    <!-- Stat Cards -->
    <section class="stats-row">
      <StatCard
        v-for="(stat, i) in displayStatCards"
        :key="stat.id"
        :label="stat.label"
        :value="stat.value"
        :trend-text="stat.trendText"
        :trend-icon="stat.trendIcon"
        :value-class="stat.valueClass"
        :trend-class="stat.trendClass"
        :visible="visible"
        :delay="i * 55"
      />
    </section>

    <!-- Middle Row: Chart + Province Risk -->
    <section class="mid-row">
      <div class="panel panel--chart">
        <div class="panel-header">
          <h2 class="panel-title">AMR Detections by Month</h2>
          <div class="chart-header-right">
            <div class="year-toggle" role="group" aria-label="Select year">
              <button
                v-for="y in store.availableYears"
                :key="y"
                class="year-btn"
                :class="{ 'year-btn--active': store.selectedYear === y }"
                :disabled="store.trendLoading"
                @click="store.selectYear(y)"
              >{{ y }}</button>
            </div>
            <span class="panel-subtitle">Click bar to view organism detail</span>
          </div>
        </div>
        <VChart
          class="echart"
          :class="{ 'echart--loading': store.trendLoading }"
          :option="chartOption"
          :autoresize="true"
          style="cursor: pointer"
          @click="onChartClick"
        />
      </div>

      <div class="panel panel--province">
        <h2 class="panel-title">Risk by Province</h2>
        <div class="province-list">
          <div v-for="p in displayProvinces" :key="p.name" class="province-row">
            <span class="province-name">{{ p.name }}</span>
            <div class="province-bar-track">
              <div
                class="province-bar-fill"
                :style="{ width: `${p.percent}%`, background: riskColor(p.risk) }"
              ></div>
            </div>
            <span class="province-badge" :class="riskBadgeClass(p.risk)">{{ p.risk }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Top Detected Organisms — R1.4 — clickable rows → Bacteria Detail -->
    <section>
      <div class="panel">
        <div class="panel-header">
          <h2 class="panel-title">Top Detected Organisms</h2>
          <span class="panel-subtitle">Click any row to view full AMR resistance profile →</span>
        </div>
        <DataTable
          :value="displayOrganisms"
          class="amr-table organisms-table"
          size="small"
          @row-click="(e: { data: { name: string } }) => goToBacteria(e.data.name)"
        >
          <Column field="name" header="Organism">
            <template #body="{ data }">
              <em class="org-name">{{ data.name }}</em>
            </template>
          </Column>
          <Column field="arCode" header="MDRO Type" style="width: 100px">
            <template #body="{ data }">
              <Tag :value="data.arCode" :severity="arCodeSeverity(data.arCode)" />
            </template>
          </Column>
          <Column field="detectionCount" header="Detections" style="width: 96px; text-align: right">
            <template #body="{ data }">
              <span class="num-cell">{{ data.detectionCount.toLocaleString() }}</span>
            </template>
          </Column>
          <Column field="siteCount" header="Sites" style="width: 64px; text-align: right">
            <template #body="{ data }">
              <span class="num-cell">{{ data.siteCount }}</span>
            </template>
          </Column>
          <Column field="resistanceRate" header="MDRO Rate" style="width: 90px; text-align: right">
            <template #body="{ data }">
              <span class="identity-cell">{{ data.resistanceRate }}%</span>
            </template>
          </Column>
          <Column field="yoyTrend" header="YoY" style="width: 60px; text-align: center">
            <template #body="{ data }">
              <i
                :class="trendIcon(data.yoyTrend)"
                :style="trendStyle(data.yoyTrend)"
                style="font-size: 13px"
              ></i>
            </template>
          </Column>
          <Column header="" style="width: 36px; text-align: right">
            <template #body>
              <i class="pi pi-angle-right" style="color: var(--c-text-dim); font-size: 12px"></i>
            </template>
          </Column>
        </DataTable>
      </div>
    </section>

    <!-- Bottom Row: Resistance Genes + River Sites -->
    <section class="bottom-row">
      <!-- Top Resistance Genes — AMRFinderPlus data -->
      <div class="panel">
        <div class="panel-header">
          <h2 class="panel-title">Top Resistance Genes</h2>
          <span class="panel-subtitle">AMRFinderPlus · by isolate count</span>
        </div>
        <DataTable :value="displayResistanceGenes" class="amr-table" size="small">
          <Column field="gene" header="Gene Symbol">
            <template #body="{ data }">
              <code class="gene-name">{{ data.gene }}</code>
            </template>
          </Column>
          <Column field="resistanceClass" header="Class">
            <template #body="{ data }">
              <Tag
                :value="data.resistanceClass"
                :severity="classSeverity(data.resistanceClass)"
                class="class-tag"
              />
            </template>
          </Column>
          <Column field="subclass" header="Subclass">
            <template #body="{ data }">
              <span class="subclass-text">{{ data.subclass }}</span>
            </template>
          </Column>
          <Column field="isolates" header="Isolates" style="width: 72px; text-align: right">
            <template #body="{ data }">
              <span class="num-cell">{{ data.isolates }}</span>
            </template>
          </Column>
          <Column field="identity" header="Avg. ID%" style="width: 72px; text-align: right">
            <template #body="{ data }">
              <span class="identity-cell">{{ data.identity.toFixed(1) }}%</span>
            </template>
          </Column>
        </DataTable>
      </div>

      <!-- Affected River Sites — Epicollect data -->
      <div class="panel">
        <div class="panel-header">
          <h2 class="panel-title">Affected River Sites</h2>
          <span class="panel-subtitle">Epicollect · South African rivers</span>
        </div>
        <DataTable :value="displayRiverSites" class="amr-table" size="small">
          <Column field="siteId" header="Site" style="width: 56px">
            <template #body="{ data }">
              <span class="site-id">{{ data.siteId }}</span>
            </template>
          </Column>
          <Column field="river" header="River"></Column>
          <Column field="province" header="Province"></Column>
          <Column field="lastSampled" header="Last Sampled" style="width: 110px"></Column>
          <Column field="isolates" header="Isolates" style="width: 64px; text-align: right">
            <template #body="{ data }">
              <span class="num-cell">{{ data.isolates }}</span>
            </template>
          </Column>
          <Column field="risk" header="Risk" style="width: 68px">
            <template #body="{ data }">
              <Tag :value="data.risk" :severity="riskSeverity(data.risk)" class="risk-tag" />
            </template>
          </Column>
        </DataTable>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* ── Shell ─────────────────────────────────────────── */
.dashboard {
  padding: 0 28px 40px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* ── Header ────────────────────────────────────────── */
.page-header {
  padding: 22px 0 14px;
  text-align: center;
  border-bottom: 1px solid var(--c-border);
}

.page-title {
  font-family: 'DM Sans', sans-serif;
  font-size: 18px;
  font-weight: 400;
  color: var(--c-heading);
  letter-spacing: 0.01em;
}

/* ── Stat Cards ────────────────────────────────────── */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

/* ── Panels ────────────────────────────────────────── */
.mid-row {
  display: grid;
  grid-template-columns: 1.45fr 1fr;
  gap: 14px;
}

.bottom-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.panel {
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: 8px;
  padding: 18px 20px;
  box-shadow: var(--c-shadow);
  display: flex;
  flex-direction: column;
  gap: 14px;
  transition:
    background 0.2s ease,
    border-color 0.2s ease;
}

.panel-header {
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
  font-weight: 400;
  white-space: nowrap;
}

/* ── Chart header right slot ───────────────────────────────── */
.chart-header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 5px;
}

.year-toggle {
  display: flex;
  gap: 3px;
}

.year-btn {
  font-family: 'DM Sans', sans-serif;
  font-size: 11px;
  font-weight: 500;
  padding: 3px 10px;
  border-radius: 4px;
  border: 1px solid var(--c-border);
  background: transparent;
  color: var(--c-text-dim);
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
  line-height: 1.6;
}

.year-btn:hover:not(:disabled) {
  background: var(--c-brand-dim);
  border-color: var(--c-brand);
  color: var(--c-brand);
}

.year-btn--active {
  background: var(--c-brand-dim) !important;
  border-color: var(--c-brand) !important;
  color: var(--c-brand) !important;
  font-weight: 600;
}

.year-btn:disabled {
  opacity: 0.45;
  cursor: default;
}

.echart--loading {
  opacity: 0.5;
  pointer-events: none;
  transition: opacity 0.2s;
}

/* ── ECharts ───────────────────────────────────────── */
.echart {
  height: 220px;
  width: 100%;
}

/* ── Province Risk ─────────────────────────────────── */
.province-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.province-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.province-name {
  font-size: 12px;
  color: var(--c-text);
  flex-shrink: 0;
  width: 110px;
}

.province-bar-track {
  flex: 1;
  height: 6px;
  background: var(--c-border);
  border-radius: 99px;
  overflow: hidden;
}

.province-bar-fill {
  height: 100%;
  border-radius: 99px;
  transition: width 1s cubic-bezier(0.22, 1, 0.36, 1);
}

.province-badge {
  flex-shrink: 0;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.07em;
  padding: 2px 7px;
  border-radius: 4px;
  border: 1px solid;
  width: 36px;
  text-align: center;
}

.badge-high {
  color: var(--c-risk-high);
  border-color: var(--c-risk-high);
  background: var(--c-red-dim);
}
.badge-med {
  color: var(--c-risk-med);
  border-color: var(--c-risk-med);
  background: var(--c-amber-dim);
}
.badge-low {
  color: var(--c-risk-low);
  border-color: var(--c-risk-low);
  background: var(--c-green-dim);
}

/* ── Tables ────────────────────────────────────────── */
.amr-table {
  font-family: 'DM Sans', sans-serif;
}

.organisms-table :deep(tr) {
  cursor: pointer !important;
}

.org-name {
  font-style: italic;
  font-size: 13px;
  color: var(--c-heading);
  font-weight: 500;
}

.gene-name {
  font-family: 'DM Mono', 'JetBrains Mono', monospace;
  font-size: 12px;
  color: var(--c-brand);
  background: var(--c-brand-dim);
  padding: 1px 6px;
  border-radius: 3px;
}

.class-tag {
  font-size: 9.5px !important;
  font-weight: 700 !important;
}

.subclass-text {
  font-size: 11.5px;
  color: var(--c-text-muted);
  text-transform: capitalize;
  font-style: italic;
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

.site-id {
  font-family: 'DM Mono', monospace;
  font-size: 12px;
  font-weight: 600;
  color: var(--c-brand);
  background: var(--c-brand-dim);
  padding: 1px 6px;
  border-radius: 3px;
}

.risk-tag {
  font-size: 9.5px !important;
  font-weight: 700 !important;
}

/* ── Responsive ────────────────────────────────────── */
@media (max-width: 1080px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .mid-row {
    grid-template-columns: 1fr;
  }
  .bottom-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .dashboard {
    padding: 0 14px 32px;
  }
  .stats-row {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
