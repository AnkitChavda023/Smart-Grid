import { useState } from 'react'
import type { ReactNode, SVGProps } from 'react'
import { useQuery } from '@tanstack/react-query'
import * as agentsApi from '../api/agents'
import { Card } from '../components/ui/Card'
import { Badge } from '../components/ui/Badge'
import { InfoTooltip } from '../components/ui/InfoTooltip'
import { LoadingState, ErrorState, EmptyState } from '../components/ui/StateViews'

type Accent = 'accent' | 'success' | 'warning' | 'danger' | 'info'

const ACCENT_ICON_CLASSES: Record<Accent, string> = {
  accent: 'bg-accent/10 text-accent ring-accent/20',
  success: 'bg-success/10 text-success ring-success/20',
  warning: 'bg-warning/10 text-warning ring-warning/20',
  danger: 'bg-danger/10 text-danger ring-danger/20',
  info: 'bg-info/10 text-info ring-info/20',
}

const ACCENT_BORDER_CLASSES: Record<Accent, string> = {
  accent: 'border-t-accent',
  success: 'border-t-success',
  warning: 'border-t-warning',
  danger: 'border-t-danger',
  info: 'border-t-info',
}

const METER_FILL_CLASSES: Record<'success' | 'warning' | 'danger', string> = {
  success: 'bg-success',
  warning: 'bg-warning',
  danger: 'bg-danger',
}

function Icon({ children, ...props }: { children: ReactNode } & SVGProps<SVGSVGElement>) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      className="h-[18px] w-[18px] shrink-0"
      {...props}
    >
      {children}
    </svg>
  )
}

const AGENT_ICONS = {
  disruption: (
    <Icon>
      <path d="M13 2 4 14h6l-1 8 9-12h-6l1-8Z" />
    </Icon>
  ),
  vendor: (
    <Icon>
      <path d="M12 2 3 6v2h18V6l-9-4Z" />
      <path d="M5 8v11M9 8v11M15 8v11M19 8v11" />
      <path d="M3 19h18v2H3z" />
    </Icon>
  ),
  breach: (
    <Icon>
      <path d="M12 3a9 9 0 1 0 9 9" />
      <path d="M12 12 17 7" />
      <path d="M12 3v2M21 12h-2M6.3 6.3 4.9 4.9" />
    </Icon>
  ),
  forecast: (
    <Icon>
      <path d="M4 19h16" />
      <path d="M6 15l4-5 3 3 5-7" />
    </Icon>
  ),
  contract: (
    <Icon>
      <path d="M7 3h7l4 4v14H7Z" />
      <path d="M14 3v4h4" />
      <path d="M9.5 12h5M9.5 15.5h5" />
    </Icon>
  ),
}

