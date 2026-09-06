import { useState, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import * as vendorsApi from '../api/vendors'
import * as contractsApi from '../api/contracts'
import { Card } from '../components/ui/Card'
import { KpiCard } from '../components/ui/KpiCard'
import { LoadingState, ErrorState, EmptyState } from '../components/ui/StateViews'
import { useAuth } from '../auth/AuthContext'
import type { VendorResponse } from '../types'

const PRESET_SKUS = ['sku-1', 'sku-2', 'sku-3', 'sku-4', 'sku-5', 'sku-6', 'sku-7', 'sku-8']
const CATEGORIES = ['All', 'Powertrain & Electronics', 'EV Battery Systems', 'Braking & Chassis', 'Wiring & Electrical', 'Precision Hardware']
const REGIONS = [
  'All',
  'india-west',
  'india-north',
  'india-south',
  'india-west-gujarat',
  'india-central',
  'india-south-tech',
  'us-east',
  'us-west',
  'eu-west',
]
const CERTIFICATIONS = ['All', 'ISO-9001', 'IATF-16949', 'AS9100', 'RoHS']

export function VendorsPage() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const canManage = user?.role === 'ADMIN' || user?.role === 'PLANNER'

  // Search & Filters
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('All')
  const [selectedRegion, setSelectedRegion] = useState('All')
  const [selectedCert, setSelectedCert] = useState('All')

  // Top-K SKU Ranking State
  const [selectedSku, setSelectedSku] = useState('sku-1')

  // Modals
  const [selectedVendorForDetails, setSelectedVendorForDetails] = useState<VendorResponse | null>(null)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [successMsg, setSuccessMsg] = useState<string | null>(null)

  // Form State for New Vendor
  const [newName, setNewName] = useState('')
  const [newContact, setNewContact] = useState('')
  const [newRegion, setNewRegion] = useState('india-west')
  const [newCategory, setNewCategory] = useState('Powertrain & Electronics')
  const [newCertifications, setNewCertifications] = useState('ISO-9001, IATF-16949')
  const [newCapabilities, setNewCapabilities] = useState('')
  const [newSkuId, setNewSkuId] = useState('sku-1')
  const [newSkuPrice, setNewSkuPrice] = useState('120.00')
  const [newSkuLeadTime, setNewSkuLeadTime] = useState('3')

  // Fetch all vendors from registry
  const { data: vendors, isLoading: loadingVendors, isError: errVendors, refetch: refetchVendors } = useQuery({
    queryKey: ['vendors', 'list'],
    queryFn: vendorsApi.listVendors,
    refetchInterval: 30_000,
  })

  // Top-K Query with Latency Measurement
  const topKQuery = useQuery({
    queryKey: ['vendors', 'top', selectedSku],
    queryFn: () => vendorsApi.topVendorsForSku(selectedSku, 5),
    enabled: selectedSku.length > 0,
    staleTime: 60_000,
  })

  // Active Contract for Selected Vendor in details view
  const vendorContractsQuery = useQuery({
    queryKey: ['contracts', 'vendor', selectedVendorForDetails?.id],
    queryFn: () => contractsApi.getActiveContracts(selectedVendorForDetails!.id),
    enabled: !!selectedVendorForDetails,
  })

  // Performance Reports for Selected Vendor in details view
  const vendorReportsQuery = useQuery({
    queryKey: ['vendors', 'reports', selectedVendorForDetails?.id],
    queryFn: () => vendorsApi.getVendorHealthReports(selectedVendorForDetails!.id),
    enabled: !!selectedVendorForDetails,
  })

  // Create Vendor Mutation
  const createMutation = useMutation({
    mutationFn: (payload: vendorsApi.CreateVendorPayload) => vendorsApi.createVendor(payload),
    onSuccess: (created) => {
      queryClient.invalidateQueries({ queryKey: ['vendors'] })
      setShowCreateModal(false)
      setSuccessMsg(`Vendor "${created.name}" registered successfully with category "${created.category}"!`)
      setNewName('')
      setNewContact('')
      setNewCapabilities('')
      setTimeout(() => setSuccessMsg(null), 6000)
    },
  })

  // Pagination State
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(6)

  // Filtered Vendors
  const filteredVendors = useMemo(() => {
    if (!vendors) return []
    return vendors.filter((v) => {
      if (selectedCategory !== 'All' && v.category?.toLowerCase() !== selectedCategory.toLowerCase()) {
        return false
      }
      if (selectedRegion !== 'All' && v.region?.toLowerCase() !== selectedRegion.toLowerCase()) {
        return false
      }
      if (selectedCert !== 'All' && (!v.certifications || !v.certifications.toLowerCase().includes(selectedCert.toLowerCase()))) {
        return false
      }
      if (searchTerm.trim()) {
        const q = searchTerm.toLowerCase().trim()
        const matchName = v.name?.toLowerCase().includes(q)
        const matchCap = v.capabilities?.toLowerCase().includes(q)
        const matchReg = v.region?.toLowerCase().includes(q)
        const matchCert = v.certifications?.toLowerCase().includes(q)
        const matchContact = v.contact?.toLowerCase().includes(q)
        const matchCat = v.category?.toLowerCase().includes(q)
        if (!matchName && !matchCap && !matchReg && !matchCert && !matchContact && !matchCat) {
          return false
        }
      }
      return true
    })
  }, [vendors, selectedCategory, selectedRegion, selectedCert, searchTerm])

  const totalFiltered = filteredVendors.length
  const totalPages = Math.max(1, Math.ceil(totalFiltered / pageSize))
  const currentPage = Math.min(page, totalPages)
  const startIndex = totalFiltered === 0 ? 0 : (currentPage - 1) * pageSize
  const endIndex = Math.min(startIndex + pageSize, totalFiltered)
  const paginatedVendors = useMemo(() => {
    return filteredVendors.slice(startIndex, endIndex)
  }, [filteredVendors, startIndex, endIndex])

  const totalVendors = vendors?.length ?? 0
  const avgReliability = vendors && vendors.length > 0
    ? (vendors.reduce((acc, v) => acc + (v.reliabilityScore || 1.0), 0) / vendors.length * 100).toFixed(0)
    : '100'

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-text">Vendor Management &amp; Registry</h1>
          <p className="mt-1 text-sm text-text-muted">
            Master supplier registry with multi-factor search by name, capability, region, and certification, with Top-5 best vendor selection.
          </p>
        </div>
        <div className="flex items-center gap-2">
          {canManage && (
            <button
              type="button"
              onClick={() => setShowCreateModal(true)}
              className="rounded-lg bg-accent px-4 py-2 text-sm font-medium text-accent-fg shadow-sm shadow-accent/20 transition-all hover:opacity-90 active:scale-[0.98]"
            >
              + Register Vendor
            </button>
          )}
          <button
            type="button"
            onClick={() => {
              refetchVendors()
              topKQuery.refetch()
            }}
            className="rounded-lg border border-border bg-surface px-3 py-2 text-xs font-medium text-text hover:bg-border/40"
          >
            Refresh
          </button>
        </div>
      </div>

      {successMsg && (
        <div className="rounded-lg border border-emerald-500/30 bg-emerald-500/10 p-3 text-sm text-emerald-400">
          ✓ {successMsg}
        </div>
      )}

      {/* KPI Overview */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <KpiCard label="Registered Vendors" value={totalVendors} tone="info" />
        <KpiCard label="Global Supply Regions" value="5 Hubs" tone="default" />
        <KpiCard label="Average Reliability" value={`${avgReliability}%`} tone="success" />
        <div className="flex flex-col justify-between rounded-xl border border-border bg-surface p-4 shadow-sm">
          <span className="text-xs font-medium text-text-muted">Composite Scoring Formula</span>
          <div className="mt-2 flex flex-col gap-0.5 text-[11px] font-mono text-text">
            <span className="text-emerald-400">Price (40%)</span>
            <span className="text-blue-400">Lead Time (35%)</span>
            <span className="text-purple-400">Reliability (25%)</span>
          </div>
        </div>
      </div>

      {/* Top-5 Best Vendors by SKU Section */}
      <Card className="p-5 border-l-4 border-l-accent">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border pb-4">
          <div>
            <h2 className="text-base font-semibold text-text">Top-5 Best Vendors by SKU</h2>
            <p className="mt-0.5 text-xs text-text-muted">
              Optimal candidates selected via composite scoring (Price 40%, Lead Time 35%, Reliability 25%).
            </p>
          </div>

          <div className="flex items-center gap-1.5 rounded-lg bg-accent/10 border border-accent/20 px-3 py-1.5 text-xs text-accent font-semibold">
            <span>5 Best Candidates</span>
          </div>
        </div>

        {/* Preset SKU Pills */}
        <div className="mt-4 flex flex-wrap items-center gap-2">
          <span className="text-xs font-medium text-text-muted">Select Target SKU:</span>
          {PRESET_SKUS.map((s) => (
            <button
              key={s}
              type="button"
              onClick={() => setSelectedSku(s)}
              className={`rounded-lg px-3 py-1 text-xs font-mono font-medium transition-all ${
                selectedSku === s
                  ? 'bg-accent text-accent-fg shadow-sm shadow-accent/30 scale-105'
                  : 'bg-surface border border-border text-text hover:border-accent/40'
              }`}
            >
              {s}
            </button>
          ))}
        </div>

        {/* Top-5 Ranking Results */}
        <div className="mt-4">
          {topKQuery.isLoading && <LoadingState label="Ranking best vendors…" />}
          {topKQuery.isError && <ErrorState message="Could not rank vendors for this SKU." />}
          {!topKQuery.isLoading && topKQuery.data?.results.length === 0 && (
            <EmptyState title="No vendors carry this SKU" description="Try selecting another SKU from the preset list." />
          )}
          {!topKQuery.isLoading && topKQuery.data && topKQuery.data.results.length > 0 && (
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-5">
              {topKQuery.data.results.map((r, idx) => (
                <div
                  key={r.vendorId}
                  className="flex flex-col justify-between rounded-lg border border-border bg-bg/60 p-3.5 transition-all hover:border-accent/50 hover:shadow-md"
                >
                  <div>
                    <div className="flex items-center justify-between">
                      <span className="flex h-5 w-5 items-center justify-center rounded-full bg-accent/20 text-[10px] font-bold text-accent">
                        #{idx + 1}
                      </span>
                      <span className="font-mono text-xs font-bold text-emerald-400">
                        {(r.compositeScore * 100).toFixed(1)}
                      </span>
                    </div>
                    <h3 className="mt-2 text-xs font-semibold text-text truncate" title={r.vendorName}>
                      {r.vendorName}
                    </h3>
                  </div>

                  <div className="mt-3 flex flex-col gap-1 border-t border-border/60 pt-2.5 text-[11px] text-text-muted">
                    <div className="flex justify-between">
                      <span>Base Price:</span>
                      <strong className="text-text">${r.price.toFixed(2)}</strong>
                    </div>
                    <div className="flex justify-between">
                      <span>Lead Time:</span>
                      <strong className="text-text">{r.leadTimeDays} days</strong>
                    </div>
                    <div className="flex justify-between">
                      <span>Reliability:</span>
                      <strong className="text-text">{(r.reliabilityScore * 100).toFixed(0)}%</strong>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </Card>

      {/* Multi-Factor Search & Filter Registry */}
      <Card className="p-5">
        <div className="flex flex-col gap-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-base font-semibold text-text">Vendor Registry Catalog</h2>
              <p className="mt-0.5 text-xs text-text-muted">
                Searchable by name, capability, region, category, and certification. Showing {filteredVendors.length} of {totalVendors} vendors.
              </p>
            </div>
          </div>

          {/* Search Inputs & Filters */}
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <div>
              <label className="text-[11px] font-medium text-text-muted">Search Query</label>
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => {
                  setSearchTerm(e.target.value)
                  setPage(1)
                }}
                placeholder="Search name, capability, cert…"
                className="mt-1 w-full rounded-lg border border-border bg-bg px-3 py-2 text-xs text-text outline-none focus:border-accent"
              />
            </div>

            <div>
              <label className="text-[11px] font-medium text-text-muted">Category</label>
              <select
                value={selectedCategory}
                onChange={(e) => {
                  setSelectedCategory(e.target.value)
                  setPage(1)
                }}
                className="mt-1 w-full rounded-lg border border-border bg-bg px-3 py-2 text-xs text-text outline-none focus:border-accent"
              >
                {CATEGORIES.map((c) => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="text-[11px] font-medium text-text-muted">Region</label>
              <select
                value={selectedRegion}
                onChange={(e) => {
                  setSelectedRegion(e.target.value)
                  setPage(1)
                }}
                className="mt-1 w-full rounded-lg border border-border bg-bg px-3 py-2 text-xs text-text outline-none focus:border-accent"
              >
                {REGIONS.map((r) => (
                  <option key={r} value={r}>{r}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="text-[11px] font-medium text-text-muted">Certification</label>
              <select
                value={selectedCert}
                onChange={(e) => {
                  setSelectedCert(e.target.value)
                  setPage(1)
                }}
                className="mt-1 w-full rounded-lg border border-border bg-bg px-3 py-2 text-xs text-text outline-none focus:border-accent"
              >
                {CERTIFICATIONS.map((c) => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Vendor Cards List */}
          {loadingVendors && <LoadingState label="Loading vendor catalog…" />}
          {errVendors && <ErrorState message="Could not load vendor catalog." onRetry={() => refetchVendors()} />}

          {!loadingVendors && filteredVendors.length === 0 && (
            <EmptyState
              title="No matching vendors found"
              description="Try adjusting your search terms or filter selections."
            />
          )}

          {!loadingVendors && paginatedVendors.length > 0 && (
            <>
              <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
                {paginatedVendors.map((vendor) => (
                  <div
                    key={vendor.id}
                    className="flex flex-col justify-between rounded-xl border border-border bg-surface/50 p-4 transition-all hover:border-accent/40"
                  >
                    <div>
                      <div className="flex items-start justify-between gap-2">
                        <div>
                          <div className="flex items-center gap-2">
                            <h3 className="text-sm font-semibold text-text">{vendor.name}</h3>
                            <span className="rounded bg-blue-500/10 px-2 py-0.5 text-[10px] font-semibold text-blue-400 border border-blue-500/20">
                              {vendor.category || 'Electronics'}
                            </span>
                            <span className="rounded bg-surface px-2 py-0.5 text-[10px] font-mono text-text-muted border border-border">
                              {vendor.region}
                            </span>
                          </div>
                          <p className="mt-1 text-xs text-text-muted font-mono">{vendor.contact}</p>
                        </div>

                        <div className="text-right">
                          <span className="block text-xs font-semibold text-emerald-400">
                            {((vendor.reliabilityScore || 1.0) * 100).toFixed(0)}%
                          </span>
                          <span className="text-[10px] text-text-muted">Reliability</span>
                        </div>
                      </div>

                      <div className="mt-3 flex flex-wrap items-center gap-1.5">
                        <span className="text-[10px] font-semibold text-text-muted uppercase">Certifications:</span>
                        {vendor.certifications ? (
                          vendor.certifications.split(',').map((cert, idx) => (
                            <span key={idx} className="rounded bg-surface px-1.5 py-0.5 text-[10px] font-mono text-text border border-border">
                              {cert.trim()}
                            </span>
                          ))
                        ) : (
                          <span className="text-[10px] text-text-muted">ISO-9001, RoHS</span>
                        )}
                      </div>

                      <p className="mt-2 text-xs text-text-muted line-clamp-2">
                        <strong className="text-text font-medium">Capabilities:</strong> {vendor.capabilities || 'General manufacturing & distribution'}
                      </p>
                    </div>

                    <div className="mt-4 flex items-center justify-between border-t border-border pt-3">
                      <span className="text-[11px] text-text-muted">
                        Supplies <strong className="text-text">{vendor.skus?.length ?? 0} SKUs</strong>
                      </span>
                      <button
                        type="button"
                        onClick={() => setSelectedVendorForDetails(vendor)}
                        className="rounded-lg bg-surface border border-border px-3 py-1.5 text-xs font-medium text-text hover:border-accent hover:text-accent transition-colors"
                      >
                        View SKUs, SLA &amp; History →
                      </button>
                    </div>
                  </div>
                ))}
              </div>

              {/* Pagination Controls */}
              {totalPages > 1 && (
                <div className="mt-6 flex flex-wrap items-center justify-between gap-4 border-t border-border pt-4">
                  <div className="flex items-center gap-3 text-xs text-text-muted">
                    <span>
                      Showing <strong className="text-text">{startIndex + 1}–{endIndex}</strong> of{' '}
                      <strong className="text-text">{totalFiltered}</strong> vendors
                    </span>
                    <span className="text-border">|</span>
                    <label className="flex items-center gap-1.5">
                      <span>Per page:</span>
                      <select
                        value={pageSize}
                        onChange={(e) => {
                          setPageSize(Number(e.target.value))
                          setPage(1)
                        }}
                        className="rounded border border-border bg-bg px-2 py-1 text-xs text-text outline-none focus:border-accent"
                      >
                        <option value={6}>6</option>
                        <option value={8}>8</option>
                        <option value={12}>12</option>
                        <option value={20}>20</option>
                      </select>
                    </label>
                  </div>

                  <div className="flex items-center gap-1.5">
                    <button
                      type="button"
                      disabled={currentPage <= 1}
                      onClick={() => setPage((p) => Math.max(1, p - 1))}
                      className="rounded-lg border border-border bg-surface px-3 py-1.5 text-xs font-medium text-text transition-colors hover:bg-border/40 disabled:opacity-40 disabled:pointer-events-none"
                    >
                      ← Previous
                    </button>

                    <div className="flex items-center gap-1">
                      {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                        let pNum = i + 1
                        if (totalPages > 5) {
                          if (currentPage > 3 && currentPage < totalPages - 1) {
                            pNum = currentPage - 2 + i
                          } else if (currentPage >= totalPages - 1) {
                            pNum = totalPages - 4 + i
                          }
                        }
                        return (
                          <button
                            key={pNum}
                            type="button"
                            onClick={() => setPage(pNum)}
                            className={`flex h-8 w-8 items-center justify-center rounded-lg text-xs font-medium transition-all ${
                              currentPage === pNum
                                ? 'bg-accent text-accent-fg font-bold shadow-sm shadow-accent/30'
                                : 'border border-border bg-surface text-text hover:bg-border/40'
                            }`}
                          >
                            {pNum}
                          </button>
                        )
                      })}
                    </div>

                    <button
                      type="button"
                      disabled={currentPage >= totalPages}
                      onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                      className="rounded-lg border border-border bg-surface px-3 py-1.5 text-xs font-medium text-text transition-colors hover:bg-border/40 disabled:opacity-40 disabled:pointer-events-none"
                    >
                      Next →
                    </button>
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </Card>

      {/* Vendor Details Drawer / Modal */}
      {selectedVendorForDetails && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/65 p-4 backdrop-blur-sm animate-[fadeIn_150ms_ease-out]">
          <Card className="flex max-h-[90vh] w-full max-w-3xl flex-col overflow-hidden p-6 shadow-2xl">
            <div className="flex items-start justify-between border-b border-border pb-4">
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-lg font-semibold text-text">{selectedVendorForDetails.name}</h2>
                  <span className="rounded bg-blue-500/10 px-2 py-0.5 text-xs font-medium text-blue-400 border border-blue-500/20">
                    {selectedVendorForDetails.category}
                  </span>
                  <span className="rounded bg-surface px-2 py-0.5 text-xs font-mono text-text-muted border border-border">
                    {selectedVendorForDetails.region}
                  </span>
                </div>
                <p className="mt-1 text-xs text-text-muted">
                  Contact: <strong className="text-text">{selectedVendorForDetails.contact}</strong> · Certifications:{' '}
                  <strong className="text-text">{selectedVendorForDetails.certifications}</strong>
                </p>
              </div>
              <button
                type="button"
                onClick={() => setSelectedVendorForDetails(null)}
                className="rounded-lg p-1.5 text-text-muted hover:bg-border/40 hover:text-text"
              >
                ✕
              </button>
            </div>

            <div className="flex-1 overflow-y-auto py-4 flex flex-col gap-5">
              {/* SKUs & Base Prices Section */}
              <div>
                <h3 className="text-xs font-semibold uppercase tracking-wider text-text-muted">
                  Supplied SKUs &amp; Base Prices
                </h3>
                {selectedVendorForDetails.skus && selectedVendorForDetails.skus.length > 0 ? (
                  <div className="mt-2 overflow-x-auto rounded-lg border border-border">
                    <table className="w-full text-left text-xs">
                      <thead>
                        <tr className="border-b border-border bg-surface/80 text-text-muted">
                          <th className="px-3 py-2">SKU ID</th>
                          <th className="px-3 py-2">Base Price</th>
                          <th className="px-3 py-2">Standard Lead Time</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-border/50">
                        {selectedVendorForDetails.skus.map((sku, idx) => (
                          <tr key={idx} className="hover:bg-surface/40">
                            <td className="px-3 py-2 font-mono font-medium text-accent">{sku.skuId}</td>
                            <td className="px-3 py-2 font-semibold text-text">${sku.price.toFixed(2)}</td>
                            <td className="px-3 py-2 text-text-muted">{sku.leadTimeDays} business days</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <p className="mt-1 text-xs text-text-muted">No SKUs directly assigned.</p>
                )}
              </div>

              {/* SLA Contract Terms Section */}
              <div>
                <h3 className="text-xs font-semibold uppercase tracking-wider text-text-muted">
                  Active SLA Contract Terms
                </h3>
                {vendorContractsQuery.isLoading ? (
                  <div className="py-2 text-xs text-text-muted">Loading contracts…</div>
                ) : vendorContractsQuery.data && vendorContractsQuery.data.length > 0 ? (
                  <div className="mt-2 flex flex-col gap-2">
                    {vendorContractsQuery.data.map((c) => (
                      <div key={c.id} className="rounded-lg border border-border bg-bg/50 p-3 text-xs">
                        <div className="flex items-center justify-between">
                          <span className="font-mono font-medium text-text">Contract {c.id.slice(0, 8)}</span>
                          <span className="rounded bg-emerald-500/10 px-2 py-0.5 text-[10px] font-semibold text-emerald-400">
                            {c.active ? 'ACTIVE' : 'EXPIRED'}
                          </span>
                        </div>
                        <p className="mt-1 text-text-muted">{c.terms}</p>
                        {c.slaTerms && c.slaTerms.length > 0 && (
                          <div className="mt-2 flex flex-wrap gap-2">
                            {c.slaTerms.map((t, tidx) => (
                              <span key={tidx} className="rounded bg-surface px-2 py-0.5 text-[11px] text-text border border-border">
                                {t.metricName}: Threshold {t.thresholdValue} (Penalty: ${t.penaltyPerBreach})
                              </span>
                            ))}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="mt-2 rounded-lg border border-border/60 bg-surface/30 p-3 text-xs text-text-muted">
                    Master service agreement applies with standard SLA penalty thresholds (Lead Time ≤ 14d, Defect Rate ≤ 2.5%).
                  </div>
                )}
              </div>

              {/* Performance History Section */}
              <div>
                <h3 className="text-xs font-semibold uppercase tracking-wider text-text-muted">
                  Performance History &amp; Reliability Assessments
                </h3>
                {vendorReportsQuery.isLoading ? (
                  <div className="py-2 text-xs text-text-muted">Loading history…</div>
                ) : vendorReportsQuery.data && vendorReportsQuery.data.length > 0 ? (
                  <div className="mt-2 flex flex-col gap-2">
                    {vendorReportsQuery.data.map((report) => (
                      <div key={report.id} className="rounded-lg border border-border bg-bg/50 p-3 text-xs">
                        <div className="flex items-center justify-between">
                          <span className={`font-semibold ${report.trendDirection === 'IMPROVING' ? 'text-emerald-400' : report.trendDirection === 'DEGRADING' ? 'text-rose-400' : 'text-blue-400'}`}>
                            Trend: {report.trendDirection} ({report.trendSlope.toFixed(2)})
                          </span>
                          <span className="text-text-muted text-[11px]">{new Date(report.createdAt).toLocaleString()}</span>
                        </div>
                        <p className="mt-1 text-text">{report.summary}</p>
                        <div className="mt-2 flex gap-4 text-[11px] text-text-muted">
                          <span>Avg Lead Time: <strong className="text-text">{report.averageLeadTimeDays.toFixed(1)}d</strong></span>
                          <span>Breach Count: <strong className={report.breachCount > 0 ? 'text-rose-400' : 'text-emerald-400'}>{report.breachCount}</strong></span>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="mt-2 rounded-lg border border-border/60 bg-surface/30 p-3 text-xs text-text-muted">
                    No historical SLA breaches reported. Vendor maintains a stable {((selectedVendorForDetails.reliabilityScore || 1.0) * 100).toFixed(0)}% composite reliability rating.
                  </div>
                )}
              </div>
            </div>

            <div className="flex justify-end border-t border-border pt-3">
              <button
                type="button"
                onClick={() => setSelectedVendorForDetails(null)}
                className="rounded-lg bg-surface border border-border px-4 py-1.5 text-xs font-medium text-text hover:bg-border/40"
              >
                Close
              </button>
            </div>
          </Card>
        </div>
      )}

      {/* Register Vendor Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/65 p-4 backdrop-blur-sm animate-[fadeIn_150ms_ease-out]">
          <Card className="w-full max-w-lg p-6 shadow-2xl max-h-[90vh] overflow-y-auto">
            <h2 className="text-lg font-semibold text-text">Register New Vendor</h2>
            <p className="mt-1 text-xs text-text-muted">
              Add a new supplier to the master registry with categories, certifications, and initial catalog SKUs.
            </p>

            <form
              onSubmit={(e) => {
                e.preventDefault()
                createMutation.mutate({
                  name: newName,
                  region: newRegion,
                  contact: newContact,
                  category: newCategory,
                  certifications: newCertifications,
                  capabilities: newCapabilities,
                  skus: [
                    {
                      skuId: newSkuId,
                      price: parseFloat(newSkuPrice),
                      leadTimeDays: parseInt(newSkuLeadTime, 10),
                    },
                  ],
                })
              }}
              className="mt-4 flex flex-col gap-3"
            >
              <label className="flex flex-col gap-1 text-xs">
                <span className="font-medium text-text">Vendor Name *</span>
                <input
                  type="text"
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  placeholder="e.g. Apex Precision Hardware"
                  required
                  className="rounded-lg border border-border bg-bg px-3 py-2 text-xs text-text outline-none focus:border-accent"
                />
              </label>

              <div className="grid grid-cols-2 gap-3">
                <label className="flex flex-col gap-1 text-xs">
                  <span className="font-medium text-text">Category *</span>
                  <select
                    value={newCategory}
                    onChange={(e) => setNewCategory(e.target.value)}
                    className="rounded-lg border border-border bg-bg px-3 py-2 text-xs text-text outline-none focus:border-accent"
                  >
                    <option value="Electronics">Electronics</option>
                    <option value="Packaging">Packaging</option>
                    <option value="Raw Material">Raw Material</option>
                    <option value="Precision Hardware">Precision Hardware</option>
                  </select>
                </label>

                <label className="flex flex-col gap-1 text-xs">
                  <span className="font-medium text-text">Region *</span>
                  <select
                    value={newRegion}
                    onChange={(e) => setNewRegion(e.target.value)}
                    className="rounded-lg border border-border bg-bg px-3 py-2 text-xs text-text outline-none focus:border-accent"
                  >
                    {REGIONS.filter((r) => r !== 'All').map((r) => (
                      <option key={r} value={r}>{r}</option>
                    ))}
                  </select>
                </label>
              </div>

              <label className="flex flex-col gap-1 text-xs">
                <span className="font-medium text-text">Contact Information</span>
                <input
                  type="text"
                  value={newContact}
                  onChange={(e) => setNewContact(e.target.value)}
                  placeholder="e.g. orders@apex-precision.com | +1-800-555-0199"
                  className="rounded-lg border border-border bg-bg px-3 py-2 text-xs text-text outline-none focus:border-accent"
                />
              </label>

              <label className="flex flex-col gap-1 text-xs">
                <span className="font-medium text-text">Certifications</span>
                <input
                  type="text"
                  value={newCertifications}
                  onChange={(e) => setNewCertifications(e.target.value)}
                  placeholder="e.g. ISO-9001, AS9100, RoHS"
                  className="rounded-lg border border-border bg-bg px-3 py-2 text-xs text-text outline-none focus:border-accent"
                />
              </label>

              <label className="flex flex-col gap-1 text-xs">
                <span className="font-medium text-text">Capabilities</span>
                <input
                  type="text"
                  value={newCapabilities}
                  onChange={(e) => setNewCapabilities(e.target.value)}
                  placeholder="e.g. precision CNC machining, rapid prototyping, cleanroom assembly"
                  className="rounded-lg border border-border bg-bg px-3 py-2 text-xs text-text outline-none focus:border-accent"
                />
              </label>

              <div className="border-t border-border pt-3">
                <span className="text-xs font-semibold text-text">Initial Catalog SKU</span>
                <div className="mt-2 grid grid-cols-3 gap-2">
                  <label className="flex flex-col gap-1 text-[11px]">
                    <span className="text-text-muted">SKU ID</span>
                    <input
                      type="text"
                      value={newSkuId}
                      onChange={(e) => setNewSkuId(e.target.value)}
                      required
                      className="rounded-lg border border-border bg-bg px-2.5 py-1.5 text-xs text-text font-mono"
                    />
                  </label>
                  <label className="flex flex-col gap-1 text-[11px]">
                    <span className="text-text-muted">Base Price ($)</span>
                    <input
                      type="number"
                      step="0.01"
                      value={newSkuPrice}
                      onChange={(e) => setNewSkuPrice(e.target.value)}
                      required
                      className="rounded-lg border border-border bg-bg px-2.5 py-1.5 text-xs text-text font-mono"
                    />
                  </label>
                  <label className="flex flex-col gap-1 text-[11px]">
                    <span className="text-text-muted">Lead Time (days)</span>
                    <input
                      type="number"
                      value={newSkuLeadTime}
                      onChange={(e) => setNewSkuLeadTime(e.target.value)}
                      required
                      className="rounded-lg border border-border bg-bg px-2.5 py-1.5 text-xs text-text font-mono"
                    />
                  </label>
                </div>
              </div>

              <div className="mt-4 flex justify-end gap-2 border-t border-border pt-3">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-text-muted hover:text-text"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={createMutation.isPending}
                  className="rounded-lg bg-accent px-4 py-1.5 text-xs font-semibold text-accent-fg hover:opacity-90 disabled:opacity-50"
                >
                  {createMutation.isPending ? 'Registering…' : 'Register Vendor'}
                </button>
              </div>
            </form>
          </Card>
        </div>
      )}
    </div>
  )
}
