<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import { useThemeStore } from '@/stores/theme'

use([CanvasRenderer, BarChart, GridComponent, TooltipComponent, LegendComponent])

const themeStore = useThemeStore()

/* ── Mock data — shaped to match actual schema ─────────────────────
   Sources:
   • Stat cards  → Epicollect (sites), Binary_Information (isolates, ESBL), AMRFinderPlus (genes)
   • Bar chart   → isolates detected per month (Binary_Information × Epicollect dates)
   • Risk by Province → derived from Epicollect site locations + Binary_Information AR codes
   • Top Resistance Genes → AMRFinderPlus_Results (Gene Symbol, Class, Subclass, Identity %)
   • Affected River Sites → Epicollect (Site ID, River Name, Location Name, Date)
─────────────────────────────────────────────────────────────────── */

const stats = [
  {
    id: 'esbl',
    label: 'ESBL Prevalence Rate',
    value: '38.7%',
    trendIcon: 'pi-sort-up-fill',
    trendText: '0.2% from 2025',
    valueClass: '',
    trendClass: 'trend-danger',
  },
  {
    id: 'isolates',
    label: 'Total Isolates Analysed',
    value: '4 812',
    trendIcon: null,
    trendText: 'across 18 sites',
    valueClass: '',
    trendClass: 'trend-muted',
  },
  {
    id: 'sites',
    label: 'Sampling Sites Active',
    value: '18',
    trendIcon: null,
    trendText: '2 new this campaign',
    valueClass: '',
    trendClass: 'trend-muted',
  },
  {
    id: 'genes',
    label: 'Resistance Genes Detected',
    value: '47',
    trendIcon: 'pi-sort-down-fill',
    trendText: '3 fewer than last trip',
    valueClass: 'value-blue',
    trendClass: 'trend-success',
  },
]

/* Monthly isolate detections — from Epicollect dates × Binary_Information */
const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
const normalVals = [210, 240, 268, 290, 318, 352, 401, 438, 490, null, null, null]
const alertVals  = [null, null, null, null, null, null, null, null, null, 562, 638, 714]

/* Province risk — derived from Epicollect site lat/lng mapped to province */
const provinces = [
  { name: 'Gauteng',        risk: 'HIGH', percent: 85 },
  { name: 'KwaZulu-Natal',  risk: 'HIGH', percent: 78 },
  { name: 'Western Cape',   risk: 'MED',  percent: 52 },
  { name: 'Eastern Cape',   risk: 'MED',  percent: 45 },
  { name: 'Limpopo',        risk: 'MED',  percent: 38 },
  { name: 'Mpumalanga',     risk: 'LOW',  percent: 22 },
  { name: 'Free State',     risk: 'LOW',  percent: 18 },
]

/* Top Resistance Genes — from AMRFinderPlus (Gene Symbol, Class, Subclass, % Identity) */
const resistanceGenes = ref([
  { gene: 'blaCTX-M-14',   resistanceClass: 'BETA-LACTAM',     subclass: 'CEPHALOSPORIN',  isolates: 28, identity: 99.8 },
  { gene: 'blaTEM-1B',     resistanceClass: 'BETA-LACTAM',     subclass: 'BETA-LACTAM',    isolates: 22, identity: 100.0 },
  { gene: "aph(3')-Ia",    resistanceClass: 'AMINOGLYCOSIDE',  subclass: 'KANAMYCIN',      isolates: 18, identity: 100.0 },
  { gene: 'tet(A)',         resistanceClass: 'TETRACYCLINE',    subclass: 'TETRACYCLINE',   isolates: 15, identity: 97.2 },
  { gene: 'erm',            resistanceClass: 'MACROLIDE',       subclass: 'MACROLIDE',      isolates: 11, identity: 87.5 },
])

/* Affected River Sites — from Epicollect (Site ID, River Name, Location, Date, Isolate count) */
const riverSites = ref([
  { siteId: 'A10', river: 'Apies River',    location: 'Farm A Dispatch',       province: 'Gauteng',       lastSampled: 'May 10, 2025', isolates: 14, risk: 'HIGH' },
  { siteId: 'B26', river: 'Apies River',    location: 'Farm B Pivot 1',        province: 'Gauteng',       lastSampled: 'May 10, 2025', isolates: 9,  risk: 'MED'  },
  { siteId: 'T08', river: 'Tugela River',   location: 'Midlands Site T',       province: 'KwaZulu-Natal', lastSampled: 'Jul 15, 2025', isolates: 12, risk: 'HIGH' },
  { siteId: 'B05', river: 'Breede River',   location: 'Breede Valley Station', province: 'Western Cape',  lastSampled: 'Mar 12, 2025', isolates: 6,  risk: 'MED'  },
  { siteId: 'L04', river: 'Limpopo River',  location: 'Limpopo Crossing L4',   province: 'Limpopo',       lastSampled: 'Feb 28, 2025', isolates: 8,  risk: 'MED'  },
])

/* ── Helpers ─────────────────────────────────────────────────── */

function riskColor(risk: string) {
  if (risk === 'HIGH') return 'var(--c-risk-high)'
  if (risk === 'MED')  return 'var(--c-risk-med)'
  return 'var(--c-risk-low)'
}

