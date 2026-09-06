import { useQuery, useQueryClient, useMutation, keepPreviousData } from '@tanstack/react-query'
import { useEffect, useState, type FormEvent } from 'react'
import * as ordersApi from '../api/orders'
import { getOrder } from '../api/orders'
import { Card } from '../components/ui/Card'
import { OrderGraph } from '../components/graph/OrderGraph'
import { LoadingState, ErrorState, EmptyState } from '../components/ui/StateViews'
import { StatusBadge } from '../components/ui/Badge'
import { useNotifications } from '../ws/NotificationsContext'
import { useAuth } from '../auth/AuthContext'
import type { OrderResponse } from '../types'

const PAGE_SIZE_OPTIONS = [10, 20, 50]

const STATUS_FILTERS = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'ALL', label: 'All' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'CONFIRMED', label: 'Confirmed' },
  { value: 'SHIPPED', label: 'Shipped' },
  { value: 'DELIVERED', label: 'Delivered' },
  { value: 'CANCELLED', label: 'Cancelled' },
  { value: 'CLOSED', label: 'Closed' },
] as const

// Orders reach CANCELLED/CONFIRMED only from PENDING, and CANCELLED only from PENDING/CONFIRMED
// (see OrderStateMachine in order-service) - mirror that here so the button doesn't offer an
// action the backend would reject with a 409.
const CANCELLABLE_STATUSES = new Set(['PENDING', 'CONFIRMED'])

function ordersQueryKey(statusFilter: string, page: number, size: number) {
  return ['orders', statusFilter, page, size] as const
}

