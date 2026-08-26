import { apiClient } from './client'
import type { Role, TokenPair } from '../types'

export function login(username: string, password: string) {
  return apiClient.post<TokenPair>('/auth/login', { username, password }).then((r) => r.data)
}

export function register(username: string, password: string, role: Role) {
  return apiClient.post<void>('/auth/register', { username, password, role })
}

export function logout(refreshToken: string) {
  return apiClient.post<void>('/auth/logout', { refreshToken })
}
