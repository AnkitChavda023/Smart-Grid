import type { TokenPair } from '../types'

const STORAGE_KEY = 'smartgrid_auth_tokens'

interface PersistedTokens {
  accessToken: string
  refreshToken: string
  expiresAt: number
}

function loadInitialTokens(): PersistedTokens | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as PersistedTokens
    if (parsed.refreshToken) {
      return parsed
    }
  } catch {
    // Ignore invalid stored json
  }
  return null
}

const initial = loadInitialTokens()

let accessToken: string | null = initial?.accessToken ?? null
let refreshToken: string | null = initial?.refreshToken ?? null
let expiresAt: number | null = initial?.expiresAt ?? null

type Listener = (authenticated: boolean) => void
const listeners = new Set<Listener>()

export function setTokens(tokens: TokenPair) {
  accessToken = tokens.accessToken
  refreshToken = tokens.refreshToken
  expiresAt = Date.now() + tokens.expiresInSeconds * 1000

  try {
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({
        accessToken,
        refreshToken,
        expiresAt,
      }),
    )
  } catch {
    // Storage quota or disabled
  }

  listeners.forEach((l) => l(true))
}

export function clearTokens() {
  accessToken = null
  refreshToken = null
  expiresAt = null

  try {
    localStorage.removeItem(STORAGE_KEY)
  } catch {
    // Ignore
  }

  listeners.forEach((l) => l(false))
}

export function getAccessToken() {
  return accessToken
}

export function getRefreshToken() {
  return refreshToken
}

export function getExpiresAt() {
  return expiresAt
}

export function isAuthenticated() {
  return accessToken !== null || refreshToken !== null
}

export function subscribe(listener: Listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}
