import { vi } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../../test/mocks/server'
import { signup, login, logout, refresh } from '../auth'
import { useAuthStore } from '../../store/authStore'

// axios interceptor가 401 시 window.location.href = '/login' 을 실행한다.
// setter를 no-op으로 막아 MSW의 URL 베이스가 오염되지 않도록 한다.
beforeAll(() => {
  vi.stubGlobal('location', {
    get href() { return 'http://localhost' },
    set href(_val: string) { /* no-op */ },
  })
})
afterAll(() => {
  vi.unstubAllGlobals()
})
beforeEach(() => {
  useAuthStore.getState().clearAuth()
})

describe('auth API', () => {
  describe('login', () => {
    it('성공 시 TokenResponse를 반환한다', async () => {
      const result = await login({ email: 'test@example.com', password: 'password123' })
      expect(result.accessToken).toBe('mock-access-token')
      expect(result.refreshToken).toBe('mock-refresh-token')
      expect(result.userId).toBe(1)
      expect(result.role).toBe('ROLE_CLIENT')
    })

    it('자격증명이 잘못되면 에러를 던진다', async () => {
      server.use(
        http.post('/api/v1/auth/login', () =>
          HttpResponse.json(
            { success: false, message: '이메일 또는 비밀번호가 올바르지 않습니다.' },
            { status: 401 }
          )
        )
      )
      await expect(login({ email: 'wrong@example.com', password: 'wrong' })).rejects.toThrow()
    })
  })

  describe('signup', () => {
    it('성공 시 TokenResponse를 반환한다', async () => {
      const result = await signup({
        email: 'new@example.com',
        password: 'password123',
        name: '홍길동',
        role: 'ROLE_CLIENT',
      })
      expect(result.accessToken).toBe('mock-access-token')
    })

    it('중복 이메일이면 에러를 던진다', async () => {
      server.use(
        http.post('/api/v1/auth/signup', () =>
          HttpResponse.json(
            { success: false, message: '이미 사용 중인 이메일입니다.' },
            { status: 409 }
          )
        )
      )
      await expect(
        signup({ email: 'dup@example.com', password: 'password123', name: '홍길동', role: 'ROLE_CLIENT' })
      ).rejects.toThrow()
    })
  })

  describe('logout', () => {
    it('에러 없이 완료된다', async () => {
      await expect(logout()).resolves.toBeUndefined()
    })
  })

  describe('refresh', () => {
    it('새 accessToken을 반환한다', async () => {
      const result = await refresh('mock-refresh-token')
      expect(result.accessToken).toBe('new-access-token')
    })

    it('유효하지 않은 refreshToken이면 에러를 던진다', async () => {
      server.use(
        http.post('/api/v1/auth/refresh', () =>
          HttpResponse.json(
            { success: false, message: '유효하지 않은 토큰입니다.' },
            { status: 401 }
          )
        )
      )
      await expect(refresh('invalid-refresh-token')).rejects.toThrow()
    })
  })
})
