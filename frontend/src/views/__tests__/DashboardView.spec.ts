import { describe, it, expect, vi, beforeEach } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'

/* ── Mock the dashboard store ─────────────────────────────────── */
const mockStore = vi.hoisted(() => ({
  loading: false,
  trendLoading: false,
  error: null as string | null,
  availableYears: [2024, 2025] as number[],
  selectedYear: 2025,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  statCards: null as any,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  provinces: null as any,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  topOrganisms: null as any,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  resistanceGenes: null as any,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  affectedRiverSites: null as any,
  trendYear: 2025,
  months: [] as string[],
  monthlyNormal: [] as (number | null)[],
  monthlyAlert: [] as (number | null)[],
  selectYear: vi.fn().mockResolvedValue(undefined),
  fetchAll: vi.fn().mockResolvedValue(undefined),
}))

vi.mock('@/stores/dashboard', () => ({
  useDashboardStore: vi.fn(() => mockStore),
}))

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

import DashboardView from '@/views/DashboardView.vue'

const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    { path: '/', component: DashboardView },
    { path: '/bacteria/:name', name: 'bacteria-detail', component: { template: '<div/>' } },
  ],
})

async function mountDashboard() {
  await router.push('/')
  await router.isReady()
  return shallowMount(DashboardView, {
    global: { plugins: [router] },
  })
}

