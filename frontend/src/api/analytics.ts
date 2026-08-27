import { apiClient } from './client'
import type { DisruptionSummary, OrderThroughputPoint, RerouteSuccessRate, VendorPerformance } from '../types'

export function disruptionsSummary() {
  return apiClient.get<DisruptionSummary[]>('/analytics/disruptions/summary').then((r) => r.data)
}

export function ordersThroughput() {
  return apiClient.get<OrderThroughputPoint[]>('/analytics/orders/throughput').then((r) => r.data)
}

export function vendorsPerformance(period = '7d') {
  return apiClient
    .get<VendorPerformance[]>('/analytics/vendors/performance', { params: { period } })
    .then((r) => r.data)
}

export function reroutesSuccessRate() {
  return apiClient.get<RerouteSuccessRate>('/analytics/reroutes/success-rate').then((r) => r.data)
}
