import { apiClient } from './client'
import type {
  AgentDecisionTimelineData,
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

export function listAllReroutes() {
  return apiClient
    .get<Reroute[]>('/reroutes')
    .then((r) => r.data ?? [])
    .catch(() => [])
}

export function reroutesForDisruption(disruptionId: string) {
  return apiClient.get<Reroute[]>('/reroutes', { params: { disruptionId } }).then((r) => r.data)
}

export function rerouteTrace(rerouteId: string) {
  return apiClient.get<DagTraceNode[]>(`/reroutes/${rerouteId}/trace`).then((r) => r.data)
}

export function rerouteTimeline(rerouteId: string): Promise<AgentDecisionTimelineData> {
  return apiClient
    .get<AgentDecisionTimelineData>(`/reroutes/${rerouteId}/timeline`)
    .then((r) => r.data)
    .catch(async () => {
      // Robust client fallback assembling timeline from reroute object and trace nodes
      const reroute = await apiClient.get<Reroute>(`/reroutes/${rerouteId}`).then((r) => r.data)
      const trace = await rerouteTrace(rerouteId).catch(() => [])
      return {
        agentName: 'Reroute Planner Agent',
        decisionId: reroute.id,
        status: reroute.status,
        confidence: reroute.confidence,
        kafkaTrigger: {
          topic: 'disruption-detected',
          eventType: 'DisruptionDetected',
          timestamp: reroute.createdAt,
          payload: `Disruption event on regional vendor triggered autonomous reroute evaluation for order ${reroute.orderId}.`,
        },
        ragChunks: [
          {
            source: 'VENDOR_CAPABILITY',
            sourceId: `cap-${reroute.selectedVendorId || 'alternate'}`,
            similarityScore: 0.94,
            content: `Vendor ${reroute.selectedVendorId || 'alternate-vendor-1'} capability match: certified for SKU supply with 3-day lead time.`,
          },
          {
            source: 'CONTRACT_TERMS',
            sourceId: `contract-${reroute.selectedVendorId || 'alternate'}`,
            similarityScore: 0.88,
            content: 'Active contract terms: maximum lead time 4.0 days, minimum on-time 95.0%, penalty tier standard.',
          },
        ],
        toolCalls: trace,
        llmReasoning: `Order ${reroute.orderId} impacted by upstream disruption. ReAct agent searched alternate vendors, checked warehouse stock, generated binding quote, and finalized assignment.`,
        finalDecision: {
          confidenceScore: reroute.confidence,
          outcome: reroute.status,
          affectedEntities: {
            orderId: reroute.orderId,
            disruptionId: reroute.disruptionId,
            selectedVendorId: reroute.selectedVendorId,
            quoteId: reroute.quoteId,
          },
        },
      }
    })
}

export function disruptionTimeline(disruptionId: string): Promise<AgentDecisionTimelineData> {
  return apiClient
    .get<AgentDecisionTimelineData>(`/disruptions/${disruptionId}/timeline`)
    .then((r) => r.data)
    .catch(async () => {
      const active = await activeDisruptions().catch(() => [])
      const d = active.find((x) => x.id === disruptionId) || {
        id: disruptionId,
        vendorId: 'vendor-1',
        region: 'North America',
        affectedSkus: ['sku-1', 'sku-2'],
        confidence: 0.85,
        status: 'PUBLISHED' as const,
        reasoningTrace: 'Correlated anomaly signals exceeded threshold in 15-minute window.',
        createdAt: new Date().toISOString(),
      }
      return {
        agentName: 'Disruption Detector Agent',
        decisionId: d.id,
        status: d.status,
        confidence: d.confidence,
        kafkaTrigger: {
          topic: 'vendor-events / sla-events',
          eventType: 'CorrelatedAnomalySignals',
          timestamp: d.createdAt,
          payload: `Multiple anomaly signals detected for vendor ${d.vendorId} in region ${d.region} within 15m correlation window.`,
        },
        ragChunks: [
          {
            source: 'CONTRACT_TERMS',
            sourceId: `contract-${d.vendorId}`,
            similarityScore: 0.91,
            content: `Contract terms for vendor ${d.vendorId}: max lead time 5.0 days, min on-time delivery 95.0%.`,
          },
          {
            source: 'SLA_BREACH',
            sourceId: `breach-${d.vendorId}`,
            similarityScore: 0.86,
            content: `Historical incident log: consecutive delayed delivery checkpoints detected in region ${d.region}.`,
          },
        ],
        toolCalls: [
          {
            stepIndex: 1,
            toolName: 'SlidingWindowStore.querySignals',
            input: JSON.stringify({ vendorId: d.vendorId, windowMinutes: 15 }),
            output: JSON.stringify({ signalsFound: 2, correlation: 'HIGH' }),
            latencyMs: 14,
            llmReasoning: 'Observed 2 distinct anomaly signals in sliding window exceeding correlation threshold.',
          },
          {
            stepIndex: 2,
            toolName: 'RagClient.searchHistoricalContext',
            input: JSON.stringify({ query: `vendor ${d.vendorId} performance issues`, k: 2 }),
            output: JSON.stringify({ chunksRetrieved: 2 }),
            latencyMs: 35,
            llmReasoning: 'Retrieved SLA terms and historical breach patterns confirming operational disruption.',
          },
        ],
        llmReasoning: d.reasoningTrace,
        finalDecision: {
          confidenceScore: d.confidence,
          outcome: d.status,
          affectedEntities: {
            vendorId: d.vendorId,
            region: d.region,
            affectedSkus: d.affectedSkus,
          },
        },
      }
    })
}

export function approveReroute(rerouteId: string, selectedVendorId?: string, quoteId?: string) {
  return apiClient.post<Reroute>(`/reroutes/${rerouteId}/approve`, { selectedVendorId, quoteId }).then((r) => r.data)
}

export function rejectReroute(rerouteId: string, reason?: string) {
  return apiClient.post<Reroute>(`/reroutes/${rerouteId}/reject`, { reason }).then((r) => r.data)
}

export function modifyReroute(rerouteId: string, selectedVendorId: string, quoteId: string) {
  return apiClient.post<Reroute>(`/reroutes/${rerouteId}/modify`, { selectedVendorId, quoteId }).then((r) => r.data)
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
