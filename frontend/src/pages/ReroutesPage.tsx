import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import * as analyticsApi from '../api/analytics'
import { Card } from '../components/ui/Card'
import { KpiCard } from '../components/ui/KpiCard'
import { LoadingState, ErrorState } from '../components/ui/StateViews'

export function ReroutesPage() {
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['analytics', 'reroute-rate'],
    queryFn: analyticsApi.reroutesSuccessRate,
    refetchInterval: 15_000,
  })

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-semibold text-text">Reroutes</h1>

      {isLoading && <LoadingState label="Loading reroute stats…" />}
      {isError && <ErrorState message="Couldn't load reroute data." onRetry={() => refetch()} />}

      {!isLoading && !isError && data && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <KpiCard label="Successful reroutes" value={data.successCount} tone="success" />
          <KpiCard label="Escalated to human" value={data.escalationCount} tone="warning" />
          <KpiCard label="Success rate" value={`${(data.successRate * 100).toFixed(0)}%`} tone="info" />
        </div>
      )}

      <Card>
        <p className="text-sm text-text-muted">
          For the detailed history behind a specific reroute, open{' '}
          <Link to="/agent-traces" className="font-medium text-accent hover:underline">
            Agent Traces
          </Link>
          .
        </p>
      </Card>
    </div>
  )
}
