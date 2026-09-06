import { apiClient } from './client'

export interface ShipmentResponse {
  id: string
  orderId: string
  status: 'DISPATCHED' | 'IN_TRANSIT' | 'DELIVERED' | 'DELAYED'
  etaMinutes: number | null
  checkpointCount: number
  createdAt: string
}

export interface CheckpointRequest {
  latitude: number
  longitude: number
}

export interface WarehouseGeo {
  id: string
  name: string
  latitude: number
  longitude: number
}

export function listShipments() {
  return apiClient
    .get<ShipmentResponse[]>('/shipments')
    .then((r) => r.data ?? [])
    .catch(() => [])
}

export function getShipment(id: string) {
  return apiClient.get<ShipmentResponse>(`/shipments/${id}`).then((r) => r.data)
}

export function recordCheckpoint(shipmentId: string, latitude: number, longitude: number) {
  return apiClient
    .post<ShipmentResponse>(`/shipments/${shipmentId}/checkpoint`, { latitude, longitude })
    .then((r) => r.data)
}

export function listWarehouses() {
  return apiClient.get<WarehouseGeo[]>('/warehouses').then((r) => r.data)
}

export function markDelivered(shipmentId: string) {
  return apiClient.post<void>(`/shipments/${shipmentId}/deliver`).then((r) => r.data)
}

