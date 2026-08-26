interface JwtClaims {
  sub?: string
  role?: string
  exp?: number
  [key: string]: unknown
}

export function decodeJwt(token: string): JwtClaims | null {
  try {
    const payload = token.split('.')[1]
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), '=')
    return JSON.parse(atob(padded)) as JwtClaims
  } catch {
    return null
  }
}
