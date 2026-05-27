import apiClient from './axios'
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
  const res = await apiClient.post<ApiResponse<TokenResponse>>('/auth/refresh', { refreshToken })
  return res.data.data
}
