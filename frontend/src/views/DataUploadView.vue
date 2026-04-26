<script setup lang="ts">
import { reactive, ref, computed } from 'vue'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

type EtlFileType = 'EPICOLLECT' | 'BINARY_INFO' | 'AMR_FINDER' | 'STAR_AMR'

const categories: {
  fileType: EtlFileType
  formKey: string
  title: string
  description: string
  icon: string
}[] = [
  {
    fileType: 'EPICOLLECT',
    formKey: 'epicollect',
    title: 'Epicollect (Field Data)',
    description: 'Field collection exports from Epicollect5.',
    icon: 'pi pi-table',
  },
  {
    fileType: 'BINARY_INFO',
    formKey: 'binaryInfo',
    title: 'Binary Info (Isolates)',
    description: 'Isolate and binary characterisation spreadsheets.',
    icon: 'pi pi-database',
  },
  {
    fileType: 'AMR_FINDER',
    formKey: 'amrFinder',
    title: 'AMR Finder (Gene Sequences)',
    description: 'Resistance gene detection outputs for sequences.',
    icon: 'pi pi-sitemap',
  },
  {
    fileType: 'STAR_AMR',
    formKey: 'starAmr',
    title: 'Star AMR (WGS Metrics)',
    description: 'Whole-genome sequencing AMR metrics and summaries.',
    icon: 'pi pi-chart-bar',
  },
]

interface SlotState {
  file: File | null
  error: string
}

function initialSlots(): Record<EtlFileType, SlotState> {
  return {
    EPICOLLECT: { file: null, error: '' },
    BINARY_INFO: { file: null, error: '' },
    AMR_FINDER: { file: null, error: '' },
    STAR_AMR: { file: null, error: '' },
  }
}

const slots = reactive<Record<EtlFileType, SlotState>>(initialSlots())

// Support for multiple formats
const acceptedFormats = '.xlsx, .csv, .tsv, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, text/csv, text/tab-separated-values'
const validExtensions = ['.xlsx', '.csv', '.tsv']

// Global Batch State
const batchStatus = ref<'idle' | 'uploading' | 'success' | 'error'>('idle')
const globalMessage = ref('')
const globalWarnings = ref<string[]>([])

const hasStagedFiles = computed(() => Object.values(slots).some(slot => slot.file !== null))

function isValidFormat(file: File): boolean {
  const n = file.name.toLowerCase()
  return validExtensions.some(ext => n.endsWith(ext))
}

function assignFile(fileType: EtlFileType, file: File | null, clearInputId?: string): void {
  if (!file) return

  if (!isValidFormat(file)) {
    slots[fileType].file = null
    slots[fileType].error = 'Invalid format. Use .xlsx, .csv, or .tsv'
    if (clearInputId) {
      const el = document.getElementById(clearInputId) as HTMLInputElement | null
      if (el) el.value = ''
    }
    return
  }

  slots[fileType].file = file
  slots[fileType].error = ''
  // Reset global states if user modifies selection
  batchStatus.value = 'idle'
  globalMessage.value = ''
  globalWarnings.value = []
}

function onFileSelected(fileType: EtlFileType, ev: Event): void {
  const input = ev.target as HTMLInputElement
  const file = input.files?.[0] ?? null
  assignFile(fileType, file, `file-${fileType}`)
}

function onDropFile(fileType: EtlFileType, ev: DragEvent): void {
  const file = ev.dataTransfer?.files?.[0] ?? null
  assignFile(fileType, file, `file-${fileType}`)
}

function clearSlot(fileType: EtlFileType): void {
  slots[fileType].file = null
  slots[fileType].error = ''
  const el = document.getElementById(`file-${fileType}`) as HTMLInputElement | null
  if (el) el.value = ''
}

function clearAllSlots(): void {
  Object.keys(slots).forEach((key) => clearSlot(key as EtlFileType))
  batchStatus.value = 'idle'
  globalMessage.value = ''
  globalWarnings.value = []
}

async function uploadBatch(): Promise<void> {
  if (!hasStagedFiles.value) return

  batchStatus.value = 'uploading'
  globalMessage.value = ''
  globalWarnings.value = []

  const formData = new FormData()
  
  // Map our slotted files to the exact param names expected by the Spring Boot controller
  categories.forEach(cat => {
    const file = slots[cat.fileType].file
    if (file) formData.append(cat.formKey, file)
  })

  try {
    const res = await fetch(`${API_BASE}/etl/upload-batch`, {
      method: 'POST',
      body: formData,
    })

    if (res.ok) {
      const data = await res.json()
      batchStatus.value = 'success'
      globalMessage.value = data.message || 'Batch uploaded successfully.'
      globalWarnings.value = data.warnings || []
      
      // Clear out the files so the user knows it's done
      Object.keys(slots).forEach((key) => clearSlot(key as EtlFileType))
    } else {
      // Backend returns plain text for 400/500 errors
      const errorText = await res.text()
      batchStatus.value = 'error'
      globalMessage.value = errorText || `Upload failed with status: ${res.status}`
    }
  } catch (e) {
    batchStatus.value = 'error'
    globalMessage.value = e instanceof Error ? e.message : 'Network error. Ensure the backend is running.'
  }
}

