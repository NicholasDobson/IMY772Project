<script setup lang="ts">
import { computed } from 'vue'
import Tag from 'primevue/tag'
import type { WgsMetrics } from '@/types/amr'

const props = defineProps<{
  wgs: WgsMetrics
  rCount: number
  iCount: number
  sCount: number
}>()

const metaItems = computed(() => [
  { label: 'Predicted Phenotype', value: props.wgs.predictedPhenotype, type: 'phenotype' as const },
  { label: 'Sequence Type / Genotype', value: props.wgs.genotype, type: 'code' as const },
  { label: 'Plasmid / Mobile Element', value: props.wgs.plasmid, type: 'code' as const },
  {
    label: 'Genome Length',
    value: `${props.wgs.genomeLength.toLocaleString()} bp`,
    type: 'mono' as const,
  },
  { label: 'N50 Value', value: `${props.wgs.n50.toLocaleString()} bp`, type: 'mono' as const },
])

const risSummary = computed(() => [
  { label: 'Resistant', count: props.rCount, cls: 'r' as const },
  { label: 'Intermediate', count: props.iCount, cls: 'i' as const },
  { label: 'Susceptible', count: props.sCount, cls: 's' as const },
])
</script>

<template>
  <div class="wgs-content">
    <div class="wgs-grid">
      <!-- QC Status first -->
      <div class="wgs-item">
        <span class="wgs-label">QC Status</span>
        <Tag
          :value="wgs.qualityStatus"
          :severity="wgs.qualityStatus === 'PASS' ? 'success' : 'danger'"
        />
      </div>

      <div v-for="item in metaItems" :key="item.label" class="wgs-item">
        <span class="wgs-label">{{ item.label }}</span>
        <code v-if="item.type === 'code'" class="wgs-code">{{ item.value }}</code>
        <span v-else-if="item.type === 'phenotype'" class="wgs-phenotype">{{ item.value }}</span>
        <span v-else class="wgs-mono">{{ item.value }}</span>
      </div>
    </div>

    <!-- R/I/S proportion bar -->
    <div class="ris-summary">
      <div class="ris-bar-visual">
        <div class="ris-segment ris-segment--r" :style="{ flex: rCount }" />
        <div class="ris-segment ris-segment--i" :style="{ flex: iCount }" />
        <div class="ris-segment ris-segment--s" :style="{ flex: sCount }" />
      </div>
      <div class="ris-counts">
        <div v-for="item in risSummary" :key="item.label" class="ris-count-item">
          <span class="ris-count-dot" :class="`ris-count-dot--${item.cls}`"></span>
          <span class="ris-count-label">{{ item.label }}</span>
          <span class="ris-count-val">{{ item.count }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.wgs-content {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.wgs-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

.wgs-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--c-border);
}

.wgs-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.wgs-label {
  font-size: 9.5px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--c-text-dim);
}

.wgs-phenotype {
  font-size: 12px;
  font-weight: 500;
  color: var(--c-amber);
}

.wgs-code {
  font-family: 'DM Mono', monospace;
  font-size: 12px;
  color: var(--c-brand);
}

.wgs-mono {
  font-family: 'DM Mono', monospace;
  font-size: 12px;
  font-weight: 600;
  color: var(--c-heading);
}

/* R/I/S summary */
.ris-summary {
  border-top: 1px solid var(--c-border);
  padding-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ris-bar-visual {
  display: flex;
  height: 8px;
  border-radius: 99px;
  overflow: hidden;
  gap: 2px;
}

.ris-segment {
  border-radius: 99px;
  min-width: 4px;
  transition: flex 0.8s cubic-bezier(0.22, 1, 0.36, 1);
}
.ris-segment--r {
  background: var(--c-red);
}
.ris-segment--i {
  background: var(--c-amber);
}
.ris-segment--s {
  background: var(--c-green);
}

.ris-counts {
  display: flex;
  gap: 14px;
}

.ris-count-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.ris-count-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}
.ris-count-dot--r {
  background: var(--c-red);
}
.ris-count-dot--i {
  background: var(--c-amber);
}
.ris-count-dot--s {
  background: var(--c-green);
}

.ris-count-label {
  font-size: 11px;
  color: var(--c-text-muted);
}

.ris-count-val {
  font-family: 'DM Mono', monospace;
  font-size: 12px;
  font-weight: 700;
  color: var(--c-heading);
}
</style>
