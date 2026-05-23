<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import {
  getDocuments,
  uploadDocument,
  deleteDocument,
  getDocumentStatus,
  reprocessDocument,
  type KnowledgeDocumentDTO,
} from '@/api/knowledge'

const documents = ref<KnowledgeDocumentDTO[]>([])
const loading = ref(false)
const uploading = ref(false)
const error = ref('')
const success = ref('')

const selectedFile = ref<File | null>(null)
const titleInput = ref('')
const dragOver = ref(false)
const visible = ref(false)

const pollingTimers = new Map<number, ReturnType<typeof setInterval>>()

onMounted(async () => {
  setTimeout(() => (visible.value = true), 60)
  await fetchDocuments()
})

onUnmounted(() => {
  pollingTimers.forEach((timer) => clearInterval(timer))
  pollingTimers.clear()
})

async function fetchDocuments() {
  loading.value = true
  try {
    documents.value = await getDocuments()
    documents.value.forEach((doc) => {
      if (doc.status === 'PROCESSING') startPolling(doc.documentId)
    })
  } catch (e: any) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function startPolling(docId: number) {
  if (pollingTimers.has(docId)) return
  const timer = setInterval(async () => {
    try {
      const status = await getDocumentStatus(docId)
      const doc = documents.value.find((d) => d.documentId === docId)
      if (doc) {
        doc.status = status.status as KnowledgeDocumentDTO['status']
        doc.chunkCount = status.chunkCount
        if (status.errorMessage) doc.errorMessage = status.errorMessage
      }
      if (status.status === 'READY' || status.status === 'FAILED') {
        clearInterval(timer)
        pollingTimers.delete(docId)
      }
    } catch {
      clearInterval(timer)
      pollingTimers.delete(docId)
    }
  }, 3000)
  pollingTimers.set(docId, timer)
}

function onDrop(e: DragEvent) {
  dragOver.value = false
  const file = e.dataTransfer?.files[0]
  if (file && file.type === 'application/pdf') {
    selectedFile.value = file
    if (!titleInput.value) titleInput.value = file.name.replace(/\.pdf$/i, '')
  } else {
    error.value = 'Only PDF files are accepted'
  }
}

function onFileSelect(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) {
    selectedFile.value = file
    if (!titleInput.value) titleInput.value = file.name.replace(/\.pdf$/i, '')
  }
  input.value = ''
}

async function handleUpload() {
  if (!selectedFile.value) return
  uploading.value = true
  error.value = ''
  success.value = ''
  try {
    const doc = await uploadDocument(selectedFile.value, titleInput.value || undefined)
    documents.value.unshift(doc)
    startPolling(doc.documentId)
    selectedFile.value = null
    titleInput.value = ''
    success.value = 'Document uploaded and processing started'
    setTimeout(() => (success.value = ''), 4000)
  } catch (e: any) {
    error.value = e.message
  } finally {
    uploading.value = false
  }
}

async function handleDelete(docId: number) {
  if (!confirm('Delete this document and all its chunks?')) return
  try {
    await deleteDocument(docId)
    documents.value = documents.value.filter((d) => d.documentId !== docId)
    if (pollingTimers.has(docId)) {
      clearInterval(pollingTimers.get(docId))
      pollingTimers.delete(docId)
    }
  } catch (e: any) {
    error.value = e.message
  }
}

async function handleReprocess(docId: number) {
  try {
    const doc = await reprocessDocument(docId)
    const idx = documents.value.findIndex((d) => d.documentId === docId)
    if (idx >= 0) documents.value[idx] = doc
    startPolling(docId)
  } catch (e: any) {
    error.value = e.message
  }
}

function formatSize(bytes: number | null): string {
  if (!bytes) return '—'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1048576).toFixed(1) + ' MB'
}

