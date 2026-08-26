import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'

type ThemePreference = 'light' | 'dark' | 'system'
type ResolvedTheme = 'light' | 'dark'

interface ThemeContextValue {
  preference: ThemePreference
  resolved: ResolvedTheme
  setPreference: (preference: ThemePreference) => void
  toggle: () => void
}

const STORAGE_KEY = 'smartgrid-theme-preference'

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined)

function resolveTheme(preference: ThemePreference): ResolvedTheme {
  if (preference === 'system') {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  }
  return preference
}

function readStoredPreference(): ThemePreference {
  const stored = localStorage.getItem(STORAGE_KEY)
  return stored === 'light' || stored === 'dark' || stored === 'system' ? stored : 'system'
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [preference, setPreferenceState] = useState<ThemePreference>(readStoredPreference)
  const [resolved, setResolved] = useState<ResolvedTheme>(() => resolveTheme(readStoredPreference()))

  useEffect(() => {
    const root = document.documentElement
    root.classList.toggle('dark', resolved === 'dark')
  }, [resolved])

  useEffect(() => {
    setResolved(resolveTheme(preference))
    if (preference !== 'system') return

    const media = window.matchMedia('(prefers-color-scheme: dark)')
    const listener = () => setResolved(resolveTheme('system'))
    media.addEventListener('change', listener)
    return () => media.removeEventListener('change', listener)
  }, [preference])

  const setPreference = useCallback((next: ThemePreference) => {
    localStorage.setItem(STORAGE_KEY, next)
    setPreferenceState(next)
  }, [])

  const toggle = useCallback(() => {
    setPreference(resolveTheme(preference) === 'dark' ? 'light' : 'dark')
  }, [preference, setPreference])

  const value = useMemo(
    () => ({ preference, resolved, setPreference, toggle }),
    [preference, resolved, setPreference, toggle],
  )

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

export function useTheme() {
  const context = useContext(ThemeContext)
  if (!context) throw new Error('useTheme must be used within a ThemeProvider')
  return context
}
