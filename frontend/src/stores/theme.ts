import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export type ThemeMode = 'light' | 'dark' | 'system'

export const useThemeStore = defineStore('theme', () => {
  const mode = ref<ThemeMode>((localStorage.getItem('amr-theme') as ThemeMode) || 'system')

  const resolvedTheme = computed<'light' | 'dark'>(() => {
    if (mode.value === 'system') {
      return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
    }
    return mode.value
  })

  function apply(m: ThemeMode) {
    const html = document.documentElement
    const isDark =
      m === 'dark' || (m === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches)

    html.setAttribute('data-theme', isDark ? 'dark' : 'light')
    // PrimeVue dark mode toggle
    html.classList.toggle('dark-mode', isDark)
  }

  function setMode(m: ThemeMode) {
    mode.value = m
    localStorage.setItem('amr-theme', m)
    apply(m)
  }

  function init() {
    apply(mode.value)
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
      if (mode.value === 'system') apply('system')
    })
  }

  return { mode, resolvedTheme, setMode, init }
})
