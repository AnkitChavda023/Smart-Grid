import { useState } from 'react'
import type { AgentDecisionTimelineData } from '../../types'
import { Badge } from './Badge'

export function AgentDecisionTimeline({
  data,
  onClose,
}: {
  data: AgentDecisionTimelineData
  onClose?: () => void
}) {
  const [expandedToolIndex, setExpandedToolIndex] = useState<number | null>(null)
  const isHighConfidence = data.confidence >= 0.7
  const confidencePct = Math.round(data.confidence * 100)

  return (
    <div className="flex flex-col gap-6">
      {/* Header Summary */}
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border/70 pb-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="flex h-2.5 w-2.5 rounded-full bg-accent animate-pulse" />
            <h3 className="text-base font-bold tracking-tight text-text">
              {data.agentName} Execution Timeline
            </h3>
          </div>
          <p className="mt-0.5 text-xs text-text-muted">
            Decision ID: <span className="font-mono text-text">{data.decisionId}</span>
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Badge tone={isHighConfidence ? 'success' : 'warning'}>
            {data.status}
          </Badge>
          <span className="flex items-center gap-1 rounded-full bg-surface-raised px-2.5 py-1 font-mono text-xs font-bold text-text border border-border">
            {confidencePct}% Confidence
          </span>
          {onClose && (
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg p-1 text-text-muted hover:bg-border/40 hover:text-text transition ml-2"
            >
              ✕
            </button>
          )}
        </div>
      </div>

      {/* 5-Stage Chronological Timeline */}
      <ol className="relative ml-3 space-y-6 border-l border-border/80">
        {/* STAGE 1: Kafka Triggering Event */}
        <li className="relative pl-6">
          <span className="absolute -left-3.5 flex h-7 w-7 items-center justify-center rounded-full bg-blue-500/20 text-blue-400 ring-2 ring-blue-500/40 text-xs font-bold">
            1
          </span>
          <div className="rounded-xl border border-blue-500/30 bg-blue-500/5 p-4 transition hover:bg-blue-500/10">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <span className="rounded bg-blue-500/20 px-2 py-0.5 font-mono text-[11px] font-bold text-blue-400">
                  KAFKA TRIGGER
                </span>
                <span className="font-mono text-xs font-semibold text-text">
                  Topic: {data.kafkaTrigger.topic}
                </span>
              </div>
              <span className="text-[11px] text-text-muted">
                {new Date(data.kafkaTrigger.timestamp).toLocaleTimeString()}
              </span>
            </div>

            <p className="mt-2 text-xs font-medium text-text">
              Event: <strong className="text-blue-400 font-mono">{data.kafkaTrigger.eventType}</strong>
            </p>
            <p className="mt-1 text-xs leading-relaxed text-text-muted">
              {data.kafkaTrigger.payload}
            </p>
          </div>
        </li>

        {/* STAGE 2: RAG Chunks Retrieved */}
        <li className="relative pl-6">
          <span className="absolute -left-3.5 flex h-7 w-7 items-center justify-center rounded-full bg-purple-500/20 text-purple-400 ring-2 ring-purple-500/40 text-xs font-bold">
            2
          </span>
          <div className="rounded-xl border border-purple-500/30 bg-purple-500/5 p-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="rounded bg-purple-500/20 px-2 py-0.5 font-mono text-[11px] font-bold text-purple-400">
                  RAG RETRIEVAL
                </span>
                <span className="text-xs font-semibold text-text">
                  Retrieved Knowledge Chunks ({data.ragChunks.length})
                </span>
              </div>
              <span className="text-[11px] text-text-muted">Hybrid pgvector + BM25</span>
            </div>

            <div className="mt-3 space-y-2.5">
              {data.ragChunks.map((chunk, idx) => {
                const matchPct = Math.round(chunk.similarityScore * 100)
                return (
                  <div
                    key={chunk.sourceId || idx}
                    className="rounded-lg border border-border/80 bg-surface/80 p-3 text-xs"
                  >
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex items-center gap-2">
                        <span className="rounded bg-purple-500/15 px-1.5 py-0.5 font-mono text-[10px] font-semibold text-purple-300">
                          {chunk.source}
                        </span>
                        <span className="font-mono text-[11px] text-text-muted">{chunk.sourceId}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-[11px] font-bold text-emerald-400">
                          {matchPct}% similarity
                        </span>
                        <div className="h-1.5 w-12 overflow-hidden rounded-full bg-surface-raised">
                          <div
                            className="h-full bg-emerald-400"
                            style={{ width: `${matchPct}%` }}
                          />
                        </div>
                      </div>
                    </div>
                    <p className="mt-2 text-xs leading-relaxed text-text italic">
                      "{chunk.content}"
                    </p>
                  </div>
                )
              })}
            </div>
          </div>
        </li>

        {/* STAGE 3: MCP Tool Calls */}
        <li className="relative pl-6">
          <span className="absolute -left-3.5 flex h-7 w-7 items-center justify-center rounded-full bg-amber-500/20 text-amber-400 ring-2 ring-amber-500/40 text-xs font-bold">
            3
          </span>
          <div className="rounded-xl border border-amber-500/30 bg-amber-500/5 p-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="rounded bg-amber-500/20 px-2 py-0.5 font-mono text-[11px] font-bold text-amber-400">
                  MCP TOOL CALLS
                </span>
                <span className="text-xs font-semibold text-text">
                  Executed ReAct Actions ({data.toolCalls.length})
                </span>
              </div>
              <span className="text-[11px] text-text-muted">Model Context Protocol</span>
            </div>

            <div className="mt-3 space-y-3">
              {data.toolCalls.map((tool, idx) => {
                const isExpanded = expandedToolIndex === idx
                return (
                  <div
                    key={tool.stepIndex}
                    className="rounded-lg border border-border/80 bg-surface/80 p-3.5 text-xs transition"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <span className="flex h-5 w-5 items-center justify-center rounded-full bg-amber-500/20 text-[10px] font-bold text-amber-400 font-mono">
                          {tool.stepIndex + 1}
                        </span>
                        <span className="font-mono text-xs font-bold text-accent">
                          {tool.toolName}
                        </span>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="rounded bg-surface-raised px-2 py-0.5 font-mono text-[10px] text-text-muted">
                          {tool.latencyMs} ms
                        </span>
                        <button
                          type="button"
                          onClick={() => setExpandedToolIndex(isExpanded ? null : idx)}
                          className="text-[11px] text-accent hover:underline"
                        >
                          {isExpanded ? 'Hide Payload' : 'View Payload'}
                        </button>
                      </div>
                    </div>

                    {tool.llmReasoning && (
                      <div className="mt-2 rounded border-l-2 border-amber-400 bg-bg/60 p-2 text-xs italic text-text-muted">
                        "{tool.llmReasoning}"
                      </div>
                    )}

                    {isExpanded && (
                      <div className="mt-3 grid grid-cols-1 gap-2 sm:grid-cols-2 text-[11px] border-t border-border/60 pt-2">
                        <div>
                          <span className="font-semibold text-text-muted uppercase text-[9px]">Input</span>
                          <pre className="mt-1 overflow-x-auto rounded bg-bg p-2 font-mono text-[10px] text-text">
                            {typeof tool.input === 'string' ? tool.input : JSON.stringify(tool.input, null, 2)}
                          </pre>
                        </div>
                        <div>
                          <span className="font-semibold text-text-muted uppercase text-[9px]">Output</span>
                          <pre className="mt-1 overflow-x-auto rounded bg-bg p-2 font-mono text-[10px] text-text">
                            {typeof tool.output === 'string' ? tool.output : JSON.stringify(tool.output, null, 2)}
                          </pre>
                        </div>
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          </div>
        </li>

        {/* STAGE 4: LLM Reasoning */}
        <li className="relative pl-6">
          <span className="absolute -left-3.5 flex h-7 w-7 items-center justify-center rounded-full bg-emerald-500/20 text-emerald-400 ring-2 ring-emerald-500/40 text-xs font-bold">
            4
          </span>
          <div className="rounded-xl border border-emerald-500/30 bg-emerald-500/5 p-4">
            <div className="flex items-center gap-2">
              <span className="rounded bg-emerald-500/20 px-2 py-0.5 font-mono text-[11px] font-bold text-emerald-400">
                LLM REASONING
              </span>
              <span className="text-xs font-semibold text-text">Synthesis &amp; Cognitive Chain</span>
            </div>
            <div className="mt-2.5 rounded-lg border border-border/80 bg-surface/80 p-3 text-xs leading-relaxed text-text">
              <p>{data.llmReasoning}</p>
            </div>
          </div>
        </li>

        {/* STAGE 5: Final Decision & Affected Entities */}
        <li className="relative pl-6">
          <span className="absolute -left-3.5 flex h-7 w-7 items-center justify-center rounded-full bg-accent/20 text-accent ring-2 ring-accent/40 text-xs font-bold">
            5
          </span>
          <div className="rounded-xl border border-accent/40 bg-accent/5 p-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <span className="rounded bg-accent/20 px-2 py-0.5 font-mono text-[11px] font-bold text-accent">
                  FINAL DECISION
                </span>
                <Badge tone={isHighConfidence ? 'success' : 'warning'}>
                  Outcome: {data.finalDecision.outcome}
                </Badge>
              </div>
              <div className="flex items-center gap-2">
                <span className="text-xs font-semibold text-text">Confidence:</span>
                <span
                  className={`font-mono text-sm font-extrabold ${
                    isHighConfidence ? 'text-emerald-400' : 'text-amber-400'
                  }`}
                >
                  {confidencePct}%
                </span>
              </div>
            </div>

            <p className="mt-2 text-xs text-text-muted">
              {isHighConfidence
                ? 'Autonomous threshold (≥ 70%) satisfied — decision committed and propagated automatically.'
                : 'Confidence (< 70%) below autonomous threshold — routed to human review queue.'}
            </p>

            <div className="mt-4 border-t border-border/70 pt-3">
              <span className="text-[10px] font-bold uppercase tracking-wider text-text-muted">
                Affected Entities
              </span>
              <div className="mt-2 grid grid-cols-2 gap-2 sm:grid-cols-4">
                {Object.entries(data.finalDecision.affectedEntities).map(([key, val]) => (
                  <div key={key} className="rounded bg-surface p-2 text-xs">
                    <span className="text-[10px] text-text-muted font-mono">{key}</span>
                    <div className="truncate font-mono font-bold text-text mt-0.5">
                      {Array.isArray(val) ? val.join(', ') : String(val ?? '-')}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </li>
      </ol>
    </div>
  )
}
