import { create } from 'zustand'
import { TokenResponse, AuthUser } from '../types/auth'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: AuthUser | null
  isAuthenticated: boolean
  setAuth: (response: TokenResponse) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthState>()((set) => ({
  accessToken: localStorage.getItem('accessToken'),
  refreshToken: localStorage.getItem('refreshToken'),
  user: (() => {
    try {
      const u = localStorage.getItem('authUser')
      return u ? JSON.parse(u) : null
    } catch { return null }
  })(),
  isAuthenticated: !!localStorage.getItem('accessToken'),

  setAuth: (response: TokenResponse) => {
    localStorage.setItem('accessToken', response.accessToken)
    localStorage.setItem('refreshToken', response.refreshToken)
    const user: AuthUser = { userId: response.userId, role: response.role }
    localStorage.setItem('authUser', JSON.stringify(user))
    set({ accessToken: response.accessToken, refreshToken: response.refreshToken, user, isAuthenticated: true })
  },

  clearAuth: () => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('authUser')
    set({ accessToken: null, refreshToken: null, user: null, isAuthenticated: false })
  },
}))
