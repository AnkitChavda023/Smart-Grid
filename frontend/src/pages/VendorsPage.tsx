import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import * as vendorsApi from '../api/vendors'
import { Card } from '../components/ui/Card'
import { LoadingState, ErrorState, EmptyState } from '../components/ui/StateViews'

export function VendorsPage() {
  const [searchTerm, setSearchTerm] = useState('')
  const [sku, setSku] = useState('sku-1')

  const searchQuery = useQuery({
    queryKey: ['vendors', 'search', searchTerm],
    queryFn: () => vendorsApi.searchVendors(searchTerm || undefined),
  })

  const topQuery = useQuery({
    queryKey: ['vendors', 'top', sku],
    queryFn: () => vendorsApi.topVendorsForSku(sku, 5),
    enabled: sku.length > 0,
  })

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-semibold text-text">Vendors</h1>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card>
          <h2 className="mb-3 text-sm font-medium text-text-muted">Search catalog</h2>
          <input
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Name or capability…"
            className="mb-4 w-full rounded-lg border border-border bg-bg px-3 py-2 text-sm text-text outline-none focus:border-accent focus:ring-1 focus:ring-accent"
          />
          {searchQuery.isLoading && <LoadingState label="Searching…" />}
          {searchQuery.isError && <ErrorState message="Search failed." onRetry={() => searchQuery.refetch()} />}
          {searchQuery.data && searchQuery.data.length === 0 && (
            <EmptyState title="No vendors found" description="Try a different search term." />
          )}
          {searchQuery.data && searchQuery.data.length > 0 && (
            <ul className="flex flex-col divide-y divide-border">
              {searchQuery.data.map((vendor) => (
                <li key={vendor.id} className="py-2.5 text-sm">
                  <p className="font-medium text-text">{vendor.name}</p>
                  <p className="text-text-muted">
                    {vendor.region} | {vendor.capabilities}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card>
          <h2 className="mb-3 text-sm font-medium text-text-muted">Top vendors by SKU</h2>
          <input
            value={sku}
            onChange={(e) => setSku(e.target.value)}
            placeholder="SKU id…"
            className="mb-4 w-full rounded-lg border border-border bg-bg px-3 py-2 text-sm text-text outline-none focus:border-accent focus:ring-1 focus:ring-accent"
          />
          {topQuery.isLoading && <LoadingState label="Ranking vendors…" />}
          {topQuery.isError && <ErrorState message="Couldn't rank vendors." onRetry={() => topQuery.refetch()} />}
          {topQuery.data && topQuery.data.length === 0 && (
            <EmptyState title="No vendors carry this SKU" description="Try a different SKU id." />
          )}
          {topQuery.data && topQuery.data.length > 0 && (
            <ol className="flex flex-col divide-y divide-border">
              {topQuery.data.map((result, index) => (
                <li key={result.vendorId} className="flex items-center justify-between py-2.5 text-sm">
                  <div>
                    <p className="font-medium text-text">
                      #{index + 1} {result.vendorName}
                    </p>
                    <p className="text-text-muted">
                      ${result.price.toFixed(2)} | {result.leadTimeDays}d lead time | reliability{' '}
                      {(result.reliabilityScore * 100).toFixed(0)}%
                    </p>
                  </div>
                  <span className="tabular-nums text-text-muted">{result.compositeScore.toFixed(2)}</span>
                </li>
              ))}
            </ol>
          )}
        </Card>
      </div>
    </div>
  )
}
