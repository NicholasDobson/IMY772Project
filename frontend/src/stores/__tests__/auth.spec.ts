import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

// Build a minimal fake JWT whose payload is base64url-encoded JSON.
// parseJwt() only reads the second segment so the header and signature
// can be anything.
function makeToken(payload: Record<string, unknown>): string {
  const payloadB64 = btoa(JSON.stringify(payload))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '')
  return `eyJhbGciOiJSUzI1NiJ9.${payloadB64}.fakesig`
}

const FUTURE_EXP = Math.floor(Date.now() / 1000) + 3600  // 1 hour from now
const PAST_EXP   = Math.floor(Date.now() / 1000) - 1     // 1 second ago

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    sessionStorage.clear()
    // Suppress the jsdom "not implemented: navigation" warning produced by logout()
    vi.spyOn(console, 'error').mockImplementation(() => {})
    vi.spyOn(console, 'log').mockImplementation(() => {})
  })

  /* ── No token stored ────────────────────────────────────────────── */

  it('user is null when no token is stored', () => {
    expect(useAuthStore().user).toBeNull()
  })

  it('isAuthenticated is false when no token is stored', () => {
    expect(useAuthStore().isAuthenticated).toBe(false)
  })

  it('isAdmin is false when no token is stored', () => {
    expect(useAuthStore().isAdmin).toBe(false)
  })

  /* ── Token parsing ──────────────────────────────────────────────── */

  it('user contains the decoded JWT payload', () => {
    localStorage.setItem('id_token', makeToken({ sub: 'u-1', email: 'a@b.com', exp: FUTURE_EXP }))
    const store = useAuthStore()
    expect(store.user?.sub).toBe('u-1')
    expect(store.user?.email).toBe('a@b.com')
  })

  it('user is null when the stored token is not valid JWT', () => {
    localStorage.setItem('id_token', 'not.a.jwt.at.all')
    expect(useAuthStore().user).toBeNull()
  })

  /* ── isAuthenticated ────────────────────────────────────────────── */

  it('isAuthenticated is true when token has a future exp', () => {
    localStorage.setItem('id_token', makeToken({ sub: 'u-1', exp: FUTURE_EXP }))
    expect(useAuthStore().isAuthenticated).toBe(true)
  })

  it('isAuthenticated is false when token is expired', () => {
    localStorage.setItem('id_token', makeToken({ sub: 'u-1', exp: PAST_EXP }))
    expect(useAuthStore().isAuthenticated).toBe(false)
  })

  /* ── isAdmin ────────────────────────────────────────────────────── */

  it('isAdmin is false when cognito:groups is absent', () => {
    localStorage.setItem('id_token', makeToken({ sub: 'u-1', exp: FUTURE_EXP }))
    expect(useAuthStore().isAdmin).toBe(false)
  })

  it('isAdmin is false when user is not in the Admins group', () => {
    localStorage.setItem(
      'id_token',
      makeToken({ sub: 'u-1', exp: FUTURE_EXP, 'cognito:groups': ['Users'] }),
    )
    expect(useAuthStore().isAdmin).toBe(false)
  })

  it('isAdmin is true when user is in the Admins group', () => {
    localStorage.setItem(
      'id_token',
      makeToken({ sub: 'u-1', exp: FUTURE_EXP, 'cognito:groups': ['Users', 'Admins'] }),
    )
    expect(useAuthStore().isAdmin).toBe(true)
  })

  it('isAdmin is false when token is expired even if user is in Admins group', () => {
    localStorage.setItem(
      'id_token',
      makeToken({ sub: 'u-1', exp: PAST_EXP, 'cognito:groups': ['Admins'] }),
    )
    expect(useAuthStore().isAdmin).toBe(false)
  })

  /* ── logout ─────────────────────────────────────────────────────── */

  it('logout clears id_token from localStorage', () => {
    localStorage.setItem('id_token', makeToken({ sub: 'u-1', exp: FUTURE_EXP }))
    useAuthStore().logout()
    expect(localStorage.getItem('id_token')).toBeNull()
  })

  it('logout clears access_token from localStorage', () => {
    localStorage.setItem('id_token', makeToken({ sub: 'u-1', exp: FUTURE_EXP }))
    localStorage.setItem('access_token', 'some-access-token')
    useAuthStore().logout()
    expect(localStorage.getItem('access_token')).toBeNull()
  })

  /* ── Cognito SDK storage format ─────────────────────────────────── */

  it('reads id token stored using Cognito SDK key format', () => {
    // The auth store scans for keys matching
    // CognitoIdentityServiceProvider.{CLIENT_ID}.{userId}.idToken
    // CLIENT_ID resolves to import.meta.env.VITE_COGNITO_CLIENT_ID which
    // is undefined in test, so template literal produces "undefined".
    const clientId = String(import.meta.env.VITE_COGNITO_CLIENT_ID)
    const sdkKey = `CognitoIdentityServiceProvider.${clientId}.user-42.idToken`
    const token = makeToken({ sub: 'user-42', exp: FUTURE_EXP })
    localStorage.setItem(sdkKey, token)

    const store = useAuthStore()
    expect(store.user?.sub).toBe('user-42')
    expect(store.isAuthenticated).toBe(true)
  })

  it('own id_token key takes precedence over Cognito SDK format', () => {
    const clientId = String(import.meta.env.VITE_COGNITO_CLIENT_ID)
    const sdkToken  = makeToken({ sub: 'sdk-user', exp: FUTURE_EXP })
    const ownToken  = makeToken({ sub: 'own-user', exp: FUTURE_EXP })
    localStorage.setItem(`CognitoIdentityServiceProvider.${clientId}.u.idToken`, sdkToken)
    localStorage.setItem('id_token', ownToken)

    expect(useAuthStore().user?.sub).toBe('own-user')
  })
})
