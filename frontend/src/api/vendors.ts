import { apiClient } from './client'
import type { VendorDocument, VendorRankingResult, VendorResponse } from '../types'

export function getVendor(id: string) {
  return apiClient.get<VendorResponse>(`/vendors/${id}`).then((r) => r.data)
}

export function topVendorsForSku(sku: string, k = 5) {
  return apiClient.get<VendorRankingResult[]>('/vendors/top', { params: { sku, k } }).then((r) => r.data)
}

export function searchVendors(q?: string) {
  return apiClient.get<VendorDocument[]>('/vendors/search', { params: { q } }).then((r) => r.data)
}
