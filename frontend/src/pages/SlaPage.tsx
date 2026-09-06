import { useState, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import * as contractsApi from '../api/contracts'
import { Card } from '../components/ui/Card'
import { Badge } from '../components/ui/Badge'
import { InfoTooltip } from '../components/ui/InfoTooltip'
import { PaginationBar } from '../components/ui/PaginationBar'
import { LoadingState, ErrorState, EmptyState } from '../components/ui/StateViews'
import { useAuth } from '../auth/AuthContext'
import type { ContractDraftResponse } from '../api/contracts'

export function SlaPage() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const [successMsg, setSuccessMsg] = useState<string | null>(null)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  // Modifying Draft state
  const [editingDraft, setEditingDraft] = useState<ContractDraftResponse | null>(null)
  const [draftProposedTerms, setDraftProposedTerms] = useState('')
  const [draftSummary, setDraftSummary] = useState('')

  // Drafts Filter & Pagination state
  const [draftPage, setDraftPage] = useState(1)
  const [draftPageSize, setDraftPageSize] = useState(5)
  const [draftStatusFilter, setDraftStatusFilter] = useState<'ALL' | 'DRAFT' | 'SUBMITTED' | 'REJECTED'>('ALL')

  // Contracts Filter & Pagination state
  const [contractPage, setContractPage] = useState(1)
  const [contractPageSize, setContractPageSize] = useState(4)
  const [contractSearch, setContractSearch] = useState('')
  const [contractStatusFilter, setContractStatusFilter] = useState<'ALL' | 'ACTIVE' | 'EXPIRED'>('ALL')

  // Breaches Filter & Pagination state
  const [breachPage, setBreachPage] = useState(1)
  const [breachPageSize, setBreachPageSize] = useState(5)
  const [breachSearch, setBreachSearch] = useState('')
  const [breachStatusFilter, setBreachStatusFilter] = useState<'ALL' | 'RECORDED' | 'RESOLVED' | 'EXCUSED'>('ALL')

  const isPlannerOrAdmin = user?.role === 'PLANNER' || user?.role === 'ADMIN'
  const isSupplier = user?.role === 'SUPPLIER'

  const { data: contracts, isLoading: loadingContracts, isError: errContracts, refetch: refetchContracts } = useQuery({
    queryKey: ['contracts'],
    queryFn: contractsApi.listContracts,
    refetchInterval: 15_000,
  })

  const { data: breaches, isLoading: loadingBreaches, refetch: refetchBreaches } = useQuery({
    queryKey: ['sla-breaches'],
    queryFn: () => contractsApi.getSlaBreaches(),
    refetchInterval: 15_000,
  })

  const { data: drafts, isLoading: loadingDrafts, refetch: refetchDrafts } = useQuery({
    queryKey: ['contract-drafts'],
    queryFn: contractsApi.listContractDrafts,
    refetchInterval: 10_000,
  })

  // Submit Draft Mutation (Turns draft into active binding contract)
  const submitDraftMutation = useMutation({
    mutationFn: (id: string) => contractsApi.submitContractDraft(id),
    onSuccess: (contract) => {
      queryClient.invalidateQueries({ queryKey: ['contracts'] })
      queryClient.invalidateQueries({ queryKey: ['contract-drafts'] })
      setSuccessMsg(`Draft submitted and activated as new Contract ${contract.id.slice(0, 8)}!`)
      setTimeout(() => setSuccessMsg(null), 5000)
    },
    onError: (err: any) => {
      setErrorMsg(err?.response?.data?.message || err.message || 'Failed to submit contract draft.')
      setTimeout(() => setErrorMsg(null), 6000)
    },
  })

  // Reject Draft Mutation
  const rejectDraftMutation = useMutation({
    mutationFn: (id: string) => contractsApi.rejectContractDraft(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['contract-drafts'] })
      setSuccessMsg('Contract draft rejected by human procurement reviewer.')
      setTimeout(() => setSuccessMsg(null), 5000)
    },
    onError: (err: any) => {
      setErrorMsg(err?.response?.data?.message || err.message || 'Failed to reject draft.')
      setTimeout(() => setErrorMsg(null), 6000)
    },
  })

  // Modify Draft Mutation
  const modifyDraftMutation = useMutation({
    mutationFn: ({ id, terms, summary }: { id: string; terms: string; summary: string }) =>
      contractsApi.modifyContractDraft(id, terms, summary),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['contract-drafts'] })
      setEditingDraft(null)
      setSuccessMsg('Contract draft terms modified successfully by human reviewer.')
      setTimeout(() => setSuccessMsg(null), 5000)
    },
    onError: (err: any) => {
      setErrorMsg(err?.response?.data?.message || err.message || 'Failed to modify draft.')
      setTimeout(() => setErrorMsg(null), 6000)
    },
  })

  function handleOpenEditDraft(d: ContractDraftResponse) {
    setEditingDraft(d)
    setDraftProposedTerms(d.proposedTerms)
    setDraftSummary(d.summary)
  }

  // 1. Filtered & Paginated Drafts
  const allDrafts = drafts || []
  const filteredDrafts = useMemo(() => {
    return allDrafts.filter((d) => {
      if (draftStatusFilter === 'ALL') return true
      return d.status === draftStatusFilter
    })
  }, [allDrafts, draftStatusFilter])

  const totalDraftPages = Math.max(1, Math.ceil(filteredDrafts.length / draftPageSize))
  const currentDraftPage = Math.min(draftPage, totalDraftPages)
  const draftStartIndex = filteredDrafts.length === 0 ? 0 : (currentDraftPage - 1) * draftPageSize
  const paginatedDrafts = useMemo(() => {
    return filteredDrafts.slice(draftStartIndex, draftStartIndex + draftPageSize)
  }, [filteredDrafts, draftStartIndex, draftPageSize])

  // 2. Filtered & Paginated Contracts
  const allContracts = contracts || []
  const filteredContracts = useMemo(() => {
    return allContracts.filter((c) => {
      const q = contractSearch.trim().toLowerCase()
      const matchesSearch =
        q === '' ||
        c.id.toLowerCase().includes(q) ||
        c.vendorId.toLowerCase().includes(q) ||
        c.terms.toLowerCase().includes(q)

      if (!matchesSearch) return false

      if (contractStatusFilter === 'ALL') return true
      if (contractStatusFilter === 'ACTIVE') return c.active
      if (contractStatusFilter === 'EXPIRED') return !c.active
      return true
    })
  }, [allContracts, contractSearch, contractStatusFilter])

  const totalContractPages = Math.max(1, Math.ceil(filteredContracts.length / contractPageSize))
  const currentContractPage = Math.min(contractPage, totalContractPages)
  const contractStartIndex = filteredContracts.length === 0 ? 0 : (currentContractPage - 1) * contractPageSize
  const paginatedContracts = useMemo(() => {
    return filteredContracts.slice(contractStartIndex, contractStartIndex + contractPageSize)
  }, [filteredContracts, contractStartIndex, contractPageSize])

  // 3. Filtered & Paginated Breaches
  const allBreaches = breaches || []
  const filteredBreaches = useMemo(() => {
    return allBreaches.filter((b) => {
      const q = breachSearch.trim().toLowerCase()
      const matchesSearch =
        q === '' ||
        b.metricName.toLowerCase().includes(q) ||
        b.vendorId.toLowerCase().includes(q) ||
        b.contractId.toLowerCase().includes(q)

      if (!matchesSearch) return false

      if (breachStatusFilter === 'ALL') return true
      return b.status === breachStatusFilter
    })
  }, [allBreaches, breachSearch, breachStatusFilter])

  const totalBreachPages = Math.max(1, Math.ceil(filteredBreaches.length / breachPageSize))
  const currentBreachPage = Math.min(breachPage, totalBreachPages)
  const breachStartIndex = filteredBreaches.length === 0 ? 0 : (currentBreachPage - 1) * breachPageSize
  const paginatedBreaches = useMemo(() => {
    return filteredBreaches.slice(breachStartIndex, breachStartIndex + breachPageSize)
  }, [filteredBreaches, breachStartIndex, breachPageSize])

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="flex items-center gap-2 text-2xl font-semibold text-text">
            SLA &amp; Master Contract Management
            <InfoTooltip label="Human-in-the-Loop Contract Governance">
              The Contract Negotiation Agent can only draft proposed terms — it can never auto-submit. Only PLANNER and
              ADMIN users have authorization to submit, modify, or reject contract proposals.
            </InfoTooltip>
          </h1>
          <p className="mt-1 text-sm text-text-muted">
            {isSupplier
              ? 'Review your active master service agreements, SLA performance thresholds, and breach history.'
              : 'Enterprise vendor contracts, SLA commitments, penalty rules, and human review of agent-drafted renegotiations.'}
          </p>
        </div>
        <button
          type="button"
          onClick={() => {
            refetchContracts()
            refetchBreaches()
            refetchDrafts()
          }}
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

      {/* Human-in-the-Loop: Contract Negotiation Drafts Review Queue */}
      <Card className="p-5 border-t-2 border-accent">
        <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
          <div>
            <div className="flex items-center gap-2">
              <span className="flex h-2.5 w-2.5 rounded-full bg-accent animate-pulse" />
              <h2 className="text-base font-semibold text-text">
                Contract Negotiation Drafts (Human Review Queue)
              </h2>
            </div>
            <p className="text-xs text-text-muted mt-0.5">
              Drafts prepared autonomously by the Contract Negotiation Agent. Review, modify clauses, or submit to activate.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <select
              value={draftStatusFilter}
              onChange={(e) => {
                setDraftStatusFilter(e.target.value as any)
                setDraftPage(1)
              }}
              className="rounded-lg border border-border bg-bg px-2.5 py-1 text-xs text-text outline-none focus:border-accent"
            >
              <option value="ALL">All Statuses ({allDrafts.length})</option>
              <option value="DRAFT">Pending Drafts</option>
              <option value="SUBMITTED">Submitted</option>
              <option value="REJECTED">Rejected</option>
            </select>
            <span className="rounded bg-accent/15 px-2.5 py-0.5 text-xs font-bold text-accent">
              {drafts?.filter((d) => d.status === 'DRAFT').length ?? 0} Pending
            </span>
          </div>
        </div>

        {loadingDrafts && <LoadingState label="Loading contract negotiation drafts…" />}
        {!loadingDrafts && (
          <div className="overflow-x-auto">
            {filteredDrafts.length > 0 ? (
              <>
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="border-b border-border text-text-muted">
                      <th className="pb-3 font-medium">Draft ID</th>
                      <th className="pb-3 font-medium">Vendor</th>
                      <th className="pb-3 font-medium">Agent Summary</th>
                      <th className="pb-3 font-medium">Status</th>
                      <th className="pb-3 font-medium">Created</th>
                      <th className="pb-3 text-right font-medium">Human Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border/60">
                    {paginatedDrafts.map((d) => {
                      const isDraft = d.status === 'DRAFT'
                      return (
                        <tr key={d.id} className="hover:bg-surface-raised/40 transition">
                          <td className="py-3 font-mono font-bold text-text">{d.id.slice(0, 8)}</td>
                          <td className="py-3 font-mono font-semibold text-accent">{d.vendorId}</td>
                          <td className="py-3 max-w-xs text-text-muted truncate" title={d.summary}>
                            {d.summary}
                          </td>
                          <td className="py-3">
                            <Badge
                              tone={
                                d.status === 'SUBMITTED'
                                  ? 'success'
                                  : d.status === 'REJECTED'
                                    ? 'danger'
                                    : 'warning'
                              }
                            >
                              {d.status}
                            </Badge>
                          </td>
                          <td className="py-3 text-text-muted">
                            {new Date(d.createdAt).toLocaleTimeString()}
                          </td>
                          <td className="py-3 text-right">
                            <div className="flex items-center justify-end gap-1.5">
                              {isPlannerOrAdmin && isDraft ? (
                                <>
                                  <button
                                    type="button"
                                    onClick={() => handleOpenEditDraft(d)}
                                    className="rounded border border-border bg-surface px-2.5 py-1 text-[11px] font-semibold text-text hover:border-accent hover:text-accent shadow-sm"
                                  >
                                    Modify
                                  </button>
                                  <button
                                    type="button"
                                    onClick={() => submitDraftMutation.mutate(d.id)}
                                    disabled={submitDraftMutation.isPending}
                                    className="rounded bg-accent px-2.5 py-1 text-[11px] font-semibold text-accent-fg shadow-sm hover:opacity-90 disabled:opacity-50"
                                  >
                                    Submit &amp; Activate
                                  </button>
                                  <button
                                    type="button"
                                    onClick={() => rejectDraftMutation.mutate(d.id)}
                                    disabled={rejectDraftMutation.isPending}
                                    className="rounded border border-rose-500/30 bg-rose-500/10 px-2.5 py-1 text-[11px] font-semibold text-rose-400 hover:bg-rose-500/20 disabled:opacity-50"
                                  >
                                    Reject
                                  </button>
                                </>
                              ) : (
                                <span className="text-[11px] text-text-muted">
                                  {isDraft ? 'Planner/Admin review required' : 'Archived'}
                                </span>
                              )}
                            </div>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>

                {/* Drafts Pagination Bar */}
                <PaginationBar
                  currentPage={currentDraftPage}
                  totalPages={totalDraftPages}
                  totalItems={filteredDrafts.length}
                  pageSize={draftPageSize}
                  onPageChange={setDraftPage}
                  onPageSizeChange={setDraftPageSize}
                  itemLabel="negotiation drafts"
                />
              </>
            ) : (
              <EmptyState
                title="No negotiation drafts found"
                description="The Contract Negotiation Agent creates drafts when contracts near expiration or SLA breaches increase."
              />
            )}
          </div>
        )}
      </Card>

      {/* Active Master Contracts */}
      <div>
        <div className="flex flex-wrap items-center justify-between gap-3 mb-3">
          <h2 className="text-base font-semibold text-text">Active Master Service Agreements</h2>

          <div className="flex flex-wrap items-center gap-2">
            <input
              type="text"
              placeholder="Search contracts…"
              value={contractSearch}
              onChange={(e) => {
                setContractSearch(e.target.value)
                setContractPage(1)
              }}
              className="w-48 rounded-lg border border-border bg-bg px-3 py-1 text-xs text-text outline-none focus:border-accent"
            />
            <select
              value={contractStatusFilter}
              onChange={(e) => {
                setContractStatusFilter(e.target.value as any)
                setContractPage(1)
              }}
              className="rounded-lg border border-border bg-bg px-2.5 py-1 text-xs text-text outline-none focus:border-accent"
            >
              <option value="ALL">All Contracts ({allContracts.length})</option>
              <option value="ACTIVE">Active Only</option>
              <option value="EXPIRED">Expired Only</option>
            </select>
          </div>
        </div>

        {loadingContracts && <LoadingState label="Loading contracts &amp; SLA commitments…" />}
        {errContracts && <ErrorState message="Could not load contract status." />}

        {!loadingContracts && !errContracts && filteredContracts.length === 0 && (
          <EmptyState
            title="No matching contracts found"
            description="Try adjusting your contract search or status filter."
          />
        )}

        {!loadingContracts && paginatedContracts.length > 0 && (
          <div className="flex flex-col gap-4">
            <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
              {paginatedContracts.map((c) => (
                <Card key={c.id} className="flex flex-col justify-between gap-4 p-5">
                  <div>
                    <div className="flex items-center justify-between gap-2">
                      <span className="font-mono text-sm font-semibold text-text">Contract {c.id.slice(0, 8)}</span>
                      <span
                        className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${
                          c.active
                            ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30'
                            : 'bg-text-muted/15 text-text-muted border border-border'
                        }`}
                      >
                        {c.active ? 'ACTIVE' : 'EXPIRED'}
                      </span>
                    </div>

                    <p className="mt-2 text-xs text-text-muted">Vendor ID: <span className="font-mono text-text">{c.vendorId}</span></p>
                    <p className="mt-2 text-xs leading-relaxed text-text line-clamp-3">{c.terms}</p>

                    <div className="mt-4 border-t border-border pt-3">
                      <h3 className="text-xs font-semibold text-text-muted uppercase tracking-wider">SLA Thresholds</h3>
                      {c.slaTerms && c.slaTerms.length > 0 ? (
                        <div className="mt-2 flex flex-col gap-1.5">
                          {c.slaTerms.map((term, idx) => (
                            <div key={idx} className="flex items-center justify-between text-xs rounded bg-surface/60 px-2.5 py-1.5">
                              <span className="font-medium text-text">{term.metricName}</span>
                              <span className="text-text-muted">
                                Threshold: <strong className="text-text">{term.thresholdValue}</strong> | Penalty: <strong className="text-rose-400">${term.penaltyPerBreach}</strong>
                              </span>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <p className="mt-1 text-xs text-text-muted">Standard SLA terms applied.</p>
                      )}
                    </div>
                  </div>

                  <div className="flex items-center justify-between border-t border-border pt-3 text-[11px] text-text-muted">
                    <span>Start: {c.startDate}</span>
                    <span>End: {c.endDate}</span>
                  </div>
                </Card>
              ))}
            </div>

            {/* Contracts Pagination Bar */}
            <PaginationBar
              currentPage={currentContractPage}
              totalPages={totalContractPages}
              totalItems={filteredContracts.length}
              pageSize={contractPageSize}
              onPageChange={setContractPage}
              onPageSizeChange={setContractPageSize}
              pageSizeOptions={[4, 8, 12]}
              itemLabel="contracts"
            />
          </div>
        )}
      </div>

      {/* Breach History Section */}
      <Card className="p-5">
        <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
          <div>
            <h2 className="text-base font-semibold text-text">SLA Breach Assessment History</h2>
            <p className="mt-0.5 text-xs text-text-muted">
              Evaluated periodically via Poisson risk modeling and real-time delivery telemetry.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <input
              type="text"
              placeholder="Search metric or vendor…"
              value={breachSearch}
              onChange={(e) => {
                setBreachSearch(e.target.value)
                setBreachPage(1)
              }}
              className="w-48 rounded-lg border border-border bg-bg px-3 py-1 text-xs text-text outline-none focus:border-accent"
            />
            <select
              value={breachStatusFilter}
              onChange={(e) => {
                setBreachStatusFilter(e.target.value as any)
                setBreachPage(1)
              }}
              className="rounded-lg border border-border bg-bg px-2.5 py-1 text-xs text-text outline-none focus:border-accent"
            >
              <option value="ALL">All Breach Statuses ({allBreaches.length})</option>
              <option value="RECORDED">Recorded</option>
              <option value="RESOLVED">Resolved</option>
              <option value="EXCUSED">Excused</option>
            </select>
          </div>
        </div>

        {loadingBreaches ? (
          <div className="py-4 text-xs text-text-muted">Loading breach logs…</div>
        ) : filteredBreaches.length === 0 ? (
          <div className="mt-3 rounded-lg border border-emerald-500/20 bg-emerald-500/5 p-4 text-xs text-emerald-400">
            ✓ No matching SLA breaches found. All delivery metrics within acceptable thresholds!
          </div>
        ) : (
          <div className="mt-3 overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-border text-text-muted">
                  <th className="pb-2">Metric</th>
                  <th className="pb-2">Vendor ID</th>
                  <th className="pb-2">Actual</th>
                  <th className="pb-2">Threshold</th>
                  <th className="pb-2">Penalty</th>
                  <th className="pb-2">Status</th>
                  <th className="pb-2">Detected At</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/40">
                {paginatedBreaches.map((b) => (
                  <tr key={b.id}>
                    <td className="py-2.5 font-medium text-text">{b.metricName}</td>
                    <td className="py-2.5 font-mono text-text-muted">{b.vendorId}</td>
                    <td className="py-2.5 text-rose-400">{b.actualValue}</td>
                    <td className="py-2.5 text-text-muted">{b.thresholdValue}</td>
                    <td className="py-2.5 font-medium text-rose-400">${b.penaltyAmount}</td>
                    <td className="py-2.5">
                      <span className={`rounded px-2 py-0.5 text-[10px] font-semibold border ${
                        b.status === 'RESOLVED'
                          ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                          : b.status === 'EXCUSED'
                            ? 'bg-blue-500/10 text-blue-400 border-blue-500/20'
                            : 'bg-rose-500/10 text-rose-400 border-rose-500/20'
                      }`}>
                        {b.status}
                      </span>
                    </td>
                    <td className="py-2.5 text-text-muted">{new Date(b.detectedAt).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>

            {/* Breaches Pagination Bar */}
            <PaginationBar
              currentPage={currentBreachPage}
              totalPages={totalBreachPages}
              totalItems={filteredBreaches.length}
              pageSize={breachPageSize}
              onPageChange={setBreachPage}
              onPageSizeChange={setBreachPageSize}
              itemLabel="breaches"
            />
          </div>
        )}
      </Card>

      {/* Modify Draft Modal */}
      {editingDraft && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/65 p-4 backdrop-blur-sm animate-[fadeIn_150ms_ease-out]">
          <Card className="w-full max-w-lg p-6 shadow-2xl">
            <h2 className="text-base font-semibold text-text">Modify Contract Draft Terms</h2>
            <p className="mt-1 text-xs text-text-muted">
              Human reviewer override for Draft <strong className="font-mono text-text">{editingDraft.id.slice(0, 8)}</strong> (Vendor: {editingDraft.vendorId}).
            </p>

            <form
              onSubmit={(e) => {
                e.preventDefault()
                modifyDraftMutation.mutate({
                  id: editingDraft.id,
                  terms: draftProposedTerms,
                  summary: draftSummary,
                })
              }}
              className="mt-4 flex flex-col gap-3"
            >
              <label className="flex flex-col gap-1 text-xs">
                <span className="font-medium text-text">Proposed Contract Terms (Clauses)</span>
                <textarea
                  rows={5}
                  value={draftProposedTerms}
                  onChange={(e) => setDraftProposedTerms(e.target.value)}
                  required
                  className="rounded-lg border border-border bg-bg px-3 py-2 text-xs text-text outline-none focus:border-accent font-mono"
                />
              </label>

              <label className="flex flex-col gap-1 text-xs">
                <span className="font-medium text-text">Human Reviewer Summary / Note</span>
                <input
                  type="text"
                  value={draftSummary}
                  onChange={(e) => setDraftSummary(e.target.value)}
                  required
                  className="rounded-lg border border-border bg-bg px-3 py-2 text-xs text-text outline-none focus:border-accent"
                />
              </label>

              <div className="mt-4 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setEditingDraft(null)}
                  className="rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-text-muted hover:text-text"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={modifyDraftMutation.isPending}
                  className="rounded-lg bg-accent px-4 py-1.5 text-xs font-semibold text-accent-fg hover:opacity-90 disabled:opacity-50"
                >
                  {modifyDraftMutation.isPending ? 'Saving…' : 'Save Modified Draft'}
                </button>
              </div>
            </form>
          </Card>
        </div>
      )}
    </div>
  )
}
