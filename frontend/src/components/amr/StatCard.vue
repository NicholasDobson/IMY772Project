<script setup lang="ts">
interface Props {
  label: string
  value: string
  trendText: string
  trendIcon?: string | null
  valueClass?: string
  trendClass?: string
  /** Controls the entry animation. Pass the parent's `visible` ref. */
  visible?: boolean
  /** CSS transition-delay in ms for staggered entry. */
  delay?: number
}

withDefaults(defineProps<Props>(), {
  trendIcon: null,
  valueClass: '',
  trendClass: 'trend-muted',
  visible: false,
  delay: 0,
})
</script>

<template>
  <div
    class="stat-card"
    :class="{ 'stat-card--visible': visible }"
    :style="{ transitionDelay: `${delay}ms` }"
  >
    <p class="stat-label">{{ label }}</p>
    <p class="stat-value" :class="valueClass">{{ value }}</p>
    <div class="stat-trend" :class="trendClass">
      <i v-if="trendIcon" :class="`pi ${trendIcon}`"></i>
      <span>{{ trendText }}</span>
    </div>
  </div>
</template>

<style scoped>
.stat-card {
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: 8px;
  padding: 18px 20px 16px;
  box-shadow: var(--c-shadow);
  opacity: 0;
  transform: translateY(10px);
  transition:
    opacity 0.35s ease,
    transform 0.35s ease,
    box-shadow 0.2s;
}

.stat-card--visible {
  opacity: 1;
  transform: translateY(0);
}
.stat-card:hover {
  box-shadow: var(--c-shadow-md);
}

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
  font-family: 'Inter', sans-serif;
  font-size: 34px;
  font-weight: 700;
  color: var(--c-heading);
  line-height: 1;
  letter-spacing: -0.5px;
  margin-bottom: 8px;
}

/* Value colour modifiers — applied from parent via :valueClass */
:global(.value-blue) {
  color: var(--c-brand) !important;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10.5px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.stat-trend .pi {
  font-size: 10px;
}

/* Trend colour classes — used by parent via :trendClass */
:global(.trend-danger) {
  color: var(--c-red);
}
:global(.trend-success) {
  color: var(--c-green);
}
:global(.trend-muted) {
  color: var(--c-text-dim);
}
</style>
