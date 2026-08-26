import type { TokenPair } from '../types'


let accessToken: string | null = null
let refreshToken: string | null = null
let expiresAt: number | null = null

type Listener = (authenticated: boolean) => void
const listeners = new Set<Listener>()

export function setTokens(tokens: TokenPair) {
  accessToken = tokens.accessToken
  refreshToken = tokens.refreshToken
  expiresAt = Date.now() + tokens.expiresInSeconds * 1000
  listeners.forEach((l) => l(true))
}

export function clearTokens() {
  accessToken = null
  refreshToken = null
  expiresAt = null
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
  return accessToken !== null
}

export function subscribe(listener: Listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}
