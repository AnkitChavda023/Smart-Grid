import { apiClient } from './client'
import type { VendorHealthReportItem, VendorRankingResult, VendorResponse, VendorSkuItem } from '../types'

export interface TopKResponseWithLatency {
  results: VendorRankingResult[]
  latencyMs: number
}

export interface CreateVendorPayload {
  name: string
  region: string
  contact?: string
  category?: string
  certifications?: string
  capabilities?: string
  skus?: { skuId: string; price: number; leadTimeDays: number }[]
}

export function listVendors() {
  return apiClient.get<VendorResponse[]>('/vendors').then((r) => r.data)
}

export function getVendor(id: string) {
  return apiClient.get<VendorResponse>(`/vendors/${id}`).then((r) => r.data)
}

export function getVendorHealthReports(vendorId: string) {
  return apiClient.get<VendorHealthReportItem[]>(`/vendors/${vendorId}/health-reports`).then((r) => r.data)
}

export function getVendorSkus(vendorId: string) {
  return apiClient.get<VendorSkuItem[]>(`/vendors/${vendorId}/skus`).then((r) => r.data)
}

export async function topVendorsForSku(sku: string, k = 5): Promise<TopKResponseWithLatency> {
  const start = performance.now()
  const response = await apiClient.get<VendorRankingResult[]>('/vendors/top', { params: { sku, k } })
  const latencyMs = Math.round(performance.now() - start)
  return {
    results: response.data,
    latencyMs,
  }
}

export function searchVendors(params?: {
  q?: string
  category?: string
  region?: string
  certification?: string
}) {
  return apiClient.get<VendorResponse[]>('/vendors/search', { params }).then((r) => r.data)
}

export function createVendor(payload: CreateVendorPayload) {
  return apiClient.post<VendorResponse>('/vendors', payload).then((r) => r.data)
}
