import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import * as authApi from '../api/auth'
import { refreshAccessToken } from '../api/client'
import { clearTokens, getAccessToken, getRefreshToken, isAuthenticated, setTokens, subscribe } from '../api/authStore'
import { decodeJwt } from './jwt'
import type { Role } from '../types'

export interface CurrentUser {
  username: string
  role: Role | null
}

interface AuthContextValue {
  authenticated: boolean
  user: CurrentUser | null
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

function userFromToken(): CurrentUser | null {
  const token = getAccessToken()
  if (!token) return null
  const claims = decodeJwt(token)
  if (!claims?.sub) return null
  return { username: claims.sub, role: (claims.role as Role) ?? null }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [authenticated, setAuthenticated] = useState<boolean>(() => {
    return isAuthenticated()
  })
  const [user, setUser] = useState<CurrentUser | null>(() => {
    return userFromToken()
  })
  const refreshTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  const clearRefreshTimer = useCallback(() => {
    if (refreshTimer.current) {
      clearTimeout(refreshTimer.current)
      refreshTimer.current = null
    }
  }, [])

  const scheduleRefresh = useCallback(() => {
    clearRefreshTimer()
    const token = getAccessToken()
    if (!token) return
    const claims = decodeJwt(token)
    if (!claims?.exp) return

    const msUntilExpiry = claims.exp * 1000 - Date.now()
    // Refresh 60 seconds before expiration, or in at least 5 seconds
    const msUntilRefresh = Math.max(msUntilExpiry - 60_000, 5_000)

    refreshTimer.current = setTimeout(async () => {
      const newToken = await refreshAccessToken()
      if (!newToken) {
        clearTokens()
      } else {
        // Upon refresh, update user claims and schedule next refresh
        setUser(userFromToken())
        scheduleRefresh()
      }
    }, msUntilRefresh)
  }, [clearRefreshTimer])

  // Bootstrap initial session and listen to store changes
  useEffect(() => {
    async function initSession() {
      const token = getAccessToken()
      const refreshToken = getRefreshToken()

      if (token) {
        const claims = decodeJwt(token)
        const isExp = !claims?.exp || claims.exp * 1000 <= Date.now() + 30_000
        if (isExp && refreshToken) {
          // Token expired or about to expire, silently refresh using the 7-day refresh token
          const refreshed = await refreshAccessToken()
          if (refreshed) {
            setAuthenticated(true)
            setUser(userFromToken())
            scheduleRefresh()
            return
          } else {
            clearTokens()
            setAuthenticated(false)
            setUser(null)
            return
          }
        } else if (!isExp) {
          setAuthenticated(true)
          setUser(userFromToken())
          scheduleRefresh()
          return
        }
      } else if (refreshToken) {
        // No access token in memory, but refresh token exists: silently restore session
        const refreshed = await refreshAccessToken()
        if (refreshed) {
          setAuthenticated(true)
          setUser(userFromToken())
          scheduleRefresh()
          return
        } else {
          clearTokens()
          setAuthenticated(false)
          setUser(null)
          return
        }
      }

      setAuthenticated(false)
      setUser(null)
    }

    initSession()

    const unsubscribe = subscribe((isAuth) => {
      setAuthenticated(isAuth)
      setUser(isAuth ? userFromToken() : null)
      if (isAuth) {
        scheduleRefresh()
      } else {
        clearRefreshTimer()
      }
    })

    // Handle background tab reactivation
    const handleVisibility = () => {
      if (document.visibilityState === 'visible') {
        const current = getAccessToken()
        if (current) {
          const c = decodeJwt(current)
          if (c?.exp && c.exp * 1000 <= Date.now() + 90_000) {
            refreshAccessToken().then((refreshed) => {
              if (refreshed) {
                setUser(userFromToken())
                scheduleRefresh()
              }
            })
          }
        }
      }
    }
    document.addEventListener('visibilitychange', handleVisibility)

    return () => {
      unsubscribe()
      clearRefreshTimer()
      document.removeEventListener('visibilitychange', handleVisibility)
    }
  }, [scheduleRefresh, clearRefreshTimer])

  const login = useCallback(async (username: string, password: string) => {
    const tokens = await authApi.login(username, password)
    setTokens(tokens)
    setUser(userFromToken())
    setAuthenticated(true)
  }, [])

  const logout = useCallback(async () => {
    const refreshToken = getRefreshToken()
    clearTokens()
    setUser(null)
    setAuthenticated(false)
    if (refreshToken) {
      await authApi.logout(refreshToken).catch(() => undefined)
    }
  }, [])

  const value = useMemo(() => ({ authenticated, user, login, logout }), [authenticated, user, login, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within an AuthProvider')
  return context
}