function useOrderLiveUpdates(orders: OrderResponse[] | undefined, queryKey: readonly unknown[]) {
  const { subscribeToOrder, connected } = useNotifications()
  const queryClient = useQueryClient()

  useEffect(() => {
    if (!connected || !orders?.length) return

    const unsubscribers = orders
      .filter((order) => order.status === 'PENDING')
      .map((order) =>
        subscribeToOrder(order.id, async () => {
          const updated = await getOrder(order.id)
          queryClient.setQueryData<{ content: OrderResponse[] } | undefined>(queryKey, (current) => {
            if (!current) return current
            return { ...current, content: current.content.map((o) => (o.id === updated.id ? updated : o)) }
          })
        }),
      )

    return () => unsubscribers.forEach((unsub) => unsub())
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [connected, orders?.map((o) => `${o.id}:${o.status}`).join(','), queryClient, subscribeToOrder])
}

export function OrdersPage() {
  const queryClient = useQueryClient()
  const [statusFilter, setStatusFilter] = useState<string>('ACTIVE')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)
  const [showCreate, setShowCreate] = useState(false)
  const [cancelTarget, setCancelTarget] = useState<OrderResponse | null>(null)

  const queryKey = ordersQueryKey(statusFilter, page, size)
  const { data, isLoading, isError, isFetching, refetch } = useQuery({
    queryKey,
    queryFn: () => ordersApi.listOrders(statusFilter === 'ACTIVE' ? undefined : statusFilter, page, size),
    placeholderData: keepPreviousData,
  })

  useOrderLiveUpdates(data?.content, queryKey)

  function changeFilter(next: string) {
    setStatusFilter(next)
    setPage(0)
  }

  function changeSize(next: number) {
    setSize(next)
    setPage(0)
  }

  const createMutation = useMutation({
    mutationFn: ordersApi.createOrder,
    onSuccess: () => {
      setShowCreate(false)
      setStatusFilter('ACTIVE')
      setPage(0)
      queryClient.invalidateQueries({ queryKey: ['orders'] })
    },
  })

  const cancelMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => ordersApi.cancelOrder(id, reason),
    onSuccess: () => {
      setCancelTarget(null)
      queryClient.invalidateQueries({ queryKey: ['orders'] })
    },
  })

  const totalElements = data?.totalElements ?? 0
  const pageCount = size > 0 ? Math.max(1, Math.ceil(totalElements / size)) : 1
  const rangeStart = totalElements === 0 ? 0 : page * size + 1
  const rangeEnd = Math.min(totalElements, (page + 1) * size)

  const { user } = useAuth()
  const canCreate = user?.role === 'PLANNER' || user?.role === 'ADMIN'

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-text">Orders</h1>
          {user?.role === 'SUPPLIER' && (
            <p className="mt-0.5 text-xs text-text-muted">
              Logged in as Supplier representative. Showing assigned orders and fulfillment status.
            </p>
          )}
        </div>
        {canCreate ? (
          <button
            type="button"
            onClick={() => setShowCreate(true)}
            className="rounded-lg bg-accent px-4 py-2 text-sm font-medium text-accent-fg shadow-sm shadow-accent/20 transition-all hover:opacity-90 active:scale-[0.98]"
          >
            New order
          </button>
        ) : (
          <span className="rounded-full border border-emerald-500/30 bg-emerald-500/10 px-3 py-1.5 text-xs font-semibold text-emerald-400">
            Supplier View (Orders Read-Only)
          </span>
        )}
      </div>

      <div className="flex flex-wrap items-center gap-2">
        {STATUS_FILTERS.map((f) => (
          <button
            key={f.value}
            type="button"
            onClick={() => changeFilter(f.value)}
            className={`rounded-full border px-3 py-1.5 text-sm font-medium transition-colors ${
              statusFilter === f.value
                ? 'border-accent bg-accent/10 text-accent'
                : 'border-border bg-surface text-text-muted hover:bg-border/40 hover:text-text'
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      {isLoading && <LoadingState label="Loading orders…" />}
      {isError && <ErrorState message="Couldn't load orders." onRetry={() => refetch()} />}

      {!isLoading && !isError && (
        <>
          <Card>
            <h2 className="mb-3 text-sm font-medium text-text-muted">
              Order graph <span className="font-normal">(current page)</span>
            </h2>
            {data?.content.length ? (
              <OrderGraph orders={data.content} />
            ) : (
              <EmptyState title="No orders" description="No orders match this filter." />
            )}
          </Card>

          <Card>
            <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
              <h2 className="text-sm font-medium text-text-muted">Order list</h2>
              <div className="flex items-center gap-3">
                <label className="flex items-center gap-1.5 text-xs text-text-muted">
                  Rows
                  <select
                    value={size}
                    onChange={(e) => changeSize(Number(e.target.value))}
                    className="rounded-md border border-border bg-surface px-1.5 py-1 text-xs text-text outline-none focus:border-accent"
                  >
                    {PAGE_SIZE_OPTIONS.map((n) => (
                      <option key={n} value={n}>
                        {n}
                      </option>
                    ))}
                  </select>
                </label>
                <button
                  type="button"
                  onClick={() => refetch()}
                  disabled={isFetching}
                  title="Refresh"
                  className="rounded-md border border-border p-1.5 text-text-muted transition-colors hover:bg-border/40 hover:text-text disabled:opacity-50"
                >
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth={1.75}
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    className={`h-4 w-4 ${isFetching ? 'animate-spin' : ''}`}
                  >
                    <path d="M21 12a9 9 0 1 1-2.64-6.36" />
                    <path d="M21 3v6h-6" />
                  </svg>
                </button>
              </div>
            </div>

            {data?.content.length ? (
              <>
                <OrdersTable
                  orders={data.content}
                  onCancel={(order) => setCancelTarget(order)}
                />
                <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-border pt-3 text-sm text-text-muted">
                  <span>
                    {rangeStart}-{rangeEnd} of {totalElements}
                  </span>
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => setPage((p) => Math.max(0, p - 1))}
                      disabled={page === 0}
                      className="rounded-lg border border-border px-3 py-1.5 font-medium text-text transition-colors hover:bg-border/40 disabled:cursor-not-allowed disabled:opacity-40"
                    >
                      Previous
                    </button>
                    <span className="tabular-nums">
                      Page {page + 1} of {pageCount}
                    </span>
                    <button
                      type="button"
                      onClick={() => setPage((p) => Math.min(pageCount - 1, p + 1))}
                      disabled={page + 1 >= pageCount}
                      className="rounded-lg border border-border px-3 py-1.5 font-medium text-text transition-colors hover:bg-border/40 disabled:cursor-not-allowed disabled:opacity-40"
                    >
                      Next
                    </button>
                  </div>
                </div>
              </>
            ) : (
              <EmptyState
                title="No orders"
                description={
                  statusFilter === 'ACTIVE'
                    ? 'Orders you create will show up here.'
                    : `No orders currently have status "${statusFilter}".`
                }
              />
            )}
          </Card>
        </>
      )}

      {showCreate && (
        <CreateOrderModal
          onClose={() => setShowCreate(false)}
          onSubmit={(request) => createMutation.mutate(request)}
          submitting={createMutation.isPending}
          error={createMutation.isError ? 'Failed to create order.' : null}
        />
      )}

      {cancelTarget && (
        <CancelOrderModal
          order={cancelTarget}
          onClose={() => setCancelTarget(null)}
          onConfirm={(reason) => cancelMutation.mutate({ id: cancelTarget.id, reason })}
          submitting={cancelMutation.isPending}
          error={
            cancelMutation.isError
              ? 'Could not cancel this order — its status may have just changed. Refresh and try again.'
              : null
          }
        />
      )}
    </div>
  )
}

function formatDate(iso: string) {
  try {
    return new Date(iso).toLocaleString(undefined, {
      dateStyle: 'medium',
      timeStyle: 'short',
    })
  } catch {
    return iso
  }
}

function OrdersTable({
  orders,
  onCancel,
}: {
  orders: OrderResponse[]
  onCancel: (order: OrderResponse) => void
}) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead>
          <tr className="border-b border-border text-text-muted">
            <th className="py-2 pr-4 font-medium">Order</th>
            <th className="py-2 pr-4 font-medium">Created</th>
            <th className="py-2 pr-4 font-medium">Region</th>
            <th className="py-2 pr-4 font-medium">Status</th>
            <th className="py-2 pr-4 font-medium">Vendor</th>
            <th className="py-2 pr-4 font-medium">Items</th>
            <th className="py-2 pr-4 font-medium" />
          </tr>
        </thead>
        <tbody>
          {orders.map((order) => (
            <tr key={order.id} className="border-b border-border/60 last:border-0">
              <td className="py-2 pr-4 font-mono text-xs text-text-muted">{order.id.slice(0, 8)}</td>
              <td className="py-2 pr-4 whitespace-nowrap text-text-muted">{formatDate(order.createdAt)}</td>
              <td className="py-2 pr-4">{order.destinationRegion}</td>
              <td className="py-2 pr-4">
                <StatusBadge status={order.status} />
              </td>
              <td className="py-2 pr-4 font-mono text-xs text-text-muted">{order.vendorId?.slice(0, 8) ?? '-'}</td>
              <td className="py-2 pr-4 text-text-muted">{order.items.map((i) => `${i.skuId} ×${i.quantity}`).join(', ')}</td>
              <td className="py-2 pr-4 text-right">
                {CANCELLABLE_STATUSES.has(order.status) && (
                  <button
                    type="button"
                    onClick={() => onCancel(order)}
                    className="rounded-md border border-border px-2.5 py-1 text-xs font-medium text-text-muted transition-colors hover:border-danger/40 hover:bg-danger/10 hover:text-danger"
                  >
                    Cancel
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function CancelOrderModal({
  order,
  onClose,
  onConfirm,
  submitting,
  error,
}: {
  order: OrderResponse
  onClose: () => void
  onConfirm: (reason: string) => void
  submitting: boolean
  error: string | null
}) {
  const [reason, setReason] = useState('')

  return (
    <div className="fixed inset-0 z-20 flex animate-[fadeIn_150ms_ease-out] items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
      <Card className="w-full max-w-md animate-[fadeInScale_180ms_ease-out] shadow-xl">
        <h2 className="text-lg font-semibold text-text">Cancel order {order.id.slice(0, 8)}?</h2>
        <p className="mt-1 text-sm text-text-muted">
          This stops the order permanently — it can't be un-cancelled. Anything already reserved for it will be
          released.
        </p>
        <form
          onSubmit={(e) => {
            e.preventDefault()
            onConfirm(reason.trim() || 'Cancelled by planner')
          }}
          className="mt-4 flex flex-col gap-3"
        >
          <label className="flex flex-col gap-1.5 text-sm">
            <span className="font-medium text-text">Reason (optional)</span>
            <input
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="e.g. duplicate order"
              className="rounded-lg border border-border bg-bg px-3 py-2 text-text outline-none focus:border-accent focus:ring-1 focus:ring-accent"
            />
          </label>

          {error && <p className="text-sm text-danger">{error}</p>}

          <div className="mt-2 flex justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-border px-4 py-2 text-sm font-medium text-text hover:bg-border/40"
            >
              Keep order
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="rounded-lg bg-danger px-4 py-2 text-sm font-medium text-white shadow-sm shadow-danger/20 transition-all hover:opacity-90 active:scale-[0.98] disabled:opacity-60"
            >
              {submitting ? 'Cancelling…' : 'Cancel order'}
            </button>
          </div>
        </form>
      </Card>
    </div>
  )
}

function CreateOrderModal({
  onClose,
  onSubmit,
  submitting,
  error,
}: {
  onClose: () => void
  onSubmit: (request: ordersApi.CreateOrderRequest) => void
  submitting: boolean
  error: string | null
}) {
  const { user } = useAuth()
  const [region, setRegion] = useState('india-west')
  const [skuId, setSkuId] = useState('sku-1')
  const [quantity, setQuantity] = useState(1)

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    onSubmit({
      requestedBy: user?.username ?? 'planner',
      destinationRegion: region,
      items: [{ skuId, quantity }],
    })
  }

  return (
    <div className="fixed inset-0 z-20 flex animate-[fadeIn_150ms_ease-out] items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
      <Card className="w-full max-w-md animate-[fadeInScale_180ms_ease-out] shadow-xl">
        <h2 className="text-lg font-semibold text-text">New order</h2>
        <form onSubmit={handleSubmit} className="mt-4 flex flex-col gap-3">
          <label className="flex flex-col gap-1.5 text-sm">
            <span className="font-medium text-text">Destination region</span>
            <input
              value={region}
              onChange={(e) => setRegion(e.target.value)}
              required
              className="rounded-lg border border-border bg-bg px-3 py-2 text-text outline-none focus:border-accent focus:ring-1 focus:ring-accent"
            />
          </label>
          <label className="flex flex-col gap-1.5 text-sm">
            <span className="font-medium text-text">SKU</span>
            <input
              value={skuId}
              onChange={(e) => setSkuId(e.target.value)}
              required
              className="rounded-lg border border-border bg-bg px-3 py-2 text-text outline-none focus:border-accent focus:ring-1 focus:ring-accent"
            />
          </label>
          <label className="flex flex-col gap-1.5 text-sm">
            <span className="font-medium text-text">Quantity</span>
            <input
              type="number"
              min={1}
              value={quantity}
              onChange={(e) => setQuantity(Number(e.target.value))}
              required
              className="rounded-lg border border-border bg-bg px-3 py-2 text-text outline-none focus:border-accent focus:ring-1 focus:ring-accent"
            />
          </label>

          {error && <p className="text-sm text-danger">{error}</p>}

          <div className="mt-2 flex justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-border px-4 py-2 text-sm font-medium text-text hover:bg-border/40"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="rounded-lg bg-accent px-4 py-2 text-sm font-medium text-accent-fg shadow-sm shadow-accent/20 transition-all hover:opacity-90 active:scale-[0.98] disabled:opacity-60"
            >
              {submitting ? 'Creating…' : 'Create order'}
            </button>
          </div>
        </form>
      </Card>
    </div>
  )
}
