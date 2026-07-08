import { http, HttpResponse } from 'msw'

const MOCK_TOKEN_RESPONSE = {
  accessToken: 'mock-access-token',
  refreshToken: 'mock-refresh-token',
  userId: 1,
  role: 'ROLE_CLIENT',
}

export const handlers = [
  http.get('/api/v1/health', () =>
    HttpResponse.json({ success: true, data: { status: 'UP', service: 'MindFit API' } })
  ),
  http.post('/api/v1/auth/signup', () =>
    HttpResponse.json({ success: true, message: '회원가입이 완료되었습니다.', data: MOCK_TOKEN_RESPONSE }, { status: 201 })
  ),
  http.post('/api/v1/auth/login', () =>
    HttpResponse.json({ success: true, data: MOCK_TOKEN_RESPONSE })
  ),
  http.post('/api/v1/auth/logout', () =>
    HttpResponse.json({ success: true, data: null })
  ),
  http.post('/api/v1/auth/refresh', () =>
    HttpResponse.json({ success: true, data: { ...MOCK_TOKEN_RESPONSE, accessToken: 'new-access-token' } })
  ),
]
