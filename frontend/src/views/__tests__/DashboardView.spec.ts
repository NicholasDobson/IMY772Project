import { describe, it, expect, vi, beforeEach } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'

/* ── Mock the dashboard store ─────────────────────────────────── */
const mockFetchAll = vi.fn().mockResolvedValue(undefined)
const mockSelectYear = vi.fn().mockResolvedValue(undefined)

const mockStore = vi.hoisted(() => ({
  loading: false,
  trendLoading: false,
  error: null as string | null,
  availableYears: [2024, 2025],
  selectedYear: 2025,
  hasIsolateData: false,
  statCards: null,
  provinces: null,
  topOrganisms: null,
  resistanceGenes: null,
  affectedRiverSites: null,
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
import { STAT_CARDS, PROVINCES, TOP_ORGANISMS } from '@/data/dashboard'

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
    // Reset to default mock state (no live data)
    mockStore.hasIsolateData = false
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

  it('renders 4 StatCard stubs', async () => {
    const wrapper = await mountDashboard()
    // shallowMount stubs StatCard as <stat-card-stub>
    const cards = wrapper.findAll('stat-card-stub')
    expect(cards).toHaveLength(4)
  })

  it('renders year toggle buttons for each available year', async () => {
    const wrapper = await mountDashboard()
    const buttons = wrapper.findAll('.year-btn')
    expect(buttons).toHaveLength(2)
    expect(buttons[0].text()).toBe('2024')
    expect(buttons[1].text()).toBe('2025')
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
    await buttons[0].trigger('click')
    expect(mockStore.selectYear).toHaveBeenCalledWith(2024)
  })

  /* ── Mockdata fallback (hasIsolateData = false) ───────────────── */

  it('renders province rows from mockdata when hasIsolateData is false', async () => {
    const wrapper = await mountDashboard()
    const provinceNames = wrapper.findAll('.province-name').map((n) => n.text())
    expect(provinceNames).toContain('Gauteng')
    expect(provinceNames).toContain('Western Cape')
  })

  it('passes STAT_CARDS label to first StatCard stub when hasIsolateData is false', async () => {
    const wrapper = await mountDashboard()
    const firstCard = wrapper.findAll('stat-card-stub')[0]
    expect(firstCard.attributes('label')).toBe(STAT_CARDS[0].label)
  })

  /* ── Live data override (hasIsolateData = true) ───────────────── */

  it('passes live statCards data to StatCard stubs when hasIsolateData is true', async () => {
    mockStore.hasIsolateData = true
    mockStore.statCards = [
      { id: 'incident-rate', label: 'LIVE Label', value: '99%', trendText: '+1%', trendIcon: null, valueClass: '', trendClass: 'trend-danger' },
      { id: 'sample-count',  label: 'L2', value: '200', trendText: 'T2', trendIcon: null, valueClass: '', trendClass: 'trend-muted' },
      { id: 'high-risk-sites', label: 'L3', value: '8', trendText: 'T3', trendIcon: null, valueClass: '', trendClass: 'trend-muted' },
      { id: 'monthly-cases', label: 'L4', value: '45', trendText: 'T4', trendIcon: null, valueClass: '', trendClass: 'trend-muted' },
    ]

    const wrapper = await mountDashboard()
    const firstCard = wrapper.findAll('stat-card-stub')[0]
    expect(firstCard.attributes('label')).toBe('LIVE Label')
    expect(firstCard.attributes('value')).toBe('99%')
  })

  it('uses PROVINCES mockdata for province bars when hasIsolateData is false', async () => {
    const wrapper = await mountDashboard()
    const provinceRows = wrapper.findAll('.province-row')
    expect(provinceRows).toHaveLength(PROVINCES.length)
  })

  it('shows live province data when hasIsolateData is true and store.provinces is populated', async () => {
    mockStore.hasIsolateData = true
    mockStore.provinces = [
      { name: 'Limpopo', risk: 'LOW', percent: 20 },
    ]

    const wrapper = await mountDashboard()
    const names = wrapper.findAll('.province-name').map((n) => n.text())
    expect(names).toEqual(['Limpopo'])
  })

  /* ── riskBadgeClass helper (via province badges) ─────────────── */

  it('assigns badge-high class to HIGH risk provinces', async () => {
    mockStore.hasIsolateData = true
    mockStore.provinces = [{ name: 'Gauteng', risk: 'HIGH', percent: 88 }]

    const wrapper = await mountDashboard()
    expect(wrapper.find('.province-badge').classes()).toContain('badge-high')
  })

  it('assigns badge-med class to MED risk provinces', async () => {
    mockStore.hasIsolateData = true
    mockStore.provinces = [{ name: 'Western Cape', risk: 'MED', percent: 48 }]

    const wrapper = await mountDashboard()
    expect(wrapper.find('.province-badge').classes()).toContain('badge-med')
  })

  it('assigns badge-low class to LOW risk provinces', async () => {
    mockStore.hasIsolateData = true
    mockStore.provinces = [{ name: 'Free State', risk: 'LOW', percent: 15 }]

    const wrapper = await mountDashboard()
    expect(wrapper.find('.province-badge').classes()).toContain('badge-low')
  })

  /* ── riskColor helper (via province bar fill inline style) ────── */

  it('sets HIGH risk bar fill to the high-risk CSS variable', async () => {
    mockStore.hasIsolateData = true
    mockStore.provinces = [{ name: 'Gauteng', risk: 'HIGH', percent: 88 }]

    const wrapper = await mountDashboard()
    const fill = wrapper.find('.province-bar-fill')
    expect(fill.attributes('style')).toContain('var(--c-risk-high)')
  })

  it('sets LOW risk bar fill to the low-risk CSS variable', async () => {
    mockStore.hasIsolateData = true
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