const visible = ref(false)
setTimeout(() => {
  visible.value = true
}, 60)
</script>

<template>
  <div class="upload-page" :class="{ 'upload-page--visible': visible }">
    <div class="page-header">
      <h1 class="page-title">Data upload</h1>
      <p class="page-lead">
        Import datasets into the ETL pipeline. You can upload files individually or queue multiple files for a single, synchronized import.
      </p>
    </div>

    <section class="upload-section">
      <div class="section-header">
        <h2 class="section-title">Data Sources</h2>
        <p class="section-desc">Accepted formats: <strong>.xlsx, .csv, .tsv</strong>.</p>
      </div>

      <div class="upload-grid">
        <article
          v-for="cat in categories"
          :key="cat.fileType"
          class="upload-card"
          :class="{
            'upload-card--staged': slots[cat.fileType].file,
            'upload-card--err': slots[cat.fileType].error,
            'upload-card--disabled': batchStatus === 'uploading'
          }"
        >
          <div class="card-head">
            <span class="card-icon-wrap" aria-hidden="true">
              <i :class="`pi ${cat.icon} card-icon`"></i>
            </span>
            <div class="card-titles">
              <h3 class="card-title">{{ cat.title }}</h3>
              <p class="card-desc">{{ cat.description }}</p>
            </div>
          </div>

          <label
            :for="`file-${cat.fileType}`"
            class="drop-zone"
            @dragover.prevent
            @drop.prevent="onDropFile(cat.fileType, $event)"
          >
            <input
              :id="`file-${cat.fileType}`"
              type="file"
              class="sr-only"
              :accept="acceptedFormats"
              :disabled="batchStatus === 'uploading'"
              @change="onFileSelected(cat.fileType, $event)"
            />
            <i class="pi pi-cloud-upload drop-icon"></i>
            <span class="drop-text">
              <span class="drop-primary">Choose a file</span>
              <span class="drop-muted">or drop here</span>
            </span>
          </label>

          <div v-if="slots[cat.fileType].file" class="file-row">
            <span class="file-name" :title="slots[cat.fileType].file!.name">
              <i class="pi pi-file"></i> {{ slots[cat.fileType].file!.name }}
            </span>
            <button
              type="button"
              class="btn btn--ghost btn--small"
              :disabled="batchStatus === 'uploading'"
              @click="clearSlot(cat.fileType)"
            >
              Clear
            </button>
          </div>

          <p v-if="slots[cat.fileType].error" class="status-msg error-text">
            <i class="pi pi-exclamation-circle status-icon"></i>
            {{ slots[cat.fileType].error }}
          </p>
        </article>
      </div>
    </section>

    <section class="batch-actions-section">
      <div class="batch-buttons">
        <button 
          class="btn btn--ghost" 
          @click="clearAllSlots" 
          :disabled="!hasStagedFiles || batchStatus === 'uploading'">
          Clear All
        </button>
        <button 
          class="btn btn--primary btn--large" 
          @click="uploadBatch" 
          :disabled="!hasStagedFiles || batchStatus === 'uploading'">
          <span v-if="batchStatus === 'uploading'"><i class="pi pi-spin pi-spinner"></i> Uploading Batch...</span>
          <span v-else><i class="pi pi-upload"></i> Upload Selected Files</span>
        </button>
      </div>

      <div v-if="globalMessage" class="global-alert" :class="`alert--${batchStatus}`">
        <div class="alert-header">
          <i v-if="batchStatus === 'success'" class="pi pi-check-circle"></i>
          <i v-else-if="batchStatus === 'error'" class="pi pi-times-circle"></i>
          <strong>{{ globalMessage }}</strong>
        </div>
        
        <div v-if="globalWarnings.length > 0" class="warnings-list">
          <p class="warnings-title">Warnings generated during import:</p>
          <ul>
            <li v-for="(warn, idx) in globalWarnings" :key="idx">{{ warn }}</li>
          </ul>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.upload-page {
  padding: 0 28px 60px;
  max-width: 980px;
  opacity: 0;
  transform: translateY(6px);
  transition:
    opacity 0.35s ease,
    transform 0.35s ease;
}

.upload-page--visible {
  opacity: 1;
  transform: none;
}

.page-header {
  padding: 22px 0 18px;
  border-bottom: 1px solid var(--c-border);
  margin-bottom: 28px;
}

.page-title {
  font-family: 'DM Sans', sans-serif;
  font-size: 18px;
  font-weight: 400;
  color: var(--c-heading);
  margin-bottom: 8px;
}

