import { computed } from 'vue'
import { useThemeStore } from '@/stores/theme'

/* ─── useChartTheme ───────────────────────────────────────────────
   Centralises ECharts theming so chart options only read isDark once.
   Eliminates the repeated isDark ternaries scattered through views.
─────────────────────────────────────────────────────────────────── */
export function useChartTheme() {
  const themeStore = useThemeStore()
  const isDark = computed(() => themeStore.resolvedTheme === 'dark')

  /* Shared tooltip base config */
  const tooltipBase = computed(() => ({
    backgroundColor: isDark.value ? '#1A2D42' : '#FFFFFF',
    borderColor: isDark.value ? 'rgba(255,255,255,0.12)' : '#E5E7EB',
    borderWidth: 1,
    textStyle: {
      color: isDark.value ? '#C9D5E2' : '#111827',
      fontFamily: 'DM Sans, sans-serif',
      fontSize: 12,
    },
  }))

  /* X/Y axis label style */
  const axisLabel = computed(() => ({
    color: isDark.value ? '#4B6174' : '#9CA3AF',
    fontFamily: 'DM Sans, sans-serif',
    fontSize: 11,
  }))

  /* Grid split lines */
  const splitLine = computed(() => ({
    lineStyle: { color: isDark.value ? 'rgba(255,255,255,0.06)' : '#F3F4F6' },
  }))

  /* Axis line (for category axes) */
  const axisLine = computed(() => ({
    lineStyle: { color: isDark.value ? 'rgba(255,255,255,0.08)' : '#E5E7EB' },
  }))

  /* Chart brand colours */
  const blue  = computed(() => isDark.value ? '#3B82F6' : '#2563EB')
  const blueHover = computed(() => isDark.value ? '#60A5FA' : '#1976D2')
  const red   = computed(() => isDark.value ? '#EF4444' : '#C62828')
  const redHover  = computed(() => isDark.value ? '#F87171' : '#E53935')
  const bgBase    = computed(() => isDark.value ? '#0D1520' : '#FFFFFF')

  return {
    isDark,
    tooltipBase,
    axisLabel,
    splitLine,
    axisLine,
    blue,
    blueHover,
    red,
    redHover,
    bgBase,
  }
}
