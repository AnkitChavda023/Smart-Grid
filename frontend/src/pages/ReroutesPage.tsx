import { useState, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import * as analyticsApi from '../api/analytics'
import * as agentsApi from '../api/agents'
import { Card } from '../components/ui/Card'
import { KpiCard } from '../components/ui/KpiCard'
import { PaginationBar } from '../components/ui/PaginationBar'
import { LoadingState, ErrorState, EmptyState } from '../components/ui/StateViews'
import { useAuth } from '../auth/AuthContext'
import { AgentDecisionTimeline } from '../components/ui/AgentDecisionTimeline'
import type { Reroute } from '../types'

export function ReroutesPage() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const [successMsg, setSuccessMsg] = useState<string | null>(null)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  // Filter & Pagination state
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(5)
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ESCALATED' | 'APPROVED' | 'REJECTED' | 'MODIFIED' | 'PUBLISHED'>('ALL')

  // Modals state
  const [selectedRerouteForTrace, setSelectedRerouteForTrace] = useState<Reroute | null>(null)
  const [selectedRerouteForApprove, setSelectedRerouteForApprove] = useState<Reroute | null>(null)
  const [selectedRerouteForModify, setSelectedRerouteForModify] = useState<Reroute | null>(null)
  const [selectedRerouteForReject, setSelectedRerouteForReject] = useState<Reroute | null>(null)

  // Form fields
  const [approvalVendorId, setApprovalVendorId] = useState('')
  const [approvalQuoteId, setApprovalQuoteId] = useState('')
  const [rejectionReason, setRejectionReason] = useState('')

  const isPlannerOrAdmin = user?.role === 'PLANNER' || user?.role === 'ADMIN'

  // KPIs
  const { data: rerouteKpi } = useQuery({
    queryKey: ['analytics', 'reroute-rate'],
    queryFn: analyticsApi.reroutesSuccessRate,
    refetchInterval: 15_000,
  })

  const { data: reroutes, isLoading: loadingReroutes, isError: errReroutes, refetch: refetchReroutes } = useQuery({
    queryKey: ['reroutes', 'all'],
    queryFn: agentsApi.listAllReroutes,
    refetchInterval: 10_000,
  })

  const { data: timelineData, isLoading: loadingTrace } = useQuery({
    queryKey: ['reroutes', 'timeline', selectedRerouteForTrace?.id],
    queryFn: () => agentsApi.rerouteTimeline(selectedRerouteForTrace!.id),
    enabled: !!selectedRerouteForTrace,
  })

  // Approve Mutation
  const approveMutation = useMutation({
    mutationFn: ({ rerouteId, vendorId, quoteId }: { rerouteId: string; vendorId?: string; quoteId?: string }) =>
      agentsApi.approveReroute(rerouteId, vendorId, quoteId),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['reroutes'] })
      queryClient.invalidateQueries({ queryKey: ['analytics', 'reroute-rate'] })
      setSuccessMsg(`Reroute ${updated.id.slice(0, 8)} approved! Assigned vendor: ${updated.selectedVendorId ?? 'confirmed'}`)
      setSelectedRerouteForApprove(null)
      setTimeout(() => setSuccessMsg(null), 5000)
    },
    onError: (err: any) => {
      setErrorMsg(err?.response?.data?.message || err.message || 'Failed to approve reroute.')
      setTimeout(() => setErrorMsg(null), 6000)
    },
  })

  // Modify Mutation
  const modifyMutation = useMutation({
    mutationFn: ({ rerouteId, vendorId, quoteId }: { rerouteId: string; vendorId: string; quoteId: string }) =>
      agentsApi.modifyReroute(rerouteId, vendorId, quoteId),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['reroutes'] })
      queryClient.invalidateQueries({ queryKey: ['analytics', 'reroute-rate'] })
      setSuccessMsg(`Reroute ${updated.id.slice(0, 8)} modified and approved with vendor ${updated.selectedVendorId}!`)
      setSelectedRerouteForModify(null)
      setTimeout(() => setSuccessMsg(null), 5000)
    },
    onError: (err: any) => {
      setErrorMsg(err?.response?.data?.message || err.message || 'Failed to modify reroute.')
      setTimeout(() => setErrorMsg(null), 6000)
    },
  })

  // Reject Mutation
  const rejectMutation = useMutation({
    mutationFn: ({ rerouteId, reason }: { rerouteId: string; reason?: string }) =>
      agentsApi.rejectReroute(rerouteId, reason),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['reroutes'] })
      queryClient.invalidateQueries({ queryKey: ['analytics', 'reroute-rate'] })
      setSuccessMsg(`Reroute ${updated.id.slice(0, 8)} rejected by human reviewer.`)
      setSelectedRerouteForReject(null)
      setTimeout(() => setSuccessMsg(null), 5000)
    },
    onError: (err: any) => {
      setErrorMsg(err?.response?.data?.message || err.message || 'Failed to reject reroute.')
      setTimeout(() => setErrorMsg(null), 6000)
    },
  })

  function handleOpenApprove(r: Reroute) {
    setSelectedRerouteForApprove(r)
    setApprovalVendorId(r.selectedVendorId || 'alternate-vendor-1')
    setApprovalQuoteId(r.quoteId || `quote-${Math.random().toString(36).substring(2, 9)}`)
  }

  function handleOpenModify(r: Reroute) {
    setSelectedRerouteForModify(r)
    setApprovalVendorId(r.selectedVendorId || 'alternate-vendor-1')
    setApprovalQuoteId(r.quoteId || `quote-${Math.random().toString(36).substring(2, 9)}`)
  }

  function handleOpenReject(r: Reroute) {
    setSelectedRerouteForReject(r)
    setRejectionReason('Alternative supplier selected offline by procurement team')
  }

  const allReroutes = reroutes || []
  const stats = reroutes
    ? {
        total: allReroutes.length,
        autonomous: allReroutes.filter((r) => r.status === 'PUBLISHED').length,
        escalated: allReroutes.filter((r) => r.status === 'ESCALATED').length,
        approved: allReroutes.filter((r) => r.status === 'APPROVED').length,
        rejected: allReroutes.filter((r) => r.status === 'REJECTED').length,
      }
    : null

  const filteredReroutes = useMemo(() => {
    return allReroutes.filter((r) => {
      const q = searchTerm.trim().toLowerCase()
      const matchesSearch =
        q === '' ||
        r.orderId.toLowerCase().includes(q) ||
        (r.selectedVendorId && r.selectedVendorId.toLowerCase().includes(q)) ||
        (r.disruptionId && r.disruptionId.toLowerCase().includes(q))

      if (!matchesSearch) return false

      if (statusFilter === 'ALL') return true
      return r.status === statusFilter
    })
  }, [allReroutes, searchTerm, statusFilter])

  const totalFiltered = filteredReroutes.length
  const totalPages = Math.max(1, Math.ceil(totalFiltered / pageSize))
  const currentPage = Math.min(page, totalPages)
  const startIndex = totalFiltered === 0 ? 0 : (currentPage - 1) * pageSize
  const endIndex = Math.min(startIndex + pageSize, totalFiltered)

  const paginatedReroutes = useMemo(() => {
    return filteredReroutes.slice(startIndex, endIndex)
  }, [filteredReroutes, startIndex, endIndex])

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-text">Autonomous Reroutes &amp; Human Review Queue</h1>
          <p className="mt-1 text-sm text-text-muted">
            {isPlannerOrAdmin
              ? 'Human-in-the-Loop review queue: approve, modify, or reject autonomous agent reroute decisions.'
              : 'Autonomous reroute decisions and fulfillment intervention history.'}
          </p>
        </div>
        <button
          type="button"
          onClick={() => refetchReroutes()}
          className="rounded-lg border border-border bg-surface px-3 py-1.5 text-xs font-medium text-text hover:bg-border/40"
        >
          Refresh
        </button>
      </div>

      {successMsg && (
        <div className="rounded-lg border border-emerald-500/30 bg-emerald-500/10 p-3 text-sm text-emerald-400">
          ✓ {successMsg}
        </div>
      )}

      {errorMsg && (
        <div className="rounded-lg border border-rose-500/30 bg-rose-500/10 p-3 text-sm text-rose-400">
          ✕ {errorMsg}
        </div>
      )}

      {stats && (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-5">
          <KpiCard label="Total Decisions" value={stats.total} />
          <KpiCard label="Auto-Committed (≥70%)" value={stats.autonomous} tone="default" />
          <KpiCard label="Escalated (<70%)" value={stats.escalated} tone={stats.escalated > 0 ? 'warning' : 'default'} />
          <KpiCard label="Human Approved" value={stats.approved} tone="success" />
          <KpiCard label="Human Rejected" value={stats.rejected} tone={stats.rejected > 0 ? 'danger' : 'default'} />
        </div>
      )}

      <Card className="p-5">
        <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
          <div>
            <h2 className="text-sm font-semibold text-text">Reroute Decision Queue</h2>
            {rerouteKpi && (
              <span className="text-xs text-text-muted">
                Live fulfillment success rate:{' '}
                <strong className="text-emerald-400">{(rerouteKpi.successRate * 100).toFixed(0)}%</strong>
              </span>
            )}
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <input
              type="text"
              placeholder="Search order, vendor, disruption…"
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value)
                setPage(1)
              }}
              className="w-52 rounded-lg border border-border bg-bg px-3 py-1.5 text-xs text-text outline-none focus:border-accent"
            />
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value as any)
                setPage(1)
              }}
              className="rounded-lg border border-border bg-bg px-2.5 py-1.5 text-xs text-text outline-none focus:border-accent"
            >
              <option value="ALL">All Statuses ({allReroutes.length})</option>
              <option value="ESCALATED">Escalated (&lt;70%)</option>
              <option value="APPROVED">Human Approved</option>
              <option value="MODIFIED">Modified &amp; Approved</option>
              <option value="REJECTED">Human Rejected</option>
              <option value="PUBLISHED">Auto-Committed</option>
            </select>
          </div>
        </div>

        {loadingReroutes && <LoadingState label="Loading reroutes…" />}
        {errReroutes && <ErrorState message="Could not load reroutes queue." />}

        {!loadingReroutes && !errReroutes && filteredReroutes.length === 0 && (
          <EmptyState
            title="No matching reroutes found"
            description="Try adjusting your search keywords or status filter."
          />
        )}

        {!loadingReroutes && paginatedReroutes.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-border text-text-muted">
                  <th className="pb-3 font-medium">Order ID</th>
                  <th className="pb-3 font-medium">Disruption</th>
                  <th className="pb-3 font-medium">Selected Vendor</th>
                  <th className="pb-3 font-medium">Confidence</th>
                  <th className="pb-3 font-medium">Status</th>
                  <th className="pb-3 font-medium">Timestamp</th>
                  <th className="pb-3 text-right font-medium">Human Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/60">
                {paginatedReroutes.map((r) => {
                  const isApproved = r.status === 'APPROVED' || r.status === 'MODIFIED'
                  const isRejected = r.status === 'REJECTED'
                  const isPendingReview = r.status === 'ESCALATED'
                  return (
                    <tr key={r.id} className="hover:bg-surface-raised/40 transition">
                      <td className="py-3 font-mono font-semibold text-text">{r.orderId.slice(0, 8)}</td>
                      <td className="py-3 font-mono text-text-muted">
                        {r.disruptionId ? r.disruptionId.slice(0, 8) : 'Manual/Sim'}
                      </td>
                      <td className="py-3 font-mono text-text">
                        {r.selectedVendorId ? r.selectedVendorId.slice(0, 12) : 'Awaiting Selection'}
                      </td>
                      <td className="py-3">
                        <span
                          className={`font-semibold ${
                            r.confidence >= 0.7 ? 'text-emerald-400' : 'text-amber-400'
                          }`}
                        >
                          {(r.confidence * 100).toFixed(0)}%
                        </span>
                      </td>
                      <td className="py-3">
                        <span
                          className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${
                            isApproved
                              ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30'
                              : isRejected
                                ? 'bg-rose-500/15 text-rose-400 border border-rose-500/30'
                                : isPendingReview
                                  ? 'bg-amber-500/15 text-amber-400 border border-amber-500/30'
                                  : 'bg-blue-500/15 text-blue-400 border border-blue-500/30'
                          }`}
                        >
                          {r.status}
                        </span>
                      </td>
                      <td className="py-3 text-text-muted">{new Date(r.createdAt).toLocaleTimeString()}</td>
                      <td className="py-3 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            type="button"
                            onClick={() => setSelectedRerouteForTrace(r)}
                            className="rounded border border-border bg-surface px-2 py-1 text-[11px] font-medium text-text transition hover:border-accent hover:text-accent shadow-sm"
                          >
                            Trace
                          </button>
                          {isPlannerOrAdmin && !isApproved && !isRejected && (
                            <>
                              <button
                                type="button"
                                onClick={() => handleOpenApprove(r)}
                                className="rounded bg-accent px-2 py-1 text-[11px] font-semibold text-accent-fg shadow-sm hover:opacity-90 active:scale-95"
                              >
                                Approve
                              </button>
                              <button
                                type="button"
                                onClick={() => handleOpenModify(r)}
                                className="rounded border border-border bg-surface px-2 py-1 text-[11px] font-semibold text-text hover:border-accent hover:text-accent shadow-sm active:scale-95"
                              >
                                Modify
                              </button>
                              <button
                                type="button"
                                onClick={() => handleOpenReject(r)}
                                className="rounded border border-rose-500/30 bg-rose-500/10 px-2 py-1 text-[11px] font-semibold text-rose-400 hover:bg-rose-500/20 active:scale-95"
                              >
                                Reject
                              </button>
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>

            {/* Pagination Controls */}
            <PaginationBar
              currentPage={currentPage}
              totalPages={totalPages}
              totalItems={totalFiltered}
              pageSize={pageSize}
              onPageChange={setPage}
              onPageSizeChange={setPageSize}
              itemLabel="reroutes"
            />
          </div>
        )}
      </Card>

      {/* Reroute Agent Decision Timeline Modal */}
      {selectedRerouteForTrace && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/65 p-4 backdrop-blur-sm animate-[fadeIn_150ms_ease-out]">
          <Card className="flex max-h-[85vh] w-full max-w-3xl flex-col overflow-hidden p-6 shadow-2xl">
            <div className="flex-1 overflow-y-auto py-2 pr-1">
              {loadingTrace ? (
                <LoadingState label="Loading comprehensive agent execution timeline…" />
              ) : timelineData ? (
                <AgentDecisionTimeline
                  data={timelineData}
                  onClose={() => setSelectedRerouteForTrace(null)}
                />
              ) : (
                <div className="flex flex-col items-center justify-center gap-2 rounded-lg border border-border/60 bg-surface/40 p-8 text-center">
                  <span className="text-2xl">⚡</span>
                  <h3 className="text-sm font-semibold text-text">Human-in-the-Loop Review Triggered</h3>
                  <p className="max-w-md text-xs leading-relaxed text-text-muted">
                    This order's disruption confidence ({(selectedRerouteForTrace.confidence * 100).toFixed(0)}%) fell
                    below the autonomous threshold (70%).
                  </p>
                </div>
              )}
            </div>

            <div className="flex justify-end border-t border-border pt-3 mt-4">
              <button
                type="button"
                onClick={() => setSelectedRerouteForTrace(null)}
                className="rounded-lg bg-surface border border-border px-4 py-1.5 text-xs font-medium text-text hover:bg-border/40"
              >
                Close Trace
              </button>
            </div>
          </Card>
        </div>
      )}

      {/* Reroute Approval Modal */}
      {selectedRerouteForApprove && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/65 p-4 backdrop-blur-sm animate-[fadeIn_150ms_ease-out]">
          <Card className="w-full max-w-md p-6 shadow-2xl">
            <h2 className="text-base font-semibold text-text">Approve Supply Chain Reroute</h2>
            <p className="mt-1 text-xs text-text-muted">
              Confirm the replacement vendor and quote to fulfill order{' '}
              <strong className="text-text font-mono">{selectedRerouteForApprove.orderId.slice(0, 8)}</strong>.
            </p>

            <form
              onSubmit={(e) => {
                e.preventDefault()
                approveMutation.mutate({
                  rerouteId: selectedRerouteForApprove.id,
                  vendorId: approvalVendorId,
                  quoteId: approvalQuoteId,
                })
              }}
              className="mt-4 flex flex-col gap-3"
            >
              <label className="flex flex-col gap-1 text-xs">
                <span className="font-medium text-text">Assigned Vendor ID</span>
                <input
                  type="text"
                  value={approvalVendorId}
                  onChange={(e) => setApprovalVendorId(e.target.value)}
                  placeholder="e.g. alternate-vendor-1"
                  required
                  className="rounded-lg border border-border bg-bg px-3 py-2 text-sm text-text outline-none focus:border-accent font-mono"
                />
              </label>

              <label className="flex flex-col gap-1 text-xs">
                <span className="font-medium text-text">Quote ID</span>
                <input
                  type="text"
                  value={approvalQuoteId}
                  onChange={(e) => setApprovalQuoteId(e.target.value)}
                  placeholder="e.g. quote-12345"
                  required
                  className="rounded-lg border border-border bg-bg px-3 py-2 text-sm text-text outline-none focus:border-accent font-mono"
                />
              </label>

              <div className="mt-4 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setSelectedRerouteForApprove(null)}
                  className="rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-text-muted hover:text-text"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={approveMutation.isPending}
                  className="rounded-lg bg-accent px-4 py-1.5 text-xs font-semibold text-accent-fg hover:opacity-90 disabled:opacity-50"
                >
                  {approveMutation.isPending ? 'Approving…' : 'Confirm & Approve'}
                </button>
              </div>
            </form>
          </Card>
        </div>
      )}

      {/* Reroute Modify Modal */}
      {selectedRerouteForModify && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/65 p-4 backdrop-blur-sm animate-[fadeIn_150ms_ease-out]">
          <Card className="w-full max-w-md p-6 shadow-2xl">
            <h2 className="text-base font-semibold text-text">Modify &amp; Override Reroute Decision</h2>
            <p className="mt-1 text-xs text-text-muted">
              Override agent recommendation for order{' '}
              <strong className="text-text font-mono">{selectedRerouteForModify.orderId.slice(0, 8)}</strong> with human-chosen vendor/quote.
            </p>

            <form
              onSubmit={(e) => {
                e.preventDefault()
                modifyMutation.mutate({
                  rerouteId: selectedRerouteForModify.id,
                  vendorId: approvalVendorId,
                  quoteId: approvalQuoteId,
                })
              }}
              className="mt-4 flex flex-col gap-3"
            >
              <label className="flex flex-col gap-1 text-xs">
                <span className="font-medium text-text">Override Vendor ID</span>
                <input
                  type="text"
                  value={approvalVendorId}
                  onChange={(e) => setApprovalVendorId(e.target.value)}
                  placeholder="e.g. preferred-vendor-custom"
                  required
                  className="rounded-lg border border-border bg-bg px-3 py-2 text-sm text-text outline-none focus:border-accent font-mono"
                />
              </label>

              <label className="flex flex-col gap-1 text-xs">
                <span className="font-medium text-text">Override Quote ID</span>
                <input
                  type="text"
                  value={approvalQuoteId}
                  onChange={(e) => setApprovalQuoteId(e.target.value)}
                  placeholder="e.g. quote-custom-negotiated"
                  required
                  className="rounded-lg border border-border bg-bg px-3 py-2 text-sm text-text outline-none focus:border-accent font-mono"
                />
              </label>

              <div className="mt-4 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setSelectedRerouteForModify(null)}
                  className="rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-text-muted hover:text-text"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={modifyMutation.isPending}
                  className="rounded-lg bg-accent px-4 py-1.5 text-xs font-semibold text-accent-fg hover:opacity-90 disabled:opacity-50"
                >
                  {modifyMutation.isPending ? 'Saving…' : 'Save & Approve'}
                </button>
              </div>
            </form>
          </Card>
        </div>
      )}

      {/* Reroute Reject Modal */}
      {selectedRerouteForReject && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/65 p-4 backdrop-blur-sm animate-[fadeIn_150ms_ease-out]">
          <Card className="w-full max-w-md p-6 shadow-2xl">
            <h2 className="text-base font-semibold text-rose-400">Reject Agent Reroute Decision</h2>
            <p className="mt-1 text-xs text-text-muted">
              Rejecting will mark this proposal as rejected and keep order{' '}
              <strong className="text-text font-mono">{selectedRerouteForReject.orderId.slice(0, 8)}</strong> in its current assignment.
            </p>

            <form
              onSubmit={(e) => {
                e.preventDefault()
                rejectMutation.mutate({
                  rerouteId: selectedRerouteForReject.id,
                  reason: rejectionReason,
                })
              }}
              className="mt-4 flex flex-col gap-3"
            >
              <label className="flex flex-col gap-1 text-xs">
                <span className="font-medium text-text">Rejection Reason / Procurement Note</span>
                <textarea
                  rows={3}
                  value={rejectionReason}
                  onChange={(e) => setRejectionReason(e.target.value)}
                  required
                  className="rounded-lg border border-border bg-bg px-3 py-2 text-xs text-text outline-none focus:border-rose-400"
                />
              </label>

              <div className="mt-4 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setSelectedRerouteForReject(null)}
                  className="rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-text-muted hover:text-text"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={rejectMutation.isPending}
                  className="rounded-lg bg-rose-600 px-4 py-1.5 text-xs font-semibold text-white hover:bg-rose-500 disabled:opacity-50"
                >
                  {rejectMutation.isPending ? 'Rejecting…' : 'Confirm Rejection'}
                </button>
              </div>
            </form>
          </Card>
        </div>
      )}
    </div>
  )
}
