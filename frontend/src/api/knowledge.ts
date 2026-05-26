const API_BASE = '/api/v1/knowledge/documents'

export interface KnowledgeDocumentDTO {
  documentId: number
  title: string
  originalFilename: string
  status: 'UPLOADING' | 'PROCESSING' | 'READY' | 'FAILED'
  errorMessage: string | null
  chunkCount: number | null
  fileSizeBytes: number | null
  uploadedBy: string | null
  uploadedAt: string
  processedAt: string | null
}

export interface DocumentStatusResponse {
  status: string
  chunkCount: number
  errorMessage: string
}

export async function getDocuments(): Promise<KnowledgeDocumentDTO[]> {
  const res = await fetch(API_BASE)
  if (!res.ok) throw new Error('Failed to fetch documents')
  return res.json()
}

export async function uploadDocument(file: File, title?: string): Promise<KnowledgeDocumentDTO> {
  const form = new FormData()
  form.append('file', file)
  if (title) form.append('title', title)

  const res = await fetch(API_BASE, { method: 'POST', body: form })
  if (!res.ok) {
    const err = await res.json().catch(() => ({ error: 'Upload failed' }))
    throw new Error(err.error || 'Upload failed')
  }
  return res.json()
}

export async function deleteDocument(id: number): Promise<void> {
  const res = await fetch(`${API_BASE}/${id}`, { method: 'DELETE' })
  if (!res.ok) throw new Error('Failed to delete document')
}

export async function getDocumentStatus(id: number): Promise<DocumentStatusResponse> {
  const res = await fetch(`${API_BASE}/${id}/status`)
  if (!res.ok) throw new Error('Failed to fetch status')
  return res.json()
}

export async function reprocessDocument(id: number): Promise<KnowledgeDocumentDTO> {
  const res = await fetch(`${API_BASE}/${id}/reprocess`, { method: 'POST' })
  if (!res.ok) throw new Error('Failed to reprocess document')
  return res.json()
}
