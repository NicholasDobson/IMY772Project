<script setup lang="ts">
import { useThemeStore, type ThemeMode } from '@/stores/theme'

const themeStore = useThemeStore()

const options: { value: ThemeMode; label: string; light: string; dark: string }[] = [
  { value: 'light',  label: 'Light',  light: '#FFFFFF', dark: '#F3F4F6' },
  { value: 'dark',   label: 'Dark',   light: '#0F172A', dark: '#1E293B' },
  { value: 'system', label: 'Auto',   light: '#FFFFFF', dark: '#0F172A' },
]
</script>

<template>
  <div class="theme-toggle" aria-label="Theme selector">
    <button
      v-for="opt in options"
      :key="opt.value"
      class="swatch-btn"
      :class="{ 'swatch-btn--active': themeStore.mode === opt.value }"
      :title="opt.label"
      @click="themeStore.setMode(opt.value)"
    >
      <!-- The palette square -->
      <span
        class="swatch"
        :class="`swatch--${opt.value}`"
      >
        <!-- Split preview for system, solid for light/dark -->
        <template v-if="opt.value === 'system'">
          <span class="swatch-half swatch-half--left" :style="{ background: opt.light }"></span>
          <span class="swatch-half swatch-half--right" :style="{ background: opt.dark }"></span>
        </template>
        <template v-else>
          <span class="swatch-solid" :style="{ background: opt.light }"></span>
          <span class="swatch-icon">
            <i v-if="opt.value === 'light'" class="pi pi-sun"></i>
            <i v-else class="pi pi-moon"></i>
          </span>
        </template>
      </span>
      <span class="swatch-label">{{ opt.label }}</span>
    </button>
  </div>
</template>

<style scoped>
.theme-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
}

.swatch-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 2px;
  border-radius: 6px;
  transition: opacity 0.15s;
}

.swatch-btn:hover { opacity: 0.8; }

.swatch {
  position: relative;
  width: 28px;
  height: 20px;
  border-radius: 5px;
  overflow: hidden;
  border: 1.5px solid var(--c-border);
  transition: border-color 0.15s, box-shadow 0.15s;
  display: flex;
}

.swatch-btn--active .swatch {
  border-color: var(--c-brand);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--c-brand) 30%, transparent);
}

/* Light/Dark solid fill */
.swatch-solid {
  position: absolute;
  inset: 0;
  border-radius: 3px;
}

.swatch--light .swatch-solid { background: #FFFFFF; }
.swatch--dark  .swatch-solid { background: #0F172A; }

.swatch-icon {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
}
.swatch--light .swatch-icon { color: #888; }
.swatch--dark  .swatch-icon { color: #94A3B8; }

/* System half/half */
.swatch-half {
  flex: 1;
  height: 100%;
}
.swatch-half--left {
  background: #FFFFFF;
  border-right: 1px solid var(--c-border);
}
.swatch-half--right { background: #0F172A; }

/* Label */
.swatch-label {
  font-family: 'DM Sans', sans-serif;
  font-size: 9.5px;
  font-weight: 500;
  color: var(--c-text-muted);
  letter-spacing: 0.02em;
}

.swatch-btn--active .swatch-label {
  color: var(--c-brand);
}
</style>
