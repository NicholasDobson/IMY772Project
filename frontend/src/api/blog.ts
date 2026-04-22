/* ── Blog API — typed fetch helpers ──────────────────────────
   All blog endpoints are public (no auth required).
   Requests hit /api/v1/... which Vite proxies to Spring Boot :8080.
────────────────────────────────────────────────────────────────── */

const BASE = '/api/v1/blogs'

async function get<T>(path: string, params?: Record<string, string | number>): Promise<T> {
  const url = new URL(path, window.location.origin)
  if (params) {
    Object.entries(params).forEach(([k, v]) => url.searchParams.set(k, String(v)))
  }
  const res = await fetch(url.toString())
  if (!res.ok) throw new Error(`API ${res.status}: ${url.pathname}`)
  return res.json() as Promise<T>
}

async function post<T>(path: string, body: any): Promise<T> {
  const res = await fetch(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error(`API ${res.status}: ${path}`)
  return res.json() as Promise<T>
}

async function put<T>(path: string, body: any): Promise<T> {
  const res = await fetch(path, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error(`API ${res.status}: ${path}`)
  return res.json() as Promise<T>
}

async function del(path: string): Promise<void> {
  const res = await fetch(path, { method: 'DELETE' })
  if (!res.ok) throw new Error(`API ${res.status}: ${path}`)
}

/* ── Response shapes ─────── */

export interface Blog {
  blogId: number
  title: string
  image?: string
  author: string
  content: string
  datePublished: string
}

/* ── API functions ─────── */

export async function getAllBlogs(): Promise<Blog[]> {
  return get<Blog[]>(BASE)
}

export async function getBlogById(blogId: number): Promise<Blog> {
  return get<Blog>(`${BASE}/${blogId}`)
}

export async function createBlog(blog: Omit<Blog, 'blogId' | 'datePublished'>): Promise<Blog> {
  return post<Blog>(BASE, blog)
}

export async function updateBlog(blogId: number, blog: Partial<Blog>): Promise<Blog> {
  return put<Blog>(`${BASE}/${blogId}`, blog)
}

export async function deleteBlog(blogId: number): Promise<void> {
  return del(`${BASE}/${blogId}`)
}

export async function searchBlogs(query: string): Promise<Blog[]> {
  return get<Blog[]>(`${BASE}/search`, { query })
}