function formatDate(dateStr: string | null): string {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleDateString('en-ZA', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}
</script>

<template>
  <div class="knowledge-page" :class="{ visible }">
    <header class="page-header">
      <div>
        <h1 class="page-title">Knowledge Base</h1>
        <p class="page-lead">
          Upload research papers and documents to enhance AI advisor responses
        </p>
      </div>
    </header>

    <!-- Alerts -->
    <div v-if="error" class="alert alert-error">
      <i class="pi pi-exclamation-circle"></i>
      <span>{{ error }}</span>
      <button class="alert-close" @click="error = ''"><i class="pi pi-times"></i></button>
    </div>
    <div v-if="success" class="alert alert-success">
      <i class="pi pi-check-circle"></i>
      <span>{{ success }}</span>
    </div>

    <!-- Upload Section -->
    <section class="upload-section">
      <h2 class="section-title">Upload Document</h2>
      <div
        class="drop-zone"
        :class="{ 'drop-zone--active': dragOver, 'drop-zone--has-file': selectedFile }"
        @dragover.prevent="dragOver = true"
        @dragleave="dragOver = false"
        @drop.prevent="onDrop"
      >
        <template v-if="!selectedFile">
          <i class="pi pi-cloud-upload drop-icon"></i>
          <p class="drop-text">Drag & drop a PDF here</p>
          <p class="drop-sub">or</p>
          <label class="browse-btn">
            Browse files
            <input type="file" accept=".pdf,application/pdf" hidden @change="onFileSelect" />
          </label>
        </template>
        <template v-else>
          <i class="pi pi-file-pdf drop-icon drop-icon--file"></i>
          <p class="drop-text">{{ selectedFile.name }}</p>
          <p class="drop-sub">{{ formatSize(selectedFile.size) }}</p>
          <button class="clear-btn" @click="selectedFile = null; titleInput = ''">
            <i class="pi pi-times"></i> Remove
          </button>
        </template>
      </div>

      <div v-if="selectedFile" class="upload-controls">
        <input
          v-model="titleInput"
          type="text"
          class="title-input"
          placeholder="Document title (optional)"
        />
        <button class="upload-btn" :disabled="uploading" @click="handleUpload">
          <i :class="uploading ? 'pi pi-spin pi-spinner' : 'pi pi-upload'"></i>
          {{ uploading ? 'Uploading...' : 'Upload & Process' }}
        </button>
      </div>
    </section>

    <!-- Documents Table -->
    <section class="docs-section">
      <h2 class="section-title">
        Uploaded Documents
        <span class="doc-count">{{ documents.length }}</span>
      </h2>

      <div v-if="loading" class="loading-state">
        <i class="pi pi-spin pi-spinner"></i> Loading documents...
      </div>

      <div v-else-if="documents.length === 0" class="empty-state">
        <i class="pi pi-inbox empty-icon"></i>
        <p>No documents uploaded yet</p>
        <p class="empty-sub">Upload research papers to enhance the AI advisor</p>
      </div>

      <div v-else class="docs-table-wrap">
        <table class="docs-table">
          <thead>
            <tr>
              <th>Title</th>
              <th>File</th>
              <th>Size</th>
              <th>Status</th>
              <th>Chunks</th>
              <th>Uploaded</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="doc in documents" :key="doc.documentId">
              <td class="doc-title">{{ doc.title }}</td>
              <td class="doc-filename">{{ doc.originalFilename }}</td>
              <td>{{ formatSize(doc.fileSizeBytes) }}</td>
              <td>
                <span class="status-badge" :class="'status-' + doc.status.toLowerCase()">
                  <i v-if="doc.status === 'PROCESSING'" class="pi pi-spin pi-spinner"></i>
                  {{ doc.status }}
                </span>
                <span v-if="doc.status === 'FAILED' && doc.errorMessage" class="error-hint" :title="doc.errorMessage">
                  <i class="pi pi-info-circle"></i>
                </span>
              </td>
              <td>{{ doc.chunkCount ?? '—' }}</td>
              <td>{{ formatDate(doc.uploadedAt) }}</td>
              <td class="actions-cell">
                <button
                  v-if="doc.status === 'FAILED'"
                  class="action-btn action-retry"
                  title="Reprocess"
                  @click="handleReprocess(doc.documentId)"
                >
                  <i class="pi pi-refresh"></i>
                </button>
                <button
                  class="action-btn action-delete"
                  title="Delete"
                  @click="handleDelete(doc.documentId)"
                >
                  <i class="pi pi-trash"></i>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.knowledge-page {
  padding: 28px 32px;
  max-width: 960px;
  opacity: 0;
  transform: translateY(8px);
  transition: opacity 0.35s ease, transform 0.35s ease;
}
.knowledge-page.visible {
  opacity: 1;
  transform: translateY(0);
}

.page-header {
  margin-bottom: 24px;
}
.page-title {
  font-family: 'Inter', sans-serif;
  font-size: 26px;
  font-weight: 700;
  color: var(--c-heading);
  margin: 0 0 4px;
}
.page-lead {
  font-size: 13px;
  color: var(--c-text-muted);
}

/* Alerts */
.alert {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 6px;
  font-size: 12.5px;
  margin-bottom: 16px;
}
.alert-error {
  background: var(--c-red-dim);
  color: var(--c-red-text);
  border: 1px solid var(--c-red);
}
.alert-success {
  background: var(--c-green-dim);
  color: var(--c-green-text);
  border: 1px solid var(--c-green);
}
.alert-close {
  margin-left: auto;
  background: none;
  border: none;
  color: inherit;
  font-size: 12px;
  cursor: pointer;
}

/* Section */
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-heading);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.doc-count {
  font-size: 11px;
  font-weight: 600;
  background: var(--c-brand-dim);
  color: var(--c-brand);
  padding: 1px 8px;
  border-radius: 10px;
}

