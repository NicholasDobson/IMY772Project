<script setup lang="ts">
import { reactive, ref, computed } from 'vue'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

type EtlFileType = 'EPICOLLECT' | 'BINARY_INFO' | 'AMR_FINDER' | 'STAR_AMR'

interface FileStructure {
  headers: string[]
  exampleRow: string[]
}

interface CategoryInfo {
  fileType: EtlFileType
  formKey: string
  title: string
  description: string
  icon: string
  structure: FileStructure
}

const categories: CategoryInfo[] = [
  {
    fileType: 'EPICOLLECT',
    formKey: 'epicollect',
    title: 'Epicollect (Field Data)',
    description: 'Field collection exports from Epicollect5.',
    icon: 'pi pi-table',
    structure: {
      headers: ['Site ID', 'Location Name', 'River Name', 'Lat', 'Lng', 'Sample ID', 'Sample Name', 'Analysis Type', 'Trip ID', 'Date', 'Temp', 'pH', 'TDS', 'EC', 'DO', 'Collector Email'],
      exampleRow: ['A11', 'Pretoria (Midstream)', 'Apies River', '-25.7470', '28.2290', 'SAMP-0001', 'AR-001-2401', 'WGS', '2024-T01', '2024-01-09', '13.5', '6.7', '107', '714', '4', 'jane.doe@tuks.co.za']
    }
  },
  {
    fileType: 'BINARY_INFO',
    formKey: 'binaryInfo',
    title: 'Binary Info (Isolates)',
    description: 'Isolate and binary characterisation spreadsheets.',
    icon: 'pi pi-database',
    structure: {
      headers: ['Sample ID', 'Isolate ID', 'Isolate Number', 'Organism', 'Context', 'AR Code', 'Virulence Genes', 'Intl1', 'Intl2', 'Intl3', 'TEM', 'SHV', 'Owner Email'],
      exampleRow: ['SAMP-0001', 'ISO-101', 'A112H', 'Citrobacter freundii', 'River water (upstream)', 'ESBL', 'fyuA', '1', '1', '1', '0', '1', 'jane.doe@tuks.co.za']
    }
  },
  {
    fileType: 'AMR_FINDER',
    formKey: 'amrFinder',
    title: 'AMR Finder (Gene Sequences)',
    description: 'Resistance gene detection outputs for sequences.',
    icon: 'pi pi-sitemap',
    structure: {
      headers: ['Isolate ID', 'Gene Symbol', 'Sequence Name', 'Element Type', 'Class', 'Subclass', 'Target Length', 'Reference Length', 'Identity %', 'Coverage %', 'Alignment Length', 'Accession'],
      exampleRow: ['ISO-100', 'aph(3\')-Ia', 'aminoglycoside O-phosphotransferase APH(3\')-Ia', 'AMR', 'AMINOGLYCOSIDE', 'KANAMYCIN', '816', '816', '79.23', '76.44', '623', 'WP_015345217.1']
    }
  },
  {
    fileType: 'STAR_AMR',
    formKey: 'starAmr',
    title: 'Star AMR (WGS Metrics)',
    description: 'Whole-genome sequencing AMR metrics and summaries.',
    icon: 'pi pi-chart-bar',
    structure: {
      headers: ['Isolate ID', 'Quality Status', 'Genotype', 'Predicted Phenotype', 'SIR Profile', 'Plasmid', 'Genome Length', 'N50 Value'],
      exampleRow: ['ISO-100', 'Passed', 'aph(6)-Id', 'ertapenem', 'Intermediate', 'IncFIB(AP001918)', '4703638', '500571']
    }
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

// Info Modal State
const selectedInfo = ref<CategoryInfo | null>(null)

const hasStagedFiles = computed(() => Object.values(slots).some(slot => slot.file !== null))

// --- Upload mode tabs ---
type UploadMode = 'multi' | 'single' | 'imports'
const activeTab = ref<UploadMode>('multi')

function switchTab(tab: UploadMode): void {
  activeTab.value = tab
  if (tab === 'imports') fetchImports()
}

// --- Single File Upload ---
// The single file is a flattened subset of the four multi-file sources. Columns not
// present here are stored blank. (See docs mapping — Site is keyed on Location.)
const singleCategory: CategoryInfo = {
  fileType: 'EPICOLLECT', // filler: not used for single-file routing
  formKey: 'file',
  title: 'Single File',
  description: 'One consolidated file containing the essential subset of all data.',
  icon: 'pi pi-file-import',
  structure: {
    headers: [
      'Sample Name', 'Sample Analysis Type', 'Isolate ID', 'Organism', 'Location', 'River Name',
      'Isolation source', 'Collection Date', 'Latitude', 'Longitude', 'Collected By',
      'AMR Resistance genes', 'Sequence Name', 'Element type', 'Class', 'Subclass',
      'Target length', 'Reference sequence length', '% Coverage of reference sequence',
      '% Identity to reference sequence', 'Alignment Length', 'Accession of Closest Sequence',
      'Virulence Genes', 'Plasmid Replicons', 'Predicted SIR profile',
      'pH', 'Temp of water', 'TDS (mg/L)', 'Dissolved Oxygen (mg/L)'
    ],
    exampleRow: [
      'SAMP-0001', 'WGS', 'ISO-100', 'Klebsiella pneumoniae', 'Groenkloof', 'Apies River',
      'River Water', '30-11-2017', '-25.7470', '28.2290', 'jane.doe@tuks.co.za',
      "aph(3')-Ia", "aminoglycoside O-phosphotransferase APH(3')-Ia", 'AMR', 'AMINOGLYCOSIDE', 'KANAMYCIN',
      '816', '816', '76,44',
      '79,23', '623', 'WP_015345217.1',
      'fyuA', 'IncFII', 'R',
      '6,7', '13,5', '107', '4'
    ]
  }
}

const singleSlot = reactive<SlotState>({ file: null, error: '' })
const singleStatus = ref<'idle' | 'uploading' | 'success' | 'error'>('idle')
const singleMessage = ref('')
const singleWarnings = ref<string[]>([])
const hasSingleFile = computed(() => singleSlot.file !== null)

function assignSingleFile(file: File | null): void {
  if (!file) return

  if (!isValidFormat(file)) {
    singleSlot.file = null
    singleSlot.error = 'Invalid format. Use .xlsx, .csv, or .tsv'
    const el = document.getElementById('file-single') as HTMLInputElement | null
    if (el) el.value = ''
    return
  }

  singleSlot.file = file
  singleSlot.error = ''
  singleStatus.value = 'idle'
  singleMessage.value = ''
  singleWarnings.value = []
}

function onSingleFileSelected(ev: Event): void {
  const input = ev.target as HTMLInputElement
  assignSingleFile(input.files?.[0] ?? null)
}

function onDropSingleFile(ev: DragEvent): void {
  assignSingleFile(ev.dataTransfer?.files?.[0] ?? null)
}

function clearSingle(): void {
  singleSlot.file = null
  singleSlot.error = ''
  singleStatus.value = 'idle'
  singleMessage.value = ''
  singleWarnings.value = []
  const el = document.getElementById('file-single') as HTMLInputElement | null
  if (el) el.value = ''
}

async function uploadSingle(): Promise<void> {
  if (!hasSingleFile.value) return

  singleStatus.value = 'uploading'
  singleMessage.value = ''
  singleWarnings.value = []

  const formData = new FormData()
  formData.append('file', singleSlot.file as File)

  try {
    const res = await fetch(`${API_BASE}/etl/upload-single`, {
      method: 'POST',
      body: formData,
    })

    if (res.ok) {
      const data = await res.json()
      singleStatus.value = 'success'
      singleMessage.value = data.message || 'File uploaded successfully.'
      singleWarnings.value = data.warnings || []

      singleSlot.file = null
      const el = document.getElementById('file-single') as HTMLInputElement | null
      if (el) el.value = ''
      fetchImports()
    } else {
      const errorText = await res.text()
      singleStatus.value = 'error'
      singleMessage.value = errorText || `Upload failed with status: ${res.status}`
    }
  } catch (e) {
    singleStatus.value = 'error'
    singleMessage.value = e instanceof Error ? e.message : 'Network error. Ensure the backend is running.'
  }
}

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

function openInfo(cat: CategoryInfo) {
  selectedInfo.value = cat
}

function closeInfo() {
  selectedInfo.value = null
}

async function uploadBatch(): Promise<void> {
  if (!hasStagedFiles.value) return

  batchStatus.value = 'uploading'
  globalMessage.value = ''
  globalWarnings.value = []

  const formData = new FormData()
  
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

      Object.keys(slots).forEach((key) => clearSlot(key as EtlFileType))
      fetchImports()
    } else {
      const errorText = await res.text()
      batchStatus.value = 'error'
      globalMessage.value = errorText || `Upload failed with status: ${res.status}`
    }
  } catch (e) {
    batchStatus.value = 'error'
    globalMessage.value = e instanceof Error ? e.message : 'Network error. Ensure the backend is running.'
  }
}

