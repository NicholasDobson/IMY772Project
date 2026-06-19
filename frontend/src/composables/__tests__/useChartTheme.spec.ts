import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useThemeStore } from '@/stores/theme'
import { useChartTheme } from '@/composables/useChartTheme'

const makeMatchMedia = (prefersDark: boolean) =>
  vi.fn().mockReturnValue({
    matches: prefersDark,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  })

describe('useChartTheme', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.stubGlobal('matchMedia', makeMatchMedia(false))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  function setupTheme(mode: 'light' | 'dark') {
    const theme = useThemeStore()
    theme.setMode(mode)
    return useChartTheme()
  }

  /* ── isDark ─────────────────────────────────────────────────────── */

  it('isDark is false in light mode', () => {
    const { isDark } = setupTheme('light')
    expect(isDark.value).toBe(false)
  })

  it('isDark is true in dark mode', () => {
    const { isDark } = setupTheme('dark')
    expect(isDark.value).toBe(true)
  })

  /* ── tooltipBase ────────────────────────────────────────────────── */

  it('tooltipBase has white background in light mode', () => {
    const { tooltipBase } = setupTheme('light')
    expect(tooltipBase.value.backgroundColor).toBe('#FFFFFF')
  })

  it('tooltipBase has dark background in dark mode', () => {
    const { tooltipBase } = setupTheme('dark')
    expect(tooltipBase.value.backgroundColor).toBe('#1A2D42')
  })

  it('tooltipBase text colour is dark in light mode', () => {
    const { tooltipBase } = setupTheme('light')
    expect(tooltipBase.value.textStyle.color).toBe('#111827')
  })

  it('tooltipBase text colour is light in dark mode', () => {
    const { tooltipBase } = setupTheme('dark')
    expect(tooltipBase.value.textStyle.color).toBe('#C9D5E2')
  })

  /* ── Brand colours ──────────────────────────────────────────────── */

  it('blue is darker shade in light mode', () => {
    const { blue } = setupTheme('light')
    expect(blue.value).toBe('#2563EB')
  })

  it('blue is lighter shade in dark mode', () => {
    const { blue } = setupTheme('dark')
    expect(blue.value).toBe('#3B82F6')
  })

  it('red is darker shade in light mode', () => {
    const { red } = setupTheme('light')
    expect(red.value).toBe('#C62828')
  })

  it('red is lighter shade in dark mode', () => {
    const { red } = setupTheme('dark')
    expect(red.value).toBe('#EF4444')
  })

  it('bgBase is white in light mode', () => {
    const { bgBase } = setupTheme('light')
    expect(bgBase.value).toBe('#FFFFFF')
  })

  it('bgBase is near-black in dark mode', () => {
    const { bgBase } = setupTheme('dark')
    expect(bgBase.value).toBe('#0D1520')
  })

  /* ── Reactivity — values update when theme changes ─────────────── */

  it('isDark updates reactively when theme is switched', () => {
    const themeStore = useThemeStore()
    themeStore.setMode('light')
    const { isDark } = useChartTheme()

    expect(isDark.value).toBe(false)
    themeStore.setMode('dark')
    expect(isDark.value).toBe(true)
  })

  it('blue updates reactively when theme is switched', () => {
    const themeStore = useThemeStore()
    themeStore.setMode('light')
    const { blue } = useChartTheme()

    expect(blue.value).toBe('#2563EB')
    themeStore.setMode('dark')
    expect(blue.value).toBe('#3B82F6')
  })
})