/* Upload */
.upload-section {
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 24px;
  box-shadow: var(--c-shadow);
}

.drop-zone {
  border: 2px dashed var(--c-border-strong);
  border-radius: 8px;
  padding: 36px 20px;
  text-align: center;
  transition: border-color 0.15s, background 0.15s;
  cursor: pointer;
}
.drop-zone--active {
  border-color: var(--c-brand);
  background: var(--c-brand-dim);
}
.drop-zone--has-file {
  border-style: solid;
  border-color: var(--c-brand);
  background: var(--c-brand-dim);
}
.drop-icon {
  font-size: 32px;
  color: var(--c-text-dim);
  margin-bottom: 8px;
}
.drop-icon--file {
  color: var(--c-red);
}
.drop-text {
  font-size: 13px;
  font-weight: 500;
  color: var(--c-text);
  margin-bottom: 4px;
}
.drop-sub {
  font-size: 11.5px;
  color: var(--c-text-muted);
  margin-bottom: 8px;
}
.browse-btn {
  display: inline-block;
  padding: 6px 16px;
  border-radius: 6px;
  border: 1px solid var(--c-brand);
  background: var(--c-brand-dim);
  color: var(--c-brand);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.12s;
}
.browse-btn:hover {
  background: var(--c-brand);
  color: #fff;
}
.clear-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border: 1px solid var(--c-border);
  border-radius: 5px;
  background: var(--c-card);
  color: var(--c-text-muted);
  font-size: 11.5px;
  cursor: pointer;
}

.upload-controls {
  display: flex;
  gap: 10px;
  margin-top: 14px;
}
.title-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid var(--c-border);
  border-radius: 6px;
  background: var(--c-surface);
  color: var(--c-text);
  font-size: 13px;
  font-family: 'DM Sans', sans-serif;
  outline: none;
  transition: border-color 0.15s;
}
.title-input:focus {
  border-color: var(--c-brand);
}
.upload-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border: none;
  border-radius: 6px;
  background: var(--c-brand);
  color: #fff;
  font-size: 12.5px;
  font-weight: 600;
  font-family: 'DM Sans', sans-serif;
  cursor: pointer;
  transition: opacity 0.15s;
  white-space: nowrap;
}
.upload-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Documents */
.docs-section {
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: 8px;
  padding: 20px;
  box-shadow: var(--c-shadow);
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 32px 16px;
  color: var(--c-text-muted);
  font-size: 13px;
}
.empty-icon {
  font-size: 36px;
  color: var(--c-text-dim);
  margin-bottom: 8px;
}
.empty-sub {
  font-size: 12px;
  color: var(--c-text-dim);
}

.docs-table-wrap {
  overflow-x: auto;
}
.docs-table {
  width: 100%;
  border-collapse: collapse;
}
.docs-table th {
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.09em;
  color: var(--c-text-muted);
  padding: 8px 12px;
  border-bottom: 1px solid var(--c-border);
  text-align: left;
}
.docs-table td {
  font-size: 12.5px;
  padding: 10px 12px;
  border-bottom: 1px solid var(--c-border);
  color: var(--c-text);
}
.docs-table tbody tr:hover {
  background: var(--c-brand-dim);
}
.doc-title {
  font-weight: 500;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.doc-filename {
  font-size: 11.5px;
  color: var(--c-text-muted);
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Status badges */
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.06em;
  padding: 2px 8px;
  border-radius: 4px;
  text-transform: uppercase;
}
.status-ready {
  background: var(--c-green-dim);
  color: var(--c-green-text);
}
.status-processing {
  background: var(--c-amber-dim);
  color: var(--c-amber-text);
}
.status-failed {
  background: var(--c-red-dim);
  color: var(--c-red-text);
}
.status-uploading {
  background: var(--c-brand-dim);
  color: var(--c-brand);
}
.error-hint {
  color: var(--c-red);
  font-size: 12px;
  margin-left: 4px;
  cursor: help;
}

/* Actions */
.actions-cell {
  display: flex;
  gap: 4px;
}
.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px solid var(--c-border);
  border-radius: 5px;
  background: var(--c-card);
  color: var(--c-text-muted);
  font-size: 12px;
  cursor: pointer;
  transition: background 0.12s, color 0.12s;
}
.action-btn:hover {
  background: var(--c-brand-dim);
  color: var(--c-brand);
}
.action-delete:hover {
  background: var(--c-red-dim);
  color: var(--c-red);
}
</style>