// --- Imports tab (history + rollback) ---
interface ImportRecord {
  importId: string
  importType: 'SINGLE' | 'MULTI' | string
  fileNames: string | null
  importedAt: string | null
  siteCount: number | null
  sampleCount: number | null
  isolateCount: number | null
  sequenceCount: number | null
  wgsCount: number | null
}

const imports = ref<ImportRecord[]>([])
const importsLoading = ref(false)
const importsError = ref('')
const rollbackTarget = ref<ImportRecord | null>(null)
const rollbackBusy = ref(false)
const importsNotice = ref('')
const importsNoticeType = ref<'success' | 'error'>('success')

async function fetchImports(): Promise<void> {
  importsLoading.value = true
  importsError.value = ''
  try {
    const res = await fetch(`${API_BASE}/etl/imports`)
    if (res.ok) {
      imports.value = await res.json()
    } else {
      importsError.value = `Failed to load imports (status ${res.status}).`
    }
  } catch (e) {
    importsError.value = e instanceof Error ? e.message : 'Network error. Ensure the backend is running.'
  } finally {
    importsLoading.value = false
  }
}

function askRollback(record: ImportRecord): void {
  rollbackTarget.value = record
}

function cancelRollback(): void {
  if (rollbackBusy.value) return
  rollbackTarget.value = null
}