describe('DashboardView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Reset to default mock state (no live data yet)
    mockStore.statCards = null
    mockStore.provinces = null
    mockStore.topOrganisms = null
    mockStore.availableYears = [2024, 2025]
    mockStore.selectedYear = 2025
    mockStore.error = null
  })

  /* ── Structural rendering ────────────────────────────────────── */

  it('renders the page title', async () => {
    const wrapper = await mountDashboard()
    expect(wrapper.find('.page-title').text()).toBe('Dashboard')
  })

  it('renders the stats-row section', async () => {
    const wrapper = await mountDashboard()
    expect(wrapper.find('.stats-row').exists()).toBe(true)
  })

  it('calls store.fetchAll on mount', async () => {
    await mountDashboard()
    expect(mockStore.fetchAll).toHaveBeenCalledOnce()
  })

  it('renders 4 StatCard stubs when store.statCards has 4 items', async () => {
    mockStore.statCards = [
      { id: 'incident-rate',  label: 'L1', value: '63%', trendText: 'T1', trendIcon: null, valueClass: '', trendClass: 'trend-danger' },
      { id: 'sample-count',   label: 'L2', value: '200', trendText: 'T2', trendIcon: null, valueClass: '', trendClass: 'trend-muted' },
      { id: 'high-risk-sites',label: 'L3', value: '6',   trendText: 'T3', trendIcon: null, valueClass: '', trendClass: 'trend-muted' },
      { id: 'monthly-cases',  label: 'L4', value: '45',  trendText: 'T4', trendIcon: null, valueClass: '', trendClass: 'trend-muted' },
    ]
    const wrapper = await mountDashboard()
    const cards = wrapper.findAll('stat-card-stub')
    expect(cards).toHaveLength(4)
  })

  it('renders year toggle buttons for each available year', async () => {
    const wrapper = await mountDashboard()
    const buttons = wrapper.findAll('.year-btn')
    expect(buttons).toHaveLength(2)
    expect(buttons[0]!.text()).toBe('2024')
    expect(buttons[1]!.text()).toBe('2025')
  })

  it('marks the selected year button as active', async () => {
    const wrapper = await mountDashboard()
    const activeBtn = wrapper.find('.year-btn--active')
    expect(activeBtn.exists()).toBe(true)
    expect(activeBtn.text()).toBe('2025')
  })

  it('disables year buttons when trendLoading is true', async () => {
    mockStore.trendLoading = true
    const wrapper = await mountDashboard()
    const buttons = wrapper.findAll('.year-btn')
    buttons.forEach((btn) => {
      expect(btn.attributes('disabled')).toBeDefined()
    })
    mockStore.trendLoading = false
  })

  it('calls store.selectYear when a year button is clicked', async () => {
    const wrapper = await mountDashboard()
    const buttons = wrapper.findAll('.year-btn')
    await buttons[0]!.trigger('click')
    expect(mockStore.selectYear).toHaveBeenCalledWith(2024)
  })

  /* ── No data state (store returns null) ──────────────────────── */

  it('renders no province rows when store.provinces is null', async () => {
    const wrapper = await mountDashboard()
    const provinceRows = wrapper.findAll('.province-row')
    expect(provinceRows).toHaveLength(0)
  })

  it('renders no StatCard stubs when store.statCards is null', async () => {
    const wrapper = await mountDashboard()
    const cards = wrapper.findAll('stat-card-stub')
    expect(cards).toHaveLength(0)
  })

  /* ── Live data from store ────────────────────────────────────── */

  it('passes live statCards data to StatCard stubs when store.statCards is populated', async () => {
    mockStore.statCards = [
      { id: 'incident-rate', label: 'LIVE Label', value: '99%', trendText: '+1%', trendIcon: null, valueClass: '', trendClass: 'trend-danger' },
      { id: 'sample-count',  label: 'L2', value: '200', trendText: 'T2', trendIcon: null, valueClass: '', trendClass: 'trend-muted' },
      { id: 'high-risk-sites', label: 'L3', value: '8', trendText: 'T3', trendIcon: null, valueClass: '', trendClass: 'trend-muted' },
      { id: 'monthly-cases', label: 'L4', value: '45', trendText: 'T4', trendIcon: null, valueClass: '', trendClass: 'trend-muted' },
    ]

    const wrapper = await mountDashboard()
    const firstCard = wrapper.findAll('stat-card-stub')[0]!
    expect(firstCard.attributes('label')).toBe('LIVE Label')
    expect(firstCard.attributes('value')).toBe('99%')
  })

  it('shows live province data when store.provinces is populated', async () => {
    mockStore.provinces = [{ name: 'Limpopo', risk: 'LOW', percent: 20 }]

    const wrapper = await mountDashboard()
    const names = wrapper.findAll('.province-name').map((n) => n.text())
    expect(names).toEqual(['Limpopo'])
  })

  /* ── riskBadgeClass helper (via province badges) ─────────────── */

  it('assigns badge-high class to HIGH risk provinces', async () => {
    mockStore.provinces = [{ name: 'Gauteng', risk: 'HIGH', percent: 88 }]

    const wrapper = await mountDashboard()
    expect(wrapper.find('.province-badge').classes()).toContain('badge-high')
  })

  it('assigns badge-med class to MED risk provinces', async () => {
    mockStore.provinces = [{ name: 'Western Cape', risk: 'MED', percent: 48 }]

    const wrapper = await mountDashboard()
    expect(wrapper.find('.province-badge').classes()).toContain('badge-med')
  })

  it('assigns badge-low class to LOW risk provinces', async () => {
    mockStore.provinces = [{ name: 'Free State', risk: 'LOW', percent: 15 }]

    const wrapper = await mountDashboard()
    expect(wrapper.find('.province-badge').classes()).toContain('badge-low')
  })

  /* ── riskColor helper (via province bar fill inline style) ────── */

  it('sets HIGH risk bar fill to the high-risk CSS variable', async () => {
    mockStore.provinces = [{ name: 'Gauteng', risk: 'HIGH', percent: 88 }]

    const wrapper = await mountDashboard()
    const fill = wrapper.find('.province-bar-fill')
    expect(fill.attributes('style')).toContain('var(--c-risk-high)')
  })

  it('sets LOW risk bar fill to the low-risk CSS variable', async () => {
    mockStore.provinces = [{ name: 'Free State', risk: 'LOW', percent: 15 }]

    const wrapper = await mountDashboard()
    const fill = wrapper.find('.province-bar-fill')
    expect(fill.attributes('style')).toContain('var(--c-risk-low)')
  })

  /* ── Navigation ──────────────────────────────────────────────── */

  it('renders the organisms panel section', async () => {
    const wrapper = await mountDashboard()
    const titles = wrapper.findAll('.panel-title').map((el) => el.text())
    expect(titles).toContain('Top Detected Organisms')
  })

  it('renders the resistance genes panel section', async () => {
    const wrapper = await mountDashboard()
    const titles = wrapper.findAll('.panel-title').map((el) => el.text())
    expect(titles).toContain('Top Resistance Genes')
  })

  it('renders the river sites panel section', async () => {
    const wrapper = await mountDashboard()
    const titles = wrapper.findAll('.panel-title').map((el) => el.text())
    expect(titles).toContain('Affected River Sites')
  })
})
