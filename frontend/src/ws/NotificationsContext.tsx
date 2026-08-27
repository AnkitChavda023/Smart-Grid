import { Client, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { createContext, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { useAuth } from '../auth/AuthContext'
import type { NotificationPushMessage } from '../types'

const NOTIFICATION_WS_URL = 'http://localhost:8084/ws-sockjs'

interface NotificationsContextValue {
  connected: boolean
  recent: NotificationPushMessage[]
  subscribeToOrder: (orderId: string, onMessage: (message: NotificationPushMessage) => void) => () => void
}

const NotificationsContext = createContext<NotificationsContextValue | undefined>(undefined)

export function NotificationsProvider({ children }: { children: ReactNode }) {
  const { authenticated } = useAuth()
  const [connected, setConnected] = useState(false)
  const [recent, setRecent] = useState<NotificationPushMessage[]>([])
  const clientRef = useRef<Client | null>(null)

  useEffect(() => {
    if (!authenticated) return

    const client = new Client({
      webSocketFactory: () => new SockJS(NOTIFICATION_WS_URL) as WebSocket,
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true)
        client.subscribe('/topic/alerts/updates', (message: IMessage) => {
          const parsed = JSON.parse(message.body) as NotificationPushMessage
          setRecent((prev) => [parsed, ...prev].slice(0, 20))
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

  const value = useMemo<NotificationsContextValue>(
    () => ({
      connected,
      recent,
      subscribeToOrder: (orderId, onMessage) => {
        const client = clientRef.current
        if (!client || !client.connected) return () => undefined

        const subscription = client.subscribe(`/topic/orders/${orderId}/updates`, (message: IMessage) => {
          onMessage(JSON.parse(message.body) as NotificationPushMessage)
        })
        return () => subscription.unsubscribe()
      },
    }),
    [connected, recent],
  )

  return <NotificationsContext.Provider value={value}>{children}</NotificationsContext.Provider>
}

export function useNotifications() {
  const context = useContext(NotificationsContext)
  if (!context) throw new Error('useNotifications must be used within a NotificationsProvider')
  return context
}
