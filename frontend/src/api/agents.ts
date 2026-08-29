import { apiClient } from './client'
import type {
  BreachRiskAssessment,
  DagTraceNode,
  DemandForecast,
  Disruption,
  NegotiationRun,
  Reroute,
  VendorEvaluation,
} from '../types'

export function activeDisruptions() {
  return apiClient.get<Disruption[]>('/disruptions/active').then((r) => r.data)
}

export function reroutesForDisruption(disruptionId: string) {
  return apiClient.get<Reroute[]>('/reroutes', { params: { disruptionId } }).then((r) => r.data)
}

export function rerouteTrace(rerouteId: string) {
  return apiClient.get<DagTraceNode[]>(`/reroutes/${rerouteId}/trace`).then((r) => r.data)
}

export function vendorEvaluations(vendorId: string) {
  return apiClient.get<VendorEvaluation[]>(`/vendor-evaluations/${vendorId}`).then((r) => r.data)
}

export function simulateVendorEvaluation(vendorId: string) {
  return apiClient.post<VendorEvaluation>('/vendor-evaluations/simulate', { vendorId }).then((r) => r.data)
}

export function breachAssessments(vendorId: string) {
  return apiClient.get<BreachRiskAssessment[]>(`/breach-assessments/${vendorId}`).then((r) => r.data)
}

export function simulateBreachAssessment(vendorId: string) {
  return apiClient.post<BreachRiskAssessment>('/breach-assessments/simulate', { vendorId }).then((r) => r.data)
}

export function demandForecasts(skuId: string) {
  return apiClient.get<DemandForecast[]>(`/demand-forecasts/${skuId}`).then((r) => r.data)
}

export function simulateDemandForecast(skuId: string) {
  return apiClient.post<DemandForecast[]>('/demand-forecasts/simulate', { skuId }).then((r) => r.data)
}

export function negotiationRuns(vendorId: string) {
  return apiClient.get<NegotiationRun[]>(`/negotiations/${vendorId}`).then((r) => r.data)
}

export function simulateNegotiation(vendorId: string) {
  return apiClient.post<NegotiationRun>('/negotiations/simulate', { vendorId }).then((r) => r.data)
}
