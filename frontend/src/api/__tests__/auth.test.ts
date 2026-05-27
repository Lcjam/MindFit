import { vi } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../../test/mocks/server'
import { signup, login, logout, refresh } from '../auth'
import apiClient from '../axios'
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

  describe('apiClient 401 인터셉터', () => {
    it('401 → refresh 성공 → 원요청을 새 토큰으로 재시도해 성공한다', async () => {
      useAuthStore.getState().setAuth({
        accessToken: 'expired-access-token',
        refreshToken: 'mock-refresh-token',
        userId: 1,
        role: 'ROLE_CLIENT',
      })

      let protectedCalls = 0
      let retryAuthHeader: string | null = null
      server.use(
        http.get('/api/v1/protected', ({ request }) => {
          protectedCalls += 1
          const auth = request.headers.get('Authorization')
          if (protectedCalls === 1) {
            return HttpResponse.json({ success: false, message: 'unauthorized' }, { status: 401 })
          }
          retryAuthHeader = auth
          return HttpResponse.json({ success: true, data: { ok: true } })
        })
      )

      const res = await apiClient.get('/protected')

      expect(res.data.data.ok).toBe(true)
      expect(protectedCalls).toBe(2)
      // 갱신된 토큰으로 재시도되어야 한다
      expect(retryAuthHeader).toBe('Bearer new-access-token')
      // 스토어에도 새 토큰이 반영되어야 한다
      expect(useAuthStore.getState().accessToken).toBe('new-access-token')
    })

    it('refresh 실패(갱신 401) 시 clearAuth가 호출된다', async () => {
      useAuthStore.getState().setAuth({
        accessToken: 'expired-access-token',
        refreshToken: 'mock-refresh-token',
        userId: 1,
        role: 'ROLE_CLIENT',
      })
      const clearSpy = vi.spyOn(useAuthStore.getState(), 'clearAuth')

      server.use(
        http.get('/api/v1/protected', () =>
          HttpResponse.json({ success: false, message: 'unauthorized' }, { status: 401 })
        ),
        http.post('/api/v1/auth/refresh', () =>
          HttpResponse.json({ success: false, message: '유효하지 않은 토큰입니다.' }, { status: 401 })
        )
      )

      await expect(apiClient.get('/protected')).rejects.toBeTruthy()
      expect(clearSpy).toHaveBeenCalled()
      expect(useAuthStore.getState().accessToken).toBeNull()
      clearSpy.mockRestore()
    })

    it('동시 401 두 요청 시 refresh 엔드포인트는 1회만 호출되고 두 요청 모두 성공한다', async () => {
      useAuthStore.getState().setAuth({
        accessToken: 'expired-access-token',
        refreshToken: 'mock-refresh-token',
        userId: 1,
        role: 'ROLE_CLIENT',
      })

      let refreshCalls = 0
      const seenByPath: Record<string, number> = { a: 0, b: 0 }
      server.use(
        http.post('/api/v1/auth/refresh', () => {
          refreshCalls += 1
          return HttpResponse.json({
            success: true,
            data: {
              accessToken: 'new-access-token',
              refreshToken: 'mock-refresh-token',
              userId: 1,
              role: 'ROLE_CLIENT',
            },
          })
        }),
        http.get('/api/v1/res-:id', ({ params }) => {
          const id = params.id as string
          seenByPath[id] = (seenByPath[id] ?? 0) + 1
          if (seenByPath[id] === 1) {
            return HttpResponse.json({ success: false, message: 'unauthorized' }, { status: 401 })
          }
          return HttpResponse.json({ success: true, data: { id } })
        })
      )

      const [r1, r2] = await Promise.all([
        apiClient.get('/res-a'),
        apiClient.get('/res-b'),
      ])

      expect(refreshCalls).toBe(1)
      expect(r1.data.data.id).toBe('a')
      expect(r2.data.data.id).toBe('b')
    })
  })
})
