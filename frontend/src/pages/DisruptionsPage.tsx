import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import * as analyticsApi from '../api/analytics'
import * as agentsApi from '../api/agents'
import { Card } from '../components/ui/Card'
import { Badge } from '../components/ui/Badge'
import { InfoTooltip } from '../components/ui/InfoTooltip'
import { LoadingState, ErrorState, EmptyState } from '../components/ui/StateViews'
import { AgentDecisionTimeline } from '../components/ui/AgentDecisionTimeline'
import type { Disruption } from '../types'

export function DisruptionsPage() {
  const [selectedDisruptionForTrace, setSelectedDisruptionForTrace] = useState<Disruption | null>(null)

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['analytics', 'disruptions'],
    queryFn: analyticsApi.disruptionsSummary,
    refetchInterval: 15_000,
  })

  const { data: activeDisruptionsList, isLoading: loadingActive } = useQuery({
    queryKey: ['disruptions', 'active'],
    queryFn: agentsApi.activeDisruptions,
    refetchInterval: 15_000,
  })

  const { data: timelineData, isLoading: loadingTimeline } = useQuery({
    queryKey: ['disruptions', 'timeline', selectedDisruptionForTrace?.id],
    queryFn: () => agentsApi.disruptionTimeline(selectedDisruptionForTrace!.id),
    enabled: !!selectedDisruptionForTrace,
  })

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="flex items-center gap-1.5 text-2xl font-bold tracking-tight text-text">
            Disruption Detection &amp; Agent Traces
            <InfoTooltip label="Disruption Detector Agent">
              The Disruption Detector monitors anomaly signals in 15-minute sliding windows. When 2+ correlated signals
              occur, the agent evaluates RAG context, reasons with an LLM, and publishes a disruption event.
            </InfoTooltip>
          </h1>
          <p className="mt-1 text-xs text-text-muted">
            Live vendor disruption alerts, regional impact summaries, and full agent execution traces.
          </p>
        </div>
      </div>

      {/* Active Disruptions with Trace Inspection */}
      <Card className="p-5">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-sm font-semibold text-text">Active Detected Disruptions</h2>
            <p className="text-xs text-text-muted">
              Inspect agent reasoning and triggering Kafka events for each active disruption.
            </p>
          </div>
          <span className="rounded bg-accent/15 px-2 py-0.5 text-xs font-bold text-accent">
            {activeDisruptionsList?.length ?? 0} active
          </span>
        </div>

        {loadingActive && <LoadingState label="Loading active disruptions…" />}
        {!loadingActive && (
          <div className="overflow-x-auto">
            {activeDisruptionsList?.length ? (
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="border-b border-border text-text-muted">
                    <th className="pb-3 font-medium">Disruption ID</th>
                    <th className="pb-3 font-medium">Vendor</th>
                    <th className="pb-3 font-medium">Region</th>
                    <th className="pb-3 font-medium">Confidence</th>
                    <th className="pb-3 font-medium">Status</th>
                    <th className="pb-3 font-medium">Detected</th>
                    <th className="pb-3 text-right font-medium">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border/60">
                  {activeDisruptionsList.map((d) => (
                    <tr key={d.id} className="hover:bg-surface-raised/40 transition">
                      <td className="py-3 font-mono font-bold text-text">{d.id.slice(0, 8)}</td>
                      <td className="py-3 font-mono text-accent font-semibold">{d.vendorId}</td>
                      <td className="py-3 text-text">{d.region}</td>
                      <td className="py-3">
                        <span
                          className={`font-mono font-bold ${
                            d.confidence >= 0.7 ? 'text-emerald-400' : 'text-amber-400'
                          }`}
                        >
                          {(d.confidence * 100).toFixed(0)}%
                        </span>
                      </td>
                      <td className="py-3">
                        <Badge tone={d.status === 'PUBLISHED' ? 'danger' : 'warning'}>
                          {d.status}
                        </Badge>
                      </td>
                      <td className="py-3 text-text-muted">
                        {new Date(d.createdAt).toLocaleTimeString()}
                      </td>
                      <td className="py-3 text-right">
                        <button
                          type="button"
                          onClick={() => setSelectedDisruptionForTrace(d)}
                          className="rounded border border-border bg-surface px-2.5 py-1 text-[11px] font-semibold text-text transition hover:border-accent hover:text-accent shadow-sm active:scale-95"
                        >
                          View Agent Trace
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <EmptyState
                title="No active disruptions"
                description="No active disruptions requiring intervention."
              />
            )}
          </div>
        )}
      </Card>

      {/* Regional Window Aggregates */}
      <Card className="p-5">
        <h2 className="text-sm font-semibold text-text mb-3">Regional Window Aggregates</h2>
        {isLoading && <LoadingState label="Loading window aggregates…" />}
        {isError && <ErrorState message="Couldn't load disruption data." onRetry={() => refetch()} />}

        {!isLoading && !isError && (
          <>
            {data?.length ? (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="border-b border-border text-text-muted">
                      <th className="py-2.5 pr-4 font-medium">Region</th>
                      <th className="py-2.5 pr-4 font-medium">Window Type</th>
                      <th className="py-2.5 pr-4 font-medium">Count</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border/60">
                    {data.map((d) => (
                      <tr key={`${d.region}-${d.windowType}-${d.windowStart}`}>
                        <td className="py-2.5 pr-4 text-text font-medium">{d.region}</td>
                        <td className="py-2.5 pr-4">
                          <Badge tone={d.windowType === 'TUMBLING' ? 'info' : 'default'}>{d.windowType}</Badge>
                        </td>
                        <td className="py-2.5 pr-4 font-mono font-bold text-text tabular-nums">{d.count}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <EmptyState title="No disruptions" description="No disruption events recorded in current window." />
            )}
          </>
        )}
      </Card>

      {/* Disruption Trace Modal */}
      {selectedDisruptionForTrace && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/65 p-4 backdrop-blur-sm animate-[fadeIn_150ms_ease-out]">
          <Card className="flex max-h-[85vh] w-full max-w-3xl flex-col overflow-hidden p-6 shadow-2xl">
            <div className="flex-1 overflow-y-auto py-2 pr-1">
              {loadingTimeline ? (
                <LoadingState label="Loading disruption agent execution timeline…" />
              ) : timelineData ? (
                <AgentDecisionTimeline
                  data={timelineData}
                  onClose={() => setSelectedDisruptionForTrace(null)}
                />
              ) : (
                <EmptyState title="No timeline" description="Could not load timeline for this disruption." />
              )}
            </div>

            <div className="flex justify-end border-t border-border pt-3 mt-4">
              <button
                type="button"
                onClick={() => setSelectedDisruptionForTrace(null)}
                className="rounded-lg bg-surface border border-border px-4 py-1.5 text-xs font-medium text-text hover:bg-border/40"
              >
                Close Trace
              </button>
            </div>
          </Card>
        </div>
      )}
    </div>
  )
}
