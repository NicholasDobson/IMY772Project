import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useThemeStore } from '@/stores/theme'

const makeMatchMedia = (prefersDark: boolean) =>
  vi.fn().mockReturnValue({
    matches: prefersDark,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  })

describe('useThemeStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    document.documentElement.removeAttribute('data-theme')
    document.documentElement.classList.remove('dark-mode')
    vi.stubGlobal('matchMedia', makeMatchMedia(false))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  /* ── Initial state ─────────────────────────────────────────────── */

  it('defaults to system mode when nothing is stored in localStorage', () => {
    const store = useThemeStore()
    expect(store.mode).toBe('system')
  })

  it('reads persisted mode from localStorage on creation', () => {
    localStorage.setItem('amr-theme', 'dark')
    const store = useThemeStore()
    expect(store.mode).toBe('dark')
  })

  /* ── resolvedTheme computed ─────────────────────────────────────── */

  it('resolves to light when mode is explicitly light', () => {
    localStorage.setItem('amr-theme', 'light')
    const store = useThemeStore()
    expect(store.resolvedTheme).toBe('light')
  })

  it('resolves to dark when mode is explicitly dark', () => {
    localStorage.setItem('amr-theme', 'dark')
    const store = useThemeStore()
    expect(store.resolvedTheme).toBe('dark')
  })

  it('resolves to light when mode is system and OS prefers light', () => {
    vi.stubGlobal('matchMedia', makeMatchMedia(false))
    const store = useThemeStore()
    expect(store.resolvedTheme).toBe('light')
  })

  it('resolves to dark when mode is system and OS prefers dark', () => {
    vi.stubGlobal('matchMedia', makeMatchMedia(true))
    const store = useThemeStore()
    expect(store.resolvedTheme).toBe('dark')
  })

  /* ── setMode ────────────────────────────────────────────────────── */

  it('setMode updates the mode ref', () => {
    const store = useThemeStore()
    store.setMode('dark')
    expect(store.mode).toBe('dark')
  })

  it('setMode persists the choice to localStorage', () => {
    const store = useThemeStore()
    store.setMode('light')
    expect(localStorage.getItem('amr-theme')).toBe('light')
  })

  it('setMode sets data-theme="dark" on documentElement when switching to dark', () => {
    const store = useThemeStore()
    store.setMode('dark')
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
  })

  it('setMode sets data-theme="light" on documentElement when switching to light', () => {
    const store = useThemeStore()
    store.setMode('light')
    expect(document.documentElement.getAttribute('data-theme')).toBe('light')
  })

  it('setMode adds dark-mode class when switching to dark', () => {
    const store = useThemeStore()
    store.setMode('dark')
    expect(document.documentElement.classList.contains('dark-mode')).toBe(true)
  })

  it('setMode removes dark-mode class when switching to light', () => {
    document.documentElement.classList.add('dark-mode')
    const store = useThemeStore()
    store.setMode('light')
    expect(document.documentElement.classList.contains('dark-mode')).toBe(false)
  })

  it('setMode can be called multiple times and always reflects latest value', () => {
    const store = useThemeStore()
    store.setMode('dark')
    store.setMode('light')
    store.setMode('system')
    expect(store.mode).toBe('system')
    expect(localStorage.getItem('amr-theme')).toBe('system')
  })
})
