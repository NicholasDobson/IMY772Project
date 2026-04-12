<script setup lang="ts">
import { reactive, ref } from 'vue'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

type EtlFileType = 'EPICOLLECT' | 'BINARY_INFO' | 'AMR_FINDER' | 'STAR_AMR'

const categories: {
  fileType: EtlFileType
  title: string
  description: string
  icon: string
}[] = [
  {
    fileType: 'EPICOLLECT',
    title: 'Epicollect (Field Data)',
    description: 'Field collection exports from Epicollect5.',
    icon: 'pi pi-table',
  },
  {
    fileType: 'BINARY_INFO',
    title: 'Binary Info (Isolates)',
    description: 'Isolate and binary characterisation spreadsheets.',
    icon: 'pi pi-database',
  },
  {
    fileType: 'AMR_FINDER',
    title: 'AMR Finder (Gene Sequences)',
    description: 'Resistance gene detection outputs for sequences.',
    icon: 'pi pi-sitemap',
  },
  {
    fileType: 'STAR_AMR',
    title: 'Star AMR (WGS Metrics)',
    description: 'Whole-genome sequencing AMR metrics and summaries.',
    icon: 'pi pi-chart-bar',
  },
]

type SlotStatus = 'idle' | 'uploading' | 'success' | 'error'

interface SlotState {
  file: File | null
  status: SlotStatus
  message: string
}

function initialSlots(): Record<EtlFileType, SlotState> {
  return {
    EPICOLLECT: { file: null, status: 'idle', message: '' },
    BINARY_INFO: { file: null, status: 'idle', message: '' },
    AMR_FINDER: { file: null, status: 'idle', message: '' },
    STAR_AMR: { file: null, status: 'idle', message: '' },
  }
}

const slots = reactive<Record<EtlFileType, SlotState>>(initialSlots())

const xlsxAccept = '.xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'

function isXlsx(file: File): boolean {
  const n = file.name.toLowerCase()
  return n.endsWith('.xlsx')
}

function assignFile(fileType: EtlFileType, file: File | null, clearInputId?: string): void {
  if (!file) return

  if (!isXlsx(file)) {
    slots[fileType].file = null
    slots[fileType].status = 'error'
    slots[fileType].message = 'Only .xlsx workbooks are accepted.'
    if (clearInputId) {
      const el = document.getElementById(clearInputId) as HTMLInputElement | null
      if (el) el.value = ''
    }
    return
  }

  slots[fileType].file = file
  slots[fileType].status = 'idle'
  slots[fileType].message = ''
}

function onFileSelected(fileType: EtlFileType, ev: Event): void {
  const input = ev.target as HTMLInputElement
  const file = input.files?.[0] ?? null
  if (!file) return
  assignFile(fileType, file, `file-${fileType}`)
}

function onDropFile(fileType: EtlFileType, ev: DragEvent): void {
  const file = ev.dataTransfer?.files?.[0] ?? null
  if (!file) return
  assignFile(fileType, file, `file-${fileType}`)
}

function clearSlot(fileType: EtlFileType): void {
  slots[fileType].file = null
  slots[fileType].status = 'idle'
  slots[fileType].message = ''
  const el = document.getElementById(`file-${fileType}`) as HTMLInputElement | null
  if (el) el.value = ''
}

