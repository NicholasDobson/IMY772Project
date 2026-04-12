import { describe, it, expect, vi } from 'vitest'
import { shallowMount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'

/* ── Mock the chart theme composable ──────────────────────────── */
vi.mock('@/composables/useChartTheme', () => ({
  useChartTheme: () => ({
    isDark:      { value: false },
    tooltipBase: { value: {} },
    axisLabel:   { value: {} },
    splitLine:   { value: {} },
    axisLine:    { value: { show: true } },
    blue:        { value: '#3B82F6' },
    blueHover:   { value: '#60A5FA' },
    red:         { value: '#EF4444' },
    redHover:    { value: '#F87171' },
    bgBase:      { value: '#FFFFFF' },
  }),
}))

import BacteriaDetailView from '@/views/BacteriaDetailView.vue'
import { ORGANISM_DB, DEFAULT_ORGANISM } from '@/data/organisms'

/* ── Router factory ───────────────────────────────────────────── */
function makeRouter(name: string) {
  const r = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div/>' } },
      { path: '/bacteria/:name', name: 'bacteria-detail', component: BacteriaDetailView },
    ],
  })
  return r
}

async function mountForOrganism(encodedName: string) {
  const router = makeRouter(encodedName)
  await router.push(`/bacteria/${encodedName}`)
  await router.isReady()
  return shallowMount(BacteriaDetailView, {
    global: { plugins: [router] },
  })
}

