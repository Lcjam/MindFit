import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import App from './App'
import { useAuthStore } from './store/authStore'

function makeToken(payload: Record<string, unknown>): string {
  const encoded = btoa(JSON.stringify(payload))
  return `eyJhbGciOiJIUzI1NiJ9.${encoded}.sig`
}

const validToken = makeToken({ exp: Math.floor(Date.now() / 1000) + 3600 })

function renderApp(path: string) {
  window.history.pushState({}, '', path)
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  )
}

beforeEach(() => {
  useAuthStore.getState().clearAuth()
})

describe('App routing', () => {
  it('"/login" 접근 시 로그인 페이지를 렌더한다', () => {
    renderApp('/login')
    expect(screen.getByRole('button', { name: /로그인/i })).toBeInTheDocument()
  })

  it('미인증 상태에서 "/" 접근 시 로그인 페이지로 리다이렉트한다', () => {
    renderApp('/')
    expect(screen.getByRole('button', { name: /로그인/i })).toBeInTheDocument()
    expect(screen.queryByText('MindFit 홈')).not.toBeInTheDocument()
  })

  it('알 수 없는 경로 접근 시 로그인 페이지로 리다이렉트한다', () => {
    renderApp('/unknown-route')
    expect(screen.getByRole('button', { name: /로그인/i })).toBeInTheDocument()
  })

  it('유효한 토큰으로 인증된 경우 "/" 접근 시 홈을 렌더한다', () => {
    useAuthStore.getState().setAuth({
      accessToken: validToken,
      refreshToken: 'refresh',
      userId: 1,
      role: 'ROLE_CLIENT',
    })
    renderApp('/')
    expect(screen.getByText('MindFit 홈')).toBeInTheDocument()
  })
})
