import { useState, type ReactNode, type SVGProps } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { ThemeToggle } from '../../theme/ThemeToggle'
import { useNotifications } from '../../ws/NotificationsContext'
import { NotificationDrawer } from './NotificationDrawer'
import type { Role } from '../../types'

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
  roles?: Role[] // Allowed roles. If omitted, available to all.
}

const ALL_NAV_ITEMS: NavItem[] = [
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
    to: '/shipments',
    label: 'Shipments & GPS',
    icon: (
      <Icon>
        <rect width="16" height="13" x="4" y="5" rx="2" />
        <path d="m4 9 8 5 8-5" />
      </Icon>
    ),
  },
  {
    to: '/sla',
    label: 'Contracts & SLA',
    icon: (
      <Icon>
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
        <path d="M14 2v6h6" />
        <path d="M16 13H8" />
        <path d="M16 17H8" />
        <path d="M10 9H8" />
      </Icon>
    ),
  },
  {
    to: '/vendors',
    label: 'Vendors',
    roles: ['ADMIN', 'PLANNER'],
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
    roles: ['ADMIN', 'PLANNER'],
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
    label: 'Reroutes & Approvals',
    roles: ['ADMIN', 'PLANNER'],
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
    roles: ['ADMIN', 'PLANNER'],
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
    roles: ['ADMIN', 'PLANNER'],
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
  const { connected, unreadCount } = useNotifications()
  const [isNotificationDrawerOpen, setIsNotificationDrawerOpen] = useState(false)

  const currentRole = user?.role ?? 'PLANNER'

  const filteredNavItems = ALL_NAV_ITEMS.filter((item) => {
    if (!item.roles) return true
    return item.roles.includes(currentRole)
  })

  // Role badge tone and label
  const roleBadge = (() => {
    switch (currentRole) {
      case 'ADMIN':
        return {
          label: 'ADMIN',
          title: 'System Administrator — Manage users, vendors, contracts, all agents',
          className: 'bg-purple-500/15 text-purple-400 border border-purple-500/30',
        }
      case 'PLANNER':
        return {
          label: 'PLANNER',
          title: 'Procurement Manager — Create orders, approve reroutes, approve contract drafts, manage vendors',
          className: 'bg-blue-500/15 text-blue-400 border border-blue-500/30',
        }
      case 'SUPPLIER':
        return {
          label: 'SUPPLIER',
          title: 'Vendor Representative — View assigned orders, update delivery checkpoints, view SLA status',
          className: 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30',
        }
    }
  })()

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

        <div className="flex items-center gap-3">
          <span
            className="flex items-center gap-1.5 text-xs text-text-muted mr-1"
            title={connected ? 'Live WebSocket updates connected' : 'Live updates disconnected'}
          >
            <span className={`h-2 w-2 rounded-full transition-colors ${connected ? 'bg-success shadow-[0_0_6px] shadow-success/60' : 'bg-text-muted'}`} />
            {connected ? 'Live' : 'Offline'}
          </span>

          {/* Notification Bell with Real-Time Badge */}
          <button
            type="button"
            id="notification-bell-btn"
            onClick={() => setIsNotificationDrawerOpen(true)}
            className="relative rounded-lg p-2 text-text-muted transition-colors hover:bg-border/50 hover:text-text focus:outline-none"
            aria-label="View notifications"
            title={unreadCount > 0 ? `${unreadCount} unread notification(s)` : 'Notifications'}
          >
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.8}
                d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
              />
            </svg>
            {unreadCount > 0 && (
              <span className="absolute 1 top-0.5 right-0.5 flex h-4 min-w-[1rem] items-center justify-center rounded-full bg-rose-500 px-1 text-[10px] font-bold text-white shadow-sm ring-2 ring-surface animate-pulse">
                {unreadCount > 9 ? '9+' : unreadCount}
              </span>
            )}
          </button>

          <ThemeToggle />

          {user && (
            <div className="flex items-center gap-3 border-l border-border pl-4">
              <div className="flex flex-col items-end">
                <span className="max-w-[10rem] truncate text-xs font-medium text-text">{user.username}</span>
                <span
                  className={`mt-0.5 rounded-full px-2 py-0.2 text-[10px] font-bold tracking-wider ${roleBadge.className}`}
                  title={roleBadge.title}
                >
                  {roleBadge.label}
                </span>
              </div>
              <button
                type="button"
                onClick={() => logout()}
                className="shrink-0 rounded-lg px-2.5 py-1.5 text-xs font-medium text-text-muted transition-colors hover:bg-border/50 hover:text-text active:scale-95"
              >
                Log out
              </button>
            </div>
          )}
        </div>
      </header>

      {/* Slide-out Notification Drawer */}
      <NotificationDrawer
        isOpen={isNotificationDrawerOpen}
        onClose={() => setIsNotificationDrawerOpen(false)}
      />

      <div className="flex">
        <aside className="sticky top-16 flex h-[calc(100vh-4rem)] w-16 shrink-0 flex-col border-r border-border bg-surface transition-[width] lg:w-56">
          <nav className="flex flex-1 flex-col gap-1 overflow-y-auto p-3">
            {filteredNavItems.map((item) => (
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