function riskBadgeClass(risk: string) {
  if (risk === 'HIGH') return 'badge-high'
  if (risk === 'MED')  return 'badge-med'
  return 'badge-low'
}

function classSeverity(cls: string): 'danger' | 'warn' | 'secondary' | 'info' {
  if (cls === 'BETA-LACTAM')    return 'danger'
  if (cls === 'AMINOGLYCOSIDE') return 'warn'
  if (cls === 'TETRACYCLINE')   return 'secondary'
  return 'info'
}

function riskSeverity(r: string): 'danger' | 'warn' | 'success' {
  if (r === 'HIGH') return 'danger'
  if (r === 'MED')  return 'warn'
  return 'success'
}

/* ── ECharts ─────────────────────────────────────────────────── */

const isDark = computed(() => themeStore.resolvedTheme === 'dark')

const chartOption = computed(() => ({
  backgroundColor: 'transparent',
  animation: true,
  animationDuration: 800,
  animationEasing: 'cubicOut' as const,
  grid: { left: 48, right: 16, top: 16, bottom: 36 },
  tooltip: {
    trigger: 'axis' as const,
    backgroundColor: isDark.value ? '#1A2D42' : '#FFFFFF',
    borderColor: isDark.value ? 'rgba(255,255,255,0.12)' : '#E5E7EB',
    borderWidth: 1,
    textStyle: {
      color: isDark.value ? '#C9D5E2' : '#111827',
      fontFamily: 'DM Sans, sans-serif',
      fontSize: 12,
    },
    formatter: (params: any[]) => {
      const p = params.find((x: any) => x.value != null)
      if (!p) return ''
      return `<div style="font-family:DM Sans,sans-serif;font-size:12px">
        <strong>${p.name}</strong><br/>
        <span style="color:${p.color}">●</span> Isolates: <strong>${p.value}</strong>
      </div>`
    },
  },
  xAxis: {
    type: 'category' as const,
    data: months,
    axisTick: { show: false },
    axisLine: { lineStyle: { color: isDark.value ? 'rgba(255,255,255,0.08)' : '#E5E7EB' } },
    axisLabel: { color: isDark.value ? '#4B6174' : '#9CA3AF', fontFamily: 'DM Sans, sans-serif', fontSize: 11 },
  },
  yAxis: {
    type: 'value' as const,
    min: 0,
    max: 800,
    interval: 200,
    splitLine: { lineStyle: { color: isDark.value ? 'rgba(255,255,255,0.06)' : '#F3F4F6', type: 'solid' as const } },
    axisLabel: { color: isDark.value ? '#4B6174' : '#9CA3AF', fontFamily: 'DM Sans, sans-serif', fontSize: 11 },
    axisLine: { show: false },
    axisTick: { show: false },
  },
  series: [
    {
      name: 'Normal',
      type: 'bar' as const,
      data: normalVals,
      barMaxWidth: 28,
      itemStyle: { color: isDark.value ? '#3B82F6' : '#1565C0', borderRadius: [3, 3, 0, 0] },
      emphasis: { itemStyle: { color: isDark.value ? '#60A5FA' : '#1976D2' } },
    },
    {
      name: 'Elevated',
      type: 'bar' as const,
      data: alertVals,
      barMaxWidth: 28,
      itemStyle: { color: isDark.value ? '#EF4444' : '#C62828', borderRadius: [3, 3, 0, 0] },
      emphasis: { itemStyle: { color: isDark.value ? '#F87171' : '#E53935' } },
    },
  ],
  legend: {
    show: true,
    bottom: 0,
    itemWidth: 10,
    itemHeight: 10,
    borderRadius: 2,
    textStyle: { color: isDark.value ? '#5C7A94' : '#9CA3AF', fontFamily: 'DM Sans, sans-serif', fontSize: 11 },
  },
}))

/* ── Animations ──────────────────────────────────────────────── */
const visible = ref(false)
onMounted(() => { setTimeout(() => { visible.value = true }, 60) })
</script>

