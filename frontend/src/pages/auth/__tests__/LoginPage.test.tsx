import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { renderWithProviders } from '../../../test/utils'
import { server } from '../../../test/mocks/server'
import LoginPage from '../LoginPage'
import { useAuthStore } from '../../../store/authStore'

beforeEach(() => {
  useAuthStore.getState().clearAuth()
})

describe('LoginPage', () => {
  it('이메일·비밀번호 입력 후 제출하면 login API를 호출한다', async () => {
    renderWithProviders(<LoginPage />)
    await userEvent.type(screen.getByLabelText(/이메일/i), 'test@example.com')
    await userEvent.type(screen.getByLabelText(/비밀번호/i), 'password123')
    await userEvent.click(screen.getByRole('button', { name: /로그인/i }))
    await waitFor(() => expect(useAuthStore.getState().isAuthenticated).toBe(true))
  })

  it('로그인 성공 시 setAuth가 호출되어 isAuthenticated가 true가 된다', async () => {
    renderWithProviders(<LoginPage />)
    await userEvent.type(screen.getByLabelText(/이메일/i), 'test@example.com')
    await userEvent.type(screen.getByLabelText(/비밀번호/i), 'password123')
    await userEvent.click(screen.getByRole('button', { name: /로그인/i }))
    await waitFor(() => {
      expect(useAuthStore.getState().accessToken).toBe('mock-access-token')
    })
  })

  it('이메일 형식이 잘못되면 에러 메시지를 표시하고 API를 호출하지 않는다', async () => {
    renderWithProviders(<LoginPage />)
    await userEvent.type(screen.getByLabelText(/이메일/i), 'invalid-email')
    await userEvent.type(screen.getByLabelText(/비밀번호/i), 'password123')
    await userEvent.click(screen.getByRole('button', { name: /로그인/i }))
    expect(await screen.findByText(/올바른 이메일/i)).toBeInTheDocument()
    expect(useAuthStore.getState().isAuthenticated).toBe(false)
  })

  it('비밀번호가 비어있으면 에러 메시지를 표시한다', async () => {
    renderWithProviders(<LoginPage />)
    await userEvent.type(screen.getByLabelText(/이메일/i), 'test@example.com')
    await userEvent.click(screen.getByRole('button', { name: /로그인/i }))
    expect(await screen.findByText(/비밀번호를 입력/i)).toBeInTheDocument()
  })

  it('잘못된 자격증명으로 로그인 실패(401) 시 에러 문구를 표시하고 인증되지 않는다', async () => {
    server.use(
      http.post('*/auth/login', () =>
        HttpResponse.json(
          { success: false, message: '아이디 또는 비밀번호가 올바르지 않습니다.' },
          { status: 401 }
        )
      )
    )
    renderWithProviders(<LoginPage />)
    await userEvent.type(screen.getByLabelText(/이메일/i), 'wrong@example.com')
    await userEvent.type(screen.getByLabelText(/비밀번호/i), 'wrongpassword')
    await userEvent.click(screen.getByRole('button', { name: /로그인/i }))
    expect(await screen.findByText(/이메일 또는 비밀번호를 확인해주세요/i)).toBeInTheDocument()
    expect(useAuthStore.getState().isAuthenticated).toBe(false)
  })

  it('회원가입 링크가 렌더링된다', () => {
    renderWithProviders(<LoginPage />)
    expect(screen.getByRole('link', { name: /회원가입/i })).toBeInTheDocument()
  })
})
