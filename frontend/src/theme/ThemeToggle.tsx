import { useTheme } from './ThemeContext'

const OPTIONS = [
  { value: 'light', label: 'Light', icon: SunIcon },
  { value: 'dark', label: 'Dark', icon: MoonIcon },
  { value: 'system', label: 'System', icon: SystemIcon },
] as const

export function ThemeToggle() {
  const { preference, setPreference } = useTheme()

  return (
    <div className="inline-flex items-center gap-0.5 rounded-full border border-border bg-surface p-0.5">
      {OPTIONS.map(({ value, label, icon: Icon }) => {
        const active = preference === value
        return (
          <button
            key={value}
            type="button"
            aria-label={`${label} theme`}
            aria-pressed={active}
            onClick={() => setPreference(value)}
            className={`flex h-7 w-7 items-center justify-center rounded-full transition-colors ${
              active
                ? 'bg-accent text-accent-fg'
                : 'text-text-muted hover:bg-border/60 hover:text-text'
            }`}
          >
            <Icon className="h-4 w-4" />
          </button>
        )
      })}
    </div>
  )
}

function SunIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} className={className}>
      <circle cx="12" cy="12" r="4" />
      <path
        strokeLinecap="round"
        d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41"
      />
    </svg>
  )
}

function MoonIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} className={className}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79Z" />
    </svg>
  )
}

function SystemIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} className={className}>
      <rect x="3" y="4" width="18" height="13" rx="1.5" />
      <path strokeLinecap="round" d="M8 21h8M12 17v4" />
    </svg>
  )
}
