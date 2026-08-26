import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from './authStore'
import type { TokenPair } from '../types'

export const GATEWAY_BASE_URL = 'http://localhost:8000'

export const apiClient = axios.create({ baseURL: GATEWAY_BASE_URL })

const refreshClient = axios.create({ baseURL: GATEWAY_BASE_URL })

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAccessToken()
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

let refreshInFlight: Promise<string | null> | null = null

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return null

  if (!refreshInFlight) {
    refreshInFlight = refreshClient
      .post<TokenPair>('/auth/refresh', { refreshToken })
      .then((response) => {
        setTokens(response.data)
        return response.data.accessToken
      })
      .catch(() => {
        clearTokens()
        return null
      })
      .finally(() => {
        refreshInFlight = null
      })
  }
  return refreshInFlight
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined

    if (error.response?.status === 401 && originalRequest && !originalRequest._retried) {
      originalRequest._retried = true
      const newToken = await refreshAccessToken()
      if (newToken) {
        originalRequest.headers.set('Authorization', `Bearer ${newToken}`)
        return apiClient.request(originalRequest)
      }
    }
    return Promise.reject(error)
  },
)

export { refreshAccessToken }
