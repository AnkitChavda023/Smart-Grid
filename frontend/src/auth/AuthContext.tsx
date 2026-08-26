import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import * as authApi from '../api/auth'
import { refreshAccessToken } from '../api/client'
import { clearTokens, getAccessToken, getRefreshToken, isAuthenticated, setTokens, subscribe } from '../api/authStore'
import { decodeJwt } from './jwt'
import type { Role } from '../types'

interface CurrentUser {
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
  const [authenticated, setAuthenticated] = useState(isAuthenticated)
  const [user, setUser] = useState<CurrentUser | null>(userFromToken)
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
    const msUntilRefresh = Math.max(msUntilExpiry - 60_000, 5_000)
    refreshTimer.current = setTimeout(async () => {
      const newToken = await refreshAccessToken()
      if (!newToken) clearTokens()
    }, msUntilRefresh)
  }, [clearRefreshTimer])

  useEffect(() => {
    const unsubscribe = subscribe((isAuth) => {
      setAuthenticated(isAuth)
      setUser(isAuth ? userFromToken() : null)
      if (isAuth) {
        scheduleRefresh()
      } else {
        clearRefreshTimer()
      }
    })
    return () => {
      unsubscribe()
      clearRefreshTimer()
    }
  }, [scheduleRefresh, clearRefreshTimer])

  const login = useCallback(async (username: string, password: string) => {
    const tokens = await authApi.login(username, password)
    setTokens(tokens)
  }, [])

  const logout = useCallback(async () => {
    const refreshToken = getRefreshToken()
    clearTokens()
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
