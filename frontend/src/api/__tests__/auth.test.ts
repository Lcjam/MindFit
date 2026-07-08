import { signup, login, logout, refresh } from '../auth'

describe('auth API', () => {
  describe('login', () => {
    it('성공 시 TokenResponse를 반환한다', async () => {
      const result = await login({ email: 'test@example.com', password: 'password123' })
      expect(result.accessToken).toBe('mock-access-token')
      expect(result.refreshToken).toBe('mock-refresh-token')
      expect(result.userId).toBe(1)
      expect(result.role).toBe('ROLE_CLIENT')
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
  })
})
