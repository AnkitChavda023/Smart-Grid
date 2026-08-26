import type { ReactNode, SVGProps } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { ThemeToggle } from '../../theme/ThemeToggle'
import { useNotifications } from '../../ws/NotificationsContext'

function Icon({ children, ...props }: { children: ReactNode } & SVGProps<SVGSVGElement>) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      className="h-4 w-4 shrink-0"
      {...props}
    >
      {children}
    </svg>
  )
}

interface NavItem {
  to: string
  label: string
  end?: boolean
  icon: ReactNode
}

const NAV_ITEMS: NavItem[] = [
  {
    to: '/',
    label: 'Overview',
    end: true,
    icon: (
      <Icon>
        <path d="M3 11.5 12 4l9 7.5" />
        <path d="M5 10v9a1 1 0 0 0 1 1h4v-6h4v6h4a1 1 0 0 0 1-1v-9" />
      </Icon>
    ),
  },
  {
    to: '/orders',
    label: 'Orders',
    icon: (
      <Icon>
        <path d="M3 7h18l-1.5 12.5a1 1 0 0 1-1 .5H5.5a1 1 0 0 1-1-.5L3 7Z" />
        <path d="M8 7V5a4 4 0 0 1 8 0v2" />
      </Icon>
    ),
  },
  {
    to: '/vendors',
    label: 'Vendors',
    icon: (
      <Icon>
        <path d="M4 21V9l8-5 8 5v12" />
        <path d="M9 21v-6h6v6" />
      </Icon>
    ),
  },
  {
    to: '/disruptions',
    label: 'Disruptions',
    icon: (
      <Icon>
        <path d="M12 3 2 20h20L12 3Z" />
        <path d="M12 10v4" />
        <path d="M12 17h.01" />
      </Icon>
    ),
  },
  {
    to: '/reroutes',
    label: 'Reroutes',
    icon: (
      <Icon>
        <path d="M4 6h11a4 4 0 0 1 0 8H7" />
        <path d="m10 10-3 4 3 4" />
      </Icon>
    ),
  },
  {
    to: '/analytics',
    label: 'Analytics',
    icon: (
      <Icon>
        <path d="M4 20V10" />
        <path d="M11 20V4" />
        <path d="M18 20v-7" />
      </Icon>
    ),
  },
  {
    to: '/agent-traces',
    label: 'Agent Traces',
    icon: (
      <Icon>
        <circle cx="12" cy="12" r="8" />
        <path d="M12 8v4l2.5 2.5" />
      </Icon>
    ),
  },
]

export function AppShell() {
  const { user, logout } = useAuth()
  const { connected } = useNotifications()

  return (
    <div className="min-h-screen bg-bg text-text">
      <header className="sticky top-0 z-20 flex h-16 items-center justify-between border-b border-border bg-surface/80 px-6 backdrop-blur-md">
        <div className="flex items-center gap-2.5">
          <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent text-sm font-bold text-accent-fg shadow-sm shadow-accent/30">
            S
          </span>
          <div className="leading-tight">
            <span className="block text-base font-semibold tracking-tight">SmartGrid</span>
            <span className="hidden text-[11px] text-text-muted sm:block">Disruption detection &amp; autonomous rerouting</span>
          </div>
        </div>

        <div className="flex items-center gap-4">
          <span
            className="flex items-center gap-1.5 text-xs text-text-muted"
            title={connected ? 'Live updates connected' : 'Live updates disconnected'}
          >
            <span className={`h-2 w-2 rounded-full transition-colors ${connected ? 'bg-success shadow-[0_0_6px] shadow-success/60' : 'bg-text-muted'}`} />
            {connected ? 'Live' : 'Offline'}
          </span>
          <ThemeToggle />
          {user && (
            <div className="flex items-center gap-3 border-l border-border pl-4">
              <span className="max-w-[10rem] truncate text-sm text-text-muted">{user.username}</span>
              <button
                type="button"
                onClick={() => logout()}
                className="shrink-0 rounded-lg px-2.5 py-1.5 text-sm font-medium text-text-muted transition-colors hover:bg-border/50 hover:text-text active:scale-95"
              >
                Log out
              </button>
            </div>
          )}
        </div>
      </header>

      <div className="flex">
        <aside className="sticky top-16 flex h-[calc(100vh-4rem)] w-16 shrink-0 flex-col border-r border-border bg-surface transition-[width] lg:w-56">
          <nav className="flex flex-1 flex-col gap-1 overflow-y-auto p-3">
            {NAV_ITEMS.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                title={item.label}
                className={({ isActive }) =>
                  `group relative flex items-center gap-2.5 rounded-lg px-3 py-2.5 text-sm font-medium transition-all duration-150 ease-out active:scale-[0.97] ${
                    isActive
                      ? 'bg-accent text-accent-fg shadow-sm shadow-accent/30'
                      : 'text-text-muted hover:translate-x-0.5 hover:bg-border/50 hover:text-text'
                  }`
                }
              >
                {item.icon}
                <span className="hidden truncate lg:inline">{item.label}</span>
              </NavLink>
            ))}
          </nav>
        </aside>

        <main className="min-w-0 flex-1 px-8 py-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
