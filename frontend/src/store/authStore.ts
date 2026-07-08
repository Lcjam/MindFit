import { create } from 'zustand'
import { TokenResponse, AuthUser } from '../types/auth'

// Safari Private Mode 등에서 localStorage 접근이 SecurityError를 던질 수 있다.
// 스토어 초기화·갱신이 예외로 중단되지 않도록 모든 접근을 안전하게 감싼다.
const safeGet = (key: string): string | null => {
  try { return localStorage.getItem(key) } catch { return null }
}

const safeSet = (key: string, value: string): void => {
  try { localStorage.setItem(key, value) } catch { /* no-op */ }
}

const safeRemove = (key: string): void => {
  try { localStorage.removeItem(key) } catch { /* no-op */ }
}

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: AuthUser | null
  isAuthenticated: boolean
  setAuth: (response: TokenResponse) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthState>()((set) => ({
  accessToken: safeGet('accessToken'),
  refreshToken: safeGet('refreshToken'),
  user: (() => {
    try {
      const u = safeGet('authUser')
      return u ? JSON.parse(u) : null
    } catch { return null }
  })(),
  isAuthenticated: !!safeGet('accessToken'),

  setAuth: (response: TokenResponse) => {
    safeSet('accessToken', response.accessToken)
    safeSet('refreshToken', response.refreshToken)
    const user: AuthUser = { userId: response.userId, role: response.role }
    safeSet('authUser', JSON.stringify(user))
    set({ accessToken: response.accessToken, refreshToken: response.refreshToken, user, isAuthenticated: true })
  },

  clearAuth: () => {
    safeRemove('accessToken')
    safeRemove('refreshToken')
    safeRemove('authUser')
    set({ accessToken: null, refreshToken: null, user: null, isAuthenticated: false })
  },
}))
