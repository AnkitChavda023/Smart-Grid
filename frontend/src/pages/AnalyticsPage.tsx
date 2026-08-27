import { useQuery } from '@tanstack/react-query'
import * as analyticsApi from '../api/analytics'
import { Card } from '../components/ui/Card'
import { KpiCard } from '../components/ui/KpiCard'
import { LoadingState, ErrorState, EmptyState } from '../components/ui/StateViews'

const REFRESH_INTERVAL_MS = 15_000

export function AnalyticsPage() {
  const disruptions = useQuery({
    queryKey: ['analytics', 'disruptions'],
    queryFn: analyticsApi.disruptionsSummary,
    refetchInterval: REFRESH_INTERVAL_MS,
  })
  const throughput = useQuery({
    queryKey: ['analytics', 'throughput'],
    queryFn: analyticsApi.ordersThroughput,
    refetchInterval: REFRESH_INTERVAL_MS,
  })
  const vendorPerformance = useQuery({
    queryKey: ['analytics', 'vendor-performance'],
    queryFn: () => analyticsApi.vendorsPerformance('7d'),
    refetchInterval: REFRESH_INTERVAL_MS,
  })
  const rerouteRate = useQuery({
    queryKey: ['analytics', 'reroute-rate'],
    queryFn: analyticsApi.reroutesSuccessRate,
    refetchInterval: REFRESH_INTERVAL_MS,
  })

  const isLoading = disruptions.isLoading || throughput.isLoading || vendorPerformance.isLoading || rerouteRate.isLoading
  const isError = disruptions.isError || throughput.isError || vendorPerformance.isError || rerouteRate.isError

  const totalDisruptions = disruptions.data
    ?.filter((d) => d.windowType === 'TUMBLING')
    .reduce((sum, d) => sum + d.count, 0)
  const totalFulfilled = throughput.data?.filter((t) => t.stage === 'FULFILLED').reduce((sum, t) => sum + t.count, 0)
  const breachedVendorCount = vendorPerformance.data?.filter((v) => (v.slaBreachCount ?? 0) > 0).length

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-semibold text-text">Analytics</h1>

      {isLoading && <LoadingState label="Loading analytics…" />}
      {isError && (
        <ErrorState
          message="Couldn't load analytics."
          onRetry={() => {
            disruptions.refetch()
            throughput.refetch()
            vendorPerformance.refetch()
            rerouteRate.refetch()
          }}
        />
      )}

      {!isLoading && !isError && (
        <>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <KpiCard label="Disruptions (current hour)" value={totalDisruptions ?? 0} tone={totalDisruptions ? 'warning' : 'default'} />
            <KpiCard label="Orders fulfilled (current hour)" value={totalFulfilled ?? 0} tone="success" />
            <KpiCard
              label="Reroute success rate"
              value={`${((rerouteRate.data?.successRate ?? 0) * 100).toFixed(0)}%`}
              hint={`${rerouteRate.data?.successCount ?? 0} succeeded, ${rerouteRate.data?.escalationCount ?? 0} escalated`}
              tone="info"
            />
            <KpiCard
              label="Vendors with SLA breaches"
              value={breachedVendorCount ?? 0}
              tone={breachedVendorCount ? 'danger' : 'default'}
            />
          </div>

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <Card>
              <h2 className="mb-3 text-sm font-medium text-text-muted">Disruptions by region</h2>
              {disruptions.data?.length ? (
                <ul className="flex flex-col divide-y divide-border text-sm">
                  {disruptions.data
                    .filter((d) => d.windowType === 'TUMBLING')
                    .map((d) => (
                      <li key={`${d.region}-${d.windowStart}`} className="flex justify-between py-2">
                        <span className="text-text">{d.region}</span>
                        <span className="tabular-nums text-text-muted">{d.count}</span>
                      </li>
                    ))}
                </ul>
              ) : (
                <EmptyState title="No disruptions detected" description="Nothing to show for the current window." />
              )}
            </Card>

            <Card>
              <h2 className="mb-3 text-sm font-medium text-text-muted">Vendor performance (7d)</h2>
              {vendorPerformance.data?.length ? (
                <ul className="flex flex-col divide-y divide-border text-sm">
                  {vendorPerformance.data.map((v) => (
                    <li key={v.vendorId} className="flex justify-between py-2">
                      <span className="font-mono text-xs text-text-muted">{v.vendorId.slice(0, 8)}</span>
                      <span className="flex gap-3 tabular-nums">
                        <span className={v.slaBreachCount ? 'text-danger' : 'text-text-muted'}>
                          {v.slaBreachCount ?? 0} breaches
                        </span>
                        <span className="text-text-muted">score {v.latestScore?.toFixed(2) ?? '-'}</span>
                      </span>
                    </li>
                  ))}
                </ul>
              ) : (
                <EmptyState title="No vendor performance data" description="No SLA breaches or score updates yet." />
              )}
            </Card>
          </div>
        </>
      )}
    </div>
  )
}
