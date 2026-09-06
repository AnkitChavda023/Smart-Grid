import { Client, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { createContext, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { useAuth } from '../auth/AuthContext'
import type { NotificationPushMessage } from '../types'

const NOTIFICATION_WS_URL = 'http://localhost:8084/ws-sockjs'

export interface AppNotification extends NotificationPushMessage {
  localId: string
  receivedAt: string
  read: boolean
}

interface NotificationsContextValue {
  connected: boolean
  recent: NotificationPushMessage[]
  notifications: AppNotification[]
  unreadCount: number
  markAsRead: (localId: string) => void
  markAllRead: () => void
  clearAll: () => void
  subscribeToOrder: (orderId: string, onMessage: (message: NotificationPushMessage) => void) => () => void
}

const NotificationsContext = createContext<NotificationsContextValue | undefined>(undefined)

export function NotificationsProvider({ children }: { children: ReactNode }) {
  const { authenticated } = useAuth()
  const [connected, setConnected] = useState(false)
  const [notifications, setNotifications] = useState<AppNotification[]>(() => {
    try {
      const saved = localStorage.getItem('smartgrid_notifications')
      return saved ? JSON.parse(saved) : []
    } catch {
      return []
    }
  })
  const clientRef = useRef<Client | null>(null)

  // Save to localStorage when notifications change
  useEffect(() => {
    try {
      localStorage.setItem('smartgrid_notifications', JSON.stringify(notifications.slice(0, 50)))
    } catch {
      // ignore storage write errors
    }
  }, [notifications])

  useEffect(() => {
    if (!authenticated) return

    const client = new Client({
      webSocketFactory: () => new SockJS(NOTIFICATION_WS_URL) as WebSocket,
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true)
        client.subscribe('/topic/alerts/updates', (message: IMessage) => {
          try {
            const parsed = JSON.parse(message.body) as NotificationPushMessage
            const appNotif: AppNotification = {
              ...parsed,
              localId: `${parsed.notificationId || Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
              receivedAt: new Date().toISOString(),
              read: false,
            }
            setNotifications((prev) => [appNotif, ...prev].slice(0, 50))
          } catch (e) {
            console.warn('Failed to parse incoming notification:', e)
          }
        })
      },
      onDisconnect: () => setConnected(false),
      onWebSocketClose: () => setConnected(false),
    })

    client.activate()
    clientRef.current = client

    return () => {
      client.deactivate()
      clientRef.current = null
      setConnected(false)
    }
  }, [authenticated])

  const unreadCount = useMemo(() => notifications.filter((n) => !n.read).length, [notifications])

  const markAsRead = (localId: string) => {
    setNotifications((prev) =>
      prev.map((n) => (n.localId === localId ? { ...n, read: true } : n)),
    )
  }

  const markAllRead = () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })))
  }

  const clearAll = () => {
    setNotifications([])
  }

  const value = useMemo<NotificationsContextValue>(
    () => ({
      connected,
      recent: notifications,
      notifications,
      unreadCount,
      markAsRead,
      markAllRead,
      clearAll,
      subscribeToOrder: (orderId, onMessage) => {
        const client = clientRef.current
        if (!client || !client.connected) return () => undefined

        const subscription = client.subscribe(`/topic/orders/${orderId}/updates`, (message: IMessage) => {
          onMessage(JSON.parse(message.body) as NotificationPushMessage)
        })
        return () => subscription.unsubscribe()
      },
    }),
    [connected, notifications, unreadCount],
  )

  return <NotificationsContext.Provider value={value}>{children}</NotificationsContext.Provider>
}

export function useNotifications() {
  const context = useContext(NotificationsContext)
  if (!context) throw new Error('useNotifications must be used within a NotificationsProvider')
  return context
}
