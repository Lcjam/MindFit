import apiClient, { plainAxios } from './axios'
import { SignupRequest, LoginRequest, TokenResponse, ApiResponse } from '../types/auth'

export async function signup(data: SignupRequest): Promise<TokenResponse> {
  const res = await apiClient.post<ApiResponse<TokenResponse>>('/auth/signup', data)
  return res.data.data
}

export async function login(data: LoginRequest): Promise<TokenResponse> {
  const res = await apiClient.post<ApiResponse<TokenResponse>>('/auth/login', data)
  return res.data.data
}

export async function logout(): Promise<void> {
  await apiClient.post('/auth/logout')
}

export async function refresh(refreshToken: string): Promise<TokenResponse> {
  // 인터셉터 없는 plainAxios 사용 — 401 응답 시 refresh 인터셉터 재진입 무한루프 방지
  const res = await plainAxios.post<ApiResponse<TokenResponse>>('/auth/refresh', { refreshToken })
  return res.data.data
}
