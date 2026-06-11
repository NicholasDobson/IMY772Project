/* ── Advisor API — proxied to Spring Boot :8080 ────────────────── */

export type AdvisorContextType = 'site' | 'organism' | 'general'

export interface AdvisorRequest {
  message: string
  contextType: AdvisorContextType
  contextId?: string
}

export interface SourceReference {
  documentId: number
  documentTitle: string
  chunkPreview: string
  pageNumbers: string | null
  similarity: number
}

export interface AdvisorResponse {
  reply: string | null
  ok: boolean
  error: string | null
  sources?: SourceReference[] | null
}

export async function askAdvisor(req: AdvisorRequest): Promise<AdvisorResponse> {
  const res = await fetch('/api/v1/advisor/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
  // Parse JSON regardless of status — backend returns AdvisorResponse for 4xx/5xx too
  try {
    return (await res.json()) as AdvisorResponse
  } catch {
    return { reply: null, ok: false, error: `Request failed (${res.status})` }
  }
}
