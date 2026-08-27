import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query'
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

const ORDERS_QUERY_KEY = ['orders'] as const

function useOrderLiveUpdates(orders: OrderResponse[] | undefined) {
  const { subscribeToOrder, connected } = useNotifications()
  const queryClient = useQueryClient()

  useEffect(() => {
    if (!connected || !orders?.length) return

    const unsubscribers = orders
      .filter((order) => order.status === 'PENDING')
      .map((order) =>
        subscribeToOrder(order.id, async () => {
          const updated = await getOrder(order.id)
          queryClient.setQueryData<{ content: OrderResponse[] } | undefined>(ORDERS_QUERY_KEY, (current) => {
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
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ORDERS_QUERY_KEY,
    queryFn: () => ordersApi.listOrders(),
  })
  const [showCreate, setShowCreate] = useState(false)

  useOrderLiveUpdates(data?.content)

  const createMutation = useMutation({
    mutationFn: ordersApi.createOrder,
    onSuccess: (created) => {
      queryClient.setQueryData<{ content: OrderResponse[]; page: number; size: number; totalElements: number } | undefined>(
        ORDERS_QUERY_KEY,
        (current) =>
          current
            ? { ...current, content: [created, ...current.content], totalElements: current.totalElements + 1 }
            : { content: [created], page: 0, size: 20, totalElements: 1 },
      )
      setShowCreate(false)
    },
  })

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-text">Orders</h1>
        <button
          type="button"
          onClick={() => setShowCreate(true)}
          className="rounded-lg bg-accent px-4 py-2 text-sm font-medium text-accent-fg shadow-sm shadow-accent/20 transition-all hover:opacity-90 active:scale-[0.98]"
        >
          New order
        </button>
      </div>

      {isLoading && <LoadingState label="Loading orders…" />}
      {isError && <ErrorState message="Couldn't load orders." onRetry={() => refetch()} />}

      {!isLoading && !isError && (
        <>
          <Card>
            <h2 className="mb-3 text-sm font-medium text-text-muted">Order graph</h2>
            {data?.content.length ? (
              <OrderGraph orders={data.content} />
            ) : (
              <EmptyState title="No orders yet" description="Create an order to see it appear in the graph." />
            )}
          </Card>

          <Card>
            <h2 className="mb-3 text-sm font-medium text-text-muted">Order list</h2>
            {data?.content.length ? (
              <OrdersTable orders={data.content} />
            ) : (
              <EmptyState title="No orders yet" description="Orders you create will show up here." />
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
    </div>
  )
}

function OrdersTable({ orders }: { orders: OrderResponse[] }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead>
          <tr className="border-b border-border text-text-muted">
            <th className="py-2 pr-4 font-medium">Order</th>
            <th className="py-2 pr-4 font-medium">Region</th>
            <th className="py-2 pr-4 font-medium">Status</th>
            <th className="py-2 pr-4 font-medium">Vendor</th>
            <th className="py-2 pr-4 font-medium">Items</th>
          </tr>
        </thead>
        <tbody>
          {orders.map((order) => (
            <tr key={order.id} className="border-b border-border/60 last:border-0">
              <td className="py-2 pr-4 font-mono text-xs text-text-muted">{order.id.slice(0, 8)}</td>
              <td className="py-2 pr-4">{order.destinationRegion}</td>
              <td className="py-2 pr-4">
                <StatusBadge status={order.status} />
              </td>
              <td className="py-2 pr-4 font-mono text-xs text-text-muted">{order.vendorId?.slice(0, 8) ?? '-'}</td>
              <td className="py-2 pr-4 text-text-muted">{order.items.map((i) => `${i.skuId} ×${i.quantity}`).join(', ')}</td>
            </tr>
          ))}
        </tbody>
      </table>
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
  const [region, setRegion] = useState('us-east')
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
