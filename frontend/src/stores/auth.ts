import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const COGNITO_DOMAIN = import.meta.env.VITE_COGNITO_DOMAIN as string
const CLIENT_ID = import.meta.env.VITE_COGNITO_CLIENT_ID as string
const CLIENT_SECRET = import.meta.env.VITE_COGNITO_CLIENT_SECRET as string | undefined
const REDIRECT_URI = (import.meta.env.VITE_COGNITO_REDIRECT_URI as string) || 'http://localhost:5173/'

function parseJwt(token: string): Record<string, unknown> {
  const base64Url = token.split('.')[1] ?? ''
  const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
  const json = decodeURIComponent(
    atob(base64)
      .split('')
      .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
      .join(''),
  )
  return JSON.parse(json)
}

async function generateCodeVerifier(): Promise<string> {
  const array = new Uint8Array(32)
  crypto.getRandomValues(array)
  return btoa(String.fromCharCode(...array))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '')
}

async function generateCodeChallenge(verifier: string): Promise<string> {
  const data = new TextEncoder().encode(verifier)
  const digest = await crypto.subtle.digest('SHA-256', data)
  return btoa(String.fromCharCode(...new Uint8Array(digest)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '')
}

// Reads an id/access token regardless of which storage key format was used.
// Our PKCE flow writes to 'id_token'/'access_token'; the Cognito SDK writes to
// 'CognitoIdentityServiceProvider.{clientId}.{userId}.idToken'.
function readStoredTokens(): { idToken: string | null; accessToken: string | null } {
  const own = localStorage.getItem('id_token')
  if (own) return { idToken: own, accessToken: localStorage.getItem('access_token') }

  // Scan for Cognito SDK keys scoped to THIS app's client ID only
  const sdkPrefix = `CognitoIdentityServiceProvider.${CLIENT_ID}.`
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i)
    if (key?.startsWith(sdkPrefix) && key.endsWith('.idToken')) {
      return {
        idToken: localStorage.getItem(key),
        accessToken: localStorage.getItem(key.replace('.idToken', '.accessToken')),
      }
    }
  }
  return { idToken: null, accessToken: null }
}

export const useAuthStore = defineStore('auth', () => {
  const stored = readStoredTokens()
  const idToken = ref<string | null>(stored.idToken)
  const accessToken = ref<string | null>(stored.accessToken)

  const user = computed<Record<string, unknown> | null>(() => {
    if (!idToken.value) return null
    try {
      return parseJwt(idToken.value)
    } catch {
      return null
    }
  })

  const isAuthenticated = computed(
    () => !!user.value && Date.now() / 1000 < (user.value.exp as number),
  )

  const isAdmin = computed(() => {
    if (!isAuthenticated.value) return false
    const groups = (user.value?.['cognito:groups'] as string[]) ?? []
    return groups.includes('Admins')
  })

  async function login() {
    const verifier = await generateCodeVerifier()
    const challenge = await generateCodeChallenge(verifier)
    localStorage.setItem('pkce_verifier', verifier)

    const params = new URLSearchParams({
      response_type: 'code',
      client_id: CLIENT_ID,
      redirect_uri: REDIRECT_URI,
      scope: 'openid email profile',
      code_challenge_method: 'S256',
      code_challenge: challenge,
    })

    window.location.href = `${COGNITO_DOMAIN}/oauth2/authorize?${params}`
  }

  async function handleCallback(code: string) {
    const verifier = localStorage.getItem('pkce_verifier') ?? ''
    console.log('[auth] handleCallback — verifier present:', !!verifier, '| client_id:', CLIENT_ID)

    const body = new URLSearchParams({
      grant_type: 'authorization_code',
      client_id: CLIENT_ID,
      code,
      redirect_uri: REDIRECT_URI,
    })
    if (verifier) body.set('code_verifier', verifier)
    if (CLIENT_SECRET) body.set('client_secret', CLIENT_SECRET)

    const res = await fetch(`${COGNITO_DOMAIN}/oauth2/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body,
    })

    const rawText = await res.text()
    console.log('[auth] token exchange status:', res.status, '| response:', rawText)

    if (!res.ok) {
      console.error('[auth] Token exchange failed — see response above')
      return
    }

    const tokens = JSON.parse(rawText)

    idToken.value = tokens.id_token
    accessToken.value = tokens.access_token
    localStorage.setItem('id_token', tokens.id_token)
    localStorage.setItem('access_token', tokens.access_token)
    localStorage.removeItem('pkce_verifier')

    window.history.replaceState({}, '', REDIRECT_URI)
  }

  async function initialize() {
    const params = new URLSearchParams(window.location.search)
    const code = params.get('code')
    console.log('[auth] initialize — code in URL:', !!code, '| search:', window.location.search)
    if (code) {
      await handleCallback(code)
    }
  }

  function logout() {
    idToken.value = null
    accessToken.value = null
    localStorage.removeItem('id_token')
    localStorage.removeItem('access_token')

    const params = new URLSearchParams({ client_id: CLIENT_ID, logout_uri: REDIRECT_URI })
    window.location.href = `${COGNITO_DOMAIN}/logout?${params}`
  }

  return { user, isAuthenticated, isAdmin, accessToken, login, logout, initialize }
})
