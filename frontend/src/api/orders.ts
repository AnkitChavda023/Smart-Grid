import { apiClient } from './client'
import type { OrderItem, OrderResponse, PageResponse } from '../types'

export interface CreateOrderRequest {
  requestedBy: string
  destinationRegion: string
  items: OrderItem[]
}

export function listOrders(status?: string, page = 0, size = 20) {
  return apiClient
    .get<PageResponse<OrderResponse>>('/orders', { params: { status, page, size } })
    .then((r) => r.data)
}

export function getOrder(id: string) {
  return apiClient.get<OrderResponse>(`/orders/${id}`).then((r) => r.data)
}

export function createOrder(request: CreateOrderRequest) {
  return apiClient.post<OrderResponse>('/orders', request).then((r) => r.data)
}

export function cancelOrder(id: string, reason?: string) {
  return apiClient.patch<void>(`/orders/${id}/cancel`, reason ? { reason } : undefined)
}