describe('BacteriaDetailView', () => {

  /* ── Organism loading ────────────────────────────────────────── */

  it('renders the organism name from the route param', async () => {
    const wrapper = await mountForOrganism('Escherichia%20coli')
    expect(wrapper.find('.organism-name').text()).toContain('Escherichia coli')
  })

  it('falls back to DEFAULT_ORGANISM (E. coli) data for an unknown organism name', async () => {
    // The route param is always shown as the heading (even when unknown),
    // but the organism data (description, gram stain, etc.) falls back to E. coli.
    const wrapper = await mountForOrganism('Unknown%20Organism')
    const fallbackOrganism = ORGANISM_DB[DEFAULT_ORGANISM]
    // Description comes from the fallback organism, not the unknown name
    expect(wrapper.find('.organism-desc').text()).toBe(fallbackOrganism.description)
    expect(wrapper.find('.gram-pill').text()).toBe(fallbackOrganism.gramStain)
  })

  it('renders the organism description', async () => {
    const wrapper = await mountForOrganism('Escherichia%20coli')
    const ecoli = ORGANISM_DB['Escherichia coli']
    expect(wrapper.find('.organism-desc').text()).toBe(ecoli.description)
  })

  it('renders the gram stain pill', async () => {
    const wrapper = await mountForOrganism('Escherichia%20coli')
    expect(wrapper.find('.gram-pill').text()).toBe(ORGANISM_DB['Escherichia coli'].gramStain)
  })

  it('renders the meta pills including common name', async () => {
    const wrapper = await mountForOrganism('Escherichia%20coli')
    const pillText = wrapper.findAll('.meta-pill').map((p) => p.text()).join(' ')
    expect(pillText).toContain('E. coli')
  })

  /* ── KPI cards ───────────────────────────────────────────────── */

  it('shows 4 KPI cards', async () => {
    const wrapper = await mountForOrganism('Escherichia%20coli')
    expect(wrapper.findAll('.kpi-card')).toHaveLength(4)
  })

  it('displays total detections in the first KPI card', async () => {
    const wrapper = await mountForOrganism('Escherichia%20coli')
    const ecoli = ORGANISM_DB['Escherichia coli']
    const kpiValues = wrapper.findAll('.kpi-value').map((v) => v.text())
    expect(kpiValues[0]).toContain(ecoli.detectionCount.toLocaleString())
  })

  it('displays site count in the second KPI card', async () => {
    const wrapper = await mountForOrganism('Escherichia%20coli')
    const ecoli = ORGANISM_DB['Escherichia coli']
    const kpiValues = wrapper.findAll('.kpi-value').map((v) => v.text())
    expect(kpiValues[1]).toContain(String(ecoli.siteCount))
  })

  it('displays resistance rate in the third KPI card', async () => {
    const wrapper = await mountForOrganism('Escherichia%20coli')
    const ecoli = ORGANISM_DB['Escherichia coli']
    const kpiValues = wrapper.findAll('.kpi-value').map((v) => v.text())
    expect(kpiValues[2]).toContain(String(ecoli.resistanceRate))
  })

  /* ── R/I/S counts ────────────────────────────────────────────── */

  it('computes R/I/S counts from the resistance profile', async () => {
    const wrapper = await mountForOrganism('Escherichia%20coli')
    const ecoli = ORGANISM_DB['Escherichia coli']

    const expectedR = ecoli.resistanceProfile.filter((a) => a.level === 'R').length
    const expectedI = ecoli.resistanceProfile.filter((a) => a.level === 'I').length
    const expectedS = ecoli.resistanceProfile.filter((a) => a.level === 'S').length

    expect(wrapper.find('.ris-r').text()).toContain(`R ${expectedR}`)
    expect(wrapper.find('.ris-i').text()).toContain(`I ${expectedI}`)
    expect(wrapper.find('.ris-s').text()).toContain(`S ${expectedS}`)
  })

  /* ── Panel titles ────────────────────────────────────────────── */

  it('renders the AMR genes table panel', async () => {
    const wrapper = await mountForOrganism('Escherichia%20coli')
    const titles = wrapper.findAll('.panel-title').map((el) => el.text())
    expect(titles).toContain('AMR Resistance Genes')
  })

  it('renders the Antibiotic Resistance Profile panel', async () => {
    const wrapper = await mountForOrganism('Escherichia%20coli')
    const titles = wrapper.findAll('.panel-title').map((el) => el.text())
    expect(titles).toContain('Antibiotic Resistance Profile')
  })

  it('renders the Affected River Sites panel', async () => {
    const wrapper = await mountForOrganism('Escherichia%20coli')
    const titles = wrapper.findAll('.panel-title').map((el) => el.text())
    expect(titles).toContain('Affected River Sites')
  })

  it('renders the WGS / Genomic Metrics panel', async () => {
    const wrapper = await mountForOrganism('Escherichia%20coli')
    const titles = wrapper.findAll('.panel-title').map((el) => el.text())
    expect(titles).toContain('WGS / Genomic Metrics')
  })

  it('shows the gene count subtitle', async () => {
    const wrapper = await mountForOrganism('Escherichia%20coli')
    const ecoli = ORGANISM_DB['Escherichia coli']
    const subtitle = wrapper.find('.panel-subtitle')
    expect(subtitle.text()).toContain(`${ecoli.genes.length} genes detected`)
  })

  /* ── Different organisms ─────────────────────────────────────── */

  it('loads Klebsiella pneumoniae data from the DB', async () => {
    const wrapper = await mountForOrganism('Klebsiella%20pneumoniae')
    expect(wrapper.find('.organism-name').text()).toContain('Klebsiella pneumoniae')
    const ecoli = ORGANISM_DB['Klebsiella pneumoniae']
    expect(wrapper.find('.organism-desc').text()).toBe(ecoli.description)
  })

  /* ── trendClass helper (via kpi-sub trend class) ─────────────── */

  it('applies trend-danger class when yoyTrend is "up"', async () => {
    // E. coli has yoyTrend: 'up'
    const wrapper = await mountForOrganism('Escherichia%20coli')
    expect(wrapper.find('.kpi-sub').classes()).toContain('trend-danger')
  })

  /* ── Back button navigation ──────────────────────────────────── */

  it('renders a back button to Dashboard', async () => {
    const wrapper = await mountForOrganism('Escherichia%20coli')
    const btn = wrapper.find('.back-btn')
    expect(btn.exists()).toBe(true)
    expect(btn.text()).toContain('Dashboard')
  })

  it('navigates to / when back button is clicked', async () => {
    const router = makeRouter('Escherichia%20coli')
    await router.push('/bacteria/Escherichia%20coli')
    await router.isReady()

    const wrapper = shallowMount(BacteriaDetailView, {
      global: { plugins: [router] },
    })

    await wrapper.find('.back-btn').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/')
  })

  /* ── Chart panels ────────────────────────────────────────────── */

  it('renders two chart panels (trend and resistance by class)', async () => {
    const wrapper = await mountForOrganism('Escherichia%20coli')
    // VChart elements carry class="chart-main" and class="chart-classes" in the template
    expect(wrapper.find('.chart-main').exists()).toBe(true)
    expect(wrapper.find('.chart-classes').exists()).toBe(true)
  })
})
