import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import PrivateRoute from './PrivateRoute'
import { useAuthStore } from '../store/authStore'

function makeToken(payload: Record<string, unknown>): string {
  const encoded = btoa(JSON.stringify(payload))
  return `eyJhbGciOiJIUzI1NiJ9.${encoded}.sig`
}

const validToken = makeToken({ exp: Math.floor(Date.now() / 1000) + 3600 })
const expiredToken = makeToken({ exp: 1 })

function setToken(token: string) {
  useAuthStore.getState().setAuth({
    accessToken: token,
    refreshToken: 'refresh',
    userId: 1,
    role: 'ROLE_CLIENT',
  })
}

function renderProtected() {
  return render(
    <MemoryRouter initialEntries={['/protected']}>
      <Routes>
        <Route element={<PrivateRoute />}>
          <Route path="/protected" element={<div>보호 컨텐츠</div>} />
        </Route>
        <Route path="/login" element={<div>로그인 페이지</div>} />
      </Routes>
    </MemoryRouter>
  )
}

beforeEach(() => {
  useAuthStore.getState().clearAuth()
})

describe('PrivateRoute', () => {
  it('유효한 토큰으로 인증된 경우 보호 컨텐츠를 렌더한다', () => {
    setToken(validToken)
    renderProtected()
    expect(screen.getByText('보호 컨텐츠')).toBeInTheDocument()
  })

  it('만료된 토큰이면 로그인 페이지로 리다이렉트한다', () => {
    setToken(expiredToken)
    renderProtected()
    expect(screen.getByText('로그인 페이지')).toBeInTheDocument()
    expect(screen.queryByText('보호 컨텐츠')).not.toBeInTheDocument()
  })

  it('미인증 상태면 로그인 페이지로 리다이렉트한다', () => {
    renderProtected()
    expect(screen.getByText('로그인 페이지')).toBeInTheDocument()
    expect(screen.queryByText('보호 컨텐츠')).not.toBeInTheDocument()
  })
})
