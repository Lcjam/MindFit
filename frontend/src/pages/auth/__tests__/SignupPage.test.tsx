import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { renderWithProviders } from '../../../test/utils'
import { server } from '../../../test/mocks/server'
import SignupPage from '../SignupPage'
import { useAuthStore } from '../../../store/authStore'

beforeEach(() => {
  useAuthStore.getState().clearAuth()
})

describe('SignupPage', () => {
  async function fillAndSubmit(overrides: Partial<{ email: string; password: string; name: string; role: string }> = {}) {
    const vals = { email: 'new@example.com', password: 'password123', name: '홍길동', role: '내담자', ...overrides }
    await userEvent.type(screen.getByLabelText(/이메일/i), vals.email)
    await userEvent.type(screen.getByLabelText(/비밀번호/i), vals.password)
    await userEvent.type(screen.getByLabelText(/이름/i), vals.name)
    // 역할 선택 (radio or select)
    const roleEl = screen.queryByRole('radio', { name: new RegExp(vals.role, 'i') })
      || screen.queryByRole('option', { name: new RegExp(vals.role, 'i') })
    if (roleEl) await userEvent.click(roleEl)
    await userEvent.click(screen.getByRole('button', { name: /회원가입/i }))
  }

  it('유효한 정보 입력 후 제출하면 signup API를 호출한다', async () => {
    renderWithProviders(<SignupPage />)
    await fillAndSubmit()
    await waitFor(() => expect(useAuthStore.getState().isAuthenticated).toBe(true))
  })

  it('회원가입 성공 시 setAuth가 호출되어 accessToken이 저장된다', async () => {
    renderWithProviders(<SignupPage />)
    await fillAndSubmit()
    await waitFor(() => expect(useAuthStore.getState().accessToken).toBe('mock-access-token'))
  })

  it('이메일 형식이 잘못되면 에러 메시지를 표시한다', async () => {
    renderWithProviders(<SignupPage />)
    await userEvent.type(screen.getByLabelText(/이메일/i), 'invalid-email')
    await userEvent.type(screen.getByLabelText(/비밀번호/i), 'password123')
    await userEvent.type(screen.getByLabelText(/이름/i), '홍길동')
    await userEvent.click(screen.getByRole('button', { name: /회원가입/i }))
    expect(await screen.findByText(/올바른 이메일/i)).toBeInTheDocument()
    expect(useAuthStore.getState().isAuthenticated).toBe(false)
  })

  it('비밀번호가 8자 미만이면 에러 메시지를 표시한다', async () => {
    renderWithProviders(<SignupPage />)
    await userEvent.type(screen.getByLabelText(/이메일/i), 'new@example.com')
    await userEvent.type(screen.getByLabelText(/비밀번호/i), 'short')
    await userEvent.type(screen.getByLabelText(/이름/i), '홍길동')
    await userEvent.click(screen.getByRole('button', { name: /회원가입/i }))
    expect(await screen.findByText(/8자 이상/i)).toBeInTheDocument()
  })

  it('이메일 중복(409)으로 회원가입 실패 시 에러 문구를 표시하고 인증되지 않는다', async () => {
    server.use(
      http.post('*/auth/signup', () =>
        HttpResponse.json(
          { success: false, message: '이미 사용 중인 이메일입니다.' },
          { status: 409 }
        )
      )
    )
    renderWithProviders(<SignupPage />)
    await fillAndSubmit({ email: 'duplicate@example.com' })
    expect(
      await screen.findByText(/회원가입 중 오류가 발생했습니다\. 다시 시도해주세요\./i)
    ).toBeInTheDocument()
    expect(useAuthStore.getState().isAuthenticated).toBe(false)
  })

  it('로그인 링크가 렌더링된다', () => {
    renderWithProviders(<SignupPage />)
    expect(screen.getByRole('link', { name: /로그인/i })).toBeInTheDocument()
  })
})
