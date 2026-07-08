import axios from 'axios'
import { useAuthStore } from '../store/authStore'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

// Separate axios instance without interceptors — used for token refresh to avoid
// circular dependency / 401 재진입 무한루프. auth.ts의 refresh()도 이 인스턴스를 사용한다.
const plainAxios = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 동시 401 발생 시 갱신 요청이 중복 실행되지 않도록 단일 갱신만 수행하고,
// 나머지 요청은 큐에서 대기시킨다.
let isRefreshing = false
let failedQueue: { resolve: (token: string) => void; reject: (error: unknown) => void }[] = []

function processQueue(error: unknown, token: string | null) {
  failedQueue.forEach(({ resolve, reject }) => {
    if (token) resolve(token)
    else reject(error)
  })
  failedQueue = []
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true

      // 이미 갱신 중이면 큐에 대기했다가 새 토큰으로 재시도한다.
      if (isRefreshing) {
        return new Promise<string>((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then((token) => {
          original.headers.Authorization = `Bearer ${token}`
          return apiClient(original)
        })
      }

      isRefreshing = true
      try {
        const refreshToken = useAuthStore.getState().refreshToken
        if (!refreshToken) throw new Error('no refresh token')
        const { data } = await plainAxios.post('/auth/refresh', { refreshToken })
        const tokens = data.data
        useAuthStore.getState().setAuth(tokens)
        processQueue(null, tokens.accessToken)
        original.headers.Authorization = `Bearer ${tokens.accessToken}`
        return apiClient(original)
      } catch (refreshError) {
        processQueue(refreshError, null)
        useAuthStore.getState().clearAuth()
        window.location.href = '/login'
        return Promise.reject(error)
      } finally {
        isRefreshing = false
      }
    }
    return Promise.reject(error)
  }
)

export default apiClient
export { plainAxios }
