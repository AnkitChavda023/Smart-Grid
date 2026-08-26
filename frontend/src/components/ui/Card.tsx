import type { ReactNode } from 'react'

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return (
    <div className={`rounded-xl border border-border bg-surface p-5 shadow-sm shadow-black/[0.03] transition-shadow duration-200 ${className}`}>
      {children}
    </div>
  )
}
