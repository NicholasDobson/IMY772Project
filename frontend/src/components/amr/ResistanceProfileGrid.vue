<script setup lang="ts">
import type { AntibioticEntry } from '@/types/amr'

defineProps<{ entries: AntibioticEntry[] }>()

function levelColor(level: string): string {
  if (level === 'R') return 'var(--c-red)'
  if (level === 'I') return 'var(--c-amber)'
  return 'var(--c-green)'
}

function levelBg(level: string): string {
  if (level === 'R') return 'var(--c-red-dim)'
  if (level === 'I') return 'var(--c-amber-dim)'
  return 'var(--c-green-dim)'
}
</script>

<template>
  <div class="resistance-grid">
    <div
      v-for="entry in entries"
      :key="entry.antibiotic"
      class="resistance-row"
    >
      <span class="abx-name">{{ entry.antibiotic }}</span>
      <div class="abx-bar-track">
        <div
          class="abx-bar-fill"
          :style="{
            width: `${Math.round(entry.resistanceRate * 100)}%`,
            background: levelColor(entry.level),
          }"
        ></div>
      </div>
      <span class="abx-pct" :style="{ color: levelColor(entry.level) }">
        {{ Math.round(entry.resistanceRate * 100) }}%
      </span>
      <span
        class="abx-level-badge"
        :style="{ color: levelColor(entry.level), background: levelBg(entry.level) }"
      >
        {{ entry.level }}
      </span>
    </div>
  </div>
</template>

<style scoped>
.resistance-grid {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.resistance-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.abx-name {
  font-size: 12px;
  color: var(--c-text);
  flex-shrink: 0;
  width: 148px;
}

.abx-bar-track {
  flex: 1;
  height: 7px;
  background: var(--c-border);
  border-radius: 99px;
  overflow: hidden;
}

.abx-bar-fill {
  height: 100%;
  border-radius: 99px;
  opacity: 0.8;
  transition: width 0.9s cubic-bezier(0.22, 1, 0.36, 1);
}

.abx-pct {
  font-family: 'DM Mono', monospace;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
  width: 36px;
  text-align: right;
}

.abx-level-badge {
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.05em;
  padding: 2px 8px;
  border-radius: 4px;
  width: 24px;
  text-align: center;
  flex-shrink: 0;
}

@media (max-width: 640px) {
  .abx-name { width: 110px; }
}
</style>
