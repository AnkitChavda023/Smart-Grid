export type Role = 'SUPPLIER' | 'PLANNER' | 'ADMIN'

export interface TokenPair {
  accessToken: string
  refreshToken: string
  expiresInSeconds: number
}

export interface OrderItem {
  skuId: string
  quantity: number
}

export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED' | 'CLOSED'

export interface OrderResponse {
  id: string
  status: OrderStatus
  version: number
  requestedBy: string
  destinationRegion: string
  items: OrderItem[]
  vendorId: string | null
  quoteId: string | null
  createdAt: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
}

export interface VendorSkuItem {
  id?: string
  skuId: string
  price: number
  leadTimeDays: number
}

export interface VendorHealthReportItem {
  id: string
  vendorId: string
  trendDirection: 'IMPROVING' | 'STABLE' | 'DEGRADING'
  trendSlope: number
  averageLeadTimeDays: number
  breachCount: number
  summary: string
  createdAt: string
}

export interface VendorResponse {
  id: string
  name: string
  region: string
  latitude: number | null
  longitude: number | null
  capabilities: string
  contact?: string
  category?: string
  certifications?: string
  suspended: boolean
  reliabilityScore: number
  skus?: VendorSkuItem[]
  scoreFormula?: string
}

export interface VendorDocument {
  id: string
  name: string
  region: string
  capabilities: string
  contact?: string
  category?: string
  certifications?: string
}

export interface VendorRankingResult {
  vendorId: string
  vendorName: string
  compositeScore: number
  price: number
  leadTimeDays: number
  reliabilityScore: number
}

export interface DisruptionSummary {
  region: string
  windowType: 'TUMBLING' | 'HOPPING'
  windowStart: number
  windowEnd: number
  count: number
}

export interface OrderThroughputPoint {
  stage: 'CREATED' | 'FULFILLED' | 'DELIVERED'
  region: string
  windowStart: number
  windowEnd: number
  count: number
}

export interface VendorPerformance {
  vendorId: string
  slaBreachCount: number | null
  latestScore: number | null
}

export interface RerouteSuccessRate {
  successCount: number
  escalationCount: number
  successRate: number
}

export interface OrderMetricsSummary {
  today: number
  thisWeek: number
  thisMonth: number
  total: number
}

export interface DisruptionWeeklyMetrics {
  count: number
  severityBreakdown: {
    CRITICAL: number
    HIGH: number
    MEDIUM: number
    LOW: number
  }
  confidences: number[]
}

export interface LeadTimeTrendPoint {
  date: string
  averageLeadTimeDays: number
}

export interface VendorSlaBreachRate {
  vendorId: string
  vendorName?: string
  totalOrders: number
  breachCount: number
  breachRatePct: number
  reliabilityScore?: number
}

export interface RerouteKpis {
  successCount: number
  escalationCount: number
  successRate: number
  confidences: number[]
}

export interface ConfidenceHistogramBucket {
  bucket: string
  range: string
  count: number
  percentage: number
}

export interface NotificationPushMessage {
  notificationId: string | null
  title: string
  body: string
  relatedOrderId: string | null
}

export type DisruptionStatus = 'PUBLISHED' | 'PENDING_REVIEW'

export interface Disruption {
  id: string
  vendorId: string
  region: string
  affectedSkus: string[]
  confidence: number
  reasoningTrace: string
  status: DisruptionStatus
  createdAt: string
}

export type RerouteStatus = 'PUBLISHED' | 'ESCALATED' | 'APPROVED' | 'REJECTED' | 'MODIFIED'

export interface Reroute {
  id: string
  orderId: string
  disruptionId: string | null
  selectedVendorId: string | null
  quoteId: string | null
  confidence: number
  agentTraceJson: string
  status: RerouteStatus
  createdAt: string
}

export interface ContractDraft {
  id: string
  vendorId: string
  existingContractId: string | null
  proposedTerms: string
  summary: string
  status: 'DRAFT' | 'SUBMITTED' | 'REJECTED'
  createdAt: string
}

export interface DagTraceNode {
  stepIndex: number
  toolName: string
  input: string
  output: string
  latencyMs: number
  llmReasoning: string
}

export interface VendorEvaluation {
  id: string
  vendorId: string
  trendDirection: string
  trendSlope: number
  averageLeadTimeDays: number
  breachCount: number
  confidence: number
  summary: string
  draftId?: string
  status: 'PUBLISHED' | 'PENDING_REVIEW'
  createdAt: string
}

export interface BreachRiskAssessment {
  id: string
  vendorId: string
  breachCountInWindow: number
  breachProbability: number
  confidence: number
  summary: string
  restockTriggered: boolean
  status: 'PUBLISHED' | 'PENDING_REVIEW'
  createdAt: string
}

export interface DemandForecast {
  id: string
  skuId: string
  horizonDays: number
  p10: number
  p50: number
  p90: number
  confidence: number
  summary: string
  createdAt: string
}

export interface NegotiationRun {
  id: string
  vendorId: string
  draftId: string | null
  clauseSimilarity: number
  confidence: number
  summary: string
  status: 'PUBLISHED' | 'PENDING_REVIEW'
  createdAt: string
}

export interface KafkaTriggerEvent {
  topic: string
  eventType: string
  timestamp: string
  payload: string
}

export interface RetrievedRagChunk {
  source: string
  sourceId: string
  similarityScore: number
  content: string
}

export interface McpToolCallTrace {
  stepIndex: number
  toolName: string
  input: string
  output: string
  latencyMs: number
  llmReasoning: string
}

export interface AgentFinalDecision {
  confidenceScore: number
  outcome: string
  affectedEntities: Record<string, any>
}

export interface AgentDecisionTimelineData {
  agentName: string
  decisionId: string
  status: string
  confidence: number
  kafkaTrigger: KafkaTriggerEvent
  ragChunks: RetrievedRagChunk[]
  toolCalls: McpToolCallTrace[]
  llmReasoning: string
  finalDecision: AgentFinalDecision
}