<template>
  <div class="dashboard">
    <!-- Page Title -->
    <div class="page-header">
      <h1 class="page-title">Dashboard</h1>
    </div>

    <!-- Stat Cards -->
    <section class="stats-row">
      <div
        v-for="(stat, i) in stats"
        :key="stat.id"
        class="stat-card"
        :class="{ 'stat-card--visible': visible }"
        :style="{ transitionDelay: `${i * 55}ms` }"
      >
        <p class="stat-label">{{ stat.label }}</p>
        <p class="stat-value" :class="stat.valueClass">{{ stat.value }}</p>
        <div class="stat-trend" :class="stat.trendClass">
          <i v-if="stat.trendIcon" :class="`pi ${stat.trendIcon}`"></i>
          <span>{{ stat.trendText }}</span>
        </div>
      </div>
    </section>

    <!-- Middle Row: Chart + Province Risk -->
    <section class="mid-row">
      <div class="panel panel--chart">
        <h2 class="panel-title">AMR Detections by Month</h2>
        <VChart class="echart" :option="chartOption" :autoresize="true" />
      </div>

      <div class="panel panel--province">
        <h2 class="panel-title">Risk by Province</h2>
        <div class="province-list">
          <div v-for="p in provinces" :key="p.name" class="province-row">
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

    <!-- Bottom Row: Resistance Genes + River Sites -->
    <section class="bottom-row">
      <!-- Top Resistance Genes — AMRFinderPlus data -->
      <div class="panel">
        <div class="panel-header">
          <h2 class="panel-title">Top Resistance Genes</h2>
          <span class="panel-subtitle">AMRFinderPlus · by isolate count</span>
        </div>
        <DataTable :value="resistanceGenes" class="amr-table" size="small">
          <Column field="gene" header="Gene Symbol">
            <template #body="{ data }">
              <code class="gene-name">{{ data.gene }}</code>
            </template>
          </Column>
          <Column field="resistanceClass" header="Class">
            <template #body="{ data }">
              <Tag :value="data.resistanceClass" :severity="classSeverity(data.resistanceClass)" class="class-tag" />
            </template>
          </Column>
          <Column field="subclass" header="Subclass">
            <template #body="{ data }">
              <span class="subclass-text">{{ data.subclass }}</span>
            </template>
          </Column>
          <Column field="isolates" header="Isolates" style="width:72px; text-align:right">
            <template #body="{ data }">
              <span class="num-cell">{{ data.isolates }}</span>
            </template>
          </Column>
          <Column field="identity" header="Avg. ID%" style="width:72px; text-align:right">
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
        <DataTable :value="riverSites" class="amr-table" size="small">
          <Column field="siteId" header="Site" style="width:56px">
            <template #body="{ data }">
              <span class="site-id">{{ data.siteId }}</span>
            </template>
          </Column>
          <Column field="river" header="River"></Column>
          <Column field="province" header="Province"></Column>
          <Column field="lastSampled" header="Last Sampled" style="width:110px"></Column>
          <Column field="isolates" header="Isolates" style="width:64px; text-align:right">
            <template #body="{ data }">
              <span class="num-cell">{{ data.isolates }}</span>
            </template>
          </Column>
          <Column field="risk" header="Risk" style="width:68px">
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

.stat-card {
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: 8px;
  padding: 18px 20px 16px;
  box-shadow: var(--c-shadow);
  opacity: 0;
  transform: translateY(10px);
  transition: opacity 0.35s ease, transform 0.35s ease, box-shadow 0.2s;
}

.stat-card--visible { opacity: 1; transform: translateY(0); }
.stat-card:hover { box-shadow: var(--c-shadow-md); }

.stat-label {
  font-size: 9.5px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--c-text-muted);
  margin-bottom: 8px;
  line-height: 1.4;
}

.stat-value {
  font-family: 'Zen Dots', sans-serif;
  font-size: 34px;
  font-weight: 400;
  color: var(--c-heading);
  line-height: 1;
  letter-spacing: -1px;
  margin-bottom: 8px;
}

.value-blue { color: var(--c-brand); }

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10.5px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.stat-trend .pi { font-size: 10px; }
.trend-danger  { color: var(--c-red); }
.trend-success { color: var(--c-green); }
.trend-muted   { color: var(--c-text-dim); }

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
  transition: background 0.2s ease, border-color 0.2s ease;
}

.panel-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
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

/* ── ECharts ───────────────────────────────────────── */
.echart { height: 220px; width: 100%; }

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

.badge-high { color: var(--c-risk-high); border-color: var(--c-risk-high); background: var(--c-red-dim); }
.badge-med  { color: var(--c-risk-med);  border-color: var(--c-risk-med);  background: var(--c-amber-dim); }
.badge-low  { color: var(--c-risk-low);  border-color: var(--c-risk-low);  background: var(--c-green-dim); }

/* ── Tables ────────────────────────────────────────── */
.amr-table { font-family: 'DM Sans', sans-serif; }

/* Resistance Genes table */
.gene-name {
  font-family: 'DM Mono', 'JetBrains Mono', monospace;
  font-size: 12px;
  color: var(--c-brand);
  background: var(--c-brand-dim);
  padding: 1px 6px;
  border-radius: 3px;
}

.class-tag { font-size: 9.5px !important; font-weight: 700 !important; }

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

/* River Sites table */
.site-id {
  font-family: 'DM Mono', monospace;
  font-size: 12px;
  font-weight: 600;
  color: var(--c-brand);
  background: var(--c-brand-dim);
  padding: 1px 6px;
  border-radius: 3px;
}

.risk-tag { font-size: 9.5px !important; font-weight: 700 !important; }

/* ── Responsive ────────────────────────────────────── */
@media (max-width: 1080px) {
  .stats-row  { grid-template-columns: repeat(2, 1fr); }
  .mid-row    { grid-template-columns: 1fr; }
  .bottom-row { grid-template-columns: 1fr; }
}

@media (max-width: 600px) {
  .dashboard  { padding: 0 14px 32px; }
  .stats-row  { grid-template-columns: 1fr 1fr; }
  .stat-value { font-size: 26px; }
}
</style>