async function uploadSlot(fileType: EtlFileType): Promise<void> {
  const slot = slots[fileType]
  if (!slot.file) return

  slot.status = 'uploading'
  slot.message = ''

  const formData = new FormData()
  formData.append('file', slot.file)
  formData.append('fileType', fileType)

  try {
    const res = await fetch(`${API_BASE}/etl/upload`, {
      method: 'POST',
      body: formData,
    })
    const text = await res.text()
    if (res.ok) {
      slot.status = 'success'
      slot.message = text || 'Upload completed.'
      slot.file = null
      const el = document.getElementById(`file-${fileType}`) as HTMLInputElement | null
      if (el) el.value = ''
    } else {
      slot.status = 'error'
      slot.message = text || `Request failed (${res.status}).`
    }
  } catch (e) {
    slot.status = 'error'
    slot.message =
      e instanceof Error
        ? e.message
        : 'Network error. Check that the API is running and CORS is configured.'
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
        Import Excel workbooks into the ETL pipeline. Each data source has its own slot so files are
        labelled correctly on the server.
      </p>
    </div>

    <section class="upload-section">
      <div class="section-header">
        <h2 class="section-title">Excel workbooks</h2>
        <p class="section-desc">Only <strong>.xlsx</strong> files are accepted for now.</p>
      </div>

      <div class="upload-grid">
        <article
          v-for="cat in categories"
          :key="cat.fileType"
          class="upload-card"
          :class="{
            'upload-card--busy': slots[cat.fileType].status === 'uploading',
            'upload-card--ok': slots[cat.fileType].status === 'success',
            'upload-card--err': slots[cat.fileType].status === 'error',
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
              :accept="xlsxAccept"
              :disabled="slots[cat.fileType].status === 'uploading'"
              @change="onFileSelected(cat.fileType, $event)"
            />
            <i class="pi pi-cloud-upload drop-icon"></i>
            <span class="drop-text">
              <span class="drop-primary">Choose .xlsx file</span>
              <span class="drop-muted">or drop here (browser permitting)</span>
            </span>
          </label>

          <div v-if="slots[cat.fileType].file" class="file-row">
            <span class="file-name" :title="slots[cat.fileType].file!.name">{{
              slots[cat.fileType].file!.name
            }}</span>
            <div class="file-actions">
              <button
                type="button"
                class="btn btn--ghost"
                :disabled="slots[cat.fileType].status === 'uploading'"
                @click="clearSlot(cat.fileType)"
              >
                Clear
              </button>
              <button
                type="button"
                class="btn btn--primary"
                :disabled="slots[cat.fileType].status === 'uploading'"
                @click="uploadSlot(cat.fileType)"
              >
                <span v-if="slots[cat.fileType].status !== 'uploading'">Upload</span>
                <span v-else class="uploading-label"><i class="pi pi-spin pi-spinner"></i> Uploading…</span>
              </button>
            </div>
          </div>

          <p v-if="slots[cat.fileType].message" class="status-msg" role="status">
            <i
              v-if="slots[cat.fileType].status === 'success'"
              class="pi pi-check-circle status-icon status-icon--ok"
            ></i>
            <i
              v-else-if="slots[cat.fileType].status === 'error'"
              class="pi pi-times-circle status-icon status-icon--err"
            ></i>
            {{ slots[cat.fileType].message }}
          </p>
        </article>
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
  transition: opacity 0.35s ease, transform 0.35s ease;
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
  transition: border-color 0.18s, box-shadow 0.18s;
}

.upload-card--busy {
  border-color: color-mix(in srgb, var(--c-brand) 35%, var(--c-border));
}

.upload-card--ok {
  border-color: color-mix(in srgb, var(--c-green) 45%, var(--c-border));
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
  transition: border-color 0.15s, background 0.15s;
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
  flex-direction: column;
  gap: 10px;
}

.file-name {
  font-size: 12px;
  color: var(--c-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.btn {
  font-family: 'DM Sans', sans-serif;
  font-size: 12px;
  font-weight: 500;
  padding: 7px 14px;
  border-radius: 6px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: background 0.12s, border-color 0.12s, color 0.12s;
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
  filter: brightness(1.06);
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

.uploading-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.status-msg {
  font-size: 11.5px;
  line-height: 1.45;
  color: var(--c-text-muted);
  margin: 0;
  padding-top: 2px;
  display: flex;
  align-items: flex-start;
  gap: 6px;
}

.status-icon {
  font-size: 13px;
  margin-top: 1px;
  flex-shrink: 0;
}

.status-icon--ok {
  color: var(--c-green);
}

.status-icon--err {
  color: var(--c-red);
}
</style>
