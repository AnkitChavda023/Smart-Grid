import { apiClient } from './client'

export interface SlaTerm {
  metricName: string
  thresholdValue: number
  penaltyPerBreach: number
}

export interface ContractResponse {
  id: string
  vendorId: string
  terms: string
  startDate: string
  endDate: string
  active: boolean
  slaTerms?: SlaTerm[]
}

export interface SlaBreachResponse {
  id: string
  vendorId: string
  contractId: string
  metricName: string
  actualValue: number
  thresholdValue: number
  penaltyAmount: number
  status: string
  detectedAt: string
}

export interface ContractDraftResponse {
  id: string
  vendorId: string
  existingContractId: string | null
  proposedTerms: string
  summary: string
  status: 'DRAFT' | 'SUBMITTED' | 'REJECTED'
  createdAt: string
}

export function listContracts() {
  return apiClient
    .get<ContractResponse[]>('/contracts')
    .then((r) => r.data ?? [])
    .catch(() => [])
}

export function getActiveContracts(vendorId: string) {
  return apiClient.get<ContractResponse[]>(`/contracts/${vendorId}/active`).then((r) => r.data)
}

export function getSlaBreaches(vendorId?: string) {
  return apiClient
    .get<SlaBreachResponse[]>('/sla-breaches', { params: vendorId ? { vendorId } : {} })
    .then((r) => r.data ?? [])
    .catch(() => [])
}

export function listContractDrafts() {
  return apiClient
    .get<ContractDraftResponse[]>('/contracts/drafts')
    .then((r) => r.data ?? [])
    .catch(() => [])
}

export function getContractDraft(id: string) {
  return apiClient.get<ContractDraftResponse>(`/contracts/drafts/${id}`).then((r) => r.data)
}

export function submitContractDraft(id: string) {
  return apiClient.post<ContractResponse>(`/contracts/drafts/${id}/submit`).then((r) => r.data)
}

export function rejectContractDraft(id: string) {
  return apiClient.post<ContractDraftResponse>(`/contracts/drafts/${id}/reject`).then((r) => r.data)
}

export function modifyContractDraft(id: string, proposedTerms: string, summary?: string) {
  return apiClient.put<ContractDraftResponse>(`/contracts/drafts/${id}`, { proposedTerms, summary }).then((r) => r.data)
}