async function confirmRollback(): Promise<void> {
  const target = rollbackTarget.value
  if (!target) return

  rollbackBusy.value = true
  importsNotice.value = ''
  try {
    const res = await fetch(`${API_BASE}/etl/imports/${target.importId}`, { method: 'DELETE' })
    if (res.ok) {
      importsNoticeType.value = 'success'
      importsNotice.value = `Rolled back the ${target.importType === 'SINGLE' ? 'single-file' : 'multi-file'} import and deleted its data.`
      rollbackTarget.value = null
      await fetchImports()
    } else {
      const text = await res.text()
      importsNoticeType.value = 'error'
      importsNotice.value = text || `Rollback failed (status ${res.status}).`
    }
  } catch (e) {
    importsNoticeType.value = 'error'
    importsNotice.value = e instanceof Error ? e.message : 'Network error. Ensure the backend is running.'
  } finally {
    rollbackBusy.value = false
  }
}

function totalRows(r: ImportRecord): number {
  return (r.siteCount || 0) + (r.sampleCount || 0) + (r.isolateCount || 0) + (r.sequenceCount || 0) + (r.wgsCount || 0)
}

function formatDate(iso: string | null): string {
  if (!iso) return '—'
  const d = new Date(iso)
  if (isNaN(d.getTime())) return iso
  return d.toLocaleString(undefined, {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
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

    <div class="tabs" role="tablist">
      <button
        class="tab"
        :class="{ 'tab--active': activeTab === 'multi' }"
        role="tab"
        :aria-selected="activeTab === 'multi'"
        @click="switchTab('multi')"
      >
        <i class="pi pi-clone"></i> Multi file
      </button>
      <button
        class="tab"
        :class="{ 'tab--active': activeTab === 'single' }"
        role="tab"
        :aria-selected="activeTab === 'single'"
        @click="switchTab('single')"
      >
        <i class="pi pi-file-import"></i> Single file
      </button>
      <button
        class="tab"
        :class="{ 'tab--active': activeTab === 'imports' }"
        role="tab"
        :aria-selected="activeTab === 'imports'"
        @click="switchTab('imports')"
      >
        <i class="pi pi-history"></i> Imports
      </button>
    </div>

    <section v-show="activeTab === 'multi'" class="upload-section">
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
              <div class="card-title-row">
                <h3 class="card-title">{{ cat.title }}</h3>
                <button class="info-btn" @click.prevent="openInfo(cat)" title="View required format">
                  <i class="pi pi-info-circle"></i>
                </button>
              </div>
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

    <section v-show="activeTab === 'multi'" class="batch-actions-section">
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

    <section v-show="activeTab === 'single'" class="upload-section">
      <div class="section-header">
        <h2 class="section-title">Single File</h2>
        <p class="section-desc">
          Upload one consolidated file containing the essential subset of all data.
          Fields not included in this file are stored blank. Accepted formats: <strong>.xlsx, .csv, .tsv</strong>.
        </p>
      </div>

      <article
        class="upload-card single-card"
        :class="{
          'upload-card--staged': singleSlot.file,
          'upload-card--err': singleSlot.error,
          'upload-card--disabled': singleStatus === 'uploading'
        }"
      >
        <div class="card-head">
          <span class="card-icon-wrap" aria-hidden="true">
            <i :class="`pi ${singleCategory.icon} card-icon`"></i>
          </span>
          <div class="card-titles">
            <div class="card-title-row">
              <h3 class="card-title">{{ singleCategory.title }}</h3>
              <button class="info-btn" @click.prevent="openInfo(singleCategory)" title="View required format">
                <i class="pi pi-info-circle"></i>
              </button>
            </div>
            <p class="card-desc">{{ singleCategory.description }}</p>
          </div>
        </div>

        <label
          for="file-single"
          class="drop-zone"
          @dragover.prevent
          @drop.prevent="onDropSingleFile($event)"
        >
          <input
            id="file-single"
            type="file"
            class="sr-only"
            :accept="acceptedFormats"
            :disabled="singleStatus === 'uploading'"
            @change="onSingleFileSelected($event)"
          />
          <i class="pi pi-cloud-upload drop-icon"></i>
          <span class="drop-text">
            <span class="drop-primary">Choose a file</span>
            <span class="drop-muted">or drop here</span>
          </span>
        </label>

        <div v-if="singleSlot.file" class="file-row">
          <span class="file-name" :title="singleSlot.file!.name">
            <i class="pi pi-file"></i> {{ singleSlot.file!.name }}
          </span>
          <button
            type="button"
            class="btn btn--ghost btn--small"
            :disabled="singleStatus === 'uploading'"
            @click="clearSingle"
          >
            Clear
          </button>
        </div>

        <p v-if="singleSlot.error" class="status-msg error-text">
          <i class="pi pi-exclamation-circle status-icon"></i>
          {{ singleSlot.error }}
        </p>
      </article>

      <div class="batch-actions-section single-actions">
        <div class="batch-buttons">
          <button
            class="btn btn--ghost"
            @click="clearSingle"
            :disabled="!hasSingleFile || singleStatus === 'uploading'">
            Clear
          </button>
          <button
            class="btn btn--primary btn--large"
            @click="uploadSingle"
            :disabled="!hasSingleFile || singleStatus === 'uploading'">
            <span v-if="singleStatus === 'uploading'"><i class="pi pi-spin pi-spinner"></i> Uploading...</span>
            <span v-else><i class="pi pi-upload"></i> Upload File</span>
          </button>
        </div>

        <div v-if="singleMessage" class="global-alert" :class="`alert--${singleStatus}`">
          <div class="alert-header">
            <i v-if="singleStatus === 'success'" class="pi pi-check-circle"></i>
            <i v-else-if="singleStatus === 'error'" class="pi pi-times-circle"></i>
            <strong>{{ singleMessage }}</strong>
          </div>

          <div v-if="singleWarnings.length > 0" class="warnings-list">
            <p class="warnings-title">Warnings generated during import:</p>
            <ul>
              <li v-for="(warn, idx) in singleWarnings" :key="idx">{{ warn }}</li>
            </ul>
          </div>
        </div>
      </div>
    </section>

    <section v-show="activeTab === 'imports'" class="upload-section">
      <div class="section-header section-header--row">
        <div>
          <h2 class="section-title">Previous imports</h2>
          <p class="section-desc">
            Each row is one upload. Rolling back permanently deletes the data that import created.
          </p>
        </div>
        <button class="btn btn--ghost btn--small" @click="fetchImports" :disabled="importsLoading">
          <i class="pi" :class="importsLoading ? 'pi-spin pi-spinner' : 'pi-refresh'"></i> Refresh
        </button>
      </div>

      <div v-if="importsNotice" class="global-alert" :class="`alert--${importsNoticeType}`">
        <div class="alert-header">
          <i v-if="importsNoticeType === 'success'" class="pi pi-check-circle"></i>
          <i v-else class="pi pi-times-circle"></i>
          <strong>{{ importsNotice }}</strong>
        </div>
      </div>

      <p v-if="importsError" class="status-msg error-text">
        <i class="pi pi-exclamation-circle status-icon"></i> {{ importsError }}
      </p>

      <div v-if="importsLoading && imports.length === 0" class="imports-empty">
        <i class="pi pi-spin pi-spinner"></i> Loading imports…
      </div>

      <div v-else-if="imports.length === 0 && !importsError" class="imports-empty">
        <i class="pi pi-inbox"></i>
        <p>No imports yet. Upload a single or multi-file dataset to see it here.</p>
      </div>

      <div v-else class="table-responsive imports-table-wrap">
        <table class="format-table imports-table">
          <thead>
            <tr>
              <th>Type</th>
              <th>File(s)</th>
              <th>Date</th>
              <th>Sites</th>
              <th>Samples</th>
              <th>Isolates</th>
              <th>Sequences</th>
              <th>WGS</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="imp in imports" :key="imp.importId">
              <td>
                <span class="type-badge" :class="imp.importType === 'SINGLE' ? 'type-badge--single' : 'type-badge--multi'">
                  <i class="pi" :class="imp.importType === 'SINGLE' ? 'pi-file-import' : 'pi-clone'"></i>
                  {{ imp.importType === 'SINGLE' ? 'Single' : 'Multi' }}
                </span>
              </td>
              <td class="files-cell" :title="imp.fileNames || ''">{{ imp.fileNames || '—' }}</td>
              <td>{{ formatDate(imp.importedAt) }}</td>
              <td>{{ imp.siteCount ?? 0 }}</td>
              <td>{{ imp.sampleCount ?? 0 }}</td>
              <td>{{ imp.isolateCount ?? 0 }}</td>
              <td>{{ imp.sequenceCount ?? 0 }}</td>
              <td>{{ imp.wgsCount ?? 0 }}</td>
              <td class="action-cell">
                <button
                  class="btn btn--danger btn--small"
                  @click="askRollback(imp)"
                  :disabled="rollbackBusy"
                >
                  <i class="pi pi-undo"></i> Rollback
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <Transition name="fade">
      <div v-if="rollbackTarget" class="modal-backdrop" @click="cancelRollback">
        <div class="modal-content modal-content--narrow" @click.stop>
          <div class="modal-header">
            <div class="modal-title-group">
              <i class="pi pi-exclamation-triangle modal-title-icon modal-title-icon--danger"></i>
              <h2>Roll back this import?</h2>
            </div>
            <button class="modal-close-btn" @click="cancelRollback" :disabled="rollbackBusy"><i class="pi pi-times"></i></button>
          </div>

          <div class="modal-body">
            <p class="modal-instruction">
              This will <strong>permanently delete</strong> all data created by this
              {{ rollbackTarget.importType === 'SINGLE' ? 'single-file' : 'multi-file' }} import. This action cannot be undone.
            </p>
            <ul class="rollback-summary">
              <li><strong>File(s):</strong> {{ rollbackTarget.fileNames || '—' }}</li>
              <li><strong>Imported:</strong> {{ formatDate(rollbackTarget.importedAt) }}</li>
              <li>
                <strong>Will delete:</strong>
                {{ rollbackTarget.siteCount ?? 0 }} sites,
                {{ rollbackTarget.sampleCount ?? 0 }} samples,
                {{ rollbackTarget.isolateCount ?? 0 }} isolates,
                {{ rollbackTarget.sequenceCount ?? 0 }} sequences,
                {{ rollbackTarget.wgsCount ?? 0 }} WGS records
                ({{ totalRows(rollbackTarget) }} rows total)
              </li>
            </ul>
          </div>

          <div class="modal-footer">
            <button class="btn btn--ghost" @click="cancelRollback" :disabled="rollbackBusy">Cancel</button>
            <button class="btn btn--danger" @click="confirmRollback" :disabled="rollbackBusy">
              <span v-if="rollbackBusy"><i class="pi pi-spin pi-spinner"></i> Rolling back…</span>
              <span v-else><i class="pi pi-undo"></i> Yes, roll back</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <Transition name="fade">
      <div v-if="selectedInfo" class="modal-backdrop" @click="closeInfo">
        <div class="modal-content" @click.stop>
          <div class="modal-header">
            <div class="modal-title-group">
              <i :class="`pi ${selectedInfo.icon} modal-title-icon`"></i>
              <h2>{{ selectedInfo.title }} Format</h2>
            </div>
            <button class="modal-close-btn" @click="closeInfo"><i class="pi pi-times"></i></button>
          </div>
          
          <div class="modal-body">
            <p class="modal-instruction">
              Your file must contain the following column headers. The order of the columns does not matter, but the header names must match exactly.
            </p>
            
            <div class="table-responsive">
              <table class="format-table">
                <thead>
                  <tr>
                    <th v-for="header in selectedInfo.structure.headers" :key="header">{{ header }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td v-for="(cell, idx) in selectedInfo.structure.exampleRow" :key="idx">{{ cell }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
/* Keeping all your existing styles below */
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

.tabs {
  display: flex;
  gap: 4px;
  border-bottom: 1px solid var(--c-border);
  margin-bottom: 24px;
}

.tab {
  font-family: 'DM Sans', sans-serif;
  font-size: 13px;
  font-weight: 500;
  color: var(--c-text-muted);
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  padding: 10px 16px;
  margin-bottom: -1px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: color 0.15s, border-color 0.15s;
}

.tab:hover {
  color: var(--c-heading);
}

.tab--active {
  color: var(--c-brand);
  border-bottom-color: var(--c-brand);
}

.upload-section {
  padding-bottom: 24px;
}

.single-card {
  max-width: 520px;
}

.single-actions {
  border-top: none;
  padding-top: 20px;
  margin-top: 0;
}

/* --- Imports tab --- */
.section-header--row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.imports-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 48px 20px;
  color: var(--c-text-muted);
  font-size: 13px;
  text-align: center;
  border: 1px dashed var(--c-border-strong);
  border-radius: 10px;
  background: var(--c-bg);
}

.imports-empty .pi {
  font-size: 24px;
  color: var(--c-text-dim);
}

.imports-table-wrap {
  margin-top: 8px;
}

.imports-table th,
.imports-table td {
  white-space: nowrap;
}

.imports-table .files-cell {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.imports-table .action-cell {
  text-align: right;
}

.type-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 9px;
  border-radius: 999px;
  border: 1px solid transparent;
}

.type-badge--single {
  background: var(--c-brand-dim);
  color: var(--c-brand);
  border-color: color-mix(in srgb, var(--c-brand) 30%, transparent);
}

.type-badge--multi {
  background: color-mix(in srgb, var(--c-green) 12%, transparent);
  color: color-mix(in srgb, var(--c-green) 80%, black);
  border-color: color-mix(in srgb, var(--c-green) 30%, transparent);
}

.modal-content--narrow {
  max-width: 460px;
}

.modal-title-icon--danger {
  color: var(--c-red);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px 20px;
  border-top: 1px solid var(--c-border);
}

.rollback-summary {
  margin: 12px 0 0;
  padding: 14px 16px;
  list-style: none;
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  border-radius: 8px;
  font-size: 12.5px;
  color: var(--c-text);
  line-height: 1.7;
}

.rollback-summary li + li {
  margin-top: 2px;
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
  transition: border-color 0.18s, box-shadow 0.18s, opacity 0.2s;
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
  flex: 1;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.card-title {
  font-family: 'DM Sans', sans-serif;
  font-size: 13px;
  font-weight: 600;
  color: var(--c-heading);
  line-height: 1.3;
  margin: 0;
}

.info-btn {
  background: transparent;
  border: none;
  color: var(--c-text-dim);
  cursor: pointer;
  padding: 4px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.info-btn:hover {
  color: var(--c-brand);
  background: var(--c-brand-dim);
}

.card-desc {
  font-size: 11.5px;
  color: var(--c-text-muted);
  line-height: 1.45;
  margin: 0;
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

.btn--danger {
  background: var(--c-red);
  color: #fff;
  border-color: var(--c-red);
}

.btn--danger:hover:not(:disabled) {
  filter: brightness(1.08);
}

.btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
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

/* --- Modal Styles --- */
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 20px;
}

.modal-content {
  background: var(--c-bg);
  border-radius: 12px;
  width: 100%;
  max-width: 900px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  max-height: 85vh;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--c-border);
}

.modal-title-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.modal-title-icon {
  color: var(--c-brand);
  font-size: 20px;
}

.modal-header h2 {
  margin: 0;
  font-family: 'DM Sans', sans-serif;
  font-size: 16px;
  font-weight: 600;
  color: var(--c-heading);
}

.modal-close-btn {
  background: transparent;
  border: none;
  font-size: 16px;
  color: var(--c-text-muted);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: background 0.2s, color 0.2s;
}

.modal-close-btn:hover {
  background: var(--c-border);
  color: var(--c-heading);
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
}

.modal-instruction {
  font-size: 13px;
  color: var(--c-text-muted);
  margin: 0 0 16px 0;
  line-height: 1.5;
}

.table-responsive {
  overflow-x: auto;
  border: 1px solid var(--c-border);
  border-radius: 8px;
  background: var(--c-card);
}

.format-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  white-space: nowrap;
}

.format-table th {
  background: var(--c-bg);
  font-family: 'DM Sans', sans-serif;
  font-weight: 600;
  font-size: 12px;
  color: var(--c-heading);
  padding: 12px 16px;
  border-bottom: 2px solid var(--c-border-strong);
}

.format-table td {
  padding: 12px 16px;
  font-size: 12.5px;
  color: var(--c-text);
  border-bottom: 1px solid var(--c-border);
}

.format-table tbody tr:last-child td {
  border-bottom: none;
}

/* Modal Vue Transitions */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>