.page-lead {
  font-size: 12.5px;
  color: var(--c-text-muted);
  line-height: 1.55;
  max-width: 640px;
}

.upload-section {
  padding-bottom: 24px;
}

.section-header {
  margin-bottom: 18px;
}

.section-title {
  font-family: 'DM Sans', sans-serif;
  font-size: 14px;
  font-weight: 600;
  color: var(--c-heading);
  margin-bottom: 3px;
}

.section-desc {
  font-size: 12.5px;
  color: var(--c-text-muted);
}

.section-desc strong {
  font-weight: 600;
  color: var(--c-text);
}

.upload-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 820px) {
  .upload-grid {
    grid-template-columns: 1fr;
  }
}

.upload-card {
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: 10px;
  padding: 18px 18px 16px;
  box-shadow: var(--c-shadow);
  display: flex;
  flex-direction: column;
  gap: 14px;
  transition:
    border-color 0.18s,
    box-shadow 0.18s,
    opacity 0.2s;
}

.upload-card--staged {
  border-color: color-mix(in srgb, var(--c-brand) 45%, var(--c-border));
  background: color-mix(in srgb, var(--c-brand) 3%, var(--c-bg));
}

.upload-card--disabled {
  opacity: 0.6;
  pointer-events: none;
}

.upload-card--err {
  border-color: color-mix(in srgb, var(--c-red) 45%, var(--c-border));
}

.card-head {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.card-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: var(--c-brand-dim);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.card-icon {
  font-size: 18px;
  color: var(--c-brand);
}

.card-titles {
  min-width: 0;
}

.card-title {
  font-family: 'DM Sans', sans-serif;
  font-size: 13px;
  font-weight: 600;
  color: var(--c-heading);
  line-height: 1.3;
  margin-bottom: 4px;
}

.card-desc {
  font-size: 11.5px;
  color: var(--c-text-muted);
  line-height: 1.45;
}

.drop-zone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px 14px;
  border: 1px dashed var(--c-border-strong);
  border-radius: 8px;
  background: var(--c-bg);
  cursor: pointer;
  transition:
    border-color 0.15s,
    background 0.15s;
}

.drop-zone:hover {
  border-color: var(--c-brand);
  background: var(--c-brand-dim);
}

.drop-icon {
  font-size: 22px;
  color: var(--c-brand);
  opacity: 0.85;
}

.drop-text {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  text-align: center;
}

.drop-primary {
  font-size: 12.5px;
  font-weight: 500;
  color: var(--c-heading);
}

.drop-muted {
  font-size: 11px;
  color: var(--c-text-dim);
}

.file-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--c-bg);
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid var(--c-border);
}

.file-name {
  font-size: 12px;
  font-weight: 500;
  color: var(--c-brand);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn {
  font-family: 'DM Sans', sans-serif;
  font-size: 13px;
  font-weight: 500;
  padding: 8px 16px;
  border-radius: 6px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.15s ease;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.btn--small {
  font-size: 11px;
  padding: 4px 10px;
}

.btn--large {
  font-size: 14px;
  padding: 10px 24px;
}

.btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.btn--primary {
  background: var(--c-brand);
  color: #fff;
  border-color: var(--c-brand);
}

.btn--primary:hover:not(:disabled) {
  filter: brightness(1.08);
}

.btn--ghost {
  background: transparent;
  color: var(--c-text-muted);
  border-color: var(--c-border-strong);
}

.btn--ghost:hover:not(:disabled) {
  background: var(--c-brand-dim);
  color: var(--c-heading);
  border-color: var(--c-border-strong);
}

.batch-actions-section {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  border-top: 1px solid var(--c-border);
  padding-top: 24px;
}

.batch-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.global-alert {
  padding: 16px;
  border-radius: 8px;
  font-size: 13px;
  border: 1px solid transparent;
}

.alert--success {
  background: color-mix(in srgb, var(--c-green) 10%, transparent);
  border-color: color-mix(in srgb, var(--c-green) 30%, transparent);
  color: color-mix(in srgb, var(--c-green) 80%, black);
}

.alert--error {
  background: color-mix(in srgb, var(--c-red) 10%, transparent);
  border-color: color-mix(in srgb, var(--c-red) 30%, transparent);
  color: color-mix(in srgb, var(--c-red) 80%, black);
}

.alert-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.warnings-list {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed currentColor;
}

.warnings-title {
  font-weight: 600;
  margin-bottom: 6px;
}

.warnings-list ul {
  margin: 0;
  padding-left: 20px;
  font-size: 12.5px;
  opacity: 0.9;
}

.status-msg {
  font-size: 11.5px;
  line-height: 1.45;
  margin: 0;
  display: flex;
  align-items: flex-start;
  gap: 6px;
}

.error-text {
  color: var(--c-red);
}
</style>