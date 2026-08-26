import { Card } from './Card'

interface KpiCardProps {
  label: string
  value: string | number
  hint?: string
  tone?: 'default' | 'success' | 'warning' | 'danger' | 'info'
}

const TONE_CLASSES: Record<NonNullable<KpiCardProps['tone']>, string> = {
  default: 'text-text',
  success: 'text-success',
  warning: 'text-warning',
  danger: 'text-danger',
  info: 'text-info',
}

const TONE_BORDER_CLASSES: Record<NonNullable<KpiCardProps['tone']>, string> = {
  default: 'border-t-border',
  success: 'border-t-success',
  warning: 'border-t-warning',
  danger: 'border-t-danger',
  info: 'border-t-info',
}

export function KpiCard({ label, value, hint, tone = 'default' }: KpiCardProps) {
  return (
    <Card className={`border-t-2 ${TONE_BORDER_CLASSES[tone]} hover:shadow-md`}>
      <p className="text-sm font-medium text-text-muted">{label}</p>
      <p className={`mt-2 text-3xl font-semibold tabular-nums ${TONE_CLASSES[tone]}`}>{value}</p>
      {hint && <p className="mt-1 text-xs text-text-muted">{hint}</p>}
    </Card>
  )
}
