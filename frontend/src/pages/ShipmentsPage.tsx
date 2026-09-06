import { useState, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import * as shipmentsApi from '../api/shipments'
import { Card } from '../components/ui/Card'
import { PaginationBar } from '../components/ui/PaginationBar'
import { LoadingState, ErrorState, EmptyState } from '../components/ui/StateViews'
import { useAuth } from '../auth/AuthContext'

export function ShipmentsPage() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const [selectedShipment, setSelectedShipment] = useState<string | null>(null)
  const [lat, setLat] = useState<number>(40.7128)
  const [lon, setLon] = useState<number>(-74.006)
  const [successMsg, setSuccessMsg] = useState<string | null>(null)

  // Filter & Pagination state
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'DISPATCHED' | 'IN_TRANSIT' | 'DELAYED' | 'DELIVERED'>('ALL')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(5)

  const { data: shipments, isLoading, isError, refetch } = useQuery({
    queryKey: ['shipments'],
    queryFn: shipmentsApi.listShipments,
    refetchInterval: 10_000,
  })

  const checkpointMutation = useMutation({
    mutationFn: ({ shipmentId, latitude, longitude }: { shipmentId: string; latitude: number; longitude: number }) =>
      shipmentsApi.recordCheckpoint(shipmentId, latitude, longitude),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['shipments'] })
      setSuccessMsg(`Checkpoint recorded! Checkpoint #${updated.checkpointCount}, Updated ETA: ${updated.etaMinutes ?? 'N/A'} mins`)
      setSelectedShipment(null)
      setTimeout(() => setSuccessMsg(null), 6000)
    },
  })

  const deliverMutation = useMutation({
    mutationFn: (shipmentId: string) => shipmentsApi.markDelivered(shipmentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['shipments'] })
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      setSuccessMsg('Shipment marked as DELIVERED successfully!')
      setTimeout(() => setSuccessMsg(null), 6000)
    },
  })

  const isSupplier = user?.role === 'SUPPLIER'

  const allShipments = shipments || []
  const filteredShipments = useMemo(() => {
    return allShipments.filter((s) => {
      const q = searchTerm.trim().toLowerCase()
      const matchesSearch =
        q === '' ||
        s.id.toLowerCase().includes(q) ||
        s.orderId.toLowerCase().includes(q)

      if (!matchesSearch) return false

      if (statusFilter === 'ALL') return true
      return s.status === statusFilter
    })
  }, [allShipments, searchTerm, statusFilter])

  const totalFiltered = filteredShipments.length
  const totalPages = Math.max(1, Math.ceil(totalFiltered / pageSize))
  const currentPage = Math.min(page, totalPages)
  const startIndex = totalFiltered === 0 ? 0 : (currentPage - 1) * pageSize
  const endIndex = Math.min(startIndex + pageSize, totalFiltered)

  const paginatedShipments = useMemo(() => {
    return filteredShipments.slice(startIndex, endIndex)
  }, [filteredShipments, startIndex, endIndex])

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-text">Delivery Checkpoints &amp; Shipments</h1>
          <p className="mt-1 text-sm text-text-muted">
            {isSupplier
              ? 'Update GPS transit checkpoints for your active shipments to dynamically recalculate ETAs.'
              : 'Monitor real-time physical shipment tracking and automated WMA ETA calculations.'}
          </p>
        </div>
        <button
          type="button"
          onClick={() => refetch()}
          className="rounded-lg border border-border bg-surface px-3 py-1.5 text-xs font-medium text-text transition-colors hover:bg-border/40"
        >
          Refresh
        </button>
      </div>

      {successMsg && (
        <div className="rounded-lg border border-emerald-500/30 bg-emerald-500/10 p-3 text-sm text-emerald-400">
          ✓ {successMsg}
        </div>
      )}

      {/* Filter and Search Bar */}
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border bg-surface/60 p-4">
        <div className="flex flex-wrap items-center gap-3">
          <input
            type="text"
            placeholder="Filter by shipment or order ID…"
            value={searchTerm}
            onChange={(e) => {
              setSearchTerm(e.target.value)
              setPage(1)
            }}
            className="w-56 rounded-lg border border-border bg-bg px-3 py-1.5 text-xs text-text outline-none focus:border-accent"
          />

          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value as any)
              setPage(1)
            }}
            className="rounded-lg border border-border bg-bg px-2.5 py-1.5 text-xs text-text outline-none focus:border-accent"
          >
            <option value="ALL">All Statuses ({allShipments.length})</option>
            <option value="IN_TRANSIT">In Transit</option>
            <option value="DELAYED">Delayed</option>
            <option value="DELIVERED">Delivered</option>
            <option value="DISPATCHED">Dispatched</option>
          </select>
        </div>

        <span className="text-xs text-text-muted">
          Found <strong className="text-text">{totalFiltered}</strong> shipments
        </span>
      </div>

      {isLoading && <LoadingState label="Loading shipments…" />}
      {isError && <ErrorState message="Could not load shipments." onRetry={() => refetch()} />}

      {!isLoading && !isError && totalFiltered === 0 && (
        <EmptyState
          title="No matching shipments found"
          description="Try adjusting your filter or search keywords."
        />
      )}

      {!isLoading && !isError && paginatedShipments.length > 0 && (
        <div className="flex flex-col gap-4">
          <div className="grid grid-cols-1 gap-4">
            {paginatedShipments.map((s) => (
              <Card key={s.id} className="flex flex-col justify-between gap-4 p-5 sm:flex-row sm:items-center">
                <div className="flex flex-col gap-1.5">
                  <div className="flex items-center gap-2.5">
                    <span className="font-mono text-sm font-semibold text-text">Shipment {s.id.slice(0, 8)}</span>
                    <span
                      className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${
                        s.status === 'DELIVERED'
                          ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30'
                          : s.status === 'DELAYED'
                            ? 'bg-rose-500/15 text-rose-400 border border-rose-500/30'
                            : 'bg-blue-500/15 text-blue-400 border border-blue-500/30'
                      }`}
                    >
                      {s.status}
                    </span>
                  </div>
                  <div className="flex flex-wrap items-center gap-4 text-xs text-text-muted">
                    <span>Order: <span className="font-mono text-text">{s.orderId.slice(0, 8)}</span></span>
                    <span>Checkpoints Logged: <strong className="text-text">{s.checkpointCount}</strong></span>
                    <span>
                      ETA (Dynamic WMA):{' '}
                      <strong className="text-accent">
                        {s.etaMinutes != null ? `${s.etaMinutes.toFixed(1)} mins` : 'Pending initial checkpoints'}
                      </strong>
                    </span>
                    <span>Created: {new Date(s.createdAt).toLocaleTimeString()}</span>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  {s.status !== 'DELIVERED' && (
                    <>
                      <button
                        type="button"
                        onClick={() => {
                          setSelectedShipment(s.id)
                          setLat(40.7128 + Math.random() * 0.05)
                          setLon(-74.006 + Math.random() * 0.05)
                        }}
                        className="rounded-lg bg-accent px-3 py-1.5 text-xs font-medium text-accent-fg shadow-sm transition-all hover:opacity-90 active:scale-95"
                      >
                        Record Checkpoint
                      </button>
                      <button
                        type="button"
                        disabled={deliverMutation.isPending}
                        onClick={() => deliverMutation.mutate(s.id)}
                        className="rounded-lg border border-emerald-500/40 bg-emerald-500/10 px-3 py-1.5 text-xs font-medium text-emerald-400 hover:bg-emerald-500/20 disabled:opacity-50"
                      >
                        Mark Delivered
                      </button>
                    </>
                  )}
                </div>
              </Card>
            ))}
          </div>

          {/* Pagination Controls */}
          <PaginationBar
            currentPage={currentPage}
            totalPages={totalPages}
            totalItems={totalFiltered}
            pageSize={pageSize}
            onPageChange={setPage}
            onPageSizeChange={setPageSize}
            itemLabel="shipments"
          />
        </div>
      )}

      {/* Record Checkpoint Modal */}
      {selectedShipment && (
        <div className="fixed inset-0 z-30 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
          <Card className="w-full max-w-md p-6 shadow-2xl">
            <h2 className="text-lg font-semibold text-text">Record GPS Delivery Checkpoint</h2>
            <p className="mt-1 text-xs text-text-muted">
              Logging a checkpoint updates the shipment movement event stream and dynamically recalculates the WMA ETA.
            </p>

            <form
              onSubmit={(e) => {
                e.preventDefault()
                checkpointMutation.mutate({
                  shipmentId: selectedShipment,
                  latitude: Number(lat),
                  longitude: Number(lon),
                })
              }}
              className="mt-4 flex flex-col gap-3"
            >
              <label className="flex flex-col gap-1 text-xs">
                <span className="font-medium text-text">Latitude</span>
                <input
                  type="number"
                  step="0.0001"
                  value={lat}
                  onChange={(e) => setLat(parseFloat(e.target.value))}
                  required
                  className="rounded-lg border border-border bg-bg px-3 py-2 text-sm text-text outline-none focus:border-accent"
                />
              </label>

              <label className="flex flex-col gap-1 text-xs">
                <span className="font-medium text-text">Longitude</span>
                <input
                  type="number"
                  step="0.0001"
                  value={lon}
                  onChange={(e) => setLon(parseFloat(e.target.value))}
                  required
                  className="rounded-lg border border-border bg-bg px-3 py-2 text-sm text-text outline-none focus:border-accent"
                />
              </label>

              <div className="flex flex-wrap gap-1.5 pt-1">
                <span className="text-[11px] text-text-muted w-full">Quick Presets:</span>
                <button
                  type="button"
                  onClick={() => { setLat(18.7560); setLon(73.8560); }}
                  className="rounded border border-accent/40 bg-accent/10 px-2 py-0.5 text-[11px] font-medium text-accent hover:bg-accent hover:text-accent-fg"
                >
                  Chakan Plant (Pune)
                </button>
                <button
                  type="button"
                  onClick={() => { setLat(22.9880); setLon(72.3810); }}
                  className="rounded border border-border bg-surface px-2 py-0.5 text-[11px] text-text-muted hover:text-text"
                >
                  Sanand EV Hub (Gujarat)
                </button>
                <button
                  type="button"
                  onClick={() => { setLat(12.9860); setLon(80.0450); }}
                  className="rounded border border-border bg-surface px-2 py-0.5 text-[11px] text-text-muted hover:text-text"
                >
                  Sriperumbudur (Chennai)
                </button>
                <button
                  type="button"
                  onClick={() => { setLat(40.7128); setLon(-74.006); }}
                  className="rounded border border-border bg-surface px-2 py-0.5 text-[11px] text-text-muted hover:text-text"
                >
                  US-East Hub (NYC)
                </button>
              </div>

              <div className="mt-4 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setSelectedShipment(null)}
                  className="rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-text-muted hover:text-text"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={checkpointMutation.isPending}
                  className="rounded-lg bg-accent px-4 py-1.5 text-xs font-medium text-accent-fg hover:opacity-90 disabled:opacity-50"
                >
                  {checkpointMutation.isPending ? 'Logging…' : 'Submit Checkpoint'}
                </button>
              </div>
            </form>
          </Card>
        </div>
      )}
    </div>
  )
}
