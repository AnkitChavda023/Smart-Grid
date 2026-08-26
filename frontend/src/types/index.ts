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

export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'CLOSED'

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

export interface VendorResponse {
  id: string
  name: string
  region: string
  latitude: number | null
  longitude: number | null
  capabilities: string
  suspended: boolean
  reliabilityScore: number
}

export interface VendorDocument {
  id: string
  name: string
  region: string
  capabilities: string
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

export type RerouteStatus = 'PUBLISHED' | 'ESCALATED' | 'APPROVED'

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
