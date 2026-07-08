import { act } from 'react'
import { useAuthStore } from '../authStore'

const MOCK_RESPONSE = {
  accessToken: 'access-token',
  refreshToken: 'refresh-token',
  userId: 1,
  role: 'ROLE_CLIENT',
}

beforeEach(() => {
  useAuthStore.getState().clearAuth()
  localStorage.clear()
})

describe('authStore', () => {
  it('초기 상태는 미인증이다', () => {
    const state = useAuthStore.getState()
    expect(state.isAuthenticated).toBe(false)
    expect(state.accessToken).toBeNull()
    expect(state.user).toBeNull()
  })

  it('setAuth 호출 시 토큰과 유저 정보가 저장된다', () => {
    act(() => { useAuthStore.getState().setAuth(MOCK_RESPONSE) })
    const state = useAuthStore.getState()
    expect(state.isAuthenticated).toBe(true)
    expect(state.accessToken).toBe('access-token')
    expect(state.refreshToken).toBe('refresh-token')
    expect(state.user).toEqual({ userId: 1, role: 'ROLE_CLIENT' })
  })

  it('setAuth 호출 시 localStorage에 토큰이 저장된다', () => {
    act(() => { useAuthStore.getState().setAuth(MOCK_RESPONSE) })
    expect(localStorage.getItem('accessToken')).toBe('access-token')
    expect(localStorage.getItem('refreshToken')).toBe('refresh-token')
  })

  it('clearAuth 호출 시 모든 상태가 초기화된다', () => {
    act(() => { useAuthStore.getState().setAuth(MOCK_RESPONSE) })
    act(() => { useAuthStore.getState().clearAuth() })
    const state = useAuthStore.getState()
    expect(state.isAuthenticated).toBe(false)
    expect(state.accessToken).toBeNull()
    expect(state.user).toBeNull()
    expect(localStorage.getItem('accessToken')).toBeNull()
  })
})
