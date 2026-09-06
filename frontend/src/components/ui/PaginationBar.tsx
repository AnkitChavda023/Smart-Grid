interface PaginationBarProps {
  currentPage: number
  totalPages: number
  totalItems: number
  pageSize: number
  onPageChange: (page: number) => void
  onPageSizeChange?: (pageSize: number) => void
  pageSizeOptions?: number[]
  itemLabel?: string
}

export function PaginationBar({
  currentPage,
  totalPages,
  totalItems,
  pageSize,
  onPageChange,
  onPageSizeChange,
  pageSizeOptions = [5, 10, 20],
  itemLabel = 'items',
}: PaginationBarProps) {
  if (totalItems <= 0) return null

  const startIndex = (currentPage - 1) * pageSize
  const endIndex = Math.min(startIndex + pageSize, totalItems)

  return (
    <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-border pt-4 text-xs">
      <div className="flex flex-wrap items-center gap-3 text-text-muted">
        <span>
          Showing <strong className="text-text">{totalItems === 0 ? 0 : startIndex + 1}</strong> to{' '}
          <strong className="text-text">{endIndex}</strong> of{' '}
          <strong className="text-text">{totalItems}</strong> {itemLabel}
        </span>
        {onPageSizeChange && (
          <label className="flex items-center gap-1.5">
            <span>Rows per page:</span>
            <select
              value={pageSize}
              onChange={(e) => {
                onPageSizeChange(Number(e.target.value))
                onPageChange(1)
              }}
              className="rounded border border-border bg-bg px-2 py-1 text-xs text-text outline-none focus:border-accent"
            >
              {pageSizeOptions.map((opt) => (
                <option key={opt} value={opt}>
                  {opt}
                </option>
              ))}
            </select>
          </label>
        )}
      </div>

      <div className="flex items-center gap-1.5">
        <button
          type="button"
          disabled={currentPage <= 1}
          onClick={() => onPageChange(Math.max(1, currentPage - 1))}
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
                onClick={() => onPageChange(pNum)}
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
          onClick={() => onPageChange(Math.min(totalPages, currentPage + 1))}
          className="rounded-lg border border-border bg-surface px-3 py-1.5 text-xs font-medium text-text transition-colors hover:bg-border/40 disabled:opacity-40 disabled:pointer-events-none"
        >
          Next →
        </button>
      </div>
    </div>
  )
}
