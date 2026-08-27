import { useQuery } from '@tanstack/react-query'
import * as analyticsApi from '../api/analytics'
import { Card } from '../components/ui/Card'
import { Badge } from '../components/ui/Badge'
import { InfoTooltip } from '../components/ui/InfoTooltip'
import { LoadingState, ErrorState, EmptyState } from '../components/ui/StateViews'

export function DisruptionsPage() {
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['analytics', 'disruptions'],
    queryFn: analyticsApi.disruptionsSummary,
    refetchInterval: 15_000,
  })

  return (
    <div className="flex flex-col gap-6">
      <h1 className="flex items-center gap-1.5 text-2xl font-semibold text-text">
        Disruptions
        <InfoTooltip label="Tumbling vs. hopping windows">
          Both just mean "counted over a time period." A tumbling window resets every hour, so it's a clean count
          for that hour alone. A hopping window is a rolling 7-day total that updates continuously, useful for
          spotting a trend rather than a single spike.
        </InfoTooltip>
      </h1>

      {isLoading && <LoadingState label="Loading disruptions…" />}
      {isError && <ErrorState message="Couldn't load disruption data." onRetry={() => refetch()} />}

      {!isLoading && !isError && (
        <Card>
          {data?.length ? (
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-border text-text-muted">
                  <th className="py-2 pr-4 font-medium">Region</th>
                  <th className="py-2 pr-4 font-medium">Window</th>
                  <th className="py-2 pr-4 font-medium">Count</th>
                </tr>
              </thead>
              <tbody>
                {data.map((d) => (
                  <tr key={`${d.region}-${d.windowType}-${d.windowStart}`} className="border-b border-border/60 last:border-0">
                    <td className="py-2 pr-4">{d.region}</td>
                    <td className="py-2 pr-4">
                      <Badge tone={d.windowType === 'TUMBLING' ? 'info' : 'default'}>{d.windowType}</Badge>
                    </td>
                    <td className="py-2 pr-4 tabular-nums">{d.count}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <EmptyState title="No disruptions" description="No disruption events have been detected yet." />
          )}
        </Card>
      )}
    </div>
  )
}
