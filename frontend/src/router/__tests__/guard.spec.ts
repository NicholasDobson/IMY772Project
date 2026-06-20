import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
import { setActivePinia, createPinia } from 'pinia'

// Control isAdmin from inside tests without recreating the mock each time
let _isAdmin = false

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ get isAdmin() { return _isAdmin } }),
}))

import { useAuthStore } from '@/stores/auth'

// Minimal dummy component — just needs to mount without error
const Dummy = { template: '<div />' }

// Mirror the production route list (only routes relevant to the guard test)
function makeRouter() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/',             name: 'dashboard',          component: Dummy },
      { path: '/settings',     name: 'settings',           component: Dummy },
      { path: '/upload',       name: 'upload',             component: Dummy, meta: { requiresAdmin: true } },
      { path: '/admin/users',  name: 'user-management',    component: Dummy, meta: { requiresAdmin: true } },
      { path: '/admin/knowledge', name: 'knowledge-management', component: Dummy, meta: { requiresAdmin: true } },
    ],
  })

  // Same guard as router/index.ts
  router.beforeEach((to) => {
    if (to.meta.requiresAdmin) {
      const auth = useAuthStore()
      if (!auth.isAdmin) return { name: 'dashboard' }
    }
  })

  return router
}

describe('Router admin guard', () => {
  let router: ReturnType<typeof makeRouter>

  beforeEach(async () => {
    setActivePinia(createPinia())
    _isAdmin = false
    router = makeRouter()
    await router.push('/')
  })

  /* ── Non-admin user ─────────────────────────────────────────────── */

  it('redirects non-admin away from /upload to dashboard', async () => {
    await router.push('/upload')
    expect(router.currentRoute.value.name).toBe('dashboard')
  })

  it('redirects non-admin away from /admin/users to dashboard', async () => {
    await router.push('/admin/users')
    expect(router.currentRoute.value.name).toBe('dashboard')
  })

  it('redirects non-admin away from /admin/knowledge to dashboard', async () => {
    await router.push('/admin/knowledge')
    expect(router.currentRoute.value.name).toBe('dashboard')
  })

  it('allows non-admin to access /settings', async () => {
    await router.push('/settings')
    expect(router.currentRoute.value.name).toBe('settings')
  })

  it('allows non-admin to access the dashboard', async () => {
    await router.push('/')
    expect(router.currentRoute.value.name).toBe('dashboard')
  })

  /* ── Admin user ─────────────────────────────────────────────────── */

  it('allows admin to access /upload', async () => {
    _isAdmin = true
    await router.push('/upload')
    expect(router.currentRoute.value.name).toBe('upload')
  })

  it('allows admin to access /admin/users', async () => {
    _isAdmin = true
    await router.push('/admin/users')
    expect(router.currentRoute.value.name).toBe('user-management')
  })

  it('allows admin to access /admin/knowledge', async () => {
    _isAdmin = true
    await router.push('/admin/knowledge')
    expect(router.currentRoute.value.name).toBe('knowledge-management')
  })

  /* ── Mid-session privilege change ───────────────────────────────── */

  it('blocks access after admin privilege is revoked', async () => {
    _isAdmin = true
    await router.push('/upload')
    expect(router.currentRoute.value.name).toBe('upload')

    _isAdmin = false
    await router.push('/admin/users')
    expect(router.currentRoute.value.name).toBe('dashboard')
  })
})