function AgentIcon({ accent, children }: { accent: Accent; children: ReactNode }) {
  return (
    <span
      className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ring-1 ${ACCENT_ICON_CLASSES[accent]}`}
    >
      {children}
    </span>
  )
}

function AgentPanelHeader({
  accent,
  icon,
  title,
  description,
  tooltipLabel,
  tooltipBody,
}: {
  accent: Accent
  icon: ReactNode
  title: string
  description: string
  tooltipLabel?: string
  tooltipBody?: string
}) {
  return (
    <div className="flex items-start gap-3">
      <AgentIcon accent={accent}>{icon}</AgentIcon>
      <div className="min-w-0 flex-1 pt-0.5">
        <h3 className="flex items-center gap-1.5 text-sm font-semibold text-text">
          {title}
          {tooltipLabel && tooltipBody && <InfoTooltip label={tooltipLabel}>{tooltipBody}</InfoTooltip>}
        </h3>
        <p className="mt-0.5 text-xs leading-relaxed text-text-muted">{description}</p>
      </div>
    </div>
  )
}

function ConfidenceBadge({ confidence }: { confidence: number }) {
  const tone = confidence >= 0.7 ? 'success' : confidence >= 0.4 ? 'warning' : 'danger'
  const pct = Math.round(confidence * 100)
  return (
    <span className="inline-flex items-center gap-2">
      <Badge tone={tone}>{pct}% confidence</Badge>
      <span className="hidden h-1.5 w-16 overflow-hidden rounded-full bg-border sm:inline-block">
        <span
          className={`block h-full rounded-full transition-[width] duration-500 ${METER_FILL_CLASSES[tone]}`}
          style={{ width: `${pct}%` }}
        />
      </span>
    </span>
  )
}

function RerouteTraceTimeline({ rerouteId }: { rerouteId: string }) {
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['reroute-trace', rerouteId],
    queryFn: () => agentsApi.rerouteTrace(rerouteId),
  })

  if (isLoading) return <LoadingState label="Loading tool call trace…" />
  if (isError) return <ErrorState message="Couldn't load the trace for this reroute." onRetry={() => refetch()} />
  if (!data?.length) return <EmptyState title="No trace recorded" description="This reroute has no stored tool call trace." />

  return (
    <ol className="flex flex-col gap-4">
      {data.map((step, idx) => (
        <li key={step.stepIndex} className="relative flex gap-4">
          <div className="flex flex-col items-center">
            <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-accent/10 text-xs font-semibold text-accent ring-1 ring-accent/20">
              {step.stepIndex + 1}
            </span>
            {idx < data.length - 1 && <span className="mt-1 w-px flex-1 bg-border" />}
          </div>
          <div className="min-w-0 flex-1 rounded-lg border border-border bg-bg/40 p-4 pb-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <span className="text-sm font-semibold text-text">
                <span className="font-mono text-accent">{step.toolName}</span>
              </span>
              <span className="rounded-full bg-border/50 px-2 py-0.5 text-xs text-text-muted tabular-nums">
                {step.latencyMs}ms
              </span>
            </div>
            <div className="mt-3 grid gap-2 sm:grid-cols-2">
              <div>
                <p className="text-[11px] font-semibold uppercase tracking-wide text-text-muted">Input</p>
                <pre className="mt-1 max-h-32 overflow-auto rounded-md bg-surface p-2 text-xs text-text">{step.input}</pre>
              </div>
              <div>
                <p className="text-[11px] font-semibold uppercase tracking-wide text-text-muted">Output</p>
                <pre className="mt-1 max-h-32 overflow-auto rounded-md bg-surface p-2 text-xs text-text">{step.output}</pre>
              </div>
            </div>
            {step.llmReasoning && (
              <div className="mt-3 border-t border-border pt-3">
                <p className="text-[11px] font-semibold uppercase tracking-wide text-text-muted">LLM reasoning</p>
                <p className="mt-1 text-sm leading-relaxed text-text">{step.llmReasoning}</p>
              </div>
            )}
          </div>
        </li>
      ))}
    </ol>
  )
}

function DisruptionTimelinePanel() {
  const [selectedDisruptionId, setSelectedDisruptionId] = useState<string | null>(null)

  const disruptions = useQuery({
    queryKey: ['disruptions', 'active'],
    queryFn: agentsApi.activeDisruptions,
    refetchInterval: 15_000,
  })

  const reroutes = useQuery({
    queryKey: ['reroutes', 'by-disruption', selectedDisruptionId],
    queryFn: () => agentsApi.reroutesForDisruption(selectedDisruptionId as string),
    enabled: !!selectedDisruptionId,
  })

  const selectedDisruption = disruptions.data?.find((d) => d.id === selectedDisruptionId)
  const latestReroute = reroutes.data?.[0]

  return (
    <Card className={`border-t-2 ${ACCENT_BORDER_CLASSES.warning}`}>
      <AgentPanelHeader
        accent="warning"
        icon={AGENT_ICONS.disruption}
        title="Disruption Detector & Reroute Planner"
        description="Watches every vendor for early trouble and, when confident enough, automatically rebooks affected orders to a backup."
        tooltipLabel="Tool call trace"
        tooltipBody="Step-by-step record of every real action the AI agent took to fix this disruption: which vendor it searched for, which one it checked stock with, and which quote it created and accepted, in order, with timing. Nothing here is guessed after the fact; it's the agent's actual work log. One overall confidence score applies to the whole reroute decision, not a separate score per step."
      />

      {disruptions.isLoading && <div className="mt-4"><LoadingState label="Loading disruptions…" /></div>}
      {disruptions.isError && (
        <div className="mt-4">
          <ErrorState message="Couldn't load disruptions." onRetry={() => disruptions.refetch()} />
        </div>
      )}

      {!disruptions.isLoading && !disruptions.isError && (
        <>
          {disruptions.data?.length ? (
            <div className="mt-4 flex flex-wrap gap-2">
              {disruptions.data.map((d) => (
                <button
                  key={d.id}
                  type="button"
                  onClick={() => setSelectedDisruptionId(d.id)}
                  className={`flex items-center gap-2 rounded-lg border px-3 py-1.5 text-sm font-medium transition-all duration-150 active:scale-[0.97] ${
                    d.id === selectedDisruptionId
                      ? 'border-accent bg-accent/10 text-accent shadow-sm shadow-accent/20'
                      : 'border-border bg-surface text-text hover:border-accent/40 hover:bg-border/40'
                  }`}
                >
                  <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-warning" />
                  {d.vendorId} <span className="text-text-muted">·</span> {d.region}
                </button>
              ))}
            </div>
          ) : (
            <div className="mt-4">
              <EmptyState title="No active disruptions" description="No disruptions have been published yet." />
            </div>
          )}
        </>
      )}

      {selectedDisruption && (
        <div className="mt-5 animate-[fadeIn_150ms_ease-out] border-t border-border pt-5">
          <div className="flex flex-wrap items-center gap-3">
            <h3 className="text-sm font-semibold text-text">
              Disruption: vendor {selectedDisruption.vendorId} in {selectedDisruption.region}
            </h3>
            <ConfidenceBadge confidence={selectedDisruption.confidence} />
          </div>
          <p className="mt-2 text-sm leading-relaxed text-text-muted">{selectedDisruption.reasoningTrace}</p>

          <div className="mt-4">
            {reroutes.isLoading && <LoadingState label="Loading reroute decisions…" />}
            {reroutes.isError && <ErrorState message="Couldn't load reroutes." onRetry={() => reroutes.refetch()} />}
            {!reroutes.isLoading && !reroutes.isError && !reroutes.data?.length && (
              <EmptyState title="No reroute decision yet" description="The reroute-planner agent has not acted on this disruption." />
            )}
            {latestReroute && (
              <div>
                <div className="flex flex-wrap items-center gap-3">
                  <h4 className="text-sm font-semibold text-text">
                    Reroute decision for order {latestReroute.orderId}
                  </h4>
                  <Badge tone={latestReroute.status === 'ESCALATED' ? 'warning' : 'success'}>{latestReroute.status}</Badge>
                  <ConfidenceBadge confidence={latestReroute.confidence} />
                </div>
                <div className="mt-4">
                  <RerouteTraceTimeline rerouteId={latestReroute.id} />
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </Card>
  )
}

function LookupField({
  label,
  placeholder,
  value,
  onSubmit,
}: {
  label: string
  placeholder: string
  value: string
  onSubmit: (value: string) => void
}) {
  const [draft, setDraft] = useState(value)
  return (
    <form
      className="flex items-end gap-2"
      onSubmit={(e) => {
        e.preventDefault()
        onSubmit(draft.trim())
      }}
    >
      <label className="flex flex-1 flex-col gap-1 text-xs font-medium text-text-muted">
        {label}
        <input
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          placeholder={placeholder}
          className="rounded-lg border border-border bg-surface px-3 py-2 text-sm text-text outline-none transition-colors focus:border-accent focus:ring-2 focus:ring-accent/20"
        />
      </label>
      <button
        type="submit"
        className="shrink-0 rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-text transition-all hover:border-accent/40 hover:bg-border/40 active:scale-95"
      >
        Look up
      </button>
    </form>
  )
}

function VendorEvaluatorPanel() {
  const [vendorId, setVendorId] = useState('')
  const query = useQuery({
    queryKey: ['vendor-evaluations', vendorId],
    queryFn: () => agentsApi.vendorEvaluations(vendorId),
    enabled: !!vendorId,
  })
  const latest = query.data?.[0]

  return (
    <Card className={`border-t-2 ${ACCENT_BORDER_CLASSES.info}`}>
      <AgentPanelHeader
        accent="info"
        icon={AGENT_ICONS.vendor}
        title="Vendor Evaluator"
        description="Scores a vendor's recent track record and flags whether it's trending up or down."
      />
      <div className="mt-4">
        <LookupField label="Vendor ID" placeholder="vendor UUID" value={vendorId} onSubmit={setVendorId} />
      </div>
      {query.isLoading && <div className="mt-3"><LoadingState label="Loading…" /></div>}
      {query.isError && <div className="mt-3"><ErrorState message="Lookup failed." onRetry={() => query.refetch()} /></div>}
      {vendorId && !query.isLoading && !query.isError && !latest && (
        <div className="mt-3"><EmptyState title="No evaluations yet" /></div>
      )}
      {latest && (
        <div className="mt-4 animate-[fadeIn_150ms_ease-out] flex flex-col gap-2 rounded-lg border border-border bg-bg/40 p-3 text-sm text-text">
          <div className="flex flex-wrap items-center gap-2">
            <Badge tone={latest.trendDirection === 'DECLINING' ? 'danger' : latest.trendDirection === 'IMPROVING' ? 'success' : 'default'}>
              {latest.trendDirection}
            </Badge>
            <ConfidenceBadge confidence={latest.confidence} />
          </div>
          <p className="text-text-muted">{latest.summary}</p>
        </div>
      )}
    </Card>
  )
}

function BreachAnalystPanel() {
  const [vendorId, setVendorId] = useState('')
  const query = useQuery({
    queryKey: ['breach-assessments', vendorId],
    queryFn: () => agentsApi.breachAssessments(vendorId),
    enabled: !!vendorId,
  })
  const latest = query.data?.[0]

  return (
    <Card className={`border-t-2 ${ACCENT_BORDER_CLASSES.danger}`}>
      <AgentPanelHeader
        accent="danger"
        icon={AGENT_ICONS.breach}
        title="SLA Breach Analyst"
        description="Estimates how likely a vendor is to miss its next delivery window, based on its real history."
        tooltipLabel="Poisson-process breach probability"
        tooltipBody={
          'A standard statistical model for "how often does a rare event happen over time," applied here to SLA breaches. It turns a vendor\'s breach history into a probability of another breach happening soon: not a guess, but a calculated number from that vendor\'s real track record.'
        }
      />
      <div className="mt-4">
        <LookupField label="Vendor ID" placeholder="vendor UUID" value={vendorId} onSubmit={setVendorId} />
      </div>
      {query.isLoading && <div className="mt-3"><LoadingState label="Loading…" /></div>}
      {query.isError && <div className="mt-3"><ErrorState message="Lookup failed." onRetry={() => query.refetch()} /></div>}
      {vendorId && !query.isLoading && !query.isError && !latest && (
        <div className="mt-3"><EmptyState title="No assessments yet" /></div>
      )}
      {latest && (
        <div className="mt-4 animate-[fadeIn_150ms_ease-out] flex flex-col gap-2 rounded-lg border border-border bg-bg/40 p-3 text-sm text-text">
          <div className="flex flex-wrap items-center gap-2">
            <Badge tone={latest.breachProbability > 0.65 ? 'danger' : 'default'}>
              {(latest.breachProbability * 100).toFixed(0)}% breach probability
            </Badge>
            {latest.restockTriggered && <Badge tone="warning">restock triggered</Badge>}
            <ConfidenceBadge confidence={latest.confidence} />
          </div>
          <p className="text-text-muted">{latest.summary}</p>
        </div>
      )}
    </Card>
  )
}

function DemandForecasterPanel() {
  const [skuId, setSkuId] = useState('')
  const query = useQuery({
    queryKey: ['demand-forecasts', skuId],
    queryFn: () => agentsApi.demandForecasts(skuId),
    enabled: !!skuId,
  })
  const latest = query.data?.[0]

  return (
    <Card className={`border-t-2 ${ACCENT_BORDER_CLASSES.accent}`}>
      <AgentPanelHeader
        accent="accent"
        icon={AGENT_ICONS.forecast}
        title="Demand Forecaster"
        description="Predicts how much of a SKU will be needed soon, as a range instead of one guess."
        tooltipLabel="P10 / P50 / P90"
        tooltipBody='Three demand estimates instead of one. P50 is the "most likely" forecast. P10 is a cautious low estimate (demand is above this 90% of the time) and P90 is a cautious high estimate (demand is below this 90% of the time). Together they show a realistic range instead of false precision.'
      />
      <div className="mt-4">
        <LookupField label="SKU ID" placeholder="sku id" value={skuId} onSubmit={setSkuId} />
      </div>
      {query.isLoading && <div className="mt-3"><LoadingState label="Loading…" /></div>}
      {query.isError && <div className="mt-3"><ErrorState message="Lookup failed." onRetry={() => query.refetch()} /></div>}
      {skuId && !query.isLoading && !query.isError && !latest && (
        <div className="mt-3"><EmptyState title="No forecasts yet" /></div>
      )}
      {latest && (
        <div className="mt-4 animate-[fadeIn_150ms_ease-out] flex flex-col gap-2 rounded-lg border border-border bg-bg/40 p-3 text-sm text-text">
          <div className="flex flex-wrap items-center gap-2">
            <Badge tone="info">{latest.horizonDays}-day horizon</Badge>
            <ConfidenceBadge confidence={latest.confidence} />
          </div>
          <div className="flex items-center gap-3 tabular-nums text-text-muted">
            <span>P10 <span className="font-semibold text-text">{latest.p10.toFixed(1)}</span></span>
            <span className="h-3 w-px bg-border" />
            <span>P50 <span className="font-semibold text-text">{latest.p50.toFixed(1)}</span></span>
            <span className="h-3 w-px bg-border" />
            <span>P90 <span className="font-semibold text-text">{latest.p90.toFixed(1)}</span></span>
          </div>
        </div>
      )}
    </Card>
  )
}

function ContractNegotiationPanel() {
  const [vendorId, setVendorId] = useState('')
  const query = useQuery({
    queryKey: ['negotiations', vendorId],
    queryFn: () => agentsApi.negotiationRuns(vendorId),
    enabled: !!vendorId,
  })
  const latest = query.data?.[0]

  return (
    <Card className={`border-t-2 ${ACCENT_BORDER_CLASSES.success}`}>
      <AgentPanelHeader
        accent="success"
        icon={AGENT_ICONS.contract}
        title="Contract Negotiation"
        description="Drafts a revised contract when terms drift from market rates. A person always approves before it's real."
        tooltipLabel="Clause similarity"
        tooltipBody="How much of the current contract's wording survived in the AI's proposed rewrite, from 0-100%. A high number means a light edit; a low number means a substantial rewrite. It's a measurement, not a judgment: a human reviewer decides whether the proposed change is actually a good one. The AI can only ever save a draft; only a Planner or Admin can submit it for real."
      />
      <div className="mt-4">
        <LookupField label="Vendor ID" placeholder="vendor UUID" value={vendorId} onSubmit={setVendorId} />
      </div>
      {query.isLoading && <div className="mt-3"><LoadingState label="Loading…" /></div>}
      {query.isError && <div className="mt-3"><ErrorState message="Lookup failed." onRetry={() => query.refetch()} /></div>}
      {vendorId && !query.isLoading && !query.isError && !latest && (
        <div className="mt-3"><EmptyState title="No negotiation runs yet" /></div>
      )}
      {latest && (
        <div className="mt-4 animate-[fadeIn_150ms_ease-out] flex flex-col gap-2 rounded-lg border border-border bg-bg/40 p-3 text-sm text-text">
          <div className="flex flex-wrap items-center gap-2">
            {latest.draftId ? <Badge tone="success">draft {latest.draftId.slice(0, 8)}</Badge> : <Badge tone="warning">pending review</Badge>}
            <Badge tone="default">{(latest.clauseSimilarity * 100).toFixed(0)}% clause similarity</Badge>
            <ConfidenceBadge confidence={latest.confidence} />
          </div>
          <p className="text-text-muted">{latest.summary}</p>
        </div>
      )}
    </Card>
  )
}

export function AgentTracesPage() {
  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="flex items-center gap-1.5 text-2xl font-semibold text-text">
          Agent Traces
          <InfoTooltip label="Confidence score">
            How sure the AI is about its own conclusion, from 0-100%. Above the agent's threshold (usually 60-70%),
            it acts on its own. Below it, the decision is sent to a person to review instead of being applied
            automatically. The AI never silently guesses on something it isn't confident about.
          </InfoTooltip>
        </h1>
        <p className="mt-1 max-w-2xl text-sm text-text-muted">
          A live look at what each AI agent decided, how confident it was, and the real actions it took to get there.
        </p>
      </div>

      <DisruptionTimelinePanel />

      <div>
        <h2 className="text-lg font-semibold text-text">Other Agent Activity</h2>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <VendorEvaluatorPanel />
          <BreachAnalystPanel />
          <DemandForecasterPanel />
          <ContractNegotiationPanel />
        </div>
      </div>
    </div>
  )
}
