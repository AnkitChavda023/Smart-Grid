import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useNotifications, type AppNotification } from '../../ws/NotificationsContext'

interface NotificationDrawerProps {
  isOpen: boolean
  onClose: () => void
}

function timeAgo(isoDate: string): string {
  const diff = Date.now() - new Date(isoDate).getTime()
  const seconds = Math.floor(diff / 1000)
  if (seconds < 30) return 'Just now'
  if (seconds < 60) return `${seconds}s ago`
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h ago`
  const days = Math.floor(hours / 24)
  return `${days}d ago`
}

function getNotificationTarget(n: AppNotification): { path: string; label: string } {
  const text = `${n.title} ${n.body}`.toLowerCase()
  if (text.includes('reroute')) {
    return { path: '/reroutes', label: 'View Reroute' }
  }
  if (text.includes('disruption')) {
    return { path: '/disruptions', label: 'View Disruption' }
  }
  if (text.includes('sla') || text.includes('breach') || text.includes('contract') || text.includes('penalty')) {
    return { path: '/sla', label: 'View in SLA & Contracts' }
  }
  if (text.includes('shipment') || text.includes('checkpoint') || text.includes('transit') || text.includes('delivery')) {
    return { path: '/shipments', label: 'View in Shipments' }
  }
  if (text.includes('vendor')) {
    return { path: '/vendors', label: 'View in Vendors' }
  }
  if (n.relatedOrderId || text.includes('order')) {
    return { path: '/orders', label: 'View in Orders' }
  }
  return { path: '/', label: 'View Details' }
}

function getNotificationStyle(title: string) {
  const t = title.toLowerCase()
  if (t.includes('order')) {
    return {
      badgeText: 'ORDER',
      badgeClass: 'bg-blue-500/15 text-blue-400 border border-blue-500/30',
      borderClass: 'border-l-blue-500',
      icon: (
        <svg className="h-4 w-4 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z" />
        </svg>
      ),
    }
  }
  if (t.includes('disruption')) {
    return {
      badgeText: 'DISRUPTION',
      badgeClass: 'bg-amber-500/15 text-amber-400 border border-amber-500/30',
      borderClass: 'border-l-amber-500',
      icon: (
        <svg className="h-4 w-4 text-amber-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
      ),
    }
  }
  if (t.includes('reroute')) {
    return {
      badgeText: 'REROUTE',
      badgeClass: 'bg-purple-500/15 text-purple-400 border border-purple-500/30',
      borderClass: 'border-l-purple-500',
      icon: (
        <svg className="h-4 w-4 text-purple-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
        </svg>
      ),
    }
  }
  if (t.includes('sla') || t.includes('penalty')) {
    return {
      badgeText: 'SLA BREACH',
      badgeClass: 'bg-rose-500/15 text-rose-400 border border-rose-500/30',
      borderClass: 'border-l-rose-500',
      icon: (
        <svg className="h-4 w-4 text-rose-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      ),
    }
  }
  return {
    badgeText: 'TRANSIT',
    badgeClass: 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30',
    borderClass: 'border-l-emerald-500',
    icon: (
      <svg className="h-4 w-4 text-emerald-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
    ),
  }
}

export function NotificationDrawer({ isOpen, onClose }: NotificationDrawerProps) {
  const { notifications, unreadCount, markAsRead, markAllRead, clearAll } = useNotifications()
  const drawerRef = useRef<HTMLDivElement>(null)
  const navigate = useNavigate()

  const handleNotificationClick = (n: AppNotification) => {
    markAsRead(n.localId)
    onClose()
    const target = getNotificationTarget(n)
    navigate(target.path)
  }

  // Close on Escape
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    if (isOpen) {
      window.addEventListener('keydown', handleKeyDown)
    }
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, onClose])

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 flex justify-end">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/50 backdrop-blur-sm transition-opacity animate-fade-in"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Drawer panel */}
      <div
        ref={drawerRef}
        className="relative z-10 flex h-full w-full max-w-md flex-col border-l border-border bg-surface shadow-2xl transition-transform animate-slide-in"
      >
        {/* Header */}
        <div className="flex items-center justify-between border-b border-border px-5 py-4">
          <div className="flex items-center gap-2">
            <h2 className="text-base font-semibold text-text">Notifications</h2>
            {unreadCount > 0 && (
              <span className="rounded-full bg-accent/15 px-2 py-0.5 text-xs font-medium text-accent">
                {unreadCount} unread
              </span>
            )}
          </div>

          <div className="flex items-center gap-1">
            {notifications.length > 0 && (
              <>
                <button
                  type="button"
                  onClick={markAllRead}
                  className="rounded-lg px-2.5 py-1 text-xs font-medium text-text-muted transition-colors hover:bg-border/50 hover:text-text"
                >
                  Mark all read
                </button>
                <button
                  type="button"
                  onClick={clearAll}
                  className="rounded-lg px-2.5 py-1 text-xs font-medium text-rose-400 transition-colors hover:bg-rose-500/10"
                >
                  Clear
                </button>
              </>
            )}
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg p-1 text-text-muted transition-colors hover:bg-border/50 hover:text-text"
              aria-label="Close notification drawer"
            >
              <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>

        {/* List of Notification Cards */}
        <div className="flex-1 overflow-y-auto p-4 space-y-3">
          {notifications.length === 0 ? (
            <div className="flex h-full flex-col items-center justify-center p-8 text-center text-text-muted">
              <div className="flex h-14 w-14 items-center justify-center rounded-full bg-border/40 text-text-muted/60 mb-3">
                <svg className="h-7 w-7" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={1.5}
                    d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
                  />
                </svg>
              </div>
              <p className="text-sm font-medium text-text">You're all caught up!</p>
              <p className="mt-1 text-xs text-text-muted max-w-[16rem]">
                Real-time alerts for orders, disruptions, reroutes, and SLA events will appear here automatically.
              </p>
            </div>
          ) : (
            notifications.map((n: AppNotification) => {
              const style = getNotificationStyle(n.title)
              const target = getNotificationTarget(n)
              return (
                <div
                  key={n.localId}
                  onClick={() => handleNotificationClick(n)}
                  className={`group relative flex flex-col gap-1.5 rounded-xl border border-border/80 bg-bg p-3.5 transition-all hover:border-accent/40 hover:shadow-md cursor-pointer border-l-4 ${style.borderClass} ${
                    !n.read ? 'bg-surface/90 shadow-sm' : 'opacity-85'
                  }`}
                >
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <span className="p-1 rounded-md bg-surface border border-border/60">
                        {style.icon}
                      </span>
                      <span className="text-xs font-semibold text-text">
                        {n.title}
                      </span>
                    </div>

                    <div className="flex items-center gap-2">
                      <span className={`rounded-full px-2 py-0.2 text-[10px] font-bold tracking-wider ${style.badgeClass}`}>
                        {style.badgeText}
                      </span>
                      {!n.read && (
                        <span
                          className="h-2 w-2 rounded-full bg-accent animate-pulse"
                          title="Unread notification"
                        />
                      )}
                    </div>
                  </div>

                  <p className="text-xs text-text-muted pl-7 leading-relaxed">
                    {n.body}
                  </p>

                  <div className="mt-1 flex items-center justify-between pl-7 text-[11px] text-text-muted/70">
                    <span>{timeAgo(n.receivedAt)}</span>
                    <span className="font-medium text-accent flex items-center gap-1 group-hover:underline">
                      {target.label} &rarr;
                    </span>
                  </div>
                </div>
              )
            })
          )}
        </div>
      </div>
    </div>
  )
}
