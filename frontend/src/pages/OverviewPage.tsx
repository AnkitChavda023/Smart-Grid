import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import * as ordersApi from '../api/orders'
import * as agentsApi from '../api/agents'
import * as analyticsApi from '../api/analytics'
import { Card } from '../components/ui/Card'
import { KpiCard } from '../components/ui/KpiCard'
import { useAuth } from '../auth/AuthContext'
import { useNotifications } from '../ws/NotificationsContext'

interface QuickLink {
  to: string
  title: string
  description: string
}

const QUICK_LINKS: QuickLink[] = [
  {
    to: '/orders',
    title: 'Orders',
    description: 'See every order moving through the system and its current stage, and create a new one.',
  },
  {
    to: '/vendors',
    title: 'Vendors',
    description: 'Search and compare the suppliers SmartGrid can order from, ranked by price, speed, and reliability.',
  },
  {
    to: '/disruptions',
    title: 'Disruptions',
    description: 'Counts of supply problems the system has automatically detected, grouped by region.',
  },
  {
    to: '/reroutes',
    title: 'Reroutes',
    description: 'How often the AI successfully switched an affected order to a backup vendor on its own.',
  },
  {
    to: '/agent-traces',
    title: 'Agent Traces',
    description: 'The evidence trail behind every AI decision: what it checked, what it concluded, and why.',
  },
  {
    to: '/analytics',
    title: 'Analytics',
    description: 'Order volume and vendor performance trends over time.',
  },
]

export function OverviewPage() {
  const { user } = useAuth()
  const { connected } = useNotifications()

  const orders = useQuery({ queryKey: ['overview', 'orders'], queryFn: () => ordersApi.listOrders() })
  const disruptions = useQuery({ queryKey: ['overview', 'disruptions'], queryFn: agentsApi.activeDisruptions })
  const rerouteRate = useQuery({ queryKey: ['overview', 'reroute-rate'], queryFn: analyticsApi.reroutesSuccessRate })

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold text-text">
          Welcome{user?.username ? `, ${user.username}` : ''}
        </h1>
        <p className="mt-1 max-w-2xl text-sm text-text-muted">
          SmartGrid watches every order your company places with its vendors. When something threatens to disrupt
          one, such as a supplier missing deliveries or a shipment falling behind, an AI agent notices the pattern,
          checks the situation automatically, and either fixes it by rebooking a backup vendor or hands it to a
          person to decide. This page is a starting point; use the links below to explore any part of it.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <KpiCard
          label="Total orders"
          value={orders.isLoading ? '-' : orders.isError ? '-' : (orders.data?.totalElements ?? 0)}
          hint="All orders ever placed"
        />
        <KpiCard
          label="Active disruptions"
          value={disruptions.isLoading ? '-' : disruptions.isError ? '-' : (disruptions.data?.length ?? 0)}
          tone={disruptions.data?.length ? 'warning' : 'success'}
          hint="Supply problems currently being handled"
        />
        <KpiCard
          label="Automatic reroute success"
          value={
            rerouteRate.isLoading || rerouteRate.isError
              ? '-'
              : `${((rerouteRate.data?.successRate ?? 0) * 100).toFixed(0)}%`
          }
          tone="info"
          hint="Disruptions the AI fixed without a human"
        />
        <KpiCard
          label="Live updates"
          value={connected ? 'Connected' : 'Offline'}
          tone={connected ? 'success' : 'default'}
          hint="Real-time notifications for this session"
        />
      </div>

      <div>
        <h2 className="text-lg font-semibold text-text">Explore SmartGrid</h2>
        <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {QUICK_LINKS.map((link) => (
            <Link key={link.to} to={link.to} className="block">
              <Card className="h-full transition-all duration-200 hover:-translate-y-0.5 hover:border-accent hover:shadow-lg hover:shadow-accent/10">
                <h3 className="text-base font-semibold text-text">{link.title}</h3>
                <p className="mt-1 text-sm text-text-muted">{link.description}</p>
              </Card>
            </Link>
          ))}
        </div>
      </div>
    </div>
  )
}
